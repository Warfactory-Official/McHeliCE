package com.norwood.mcheli.aircraft;

import net.minecraft.util.math.Vec3d;

public class MCH_MobDropOption {

    public Vec3d pos = Vec3d.ZERO;
    public int interval = 1;

    @Override
    public String toString() {
        return "MCH_MobDropOption{" +
                "pos=" + pos +
                ", interval=" + interval +
                '}';
    }
}
