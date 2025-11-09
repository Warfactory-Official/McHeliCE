package com.norwood.mcheli;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Client relies only on “full snapshot heartbeats” for incremental/overwrite updates;
 * deletions are handled purely through local expiration cleanup.
 * <p>
 * - Out-of-order protection: only accepts snapshots with snapshotSeq >= lastAppliedSeq.
 * - Expiration policy: judged both by elapsed milliseconds and by missing sequence threshold.
 * - Timer: uses ClientTick to avoid cross-thread issues from Timer.
 *
 * @Author: TV90
 */
public class MCH_EntityInfoClientTracker {

    private static final Map<Integer, Tracked> tracked = new ConcurrentHashMap<>();
    /**
     * Adjustable: timeout threshold in milliseconds for missing heartbeats (e.g., 5s)
     */
    public static long EXPIRATION_MS = 1_000L;
    /**
     * Adjustable: missing heartbeat threshold in sequence numbers (based on server ticks, at 20 TPS, 100 ≈ 5s)
     */
    public static long MISSING_SEQ_THRESHOLD = 20L;
    /**
     * Adjustable: tick interval for cleanup scans (e.g., scan every 10 client ticks)
     */
    public static int CLEANUP_TICK_INTERVAL = 10;
    private static volatile long lastAppliedSeq = -1L;    // Latest snapshot sequence that has been applied
    private static volatile long latestSeqObserved = -1L; // Maximum sequence number recently received (used for absence
                                                          // detection)
    private static int clientTickCounter = 0;

    static {
        FMLCommonHandler.instance().bus().register(new ClientTicker());
    }

    /**
     * Called by network packet callback: applies a batch of entities and records the snapshot sequence
     */
    public static void updateEntities(List<EntityInfo> infos, long snapshotSeq) {
        // Protect against out-of-order or delayed packets
        if (snapshotSeq < lastAppliedSeq) {
            return;
        }

        long now = System.currentTimeMillis();
        latestSeqObserved = Math.max(latestSeqObserved, snapshotSeq);

        for (EntityInfo info : infos) {
            Tracked t = tracked.get(info.entityId);
            if (t == null) {
                tracked.put(info.entityId, new Tracked(info, now, snapshotSeq));
            } else {
                t.info = info;
                t.lastSeenMillis = now;
                t.lastSeenSeq = snapshotSeq;
            }
        }

        lastAppliedSeq = snapshotSeq;
    }

    public static EntityInfo getEntityInfo(int entityId) {
        Tracked t = tracked.get(entityId);
        return t == null ? null : t.info;
    }

    public static Collection<EntityInfo> getAllTrackedEntities() {
        List<EntityInfo> out = new ArrayList<>(tracked.size());
        for (Tracked t : tracked.values()) {
            out.add(t.info);
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Periodic cleanup: remove entries that have been missing too long (by time or sequence)
     */
    private static void cleanupExpired() {
        if (tracked.isEmpty()) return;

        long now = System.currentTimeMillis();
        long seqNow = latestSeqObserved;

        Iterator<Map.Entry<Integer, Tracked>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Tracked> e = it.next();
            Tracked t = e.getValue();

            boolean timeExpired = (now - t.lastSeenMillis) > EXPIRATION_MS;
            boolean seqExpired = (seqNow - t.lastSeenSeq) > MISSING_SEQ_THRESHOLD;

            if (timeExpired || seqExpired) {
                it.remove();
            }
        }
    }

    private static final class Tracked {

        EntityInfo info;
        long lastSeenMillis;
        long lastSeenSeq;

        Tracked(EntityInfo info, long now, long seq) {
            this.info = info;
            this.lastSeenMillis = now;
            this.lastSeenSeq = seq;
        }
    }

    /**
     * Note: must be public to be accessed by ASMEventHandler
     */
    public static class ClientTicker {

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            clientTickCounter++;
            if (clientTickCounter % CLEANUP_TICK_INTERVAL == 0) {
                cleanupExpired();
            }
        }
    }
}
