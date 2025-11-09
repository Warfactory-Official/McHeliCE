package com.norwood.mcheli.helicopter;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.MCH_AircraftClientTickHandler;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.networking.data.DataPlayerControlVehicle;
import com.norwood.mcheli.networking.packet.control.PacketPlayerControlHeli;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Reflection;

public class MCH_ClientHeliTickHandler extends MCH_AircraftClientTickHandler {

    public MCH_Key KeySwitchMode;
    public MCH_Key KeySwitchHovering;
    public MCH_Key KeyZoom;
    public MCH_Key[] Keys;

    public MCH_ClientHeliTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft, config);
        this.updateKeybind(config);
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        super.updateKeybind(config);
        this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
        this.KeySwitchHovering = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
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
                this.KeyFreeLook,
                this.KeyGUI,
                this.KeyGearUpDown,
                this.KeyPutToRack,
                this.KeyDownFromRack
        };
    }

    protected void update(EntityPlayer player, MCH_EntityHeli heli, boolean isPilot) {
        if (heli.getIsGunnerMode(player)) {
            MCH_SeatInfo seatInfo = heli.getSeatInfo(player);
            if (seatInfo != null) {
                setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
            }
        }

        heli.updateCameraRotate(player.rotationYaw, player.rotationPitch);
        heli.updateRadar(5);
    }

    @Override
    protected void onTick(boolean inGUI) {
        for (MCH_Key k : this.Keys) {
            k.update();
        }

        this.isBeforeRiding = this.isRiding;
        EntityPlayer player = this.mc.player;
        MCH_EntityHeli heli = null;
        boolean isPilot = true;
        if (player != null) {
            if (player.getRidingEntity() instanceof MCH_EntityHeli) {
                heli = (MCH_EntityHeli) player.getRidingEntity();
            } else if (player.getRidingEntity() instanceof MCH_EntitySeat seat) {
                if (seat.getParent() instanceof MCH_EntityHeli) {
                    isPilot = false;
                    heli = (MCH_EntityHeli) seat.getParent();
                }
            } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
                if (uavStation.getControlAircract() instanceof MCH_EntityHeli) {
                    heli = (MCH_EntityHeli) uavStation.getControlAircract();
                }
            }
        }

        if (heli != null && heli.getAcInfo() != null) {
            this.update(player, heli, isPilot);
            MCH_ViewEntityDummy viewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.world);
            viewEntityDummy.update(heli.camera);
            if (!inGUI) {
                if (!heli.isDestroyed()) {
                    this.playerControl(player, heli, isPilot);
                }
            } else {
                this.playerControlInGUI(player, heli, isPilot);
            }

            boolean hideHand = true;
            if ((!isPilot || !heli.isAlwaysCameraView()) && !heli.getIsGunnerMode(player)) {
                MCH_Lib.setRenderViewEntity(player);
                if (!isPilot && heli.getCurrentWeaponID(player) < 0) {
                    hideHand = false;
                }
            } else {
                MCH_Lib.setRenderViewEntity(viewEntityDummy);
            }

            if (hideHand) {
                MCH_Lib.disableFirstPersonItemRender(player.getHeldItemMainhand());
            }

            this.isRiding = true;
        } else {
            this.isRiding = false;
        }

        if (!this.isBeforeRiding && this.isRiding) {
            W_Reflection.setThirdPersonDistance(heli.thirdPersonDist);
        } else if (this.isBeforeRiding && !this.isRiding) {
            W_Reflection.restoreDefaultThirdPersonDistance();
            W_Reflection.setCameraRoll(0.0F);
            MCH_Lib.enableFirstPersonItemRender();
            MCH_Lib.setRenderViewEntity(player);
        }
    }

    protected void playerControlInGUI(EntityPlayer player, MCH_EntityHeli heli, boolean isPilot) {
        this.commonPlayerControlInGUI(player, heli, isPilot,
                new PacketPlayerControlHeli(new DataPlayerControlVehicle()));
    }

    protected void playerControl(EntityPlayer player, MCH_EntityHeli heli, boolean isPilot) {
        var pc = new DataPlayerControlVehicle();
        boolean send;
        send = this.commonPlayerControl(player, heli, isPilot, pc);
        if (isPilot) {
            if (this.KeyExtra.isKeyDown()) {
                if (heli.getTowChainEntity() != null) {
                    playSoundOK();
                    pc.unhitchChainId = W_Entity.getEntityId(heli.getTowChainEntity());
                    send = true;
                } else if (heli.canSwitchFoldBlades()) {
                    if (heli.isFoldBlades()) {
                        heli.unfoldBlades();
                        pc.setBladeStatus(DataPlayerControlVehicle.BladeStatus.UNFOLD);
                    } else {
                        heli.foldBlades();
                        pc.setBladeStatus(DataPlayerControlVehicle.BladeStatus.FOLD);
                    }

                    send = true;
                    playSoundOK();
                } else {
                    playSoundNG();
                }
            }

            if (this.KeySwitchHovering.isKeyDown()) {
                if (heli.canSwitchHoveringMode()) {
                    pc.switchMode = (heli.isHoveringMode() ? DataPlayerControlAircraft.ModeSwitch.HOVERING_OFF :
                            DataPlayerControlAircraft.ModeSwitch.HOVERING_ON);
                    heli.switchHoveringMode(!heli.isHoveringMode());
                    send = true;
                } else {
                    playSoundNG();
                }
            } else if (this.KeySwitchMode.isKeyDown()) {
                if (heli.canSwitchGunnerMode()) {
                    pc.setSwitchMode(heli.getIsGunnerMode(player) ? DataPlayerControlAircraft.ModeSwitch.GUNNER_OFF :
                            DataPlayerControlAircraft.ModeSwitch.GUNNER_ON);
                    heli.switchGunnerMode(!heli.getIsGunnerMode(player));
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        } else if (this.KeySwitchMode.isKeyDown()) {
            if (heli.canSwitchGunnerModeOtherSeat(player)) {
                heli.switchGunnerModeOtherSeat(player);
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (this.KeyZoom.isKeyDown()) {
            boolean isUav = heli.isUAV() && !heli.getAcInfo().haveHatch();
            if (heli.getIsGunnerMode(player) || isUav) {
                heli.zoomCamera();
                playSound("zoom", 0.5F, 1.0F);
            } else if (isPilot && heli.getAcInfo().haveHatch()) {
                if (heli.canFoldHatch()) {
                    pc.setSwitchHatch(DataPlayerControlAircraft.HatchSwitch.FOLD);
                    send = true;
                } else if (heli.canUnfoldHatch()) {

                    pc.setSwitchHatch(DataPlayerControlAircraft.HatchSwitch.UNFOLD);
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        }

        if (send) {
            new PacketPlayerControlHeli(pc).sendToServer();
        }
    }
}
