package com.norwood.mcheli.tank;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiTank extends MCH_AircraftCommonGui {

    public MCH_GuiTank(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(player) instanceof MCH_EntityTank;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (ac instanceof MCH_EntityTank tank && !ac.isDestroyed()) {
            int seatID = ac.getSeatIdByEntity(player);
            GL11.glLineWidth(scaleFactor);
            if (tank.getCameraMode(player) == 1) {
                this.drawNightVisionNoise();
            }

            if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                this.drawHud(ac, player, seatID);
                this.renderCrosshair(tank, player);
            }

            this.drawDebugtInfo(tank);
            if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                if (tank.getTVMissile() == null || !tank.getIsGunnerMode(player) && !tank.isUAV()) {
                    this.drawKeybind(tank, player, seatID);
                } else {
                    this.drawTvMissileNoise(tank, tank.getTVMissile());
                }
            }

            this.drawHitMarker(tank, -14101432, seatID);
        }
    }

    public void drawDebugtInfo(MCH_EntityTank ac) {
        if (MCH_Config.DebugLog) {
        }
    }

    public void drawKeybind(MCH_EntityTank tank, EntityPlayer player, int seatID) {
        var info = tank.getTankInfo();
        if (MCH_Config.HideKeybind.prmBool || info == null) return;

        var colorActive = -1342177281;
        var colorInactive = -1349546097;

        var screenWidth = this.centerX * 2;
        var leftX = Math.max(10, (int) (screenWidth * 0.05));
        var rightX = Math.min(screenWidth - 100, (int) (screenWidth * 0.80));

        this.currentLeftY = this.centerY - 80;
        this.currentRightY = this.centerY - 80;

        var theme = new LayoutTheme(leftX, rightX, colorActive, colorInactive);

        drawAircraftKeyBinds(tank, info, player, seatID, theme);

        if (seatID == 0 && tank.hasBrake()) {
            drawRightKey("Brake", MCH_Config.KeySwitchHovering.prmInt, theme.rightX(), theme.active());
        }

        if (seatID > 0 && tank.canSwitchGunnerModeOtherSeat(player)) {
            var mode = tank.getIsGunnerMode(player) ? "Normal" : "Camera";
            drawRightKey(mode, MCH_Config.KeySwitchMode.prmInt, theme.rightX(), theme.active());
        }

        if (tank.getIsGunnerMode(player) && info.cameraZoom > 1) {
            drawLeftKey("Zoom", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
        } else if (seatID == 0 && (tank.canUnfoldHatch() || tank.canFoldHatch())) {
            drawLeftKey("OpenHatch", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
        }
    }
}
