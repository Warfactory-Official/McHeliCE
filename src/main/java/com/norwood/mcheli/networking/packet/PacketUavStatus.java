package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.uav.MCH_EntityUavStation;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketUavStatus extends PacketBase implements ClientToServerPacket {

    public byte posUavX = 0;
    public byte posUavY = 0;
    public byte posUavZ = 0;
    public boolean continueControl = false;

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (player.getRidingEntity() instanceof MCH_EntityUavStation) {
            ((MCH_EntityUavStation) player.getRidingEntity()).setUavPosition(this.posUavX, this.posUavY, this.posUavZ);
            if (this.continueControl) {
                ((MCH_EntityUavStation) player.getRidingEntity()).controlLastAircraft(player);
            }
        }
    }
}
