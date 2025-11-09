package com.norwood.mcheli.weapon;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MCH_RenderASMissile extends MCH_RenderBulletBase<MCH_EntityBaseBullet> {

    public static final IRenderFactory<MCH_EntityBaseBullet> FACTORY = MCH_RenderASMissile::new;

    public MCH_RenderASMissile(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    public void renderBullet(MCH_EntityBaseBullet entity, double posX, double posY, double posZ, float yaw,
                             float partialTickTime) {
        if (entity instanceof MCH_EntityBaseBullet) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(posX, posY, posZ);
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-entity.rotationPitch, -1.0F, 0.0F, 0.0F);
            this.renderModel(entity);
            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(MCH_EntityBaseBullet entity) {
        return TEX_DEFAULT;
    }
}
