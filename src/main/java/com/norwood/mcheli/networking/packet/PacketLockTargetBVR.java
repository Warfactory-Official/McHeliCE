package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponGuidanceSystem;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ClientToServerPacket;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
@AllArgsConstructor
public class PacketLockTargetBVR implements ClientToServerPacket {

    public final int targetId;

    @Override
    public void onReceive(EntityPlayerMP player) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (ac != null) {
            com.norwood.mcheli.MCH_MOD.rwrThreatManager.reportRadarTracking(player, ac.getEntityId(), targetId);
            MCH_WeaponSet ws = ac.getCurrentWeapon(player);
            if (ws != null && ws.getCurrentWeapon() != null) {
                MCH_WeaponGuidanceSystem gs = ws.getCurrentWeapon().getGuidanceSystem();
                if (gs != null) {
                    if (targetId <= 0) {
                        gs.setTarget(null);
                    } else {
                        Entity target = player.world.getEntityByID(targetId);
                        if (target != null) {
                            gs.setTarget(target);
                        }
                    }
                }
            }
        }
    }
}
