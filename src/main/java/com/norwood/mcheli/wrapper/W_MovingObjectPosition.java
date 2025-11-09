package com.norwood.mcheli.wrapper;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;

public class W_MovingObjectPosition {

    public static boolean isHitTypeEntity(RayTraceResult m) {
        return m != null && m.typeOfHit == Type.ENTITY;
    }

    public static boolean isHitTypeTile(RayTraceResult m) {
        return m != null && m.typeOfHit == Type.BLOCK;
    }

    public static RayTraceResult newMOP(int p1, int p2, int p3, int p4, Vec3d p5, boolean p6) {
        return new RayTraceResult(p5, EnumFacing.byIndex(p4), new BlockPos(p1, p2, p3));
    }
}
