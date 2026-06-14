package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Client-side CCIP (Continuously Computed Impact Point) fire-control overlay.
 *
 * <p>Ported from the 1.7.10 immediate-mode renderer (MCH_RenderCCIP, GL11 + Tessellator
 * textured quad) to CE's VBO pipeline. The marker is a world-space billboard drawn at the
 * weapon's predicted impact point ({@link MCH_EntityAircraft#getPredictedImpactPoint}); it is
 * scaled to a roughly constant screen size and rendered procedurally (a ring + ticks + centre
 * cross) via {@link BufferBuilder} with {@link DefaultVertexFormats#POSITION_COLOR} and
 * {@link GlStateManager} — no immediate mode, no texture asset dependency.</p>
 *
 * <p>The Reforged version read a {@code CCIPTexture} key to bind a sprite; CE renders the pipper
 * procedurally instead, so {@code ccipTexture} is currently unused while {@code ccipFactor}
 * still scales the marker. Register a single instance on the client event bus
 * (see MCH_ClientProxy#init).</p>
 */
@SideOnly(Side.CLIENT)
public class MCH_RenderCCIP {

    /** Base marker radius in GUI pixels (before ccipFactor). */
    private static final float BASE_RADIUS_PX = 14.0F;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }

        MCH_EntityAircraft ac = resolveAircraft(player);
        if (ac == null) {
            return;
        }

        MCH_WeaponSet currentWs = ac.getCurrentWeapon(player);
        if (currentWs == null || currentWs.getInfo() == null || currentWs.getInfo().type == null
                || !currentWs.getInfo().ccip
                || !MCH_WeaponInfo.isCCIPSupportedType(currentWs.getInfo().type)) {
            return;
        }

        Vec3d impact = ac.getPredictedImpactPoint(player);
        if (impact == null) {
            return;
        }

        RenderManager rm = mc.getRenderManager();
        double x = impact.x - rm.viewerPosX;
        double y = impact.y - rm.viewerPosY + 0.2D;
        double z = impact.z - rm.viewerPosZ;
        double dist = Math.sqrt(x * x + y * y + z * z);
        if (dist < 0.5D) {
            return;
        }

        // Keep the marker a constant screen size: world-units-per-GUI-pixel at this distance.
        ScaledResolution sc = new ScaledResolution(mc);
        double fovRad = Math.toRadians(mc.gameSettings.fovSetting);
        float sPerPixel = (float) ((2.0D * dist * Math.tan(fovRad * 0.5D)) / (double) sc.getScaledHeight());

        float ccipFactor = currentWs.getInfo().ccipFactor;
        if (ccipFactor < 0.1F) {
            ccipFactor = 0.1F;
        } else if (ccipFactor > 10.0F) {
            ccipFactor = 10.0F;
        }
        float radius = BASE_RADIUS_PX * ccipFactor;

        boolean hot = hasEntityAroundImpact(ac, player, impact, 3.0D);
        float r = hot ? 1.0F : 0.1F;
        float g = hot ? 0.1F : 1.0F;
        float b = 0.1F;
        float a = 0.9F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(ac.getRoll(), 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(-sPerPixel, -sPerPixel, sPerPixel);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Ring.
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 24; i++) {
            double ang = Math.PI * 2.0D * i / 24.0D;
            buffer.pos(Math.cos(ang) * radius, Math.sin(ang) * radius, 0.0D).color(r, g, b, a).endVertex();
        }
        tessellator.draw();

        // Four cardinal ticks (from 0.55R out to R) + a short centre cross.
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        float inner = radius * 0.55F;
        float cross = radius * 0.30F;
        // ticks
        buffer.pos(inner, 0.0D, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(radius, 0.0D, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(-inner, 0.0D, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(-radius, 0.0D, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(0.0D, inner, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(0.0D, radius, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(0.0D, -inner, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(0.0D, -radius, 0.0D).color(r, g, b, a).endVertex();
        // centre cross
        buffer.pos(-cross, 0.0D, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(cross, 0.0D, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(0.0D, -cross, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(0.0D, cross, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    private static MCH_EntityAircraft resolveAircraft(EntityPlayer player) {
        Entity riding = player.getRidingEntity();
        if (riding instanceof MCH_EntityAircraft) {
            return (MCH_EntityAircraft) riding;
        }
        if (riding instanceof MCH_EntitySeat) {
            return ((MCH_EntitySeat) riding).getParent();
        }
        if (riding instanceof MCH_EntityUavStation) {
            return ((MCH_EntityUavStation) riding).getControlled();
        }
        return null;
    }

    private static boolean hasEntityAroundImpact(MCH_EntityAircraft ac, EntityPlayer player, Vec3d impact, double radius) {
        AxisAlignedBB aabb = new AxisAlignedBB(
                impact.x - radius, impact.y - radius, impact.z - radius,
                impact.x + radius, impact.y + radius, impact.z + radius);
        List<Entity> list = ac.world.getEntitiesWithinAABB(Entity.class, aabb);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (Entity entity : list) {
            if (entity == player || entity == ac || entity.isDead) {
                continue;
            }
            return true;
        }
        return false;
    }
}
