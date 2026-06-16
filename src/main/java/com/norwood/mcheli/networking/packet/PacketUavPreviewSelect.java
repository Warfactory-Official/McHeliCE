package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wingman.handler.UavChunkStreamer;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

/**
 * Client -> server heartbeat telling the server which paired UAV the player is previewing in the
 * open station screen, so {@link UavChunkStreamer} can stream that UAV's terrain to the client for
 * the live camera-feed viewport. Re-sent periodically while the screen is open; the server expires
 * the target shortly after the heartbeats stop (screen closed).
 */
@ElegantPacket
public class PacketUavPreviewSelect extends PacketBase implements ClientToServerPacket {

    public boolean active;
    public long uavIdMost;
    public long uavIdLeast;

    public PacketUavPreviewSelect() {
    }

    public PacketUavPreviewSelect(boolean active, UUID uavId) {
        this.active = active;
        if (uavId != null) {
            this.uavIdMost = uavId.getMostSignificantBits();
            this.uavIdLeast = uavId.getLeastSignificantBits();
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (!this.active) {
            UavChunkStreamer.clearPreview(player.getUniqueID());
            return;
        }
        UUID uavId = new UUID(this.uavIdMost, this.uavIdLeast);
        // Only stream for a UAV actually paired to the station the player currently occupies — this
        // prevents a client from forcing arbitrary chunks to load.
        if (player.getRidingEntity() instanceof MCH_EntityUavStation station && station.isPaired(uavId)) {
            UavChunkStreamer.setPreview(player.getUniqueID(), uavId, player.world.getTotalWorldTime());
        } else {
            UavChunkStreamer.clearPreview(player.getUniqueID());
        }
    }
}
