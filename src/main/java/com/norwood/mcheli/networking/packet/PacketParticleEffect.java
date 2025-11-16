package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.helper.world.MCH_ExplosionV2;
import com.norwood.mcheli.networking.data.DataExplosionParameters;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

@ElegantPacket// TODO:Will be reworked, I just want to be done with packets
// We can use NBT to get way more flexible effect packet handling
@RequiredArgsConstructor
public class PacketParticleEffect implements ServerToClientPacket {

    public final DataExplosionParameters explosionParameters;
    public static final int RANGE = 1000;

    public void sendAround(World world) {
        sendPacketToAllAround(world, explosionParameters.x, explosionParameters.y, explosionParameters.z, RANGE);
    };

    @Override
    public void onReceive(Minecraft mc) {
        var player = mc.player;

        Entity exploder = null;
        if (!explosionParameters.inWater) {
            if (!MCH_Config.DefaultExplosionParticle.prmBool) {
                List<BlockPos> affectedPositions = explosionParameters.getAffectedPositions();
                MCH_ExplosionV2.effectMODExplosion(player.world, explosionParameters.x, explosionParameters.y,
                        explosionParameters.z, explosionParameters.size, affectedPositions);
            } else {
                List<BlockPos> affectedPositions = explosionParameters.getAffectedPositions();
                MCH_ExplosionV2.effectVanillaExplosion(player.world, explosionParameters.x, explosionParameters.y,
                        explosionParameters.z, explosionParameters.size, affectedPositions);
            }
        } else {
            MCH_ExplosionV2.effectExplosionInWater(player.world, explosionParameters.x, explosionParameters.y,
                    explosionParameters.z, explosionParameters.size);
        }
    }
}
