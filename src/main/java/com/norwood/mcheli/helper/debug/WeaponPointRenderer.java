package com.norwood.mcheli.helper.debug;

import java.util.Map;

import javax.vecmath.Color4f;

import com.norwood.mcheli.wrapper.GLStateManagerExt;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponSet;

public class WeaponPointRenderer {

    private static final Color4f[] C = new Color4f[] {
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
        GlStateManager.enableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GLStateManagerExt.setPointSize(20F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        for (MCH_AircraftInfo.WeaponSet wsInfo : info.weaponSetList) {
            MCH_WeaponSet ws = ac.getWeaponByName(wsInfo.type);
            if (ws != null) {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder builder = tessellator.getBuffer();
                builder.begin(0, DefaultVertexFormats.POSITION_COLOR);

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
                        builder.pos(vec3d.x, vec3d.y + d, vec3d.z).color(in(c.x + f), in(c.y + f), in(c.z + f), c.w)
                                .endVertex();
                    }
                }

                tessellator.draw();
                id++;
            }
        }

        GlStateManager.popMatrix();
        GLStateManagerExt.restorePointSize();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    static float in(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }
}
