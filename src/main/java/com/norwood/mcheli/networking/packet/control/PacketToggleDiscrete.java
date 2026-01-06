package com.norwood.mcheli.networking.packet.control;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketToggleDiscrete implements ClientToServerPacket {
    @Override
    public void onReceive(EntityPlayerMP player) {

    }

}
