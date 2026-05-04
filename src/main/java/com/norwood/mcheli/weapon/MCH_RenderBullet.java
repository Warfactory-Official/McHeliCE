package com.norwood.mcheli.weapon;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MCH_RenderBullet extends MCH_RenderBulletBase<MCH_EntityBaseBullet> {

    public static final IRenderFactory<MCH_EntityBaseBullet> FACTORY = MCH_RenderBullet::new;

    protected MCH_RenderBullet(RenderManager renderManager) {
        super(renderManager);
    }

    public void renderBullet(MCH_EntityBaseBullet entity, double posX, double posY, double posZ, float yaw,
                             float tickTime) {
        GlStateManager.pushMatrix();
        float interpYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickTime;
        float interpPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * tickTime;
        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.rotate(-interpYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(interpPitch, 1.0F, 0.0F, 0.0F);
        this.renderModel(entity);
        GlStateManager.popMatrix();
    }

    protected ResourceLocation getEntityTexture(MCH_EntityBaseBullet entity) {
        return TEX_DEFAULT;
    }
}
