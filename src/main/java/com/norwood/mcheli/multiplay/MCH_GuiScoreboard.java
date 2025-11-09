package com.norwood.mcheli.multiplay;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.wrapper.W_GuiButton;
import com.norwood.mcheli.wrapper.W_GuiContainer;

public class MCH_GuiScoreboard extends W_GuiContainer implements MCH_IGuiScoreboard {

    public final EntityPlayer thePlayer;
    private MCH_GuiScoreboard_Base.SCREEN_ID screenID;
    private Map<MCH_GuiScoreboard_Base.SCREEN_ID, MCH_GuiScoreboard_Base> listScreen;
    private int lastTeamNum = 0;

    public MCH_GuiScoreboard(EntityPlayer player) {
        super(new MCH_ContainerScoreboard(player));
        this.thePlayer = player;
    }

    public static void setVisible(Object g, boolean v) {
        if (g instanceof GuiButton) {
            ((GuiButton) g).visible = v;
        }

        if (g instanceof GuiTextField) {
            ((GuiTextField) g).setVisible(v);
        }
    }

    public static void drawList(Minecraft mc, FontRenderer fontRendererObj, boolean mng) {
        MCH_GuiScoreboard_Base.drawList(mc, fontRendererObj, mng);
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        super.initGui();
        this.buttonList.clear();
        this.labelList.clear();
        this.guiLeft = 0;
        this.guiTop = 0;
        this.listScreen = new HashMap<>();
        this.listScreen.put(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN, new MCH_GuiScoreboard_Main(this, this.thePlayer));
        this.listScreen.put(MCH_GuiScoreboard_Base.SCREEN_ID.CREATE_TEAM,
                new MCH_GuiScoreboard_CreateTeam(this, this.thePlayer));

        for (MCH_GuiScoreboard_Base s : this.listScreen.values()) {
            s.initGui(this.buttonList, this);
        }

        this.lastTeamNum = this.mc.world.getScoreboard().getTeams().size();
        this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
    }

    public void updateScreen() {
        super.updateScreen();
        int nowTeamNum = this.mc.world.getScoreboard().getTeams().size();
        if (this.lastTeamNum != nowTeamNum) {
            this.lastTeamNum = nowTeamNum;
            this.initGui();
        }

        for (MCH_GuiScoreboard_Base s : this.listScreen.values()) {
            try {
                s.updateScreenButtons(this.buttonList);
                s.updateScreen();
            } catch (Exception var5) {}
        }
    }

    @Override
    public void switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID id) {
        for (MCH_GuiScoreboard_Base b : this.listScreen.values()) {
            b.leaveScreen();
        }

        this.screenID = id;
        this.getCurrentScreen().onSwitchScreen();
    }

    private MCH_GuiScoreboard_Base getCurrentScreen() {
        return this.listScreen.get(this.screenID);
    }

    protected void keyTyped(char c, int code) throws IOException {
        this.getCurrentScreen().keyTypedScreen(c, code);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            for (MCH_GuiScoreboard_Base s : this.listScreen.values()) {
                s.mouseClickedScreen(mouseX, mouseY, mouseButton);
            }

            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (Exception var6) {}
    }

    protected void actionPerformed(@NotNull GuiButton btn) throws IOException {
        if (btn.enabled) {
            this.getCurrentScreen().actionPerformedScreen(btn);
        }
    }

    public void func_146276_q_() {}

    public void func_146278_c(int tint) {
        GlStateManager.disableLighting();
        GL11.glDisable(2912);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void drawGuiContainerForegroundLayer(int x, int y) {
        this.getCurrentScreen().drawGuiContainerForegroundLayerScreen(x, y);

        for (Object o : this.buttonList) {
            if (o instanceof W_GuiButton btn) {
                if (btn.isOnMouseOver() && btn.hoverStringList != null) {
                    this.drawHoveringText(btn.hoverStringList, x, y, this.fontRenderer);
                    break;
                }
            }
        }
    }

    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        this.getCurrentScreen().drawGuiContainerBackgroundLayer(par1, par2, par3);
    }

    public void setWorldAndResolution(@NotNull Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);

        for (MCH_GuiScoreboard_Base s : this.listScreen.values()) {
            s.setWorldAndResolution(mc, width, height);
        }
    }
}
