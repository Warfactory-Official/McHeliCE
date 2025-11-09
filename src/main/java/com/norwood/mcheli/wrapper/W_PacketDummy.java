package com.norwood.mcheli.wrapper;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

public class W_PacketDummy implements IMessage {

    public void fromBytes(ByteBuf buf) {}

    public void toBytes(ByteBuf buf) {}
}
