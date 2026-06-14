package com.norwood.mcheli.wingman.client;

import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.handler.ClientAutopilotHandler;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Client-only registration for the Wingman subsystem. In 1.12.2 both
 * {@link ModelRegistryEvent} and {@code RegistryEvent.Register} fire on the
 * Forge event bus, so this {@link Mod.EventBusSubscriber} (CLIENT side) receives
 * the model event after the marker block has been registered in PreInit.
 */
@Mod.EventBusSubscriber(modid = "mcheli", value = Side.CLIENT)
public final class WingmanClientReg {

    private static boolean handlersRegistered = false;

    private WingmanClientReg() {}

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        if (McHeliWingman.MARKER_BLOCK != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(McHeliWingman.MARKER_BLOCK), 0,
                    new ModelResourceLocation("mcheli:wingman_marker", "inventory"));
        }

        // ModelRegistryEvent fires once during client startup — a convenient point
        // to wire up the client-side keybind and per-tick handlers.
        if (!handlersRegistered) {
            handlersRegistered = true;
            WingmanKeyHandler.registerClient();
            MinecraftForge.EVENT_BUS.register(new WingmanKeyHandler());
            MinecraftForge.EVENT_BUS.register(new ClientAutopilotHandler());
        }
    }
}
