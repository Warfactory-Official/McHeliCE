package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_RWRThreatClientTracker;
import com.norwood.mcheli.aircraft.MCH_RWRThreatTable;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;

@ElegantPacket
@AllArgsConstructor
public class PacketRWRThreatSync implements ServerToClientPacket {

    public final MCH_RWRThreatTable table;

    @Override
    public void onReceive(Minecraft mc) {
        MCH_RWRThreatClientTracker.updateTable(table);
    }
}
