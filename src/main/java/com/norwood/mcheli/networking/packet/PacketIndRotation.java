package com.norwood.mcheli.networking.packet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;

@ElegantPacket
public class PacketIndRotation extends PacketBase implements ClientToServerPacket {

    public int entityID_Ac = -1;
    public float yaw = 0.0F;
    public float pitch = 0.0F;
    public float roll = 0.0F;
    public boolean rollRev = false;

    public static void send(MCH_EntityAircraft ac) {
        if (ac != null) {
            var packet = new PacketIndRotation();
            packet.entityID_Ac = W_Entity.getEntityId(ac);
            packet.yaw = ac.getRotYaw();
            packet.pitch = ac.getRotPitch();
            packet.roll = ac.getRotRoll();
            packet.rollRev = ac.aircraftRollRev;
            packet.sendToServer();
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (player == null || player.world.isRemote || this.entityID_Ac <= 0) {
            return;
        }

        Entity entity = player.world.getEntityByID(this.entityID_Ac);
        if (!(entity instanceof MCH_EntityAircraft ac)) {
            return;
        }

        ac.setRotRoll(this.roll);

        if (this.rollRev) {
            MCH_Lib.DbgLog(ac.world,
                    "onPacketIndRotation Error:this.rollRev y=%.2f, p=%.2f, r=%.2f",
                    this.yaw, this.pitch, this.roll);

            Entity rider = ac.getRiddenByEntity();
            if (rider != null) {
                rider.rotationYaw = this.yaw;
                rider.prevRotationYaw = this.yaw;
            }

            for (int sid = 0; sid < ac.getSeatNum(); sid++) {
                Entity passenger = ac.getEntityBySeatId(sid);
                if (passenger != null) {
                    passenger.rotationYaw += (passenger.rotationYaw <= 0.0F ? 180.0F : -180.0F);
                }
            }
        }

        ac.setRotYaw(this.yaw);
        ac.setRotPitch(this.pitch);
    }
}
