package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.MCH_SoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class W_WorldFunc {

    public static void MOD_playSoundEffect(World w, double x, double y, double z, String name, float volume,
                                           float pitch) {
        MCH_SoundEvents.playSound(w, x, y, z, MCH_MOD.DOMAIN + ":" + name, volume, pitch);
    }

    public static void MOD_playSoundAtEntity(Entity e, String name, float volume, float pitch) {
        e.playSound(MCH_SoundEvents.getSound(MCH_MOD.DOMAIN + ":" + name), volume, pitch);
    }

    public static int getBlockId(World w, int x, int y, int z) {
        return Block.getIdFromBlock(getBlock(w, x, y, z));
    }

    public static Block getBlock(World w, int x, int y, int z) {
        return w.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static Material getBlockMaterial(World w, int x, int y, int z) {
        return w.getBlockState(new BlockPos(x, y, z)).getMaterial();
    }

    public static boolean isBlockWater(World w, int x, int y, int z) {
        return isEqualBlock(w, x, y, z, W_Block.getWater());
    }

    public static boolean isEqualBlock(World w, int x, int y, int z, Block block) {
        return Block.isEqualTo(getBlock(w, x, y, z), block);
    }

    @Nullable
    public static RayTraceResult clip(World w, Vec3d par1Vec3, Vec3d par2Vec3) {
        return w.rayTraceBlocks(par1Vec3, par2Vec3);
    }

    @Nullable
    public static RayTraceResult clip(World w, Vec3d par1Vec3, Vec3d par2Vec3, boolean b) {
        return w.rayTraceBlocks(par1Vec3, par2Vec3, b);
    }

    @Nullable
    public static RayTraceResult clip(World w, Vec3d par1Vec3, Vec3d par2Vec3, boolean b1, boolean b2, boolean b3) {
        return w.rayTraceBlocks(par1Vec3, par2Vec3, b1, b2, b3);
    }
}
