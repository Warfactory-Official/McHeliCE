package com.norwood.mcheli.wingman.handler;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.util.McheliReflect;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * Keeps chunks loaded around McHeli UAV entities using ForgeChunkManager
 * so they survive ChunkProviderServer.tick() unload sweeps.
 *
 * Approach:
 *   - Phase.START: fires BEFORE entity movement this tick, so chunks are
 *     resident before McHeli moves the UAV into them.
 *   - One ForgeChunkManager ticket per live aircraft entity (UUID-keyed).
 *   - Ticket covers a (2*CHUNK_RADIUS+1)^2 = 5×5 = 25 chunk area around
 *     the aircraft. 25 matches Forge's default maximumChunksPerTicket, so
 *     no chunks are silently dropped.
 *   - Position derived from posX/posZ (not stale chunkCoordX/Z) via
 *     Math.floor(pos / 16).
 *   - Ticket is updated only when the aircraft crosses a chunk boundary
 *     (lastPos cache). On departure/death the ticket is released.
 *   - ticketsLoaded() releases any stale tickets left over from a crash
 *     or previous session; we rebuild them from scratch each run.
 *
 * NOTE: Targets Forge 1.12.2 / McHeli CE 1.1.4.
 */
public class ChunkLoadHandler implements ForgeChunkManager.LoadingCallback {

    // 5×5 = 25 chunks — matches Forge's default maximumChunksPerTicket exactly.
    private static final int CHUNK_RADIUS = 2;

    /** ForgeChunkManager ticket per aircraft entity UUID. */
    private final Map<UUID, Ticket> tickets = new HashMap<>();

    /** Dimension ID of the world each ticket was issued for. */
    private final Map<UUID, Integer> ticketDim = new HashMap<>();

    /** Last chunk center per entity, to skip updates when nothing moved. */
    private final Map<UUID, ChunkPos> lastPos = new HashMap<>();

    // -------------------------------------------------------------------------
    // Tick handler
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return; // BEFORE entity movement
        if (event.world.isRemote) return;

        World world = event.world;

        // Track which UUIDs we see this tick so we can clean up departed entities.
        Set<UUID> alive = new HashSet<>();

        for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
            if (!McheliReflect.isAircraft(entity)) continue;
            if (entity.isDead) continue;

            UUID id = entity.getUniqueID();
            alive.add(id);

            // Use posX/Z directly — chunkCoordX/Z can lag when McHeli writes
            // entity position directly rather than through moveEntity().
            int cx = (int) Math.floor(entity.posX / 16.0);
            int cz = (int) Math.floor(entity.posZ / 16.0);
            ChunkPos center = new ChunkPos(cx, cz);

            // Skip if the chunk hasn't changed since last tick.
            if (center.equals(lastPos.get(id))) continue;
            lastPos.put(id, center);

            // Get or request a ticket for this entity.
            Ticket ticket = tickets.get(id);
            if (ticket == null) {
                ticket = ForgeChunkManager.requestTicket(
                        McHeliWingman.instance, world, ForgeChunkManager.Type.NORMAL);
                if (ticket == null) {
                    McHeliWingman.logger.warn(
                            "[ChunkLoad] ForgeChunkManager denied ticket for aircraft {} ({}). "
                            + "Chunk forcing disabled for this entity.",
                            entity.getClass().getSimpleName(), id);
                    continue;
                }
                tickets.put(id, ticket);
                ticketDim.put(id, world.provider.getDimension());
                McHeliWingman.logger.info("[ChunkLoad] Acquired ticket for aircraft {}",
                        entity.getClass().getSimpleName());
            }

            // Build the desired 5×5 chunk set.
            Set<ChunkPos> desired = new HashSet<>();
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    desired.add(new ChunkPos(cx + dx, cz + dz));
                }
            }

            // Unforce chunks that are no longer in range.
            for (ChunkPos old : new ArrayList<>(ticket.getChunkList())) {
                if (!desired.contains(old)) {
                    ForgeChunkManager.unforceChunk(ticket, old);
                }
            }

            // Force chunks that aren't forced yet.
            ImmutableSetSnapshot forced = new ImmutableSetSnapshot(ticket.getChunkList());
            for (ChunkPos pos : desired) {
                if (!forced.contains(pos)) {
                    ForgeChunkManager.forceChunk(ticket, pos);
                }
            }

            McHeliWingman.logger.debug(
                    "[ChunkLoad] Forced {} chunks around {} @ chunk ({},{})",
                    desired.size(), entity.getClass().getSimpleName(), cx, cz);
        }

        // ─── 自律飛行中の機体: loadedEntityList に存在しなくても最終位置のチャンクを維持 ──────
        // McHeli の物理更新でエンティティが未ロードチャンクへ移動すると、loadedEntityList から
        // 消えても entitiesByUuid には残る。チケットを解放すると強制ロードが解除されてチャンクが
        // アンロードされ、その後 WTH/AFH が getEntityFromUuid() で取得できなくなる。
        // → WingmanEntry に active なオーダーがある間はチケットを保持し、
        //   lastPos（最終既知チャンク）周囲を強制ロードし続ける。
        for (Map.Entry<UUID, WingmanEntry> we : WingmanRegistry.snapshot().entrySet()) {
            if (!we.getValue().isAutonomous()) continue;
            UUID uid = we.getKey();
            if (alive.contains(uid)) continue; // loadedEntityList にあるので通常処理で OK
            ChunkPos lastKnown = lastPos.get(uid);
            Ticket ticket = tickets.get(uid);
            if (lastKnown == null || ticket == null) continue;
            // 最終既知位置の 5×5 を強制ロード維持
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    ChunkPos cp = new ChunkPos(lastKnown.x + dx, lastKnown.z + dz);
                    ImmutableSetSnapshot forced = new ImmutableSetSnapshot(ticket.getChunkList());
                    if (!forced.contains(cp)) ForgeChunkManager.forceChunk(ticket, cp);
                }
            }
            McHeliWingman.logger.debug("[ChunkLoad] {} not in loadedEntityList but has active order — keeping chunk forced at ({},{})",
                    uid, lastKnown.x, lastKnown.z);
        }

        // Release tickets for entities in THIS dimension that are no longer present.
        // We must guard by dimension ID: other worlds (nether, end) also fire
        // WorldTickEvent, and their 'alive' sets would otherwise incorrectly
        // trigger cleanup of tickets issued for a different dimension.
        int currentDim = world.provider.getDimension();
        Iterator<Map.Entry<UUID, Ticket>> it = tickets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Ticket> entry = it.next();
            UUID uid = entry.getKey();
            if (ticketDim.getOrDefault(uid, Integer.MIN_VALUE) != currentDim) continue;
            if (!alive.contains(uid)) {
                // アクティブなオーダーがある機体はチケットを解放しない（上記ブロックで維持済み）
                WingmanEntry we = WingmanRegistry.get(uid);
                if (we != null && we.isAutonomous()) continue;
                ForgeChunkManager.releaseTicket(entry.getValue());
                lastPos.remove(uid);
                ticketDim.remove(uid);
                it.remove();
                McHeliWingman.logger.info("[ChunkLoad] Released ticket for departed aircraft {}", uid);
            }
        }
    }

    // -------------------------------------------------------------------------
    // ForgeChunkManager.LoadingCallback
    // -------------------------------------------------------------------------

    /**
     * Called on world load for any tickets saved from a previous session.
     * We release them all — we'll acquire fresh tickets as aircraft are found.
     *
     * IMPORTANT: also clears our instance maps here.
     * The ticket objects stored in the `tickets` map are Java references from the
     * PREVIOUS world session.  Forge rebuilds new ticket objects on reload; the old
     * references are stale and will NPE if we try to release them again in
     * onWorldTick.  Clearing the maps here prevents that.
     */
    @Override
    public void ticketsLoaded(List<Ticket> savedTickets, World world) {
        for (Ticket ticket : savedTickets) {
            ForgeChunkManager.releaseTicket(ticket);
        }
        if (!savedTickets.isEmpty()) {
            McHeliWingman.logger.info("[ChunkLoad] Released {} stale ticket(s) from previous session.",
                    savedTickets.size());
        }

        // Drop all stale references so onWorldTick doesn't try to re-release them.
        int staleCount = tickets.size();
        tickets.clear();
        ticketDim.clear();
        lastPos.clear();
        if (staleCount > 0) {
            McHeliWingman.logger.info("[ChunkLoad] Cleared {} stale ticket reference(s) from previous session.",
                    staleCount);
        }
    }

    // -------------------------------------------------------------------------
    // Helper: snapshot of ImmutableSet to avoid ConcurrentModificationException
    // -------------------------------------------------------------------------

    /** Thin wrapper so we can call contains() on the ticket's ImmutableSet snapshot. */
    private static final class ImmutableSetSnapshot {
        private final Set<ChunkPos> set;
        ImmutableSetSnapshot(com.google.common.collect.ImmutableSet<ChunkPos> src) {
            this.set = new HashSet<>(src);
        }
        boolean contains(ChunkPos pos) { return set.contains(pos); }
    }
}
