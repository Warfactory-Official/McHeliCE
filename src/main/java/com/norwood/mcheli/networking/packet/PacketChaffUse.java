package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@ElegantPacket
public class PacketChaffUse implements ServerToClientPacket {


    final public int acId;
    final public int time;

    public PacketChaffUse(int acId, int time) {
        this.acId = acId;
        this.time = time;
    }

    @Override
    public void onReceive(Minecraft mc) {
        Entity e = mc.player.world.getEntityByID(acId);
        if(e instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft) e).setChaffUseTime(time);
        }
    }
}
