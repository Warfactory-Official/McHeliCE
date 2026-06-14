package com.norwood.mcheli;

import net.minecraft.potion.PotionEffect;

public class MCH_PotionEffect {

    public final PotionEffect potionEffect;
    public final int startDist;
    public final int endDist;

    public MCH_PotionEffect(PotionEffect potionEffect, int startDist, int endDist) {
        this.potionEffect = potionEffect;
        this.startDist = startDist;
        this.endDist = endDist;
    }
}
