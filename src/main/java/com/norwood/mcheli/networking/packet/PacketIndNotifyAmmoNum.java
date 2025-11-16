package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketIndNotifyAmmoNum extends PacketBase implements ClientToServerPacket {

    public int entityID_Ac = -1;
    public byte weaponID = -1;

    public static void send(MCH_EntityAircraft ac, int wid) {
        var packet = new PacketIndNotifyAmmoNum();
        packet.entityID_Ac = W_Entity.getEntityId(ac);
        packet.weaponID = (byte) wid;
        packet.sendToServer();
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (this.entityID_Ac > 0) {
            Entity e = player.world.getEntityByID(this.entityID_Ac);
            if (e instanceof MCH_EntityAircraft) {
                if (this.weaponID >= 0) {
                    PacketNotifyAmmoNum.sendAmmoNum((MCH_EntityAircraft) e, player, this.weaponID);
                } else {
                    PacketNotifyAmmoNum.sendAllAmmoNum((MCH_EntityAircraft) e, player);
                }
            }
        }
    }
}
