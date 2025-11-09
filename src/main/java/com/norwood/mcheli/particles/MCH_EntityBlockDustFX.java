package com.norwood.mcheli.particles;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleBlockDust;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

public class MCH_EntityBlockDustFX extends ParticleBlockDust {

    protected MCH_EntityBlockDustFX(World world, double x, double y, double z, double xSpeed, double ySpeed,
                                    double zSpeed, IBlockState state) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed, state);
    }

    public void setScale(float s) {
        this.particleScale = s;
    }

    public static class Factory implements IParticleFactory {

        @Nullable
        public Particle createParticle(
                                       int particleID,
                                       @NotNull World worldIn,
                                       double xCoordIn,
                                       double yCoordIn,
                                       double zCoordIn,
                                       double xSpeedIn,
                                       double ySpeedIn,
                                       double zSpeedIn,
                                       int... p_178902_15_) {
            IBlockState iblockstate = Block.getStateById(p_178902_15_[0]);
            return iblockstate.getRenderType() == EnumBlockRenderType.INVISIBLE ? null :
                    new MCH_EntityBlockDustFX(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn,
                            iblockstate).init();
        }
    }
}
