package com.norwood.mcheli.aircraft;

import hohserg.elegant.networking.api.IByteBufSerializable;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.MathHelper;

/**
 * Data class for RWR threat events.
 * Ported from MCHeli Reforged.
 */
public class MCH_RWRThreatEvent implements IByteBufSerializable {
    public static final byte EMITTER_UNKNOWN = 0;
    public static final byte EMITTER_AIRCRAFT = 1;
    public static final byte EMITTER_MISSILE = 2;
    public static final byte EMITTER_SAM = 3;

    public static final byte MODE_SEARCH = 0;
    public static final byte MODE_STT = 1;      // Single Target Track (Lock-on)
    public static final byte MODE_MSL_DATALINK = 2;
    public static final byte MODE_MSL_ACTIVE = 3;

    public int emitterEntityId;
    public byte emitterKind;
    public byte threatMode;
    public String sourceName;
    public float angleDeg;
    public float strength;   // 0.0 to 1.0
    public float confidence; // 0.0 to 1.0

    public MCH_RWRThreatEvent() {
    }

    public MCH_RWRThreatEvent(int emitterId, byte emitterKind, byte threatMode, String sourceName, float angleDeg, float strength, float confidence) {
        this.emitterEntityId = emitterId;
        this.emitterKind = emitterKind;
        this.threatMode = threatMode;
        this.sourceName = sourceName != null ? sourceName : "?";
        this.angleDeg = angleDeg;
        this.strength = clamp01(strength);
        this.confidence = clamp01(confidence);
    }

    public MCH_RWRThreatEvent(ByteBuf buf) {
        this.emitterEntityId = buf.readInt();
        this.emitterKind = buf.readByte();
        this.threatMode = buf.readByte();
        this.sourceName = readString(buf);
        this.angleDeg = buf.readFloat();
        this.strength = buf.readFloat();
        this.confidence = buf.readFloat();
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(emitterEntityId);
        buf.writeByte(emitterKind);
        buf.writeByte(threatMode);
        writeString(buf, sourceName);
        buf.writeFloat(angleDeg);
        buf.writeFloat(strength);
        buf.writeFloat(confidence);
    }

    public static float clamp01(float f) {
        return MathHelper.clamp(f, 0.0F, 1.0F);
    }

    private static String readString(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes);
    }

    private static void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes();
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }
}
