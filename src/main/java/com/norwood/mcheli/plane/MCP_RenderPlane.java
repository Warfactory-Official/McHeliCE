package com.norwood.mcheli.plane;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.GlStateManager;

@SideOnly(Side.CLIENT)
public class MCP_RenderPlane extends MCH_RenderAircraft<MCP_EntityPlane> {
    public static final IRenderFactory<MCP_EntityPlane> FACTORY = MCP_RenderPlane::new;

    public MCP_RenderPlane(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 2.0F;
    }

    @Override
    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
        MCH_PlaneInfo planeInfo;
        if (entity instanceof MCP_EntityPlane plane) {
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
            }
        }
    }

    public void renderRotor(MCP_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();

        for (MCH_PlaneInfo.Rotor r : planeInfo.rotorList) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(r.pos.x, r.pos.y, r.pos.z);
            GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * r.maxRotFactor, (float) r.rot.x, (float) r.rot.y, (float) r.rot.z);
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

    public void renderWing(MCP_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getWingRotation();
        float prevRot = plane.getPrevWingRotation();

        for (MCH_PlaneInfo.Wing w : planeInfo.wingList) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(w.pos.x, w.pos.y, w.pos.z);
            GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * w.maxRotFactor, (float) w.rot.x, (float) w.rot.y, (float) w.rot.z);
            GlStateManager.translate(-w.pos.x, -w.pos.y, -w.pos.z);
            renderPart(w.model, planeInfo.model, w.modelName);
            if (w.pylonList != null) {
                for (MCH_PlaneInfo.Pylon p : w.pylonList) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(p.pos.x, p.pos.y, p.pos.z);
                    GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * p.maxRotFactor, (float) p.rot.x, (float) p.rot.y, (float) p.rot.z);
                    GlStateManager.translate(-p.pos.x, -p.pos.y, -p.pos.z);
                    renderPart(p.model, planeInfo.model, p.modelName);
                    GlStateManager.popMatrix();
                }
            }

            GlStateManager.popMatrix();
        }
    }

    public void renderNozzle(MCP_EntityPlane plane, MCH_PlaneInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();

        for (MCH_AircraftInfo.DrawnPart n : planeInfo.nozzles) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(n.pos.x, n.pos.y, n.pos.z);
            GlStateManager.rotate(prevRot + (rot - prevRot) * tickTime, (float) n.rot.x, (float) n.rot.y, (float) n.rot.z);
            GlStateManager.translate(-n.pos.x, -n.pos.y, -n.pos.z);
            renderPart(n.model, planeInfo.model, n.modelName);
            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(MCP_EntityPlane entity) {
        return TEX_DEFAULT;
    }
}
