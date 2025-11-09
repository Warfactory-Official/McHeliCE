package com.norwood.mcheli.helper.client.model;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class MCH_WrapperItemLayerModel implements IModel {

    private final ItemLayerModel model;
    private final ModelBlock raw;

    public MCH_WrapperItemLayerModel(ModelBlock modelBlock) {
        this.raw = modelBlock;
        this.model = new ItemLayerModel(modelBlock);
    }

    public @NotNull Collection<ResourceLocation> getTextures() {
        return this.model.getTextures();
    }

    public @NotNull IModel retexture(@NotNull ImmutableMap<String, String> textures) {
        return this.model.retexture(textures);
    }

    public @NotNull IBakedModel bake(@NotNull IModelState state, @NotNull VertexFormat format,
                                     @NotNull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        ItemCameraTransforms transforms = this.raw.getAllTransforms();
        Map<TransformType, TRSRTransformation> tMap = Maps.newEnumMap(TransformType.class);
        tMap.putAll(PerspectiveMapWrapper.getTransforms(transforms));
        tMap.putAll(PerspectiveMapWrapper.getTransforms(state));
        IModelState perState = new SimpleModelState(ImmutableMap.copyOf(tMap), state.apply(Optional.empty()));
        return this.model.bake(perState, format, bakedTextureGetter);
    }
}
