package com.norwood.mcheli.helper.block;

import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import org.jetbrains.annotations.NotNull;

public enum EnumDirection8 implements IStringSerializable {

    SOUTH(0, 4, "south", new Vec3i(0, 0, 1)),
    SOUTHWEST(1, 5, "southwest", new Vec3i(-1, 0, 1)),
    WEST(2, 6, "west", new Vec3i(-1, 0, 0)),
    NORTHWEST(3, 7, "northwest", new Vec3i(-1, 0, -1)),
    NORTH(4, 0, "north", new Vec3i(0, 0, -1)),
    NORTHEAST(5, 1, "northeast", new Vec3i(1, 0, -1)),
    EAST(6, 2, "east", new Vec3i(1, 0, 0)),
    SOUTHEAST(7, 3, "southeast", new Vec3i(1, 0, 1));

    public static final EnumDirection8[] VALUES = new EnumDirection8[8];

    static {
        for (EnumDirection8 value : values()) {
            VALUES[value.index] = value;
        }
    }

    private final int index;
    private final int opposite;
    private final String name;
    private final Vec3i directionVec;

    EnumDirection8(int index, int opposite, String name, Vec3i dirVec) {
        this.index = index;
        this.opposite = opposite;
        this.name = name;
        this.directionVec = dirVec;
    }

    public static EnumDirection8 getFront(int index) {
        return VALUES[MathHelper.abs(index % VALUES.length)];
    }

    public static EnumDirection8 fromAngle(double angle) {
        return getFront(MathHelper.floor(angle / 45.0 + 0.5) & 7);
    }

    public int getIndex() {
        return this.index;
    }

    public @NotNull String getName() {
        return this.name;
    }

    public EnumDirection8 opposite() {
        return getFront(this.opposite);
    }

    public Vec3i getDirectionVec() {
        return this.directionVec;
    }

    public float getAngle() {
        return (this.index & 7) * 45;
    }
}
