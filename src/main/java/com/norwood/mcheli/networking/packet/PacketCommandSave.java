package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketCommandSave extends PacketBase implements ClientToServerPacket {

    public String str = "";

    public static void send(String cmd) {
        var packet = new PacketCommandSave();
        packet.str = cmd;
        packet.sendToServer();
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (player != null) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
            if (ac != null) {
                ac.setCommand(str, player);
            }
        }
    }
}
