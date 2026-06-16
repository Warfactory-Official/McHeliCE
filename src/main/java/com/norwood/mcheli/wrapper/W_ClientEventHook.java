package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.core.MCHCore;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
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
        if (!MCHCore.isRemoveClientTrackingRestrictions()) return;
        World world = event.getWorld();
        if (!world.isRemote) return;
        // A force-tracked W_Entity can be kept alive in loadedEntityList while its chunk is unloaded
        // (the client-unload filter, see MixinWorld). When that chunk loads, the entity must be put
        // into the chunk's entity list or it has no working hitbox / can't be interacted with.
        //
        // CRITICAL: only re-home entities whose ACTUAL position falls in THIS chunk. The old code
        // matched every entity not already tagged for this chunk and shoved them all in here (and
        // rewrote their chunkCoordX/Z), so a single entity ended up referenced by many chunks' entity
        // lists at once while chunkCoordX/Z pointed at just one. On destroy, World.removeEntity only
        // cleans the one chunk it points at, leaving the entity (and its AABB) stuck in all the
        // others — a ghost hitbox that survives destruction. Homing each entity into exactly its own
        // chunk (and clearing any stale membership) keeps removeEntity able to fully clean it.
        Chunk chunk = event.getChunk();
        for (W_Entity e : world.getEntities(W_Entity.class, e -> belongsToChunk(e, chunk.x, chunk.z))) {
            homeEntityInChunk(world, chunk, e);
        }
    }

    /** True when the entity's real position maps to chunk (chunkX, chunkZ). */
    private static boolean belongsToChunk(Entity e, int chunkX, int chunkZ) {
        return e != null && !e.isDead
                && MathHelper.floor(e.posX / 16.0) == chunkX
                && MathHelper.floor(e.posZ / 16.0) == chunkZ;
    }

    /**
     * Ensure {@code entity} is a member of {@code chunk} (its real chunk) and of no other, using the
     * vanilla {@link Chunk#addEntity} so {@code addedToChunk} and {@code chunkCoordX/Y/Z} are set
     * consistently — the exact invariant {@code World.removeEntity} relies on to clean up later.
     */
    private static void homeEntityInChunk(World world, Chunk chunk, Entity entity) {
        // Decide on ACTUAL list membership, not the addedToChunk/chunkCoord flags. A freshly (re)loaded
        // chunk is a brand-new object with an empty entity list, yet a force-tracked entity kept alive
        // across the unload still carries stale flags pointing at these very coords. Trusting the flags
        // would skip the add and leave the entity flagged-as-homed but absent from the list — it renders
        // from far (hitbox visible) but has no collision and goes uninteractable the moment you approach
        // and its chunk reloads. So only skip when it is genuinely in this chunk's list already.
        if (inChunkList(chunk, entity)) {
            return;
        }
        // Drop a stale membership in a different, still-loaded chunk so we never leave a dangling ref.
        if (entity.addedToChunk
                && (entity.chunkCoordX != chunk.x || entity.chunkCoordZ != chunk.z)
                && world.isBlockLoaded(new BlockPos(entity.chunkCoordX << 4, 0, entity.chunkCoordZ << 4))) {
            world.getChunk(entity.chunkCoordX, entity.chunkCoordZ).removeEntity(entity);
        }
        chunk.addEntity(entity); // sets addedToChunk + chunkCoordX/Y/Z and inserts into the right slice
    }

    /** True only if the entity is physically present in one of this chunk's sub-lists (not just flagged). */
    private static boolean inChunkList(Chunk chunk, Entity entity) {
        for (var list : chunk.getEntityLists()) {
            if (list.contains(entity)) {
                return true;
            }
        }
        return false;
    }
}
