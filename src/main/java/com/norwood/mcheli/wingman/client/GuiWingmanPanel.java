package com.norwood.mcheli.wingman.client;

import com.norwood.mcheli.networking.packet.PacketWingmanPanelAction;
import com.norwood.mcheli.networking.packet.PacketWingmanPanelData;
import com.norwood.mcheli.networking.packet.PacketWingmanPanelOpen;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ウィングマンコントロールパネル。
 * McHeli 機体に搭乗中に V キー（デフォルト）で開く。
 *
 * タブ 0: Wingmen  — 近傍機体のフォロー割り当て・攻撃モード・武器指定
 * タブ 1: Formation — 編隊パラメータ・攻撃高度制限
 *
 * テキストフィールドは使わず [-][+] ボタンで値を変更するため、
 * 操縦キー（W/S/A/D 等）と干渉しない。
 */
public class GuiWingmanPanel extends GuiScreen {

    // ─── IDs ─────────────────────────────────────────────────────────────────

    private static final int BTN_TAB_WINGMEN   = 200;
    private static final int BTN_TAB_FORMATION = 201;
    private static final int BTN_CLOSE         = 202;
    private static final int BTN_REFRESH       = 203;

    // Wingmen タブ — nearby [Follow]
    private static final int BTN_FOLLOW_BASE   = 10; // +i

    // Wingmen タブ — per wingman (各ウィングマン 5 ボタン、最大 8 機)
    // base + i*5 + 0:AUTO  +1:HOLD  +2:STOP  +3:WPN_PREV  +4:WPN_NEXT
    private static final int BTN_WINGMAN_BASE  = 40;
    private static final int WINGMAN_BTN_STRIDE = 5;
    private static final int WM_AUTO = 0, WM_HOLD = 1, WM_STOP = 2, WM_WPN_PREV = 3, WM_WPN_NEXT = 4;

    private static final int MAX_NEARBY  = 8;
    private static final int MAX_WINGMEN = 8;

    // 1機あたり: 名前行(9px) + 余白(4px) + ボタン行(14px) + 行間(5px) = 32px
    private static final int WM_ROW_H          = 32;
    // セクションタイトルから最初の行までのオフセット
    private static final int WM_TOP            = 18;
    /** Wingmen タブのコンテンツ総高さ（最大機体数想定）。スクロール計算に使用。 */
    private static final int WINGMEN_CONTENT_H = WM_TOP + MAX_WINGMEN * WM_ROW_H + 8;

    // Formation タブ
    private static final int BTN_SIDE_DEC  = 100;
    private static final int BTN_SIDE_INC  = 101;
    private static final int BTN_ALT_DEC   = 102;
    private static final int BTN_ALT_INC   = 103;
    private static final int BTN_REAR_DEC  = 104;
    private static final int BTN_REAR_INC  = 105;
    private static final int BTN_MAXW_DEC  = 106;
    private static final int BTN_MAXW_INC  = 107;
    private static final int BTN_MINAT_DEC = 108;
    private static final int BTN_MINAT_INC = 109;
    private static final int BTN_MAXAT_DEC = 110;
    private static final int BTN_MAXAT_INC = 111;
    private static final int BTN_APPLY     = 120;

    // ─── State ───────────────────────────────────────────────────────────────

    private int currentTab = 0;

    private final List<PacketWingmanPanelData.AircraftDto> nearby;
    private final List<PacketWingmanPanelData.WingmanDto>  wingmen;

    // 武器タイプ循環リスト（クライアント側暫定値）
    private static final String[] WEAPON_TYPES = {
        "", "gun", "aamissile", "asmissile", "cas", "rocket", "bomb"
    };
    // 各ウィングマンの現在武器インデックス（GUI内でのみ管理）
    private final int[] weaponIdx;

    // スクロール
    private int scrollWingmen = 0;

    // Formation 編集値（Apply で送信）
    private double fSideDist;
    private double fAltOffset;
    private double fRearDist;
    private int    fMaxWings;
    private double fMinAlt;
    private double fMaxAlt;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public GuiWingmanPanel(PacketWingmanPanelData pkt) {
        this.nearby  = new ArrayList<>(pkt.nearby);
        this.wingmen = new ArrayList<>(pkt.wingmen);

        this.fSideDist  = pkt.sideDist;
        this.fAltOffset = pkt.altOffset;
        this.fRearDist  = pkt.rearDist;
        this.fMaxWings  = pkt.maxWings;
        this.fMinAlt    = pkt.minAlt;
        this.fMaxAlt    = pkt.maxAlt;

        // 現在の武器タイプから初期インデックスを特定
        weaponIdx = new int[wingmen.size()];
        for (int i = 0; i < wingmen.size(); i++) {
            weaponIdx[i] = findWeaponIdx(wingmen.get(i).weaponType);
        }
    }

    private static int findWeaponIdx(String wt) {
        if (wt == null || wt.isEmpty()) return 0;
        for (int i = 0; i < WEAPON_TYPES.length; i++) {
            if (WEAPON_TYPES[i].equals(wt)) return i;
        }
        return 0;
    }

    // ─── Init ────────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(false);
        buttonList.clear();

        int cx = width / 2;
        int ty = height / 4;

        // タブボタン
        buttonList.add(new GuiButton(BTN_TAB_WINGMEN,   cx - 82, ty - 14, 80, 16, "Wingmen"));
        buttonList.add(new GuiButton(BTN_TAB_FORMATION, cx + 2,  ty - 14, 80, 16, "Formation"));

        // 共通ボタン（画面下端固定）
        buttonList.add(new GuiButton(BTN_REFRESH, cx - 82, height - 28, 55, 14, "Refresh"));
        buttonList.add(new GuiButton(BTN_CLOSE,   cx + 30, height - 28, 52, 14, "Close"));

        if (currentTab == 0) initWingmenTab();
        else                  initFormationTab();
    }

    // ─── Wingmen タブ ────────────────────────────────────────────────────────

    private void initWingmenTab() {
        int cx  = width / 2;
        int ty  = height / 4;
        int sty = ty - scrollWingmen; // スクロール適用後の基準Y

        // ─── Nearby 列（左半分） ───────────────────────────────────────────
        int lx = cx - 190;
        for (int i = 0; i < Math.min(nearby.size(), MAX_NEARBY); i++) {
            buttonList.add(new GuiButton(BTN_FOLLOW_BASE + i,
                    lx + 90, sty + 8 + i * 18, 52, 14, "Follow"));
        }

        // ─── Wingmen 列（右半分） ──────────────────────────────────────────
        int rx = cx - 30;
        for (int i = 0; i < Math.min(wingmen.size(), MAX_WINGMEN); i++) {
            PacketWingmanPanelData.WingmanDto wm = wingmen.get(i);
            int base = BTN_WINGMAN_BASE + i * WINGMAN_BTN_STRIDE;
            int bx   = rx;
            // 名前行(9px) + 余白(2px) = 11px下がったところにボタン行を置く
            int by   = sty + WM_TOP + i * WM_ROW_H + 11;

            boolean isAuto = (wm.attackMode == WingmanEntry.ATK_AUTO);
            buttonList.add(new GuiButton(base + WM_AUTO, bx,      by, 34, 14, isAuto ? "§2Auto" : "Auto"));
            buttonList.add(new GuiButton(base + WM_HOLD, bx + 36, by, 32, 14, "Hold"));
            buttonList.add(new GuiButton(base + WM_STOP, bx + 70, by, 30, 14, "§cStop"));
            buttonList.add(new GuiButton(base + WM_WPN_PREV, bx + 104, by, 16, 14, "<"));
            buttonList.add(new GuiButton(base + WM_WPN_NEXT, bx + 156, by, 16, 14, ">"));
        }

        // クリップ外のボタンを非表示
        int clipTop = ty, clipBot = height - 28;
        for (GuiButton b : buttonList) {
            if (isWingmenButton(b.id)) {
                b.visible = b.y + b.height > clipTop && b.y < clipBot;
            }
        }
    }

    private boolean isWingmenButton(int id) {
        return (id >= BTN_FOLLOW_BASE  && id < BTN_FOLLOW_BASE  + MAX_NEARBY)
            || (id >= BTN_WINGMAN_BASE && id < BTN_WINGMAN_BASE + MAX_WINGMEN * WINGMAN_BTN_STRIDE);
    }

    private static String weaponLabel(int idx) {
        String t = WEAPON_TYPES[idx];
        if (t.isEmpty()) return "Any";
        return t;
    }

    // ─── Formation タブ ──────────────────────────────────────────────────────

    private void initFormationTab() {
        int cx = width / 2;
        int ty = height / 4 + 10;
        int btnW = 18;
        int rowH = 22;

        addSpinner(BTN_SIDE_DEC,  BTN_SIDE_INC,  cx, ty,           btnW);
        addSpinner(BTN_ALT_DEC,   BTN_ALT_INC,   cx, ty + rowH,    btnW);
        addSpinner(BTN_REAR_DEC,  BTN_REAR_INC,  cx, ty + rowH*2,  btnW);
        addSpinner(BTN_MAXW_DEC,  BTN_MAXW_INC,  cx, ty + rowH*3,  btnW);
        addSpinner(BTN_MINAT_DEC, BTN_MINAT_INC, cx, ty + rowH*4 + 8, btnW);
        addSpinner(BTN_MAXAT_DEC, BTN_MAXAT_INC, cx, ty + rowH*5 + 8, btnW);

        buttonList.add(new GuiButton(BTN_APPLY, cx - 30, ty + rowH*7 + 10, 60, 16, "Apply"));
    }

    private void addSpinner(int decId, int incId, int cx, int y, int btnW) {
        // - ボタン: cx-50, + ボタン: cx+32（gap 中央 = cx-1 に値テキストを描画するため）
        buttonList.add(new GuiButton(decId, cx - 50, y, btnW, 14, "-"));
        buttonList.add(new GuiButton(incId, cx + 32, y, btnW, 14, "+"));
    }

    // ─── Draw ────────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        int cx = width / 2;
        int ty = height / 4;

        drawCenteredString(fontRenderer, "§eWingman Panel", cx, ty - 26, 0xFFFFFF);

        // タブ強調下線
        highlightTab(BTN_TAB_WINGMEN,   currentTab == 0);
        highlightTab(BTN_TAB_FORMATION, currentTab == 1);

        if (currentTab == 0) drawWingmenTab(cx, ty);
        else                   drawFormationTab(cx, ty);

        super.drawScreen(mx, my, pt);
    }

    private void drawWingmenTab(int cx, int ty) {
        // GL Scissor でスクロール領域をクリップ
        int clipTop = ty, clipBot = height - 28;
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, (height - clipBot) * sf, width * sf, (clipBot - clipTop) * sf);

        int sty = ty - scrollWingmen;
        int lx  = cx - 190;
        int ry  = sty + 8;

        // ─── Nearby タイトル ──────────────────────────────────────────────
        drawString(fontRenderer, "§7─ Nearby (unassigned) ─", lx, sty + 2, 0x888888);
        for (int i = 0; i < Math.min(nearby.size(), MAX_NEARBY); i++) {
            PacketWingmanPanelData.AircraftDto ac = nearby.get(i);
            String label = ac.name + " §7" + shortUuid(ac.uuid);
            drawString(fontRenderer, label, lx, ry + i * 18 + 3, 0xFFFFFF);
        }
        if (nearby.isEmpty()) {
            drawString(fontRenderer, "§7(none nearby)", lx, ry + 3, 0x888888);
        }

        // ─── Wingmen タイトル ─────────────────────────────────────────────
        int rx = cx - 30;
        drawString(fontRenderer, "§7─ Your Wingmen ─", rx, sty + 2, 0x888888);
        for (int i = 0; i < Math.min(wingmen.size(), MAX_WINGMEN); i++) {
            PacketWingmanPanelData.WingmanDto wm = wingmen.get(i);
            // 各行の先頭Y（WM_TOP + i * WM_ROW_H 分下がったところ）
            int rowY = sty + WM_TOP + i * WM_ROW_H;
            String header = "§f" + wm.name + " §7#" + wm.slot + " §8[" + wm.state + "]";
            drawString(fontRenderer, header, rx, rowY, 0xFFFFFF);
            // 武器ラベル: ボタンY = rowY+11、ボタン高さ14 なので中央 = rowY+11+3
            int wpnLabelX = rx + 104 + 16 + (156 - (104 + 16)) / 2; // < > の間の中央X
            String wpnLabel = "§e" + weaponLabel(weaponIdx[i]);
            drawCenteredString(fontRenderer, wpnLabel, wpnLabelX, rowY + 14, 0xFFFFFF);
        }
        if (wingmen.isEmpty()) {
            drawString(fontRenderer, "§7(no wingmen assigned)", rx, ry + 3, 0x888888);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawScrollbar(clipTop, clipBot, scrollWingmen, WINGMEN_CONTENT_H);
    }

    private void drawFormationTab(int cx, int ty) {
        int labelX = cx - 140;
        int valueX = cx - 1; // gap 中央（- は cx-50 幅18 → 終点 cx-32、+ は cx+32 → gap cx-32〜cx+32 中央 = cx）
        int rowH   = 22;
        int baseY  = ty + 10;

        drawString(fontRenderer, "§7─ Formation ─", labelX, baseY - 8, 0x888888);
        drawRow(labelX, valueX, baseY,           "Side dist:",    fmt(fSideDist));
        drawRow(labelX, valueX, baseY + rowH,    "Alt offset:",   fmt(fAltOffset));
        drawRow(labelX, valueX, baseY + rowH*2,  "Rear dist:",    fmt(fRearDist));
        drawRow(labelX, valueX, baseY + rowH*3,  "Max wingmen:",  Integer.toString(fMaxWings));

        drawString(fontRenderer, "§7─ Attack Altitude ─", labelX, baseY + rowH*4 + 2, 0x888888);
        drawRow(labelX, valueX, baseY + rowH*4 + 8, "Min alt:", fMinAlt == 0 ? "off" : fmt(fMinAlt));
        drawRow(labelX, valueX, baseY + rowH*5 + 8, "Max alt:", fMaxAlt == 0 ? "off" : fmt(fMaxAlt));

        drawString(fontRenderer, "§7(0 = no limit)", labelX, baseY + rowH*6 + 4, 0x666666);
    }

    private void drawRow(int lx, int vx, int y, String label, String value) {
        drawString(fontRenderer, "§7" + label, lx, y + 3, 0xAAAAAA);
        drawCenteredString(fontRenderer, "§f" + value, vx, y + 3, 0xFFFFFF);
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }

    private void highlightTab(int btnId, boolean active) {
        if (!active) return;
        for (GuiButton b : buttonList) {
            if (b.id == btnId) {
                drawRect(b.x, b.y + b.height, b.x + b.width, b.y + b.height + 2, 0xFFFFAA00);
                break;
            }
        }
    }

    private static String shortUuid(String uuid) {
        return uuid.length() >= 8 ? uuid.substring(0, 8) + "..." : uuid;
    }

    /** 汎用スクロールバーを描画する。 */
    private void drawScrollbar(int top, int bot, int scroll, int contentH) {
        int visH  = bot - top;
        int maxSc = Math.max(0, contentH - visH);
        if (maxSc <= 0) return;
        int bx   = width - 7;
        int barH = Math.max(16, visH * visH / contentH);
        int barY = top + (int) ((long) scroll * (visH - barH) / maxSc);
        drawRect(bx, top,  bx + 5, bot,       0x44FFFFFF);
        drawRect(bx, barY, bx + 5, barY + barH, 0xAAFFFFFF);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int step = wheel > 0 ? -14 : 14;
        if (currentTab == 0) {
            int visH  = (height - 28) - (height / 4);
            int maxSc = Math.max(0, WINGMEN_CONTENT_H - visH);
            scrollWingmen = Math.max(0, Math.min(scrollWingmen + step, maxSc));
            initGui();
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        switch (btn.id) {
            case BTN_TAB_WINGMEN:
                if (currentTab != 0) { currentTab = 0; scrollWingmen = 0; initGui(); } return;
            case BTN_TAB_FORMATION:
                if (currentTab != 1) { currentTab = 1; initGui(); } return;
            case BTN_CLOSE:
                mc.player.closeScreen(); return;
            case BTN_REFRESH:
                new PacketWingmanPanelOpen().sendToServer();
                return;
            case BTN_APPLY:
                sendFormation(); return;
        }

        if (currentTab == 0) {
            actionWingmen(btn);
        } else {
            actionFormation(btn);
        }
    }

    // ─── Wingmen tab actions ──────────────────────────────────────────────────

    private void actionWingmen(GuiButton btn) {
        // Follow ボタン
        int followIdx = btn.id - BTN_FOLLOW_BASE;
        if (followIdx >= 0 && followIdx < nearby.size()) {
            PacketWingmanPanelAction pkt = new PacketWingmanPanelAction();
            pkt.action = PacketWingmanPanelAction.FOLLOW;
            pkt.uuid   = nearby.get(followIdx).uuid;
            pkt.sendToServer();
            // ローカルからリストを消してリフレッシュ
            nearby.remove(followIdx);
            initGui();
            return;
        }

        // Wingman コントロールボタン
        int rel = btn.id - BTN_WINGMAN_BASE;
        if (rel < 0) return;
        int wmIdx   = rel / WINGMAN_BTN_STRIDE;
        int btnType = rel % WINGMAN_BTN_STRIDE;
        if (wmIdx >= wingmen.size()) return;

        PacketWingmanPanelData.WingmanDto wm = wingmen.get(wmIdx);

        switch (btnType) {
            case WM_AUTO: {
                PacketWingmanPanelAction pkt = new PacketWingmanPanelAction();
                pkt.action = PacketWingmanPanelAction.AUTO;
                pkt.uuid   = wm.uuid;
                pkt.sendToServer();
                wm.attackMode = WingmanEntry.ATK_AUTO;
                initGui();
                break;
            }
            case WM_HOLD: {
                PacketWingmanPanelAction pkt = new PacketWingmanPanelAction();
                pkt.action = PacketWingmanPanelAction.HOLD;
                pkt.uuid   = wm.uuid;
                pkt.sendToServer();
                wm.attackMode = WingmanEntry.ATK_NONE;
                initGui();
                break;
            }
            case WM_STOP: {
                PacketWingmanPanelAction pkt = new PacketWingmanPanelAction();
                pkt.action = PacketWingmanPanelAction.STOP;
                pkt.uuid   = wm.uuid;
                pkt.sendToServer();
                wingmen.remove(wmIdx);
                initGui();
                break;
            }
            case WM_WPN_PREV: {
                weaponIdx[wmIdx] = (weaponIdx[wmIdx] - 1 + WEAPON_TYPES.length) % WEAPON_TYPES.length;
                sendWeapon(wm.uuid, weaponIdx[wmIdx]);
                // ラベルは drawScreen で更新されるのでinitGui不要だがボタン再描画のため呼ぶ
                break;
            }
            case WM_WPN_NEXT: {
                weaponIdx[wmIdx] = (weaponIdx[wmIdx] + 1) % WEAPON_TYPES.length;
                sendWeapon(wm.uuid, weaponIdx[wmIdx]);
                break;
            }
        }
    }

    private void sendWeapon(String uuid, int idx) {
        PacketWingmanPanelAction pkt = new PacketWingmanPanelAction();
        pkt.action = PacketWingmanPanelAction.WEAPON;
        pkt.uuid   = uuid;
        pkt.extra  = WEAPON_TYPES[idx]; // "" = any
        pkt.sendToServer();
    }

    // ─── Formation tab actions ────────────────────────────────────────────────

    private static final double STEP_DIST = 5.0;
    private static final double STEP_ALT  = 10.0;

    private void actionFormation(GuiButton btn) {
        switch (btn.id) {
            case BTN_SIDE_DEC:  fSideDist  = Math.max(0, fSideDist  - STEP_DIST); break;
            case BTN_SIDE_INC:  fSideDist  += STEP_DIST; break;
            case BTN_ALT_DEC:   fAltOffset -= STEP_DIST; break;
            case BTN_ALT_INC:   fAltOffset += STEP_DIST; break;
            case BTN_REAR_DEC:  fRearDist  = Math.max(0, fRearDist  - STEP_DIST); break;
            case BTN_REAR_INC:  fRearDist  += STEP_DIST; break;
            case BTN_MAXW_DEC:  fMaxWings  = Math.max(1, fMaxWings  - 1); break;
            case BTN_MAXW_INC:  fMaxWings  = Math.min(64, fMaxWings + 1); break;
            case BTN_MINAT_DEC: fMinAlt    = Math.max(0, fMinAlt - STEP_ALT); break;
            case BTN_MINAT_INC: fMinAlt    += STEP_ALT; break;
            case BTN_MAXAT_DEC: fMaxAlt    = Math.max(0, fMaxAlt - STEP_ALT); break;
            case BTN_MAXAT_INC: fMaxAlt    += STEP_ALT; break;
        }
        // Formation タブは即時再描画（drawScreen が呼ばれるので initGui 不要）
    }

    private void sendFormation() {
        PacketWingmanPanelAction pkt = new PacketWingmanPanelAction();
        pkt.action    = PacketWingmanPanelAction.FORMATION;
        pkt.sideDist  = fSideDist;
        pkt.altOffset = fAltOffset;
        pkt.rearDist  = fRearDist;
        pkt.maxWings  = fMaxWings;
        pkt.minAlt    = fMinAlt;
        pkt.maxAlt    = fMaxAlt;
        pkt.sendToServer();
    }

    // ─── Misc ────────────────────────────────────────────────────────────────

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.player.closeScreen();
        }
        // 他のキー入力はゲームに渡さない（テキストフィールドがないため不要）
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
