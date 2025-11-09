package com.norwood.mcheli.networking.packet;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

@ElegantPacket
public class PacketSpotEnemy implements ServerToClientPacket {

    private static final int MAX_COUNT = 30000;
    private static final int MAX_ENTITIES = 300;

    public int count = 0;
    public int num = 0;
    public int[] entityId = new int[0];

    public static void send(EntityPlayerMP player, int count, int[] entityIds) {
        if (player == null || entityIds == null || entityIds.length == 0 || count <= 0) return;

        int limitedCount = Math.min(count, MAX_COUNT);
        int limitedNum = Math.min(entityIds.length, MAX_ENTITIES);

        var packet = new PacketSpotEnemy();
        packet.count = limitedCount;
        packet.num = limitedNum;
        packet.entityId = Arrays.copyOf(entityIds, limitedNum);

        packet.sendToPlayer(player);
    }

    @Override
    public void onReceive(Minecraft mc) {
        if (count > 0) {
            for (int i = 0; i < num; i++) {
                MCH_GuiTargetMarker.addSpotedEntity(entityId[i], count);
            }
        }
    }
}
