package com.norwood.mcheli.helper.debug;

import com.google.common.collect.Maps;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Color4f;
import java.util.Map;

public class WeaponPointRenderer {
    private static final Color4f[] C = new Color4f[]{
            new Color4f(1.0F, 0.0F, 0.0F, 1.0F),
            new Color4f(0.0F, 1.0F, 0.0F, 1.0F),
            new Color4f(0.0F, 0.0F, 1.0F, 1.0F),
            new Color4f(1.0F, 1.0F, 0.0F, 1.0F),
            new Color4f(1.0F, 0.0F, 1.0F, 1.0F),
            new Color4f(0.0F, 1.0F, 1.0F, 1.0F),
            new Color4f(0.95686275F, 0.6431373F, 0.3764706F, 1.0F),
            new Color4f(0.5411765F, 0.16862746F, 0.42477876F, 1.0F)
    };

    public static void renderWeaponPoints(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z) {
        int id = 0;
        Map<Vec3d, Integer> poses = Maps.newHashMap();

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        for (MCH_AircraftInfo.WeaponSet wsInfo : info.weaponSetList) {
            MCH_WeaponSet ws = ac.getWeaponByName(wsInfo.type);
            if (ws != null) {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder builder = tessellator.getBuffer();
                builder.begin(7, DefaultVertexFormats.POSITION_COLOR);

                for (int i = 0; i < ws.getWeaponsCount(); i++) {
                    MCH_WeaponBase weapon = ws.getWeapon(i);
                    if (weapon != null) {
                        int j = 0;
                        if (poses.containsKey(weapon.position)) {
                            j = poses.get(weapon.position);
                            j++;
                        }

                        poses.put(weapon.position, j);
                        Vec3d vec3d = weapon.getShotPos(ac);
                        Color4f c = C[id % C.length];
                        float f = i * 0.1F;
                        double d = j * 0.04;

                        float r = in(c.x + f);
                        float g = in(c.y + f);
                        float b = in(c.z + f);

                        draw3DPointBox(builder, vec3d.x, vec3d.y + d, vec3d.z, 0.05, r, g, b, c.w);
                    }
                }

                tessellator.draw();
                id++;
            }
        }

        renderWeaponChildLinks(ac, info);
        if (info instanceof MCH_VehicleInfo vehicleInfo) {
            renderVehiclePartChildLinks(ac, vehicleInfo);
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }

    private static void renderWeaponChildLinks(MCH_EntityAircraft ac, MCH_AircraftInfo info) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        int id = 0;
        for (MCH_AircraftInfo.PartWeapon part : info.partWeapon) {
            Color4f c = C[id % C.length];
            Vec3d parentLocal = part.pos;
            Vec3d parentWorld = getRelativePos(ac, parentLocal);
            for (MCH_AircraftInfo.PartWeaponChild child : part.child) {
                drawWeaponChildLine(ac, builder, parentLocal, parentWorld, child, c);
            }
            id++;
        }

        tessellator.draw();
    }

    private static void renderVehiclePartChildLinks(MCH_EntityAircraft ac, MCH_VehicleInfo info) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        Color4f c = new Color4f(0.85F, 0.85F, 0.85F, 1.0F);
        for (MCH_VehicleInfo.VPart part : info.partList) {
            drawVehicleChildLinesRecursive(ac, builder, part, part.pos, c);
        }

        tessellator.draw();
    }

    private static void drawWeaponChildLine(MCH_EntityAircraft ac, BufferBuilder builder, Vec3d parentLocalPos,
                                            Vec3d parentWorldPos, MCH_AircraftInfo.PartWeaponChild child, Color4f c) {
        Vec3d childLocalPos = parentLocalPos.add(child.pos);
        Vec3d childWorldPos = getRelativePos(ac, childLocalPos);
        drawLine(builder, parentWorldPos, childWorldPos, c);
    }

    private static void drawVehicleChildLinesRecursive(MCH_EntityAircraft ac, BufferBuilder builder,
                                                       MCH_VehicleInfo.VPart parent, Vec3d parentLocalPos, Color4f c) {
        if (parent.child == null) {
            return;
        }

        Vec3d parentWorldPos = getRelativePos(ac, parentLocalPos);
        for (MCH_VehicleInfo.VPart child : parent.child) {
            Vec3d childLocalPos = parentLocalPos.add(child.pos);
            Vec3d childWorldPos = getRelativePos(ac, childLocalPos);
            drawLine(builder, parentWorldPos, childWorldPos, c);
            drawVehicleChildLinesRecursive(ac, builder, child, childLocalPos, c);
        }
    }

    private static Vec3d getRelativePos(MCH_EntityAircraft ac, Vec3d localPos) {
        return ac.getTransformedPosition(localPos.x, localPos.y, localPos.z, 0, 0, 0);
    }

    private static void drawLine(BufferBuilder builder, Vec3d start, Vec3d end, Color4f c) {
        builder.pos(start.x, start.y, start.z).color(c.x, c.y, c.z, c.w).endVertex();
        builder.pos(end.x, end.y, end.z).color(c.x, c.y, c.z, c.w).endVertex();
    }


    private static void draw3DPointBox(BufferBuilder builder, double x, double y, double z, double size, float r, float g, float b, float a) {
        double minX = x - size, minY = y - size, minZ = z - size;
        double maxX = x + size, maxY = y + size, maxZ = z + size;

        // Bottom
        builder.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        // Top
        builder.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        builder.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        // North
        builder.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        builder.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        // South
        builder.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        // West
        builder.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        builder.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        // East
        builder.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        builder.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
    }

    static float in(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }

}
