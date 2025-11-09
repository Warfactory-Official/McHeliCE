package com.norwood.mcheli.wrapper;

import net.minecraft.util.math.BlockPos;

public class W_ChunkPosition {

    public static int getChunkPosX(BlockPos c) {
        return c.getX();
    }

    public static int getChunkPosY(BlockPos c) {
        return c.getY();
    }

    public static int getChunkPosZ(BlockPos c) {
        return c.getZ();
    }
}
