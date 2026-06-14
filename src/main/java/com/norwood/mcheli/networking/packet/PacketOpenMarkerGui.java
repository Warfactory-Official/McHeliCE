package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.block.MarkerType;
import com.norwood.mcheli.wingman.client.GuiMarkerConfig;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/**
 * Server → Client: open the marker-block configuration GUI.
 */
@ElegantPacket
public class PacketOpenMarkerGui implements ServerToClientPacket {

    public int x, y, z;
    public String typeName = "";
    public String id = "";
    public String baseId = "";
    public int    parkingHeading = -1;

    public PacketOpenMarkerGui() {}

    public PacketOpenMarkerGui(BlockPos pos, MarkerType type, String id, String baseId, int parkingHeading) {
        this.x              = pos.getX();
        this.y              = pos.getY();
        this.z              = pos.getZ();
        this.typeName       = type.name();
        this.id             = id;
        this.baseId         = baseId;
        this.parkingHeading = parkingHeading;
    }

    @Override
    public void onReceive(Minecraft mc) {
        mc.addScheduledTask(() -> {
            MarkerType type;
            try {
                type = MarkerType.valueOf(this.typeName);
            } catch (Exception e) {
                type = MarkerType.PARKING;
            }
            BlockPos pos = new BlockPos(this.x, this.y, this.z);
            mc.displayGuiScreen(new GuiMarkerConfig(pos, type, this.id, this.baseId, this.parkingHeading));
        });
    }
}
