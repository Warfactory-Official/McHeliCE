package com.norwood.mcheli.parachute;

import java.util.Random;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.wrapper.W_Render;

@SideOnly(Side.CLIENT)
public class MCH_RenderParachute extends W_Render<MCH_EntityParachute> {

    public static final IRenderFactory<MCH_EntityParachute> FACTORY = MCH_RenderParachute::new;
    public static final Random rand = new Random();

    public MCH_RenderParachute(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    public void doRender(@NotNull MCH_EntityParachute entity, double posX, double posY, double posZ, float par8,
                         float tickTime) {
        int type = entity.getType();
        if (type > 0) {
            GlStateManager.pushMatrix();
            GlStateManager.enableCull();
            GlStateManager.translate(posX, posY, posZ);
            float prevYaw = entity.prevRotationYaw;
            if (entity.rotationYaw - prevYaw < -180.0F) {
                prevYaw -= 360.0F;
            } else if (prevYaw - entity.rotationYaw < -180.0F) {
                prevYaw += 360.0F;
            }

            float yaw = prevYaw + (entity.rotationYaw - prevYaw) * tickTime;
            GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
            GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (MCH_Config.SmoothShading.prmBool) {
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
            }

            switch (type) {
                case 1:
                    this.bindTexture("textures/parachute1.png");
                    MCH_ModelManager.render("parachute1");
                    break;
                case 2:
                    this.bindTexture("textures/parachute2.png");
                    if (entity.isOpenParachute()) {
                        MCH_ModelManager.renderPart("parachute2", "$parachute");
                    } else {
                        MCH_ModelManager.renderPart("parachute2", "$seat");
                    }
                    break;
                case 3:
                    this.bindTexture("textures/parachute2.png");
                    MCH_ModelManager.renderPart("parachute2", "$parachute");
            }

            GlStateManager.disableBlend();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();
        }
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityParachute entity) {
        return TEX_DEFAULT;
    }
}
