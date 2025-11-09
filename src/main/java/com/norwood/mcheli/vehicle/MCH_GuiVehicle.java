package com.norwood.mcheli.vehicle;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import com.norwood.mcheli.weapon.MCH_WeaponSet;

@SideOnly(Side.CLIENT)
public class MCH_GuiVehicle extends MCH_AircraftCommonGui {

    static final int COLOR1 = -14066;
    static final int COLOR2 = -2161656;

    public MCH_GuiVehicle(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return player.getRidingEntity() != null && player.getRidingEntity() instanceof MCH_EntityVehicle;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (player.getRidingEntity() != null && player.getRidingEntity() instanceof MCH_EntityVehicle vehicle) {
            if (!vehicle.isDestroyed()) {
                int seatID = vehicle.getSeatIdByEntity(player);
                GL11.glLineWidth(scaleFactor);
                if (vehicle.getCameraMode(player) == 1) {
                    this.drawNightVisionNoise();
                }

                if (vehicle.getIsGunnerMode(player) && vehicle.getTVMissile() != null) {
                    this.drawTvMissileNoise(vehicle, vehicle.getTVMissile());
                }

                this.drawDebugtInfo(vehicle);
                if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                    this.drawHud(vehicle, player, seatID);
                    this.drawKeyBind(vehicle, player);
                }

                this.drawHitMarker(vehicle, 51470, seatID);
            }
        }
    }

    public void drawKeyBind(MCH_EntityVehicle vehicle, EntityPlayer player) {
        if (!MCH_Config.HideKeybind.prmBool) {
            MCH_VehicleInfo info = vehicle.getVehicleInfo();
            if (info != null) {
                int colorActive = -1342177281;
                int colorInactive = -1349546097;
                int RX = this.centerX + 120;
                int LX = this.centerX - 200;
                if (vehicle.haveFlare()) {
                    int c = vehicle.isFlarePreparation() ? colorInactive : colorActive;
                    String msg = "Flare : " + MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt);
                    this.drawString(msg, RX, this.centerY - 50, c);
                }

                String msg = "Gunner " + (vehicle.getGunnerStatus() ? "ON" : "OFF") + " : " +
                        MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt) + " + " +
                        MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt);
                this.drawString(msg, LX, this.centerY - 40, colorActive);
                if (vehicle.getSizeInventory() <= 0 ||
                        vehicle.getTowChainEntity() != null && !vehicle.getTowChainEntity().isDead) {
                    msg = "Drop  : " + MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt);
                    this.drawString(msg, RX, this.centerY - 30, colorActive);
                }

                if (vehicle.camera.getCameraZoom() > 1.0F) {
                    msg = "Zoom : " + MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt);
                    this.drawString(msg, LX, this.centerY - 80, colorActive);
                }

                MCH_WeaponSet ws = vehicle.getCurrentWeapon(player);
                if (vehicle.getWeaponNum() > 1) {
                    msg = "Weapon : " + MCH_KeyName.getDescOrName(MCH_Config.KeySwitchWeapon2.prmInt);
                    this.drawString(msg, LX, this.centerY - 70, colorActive);
                }

                if (ws.getCurrentWeapon().numMode > 0) {
                    msg = "WeaponMode : " + MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt);
                    this.drawString(msg, LX, this.centerY - 60, colorActive);
                }

                if (info.isEnableNightVision) {
                    msg = "CameraMode : " + MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt);
                    this.drawString(msg, LX, this.centerY - 50, colorActive);
                }

                msg = "Dismount all : LShift";
                this.drawString(msg, LX, this.centerY - 30, colorActive);
                if (vehicle.getSeatNum() >= 2) {
                    msg = "Dismount : " + MCH_KeyName.getDescOrName(MCH_Config.KeyUnmount.prmInt);
                    this.drawString(msg, LX, this.centerY - 40, colorActive);
                }
            }
        }
    }
}
