package com.norwood.mcheli.compat.hbm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import com.google.common.collect.ImmutableSet;

@Optional.Interface(iface = "com.hbm.explosion.ExplosionNT", modid = "hbm")
public class NTSettingContainer {

    public static final Set<String> POSSIBLE_ATTRIB = ImmutableSet.of(
            "FIRE", "BALEFIRE", "DIGAMMA", "DIGAMMA_CIRCUIT", "LAVA", "LAVA_V", "ERRODE", "ALLMOD", "ALLDROP", "NODROP",
            "NOPARTICLE", "NOSOUND", "NOHURT");

    public final List<String> attributes = new ArrayList<>() {};
    List<Object> runtimeAttribs;
    private int resolution = 16;

    public NTSettingContainer(List<String> runtimeAttribs, int resolution) {
        if (runtimeAttribs != null)
            this.attributes.addAll(runtimeAttribs);
        this.resolution = resolution;
    }

    @Optional.Method(modid = "hbm")
    public void loadRuntimeInstances() {
        runtimeAttribs = attributes.stream().map(com.hbm.explosion.ExplosionNT.ExAttrib::valueOf)
                .collect(Collectors.toList());
    }

    @Optional.Method(modid = "hbm")
    public void explode(World world, Entity exploder, double x, double y, double z, int strenght) {
        var explosionNT = new com.hbm.explosion.ExplosionNT(world, exploder, x, y, z, strenght);
        explosionNT.addAllAttrib((List) runtimeAttribs);
        explosionNT.overrideResolution(resolution);
        explosionNT.explode();
    }
}
