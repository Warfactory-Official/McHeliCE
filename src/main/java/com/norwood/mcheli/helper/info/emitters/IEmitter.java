package com.norwood.mcheli.helper.info.emitters;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;

import java.io.IOException;
import java.nio.file.Path;

public interface IEmitter {
    public class EmissionException extends Exception {
        private final MCH_BaseInfo info;

        public EmissionException(String message, MCH_BaseInfo info) {
            super(message);
            this.info = info;
        }

        public EmissionException(String message, MCH_BaseInfo info, Throwable cause) {
            super(message, cause);
            this.info = info;
        }

        public MCH_BaseInfo getInfo() {
            return info;
        }
    }

    String emitHelicopter(MCH_HeliInfo info) throws EmissionException;
    String emitPlane(MCH_PlaneInfo info) throws EmissionException;
    String emitShip(MCH_ShipInfo info) throws EmissionException;
    String emitTank(MCH_TankInfo info) throws EmissionException;
    String emitVehicle(MCH_VehicleInfo info) throws EmissionException;
    String emitWeapon(MCH_WeaponInfo info) throws EmissionException;
    String emitThrowable(MCH_ThrowableInfo info) throws EmissionException;
    String emitHud(MCH_Hud hud) throws EmissionException;
    String emitItem(MCH_ItemInfo info) throws EmissionException;

    default void writeHelicopter(MCH_HeliInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitHelicopter(info));
    }

    default void writePlane(MCH_PlaneInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitPlane(info));
    }

    default void writeShip(MCH_ShipInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitShip(info));
    }

    default void writeTank(MCH_TankInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitTank(info));
    }

    default void writeVehicle(MCH_VehicleInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitVehicle(info));
    }

    default void writeWeapon(MCH_WeaponInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitWeapon(info));
    }

    default void writeThrowable(MCH_ThrowableInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitThrowable(info));
    }

    default void writeHud(MCH_Hud hud, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitHud(hud));
    }

    default void writeItem(MCH_ItemInfo info, Path out) throws IOException, EmissionException {
        YamlEmitter.writeTo(out, emitItem(info));
    }
}
