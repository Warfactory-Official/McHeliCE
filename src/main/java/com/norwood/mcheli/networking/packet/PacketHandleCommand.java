package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.multiplay.MCH_Multiplay;
import com.norwood.mcheli.multiplay.MCH_MultiplayClient;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.server.CommandScoreboard;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import static com.norwood.mcheli.multiplay.MultiplayerHandler.destoryAllAircraft;

@ElegantPacket
@AllArgsConstructor
@NoArgsConstructor
public class PacketHandleCommand implements ServerToClientPacket, ClientToServerPacket {

    public CommandAction id = CommandAction.NONE;
    public String commandArgs;

    public static void send(EntityPlayerMP player, CommandAction id, String str) {
        if (id != CommandAction.NONE) {
            var packet = new PacketHandleCommand();
            packet.id = id;
            packet.commandArgs = str;
            packet.sendToPlayer(player);
        }
    }

    @Override
    public void onReceive(Minecraft mc) {
        switch (id) {
            case REQUEST_SCREENSHOT -> MCH_MultiplayClient.startSendImageData();
            case REQUEST_MOD_INFO -> MCH_MultiplayClient.sendModsInfo(
                    mc.player.getDisplayName().getFormattedText(), mc.player.getDisplayName().getUnformattedText(),
                    Integer.parseInt(commandArgs));
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        MinecraftServer minecraftServer = MCH_Utils.getServer();
        MCH_Lib.DbgLog(false, "MCH_MultiplayPacketHandler.onPacket_Command cmd:%d:%s", id, commandArgs);
        switch (id) {
            case SHUFFLE_TEAM -> MCH_Multiplay.shuffleTeam(player);
            case JUMP_SPAWNPOINT -> MCH_Multiplay.jumpSpawnPoint(player);
            case RAW_COMMAND -> { // Seeminly just passes command stright to the server, doesnt look right
                ICommandManager icommandmanager = minecraftServer.getCommandManager();
                icommandmanager.executeCommand(player, commandArgs);
            }
            case SETPVP -> {
                if (new CommandScoreboard().checkPermission(minecraftServer, player)) {
                    minecraftServer.setAllowPvp(!minecraftServer.isPVPEnabled());
                    PacketSyncServerSettings.send(null);
                }
            }
            case DESTROY_AIRCRAFT -> destoryAllAircraft(player);
            default -> MCH_Lib.DbgLog(false, "MCH_MultiplayPacketHandler.onPacket_Command unknown cmd:%d:%s", id,
                    commandArgs);
        }
    }

    public static enum CommandAction {
        NONE,
        REQUEST_SCREENSHOT,
        REQUEST_MOD_INFO,
        SHUFFLE_TEAM,
        JUMP_SPAWNPOINT,
        RAW_COMMAND,
        SETPVP,
        DESTROY_AIRCRAFT

    }
}
