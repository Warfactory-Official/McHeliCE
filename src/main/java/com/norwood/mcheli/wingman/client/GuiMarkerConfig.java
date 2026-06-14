package com.norwood.mcheli.wingman.client;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.block.MarkerType;
import com.norwood.mcheli.networking.packet.PacketMarkerUpdate;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * WingmanMarkerBlock 右クリックで開くコンフィグ画面。
 *
 * レイアウト:
 *   タイトル
 *   ── 種別ボタン行1（BASE / Runway-A / Runway-B / Parking）
 *   ── 種別ボタン行2（Waypoint / Helipad / Helipad-B）
 *   ID フィールド
 *   ベース フィールド（BASE 種別のときは無効）
 *   [駐機方位] ボタン行（PARKING 種別のときのみ表示）
 *   [Save] [Cancel]
 */
public class GuiMarkerConfig extends GuiScreen {

    private static final int BTN_BASE      = 0;
    private static final int BTN_RUNWAY_A  = 1;
    private static final int BTN_RUNWAY_B  = 2;
    private static final int BTN_PARKING   = 3;
    private static final int BTN_WAYPOINT  = 4;
    private static final int BTN_HELIPAD   = 5;
    private static final int BTN_HELIPAD_B = 6;
    private static final int BTN_SAVE      = 10;
    private static final int BTN_CANCEL    = 11;
    // 駐機方位ボタン (PARKING のみ表示)
    private static final int BTN_HDG_ANY   = 20; // 任意
    private static final int BTN_HDG_N     = 21; // 北
    private static final int BTN_HDG_E     = 22; // 東
    private static final int BTN_HDG_S     = 23; // 南
    private static final int BTN_HDG_W     = 24; // 西

    private static final MarkerType[] TYPE_ORDER = {
        MarkerType.BASE, MarkerType.RUNWAY_A, MarkerType.RUNWAY_B,
        MarkerType.PARKING, MarkerType.WAYPOINT, MarkerType.HELIPAD,
        MarkerType.HELIPAD_B
    };

    private final BlockPos pos;
    private MarkerType selectedType;
    /** 駐機方位: -1=任意, 0=N, 1=E, 2=S, 3=W */
    private int parkingHeading;

    private GuiTextField idField;
    private GuiTextField baseIdField;

    public GuiMarkerConfig(BlockPos pos, MarkerType type, String id, String baseId) {
        this(pos, type, id, baseId, -1);
    }

    public GuiMarkerConfig(BlockPos pos, MarkerType type, String id, String baseId, int parkingHeading) {
        this.pos            = pos;
        this.selectedType   = type;
        this.parkingHeading = parkingHeading;
        this._initId        = id;
        this._initBaseId    = baseId;
    }

    // initGui 前の初期値を保持するバッファ
    private final String _initId;
    private final String _initBaseId;

    // ─── 初期化 ──────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int cx   = width / 2;
        int top  = height / 4;

        // 種別ボタン（2行レイアウト）
        int btnW = 78;
        int gap  = 4;
        // 行1 (インデックス 0〜3)
        int row1Count = 4;
        int row1W = btnW * row1Count + gap * (row1Count - 1);
        int row1X = cx - row1W / 2;
        for (int i = 0; i < row1Count && i < TYPE_ORDER.length; i++) {
            buttonList.add(new GuiButton(i, row1X + i * (btnW + gap), top, btnW, 20,
                TYPE_ORDER[i].shortName()));
        }
        // 行2 (インデックス 4〜6)
        int row2Count = TYPE_ORDER.length - row1Count;
        int row2W = btnW * row2Count + gap * (row2Count - 1);
        int row2X = cx - row2W / 2;
        for (int i = 0; i < row2Count; i++) {
            int idx = row1Count + i;
            buttonList.add(new GuiButton(idx, row2X + i * (btnW + gap), top + 24, btnW, 20,
                TYPE_ORDER[idx].shortName()));
        }

        // ID フィールド（ボタン2行分 +24px 下にシフト）
        idField = new GuiTextField(0, fontRenderer, cx - 100, top + 60, 200, 20);
        idField.setMaxStringLength(64);
        idField.setText(_initId);
        idField.setFocused(true);

        // ベース フィールド
        baseIdField = new GuiTextField(1, fontRenderer, cx - 100, top + 96, 200, 20);
        baseIdField.setMaxStringLength(64);
        baseIdField.setText(_initBaseId);

        // 駐機方位ボタン（PARKING のみ）
        if (selectedType == MarkerType.PARKING) {
            int hdgY  = top + 130;
            int hdgW  = 44;
            int hdgGap = 4;
            int totalW = 5 * hdgW + 4 * hdgGap;
            int hdgX  = cx - totalW / 2;
            buttonList.add(new GuiButton(BTN_HDG_ANY, hdgX,                      hdgY, hdgW, 18, "任意"));
            buttonList.add(new GuiButton(BTN_HDG_N,   hdgX +   (hdgW + hdgGap), hdgY, hdgW, 18, "北 ↑"));
            buttonList.add(new GuiButton(BTN_HDG_E,   hdgX + 2*(hdgW + hdgGap), hdgY, hdgW, 18, "東 →"));
            buttonList.add(new GuiButton(BTN_HDG_S,   hdgX + 3*(hdgW + hdgGap), hdgY, hdgW, 18, "南 ↓"));
            buttonList.add(new GuiButton(BTN_HDG_W,   hdgX + 4*(hdgW + hdgGap), hdgY, hdgW, 18, "西 ←"));
        }

        // Save / Cancel（駐機方位ボタンの有無でY位置を調整）
        int saveY = (selectedType == MarkerType.PARKING) ? top + 156 : top + 132;
        buttonList.add(new GuiButton(BTN_SAVE,   cx - 54, saveY, 50, 20, "Save"));
        buttonList.add(new GuiButton(BTN_CANCEL,  cx + 4,  saveY, 50, 20, "Cancel"));

        refreshButtonState();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ─── 更新 ─────────────────────────────────────────────────────────────────

    @Override
    public void updateScreen() {
        idField.updateCursorCounter();
        baseIdField.updateCursorCounter();
    }

    // ─── 描画 ─────────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int cx  = width / 2;
        int top = height / 4;

        // タイトル
        drawCenteredString(fontRenderer, "Wingman Marker Config", cx, top - 22, 0xFFFFFF);

        // 現在選択されている種別をハイライト（ボタン上に下線）
        for (int i = 0; i < TYPE_ORDER.length; i++) {
            if (TYPE_ORDER[i] == selectedType) {
                GuiButton btn = buttonList.get(i);
                drawRect(btn.x, btn.y + btn.height, btn.x + btn.width, btn.y + btn.height + 2, 0xFFFFAA00);
            }
        }

        // フィールドラベル（タイプに応じて切り替え）
        if (selectedType == MarkerType.BASE) {
            drawString(fontRenderer, "Base Name:", cx - 100, top + 52, 0xAAAAAA);
        } else {
            drawString(fontRenderer, "Marker ID:", cx - 100, top + 52, 0xAAAAAA);
            drawString(fontRenderer, "Parent Base:", cx - 100, top + 88, 0xAAAAAA);
        }

        // 駐機方位ラベル＆ハイライト（PARKING のみ）
        if (selectedType == MarkerType.PARKING) {
            drawString(fontRenderer, "駐機方位:", cx - 100, top + 122, 0xAAAAAA);
            // 選択中の方位ボタンをハイライト
            int selBtn = parkingHeading < 0 ? BTN_HDG_ANY
                       : BTN_HDG_N + parkingHeading;
            for (GuiButton b : buttonList) {
                if (b.id == selBtn) {
                    drawRect(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, 0xAAFFAA00);
                    break;
                }
            }
        }

        idField.drawTextBox();
        if (selectedType != MarkerType.BASE) baseIdField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ─── 入力 ─────────────────────────────────────────────────────────────────

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_BASE:
            case BTN_RUNWAY_A:
            case BTN_RUNWAY_B:
            case BTN_PARKING:
            case BTN_WAYPOINT:
            case BTN_HELIPAD:
            case BTN_HELIPAD_B:
                selectedType = TYPE_ORDER[button.id];
                refreshButtonState();
                initGui(); // ボタン行の再構築（方位ボタンの表示/非表示切り替え）
                break;
            case BTN_HDG_ANY: parkingHeading = -1; break;
            case BTN_HDG_N:   parkingHeading =  0; break;
            case BTN_HDG_E:   parkingHeading =  1; break;
            case BTN_HDG_S:   parkingHeading =  2; break;
            case BTN_HDG_W:   parkingHeading =  3; break;
            case BTN_SAVE:
                save();
                mc.player.closeScreen();
                break;
            case BTN_CANCEL:
                mc.player.closeScreen();
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.player.closeScreen();
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            // ID ↔ Base フォーカス切り替え
            if (idField.isFocused()) {
                idField.setFocused(false);
                baseIdField.setFocused(true);
            } else {
                baseIdField.setFocused(false);
                idField.setFocused(true);
            }
            return;
        }
        if (idField.isFocused())     idField.textboxKeyTyped(typedChar, keyCode);
        else if (baseIdField.isFocused()) baseIdField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        idField.mouseClicked(mouseX, mouseY, mouseButton);
        baseIdField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // ─── ヘルパー ─────────────────────────────────────────────────────────────

    /** BASE 種別のときは Parent Base フィールドを無効化・非表示扱い */
    private void refreshButtonState() {
        boolean isBase = (selectedType == MarkerType.BASE);
        baseIdField.setEnabled(!isBase);
        if (isBase) {
            baseIdField.setText("");
            if (baseIdField.isFocused()) {
                baseIdField.setFocused(false);
                idField.setFocused(true);
            }
        }
        // 非 PARKING に切り替えたとき方位をリセット
        if (selectedType != MarkerType.PARKING) {
            parkingHeading = -1;
        }
    }

    private void save() {
        String id     = idField.getText().trim();
        String baseId = selectedType == MarkerType.BASE ? "" : baseIdField.getText().trim();
        int heading   = (selectedType == MarkerType.PARKING) ? parkingHeading : -1;
        new PacketMarkerUpdate(pos, selectedType, id, baseId, heading).sendToServer();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
