package com.norwood.mcheli.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Keeps the chunk renderer in sync when the camera (render-view entity) is DETACHED from the player
 * body — piloting a far UAV, or flying a TV missile.
 *
 * <p>The client render grid ({@code ViewFrustum}) only schedules a chunk's VBO rebuild when that
 * chunk's data arrives (or a block changes) while the grid is centered nearby. Re-centering the grid
 * ({@code ViewFrustum.updateChunkPositions} / {@code RenderChunk.setPosition}) repositions the render
 * chunks but does <b>not</b> re-dirty them. So chunks streamed around a detached view origin render
 * stale or empty — "only updating when a block is modified" — until a full reload. That is the
 * manual {@code loadRenderers()} workaround; this does it automatically and cheaply:
 *
 * <ul>
 *   <li>on a detach / re-attach / view-origin swap: mark the whole render box around the new origin
 *       for a render update (one call; no GL-resource realloc, unlike {@code loadRenderers});</li>
 *   <li>while detached and the origin crosses a chunk boundary: mark only the newly-entered chunk
 *       ring (what walking does implicitly). Run at {@code RenderTickEvent.END}, after the world pass
 *       has already centered the grid on the origin, so the marks land on the correct grid slots.</li>
 * </ul>
 *
 * <p>"Detached" means the render-view entity is not the player <i>and</i> is well away from the body
 * (a normal third-person vehicle camera sits on top of the player and needs none of this).
 */
@SideOnly(Side.CLIENT)
public class MCH_DetachedViewRefresh {

    /** Beyond this horizontal distance (blocks) from the body, the view counts as detached. */
    private static final double DETACH_DIST_SQ = 48.0 * 48.0;

    private Entity lastViewEntity;
    private int lastChunkX;
    private int lastChunkZ;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.renderGlobal == null) {
            this.lastViewEntity = null;
            return;
        }

        Entity view = mc.getRenderViewEntity();
        int r = mc.gameSettings.renderDistanceChunks;

        double dx = view == null ? 0.0 : view.posX - mc.player.posX;
        double dz = view == null ? 0.0 : view.posZ - mc.player.posZ;
        boolean detached = view != null && view != mc.player && (dx * dx + dz * dz) > DETACH_DIST_SQ;

        if (!detached) {
            // Re-attached this frame: the body's own surroundings may be stale from the detached
            // period (the grid was centered on the UAV), so refresh the box around the player once.
            if (this.lastViewEntity != null) {
                markBox(mc.renderGlobal, MathHelper.floor(mc.player.posX) >> 4,
                        MathHelper.floor(mc.player.posZ) >> 4, r);
                this.lastViewEntity = null;
            }
            return;
        }

        int cx = MathHelper.floor(view.posX) >> 4;
        int cz = MathHelper.floor(view.posZ) >> 4;

        if (view != this.lastViewEntity) {
            // Just detached, or swapped to a different view origin (e.g. a new UAV / missile).
            markBox(mc.renderGlobal, cx, cz, r);
        } else if (cx != this.lastChunkX || cz != this.lastChunkZ) {
            if (Math.abs(cx - this.lastChunkX) > 2 * r + 1 || Math.abs(cz - this.lastChunkZ) > 2 * r + 1) {
                markBox(mc.renderGlobal, cx, cz, r); // jumped further than the grid — refresh all
            } else {
                markRing(mc.renderGlobal, this.lastChunkX, this.lastChunkZ, cx, cz, r);
            }
        }

        this.lastViewEntity = view;
        this.lastChunkX = cx;
        this.lastChunkZ = cz;
    }

    /** Mark the full render box (chunk radius {@code r}) around chunk (cx,cz) for a render update. */
    private static void markBox(RenderGlobal rg, int cx, int cz, int r) {
        rg.markBlockRangeForRenderUpdate(
                (cx - r) << 4, 0, (cz - r) << 4,
                ((cx + r) << 4) + 15, 255, ((cz + r) << 4) + 15);
    }

    /** Mark only the chunks that entered the render box when the origin moved (px,pz) -> (cx,cz). */
    private static void markRing(RenderGlobal rg, int px, int pz, int cx, int cz, int r) {
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                if (Math.abs(x - px) > r || Math.abs(z - pz) > r) { // outside the previous box
                    rg.markBlockRangeForRenderUpdate(x << 4, 0, z << 4, (x << 4) + 15, 255, (z << 4) + 15);
                }
            }
        }
    }
}
