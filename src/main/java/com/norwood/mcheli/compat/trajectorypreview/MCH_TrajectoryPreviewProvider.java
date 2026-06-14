package com.norwood.mcheli.compat.trajectorypreview;

import alexiy.projectile.preview.api.PreviewPlugin;
import alexiy.projectile.preview.api.PreviewProvider;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponCAS;
import com.norwood.mcheli.weapon.MCH_WeaponDummy;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.weapon.MCH_WeaponSmoke;
import com.norwood.mcheli.weapon.MCH_WeaponTargetingPod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;

@PreviewPlugin(mod = "mcheli")
@SuppressWarnings("unused")
public class MCH_TrajectoryPreviewProvider implements PreviewProvider {

    @Override
    public Class<? extends Entity> getPreviewEntityFor(EntityPlayer player, Item item) {
        MCH_EntityAircraft aircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (aircraft == null || aircraft.isDestroyed()) {
            return null;
        }

        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        MCH_WeaponBase weapon = weaponSet != null ? weaponSet.getCurrentWeapon() : null;
        return isPreviewable(weapon) ? MCH_WeaponTrajectoryPreviewEntity.class : null;
    }

    private boolean isPreviewable(MCH_WeaponBase weapon) {
        return weapon != null &&
                weapon.getInfo() != null &&
                !(weapon instanceof MCH_WeaponDummy) &&
                !(weapon instanceof MCH_WeaponSmoke) &&
                !(weapon instanceof MCH_WeaponTargetingPod) &&
                !(weapon instanceof MCH_WeaponCAS);
    }
}
