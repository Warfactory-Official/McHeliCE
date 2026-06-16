package com.norwood.mcheli.uav;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Implemented by a UAV-capable aircraft ({@link com.norwood.mcheli.aircraft.MCH_EntityAircraft}).
 * A UAV may be paired to at most one station at a time; the owning station's entity UUID is stored
 * here so the single-station constraint can be enforced even while the station is unloaded.
 *
 * <p>Exposed as an interface to keep the pairing storage strategy flexible (see
 * {@link IUavPairingHolder}).
 */
public interface IPairableUav {

    /** @return the entity UUID of the owning station, or {@code null} if this UAV is unpaired. */
    @Nullable
    UUID getPairedStation();

    void setPairedStation(@Nullable UUID stationId);

    default boolean isPairedToStation() {
        return getPairedStation() != null;
    }
}
