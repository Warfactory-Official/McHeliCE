package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@ElegantPacket
public class PacketIronCurtainUse implements ServerToClientPacket {

    public final int acId;
    public final int time;

    public PacketIronCurtainUse(int acId, int time) {
        this.acId = acId;
        this.time = time;
    }

    @Override
    public void onReceive(Minecraft mc) {
        Entity e = mc.world.getEntityByID(acId);
        if (e instanceof MCH_EntityAircraft aircraft) {
            aircraft.weaponSystem.startIronCurtain(time);
        }
    }
}
