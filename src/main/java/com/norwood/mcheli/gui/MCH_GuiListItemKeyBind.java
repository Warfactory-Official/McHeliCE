package com.norwood.mcheli.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import com.norwood.mcheli.MCH_ConfigPrm;
import com.norwood.mcheli.MCH_KeyName;

public class MCH_GuiListItemKeyBind extends MCH_GuiListItem {

    public final int defaultKeycode;
    public final String displayString;
    public final GuiButton button;
    public final GuiButton buttonReset;
    public int keycode;
    public final MCH_ConfigPrm config;
    public GuiButton lastPushButton;

    public MCH_GuiListItemKeyBind(int id, int idReset, int posX, String dispStr, MCH_ConfigPrm prm) {
        this.displayString = dispStr;
        this.defaultKeycode = prm.prmIntDefault;
        this.button = new GuiButton(id, posX + 160, 0, 70, 20, "");
        this.buttonReset = new GuiButton(idReset, posX + 240, 0, 40, 20, "Reset");
        this.config = prm;
        this.lastPushButton = null;
        this.setKeycode(prm.prmInt);
    }

    @Override
    public void mouseReleased(int x, int y) {
        this.button.mouseReleased(x, y);
        this.buttonReset.mouseReleased(x, y);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int x, int y) {
        if (this.button.mousePressed(mc, x, y)) {
            this.lastPushButton = this.button;
            return true;
        } else if (this.buttonReset.mousePressed(mc, x, y)) {
            this.lastPushButton = this.buttonReset;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, int posX, int posY, float partialTicks) {
        int y = 6;
        this.button.drawString(mc.fontRenderer, this.displayString, posX + 10, posY + y, -1);
        this.button.y = posY;
        this.button.drawButton(mc, mouseX, mouseY, partialTicks);
        this.buttonReset.enabled = this.keycode != this.defaultKeycode;
        this.buttonReset.y = posY;
        this.buttonReset.drawButton(mc, mouseX, mouseY, partialTicks);
    }

    public void applyKeycode() {
        this.config.setPrm(this.keycode);
    }

    public void resetKeycode() {
        this.setKeycode(this.defaultKeycode);
    }

    public void setKeycode(int k) {
        if (k != 0 && !MCH_KeyName.getDescOrName(k).isEmpty()) {
            this.keycode = k;
            this.button.displayString = MCH_KeyName.getDescOrName(k);
        }
    }
}
