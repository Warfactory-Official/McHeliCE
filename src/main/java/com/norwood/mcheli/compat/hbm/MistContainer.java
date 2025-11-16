package com.norwood.mcheli.compat.hbm;

import lombok.NoArgsConstructor;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

@NoArgsConstructor
public class MistContainer {

    public String fluidType = "None";
    public int cloudCount = 1;
    public float width = 10;
    public float height = 5;
    public int areaSpread = 0;
    public int lifetime = 80;
    public int lifetimeVariance = 0;

    @Optional.Method(modid = "hbm")
    public void execute(World world, double x, double y, double z) {
        if (fluidType.equals("None")) return;
        for (int i = 0; i < cloudCount; i++) {
            com.hbm.entity.effect.EntityMist mist = new com.hbm.entity.effect.EntityMist(world);
            mist.setType(com.hbm.inventory.fluid.Fluids.fromName(fluidType));
            double xVariance = areaSpread == 0 ? x : x + world.rand.nextInt(areaSpread * 2) - areaSpread;
            double zVariance = areaSpread == 0 ? z : z + world.rand.nextInt(areaSpread * 2) - areaSpread;
            int calculatedLifetime = lifetimeVariance == 0 ? lifetime : lifetime + world.rand.nextInt(lifetimeVariance);
            mist.setPosition(xVariance, y, zVariance);
            mist.setArea(width, height);
            mist.setDuration(calculatedLifetime);
            world.spawnEntity(mist);
        }
    }
}
