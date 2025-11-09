package com.norwood.mcheli.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Sets;
import com.norwood.mcheli.MCH_ClientCommonTickHandler;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.networking.packet.PacketContentReload;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.wrapper.W_GuiButton;
import com.norwood.mcheli.wrapper.W_GuiContainer;
import com.norwood.mcheli.wrapper.W_McClient;

public class MCH_ConfigGui extends W_GuiContainer {

    public static final int BUTTON_RENDER = 50;
    public static final int BUTTON_KEY_BINDING = 51;
    public static final int BUTTON_PREV_CONTROL = 52;
    public static final int BUTTON_DEVELOP = 55;
    public static final int BUTTON_KEY_LIST = 53;
    public static final int BUTTON_KEY_RESET_ALL = 54;
    public static final int BUTTON_KEY_LIST_BASE = 200;
    public static final int BUTTON_KEY_RESET_BASE = 300;
    public static final int BUTTON_DEV_RELOAD_AC = 400;
    public static final int BUTTON_DEV_RELOAD_WEAPON = 401;
    public static final int BUTTON_DEV_RELOAD_HUD = 402;
    public static final int BUTTON_SAVE_CLOSE = 100;
    public static final int BUTTON_CANCEL = 101;
    public static final int SCREEN_CONTROLS = 0;
    public static final int SCREEN_RENDER = 1;
    public static final int SCREEN_KEY_BIND = 2;
    public static final int SCREEN_DEVELOP = 3;
    private final EntityPlayer thePlayer;
    public List<W_GuiButton> listControlButtons;
    public List<W_GuiButton> listRenderButtons;
    public List<W_GuiButton> listKeyBindingButtons;
    public List<W_GuiButton> listDevelopButtons;
    public MCH_GuiList keyBindingList;
    public int waitKeyButtonId;
    public int waitKeyAcceptCount;
    public int currentScreenId = 0;
    private MCH_GuiOnOffButton buttonMouseInv;
    private MCH_GuiOnOffButton buttonStickModeHeli;
    private MCH_GuiOnOffButton buttonStickModePlane;
    private MCH_GuiOnOffButton buttonHideKeyBind;
    private MCH_GuiOnOffButton buttonShowHUDTP;
    private MCH_GuiOnOffButton buttonSmoothShading;
    private MCH_GuiOnOffButton buttonShowEntityMarker;
    private MCH_GuiOnOffButton buttonMarkThroughWall;
    private MCH_GuiOnOffButton buttonReplaceCamera;
    private MCH_GuiOnOffButton buttonNewExplosion;
    private MCH_GuiSlider sliderEntityMarkerSize;
    private MCH_GuiSlider sliderBlockMarkerSize;
    private MCH_GuiSlider sliderSensitivity;
    private MCH_GuiSlider[] sliderHitMark;
    private MCH_GuiOnOffButton buttonTestMode;
    private MCH_GuiOnOffButton buttonThrottleHeli;
    private MCH_GuiOnOffButton buttonThrottlePlane;
    private MCH_GuiOnOffButton buttonThrottleTank;
    private MCH_GuiOnOffButton buttonFlightSimMode;
    private MCH_GuiOnOffButton buttonSwitchWeaponWheel;
    private W_GuiButton buttonReloadAircraftInfo;
    private W_GuiButton buttonReloadWeaponInfo;
    private W_GuiButton buttonReloadAllHUD;
    private MCH_GuiSlider __sliderTextureAlpha;
    private int ignoreButtonCounter = 0;

    public MCH_ConfigGui(EntityPlayer player) {
        super(new MCH_ConfigGuiContainer(player));
        this.thePlayer = player;
        this.xSize = 330;
        this.ySize = 200;
    }

    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int x1 = this.guiLeft + 10;
        int x2 = this.guiLeft + 10 + 150 + 10;
        int y = this.guiTop;
        this.listControlButtons = new ArrayList<>();
        this.buttonMouseInv = new MCH_GuiOnOffButton(0, x1, y + 25, 150, 20, "Invert Mouse : ");
        this.sliderSensitivity = new MCH_GuiSlider(0, x1, y + 50, 150, 20, "Sensitivity : %.1f", 0.0F, 0.0F, 30.0F,
                0.1F);
        this.buttonFlightSimMode = new MCH_GuiOnOffButton(0, x1, y + 75, 150, 20, "Mouse Flight Sim Mode : ");
        this.buttonSwitchWeaponWheel = new MCH_GuiOnOffButton(0, x1, y + 100, 150, 20, "Switch Weapon Wheel : ");
        this.listControlButtons.add(new W_GuiButton(50, x1, y + 125, 150, 20, "Render Settings >>"));
        this.listControlButtons.add(new W_GuiButton(51, x1, y + 150, 150, 20, "Key Binding >>"));
        this.listControlButtons.add(new W_GuiButton(55, x2, y + 150, 150, 20, "Development >>"));
        this.buttonTestMode = new MCH_GuiOnOffButton(0, x1, y + 175, 150, 20, "Test Mode : ");
        this.buttonStickModeHeli = new MCH_GuiOnOffButton(0, x2, y + 25, 150, 20, "Stick Mode Heli : ");
        this.buttonStickModePlane = new MCH_GuiOnOffButton(0, x2, y + 50, 150, 20, "Stick Mode Plane : ");
        this.buttonThrottleHeli = new MCH_GuiOnOffButton(0, x2, y + 75, 150, 20, "Throttle Down Heli : ");
        this.buttonThrottlePlane = new MCH_GuiOnOffButton(0, x2, y + 100, 150, 20, "Throttle Down Plane : ");
        this.buttonThrottleTank = new MCH_GuiOnOffButton(0, x2, y + 125, 150, 20, "Throttle Down Tank : ");
        this.listControlButtons.add(this.buttonMouseInv);
        this.listControlButtons.add(this.buttonStickModeHeli);
        this.listControlButtons.add(this.buttonStickModePlane);
        this.listControlButtons.add(this.sliderSensitivity);
        this.listControlButtons.add(this.buttonThrottleHeli);
        this.listControlButtons.add(this.buttonThrottlePlane);
        this.listControlButtons.add(this.buttonThrottleTank);
        this.listControlButtons.add(this.buttonTestMode);
        this.listControlButtons.add(this.buttonFlightSimMode);
        this.listControlButtons.add(this.buttonSwitchWeaponWheel);

        this.buttonList.addAll(this.listControlButtons);

        this.listRenderButtons = new ArrayList<>();
        this.buttonShowHUDTP = new MCH_GuiOnOffButton(0, x1, y + 25, 150, 20, "Show HUD Third Person : ");
        this.buttonHideKeyBind = new MCH_GuiOnOffButton(0, x1, y + 50, 150, 20, "Hide Key Binding : ");
        this.sliderHitMark = new MCH_GuiSlider[] {
                new MCH_GuiSlider(0, x1, y + 125, 75, 20, "Alpha:%.0f", 0.0F, 0.0F, 255.0F, 16.0F),
                new MCH_GuiSlider(0, x1 + 75, y + 75, 75, 20, "Red:%.0f", 0.0F, 0.0F, 255.0F, 16.0F),
                new MCH_GuiSlider(0, x1 + 75, y + 100, 75, 20, "Green:%.0f", 0.0F, 0.0F, 255.0F, 16.0F),
                new MCH_GuiSlider(0, x1 + 75, y + 125, 75, 20, "Blue:%.0f", 0.0F, 0.0F, 255.0F, 16.0F)
        };
        this.buttonReplaceCamera = new MCH_GuiOnOffButton(0, x1, y + 150, 150, 20, "Change Camera Pos : ");
        this.listRenderButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, "Controls <<"));
        this.buttonSmoothShading = new MCH_GuiOnOffButton(0, x2, y + 25, 150, 20, "Smooth Shading : ");
        this.buttonShowEntityMarker = new MCH_GuiOnOffButton(0, x2, y + 50, 150, 20, "Show Entity Maker : ");
        this.sliderEntityMarkerSize = new MCH_GuiSlider(0, x2 + 30, y + 75, 120, 20, "Entity Marker Size:%.0f", 10.0F,
                0.0F, 30.0F, 1.0F);
        this.sliderBlockMarkerSize = new MCH_GuiSlider(0, x2 + 60, y + 100, 90, 20, "Block Marker Size:%.0f", 10.0F,
                0.0F, 20.0F, 1.0F);
        this.buttonMarkThroughWall = new MCH_GuiOnOffButton(0, x2 + 30, y + 100, 120, 20, "Mark Through Wall : ");
        this.buttonNewExplosion = new MCH_GuiOnOffButton(0, x2, y + 150, 150, 20, "Default Explosion : ");
        this.listRenderButtons.add(this.buttonShowHUDTP);

        Collections.addAll(this.listRenderButtons, this.sliderHitMark);

        this.listRenderButtons.add(this.buttonSmoothShading);
        this.listRenderButtons.add(this.buttonHideKeyBind);
        this.listRenderButtons.add(this.buttonShowEntityMarker);
        this.listRenderButtons.add(this.buttonReplaceCamera);
        this.listRenderButtons.add(this.buttonNewExplosion);
        this.listRenderButtons.add(this.sliderEntityMarkerSize);
        this.listRenderButtons.add(this.sliderBlockMarkerSize);

        this.buttonList.addAll(this.listRenderButtons);

        this.listKeyBindingButtons = new ArrayList<>();
        this.waitKeyButtonId = 0;
        this.waitKeyAcceptCount = 0;
        this.keyBindingList = new MCH_GuiList(53, 7, x1, y + 25 - 2, 310, 150, "");
        this.listKeyBindingButtons.add(this.keyBindingList);
        this.listKeyBindingButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, "Controls <<"));
        this.listKeyBindingButtons.add(new W_GuiButton(54, x1 + 90, y + 175, 60, 20, "Reset All"));
        MCH_GuiListItemKeyBind[] listKeyBindItems = new MCH_GuiListItemKeyBind[] {
                new MCH_GuiListItemKeyBind(200, 300, x1, "Up", MCH_Config.KeyUp),
                new MCH_GuiListItemKeyBind(201, 301, x1, "Down", MCH_Config.KeyDown),
                new MCH_GuiListItemKeyBind(202, 302, x1, "Right", MCH_Config.KeyRight),
                new MCH_GuiListItemKeyBind(203, 303, x1, "Left", MCH_Config.KeyLeft),
                new MCH_GuiListItemKeyBind(204, 304, x1, "Switch Gunner", MCH_Config.KeySwitchMode),
                new MCH_GuiListItemKeyBind(205, 305, x1, "Switch Hovering", MCH_Config.KeySwitchHovering),
                new MCH_GuiListItemKeyBind(206, 306, x1, "Switch Weapon1", MCH_Config.KeySwitchWeapon1),
                new MCH_GuiListItemKeyBind(207, 307, x1, "Switch Weapon2", MCH_Config.KeySwitchWeapon2),
                new MCH_GuiListItemKeyBind(208, 308, x1, "Switch Weapon Mode", MCH_Config.KeySwWeaponMode),
                new MCH_GuiListItemKeyBind(209, 309, x1, "Zoom / Fold Wing", MCH_Config.KeyZoom),
                new MCH_GuiListItemKeyBind(210, 310, x1, "Camera Mode", MCH_Config.KeyCameraMode),
                new MCH_GuiListItemKeyBind(211, 311, x1, "Unmount Mobs", MCH_Config.KeyUnmount),
                new MCH_GuiListItemKeyBind(212, 312, x1, "Flare", MCH_Config.KeyFlare),
                new MCH_GuiListItemKeyBind(213, 313, x1, "Vtol / Drop / Fold Blade", MCH_Config.KeyExtra),
                new MCH_GuiListItemKeyBind(214, 314, x1, "Third Person Distance Up", MCH_Config.KeyCameraDistUp),
                new MCH_GuiListItemKeyBind(215, 315, x1, "Third Person Distance Down", MCH_Config.KeyCameraDistDown),
                new MCH_GuiListItemKeyBind(216, 316, x1, "Switch Free Look", MCH_Config.KeyFreeLook),
                new MCH_GuiListItemKeyBind(217, 317, x1, "Open GUI", MCH_Config.KeyGUI),
                new MCH_GuiListItemKeyBind(218, 318, x1, "Gear Up Down", MCH_Config.KeyGearUpDown),
                new MCH_GuiListItemKeyBind(219, 319, x1, "Put entity in the rack", MCH_Config.KeyPutToRack),
                new MCH_GuiListItemKeyBind(220, 320, x1, "Drop entity from the rack", MCH_Config.KeyDownFromRack),
                new MCH_GuiListItemKeyBind(221, 321, x1, "[MP]Score board", MCH_Config.KeyScoreboard),
                new MCH_GuiListItemKeyBind(222, 322, x1, "[MP][OP]Multiplay manager", MCH_Config.KeyMultiplayManager)
        };

        for (MCH_GuiListItemKeyBind item : listKeyBindItems) {
            this.keyBindingList.addItem(item);
        }

        this.buttonList.addAll(this.listKeyBindingButtons);

        this.listDevelopButtons = new ArrayList<>();
        if (Minecraft.getMinecraft().isSingleplayer()) {
            this.buttonReloadAircraftInfo = new W_GuiButton(400, x1, y + 50, 150, 20, "Reload aircraft setting");
            this.buttonReloadWeaponInfo = new W_GuiButton(401, x1, y + 75, 150, 20, "Reload All Weapons");
            this.buttonReloadAllHUD = new W_GuiButton(402, x1, y + 100, 150, 20, "Reload All HUD");
            this.__sliderTextureAlpha = new MCH_GuiSlider(432, x1, y + 125, 150, 20, "Texture Alpha:%.0f", 1.0F, 0.0F,
                    255.0F, 1.0F);
            this.listDevelopButtons.add(this.buttonReloadAircraftInfo);
            this.listDevelopButtons.add(this.buttonReloadWeaponInfo);
            this.listDevelopButtons.add(this.buttonReloadAllHUD);
            this.listDevelopButtons.add(this.__sliderTextureAlpha);
        }

        this.listDevelopButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, "Controls <<"));

        this.buttonList.addAll(this.listDevelopButtons);

        this.buttonList.add(new GuiButton(100, x2, y + 175, 80, 20, "Save & Close"));
        this.buttonList.add(new GuiButton(101, x2 + 90, y + 175, 60, 20, "Cancel"));
        this.switchScreen(0);
        this.applySwitchScreen();
        this.getAllStatusFromConfig();
    }

    public boolean canButtonClick() {
        return this.ignoreButtonCounter <= 0;
    }

    public void getAllStatusFromConfig() {
        this.buttonMouseInv.setOnOff(MCH_Config.InvertMouse.prmBool);
        this.buttonStickModeHeli.setOnOff(MCH_Config.MouseControlStickModeHeli.prmBool);
        this.buttonStickModePlane.setOnOff(MCH_Config.MouseControlStickModePlane.prmBool);
        this.sliderSensitivity.setSliderValue((float) MCH_Config.MouseSensitivity.prmDouble);
        this.buttonShowHUDTP.setOnOff(MCH_Config.DisplayHUDThirdPerson.prmBool);
        this.buttonSmoothShading.setOnOff(MCH_Config.SmoothShading.prmBool);
        this.buttonHideKeyBind.setOnOff(MCH_Config.HideKeybind.prmBool);
        this.buttonShowEntityMarker.setOnOff(MCH_Config.DisplayEntityMarker.prmBool);
        this.buttonMarkThroughWall.setOnOff(MCH_Config.DisplayMarkThroughWall.prmBool);
        this.sliderEntityMarkerSize.setSliderValue((float) MCH_Config.EntityMarkerSize.prmDouble);
        this.sliderBlockMarkerSize.setSliderValue((float) MCH_Config.BlockMarkerSize.prmDouble);
        this.buttonReplaceCamera.setOnOff(MCH_Config.ReplaceRenderViewEntity.prmBool);
        this.buttonNewExplosion.setOnOff(MCH_Config.DefaultExplosionParticle.prmBool);
        this.sliderHitMark[0].setSliderValue(MCH_Config.hitMarkColorAlpha * 255.0F);
        this.sliderHitMark[1].setSliderValue(MCH_Config.hitMarkColorRGB >> 16 & 0xFF);
        this.sliderHitMark[2].setSliderValue(MCH_Config.hitMarkColorRGB >> 8 & 0xFF);
        this.sliderHitMark[3].setSliderValue(MCH_Config.hitMarkColorRGB >> 0 & 0xFF);
        this.buttonThrottleHeli.setOnOff(MCH_Config.AutoThrottleDownHeli.prmBool);
        this.buttonThrottlePlane.setOnOff(MCH_Config.AutoThrottleDownPlane.prmBool);
        this.buttonThrottleTank.setOnOff(MCH_Config.AutoThrottleDownTank.prmBool);
        this.buttonTestMode.setOnOff(MCH_Config.TestMode.prmBool);
        this.buttonFlightSimMode.setOnOff(MCH_Config.MouseControlFlightSimMode.prmBool);
        this.buttonSwitchWeaponWheel.setOnOff(MCH_Config.SwitchWeaponWithMouseWheel.prmBool);
        if (this.__sliderTextureAlpha != null) {
            this.__sliderTextureAlpha.setSliderValue((float) MCH_Config.__TextureAlpha.prmDouble * 255.0F);
        }
    }

    public void saveAndApplyConfig() {
        MCH_Config.InvertMouse.setPrm(this.buttonMouseInv.getOnOff());
        MCH_Config.MouseControlStickModeHeli.setPrm(this.buttonStickModeHeli.getOnOff());
        MCH_Config.MouseControlStickModePlane.setPrm(this.buttonStickModePlane.getOnOff());
        MCH_Config.MouseControlFlightSimMode.setPrm(this.buttonFlightSimMode.getOnOff());
        MCH_Config.SwitchWeaponWithMouseWheel.setPrm(this.buttonSwitchWeaponWheel.getOnOff());
        MCH_Config.MouseSensitivity.setPrm(this.sliderSensitivity.getSliderValueInt(1));
        MCH_Config.DisplayHUDThirdPerson.setPrm(this.buttonShowHUDTP.getOnOff());
        MCH_Config.SmoothShading.setPrm(this.buttonSmoothShading.getOnOff());
        MCH_Config.HideKeybind.setPrm(this.buttonHideKeyBind.getOnOff());
        MCH_Config.DisplayEntityMarker.setPrm(this.buttonShowEntityMarker.getOnOff());
        MCH_Config.DisplayMarkThroughWall.setPrm(this.buttonMarkThroughWall.getOnOff());
        MCH_Config.EntityMarkerSize.setPrm(this.sliderEntityMarkerSize.getSliderValueInt(1));
        MCH_Config.BlockMarkerSize.setPrm(this.sliderBlockMarkerSize.getSliderValueInt(1));
        MCH_Config.ReplaceRenderViewEntity.setPrm(this.buttonReplaceCamera.getOnOff());
        MCH_Config.DefaultExplosionParticle.setPrm(this.buttonNewExplosion.getOnOff());
        float a = this.sliderHitMark[0].getSliderValue();
        int r = (int) this.sliderHitMark[1].getSliderValue();
        int g = (int) this.sliderHitMark[2].getSliderValue();
        int b = (int) this.sliderHitMark[3].getSliderValue();
        MCH_Config.hitMarkColorAlpha = a / 255.0F;
        MCH_Config.hitMarkColorRGB = r << 16 | g << 8 | b;
        MCH_Config.HitMarkColor.setPrm(String.format("%d, %d, %d, %d", (int) a, r, g, b));
        boolean b1 = MCH_Config.AutoThrottleDownHeli.prmBool;
        boolean b2 = MCH_Config.AutoThrottleDownPlane.prmBool;
        MCH_Config.AutoThrottleDownHeli.setPrm(this.buttonThrottleHeli.getOnOff());
        MCH_Config.AutoThrottleDownPlane.setPrm(this.buttonThrottlePlane.getOnOff());
        MCH_Config.AutoThrottleDownTank.setPrm(this.buttonThrottleTank.getOnOff());
        if (b1 != MCH_Config.AutoThrottleDownHeli.prmBool || b2 != MCH_Config.AutoThrottleDownPlane.prmBool) {
            this.sendClientSettings();
        }

        for (int i = 0; i < this.keyBindingList.getItemNum(); i++) {
            ((MCH_GuiListItemKeyBind) this.keyBindingList.getItem(i)).applyKeycode();
        }

        MCH_ClientCommonTickHandler.instance.updatekeybind(MCH_MOD.config);
        MCH_Config.TestMode.setPrm(this.buttonTestMode.getOnOff());
        if (this.__sliderTextureAlpha != null) {
            MCH_Config.__TextureAlpha.setPrm(this.__sliderTextureAlpha.getSliderValue() / 255.0);
        }

        MCH_MOD.config.write();
    }

    public void switchScreen(int screenID) {
        this.waitKeyButtonId = 0;
        this.currentScreenId = screenID;

        for (W_GuiButton b : this.listControlButtons) {
            b.setVisible(false);
        }

        for (W_GuiButton b : this.listRenderButtons) {
            b.setVisible(false);
        }

        for (W_GuiButton b : this.listKeyBindingButtons) {
            b.setVisible(false);
        }

        for (W_GuiButton b : this.listDevelopButtons) {
            b.setVisible(false);
        }

        this.ignoreButtonCounter = 3;
    }

    public void applySwitchScreen() {
        switch (this.currentScreenId) {
            case 1 -> {
                for (W_GuiButton b : this.listRenderButtons) {
                    b.setVisible(true);
                }
            }
            case 2 -> {
                for (W_GuiButton b : this.listKeyBindingButtons) {
                    b.setVisible(true);
                }
            }
            case 3 -> {
                for (W_GuiButton b : this.listDevelopButtons) {
                    b.setVisible(true);
                }
            }
            default -> {
                for (W_GuiButton b : this.listControlButtons) {
                    b.setVisible(true);
                }
            }
        }
    }

    public void sendClientSettings() {
        if (this.mc.player != null) {
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(this.mc.player);
            if (ac != null) {
                int seatId = ac.getSeatIdByEntity(this.mc.player);
                if (seatId == 0) {
                    ac.updateClientSettings(seatId);
                }
            }
        }
    }

    public void keyTyped(char a, int code) throws IOException {
        if (this.waitKeyButtonId != 0) {
            if (code != 1) {
                super.keyTyped(a, code);
            }

            this.acceptKeycode(code);
            this.waitKeyButtonId = 0;
        } else {
            super.keyTyped(a, code);
        }
    }

    protected void mouseClicked(int par1, int par2, int par3) throws IOException {
        super.mouseClicked(par1, par2, par3);
        if (this.waitKeyButtonId != 0 && this.waitKeyAcceptCount == 0) {
            this.acceptKeycode(par3 - 100);
            this.waitKeyButtonId = 0;
        }
    }

    public void acceptKeycode(int code) {
        if (code != 1 && this.mc.currentScreen instanceof MCH_ConfigGui) {
            MCH_GuiListItemKeyBind kb = (MCH_GuiListItemKeyBind) this.keyBindingList
                    .getItem(this.waitKeyButtonId - 200);
            if (kb != null) {
                kb.setKeycode(code);
            }
        }
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (this.waitKeyButtonId == 0) {
            int var16 = Mouse.getEventDWheel();
            if (var16 != 0) {
                if (var16 > 0) {
                    this.keyBindingList.scrollDown(2.0F);
                } else {
                    this.keyBindingList.scrollUp(2.0F);
                }
            }
        }
    }

    public void updateScreen() {
        super.updateScreen();
        if (this.waitKeyAcceptCount > 0) {
            this.waitKeyAcceptCount--;
        }

        if (this.ignoreButtonCounter > 0) {
            this.ignoreButtonCounter--;
            if (this.ignoreButtonCounter == 0) {
                this.applySwitchScreen();
            }
        }
    }

    public void onGuiClosed() {
        super.onGuiClosed();
    }

    protected void actionPerformed(@NotNull GuiButton button) {
        try {
            super.actionPerformed(button);
            if (!button.enabled) {
                return;
            }

            if (this.waitKeyButtonId != 0) {
                return;
            }

            if (!this.canButtonClick()) {
                return;
            }

            switch (button.id) {
                case BUTTON_RENDER -> this.switchScreen(SCREEN_RENDER);
                case BUTTON_KEY_BINDING -> this.switchScreen(SCREEN_KEY_BIND);
                case BUTTON_PREV_CONTROL -> this.switchScreen(SCREEN_CONTROLS);
                case BUTTON_KEY_LIST -> {
                    MCH_GuiListItem item = this.keyBindingList.lastPushItem;
                    if (item instanceof MCH_GuiListItemKeyBind kb && kb.lastPushButton != null) {
                        int kbNum = this.keyBindingList.getItemNum();
                        if (kb.lastPushButton.id >= BUTTON_KEY_LIST_BASE &&
                                kb.lastPushButton.id < BUTTON_KEY_LIST_BASE + kbNum) {
                            this.waitKeyButtonId = kb.lastPushButton.id;
                            this.waitKeyAcceptCount = 5;
                        } else if (kb.lastPushButton.id >= BUTTON_KEY_RESET_BASE &&
                                kb.lastPushButton.id < BUTTON_KEY_RESET_BASE + kbNum) {
                                    kb.resetKeycode();
                                }
                        kb.lastPushButton = null;
                    }
                }
                case BUTTON_KEY_RESET_ALL -> {
                    for (int i = 0; i < this.keyBindingList.getItemNum(); i++) {
                        ((MCH_GuiListItemKeyBind) this.keyBindingList.getItem(i)).resetKeycode();
                    }
                }
                case BUTTON_DEVELOP -> this.switchScreen(SCREEN_DEVELOP);
                case BUTTON_SAVE_CLOSE -> {
                    this.saveAndApplyConfig();
                    this.mc.player.closeScreen();
                }
                case BUTTON_CANCEL -> this.mc.player.closeScreen();
                case BUTTON_DEV_RELOAD_WEAPON -> {
                    MCH_Lib.DbgLog(true, "MCH_BaseInfo.reload all weapon info.");
                    ContentRegistries.get(MCH_WeaponInfo.class).reloadAll();
                    new PacketContentReload(PacketContentReload.ReloadType.WEAPON).sendToServer();
                    List<Entity> list = new ArrayList<>(this.mc.world.loadedEntityList);
                    Set<String> reloaded = Sets.newHashSet();

                    for (Entity value : list) {
                        if (value instanceof MCH_EntityAircraft ac) {
                            if (ac.getAcInfo() != null && reloaded.add(ac.getAcInfo().name)) {
                                ContentRegistries.get(ac.getAcInfo().getClass()).reload(ac.getAcInfo().name);
                                ac.changeType(ac.getAcInfo().name);
                                ac.onAcInfoReloaded();
                            }
                        }
                    }
                    this.mc.player.closeScreen();
                }
                case BUTTON_DEV_RELOAD_HUD, BUTTON_DEV_RELOAD_AC -> {
                    if (button.id == BUTTON_DEV_RELOAD_HUD)
                        MCH_MOD.proxy.reloadHUD();

                    MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(this.thePlayer);
                    if (ac != null && ac.getAcInfo() != null) {
                        String name = ac.getAcInfo().name;
                        MCH_Lib.DbgLog(true, "MCH_BaseInfo.reload : " + name);
                        ContentRegistries.get(ac.getAcInfo().getClass()).reload(name);
                        List<Entity> entityList = new ArrayList<>(this.mc.world.loadedEntityList);

                        for (Entity entity : entityList) {
                            if (entity instanceof MCH_EntityAircraft aircraft &&
                                    aircraft.getAcInfo() != null &&
                                    aircraft.getAcInfo().name.equals(name)) {
                                aircraft.changeType(name);
                                aircraft.onAcInfoReloaded();
                            }
                        }
                        new PacketContentReload(PacketContentReload.ReloadType.VEHICLE).sendToServer();
                    }
                    this.mc.player.closeScreen();
                }
                default -> throw new IllegalStateException("Unexpected value: " + button.id);
            }

        } catch (Exception var7) {
            var7.printStackTrace();
        }
    }

    public boolean doesGuiPauseGame() {
        return true;
    }

    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        super.drawGuiContainerForegroundLayer(par1, par2);
        this.drawString("MC Helicopter MOD Options", 10, 10, 16777215);
        if (this.currentScreenId == 0) {
            this.drawString("< Controls >", 170, 10, 16777215);
        } else if (this.currentScreenId == 1) {
            this.drawString("< Render >", 170, 10, 16777215);
            this.drawString("Hit Mark", 10, 75, 16777215);
            int color = 0;
            color |= (int) this.sliderHitMark[0].getSliderValue() << 24;
            color |= (int) this.sliderHitMark[1].getSliderValue() << 16;
            color |= (int) this.sliderHitMark[2].getSliderValue() << 8;
            color |= (int) this.sliderHitMark[3].getSliderValue() << 0;
            this.drawSampleHitMark(40, 105, color);
            double size = this.sliderEntityMarkerSize.getSliderValue();
            double x = 170.0 + (30.0 - size) / 2.0;
            double y = this.sliderEntityMarkerSize.y - this.sliderEntityMarkerSize.getHeight();
            double[] ls = new double[] { x + size, y, x, y, x + size / 2.0, y + size };
            this.drawLine(ls, -65536, 4);
            size = this.sliderBlockMarkerSize.getSliderValue();
            x = 185.0;
            y = this.sliderBlockMarkerSize.y;
            color = -65536;
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color >> 0 & 0xFF),
                    (byte) (color >> 24 & 0xFF));
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder builder = tessellator.getBuffer();
            builder.begin(1, DefaultVertexFormats.POSITION_COLOR);
            MCH_GuiTargetMarker.drawRhombus(builder, 15, x, y, this.zLevel, size, color);
            tessellator.draw();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color((byte) -1, (byte) -1, (byte) -1, (byte) -1);
            GlStateManager.popMatrix();
        } else if (this.currentScreenId == 2) {
            this.drawString("< Key Binding >", 170, 10, 16777215);
            if (this.waitKeyButtonId != 0) {
                drawRect(30, 30, this.xSize - 30, this.ySize - 30, -533712848);
                String msg = "Please ant key or mouse button.";
                int w = this.getStringWidth(msg);
                this.drawString(msg, (this.xSize - w) / 2, this.ySize / 2 - 4, 16777215);
            }
        } else if (this.currentScreenId == 3) {
            this.drawString("< Development >", 170, 10, 16777215);
            this.drawString("Single player only!", 10, 30, 16711680);
            if (this.buttonReloadAircraftInfo != null && this.buttonReloadAircraftInfo.isOnMouseOver()) {
                this.drawString("The following items are not reload.", 170, 30, 16777215);
                String[] ignoreItems = MCH_AircraftInfo.getCannotReloadItem();
                int y = 10;

                for (String s : ignoreItems) {
                    this.drawString("  " + s, 170, 30 + y, 16777215);
                    y += 10;
                }
            }
        }
    }

    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        W_McClient.MOD_bindTexture("textures/gui/config.png");
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRectRotate(x, y, this.xSize, this.ySize, 0.0, 0.0, this.xSize, this.ySize, 0.0F, 512.0,
                256.0);
    }

    public void drawSampleHitMark(int x, int y, int color) {
        int IVX = 10;
        int IVY = 10;
        int SZX = 5;
        int SZY = 5;
        double[] ls = new double[] {
                x - IVX, y - IVY, x - SZX, y - SZY, x - IVX, y + IVY, x - SZX, y + SZY, x + IVX, y - IVY, x + SZX,
                y - SZY, x + IVX, y + IVY, x + SZX, y + SZY
        };
        this.drawLine(ls, color, 1);
    }

    public void drawLine(double[] line, int color, int mode) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color >> 0 & 0xFF),
                (byte) (color >> 24 & 0xFF));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(mode, DefaultVertexFormats.POSITION);

        for (int i = 0; i < line.length; i += 2) {
            buffer.pos(line[i], line[i + 1], this.zLevel).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GlStateManager.popMatrix();
    }

    public void drawTexturedModalRectRotate(
                                            double left,
                                            double top,
                                            double width,
                                            double height,
                                            double uLeft,
                                            double vTop,
                                            double uWidth,
                                            double vHeight,
                                            float rot,
                                            double texWidth,
                                            double texHeight) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(left + width / 2.0, top + height / 2.0, 0.0);
        GlStateManager.rotate(rot, 0.0F, 0.0F, 1.0F);
        float fw = (float) (1.0 / texWidth);
        float fh = (float) (1.0 / texHeight);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-width / 2.0, height / 2.0, this.zLevel).tex(uLeft * fw, (vTop + vHeight) * fh).endVertex();
        buffer.pos(width / 2.0, height / 2.0, this.zLevel).tex((uLeft + uWidth) * fw, (vTop + vHeight) * fh)
                .endVertex();
        buffer.pos(width / 2.0, -height / 2.0, this.zLevel).tex((uLeft + uWidth) * fw, vTop * fh).endVertex();
        buffer.pos(-width / 2.0, -height / 2.0, this.zLevel).tex(uLeft * fw, vTop * fh).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
    }
}
