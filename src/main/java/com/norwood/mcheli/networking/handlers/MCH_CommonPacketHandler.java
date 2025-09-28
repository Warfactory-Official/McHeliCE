package com.norwood.mcheli.networking.handlers;

import com.google.common.io.ByteArrayDataInput;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.helper.network.HandleSide;
import com.norwood.mcheli.helper.world.MCH_ExplosionV2;
import com.norwood.mcheli.networking.packet.MCH_PacketEffectExplosion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

public class MCH_CommonPacketHandler {
    @HandleSide({Side.CLIENT})
    public static void onPacketEffectExplosion(EntityPlayer player, ByteArrayDataInput data, IThreadListener scheduler) {
        if (player.world.isRemote) {
            MCH_PacketEffectExplosion pkt = new MCH_PacketEffectExplosion();
            pkt.readData(data);
            scheduler.addScheduledTask(
                    () -> {
                        Entity exploder = null;
                        if (player.getDistanceSq(pkt.prm.posX, pkt.prm.posY, pkt.prm.posZ) <= 40000.0) {
                            if (!pkt.prm.inWater) {
                                if (!MCH_Config.DefaultExplosionParticle.prmBool) {
                                    List<BlockPos> affectedPositions = pkt.prm.getAffectedBlockPositions();
                                    MCH_ExplosionV2.effectMODExplosion(player.world, pkt.prm.posX, pkt.prm.posY, pkt.prm.posZ, pkt.prm.size, affectedPositions);
                                } else {
                                    List<BlockPos> affectedPositions = pkt.prm.getAffectedBlockPositions();
                                    MCH_ExplosionV2.effectVanillaExplosion(player.world, pkt.prm.posX, pkt.prm.posY, pkt.prm.posZ, pkt.prm.size, affectedPositions);
                                }
                            } else {
                                MCH_ExplosionV2.effectExplosionInWater(player.world, pkt.prm.posX, pkt.prm.posY, pkt.prm.posZ, pkt.prm.size);
                            }
                        }
                    }
            );
        }
    }


}
