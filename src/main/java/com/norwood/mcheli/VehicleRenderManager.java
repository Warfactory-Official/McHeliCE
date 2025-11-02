package com.norwood.mcheli;

import com.google.common.base.Predicates;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.List;

@Mod.EventBusSubscriber(modid = MCH_MOD.MOD_ID)
public class VehicleRenderManager {

    Minecraft minecraft = Minecraft.getMinecraft();
    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderVehicles(RenderWorldLastEvent event) {

        List<MCH_EntityAircraft> list = minecraft.world.getEntities(MCH_EntityAircraft.class, Predicates.alwaysTrue());

        for (Entity entity : list) {
            float partialTicks = event.getPartialTicks();
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
            float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            Entity playerCamera = minecraft.getRenderViewEntity();
            double d3 = playerCamera.lastTickPosX + (playerCamera.posX - playerCamera.lastTickPosX) * (double) partialTicks;
            double d4 = playerCamera.lastTickPosY + (playerCamera.posY - playerCamera.lastTickPosY) * (double) partialTicks;
            double d5 = playerCamera.lastTickPosZ + (playerCamera.posZ - playerCamera.lastTickPosZ) * (double) partialTicks;


            Render<Entity> render = minecraft.getRenderManager().getEntityRenderObject(entity);
            GlStateManager.enableLighting();
            GlStateManager.enableColorMaterial();
            GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
            GlStateManager.enableRescaleNormal();

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);


            minecraft.entityRenderer.enableLightmap();



            render.doRender(entity, d0 - d3, d1 - d4, d2 - d5, f, partialTicks);
            minecraft.entityRenderer.disableLightmap();

        }
    }
}
