package com.norwood.mcheli.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class W_Particle {

    public static W_Particle.BlockParticleParam getParticleTileCrackName(World w, int blockX, int blockY, int blockZ) {
        IBlockState iblockstate = w.getBlockState(new BlockPos(blockX, blockY, blockZ));
        return iblockstate.getMaterial() != Material.AIR ?
                new W_Particle.BlockParticleParam("blockcrack", Block.getStateId(iblockstate)) :
                W_Particle.BlockParticleParam.EMPTY;
    }

    public static W_Particle.BlockParticleParam getParticleTileDustName(World w, int blockX, int blockY, int blockZ) {
        IBlockState iblockstate = w.getBlockState(new BlockPos(blockX, blockY, blockZ));
        return iblockstate.getMaterial() != Material.AIR ?
                new W_Particle.BlockParticleParam("blockdust", Block.getStateId(iblockstate)) :
                W_Particle.BlockParticleParam.EMPTY;
    }

    public static class BlockParticleParam {

        public static final W_Particle.BlockParticleParam EMPTY = new W_Particle.BlockParticleParam();
        public final String name;
        public final int stateId;
        private final boolean empty;

        public BlockParticleParam(String name, int stateId) {
            this.name = name;
            this.stateId = stateId;
            this.empty = false;
        }

        private BlockParticleParam() {
            this.name = "";
            this.stateId = 0;
            this.empty = true;
        }

        public boolean isEmpty() {
            return this.empty;
        }
    }
}
