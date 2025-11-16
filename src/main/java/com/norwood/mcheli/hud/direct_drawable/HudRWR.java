package com.norwood.mcheli.hud.direct_drawable;

import com.norwood.mcheli.EntityInfo;
import com.norwood.mcheli.MCH_EntityInfoClientTracker;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.tank.MCH_EntityTank;
import lombok.AllArgsConstructor;
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

public class HudRWR implements DirectDrawable {

    public static HudRWR INSTANCE = new HudRWR();

    public static final ResourceLocation RWR = new ResourceLocation(Tags.MODID, "textures/rwr.png");
    public static final ResourceLocation RWR_HELI = new ResourceLocation(Tags.MODID, "textures/rwr_heli.png");
    public static final ResourceLocation RWR_TANK = new ResourceLocation(Tags.MODID, "textures/rwr_tank.png");
    public static final ResourceLocation RWR_FAC = new ResourceLocation(Tags.MODID, "textures/rwr_fac.png");
    private static final int _RWR_SIZE = 180;
    private static final int _RWR_CENTER_X = 100;
    private static final int _RWR_CENTER_Y = 280;
    private static final double SCREEN_HEIGHT_ADAPT_CONSTANT = 520;

    private static final double _MIN_DISTANCE = 50.0;
    private static final double _MAX_DISTANCE = 1000.0;
    private static final int _MIN_RADIUS = 30;

    public void renderHud(RenderGameOverlayEvent.Post event, Tuple<EntityPlayer, MCH_EntityAircraft> ctx) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        MCH_EntityAircraft ac = ctx.getSecond();
        EntityPlayer player = ctx.getFirst();
        ScaledResolution sc = new ScaledResolution(mc);

        int RWR_SIZE = _RWR_SIZE;
        int RWR_CENTER_X = _RWR_CENTER_X;
        int RWR_CENTER_Y = _RWR_CENTER_Y;
        double MIN_DISTANCE = _MIN_DISTANCE;
        double MAX_DISTANCE = _MAX_DISTANCE;
        int MIN_RADIUS = _MIN_RADIUS;

        GlStateManager.pushMatrix();
        {
            ResourceLocation rwr;
            if (ac instanceof MCH_EntityPlane) {
                rwr = RWR;
                if (ac.getAcInfo().isFloat) {
                    rwr = RWR_FAC;
                    RWR_SIZE = 160;
                    RWR_CENTER_X = 220;
                    RWR_CENTER_Y = 370;
                    MIN_DISTANCE = 15;
                    MAX_DISTANCE = 800;
                    MIN_RADIUS = 30;
                }
            } else if (ac instanceof MCH_EntityHeli) {
                rwr = RWR_HELI;
            } else if (ac instanceof MCH_EntityTank) {
                rwr = RWR_TANK;
                RWR_SIZE = 160;
                RWR_CENTER_X = 220;
                RWR_CENTER_Y = 370;
                MIN_DISTANCE = 15;
                MAX_DISTANCE = 800;
                MIN_RADIUS = 30;
            } else {
                rwr = RWR;
            }

            double sx = sc.getScaledHeight() * (RWR_CENTER_X / SCREEN_HEIGHT_ADAPT_CONSTANT);
            double sy = sc.getScaledHeight() * (RWR_CENTER_Y / SCREEN_HEIGHT_ADAPT_CONSTANT);
            drawRWRCircle(sx, sy, sc, rwr, RWR_SIZE);

            double circleRadius = sc.getScaledHeight() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
            for (EntityInfo entity : getServerLoadedEntity()) {
                if (!isValidEntity(entity, player, MIN_DISTANCE)) continue;

                double xPos = interpolate(entity.x, entity.prevX, event.getPartialTicks());
                double yPos = interpolate(entity.y, entity.prevY, event.getPartialTicks());
                double zPos = interpolate(entity.z, entity.prevZ, event.getPartialTicks());

                Vec3d playerInterp = new Vec3d(
                        player.posX + (player.posX - player.lastTickPosX) * event.getPartialTicks(),
                        player.posY + (player.posY - player.lastTickPosY) * event.getPartialTicks(),
                        player.posZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks());

                Vec3d delta = new Vec3d(xPos, yPos, zPos).subtract(playerInterp);

                Vec3d lookVec = getDirection(ac, event.getPartialTicks());
                Vec3d deltaHorizontal = new Vec3d(delta.x, 0, delta.z).normalize();
                Vec3d lookHorizontal = new Vec3d(lookVec.x, 0, lookVec.z).normalize();

                double dot = lookHorizontal.dotProduct(deltaHorizontal);
                double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
                if (lookHorizontal.crossProduct(deltaHorizontal).y < 0) angle = -angle;

                double distance = Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
                double radiusRatio = Math.min(Math.max((distance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE), 0.0),
                        1.0);
                double renderRadius = MIN_RADIUS + (circleRadius - MIN_RADIUS) * radiusRatio;

                double radian = Math.toRadians(angle);
                double markerX = sx + renderRadius * Math.sin(-radian);
                double markerY = sy - renderRadius * Math.cos(radian);

                RWRResult rwrResult = getTargetTypeOnRadar(entity, ac);
                String text = rwrResult.name;
                int color = rwrResult.color;
                int textWidth = mc.fontRenderer.getStringWidth(text);
                mc.fontRenderer.drawString(text, (int) (markerX - textWidth / 2.0), (int) (markerY - 4.0), color, true);
            }
        }
        GlStateManager.popMatrix();
    }

    @Override
    public DirectDrawable getInstance() {
        return INSTANCE;
    }

    public Vec3d getDirection(Entity e, float factor) {
        float f1;
        float f2;
        float f3;
        float f4;

        if (factor == 1.0F) {
            f1 = MathHelper.cos(-e.rotationYaw * 0.017453292F - (float) Math.PI);
            f2 = MathHelper.sin(-e.rotationYaw * 0.017453292F - (float) Math.PI);
            f3 = -MathHelper.cos(-e.rotationPitch * 0.017453292F);
            f4 = MathHelper.sin(-e.rotationPitch * 0.017453292F);
            return new Vec3d(f2 * f3, f4, f1 * f3);
        } else {
            f1 = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * factor;
            f2 = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * factor;
            f3 = MathHelper.cos(-f2 * 0.017453292F - (float) Math.PI);
            f4 = MathHelper.sin(-f2 * 0.017453292F - (float) Math.PI);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            return new Vec3d(f4 * f5, f6, f3 * f5);
        }
    }

    private boolean isValidEntity(EntityInfo entity, EntityPlayer player, double minDist) {
        if (entity.entityClassName.contains("MCH_EntityChaff") || entity.entityClassName.contains("MCH_EntityFlare") ||
                entity.entityClassName.contains("EntityPlayer") || entity.entityClassName.contains("EntitySoldier")) {
            return false;
        }
        if (entity.getDistanceSqToEntity(player) < minDist * minDist) {
            return false;
        }
        return true;
    }

    private RWRResult getTargetTypeOnRadar(EntityInfo entity, MCH_EntityAircraft ac) {
        int color = 0x00FF00;
        if (ac instanceof MCH_EntityTank || (ac instanceof MCH_EntityPlane && ac.getAcInfo().isFloat)) {
            color = 0xFFCC00;
        }
        switch (ac.getAcInfo().rwrType) {
            case DIGITAL: {
                if (entity.entityClassName.contains("MCH_EntityHeli") ||
                        entity.entityClassName.contains("MCP_EntityPlane") ||
                        entity.entityClassName.contains("MCH_EntityTank") ||
                        entity.entityClassName.contains("MCH_EntityVehicle")) {
                    return new RWRResult(ac.getNameOnMyRadar(entity), color);
                } else {
                    return new RWRResult("MSL", 0xFF0000);
                }
            }
        }
        return new RWRResult("?", 0x00FF00);
    }

    private void drawRWRCircle(double x, double y, ScaledResolution sc, ResourceLocation rwr, int size) {
        prepareRenderState();
        Minecraft.getMinecraft().renderEngine.bindTexture(rwr);

        double halfSize = sc.getScaledHeight() * (size / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(x - halfSize, y + halfSize, 0.0).tex(0.0, 1.0).endVertex();
        buffer.pos(x + halfSize, y + halfSize, 0.0).tex(1.0, 1.0).endVertex();
        buffer.pos(x + halfSize, y - halfSize, 0.0).tex(1.0, 0.0).endVertex();
        buffer.pos(x - halfSize, y - halfSize, 0.0).tex(0.0, 0.0).endVertex();

        tessellator.draw();
        restoreRenderState();
    }

    private void prepareRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void restoreRenderState() {
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public List<EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    @AllArgsConstructor
    public static class RWRResult {

        public final String name;
        public final int color;
    }
}
