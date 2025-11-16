package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketNotifyLock extends PacketBase implements ServerToClientPacket, ClientToServerPacket {

    public int entityID = -1;

    public static void send(Entity target) {
        if (target != null) {
            var packet = new PacketNotifyLock();
            packet.entityID = target.getEntityId();
            packet.sendToServer();
        }
    }

    @Override// Server
    public void onReceive(EntityPlayerMP player) {
        if (this.entityID <= 0) return;
        Entity target = player.world.getEntityByID(this.entityID);
        if (target != null) {
            MCH_EntityAircraft ac;
            if (target instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft) target;
            } else if (target instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat) target).getParent();
            } else {
                ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(target);
            }

            if (ac != null && ac.haveFlare() && !ac.isDestroyed()) {
                for (int i = 0; i < 2; i++) {
                    Entity entity = ac.getEntityBySeatId(i);
                    if (entity instanceof EntityPlayerMP) {
                        new PacketNotifyLock().sendToPlayer((EntityPlayerMP) entity);
                    }
                }
            } else if (target.getRidingEntity() != null && target instanceof EntityPlayerMP) {
                new PacketNotifyLock().sendToPlayer((EntityPlayerMP) target);
            }
        }
    }

    @Override// Client
    public void onReceive(Minecraft mc) {
        mc.addScheduledTask(() -> MCH_MOD.proxy.clientLocked());
    }
}
