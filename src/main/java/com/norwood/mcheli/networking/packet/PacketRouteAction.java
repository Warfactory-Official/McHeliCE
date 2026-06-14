package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.mission.MissionNode;
import com.norwood.mcheli.wingman.mission.MissionPlan;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Client → Server: create or delete a saved (node-list) mission route.
 */
@ElegantPacket
public class PacketRouteAction implements ClientToServerPacket {

    public static final int CREATE = 0;
    public static final int DELETE = 1;

    public int          action;
    public String       name = "";
    public List<String> nodes = new ArrayList<>();

    public PacketRouteAction() {}

    public PacketRouteAction(int action, String name, List<String> nodes) {
        this.action = action;
        this.name   = name;
        this.nodes  = nodes;
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        WorldServer ws = player.getServerWorld();
        ws.addScheduledTask(() -> {
            if (this.action == CREATE) {
                if (this.name.isEmpty() || this.nodes.isEmpty()) {
                    player.sendMessage(new TextComponentString("§cRoute name and nodes required."));
                    return;
                }
                List<MissionNode> parsed = new ArrayList<>();
                for (String s : this.nodes) {
                    try { parsed.add(MissionNode.parse(s)); }
                    catch (Exception e) {
                        player.sendMessage(new TextComponentString("§cInvalid node: " + s));
                        return;
                    }
                }
                MissionPlan.put(this.name, parsed);
                player.sendMessage(new TextComponentString(
                    "§aRoute §e" + this.name + "§a saved (" + parsed.size() + " nodes)."));
            } else {
                boolean removed = MissionPlan.remove(this.name);
                player.sendMessage(removed
                    ? new TextComponentString("§aRoute §e" + this.name + "§a deleted.")
                    : new TextComponentString("§cRoute not found: " + this.name));
            }
        });
    }
}
