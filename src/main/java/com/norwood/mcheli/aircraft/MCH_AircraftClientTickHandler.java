package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.event.MCH_ClientTickHandlerBase;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.networking.packet.PacketSeatPlayerControl;
import com.norwood.mcheli.networking.packet.control.PacketPlayerControlBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

import java.util.Objects;
import java.util.stream.Stream;

import static com.norwood.mcheli.networking.packet.PacketSeatPlayerControl.PlayerControlState;

public abstract class MCH_AircraftClientTickHandler extends MCH_ClientTickHandlerBase {

    public MCH_Key KeyUp;
    public MCH_Key KeyDown;
    public MCH_Key KeyRight;
    public MCH_Key KeyLeft;
    public MCH_Key KeyUseWeapon;
    public MCH_Key KeyReloadWeapon;
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
    /**
     * 箔条按键
     */
    public MCH_Key KeyChaff;
    /**
     * 维修按键
     */
    public MCH_Key KeyMaintenance;
    /**
     * APS按键
     */
    public MCH_Key KeyAPS;
    public MCH_Key KeyAirburstDistReset;
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
        this.KeyReloadWeapon = new MCH_Key(MCH_Config.KeyReloadWeapon.prmInt);
        this.KeyChaff = new MCH_Key(MCH_Config.KeyChaff.prmInt);
    }

    protected void commonPlayerControlInGUI(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot,
                                            PacketPlayerControlBase pc) {
    }

    public boolean commonPlayerControl(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot, DataPlayerControlAircraft pc) {
        if (ac.supportsDetachedTurretAim() && ac.isDetachedWeaponAimActive()) {
            pc.detachedWeaponAim = true;
            pc.detachedWeaponAimYaw = ac.getDetachedWeaponAimYaw();
            pc.detachedWeaponAimPitch = ac.getDetachedWeaponAimPitch();
        }

        if (handleSeatControls()) return false;

        boolean send = false;

        send |= handleCameraAndModes(player, ac, pc);

        if (isPilot) {
            send |= handlePilotActions(ac, pc);
            send |= handleMovementAndBraking(ac, pc);
        }

        send |= handleCombatSystems(player, ac, pc);

        return send || player.ticksExisted % 100 == 0;
    }

    private boolean handleSeatControls() {
        boolean isFreeLook = Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt);

        // Directly evaluate the direction based on boolean logic
        PlayerControlState switchDir = (isFreeLook && KeyGUI.isKeyDown()) ? PlayerControlState.NEXT :
                (isFreeLook && KeyExtra.isKeyDown()) ? PlayerControlState.PREV :
                null;

        if (switchDir != null) {
            var packet = new PacketSeatPlayerControl();
            packet.setSwitchSeat(switchDir);
            packet.sendToServer();
            return true;
        }
        return false;
    }

    private boolean handleCameraAndModes(EntityPlayer player, MCH_EntityAircraft ac, DataPlayerControlAircraft pc) {
        if (!KeyCameraMode.isKeyDown()) return false;

        if (Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
            pc.setSwitchGunnerStatus(true);
            playSoundOK();
            return true;
        }

        if (ac.haveSearchLight() && ac.canSwitchSearchLight(player)) {
            pc.setSwitchSearchLight(true);
            playSoundOK();
            return true;
        }

        int beforeMode = ac.getCameraMode(player);
        ac.switchCameraMode(player);
        int mode = ac.getCameraMode(player);

        if (mode != beforeMode) {
            pc.switchCameraMode = DataPlayerControlAircraft.CameraMode.values()[mode];
            playSoundOK();
            return true;
        }

        playSoundNG();
        return false;
    }

    private boolean handlePilotActions(MCH_EntityAircraft ac, DataPlayerControlAircraft pc) {
        boolean send = false;

        // Unmount logic
        if (KeyUnmount.isKeyDown()) {
            pc.isUnmount = DataPlayerControlAircraft.UnmountAction.UNMOUNT_SELF;
            send = true;
        }

        // Rack Logic
        if (KeyPutToRack.isKeyDown()) {
            ac.checkRideRack();
            if (ac.canRideRack()) pc.putDownRack = DataPlayerControlAircraft.RackAction.RIDE;
            else if (ac.canPutToRack()) pc.putDownRack = DataPlayerControlAircraft.RackAction.MOUNT;
            send = true;
        } else if (KeyDownFromRack.isKeyDown()) {
            if (ac.getRidingEntity() != null) pc.isUnmount = DataPlayerControlAircraft.UnmountAction.UNMOUNT_AIRCRAFT;
            else if (ac.canDownFromRack()) pc.putDownRack = DataPlayerControlAircraft.RackAction.UNMOUNT;
            send = true;
        }

        // Gear and FreeLook
        if (KeyGearUpDown.isKeyDown() && Objects.requireNonNull(ac.getAcInfo()).haveLandingGear()) {
            if (ac.canFoldLandingGear()) pc.switchGear = DataPlayerControlAircraft.GearSwitch.FOLD;
            else if (ac.canUnfoldLandingGear()) pc.switchGear = DataPlayerControlAircraft.GearSwitch.UNFOLD;
            send = true;
        }

        if (KeyFreeLook.isKeyDown() && ac.canSwitchFreeLook()) {
            pc.switchFreeLook = (byte) (ac.isFreeLookMode() ? 2 : 1);
            send = true;
        }

        if (KeyGUI.isKeyDown()) {
            pc.setOpenGui(true);
            send = true;
        }

        return send;
    }

    private boolean handleMovementAndBraking(MCH_EntityAircraft ac, DataPlayerControlAircraft pc) {
        if (ac.isRepelling()) {
            pc.setThrottleDown(ac.throttleDown = false);
            pc.setThrottleUp(ac.throttleUp = false);
            pc.setMoveRight(ac.moveRight = false);
            pc.setMoveLeft(ac.moveLeft = false);
            return false;
        }

        if (ac.hasBrake() && KeyBrake.isKeyPress()) {
            pc.setThrottleDown(ac.throttleDown = false);
            pc.setThrottleUp(ac.throttleUp = false);

            double distSq = ac.getDistanceSq(ac.prevPosX, ac.posY, ac.prevPosZ);
            if (ac.getCurrentThrottle() <= 0.03 && distSq < 0.01) {
                pc.setMoveRight(ac.moveRight = false);
                pc.setMoveLeft(ac.moveLeft = false);
            }
            pc.setUseBrake(true);
            return KeyBrake.isKeyDown();
        }

        boolean inputChanged = Stream.of(KeyUp, KeyDown, KeyRight, KeyLeft)
                .anyMatch(k -> k.isKeyDown() || k.isKeyUp());

        pc.setThrottleDown(ac.throttleDown = KeyDown.isKeyPress());
        pc.setThrottleUp(ac.throttleUp = KeyUp.isKeyPress());
        pc.setMoveRight(ac.moveRight = KeyRight.isKeyPress());
        pc.setMoveLeft(ac.moveLeft = KeyLeft.isKeyPress());

        return inputChanged || KeyBrake.isKeyUp();
    }

    private boolean handleCombatSystems(EntityPlayer player, MCH_EntityAircraft ac, DataPlayerControlAircraft pc) {
        if (ac.isDestroyed()) return false;
        boolean send = false;

        // Flares
        if (KeyFlare.isKeyDown() && ac.getSeatIdByEntity(player) <= 1) {
            if (ac.canUseFlare() && ac.useFlare(ac.getCurrentFlareType())) {
                pc.useFlareType = (byte) ac.getCurrentFlareType();
                ac.nextFlareType();
                send = true;
            } else {
                playSoundNG();
            }
        }
        if (ac.getSeatIdByEntity(player) <= 1) {
            if (ac.canUseChaff() && ac.useChaff()) {
                pc.setUseChaff(true);
                send = true;
            } else {
                playSoundNG();
            }
        }

        // Weapons
        if (!ac.isPilotReloading()) {
            if (KeyReloadWeapon.isKeyDown()) {
                pc.setReload(true);
                send = true;
            }

            int wheel = getMouseWheel();
            if (wheel == 0 && !KeySwitchWeapon1.isKeyDown() && !KeySwitchWeapon2.isKeyDown()) {
                if (KeySwWeaponMode.isKeyDown()) {
                    ac.switchCurrentWeaponMode(player);
                } else if (KeyUseWeapon.isKeyPress() && ac.prepareCurrentWeapon(player)) {
                    var weapon = ac.getCurrentWeapon(player);
                    pc.setUseWeapon(true);
                    pc.useWeaponOption1 = weapon.getOptionParm1();
                    pc.useWeaponOption2 = weapon.getOptionParm2();
                    pc.useWeaponPosX = ac.prevPosX;
                    pc.useWeaponPosY = ac.prevPosY;
                    pc.useWeaponPosZ = ac.prevPosZ;
                    send = true;
                }
            } else if (wheel != 0 || KeySwitchWeapon1.isKeyDown() || KeySwitchWeapon2.isKeyDown()) {
                pc.switchWeapon = (byte) ac.getNextWeaponID(player, wheel > 0 ? -1 : 1);
                setMouseWheel(0);
                ac.switchWeapon(player, pc.switchWeapon);
                send = true;
            }
        }
        return send;
    }
}
