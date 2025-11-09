package com.norwood.mcheli.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import com.norwood.mcheli.helper.network.MCH_WrapPacketData;

public class W_Network {

    static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("MCHeli_CH");

    public static void sendToServer(W_PacketBase pkt) {
        INSTANCE.sendToServer(new MCH_WrapPacketData(pkt));
    }

    public static void sendToPlayer(W_PacketBase pkt, EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            INSTANCE.sendTo(new MCH_WrapPacketData(pkt), (EntityPlayerMP) player);
        }
    }

    public static void sendToAllAround(W_PacketBase pkt, Entity sender, double renge) {
        TargetPoint t = new TargetPoint(sender.dimension, sender.posX, sender.posY, sender.posZ, renge);
        INSTANCE.sendToAllAround(new MCH_WrapPacketData(pkt), t);
    }

    public static void sendToAllPlayers(W_PacketBase pkt) {
        INSTANCE.sendToAll(new MCH_WrapPacketData(pkt));
    }
}
