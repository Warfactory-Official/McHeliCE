package com.norwood.mcheli.event;

import com.norwood.mcheli.MCH_GuiCommon;
import com.norwood.mcheli.command.MCH_GuiTitle;
import com.norwood.mcheli.gltd.MCH_GuiGLTD;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.helicopter.MCH_GuiHeli;
import com.norwood.mcheli.lweapon.MCH_GuiLightWeapon;
import com.norwood.mcheli.mob.MCH_GuiSpawnGunner;
import com.norwood.mcheli.multiplay.MCH_GuiScoreboard;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.plane.MCP_GuiPlane;
import com.norwood.mcheli.ship.MCH_GuiShip;
import com.norwood.mcheli.tank.MCH_GuiTank;
import com.norwood.mcheli.tool.MCH_GuiWrench;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.tool.rangefinder.MCH_GuiRangeFinder;
import com.norwood.mcheli.vehicle.MCH_GuiVehicle;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import static com.norwood.mcheli.event.ClientCommonTickHandler.isDrawScoreboard;

public class GuiTickHandler {

    public final ClientCommonTickHandler HANDLER;
    public final MCH_GuiCommon gui_Common;
    public final MCH_Gui gui_Heli;
    public final MCH_Gui gui_Plane;
    public final MCH_Gui gui_Ship;
    public final MCH_Gui gui_Tank;
    public final MCH_Gui gui_GLTD;
    public final MCH_Gui gui_Vehicle;
    public final MCH_Gui gui_LWeapon;
    public final MCH_Gui gui_Wrench;
    public final MCH_Gui gui_EMarker;
    public final MCH_Gui gui_SwnGnr;
    public final MCH_Gui gui_RngFndr;
    public final MCH_Gui gui_Title;
    public final MCH_Gui[] guis;
    public final MCH_Gui[] guiTicks;

    public GuiTickHandler(ClientCommonTickHandler HANDLER) {
        var minecraft = HANDLER.mc;
        this.HANDLER = HANDLER;
        this.gui_Common = new MCH_GuiCommon(minecraft);
        this.gui_Heli = new MCH_GuiHeli(minecraft);
        this.gui_Plane = new MCP_GuiPlane(minecraft);
        this.gui_Ship = new MCH_GuiShip(minecraft);
        this.gui_Tank = new MCH_GuiTank(minecraft);
        this.gui_GLTD = new MCH_GuiGLTD(minecraft);
        this.gui_Vehicle = new MCH_GuiVehicle(minecraft);
        this.gui_LWeapon = new MCH_GuiLightWeapon(minecraft);
        this.gui_Wrench = new MCH_GuiWrench(minecraft);
        this.gui_SwnGnr = new MCH_GuiSpawnGunner(minecraft);
        this.gui_RngFndr = new MCH_GuiRangeFinder(minecraft);
        this.gui_EMarker = new MCH_GuiTargetMarker(minecraft);
        this.gui_Title = new MCH_GuiTitle(minecraft);
        this.guis = new MCH_Gui[]{this.gui_RngFndr, this.gui_LWeapon, this.gui_Heli, this.gui_Plane, this.gui_Ship, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle};
        this.guiTicks = new MCH_Gui[]{this.gui_Common, this.gui_Heli, this.gui_Plane, this.gui_Ship, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle, this.gui_LWeapon, this.gui_Wrench, this.gui_SwnGnr, this.gui_RngFndr, this.gui_EMarker, this.gui_Title};
    }

    public boolean drawGui(MCH_Gui gui, float partialTicks) {
        if (gui.isDrawGui(Minecraft.getMinecraft().player)) {
            gui.drawScreen(0, 0, partialTicks);
            return true;
        } else {
            return false;
        }
    }


    protected static void handleWrenchUI(EntityPlayer player) {
        ItemStack currentItemstack = player.getHeldItem(EnumHand.MAIN_HAND);
        if (currentItemstack.getItem() instanceof MCH_ItemWrench && player.getItemInUseCount() > 0) {
            W_Reflection.setItemRendererMainProgress(1.0F);
        }
    }

    protected void handleGui(float partialTicks) {
        var mc = Minecraft.getMinecraft();

        if (mc.currentScreen == null || mc.currentScreen instanceof GuiChat || mc.currentScreen.getClass().toString().contains("GuiDriveableController")) {
            for (MCH_Gui gui : this.guis) {
                if (this.drawGui(gui, partialTicks)) {
                    break;
                }
            }

            this.drawGui(this.gui_Common, partialTicks);
            this.drawGui(this.gui_Wrench, partialTicks);
            this.drawGui(this.gui_SwnGnr, partialTicks);
            this.drawGui(this.gui_EMarker, partialTicks);
            if (isDrawScoreboard) {
                MCH_GuiScoreboard.drawList(mc, mc.fontRenderer, false);
            }

            this.drawGui(this.gui_Title, partialTicks);
        }
    }
}
