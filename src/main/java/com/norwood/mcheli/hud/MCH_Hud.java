package com.norwood.mcheli.hud;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

public class MCH_Hud extends MCH_BaseInfo {

    public static final MCH_Hud NoDisp = new MCH_Hud(MCH_Utils.buildinAddon("none"), "none");
    public final String name;
    public final String fileName;
    public boolean isWaitEndif;
    public boolean isIfFalse;
    public boolean exit;
    public final List<MCH_HudItem> list;
    private boolean isDrawing;

    public MCH_Hud(AddonResourceLocation location, String filePath) {
        super(location, filePath);
        this.name = location.getPath();
        this.fileName = filePath;
        this.list = new ArrayList<>();
        this.isDrawing = false;
        this.isIfFalse = false;
        this.exit = false;
    }

    @Override
    public boolean validate() {
        for (MCH_HudItem hud : this.list) {
            hud.parent = this;
        }

        if (this.isWaitEndif) {
            throw new RuntimeException("Endif not found!");
        } else {
            return true;
        }
    }

    @Override
    public void onPostReload() {}

    public void draw(MCH_EntityAircraft ac, EntityPlayer player, float partialTicks) {
        if (MCH_HudItem.mc == null) {
            MCH_HudItem.mc = Minecraft.getMinecraft();
        }

        MCH_HudItem.ac = ac;
        MCH_HudItem.player = player;
        MCH_HudItem.partialTicks = partialTicks;
        ScaledResolution scaledresolution = new W_ScaledResolution(MCH_HudItem.mc, MCH_HudItem.mc.displayWidth,
                MCH_HudItem.mc.displayHeight);
        MCH_HudItem.scaleFactor = scaledresolution.getScaleFactor();
        if (MCH_HudItem.scaleFactor <= 0) {
            MCH_HudItem.scaleFactor = 1;
        }

        MCH_HudItem.width = (double) MCH_HudItem.mc.displayWidth / MCH_HudItem.scaleFactor;
        MCH_HudItem.height = (double) MCH_HudItem.mc.displayHeight / MCH_HudItem.scaleFactor;
        MCH_HudItem.centerX = MCH_HudItem.width / 2.0;
        MCH_HudItem.centerY = MCH_HudItem.height / 2.0;
        this.isIfFalse = false;
        this.isDrawing = false;
        this.exit = false;
        if (ac != null && ac.getAcInfo() != null && player != null) {
            MCH_HudItem.update();
            this.drawItems();
            MCH_HudItem.drawVarMap();
        }
    }

    protected void drawItems() {
        if (!this.isDrawing) {
            this.isDrawing = true;

            for (MCH_HudItem hud : this.list) {
                int line = -1;

                try {
                    line = hud.fileLine;
                    if (hud.canExecute()) {
                        hud.execute();
                        if (this.exit) {
                            break;
                        }
                    }
                } catch (Exception var5) {
                    MCH_Lib.Log("#### Draw HUD Error!!!: line=%d, file=%s", line, this.fileName);
                    var5.printStackTrace();
                    throw new RuntimeException(var5);
                }
            }

            this.exit = false;
            this.isIfFalse = false;
            this.isDrawing = false;
        }
    }

    @Override
    public String toString() {
        return "MCH_Hud{" +
                "name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", isWaitEndif=" + isWaitEndif +
                ", isIfFalse=" + isIfFalse +
                ", exit=" + exit +
                ", list=" + list +
                ", isDrawing=" + isDrawing +
                ", filePath='" + filePath + '\'' +
                ", location=" + location +
                '}';
    }
}
