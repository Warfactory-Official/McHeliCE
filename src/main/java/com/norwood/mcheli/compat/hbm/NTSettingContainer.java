package com.norwood.mcheli.compat.hbm;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class NTSettingContainer {

    private static final String C_EXPLOSION_NT = "com.hbm.explosion.ExplosionNT";
    private static final String C_EX_ATTRIB = "com.hbm.explosion.ExplosionNT$ExAttrib";

    public final List<String> attributes = new ArrayList<>();
    private List<Object> runtimeAttribs;
    private final int resolution;

    public NTSettingContainer(List<String> runtimeAttribs, int resolution) {
        if (runtimeAttribs != null)
            this.attributes.addAll(runtimeAttribs);
        this.resolution = resolution;
    }

    public void loadRuntimeInstances() {
        runtimeAttribs = HBMReflect.enumList(C_EX_ATTRIB, attributes);
    }

    public void explode(World world, Entity exploder, double x, double y, double z, int strenght) {
        if (!HBMReflect.available()) return;

        // ExplosionNT(World, Entity, double, double, double, float strength)
        Object explosionNT = HBMReflect.construct(C_EXPLOSION_NT, world, exploder, x, y, z, (float) strenght);
        if (explosionNT == null) return;

        if (runtimeAttribs != null && !runtimeAttribs.isEmpty()) {
            HBMReflect.call(explosionNT, "addAllAttrib", new ArrayList<>(runtimeAttribs));
        }
        HBMReflect.call(explosionNT, "overrideResolution", resolution);
        HBMReflect.call(explosionNT, "explode");
    }
}
