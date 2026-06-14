package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.networking.packet.PacketRWRThreatSync;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

/**
 * Server side manager for RWR threats.
 * Ported from MCHeli Reforged.
 */
public class MCH_RWRThreatManager {
    private static final long LOCK_REPORT_EXPIRE_TICK = 40L;
    private static final int MIN_SCAN_DISTANCE = 15;
    private static final int LOCK_EVENT_TTL_TICK = 8;
    private static final int SEARCH_EVENT_TTL_TICK = 40;
    private static final int MISSILE_EVENT_TTL_TICK = 8;
    private static final int MAX_RADAR_SCAN_TICK = 400;
    private static final long SCAN_COUNTER_STALE_TICK = 400L;
    private static final long THREAT_STALE_TICK = 200L;

    private final Map<Integer, RadarTrackingReport> trackingReports = new HashMap<>();
    private final Map<Integer, RadarTrackingReport> gunnerTrackingReports = new HashMap<>();
    private final Map<Long, Integer> scanHitCounters = new HashMap<>();
    private final Map<Long, Long> scanPairLastSlot = new HashMap<>();
    private final Map<Long, Long> scanPairLastTouchedTick = new HashMap<>();
    private final Map<ThreatKey, ActiveThreatState> activeThreats = new HashMap<>();

    private long snapshotSeq = 0L;
    private long threatManagerTick = 0L;

    public long getCurrentTick() {
        return this.threatManagerTick;
    }

    public void reportRadarTracking(EntityPlayerMP reporter, int emitterAircraftId, int trackingTargetId) {
        if (reporter == null || reporter.world == null || emitterAircraftId <= 0) {
            return;
        }
        Entity emitter = reporter.world.getEntityByID(emitterAircraftId);
        if (!(emitter instanceof MCH_EntityAircraft ac)) {
            return;
        }
        if (!canEmitRadarThreat(ac)) {
            trackingReports.remove(emitterAircraftId);
            return;
        }
        if (!isReporterControllingAircraft(ac, reporter)) {
            return;
        }
        long now = this.threatManagerTick;
        if (trackingTargetId <= 0) {
            trackingReports.remove(emitterAircraftId);
        } else {
            trackingReports.put(emitterAircraftId, new RadarTrackingReport(trackingTargetId, now + LOCK_REPORT_EXPIRE_TICK));
        }
    }

    public void reportGunnerTracking(MCH_EntityAircraft emitter, Entity tracker, Entity targetEntity) {
        if (emitter == null || emitter.world == null) {
            return;
        }
        int emitterAircraftId = emitter.getEntityId();
        if (emitterAircraftId <= 0) {
            return;
        }
        if (!canEmitRadarThreat(emitter)) {
            gunnerTrackingReports.remove(emitterAircraftId);
            return;
        }
        if (tracker != null && emitter.getSeatIdByEntity(tracker) < 0) {
            return;
        }
        long now = this.threatManagerTick;
        MCH_EntityAircraft target = resolveThreatReceiverAircraft(targetEntity);
        if (target == null || target == emitter || target.isDead || isSameTeam(emitter, target)) {
            gunnerTrackingReports.remove(emitterAircraftId);
            return;
        }
        gunnerTrackingReports.put(emitterAircraftId, new RadarTrackingReport(target.getEntityId(), now + LOCK_REPORT_EXPIRE_TICK));
    }

    public void serverTick() {
        this.threatManagerTick++;
        long now = this.threatManagerTick;
        snapshotSeq++;

        for (World world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
            List<Entity> loaded = world.loadedEntityList;
            for (Entity entity : loaded) {
                if (entity instanceof MCH_EntityAircraft emitter) {
                    if (canEmitRadarThreat(emitter)) {
                        processRadarEmission(emitter, now);
                    }
                } else if (entity instanceof MCH_EntityBaseBullet bullet) {
                    processMissileEmission(bullet, now);
                }
            }
        }

        cleanup(now);
        syncThreatsToPlayers(now);
    }

    private void processRadarEmission(MCH_EntityAircraft emitter, long now) {
        MCH_AircraftInfo info = emitter.getAcInfo();
        if (info == null) return;

        double maxRange = info.radarMaxTargetRange > 0.0F ? info.radarMaxTargetRange : 4096.0D;
        float scanAz = MathHelper.clamp(info.radarScanAzimuthDeg, 0.0F, 360.0F);
        float scanEl = MathHelper.clamp(info.radarScanElevationDeg, 0.0F, 180.0F);
        int radarScanTick = normalizeRadarScanTick(info.radarScanTick);
        int hitNeed = getScanHitNeed(radarScanTick);
        long scanSlot = now / radarScanTick;
        String sourceName = getEmitterRwrName(emitter);

        TrackingSource tracking = selectTrackingSource(emitter.getEntityId(), now);

        for (World world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
            if (world != emitter.world) continue;
            for (Entity entity : world.loadedEntityList) {
                if (!(entity instanceof MCH_EntityAircraft target) || target == emitter || target.isDead) continue;
                if (!isTargetRwrReceivable(target)) continue;

                if (tracking != null && tracking.report.targetEntityId == target.getEntityId()) {
                    // STT (Lock-on)
                    touchThreat(target, emitter.getEntityId(), getEmitterKind(emitter), MCH_RWRThreatEvent.MODE_STT, sourceName, now + LOCK_EVENT_TTL_TICK, maxRange);
                } else if (!isTargetCountermeasureActive(target)) {
                    // Search mode
                    if (isTargetInsideScanCone(emitter, target, maxRange, scanAz, scanEl)) {
                        long pairKey = toPairKey(emitter.getEntityId(), target.getEntityId());
                        Long lastSlot = scanPairLastSlot.get(pairKey);
                        if (lastSlot == null || lastSlot != scanSlot) {
                            scanPairLastSlot.put(pairKey, scanSlot);
                            int hits = scanHitCounters.getOrDefault(pairKey, 0) + 1;
                            scanHitCounters.put(pairKey, hits);
                            scanPairLastTouchedTick.put(pairKey, now);
                            if (hits >= hitNeed) {
                                touchThreat(target, emitter.getEntityId(), getEmitterKind(emitter), MCH_RWRThreatEvent.MODE_SEARCH, sourceName, now + SEARCH_EVENT_TTL_TICK, maxRange);
                            }
                        }
                    } else {
                        decreaseScanHitCounter(emitter.getEntityId(), target.getEntityId());
                    }
                }
            }
        }
    }

    private void processMissileEmission(MCH_EntityBaseBullet bullet, long now) {
        if (bullet.isDead) return;
        Entity targetEntity = bullet.targetEntity;
        MCH_EntityAircraft target = resolveThreatReceiverAircraft(targetEntity);
        if (target == null || target.isDead) return;

        int emitterId = bullet.shootingEntity != null ? bullet.shootingEntity.getEntityId() : bullet.getEntityId();
        byte mode = MCH_RWRThreatEvent.MODE_MSL_ACTIVE; // Default to active for now

        String sourceName = bullet.getName();
        touchThreatFromPosition(target, emitterId, MCH_RWRThreatEvent.EMITTER_MISSILE, mode, sourceName, now + MISSILE_EVENT_TTL_TICK, bullet.posX, bullet.posY, bullet.posZ, 2048.0D);
    }

    private void touchThreat(MCH_EntityAircraft receiver, int emitterId, byte emitterKind, byte mode, String sourceName, long expireTick, double maxRange) {
        touchThreatFromPosition(receiver, emitterId, emitterKind, mode, sourceName, expireTick, 0, 0, 0, maxRange, true);
    }

    private void touchThreatFromPosition(MCH_EntityAircraft receiver, int emitterId, byte emitterKind, byte mode, String sourceName, long expireTick, double ex, double ey, double ez, double maxRange) {
        touchThreatFromPosition(receiver, emitterId, emitterKind, mode, sourceName, expireTick, ex, ey, ez, maxRange, false);
    }

    private void touchThreatFromPosition(MCH_EntityAircraft receiver, int emitterId, byte emitterKind, byte mode, String sourceName, long expireTick, double ex, double ey, double ez, double maxRange, boolean useEmitterEntity) {
        Entity emitter = useEmitterEntity ? receiver.world.getEntityByID(emitterId) : null;
        double dx = (useEmitterEntity && emitter != null) ? emitter.posX - receiver.posX : ex - receiver.posX;
        double dy = (useEmitterEntity && emitter != null) ? emitter.posY - receiver.posY : ey - receiver.posY;
        double dz = (useEmitterEntity && emitter != null) ? emitter.posZ - receiver.posZ : ez - receiver.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        double dist = Math.sqrt(distSq);

        float bearing = getRelativeBearing(receiver, dx, dz);
        float strength = (float) (1.0 - MathHelper.clamp(dist / maxRange, 0.0, 1.0));
        float confidence = 1.0F;

        ThreatKey key = new ThreatKey(receiver.getEntityId(), emitterId, mode);
        ActiveThreatState state = activeThreats.computeIfAbsent(key, k -> new ActiveThreatState());
        state.key = key;
        state.emitterKind = emitterKind;
        state.sourceName = sourceName;
        state.bearingDeg = bearing;
        state.strength = strength;
        state.confidence = confidence;
        state.distanceMeters = (float) dist;
        state.expireTick = expireTick;
        state.lastTouchTick = this.threatManagerTick;
    }

    private void syncThreatsToPlayers(long now) {
        for (EntityPlayer player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
            if (ac == null || !isTargetRwrReceivable(ac)) continue;

            List<MCH_RWRThreatEvent> events = collectThreatEventsForReceiver(ac.getEntityId(), now);
            MCH_RWRThreatTable table = new MCH_RWRThreatTable(ac.getEntityId(), snapshotSeq, events);
            new PacketRWRThreatSync(table).sendToPlayer((EntityPlayerMP) player);
        }
    }

    private List<MCH_RWRThreatEvent> collectThreatEventsForReceiver(int receiverId, long now) {
        List<MCH_RWRThreatEvent> list = new ArrayList<>();
        for (ActiveThreatState state : activeThreats.values()) {
            if (state.key.receiverId == receiverId && state.expireTick >= now) {
                list.add(new MCH_RWRThreatEvent(state.key.emitterId, state.emitterKind, state.key.mode, state.sourceName, state.bearingDeg, state.strength, state.confidence));
            }
        }
        list.sort((a, b) -> Float.compare(b.strength, a.strength));
        if (list.size() > 12) {
            return new ArrayList<>(list.subList(0, 12));
        }
        return list;
    }

    private TrackingSource selectTrackingSource(int emitterAircraftId, long now) {
        RadarTrackingReport report = trackingReports.get(emitterAircraftId);
        if (report != null && report.expireTick >= now && report.targetEntityId > 0) {
            return new TrackingSource(report, "PACKET");
        }
        report = gunnerTrackingReports.get(emitterAircraftId);
        if (report != null && report.expireTick >= now && report.targetEntityId > 0) {
            return new TrackingSource(report, "GUNNER");
        }
        return null;
    }

    private MCH_EntityAircraft resolveThreatReceiverAircraft(Entity target) {
        if (target instanceof MCH_EntityAircraft) return (MCH_EntityAircraft) target;
        if (target instanceof MCH_EntitySeat seat) return seat.getParent();
        return null;
    }

    private float getRelativeBearing(MCH_EntityAircraft receiver, double dx, double dz) {
        double absYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double yaw = receiver.rotationYaw;
        double rel = absYaw - yaw;
        while (rel > 180.0D) rel -= 360.0D;
        while (rel < -180.0D) rel += 360.0D;
        return (float) rel;
    }

    private byte getEmitterKind(MCH_EntityAircraft emitter) {
        return MCH_RWRThreatEvent.EMITTER_AIRCRAFT;
    }

    private boolean canEmitRadarThreat(MCH_EntityAircraft ac) {
        return ac != null && ac.getAcInfo() != null && ac.getAcInfo().enableRadar && ac.isRadarEnabledRuntime() && !ac.isDestroyed();
    }

    private boolean isReporterControllingAircraft(MCH_EntityAircraft ac, EntityPlayerMP reporter) {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(reporter) == ac;
    }

    private boolean isTargetRwrReceivable(MCH_EntityAircraft target) {
        return target != null && target.getAcInfo() != null && target.getAcInfo().hasRWR;
    }

    private boolean isTargetCountermeasureActive(MCH_EntityAircraft target) {
        // Reforged: an aircraft running chaff or an ECM jammer (or being jammed) is hidden from
        // enemy search-mode radar (STT/lock-on is unaffected).
        if (target == null || target.getAcInfo() == null) {
            return false;
        }
        return target.isChaffUsing() || target.isECMJammerUsing() || target.jammingTick > 0;
    }

    private boolean isTargetInsideScanCone(MCH_EntityAircraft emitter, MCH_EntityAircraft target, double maxRange, float scanAzDeg, float scanElDeg) {
        double dx = target.posX - emitter.posX;
        double dy = target.posY - emitter.posY;
        double dz = target.posZ - emitter.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < MIN_SCAN_DISTANCE * MIN_SCAN_DISTANCE || distSq > maxRange * maxRange) return false;

        double yaw = Math.toRadians(emitter.rotationYaw);
        double pitch = Math.toRadians(emitter.rotationPitch);
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);

        double horiz = Math.sqrt(dx * dx + dz * dz);
        double targetPitch = Math.toDegrees(Math.atan2(dy, horiz));
        double forwardPitch = Math.toDegrees(Math.atan2(fy, Math.sqrt(fx * fx + fz * fz)));
        double relElev = targetPitch - forwardPitch;

        double fhl = Math.sqrt(fx * fx + fz * fz);
        if (fhl <= 1.0E-6D) return false;
        double fhx = fx / fhl;
        double fhz = fz / fhl;
        double thx = dx / horiz;
        double thz = dz / horiz;
        double dot = MathHelper.clamp(fhx * thx + fhz * thz, -1.0, 1.0);
        double relAz = Math.toDegrees(Math.acos(dot));
        if (fhx * thz - fhz * thx < 0) relAz = -relAz;

        if (Math.abs(relAz) > scanAzDeg * 0.5D) return false;
        return Math.abs(relElev) <= scanElDeg * 0.5D;
    }

    private int getScanHitNeed(int radarScanTick) {
        if (radarScanTick >= 80) return 1;
        if (radarScanTick > 20) return 2;
        return 4;
    }

    private int normalizeRadarScanTick(int radarScanTick) {
        if (radarScanTick <= 0) return 1;
        return Math.min(radarScanTick, MAX_RADAR_SCAN_TICK);
    }

    private void decreaseScanHitCounter(int emitterId, int targetId) {
        long key = toPairKey(emitterId, targetId);
        scanHitCounters.computeIfPresent(key, (k, v) -> v > 0 ? v - 1 : 0);
    }

    private boolean isSameTeam(MCH_EntityAircraft emitter, MCH_EntityAircraft target) {
        Entity eOp = getPrimaryOperator(emitter);
        Entity tOp = getPrimaryOperator(target);
        if (eOp instanceof EntityLivingBase a && tOp instanceof EntityLivingBase b) {
            return a.isOnSameTeam(b);
        }
        return false;
    }

    private Entity getPrimaryOperator(MCH_EntityAircraft ac) {
        if (ac.getRiddenByEntity() != null) return ac.getRiddenByEntity();
        for (int sid = 1; sid <= ac.getSeatNum(); sid++) {
            Entity crew = ac.getEntityBySeatId(sid);
            if (crew != null) return crew;
        }
        return null;
    }

    private String getEmitterRwrName(MCH_EntityAircraft emitter) {
        return emitter.getAcInfo() != null ? emitter.getAcInfo().nameOnRWR : "?";
    }

    private void cleanup(long now) {
        trackingReports.entrySet().removeIf(e -> e.getValue().expireTick < now);
        gunnerTrackingReports.entrySet().removeIf(e -> e.getValue().expireTick < now);
        scanPairLastTouchedTick.entrySet().removeIf(e -> now - e.getValue() > SCAN_COUNTER_STALE_TICK);
        scanPairLastSlot.entrySet().removeIf(e -> !scanPairLastTouchedTick.containsKey(e.getKey()));
        scanHitCounters.entrySet().removeIf(e -> !scanPairLastTouchedTick.containsKey(e.getKey()));
        activeThreats.entrySet().removeIf(e -> e.getValue().expireTick < now || now - e.getValue().lastTouchTick > THREAT_STALE_TICK);
    }

    private long toPairKey(int a, int b) {
        return ((long) a << 32) ^ (b & 0xFFFFFFFFL);
    }

    private static class RadarTrackingReport {
        final int targetEntityId;
        final long expireTick;

        RadarTrackingReport(int targetEntityId, long expireTick) {
            this.targetEntityId = targetEntityId;
            this.expireTick = expireTick;
        }
    }

    private static class TrackingSource {
        final RadarTrackingReport report;
        final String source;

        TrackingSource(RadarTrackingReport report, String source) {
            this.report = report;
            this.source = source;
        }
    }

    private static class ThreatKey {
        final int receiverId;
        final int emitterId;
        final byte mode;

        ThreatKey(int receiverId, int emitterId, byte mode) {
            this.receiverId = receiverId;
            this.emitterId = emitterId;
            this.mode = mode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ThreatKey o)) return false;
            return this.receiverId == o.receiverId && this.emitterId == o.emitterId && this.mode == o.mode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiverId, emitterId, mode);
        }
    }

    private static class ActiveThreatState {
        ThreatKey key;
        byte emitterKind;
        String sourceName;
        float bearingDeg;
        float strength;
        float confidence;
        float distanceMeters;
        long expireTick;
        long lastTouchTick;
    }
}
