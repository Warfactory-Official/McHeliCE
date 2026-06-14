package com.norwood.mcheli.wingman.client;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class WingmanGuiHandler implements IGuiHandler {

    public static final int GUI_MARKER  = 1;
    public static final int GUI_PLANNER = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null; // コンテナ不要
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        com.norwood.mcheli.wingman.McHeliWingman.logger.info(
            "[WingmanGuiHandler] getClientGuiElement id={} pos=({},{},{})", id, x, y, z);
        if (id == GUI_MARKER) {
            BlockPos pos = new BlockPos(x, y, z);
            TileEntity te = world.getTileEntity(pos);
            com.norwood.mcheli.wingman.McHeliWingman.logger.info(
                "[WingmanGuiHandler] te={}", te);
            if (te instanceof WingmanMarkerTileEntity) {
                WingmanMarkerTileEntity wte = (WingmanMarkerTileEntity) te;
                return new GuiMarkerConfig(pos, wte.getMarkerType(), wte.getMarkerId(), wte.getBaseId());
            }
        }
        return null;
    }
}
