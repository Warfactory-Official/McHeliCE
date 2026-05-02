package com.norwood.mcheli.multiplay;

import com.norwood.mcheli.networking.packet.PacketHandleCommand;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.norwood.mcheli.networking.packet.PacketHandleCommand.CommandAction;

public class MCH_GuiScoreboard_CreateTeam extends MCH_GuiScoreboard_Base {

    private static final String[] colorNames = new String[] {
            "RESET",
            "BLACK",
            "DARK_BLUE",
            "DARK_GREEN",
            "DARK_AQUA",
            "DARK_RED",
            "DARK_PURPLE",
            "GOLD",
            "GRAY",
            "DARK_GRAY",
            "BLUE",
            "GREEN",
            "AQUA",
            "RED",
            "LIGHT_PURPLE",
            "YELLOW"
    };
    private static boolean friendlyFire = true;
    private GuiButton buttonCreateTeamOK;
    private GuiButton buttonCreateTeamFF;
    private GuiTextField editCreateTeamName;
    private int lastTeamColor = 0;

    public MCH_GuiScoreboard_CreateTeam(MCH_IGuiScoreboard switcher, EntityPlayer player) {
        super(switcher, player);
    }

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new W_ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        int factor = sr.getScaleFactor() > 0 ? sr.getScaleFactor() : 1;
        this.guiLeft = 0;
        this.guiTop = 0;
        int x = this.mc.displayWidth / 2 / factor;
        int y = this.mc.displayHeight / 2 / factor;
        GuiButton buttonCTNextC = new GuiButton(576, x + 40, y - 20, 40, 20, ">");
        GuiButton buttonCTPrevC = new GuiButton(577, x - 80, y - 20, 40, 20, "<");
        this.buttonCreateTeamFF = new GuiButton(560, x - 80, y + 20, 160, 20, "");
        this.buttonCreateTeamOK = new GuiButton(528, x - 80, y + 60, 80, 20, "OK");
        GuiButton buttonCTCancel = new GuiButton(544, x, y + 60, 80, 20, "Cancel");
        this.editCreateTeamName = new GuiTextField(599, this.fontRenderer, x - 80, y - 55, 160, 20);
        this.editCreateTeamName.setText("");
        this.editCreateTeamName.setTextColor(-1);
        this.editCreateTeamName.setMaxStringLength(16);
        this.editCreateTeamName.setFocused(true);
        this.listGui.add(buttonCTNextC);
        this.listGui.add(buttonCTPrevC);
        this.listGui.add(this.buttonCreateTeamFF);
        this.listGui.add(this.buttonCreateTeamOK);
        this.listGui.add(buttonCTCancel);
        this.listGui.add(this.editCreateTeamName);
    }

    public void updateScreen() {
        String teamName = this.editCreateTeamName.getText();
        this.buttonCreateTeamOK.enabled = !teamName.isEmpty() && teamName.length() <= 16;
        this.editCreateTeamName.updateCursorCounter();
        this.buttonCreateTeamFF.displayString = "Friendly Fire : " + (friendlyFire ? "ON" : "OFF");
    }

    @Override
    public void acviveScreen() {
        this.editCreateTeamName.setText("");
        this.editCreateTeamName.setFocused(true);
    }

    protected void keyTyped(char c, int code) {
        if (code == 1) {
            this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
        } else {
            this.editCreateTeamName.textboxKeyTyped(c, code);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.editCreateTeamName.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void actionPerformed(@NotNull GuiButton btn) {
        if (btn.enabled) {
            switch (btn.id) {
                case 528:
                    String teamName = this.editCreateTeamName.getText();
                    if (!teamName.isEmpty() && teamName.length() <= 16) {
                        new PacketHandleCommand(CommandAction.RAW_COMMAND, "scoreboard teams add " + teamName)
                                .sendToServer();
                        new PacketHandleCommand(CommandAction.RAW_COMMAND,
                                "scoreboard teams option " + teamName + " color " + colorNames[this.lastTeamColor])
                                        .sendToServer();
                        new PacketHandleCommand(CommandAction.RAW_COMMAND,
                                "scoreboard teams option " + teamName + " friendlyfire " + friendlyFire).sendToServer();
                    }

                    this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
                    break;
                case 544:
                    this.switchScreen(MCH_GuiScoreboard_Base.SCREEN_ID.MAIN);
                    break;
                case 560:
                    friendlyFire = !friendlyFire;
                    break;
                case 576:
                    this.lastTeamColor++;
                    if (this.lastTeamColor >= colorNames.length) {
                        this.lastTeamColor = 0;
                    }
                    break;
                case 577:
                    this.lastTeamColor--;
                    if (this.lastTeamColor < 0) {
                        this.lastTeamColor = colorNames.length - 1;
                    }
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        drawList(this.mc, this.fontRenderer, true);
        ScaledResolution sr = new W_ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        int factor = sr.getScaleFactor() > 0 ? sr.getScaleFactor() : 1;
        W_McClient.MOD_bindTexture("textures/gui/mp_new_team.png");
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.mc.displayWidth / factor - 222) / 2;
        int y = (this.mc.displayHeight / factor - 200) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, 222, 200);
        x = this.mc.displayWidth / 2 / factor;
        y = this.mc.displayHeight / 2 / factor;
        this.drawCenteredString("Create team", x, y - 85, -1);
        this.drawCenteredString("Team name", x, y - 70, -1);
        TextFormatting ecf = TextFormatting.getValueByName(colorNames[this.lastTeamColor]);
        this.drawCenteredString(ecf + "Team Color" + ecf, x, y - 13, -1);
        this.editCreateTeamName.drawTextBox();
    }
}
