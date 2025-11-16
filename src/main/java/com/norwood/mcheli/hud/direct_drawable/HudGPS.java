package com.norwood.mcheli.hud.direct_drawable;

import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.GPSPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public class HudGPS implements DirectDrawable {

    public static final ResourceLocation GPS_POS = new ResourceLocation(Tags.MODID, "textures/gps_position.png");
    private static final int ICON_SIZE_PX = 24;
    public static HudGPS INSTANCE = new HudGPS();

    @Override
    public void renderHud(RenderGameOverlayEvent.Post event, Tuple<EntityPlayer, MCH_EntityAircraft> ctx) {
        var gps = GPSPosition.currentClientGPSPosition;
        if (gps == null || !gps.isActive()) return;

        var player = ctx.getFirst();
        final double gx = gps.x, gy = gps.y, gz = gps.z;
        var mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        final double camX = rm.viewerPosX;
        final double camY = rm.viewerPosY;
        final double camZ = rm.viewerPosZ;
        final double x = gx - camX, y = gy - camY, z = gz - camZ;

        double px = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
        double py = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks() + player.getEyeHeight();
        double pz = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();

        Vec3d toTarget = new Vec3d(gx - px, gy - py, gz - pz);
        double vlen = toTarget.length();
        if (vlen < 1e-4) return;
        Vec3d dir = toTarget.normalize();

        Vec3d look = player.getLook(event.getPartialTicks());
        double dot = Math.max(-1.0, Math.min(1.0, dir.x * look.x + dir.y * look.y + dir.z * look.z));
        double angleDeg = Math.toDegrees(Math.acos(dot));

        float alpha;
        boolean inLock = false;
        if (angleDeg <= 1.5) {
            alpha = 1.0f;
            inLock = true;
        } else if (angleDeg <= 3.0) alpha = 1.0f;
        else if (angleDeg <= 6.0) alpha = 0.8f;
        else if (angleDeg <= 9.0) alpha = 0.6f;
        else alpha = 0.4f;

        ScaledResolution sc = new ScaledResolution(mc);
        double dist = toTarget.length();
        double fovDeg = mc.gameSettings.fovSetting;
        double fovRad = Math.toRadians(fovDeg);
        float sPerPixel = (float) ((2.0 * dist * Math.tan(fovRad * 0.5)) / sc.getScaledHeight_double());

        float rollDeg = getViewRollDeg(mc, ctx.getSecond(), event.getPartialTicks());

        GlStateManager.pushMatrix();
        {
            GlStateManager.translate(x, y + 0.2, z);
            GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-rollDeg, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(-sPerPixel, -sPerPixel, sPerPixel);

            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            if (inLock) GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
            else GlStateManager.color(0.0F, 1.0F, 0.0F, alpha);

            mc.getTextureManager().bindTexture(GPS_POS);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            float half = ICON_SIZE_PX * 0.5f;

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-half, half, 0).tex(0, 1).endVertex();
            buffer.pos(half, half, 0).tex(1, 1).endVertex();
            buffer.pos(half, -half, 0).tex(1, 0).endVertex();
            buffer.pos(-half, -half, 0).tex(0, 0).endVertex();
            tessellator.draw();

            String text = String.format("[GPS %.1fm]", player.getDistance((float) gx, (float) gy, (float) gz));
            int color = inLock ? 0xFF0000 : 0x00FF00;
            GlStateManager.translate(0.0F, ICON_SIZE_PX * 0.5f + 8.0f, 0.0F);
            int fw = mc.fontRenderer.getStringWidth(text);
            mc.fontRenderer.drawString(text, (float) -fw / 2, 0, color, false);

            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
        GlStateManager.popMatrix();
    }

    private float getViewRollDeg(Minecraft mc, MCH_EntityAircraft ac, float partialTicks) {
        return -ac.rotationRoll;
    }

    @Override
    public DirectDrawable getInstance() {
        return INSTANCE;
    }
}
