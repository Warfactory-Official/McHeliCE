package com.norwood.mcheli.wingman.util;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.uav.IUavStation;
import net.minecraft.entity.Entity;

/**
 * McHeli aircraft access helpers for the Wingman subsystem.
 *
 * <p>Historically this was a reflection layer, because Wingman shipped as a
 * separate addon and could not link against McHeli's internals directly. Now
 * that Wingman is merged into the CE source tree, every access is a direct,
 * type-safe call. The class name is kept so call sites stay unchanged.
 *
 * <p>UAV-station access deliberately goes through {@link IUavStation} to respect
 * CE's interface-based station refactor (never the concrete entity).
 */
public final class McheliReflect {

    private McheliReflect() {}

    public static boolean isAircraft(Entity entity) {
        return entity instanceof MCH_EntityAircraft;
    }

    public static boolean isUAV(Entity entity) {
        return entity instanceof MCH_EntityAircraft && ((MCH_EntityAircraft) entity).isUAV();
    }

    /** Returns the {@link IUavStation} controlling this aircraft, or null. */
    public static IUavStation getUavStation(Entity aircraft) {
        return aircraft instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) aircraft).getUavStation() : null;
    }

    /** Returns the player/entity operating the given {@link IUavStation}, or null. */
    public static Entity getStationRider(Object station) {
        return station instanceof IUavStation ? ((IUavStation) station).getOperator() : null;
    }

    // ─── Throttle ─────────────────────────────────────────────────────────────

    /** Current throttle (0.0–1.0), or -1 if not an aircraft. */
    public static double getCurrentThrottle(Entity aircraft) {
        return aircraft instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) aircraft).getCurrentThrottle() : -1;
    }

    // ─── Fuel ─────────────────────────────────────────────────────────────────

    /** Current fuel remaining, or -1 if not an aircraft. */
    public static double getFuel(Entity aircraft) {
        return aircraft instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) aircraft).getFuel() : -1;
    }

    /** Maximum fuel (from the aircraft info), or -1 if not an aircraft. */
    public static double getMaxFuel(Entity aircraft) {
        return aircraft instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) aircraft).getMaxFuel() : -1;
    }

    /** True when the aircraft has no finite fuel budget (maxFuel &lt;= 0). */
    public static boolean hasInfiniteFuel(Entity aircraft) {
        if (!(aircraft instanceof MCH_EntityAircraft)) return true; // unknown ⇒ treat as infinite
        return ((MCH_EntityAircraft) aircraft).getMaxFuel() <= 0;
    }

    /** Tops the aircraft's fuel up to maximum (no-op for infinite-fuel craft). */
    public static void fillFuel(Entity aircraft) {
        if (!(aircraft instanceof MCH_EntityAircraft)) return;
        MCH_EntityAircraft ac = (MCH_EntityAircraft) aircraft;
        int max = ac.getMaxFuel();
        if (max > 0) {
            ac.setFuel(max);
        }
    }

    // ─── Aircraft kind ─────────────────────────────────────────────────────────

    public static boolean isHelicopter(Entity aircraft) {
        return aircraft instanceof MCH_EntityHeli;
    }

    /**
     * True for genuine VTOL aircraft (a plane fitted with a thrust-vectoring
     * nozzle). Plain fixed-wing planes have a null {@code partNozzle}.
     */
    public static boolean isVtol(Entity aircraft) {
        return aircraft instanceof MCH_EntityPlane && ((MCH_EntityPlane) aircraft).partNozzle != null;
    }

    /** Helicopters and VTOL planes can use HELIPAD markers. */
    public static boolean canUseHelipad(Entity aircraft) {
        return isHelicopter(aircraft) || isVtol(aircraft);
    }

    /**
     * Human-readable aircraft name for GUIs/commands: the model name
     * ({@code getTypeName()}, e.g. "MQ-9"), falling back to the kind name and
     * finally the class simple name.
     */
    public static String getAircraftName(Entity e) {
        if (e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) e;
            String type = ac.getTypeName();
            if (type != null && !type.isEmpty()) return type;
            String kind = ac.getKindName();
            if (kind != null && !kind.isEmpty()) return kind;
        }
        return e != null ? e.getClass().getSimpleName() : "?";
    }
}
