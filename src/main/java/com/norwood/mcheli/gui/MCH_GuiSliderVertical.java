package com.norwood.mcheli.gui;

import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.wrapper.W_GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class MCH_GuiSliderVertical extends W_GuiButton {

    public final float valueMin;
    public float valueMax;
    public final float valueStep;
    private float currentSlider;
    private boolean isMousePress;

    public MCH_GuiSliderVertical(
                                 int gui_id, int posX, int posY, int sliderWidth, int sliderHeight, String string,
                                 float defaultSliderPos, float minVal, float maxVal, float step) {
        super(gui_id, posX, posY, sliderWidth, sliderHeight, string);
        this.valueMin = minVal;
        this.valueMax = maxVal;
        this.valueStep = step;
        this.setSliderValue(defaultSliderPos);
    }

    public int getHoverState(boolean mouseOver) {
        return 0;
    }

    public void scrollUp(float a) {
        if (this.isVisible() && !this.isMousePress) {
            this.setSliderValue(this.getSliderValue() + this.valueStep * a);
        }
    }

    public void scrollDown(float a) {
        if (this.isVisible() && !this.isMousePress) {
            this.setSliderValue(this.getSliderValue() - this.valueStep * a);
        }
    }

    protected void mouseDragged(@NotNull Minecraft mc, int x, int y) {
        if (this.isVisible()) {
            if (this.isMousePress) {
                this.currentSlider = (float) (y - (this.y + 4)) / (this.height - 8);
                if (this.currentSlider < 0.0F) {
                    this.currentSlider = 0.0F;
                }

                if (this.currentSlider > 1.0F) {
                    this.currentSlider = 1.0F;
                }

                this.currentSlider = this.normalizeValue(this.denormalizeValue(this.currentSlider));
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.x, this.y + (int) (this.currentSlider * (this.height - 8)), 66, 0, 20, 4);
            this.drawTexturedModalRect(this.x, this.y + (int) (this.currentSlider * (this.height - 8)) + 4, 66, 196, 20,
                    4);
            if (!MCH_Key.isKeyDown(-100)) {
                this.mouseReleased(x, y);
            }
        }
    }

    public float getSliderValue() {
        return this.denormalizeValue(this.currentSlider);
    }

    public void setSliderValue(float f) {
        this.currentSlider = this.normalizeValue(f);
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
            this.currentSlider = (float) (y - (this.y + 4)) / (this.height - 8);
            if (this.currentSlider < 0.0F) {
                this.currentSlider = 0.0F;
            }

            if (this.currentSlider > 1.0F) {
                this.currentSlider = 1.0F;
            }

            this.isMousePress = true;
            return true;
        } else {
            return false;
        }
    }

    public void mouseReleased(int mouseX, int mouseY) {
        this.isMousePress = false;
    }

    public void drawButton(@NotNull Minecraft mc, int x, int y, float partialTicks) {
        if (this.isVisible()) {
            FontRenderer fontrenderer = mc.fontRenderer;
            mc.getTextureManager().bindTexture(new ResourceLocation(Tags.MODID, "textures/gui/widgets.png"));
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.setOnMouseOver(x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height);
            int k = this.getHoverState(this.isOnMouseOver());
            this.enableBlend();
            this.drawTexturedModalRect(this.x, this.y, 46 + k * 20, 0, this.width, this.height / 2);
            this.drawTexturedModalRect(this.x, this.y + this.height / 2, 46 + k * 20, 200 - this.height / 2, this.width,
                    this.height / 2);
            this.mouseDragged(mc, x, y);
            int l = 14737632;
            if (this.packedFGColour != 0) {
                l = this.packedFGColour;
            } else if (!this.enabled) {
                l = 10526880;
            } else if (this.isOnMouseOver()) {
                l = 16777120;
            }

            this.drawCenteredString(fontrenderer, this.displayString, this.x + this.width / 2,
                    this.y + (this.height - 8) / 2, l);
            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
        }
    }
}
