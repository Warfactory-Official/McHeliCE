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
        if (info != null && !info.type.isEmpty()) {
            MCH_WeaponBase weapon = Optional.ofNullable(WeaponTypeRegistry.WEAPON_TYPES.get(info.type))
                    .map(f -> f.create(world, vec, yaw, pitch, weaponName, info))
                    .orElse(null);

            if (weapon != null) {
                weapon.displayName = info.displayName;
                weapon.power = info.power;
                weapon.acceleration = info.acceleration;
                weapon.explosionPower = info.explosion;
                weapon.explosionPowerInWater = info.explosionInWater;
                int interval = info.delay;
                weapon.interval = info.delay;
                weapon.delayedInterval = info.delay;
                weapon.setLockCountMax(info.lockTime);
                weapon.setLockChecker(lockChecker);
                weapon.numMode = info.modeNum;
                weapon.piercing = info.piercing;
                weapon.heatCount = info.heatCount;
                weapon.onTurret = onTurret;
                if (info.maxHeatCount > 0 && weapon.heatCount < 2) {
                    weapon.heatCount = 2;
                }

                if (interval < 4) {
                    interval++;
                } else if (interval < 7) {
                    interval += 2;
                } else if (interval < 10) {
                    interval += 3;
                } else if (interval < 20) {
                    interval += 6;
                } else {
                    interval += 10;
                    if (interval >= 40) {
                        interval = -interval;
                    }
                }

                weapon.delayedInterval = interval;
                if (world.isRemote) {
                    weapon.interval = interval;
                    weapon.heatCount++;
                    weapon.cartridge = info.cartridge;
                }

                weapon.modifyCommonParameters();
            }

            return weapon;
        } else {
            return null;
        }
    }
}
