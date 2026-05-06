package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ClientToServerPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketAirburstDistReset implements ClientToServerPacket {

    public final int acId;
    public final int dist;

    public PacketAirburstDistReset(int acId, int dist) {
        this.acId = acId;
        this.dist = dist;
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        Entity e = player.world.getEntityByID(acId);
        if (e instanceof MCH_EntityAircraft aircraft) {
            MCH_WeaponSet ws = aircraft.getCurrentWeapon(player);
            if (ws != null && ws.getCurrentWeapon() != null) {
                MCH_WeaponBase wb = ws.getCurrentWeapon();
                wb.airburstDist = dist;
            }
        }
    }
}
