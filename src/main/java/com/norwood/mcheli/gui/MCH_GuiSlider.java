package com.norwood.mcheli.gui;

import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.wrapper.W_GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class MCH_GuiSlider extends W_GuiButton {

    public final String stringFormat;
    public final float valueMin;
    public final float valueMax;
    public final float valueStep;
    private float currentSlider;
    private boolean isMousePress;

    public MCH_GuiSlider(
                         int gui_id, int posX, int posY, int sliderWidth, int sliderHeight, String string_format,
                         float defaultSliderPos, float minVal, float maxVal, float step) {
        super(gui_id, posX, posY, sliderWidth, sliderHeight, "");
        this.stringFormat = string_format;
        this.valueMin = minVal;
        this.valueMax = maxVal;
        this.valueStep = step;
        this.setSliderValue(defaultSliderPos);
    }

    public int getHoverState(boolean mouseOver) {
        return 0;
    }

    protected void mouseDragged(@NotNull Minecraft mc, int x, int y) {
        if (this.isVisible()) {
            if (this.isMousePress) {
                this.currentSlider = (float) (x - (this.x + 4)) / (this.width - 8);
                if (this.currentSlider < 0.0F) {
                    this.currentSlider = 0.0F;
                }

                if (this.currentSlider > 1.0F) {
                    this.currentSlider = 1.0F;
                }

                this.currentSlider = this.normalizeValue(this.denormalizeValue(this.currentSlider));
                this.updateDisplayString();
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.x + (int) (this.currentSlider * (this.width - 8)), this.y, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.x + (int) (this.currentSlider * (this.width - 8)) + 4, this.y, 196, 66, 4,
                    20);
            if (!MCH_Key.isKeyDown(-100)) {
                this.mouseReleased(x, y);
            }
        }
    }

    public void updateDisplayString() {
        this.displayString = String.format(this.stringFormat, this.denormalizeValue(this.currentSlider));
    }

    public float getSliderValue() {
        return this.denormalizeValue(this.currentSlider);
    }

    public void setSliderValue(float f) {
        this.currentSlider = this.normalizeValue(f);
        this.updateDisplayString();
    }

    public float getSliderValueInt(int digit) {
        int d = 1;

        while (digit > 0) {
            d *= 10;
            digit--;
        }

        int n = (int) (this.denormalizeValue(this.currentSlider) * d);
        return (float) n / d;
    }

    public float normalizeValue(float f) {
        return MathHelper.clamp((this.snapToStepClamp(f) - this.valueMin) / (this.valueMax - this.valueMin), 0.0F,
                1.0F);
    }

    public float denormalizeValue(float f) {
        return this.snapToStepClamp(this.valueMin + (this.valueMax - this.valueMin) * MathHelper.clamp(f, 0.0F, 1.0F));
    }

    public float snapToStepClamp(float f) {
        f = this.snapToStep(f);
        return MathHelper.clamp(f, this.valueMin, this.valueMax);
    }

    protected float snapToStep(float f) {
        if (this.valueStep > 0.0F) {
            f = this.valueStep * Math.round(f / this.valueStep);
        }

        return f;
    }

    public boolean mousePressed(@NotNull Minecraft mc, int x, int y) {
        if (super.mousePressed(mc, x, y)) {
            this.currentSlider = (float) (x - (this.x + 4)) / (this.width - 8);
            if (this.currentSlider < 0.0F) {
                this.currentSlider = 0.0F;
            }

            if (this.currentSlider > 1.0F) {
                this.currentSlider = 1.0F;
            }

            this.updateDisplayString();
            this.isMousePress = true;
            return true;
        } else {
            return false;
        }
    }

    public void mouseReleased(int mouseX, int mouseY) {
        this.isMousePress = false;
    }
}
