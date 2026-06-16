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
import net.minecraftforge.event.world.WorldEvent;
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


        for (Map.Entry<UUID, WingmanEntry> we : WingmanRegistry.snapshot().entrySet()) {
            if (!we.getValue().isAutonomous()) continue;
            UUID uid = we.getKey();
            if (alive.contains(uid)) continue;
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


        int currentDim = world.provider.getDimension();
        Iterator<Map.Entry<UUID, Ticket>> it = tickets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Ticket> entry = it.next();
            UUID uid = entry.getKey();
            if (ticketDim.getOrDefault(uid, Integer.MIN_VALUE) != currentDim) continue;
            if (!alive.contains(uid)) {
                WingmanEntry we = WingmanRegistry.get(uid);
                if (we != null && we.isAutonomous()) continue;
                Ticket ticket = entry.getValue();
                // Only release a ticket that still belongs to THIS live world. A ticket whose world
                // was unloaded is stale (same dimension id, different World object): Forge has already
                // discarded it, so releaseTicket would NPE on tickets.get(ticket.world). In that case
                // just forget our dangling reference.
                if (ticket.world == world) {
                    ForgeChunkManager.releaseTicket(ticket);
                    McHeliWingman.logger.info("[ChunkLoad] Released ticket for departed aircraft {}", uid);
                }
                lastPos.remove(uid);
                ticketDim.remove(uid);
                it.remove();
            }
        }
    }

    /**
     * When a server world unloads, drop every cached ticket reference for that dimension. Forge
     * discards its own ticket bookkeeping (and unforces the chunks) for an unloading world, leaving
     * our cached {@link Ticket} objects stale; a later {@code releaseTicket} on one NPEs inside
     * {@code ForgeChunkManager.releaseTicket} ({@code tickets.get(ticket.world)} is null). This must
     * not rely on {@link #ticketsLoaded}: Forge only calls that when it restores SAVED tickets, so it
     * never runs for worlds/sessions that had none — which is the crash this fixes. Fresh tickets are
     * reacquired when aircraft are seen again. No release is needed here: the unloading world tears
     * down its own forced chunks.
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        int dim = event.getWorld().provider.getDimension();
        int dropped = 0;
        Iterator<Map.Entry<UUID, Integer>> it = ticketDim.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            if (e.getValue() == dim) {
                UUID uid = e.getKey();
                tickets.remove(uid);
                lastPos.remove(uid);
                it.remove();
                dropped++;
            }
        }
        if (dropped > 0) {
            McHeliWingman.logger.info("[ChunkLoad] Dropped {} stale ticket reference(s) for unloading dim {}.",
                    dropped, dim);
        }
    }

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

        int staleCount = tickets.size();
        tickets.clear();
        ticketDim.clear();
        lastPos.clear();
        if (staleCount > 0) {
            McHeliWingman.logger.info("[ChunkLoad] Cleared {} stale ticket reference(s) from previous session.",
                    staleCount);
        }
    }

    /** Thin wrapper so we can call contains() on the ticket's ImmutableSet snapshot. */
    private static final class ImmutableSetSnapshot {
        private final Set<ChunkPos> set;
        ImmutableSetSnapshot(com.google.common.collect.ImmutableSet<ChunkPos> src) {
            this.set = new HashSet<>(src);
        }
        boolean contains(ChunkPos pos) { return set.contains(pos); }
    }
}
