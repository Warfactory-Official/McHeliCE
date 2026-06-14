package com.norwood.mcheli.networking.packet;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.client.GuiBaseConfig;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: open the base-configuration GUI (BASE marker right-click).
 */
@ElegantPacket
public class PacketOpenBaseGui implements ServerToClientPacket {

    public int bx, by, bz;
    public String baseId = "";
    public String runwayAId = "";
    public String runwayBId = "";
    public List<RouteDto> routes = new ArrayList<>();
    public List<MarkerDto> parkingMarkers  = new ArrayList<>();
    public List<MarkerDto> waypointMarkers = new ArrayList<>();
    public List<MarkerDto>   runwayAMarkers  = new ArrayList<>();
    public List<MarkerDto>   runwayBMarkers  = new ArrayList<>();
    public List<AircraftDto> nearbyAircraft  = new ArrayList<>();
    public List<MarkerDto>   helipads         = new ArrayList<>();
    public List<MarkerDto>   helipadBMarkers  = new ArrayList<>();

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public static class RouteDto implements IByteBufSerializable {
        public String routeId = "", parkingId = "", runwayId = "", runwayBId = "";
        public List<String> waypointIds = new ArrayList<>();
        public List<String> arrivalWaypointIds = new ArrayList<>();
        public String arrivalRunwayId = "";
        public int parkingHeading = -1;

        public RouteDto() {}

        public RouteDto(ByteBuf buf) {
            this.routeId = ByteBufUtils.readUTF8String(buf);
            this.parkingId = ByteBufUtils.readUTF8String(buf);
            this.runwayId = ByteBufUtils.readUTF8String(buf);
            this.runwayBId = ByteBufUtils.readUTF8String(buf);
            int wn = buf.readInt();
            for (int i = 0; i < wn; i++) this.waypointIds.add(ByteBufUtils.readUTF8String(buf));
            this.parkingHeading = buf.readInt();
            int an = buf.readInt();
            for (int i = 0; i < an; i++) this.arrivalWaypointIds.add(ByteBufUtils.readUTF8String(buf));
            this.arrivalRunwayId = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, routeId);
            ByteBufUtils.writeUTF8String(buf, parkingId);
            ByteBufUtils.writeUTF8String(buf, runwayId);
            ByteBufUtils.writeUTF8String(buf, runwayBId);
            buf.writeInt(waypointIds.size());
            for (String wp : waypointIds) ByteBufUtils.writeUTF8String(buf, wp);
            buf.writeInt(parkingHeading);
            buf.writeInt(arrivalWaypointIds.size());
            for (String wp : arrivalWaypointIds) ByteBufUtils.writeUTF8String(buf, wp);
            ByteBufUtils.writeUTF8String(buf, arrivalRunwayId);
        }
    }

    public static class MarkerDto implements IByteBufSerializable {
        public String id = "";
        public int x, y, z;

        public MarkerDto() {}

        public MarkerDto(ByteBuf buf) {
            this.id = ByteBufUtils.readUTF8String(buf);
            this.x = buf.readInt();
            this.y = buf.readInt();
            this.z = buf.readInt();
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, id);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
        }
    }

    public static class AircraftDto implements IByteBufSerializable {
        public String uuid = "", name = "";

        public AircraftDto() {}

        public AircraftDto(ByteBuf buf) {
            this.uuid = ByteBufUtils.readUTF8String(buf);
            this.name = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, uuid);
            ByteBufUtils.writeUTF8String(buf, name);
        }
    }

    // ─── Constructors ─────────────────────────────────────────────────────────

    public PacketOpenBaseGui() {}

    public PacketOpenBaseGui(BlockPos pos, String baseId) {
        this.bx = pos.getX();
        this.by = pos.getY();
        this.bz = pos.getZ();
        this.baseId = baseId;
    }

    @Override
    public void onReceive(Minecraft mc) {
        mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiBaseConfig(this)));
    }
}
