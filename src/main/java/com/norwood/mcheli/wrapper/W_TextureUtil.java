package com.norwood.mcheli.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class W_TextureUtil {

    private static final W_TextureUtil instance = new W_TextureUtil();

    public static W_TextureUtil.TextureParam getTextureInfo(String domain, String name) {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        ResourceLocation r = new ResourceLocation(domain, name);
        textureManager.bindTexture(r);
        W_TextureUtil.TextureParam info = instance.newParam();
        info.width = GL11.glGetTexLevelParameteri(3553, 0, 4096);
        info.height = GL11.glGetTexLevelParameteri(3553, 0, 4097);
        return info;
    }

    private W_TextureUtil.TextureParam newParam() {
        return new TextureParam(this);
    }

    public static class TextureParam {

        public int width;
        public int height;

        public TextureParam(W_TextureUtil paramW_TextureUtil) {}
    }
}
