package com.norwood.mcheli.wingman.handler;

import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.util.McheliReflect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * Streams terrain chunks around an actively-controlled UAV to its operating
 * player — specifically the chunks that fall OUTSIDE the player's normal view
 * radius — so the client renders the ground beneath a distant UAV instead of
 * void. Runs on Phase.END so entity positions are final, and only manages
 * out-of-view chunks (avoiding spurious SPacketUnloadChunk for in-view chunks
 * that would cause warps).
 *
 * <p>Entity <em>visibility</em> at long range is already guaranteed by CE's
 * coremod: {@code TrackerHook} force-watches every {@code W_Entity} (which
 * includes UAV aircraft) and returns their full tracking range. The legacy
 * reflective EntityTrackerEntry range-widening the standalone addon shipped is
 * therefore unnecessary and has been dropped.
 *
 * <p>{@code PlayerChunkMap#playerViewRadius} and {@code #getOrCreateEntry} are
 * widened to public via mcheli_at.cfg (Access Transformer) rather than reflection.
 */
public class UavChunkStreamer {

    /** 5×5 area around the aircraft's current chunk. */
    private static final int CHUNK_RADIUS = 2;

    /** Out-of-view chunk subscriptions we have added, keyed by aircraft UUID. */
    private final Map<UUID, Set<ChunkPos>> subscribed = new HashMap<>();

    /** The player subscribed on behalf of each aircraft UUID. */
    private final Map<UUID, EntityPlayerMP> subscribedPlayer = new HashMap<>();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        tickChunkSubscriptions((WorldServer) event.world);
    }

    private void tickChunkSubscriptions(WorldServer ws) {
        PlayerChunkMap pcm = ws.getPlayerChunkMap();
        int viewRadius = pcm.playerViewRadius;

        Set<UUID> processed = new HashSet<>();

        for (Entity entity : new ArrayList<>(ws.loadedEntityList)) {
            if (!McheliReflect.isAircraft(entity) || entity.isDead) continue;

            IUavStation station = McheliReflect.getUavStation(entity);
            if (station == null) continue;

            Entity rider = McheliReflect.getStationRider(station);
            if (!(rider instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) rider;

            UUID id = entity.getUniqueID();
            processed.add(id);

            int acCX = (int) Math.floor(entity.posX / 16.0);
            int acCZ = (int) Math.floor(entity.posZ / 16.0);
            int plCX = (int) Math.floor(player.posX / 16.0);
            int plCZ = (int) Math.floor(player.posZ / 16.0);

            // Chunks outside the player's view radius around the aircraft.
            Set<ChunkPos> desiredOut = new HashSet<>();
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    ChunkPos pos = new ChunkPos(acCX + dx, acCZ + dz);
                    if (!isInViewRange(pos, plCX, plCZ, viewRadius)) {
                        desiredOut.add(pos);
                    }
                }
            }

            Set<ChunkPos> currentOut = subscribed.getOrDefault(id, Collections.emptySet());

            // Remove subscriptions no longer needed (only outside view range).
            for (ChunkPos old : currentOut) {
                if (!desiredOut.contains(old) && !isInViewRange(old, plCX, plCZ, viewRadius)) {
                    removePlayerFromEntry(pcm, player, old);
                }
            }

            // Add new out-of-view subscriptions.
            for (ChunkPos pos : desiredOut) {
                if (!currentOut.contains(pos)) {
                    addPlayerToEntry(pcm, player, pos);
                }
            }

            subscribed.put(id, new HashSet<>(desiredOut));
            subscribedPlayer.put(id, player);
        }

        // Cleanup: aircraft no longer under UAV control in this world.
        Iterator<Map.Entry<UUID, Set<ChunkPos>>> it = subscribed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Set<ChunkPos>> entry = it.next();
            UUID uid = entry.getKey();
            if (processed.contains(uid)) continue;

            EntityPlayerMP player = subscribedPlayer.get(uid);
            if (player != null && player.world != ws) continue; // different world

            subscribedPlayer.remove(uid);
            if (player != null && !player.isDead) {
                int plCX = (int) Math.floor(player.posX / 16.0);
                int plCZ = (int) Math.floor(player.posZ / 16.0);
                for (ChunkPos pos : entry.getValue()) {
                    if (!isInViewRange(pos, plCX, plCZ, pcm.playerViewRadius)) {
                        removePlayerFromEntry(pcm, player, pos);
                    }
                }
            }
            it.remove();
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        Iterator<Map.Entry<UUID, Set<ChunkPos>>> it = subscribed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Set<ChunkPos>> entry = it.next();
            if (subscribedPlayer.get(entry.getKey()) != player) continue;
            subscribedPlayer.remove(entry.getKey());
            it.remove();
        }
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
}
