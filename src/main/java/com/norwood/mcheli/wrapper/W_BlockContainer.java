package com.norwood.mcheli.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import org.jetbrains.annotations.NotNull;

public abstract class W_BlockContainer extends BlockContainer {

    protected W_BlockContainer(int par1, Material par2Material) {
        super(par2Material);
    }

    public @NotNull Block setLightLevel(float f) {
        return super.setLightLevel(f);
    }

    public @NotNull Block setTranslationKey(@NotNull String s) {
        return super.setTranslationKey(s);
    }
}
