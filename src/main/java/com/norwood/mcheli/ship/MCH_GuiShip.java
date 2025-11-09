package com.norwood.mcheli.ship;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;

@SideOnly(Side.CLIENT)
public class MCH_GuiShip extends MCH_AircraftCommonGui {

    public MCH_GuiShip(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(player) instanceof MCH_EntityShip;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (ac instanceof MCH_EntityShip plane && !ac.isDestroyed()) {
            int seatID = ac.getSeatIdByEntity(player);
            GL11.glLineWidth(scaleFactor);
            if (plane.getCameraMode(player) == 1) {
                this.drawNightVisionNoise();
            }

            if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                if (seatID == 0 && plane.getIsGunnerMode(player)) {
                    this.drawHud(ac, player, 1);
                } else {
                    this.drawHud(ac, player, seatID);
                }
            }

            this.drawDebugtInfo(plane);
            if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                if (plane.getTVMissile() == null || !plane.getIsGunnerMode(player) && !plane.isUAV()) {
                    this.drawKeybind(plane, player, seatID);
                } else {
                    this.drawTvMissileNoise(plane, plane.getTVMissile());
                }
            }

            this.drawHitMarker(plane, -14101432, seatID);
        }
    }

    public void drawKeybind(MCH_EntityShip plane, EntityPlayer player, int seatID) {
        if (!MCH_Config.HideKeybind.prmBool) {
            MCH_ShipInfo info = plane.getPlaneInfo();
            if (info != null) {
                int colorActive = -1342177281;
                int colorInactive = -1349546097;
                int RX = this.centerX + 120;
                int LX = this.centerX - 200;
                this.drawKeyBind(plane, info, player, seatID, RX, LX, colorActive, colorInactive);
                if (seatID == 0 && info.isEnableGunnerMode && !Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                    int c = plane.isHoveringMode() ? colorInactive : colorActive;
                    String msg = (plane.getIsGunnerMode(player) ? "Normal" : "Gunner") + " : " +
                            MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt);
                    this.drawString(msg, RX, this.centerY - 70, c);
                }

                if (seatID > 0 && plane.canSwitchGunnerModeOtherSeat(player)) {
                    String msg = (plane.getIsGunnerMode(player) ? "Normal" : "Camera") + " : " +
                            MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt);
                    this.drawString(msg, RX, this.centerY - 40, colorActive);
                }

                if (seatID == 0 && info.isEnableVtol && !Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                    int stat = plane.getVtolMode();
                    if (stat != 1) {
                        String msg = (stat == 0 ? "VTOL : " : "Normal : ") +
                                MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt);
                        this.drawString(msg, RX, this.centerY - 60, colorActive);
                    }
                }

                if (plane.canEjectSeat(player)) {
                    String msg = "Eject seat: " + MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt);
                    this.drawString(msg, RX, this.centerY - 30, colorActive);
                }

                if (plane.getIsGunnerMode(player) && info.cameraZoom > 1) {
                    String msg = "Zoom : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                    this.drawString(msg, LX, this.centerY - 80, colorActive);
                } else if (seatID == 0) {
                    if (plane.canFoldWing() || plane.canUnfoldWing()) {
                        String msg = "FoldWing : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                        this.drawString(msg, LX, this.centerY - 80, colorActive);
                    } else if (plane.canFoldHatch() || plane.canUnfoldHatch()) {
                        String msg = "OpenHatch : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                        this.drawString(msg, LX, this.centerY - 80, colorActive);
                    }
                }
            }
        }
    }
}
