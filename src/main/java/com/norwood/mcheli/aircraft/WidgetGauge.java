package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.value.ISyncOrValue;
import com.cleanroommc.modularui.api.value.sync.IFloatSyncValue;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.norwood.mcheli.Tags;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class WidgetGauge extends Widget<WidgetGauge> {
    public static final boolean DEBUG = false;
    public final static ResourceLocation GAUGE = new ResourceLocation(Tags.MODID, "gui/uigauge");
    private static float[] PUV = uvFromPx(220, 9, 252, 114, 256, 256);
    public static final UITexture POINTER = new UITexture(new ResourceLocation(Tags.MODID, "gui/uigauge.png"), PUV[0], PUV[1], PUV[2], PUV[3], null);
    private final float ABS_ANGLE = 65f;

    public WidgetGauge() {
    }

    public static float[] uvFromPx(int x0, int y0, int x1, int y1, int width, int height) {
        return new float[]{x0 / (float) width, y0 / (float) height, x1 / (float) width, y1 / (float) height};
    }

    public static WidgetGauge make() {
        return new WidgetGauge().background(new UITexture(GAUGE, 0.03125f, 0.03125f, 0.82422f, 0.46094f, null));
    }

    public boolean isValidSyncOrValue(@NotNull ISyncOrValue syncOrValue) {
        return true;
    }

    public WidgetGauge value(IFloatSyncValue<?> supplier) {
        setSyncOrValue(supplier);
        return this;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);

        float val = DEBUG ?
                (float) (Math.sin(System.currentTimeMillis() / 2000.0 * Math.PI * 2) + 1) / 2.0f :
                getValue() instanceof IFloatSyncValue<?> s ? s.getFloatValue() : 0.5f;
        float mapped = (val * 2.0f) - 1.0f;
        float angle = mapped * ABS_ANGLE;

        int w = 17;
        int h = 51;
        int pivotFromBottom = 9;
        int localPivotY = h - pivotFromBottom;

        float screenPivotX = 40 + (w / 2f);
        float screenPivotY = 6 + localPivotY;

        GlStateManager.pushMatrix();
        GlStateManager.translate(screenPivotX, screenPivotY, 0);
        GlStateManager.rotate(angle, 0, 0, 1);

        POINTER.draw(-w / 2f, -localPivotY, w, h);

        if (DEBUG) {
            // Debug Point
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
            bufferbuilder.pos(-0.5, -0.5, 0).color(255, 0, 0, 255).endVertex();
            bufferbuilder.pos(-0.5, 0.5, 0).color(255, 0, 0, 255).endVertex();
            bufferbuilder.pos(0.5, 0.5, 0).color(255, 0, 0, 255).endVertex();
            bufferbuilder.pos(0.5, -0.5, 0).color(255, 0, 0, 255).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        GlStateManager.popMatrix();
    }
}
