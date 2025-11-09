package com.norwood.mcheli.helper;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

import com.google.common.collect.Sets;

@ObjectHolder("mcheli")
@EventBusSubscriber(
                    modid = "mcheli")
public class MCH_Items {

    private static final Set<Item> registryWrapper = Sets.newLinkedHashSet();

    @SubscribeEvent
    static void onItemRegistryEvent(Register<Item> event) {
        for (Item item : registryWrapper) {
            event.getRegistry().register(item);
        }
    }

    public static void register(Item item, String name) {
        registryWrapper.add(item.setRegistryName(MCH_Utils.suffix(name)));
    }

    public static void registerBlock(Block block) {
        registryWrapper.add(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @Nullable
    public static Item get(String name) {
        return ForgeRegistries.ITEMS.getValue(MCH_Utils.suffix(name));
    }

    public static String getName(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).toString();
    }
}
