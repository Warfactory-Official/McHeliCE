package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_Lib;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

import static com.norwood.mcheli.multiplay.MultiplayerHandler.*;

@ElegantPacket
// FIXME:Terrible implementation, low prio
public class PacketSendModlist implements ServerToClientPacket, ClientToServerPacket {

    public List<String> list = new ArrayList<>();
    public boolean firstData = false;
    public int id = 0;
    public int num = 0;

    public static void send(List<String> list, int id) {
        PacketSendModlist packet = null;
        int size = 0;
        boolean isFirst = true;

        for (String s : list) {
            if (packet == null) {
                packet = new PacketSendModlist();
                packet.id = id;
                packet.firstData = isFirst;
                isFirst = false;
            }

            packet.list.add(s);
            size += s.length() + 2;
            if (size > 1024) {
                packet.num = packet.list.size();
                packet.sendToServer();
                packet = null;
                size = 0;
            }
        }

        if (packet != null) {
            packet.num = packet.list.size();
            packet.sendToServer();
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (id == getPlayerInfoId(player)) {
            if (modListRequestPlayer != null) {
                this.sendToPlayer((EntityPlayerMP) modListRequestPlayer);

            } else {
                if (firstData) {
                    LogInfo("###### " + player.getDisplayName() + " ######");
                }

                for (String s : list) {
                    LogInfo(s);
                }
            }

        }
    }

    @Override
    public void onReceive(Minecraft mc) {
        MCH_Lib.DbgLog(mc.player.world, "MCH_MultiplayPacketHandler.onPacket_ModList : ID=%d, Num=%d", id, num);
        if (firstData) {
            MCH_Lib.Log(TextFormatting.RED + "###### " + mc.player.getDisplayName() + " ######");
        }

        for (String s : list) {
            MCH_Lib.Log(s);
            mc.player.sendMessage(new TextComponentString(s));
        }
    }
}
