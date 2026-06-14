package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.mission.AutonomousState;
import com.norwood.mcheli.wingman.mission.MissionNode;
import com.norwood.mcheli.wingman.mission.MissionPlan;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;

/**
 * Client → Server: assign or abort a (legacy node-list) mission.
 */
@ElegantPacket
public class PacketMissionAction implements ClientToServerPacket {

    public static final int ASSIGN = 0;
    public static final int ABORT  = 1;

    public int    action;
    public String uuidStr = "";
    public String routeName = "";

    public PacketMissionAction() {}

    public PacketMissionAction(int action, String uuidStr, String routeName) {
        this.action    = action;
        this.uuidStr   = uuidStr;
        this.routeName = routeName;
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        WorldServer ws = player.getServerWorld();
        ws.addScheduledTask(() -> {
            if (this.action == ASSIGN) {
                UUID uid;
                try { uid = UUID.fromString(this.uuidStr); } catch (Exception e) { return; }
                Entity wingman = ws.getEntityFromUuid(uid);
                if (wingman == null) {
                    player.sendMessage(new TextComponentString("§cEntity not found."));
                    return;
                }
                List<MissionNode> nodes = MissionPlan.get(this.routeName);
                if (nodes == null) {
                    player.sendMessage(new TextComponentString("§cRoute not found: " + this.routeName));
                    return;
                }
                WingmanEntry entry = WingmanRegistry.get(uid);
                if (entry == null) {
                    entry = new WingmanEntry();
                    WingmanRegistry.put(uid, entry);
                }
                entry.mission = nodes;
                entry.missionIndex = 0;
                entry.missionNodeTimer = 0;
                entry.autoState = AutonomousState.ENROUTE;
                player.sendMessage(new TextComponentString(
                    "§aMission §e" + this.routeName + "§a assigned to " + this.uuidStr.substring(0, 8) + "..."));
            } else {
                if (this.uuidStr.isEmpty()) {
                    int count = 0;
                    for (WingmanEntry e : WingmanRegistry.snapshot().values()) {
                        if (e.isAutonomous()) { e.mission = null; e.autoState = AutonomousState.NONE; count++; }
                    }
                    player.sendMessage(new TextComponentString("§aAborted " + count + " mission(s)."));
                } else {
                    UUID uid;
                    try { uid = UUID.fromString(this.uuidStr); } catch (Exception e) { return; }
                    WingmanEntry entry = WingmanRegistry.get(uid);
                    if (entry != null) { entry.mission = null; entry.autoState = AutonomousState.NONE; }
                    player.sendMessage(new TextComponentString("§aMission aborted."));
                }
            }
        });
    }
}
