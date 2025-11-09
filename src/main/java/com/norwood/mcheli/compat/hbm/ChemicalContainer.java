package com.norwood.mcheli.compat.hbm;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChemicalContainer {

    public final double speed;
    public final int count;
    public final ChemType type;

    public static enum ChemType {
        CHLORINE,
        CLOUD,
        PINK_CLOUD,
        ORANGE
    }
}
