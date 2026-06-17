package com.norwood.mcheli.wingman.handler;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wingman.McHeliWingman;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Force-loads chunks along the flight path of MCHeli projectiles so bullets, rockets and missiles
 * keep simulating (and can hit targets) in terrain that no player is keeping loaded.
 *
 * <p>Projectiles arrive in bursts that travel almost the same line, so this uses a shared,
 * <b>lease-based</b> design instead of one ForgeChunkManager ticket per projectile:
 *
 * <ul>
 *   <li>Each server tick a projectile (whose weapon opts in, or when globally enabled) calls
 *       {@link #requestPath} with the line of chunks ahead of it. The line is {@code BASE_LINE_CHUNKS}
 *       long plus up to {@code MAX_VELOCITY_EXTRA_CHUNKS} more scaled by the projectile's speed, so
 *       fast rounds pre-load farther ahead.</li>
 *   <li>Every requested chunk gets (or refreshes) a <b>lease</b> that expires
 *       {@code MCH_Config.ProjectileChunkLeaseTicks} ticks in the future. A chunk is force-loaded
 *       the first time it is requested and stays loaded until its lease lapses — so the gap between
 *       consecutive shots in a burst keeps the same chunks alive instead of unload/reload thrashing.</li>
 *   <li>Leases are chunk-keyed and shared, so many projectiles down the same corridor cost one
 *       forced chunk, not one per projectile.</li>
 *   <li>A {@link TickEvent.Phase#END} sweep unforces chunks whose lease has lapsed and releases
 *       tickets once they hold no chunks.</li>
 * </ul>
 *
 * <p>Tickets are pooled per dimension and packed up to each ticket's {@code getMaxChunkListDepth()}
 * (Forge's per-ticket cap), requesting more on demand. Tickets are requested against
 * {@link McHeliWingman#instance} — the same mod handle whose {@link ForgeChunkManager.LoadingCallback}
 * ({@link ChunkLoadHandler}) already releases any stale saved tickets on world load, so transient
 * projectile tickets never leak across a restart.
 *
 * <p>Server-side only. Targets Forge 1.12.2 / McHeli CE.
 */
public class ProjectileChunkLoader {

    /** Base length (chunks) of the force-loaded line ahead of a projectile, including its own chunk. */
    private static final int BASE_LINE_CHUNKS = 4;

    /** Velocity-scaled lookahead: blocks added per (block/tick) of speed. ~8 ticks of travel. */
    private static final int VELOCITY_LEAD_TICKS = 8;

    /** Cap (chunks) on the velocity-scaled extension beyond the base line. */
    private static final int MAX_VELOCITY_EXTRA_CHUNKS = 2;

    /** Step (blocks) used to walk the path and collect the chunks it crosses. */
    private static final double SAMPLE_STEP = 8.0;

    private static ProjectileChunkLoader instance;

    /** Per-dimension lease bookkeeping. */
    private final Map<Integer, DimState> dims = new HashMap<>();

    public ProjectileChunkLoader() {
        instance = this;
    }

    /** @return the registered singleton, or {@code null} before pre-init has run. */
    public static ProjectileChunkLoader instance() {
        return instance;
    }

    /**
     * (Re)lease the chunks along this projectile's path for this tick. Force-loads any chunk not yet
     * loaded and pushes every chunk's lease expiry out to {@code now + ProjectileChunkLeaseTicks}.
     * No-op on the client.
     */
    public void requestPath(MCH_EntityBaseBullet bullet) {
        World world = bullet.world;
        if (world == null || world.isRemote) return;

        DimState st = this.dims.computeIfAbsent(world.provider.getDimension(), k -> new DimState());
        long now = world.getTotalWorldTime();
        long until = now + Math.max(0, MCH_Config.ProjectileChunkLeaseTicks.prmInt);

        for (ChunkPos cp : computePathChunks(bullet)) {
            Long prev = st.expiry.get(cp);
            if (prev == null || until > prev) {
                st.expiry.put(cp, until);
            }
            if (!st.owner.containsKey(cp)) {
                Ticket ticket = acquireTicketWithRoom(world, st);
                if (ticket == null) {
                    // Out of tickets/chunk budget — drop the lease so we retry cleanly next tick.
                    st.expiry.remove(cp);
                    continue;
                }
                ForgeChunkManager.forceChunk(ticket, cp);
                st.owner.put(cp, ticket);
            }
        }
    }

    /** Unforce chunks whose lease has lapsed; release tickets that end up empty. */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        // Phase.END: projectiles have already (re)requested their paths this tick, so a chunk still
        // in use this tick has a future expiry and is never swept.
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;

        DimState st = this.dims.get(event.world.provider.getDimension());
        if (st == null || st.expiry.isEmpty()) return;

        long now = event.world.getTotalWorldTime();
        Iterator<Map.Entry<ChunkPos, Long>> it = st.expiry.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, Long> e = it.next();
            // Keep while the lease is still current (expiry >= now). Using >= (not >) means a chunk
            // requested THIS tick — expiry == now when leaseTicks is 0 — survives the tick it was
            // requested in instead of being unforced the same tick, so a projectile present this tick
            // always has its chunk loaded when it moves next tick.
            if (e.getValue() >= now) continue;

            ChunkPos cp = e.getKey();
            it.remove();
            Ticket ticket = st.owner.remove(cp);
            // Only touch a ticket that still belongs to this live world (mirror ChunkLoadHandler's
            // caution: a ticket whose world was torn down is stale and would NPE in Forge).
            if (ticket != null && ticket.world == event.world) {
                ForgeChunkManager.unforceChunk(ticket, cp);
                if (ticket.getChunkList().isEmpty()) {
                    ForgeChunkManager.releaseTicket(ticket);
                    st.tickets.remove(ticket);
                }
            }
        }
    }

    /**
     * Drop all bookkeeping for an unloading world. Forge unforces the chunks and discards its own
     * ticket records for that world, leaving our cached {@link Ticket}s stale — so we just forget them
     * (no release; mirrors {@link ChunkLoadHandler#onWorldUnload}).
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        this.dims.remove(event.getWorld().provider.getDimension());
    }

    /** Find a pooled ticket for this world with room, else request a fresh one. */
    private Ticket acquireTicketWithRoom(World world, DimState st) {
        for (Ticket t : st.tickets) {
            if (t.world == world && t.getChunkList().size() < t.getMaxChunkListDepth()) {
                return t;
            }
        }
        Ticket ticket = ForgeChunkManager.requestTicket(
                McHeliWingman.instance, world, ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            st.tickets.add(ticket);
        } else {
            McHeliWingman.logger.warn(
                    "[ProjectileChunkLoader] ForgeChunkManager denied a ticket — projectile chunk "
                    + "forcing skipped this tick (ticket budget exhausted).");
        }
        return ticket;
    }

    /**
     * Chunks to lease for a projectile: its current chunk plus the line of chunks ahead of it along
     * its horizontal motion, {@link #BASE_LINE_CHUNKS} long and extended by up to
     * {@link #MAX_VELOCITY_EXTRA_CHUNKS} more, scaled by speed.
     */
    private static Set<ChunkPos> computePathChunks(MCH_EntityBaseBullet b) {
        Set<ChunkPos> chunks = new HashSet<>();
        chunks.add(new ChunkPos((int) Math.floor(b.posX / 16.0), (int) Math.floor(b.posZ / 16.0)));

        double mx = b.motionX;
        double mz = b.motionZ;
        double speed = Math.sqrt(mx * mx + mz * mz);
        if (speed < 1.0e-4) {
            return chunks; // stationary/vertical — just hold the current chunk
        }

        double lead = BASE_LINE_CHUNKS * 16.0
                + Math.min(MAX_VELOCITY_EXTRA_CHUNKS * 16.0, speed * VELOCITY_LEAD_TICKS);
        double ux = mx / speed;
        double uz = mz / speed;
        for (double d = SAMPLE_STEP; d <= lead; d += SAMPLE_STEP) {
            int cx = (int) Math.floor((b.posX + ux * d) / 16.0);
            int cz = (int) Math.floor((b.posZ + uz * d) / 16.0);
            chunks.add(new ChunkPos(cx, cz));
        }
        return chunks;
    }

    /** Lease state for one dimension. */
    private static final class DimState {
        /** Tickets owned in this dimension, packed up to each ticket's max depth. */
        final List<Ticket> tickets = new ArrayList<>();
        /** Chunk -> the ticket currently forcing it. */
        final Map<ChunkPos, Ticket> owner = new HashMap<>();
        /** Chunk -> world tick at which its lease lapses. */
        final Map<ChunkPos, Long> expiry = new HashMap<>();
    }
}
