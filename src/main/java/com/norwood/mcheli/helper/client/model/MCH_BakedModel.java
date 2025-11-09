package com.norwood.mcheli.helper.client.model;

import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.helper.client.renderer.item.IItemModelRenderer;

@SideOnly(Side.CLIENT)
public class MCH_BakedModel implements IBakedModel {

    private final IBakedModel bakedModel;
    private final IItemModelRenderer renderer;
    private final ItemOverrideList overrides;

    public MCH_BakedModel(IBakedModel bakedModel, IItemModelRenderer renderer) {
        this.bakedModel = bakedModel;
        this.renderer = renderer;
        this.overrides = new MCH_ItemOverrideList(bakedModel);
    }

    public @NotNull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return this.bakedModel.getQuads(state, side, rand);
    }

    public boolean isAmbientOcclusion() {
        return this.bakedModel.isAmbientOcclusion();
    }

    public boolean isGui3d() {
        return this.bakedModel.isGui3d();
    }

    public boolean isBuiltInRenderer() {
        return this.renderer.shouldRenderer(PooledModelParameters.getTargetRendererStack(),
                PooledModelParameters.getTransformType()) || this.bakedModel.isBuiltInRenderer();
    }

    public @NotNull TextureAtlasSprite getParticleTexture() {
        return this.bakedModel.getParticleTexture();
    }

    @Deprecated
    public @NotNull ItemCameraTransforms getItemCameraTransforms() {
        return this.bakedModel.getItemCameraTransforms();
    }

    public @NotNull ItemOverrideList getOverrides() {
        return this.overrides;
    }

    public boolean isAmbientOcclusion(@NotNull IBlockState state) {
        return this.bakedModel.isAmbientOcclusion(state);
    }

    public @NotNull Pair<? extends IBakedModel, Matrix4f> handlePerspective(@NotNull TransformType cameraTransformType) {
        PooledModelParameters.setTransformType(cameraTransformType);
        Pair<? extends IBakedModel, Matrix4f> pair = this.bakedModel.handlePerspective(cameraTransformType);
        return Pair.of(new MCH_BakedModel(pair.getLeft(), this.renderer), pair.getRight());
    }
}
