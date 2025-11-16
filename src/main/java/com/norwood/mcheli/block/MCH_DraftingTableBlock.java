package com.norwood.mcheli.block;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.block.EnumDirection8;
import com.norwood.mcheli.helper.block.properties.PropertyDirection8;
import com.norwood.mcheli.wrapper.W_BlockContainer;
import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class MCH_DraftingTableBlock extends W_BlockContainer implements ITileEntityProvider {

    public static final PropertyDirection8 DIRECTION8 = PropertyDirection8.create("direction8");
    private final boolean isLighting;

    public MCH_DraftingTableBlock(int blockId, boolean isOn) {
        super(blockId, Material.IRON);
        this.setDefaultState(this.blockState.getBaseState().withProperty(DIRECTION8, EnumDirection8.NORTH));
        this.setSoundType(SoundType.METAL);
        this.setHardness(0.2F);
        this.isLighting = isOn;
        if (isOn) {
            this.setLightLevel(1.0F);
        }
    }

    public boolean onBlockActivated(
                                    World world, @NotNull BlockPos pos, @NotNull IBlockState state,
                                    @NotNull EntityPlayer player, @NotNull EnumHand hand, @NotNull EnumFacing facing,
                                    float par7, float par8, float par9) {
        if (!world.isRemote) {
            if (!player.isSneaking()) {
                MCH_Lib.DbgLog(
                        player.world,
                        "MCH_DraftingTableGui.MCH_DraftingTableGui OPEN GUI (%d, %d, %d)",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ());
                player.openGui(MCH_MOD.instance, 4, world, pos.getX(), pos.getY(), pos.getZ());
            } else {
                EnumDirection8 dir = state.getValue(DIRECTION8);
                MCH_Lib.DbgLog(world, "MCH_DraftingTableBlock.onBlockActivated:yaw=%d Light %s", (int) dir.getAngle(),
                        this.isLighting ? "OFF->ON" : "ON->OFF");
                if (this.isLighting) {
                    world.setBlockState(pos, MCH_MOD.blockDraftingTable.getDefaultState().withProperty(DIRECTION8, dir),
                            2);
                } else {
                    world.setBlockState(pos,
                            MCH_MOD.blockDraftingTableLit.getDefaultState().withProperty(DIRECTION8, dir), 2);
                }

                world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK, SoundCategory.BLOCKS, 0.3F, 0.5F);
            }
        }

        return true;
    }

    public TileEntity createNewTileEntity(@NotNull World world, int a) {
        return new MCH_DraftingTableTileEntity();
    }

    public boolean shouldCheckWeakPower(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                                        @NotNull EnumFacing side) {
        return true;
    }

    public boolean func_149686_d(IBlockState state) {
        return false;
    }

    public boolean func_149662_c(IBlockState state) {
        return false;
    }

    public boolean canHarvestBlock(@NotNull IBlockAccess world, @NotNull BlockPos pos, @NotNull EntityPlayer player) {
        return true;
    }

    public boolean canRenderInPass(int pass) {
        return false;
    }

    public EnumPushReaction func_149656_h(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    public IBlockState func_180642_a(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                     int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(DIRECTION8,
                EnumDirection8.fromAngle(MCH_Lib.getRotate360(placer.rotationYaw)));
    }

    public void func_180633_a(World world, BlockPos pos, IBlockState state, EntityLivingBase entity,
                              ItemStack itemStack) {
        float pyaw = (float) MCH_Lib.getRotate360(entity.rotationYaw);
        pyaw += 22.5F;
        int yaw = (int) (pyaw / 45.0F);
        if (yaw < 0) {
            yaw = yaw % 8 + 8;
        }

        world.setBlockState(pos,
                state.withProperty(DIRECTION8, EnumDirection8.fromAngle(MCH_Lib.getRotate360(entity.rotationYaw))), 2);
        MCH_Lib.DbgLog(world, "MCH_DraftingTableBlock.onBlockPlacedBy:yaw=%d", yaw);
    }

    public boolean func_149710_n(IBlockState state) {
        return true;
    }

    public Item func_180660_a(IBlockState state, Random rand, int fortune) {
        return W_Item.getItemFromBlock(MCH_MOD.blockDraftingTable);
    }

    public ItemStack func_185473_a(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(MCH_MOD.blockDraftingTable);
    }

    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(MCH_MOD.blockDraftingTable);
    }

    public int func_176201_c(IBlockState state) {
        return state.getValue(DIRECTION8).getIndex();
    }

    public IBlockState func_176203_a(int meta) {
        return this.getDefaultState().withProperty(DIRECTION8, EnumDirection8.getFront(meta));
    }

    protected BlockStateContainer func_180661_e() {
        return new BlockStateContainer(this, DIRECTION8);
    }
}
