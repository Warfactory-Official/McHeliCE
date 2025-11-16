package com.norwood.mcheli.compat.hbm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class MukeContainer {

    public static MukeContainer PARAMS_SAFE = new MukeContainer() {

        {
            safe = true;
            killRadius = 45F;
            radiationLevel = 2F;
        }
    };
    public static MukeContainer PARAMS_TOTS = new MukeContainer() {

        {
            blastRadius = 10F;
            killRadius = 30F;
            particle = "tinytot";
            shrapnelCount = 0;
            resolution = 32;
            radiationLevel = 1;
        }
    };
    public static MukeContainer PARAMS_LOW = new MukeContainer() {

        {
            blastRadius = 15F;
            killRadius = 45F;
            radiationLevel = 2;
        }
    };
    public static MukeContainer PARAMS_MEDIUM = new MukeContainer() {

        {
            blastRadius = 20F;
            killRadius = 55F;
            radiationLevel = 3;
        }
    };
    public static MukeContainer PARAMS_HIGH = new MukeContainer() {

        {
            miniNuke = false;
            blastRadius = 35;
            shrapnelCount = 0;
        }
    };
    public boolean miniNuke = true;
    public boolean safe = false;
    public float blastRadius;
    public float killRadius;
    public float radiationLevel = 1F;
    public String particle = "muke";
    public int shrapnelCount = 25;
    public int resolution = 64;
    public List<String> attributes = Arrays.asList(
            "FIRE",
            "NOPARTICLE",
            "NOSOUND",
            "NODROP",
            "NOHURT");
    @Getter
    private List<Object> runtimeAttribs;

    @Optional.Method(modid = "hbm")
    public void loadRuntimeInstances() {
        runtimeAttribs = attributes.stream().map(com.hbm.explosion.ExplosionNT.ExAttrib::valueOf)
                .collect(Collectors.toList());
    }

    @Optional.Method(modid = "hbm")
    public void explode(World world, double posX, double posY, double posZ, boolean effectOnly) {
        if (effectOnly) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("type", "muke");
            com.hbm.handler.threading.PacketThreading.createAllAroundThreadedPacket(
                    new com.hbm.packet.toclient.AuxParticlePacketNT(data, posX, posY + 0.5, posZ),
                    new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 250));
            return;
        }
        Object type = getCustomMukeConfig();

        com.hbm.explosion.ExplosionNukeSmall.explode(world, posX, posY, posZ,
                (com.hbm.explosion.ExplosionNukeSmall.MukeParams) type);
    }

    @Optional.Method(modid = "hbm")
    public Object getCustomMukeConfig() {
        var muke = new com.hbm.explosion.ExplosionNukeSmall.MukeParams();

        muke.miniNuke = miniNuke;
        muke.safe = safe;
        muke.blastRadius = blastRadius;
        muke.killRadius = killRadius;
        muke.radiationLevel = radiationLevel;
        muke.particle = particle;
        muke.shrapnelCount = shrapnelCount;
        muke.resolution = resolution;

        if (runtimeAttribs != null && !runtimeAttribs.isEmpty()) {
            muke.explosionAttribs = runtimeAttribs.toArray(new com.hbm.explosion.ExplosionNT.ExAttrib[0]);
        }

        return muke;
    }
}
