package com.norwood.mcheli.multiplay;

import com.norwood.mcheli.MCH_ServerSettings;
import com.norwood.mcheli.networking.packet.PacketHandleCommand;
import com.norwood.mcheli.wrapper.W_GuiButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.norwood.mcheli.networking.packet.PacketHandleCommand.CommandAction;

public class MCH_GuiScoreboard_Main extends MCH_GuiScoreboard_Base {

    private W_GuiButton buttonSwitchPVP;

    public MCH_GuiScoreboard_Main(MCH_IGuiScoreboard switcher, EntityPlayer player) {
        super(switcher, player);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.buttonSwitchPVP == null) {
            this.guiLeft = 0;
            this.guiTop = 0;
            int WIDTH = getScoreboradWidth(this.mc) * 3 / 4;
            if (WIDTH < 80) {
                WIDTH = 80;
            }

            int LEFT = getScoreBoardLeft(this.mc, this.getTeamNum() + 1, 0) / 4;
            this.buttonSwitchPVP = new W_GuiButton(1024, LEFT, 80, WIDTH, 20, "");
            this.listGui.add(this.buttonSwitchPVP);
            W_GuiButton btn = new W_GuiButton(256, LEFT, 100, WIDTH, 20, "Team shuffle");
            btn.addHoverString("Shuffle all players.");
            this.listGui.add(btn);
            this.listGui.add(new W_GuiButton(512, LEFT, 120, WIDTH, 20, "New team"));
            btn = new W_GuiButton(768, LEFT, 140, WIDTH, 20, "Jump spawn pos");
            btn.addHoverString("Teleport all players -> spawn point.");
            this.listGui.add(btn);
            btn = new W_GuiButton(1280, LEFT, 160, WIDTH, 20, "Destroy All");
            btn.addHoverString("Destroy all aircraft and vehicle.");
            this.listGui.add(btn);
        }
    }

    protected void keyTyped(char c, int code) {
        if (code == 1) {
            this.mc.player.closeScreen();
        }
    }

    @Override
    public void updateScreenButtons(List<GuiButton> list) {
        for (GuiButton o : list) {
            if (o.id == 1024) {
                o.displayString = "PVP : " + (MCH_ServerSettings.enablePVP ? "ON" : "OFF");
            }
        }
    }

    protected void actionPerformed(@NotNull GuiButton btn) {
        if (btn.enabled) {
            switch (btn.id) {
                case 256 -> new PacketHandleCommand(CommandAction.SHUFFLE_TEAM, "").sendToServer();
                case 512 -> this.switchScreen(SCREEN_ID.CREATE_TEAM);
                case 768 -> new PacketHandleCommand(CommandAction.JUMP_SPAWNPOINT, "").sendToServer();
                case 1024 -> new PacketHandleCommand(CommandAction.SETPVP, "").sendToServer();
                case 1280 -> new PacketHandleCommand(CommandAction.DESTROY_AIRCRAFT, "").sendToServer();
            }
        }
    }

    @Override
    public void drawGuiContainerForegroundLayerScreen(int x, int y) {
        super.drawGuiContainerForegroundLayerScreen(x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        drawList(this.mc, this.fontRenderer, true);
    }
}
