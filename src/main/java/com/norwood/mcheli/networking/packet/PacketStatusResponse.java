package com.norwood.mcheli.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

@ElegantPacket
public class PacketStatusResponse extends PacketBase implements ServerToClientPacket {

    public int entityID_AC = -1;
    public byte seatNum = -1;
    public byte[] weaponIDs = new byte[] { -1 };

    public static void sendStatus(MCH_EntityAircraft ac, EntityPlayer player) {
        PacketStatusResponse packet = new PacketStatusResponse();
        packet.setParameter(ac);
        packet.sendToPlayer((EntityPlayerMP) player);
    }

    @Override
    public void onReceive(Minecraft mc) {
        if (this.entityID_AC <= 0) {
            return;
        }
        Entity entity = mc.player.world.getEntityByID(this.entityID_AC);
        StringBuilder msg = new StringBuilder("onPacketStatusResponse:EID=").append(this.entityID_AC).append(":");

        if (!(entity instanceof MCH_EntityAircraft ac)) {
            msg.append("Not an aircraft");
            MCH_Lib.DbgLog(true, msg.toString());
            return;
        }

        if (this.seatNum <= 0 || this.weaponIDs == null || this.weaponIDs.length != this.seatNum) {
            msg.append("Error seatNum=").append(this.seatNum);
            MCH_Lib.DbgLog(true, msg.toString());
            return;
        }

        msg.append("seatNum=").append(this.seatNum).append(":");
        for (int i = 0; i < this.seatNum; i++) {
            ac.updateWeaponID(i, this.weaponIDs[i]);
            msg.append("[").append(i).append(",").append(this.weaponIDs[i]).append("]");
        }

        MCH_Lib.DbgLog(true, msg.toString());
    }

    protected void setParameter(MCH_EntityAircraft ac) {
        if (ac != null) {
            this.entityID_AC = W_Entity.getEntityId(ac);
            this.seatNum = (byte) (ac.getSeatNum() + 1);
            if (this.seatNum > 0) {
                this.weaponIDs = new byte[this.seatNum];

                for (int i = 0; i < this.seatNum; i++) {
                    this.weaponIDs[i] = (byte) ac.getWeaponIDBySeatID(i);
                }
            } else {
                this.weaponIDs = new byte[] { -1 };
            }
        }
    }
}
