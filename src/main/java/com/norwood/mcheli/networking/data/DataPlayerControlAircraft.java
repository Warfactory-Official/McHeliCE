package com.norwood.mcheli.networking.data;

import hohserg.elegant.networking.api.IByteBufSerializable;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class DataPlayerControlAircraft implements IByteBufSerializable {

    public UnmountAction isUnmount = UnmountAction.NONE;
    public VtolSwitch switchVtol = VtolSwitch.NONE;
    public ModeSwitch switchMode = ModeSwitch.NONE;
    public HatchSwitch switchHatch = HatchSwitch.NONE;
    public GearSwitch switchGear = GearSwitch.NONE;
    public RackAction putDownRack = RackAction.NONE;
    public CameraMode switchCameraMode = CameraMode.NORMAL;
    @Delegate
    public PlayerControlSwitches switches;
    public byte switchWeapon = -1;
    public byte useFlareType = 0;
    // TODO:Probably safe to squash to a short frankly
    public int useWeaponOption1 = 0;
    public int useWeaponOption2 = 0;
    public double useWeaponPosX = 0.0;
    public double useWeaponPosY = 0.0;
    public double useWeaponPosZ = 0.0;
    public byte switchFreeLook = 0;

    public DataPlayerControlAircraft() {
        switches = new PlayerControlSwitches();
    }

    public void serialize(ByteBuf buf) {
        // Serialize enums as bytes
        buf.writeByte(isUnmount.ordinal());
        buf.writeByte(switchVtol.ordinal());
        buf.writeByte(switchMode.ordinal());
        buf.writeByte(switchHatch.ordinal());
        buf.writeByte(switchGear.ordinal());
        buf.writeByte(putDownRack.ordinal());
        buf.writeByte(switchCameraMode.ordinal());

        switches.serialize(buf);

        buf.writeByte(switchWeapon);
        buf.writeByte(useFlareType);

        buf.writeInt(useWeaponOption1);
        buf.writeInt(useWeaponOption2);

        buf.writeDouble(useWeaponPosX);
        buf.writeDouble(useWeaponPosY);
        buf.writeDouble(useWeaponPosZ);

        buf.writeByte(switchFreeLook);
    }

    @SuppressWarnings("unused")
    public DataPlayerControlAircraft(ByteBuf buf) {
        this.isUnmount = UnmountAction.values()[buf.readByte()];
        this.switchVtol = VtolSwitch.values()[buf.readByte()];
        this.switchMode = ModeSwitch.values()[buf.readByte()];
        this.switchHatch = HatchSwitch.values()[buf.readByte()];
        this.switchGear = GearSwitch.values()[buf.readByte()];
        this.putDownRack = RackAction.values()[buf.readByte()];
        this.switchCameraMode = CameraMode.values()[buf.readByte()];

        this.switches = new PlayerControlSwitches(buf);

        this.switchWeapon = buf.readByte();
        this.useFlareType = buf.readByte();

        this.useWeaponOption1 = buf.readInt();
        this.useWeaponOption2 = buf.readInt();

        this.useWeaponPosX = buf.readDouble();
        this.useWeaponPosY = buf.readDouble();
        this.useWeaponPosZ = buf.readDouble();

        this.switchFreeLook = buf.readByte();
    }

    public static enum UnmountAction {
        NONE,           // 0
        UNMOUNT_SELF,   // 1
        UNMOUNT_CREW,   // 2
        UNMOUNT_AIRCRAFT // 3
    }

    public static enum VtolSwitch {
        NONE,   // 0
        VTOL_OFF, // 1
        VTOL_ON   // 2
    }

    public static enum ModeSwitch {
        NONE,            // 0
        GUNNER_OFF,      // 1
        GUNNER_ON,       // 2
        HOVERING_OFF,    // 3
        HOVERING_ON      // 4
    }

    public static enum HatchSwitch {
        NONE,     // 0
        FOLD,     // 1
        UNFOLD    // 2
    }

    public static enum RackAction {
        NONE,       // 0
        MOUNT,      // 1
        UNMOUNT,    // 2
        RIDE        // 3
    }

    public static enum GearSwitch {
        NONE,   // 0
        FOLD,   // 1
        UNFOLD  // 2
    }

    public static enum CameraMode {
        NORMAL, // 0
        NIGHT_VIS, // 1
        THERMAL_VIS // 2
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class PlayerControlSwitches implements IByteBufSerializable {

        public boolean ejectSeat = false;
        public boolean useWeapon = false;
        public boolean switchSearchLight = false;
        public boolean useBrake = false;
        public boolean switchGunnerStatus = false;
        public boolean throttleUp = false;
        public boolean throttleDown = false;
        public boolean moveLeft = false;
        public boolean moveRight = false;
        public boolean openGui = false;

        @SuppressWarnings("unused")
        public PlayerControlSwitches(ByteBuf buf) {
            short mask = buf.readShort();
            ejectSeat = (mask & (1 << 0)) != 0;
            useWeapon = (mask & (1 << 1)) != 0;
            switchSearchLight = (mask & (1 << 2)) != 0;
            useBrake = (mask & (1 << 3)) != 0;
            switchGunnerStatus = (mask & (1 << 4)) != 0;
            throttleUp = (mask & (1 << 5)) != 0;
            throttleDown = (mask & (1 << 6)) != 0;
            moveLeft = (mask & (1 << 7)) != 0;
            moveRight = (mask & (1 << 8)) != 0;
            openGui = (mask & (1 << 9)) != 0;
        }

        public void serialize(ByteBuf acc) {
            short mask = 0;
            mask |= (ejectSeat ? 1 << 0 : 0);
            mask |= (useWeapon ? 1 << 1 : 0);
            mask |= (switchSearchLight ? 1 << 2 : 0);
            mask |= (useBrake ? 1 << 3 : 0);
            mask |= (switchGunnerStatus ? 1 << 4 : 0);
            mask |= (throttleUp ? 1 << 5 : 0);
            mask |= (throttleDown ? 1 << 6 : 0);
            mask |= (moveLeft ? 1 << 7 : 0);
            mask |= (moveRight ? 1 << 8 : 0);
            mask |= (openGui ? 1 << 9 : 0);
            acc.writeShort(mask);
        }
    }
}
