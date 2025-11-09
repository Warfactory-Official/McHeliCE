package com.norwood.mcheli.networking.data;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataPlayerControlVehicle extends DataPlayerControlAircraft {

    public int unhitchChainId = -1;
    public BladeStatus bladeStatus = BladeStatus.NONE;

    @SuppressWarnings("unused")
    public DataPlayerControlVehicle(ByteBuf buf) {
        super(buf);
        this.unhitchChainId = buf.readInt();
        this.bladeStatus = BladeStatus.values()[buf.readByte()];
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf); // serialize all base class fields

        buf.writeInt(unhitchChainId);
        buf.writeByte(bladeStatus.ordinal());
    }

    public static enum BladeStatus {
        NONE,
        UNFOLD,
        FOLD
    }
}
