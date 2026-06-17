package com.norwood.mcheli.uav;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;

/**
 * Public control facade for the McHeli UAV system. Addon mods (for example a custom UAV station) use
 * this to connect and disconnect an operator to a UAV without hand-wiring the sensitive mount / link /
 * teardown steps — and without ever touching the tracking or chunk-streaming machinery directly.
 *
 * <h3>What is automatic</h3>
 * The detached-camera support — <b>extended entity tracking range, camera-aware entity visibility, and
 * terrain chunk streaming around the UAV</b> — is driven entirely off the {@link IUavStation} contract:
 * <ul>
 *   <li>{@code TrackerHook} grants the extended range / camera visibility to whoever a UAV's
 *       {@link IUavStation#getOperator()} reports (or whoever is previewing it), and</li>
 *   <li>{@code UavChunkStreamer} streams terrain around any UAV that an {@link IUavStation} reports as
 *       {@link IUavStation#getControlled()} (operator riding) or previewed.</li>
 * </ul>
 * Both engage and release purely from that interface state. So <b>any {@link net.minecraft.entity.Entity}
 * that implements {@link IUavStation} gets the full detached-camera feature set for free</b>; a custom
 * station never manages tracking/streaming itself. The only thing it must do is establish and clear the
 * operator&lt;-&gt;station&lt;-&gt;UAV link — which is exactly {@link #connect} and {@link #disconnect}.
 *
 * <p>All methods are server-side.
 */
public final class MCH_UavControl {

    private MCH_UavControl() {
    }

    public enum Result {
        /** Link established. */
        OK,
        /** The UAV is null, dead, or could not be located. */
        NOT_FOUND,
        /** The UAV exists but is destroyed and must not be controlled. */
        DESTROYED
    }

    /**
     * Connect an operator to a UAV through a station: seat the operator on the station entity (required
     * for the view/control link) and establish the two-way station&lt;-&gt;UAV link. From this point the
     * extended tracking range and chunk streaming engage automatically — the caller does not touch them.
     *
     * @param operator the controlling player (server-side)
     * @param station  the station entity; must be both an {@link net.minecraft.entity.Entity} (to be
     *                 ridden) and an {@link IUavStation} (the control contract)
     * @param uav      the UAV to take control of
     * @return the outcome; only {@link Result#OK} establishes a link
     */
    public static <S extends Entity & IUavStation> Result connect(EntityPlayerMP operator, S station,
                                                                  @Nullable MCH_EntityAircraft uav) {
        if (uav == null || uav.isDead) {
            return Result.NOT_FOUND;
        }
        if (uav.isDestroyed()) {
            return Result.DESTROYED;
        }
        if (operator.getRidingEntity() != station) {
            operator.startRiding(station, true);
        }
        uav.setUavStation(station);
        station.setControlled(uav);
        return Result.OK;
    }

    /**
     * Sever an active link: unlink the controlled UAV and dismount the operator. The extended tracking
     * and chunk streaming then release on their own, because the station no longer reports an operator
     * or controlled UAV. Safe to call when there is no active link.
     */
    public static void disconnect(@Nullable IUavStation station) {
        if (station == null) {
            return;
        }
        MCH_EntityAircraft uav = station.getControlled();
        if (uav != null) {
            uav.setUavStation(null);
        }
        station.setControlled(null);
        Entity operator = station.getOperator();
        if (operator != null) {
            operator.dismountRidingEntity();
        }
    }

    /** @return true if {@code station} currently has an operator controlling a (live) UAV. */
    public static boolean isActive(@Nullable IUavStation station) {
        return station != null && station.getOperator() != null
                && station.getControlled() != null && !station.getControlled().isDead;
    }
}
