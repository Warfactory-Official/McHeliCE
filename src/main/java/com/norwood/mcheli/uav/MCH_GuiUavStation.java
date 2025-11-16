package com.norwood.mcheli.uav;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfoManager;
import com.norwood.mcheli.helicopter.MCH_ItemHeli;
import com.norwood.mcheli.networking.packet.PacketUavStatus;
import com.norwood.mcheli.plane.MCP_ItemPlane;
import com.norwood.mcheli.plane.MCP_PlaneInfoManager;
import com.norwood.mcheli.ship.MCH_ItemShip;
import com.norwood.mcheli.ship.MCH_ShipInfoManager;
import com.norwood.mcheli.tank.MCH_ItemTank;
import com.norwood.mcheli.tank.MCH_TankInfoManager;
import com.norwood.mcheli.wrapper.W_GuiContainer;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MCH_GuiUavStation extends W_GuiContainer {

    static final int BX = 20;
    static final int BY = 22;
    final MCH_EntityUavStation uavStation;
    private GuiButton buttonContinue;

    public MCH_GuiUavStation(InventoryPlayer inventoryPlayer, MCH_EntityUavStation uavStation) {
        super(new MCH_ContainerUavStation(inventoryPlayer, uavStation));
        this.uavStation = uavStation;
    }

    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
        if (this.uavStation != null) {
            ItemStack item = this.uavStation.getStackInSlot(0);
            MCH_AircraftInfo info = null;
            if (!item.isEmpty() && item.getItem() instanceof MCP_ItemPlane) {
                info = MCP_PlaneInfoManager.getFromItem(item.getItem());
            }

            if (!item.isEmpty() && item.getItem() instanceof MCH_ItemShip) {
                info = MCH_ShipInfoManager.getFromItem(item.getItem());
            }

            if (!item.isEmpty() && item.getItem() instanceof MCH_ItemHeli) {
                info = MCH_HeliInfoManager.getFromItem(item.getItem());
            }

            if (!item.isEmpty() && item.getItem() instanceof MCH_ItemTank) {
                info = MCH_TankInfoManager.getFromItem(item.getItem());
            }

            if (item.isEmpty() || info != null && info.isUAV) {
                if (this.uavStation.getKind() <= 1) {
                    this.drawString("UAV Station", 8, 6, 16777215);
                } else if (!item.isEmpty() && !info.isSmallUAV) {
                    this.drawString("Small UAV only", 8, 6, 16711680);
                } else {
                    this.drawString("UAV Controller", 8, 6, 16777215);
                }
            } else if (!item.isEmpty()) {
                this.drawString("Not UAV", 8, 6, 16711680);
            }

            this.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 16777215);
            this.drawString(String.format("X.%+2d", this.uavStation.posUavX), 58, 15, 16777215);
            this.drawString(String.format("Y.%+2d", this.uavStation.posUavY), 58, 37, 16777215);
            this.drawString(String.format("Z.%+2d", this.uavStation.posUavZ), 58, 59, 16777215);
        }
    }

    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        W_McClient.MOD_bindTexture("textures/gui/uav_station.png");
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }

    protected void actionPerformed(@NotNull GuiButton btn) {
        if (btn.enabled) {
            if (btn.id == 256) {
                if (this.uavStation != null && !this.uavStation.isDead &&
                        this.uavStation.getLastControlAircraft() != null &&
                        !this.uavStation.getLastControlAircraft().isDead) {
                    var data = new PacketUavStatus();
                    data.posUavX = (byte) this.uavStation.posUavX;
                    data.posUavY = (byte) this.uavStation.posUavY;
                    data.posUavZ = (byte) this.uavStation.posUavZ;
                    data.continueControl = true;
                    data.sendToServer();
                }

                this.buttonContinue.enabled = false;
            } else {
                int[] pos = new int[] { this.uavStation.posUavX, this.uavStation.posUavY, this.uavStation.posUavZ };
                int i = btn.id >> 4 & 15;
                int j = (btn.id & 15) - 1;
                int[] BTN = new int[] { -10, -1, 1, 10 };
                pos[i] += BTN[j];
                if (pos[i] < -50) {
                    pos[i] = -50;
                }

                if (pos[i] > 50) {
                    pos[i] = 50;
                }

                if (this.uavStation.posUavX != pos[0] || this.uavStation.posUavY != pos[1] ||
                        this.uavStation.posUavZ != pos[2]) {
                    var data = new PacketUavStatus();
                    data.posUavX = (byte) pos[0];
                    data.posUavY = (byte) pos[1];
                    data.posUavZ = (byte) pos[2];
                    data.sendToServer();

                }
            }
        }
    }

    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int x = this.width / 2 - 5;
        int y = this.height / 2 - 76;
        String[] BTN = new String[] { "-10", "-1", "+1", "+10" };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int id = row << 4 | col + 1;
                this.buttonList.add(new GuiButton(id, x + col * 20, y + row * 22, 20, 20, BTN[col]));
            }
        }

        this.buttonContinue = new GuiButton(256, x - 80 + 3, y + 44, 50, 20, "Continue");
        this.buttonContinue.enabled = this.uavStation != null && !this.uavStation.isDead &&
                this.uavStation.getAndSearchLastControlAircraft() != null;

        this.buttonList.add(this.buttonContinue);
    }
}
