package com.norwood.mcheli.vehicle;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.aircraft.MCH_AircraftCommonGui;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiVehicle extends MCH_AircraftCommonGui {


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

                if (!isThirdPersonView || MCH_Config.DisplayHUDThirdPerson.prmBool) {
                    this.drawHud(vehicle, player, seatID);
                    this.drawTurretBallistics(vehicle, player);
                    this.drawKeyBind(vehicle, player);
                }

                this.drawHitMarker(vehicle, 51470, seatID);
            }
        }
    }

    public void drawKeyBind(MCH_EntityVehicle vehicle, EntityPlayer player) {
        var info = vehicle.getVehicleInfo();
        if (MCH_Config.HideKeybind.prmBool || info == null) return;

        var colorActive = -1342177281;
        var colorInactive = -1349546097;

        var screenWidth = this.centerX * 2;
        var leftX = Math.max(10, (int) (screenWidth * 0.05));
        var rightX = Math.min(screenWidth - 100, (int) (screenWidth * 0.80));

        this.currentLeftY = this.centerY - 80;
        this.currentRightY = this.centerY - 80;

        var theme = new LayoutTheme(leftX, rightX, colorActive, colorInactive);

        if (vehicle.haveFlare()) {
            var color = vehicle.isFlarePreparation() ? theme.inactive() : theme.active();
            drawRightKey("Flare", MCH_Config.KeyFlare.prmInt, theme.rightX(), color);
        }
        var gunnerStatus = vehicle.getGunnerStatus() ? "ON" : "OFF";
        var gunnerMsg = "Gunner %s : %s + %s".formatted(
                gunnerStatus,
                MCH_KeyName.getDescOrName(MCH_Config.KeyFreeLook.prmInt),
                MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt)
        );
        drawString(gunnerMsg, theme.leftX(), currentLeftY, theme.active());
        currentLeftY += 10;
        var towChain = vehicle.getTowChainEntity();
        if (vehicle.getSizeInventory() <= 0 || (towChain != null && !towChain.isDead)) {
            drawRightKey("Drop", MCH_Config.KeyExtra.prmInt, theme.rightX(), theme.active());
        }
        if (vehicle.camera.getCameraZoom() > 1.0F) {
            drawLeftKey("Zoom", MCH_Config.KeyZoom.prmInt, theme.leftX(), theme.active());
        }
        var ws = vehicle.getCurrentWeapon(player);
        if (vehicle.getWeaponNum() > 1) {
            drawLeftKey("Weapon", MCH_Config.KeySwitchWeapon2.prmInt, theme.leftX(), theme.active());
        }

        if (ws != null && ws.getCurrentWeapon() != null && ws.getCurrentWeapon().numMode > 0) {
            drawLeftKey("WeaponMode", MCH_Config.KeySwWeaponMode.prmInt, theme.leftX(), theme.active());
        }
        if (info.isEnableNightVision) {
            drawLeftKey("CameraMode", MCH_Config.KeyCameraMode.prmInt, theme.leftX(), theme.active());
        }
        if (vehicle.getSeatNum() >= 2) {
            drawLeftKey("Dismount", MCH_Config.KeyUnmount.prmInt, theme.leftX(), theme.active());
        }
        drawString("Dismount all : LShift", theme.leftX(), currentLeftY, theme.active());
        currentLeftY += 10;
    }
}
