package com.norwood.mcheli.gui;

import com.norwood.mcheli.wrapper.W_McClient;
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

import java.util.Random;

@SideOnly(Side.CLIENT)
public abstract class MCH_Gui extends GuiScreen {

    public static int scaleFactor;
    protected final Random rand = new Random();
    protected int centerX = 0;
    protected int centerY = 0;
    protected float smoothCamPartialTicks;

    protected static final int CIRCLE_SEGMENTS = 32;
    protected static final double[] CIRCLE_SIN = new double[CIRCLE_SEGMENTS];
    protected static final double[] CIRCLE_COS = new double[CIRCLE_SEGMENTS];

    private static final float VANILLA_TEXTURE_SCALE = 1.0F / 256.0F;
    private static final int DIGIT_TEXTURE_SIZE = 16;
    private static final int DIGIT_MINUS_U_OFFSET = 160;

    static {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / CIRCLE_SEGMENTS;
            CIRCLE_SIN[i] = Math.sin(angle);
            CIRCLE_COS[i] = Math.cos(angle);
        }
    }

    public MCH_Gui(Minecraft minecraft) {
        this.mc = minecraft;
        this.smoothCamPartialTicks = 0.0F;
        this.zLevel = -110.0F;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void onTick() {
    }

    public abstract boolean isDrawGui(EntityPlayer player);

    public abstract void drawGui(EntityPlayer player, boolean isThirdPersonView);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.smoothCamPartialTicks = partialTicks;
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        scaleFactor = scaledResolution.getScaleFactor();

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

    public void drawTexturedModalRectRotate(double left, double top, double width, double height,
                                            double uLeft, double vTop, double uWidth, double vHeight, float rotationDegrees) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(left + width / 2.0, top + height / 2.0, 0.0);
        GlStateManager.rotate(rotationDegrees, 0.0F, 0.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(7, DefaultVertexFormats.POSITION_TEX);

        builder.pos(-width / 2.0, height / 2.0, this.zLevel).tex(uLeft * VANILLA_TEXTURE_SCALE, (vTop + vHeight) * VANILLA_TEXTURE_SCALE).endVertex();
        builder.pos(width / 2.0, height / 2.0, this.zLevel).tex((uLeft + uWidth) * VANILLA_TEXTURE_SCALE, (vTop + vHeight) * VANILLA_TEXTURE_SCALE).endVertex();
        builder.pos(width / 2.0, -height / 2.0, this.zLevel).tex((uLeft + uWidth) * VANILLA_TEXTURE_SCALE, vTop * VANILLA_TEXTURE_SCALE).endVertex();
        builder.pos(-width / 2.0, -height / 2.0, this.zLevel).tex(uLeft * VANILLA_TEXTURE_SCALE, vTop * VANILLA_TEXTURE_SCALE).endVertex();

        tessellator.draw();
        GlStateManager.popMatrix();
    }

    public void drawTexturedRect(double left, double top, double width, double height,
                                 double uLeft, double vTop, double uWidth, double vHeight, double textureWidth, double textureHeight) {
        float scaleX = (float) (1.0 / textureWidth);
        float scaleY = (float) (1.0 / textureHeight);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(7, DefaultVertexFormats.POSITION_TEX);

        builder.pos(left, top + height, this.zLevel).tex(uLeft * scaleX, (vTop + vHeight) * scaleY).endVertex();
        builder.pos(left + width, top + height, this.zLevel).tex((uLeft + uWidth) * scaleX, (vTop + vHeight) * scaleY).endVertex();
        builder.pos(left + width, top, this.zLevel).tex((uLeft + uWidth) * scaleX, vTop * scaleY).endVertex();
        builder.pos(left, top, this.zLevel).tex(uLeft * scaleX, vTop * scaleY).endVertex();

        tessellator.draw();
    }

    public void drawLine(double[] coordinates, int color) {
        int glDrawModeLines = 1;
        this.drawLine(coordinates, color, glDrawModeLines);
    }

    public void drawString(String text, int x, int y, int color) {
        this.drawString(this.mc.fontRenderer, text, x, y, color);
    }

    public void drawDigit(String digits, int x, int y, int kerningInterval, int color) {
        GlStateManager.enableBlend();
        GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF));
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        W_McClient.MOD_bindTexture("textures/gui/digit.png");

        for (int i = 0; i < digits.length(); i++) {
            char character = digits.charAt(i);
            if (character >= '0' && character <= '9') {
                this.drawTexturedModalRect(x + kerningInterval * i, y, DIGIT_TEXTURE_SIZE * (character - '0'), 0, DIGIT_TEXTURE_SIZE, DIGIT_TEXTURE_SIZE);
            }

            if (character == '-') {
                this.drawTexturedModalRect(x + kerningInterval * i, y, DIGIT_MINUS_U_OFFSET, 0, DIGIT_TEXTURE_SIZE, DIGIT_TEXTURE_SIZE);
            }
        }

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    public void drawCenteredString(String text, int x, int y, int color) {
        this.drawCenteredString(this.mc.fontRenderer, text, x, y, color);
    }

    public void drawLine(double[] coordinates, int color, int glDrawMode) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color >> 0 & 0xFF), (byte) (color >> 24 & 0xFF));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(glDrawMode, DefaultVertexFormats.POSITION);

        for (int i = 0; i < coordinates.length; i += 2) {
            builder.pos(coordinates[i], coordinates[i + 1], this.zLevel).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GlStateManager.popMatrix();
    }
}
