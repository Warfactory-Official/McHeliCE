package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;


@Deprecated //Will be replaced by modular
@ElegantPacket
public class PacketOpenScreen implements ClientToServerPacket {

    public int guiID = -1; //Raw integer again...

    public static void send(int gui_id) {
        if (gui_id >= 0) {
            var packet = new PacketOpenScreen();
            packet.guiID = gui_id;
            packet.sendToServer();
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (this.guiID == 3) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
            if (ac != null) {
                ac.displayInventory(player);
            }
        } else {
            player.openGui(
                    MCH_MOD.instance, this.guiID, player.world, (int) player.posX, (int) player.posY, (int) player.posZ
            );
        }
    }

}