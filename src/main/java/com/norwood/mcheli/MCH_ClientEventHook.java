package com.norwood.mcheli;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.helper.entity.ITargetMarkerObject;
import com.norwood.mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import com.norwood.mcheli.wrapper.W_ClientEventHook;
import com.norwood.mcheli.wrapper.W_Reflection;

public class MCH_ClientEventHook extends W_ClientEventHook {

    public static final List<MCH_EntityAircraft> haveSearchLightAircraft = new ArrayList<>();
    private static final ResourceLocation ir_strobe = new ResourceLocation(Tags.MODID, "textures/ir_strobe.png");
    private static boolean cancelRender = true;
    MCH_TextureManagerDummy dummyTextureManager = null;
    private RenderPlayerEvent.Pre event;

    public static void setCancelRender(boolean cancel) {
        cancelRender = cancel;
    }

    @Override
    public void renderLivingEventSpecialsPre(Pre<EntityLivingBase> event) {
        if (MCH_Config.DisableRenderLivingSpecials.prmBool) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(Minecraft.getMinecraft().player);
            if (ac != null && ac.isMountedEntity(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }

    private void renderIRStrobe(EntityLivingBase entity, Post<EntityLivingBase> event) {
        int cm = MCH_ClientCommonTickHandler.cameraMode;
        if (cm != 0) {
            int ticks = entity.ticksExisted % 20;
            if (ticks < 4) {
                float alpha = ticks != 2 && ticks != 1 ? 0.5F : 1.0F;
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    if (player.isOnSameTeam(entity)) {
                        int j = 240;
                        int k = 240;
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
                        RenderManager rm = event.getRenderer().getRenderManager();
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        float f1 = 0.080000006F;
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(event.getX(), event.getY() + (float) (entity.height * 0.75),
                                event.getZ());
                        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                        GlStateManager.scale(-f1, -f1, f1);
                        GlStateManager.enableBlend();
                        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                        GlStateManager.enableTexture2D();
                        rm.renderEngine.bindTexture(ir_strobe);
                        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);

                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder builder = tessellator.getBuffer();
                        builder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                        int i = (int) Math.max(entity.width, entity.height) * 20;
                        builder.pos(-i, -i, 0.1).tex(0.0, 0.0).color(1.0F, 1.0F, 1.0F, alpha * (cm == 1 ? 0.9F : 0.5F))
                                .endVertex();
                        builder.pos(-i, i, 0.1).tex(0.0, 1.0).color(1.0F, 1.0F, 1.0F, alpha * (cm == 1 ? 0.9F : 0.5F))
                                .endVertex();
                        builder.pos(i, i, 0.1).tex(1.0, 1.0).color(1.0F, 1.0F, 1.0F, alpha * (cm == 1 ? 0.9F : 0.5F))
                                .endVertex();
                        builder.pos(i, -i, 0.1).tex(1.0, 0.0).color(1.0F, 1.0F, 1.0F, alpha * (cm == 1 ? 0.9F : 0.5F))
                                .endVertex();
                        tessellator.draw();
                        GlStateManager.enableLighting();
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
    }

    @Override
    public void mouseEvent(MouseEvent event) {
        if (MCH_ClientTickHandlerBase.updateMouseWheel(event.getDwheel())) {
            event.setCanceled(true);
        }
    }

    @Override
    public void renderLivingEventPre(net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityLivingBase> event) {
        for (MCH_EntityAircraft ac : haveSearchLightAircraft) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                    ac.getSearchLightValue(event.getEntity()), 240.0F);
        }

        if (!MCH_Config.EnableModEntityRender.prmBool || !cancelRender ||
                !(event.getEntity().getRidingEntity() instanceof MCH_EntityAircraft) &&
                        !(event.getEntity().getRidingEntity() instanceof MCH_EntitySeat)) {
            if (MCH_Config.EnableReplaceTextureManager.prmBool) {
                RenderManager rm = W_Reflection.getRenderManager(event.getRenderer());
                if (!(rm.renderEngine instanceof MCH_TextureManagerDummy)) {
                    if (this.dummyTextureManager == null) {
                        this.dummyTextureManager = new MCH_TextureManagerDummy(rm.renderEngine);
                    }

                    rm.renderEngine = this.dummyTextureManager;
                }
            }
        } else {
            event.setCanceled(true);
        }
    }

    @Override
    public void renderLivingEventPost(net.minecraftforge.client.event.RenderLivingEvent.Post<EntityLivingBase> event) {
        MCH_RenderAircraft.renderEntityMarker(event.getEntity());
        if (event.getEntity() instanceof ITargetMarkerObject) {
            MCH_GuiTargetMarker.addMarkEntityPos(
                    2, (ITargetMarkerObject) event.getEntity(), event.getX(),
                    event.getY() + event.getEntity().height + 0.5, event.getZ());
        } else {
            MCH_GuiTargetMarker.addMarkEntityPos(
                    2, ITargetMarkerObject.fromEntity(event.getEntity()), event.getX(),
                    event.getY() + event.getEntity().height + 0.5, event.getZ());
        }

        MCH_ClientLightWeaponTickHandler.markEntity(event.getEntity(), event.getX(),
                event.getY() + event.getEntity().height / 2.0F, event.getZ());
    }

    @Override
    public void renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        this.event = event;
        if (event.getEntity() != null) {
            if (event.getEntity().getRidingEntity() instanceof MCH_EntityAircraft riding) {
                if (riding.getAcInfo() != null && riding.getAcInfo().hideEntity) {
                    event.setCanceled(true);
                    return;
                }
            }

        }
    }

    @Override
    public void entityJoinWorldEvent(EntityJoinWorldEvent event) {
        if (event.getEntity().isEntityEqual(MCH_Lib.getClientPlayer())) {
            MCH_Lib.DbgLog(true, "MCH_ClientEventHook.entityJoinWorldEvent : " + event.getEntity());
            MCH_ItemRangeFinder.mode = Minecraft.getMinecraft().isSingleplayer() ? 1 : 0;
            MCH_ParticlesUtil.clearMarkPoint();
        }
    }
}
