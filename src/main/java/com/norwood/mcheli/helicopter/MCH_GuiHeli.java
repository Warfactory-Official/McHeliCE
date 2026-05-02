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
                this.drawTurretBallistics(heli, player);
            }

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
        var info = heli.getHeliInfo();
        if (MCH_Config.HideKeybind.prmBool || info == null) return;

        var colorActive = -1342177281;
        var colorInactive = -1349546097;

        var screenWidth = this.centerX * 2;
        var leftX = Math.max(10, (int) (screenWidth * 0.05));
        var rightX = Math.min(screenWidth - 100, (int) (screenWidth * 0.80));

        this.currentLeftY = this.centerY - 80;
        this.currentRightY = this.centerY - 80;

        var theme = new LayoutTheme(leftX, rightX, colorActive, colorInactive);

        drawAircraftKeyBinds(heli, info, player, seatID, theme);

        var freeLookPressed = Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt);

        if (seatID == 0 && info.isEnableGunnerMode && !freeLookPressed) {
            var color = heli.isHoveringMode() ? theme.inactive() : theme.active();
            var mode = heli.getIsGunnerMode(player) ? "Normal" : "Gunner";
            drawRightKey(mode, MCH_Config.KeySwitchMode.prmInt, theme.rightX(), color);
        }

        if (seatID > 0 && heli.canSwitchGunnerModeOtherSeat(player)) {
            var mode = heli.getIsGunnerMode(player) ? "Normal" : "Camera";
            drawRightKey(mode, MCH_Config.KeySwitchMode.prmInt, theme.rightX(), theme.active());
        }

        if (seatID == 0 && !freeLookPressed) {
            var color = heli.getIsGunnerMode(player) ? theme.inactive() : theme.active();
            var mode = heli.getIsGunnerMode(player) ? "Normal" : "Hovering";
            drawRightKey(mode, MCH_Config.KeySwitchHovering.prmInt, theme.rightX(), color);
        }

        if (seatID == 0) {
            var towChain = heli.getTowChainEntity();
            if (towChain != null && !towChain.isDead) {
                drawRightKey("Drop", MCH_Config.KeyExtra.prmInt, theme.rightX(), theme.active());
            } else if (info.isEnableFoldBlade &&
                    MCH_Lib.getBlockIdY(heli.world, heli.posX, heli.posY, heli.posZ, 1, -2, true) > 0 &&
                    heli.getCurrentThrottle() <= 0.01) {
                drawRightKey("FoldBlade", MCH_Config.KeyExtra.prmInt, theme.rightX(), theme.active());
            }
        }

        if ((heli.getIsGunnerMode(player) || heli.isUAV()) && info.cameraZoom > 1) {
            drawLeftKey("Zoom", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
        } else if (seatID == 0 && (heli.canUnfoldHatch() || heli.canFoldHatch())) {
            drawLeftKey("OpenHatch", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
        }
    }
}
