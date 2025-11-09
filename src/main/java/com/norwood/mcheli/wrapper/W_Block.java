package com.norwood.mcheli.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import org.jetbrains.annotations.NotNull;

public abstract class W_Block extends Block {

    protected W_Block(Material materialIn) {
        super(materialIn);
    }

    public static Block getBlockFromName(@NotNull String name) {
        return Block.getBlockFromName(name);
    }

    public static Block getSnowLayer() {
        return Blocks.SNOW_LAYER;
    }

    public static boolean isNull(Block block) {
        return block == null || block == Blocks.AIR;
    }

    public static boolean isEqual(int blockId, Block block) {
        return Block.isEqualTo(Block.getBlockById(blockId), block);
    }

    public static boolean isEqual(Block block1, Block block2) {
        return Block.isEqualTo(block1, block2);
    }

    public static Block getWater() {
        return Blocks.WATER;
    }

    public static @NotNull Block getBlockById(int i) {
        return Block.getBlockById(i);
    }
}
