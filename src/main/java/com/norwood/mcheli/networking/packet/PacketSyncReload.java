package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.MCH_Logger;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@ElegantPacket
public class PacketSyncReload implements ServerToClientPacket {
    public final int acID;

    public PacketSyncReload(int acID) {
        this.acID = acID;
    }

    @Override
    public void onReceive(Minecraft mc) {
        Entity e = mc.player.world.getEntityByID(this.acID);
        if (e instanceof MCH_EntityAircraft ac) {
            MCH_Logger.debugLog(e.world, "onPacketSyncReload :%s", ac.getAcInfo().displayName);
            ac.manualReloadForPlayer(mc.player);
        }
    }
}
