package com.norwood.mcheli.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public abstract class W_TickHandler implements ITickHandler {

    protected final Minecraft mc;

    public W_TickHandler(Minecraft m) {
        this.mc = m;
    }

    public void onPlayerTickPre(EntityPlayer player) {}

    public void onPlayerTickPost(EntityPlayer player) {}

    public void onRenderTickPre(float partialTicks) {}

    public void onRenderTickPost(float partialTicks) {}

    public void onTickPre() {}

    public void onTickPost() {}

    @SubscribeEvent
    public void onPlayerTickEvent(PlayerTickEvent event) {
        if (event.phase == Phase.START) {
            this.onPlayerTickPre(event.player);
        }

        if (event.phase == Phase.END) {
            this.onPlayerTickPost(event.player);
        }
    }

    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            this.onTickPre();
        }

        if (event.phase == Phase.END) {
            this.onTickPost();
        }
    }

    @SubscribeEvent
    public void onRenderTickEvent(RenderTickEvent event) {
        if (event.phase == Phase.START) {
            this.onRenderTickPre(event.renderTickTime);
        }

        if (event.phase == Phase.END) {
            this.onRenderTickPost(event.renderTickTime);
        }
    }

    enum TickType {
        RENDER,
        CLIENT
    }
}
