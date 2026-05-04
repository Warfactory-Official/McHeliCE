package com.norwood.mcheli.networking;

import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wrapper.W_Entity;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
public class PacketLockTarget implements ClientToServerPacket {
    final public int targetID;
    final public int entityID;


    public PacketLockTarget(int targetID, int entityID) {
        this.targetID = targetID;
        this.entityID = entityID;
    }


    @Override
    public void onReceive(EntityPlayerMP player) {
        Entity entity = player.world.getEntityByID(this.entityID);
        if (!(entity instanceof MCH_EntityBaseBullet bullet)) return;
        if (!W_Entity.isEqual(bullet.shootingEntity, player)) return;
        Entity target = player.world.getEntityByID(this.targetID);
        bullet.setTargetEntity(target);
    }
}
