package com.norwood.mcheli.wingman.client;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.networking.packet.PacketPlannerData.MarkerDto;
import com.norwood.mcheli.networking.packet.PacketPlannerData.RouteDto;
import com.norwood.mcheli.networking.packet.PacketRouteAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ルート作成画面。ノードタイプをボタンで選択し、
 * 座標は[My Pos]ボタンで自動入力、マーカーIDはクリックで選択できる。
 */
@SideOnly(Side.CLIENT)
public class GuiNewRoute extends GuiScreen {

    // ノードタイプ定数
    private static final int T_TAKEOFF = 0, T_FLYTO = 1, T_ATTACK = 2,
                              T_LOITER = 3, T_LAND = 4, T_PARK = 5;
    private static final String[] TYPE_LABELS = {"Takeoff", "Fly To", "Attack", "Loiter", "Land", "Park"};
    // マーカータイプ（タイプボタンに対応するマーカーType名）
    private static final String[] MARKER_TYPE = {"RUNWAY_A", "WAYPOINT", null, null, "RUNWAY_B", "PARKING"};

    private final GuiScreen          parent;
    private final List<RouteDto>     existingRoutes;
    private final List<MarkerDto>    markers;
    private final double             playerX, playerY, playerZ;

    // 入力状態
    private final List<String> nodes = new ArrayList<>();
    private int selectedType = -1;
    private String errorMsg  = "";

    // Widgets
    private GuiTextField nameField;
    private GuiTextField idField;                       // TAKEOFF / LAND / PARK
    private GuiTextField xField, yField, zField;        // FLY_TO
    private GuiTextField numField;                      // ATTACK(radius) / LOITER(ticks)
    private GuiButton[]  typeBtns = new GuiButton[6];
    private GuiButton    btnMyPos, btnAdd, btnSave, btnCancel;

    // Layout anchors (set in initGui)
    private int cx, formY;

    public GuiNewRoute(GuiScreen parent, List<RouteDto> existingRoutes,
                       List<MarkerDto> markers,
                       double playerX, double playerY, double playerZ) {
        this.parent         = parent;
        this.existingRoutes = existingRoutes;
        this.markers        = markers;
        this.playerX        = playerX;
        this.playerY        = playerY;
        this.playerZ        = playerZ;
    }

    @Override
    public void initGui() {
        cx    = width / 2;
        formY = height / 2 - 30;   // フォームエリアの上端

        buttonList.clear();

        // ルート名
        nameField = new GuiTextField(0, fontRenderer, cx - 120, formY - 90, 240, 20);
        nameField.setMaxStringLength(64);
        nameField.setFocused(true);

        // タイプ選択ボタン (6個横並び)
        int bw = 72, gap = 3;
        int startX = cx - (bw * 6 + gap * 5) / 2;
        for (int i = 0; i < 6; i++) {
            typeBtns[i] = new GuiButton(10 + i, startX + i * (bw + gap), formY - 60, bw, 18, TYPE_LABELS[i]);
            buttonList.add(typeBtns[i]);
        }

        // フォームフィールド
        idField  = new GuiTextField(1, fontRenderer, cx - 120, formY - 30, 200, 20);
        xField   = new GuiTextField(2, fontRenderer, cx - 120, formY - 30,  55, 20);
        yField   = new GuiTextField(3, fontRenderer, cx -  58, formY - 30,  55, 20);
        zField   = new GuiTextField(4, fontRenderer, cx +   4, formY - 30,  55, 20);
        numField = new GuiTextField(5, fontRenderer, cx -  60, formY - 30, 120, 20);
        idField.setMaxStringLength(64);
        xField.setMaxStringLength(8); yField.setMaxStringLength(8); zField.setMaxStringLength(8);
        numField.setMaxStringLength(10);

        // My Pos ボタン（FLY_TO時のみ visible）
        buttonList.add(btnMyPos  = new GuiButton(20, cx + 68,  formY - 30, 55, 20, "My Pos"));
        // Add ボタン
        buttonList.add(btnAdd    = new GuiButton(21, cx - 30,  formY - 4,  60, 18, "+ Add"));
        // Save / Cancel
        buttonList.add(btnSave   = new GuiButton(22, cx - 85,  height / 2 + 90, 75, 20, "Save Route"));
        buttonList.add(btnCancel = new GuiButton(23, cx + 10,  height / 2 + 90, 75, 20, "Cancel"));

        refreshVisibility();
    }

    private void refreshVisibility() {
        btnMyPos.visible = selectedType == T_FLYTO;
        btnAdd.enabled   = selectedType >= 0;
    }

    // ─── 描画 ────────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();

        drawCenteredString(fontRenderer, "§eNew Route", cx, height / 2 - 130, 0xFFFFFF);

        // ルート名ラベル
        drawString(fontRenderer, "Route name:", cx - 120, formY - 103, 0xAAAAAA);
        nameField.drawTextBox();

        // タイプ選択ラベル
        drawString(fontRenderer, "Add node:", cx - 120, formY - 73, 0xAAAAAA);

        // 選択中タイプをハイライト
        if (selectedType >= 0) {
            GuiButton sel = typeBtns[selectedType];
            drawRect(sel.x - 1, sel.y - 1, sel.x + sel.width + 1, sel.y + sel.height + 1, 0xFFFFAA00);
        }

        // フォームエリア
        if (selectedType >= 0) drawForm(mx, my);

        // ノードリスト
        drawNodeList(mx, my);

        // エラー
        if (!errorMsg.isEmpty()) {
            drawCenteredString(fontRenderer, "§c" + errorMsg, cx, height / 2 + 72, 0xFFFFFF);
        }

        super.drawScreen(mx, my, pt);
    }

    private void drawForm(int mx, int my) {
        int labelY = formY - 43;
        switch (selectedType) {
            case T_TAKEOFF:
                drawString(fontRenderer, "Runway-A marker ID:", cx - 120, labelY, 0xAAAAAA);
                idField.drawTextBox();
                drawMarkerHints("RUNWAY_A", mx, my);
                break;
            case T_FLYTO:
                drawString(fontRenderer, "X:", cx - 122, labelY, 0xAAAAAA);
                drawString(fontRenderer, "Y:", cx - 60,  labelY, 0xAAAAAA);
                drawString(fontRenderer, "Z:", cx + 2,   labelY, 0xAAAAAA);
                xField.drawTextBox(); yField.drawTextBox(); zField.drawTextBox();
                drawMarkerHints("WAYPOINT", mx, my);
                break;
            case T_ATTACK:
                drawString(fontRenderer, "Attack radius (blocks):", cx - 120, labelY, 0xAAAAAA);
                numField.drawTextBox();
                break;
            case T_LOITER:
                drawString(fontRenderer, "Duration (ticks, 20=1s):", cx - 120, labelY, 0xAAAAAA);
                numField.drawTextBox();
                break;
            case T_LAND:
                drawString(fontRenderer, "Runway-B marker ID:", cx - 120, labelY, 0xAAAAAA);
                idField.drawTextBox();
                drawMarkerHints("RUNWAY_B", mx, my);
                break;
            case T_PARK:
                drawString(fontRenderer, "Parking marker ID:", cx - 120, labelY, 0xAAAAAA);
                idField.drawTextBox();
                drawMarkerHints("PARKING", mx, my);
                break;
        }
    }

    /** 利用可能なマーカーIDをクリック可能なヒントとして表示する。 */
    private void drawMarkerHints(String type, int mx, int my) {
        int hy = formY - 8;
        List<MarkerDto> list = markersOfType(type);
        if (list.isEmpty()) {
            drawString(fontRenderer, "§8No " + type + " markers placed yet", cx - 120, hy, 0x666666);
            return;
        }
        int rx = cx - 120;
        drawString(fontRenderer, "§8Quick: ", rx, hy, 0x666666);
        rx += fontRenderer.getStringWidth("Quick: ");
        for (MarkerDto m : list) {
            String label = m.id.isEmpty() ? "(no-id)" : m.id;
            int w = fontRenderer.getStringWidth(label);
            boolean hov = mx >= rx && mx < rx + w && my >= hy && my < hy + 9;
            drawString(fontRenderer, hov ? "§e" + label : "§b" + label, rx, hy, 0xFFFFFF);
            rx += w + fontRenderer.getStringWidth(" §8| ") - 2;
            drawString(fontRenderer, "§8| ", rx - fontRenderer.getStringWidth("§8| ") + 2, hy, 0xFFFFFF);
        }
    }

    private void drawNodeList(int mx, int my) {
        int listY = height / 2 + 10;
        drawString(fontRenderer, "§7Nodes (" + nodes.size() + "):", cx - 120, listY - 12, 0xAAAAAA);
        if (nodes.isEmpty()) {
            drawString(fontRenderer, "§8(none yet)", cx - 120, listY, 0x666666);
            return;
        }
        int shown = Math.min(nodes.size(), 5);
        int start = Math.max(0, nodes.size() - shown);
        for (int i = 0; i < shown; i++) {
            int ry = listY + i * 12;
            String node = nodes.get(start + i);
            drawString(fontRenderer, "§7[" + (start + i) + "] §f" + node, cx - 120, ry, 0xFFFFFF);
            // [×] delete button (text-based)
            boolean hov = mx >= cx + 110 && mx < cx + 125 && my >= ry && my < ry + 9;
            drawString(fontRenderer, hov ? "§c[×]" : "§8[×]", cx + 110, ry, 0xFFFFFF);
        }
    }

    // ─── 入力処理 ────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        // ルート名フィールド
        nameField.mouseClicked(mx, my, btn);

        // タイプ別フィールド
        switch (selectedType) {
            case T_TAKEOFF: case T_LAND: case T_PARK:
                idField.mouseClicked(mx, my, btn);
                break;
            case T_FLYTO:
                xField.mouseClicked(mx, my, btn);
                yField.mouseClicked(mx, my, btn);
                zField.mouseClicked(mx, my, btn);
                break;
            case T_ATTACK: case T_LOITER:
                numField.mouseClicked(mx, my, btn);
                break;
        }

        // マーカーヒントのクリック判定
        if (selectedType == T_TAKEOFF) clickIdHint("RUNWAY_A", mx, my);
        if (selectedType == T_LAND)    clickIdHint("RUNWAY_B", mx, my);
        if (selectedType == T_PARK)    clickIdHint("PARKING",  mx, my);
        if (selectedType == T_FLYTO)   clickXyzHint("WAYPOINT", mx, my);

        // ノードの[×]クリック
        int listY = height / 2 + 10;
        int shown = Math.min(nodes.size(), 5);
        int start = Math.max(0, nodes.size() - shown);
        for (int i = 0; i < shown; i++) {
            int ry = listY + i * 12;
            if (mx >= cx + 110 && mx < cx + 125 && my >= ry && my < ry + 9) {
                nodes.remove(start + i);
                return;
            }
        }
    }

    private void clickIdHint(String type, int mx, int my) {
        int hy = formY - 8;
        int rx = cx - 120 + fontRenderer.getStringWidth("Quick: ");
        for (MarkerDto m : markersOfType(type)) {
            String label = m.id.isEmpty() ? "(no-id)" : m.id;
            int w = fontRenderer.getStringWidth(label);
            if (mx >= rx && mx < rx + w && my >= hy && my < hy + 9) {
                idField.setText(label);
                idField.setFocused(true);
                return;
            }
            rx += w + fontRenderer.getStringWidth("| ");
        }
    }

    private void clickXyzHint(String type, int mx, int my) {
        int hy = formY - 8;
        int rx = cx - 120 + fontRenderer.getStringWidth("Quick: ");
        for (MarkerDto m : markersOfType(type)) {
            String label = m.id.isEmpty() ? "(no-id)" : m.id;
            int w = fontRenderer.getStringWidth(label);
            if (mx >= rx && mx < rx + w && my >= hy && my < hy + 9) {
                xField.setText(String.valueOf(m.x));
                yField.setText(String.valueOf(m.y));
                zField.setText(String.valueOf(m.z));
                return;
            }
            rx += w + fontRenderer.getStringWidth("| ");
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { mc.displayGuiScreen(parent); return; } // ESC → 親画面へ
        if (key == 15) { // TAB → フォーカス切り替え
            if (selectedType == T_FLYTO) {
                if (xField.isFocused()) { xField.setFocused(false); yField.setFocused(true); }
                else if (yField.isFocused()) { yField.setFocused(false); zField.setFocused(true); }
                else { zField.setFocused(false); xField.setFocused(true); }
            }
            return;
        }
        nameField.textboxKeyTyped(c, key);
        if (selectedType == T_TAKEOFF || selectedType == T_LAND || selectedType == T_PARK)
            { if (idField.isFocused()) idField.textboxKeyTyped(c, key); }
        if (selectedType == T_FLYTO) {
            if (xField.isFocused()) xField.textboxKeyTyped(c, key);
            if (yField.isFocused()) yField.textboxKeyTyped(c, key);
            if (zField.isFocused()) zField.textboxKeyTyped(c, key);
        }
        if (selectedType == T_ATTACK || selectedType == T_LOITER)
            { if (numField.isFocused()) numField.textboxKeyTyped(c, key); }
    }

    @Override
    public void updateScreen() {
        nameField.updateCursorCounter();
        idField.updateCursorCounter();
        xField.updateCursorCounter(); yField.updateCursorCounter(); zField.updateCursorCounter();
        numField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;

        // タイプ選択ボタン
        if (id >= 10 && id <= 15) {
            int newType = id - 10;
            if (newType != selectedType) {
                selectedType = newType;
                clearForm();
                // マーカーが1件だけなら自動選択
                if (MARKER_TYPE[newType] != null) {
                    List<MarkerDto> list = markersOfType(MARKER_TYPE[newType]);
                    if (list.size() == 1) {
                        if (newType == T_FLYTO) {
                            xField.setText(String.valueOf(list.get(0).x));
                            yField.setText(String.valueOf(list.get(0).y));
                            zField.setText(String.valueOf(list.get(0).z));
                        } else {
                            idField.setText(list.get(0).id);
                        }
                    }
                }
            }
            refreshVisibility();
            return;
        }

        if (id == 20) { // My Pos
            xField.setText(String.valueOf((int) playerX));
            yField.setText(String.valueOf((int) playerY));
            zField.setText(String.valueOf((int) playerZ));
            return;
        }

        if (id == 21) { // Add Node
            String node = buildNode();
            if (node != null) { nodes.add(node); errorMsg = ""; }
            return;
        }

        if (id == 22) { // Save
            String name = nameField.getText().trim();
            if (name.isEmpty())  { errorMsg = "Route name required."; return; }
            if (nodes.isEmpty()) { errorMsg = "Add at least one node."; return; }
            for (RouteDto r : existingRoutes) {
                if (r.name.equals(name)) { errorMsg = "Route '" + name + "' already exists."; return; }
            }
            new PacketRouteAction(PacketRouteAction.CREATE, name, new ArrayList<>(nodes)).sendToServer();
            mc.displayGuiScreen(null);
            return;
        }

        if (id == 23) mc.displayGuiScreen(parent); // Cancel
    }

    // ─── ヘルパー ────────────────────────────────────────────────────────────

    private String buildNode() {
        try {
            switch (selectedType) {
                case T_TAKEOFF: {
                    String v = idField.getText().trim();
                    if (v.isEmpty()) { errorMsg = "Runway-A ID required."; return null; }
                    return "takeoff:" + v;
                }
                case T_FLYTO: {
                    int x = Integer.parseInt(xField.getText().trim());
                    int y = Integer.parseInt(yField.getText().trim());
                    int z = Integer.parseInt(zField.getText().trim());
                    return "flyto:" + x + "," + y + "," + z;
                }
                case T_ATTACK: {
                    String v = numField.getText().trim();
                    int r = v.isEmpty() ? 200 : Integer.parseInt(v);
                    return "attack:" + r;
                }
                case T_LOITER: {
                    String v = numField.getText().trim();
                    int t = v.isEmpty() ? 200 : Integer.parseInt(v);
                    return "loiter:" + t;
                }
                case T_LAND: {
                    String v = idField.getText().trim();
                    if (v.isEmpty()) { errorMsg = "Runway-B ID required."; return null; }
                    return "land:" + v;
                }
                case T_PARK: {
                    String v = idField.getText().trim();
                    if (v.isEmpty()) { errorMsg = "Parking ID required."; return null; }
                    return "park:" + v;
                }
                default: return null;
            }
        } catch (NumberFormatException e) {
            errorMsg = "Invalid number.";
            return null;
        }
    }

    private void clearForm() {
        idField.setText(""); numField.setText("");
        xField.setText(""); yField.setText(""); zField.setText("");
        errorMsg = "";
    }

    private List<MarkerDto> markersOfType(String type) {
        List<MarkerDto> result = new ArrayList<>();
        for (MarkerDto m : markers) {
            if (type.equals(m.type)) result.add(m);
        }
        return result;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
