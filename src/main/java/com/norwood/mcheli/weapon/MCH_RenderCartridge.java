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

    public void doRender(MCH_EntityCartridge entity, double posX, double posY, double posZ, float par8,
                         float tickTime) {
        MCH_EntityCartridge cartridge = null;
        if (entity.model != null && !entity.texture_name.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(posX, posY, posZ);
            GlStateManager.scale(entity.getScale(), entity.getScale(), entity.getScale());
            float prevYaw = entity.prevRotationYaw;
            if (entity.rotationYaw - prevYaw < -180.0F) {
                prevYaw -= 360.0F;
            } else if (prevYaw - entity.rotationYaw < -180.0F) {
                prevYaw += 360.0F;
            }

            float yaw = -(prevYaw + (entity.rotationYaw - prevYaw) * tickTime);
            float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * tickTime;
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            this.bindTexture("textures/bullets/" + entity.texture_name + ".png");
            entity.model.renderAll();
            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityCartridge entity) {
        return TEX_DEFAULT;
    }
}
