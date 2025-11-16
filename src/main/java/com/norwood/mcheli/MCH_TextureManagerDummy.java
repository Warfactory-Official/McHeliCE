package com.norwood.mcheli;

import com.norwood.mcheli.helper.MCH_Utils;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MCH_TextureManagerDummy extends TextureManager {

    public static final ResourceLocation R = MCH_Utils.suffix("textures/test.png");
    private final TextureManager tm;

    public MCH_TextureManagerDummy(TextureManager t) {
        super(null);
        this.tm = t;
    }

    public void bindTexture(@NotNull ResourceLocation resouce) {
        if (MCH_ClientCommonTickHandler.cameraMode == 2) {
            this.tm.bindTexture(R);
        } else {
            this.tm.bindTexture(resouce);
        }
    }

    public boolean loadTickableTexture(@NotNull ResourceLocation textureLocation,
                                       @NotNull ITickableTextureObject textureObj) {
        return this.tm.loadTickableTexture(textureLocation, textureObj);
    }

    public boolean loadTexture(@NotNull ResourceLocation textureLocation, @NotNull ITextureObject textureObj) {
        return this.tm.loadTexture(textureLocation, textureObj);
    }

    public @NotNull ITextureObject getTexture(@NotNull ResourceLocation textureLocation) {
        return this.tm.getTexture(textureLocation);
    }

    public @NotNull ResourceLocation getDynamicTextureLocation(@NotNull String name, @NotNull DynamicTexture texture) {
        return this.tm.getDynamicTextureLocation(name, texture);
    }

    public void tick() {
        this.tm.tick();
    }

    public void deleteTexture(@NotNull ResourceLocation textureLocation) {
        this.tm.deleteTexture(textureLocation);
    }

    public void onResourceManagerReload(@NotNull IResourceManager resourceManager) {
        this.tm.onResourceManagerReload(resourceManager);
    }
}
