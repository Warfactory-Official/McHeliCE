package com.norwood.mcheli.uav;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderUavStation extends W_Render<MCH_EntityUavStation> {

    public static final IRenderFactory<MCH_EntityUavStation> FACTORY = MCH_RenderUavStation::new;
    public static final String[] MODEL_NAME = new String[] { "uav_station", "uav_portable_controller" };
    public static final String[] TEX_NAME_ON = new String[] { "uav_station_on", "uav_portable_controller_on" };
    public static final String[] TEX_NAME_OFF = new String[] { "uav_station", "uav_portable_controller" };

    public MCH_RenderUavStation(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0F;
    }

    public void doRender(@NotNull MCH_EntityUavStation entity, double posX, double posY, double posZ, float par8,
                         float tickTime) {
        if (entity.getKind() > 0) {
            int kind = entity.getKind() - 1;
            GlStateManager.pushMatrix();
            GlStateManager.translate(posX, posY + W_Entity.GLOBAL_Y_OFFSET, posZ);
            GlStateManager.enableCull();
            GlStateManager.rotate(entity.rotationYaw, 0.0F, -1.0F, 0.0F);
            GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (kind == 0) {
                if (entity.getControlAircract() != null && entity.getRiddenByEntity() != null) {
                    this.bindTexture("textures/" + TEX_NAME_ON[kind] + ".png");
                } else {
                    this.bindTexture("textures/" + TEX_NAME_OFF[kind] + ".png");
                }

                MCH_ModelManager.render(MODEL_NAME[kind]);
            } else {
                if (entity.rotCover > 0.95F) {
                    this.bindTexture("textures/" + TEX_NAME_ON[kind] + ".png");
                } else {
                    this.bindTexture("textures/" + TEX_NAME_OFF[kind] + ".png");
                }

                this.renderPortableController(entity, MODEL_NAME[kind], tickTime);
            }

            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    public void renderPortableController(MCH_EntityUavStation uavSt, String name, float tickTime) {
        MCH_ModelManager.renderPart(name, "$body");
        float rot = MCH_Lib.smooth(uavSt.rotCover, uavSt.prevRotCover, tickTime);
        this.renderRotPart(name, "$cover", rot * 60.0F, 0.0, -0.1812, -0.3186);
        this.renderRotPart(name, "$laptop_cover", rot * 95.0F, 0.0, -0.1808, -0.0422);
        this.renderRotPart(name, "$display", rot * -85.0F, 0.0, -0.1807, 0.2294);
    }

    private void renderRotPart(String modelName, String partName, float rot, double x, double y, double z) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(rot, -1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-x, -y, -z);
        MCH_ModelManager.renderPart(modelName, partName);
        GlStateManager.popMatrix();
    }

    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityUavStation entity) {
        return TEX_DEFAULT;
    }
}
