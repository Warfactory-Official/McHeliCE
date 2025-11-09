package com.norwood.mcheli.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;

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
            super.drawDebugtInfo(ac);
        }
    }

    public void drawKeybind(MCH_EntityTank tank, EntityPlayer player, int seatID) {
        if (!MCH_Config.HideKeybind.prmBool) {
            MCH_TankInfo info = tank.getTankInfo();
            if (info != null) {
                int colorActive = -1342177281;
                int colorInactive = -1349546097;
                int RX = this.centerX + 120;
                int LX = this.centerX - 200;
                this.drawKeyBind(tank, info, player, seatID, RX, LX, colorActive, colorInactive);
                if (seatID == 0 && tank.hasBrake()) {
                    String msg = "Brake : " + MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt);
                    this.drawString(msg, RX, this.centerY - 30, colorActive);
                }

                if (seatID > 0 && tank.canSwitchGunnerModeOtherSeat(player)) {
                    String msg = (tank.getIsGunnerMode(player) ? "Normal" : "Camera") + " : " +
                            MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt);
                    this.drawString(msg, RX, this.centerY - 40, colorActive);
                }

                if (tank.getIsGunnerMode(player) && info.cameraZoom > 1) {
                    String msg = "Zoom : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                    this.drawString(msg, LX, this.centerY - 80, colorActive);
                } else if (seatID == 0 && (tank.canFoldHatch() || tank.canUnfoldHatch())) {
                    String msg = "OpenHatch : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                    this.drawString(msg, LX, this.centerY - 80, colorActive);
                }
            }
        }
    }
}
