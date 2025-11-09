package com.norwood.mcheli.core;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

import com.google.common.base.Predicates;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;

// @SideOnly(Side.CLIENT)
@SuppressWarnings("unused")
public class VehicleRenderHook {

    public static final VehicleRenderHook INSTANCE = new VehicleRenderHook();
    public static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    public void renderVehicles(Entity renderViewEntity, ICamera camera, float partialTicks) {
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        if (pass != 0) return;
        GlStateManager.enableLighting();
        MINECRAFT.entityRenderer.enableLightmap();
        List<MCH_EntityAircraft> list = MINECRAFT.world.getEntities(MCH_EntityAircraft.class, Predicates.alwaysTrue());
        for (Entity entity : list) {
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
            float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            double d3 = renderViewEntity.lastTickPosX +
                    (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double) partialTicks;
            double d4 = renderViewEntity.lastTickPosY +
                    (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double) partialTicks;
            double d5 = renderViewEntity.lastTickPosZ +
                    (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double) partialTicks;
            Render<Entity> render = MINECRAFT.getRenderManager().getEntityRenderObject(entity);
            render.doRender(entity, d0 - d3, d1 - d4, d2 - d5, f, partialTicks);
        }
        MINECRAFT.entityRenderer.disableLightmap();
    }
}
