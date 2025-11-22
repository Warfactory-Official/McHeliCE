package com.norwood.mcheli.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class EntityRendererHook {
    private static final MethodHandle getFOVModifierHandle;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            // mlbv: somehow AccessTransformer doesn't work for this
            Method getFOVModifierMethod = EntityRenderer.class.getMethod(MCHCore.runtimeDeobfEnabled() ? "func_78481_a" : "getFOVModifier", float.class, boolean.class);
            getFOVModifierMethod.setAccessible(true);
            getFOVModifierHandle = lookup.unreflect(getFOVModifierMethod);
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind getFOVModifier", e);
        }
    }

    public static void renderFarPass(EntityRenderer renderer, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.anaglyph) return;

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        float far = 3000.0F;
        float fov;
        try {
            fov = (float) getFOVModifierHandle.invokeExact(renderer, partialTicks, true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
        Project.gluPerspective(fov, aspect, 0.05F, far);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();

        ICamera superCamera = new Frustum();
        Entity viewEntity = mc.getRenderViewEntity();
        double d0 = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
        double d1 = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
        double d2 = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;
        superCamera.setPosition(d0, d1, d2);

        GlStateManager.disableFog();
        double renderDist = mc.gameSettings.renderDistanceChunks * 16.0;
        double distThresholdSq = renderDist * renderDist;

        for (Entity e : mc.world.loadedEntityList) {
            if (TrackerHook.shouldForceWatch(e)) {
                if (e.getDistanceSq(viewEntity) > distThresholdSq) {
                    if (superCamera.isBoundingBoxInFrustum(e.getEntityBoundingBox())) {
                        mc.getRenderManager().renderEntityStatic(e, partialTicks, false);
                    }
                }
            }
        }

        GlStateManager.enableFog();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
    }
}
