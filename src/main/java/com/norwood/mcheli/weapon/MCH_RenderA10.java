package com.norwood.mcheli.weapon;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.MCH_ModelManager;

@SideOnly(Side.CLIENT)
public class MCH_RenderA10 extends MCH_RenderBulletBase<MCH_EntityA10> {

    public static final IRenderFactory<MCH_EntityA10> FACTORY = MCH_RenderA10::new;

    public MCH_RenderA10(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 10.5F;
    }

    public void renderBullet(MCH_EntityA10 e, double posX, double posY, double posZ, float par8, float tickTime) {
        if (e instanceof MCH_EntityA10) {
            if (e.isRender()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(posX, posY, posZ);
                float yaw = -(e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * tickTime);
                float pitch = -(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * tickTime);
                GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                this.bindTexture("textures/bullets/a10.png");
                MCH_ModelManager.render("a-10");
                GlStateManager.popMatrix();
            }
        }
    }

    protected ResourceLocation getEntityTexture(MCH_EntityA10 entity) {
        return TEX_DEFAULT;
    }
}
