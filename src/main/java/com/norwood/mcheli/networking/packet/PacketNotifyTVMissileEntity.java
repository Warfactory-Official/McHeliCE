package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_EntityTvMissile;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElegantPacket
public class PacketNotifyTVMissileEntity implements ServerToClientPacket {

    final public int entityID_Ac;
    final public int entityID_TVMissile;

    public PacketNotifyTVMissileEntity(int entityID_Ac, int entityID_TVMissile) {
        this.entityID_Ac = entityID_Ac;
        this.entityID_TVMissile = entityID_TVMissile;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onReceive(Minecraft mc) {
        if (this.entityID_Ac <= 0 || this.entityID_TVMissile <= 0) {
            return;
        }
        Entity entity = mc.player.world.getEntityByID(this.entityID_Ac);
        if (entity instanceof MCH_EntityAircraft ac) {
            entity = mc.player.world.getEntityByID(this.entityID_TVMissile);
            if (entity instanceof MCH_EntityTvMissile) {
                ((MCH_EntityTvMissile) entity).shootingEntity = mc.player;
                ac.setTVMissile((MCH_EntityTvMissile) entity);
            }
        }
    }
}
