package com.norwood.mcheli.hud;

public enum MCH_HudItemStringArgs {

    NONE,
    NAME,
    ALTITUDE,
    DATE,
    MC_THOR,
    MC_TMIN,
    MC_TSEC,
    MAX_HP,
    HP,
    HP_PER,
    POS_X,
    POS_Y,
    POS_Z,
    MOTION_X,
    MOTION_Y,
    MOTION_Z,
    INVENTORY,
    WPN_NAME,
    WPN_AMMO,
    WPN_RM_AMMO,
    RELOAD_PER,
    RELOAD_SEC,
    MORTAR_DIST,
    MC_VER,
    MOD_VER,
    MOD_NAME,
    YAW,
    PITCH,
    ROLL,
    PLYR_YAW,
    PLYR_PITCH,
    TVM_POS_X,
    TVM_POS_Y,
    TVM_POS_Z,
    TVM_DIFF,
    CAM_ZOOM,
    UAV_DIST,
    KEY_GUI,
    THROTTLE;

    public static MCH_HudItemStringArgs toArgs(String name) {
        MCH_HudItemStringArgs a = NONE;

        try {
            a = valueOf(name.toUpperCase());
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        return a;
    }
}
