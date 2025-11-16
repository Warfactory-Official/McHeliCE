package com.norwood.mcheli.helicopter;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_EntityTvMissile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiHeli extends MCH_AircraftCommonGui {

    public MCH_GuiHeli(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return MCH_EntityAircraft.getAircraft_RiddenOrControl(player) instanceof MCH_EntityHeli;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (ac instanceof MCH_EntityHeli heli && !ac.isDestroyed()) {
            int seatID = ac.getSeatIdByEntity(player);
            GL11.glLineWidth(scaleFactor);
            if (heli.getCameraMode(player) == 1) {
                this.drawNightVisionNoise();
            }

            if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                if (seatID == 0 && heli.getIsGunnerMode(player)) {
                    this.drawHud(ac, player, 1);
                } else {
                    this.drawHud(ac, player, seatID);
                }
            }

            this.drawDebugtInfo(heli);
            if (!heli.getIsGunnerMode(player)) {
                if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                    this.drawKeyBind(heli, player, seatID);
                }

                this.drawHitMarker(heli, -14101432, seatID);
            } else {
                if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                    MCH_EntityTvMissile tvmissile = heli.getTVMissile();
                    if (!heli.isMissileCameraMode(player)) {
                        this.drawKeyBind(heli, player, seatID);
                    } else if (tvmissile != null) {
                        this.drawTvMissileNoise(heli, tvmissile);
                    }
                }

                this.drawHitMarker(heli, -805306369, seatID);
            }
        }
    }

    public void drawKeyBind(MCH_EntityHeli heli, EntityPlayer player, int seatID) {
        if (!MCH_Config.HideKeybind.prmBool) {
            MCH_HeliInfo info = heli.getHeliInfo();
            if (info != null) {
                int colorActive = -1342177281;
                int colorInactive = -1349546097;
                int RX = this.centerX + 120;
                int LX = this.centerX - 200;
                this.drawKeyBind(heli, info, player, seatID, RX, LX, colorActive, colorInactive);
                if (seatID == 0 && info.isEnableGunnerMode && !Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                    int c = heli.isHoveringMode() ? colorInactive : colorActive;
                    String msg = (heli.getIsGunnerMode(player) ? "Normal" : "Gunner") + " : " +
                            MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt);
                    this.drawString(msg, RX, this.centerY - 70, c);
                }

                if (seatID > 0 && heli.canSwitchGunnerModeOtherSeat(player)) {
                    String msg = (heli.getIsGunnerMode(player) ? "Normal" : "Camera") + " : " +
                            MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt);
                    this.drawString(msg, RX, this.centerY - 40, colorActive);
                }

                if (seatID == 0 && !Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                    int c = heli.getIsGunnerMode(player) ? colorInactive : colorActive;
                    String msg = (heli.getIsGunnerMode(player) ? "Normal" : "Hovering") + " : " +
                            MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt);
                    this.drawString(msg, RX, this.centerY - 60, c);
                }

                if (seatID == 0) {
                    if (heli.getTowChainEntity() != null && !heli.getTowChainEntity().isDead) {
                        String msg = "Drop  : " + MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt);
                        this.drawString(msg, RX, this.centerY - 30, colorActive);
                    } else if (info.isEnableFoldBlade &&
                            MCH_Lib.getBlockIdY(heli.world, heli.posX, heli.posY, heli.posZ, 1, -2, true) > 0 &&
                            heli.getCurrentThrottle() <= 0.01) {
                                String msg = "FoldBlade  : " + MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt);
                                this.drawString(msg, RX, this.centerY - 30, colorActive);
                            }
                }

                if ((heli.getIsGunnerMode(player) || heli.isUAV()) && info.cameraZoom > 1) {
                    String msg = "Zoom : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                    this.drawString(msg, LX, this.centerY - 80, colorActive);
                } else if (seatID == 0 && (heli.canFoldHatch() || heli.canUnfoldHatch())) {
                    String msg = "OpenHatch : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                    this.drawString(msg, LX, this.centerY - 80, colorActive);
                }
            }
        }
    }
}
