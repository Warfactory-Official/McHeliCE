package com.norwood.mcheli.helper.info;

public enum ContentType {

    HELICOPTER("helicopter", "helicopters"),
    PLANE("plane", "planes"),
    SHIP("ship", "ships"),
    ITEM("item", "item"),
    TANK("tank", "tanks"),
    VEHICLE("vehicle", "vehicles"),
    WEAPON("weapon", "weapons"),
    THROWABLE("throwable", "throwable"),
    HUD("hud", "hud");

    public final String type;
    public final String dirName;

    ContentType(String typeName, String dirName) {
        this.type = typeName;
        this.dirName = dirName;
    }

    public static boolean validateDirName(String dirName) {
        for (ContentType type : values()) {
            if (type.dirName.equals(dirName)) {
                return true;
            }
        }

        return false;
    }
}
