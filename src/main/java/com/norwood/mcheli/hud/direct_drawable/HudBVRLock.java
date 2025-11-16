package com.norwood.mcheli.hud.direct_drawable;

import com.norwood.mcheli.EntityInfo;
import com.norwood.mcheli.MCH_EntityInfoClientTracker;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudBVRLock implements DirectDrawable {

    public static final Map<Integer, EntityInfo> currentLockedEntities = new HashMap<>();
    public static final ResourceLocation FRAME = new ResourceLocation(Tags.MODID, "textures/bvr_lock_box.png");
    public static final ResourceLocation MSL = new ResourceLocation(Tags.MODID, "textures/msl.png");
    private static final int BOX_SIZE = 24;
    public static final HudBVRLock INSTANCE = new HudBVRLock();

    public void renderHud(RenderGameOverlayEvent.Post event, Tuple<EntityPlayer, MCH_EntityAircraft> ctx) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = (EntityPlayerSP) ctx.getFirst();
        var ac = ctx.getSecond();
        ScaledResolution sc = new ScaledResolution(mc);
        if (mc.gameSettings.thirdPersonView != 0) return;

        if (ac.getCurrentWeapon(player) == null || ac.getCurrentWeapon(player).getCurrentWeapon() == null)
            return;

        MCH_WeaponInfo wi = ac.getCurrentWeapon(player).getCurrentWeapon().getInfo();
        if (wi == null || !wi.enableBVR) return;

        GlStateManager.pushMatrix();
        {
            List<EntityInfo> entities = new ArrayList<>(getServerLoadedEntity());
            for (EntityInfo entity : entities) {
                if (!canRenderEntity(entity, player, wi)) continue;

                double x = interpolate(entity.x, entity.prevX, event.getPartialTicks());
                double y = interpolate(entity.y, entity.prevY, event.getPartialTicks()) + 1;
                double z = interpolate(entity.z, entity.prevZ, event.getPartialTicks());
                Vec3d entityPos = new Vec3d(x, y, z);
                double[] screenPos = worldToScreen(entityPos);

                double sx = screenPos[0];
                double sy = screenPos[1];
                if (sx <= 0 || sy <= 0) continue;

                boolean lock = false;
                float alpha = 0.1f;
                double ox = screenPos[2];
                double oy = screenPos[3];
                double distScreen = ox * ox + oy * oy;

                if (distScreen < Math.pow(0.038 * sc.getScaledHeight(), 2)) {
                    alpha = 1f;
                    currentLockedEntities.put(entity.entityId, entity);
                    lock = true;
                } else if (distScreen < Math.pow(0.076 * sc.getScaledHeight(), 2)) {
                    alpha = 1f;
                } else if (distScreen < Math.pow(0.152 * sc.getScaledHeight(), 2)) {
                    alpha = 0.8f;
                } else if (distScreen < Math.pow(0.228 * sc.getScaledHeight(), 2)) {
                    alpha = 0.6f;
                } else if (distScreen < Math.pow(0.288 * sc.getScaledHeight(), 2)) {
                    alpha = 0.4f;
                } else if (distScreen > Math.pow(0.384 * sc.getScaledHeight(), 2)) {
                    double distance = Math.sqrt(distScreen);
                    double ratio = 200 / distance;
                    sx = sc.getScaledWidth() / 2.0 + ox * ratio;
                    sy = sc.getScaledHeight() / 2.0 + oy * ratio;
                    alpha = 0.2f;
                }

                if (entity.entityClassName.contains("MCH_EntityAAMissile")) {
                    if (player.getDistanceSq(x, y, z) < 1000 * 1000 && alpha > 0.4) {
                        drawMSLMarker(sx, sy, true, alpha);
                        mc.fontRenderer.drawString(
                                String.format("[MSL %.1fm]", player.getDistance(x, y, z)),
                                (int) (sx - 20), (int) (sy + 12), 0xFF0000, false);
                    }
                } else {
                    drawEntityMarker(sx, sy, lock, alpha);
                    if (alpha >= 0.6f) {
                        mc.fontRenderer.drawString(
                                String.format("[%s %.1fm]", ac.getNameOnMyRadar(entity), player.getDistance(x, y, z)),
                                (int) (sx - 20), (int) (sy + 12),
                                lock ? 0xFF0000 : 0x00FF00, false);
                    }
                }

                if (!lock) currentLockedEntities.clear();
            }
        }
        GlStateManager.popMatrix();
    }

    @Override
    public DirectDrawable getInstance() {
        return INSTANCE;
    }

    private List<EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }

    private boolean canRenderEntity(EntityInfo entity, EntityPlayer player, MCH_WeaponInfo wi) {
        double distSq = entity.getDistanceSqToEntity(player);
        if (entity.entityClassName.contains("MCP_EntityPlane") || entity.entityClassName.contains("MCH_EntityHeli")) {
            return distSq > wi.minRangeBVR * wi.minRangeBVR;
        } else if (entity.entityClassName.contains("MCH_EntityChaff") && wi.isRadarMissile) {
            return distSq > wi.minRangeBVR * wi.minRangeBVR;
        } else if (entity.entityClassName.contains("MCH_EntityAAMissile")) {
            return distSq > 100 * 100;
        }
        return false;
    }

    private double[] worldToScreen(Vec3d pos) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        var renderMan = mc.getRenderManager();
        Vec3d camPos = new Vec3d(renderMan.viewerPosX, renderMan.viewerPosY, renderMan.viewerPosZ);
        Vec3d relPos = pos.subtract(camPos);
        Vec3d lookVec = player.getLookVec();

        double angle = Math.toDegrees(Math.acos(relPos.normalize().dotProduct(lookVec)));
        if (angle > 45) return new double[] { -1, -1, -1, -1 };

        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = lookVec.crossProduct(worldUp);
        if (right.lengthSquared() < 1e-5) {
            double yawRad = Math.toRadians(player.rotationYaw + 90);
            right = new Vec3d(Math.cos(yawRad), 0, -Math.sin(yawRad));
        }
        right = right.normalize();
        Vec3d up = right.crossProduct(lookVec).normalize();

        double dx = relPos.dotProduct(right);
        double dy = relPos.dotProduct(up);
        double dz = relPos.dotProduct(lookVec);
        if (dz <= 0) return new double[] { -1, -1, -1, -1 };

        ScaledResolution sc = new ScaledResolution(mc);
        double fov = mc.gameSettings.fovSetting;
        double tanHalfFov = Math.tan(Math.toRadians(fov) * 0.5);
        double aspect = (double) sc.getScaledWidth() / sc.getScaledHeight();

        double xProj = (dx / dz) / (aspect * tanHalfFov);
        double yProj = (dy / dz) / tanHalfFov;

        double screenX = sc.getScaledWidth() / 2.0 + xProj * (sc.getScaledWidth() / 2.0);
        double screenY = sc.getScaledHeight() / 2.0 - yProj * (sc.getScaledHeight() / 2.0);

        return new double[] { screenX, screenY, screenX - sc.getScaledWidth() / 2.0,
                screenY - sc.getScaledHeight() / 2.0 };
    }

    private void drawEntityMarker(double x, double y, boolean lock, float alpha) {
        prepareRenderState(lock, alpha);
        Minecraft.getMinecraft().getTextureManager().bindTexture(FRAME);
        drawQuad(x, y, BOX_SIZE);
        restoreRenderState();
    }

    private void drawMSLMarker(double x, double y, boolean lock, float alpha) {
        prepareRenderState(lock, alpha);
        Minecraft.getMinecraft().getTextureManager().bindTexture(MSL);
        drawQuad(x, y, BOX_SIZE);
        restoreRenderState();
    }

    private void drawQuad(double x, double y, double size) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        double half = size / 2.0;

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x - half, y + half, 0).tex(0, 1).endVertex();
        buf.pos(x + half, y + half, 0).tex(1, 1).endVertex();
        buf.pos(x + half, y - half, 0).tex(1, 0).endVertex();
        buf.pos(x - half, y - half, 0).tex(0, 0).endVertex();
        tess.draw();
    }

    private void prepareRenderState(boolean lock, float alpha) {
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(lock ? 1.0F : 0.0F, lock ? 0.0F : 1.0F, 0.0F, alpha);
    }

    private void restoreRenderState() {
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }
}
