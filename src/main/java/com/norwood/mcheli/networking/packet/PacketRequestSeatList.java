package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketRequestSeatList extends PacketBase implements ClientToServerPacket {

    public int entityID_AC = -1;

    public static void requestSeatList(MCH_EntityAircraft ac) {
        var packet = new PacketRequestSeatList();
        packet.entityID_AC = W_Entity.getEntityId(ac);
        packet.sendToServer();
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (!player.world.isRemote) {
            if (this.entityID_AC > 0) {
                Entity e = player.world.getEntityByID(this.entityID_AC);
                if (e instanceof MCH_EntityAircraft) {
                    PacketSeatList.sendSeatList((MCH_EntityAircraft) e, player);
                }
            }
        }
    }
}
