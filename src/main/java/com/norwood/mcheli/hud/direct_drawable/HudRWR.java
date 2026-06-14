package com.norwood.mcheli.hud.direct_drawable;

import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * Radar Warning Receiver (RWR) HUD.
 * Ported from MCHeli Reforged and integrated into CE's DirectDrawable system.
 */
public class HudRWR implements DirectDrawable {
    public static final HudRWR INSTANCE = new HudRWR();

    public static final ResourceLocation RWR = new ResourceLocation(Tags.MODID, "textures/rwr.png");
    public static final ResourceLocation RWR_HELI = new ResourceLocation(Tags.MODID, "textures/rwr_heli.png");
    public static final ResourceLocation RWR_TANK = new ResourceLocation(Tags.MODID, "textures/rwr_tank.png");
    public static final ResourceLocation RWR_FAC = new ResourceLocation(Tags.MODID, "textures/rwr_fac.png");


    private static final int _RWR_SIZE = 180;
    private static final int _RWR_CENTER_X = 100;
    private static final int _RWR_CENTER_Y = 280;
    private static final double SCREEN_HEIGHT_ADAPT_CONSTANT = 520;
    private static final double _MIN_DISTANCE = 50.0;
    private static final double _RWR_RING_MAX_DISTANCE = 4096.0;
    private static final int RWR_MSL_BLINK_TICK = 3;

    @Override
    public void renderHud(RenderGameOverlayEvent.Post event, Tuple<EntityPlayer, MCH_EntityAircraft> ctx) {
        if (!DirectDrawable.shouldRender(event)) return;
        Minecraft mc = Minecraft.getMinecraft();
        MCH_EntityAircraft ac = ctx.getSecond();
        if (ac.getAcInfo() == null || !ac.getAcInfo().hasRWR) return;

        ScaledResolution sc = new ScaledResolution(mc);
        int rwrSize = _RWR_SIZE;
        int centerX = _RWR_CENTER_X;
        int centerY = _RWR_CENTER_Y;
        double minDistance = _MIN_DISTANCE;
        double maxDistance = _RWR_RING_MAX_DISTANCE;

        ResourceLocation texture = RWR;
        if (ac instanceof MCH_EntityPlane) {
            texture = RWR;
            if (ac.getAcInfo().isFloat) {
                texture = RWR_FAC;
                rwrSize = 160; centerX = 220; centerY = 370; minDistance = 15; maxDistance = 800;
            }
        } else if (ac instanceof MCH_EntityHeli) {
            texture = RWR_HELI;
        } else if (ac instanceof MCH_EntityTank || ac instanceof MCH_EntityVehicle) {
            texture = (ac instanceof MCH_EntityTank) ? RWR_TANK : RWR_FAC;
            rwrSize = 160; centerX = 220; centerY = 370; minDistance = 15; maxDistance = 800;
        }

        double sx = sc.getScaledHeight() * (centerX / SCREEN_HEIGHT_ADAPT_CONSTANT);
        double sy = sc.getScaledHeight() * (centerY / SCREEN_HEIGHT_ADAPT_CONSTANT);

        GlStateManager.pushMatrix();
        drawRWRCircle(sx, sy, sc, texture, rwrSize);
        renderThreatRing(mc, sc, ac, sx, sy, rwrSize, minDistance, maxDistance);
        GlStateManager.popMatrix();
    }

    private void drawRWRCircle(double x, double y, ScaledResolution sc, ResourceLocation texture, int size) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        double halfSize = sc.getScaledHeight() * (size / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x - halfSize, y + halfSize, 0.0D).tex(0.0D, 1.0D).endVertex();
        bufferbuilder.pos(x + halfSize, y + halfSize, 0.0D).tex(1.0D, 1.0D).endVertex();
        bufferbuilder.pos(x + halfSize, y - halfSize, 0.0D).tex(1.0D, 0.0D).endVertex();
        bufferbuilder.pos(x - halfSize, y - halfSize, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    private void renderThreatRing(Minecraft mc, ScaledResolution sc, MCH_EntityAircraft ac, double centerX, double centerY, int rwrSize, double minDistance, double maxDistance) {
        double halfSize = sc.getScaledHeight() * (rwrSize / SCREEN_HEIGHT_ADAPT_CONSTANT) * 0.5D;
        double outerRadius = halfSize * 0.88D;
        double innerRadius = Math.max(10.0D, outerRadius * 0.22D);

        if (ac.getAcInfo().radarMaxTargetRange > 0.0F) {
            maxDistance = Math.min(maxDistance, ac.getAcInfo().radarMaxTargetRange);
        }

        List<MCH_RWRThreatEvent> events = MCH_RWRThreatClientTracker.getEvents(ac.getEntityId());
        long nowTick = ac.world.getTotalWorldTime();

        for (MCH_RWRThreatEvent evt : events) {
            if (evt == null || evt.emitterEntityId == ac.getEntityId()) continue;

            String label = evt.sourceName;
            if ("?".equals(label)) continue;

            double distance = (maxDistance - evt.strength * (maxDistance - minDistance));
            distance = Math.max(minDistance, Math.min(maxDistance, distance));
            double rangeNorm = (distance - minDistance) / Math.max(1.0D, maxDistance - minDistance);
            double ringRadius = innerRadius + (outerRadius - innerRadius) * rangeNorm;
            double ang = Math.toRadians(evt.angleDeg - 90.0D);
            double px = centerX + Math.cos(ang) * ringRadius;
            double py = centerY + Math.sin(ang) * ringRadius;

            int color = resolveThreatColor(evt, nowTick);
            drawRadarText(mc, label, px, py, color);
        }
    }

    private int resolveThreatColor(MCH_RWRThreatEvent evt, long nowTick) {
        if (evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_ACTIVE || evt.threatMode == MCH_RWRThreatEvent.MODE_MSL_DATALINK) {
            boolean strong = ((nowTick / RWR_MSL_BLINK_TICK) & 1L) == 0L;
            return strong ? 0xFFFF2D2D : 0xFFCC3030;
        }
        if (evt.threatMode == MCH_RWRThreatEvent.MODE_STT) return 0xFFFF4A4A;
        if (evt.emitterKind == MCH_RWRThreatEvent.EMITTER_AIRCRAFT) return 0xFF00FF00;
        return 0xFFFFE400; // AMBER
    }

    private void drawRadarText(Minecraft mc, String text, double x, double y, int color) {
        float scale = 0.9F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        int tx = -mc.fontRenderer.getStringWidth(text) / 2;
        mc.fontRenderer.drawString(text, tx, 0, color, false);
        GlStateManager.popMatrix();
    }

    @Override
    public DirectDrawable getInstance() {
        return INSTANCE;
    }
}
