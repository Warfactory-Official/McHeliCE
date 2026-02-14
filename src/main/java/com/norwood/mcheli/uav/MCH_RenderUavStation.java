package com.norwood.mcheli.uav;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.uav.IUavStation.StationType;
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

    public static final String[] MODELS = {"uav_station", "uav_portable_controller"};
    public static final String[] TEX_ON = {"uav_station_on", "uav_portable_controller_on"};
    public static final String[] TEX_OFF = {"uav_station", "uav_portable_controller"};

    public MCH_RenderUavStation(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0F;
    }

    @Override
    public void doRender(@NotNull MCH_EntityUavStation entity, double x, double y, double z, float yaw, float tickTime) {
        StationType type = entity.getType();
        int index = type == StationType.SMALL ? 1 : 0;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + W_Entity.GLOBAL_Y_OFFSET, z);
        GlStateManager.enableCull();
        GlStateManager.rotate(entity.rotationYaw, 0.0F, -1.0F, 0.0F);
        GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.color(0.75F, 0.75F, 0.75F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        String texName = switch (type) {
            case DEFAULT -> (entity.getControlled() != null && entity.getRiddenByEntity() != null)
                    ? TEX_ON[index] : TEX_OFF[index];
            case SMALL   -> (entity.coverRotation > 0.95F)
                    ? TEX_ON[index] : TEX_OFF[index];
        };
        this.bindTexture("textures/" + texName + ".png");

        switch (type) {
            case DEFAULT -> MCH_ModelManager.render(MODELS[index]);
            case SMALL   -> renderPortableController(entity, MODELS[index], tickTime);
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderPortableController(MCH_EntityUavStation uav, String model, float tickTime) {
        MCH_ModelManager.renderPart(model, "$body");

        float rot = MCH_Lib.smooth(uav.coverRotation, uav.prevCoverRotation, tickTime);

        renderRotPart(model, "$cover",        rot * 60.0F,  0.0, -0.1812, -0.3186);
        renderRotPart(model, "$laptop_cover", rot * 95.0F,  0.0, -0.1808, -0.0422);
        renderRotPart(model, "$display",      rot * -85.0F, 0.0, -0.1807,  0.2294);
    }

    private void renderRotPart(String model, String part, float rot, double x, double y, double z) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(rot, -1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-x, -y, -z);
        MCH_ModelManager.renderPart(model, part);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(@NotNull MCH_EntityUavStation entity) {
        return TEX_DEFAULT;
    }

}
