package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketStatusRequest extends PacketBase implements ClientToServerPacket {

    public int entityID_AC = -1;

    public static void requestStatus(MCH_EntityAircraft ac) {
        if (ac.world.isRemote) {
            var packet = new PacketStatusRequest();
            packet.entityID_AC = W_Entity.getEntityId(ac);
            packet.sendToServer();
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (this.entityID_AC > 0) {
            Entity entity = player.world.getEntityByID(this.entityID_AC);
            if (entity instanceof MCH_EntityAircraft) {
                PacketStatusResponse.sendStatus((MCH_EntityAircraft) entity, player);
            }
        }
    }
}
