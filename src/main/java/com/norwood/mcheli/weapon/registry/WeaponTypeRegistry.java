package com.norwood.mcheli.weapon.registry;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.weapon.*;

public class WeaponTypeRegistry {

    public static final Map<String, WeaponFactory> WEAPON_TYPES = new HashMap<>();
    static {
        WEAPON_TYPES.put("machinegun1", MCH_WeaponMachineGun1::new);
        WEAPON_TYPES.put("machinegun2", MCH_WeaponMachineGun2::new);
        WEAPON_TYPES.put("tvmissile", MCH_WeaponTvMissile::new);
        WEAPON_TYPES.put("torpedo", MCH_WeaponTorpedo::new);
        WEAPON_TYPES.put("cas", MCH_WeaponCAS::new);
        WEAPON_TYPES.put("rocket", MCH_WeaponRocket::new);
        WEAPON_TYPES.put("asmissile", MCH_WeaponASMissile::new);
        WEAPON_TYPES.put("aamissile", MCH_WeaponAAMissile::new);
        WEAPON_TYPES.put("atmissile", MCH_WeaponATMissile::new);
        WEAPON_TYPES.put("bomb", MCH_WeaponBomb::new);
        WEAPON_TYPES.put("mkrocket", MCH_WeaponMarkerRocket::new);
        WEAPON_TYPES.put("dummy", MCH_WeaponDummy::new);
        WEAPON_TYPES.put("smoke", MCH_WeaponSmoke::new);
        WEAPON_TYPES.put("dispenser", MCH_WeaponDispenser::new);
        WEAPON_TYPES.put("targetingpod", MCH_WeaponTargetingPod::new);

    }

    @FunctionalInterface
    public interface WeaponFactory {

        MCH_WeaponBase create(World w, Vec3d v, float yaw, float pitch, String weaponName, MCH_WeaponInfo info);
    }
}
