package com.norwood.mcheli.helper.client.model;

import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.Tags;

public enum LegacyModelLoader implements ICustomModelLoader {

    INSTANCE;

    public static final String VARIANT = "mcheli_legacy";
    static final String TEMPLATE = "{'parent':'item/generated','textures':{'layer0':'__item__'}}".replaceAll("'", "\"");

    public void onResourceManagerReload(@NotNull IResourceManager resourceManager) {}

    public boolean accepts(@NotNull ResourceLocation modelLocation) {
        if (!(modelLocation instanceof ModelResourceLocation location)) {
            return false;
        } else {
            return location.getNamespace().equals(Tags.MODID) && location.getVariant().equals("mcheli_legacy");
        }
    }

    public @NotNull IModel loadModel(ResourceLocation modelLocation) throws Exception {
        String path = modelLocation.getNamespace() + ":items/" + modelLocation.getPath();
        ModelBlock modelblock = ModelBlock.deserialize(TEMPLATE.replaceAll("__item__", path));
        modelblock.parent = ModelLoaderRegistry.getModel(modelblock.getParentLocation()).asVanillaModel().get();
        return new MCH_WrapperItemLayerModel(modelblock);
    }
}
