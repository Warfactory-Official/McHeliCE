package com.norwood.mcheli.helper.debug;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.wrapper.W_Render;

@SideOnly(Side.CLIENT)
public class MCH_RenderTest extends W_Render<Entity> {

    protected final MCH_ModelTest model;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final String textureName;

    public MCH_RenderTest(RenderManager renderManager, float x, float y, float z, String texture_name) {
        super(renderManager);
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        this.textureName = texture_name;
        this.model = new MCH_ModelTest();
    }

    public static IRenderFactory<Entity> factory(float x, float y, float z, String texture_name) {
        return renderManager -> new MCH_RenderTest(renderManager, x, y, z, texture_name);
    }

    public void doRender(@NotNull Entity e, double posX, double posY, double posZ, float par8, float par9) {
        if (MCH_Config.TestMode.prmBool) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(posX + this.offsetX, posY + this.offsetY, posZ + this.offsetZ);
            GlStateManager.scale(e.width, e.height, e.width);
            GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
            float prevYaw;
            if (e.rotationYaw - e.prevRotationYaw < -180.0F) {
                prevYaw = e.prevRotationYaw - 360.0F;
            } else if (e.prevRotationYaw - e.rotationYaw < -180.0F) {
                prevYaw = e.prevRotationYaw + 360.0F;
            } else {
                prevYaw = e.prevRotationYaw;
            }

            float yaw = -(prevYaw + (e.rotationYaw - prevYaw) * par9) - 180.0F;
            float pitch = -(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * par9);
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            this.bindTexture("textures/" + this.textureName + ".png");
            this.model.renderModel(0.0, 0.0, 0.1F);
            GlStateManager.popMatrix();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEX_DEFAULT;
    }
}
