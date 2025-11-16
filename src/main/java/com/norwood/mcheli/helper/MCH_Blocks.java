package com.norwood.mcheli.helper;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;

@EventBusSubscriber(
                    modid = "mcheli")
public class MCH_Blocks {

    private static final Set<Block> registryWrapper = Sets.newLinkedHashSet();

    @SubscribeEvent
    static void onBlockRegisterEvent(Register<Block> event) {
        for (Block block : registryWrapper) {
            event.getRegistry().register(block);
        }
    }

    public static void register(Block block, String name) {
        registryWrapper.add(block.setRegistryName(MCH_Utils.suffix(name)));
    }
}
