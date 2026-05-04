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
     * Chaff key
     */
    public MCH_Key KeyChaff;
    /**
     * Maintenance key
     */
    public MCH_Key KeyMaintenance;
    /**
     * APS key
     */
    public MCH_Key KeyAPS;
    /**
     * ECM Jammer key
     */
    public MCH_Key KeyECMJammer;
    /**
     * Radar switch key
     */
    public MCH_Key KeyRadarSwitch;
    public MCH_Key KeyAirburstDistReset;
    protected boolean isRiding = false;
    protected boolean isBeforeRiding = false;
    private long lastRwrLockSoundTick = 0;
    private long lastRwrScanSoundTick = 0;

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
        this.KeyUseWeapon = new MCH_Key(-100);
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
        this.KeyAPS = new MCH_Key(MCH_Config.KeyAPS.prmInt);
        this.KeyECMJammer = new MCH_Key(MCH_Config.KeyECMJammer.prmInt);
        this.KeyRadarSwitch = new MCH_Key(MCH_Config.KeyRadarSwitch.prmInt);
    }

    protected void commonPlayerControlInGUI(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot,
                                            PacketPlayerControlBase pc) {
    }

    public boolean commonPlayerControl(EntityPlayer player, MCH_EntityAircraft ac, boolean isPilot, DataPlayerControlAircraft pc) {
        updateRwr(ac);
        if (ac.supportsDetachedTurretAim() && ac.isDetachedWeaponAimActive()) {
            pc.detachedWeaponAim = true;
            pc.detachedWeaponAimYaw = ac.getDetachedWeaponAimYaw();
            pc.detachedWeaponAimPitch = ac.getDetachedWeaponAimPitch();
        }

        if (handleSeatControls()) return false;

        boolean send = false;

        send |= handleCameraAndModes(player, ac, pc);
        send |= handleFlightControls(player, ac, pc);
        send |= handleCombatSystems(player, ac, pc);

        return send;
    }

    private boolean handleCameraAndModes(EntityPlayer player, MCH_EntityAircraft ac, DataPlayerControlAircraft pc) {
        boolean send = false;
        if (KeyCameraMode.isKeyDown()) {
            pc.switchCameraMode = DataPlayerControlAircraft.CameraMode.values()[(ac.camera.getMode(0) + 1) % 3];
            send = true;
        }

        if (KeyFreeLook.isKeyDown()) {
            pc.switchFreeLook = (byte) (ac.isFreeLookMode() ? 2 : 1);
            send = true;
        }

        if (KeyGUI.isKeyDown()) {
            pc.setOpenGui(true);
            send = true;
        }

        if (KeyRadarSwitch.isKeyDown()) {
            if (ac.getAcInfo() != null && ac.getAcInfo().enableRadar) {
                boolean nextState = !ac.isRadarEnabledRuntime();
                pc.switchRadar = (byte) (nextState ? 1 : 2);
                send = true;

                if (nextState) {
                    com.norwood.mcheli.wrapper.W_McClient.playSound("radar_on", 1.0F, 1.0F);
                    player.sendMessage(new net.minecraft.util.text.TextComponentString("Radar [ON]"));
                } else {
                    com.norwood.mcheli.wrapper.W_McClient.playSound("radar_off", 1.0F, 1.0F);
                    player.sendMessage(new net.minecraft.util.text.TextComponentString("Radar [OFF]"));
                }
            }
        }
        return send;
        }


    private boolean handleFlightControls(EntityPlayer player, MCH_EntityAircraft ac, DataPlayerControlAircraft pc) {
        if (!ac.isPilot(player)) return false;

        boolean inputChanged = false;

        if (KeyUp.isKeyPress()) pc.setThrottleUp(true);
        if (KeyDown.isKeyPress()) pc.setThrottleDown(true);
        if (KeyRight.isKeyPress()) pc.setMoveRight(true);
        if (KeyLeft.isKeyPress()) pc.setMoveLeft(true);

        inputChanged |= Stream.of(KeyUp, KeyDown, KeyRight, KeyLeft).anyMatch(MCH_Key::isKeyDown);
        inputChanged |= Stream.of(KeyUp, KeyDown, KeyRight, KeyLeft).anyMatch(MCH_Key::isKeyUp);

        if (KeyBrake.isKeyDown()) {
            pc.switchMode = ac.isHoveringMode() ? DataPlayerControlAircraft.ModeSwitch.HOVERING_OFF : DataPlayerControlAircraft.ModeSwitch.HOVERING_ON;
            inputChanged = true;
        }

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

        if (KeyChaff.isKeyDown() && ac.getSeatIdByEntity(player) <= 1) {
            if (ac.canUseChaff() && ac.useChaff()) {
                pc.setUseChaff(true);
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (KeyAPS.isKeyDown() && ac.getSeatIdByEntity(player) <= 1) {
            if (ac.canUseAPS() && ac.useAPS()) {
                pc.setUseAPS(true);
                send = true;
            } else {
                playSoundNG();
            }
        }

        if (KeyECMJammer.isKeyDown() && ac.getSeatIdByEntity(player) <= 1) {
            if (ac.getAcInfo() != null && ac.getAcInfo().enableECMJammer && ac.useECMJammer()) {
                pc.setUseECMJammer(true);
                send = true;
            } else {
                playSoundNG();
            }
        }


        // Weapons
        if (KeyUseWeapon.isKeyPress()) pc.setUseWeapon(true);
        if (KeyReloadWeapon.isKeyDown()) pc.setReload(true);

        send |= KeyUseWeapon.isKeyDown() || KeyUseWeapon.isKeyUp();
        send |= KeyReloadWeapon.isKeyDown() || KeyReloadWeapon.isKeyUp();

        return send;
    }

    protected void updateRwr(MCH_EntityAircraft ac) {
        if (ac == null || ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) return;

        java.util.List<MCH_RWRThreatEvent> events = MCH_RWRThreatClientTracker.getEvents(ac.getEntityId());
        boolean beingLocked = false;
        boolean beingScanned = false;
        long now = ac.world.getTotalWorldTime();

        for (MCH_RWRThreatEvent evt : events) {
            if (evt.threatMode == MCH_RWRThreatEvent.MODE_STT || evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE) {
                beingLocked = true;
            } else if (evt.threatMode == MCH_RWRThreatEvent.MODE_SEARCH) {
                beingScanned = true;
            }
        }

        if (beingLocked) {
            if (now - lastRwrLockSoundTick >= 10) {
                com.norwood.mcheli.wrapper.W_McClient.playSound("locked", 1.0F, 1.0F);
                lastRwrLockSoundTick = now;
            }
        } else if (beingScanned) {
            if (now - lastRwrScanSoundTick >= 40) {
                com.norwood.mcheli.wrapper.W_McClient.playSound("alert_radar", 1.0F, 1.0F);
                lastRwrScanSoundTick = now;
            }
        }
    }

    protected boolean handleSeatControls() {
        return false;
    }
}
