package com.norwood.mcheli.weapon;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MCH_RenderBomb extends MCH_RenderBulletBase<MCH_EntityBomb> {

    public static final IRenderFactory<MCH_EntityBomb> FACTORY = MCH_RenderBomb::new;

    public MCH_RenderBomb(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
    }

    public void renderBullet(MCH_EntityBomb entity, double posX, double posY, double posZ, float yaw,
                             float partialTickTime) {
        if (entity instanceof MCH_EntityBomb) {
            if (entity.getInfo() != null) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(posX, posY, posZ);
                GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-entity.rotationPitch, -1.0F, 0.0F, 0.0F);
                if (entity.isBomblet > 0 || entity.getInfo().bomblet <= 0 || entity.getInfo().bombletSTime > 0) {
                    this.renderModel(entity);
                }

                GlStateManager.popMatrix();
            }
        }
    }

    protected ResourceLocation getEntityTexture(MCH_EntityBomb entity) {
        return TEX_DEFAULT;
    }
}
