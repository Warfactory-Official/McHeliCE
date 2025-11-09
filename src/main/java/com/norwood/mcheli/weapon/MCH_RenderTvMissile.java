package com.norwood.mcheli.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.uav.MCH_EntityUavStation;

@SideOnly(Side.CLIENT)
public class MCH_RenderTvMissile extends MCH_RenderBulletBase<MCH_EntityBaseBullet> {

    public static final IRenderFactory<MCH_EntityBaseBullet> FACTORY = MCH_RenderTvMissile::new;

    public MCH_RenderTvMissile(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    public void renderBullet(MCH_EntityBaseBullet entity, double posX, double posY, double posZ, float par8,
                             float par9) {
        MCH_EntityAircraft ac = null;
        Entity ridingEntity = Minecraft.getMinecraft().player.getRidingEntity();
        if (ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) ridingEntity;
        } else if (ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) ridingEntity).getParent();
        } else if (ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation) ridingEntity).getControlAircract();
        }

        if (ac == null || ac.isRenderBullet(entity, Minecraft.getMinecraft().player)) {
            if (entity instanceof MCH_EntityBaseBullet) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(posX, posY, posZ);
                GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-entity.rotationPitch, -1.0F, 0.0F, 0.0F);
                this.renderModel(entity);
                GlStateManager.popMatrix();
            }
        }
    }

    protected ResourceLocation getEntityTexture(MCH_EntityBaseBullet entity) {
        return TEX_DEFAULT;
    }
}
