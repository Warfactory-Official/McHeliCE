package com.norwood.mcheli.aircraft;

import static com.norwood.mcheli.networking.packet.PacketSeatPlayerControl.PlayerControlState;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import com.norwood.mcheli.MCH_ClientTickHandlerBase;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.networking.packet.PacketOpenScreen;
import com.norwood.mcheli.networking.packet.PacketSeatPlayerControl;
import com.norwood.mcheli.networking.packet.control.PacketPlayerControlBase;

public abstract class MCH_AircraftClientTickHandler extends MCH_ClientTickHandlerBase {

    public MCH_Key KeyUp;
    public MCH_Key KeyDown;
    public MCH_Key KeyRight;
    public MCH_Key KeyLeft;
    public MCH_Key KeyUseWeapon;
    public MCH_Key KeySwitchWeapon1;
    public MCH_Key KeySwitchWeapon2;
    public MCH_Key KeySwWeaponMode;
    public MCH_Key KeyUnmount;
    public MCH_Key KeyUnmountForce;
    public MCH_Key KeyExtra;
    public MCH_Key KeyFlare;
    public MCH_Key KeyCameraMode;
    public MCH_Key KeyFreeLook;
    public MCH_Key KeyGUI;
    public MCH_Key KeyGearUpDown;
    public MCH_Key KeyPutToRack;
    public MCH_Key KeyDownFromRack;
    public MCH_Key KeyBrake;
    protected boolean isRiding = false;
    protected boolean isBeforeRiding = false;

    public MCH_AircraftClientTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.updateKeybind(config);
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        this.KeyUp = new MCH_Key(MCH_Config.KeyUp.prmInt);
        this.KeyDown = new MCH_Key(MCH_Config.KeyDown.prmInt);
        this.KeyRight = new MCH_Key(MCH_Config.KeyRight.prmInt);
        this.KeyLeft = new MCH_Key(MCH_Config.KeyLeft.prmInt);
        this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
        this.KeySwitchWeapon1 = new MCH_Key(MCH_Config.KeySwitchWeapon1.prmInt);
        this.KeySwitchWeapon2 = new MCH_Key(MCH_Config.KeySwitchWeapon2.prmInt);
        this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
        this.KeyUnmount = new MCH_Key(MCH_Config.KeyUnmount.prmInt);
        this.KeyUnmountForce = new MCH_Key(42);
        this.KeyExtra = new MCH_Key(MCH_Config.KeyExtra.prmInt);
        this.KeyFlare = new MCH_Key(MCH_Config.KeyFlare.prmInt);
        this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
        this.KeyFreeLook = new MCH_Key(MCH_Config.KeyFreeLook.prmInt);
        this.KeyGUI = new MCH_Key(MCH_Config.KeyGUI.prmInt);
        this.KeyGearUpDown = new MCH_Key(MCH_Config.KeyGearUpDown.prmInt);
        this.KeyPutToRack = new MCH_Key(MCH_Config.KeyPutToRack.prmInt);
        this.KeyDownFromRack = new MCH_Key(MCH_Config.KeyDownFromRack.prmInt);
        this.KeyBrake = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
    }

    protected void commonPlayerControlInGUI(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot,
                                            PacketPlayerControlBase pc) {}

    public boolean commonPlayerControl(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot,
                                       DataPlayerControlAircraft pc) {
        if (Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
            if (this.KeyGUI.isKeyDown() || this.KeyExtra.isKeyDown()) {
                PacketSeatPlayerControl psc = new PacketSeatPlayerControl();
                if (isPilot) {
                    psc.switchSeat = this.KeyGUI.isKeyDown() ? PlayerControlState.NEXT : PlayerControlState.PREV;
                } else {
                    ac.keepOnRideRotation = true;
                    psc.switchSeat = PlayerControlState.DISMOUNT;
                }

                psc.sendToServer();
                return false;
            }
        } else if (!isPilot && ac.getSeatNum() > 1) {
            PacketSeatPlayerControl playerControl = new PacketSeatPlayerControl();
            if (this.KeyGUI.isKeyDown()) {
                playerControl.switchSeat = PlayerControlState.NEXT;
                playerControl.sendToServer();
                return false;
            }

            if (this.KeyExtra.isKeyDown()) {
                playerControl.switchSeat = PlayerControlState.PREV;
                playerControl.sendToServer();
                return false;
            }
        }

        boolean send = false;
        if (Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
            if (this.KeyCameraMode.isKeyDown()) {
                pc.setSwitchGunnerStatus(true);
                playSoundOK();
                send = true;
            }
        } else if (this.KeyCameraMode.isKeyDown()) {
            if (ac.haveSearchLight()) {
                if (ac.canSwitchSearchLight(player)) {
                    pc.setSwitchSearchLight(true);
                    playSoundOK();
                    send = true;
                }
            } else if (ac.canSwitchCameraMode()) {
                int beforeMode = ac.getCameraMode(player);
                ac.switchCameraMode(player);
                int mode = ac.getCameraMode(player);
                if (mode != beforeMode) {
                    pc.switchCameraMode = DataPlayerControlAircraft.CameraMode.values()[mode];
                    playSoundOK();
                    send = true;
                }
            } else {
                playSoundNG();
            }
        }

        if (this.KeyUnmount.isKeyDown() && !ac.isDestroyed() && ac.getSizeInventory() > 0 && !isPilot) {
            PacketOpenScreen.send(3);
        }

        if (isPilot) {
            if (this.KeyUnmount.isKeyDown()) {
                pc.isUnmount = DataPlayerControlAircraft.UnmountAction.UNMOUNT_SELF;
                send = true;
            }

            if (this.KeyPutToRack.isKeyDown()) {
                ac.checkRideRack();
                if (ac.canRideRack()) {
                    pc.putDownRack = DataPlayerControlAircraft.RackAction.RIDE;
                    send = true;
                } else if (ac.canPutToRack()) {
                    pc.putDownRack = DataPlayerControlAircraft.RackAction.MOUNT;
                    send = true;
                }
            } else if (this.KeyDownFromRack.isKeyDown()) {
                if (ac.getRidingEntity() != null) {
                    pc.isUnmount = DataPlayerControlAircraft.UnmountAction.UNMOUNT_AIRCRAFT;
                    send = true;
                } else if (ac.canDownFromRack()) {
                    pc.putDownRack = DataPlayerControlAircraft.RackAction.UNMOUNT;
                    send = true;
                }
            }

            if (this.KeyGearUpDown.isKeyDown() && ac.getAcInfo().haveLandingGear()) {
                if (ac.canFoldLandingGear()) {
                    pc.switchGear = DataPlayerControlAircraft.GearSwitch.FOLD;
                    send = true;
                } else if (ac.canUnfoldLandingGear()) {
                    pc.switchGear = DataPlayerControlAircraft.GearSwitch.UNFOLD;
                    send = true;
                }
            }

            if (this.KeyFreeLook.isKeyDown() && ac.canSwitchFreeLook()) {
                pc.switchFreeLook = (byte) (ac.isFreeLookMode() ? 2 : 1);
                send = true;
            }

            if (this.KeyGUI.isKeyDown()) {
                pc.setOpenGui(true);
                send = true;
            }

            if (ac.isRepelling()) {
                pc.setThrottleDown(ac.throttleDown = false);
                pc.setThrottleUp(ac.throttleUp = false);
                pc.setMoveRight(ac.moveRight = false);
                pc.setMoveLeft(ac.moveLeft = false);
            } else if (ac.hasBrake() && this.KeyBrake.isKeyPress()) {
                send |= this.KeyBrake.isKeyDown();
                pc.setThrottleDown(ac.throttleDown = false);
                pc.setThrottleUp(ac.throttleUp = false);
                double dx = ac.posX - ac.prevPosX;
                double dz = ac.posZ - ac.prevPosZ;
                double dist = dx * dx + dz * dz;
                if (ac.getCurrentThrottle() <= 0.03 && dist < 0.01) {
                    pc.setMoveRight(ac.moveRight = false);
                    pc.setMoveLeft(ac.moveLeft = false);
                }

                pc.setUseBrake(true);
            } else {
                send |= this.KeyBrake.isKeyUp();
                MCH_Key[] dKey = new MCH_Key[] { this.KeyUp, this.KeyDown, this.KeyRight, this.KeyLeft };

                for (MCH_Key k : dKey) {
                    if (k.isKeyDown() || k.isKeyUp()) {
                        send = true;
                        break;
                    }
                }

                pc.setThrottleDown(ac.throttleDown = this.KeyDown.isKeyPress());
                pc.setThrottleUp(ac.throttleUp = this.KeyUp.isKeyPress());
                pc.setMoveRight(ac.moveRight = this.KeyRight.isKeyPress());
                pc.setMoveLeft(ac.moveLeft = this.KeyLeft.isKeyPress());

            }
        }

        if (!ac.isDestroyed() && this.KeyFlare.isKeyDown() && ac.getSeatIdByEntity(player) <= 1) {
            if (ac.canUseFlare() && ac.useFlare(ac.getCurrentFlareType())) {
                pc.useFlareType = (byte) ac.getCurrentFlareType();
                ac.nextFlareType();
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (!ac.isDestroyed() && !ac.isPilotReloading()) {
            if (!this.KeySwitchWeapon1.isKeyDown() && !this.KeySwitchWeapon2.isKeyDown() && getMouseWheel() == 0) {
                if (this.KeySwWeaponMode.isKeyDown()) {
                    ac.switchCurrentWeaponMode(player);
                } else if (this.KeyUseWeapon.isKeyPress() && ac.useCurrentWeapon(player)) {
                    pc.setUseWeapon(true);
                    pc.useWeaponOption1 = ac.getCurrentWeapon(player).getLastUsedOptionParameter1();
                    pc.useWeaponOption2 = ac.getCurrentWeapon(player).getLastUsedOptionParameter2();
                    pc.useWeaponPosX = ac.prevPosX;
                    pc.useWeaponPosY = ac.prevPosY;
                    pc.useWeaponPosZ = ac.prevPosZ;
                    send = true;
                }
            } else {
                if (getMouseWheel() > 0) {
                    pc.switchWeapon = (byte) ac.getNextWeaponID(player, -1);
                } else {
                    pc.switchWeapon = (byte) ac.getNextWeaponID(player, 1);
                }

                setMouseWheel(0);
                ac.switchWeapon(player, pc.switchWeapon);
                send = true;
            }
        }

        return send || player.ticksExisted % 100 == 0;
    }
}
