package com.norwood.mcheli.gui;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftGui;
import com.norwood.mcheli.aircraft.MCH_AircraftGuiContainer;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.block.MCH_DraftingTableGui;
import com.norwood.mcheli.block.MCH_DraftingTableGuiContainer;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.network.PooledGuiParameter;
import com.norwood.mcheli.multiplay.MCH_ContainerScoreboard;
import com.norwood.mcheli.multiplay.MCH_GuiScoreboard;
import com.norwood.mcheli.uav.MCH_ContainerUavStation;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.MCH_GuiUavStation;

public class MCH_GuiCommonHandler implements IGuiHandler {

    public static final int GUIID_UAV_STATION = 0;
    public static final int GUIID_AIRCRAFT = 1;
    public static final int GUIID_CONFIG = 2;
    public static final int GUIID_INVENTORY = 3;
    public static final int GUIID_DRAFTING = 4;
    public static final int GUIID_MULTI_MNG = 5;

    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        MCH_Lib.DbgLog(world, "MCH_GuiCommonHandler.getServerGuiElement ID=%d (%d, %d, %d)", id, x, y, z);

        return switch (id) {
            case GUIID_UAV_STATION -> {
                Entity uavStation = PooledGuiParameter.getEntity(player);
                PooledGuiParameter.resetEntity(player);
                if (uavStation instanceof MCH_EntityUavStation station) {
                    yield new MCH_ContainerUavStation(player.inventory, station);
                }
                yield null;
            }

            case GUIID_AIRCRAFT -> {
                MCH_EntityAircraft ac = null;
                Entity riding = player.getRidingEntity();
                if (riding instanceof MCH_EntityAircraft a) {
                    ac = a;
                } else if (riding instanceof MCH_EntityUavStation station) {
                    ac = station.getControlAircract();
                }

                yield ac != null ? new MCH_AircraftGuiContainer(player, ac) : null;
            }

            case GUIID_CONFIG -> new MCH_ConfigGuiContainer(player);

            case GUIID_DRAFTING -> new MCH_DraftingTableGuiContainer(player, x, y, z);

            case GUIID_MULTI_MNG -> {
                if (!MCH_Utils.getServer().isSinglePlayer() || MCH_Config.DebugLog) {
                    yield new MCH_ContainerScoreboard(player);
                }
                yield null;
            }

            default -> null;
        };
    }

    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        MCH_Lib.DbgLog(world, "MCH_GuiCommonHandler.getClientGuiElement ID=%d (%d, %d, %d)", id, x, y, z);

        return switch (id) {
            case GUIID_UAV_STATION -> {
                Entity uavStation = PooledGuiParameter.getEntity(player);
                PooledGuiParameter.resetEntity(player);
                if (uavStation instanceof MCH_EntityUavStation station) {
                    yield new MCH_GuiUavStation(player.inventory, station);
                }
                yield null;
            }

            case GUIID_AIRCRAFT -> {
                MCH_EntityAircraft ac = null;
                Entity riding = player.getRidingEntity();
                if (riding instanceof MCH_EntityAircraft a) {
                    ac = a;
                } else if (riding instanceof MCH_EntityUavStation station) {
                    ac = station.getControlAircract();
                }

                yield ac != null ? new MCH_AircraftGui(player, ac) : null;
            }

            case GUIID_CONFIG -> new MCH_ConfigGui(player);

            case GUIID_DRAFTING -> new MCH_DraftingTableGui(player, x, y, z);

            case GUIID_MULTI_MNG -> new MCH_GuiScoreboard(player);

            default -> null;
        };
    }
}
