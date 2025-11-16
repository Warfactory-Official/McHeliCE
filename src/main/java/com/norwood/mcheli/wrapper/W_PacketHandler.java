package com.norwood.mcheli.wrapper;

import com.google.common.io.ByteArrayDataInput;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class W_PacketHandler implements IPacketHandler {

    public void onPacket(ByteArrayDataInput data, EntityPlayer player, MessageContext ctx) {}
}
