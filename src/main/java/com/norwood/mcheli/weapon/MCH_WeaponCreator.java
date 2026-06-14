package com.norwood.mcheli.weapon;

import com.norwood.mcheli.weapon.registry.WeaponTypeRegistry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

public class MCH_WeaponCreator {

    @Nullable
    public static MCH_WeaponBase createWeapon(World world, String weaponName, Vec3d vec, float yaw, float pitch,
                                              MCH_IEntityLockChecker lockChecker, boolean onTurret) {

        MCH_WeaponInfo info = MCH_WeaponInfoManager.get(weaponName);
        if (info == null || info.type.isEmpty()) {
            return null;
        }

        // Resolve factory and create instance
        WeaponTypeRegistry.WeaponFactory factory = WeaponTypeRegistry.WEAPON_TYPES.get(info.type);
        if (factory == null) {
            return null;
        }

        MCH_WeaponBase weapon = factory.create(world, vec, yaw, pitch, weaponName, info);

        if (weapon != null) {
            // Basic Parameter Assignment
            weapon.displayName = info.displayName;
            weapon.power = info.power;
            weapon.acceleration = info.acceleration;
            weapon.explosionPower = info.explosion;
            weapon.explosionPowerInWater = info.explosionInWater;
            weapon.interval = info.delay;
            weapon.delayedInterval = info.delay;
            weapon.numMode = info.modeNum;
            weapon.piercing = info.piercing;
            weapon.heatCount = info.heatCount;
            weapon.onTurret = onTurret;

            weapon.setLockCountMax(info.lockTime);
            weapon.setLockChecker(lockChecker);

            // Heat Logic Cleanup
            if (info.maxHeatCount > 0 && weapon.heatCount < 2) {
                weapon.heatCount = 2;
            }

            int adjustedInterval = info.delay;
            if (adjustedInterval < 4) {
                adjustedInterval++;
            } else if (adjustedInterval < 7) {
                adjustedInterval += 2;
            } else if (adjustedInterval < 10) {
                adjustedInterval += 3;
            } else if (adjustedInterval < 20) {
                adjustedInterval += 6;
            } else {
                adjustedInterval += 10;
                if (adjustedInterval >= 40) {
                    adjustedInterval = -adjustedInterval;
                }
            }

            // Apply Client-Side specific overrides
            if (world.isRemote) {
                weapon.interval = adjustedInterval;
                weapon.heatCount++;
                weapon.cartridge = info.cartridge;
            }

            // Always store the calculated delay for logical use
            weapon.delayedInterval = adjustedInterval;

            weapon.modifyCommonParameters();
        }

        return weapon;
    }
}
