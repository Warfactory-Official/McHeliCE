package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.UAVTracker;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

import java.util.UUID;

/**
 * Client -> server: drop a UAV from its station's paired list, severing any live control link and
 * making the UAV pairable again with other stations.
 */
@ElegantPacket
public class PacketUavDrop extends PacketBase implements ClientToServerPacket {

    public int stationEntityId;
    public long uavIdMost;
    public long uavIdLeast;

    public PacketUavDrop() {
    }

    public PacketUavDrop(int stationEntityId, UUID uavId) {
        this.stationEntityId = stationEntityId;
        this.uavIdMost = uavId.getMostSignificantBits();
        this.uavIdLeast = uavId.getLeastSignificantBits();
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        UUID uavId = new UUID(this.uavIdMost, this.uavIdLeast);
        if (!(player.world.getEntityByID(this.stationEntityId) instanceof MCH_EntityUavStation station)) {
            return;
        }
        if (player.getDistanceSq(station) > 256.0) {
            return;
        }

        boolean removed = station.unpairUav(uavId);
        if (!removed) {
            return;
        }

        // Clear the UAV's owning-station marker so it can be paired again (load it if needed).
        MCH_EntityAircraft uav = UAVTracker.locateUAV(player.world, uavId);
        if (uav != null) {
            uav.setPairedStation(null);
        }
        player.sendMessage(new TextComponentString("Dropped UAV from station."));
        player.closeScreen();
    }
}
