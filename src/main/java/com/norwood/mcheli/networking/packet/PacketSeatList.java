package com.norwood.mcheli.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.wrapper.W_Entity;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

@ElegantPacket
public class PacketSeatList extends PacketBase implements ServerToClientPacket {

    public int entityID_AC = -1;
    public byte seatNum = -1;
    public int[] seatEntityID = new int[] { -1 };

    public static void sendSeatList(MCH_EntityAircraft ac, EntityPlayerMP player) {
        var packet = new PacketSeatList();
        packet.setParameter(ac);
        packet.sendToPlayer(player);
    }

    protected void setParameter(MCH_EntityAircraft ac) {
        if (ac != null) {
            this.entityID_AC = W_Entity.getEntityId(ac);
            this.seatNum = (byte) ac.getSeats().length;
            if (this.seatNum > 0) {
                this.seatEntityID = new int[this.seatNum];

                for (int i = 0; i < this.seatNum; i++) {
                    this.seatEntityID[i] = W_Entity.getEntityId(ac.getSeat(i));
                }
            } else {
                this.seatEntityID = new int[] { -1 };
            }
        }
    }

    @Override
    public void onReceive(Minecraft mc) {
        if (!mc.player.world.isRemote) {
            return;
        }

        if (this.entityID_AC <= 0) {
            return;
        }

        Entity entity = mc.player.world.getEntityByID(this.entityID_AC);
        if (!(entity instanceof MCH_EntityAircraft ac)) {
            return;
        }

        int seatNum = this.seatNum;
        if (seatNum <= 0 ||
                ac.getSeats().length != seatNum ||
                this.seatEntityID == null ||
                this.seatEntityID.length != seatNum) {
            return;
        }

        for (int i = 0; i < seatNum; i++) {
            Entity seatEntity = mc.player.world.getEntityByID(this.seatEntityID[i]);
            if (seatEntity instanceof MCH_EntitySeat seat) {
                seat.seatID = i;
                seat.parentUniqueID = ac.getCommonUniqueId();
                ac.setSeat(i, seat);
                seat.setParent(ac);
            }
        }
    }
}
