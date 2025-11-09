package com.norwood.mcheli.wrapper;

import org.lwjgl.opengl.GL11;

public class W_OpenGlHelper {

    public static void glBlendFunc(int i, int j, int k, int l) {
        GlStateManager.blendFunc(i, j);
    }
}
