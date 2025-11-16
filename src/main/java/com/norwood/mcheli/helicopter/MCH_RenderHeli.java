package com.norwood.mcheli.helicopter;

import com.norwood.mcheli.aircraft.MCH_Blade;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.aircraft.MCH_Rotor;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MCH_RenderHeli extends MCH_RenderAircraft<MCH_EntityHeli> {

    public static final IRenderFactory<MCH_EntityHeli> FACTORY = MCH_RenderHeli::new;

    public MCH_RenderHeli(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 2.0F;
    }

    @Override
    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch,
                               float roll, float tickTime) {
        MCH_HeliInfo heliInfo;
        if (entity instanceof MCH_EntityHeli heli) {
            heliInfo = heli.getHeliInfo();
            if (heliInfo != null) {
                posY += W_Entity.GLOBAL_Y_OFFSET;
                this.renderDebugHitBox(heli, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(heli, posX, posY, posZ, yaw, pitch, roll);
                GlStateManager.translate(posX, posY, posZ);
                GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
                this.bindTexture("textures/helicopters/" + heli.getTextureName() + ".png", heli);
                renderBody(heliInfo.model);
                this.drawModelBlade(heli, heliInfo, tickTime);
            }
        }
    }

    public void drawModelBlade(MCH_EntityHeli heli, MCH_HeliInfo info, float tickTime) {
        for (int i = 0; i < heli.rotors.length && i < info.rotorList.size(); i++) {
            MCH_HeliInfo.Rotor rotorInfo = info.rotorList.get(i);
            MCH_Rotor rotor = heli.rotors[i];
            GlStateManager.pushMatrix();
            if (rotorInfo.oldRenderMethod) {
                GlStateManager.translate(rotorInfo.pos.x, rotorInfo.pos.y, rotorInfo.pos.z);
            }

            for (MCH_Blade b : rotor.blades) {
                GlStateManager.pushMatrix();
                float rot = b.getRotation();
                float prevRot = b.getPrevRotation();
                if (rot - prevRot < -180.0F) {
                    prevRot -= 360.0F;
                } else if (prevRot - rot < -180.0F) {
                    prevRot += 360.0F;
                }

                if (!rotorInfo.oldRenderMethod) {
                    GlStateManager.translate(rotorInfo.pos.x, rotorInfo.pos.y, rotorInfo.pos.z);
                }

                GlStateManager.rotate(prevRot + (rot - prevRot) * tickTime, (float) rotorInfo.rot.x,
                        (float) rotorInfo.rot.y, (float) rotorInfo.rot.z);
                if (!rotorInfo.oldRenderMethod) {
                    GlStateManager.translate(-rotorInfo.pos.x, -rotorInfo.pos.y, -rotorInfo.pos.z);
                }

                renderPart(rotorInfo.model, info.model, rotorInfo.modelName);
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(MCH_EntityHeli entity) {
        return TEX_DEFAULT;
    }
}
