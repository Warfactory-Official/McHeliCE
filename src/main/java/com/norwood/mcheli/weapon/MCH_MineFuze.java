package com.norwood.mcheli.weapon;

/**
 * Fuze/trigger type for a bomb dropped in "mine mode".
 *
 * <p>A bomb whose weapon definition declares any fuze other than {@link #NONE} becomes a
 * <b>persistent</b> entity: instead of detonating on impact it settles on the ground, arms after a
 * short delay, and then detonates when its configured trigger comes within range (see
 * {@code MCH_EntityBomb}). {@link #NONE} keeps the vanilla bomb behaviour.
 */
public enum MCH_MineFuze {
    /** Not a mine -- ordinary bomb behaviour. */
    NONE,
    /** Detonates when a (non-sneaking) player enters the proximity range. */
    PROXIMITY_PLAYER,
    /** Detonates when an MCHeli vehicle/aircraft enters the proximity range. */
    PROXIMITY_VEHICLE
}
