package com.norwood.mcheli.gui;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_ScaledResolution;

@SideOnly(Side.CLIENT)
public abstract class MCH_Gui extends GuiScreen {

    public static int scaleFactor;
    protected int centerX = 0;
    protected int centerY = 0;
    protected final Random rand = new Random();
    protected float smoothCamPartialTicks;

    public MCH_Gui(Minecraft minecraft) {
        this.mc = minecraft;
        this.smoothCamPartialTicks = 0.0F;
        this.zLevel = -110.0F;
    }

    public void initGui() {
        super.initGui();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public void onTick() {}

    public abstract boolean isDrawGui(EntityPlayer var1);

    public abstract void drawGui(EntityPlayer var1, boolean var2);

    public void drawScreen(int par1, int par2, float partialTicks) {
        this.smoothCamPartialTicks = partialTicks;
        ScaledResolution scaledresolution = new W_ScaledResolution(this.mc, this.mc.displayWidth,
                this.mc.displayHeight);
        scaleFactor = scaledresolution.getScaleFactor();
        if (!this.mc.gameSettings.hideGUI) {
            this.width = this.mc.displayWidth / scaleFactor;
            this.height = this.mc.displayHeight / scaleFactor;
            this.centerX = this.width / 2;
            this.centerY = this.height / 2;
            GlStateManager.pushMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.mc.player != null) {
                this.drawGui(this.mc.player, this.mc.gameSettings.thirdPersonView != 0);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    public void drawTexturedModalRectRotate(
                                            double left, double top, double width, double height, double uLeft,
                                            double vTop, double uWidth, double vHeight, float rot) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(left + width / 2.0, top + height / 2.0, 0.0);
        GlStateManager.rotate(rot, 0.0F, 0.0F, 1.0F);
        float f = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(7, DefaultVertexFormats.POSITION_TEX);
        builder.pos(-width / 2.0, height / 2.0, this.zLevel).tex(uLeft * f, (vTop + vHeight) * f).endVertex();
        builder.pos(width / 2.0, height / 2.0, this.zLevel).tex((uLeft + uWidth) * f, (vTop + vHeight) * f).endVertex();
        builder.pos(width / 2.0, -height / 2.0, this.zLevel).tex((uLeft + uWidth) * f, vTop * f).endVertex();
        builder.pos(-width / 2.0, -height / 2.0, this.zLevel).tex(uLeft * f, vTop * f).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    public void drawTexturedRect(
                                 double left, double top, double width, double height, double uLeft, double vTop,
                                 double uWidth, double vHeight, double textureWidth, double textureHeight) {
        float fx = (float) (1.0 / textureWidth);
        float fy = (float) (1.0 / textureHeight);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(7, DefaultVertexFormats.POSITION_TEX);
        builder.pos(left, top + height, this.zLevel).tex(uLeft * fx, (vTop + vHeight) * fy).endVertex();
        builder.pos(left + width, top + height, this.zLevel).tex((uLeft + uWidth) * fx, (vTop + vHeight) * fy)
                .endVertex();
        builder.pos(left + width, top, this.zLevel).tex((uLeft + uWidth) * fx, vTop * fy).endVertex();
        builder.pos(left, top, this.zLevel).tex(uLeft * fx, vTop * fy).endVertex();
        tessellator.draw();
    }

    public void drawLine(double[] line, int color) {
        this.drawLine(line, color, 1);
    }

    public void drawString(String s, int x, int y, int color) {
        this.drawString(this.mc.fontRenderer, s, x, y, color);
    }

    public void drawDigit(String s, int x, int y, int interval, int color) {
        GlStateManager.enableBlend();
        GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF),
                (byte) (color >> 24 & 0xFF));
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        W_McClient.MOD_bindTexture("textures/gui/digit.png");

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                this.drawTexturedModalRect(x + interval * i, y, 16 * (c - '0'), 0, 16, 16);
            }

            if (c == '-') {
                this.drawTexturedModalRect(x + interval * i, y, 160, 0, 16, 16);
            }
        }

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    public void drawCenteredString(String s, int x, int y, int color) {
        this.drawCenteredString(this.mc.fontRenderer, s, x, y, color);
    }

    public void drawLine(double[] line, int color, int mode) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color >> 0 & 0xFF),
                (byte) (color >> 24 & 0xFF));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(mode, DefaultVertexFormats.POSITION);

        for (int i = 0; i < line.length; i += 2) {
            builder.pos(line[i], line[i + 1], this.zLevel).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GlStateManager.popMatrix();
    }
}
