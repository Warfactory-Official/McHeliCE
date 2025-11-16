package com.norwood.mcheli.wrapper;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class W_PacketBase implements IMessage {

    ByteArrayDataInput data;

    public byte[] createData() {
        return null;
    }

    public void fromBytes(ByteBuf buf) {
        byte[] dst = new byte[buf.array().length - 1];
        buf.getBytes(0, dst);
        this.data = ByteStreams.newDataInput(dst);
    }

    public void toBytes(ByteBuf buf) {
        buf.writeBytes(this.createData());
    }
}
