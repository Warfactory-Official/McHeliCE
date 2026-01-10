package com.norwood.mcheli;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class MCH_Fluids {

    public static Fluid MCH_FUEL;
    public static void register(){
         MCH_FUEL = new Fluid("mch_fuel", new ResourceLocation("minecraft:blocks/water_still"),  new ResourceLocation("minecraft:blocks/water_flow"), 0xCC8A5A2B);
        FluidRegistry.addBucketForFluid(MCH_FUEL);
    }
}
