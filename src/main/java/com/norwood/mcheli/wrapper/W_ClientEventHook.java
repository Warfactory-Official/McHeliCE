package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class W_ClientEventHook {

    @SubscribeEvent
    public void onEvent_MouseEvent(MouseEvent event) {
        this.mouseEvent(event);
    }

    public void mouseEvent(MouseEvent event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventSpecialsPre(Pre<EntityLivingBase> event) {
        this.renderLivingEventSpecialsPre(event);
    }

    public void renderLivingEventSpecialsPre(Pre<EntityLivingBase> event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventSpecialsPost(Post<EntityLivingBase> event) {
        this.renderLivingEventSpecialsPost(event);
    }

    public void renderLivingEventSpecialsPost(Post<EntityLivingBase> event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventPre(net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityLivingBase> event) {
        this.renderLivingEventPre(event);
    }

    public void renderLivingEventPre(net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityLivingBase> event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventPost(net.minecraftforge.client.event.RenderLivingEvent.Post<EntityLivingBase> event) {
        this.renderLivingEventPost(event);
    }

    public void renderLivingEventPost(net.minecraftforge.client.event.RenderLivingEvent.Post<EntityLivingBase> event) {
    }

    @SubscribeEvent
    public void onEvent_renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        this.renderPlayerPre(event);
    }

    public void renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
    }

    @SubscribeEvent
    public void Event_renderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
        this.renderPlayerPost(event);
    }

    public void renderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
    }

    @SubscribeEvent
    public void onEvent_WorldEventUnload(Unload event) {
        this.worldEventUnload(event);
    }

    public void worldEventUnload(Unload event) {
    }

    @SubscribeEvent
    public void onEvent_EntityJoinWorldEvent(EntityJoinWorldEvent event) {
        this.entityJoinWorldEvent(event);
    }

    public void entityJoinWorldEvent(EntityJoinWorldEvent event) {
    }


    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if(!MCH_MOD.DEBUG_LD) return;
        World world = event.getWorld();
        if (!world.isRemote) return;
        //Fix entities on clientside not being interactable if player enters previously unloaded chunk on which a far-rendered vehicle resides
        Chunk chunk = event.getChunk();

        int chunkX = chunk.x;
        int chunkZ = chunk.z;

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;


        List<W_Entity> entities = world.getEntities(W_Entity.class,  e -> (e != null) && (e.chunkCoordX != chunkX || e.chunkCoordZ != chunkZ));

        for (W_Entity e : entities) {
                reinsertEntityIntoChunk(chunk, e);
        }

    }

    private void reinsertEntityIntoChunk(Chunk chunk, Entity entity) {
        entity.chunkCoordX = chunk.x;
        entity.chunkCoordZ = chunk.z;

        int slice = MathHelper.clamp(
                (int) (entity.posY / 16),
                0,
                chunk.getEntityLists().length - 1
        );

        var subchunk = chunk.getEntityLists()[slice];

        if (!subchunk.contains(entity))
            subchunk.add(entity);
    }
}
