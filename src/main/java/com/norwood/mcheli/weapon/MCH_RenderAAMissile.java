package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Lib;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@SideOnly(Side.CLIENT)
public class MCH_RenderAAMissile extends MCH_RenderBulletBase<MCH_EntityAAMissile> {

    public static final IRenderFactory<MCH_EntityAAMissile> FACTORY = MCH_RenderAAMissile::new;

    public MCH_RenderAAMissile(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(@NotNull MCH_EntityAAMissile entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        this.renderMissileFlame(entity, x, y, z, partialTicks, 0.8D, 4.0F);
    }

    public void renderBullet(MCH_EntityAAMissile entity, double posX, double posY, double posZ, float par8,
                             float par9) {
            double mx = entity.prevMotionX + (entity.motionX - entity.prevMotionX) * par9;
            double my = entity.prevMotionY + (entity.motionY - entity.prevMotionY) * par9;
            double mz = entity.prevMotionZ + (entity.motionZ - entity.prevMotionZ) * par9;
            GlStateManager.pushMatrix();
            GlStateManager.translate(posX, posY, posZ);
            Vec3d v = MCH_Lib.getYawPitchFromVec(mx, my, mz);
            GlStateManager.rotate((float) v.y - 90.0F, 0.0F, -1.0F, 0.0F);
            GlStateManager.rotate((float) v.z, -1.0F, 0.0F, 0.0F);
            this.renderModel(entity);
            GlStateManager.popMatrix();
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityAAMissile entity) {
        return TEX_DEFAULT;
    }
}
