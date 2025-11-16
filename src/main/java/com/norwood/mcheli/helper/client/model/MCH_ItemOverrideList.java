package com.norwood.mcheli.helper.client.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;

public class MCH_ItemOverrideList extends ItemOverrideList {

    private final IBakedModel bakedModel;

    public MCH_ItemOverrideList(IBakedModel bakedModel) {
        super(Collections.emptyList());
        this.bakedModel = bakedModel;
    }

    @Nullable
    @Deprecated
    public ResourceLocation applyOverride(@NotNull ItemStack stack, @Nullable World worldIn,
                                          @Nullable EntityLivingBase entityIn) {
        return this.bakedModel.getOverrides().applyOverride(stack, worldIn, entityIn);
    }

    public @NotNull IBakedModel handleItemState(@NotNull IBakedModel originalModel, @NotNull ItemStack stack,
                                                @Nullable World world, @Nullable EntityLivingBase entity) {
        PooledModelParameters.setItemAndUser(stack, entity);
        return this.bakedModel.getOverrides().handleItemState(originalModel, stack, world, entity);
    }

    public @NotNull ImmutableList<ItemOverride> getOverrides() {
        return this.bakedModel.getOverrides().getOverrides();
    }
}
