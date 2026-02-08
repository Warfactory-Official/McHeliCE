package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.norwood.mcheli.wrapper.modelloader.ModelVBO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;

public class WidgetAircraftViewport extends Widget<WidgetAircraftViewport> {

    public ModelVBO modelVBO;
    public ResourceLocation location;
    public MCH_AircraftInfo info;

    public WidgetAircraftViewport(ModelVBO modelVBO, ResourceLocation location, MCH_AircraftInfo info) {
        this.modelVBO = modelVBO;
        this.location = location;
        this.info = info;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        double safeWidth = Math.max(Math.max(modelVBO.sizeX, modelVBO.sizeZ), 0.001);
        double safeHeight = Math.max(modelVBO.sizeY, 0.001);

        double sclX = getArea().getWidth() / safeWidth;
        double sclY = getArea().getHeight() / safeHeight;
        double scl = 0.9 * Math.min(sclX, sclY);
        GlStateManager.pushMatrix();
        {
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableTexture2D();

            double localCenterX = getArea().getWidth() / 2.0;
            double localCenterY = getArea().getHeight() / 2.0;
            GlStateManager.translate(localCenterX, localCenterY, 100.0);

            GlStateManager.scale(scl, -scl, scl);

            float angle = (System.currentTimeMillis() % 18000L) / 18000.0f * 360.0f;
            GlStateManager.rotate(angle, 0, 1, 0);

            double meshCenterX = (modelVBO.minX + modelVBO.maxX) / 2.0;
            double meshCenterY = (modelVBO.minY + modelVBO.maxY) / 2.0;
            double meshCenterZ = (modelVBO.minZ + modelVBO.maxZ) / 2.0;

            GlStateManager.translate(-meshCenterX, -meshCenterY, -meshCenterZ);
            Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            modelVBO.renderStatic(info);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
        }
        GlStateManager.popMatrix();

    }
}
