package com.norwood.mcheli.gltd;

import com.norwood.mcheli.MCH_RenderLib;
import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class MCH_RenderGLTD extends W_Render<MCH_EntityGLTD> {

    public static final IRenderFactory<MCH_EntityGLTD> FACTORY = MCH_RenderGLTD::new;
    public static final Random rand = new Random();
    public static _IModelCustom model;

    public MCH_RenderGLTD(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    public void doRender(MCH_EntityGLTD entity, double posX, double posY, double posZ, float par8, float tickTime) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY + 0.25, posZ);
        this.setCommonRenderParam(true, entity.getBrightnessForRender());
        this.bindTexture("textures/gltd.png");
        Minecraft mc = Minecraft.getMinecraft();
        boolean isNotRenderHead = false;
        if (entity.getRiddenByEntity() != null) {
            entity.isUsedPlayer = true;
            entity.renderRotaionYaw = entity.getRiddenByEntity().rotationYaw;
            entity.renderRotaionPitch = entity.getRiddenByEntity().rotationPitch;
            isNotRenderHead = mc.gameSettings.thirdPersonView == 0 && W_Lib.isClientPlayer(entity.getRiddenByEntity());
        }

        if (entity.isUsedPlayer) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            model.renderPart("$body");
            GlStateManager.popMatrix();
        } else {
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            model.renderPart("$body");
        }

        GlStateManager.translate(0.0F, 0.45F, 0.0F);
        if (entity.isUsedPlayer) {
            GlStateManager.rotate(entity.renderRotaionYaw, 0.0F, -1.0F, 0.0F);
            GlStateManager.rotate(entity.renderRotaionPitch, 1.0F, 0.0F, 0.0F);
        }

        GlStateManager.translate(0.0F, -0.45F, 0.0F);
        if (!isNotRenderHead) {
            model.renderPart("$head");
        }

        GlStateManager.translate(0.0F, 0.45F, 0.0F);
        this.restoreCommonRenderParam();
        GlStateManager.disableLighting();
        Vec3d[] v = new Vec3d[] { new Vec3d(0.0, 0.2, 0.0), new Vec3d(0.0, 0.2, 100.0) };
        int a = rand.nextInt(64);
        MCH_RenderLib.drawLine(v, 1619066752 | a << 24);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    public boolean shouldRender(@NotNull MCH_EntityGLTD livingEntity, @NotNull ICamera camera, double camX, double camY,
                                double camZ) {
        return true;
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityGLTD entity) {
        return TEX_DEFAULT;
    }
}
