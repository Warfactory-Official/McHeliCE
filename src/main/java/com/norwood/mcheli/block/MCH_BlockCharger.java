package com.norwood.mcheli.block;

import com.norwood.mcheli.wrapper.W_BlockContainer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Charger block. Buffers external energy (FE / GregTech EU / HBM HE — selected by config) and feeds
 * nearby electric MCheli aircraft. See {@link MCH_TileEntityCharger}.
 */
public class MCH_BlockCharger extends W_BlockContainer implements ITileEntityProvider {

    public MCH_BlockCharger(int blockId) {
        super(blockId, Material.IRON);
        this.setSoundType(SoundType.METAL);
        this.setHardness(1.5F);
        this.setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(@NotNull World world, int meta) {
        return new MCH_TileEntityCharger();
    }

    @Override
    public @NotNull EnumBlockRenderType getRenderType(@NotNull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, @NotNull BlockPos pos, @NotNull IBlockState state,
                                    @NotNull EntityPlayer player, @NotNull EnumHand hand, @NotNull EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof MCH_TileEntityCharger charger) {
                player.sendStatusMessage(new TextComponentString(
                        String.format("Charger: %,d / %,d", charger.getEnergyInternal(), charger.getCapacityInternal())), true);
            }
        }
        return true;
    }
}
