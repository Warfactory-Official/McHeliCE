package com.norwood.mcheli.wrapper;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.math.MathHelper;

public abstract class W_GuiContainer extends GuiContainer {

    private float time;

    public W_GuiContainer(Container par1Container) {
        super(par1Container);
    }

    public void drawItemStack(ItemStack item, int x, int y) {
        if (!item.isEmpty()) {
            item.getItem();
            FontRenderer font = item.getItem().getFontRenderer(item);
            if (font == null) {
                font = this.fontRenderer;
            }

            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            this.itemRender.renderItemAndEffectIntoGUI(item, x, y);
            this.itemRender.renderItemOverlayIntoGUI(font, item, x, y, null);
            this.zLevel = 0.0F;
            this.itemRender.zLevel = 0.0F;
        }
    }

    public void drawIngredient(Ingredient ingredient, int x, int y) {
        if (ingredient != Ingredient.EMPTY) {
            ItemStack[] itemstacks = ingredient.getMatchingStacks();
            int index = MathHelper.floor(this.time / 20.0F) % itemstacks.length;
            this.drawItemStack(itemstacks[index], x, y);
        }
    }

    public void drawString(String s, int x, int y, int color) {
        this.drawString(this.fontRenderer, s, x, y, color);
    }

    public void drawCenteredString(String s, int x, int y, int color) {
        this.drawCenteredString(this.fontRenderer, s, x, y, color);
    }

    public int getStringWidth(String s) {
        return this.fontRenderer.getStringWidth(s);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.time += partialTicks;
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public float getAnimationTime() {
        return this.time;
    }
}
