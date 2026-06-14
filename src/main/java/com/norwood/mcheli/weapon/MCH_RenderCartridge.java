package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@SideOnly(Side.CLIENT)
public class MCH_RenderCartridge extends W_Render<MCH_EntityCartridge> {

    public static final IRenderFactory<MCH_EntityCartridge> FACTORY = MCH_RenderCartridge::new;

    public MCH_RenderCartridge(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(MCH_EntityCartridge entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.model == null || entity.texture_name.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        float scale = entity.getScale();
        GlStateManager.scale(scale, scale, scale);

        float yaw = interpolateRotation(entity.prevRotationYaw, entity.rotationYaw, partialTicks);
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

        this.bindTexture("textures/bullets/" + entity.texture_name + ".png");
        entity.model.renderAll();

        GlStateManager.popMatrix();
    }


    private float interpolateRotation(float prev, float current, float partialTicks) {
        float delta = current - prev;

        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;

        return prev + partialTicks * delta;
    }


    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityCartridge entity) {
        return TEX_DEFAULT;
    }
}
