package com.norwood.mcheli.networking.data;

import java.util.List;

import net.minecraft.util.math.BlockPos;

import lombok.Getter;
import lombok.Setter;

public class DataExplosionParameters {

    public double x, y, z;
    public float size;
    public int exploderID;
    public boolean inWater;
    @Setter
    @Getter
    private List<BlockPos> affectedPositions;
}
