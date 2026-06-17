package com.norwood.mcheli.compat.hbm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public class MukeContainer {

    private static final String C_NUKE_SMALL = "com.hbm.explosion.ExplosionNukeSmall";
    private static final String C_MUKE_PARAMS = "com.hbm.explosion.ExplosionNukeSmall$MukeParams";
    private static final String C_EX_ATTRIB = "com.hbm.explosion.ExplosionNT$ExAttrib";
    private static final String C_EFFECT_NT = "com.hbm.particle.helper.HbmEffectNT";
    private static final String C_AUX_PACKET = "com.hbm.packet.toclient.AuxParticlePacketNT";
    private static final String C_PACKET_THREADING = "com.hbm.handler.threading.PacketThreading";

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
            particle = "TinyTot";
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
    public String particle = "Muke";
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

    /** Resolve attribute names to HBM {@code ExAttrib} enum constants (unknown names are skipped). */
    public void loadRuntimeInstances() {
        runtimeAttribs = HBMReflect.enumList(C_EX_ATTRIB, attributes);
    }

    public void explode(World world, double posX, double posY, double posZ, boolean effectOnly) {
        if (!HBMReflect.available()) return;

        if (effectOnly) {
            sendEffectPacket(world, posX, posY, posZ);
            return;
        }

        Object params = buildMukeParams();
        if (params == null) return;
        // ExplosionNukeSmall.explode(World, double, double, double, MukeParams)
        HBMReflect.callStatic(C_NUKE_SMALL, "explode", world, posX, posY, posZ, params);
    }

    private Object buildMukeParams() {
        Object muke = HBMReflect.construct(C_MUKE_PARAMS);
        if (muke == null) return null;

        HBMReflect.setField(muke, "miniNuke", miniNuke);
        HBMReflect.setField(muke, "safe", safe);
        HBMReflect.setField(muke, "blastRadius", blastRadius);
        HBMReflect.setField(muke, "killRadius", killRadius);
        HBMReflect.setField(muke, "radiationLevel", radiationLevel);
        HBMReflect.setField(muke, "shrapnelCount", shrapnelCount);
        HBMReflect.setField(muke, "resolution", resolution);

        // particle: HBM field type is the HbmEffectNT enum (was a String in older versions).
        Object particleEnum = HBMReflect.enumValueIgnoreCase(C_EFFECT_NT, particle);
        if (particleEnum != null) {
            HBMReflect.setField(muke, "particle", particleEnum);
        }

        // explosionAttribs: ExAttrib[]
        if (runtimeAttribs != null && !runtimeAttribs.isEmpty()) {
            Object attribArray = HBMReflect.enumArray(C_EX_ATTRIB, runtimeAttribs);
            if (attribArray != null) {
                HBMReflect.setField(muke, "explosionAttribs", attribArray);
            }
        }

        return muke;
    }

    private void sendEffectPacket(World world, double posX, double posY, double posZ) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", "muke");
        Object packet = HBMReflect.construct(C_AUX_PACKET, data, posX, posY + 0.5, posZ);
        if (packet == null) return;
        NetworkRegistry.TargetPoint target =
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 250);
        HBMReflect.callStatic(C_PACKET_THREADING, "createAllAroundThreadedPacket", packet, target);
    }
}
