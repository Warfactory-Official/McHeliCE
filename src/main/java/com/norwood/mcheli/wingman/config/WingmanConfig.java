package com.norwood.mcheli.wingman.config;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class WingmanConfig {

    /** At or above this configured range (blocks) the UAV comm-range cut-off is treated as unlimited. */
    public static final int UAV_UNLIMITED_THRESHOLD = 30000;

    public static int uavControllerRange = 99999;
    public static int uavMaxDistance = 99999;
    public static boolean forceChunkload = false;

    /** Search radius (blocks) for re-linking a UAV station to its last controlled aircraft after load. */
    public static double uavSearchRange() {
        if (uavControllerRange < 0) return UAV_UNLIMITED_THRESHOLD;
        return Math.min(UAV_UNLIMITED_THRESHOLD, Math.max(120.0, uavControllerRange));
    }

    /** Lateral distance from leader centre-line (blocks). */
    public static double formationSideDist = 20.0;
    /** Altitude offset relative to leader Y (blocks). Positive = above leader. */
    public static double formationAltOffset = 0.0;
    /** Distance behind the leader (blocks). */
    public static double formationRearDist = 30.0;
    /** Maximum wingmen per aircraft. Runtime-writable by /wingman maxwings. */
    public static int maxWingmen = 10;
    /** Minimum absolute Y altitude during attack runs. 0 = no floor. Runtime-writable by /wingman minalt. */
    public static double minAttackAltitude = 0.0;
    /** Maximum absolute Y altitude during attack runs. 0 = no ceiling. Runtime-writable by /wingman maxalt. */
    public static double maxAttackAltitude = 0.0;

    public static void load(File configFile) {
        Configuration cfg = new Configuration(configFile);
        cfg.load();

        uavControllerRange = cfg.getInt(
            "controllerRange", "uav", 99999, -1, Integer.MAX_VALUE,
            "UAV controller effective range in blocks. -1 for unlimited."
        );
        uavMaxDistance = cfg.getInt(
            "maxDistance", "uav", 99999, -1, Integer.MAX_VALUE,
            "UAV maximum flight distance in blocks. -1 for unlimited."
        );
        forceChunkload = cfg.getBoolean(
            "forceChunkload", "uav", false,
            "Force chunk loading around UAVs. Warning: may increase server load."
        );
        formationSideDist = cfg.getFloat(
            "formationSideDist", "formation", 20.0f, 1.0f, 500.0f,
            "Lateral distance from the leader centre-line (blocks)."
        );
        formationAltOffset = cfg.getFloat(
            "formationAltOffset", "formation", 0.0f, -500.0f, 500.0f,
            "Altitude offset relative to leader Y (blocks). Positive = above, negative = below."
        );
        formationRearDist = cfg.getFloat(
            "formationRearDist", "formation", 30.0f, 1.0f, 500.0f,
            "Distance behind the leader (blocks)."
        );
        maxWingmen = cfg.getInt(
            "maxWingmen", "formation", 10, 1, 64,
            "Maximum wingmen per aircraft."
        );

        minAttackAltitude = cfg.getFloat(
            "minAttackAltitude", "formation", 0.0f, 0.0f, 10000.0f,
            "Minimum absolute Y altitude for attack runs regardless of weapon type. 0 = no floor."
        );
        maxAttackAltitude = cfg.getFloat(
            "maxAttackAltitude", "formation", 0.0f, 0.0f, 10000.0f,
            "Maximum absolute Y altitude for attack runs regardless of weapon type. 0 = no ceiling."
        );

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }
}
