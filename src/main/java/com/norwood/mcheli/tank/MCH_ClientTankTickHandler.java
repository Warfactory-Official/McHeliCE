package com.norwood.mcheli.tank;

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
import com.norwood.mcheli.networking.packet.control.PacketPlayerControlTank;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.W_Reflection;

public class MCH_ClientTankTickHandler extends MCH_AircraftClientTickHandler {

    public MCH_Key KeySwitchMode;
    public MCH_Key KeyZoom;
    public MCH_Key[] Keys;

    public MCH_ClientTankTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft, config);
        this.updateKeybind(config);
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        super.updateKeybind(config);
        this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
        this.Keys = new MCH_Key[] {
                this.KeyUp,
                this.KeyDown,
                this.KeyRight,
                this.KeyLeft,
                this.KeySwitchMode,
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
                this.KeyBrake,
                this.KeyPutToRack,
                this.KeyDownFromRack
        };
    }

    protected void update(EntityPlayer player, MCH_EntityTank tank) {
        if (tank.getIsGunnerMode(player)) {
            MCH_SeatInfo seatInfo = tank.getSeatInfo(player);
            if (seatInfo != null) {
                setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
            }
        }

        tank.updateRadar(10);
        tank.updateCameraRotate(player.rotationYaw, player.rotationPitch);
    }

    @Override
    protected void onTick(boolean inGUI) {
        for (MCH_Key k : this.Keys) {
            k.update();
        }

        this.isBeforeRiding = this.isRiding;
        EntityPlayer player = this.mc.player;
        MCH_EntityTank tank = null;
        boolean isPilot = true;
        if (player != null) {
            if (player.getRidingEntity() instanceof MCH_EntityTank) {
                tank = (MCH_EntityTank) player.getRidingEntity();
            } else if (player.getRidingEntity() instanceof MCH_EntitySeat seat) {
                if (seat.getParent() instanceof MCH_EntityTank) {
                    isPilot = false;
                    tank = (MCH_EntityTank) seat.getParent();
                }
            } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
                if (uavStation.getControlAircract() instanceof MCH_EntityTank) {
                    tank = (MCH_EntityTank) uavStation.getControlAircract();
                }
            }
        }

        if (tank != null && tank.getAcInfo() != null) {
            this.update(player, tank);
            MCH_ViewEntityDummy viewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.world);
            viewEntityDummy.update(tank.camera);
            if (!inGUI) {
                if (!tank.isDestroyed()) {
                    this.playerControl(player, tank, isPilot);
                }
            } else {
                this.playerControlInGUI(player, tank, isPilot);
            }

            boolean hideHand = true;
            if ((!isPilot || !tank.isAlwaysCameraView()) && !tank.getIsGunnerMode(player) && tank.getCameraId() <= 0) {
                MCH_Lib.setRenderViewEntity(player);
                if (!isPilot && tank.getCurrentWeaponID(player) < 0) {
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
            W_Reflection.setThirdPersonDistance(tank.thirdPersonDist);
            MCH_ViewEntityDummy.getInstance(this.mc.world).setPosition(tank.posX, tank.posY + 0.5, tank.posZ);
        } else if (this.isBeforeRiding && !this.isRiding) {
            W_Reflection.restoreDefaultThirdPersonDistance();
            MCH_Lib.enableFirstPersonItemRender();
            MCH_Lib.setRenderViewEntity(player);
            W_Reflection.setCameraRoll(0.0F);
        }
    }

    protected void playerControlInGUI(EntityPlayer player, MCH_EntityTank tank, boolean isPilot) {
        this.commonPlayerControlInGUI(player, tank, isPilot,
                new PacketPlayerControlTank(new DataPlayerControlAircraft()));
    }

    protected void playerControl(EntityPlayer player, MCH_EntityTank tank, boolean isPilot) {
        DataPlayerControlAircraft pc = new DataPlayerControlVehicle();
        boolean send;
        send = this.commonPlayerControl(player, tank, isPilot, pc);
        if (tank.getAcInfo().defaultFreelook && pc.switchFreeLook > 0) {
            pc.switchFreeLook = 0;
        }

        if (isPilot) {
            if (this.KeySwitchMode.isKeyDown()) {
                if (tank.getIsGunnerMode(player) && tank.canSwitchCameraPos()) {
                    pc.switchMode = DataPlayerControlAircraft.ModeSwitch.GUNNER_OFF;
                    tank.switchGunnerMode(false);
                    send = true;
                    tank.setCameraId(1);
                } else if (tank.getCameraId() > 0) {
                    tank.setCameraId(tank.getCameraId() + 1);
                    if (tank.getCameraId() >= tank.getCameraPosNum()) {
                        tank.setCameraId(0);
                    }
                } else if (tank.canSwitchGunnerMode()) {
                    pc.switchMode = tank.getIsGunnerMode(player) ? DataPlayerControlAircraft.ModeSwitch.GUNNER_OFF :
                            DataPlayerControlAircraft.ModeSwitch.GUNNER_ON;
                    tank.switchGunnerMode(!tank.getIsGunnerMode(player));
                    send = true;
                    tank.setCameraId(0);
                } else if (tank.canSwitchCameraPos()) {
                    tank.setCameraId(1);
                } else {
                    playSoundNG();
                }
            }
        } else if (this.KeySwitchMode.isKeyDown()) {
            if (tank.canSwitchGunnerModeOtherSeat(player)) {
                tank.switchGunnerModeOtherSeat(player);
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (this.KeyZoom.isKeyDown()) {
            boolean isUav = tank.isUAV() && !tank.getAcInfo().haveHatch();
            if (tank.getIsGunnerMode(player) || isUav) {
                tank.zoomCamera();
                playSound("zoom", 0.5F, 1.0F);
            } else if (isPilot && tank.getAcInfo().haveHatch()) {
                if (tank.canFoldHatch()) {
                    pc.setSwitchHatch(DataPlayerControlAircraft.HatchSwitch.FOLD);
                    send = true;
                } else if (tank.canUnfoldHatch()) {
                    pc.setSwitchHatch(DataPlayerControlAircraft.HatchSwitch.UNFOLD);
                    send = true;
                }
            }
        }

        if (send) {
            new PacketPlayerControlTank(pc).sendToServer();
        }
    }
}
