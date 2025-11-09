package com.norwood.mcheli.wrapper.modelloader;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class W_TextureCoordinate {

    public final float u;
    public final float v;
    public final float w;

    public W_TextureCoordinate(float u, float v) {
        this(u, v, 0.0F);
    }

    public W_TextureCoordinate(float u, float v, float w) {
        this.u = u;
        this.v = v;
        this.w = w;
    }
}
