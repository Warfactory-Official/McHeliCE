package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.client.GuiWingmanPlanner;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: data for the mission-planner GUI ({@code /wingman gui}).
 */
@ElegantPacket
public class PacketPlannerData implements ServerToClientPacket {

    public List<UavDto>    uavs    = new ArrayList<>();
    public List<RouteDto>  routes  = new ArrayList<>();
    public List<MarkerDto> markers = new ArrayList<>();
    public double playerX, playerY, playerZ;

    public static class UavDto implements IByteBufSerializable {
        public String uuid = "", name = "", state = "";
        public int nodeIdx, nodeCount;

        public UavDto() {}

        public UavDto(ByteBuf buf) {
            this.uuid = ByteBufUtils.readUTF8String(buf);
            this.name = ByteBufUtils.readUTF8String(buf);
            this.state = ByteBufUtils.readUTF8String(buf);
            this.nodeIdx = buf.readInt();
            this.nodeCount = buf.readInt();
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, uuid);
            ByteBufUtils.writeUTF8String(buf, name);
            ByteBufUtils.writeUTF8String(buf, state);
            buf.writeInt(nodeIdx);
            buf.writeInt(nodeCount);
        }
    }

    public static class RouteDto implements IByteBufSerializable {
        public String name = "";
        public List<String> nodes = new ArrayList<>();

        public RouteDto() {}

        public RouteDto(ByteBuf buf) {
            this.name = ByteBufUtils.readUTF8String(buf);
            int nc = buf.readInt();
            for (int i = 0; i < nc; i++) this.nodes.add(ByteBufUtils.readUTF8String(buf));
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, name);
            buf.writeInt(nodes.size());
            for (String n : nodes) ByteBufUtils.writeUTF8String(buf, n);
        }
    }

    public static class MarkerDto implements IByteBufSerializable {
        public String type = "", id = "";
        public int x, y, z;

        public MarkerDto() {}

        public MarkerDto(ByteBuf buf) {
            this.type = ByteBufUtils.readUTF8String(buf);
            this.id = ByteBufUtils.readUTF8String(buf);
            this.x = buf.readInt();
            this.y = buf.readInt();
            this.z = buf.readInt();
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, type);
            ByteBufUtils.writeUTF8String(buf, id);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
        }
    }

    public PacketPlannerData() {}

    @Override
    public void onReceive(Minecraft mc) {
        mc.addScheduledTask(() ->
            mc.displayGuiScreen(new GuiWingmanPlanner(uavs, routes, markers, playerX, playerY, playerZ)));
    }
}
