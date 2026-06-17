package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.MCH_UavControl;
import com.norwood.mcheli.uav.UAVTracker;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

import java.util.UUID;

/**
 * Client -> server: connect to a paired UAV from its station and take control of it. The UAV is
 * located (loading a 3x3 chunk area around its last-known position if it is unloaded, via
 * {@link UAVTracker#locateUAV}); if it cannot be found the request is rejected.
 */
@ElegantPacket
public class PacketUavConnect extends PacketBase implements ClientToServerPacket {

    public int stationEntityId;
    public long uavIdMost;
    public long uavIdLeast;

    public PacketUavConnect() {
    }

    public PacketUavConnect(int stationEntityId, UUID uavId) {
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
        //TODO: Abstract further
        if (!station.isPaired(uavId)) {
            return;
        }

        MCH_EntityAircraft uav = UAVTracker.locateUAV(player.world, uavId);

        // Establish the link through the public facade — it mounts the operator and wires the
        // station<->UAV link; tracking and chunk streaming then engage automatically.
        switch (MCH_UavControl.connect(player, station, uav)) {
            case DESTROYED -> player.sendMessage(new TextComponentString("UAV is destroyed and cannot be connected to."));
            case NOT_FOUND -> player.sendMessage(new TextComponentString("UAV is out of range or could not be located."));
            case OK -> player.closeScreen();
        }
    }
}
