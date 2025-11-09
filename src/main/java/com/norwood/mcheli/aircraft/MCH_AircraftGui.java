package com.norwood.mcheli.aircraft;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.networking.packet.PacketCommandSave;
import com.norwood.mcheli.networking.packet.PacketHandleCommand;
import com.norwood.mcheli.networking.packet.PacketOpenScreen;
import com.norwood.mcheli.networking.packet.PacketRequestReload;
import com.norwood.mcheli.weapon.MCH_WeaponDummy;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_GuiContainer;
import com.norwood.mcheli.wrapper.W_McClient;

public class MCH_AircraftGui extends W_GuiContainer {

    public static final int BUTTON_RELOAD = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int BUTTON_PREV = 3;
    public static final int BUTTON_CLOSE = 4;
    public static final int BUTTON_CONFIG = 5;
    public static final int BUTTON_INVENTORY = 6;
    private final EntityPlayer thePlayer;
    private final MCH_EntityAircraft aircraft;
    private GuiButton buttonReload;
    private GuiButton buttonNext;
    private GuiButton buttonPrev;
    private GuiButton buttonInventory;
    private int currentWeaponId;
    private int reloadWait;
    private GuiTextField editCommand;

    public MCH_AircraftGui(EntityPlayer player, MCH_EntityAircraft ac) {
        super(new MCH_AircraftGuiContainer(player, ac));
        this.aircraft = ac;
        this.thePlayer = player;
        this.xSize = 210;
        this.ySize = 236;
        this.buttonReload = null;
        this.currentWeaponId = 0;
    }

    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.buttonReload = new GuiButton(BUTTON_RELOAD, this.guiLeft + 85, this.guiTop + 40, 50, 20, "Reload");
        this.buttonNext = new GuiButton(BUTTON_PREV, this.guiLeft + 140, this.guiTop + 40, 20, 20, "<<");
        this.buttonPrev = new GuiButton(BUTTON_NEXT, this.guiLeft + 160, this.guiTop + 40, 20, 20, ">>");
        this.buttonReload.enabled = this.canReload(this.thePlayer);
        this.buttonNext.enabled = this.aircraft.getWeaponNum() >= 2;
        this.buttonPrev.enabled = this.aircraft.getWeaponNum() >= 2;
        this.buttonInventory = new GuiButton(BUTTON_INVENTORY, this.guiLeft + 210 - 30 - 60, this.guiTop + 90, 80, 20,
                "Inventory");
        this.buttonList.add(
                new GuiButton(BUTTON_CONFIG, this.guiLeft + 210 - 30 - 60, this.guiTop + 110, 80, 20, "MOD Options"));
        this.buttonList
                .add(new GuiButton(BUTTON_CLOSE, this.guiLeft + 210 - 30 - 20, this.guiTop + 10, 40, 20, "Close"));
        this.buttonList.add(this.buttonReload);
        this.buttonList.add(this.buttonNext);
        this.buttonList.add(this.buttonPrev);
        if (this.aircraft.getSizeInventory() > 0) {
            this.buttonList.add(this.buttonInventory);
        }

        this.editCommand = new GuiTextField(0, this.fontRenderer, this.guiLeft + 25, this.guiTop + 215, 160, 15);
        this.editCommand.setText(this.aircraft.getCommand());
        this.editCommand.setMaxStringLength(512);
        this.currentWeaponId = 0;
        this.reloadWait = 10;
    }

    public void closeScreen() {
        PacketCommandSave.send(this.editCommand.getText());
        this.mc.player.closeScreen();
    }

    public boolean canReload(EntityPlayer player) {
        return this.aircraft.canPlayerSupplyAmmo(player, this.currentWeaponId);
    }

    public void updateScreen() {
        super.updateScreen();
        if (this.reloadWait > 0) {
            this.reloadWait--;
            if (this.reloadWait == 0) {
                this.buttonReload.enabled = this.canReload(this.thePlayer);
                this.reloadWait = 20;
            }
        }

        this.editCommand.updateCursorCounter();
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.editCommand.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void onGuiClosed() {
        super.onGuiClosed();
    }

    protected void actionPerformed(@NotNull GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button.enabled) {
            switch (button.id) {
                case BUTTON_RELOAD:
                    this.buttonReload.enabled = this.canReload(this.thePlayer);
                    if (this.buttonReload.enabled) {
                        PacketRequestReload.send(this.aircraft, this.currentWeaponId);
                        this.aircraft.supplyAmmo(this.currentWeaponId);
                        this.reloadWait = 3;
                        this.buttonReload.enabled = false;
                    }
                    break;
                case BUTTON_NEXT:
                    this.currentWeaponId++;
                    if (this.currentWeaponId >= this.aircraft.getWeaponNum()) {
                        this.currentWeaponId = 0;
                    }

                    this.buttonReload.enabled = this.canReload(this.thePlayer);
                    break;
                case BUTTON_PREV:
                    this.currentWeaponId--;
                    if (this.currentWeaponId < 0) {
                        this.currentWeaponId = this.aircraft.getWeaponNum() - 1;
                    }

                    this.buttonReload.enabled = this.canReload(this.thePlayer);
                    break;
                case BUTTON_CLOSE:
                    this.closeScreen();
                    break;
                case BUTTON_CONFIG:
                    PacketOpenScreen.send(2);
                    break;
                case BUTTON_INVENTORY:
                    PacketOpenScreen.send(3);
            }
        }
    }

    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        super.drawGuiContainerForegroundLayer(par1, par2);
        this.drawString(this.aircraft.getGuiInventory().getInventoryName(), 10, 10, 16777215);
        if (this.aircraft.getNumEjectionSeat() > 0) {
            this.drawString("Parachute", 9, 95, 16777215);
        }

        if (this.aircraft.getWeaponNum() > 0) {
            MCH_WeaponSet ws = this.aircraft.getWeapon(this.currentWeaponId);
            if (ws != null && !(ws.getFirstWeapon() instanceof MCH_WeaponDummy)) {
                this.drawString(ws.getName(), 79, 30, 16777215);
                int rest = ws.getRestAllAmmoNum() + ws.getAmmoNum();
                int color = rest == ws.getAllAmmoNum() ? 2675784 : (rest == 0 ? 16711680 : 16777215);
                String s = String.format("%4d/%4d", rest, ws.getAllAmmoNum());
                this.drawString(s, 145, 70, color);
                int itemPosX = 90;

                for (MCH_WeaponInfo.RoundItem r : ws.getInfo().roundItems) {
                    this.drawString("" + r.num, itemPosX, 80, 16777215);
                    itemPosX += 20;
                }

                int var11 = 85;

                for (MCH_WeaponInfo.RoundItem r : ws.getInfo().roundItems) {
                    this.drawItemStack(r.itemStack, var11, 62);
                    var11 += 20;
                }
            }
        } else {
            this.drawString("None", 79, 45, 16777215);
        }
    }

    protected void keyTyped(char c, int code) {
        if (code == 1) {
            this.closeScreen();
        } else if (code == 28) {
            String s = this.editCommand.getText().trim();
            if (s.startsWith("/")) {
                s = s.substring(1);
            }

            if (!s.isEmpty()) {
                new PacketHandleCommand(PacketHandleCommand.CommandAction.RAW_COMMAND, s);
            }
        } else {
            this.editCommand.textboxKeyTyped(c, code);
        }
    }

    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        W_McClient.MOD_bindTexture("textures/gui/gui.png");
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

        for (int i = 0; i < this.aircraft.getNumEjectionSeat(); i++) {
            this.drawTexturedModalRect(x + 10 + 18 * i - 1, y + 105 - 1, 215, 55, 18, 18);
        }

        int ff = (int) (this.aircraft.getFuelP() * 50.0F);
        if (ff >= 99) {
            ff = 100;
        }

        this.drawTexturedModalRect(x + 57, y + 30 + 50 - ff, 215, 0, 12, ff);
        ff = (int) (this.aircraft.getFuelP() * 100.0F + 0.5);
        int color = ff > 20 ? -14101432 : 16711680;
        this.drawString(String.format("%3d", ff) + "%", x + 30, y + 65, color);
        this.editCommand.drawTextBox();
    }
}
