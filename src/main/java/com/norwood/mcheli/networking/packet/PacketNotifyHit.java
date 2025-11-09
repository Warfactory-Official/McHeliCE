package com.norwood.mcheli.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

@ElegantPacket
public class PacketNotifyHit extends PacketBase implements ServerToClientPacket {

    public int entityID_Ac = -1;

    public static void send(MCH_EntityAircraft ac, EntityPlayerMP rider) {
        if (rider != null && !rider.isDead) {
            PacketNotifyHit packet = new PacketNotifyHit();
            packet.entityID_Ac = ac != null && !ac.isDead ? W_Entity.getEntityId(ac) : -1;
            packet.sendToPlayer(rider);
        }
    }

    @Override
    public void onReceive(Minecraft mc) {
        if (this.entityID_Ac <= 0) {
            MCH_MOD.proxy.hitBullet();
        } else {
            Entity e = mc.player.world.getEntityByID(this.entityID_Ac);
            if (e instanceof MCH_EntityAircraft) {
                ((MCH_EntityAircraft) e).hitBullet();
            }
        }
    }
}
