package com.norwood.mcheli.gui;

import com.norwood.mcheli.weapon.GPSPosition;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Manual GPS target-coordinate entry panel (Reforged feature).
 *
 * <p>Ported from the 1.7.10 {@code MCH_GuiGPSInput} to the 1.12 {@link GuiScreen} API. Entering
 * X/Y/Z and confirming routes the coordinates through CE's existing {@link GPSPosition#set}, which
 * already mirrors the value client-side and syncs it to the server via {@code PacketResetGPS} for
 * GPS-guided weapons. Opened with the "Open GPS Panel" keybind (default K) while operating an
 * aircraft.</p>
 */
public class MCH_GuiGPSInput extends GuiScreen {

    private final EntityPlayer player;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;
    private String message = "";

    public MCH_GuiGPSInput(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(3, centerX - 102, centerY + 44, 204, 20, "Fill with player position"));
        this.buttonList.add(new GuiButton(1, centerX - 102, centerY + 68, 100, 20, "Done"));
        this.buttonList.add(new GuiButton(2, centerX + 2, centerY + 68, 100, 20, "Cancel"));
        this.xField = new GuiTextField(0, this.fontRenderer, centerX - 100, centerY - 34, 200, 20);
        this.yField = new GuiTextField(1, this.fontRenderer, centerX - 100, centerY - 8, 200, 20);
        this.zField = new GuiTextField(2, this.fontRenderer, centerX - 100, centerY + 18, 200, 20);
        this.xField.setMaxStringLength(24);
        this.yField.setMaxStringLength(24);
        this.zField.setMaxStringLength(24);
        this.xField.setFocused(true);

        GPSPosition gps = GPSPosition.currentClientGPSPosition;
        if (gps != null && gps.isActive()) {
            this.xField.setText(String.valueOf(gps.x));
            this.yField.setText(String.valueOf(gps.y));
            this.zField.setText(String.valueOf(gps.z));
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.xField.updateCursorCounter();
        this.yField.updateCursorCounter();
        this.zField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) {
            return;
        }
        if (button.id == 2) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (button.id == 1) {
            this.applyGPS();
            return;
        }
        if (button.id == 3) {
            this.fillFromPlayerPos();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            if (this.xField.isFocused()) {
                this.xField.setFocused(false);
                this.yField.setFocused(true);
            } else if (this.yField.isFocused()) {
                this.yField.setFocused(false);
                this.zField.setFocused(true);
            } else {
                this.zField.setFocused(false);
                this.xField.setFocused(true);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            this.applyGPS();
            return;
        }
        this.xField.textboxKeyTyped(typedChar, keyCode);
        this.yField.textboxKeyTyped(typedChar, keyCode);
        this.zField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.xField.mouseClicked(mouseX, mouseY, mouseButton);
        this.yField.mouseClicked(mouseX, mouseY, mouseButton);
        this.zField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.drawCenteredString(this.fontRenderer, "GPS Target Coordinates", centerX, centerY - 62, 0xFFFFFF);
        this.drawString(this.fontRenderer, "X", centerX - 112, centerY - 28, 0xFFFFFF);
        this.drawString(this.fontRenderer, "Y", centerX - 112, centerY - 2, 0xFFFFFF);
        this.drawString(this.fontRenderer, "Z", centerX - 112, centerY + 24, 0xFFFFFF);
        this.xField.drawTextBox();
        this.yField.drawTextBox();
        this.zField.drawTextBox();
        if (this.message != null && !this.message.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, this.message, centerX, centerY + 94, 0xFF5555);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void applyGPS() {
        try {
            double x = Double.parseDouble(this.xField.getText().trim());
            double y = Double.parseDouble(this.yField.getText().trim());
            double z = Double.parseDouble(this.zField.getText().trim());
            GPSPosition.set(x, y, z, true, this.player);
            this.mc.displayGuiScreen(null);
        } catch (NumberFormatException e) {
            this.message = "Invalid coordinates";
        }
    }

    private void fillFromPlayerPos() {
        this.xField.setText(String.valueOf(this.player.posX));
        this.yField.setText(String.valueOf(this.player.posY));
        this.zField.setText(String.valueOf(this.player.posZ));
        this.message = "";
    }
}
