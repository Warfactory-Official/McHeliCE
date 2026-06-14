package com.norwood.mcheli.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Client-side laser beam renderer for {@link MCH_WeaponLaser}.
 *
 * <p>Ported from the 1.7.10 immediate-mode (GL11/Tessellator.startDrawingQuads) version to CE's
 * VBO pipeline: beams are drawn as two view-facing quads (a wide additive glow + a bright core)
 * using {@link BufferBuilder} with {@link DefaultVertexFormats#POSITION_COLOR} and
 * {@link GlStateManager} for all state changes.</p>
 *
 * <p>Register a single instance on the client event bus (see MCH_ClientProxy#init).</p>
 */
@SideOnly(Side.CLIENT)
public class MCH_RenderLaser {

    private static final List<Beam> BEAMS = new LinkedList<>();

    public static void addBeam(Vec3d start, Vec3d end, int argb, float width, int lifeTicks, boolean pulsate, double renderStartDist) {
        float a = ((argb >>> 24) & 0xFF) / 255.0F;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        synchronized (BEAMS) {
            BEAMS.add(new Beam(start, end, r, g, b, a, width, pulsate, lifeTicks, renderStartDist));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().isGamePaused()) {
            return;
        }
        synchronized (BEAMS) {
            Iterator<Beam> it = BEAMS.iterator();
            while (it.hasNext()) {
                if (--it.next().lifeTicks <= 0) {
                    it.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (BEAMS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.player;
        if (viewer == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        Vec3d look = viewer.getLook(partialTicks);
        RenderManager rm = mc.getRenderManager();
        long worldTime = viewer.world.getTotalWorldTime();

        GlStateManager.pushMatrix();
        GlStateManager.translate(-rm.viewerPosX, -rm.viewerPosY, -rm.viewerPosZ);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        synchronized (BEAMS) {
            for (Beam beam : BEAMS) {
                renderBeam(beam, tessellator, buffer, look, worldTime, partialTicks);
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void renderBeam(Beam beam, Tessellator tessellator, BufferBuilder buffer, Vec3d look, long worldTime, float partialTicks) {
        double sx = beam.start.x;
        double sy = beam.start.y;
        double sz = beam.start.z;
        final double ex = beam.end.x;
        final double ey = beam.end.y;
        final double ez = beam.end.z;

        double dx = ex - sx;
        double dy = ey - sy;
        double dz = ez - sz;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-8) {
            return;
        }

        // Optionally skip the first part of the beam (renderStartDist).
        if (beam.renderStartDist > 0) {
            double startDist = Math.min(beam.renderStartDist, length);
            if (startDist > 0) {
                double ratio = startDist / length;
                sx += dx * ratio;
                sy += dy * ratio;
                sz += dz * ratio;
                dx = ex - sx;
                dy = ey - sy;
                dz = ez - sz;
                length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length < 1.0E-8) {
                    return;
                }
            }
        }

        float width = beam.width;
        if (beam.pulsate) {
            width *= (float) (0.7 + 0.3 * Math.sin((worldTime + partialTicks) * 0.4));
        }

        double dirX = dx / length;
        double dirY = dy / length;
        double dirZ = dz / length;

        // Billboard the quad: cross(beamDir, viewerLook) gives the in-plane perpendicular.
        double crossX = dirY * look.z - dirZ * look.y;
        double crossY = dirZ * look.x - dirX * look.z;
        double crossZ = dirX * look.y - dirY * look.x;
        double crossLen = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
        if (crossLen < 1.0E-8) {
            crossX = -dirZ;
            crossY = 0.0;
            crossZ = dirX;
            crossLen = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
            if (crossLen < 1.0E-8) {
                return;
            }
        }
        crossX /= crossLen;
        crossY /= crossLen;
        crossZ /= crossLen;

        double halfWidth = width * 0.5;
        double hwX = crossX * halfWidth;
        double hwY = crossY * halfWidth;
        double hwZ = crossZ * halfWidth;

        // Outer glow quad (wide, faint, brightened color).
        float gr = Math.min(beam.r * 1.5F, 1.0F);
        float gg = Math.min(beam.g * 1.5F, 1.0F);
        float gb = Math.min(beam.b * 1.5F, 1.0F);
        float ga = beam.a * 0.4F;
        double gx = hwX * 2.0;
        double gy = hwY * 2.0;
        double gz = hwZ * 2.0;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(sx - gx, sy - gy, sz - gz).color(gr, gg, gb, ga).endVertex();
        buffer.pos(sx + gx, sy + gy, sz + gz).color(gr, gg, gb, ga).endVertex();
        buffer.pos(ex + gx, ey + gy, ez + gz).color(gr, gg, gb, ga).endVertex();
        buffer.pos(ex - gx, ey - gy, ez - gz).color(gr, gg, gb, ga).endVertex();
        tessellator.draw();

        // Bright core quad.
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(sx - hwX, sy - hwY, sz - hwZ).color(beam.r, beam.g, beam.b, beam.a).endVertex();
        buffer.pos(sx + hwX, sy + hwY, sz + hwZ).color(beam.r, beam.g, beam.b, beam.a).endVertex();
        buffer.pos(ex + hwX, ey + hwY, ez + hwZ).color(beam.r, beam.g, beam.b, beam.a).endVertex();
        buffer.pos(ex - hwX, ey - hwY, ez - hwZ).color(beam.r, beam.g, beam.b, beam.a).endVertex();
        tessellator.draw();
    }

    public static class Beam {
        final Vec3d start;
        final Vec3d end;
        final float r;
        final float g;
        final float b;
        final float a;
        final float width;
        final boolean pulsate;
        final double renderStartDist;
        int lifeTicks;

        Beam(Vec3d start, Vec3d end, float r, float g, float b, float a, float width, boolean pulsate, int lifeTicks, double renderStartDist) {
            this.start = start;
            this.end = end;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.width = width;
            this.pulsate = pulsate;
            this.lifeTicks = lifeTicks;
            this.renderStartDist = renderStartDist;
        }
    }
}
