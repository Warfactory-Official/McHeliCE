package com.norwood.mcheli.wingman.handler;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.uav.UAVTracker;
import com.norwood.mcheli.weapon.MCH_EntityTvMissile;
import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.config.WingmanConfig;
import com.norwood.mcheli.wingman.util.McheliReflect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streams terrain chunks around a UAV to its operating/viewing player — specifically the chunks
 * outside the player's normal view radius — so the client renders the ground beneath a distant UAV
 * instead of void. Two sources are handled:
 *
 * <ul>
 *   <li><b>Controlled UAVs</b>: a UAV whose station has a player rider (actively flown).</li>
 *   <li><b>Previewed UAVs</b>: the UAV currently selected in a player's open UAV-station screen, so
 *       the live camera-feed viewport renders real terrain before the player connects. Preview
 *       targets are registered by {@code PacketUavPreviewSelect} and auto-expire when the client
 *       stops sending heartbeats (i.e. the screen closed).</li>
 * </ul>
 *
 * <p>Reconciliation is done per player over the union of their desired chunks, so a player who
 * controls one UAV and previews another never double-manages a shared chunk. Runs on Phase.END so
 * entity positions are final, and only manages out-of-view chunks (avoiding spurious
 * SPacketUnloadChunk for in-view chunks that would cause warps).
 *
 * <p>{@code PlayerChunkMap#playerViewRadius} and {@code #getOrCreateEntry} are widened to public via
 * mcheli_at.cfg (Access Transformer) rather than reflection.
 */
public class UavChunkStreamer {

    /**
     * Streaming radius (chunks) around a UAV that is only being PREVIEWED in a station screen, not
     * actively controlled — just enough terrain for the small camera-feed viewport. An
     * actively-controlled UAV instead streams the operator's full view radius (already clamped to the
     * server's view distance) so it loads the same area a player would.
     */
    private static final int PREVIEW_STREAM_RADIUS = 2;

    /**
     * While a player is actively controlling a UAV, keep only this radius of chunks around their
     * (station-riding) body — 0 = just the chunk they are in. The rest of their body view square is
     * wasted while their view is at the UAV, so we drop it to save bandwidth/server load.
     */
    private static final int BODY_KEEP_RADIUS = 0;

    /** A preview target expires this many ticks after its last heartbeat (screen closed). */
    private static final int PREVIEW_TIMEOUT_TICKS = 60;

    /** playerUUID -> UAV being previewed (set by PacketUavPreviewSelect). */
    private static final Map<UUID, PreviewReq> PREVIEW = new ConcurrentHashMap<>();

    /**
     * Snapshot of the out-of-view chunks streamed to each player this tick. Lets the entity tracker
     * make entities inside a streamed region visible to that player even though their body is far
     * away (camera-aware visibility — read by {@code TrackerHook#isVisibleFromViewOrigin}). Replaced
     * wholesale each tick on the server thread.
     */
    private static volatile Map<UUID, Set<ChunkPos>> STREAMED_SNAPSHOT = Collections.emptyMap();

    /** Out-of-view chunk subscriptions we have added, keyed by player UUID. */
    private final Map<UUID, Set<ChunkPos>> subscribed = new HashMap<>();

    /** Body-square chunks we have removed a controlling player from (to restore when control ends). */
    private final Map<UUID, Set<ChunkPos>> suppressed = new HashMap<>();

    /** Last (body, UAV) chunk positions a controller's body-suppression was computed for. */
    private final Map<UUID, Ctl> lastCtl = new HashMap<>();

    /** Registers/refreshes the UAV a player is previewing in the station screen. */
    public static void setPreview(UUID playerId, UUID uavId, long worldTick) {
        if (playerId != null && uavId != null) {
            PREVIEW.put(playerId, new PreviewReq(uavId, worldTick));
        }
    }

    public static void clearPreview(UUID playerId) {
        if (playerId != null) {
            PREVIEW.remove(playerId);
        }
    }

    /**
     * @return true if the given chunk is currently being streamed to the player around a UAV they
     * control or preview. Backs camera-aware entity visibility: an entity in such a chunk is shown
     * to the player even though their body is out of range.
     */
    public static boolean isChunkStreamedTo(UUID playerId, int chunkX, int chunkZ) {
        if (playerId == null) {
            return false;
        }
        Set<ChunkPos> set = STREAMED_SNAPSHOT.get(playerId);
        return set != null && set.contains(new ChunkPos(chunkX, chunkZ));
    }

    /**
     * @return true if <b>this specific player</b> is previewing the given UAV.
     */
    public static boolean isPreviewedBy(UUID uavId, UUID playerId) {
        if (uavId == null || playerId == null) {
            return false;
        }
        PreviewReq req = PREVIEW.get(playerId);
        return req != null && uavId.equals(req.uavId());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        tickChunkSubscriptions((WorldServer) event.world);
    }

    private void tickChunkSubscriptions(WorldServer ws) {
        PlayerChunkMap pcm = ws.getPlayerChunkMap();
        int viewRadius = pcm.playerViewRadius;
        long nowTick = ws.getTotalWorldTime();
        MinecraftServer server = ws.getMinecraftServer();

        // playerUUID -> the player (in this world) and the union of chunks they want streamed.
        Map<UUID, EntityPlayerMP> players = new HashMap<>();
        Map<UUID, Set<ChunkPos>> desired = new HashMap<>();
        // playerUUID -> the subset of streamed chunks in which entities may be revealed to the player.
        // UAV control/preview always reveal entities; TV missiles only when the dev debug flag is set.
        // Terrain streaming uses `desired`; entity visibility (STREAMED_SNAPSHOT) uses this.
        Map<UUID, Set<ChunkPos>> entityVisible = new HashMap<>();
        // playerUUID -> (body chunk, UAV chunk) for players ACTIVELY controlling a UAV this tick.
        Map<UUID, Ctl> controllers = new HashMap<>();

        // (1) Controlled UAVs — chunks around each actively-flown UAV for its operator.
        for (Entity entity : new ArrayList<>(ws.loadedEntityList)) {
            if (!McheliReflect.isAircraft(entity) || entity.isDead) continue;
            IUavStation station = McheliReflect.getUavStation(entity);
            if (station == null) continue;
            Entity rider = McheliReflect.getStationRider(station);
            // A dead rider is mid-disengagement (the station unmounts it on its own tick, and
            // onLivingDeath has already released its chunks). Never re-stream to a corpse.
            if (!(rider instanceof EntityPlayerMP player) || player.isDead) continue;
            // Actively controlled: stream the operator's full view radius (already clamped to the
            // server's view distance) so the UAV loads the same area a real player would.
            addDesired(desired, entityVisible, true, players, player, entity.posX, entity.posZ, viewRadius, viewRadius);
            controllers.put(player.getUniqueID(), new Ctl(
                    new ChunkPos((int) Math.floor(player.posX / 16.0), (int) Math.floor(player.posZ / 16.0)),
                    new ChunkPos((int) Math.floor(entity.posX / 16.0), (int) Math.floor(entity.posZ / 16.0))));
        }

        // (1b) TV/TA missiles in use by a player — stream the operator's full view radius around the
        // missile, exactly like a controlled UAV, so the missile camera renders real terrain instead of
        // void even far from the launcher. A TV missile self-destructs the moment its shooter disconnects
        // or dies, so any live missile with a player shooter is, by definition, in active use. (No
        // body-suppression: the operator stays seated in their aircraft and its view square is left
        // intact — the missile stream is purely additive.)
        //
        // Entities along the trajectory are NOT revealed for players — that is long-range entity ESP, a
        // developer-only debug aid gated by MCH_MOD.DEBUG_RENDER_TRAJECTORY_ENTITIES. Terrain still
        // streams regardless; only the entity-visibility contribution is flagged off.
        if (WingmanConfig.tvMissileChunkLoad) {
            for (Entity entity : new ArrayList<>(ws.loadedEntityList)) {
                if (!(entity instanceof MCH_EntityTvMissile tv) || tv.isDead) continue;
                if (!(tv.shootingEntity instanceof EntityPlayerMP player) || player.isDead) continue;
                if (player.world != ws) continue;
                addDesired(desired, entityVisible, MCH_MOD.DEBUG_RENDER_TRAJECTORY_ENTITIES,
                        players, player, tv.posX, tv.posZ, viewRadius, viewRadius);
            }
        }

        // (2) Previewed UAVs — chunks around the UAV selected in each player's open station screen.
        for (Iterator<Map.Entry<UUID, PreviewReq>> it = PREVIEW.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, PreviewReq> e = it.next();
            PreviewReq req = e.getValue();
            if (nowTick - req.heartbeatTick() > PREVIEW_TIMEOUT_TICKS) {
                it.remove();
                continue;
            }
            EntityPlayerMP player = server == null ? null : server.getPlayerList().getPlayerByUUID(e.getKey());
            if (player == null || player.world != ws) continue;
            double[] pos = locateUavXZ(ws, req.uavId());
            if (pos == null) continue;
            // Previewed only (nobody controlling): a small patch is enough for the camera-feed viewport.
            addDesired(desired, entityVisible, true, players, player, pos[0], pos[1], PREVIEW_STREAM_RADIUS, viewRadius);
        }

        // (3) Reconcile per player over the union of their desired chunks.
        Set<UUID> all = new HashSet<>(this.subscribed.keySet());
        all.addAll(desired.keySet());
        for (UUID pid : all) {
            EntityPlayerMP player = players.get(pid);
            if (player == null) {
                // Not controlling/previewing this tick, but they may still be in the world — e.g. they
                // just dismounted the station (shift). Resolve them so we actually RELEASE the chunks we
                // streamed; otherwise no SPacketUnloadChunk is sent and the client keeps hundreds of far
                // chunks loaded, which lags hard. Only when fully offline do we drop bookkeeping and let
                // onPlayerLogout release the entries.
                player = server == null ? null : server.getPlayerList().getPlayerByUUID(pid);
                if (player == null) {
                    this.subscribed.remove(pid);
                    continue;
                }
            }

            boolean inThisWorld = player.world == ws;
            Set<ChunkPos> want = inThisWorld ? desired.getOrDefault(pid, Collections.emptySet())
                                             : Collections.emptySet();
            Set<ChunkPos> have = this.subscribed.getOrDefault(pid, Collections.emptySet());
            int plCX = (int) Math.floor(player.posX / 16.0);
            int plCZ = (int) Math.floor(player.posZ / 16.0);

            for (ChunkPos old : have) {
                if (want.contains(old)) {
                    continue;
                }
                // Keep chunks the player's own (vanilla) view square owns — unless they have left this
                // world entirely, in which case release everything we added to it.
                if (inThisWorld && isInViewRange(old, plCX, plCZ, viewRadius)) {
                    continue;
                }
                removePlayerFromEntry(pcm, player, old);
            }
            for (ChunkPos pos : want) {
                if (!have.contains(pos)) {
                    addPlayerToEntry(pcm, player, pos);
                }
            }

            if (want.isEmpty()) {
                this.subscribed.remove(pid);
            } else {
                this.subscribed.put(pid, new HashSet<>(want));
            }
        }

        // (4) Publish the per-player streamed-chunk set for camera-aware entity visibility, then force
        // a deterministic visibility re-evaluation for every player we touched this tick. Vanilla only
        // re-checks entity visibility on player-BODY movement, which never happens while piloting a UAV
        // remotely — so without this, entities around a moving/just-detached UAV never spawn (or never
        // despawn when control ends). updateVisibility re-runs the (now camera-aware) isVisibleTo for
        // all entities, so they appear inside the streamed region and clear out once it is released.
        //
        // The snapshot is the streamed chunks INTERSECTED with the player's entity-visible set: UAV
        // control/preview chunks (always entity-visible) stay, while TV-missile-only chunks are excluded
        // unless the dev debug flag opted them in. Terrain (subscribed) is unaffected — only what entities
        // the player may see is narrowed.
        Map<UUID, Set<ChunkPos>> snapshot = new HashMap<>();
        for (Map.Entry<UUID, Set<ChunkPos>> e : this.subscribed.entrySet()) {
            Set<ChunkPos> ev = entityVisible.get(e.getKey());
            if (ev == null || ev.isEmpty()) {
                continue; // terrain still streamed for this player, but no entities revealed
            }
            Set<ChunkPos> vis = new HashSet<>(e.getValue());
            vis.retainAll(ev);
            if (!vis.isEmpty()) {
                snapshot.put(e.getKey(), vis);
            }
        }
        STREAMED_SNAPSHOT = snapshot;

        for (UUID pid : all) {
            EntityPlayerMP p = server == null ? null : server.getPlayerList().getPlayerByUUID(pid);
            if (p != null && p.world == ws) {
                ws.getEntityTracker().updateVisibility(p);
            }
        }

        // (5) Cost-cut: a player actively controlling a UAV is looking through the UAV, so the chunk
        // square around their (station-riding) body is wasted. Keep only BODY_KEEP_RADIUS chunks around
        // the body and drop the rest — except any chunk the UAV camera can still see (so a near UAV
        // doesn't blank the operator's surroundings). Everything is restored when control ends.
        Set<UUID> suppressTouched = new HashSet<>(this.suppressed.keySet());
        suppressTouched.addAll(controllers.keySet());
        for (UUID pid : suppressTouched) {
            Ctl ctl = controllers.get(pid);
            if (ctl != null && ctl.equals(this.lastCtl.get(pid))) {
                continue; // controller hasn't moved (body & UAV chunk unchanged) — already applied
            }

            EntityPlayerMP player = server == null ? null : server.getPlayerList().getPlayerByUUID(pid);
            boolean here = player != null && player.world == ws;

            Set<ChunkPos> nowSuppress = new HashSet<>();
            if (ctl != null && here) {
                for (int dx = -viewRadius; dx <= viewRadius; dx++) {
                    for (int dz = -viewRadius; dz <= viewRadius; dz++) {
                        if (Math.abs(dx) <= BODY_KEEP_RADIUS && Math.abs(dz) <= BODY_KEEP_RADIUS) {
                            continue; // keep the body core (the chunk(s) the operator/station sit in)
                        }
                        ChunkPos c = new ChunkPos(ctl.body().x + dx, ctl.body().z + dz);
                        if (isInViewRange(c, ctl.uav().x, ctl.uav().z, viewRadius)) {
                            continue; // the UAV camera can see this chunk — must stay loaded
                        }
                        nowSuppress.add(c);
                    }
                }
            }

            Set<ChunkPos> wasSuppress = this.suppressed.getOrDefault(pid, Collections.emptySet());
            if (here) {
                for (ChunkPos c : wasSuppress) {
                    if (!nowSuppress.contains(c)) {
                        addPlayerToEntry(pcm, player, c); // restore: control ended or chunk now needed
                    }
                }
                for (ChunkPos c : nowSuppress) {
                    if (!wasSuppress.contains(c)) {
                        removePlayerFromEntry(pcm, player, c);
                    }
                }
            }
            // If the player left this world, vanilla's dimension/logout sweep handles their entries.

            if (nowSuppress.isEmpty()) {
                this.suppressed.remove(pid);
            } else {
                this.suppressed.put(pid, nowSuppress);
            }
            if (ctl != null) {
                this.lastCtl.put(pid, ctl);
            } else {
                this.lastCtl.remove(pid);
            }
        }
    }

    private void addDesired(Map<UUID, Set<ChunkPos>> desired, Map<UUID, Set<ChunkPos>> entityVisible,
                            boolean revealEntities, Map<UUID, EntityPlayerMP> players,
                            EntityPlayerMP player, double x, double z, int streamRadius, int viewRadius) {
        players.put(player.getUniqueID(), player);
        int acCX = (int) Math.floor(x / 16.0);
        int acCZ = (int) Math.floor(z / 16.0);
        int plCX = (int) Math.floor(player.posX / 16.0);
        int plCZ = (int) Math.floor(player.posZ / 16.0);
        Set<ChunkPos> set = desired.computeIfAbsent(player.getUniqueID(), k -> new HashSet<>());
        Set<ChunkPos> evSet = revealEntities
                ? entityVisible.computeIfAbsent(player.getUniqueID(), k -> new HashSet<>()) : null;
        for (int dx = -streamRadius; dx <= streamRadius; dx++) {
            for (int dz = -streamRadius; dz <= streamRadius; dz++) {
                ChunkPos pos = new ChunkPos(acCX + dx, acCZ + dz);
                if (!isInViewRange(pos, plCX, plCZ, viewRadius)) {
                    set.add(pos);
                    if (evSet != null) {
                        evSet.add(pos);
                    }
                }
            }
        }
    }

    /** Chunk-center XZ of a UAV by UUID (loaded entity, else its last-known position), or null. */
    private static double[] locateUavXZ(WorldServer ws, UUID uavId) {
        ChunkPos cp = UAVTracker.getUAVPos(ws, uavId);
        return cp == null ? null : new double[]{cp.x * 16 + 8, cp.z * 16 + 8};
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            disengage(player);
        }
    }

    /**
     * A player can die while piloting/previewing a UAV. Death keeps the {@link EntityPlayerMP} alive
     * for the death screen but respawn swaps in a brand-new player instance — so, exactly like logout,
     * the streamed chunk entries must be released NOW, against the dying instance they still reference.
     * Otherwise reconcile (which by then sees the new respawned object) calls {@code removePlayer} on
     * the wrong instance, a no-op, and the far chunks leak forever — ghost-loaded by a discarded player
     * (vanilla's death/respawn sweep only covers the body view square, never our far chunks). The
     * station severs the control link + resets the camera on its own tick (dead rider -> unmountEntity);
     * this handles the streaming side, so the full disengagement runs on death just as on a dismount.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP player) {
            disengage(player);
        }
    }

    /**
     * Streaming-side disengagement for a player whose object is about to be discarded (logout, or
     * death -> respawn). Releases every out-of-view chunk we streamed to them — these are the entries
     * vanilla never sweeps — while this is still the instance the entries reference, then drops all
     * bookkeeping. The suppressed body-square chunks need no restore: vanilla re-adds the fresh view
     * square to the leaving/respawned player, and its own sweep ignores chunks the player is already
     * out of.
     */
    private void disengage(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        Set<ChunkPos> streamed = this.subscribed.remove(id);
        if (streamed != null && player.world instanceof WorldServer ws) {
            PlayerChunkMap pcm = ws.getPlayerChunkMap();
            for (ChunkPos c : streamed) {
                removePlayerFromEntry(pcm, player, c);
            }
        }
        this.suppressed.remove(id);
        this.lastCtl.remove(id);
        clearPreview(id);
    }

    private void addPlayerToEntry(PlayerChunkMap pcm, EntityPlayerMP player, ChunkPos pos) {
        try {
            PlayerChunkMapEntry entry = pcm.getOrCreateEntry(pos.x, pos.z);
            if (entry != null && !entry.containsPlayer(player)) {
                entry.addPlayer(player);
                McHeliWingman.logger.debug("[UavChunkStreamer] + {} → chunk ({},{})",
                        player.getName(), pos.x, pos.z);
            }
        } catch (Exception e) {
            McHeliWingman.logger.warn("[UavChunkStreamer] addPlayer ({},{}) failed: {}",
                    pos.x, pos.z, e.getMessage());
        }
    }

    private void removePlayerFromEntry(PlayerChunkMap pcm, EntityPlayerMP player, ChunkPos pos) {
        try {
            PlayerChunkMapEntry entry = pcm.getEntry(pos.x, pos.z);
            if (entry != null && entry.containsPlayer(player)) {
                entry.removePlayer(player);
                McHeliWingman.logger.debug("[UavChunkStreamer] - {} ← chunk ({},{})",
                        player.getName(), pos.x, pos.z);
            }
        } catch (Exception e) {
            McHeliWingman.logger.warn("[UavChunkStreamer] removePlayer ({},{}) failed: {}",
                    pos.x, pos.z, e.getMessage());
        }
    }

    private static boolean isInViewRange(ChunkPos pos, int plCX, int plCZ, int viewRadius) {
        return Math.abs(pos.x - plCX) <= viewRadius && Math.abs(pos.z - plCZ) <= viewRadius;
    }

    private record PreviewReq(UUID uavId, long heartbeatTick) {
    }

    /** A controlling player's body chunk and the chunk their controlled UAV is in, this tick. */
    private record Ctl(ChunkPos body, ChunkPos uav) {
    }
}
