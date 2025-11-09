package com.norwood.mcheli.tank;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.helper.MCH_ColorInt;
import com.norwood.mcheli.wrapper.W_Entity;

@SideOnly(Side.CLIENT)
public class MCH_RenderTank extends MCH_RenderAircraft<MCH_EntityTank> {

    public static final IRenderFactory<MCH_EntityTank> FACTORY = MCH_RenderTank::new;

    public MCH_RenderTank(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 2.0F;
    }

    @Override
    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch,
                               float roll, float tickTime) {
        MCH_TankInfo tankInfo;
        if (entity instanceof MCH_EntityTank tank) {
            tankInfo = tank.getTankInfo();
            if (tankInfo != null) {
                posY += W_Entity.GLOBAL_Y_OFFSET;
                this.renderWheel(tank, posX, posY, posZ);
                this.renderDebugHitBox(tank, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(tank, posX, posY, posZ, yaw, pitch, roll);
                GlStateManager.translate(posX, posY, posZ);
                GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
                this.bindTexture("textures/tanks/" + tank.getTextureName() + ".png", tank);
                renderBody(tankInfo.model);
            }
        }
    }

    public void renderWheel(MCH_EntityTank tank, double posX, double posY, double posZ) {
        if (MCH_Config.TestMode.prmBool) {
            if (debugModel != null) {
                GlStateManager.color(0.75F, 0.75F, 0.75F, 0.5F);

                for (MCH_EntityWheel w : tank.WheelMng.wheels) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(w.posX - tank.posX + posX, w.posY - tank.posY + posY + 0.25,
                            w.posZ - tank.posZ + posZ);
                    GlStateManager.scale(w.width, w.height / 2.0F, w.width);
                    this.bindTexture("textures/seat_pilot.png");
                    debugModel.renderAll();
                    GlStateManager.popMatrix();
                }

                GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder builder = tessellator.getBuffer();
                builder.begin(1, DefaultVertexFormats.POSITION_COLOR);
                Vec3d wp = tank.getTransformedPosition(tank.WheelMng.weightedCenter);
                wp = wp.subtract(tank.posX, tank.posY, tank.posZ);

                for (int i = 0; i < tank.WheelMng.wheels.length / 2; i++) {
                    MCH_ColorInt cint = new MCH_ColorInt((i & 4) > 0 ? 16711680 : 0, (i & 2) > 0 ? '\uff00' : 0,
                            (i & 1) > 0 ? 255 : 0, 192);
                    MCH_EntityWheel w1 = tank.WheelMng.wheels[i * 2];
                    MCH_EntityWheel w2 = tank.WheelMng.wheels[i * 2 + 1];
                    if (w1.isPlus) {
                        builder.pos(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(posX + wp.x, posY + wp.y, posZ + wp.z).color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(posX + wp.x, posY + wp.y, posZ + wp.z).color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                    } else {
                        builder.pos(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(posX + wp.x, posY + wp.y, posZ + wp.z).color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(posX + wp.x, posY + wp.y, posZ + wp.z).color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                        builder.pos(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ)
                                .color(cint.r, cint.g, cint.b, cint.a)
                                .endVertex();
                    }
                }

                tessellator.draw();
                GlStateManager.resetColor();
            }
        }
    }

    protected ResourceLocation getEntityTexture(MCH_EntityTank entity) {
        return TEX_DEFAULT;
    }
}
