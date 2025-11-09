package com.norwood.mcheli.wrapper;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class W_ClientEventHook {

    @SubscribeEvent
    public void onEvent_MouseEvent(MouseEvent event) {
        this.mouseEvent(event);
    }

    public void mouseEvent(MouseEvent event) {}

    @SubscribeEvent
    public void onEvent_renderLivingEventSpecialsPre(Pre<EntityLivingBase> event) {
        this.renderLivingEventSpecialsPre(event);
    }

    public void renderLivingEventSpecialsPre(Pre<EntityLivingBase> event) {}

    @SubscribeEvent
    public void onEvent_renderLivingEventSpecialsPost(Post<EntityLivingBase> event) {
        this.renderLivingEventSpecialsPost(event);
    }

    public void renderLivingEventSpecialsPost(Post<EntityLivingBase> event) {}

    @SubscribeEvent
    public void onEvent_renderLivingEventPre(net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityLivingBase> event) {
        this.renderLivingEventPre(event);
    }

    public void renderLivingEventPre(net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityLivingBase> event) {}

    @SubscribeEvent
    public void onEvent_renderLivingEventPost(net.minecraftforge.client.event.RenderLivingEvent.Post<EntityLivingBase> event) {
        this.renderLivingEventPost(event);
    }

    public void renderLivingEventPost(net.minecraftforge.client.event.RenderLivingEvent.Post<EntityLivingBase> event) {}

    @SubscribeEvent
    public void onEvent_renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        this.renderPlayerPre(event);
    }

    public void renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {}

    @SubscribeEvent
    public void Event_renderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
        this.renderPlayerPost(event);
    }

    public void renderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {}

    @SubscribeEvent
    public void onEvent_WorldEventUnload(Unload event) {
        this.worldEventUnload(event);
    }

    public void worldEventUnload(Unload event) {}

    @SubscribeEvent
    public void onEvent_EntityJoinWorldEvent(EntityJoinWorldEvent event) {
        this.entityJoinWorldEvent(event);
    }

    public void entityJoinWorldEvent(EntityJoinWorldEvent event) {}
}
