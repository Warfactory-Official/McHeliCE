package com.norwood.mcheli.uav;

import java.util.List;
import java.util.UUID;

/**
 * Implemented by anything that can hold a set of paired UAVs (currently {@link MCH_EntityUavStation}).
 * Pairing state is exposed purely through this interface so the storage strategy (entity NBT today,
 * a capability or world-saved-data later) can change without touching callers.
 *
 * <p>A station may be paired to many UAVs (bounded by {@link #getMaxPairedUavs()}); the inverse
 * "one station per UAV" constraint is enforced on the UAV side via {@link IPairableUav}.
 */
public interface IUavPairingHolder {

    /** @return an unmodifiable, stable-order view of the paired UAV entity UUIDs. */
    List<UUID> getPairedUavs();

    boolean isPaired(UUID uav);

    /**
     * Adds the UAV to the paired set.
     *
     * @return {@code true} if the UAV is paired afterwards (newly added or already present),
     *         {@code false} if it could not be added because the limit was reached.
     */
    boolean pairUav(UUID uav);

    /** Removes the UAV from the paired set. @return true if it was present. */
    boolean unpairUav(UUID uav);

    /** Maximum number of UAVs this holder may be paired to. */
    int getMaxPairedUavs();

    default boolean canPairMore() {
        return getPairedUavs().size() < getMaxPairedUavs();
    }
}
