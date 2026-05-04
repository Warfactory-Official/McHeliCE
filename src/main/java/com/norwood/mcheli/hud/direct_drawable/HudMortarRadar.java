package com.norwood.mcheli.hud.direct_drawable;

import com.norwood.mcheli.EntityInfo;
import com.norwood.mcheli.MCH_EntityInfoClientTracker;
import com.norwood.mcheli.RWRType;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class HudMortarRadar implements DirectDrawable {

    public static final HudMortarRadar INSTANCE = new HudMortarRadar();
    public static final ResourceLocation RADAR = new ResourceLocation(Tags.MODID, "textures/gui/mortar_radar.png");
    public static final ResourceLocation CROSS = new ResourceLocation(Tags.MODID, "textures/gui/mortar_cross.png");
    public static final ResourceLocation TARGET = new ResourceLocation(Tags.MODID, "textures/gui/mortar_target.png");
    private static final int RWR_SIZE = 250;
    private static final int RWR_CENTER_X = 150;
    private static final int RWR_CENTER_Y = 280;
    private static final double SCREEN_HEIGHT_ADAPT_CONSTANT = 520.0;
    private static final double MIN_DISTANCE = 20.0;
    private static final double MAX_DISTANCE = 300.0;
    private static final int MIN_RADIUS = 0;
    private static final int RWR_CROSS_SIZE = 21;

    public void renderHud(RenderGameOverlayEvent.Post event, Tuple<EntityPlayer, MCH_EntityAircraft> ctx) {
        if (!DirectDrawable.shouldRender(event)) return;
        var player = ctx.getFirst();
        var ac = ctx.getSecond();
        var mc = Minecraft.getMinecraft();
        var sc = new ScaledResolution(mc);

        double maxDist = MAX_DISTANCE;
        double currentDist = -1.0;

        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        if (ws != null) {
            MCH_WeaponInfo wi = ws.getInfo();
            if (wi.mortarRadarMaxDist > 0) maxDist = wi.mortarRadarMaxDist;
            if (wi.displayMortarDistance) currentDist = ac.getLandInDistance(player);
        }

        double interpDist = ac.prevLandInDistance + (currentDist - ac.prevLandInDistance) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        double sx = sc.getScaledHeight_double() * (RWR_CENTER_X / SCREEN_HEIGHT_ADAPT_CONSTANT);
        double sy = sc.getScaledHeight_double() * (RWR_CENTER_Y / SCREEN_HEIGHT_ADAPT_CONSTANT);
        drawRWRCircle(sx, sy, sc, RADAR);

        double circleRadius = sc.getScaledHeight_double() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
        boolean isDeadOn = false;

        double pax = interpolate(ac.posX, ac.prevPosX, event.getPartialTicks());
        double pay = interpolate(ac.posY, ac.prevPosY, event.getPartialTicks());
        double paz = interpolate(ac.posZ, ac.prevPosZ, event.getPartialTicks());

        float yaw = ac.prevRotationYaw + (ac.rotationYaw - ac.prevRotationYaw) * event.getPartialTicks();
        if (ws != null) {
            float wsYaw = ws.getPrevYaw() + (ws.getYaw() - ws.getPrevYaw()) * event.getPartialTicks();
            yaw += wsYaw + ws.getCurrentWeapon().fixRotationYaw;
        }

        float radYaw = (float) Math.toRadians(yaw);
        Vec3d lookH = new Vec3d(MathHelper.sin(-radYaw), 0, MathHelper.cos(radYaw));

        for (EntityInfo entity : getServerLoadedEntity()) {
            if (!isValidEntity(entity, ac)) continue;

            double xPos = interpolate(entity.x, entity.prevX, event.getPartialTicks());
            double yPos = interpolate(entity.y, entity.prevY, event.getPartialTicks());
            double zPos = interpolate(entity.z, entity.prevZ, event.getPartialTicks());

            Vec3d delta = new Vec3d(xPos - pax, yPos - pay, zPos - paz);
            Vec3d deltaH = new Vec3d(delta.x, 0, delta.z).normalize();

            double dot = lookH.dotProduct(deltaH);
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
            // Check cross product to determine left/right
            if ((lookH.x * deltaH.z - lookH.z * deltaH.x) > 0) angle = -angle;

            double distance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

            // High precision hit detection
            if (Math.abs(angle) < 3.0 && Math.abs(distance - interpDist) < 5.0) {
                isDeadOn = true;
            }

            double ratio = MathHelper.clamp((distance - MIN_DISTANCE) / (maxDist - MIN_DISTANCE), 0.0, 1.0);
            double renderRadius = MIN_RADIUS + (circleRadius - MIN_RADIUS) * ratio;

            double rad = Math.toRadians(angle);
            double markerX = sx + renderRadius * Math.sin(-rad);
            double markerY = sy - renderRadius * Math.cos(rad);

            drawMortarTarget(markerX, markerY, sc);
             HudRWR.RWRResult rwrResult = getTargetTypeOnRadar(entity, ac);
            String text = rwrResult.name + "[" + (int) distance + "]";
            int color = rwrResult.color;
            int fw = mc.fontRenderer.getStringWidth(text);
            mc.fontRenderer.drawString(text, (int) (markerX - fw / 2.0), (int) (markerY + 10.0), color, true);
        }
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        if (interpDist >= MIN_DISTANCE) {
            double ratio = MathHelper.clamp((interpDist - MIN_DISTANCE) / (maxDist - MIN_DISTANCE), 0.0, 1.0);
            double circleR = sc.getScaledHeight_double() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
            double renderR = MIN_RADIUS + (circleR - MIN_RADIUS) * ratio;
            double markerX = sx;
            double markerY = sy - renderR;

            float r = isDeadOn ? 1.0F : 1.0F;
            float g = isDeadOn ? 0.0F : 1.0F;
            float b = isDeadOn ? 0.0F : 1.0F;
            drawMortarCross(markerX, markerY, sc, r, g, b);
        }
        GlStateManager.popMatrix();
    }


    private boolean isValidEntity(EntityInfo entity, MCH_EntityAircraft ac) {
        if (entity.entityClassName.contains("MCH_EntityChaff") || entity.entityClassName.contains("MCH_EntityFlare"))
            return false;
        double dx = entity.x - ac.posX;
        double dz = entity.z - ac.posZ;
        return (dx * dx + dz * dz) >= MIN_DISTANCE * MIN_DISTANCE;
    }

    private HudRWR.RWRResult getTargetTypeOnRadar(EntityInfo entity, MCH_EntityAircraft ac) {
        if (ac.getAcInfo().rwrType == RWRType.DIGITAL) {
            if (entity.entityClassName.contains("MCH_EntityHeli") ||
                    entity.entityClassName.contains("MCP_EntityPlane") ||
                    entity.entityClassName.contains("MCH_EntityTank") ||
                    entity.entityClassName.contains("MCH_EntityVehicle")) {
                return new HudRWR.RWRResult(ac.getNameOnMyRadar(entity), 0xFFFFFF);
            } else return new HudRWR.RWRResult("?", 0xFFFFFF);
        }
        return new HudRWR.RWRResult("?", 0xFFFFFF);
    }

    private void drawRWRCircle(double x, double y, ScaledResolution sc, ResourceLocation rwr) {
        prepareRenderState(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rwr);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        double half = sc.getScaledHeight_double() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x - half, y + half, 0).tex(0, 1).endVertex();
        buf.pos(x + half, y + half, 0).tex(1, 1).endVertex();
        buf.pos(x + half, y - half, 0).tex(1, 0).endVertex();
        buf.pos(x - half, y - half, 0).tex(0, 0).endVertex();
        tess.draw();
        restoreRenderState();
    }

    private void drawMortarCross(double x, double y, ScaledResolution sc, float r, float g, float b) {
        prepareRenderState(r, g, b, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(CROSS);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        double half = sc.getScaledHeight_double() * (RWR_CROSS_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x - half, y + half, 0).tex(0, 1).endVertex();
        buf.pos(x + half, y + half, 0).tex(1, 1).endVertex();
        buf.pos(x + half, y - half, 0).tex(1, 0).endVertex();
        buf.pos(x - half, y - half, 0).tex(0, 0).endVertex();
        tess.draw();
        restoreRenderState();
    }

    private void drawMortarTarget(double x, double y, ScaledResolution sc) {
        prepareRenderState(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TARGET);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        double half = sc.getScaledHeight_double() * (2 / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x - half, y + half, 0).tex(0, 1).endVertex();
        buf.pos(x + half, y + half, 0).tex(1, 1).endVertex();
        buf.pos(x + half, y - half, 0).tex(1, 0).endVertex();
        buf.pos(x - half, y - half, 0).tex(0, 0).endVertex();
        tess.draw();
        restoreRenderState();
    }

    private void prepareRenderState(float r, float g, float b, float a) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(r, g, b, a);
    }

    private void restoreRenderState() {
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public List<EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    @Override
    public DirectDrawable getInstance() {
        return INSTANCE;
    }
}
