package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketRequestReload extends PacketBase implements ClientToServerPacket {

    public int entityID_Ac = -1;
    public int weaponID = -1;

    public static void send(MCH_EntityAircraft ac, int weaponId) {
        if (ac != null) {
            var packet = new PacketRequestReload();
            packet.entityID_Ac = W_Entity.getEntityId(ac);
            packet.weaponID = weaponId;
            packet.sendToServer();
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (!player.world.isRemote) {
            if (this.entityID_Ac > 0) {
                Entity e = player.world.getEntityByID(this.entityID_Ac);
                if (e instanceof MCH_EntityAircraft ac) {
                    MCH_Lib.DbgLog(e.world, "onPacketIndReload :%s", ac.getAcInfo().displayName);
                    ac.supplyAmmo(this.weaponID);
                }
            }
        }
    }
}
