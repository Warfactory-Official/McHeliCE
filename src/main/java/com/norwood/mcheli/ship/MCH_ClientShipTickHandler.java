package com.norwood.mcheli.ship;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.MCH_AircraftClientTickHandler;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.networking.packet.control.PacketPlayerControlShip;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_ClientShipTickHandler extends MCH_AircraftClientTickHandler {

    public MCH_Key KeySwitchMode;
    public MCH_Key KeyEjectSeat;
    public MCH_Key KeyZoom;
    public MCH_Key[] Keys;

    public MCH_ClientShipTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft, config);
        this.updateKeybind(config);
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        super.updateKeybind(config);
        this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
        this.KeyEjectSeat = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
        this.Keys = new MCH_Key[] {
                this.KeyUp,
                this.KeyDown,
                this.KeyRight,
                this.KeyLeft,
                this.KeySwitchMode,
                this.KeyEjectSeat,
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

    protected void update(EntityPlayer player, MCH_EntityShip plane) {
        if (plane.getIsGunnerMode(player)) {
            MCH_SeatInfo seatInfo = plane.getSeatInfo(player);
            if (seatInfo != null) {
                setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
            }
        }

        plane.updateRadar(10);
        plane.updateCameraRotate(player.rotationYaw, player.rotationPitch);
    }

    @Override
    protected void onTick(boolean inGUI) {
        for (MCH_Key k : this.Keys) {
            k.update();
        }

        this.isBeforeRiding = this.isRiding;
        EntityPlayer player = this.mc.player;
        MCH_EntityShip plane = null;
        boolean isPilot = true;
        if (player != null) {
            if (player.getRidingEntity() instanceof MCH_EntityShip) {
                plane = (MCH_EntityShip) player.getRidingEntity();
            } else if (player.getRidingEntity() instanceof MCH_EntitySeat seat) {
                if (seat.getParent() instanceof MCH_EntityShip) {
                    isPilot = false;
                    plane = (MCH_EntityShip) seat.getParent();
                }
            } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
                if (uavStation.getControlAircract() instanceof MCH_EntityShip) {
                    plane = (MCH_EntityShip) uavStation.getControlAircract();
                }
            }
        }

        if (plane != null && plane.getAcInfo() != null) {
            this.update(player, plane);
            MCH_ViewEntityDummy viewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.world);
            viewEntityDummy.update(plane.camera);
            if (!inGUI) {
                if (!plane.isDestroyed()) {
                    this.playerControl(player, plane, isPilot);
                }
            } else {
                this.playerControlInGUI(player, plane, isPilot);
            }

            boolean hideHand = true;
            if ((!isPilot || !plane.isAlwaysCameraView()) && !plane.getIsGunnerMode(player) &&
                    plane.getCameraId() <= 0) {
                MCH_Lib.setRenderViewEntity(player);
                if (!isPilot && plane.getCurrentWeaponID(player) < 0) {
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
            W_Reflection.setThirdPersonDistance(plane.thirdPersonDist);
            MCH_ViewEntityDummy.getInstance(this.mc.world).setPosition(plane.posX, plane.posY + 0.5, plane.posZ);
        } else if (this.isBeforeRiding && !this.isRiding) {
            W_Reflection.restoreDefaultThirdPersonDistance();
            MCH_Lib.enableFirstPersonItemRender();
            MCH_Lib.setRenderViewEntity(player);
            W_Reflection.setCameraRoll(0.0F);
        }
    }

    protected void playerControlInGUI(EntityPlayer player, MCH_EntityShip plane, boolean isPilot) {
        this.commonPlayerControlInGUI(player, plane, isPilot,
                new PacketPlayerControlShip(new DataPlayerControlAircraft()));
    }

    protected void playerControl(EntityPlayer player, MCH_EntityShip plane, boolean isPilot) {
        DataPlayerControlAircraft pc = new DataPlayerControlAircraft();
        boolean send;
        send = this.commonPlayerControl(player, plane, isPilot, pc);
        if (isPilot) {
            if (this.KeySwitchMode.isKeyDown()) {
                if (plane.getIsGunnerMode(player) && plane.canSwitchCameraPos()) {
                    pc.switchMode = DataPlayerControlAircraft.ModeSwitch.GUNNER_OFF;
                    plane.switchGunnerMode(false);
                    send = true;
                    plane.setCameraId(1);
                } else if (plane.getCameraId() > 0) {
                    plane.setCameraId(plane.getCameraId() + 1);
                    if (plane.getCameraId() >= plane.getCameraPosNum()) {
                        plane.setCameraId(0);
                    }
                } else if (plane.canSwitchGunnerMode()) {
                    pc.switchMode = plane.getIsGunnerMode(player) ? DataPlayerControlAircraft.ModeSwitch.GUNNER_OFF :
                            DataPlayerControlAircraft.ModeSwitch.GUNNER_ON;
                    plane.switchGunnerMode(!plane.getIsGunnerMode(player));
                    send = true;
                    plane.setCameraId(0);
                } else if (plane.canSwitchCameraPos()) {
                    plane.setCameraId(1);
                } else {
                    playSoundNG();
                }
            }

            if (this.KeyExtra.isKeyDown()) {
                if (plane.canSwitchVtol()) {
                    boolean currentMode = plane.getNozzleStat();
                    pc.switchVtol = currentMode ? DataPlayerControlAircraft.VtolSwitch.VTOL_OFF :
                            DataPlayerControlAircraft.VtolSwitch.VTOL_ON;

                    plane.swithVtolMode(!currentMode);
                    send = true;
                } else {
                    playSoundNG();
                }
            }
        } else if (this.KeySwitchMode.isKeyDown()) {
            if (plane.canSwitchGunnerModeOtherSeat(player)) {
                plane.switchGunnerModeOtherSeat(player);
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (this.KeyZoom.isKeyDown()) {
            boolean isUav = plane.isUAV() && !plane.getAcInfo().haveHatch() && !plane.getPlaneInfo().haveWing();

            if (plane.getIsGunnerMode(player) || isUav) {
                plane.zoomCamera();
                playSound("zoom", 0.5F, 1.0F);
            } else if (isPilot) {
                if (plane.getAcInfo().haveHatch()) {
                    if (plane.canFoldHatch()) {
                        pc.switchHatch = DataPlayerControlAircraft.HatchSwitch.UNFOLD;
                        send = true;
                    } else if (plane.canUnfoldHatch()) {
                        pc.switchHatch = DataPlayerControlAircraft.HatchSwitch.FOLD;
                        send = true;
                    }
                } else if (plane.canFoldWing()) {
                    pc.switchHatch = DataPlayerControlAircraft.HatchSwitch.UNFOLD;
                    send = true;
                } else if (plane.canUnfoldWing()) {
                    pc.switchHatch = DataPlayerControlAircraft.HatchSwitch.FOLD;
                    send = true;
                }
            }
        }

        if (this.KeyEjectSeat.isKeyDown() && plane.canEjectSeat(player)) {
            pc.setEjectSeat(true);
            send = true;
        }

        if (send) {
            new PacketPlayerControlShip(pc).sendToServer();
        }
    }
}
