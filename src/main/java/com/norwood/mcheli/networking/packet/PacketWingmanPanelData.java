package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.client.GuiWingmanPanel;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: data needed to open the wingman panel GUI.
 */
@ElegantPacket
public class PacketWingmanPanelData implements ServerToClientPacket {

    // ─── DTOs (self-serializing for the elegant networking codegen) ──────────

    public static class AircraftDto implements IByteBufSerializable {
        public String uuid = "";
        public String name = "";

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

    public static class WingmanDto implements IByteBufSerializable {
        public String uuid       = "";
        public String name       = "";
        public int    slot       = 0;
        public String state      = "";
        public int    attackMode = 0;   // WingmanEntry.ATK_NONE/AUTO/MANUAL
        public String weaponType = "";  // "" = any

        public WingmanDto() {}

        public WingmanDto(ByteBuf buf) {
            this.uuid       = ByteBufUtils.readUTF8String(buf);
            this.name       = ByteBufUtils.readUTF8String(buf);
            this.slot       = buf.readInt();
            this.state      = ByteBufUtils.readUTF8String(buf);
            this.attackMode = buf.readInt();
            this.weaponType = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void serialize(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, uuid);
            ByteBufUtils.writeUTF8String(buf, name);
            buf.writeInt(slot);
            ByteBufUtils.writeUTF8String(buf, state);
            buf.writeInt(attackMode);
            ByteBufUtils.writeUTF8String(buf, weaponType);
        }
    }

    // ─── Payload ─────────────────────────────────────────────────────────────

    public List<AircraftDto> nearby  = new ArrayList<>();
    public List<WingmanDto>  wingmen = new ArrayList<>();

    public double sideDist  = 20.0;
    public double altOffset = 0.0;
    public double rearDist  = 30.0;
    public int    maxWings  = 4;
    public double minAlt    = 0.0;
    public double maxAlt    = 0.0;

    public PacketWingmanPanelData() {}

    @Override
    public void onReceive(Minecraft mc) {
        mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiWingmanPanel(this)));
    }
}
