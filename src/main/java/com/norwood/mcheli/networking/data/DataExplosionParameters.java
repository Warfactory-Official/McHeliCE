package com.norwood.mcheli.networking.data;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class DataExplosionParameters {

    public double x, y, z;
    public float size;
    public int exploderID;
    public boolean inWater;
    @Setter
    @Getter
    private List<BlockPos> affectedPositions;
}
