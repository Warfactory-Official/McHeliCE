package com.norwood.mcheli.aircraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side tracker for RWR threats.
 * Ported from MCHeli Reforged.
 */
public final class MCH_RWRThreatClientTracker {
    private static final ConcurrentHashMap<Integer, MCH_RWRThreatTable> TABLES = new ConcurrentHashMap<>();

    private MCH_RWRThreatClientTracker() {
    }

    public static void updateTable(MCH_RWRThreatTable incoming) {
        if (incoming == null) {
            return;
        }
        MCH_RWRThreatTable prev = TABLES.get(incoming.receiverEntityId);
        if (prev != null && prev.snapshotSeq >= incoming.snapshotSeq) {
            return;
        }
        TABLES.put(incoming.receiverEntityId, incoming);
    }

    public static MCH_RWRThreatTable getTable(int receiverEntityId) {
        return TABLES.get(receiverEntityId);
    }

    public static List<MCH_RWRThreatEvent> getEvents(int receiverEntityId) {
        MCH_RWRThreatTable table = TABLES.get(receiverEntityId);
        if (table == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(table.events);
    }
}
