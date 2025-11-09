package com.norwood.mcheli.aircraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.weapon.MCH_EntityTvMissile;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_McClient;

@SideOnly(Side.CLIENT)
public abstract class MCH_AircraftCommonGui extends MCH_Gui {

    public MCH_AircraftCommonGui(Minecraft minecraft) {
        super(minecraft);
    }

    public void drawHud(MCH_EntityAircraft aircraft, EntityPlayer player, int seatId) {
        MCH_AircraftInfo info = aircraft.getAcInfo();
        if (info == null) return;

        if (aircraft.isMissileCameraMode(player) && aircraft.getTVMissile() != null && info.hudTvMissile != null) {
            info.hudTvMissile.draw(aircraft, player, this.smoothCamPartialTicks);
            return;
        }

        if (seatId >= 0 && seatId < info.hudList.size()) {
            MCH_Hud hud = info.hudList.get(seatId);
            if (hud != null) {
                hud.draw(aircraft, player, this.smoothCamPartialTicks);
            }
        }
    }

    public void drawDebugtInfo(MCH_EntityAircraft ac) {
        if (MCH_Config.DebugLog) {}
    }

    public void drawNightVisionNoise() {
        GlStateManager.enableBlend();
        GlStateManager.color(0.0F, 1.0F, 0.0F, 0.3F);
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        W_McClient.MOD_bindTexture("textures/gui/alpha.png");
        this.drawTexturedModalRectRotate(0.0, 0.0, this.width, this.height, this.rand.nextInt(256),
                this.rand.nextInt(256), 256.0, 256.0, 0.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    public void drawHitMarker(int hitStrength, int maxHitStrength, int baseColor) {
        if (hitStrength <= 0) return;

        final int centerX = this.centerX;
        final int centerY = this.centerY;
        final int outerOffset = 10;
        final int innerOffset = 5;

        // Define crosshair-style marker line segments (4 lines from edges to center cross)
        double[] markerLines = {
                centerX - outerOffset, centerY - outerOffset,
                centerX - innerOffset, centerY - innerOffset,

                centerX - outerOffset, centerY + outerOffset,
                centerX - innerOffset, centerY + innerOffset,

                centerX + outerOffset, centerY - outerOffset,
                centerX + innerOffset, centerY - innerOffset,

                centerX + outerOffset, centerY + outerOffset,
                centerX + innerOffset, centerY + innerOffset,
        };

        int alpha = hitStrength * (256 / maxHitStrength);
        int finalColor = (int) (MCH_Config.hitMarkColorAlpha * alpha) << 24 | MCH_Config.hitMarkColorRGB;// FIXME:
                                                                                                         // basecolor
                                                                                                         // ignored...?

        this.drawLine(markerLines, finalColor);
    }

    public void drawHitMarker(MCH_EntityAircraft ac, int color, int seatID) {
        this.drawHitMarker(ac.getHitStatus(), ac.getMaxHitStatus(), color);
    }

    protected void drawTvMissileNoise(MCH_EntityAircraft ac, MCH_EntityTvMissile tvmissile) {
        GlStateManager.enableBlend();
        GlStateManager.color(0.5F, 0.5F, 0.5F, 0.4F);
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        W_McClient.MOD_bindTexture("textures/gui/noise.png");
        this.drawTexturedModalRectRotate(0.0, 0.0, this.width, this.height, this.rand.nextInt(256),
                this.rand.nextInt(256), 256.0, 256.0, 0.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    public void drawKeyBind(
                            MCH_EntityAircraft aircraft,
                            MCH_AircraftInfo info,
                            EntityPlayer player,
                            int seatID,
                            int rightX,
                            int leftX,
                            int colorActive,
                            int colorInactive) {
        if (seatID == 0) {
            drawConditionalKeyBind(aircraft.canPutToRack(), "PutRack", MCH_Config.KeyPutToRack.prmInt, leftX, -10,
                    colorActive);
            drawConditionalKeyBind(aircraft.canDownFromRack(), "DownRack", MCH_Config.KeyDownFromRack.prmInt, leftX, 0,
                    colorActive);
            drawConditionalKeyBind(aircraft.canRideRack(), "RideRack", MCH_Config.KeyPutToRack.prmInt, leftX, 10,
                    colorActive);
            drawConditionalKeyBind(aircraft.getRidingEntity() != null, "DismountRack",
                    MCH_Config.KeyDownFromRack.prmInt, leftX, 10, colorActive);
        }

        boolean multipleSeats = aircraft.getSeatNum() > 1;
        boolean freeLookPressed = Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt);
        if (seatID > 0 && multipleSeats || freeLookPressed) {
            int seatColor = seatID == 0 ? 'Ｐ' : colorActive;
            String prefix = seatID == 0 ? MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt) + " + " : "";
            drawString("NextSeat : " + prefix + MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt), rightX,
                    centerY - 70, seatColor);
            drawString("PrevSeat : " + prefix + MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt), rightX,
                    centerY - 60, seatColor);
        }

        drawString(
                "Gunner " + (aircraft.getGunnerStatus() ? "ON" : "OFF") + " : " +
                        MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt) + " + " +
                        MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt),
                leftX, centerY - 40, colorActive);

        if (seatID >= 0 && seatID <= 1 && aircraft.haveFlare()) {
            int flareColor = aircraft.isFlarePreparation() ? colorInactive : colorActive;
            drawString("Flare : " + MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt), rightX, centerY - 50,
                    flareColor);
        }

        if (seatID == 0 && info.haveLandingGear()) {
            if (aircraft.canFoldLandingGear()) {
                drawConditionalKeyBind(true, "Gear Up", MCH_Config.KeyGearUpDown.prmInt, rightX, -40, colorActive);
            } else if (aircraft.canUnfoldLandingGear()) {
                drawConditionalKeyBind(true, "Gear Down", MCH_Config.KeyGearUpDown.prmInt, rightX, -40, colorActive);
            }
        }

        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        if (aircraft.getWeaponNum() > 1) {
            drawConditionalKeyBind(true, "Weapon", MCH_Config.KeySwitchWeapon2.prmInt, leftX, -70, colorActive);
        }

        if (weaponSet.getCurrentWeapon().numMode > 0) {
            drawConditionalKeyBind(true, "WeaponMode", MCH_Config.KeySwWeaponMode.prmInt, leftX, -60, colorActive);
        }

        if (aircraft.canSwitchSearchLight(player)) {
            drawConditionalKeyBind(true, "SearchLight", MCH_Config.KeyCameraMode.prmInt, leftX, -50, colorActive);
        } else if (aircraft.canSwitchCameraMode(seatID)) {
            drawConditionalKeyBind(true, "CameraMode", MCH_Config.KeyCameraMode.prmInt, leftX, -50, colorActive);
        }

        if (seatID == 0 && aircraft.getSeatNum() >= 1) {
            int dismountColor = colorActive;
            String dismountLabel = "Dismount";
            if (info.isEnableParachuting && MCH_Lib.getBlockIdY(aircraft, 3, -10) == 0) {
                dismountLabel = "Parachuting";
            } else if (aircraft.canStartRepelling()) {
                dismountLabel = "Repelling";
                dismountColor = 0x00FF00;
            }
            drawConditionalKeyBind(true, dismountLabel, MCH_Config.KeyUnmount.prmInt, leftX, -30, dismountColor);
        }

        boolean canFreeLook = seatID == 0 && aircraft.canSwitchFreeLook() ||
                seatID > 0 && aircraft.canSwitchGunnerModeOtherSeat(player);
        drawConditionalKeyBind(canFreeLook, "FreeLook", MCH_Config.KeyFreeLook.prmInt, leftX, -20, colorActive);
    }

    private void drawConditionalKeyBind(boolean condition, String label, int key, int x, int yOffset, int color) {
        if (condition) {
            String keyName = MCH_KeyName.getDescOrName(key);
            drawString(label + " : " + keyName, x, centerY + yOffset, color);
        }
    }
}
