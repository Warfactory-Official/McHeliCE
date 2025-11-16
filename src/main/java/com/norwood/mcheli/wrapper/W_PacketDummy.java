package com.norwood.mcheli.wrapper;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class W_PacketDummy implements IMessage {

    public void fromBytes(ByteBuf buf) {}

    public void toBytes(ByteBuf buf) {}
}
