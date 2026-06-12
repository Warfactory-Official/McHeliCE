package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.MCH_Logger;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@ElegantPacket
@RequiredArgsConstructor
public class PacketSyncReload implements ServerToClientPacket {
    public final int acID;

    @Override
    public void onReceive(Minecraft mc) {
        Entity e = mc.player.world.getEntityByID(this.acID);
        if (e instanceof MCH_EntityAircraft ac) {
            MCH_Logger.debugLog(e.world, "onPacketSyncReload :%s", ac.getAcInfo().displayName);
            ac.manualReloadForPlayer(mc.player);
        }
    }
}
