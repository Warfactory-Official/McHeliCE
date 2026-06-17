package com.norwood.mcheli.compat.hbm;

import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Config + spawn logic for HBM gas/fluid {@code EntityMist} clouds. Pure config POJO; all HBM
 * access is reflective via {@link HBMReflect} and no-ops when HBM is absent.
 */
@NoArgsConstructor
public class MistContainer {

    private static final String C_MIST = "com.hbm.entity.effect.EntityMist";
    private static final String C_FLUIDS = "com.hbm.inventory.fluid.Fluids";

    public String fluidType = "None";
    public int cloudCount = 1;
    public float width = 10;
    public float height = 5;
    public int areaSpread = 0;
    public int lifetime = 80;
    public int lifetimeVariance = 0;

    public void execute(World world, double x, double y, double z) {
        if (fluidType.equals("None") || !HBMReflect.available()) return;

        Object fluid = HBMReflect.callStatic(C_FLUIDS, "fromName", fluidType);
        if (fluid == null) return; // unknown fluid name or HBM missing

        for (int i = 0; i < cloudCount; i++) {
            Object mistObj = HBMReflect.construct(C_MIST, world);
            if (!(mistObj instanceof Entity mist)) return;

            double xVariance = areaSpread == 0 ? x : x + world.rand.nextInt(areaSpread * 2) - areaSpread;
            double zVariance = areaSpread == 0 ? z : z + world.rand.nextInt(areaSpread * 2) - areaSpread;
            int calculatedLifetime = lifetimeVariance == 0 ? lifetime : lifetime + world.rand.nextInt(lifetimeVariance);

            HBMReflect.call(mist, "setType", fluid);
            HBMReflect.call(mist, "setArea", width, height);
            HBMReflect.call(mist, "setDuration", calculatedLifetime);
            mist.setPosition(xVariance, y, zVariance);
            world.spawnEntity(mist);
        }
    }
}
