package com.norwood.mcheli.plane;

import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.modelloader.ModelVBO;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCP_RenderPlane extends MCH_RenderAircraft<MCH_EntityPlane> {

    public static final IRenderFactory<MCH_EntityPlane> FACTORY = MCP_RenderPlane::new;

    private static final String TEX_EXHAUST_DIR = "textures/exhaustflames/";

    public MCP_RenderPlane(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 2.0F;
    }

    @Override
    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch,
                               float roll, float tickTime) {
        MCH_PlaneInfo planeInfo;
        if (entity instanceof MCH_EntityPlane plane) {
            planeInfo = plane.getPlaneInfo();
            if (planeInfo != null) {
                posY += W_Entity.GLOBAL_Y_OFFSET;
                this.renderDebugHitBox(plane, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(plane, posX, posY, posZ, yaw, pitch, roll);
                GlStateManager.translate(posX, posY, posZ);
                GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
                this.bindTexture("textures/planes/" + plane.getTextureName() + ".png", plane);
                if (planeInfo.haveNozzle() && plane.partNozzle != null) {
                    this.renderNozzle(plane, planeInfo, tickTime);
                }

                if (planeInfo.haveWing() && plane.partWing != null) {
                    this.renderWing(plane, planeInfo, tickTime);
                }

                if (planeInfo.haveRotor() && plane.partNozzle != null) {
                    this.renderRotor(plane, planeInfo, tickTime);
                }

                renderBody(planeInfo.model);
                if (!planeInfo.partCrawlerTrack.isEmpty() && (isNotMoving(plane) || isTrackLod(plane)))
                    ((ModelVBO) planeInfo.model).renderTracksBuffer(planeInfo);
            }
        }
    }

    public void renderRotor(MCH_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();

        for (MCH_PlaneInfo.Rotor r : planeInfo.rotorList) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(r.pos.x, r.pos.y, r.pos.z);
            GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * r.maxRotFactor, (float) r.rot.x,
                    (float) r.rot.y, (float) r.rot.z);
            GlStateManager.translate(-r.pos.x, -r.pos.y, -r.pos.z);
            renderPart(r.model, planeInfo.model, r.modelName);

            for (MCH_PlaneInfo.Blade b : r.blades) {
                float br = plane.prevRotationRotor;
                br += (plane.rotationRotor - plane.prevRotationRotor) * tickTime;
                GlStateManager.pushMatrix();
                GlStateManager.translate(b.pos.x, b.pos.y, b.pos.z);
                GlStateManager.rotate(br, (float) b.rot.x, (float) b.rot.y, (float) b.rot.z);
                GlStateManager.translate(-b.pos.x, -b.pos.y, -b.pos.z);

                for (int i = 0; i < b.numBlade; i++) {
                    GlStateManager.translate(b.pos.x, b.pos.y, b.pos.z);
                    GlStateManager.rotate(b.rotBlade, (float) b.rot.x, (float) b.rot.y, (float) b.rot.z);
                    GlStateManager.translate(-b.pos.x, -b.pos.y, -b.pos.z);
                    renderPart(b.model, planeInfo.model, b.modelName);
                }

                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
        }
    }

    public void renderWing(MCH_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getWingRotation();
        float prevRot = plane.getPrevWingRotation();

        for (MCH_PlaneInfo.Wing w : planeInfo.wingList) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(w.pos.x, w.pos.y, w.pos.z);
            GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * w.maxRotFactor, (float) w.rot.x,
                    (float) w.rot.y, (float) w.rot.z);
            GlStateManager.translate(-w.pos.x, -w.pos.y, -w.pos.z);
            renderPart(w.model, planeInfo.model, w.modelName);
            if (w.pylonList != null) {
                for (MCH_PlaneInfo.Pylon p : w.pylonList) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(p.pos.x, p.pos.y, p.pos.z);
                    GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * p.maxRotFactor, (float) p.rot.x,
                            (float) p.rot.y, (float) p.rot.z);
                    GlStateManager.translate(-p.pos.x, -p.pos.y, -p.pos.z);
                    renderPart(p.model, planeInfo.model, p.modelName);
                    GlStateManager.popMatrix();
                }
            }

            GlStateManager.popMatrix();
        }
    }

    public void renderNozzle(MCH_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();

        for (MCH_AircraftInfo.DrawnPart n : planeInfo.nozzles) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(n.pos.x, n.pos.y, n.pos.z);
            GlStateManager.rotate(prevRot + (rot - prevRot) * tickTime, (float) n.rot.x, (float) n.rot.y,
                    (float) n.rot.z);
            GlStateManager.translate(-n.pos.x, -n.pos.y, -n.pos.z);
            renderPart(n.model, planeInfo.model, n.modelName);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void renderCommonPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
        super.renderCommonPart(ac, info, x, y, z, tickTime);
        if (ac instanceof MCH_EntityPlane plane) {
            MCH_PlaneInfo planeInfo = plane.getPlaneInfo();
            if (!planeInfo.exhaustFlames.isEmpty()) {
                this.renderExhaustFlame(plane, planeInfo, tickTime);
            }
        }
    }

    public void renderExhaustFlame(MCH_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        final int n = planeInfo.exhaustFlames.size();
        if (n == 0) return;

        MCH_EntityPlane.ExhaustAnimState st = plane.exhaustAnimState;
        if (st == null || st.frame.length != n) {
            st = new MCH_EntityPlane.ExhaustAnimState(n);
            plane.exhaustAnimState = st;
        }

        float throttle = (float) plane.getCurrentThrottle();
        throttle = clamp(throttle, 0.0F, 1.0F);
        if (throttle == 0) {
            return;
        }

        if (plane.getVtolMode() != 0) {
            return;
        }

        float yawFactor = lerp(plane.prevRotationExhaustFlameY, plane.rotationExhaustFlameY, tickTime);
        yawFactor = clamp(yawFactor, -1.0F, 1.0F);

        float scaleZ = throttle;

        for (int i = 0; i < n; i++) {
            MCH_PlaneInfo.ExhaustFlame ef = planeInfo.exhaustFlames.get(i);
            int delay = ef.delay <= 0 ? 1 : ef.delay;
            int frame = st.frame[i];
            int t = st.tick[i] + 1;

            if (t >= delay) {
                t = 0;
                int next = frame + 1;
                if (next >= MCH_PlaneInfo.exhaustFlameTextureMap.getOrDefault(ef.texturePrefix, 0)) {
                    next = 0;
                }
                frame = next;
            }

            st.frame[i] = frame;
            st.tick[i] = t;

            this.bindTexture(TEX_EXHAUST_DIR + ef.texturePrefix + frame + ".png");

            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.disableStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            // 计算旋转角度
            float yawDeg = clamp(yawFactor * ef.degreeYaw, -ef.degreeYaw, ef.degreeYaw);

            GlStateManager.pushMatrix();

            // 将火焰平移到飞机上的相对位置
            GlStateManager.translate(ef.pos.x, ef.pos.y, ef.pos.z);

            GlStateManager.rotate(-yawDeg, (float) ef.rot.x, (float) ef.rot.y, (float) ef.rot.z);

            GlStateManager.scale(1.0F, 1.0F, scaleZ);

            // 渲染火焰模型
            String flameModelName = (ef.modelName != null && !ef.modelName.isEmpty()) ? ef.modelName : "Exhaustflame";
            MCH_ModelManager.render("exhaustflames", flameModelName);

            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }

    private static float lerp(float prev, float now, float t) {
        return prev + (now - prev) * t;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (Math.min(v, max));
    }

    @Override
    protected ResourceLocation getEntityTexture(MCH_EntityPlane entity) {
        return TEX_DEFAULT;
    }
}
