package com.norwood.mcheli.helper.client;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.google.common.collect.Maps;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.client.model.MCH_BakedModel;
import com.norwood.mcheli.helper.client.renderer.item.CustomItemStackRenderer;
import com.norwood.mcheli.helper.client.renderer.item.IItemModelRenderer;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.helper.info.IItemContent;

@EventBusSubscriber(
                    modid = "mcheli",
                    value = { Side.CLIENT })
public class MCH_ItemModelRenderers {

    private static final Map<ModelResourceLocation, IItemModelRenderer> renderers = Maps.newHashMap();

    @SubscribeEvent
    static void onModelRegistryEvent(ModelRegistryEvent event) {
        registerModelLocation(Item.getItemFromBlock(MCH_MOD.blockDraftingTable));
        ModelLoader.setCustomStateMapper(MCH_MOD.blockDraftingTable, blockIn -> Maps.newHashMap());
        ModelLoader.setCustomStateMapper(MCH_MOD.blockDraftingTableLit, blockIn -> Maps.newHashMap());
        registerModelLocation(MCH_MOD.itemSpawnGunnerVsMonster);
        registerModelLocation(MCH_MOD.itemSpawnGunnerVsPlayer);
        registerModelLocation(MCH_MOD.itemRangeFinder);
        registerModelLocation(MCH_MOD.itemWrench);
        registerModelLocation(MCH_MOD.itemFuel);
        registerModelLocation(MCH_MOD.itemGLTD);
        registerModelLocation(MCH_MOD.itemChain);
        registerModelLocation(MCH_MOD.itemParachute);
        registerModelLocation(MCH_MOD.itemContainer);

        for (int i = 0; i < MCH_MOD.itemUavStation.length; i++) {
            registerModelLocation(MCH_MOD.itemUavStation[i]);
        }

        registerModelLocation(MCH_MOD.invisibleItem);
        registerModelLocation(MCH_MOD.itemStingerBullet);
        registerModelLocation(MCH_MOD.itemJavelinBullet);
        registerModelLocation(MCH_MOD.itemStinger);
        registerModelLocation(MCH_MOD.itemJavelin);
        ContentRegistries.heli().forEachValue(MCH_ItemModelRenderers::registerLegacyModelLocation);
        ContentRegistries.plane().forEachValue(MCH_ItemModelRenderers::registerLegacyModelLocation);
        ContentRegistries.tank().forEachValue(MCH_ItemModelRenderers::registerLegacyModelLocation);
        ContentRegistries.vehicle().forEachValue(MCH_ItemModelRenderers::registerLegacyModelLocation);
        ContentRegistries.ship().forEachValue(MCH_ItemModelRenderers::registerLegacyModelLocation);
        ContentRegistries.throwable().forEachValue(MCH_ItemModelRenderers::registerLegacyModelLocation);
        MCH_Lib.Log("Register models");
        MCH_MOD.proxy.registerModels();
    }

    @SubscribeEvent
    static void onBakedModelEvent(ModelBakeEvent event) {
        for (Entry<ModelResourceLocation, IItemModelRenderer> entry : renderers.entrySet()) {
            IBakedModel bakedmodel = event.getModelRegistry().getObject(entry.getKey());
            if (bakedmodel != null) {
                event.getModelRegistry().putObject(entry.getKey(), new MCH_BakedModel(bakedmodel, entry.getValue()));
            }
        }
    }

    public static void registerRenderer(Item item, IItemModelRenderer renderer) {
        item.setTileEntityItemStackRenderer(CustomItemStackRenderer.getInstance());
        renderers.put(getInventoryModel(item), renderer);
    }

    public static void registerModelLocation(Item item) {
        registerModelLocation(item, 0);
    }

    public static void registerModelLocation(Item item, int meta) {
        ModelLoader.setCustomModelResourceLocation(item, meta, getInventoryModel(item));
    }

    public static void registerLegacyModelLocation(IItemContent content) {
        ModelLoader.setCustomModelResourceLocation(content.getItem(), 0,
                new ModelResourceLocation(content.getItem().getRegistryName(), "mcheli_legacy"));
    }

    private static ModelResourceLocation getInventoryModel(Item item) {
        return new ModelResourceLocation(item.getRegistryName(), "inventory");
    }

    @Nullable
    public static IItemModelRenderer getRenderer(Item item) {
        return renderers.get(getInventoryModel(item));
    }
}
