package com.norwood.mcheli.ship;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

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
                this.renderCrosshair(plane, player);
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

    public void drawKeybind(MCH_EntityShip aircraft, EntityPlayer player, int seatID) {
        var info = aircraft.getPlaneInfo();
        if (MCH_Config.HideKeybind.prmBool || info == null) return;

        var colorActive = -1342177281;
        var colorInactive = -1349546097;

        var screenWidth = this.centerX * 2;
        var leftX = Math.max(10, (int) (screenWidth * 0.05));
        var rightX = Math.min(screenWidth - 100, (int) (screenWidth * 0.80));

        this.currentLeftY = this.centerY - 80;
        this.currentRightY = this.centerY - 80;

        var theme = new LayoutTheme(leftX, rightX, colorActive, colorInactive);
        drawAircraftKeyBinds(aircraft, info, player, seatID, theme);

        var freeLookPressed = Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt);

        if (seatID == 0 && info.isEnableGunnerMode && !freeLookPressed) {
            var color = aircraft.isHoveringMode() ? theme.inactive() : theme.active();
            var mode = aircraft.getIsGunnerMode(player) ? "Normal" : "Gunner";
            drawRightKey(mode, MCH_Config.KeySwitchMode.prmInt, theme.rightX(), color);
        }

        if (seatID > 0 && aircraft.canSwitchGunnerModeOtherSeat(player)) {
            var mode = aircraft.getIsGunnerMode(player) ? "Normal" : "Camera";
            drawRightKey(mode, MCH_Config.KeySwitchMode.prmInt, theme.rightX(), theme.active());
        }

        if (seatID == 0 && info.isEnableVtol && !freeLookPressed) {
            var stat = aircraft.getVtolMode();
            if (stat != 1) {
                var prefix = stat == 0 ? "VTOL" : "Normal";
                drawRightKey(prefix, MCH_Config.KeyExtra.prmInt, theme.rightX(), theme.active());
            }
        }

        if (aircraft.canEjectSeat(player)) {
            drawRightKey("Eject seat", MCH_Config.KeySwitchHovering.prmInt, theme.rightX(), theme.active());
        }

        if (aircraft.getIsGunnerMode(player) && info.cameraZoom > 1) {
            drawLeftKey("Zoom", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
        } else if (seatID == 0) {
            if (aircraft.canFoldWing() || aircraft.canUnfoldWing()) {
                drawLeftKey("FoldWing", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
            } else if (aircraft.canUnfoldHatch() || aircraft.canFoldHatch()) {
                drawLeftKey("OpenHatch", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
            }
        }
    }
}
