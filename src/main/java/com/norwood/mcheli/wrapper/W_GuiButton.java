package com.norwood.mcheli.wrapper;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

public class W_GuiButton extends GuiButton {

    public List<String> hoverStringList = null;

    public W_GuiButton(int par1, int par2, int par3, int par4, int par5, String par6Str) {
        super(par1, par2, par3, par4, par5, par6Str);
    }

    public static void setVisible(GuiButton button, boolean b) {
        button.visible = b;
    }

    public void addHoverString(String s) {
        if (this.hoverStringList == null) {
            this.hoverStringList = new ArrayList<>();
        }

        this.hoverStringList.add(s);
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean b) {
        this.visible = b;
    }

    public void enableBlend() {
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public boolean isOnMouseOver() {
        return this.hovered;
    }

    public void setOnMouseOver(boolean b) {
        this.hovered = b;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
