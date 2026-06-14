package com.norwood.mcheli.wingman.client;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.networking.packet.PacketMissionAction;
import com.norwood.mcheli.networking.packet.PacketRouteAction;
import com.norwood.mcheli.networking.packet.PacketPlannerData.MarkerDto;
import com.norwood.mcheli.networking.packet.PacketPlannerData.UavDto;
import com.norwood.mcheli.networking.packet.PacketPlannerData.RouteDto;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiWingmanPlanner extends GuiScreen {

    // layout
    private static final int ROW_H       = 22;   // px per list row
    private static final int VISIBLE     = 8;    // rows visible per panel
    private static final int PANEL_W     = 230;
    private static final int GAP         = 20;

    private final List<UavDto>    uavs;
    private final List<RouteDto>  routes;
    private final List<MarkerDto> markers;
    private final double playerX, playerY, playerZ;

    private int selUav   = -1;
    private int selRoute = -1;
    private int uavScroll   = 0;
    private int routeScroll = 0;

    // button ids
    private static final int BTN_ASSIGN = 0, BTN_ABORT = 1, BTN_NEW = 2, BTN_DELETE = 3, BTN_CLOSE = 4;

    private GuiButton btnAssign, btnAbort, btnNew, btnDelete, btnClose;

    // computed layout
    private int panelTop, uavX, routeX;

    public GuiWingmanPlanner(List<UavDto> uavs, List<RouteDto> routes, List<MarkerDto> markers,
                             double playerX, double playerY, double playerZ) {
        this.uavs    = uavs;
        this.routes  = routes;
        this.markers = markers;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
    }

    @Override
    public void initGui() {
        int totalW  = PANEL_W * 2 + GAP;
        uavX        = width / 2 - totalW / 2;
        routeX      = uavX + PANEL_W + GAP;

        int listH   = VISIBLE * ROW_H;
        panelTop    = height / 2 - (listH + 50) / 2;

        int btnY    = panelTop + listH + 20;
        int btnW    = 110;

        buttonList.clear();
        buttonList.add(btnAssign = new GuiButton(BTN_ASSIGN, uavX,              btnY, btnW, 20, "Assign Mission"));
        buttonList.add(btnAbort  = new GuiButton(BTN_ABORT,  uavX + btnW + 5,   btnY, btnW, 20, "Abort Mission"));
        buttonList.add(btnNew    = new GuiButton(BTN_NEW,    routeX,            btnY, btnW, 20, "New Route"));
        buttonList.add(btnDelete = new GuiButton(BTN_DELETE, routeX + btnW + 5, btnY, btnW, 20, "Delete Route"));
        buttonList.add(btnClose  = new GuiButton(BTN_CLOSE,  width / 2 - 40,    btnY + 25, 80, 20, "Close"));

        refreshButtons();
    }

    private void refreshButtons() {
        btnAssign.enabled = selUav >= 0 && selRoute >= 0;
        btnAbort.enabled  = selUav >= 0;
        btnDelete.enabled = selRoute >= 0;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();

        // Title
        drawCenteredString(fontRenderer, "§eWingman Mission Planner", width / 2, panelTop - 20, 0xFFFFFF);

        // UAV panel
        drawString(fontRenderer, "§bActive UAVs (" + uavs.size() + ")", uavX, panelTop - 10, 0xFFFFFF);
        drawPanel(uavs, uavX, panelTop, uavScroll, selUav, true, mx, my);

        // Route panel
        drawString(fontRenderer, "§aRoutes (" + routes.size() + ")", routeX, panelTop - 10, 0xFFFFFF);
        drawPanel(routes, routeX, panelTop, routeScroll, selRoute, false, mx, my);

        // Route node detail below route panel
        if (selRoute >= 0 && selRoute < routes.size()) {
            RouteDto r = routes.get(selRoute);
            int dy = panelTop + VISIBLE * ROW_H + 5;
            for (int i = 0; i < Math.min(3, r.nodes.size()); i++) {
                drawString(fontRenderer, "§8[" + i + "] " + r.nodes.get(i), routeX, dy + i * 10, 0xAAAAAA);
            }
            if (r.nodes.size() > 3) {
                drawString(fontRenderer, "§8... (" + r.nodes.size() + " total)", routeX, dy + 30, 0x888888);
            }
        }

        super.drawScreen(mx, my, pt);
    }

    private void drawPanel(List<?> items, int x, int top, int scroll, int sel,
                            boolean isUav, int mx, int my) {
        // Background
        drawRect(x - 2, top - 2, x + PANEL_W + 2, top + VISIBLE * ROW_H + 2, 0x88000000);

        int visible = Math.min(VISIBLE, items.size() - scroll);
        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            int ry  = top + i * ROW_H;
            boolean hov = mx >= x && mx < x + PANEL_W && my >= ry && my < ry + ROW_H - 1;
            boolean isSel = idx == sel;

            int bg = isSel ? 0xAA005599 : (hov ? 0x44AAAAAA : 0x00000000);
            drawRect(x, ry, x + PANEL_W, ry + ROW_H - 1, bg);

            if (isUav) {
                UavDto d = (UavDto) items.get(idx);
                String label = (isSel ? "§e" : "§f") + d.name + " §7[" + d.state + "]";
                if (d.nodeCount > 0) label += " §a" + d.nodeIdx + "/" + d.nodeCount;
                drawString(fontRenderer, label, x + 4, ry + 3, 0xFFFFFF);
                drawString(fontRenderer, "§8" + d.uuid.substring(0, 8) + "...", x + 4, ry + 12, 0x777777);
            } else {
                RouteDto d = (RouteDto) items.get(idx);
                String label = (isSel ? "§e" : "§f") + d.name;
                drawString(fontRenderer, label, x + 4, ry + 3, 0xFFFFFF);
                drawString(fontRenderer, "§8" + d.nodes.size() + " nodes", x + 4, ry + 12, 0x777777);
            }
        }

        // Scroll indicator
        if (items.size() > VISIBLE) {
            int trackH = VISIBLE * ROW_H;
            int thumbH = Math.max(10, trackH * VISIBLE / items.size());
            int thumbY = top + trackH * scroll / items.size();
            drawRect(x + PANEL_W + 3, top, x + PANEL_W + 7, top + trackH, 0x44FFFFFF);
            drawRect(x + PANEL_W + 3, thumbY, x + PANEL_W + 7, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        // UAV list click
        int visible = Math.min(VISIBLE, uavs.size() - uavScroll);
        for (int i = 0; i < visible; i++) {
            int ry = panelTop + i * ROW_H;
            if (mx >= uavX && mx < uavX + PANEL_W && my >= ry && my < ry + ROW_H - 1) {
                selUav = i + uavScroll;
                refreshButtons();
                return;
            }
        }

        // Route list click
        visible = Math.min(VISIBLE, routes.size() - routeScroll);
        for (int i = 0; i < visible; i++) {
            int ry = panelTop + i * ROW_H;
            if (mx >= routeX && mx < routeX + PANEL_W && my >= ry && my < ry + ROW_H - 1) {
                selRoute = i + routeScroll;
                refreshButtons();
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int delta = wheel > 0 ? -1 : 1;

        int mx = Mouse.getEventX() * width / mc.displayWidth;
        if (mx < width / 2) {
            // UAV panel
            uavScroll = Math.max(0, Math.min(uavScroll + delta, Math.max(0, uavs.size() - VISIBLE)));
        } else {
            // Route panel
            routeScroll = Math.max(0, Math.min(routeScroll + delta, Math.max(0, routes.size() - VISIBLE)));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_ASSIGN:
                if (selUav >= 0 && selRoute >= 0) {
                    new PacketMissionAction(
                        PacketMissionAction.ASSIGN,
                        uavs.get(selUav).uuid,
                        routes.get(selRoute).name).sendToServer();
                    mc.displayGuiScreen(null);
                }
                break;
            case BTN_ABORT:
                if (selUav >= 0) {
                    new PacketMissionAction(
                        PacketMissionAction.ABORT,
                        uavs.get(selUav).uuid, "").sendToServer();
                    mc.displayGuiScreen(null);
                }
                break;
            case BTN_NEW:
                mc.displayGuiScreen(new GuiNewRoute(this, routes, markers, playerX, playerY, playerZ));
                break;
            case BTN_DELETE:
                if (selRoute >= 0) {
                    new PacketRouteAction(
                        PacketRouteAction.DELETE,
                        routes.get(selRoute).name,
                        Collections.emptyList()).sendToServer();
                    mc.displayGuiScreen(null);
                }
                break;
            case BTN_CLOSE:
                mc.displayGuiScreen(null);
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
