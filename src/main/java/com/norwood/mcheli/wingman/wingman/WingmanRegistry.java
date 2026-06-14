package com.norwood.mcheli.wingman.wingman;

import net.minecraft.entity.Entity;

import java.util.*;

/**
 * Static registry mapping wingman aircraft UUID → WingmanEntry.
 * All public methods are synchronized for safe access from event handlers.
 */
public class WingmanRegistry {

    private static final Map<UUID, WingmanEntry> entries = new LinkedHashMap<>();

    public static synchronized void put(UUID id, WingmanEntry entry) {
        entries.put(id, entry);
    }

    public static synchronized void remove(UUID id) {
        entries.remove(id);
    }

    public static synchronized WingmanEntry get(UUID id) {
        return entries.get(id);
    }

    /** Returns a snapshot for safe iteration outside the lock. */
    public static synchronized Map<UUID, WingmanEntry> snapshot() {
        return new LinkedHashMap<>(entries);
    }

    /** Number of wingmen already assigned to this leader aircraft. */
    public static synchronized int countForLeader(Entity leader) {
        int n = 0;
        for (WingmanEntry e : entries.values()) {
            if (e.leader == leader) n++;
        }
        return n;
    }

    /** Remove all wingmen following this leader. */
    public static synchronized void removeForLeader(Entity leader) {
        entries.entrySet().removeIf(e -> e.getValue().leader == leader);
    }

    /** Return all entries whose leader matches (snapshot). */
    public static synchronized List<Map.Entry<UUID, WingmanEntry>> snapshotForLeader(Entity leader) {
        List<Map.Entry<UUID, WingmanEntry>> result = new ArrayList<>();
        for (Map.Entry<UUID, WingmanEntry> e : entries.entrySet()) {
            if (e.getValue().leader == leader) result.add(e);
        }
        return result;
    }
}
