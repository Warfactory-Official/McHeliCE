package com.norwood.mcheli.vehicle;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.MCH_AircraftClientTickHandler;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.networking.data.DataPlayerControlVehicle;
import com.norwood.mcheli.networking.packet.control.PacketPlayerControlVehicle;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientVehicleTickHandler extends MCH_AircraftClientTickHandler {

    public MCH_Key KeySwitchMode;
    public MCH_Key KeySwitchHovering;
    public MCH_Key KeyZoom;
    public MCH_Key KeyExtra;
    public MCH_Key[] Keys;

    public MCH_ClientVehicleTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft, config);
        this.updateKeybind(config);
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        super.updateKeybind(config);
        this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
        this.KeySwitchHovering = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
        this.KeyExtra = new MCH_Key(MCH_Config.KeyExtra.prmInt);
        this.Keys = new MCH_Key[] {
                this.KeyUp,
                this.KeyDown,
                this.KeyRight,
                this.KeyLeft,
                this.KeySwitchMode,
                this.KeySwitchHovering,
                this.KeyUseWeapon,
                this.KeySwWeaponMode,
                this.KeySwitchWeapon1,
                this.KeySwitchWeapon2,
                this.KeyZoom,
                this.KeyCameraMode,
                this.KeyUnmount,
                this.KeyUnmountForce,
                this.KeyFlare,
                this.KeyExtra,
                this.KeyGUI
        };
    }

    protected void update(EntityPlayer player, MCH_EntityVehicle vehicle, MCH_VehicleInfo info) {
        if (info != null) {
            setRotLimitPitch(info.minRotationPitch, info.maxRotationPitch, player);
        }

        vehicle.updateCameraRotate(player.rotationYaw, player.rotationPitch);
        vehicle.updateRadar(5);
    }

    @Override
    protected void onTick(boolean inGUI) {
        for (MCH_Key k : this.Keys) {
            k.update();
        }

        this.isBeforeRiding = this.isRiding;
        EntityPlayer player = this.mc.player;
        MCH_EntityVehicle vehicle = null;
        boolean isPilot = true;
        if (player != null) {
            if (player.getRidingEntity() instanceof MCH_EntityVehicle) {
                vehicle = (MCH_EntityVehicle) player.getRidingEntity();
            } else if (player.getRidingEntity() instanceof MCH_EntitySeat seat) {
                if (seat.getParent() instanceof MCH_EntityVehicle) {
                    isPilot = false;
                    vehicle = (MCH_EntityVehicle) seat.getParent();
                }
            }
        }

        if (vehicle != null && vehicle.getAcInfo() != null) {
            MCH_Lib.disableFirstPersonItemRender(player.getHeldItemMainhand());
            this.update(player, vehicle, vehicle.getVehicleInfo());
            MCH_ViewEntityDummy viewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.world);
            viewEntityDummy.update(vehicle.camera);
            if (!inGUI) {
                if (!vehicle.isDestroyed()) {
                    this.playerControl(player, vehicle, isPilot);
                }
            } else {
                this.playerControlInGUI(player, vehicle, isPilot);
            }

            MCH_Lib.setRenderViewEntity(viewEntityDummy);
            this.isRiding = true;
        } else {
            this.isRiding = false;
        }

        if (!this.isBeforeRiding && this.isRiding) {
            W_Reflection.setThirdPersonDistance(vehicle.thirdPersonDist);
        } else if (this.isBeforeRiding && !this.isRiding) {
            W_Reflection.restoreDefaultThirdPersonDistance();
            MCH_Lib.enableFirstPersonItemRender();
            MCH_Lib.setRenderViewEntity(player);
        }
    }

    protected void playerControlInGUI(EntityPlayer player, MCH_EntityVehicle vehicle, boolean isPilot) {
        this.commonPlayerControlInGUI(player, vehicle, isPilot,
                new PacketPlayerControlVehicle(new DataPlayerControlVehicle()));
    }

    protected void playerControl(EntityPlayer player, MCH_EntityVehicle vehicle, boolean isPilot) {
        DataPlayerControlVehicle pc = new DataPlayerControlVehicle();
        boolean send;
        send = this.commonPlayerControl(player, vehicle, isPilot, pc);
        if (this.KeyExtra.isKeyDown()) {
            if (vehicle.getTowChainEntity() != null) {
                playSoundOK();
                pc.unhitchChainId = W_Entity.getEntityId(vehicle.getTowChainEntity());
                send = true;
            } else {
                playSoundNG();
            }
        }

        // FIXME: Why is it like that?
        // if (!this.KeySwitchHovering.isKeyDown() && this.KeySwitchMode.isKeyDown()) {
        // }

        if (this.KeyZoom.isKeyDown()) {
            if (vehicle.canZoom()) {
                vehicle.zoomCamera();
                playSound("zoom", 0.5F, 1.0F);
            } else if (vehicle.getAcInfo().haveHatch()) {
                if (vehicle.canFoldHatch()) {
                    pc.setSwitchHatch(DataPlayerControlAircraft.HatchSwitch.FOLD);
                    send = true;
                } else if (vehicle.canUnfoldHatch()) {
                    pc.setSwitchHatch(DataPlayerControlAircraft.HatchSwitch.UNFOLD);
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        }

        if (send) {
            new PacketPlayerControlVehicle(pc).sendToServer();
        }
    }
}
