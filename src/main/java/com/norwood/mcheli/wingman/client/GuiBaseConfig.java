package com.norwood.mcheli.wingman.client;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.networking.packet.PacketBaseAction;
import com.norwood.mcheli.networking.packet.PacketOpenBaseGui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * BASE マーカーブロックを右クリックしたときに開く基地コンフィグGUI。
 *
 * タブ 0: Taxi Routes  — タキシールートの追加・編集・削除
 *   ・駐機場 / 滑走路A / WP はボタン一覧から選択（テキスト入力なし）
 *   ・WPは「ルート内」と「利用可能」の2列で追加/削除
 *
 * タブ 1: Mission  — ミッション策定・機体へのオーダー発令
 */
public class GuiBaseConfig extends GuiScreen {

    // ─── Tab IDs ─────────────────────────────────────────────────────────────
    private static final int BTN_TAB_ROUTES  = 200;
    private static final int BTN_TAB_MISSION = 201;

    // ─── Routes タブ: ルートリスト列 ─────────────────────────────────────────
    private static final int BTN_ROUTE_BASE  = 10;   // 10..17
    private static final int MAX_ROUTE_LIST  = 8;
    private static final int BTN_ROUTE_NEW   = 50;
    private static final int BTN_ROUTE_SAVE  = 51;
    private static final int BTN_ROUTE_DEL   = 52;
    private static final int BTN_CLOSE       = 99;

    // ─── Routes タブ: 選択ボタン群（300 番台で Mission タブボタンと非衝突）──
    // MAX_PICKER = 99 に拡張。各カテゴリに 100 スロット分の ID 空間を確保。
    // Mission タブ (100-201) とは分離されているため衝突しない。
    private static final int BTN_PARKING_BASE   = 300; // 300-398 (max 99)
    private static final int BTN_RUNWAY_BASE    = 400; // 400-403 (max 4)
    private static final int BTN_RUNWAY_B_BASE  = 410; // 410-413 (max 4)
    private static final int BTN_HELIPAD_BASE   = 500; // 500-598 (max 99)
    private static final int BTN_WP_REMOVE_BASE = 600; // 600-698 ルート内WP [×]
    private static final int BTN_WP_ADD_BASE    = 700; // 700-798 利用可能WP [+]
    private static final int BTN_WP_UP_BASE     = 800; // 800-898 ルート内WP [↑]
    private static final int BTN_WP_DOWN_BASE   = 900; // 900-998 ルート内WP [↓]
    private static final int BTN_TOGGLE_WP_MODE    = 1001; // 出発/着陸WP切替ボタン
    private static final int BTN_ARR_WP_REMOVE_BASE = 1100; // 1100-1198 着陸WP [x]
    private static final int BTN_ARR_WP_ADD_BASE    = 1200; // 1200-1298 着陸WP候補 [+]
    private static final int BTN_ARR_WP_UP_BASE     = 1300; // 1300-1398 着陸WP [↑]
    private static final int BTN_ARR_WP_DOWN_BASE   = 1400; // 1400-1498 着陸WP [↓]
    private static final int BTN_ARR_RUNWAY_SEL_BASE = 1500; // 1500-1507 着陸エントリー端点選択
    private static final int MAX_ARR_RUNWAY_SEL  = 8;
    private static final int MAX_PICKER         = 99;  // WP 最大表示数

    // ─── Mission タブ ────────────────────────────────────────────────────────
    private static final int BTN_M_CAP    = 100;
    private static final int BTN_M_CAS    = 101;
    private static final int BTN_M_STRIKE = 102;
    private static final int BTN_M_ESCORT = 103;
    private static final int BTN_M_RECON  = 104;
    private static final int BTN_M_FERRY  = 105;
    private static final int BTN_W_GUN    = 110;
    private static final int BTN_W_AA     = 111;
    private static final int BTN_W_AS     = 112;
    private static final int BTN_W_CAS    = 113;
    private static final int BTN_W_ROCKET = 114;
    private static final int BTN_W_BOMB   = 115;
    private static final int BTN_DISPATCH          = 120;
    private static final int BTN_ORBIT_ATTACK      = 121;
    // BTN_VSTOL 削除: 帰着ルートのヘリパッドから自動判定
    private static final int BTN_AIRCRAFT_BASE     = 130; // 130-137 (max 8機)
    private static final int MAX_AIRCRAFT          = 8;
    private static final int BTN_PARK_DISPATCH_BASE  = 150; // 150-157 (max 8駐機スポット)
    private static final int MAX_PARK_DISPATCH      = 8;
    private static final int BTN_ARRIVAL_ROUTE_BASE = 160; // 160-167 (max 8着陸専用ルート)
    private static final int MAX_ARRIVAL_ROUTE      = 8;
    /** Mission タブのコンテンツ総高さ（最大機体数×駐機数想定）。スクロール計算に使用。 */
    private static final int MISSION_CONTENT_H      = 460;
    /** Routes タブのコンテンツ総高さ。スクロール計算に使用。 */
    private static final int ROUTES_CONTENT_H  = 480;  // 着陸エントリー行 (+18) 込み

    // ─── State ───────────────────────────────────────────────────────────────

    private final BlockPos blockPos;
    private final String   baseId;
    private final PacketOpenBaseGui pkt;

    private int currentTab = 0;

    // Routes タブ — ルートリスト
    private final List<PacketOpenBaseGui.RouteDto> routes;
    private int selectedRouteIdx = -1;

    // Routes タブ — 編集状態（テキスト入力なし）
    private GuiTextField fRouteId;            // Route ID だけはキー入力が必要
    private String editingRouteId    = "";    // initGui() をまたいでRoute IDを保持
    private String selectedParkingId = "";    // ボタン選択
    private String selectedRunwayId  = "";    // ボタン選択
    private String selectedRunwayBId = "";    // ボタン選択
    private final List<String> routeWaypoints        = new ArrayList<>(); // 出発順WPリスト
    private final List<String> routeArrivalWaypoints = new ArrayList<>(); // 着陸順WPリスト（空=出発逆順）
    /** 着陸エントリー端点 ID（空 = selectedRunwayId を使用） */
    private String selectedArrivalRunwayId = "";
    /** true のとき右列WPエディタが着陸WPを操作 */
    private boolean editingArrivalWps = false;

    // Mission タブ
    private final Set<String> selectedMissionTypes = new LinkedHashSet<>();
    private final Set<String> selectedWeapons      = new LinkedHashSet<>();
    private GuiTextField fTargetX, fTargetZ, fOrbitRadius, fCruiseAlt;
    private GuiTextField fStrikePasses, fTimeLimit;
    private GuiTextField focusedField;
    private String selectedAircraftUuid        = "";
    private String selectedRouteForDispatch    = ""; // "" = 空中発進
    private String selectedArrivalRouteId      = ""; // "" = 未設定（フォールバック）
    private boolean orbitAttackMode            = false;
    private int    scrollMission               = 0;
    private int    scrollRoutes         = 0;

    // Mission タブのフィールド値を initGui() をまたいで保持するバッファ
    // initGui() が呼ばれるたびにフィールドが再生成されるため、値をここに退避する
    private String _mTargetX      = "0";
    private String _mTargetZ      = "0";
    private String _mOrbitRadius  = "300";
    private String _mCruiseAlt    = "80";
    private String _mStrikePasses = "2";
    private String _mTimeLimit    = "600";

    // ─── Constructor ─────────────────────────────────────────────────────────

    public GuiBaseConfig(PacketOpenBaseGui pkt) {
        this.pkt      = pkt;
        this.blockPos = new BlockPos(pkt.bx, pkt.by, pkt.bz);
        this.baseId   = pkt.baseId;
        this.routes   = new ArrayList<>(pkt.routes);
        selectedWeapons.add("gun");
        net.minecraftforge.fml.common.FMLLog.log.info(
            "[GuiBaseConfig] opened baseId={} pkt.routes={} routes={}",
            this.baseId, pkt.routes.size(), this.routes.size());
    }

    // ─── Init ────────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        buttonList.add(new GuiButton(BTN_TAB_ROUTES,  width / 2 - 82, height / 4 - 12, 80, 18, "Taxi Routes"));
        buttonList.add(new GuiButton(BTN_TAB_MISSION, width / 2 + 2,  height / 4 - 12, 80, 18, "Mission"));

        if (currentTab == 0) initRoutesTab();
        else                  initMissionTab();

        buttonList.add(new GuiButton(BTN_CLOSE, width / 2 - 25, height - 26, 50, 16, "Close"));
    }

    // =========================================================================
    // Taxi Routes タブ
    // =========================================================================

    private void initRoutesTab() {
        int cx = width / 2;
        int lx = cx - 195;   // 左列: ルートリスト
        int rx = cx - 65;    // 右列: 編集フォーム（幅 ~255px）
        int ty = height / 4 + 12;
        int sty = ty - scrollRoutes; // スクロール適用後の基準Y

        // ── 左列: ルートリストボタン ──────────────────────────────────────
        for (int i = 0; i < routes.size() && i < MAX_ROUTE_LIST; i++) {
            buttonList.add(new GuiButton(BTN_ROUTE_BASE + i,
                    lx, sty + i * 22, 110, 18, routes.get(i).routeId));
        }
        buttonList.add(new GuiButton(BTN_ROUTE_NEW,  lx,      sty + MAX_ROUTE_LIST * 22 + 4, 52, 16, "New"));
        buttonList.add(new GuiButton(BTN_ROUTE_SAVE, lx + 58, sty + MAX_ROUTE_LIST * 22 + 4, 52, 16, "Save"));
        buttonList.add(new GuiButton(BTN_ROUTE_DEL,  lx + 58, sty + MAX_ROUTE_LIST * 22 + 24, 52, 16, "Delete"));

        // ── 右列: Route ID（唯一のテキストフィールド）────────────────────
        // initGui() 再呼び出し前に入力済みテキストを退避（新規作成モードのみ）
        if (fRouteId != null && selectedRouteIdx < 0) {
            editingRouteId = fRouteId.getText();
        }
        fRouteId = new GuiTextField(0, fontRenderer, rx, sty, 200, 16);
        fRouteId.setMaxStringLength(64);
        fRouteId.setFocused(true);
        if (selectedRouteIdx >= 0 && selectedRouteIdx < routes.size()) {
            fRouteId.setText(routes.get(selectedRouteIdx).routeId);
        } else {
            fRouteId.setText(editingRouteId); // 入力中の内容を復元
        }

        // ── 右列: Parking 選択ボタン群 ───────────────────────────────────
        int py = sty + 28;
        int btnW = 58, gap = 4;
        for (int i = 0; i < pkt.parkingMarkers.size() && i < MAX_PICKER; i++) {
            String id = pkt.parkingMarkers.get(i).id;
            buttonList.add(new GuiButton(BTN_PARKING_BASE + i,
                    rx + (i % 4) * (btnW + gap),
                    py + (i / 4) * 20,
                    btnW, 16, id));
        }

        // ── 右列: Runway A 選択ボタン群 ──────────────────────────────────
        int rwy = pkt.runwayAMarkers.isEmpty() ? 0 : (pkt.parkingMarkers.size() > 4 ? 2 : 1);
        int ry = sty + 56 + rwy * 20;
        if (!pkt.runwayAMarkers.isEmpty()) {
            for (int i = 0; i < pkt.runwayAMarkers.size() && i < 4; i++) {
                String id = pkt.runwayAMarkers.get(i).id;
                buttonList.add(new GuiButton(BTN_RUNWAY_BASE + i,
                        rx + i * (btnW + gap), ry, btnW, 16, id));
            }
        }

        // ── 右列: Runway B 選択ボタン群 ──────────────────────────────────
        int rbyY = ry + 24;
        for (int i = 0; i < pkt.runwayBMarkers.size() && i < 4; i++) {
            String id = pkt.runwayBMarkers.get(i).id;
            buttonList.add(new GuiButton(BTN_RUNWAY_B_BASE + i,
                    rx + i * (btnW + gap), rbyY, btnW, 16, id));
        }

        // ── 右列: Helipad 選択ボタン群（VTOL/ヘリ用エンドポイント） ──────
        int hpy = rbyY + 24;
        for (int i = 0; i < pkt.helipads.size() && i < MAX_PICKER; i++) {
            String id = pkt.helipads.get(i).id;
            buttonList.add(new GuiButton(BTN_HELIPAD_BASE + i,
                    rx + (i % 4) * (btnW + gap), hpy, btnW, 16, id));
        }
        int hRows = pkt.helipads.isEmpty() ? 0 : (pkt.helipads.size() > 4 ? 2 : 1);

        // HELIPAD_B セクション + Departure sequence preview 行 + トグルボタン
        // hbSectionH = label(10) + info(12) + 余白(6) + seq行1(10) + seq行2(10) + 余白(4) + toggle(13) + 余白(7)
        int hbSectionH = 72;

        // ── 右列: WP ピッカー（出発 / 着陸 トグル） ──────────────────────
        int wy = hpy + hRows * 20 + 6 + hbSectionH;
        int colW = 100;

        // 出発/着陸 切替ボタン（departure sequence より下に配置してオーバーラップを防ぐ）
        buttonList.add(new GuiButton(BTN_TOGGLE_WP_MODE,
                rx, wy - 16, 90, 13,
                editingArrivalWps ? "▼ 着陸 WP" : "▶ 出発 WP"));

        // 着陸モードのとき: エントリー端点選択ボタン行を先頭に追加
        if (editingArrivalWps) {
            java.util.List<PacketOpenBaseGui.MarkerDto> eps = buildArrivalEndpoints();
            int epW = 52, epGap = 2;
            for (int i = 0; i < eps.size() && i < MAX_ARR_RUNWAY_SEL; i++) {
                buttonList.add(new GuiButton(BTN_ARR_RUNWAY_SEL_BASE + i,
                        rx + colW + 8 + i * (epW + epGap), wy, epW, 14, eps.get(i).id));
            }
        }

        // WP リスト開始 Y（着陸モード時は端点選択行の分だけ下にシフト）
        int arrOffset = editingArrivalWps ? 18 : 0;

        // 左: アクティブWPリスト + [↑][↓][×]
        List<String> activeWps = editingArrivalWps ? routeArrivalWaypoints : routeWaypoints;
        int baseRm = editingArrivalWps ? BTN_ARR_WP_REMOVE_BASE : BTN_WP_REMOVE_BASE;
        int baseUp = editingArrivalWps ? BTN_ARR_WP_UP_BASE     : BTN_WP_UP_BASE;
        int baseDn = editingArrivalWps ? BTN_ARR_WP_DOWN_BASE   : BTN_WP_DOWN_BASE;
        for (int i = 0; i < activeWps.size() && i < MAX_PICKER; i++) {
            int bx = rx + colW - 52;
            if (i > 0)
                buttonList.add(new GuiButton(baseUp + i, bx,      wy + arrOffset + i * 18, 14, 14, "^"));
            if (i < activeWps.size() - 1)
                buttonList.add(new GuiButton(baseDn + i, bx + 16, wy + arrOffset + i * 18, 14, 14, "v"));
            buttonList.add(new GuiButton(baseRm + i,     bx + 34, wy + arrOffset + i * 18, 14, 14, "x"));
        }

        // 右: 利用可能WP（アクティブリスト未追加のもの）
        List<String> available = getAvailableWps(activeWps);
        int baseAdd = editingArrivalWps ? BTN_ARR_WP_ADD_BASE : BTN_WP_ADD_BASE;
        for (int i = 0; i < available.size() && i < MAX_PICKER; i++) {
            buttonList.add(new GuiButton(baseAdd + i,
                    rx + colW + 8, wy + arrOffset + i * 18, colW - 10, 14, available.get(i)));
        }

        // ── クリップ外ボタンを非表示 ──────────────────────────────────────
        int clipTop = ty, clipBot = height - 30;
        for (GuiButton b : buttonList) {
            if (isRoutesButton(b.id)) {
                b.visible = b.y + b.height > clipTop && b.y < clipBot;
            }
        }
    }

    private boolean isRoutesButton(int id) {
        return (id >= BTN_ROUTE_BASE     && id < BTN_ROUTE_BASE     + MAX_ROUTE_LIST)
            || id == BTN_ROUTE_NEW || id == BTN_ROUTE_SAVE || id == BTN_ROUTE_DEL
            || (id >= BTN_PARKING_BASE   && id < BTN_PARKING_BASE   + MAX_PICKER)
            || (id >= BTN_RUNWAY_BASE    && id < BTN_RUNWAY_BASE    + 4)
            || (id >= BTN_RUNWAY_B_BASE  && id < BTN_RUNWAY_B_BASE  + 4)
            || (id >= BTN_HELIPAD_BASE   && id < BTN_HELIPAD_BASE   + MAX_PICKER)
            || (id >= BTN_WP_REMOVE_BASE && id < BTN_WP_REMOVE_BASE + MAX_PICKER)
            || (id >= BTN_WP_ADD_BASE    && id < BTN_WP_ADD_BASE    + MAX_PICKER)
            || (id >= BTN_WP_UP_BASE     && id < BTN_WP_UP_BASE     + MAX_PICKER)
            || (id >= BTN_WP_DOWN_BASE   && id < BTN_WP_DOWN_BASE   + MAX_PICKER)
            || id == BTN_TOGGLE_WP_MODE
            || (id >= BTN_ARR_WP_REMOVE_BASE && id < BTN_ARR_WP_REMOVE_BASE + MAX_PICKER)
            || (id >= BTN_ARR_WP_ADD_BASE    && id < BTN_ARR_WP_ADD_BASE    + MAX_PICKER)
            || (id >= BTN_ARR_WP_UP_BASE     && id < BTN_ARR_WP_UP_BASE     + MAX_PICKER)
            || (id >= BTN_ARR_WP_DOWN_BASE   && id < BTN_ARR_WP_DOWN_BASE   + MAX_PICKER)
            || (id >= BTN_ARR_RUNWAY_SEL_BASE && id < BTN_ARR_RUNWAY_SEL_BASE + MAX_ARR_RUNWAY_SEL);
    }

    /**
     * 経由点候補一覧: WAYPOINT / RUNWAY_A / RUNWAY_B / HELIPAD / PARKING の全マーカーを対象に、
     * activeList に未追加のものを返す。
     * 出発・着陸 WP ともに使用するため、どのマーカー種別でも経由点として選べる。
     */
    /** 着陸エントリー端点候補リスト（Runway A → Runway B → Helipad の順） */
    private java.util.List<PacketOpenBaseGui.MarkerDto> buildArrivalEndpoints() {
        java.util.List<PacketOpenBaseGui.MarkerDto> eps = new ArrayList<>();
        eps.addAll(pkt.runwayAMarkers);
        eps.addAll(pkt.runwayBMarkers);
        eps.addAll(pkt.helipads);
        return eps;
    }

    private List<String> getAvailableWps(List<String> activeList) {
        List<String> result = new ArrayList<>();
        for (PacketOpenBaseGui.MarkerDto m : pkt.waypointMarkers) {
            if (!activeList.contains(m.id)) result.add(m.id);
        }
        for (PacketOpenBaseGui.MarkerDto m : pkt.runwayAMarkers) {
            if (!activeList.contains(m.id)) result.add(m.id);
        }
        for (PacketOpenBaseGui.MarkerDto m : pkt.runwayBMarkers) {
            if (!activeList.contains(m.id)) result.add(m.id);
        }
        for (PacketOpenBaseGui.MarkerDto m : pkt.helipads) {
            if (!activeList.contains(m.id)) result.add(m.id);
        }
        for (PacketOpenBaseGui.MarkerDto m : pkt.parkingMarkers) {
            if (!activeList.contains(m.id)) result.add(m.id);
        }
        return result;
    }

    /** 後方互換用（出発WP専用） */
    private List<String> getAvailableWps() {
        return getAvailableWps(routeWaypoints);
    }

    // ─── Routes タブ描画 ──────────────────────────────────────────────────────

    private void drawRoutesTab(int cx, int ty) {
        // ── GL Scissor: スクロール領域をクリップ ─────────────────────────
        int clipTop = ty, clipBot = height - 30;
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, (height - clipBot) * sf, width * sf, (clipBot - clipTop) * sf);

        int sty = ty - scrollRoutes;
        int lx  = cx - 195;
        int rx  = cx - 65;

        // ルートリスト見出し
        drawString(fontRenderer, "§7─ Routes ─", lx, sty + 2, 0x888888);

        // 選択中ルートのハイライト
        if (selectedRouteIdx >= 0) {
            for (GuiButton b : buttonList) {
                if (b.id == BTN_ROUTE_BASE + selectedRouteIdx) {
                    drawRect(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, 0x44FFAA00);
                    break;
                }
            }
        }

        // フォームラベル
        drawString(fontRenderer, "Route ID:", rx, sty + 2, 0xAAAAAA);
        if (fRouteId != null) fRouteId.drawTextBox();

        // Parking
        int py = sty + 28;
        drawString(fontRenderer, "§7Parking:", rx, py - 10, 0xAAAAAA);
        for (int i = 0; i < pkt.parkingMarkers.size() && i < MAX_PICKER; i++) {
            String id = pkt.parkingMarkers.get(i).id;
            if (id.equals(selectedParkingId)) {
                highlightBtn(BTN_PARKING_BASE + i);
            }
        }

        // Runway A
        int rwy = pkt.runwayAMarkers.isEmpty() ? 0 : (pkt.parkingMarkers.size() > 4 ? 2 : 1);
        int ry = sty + 56 + rwy * 20;
        drawString(fontRenderer, "§7Runway A:", rx, ry - 10, 0xAAAAAA);
        if (pkt.runwayAMarkers.isEmpty()) {
            drawString(fontRenderer, "§c(no RUNWAY_A markers in this base)", rx, ry + 2, 0xFF4444);
        } else {
            for (int i = 0; i < pkt.runwayAMarkers.size() && i < 4; i++) {
                if (pkt.runwayAMarkers.get(i).id.equals(selectedRunwayId)) {
                    highlightBtn(BTN_RUNWAY_BASE + i);
                }
            }
        }

        // Runway B
        int rbyY = ry + 24;
        drawString(fontRenderer, "§7Runway B:", rx, rbyY - 10, 0xAAAAAA);
        if (pkt.runwayBMarkers.isEmpty()) {
            drawString(fontRenderer, "§7(no RUNWAY_B markers)", rx, rbyY + 2, 0x666666);
        } else {
            for (int i = 0; i < pkt.runwayBMarkers.size() && i < 4; i++) {
                if (pkt.runwayBMarkers.get(i).id.equals(selectedRunwayBId)) {
                    highlightBtn(BTN_RUNWAY_B_BASE + i);
                }
            }
        }

        // Helipad (runway endpoint / 純VTOL時はP+R兼用)
        int hpy = rbyY + 24;
        drawString(fontRenderer, "§9Helipad §7(Runway End / P+R):", rx, hpy - 10, 0xAAAAAA);
        if (pkt.helipads.isEmpty()) {
            drawString(fontRenderer, "§7(no HELIPAD markers)", rx, hpy + 2, 0x666666);
        } else {
            for (int i = 0; i < pkt.helipads.size() && i < MAX_PICKER; i++) {
                String hid = pkt.helipads.get(i).id;
                if (hid.equals(selectedParkingId) || hid.equals(selectedRunwayId)) {
                    highlightBtn(BTN_HELIPAD_BASE + i);
                }
            }
        }
        int hRows = pkt.helipads.isEmpty() ? 0 : (pkt.helipads.size() > 4 ? 2 : 1);

        // HELIPAD_B 方向マーカー（表示のみ）
        int hbpy = hpy + hRows * 20 + 6;
        int hbSectionH = 72; // initRoutesTab と同値
        drawString(fontRenderer, "§bHelipad-B §7(facing dir):", rx, hbpy, 0xAAAAAA);
        if (pkt.helipadBMarkers.isEmpty()) {
            drawString(fontRenderer,
                "§c(none — HELIPAD_B マーカーを設置して baseId を HELIPAD に合わせる)",
                rx, hbpy + 12, 0xFF6666);
        } else {
            int col = 0;
            for (PacketOpenBaseGui.MarkerDto m : pkt.helipadBMarkers) {
                drawString(fontRenderer, "§b" + m.id + " §7(" + m.x + "," + m.z + ")",
                        rx + col * 100, hbpy + 12, 0x55FFFF);
                col++;
                if (col >= 3) break; // 横幅の都合で最大3つ
            }
        }

        // WPピッカー見出し
        int colW = 118;
        int wy = hpy + hRows * 20 + 6 + hbSectionH;

        // ── Departure / Arrival sequence preview ───────────────────────────
        // 出発順の全経由点を 1 行で可視化: [Parking] → [WP1] → … → [RunwayA]
        // トグルボタン (wy-16, height 13) より上に収まるよう wy-52/wy-38 に配置。
        {
            StringBuilder seq = new StringBuilder("§7出発: ");
            seq.append(selectedParkingId.isEmpty() ? "§c?" : "§a" + selectedParkingId);
            for (String wp : routeWaypoints) {
                seq.append("§7→§f").append(wp);
            }
            seq.append("§7→");
            seq.append(selectedRunwayId.isEmpty() ? "§c?" : "§b" + selectedRunwayId);
            String seqStr = seq.toString();
            int seqVisLen = fontRenderer.getStringWidth(
                    net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(seqStr));
            int maxWidth  = width - rx - 12;
            if (seqVisLen > maxWidth && routeWaypoints.size() > 2) {
                StringBuilder s2 = new StringBuilder("§7出発: ");
                s2.append(selectedParkingId.isEmpty() ? "§c?" : "§a" + selectedParkingId);
                s2.append("§7→§f").append(routeWaypoints.get(0));
                s2.append("§7→§8…(").append(routeWaypoints.size() - 2).append(")…");
                s2.append("§7→§f").append(routeWaypoints.get(routeWaypoints.size() - 1));
                s2.append("§7→");
                s2.append(selectedRunwayId.isEmpty() ? "§c?" : "§b" + selectedRunwayId);
                seqStr = s2.toString();
            }
            drawString(fontRenderer, seqStr, rx, wy - 52, 0xFFFFFF);
            // 着陸順の注記（arrivalRunwayId が設定されていればそちらを優先）
            String arrEntry = (!selectedArrivalRunwayId.isEmpty()) ? selectedArrivalRunwayId : selectedRunwayId;
            String arrNote;
            if (!routeArrivalWaypoints.isEmpty()) {
                StringBuilder arr = new StringBuilder("§7着陸: §b");
                arr.append(arrEntry.isEmpty() ? "?" : arrEntry);
                for (String wp : routeArrivalWaypoints) { arr.append("§7→§f").append(wp); }
                arr.append("§7→§a").append(selectedParkingId.isEmpty() ? "?" : selectedParkingId);
                arrNote = arr.toString();
            } else {
                arrNote = "§7着陸: 逆順 (" + (arrEntry.isEmpty() ? "?" : "§b" + arrEntry + "§7")
                        + "→…→" + (selectedParkingId.isEmpty() ? "?" : "§a" + selectedParkingId + "§7") + ")";
            }
            drawString(fontRenderer, arrNote, rx, wy - 38, 0x666666);
        }

        // 出発/着陸 WP トグルボタンのハイライト（着陸編集中）
        if (editingArrivalWps) highlightBtn(BTN_TOGGLE_WP_MODE);

        // アクティブWPリストのヘッダーと内容
        List<String> activeWps = editingArrivalWps ? routeArrivalWaypoints : routeWaypoints;
        String wpHeader = editingArrivalWps ? "§b着陸 WP:" : "§f出発 WP:";
        drawString(fontRenderer, wpHeader, rx + 94, wy - 6, 0xAAAAAA);

        // 着陸モード: エントリー端点選択行のラベル＆ハイライト
        if (editingArrivalWps) {
            drawString(fontRenderer, "§bエントリー端点:", rx + colW + 8, wy - 6, 0x55FFFF);
            java.util.List<PacketOpenBaseGui.MarkerDto> eps = buildArrivalEndpoints();
            for (int i = 0; i < eps.size() && i < MAX_ARR_RUNWAY_SEL; i++) {
                if (eps.get(i).id.equals(selectedArrivalRunwayId)) {
                    highlightBtn(BTN_ARR_RUNWAY_SEL_BASE + i);
                    break;
                }
            }
            // WP ラベルは arrOffset 分下に
            drawString(fontRenderer, "§7Available:", rx + colW + 8, wy + 18 - 6, 0xAAAAAA);
        } else {
            drawString(fontRenderer, "§7Available:", rx + colW + 8, wy - 6, 0xAAAAAA);
        }

        // ルート内WPのラベル（ボタンは[×]のみ、名前はここで描く）
        int arrOffset2 = editingArrivalWps ? 18 : 0;
        for (int i = 0; i < activeWps.size() && i < MAX_PICKER; i++) {
            drawString(fontRenderer, "§f" + (i + 1) + ". " + activeWps.get(i),
                    rx, wy + arrOffset2 + i * 18 + 3, 0xFFFFFF);
        }
        if (activeWps.isEmpty()) {
            if (editingArrivalWps) {
                drawString(fontRenderer, "§7(空=出発の逆順)", rx, wy + arrOffset2 + 3, 0x555555);
            } else {
                drawString(fontRenderer, "§7(none)", rx, wy + 3, 0x666666);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // スクロールバー（クリップ外で描く）
        drawRoutesScrollbar(clipTop, clipBot);
    }

    /** Routes タブのスクロールバーを右端に描画する。 */
    private void drawRoutesScrollbar(int top, int bot) {
        int visH  = bot - top;
        int maxSc = Math.max(0, ROUTES_CONTENT_H - visH);
        if (maxSc <= 0) return;
        int bx   = width - 7;
        int barH = Math.max(16, visH * visH / ROUTES_CONTENT_H);
        int barY = top + (int) ((long) scrollRoutes * (visH - barH) / maxSc);
        drawRect(bx, top, bx + 4, bot, 0x44FFFFFF);
        drawRect(bx, barY, bx + 4, barY + barH, 0xCCCCCCCC);
    }

    private void highlightBtn(int btnId) {
        for (GuiButton b : buttonList) {
            if (b.id == btnId) {
                drawRect(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, 0xAAFFAA00);
                break;
            }
        }
    }

    // ─── Routes タブ操作 ──────────────────────────────────────────────────────

    private void actionRoutes(GuiButton btn) {
        // ルートリスト選択
        int rIdx = btn.id - BTN_ROUTE_BASE;
        if (rIdx >= 0 && rIdx < routes.size()) {
            selectedRouteIdx = rIdx;
            loadRoute(routes.get(rIdx));
            return;
        }

        switch (btn.id) {
            case BTN_ROUTE_NEW:
                selectedRouteIdx = -1;
                clearRouteForm();
                initGui();
                return;
            case BTN_ROUTE_SAVE:
                saveRoute();
                return;
            case BTN_ROUTE_DEL:
                deleteRoute();
                return;
        }

        // 出発/着陸WPモード切替
        if (btn.id == BTN_TOGGLE_WP_MODE) {
            editingArrivalWps = !editingArrivalWps;
            initGui();
            return;
        }

        // 着陸WP [^] 上移動
        int arrUpIdx = btn.id - BTN_ARR_WP_UP_BASE;
        if (arrUpIdx > 0 && arrUpIdx < routeArrivalWaypoints.size()) {
            java.util.Collections.swap(routeArrivalWaypoints, arrUpIdx, arrUpIdx - 1);
            initGui(); return;
        }
        // 着陸WP [v] 下移動
        int arrDnIdx = btn.id - BTN_ARR_WP_DOWN_BASE;
        if (arrDnIdx >= 0 && arrDnIdx < routeArrivalWaypoints.size() - 1) {
            java.util.Collections.swap(routeArrivalWaypoints, arrDnIdx, arrDnIdx + 1);
            initGui(); return;
        }
        // 着陸WP [x] 削除
        int arrRmIdx = btn.id - BTN_ARR_WP_REMOVE_BASE;
        if (arrRmIdx >= 0 && arrRmIdx < routeArrivalWaypoints.size()) {
            routeArrivalWaypoints.remove(arrRmIdx);
            initGui(); return;
        }
        // 着陸エントリー端点 選択/解除
        int arrEpIdx = btn.id - BTN_ARR_RUNWAY_SEL_BASE;
        if (arrEpIdx >= 0 && arrEpIdx < MAX_ARR_RUNWAY_SEL) {
            java.util.List<PacketOpenBaseGui.MarkerDto> eps = buildArrivalEndpoints();
            if (arrEpIdx < eps.size()) {
                String epId = eps.get(arrEpIdx).id;
                // 同じボタンを再クリックで解除（トグル）
                selectedArrivalRunwayId = selectedArrivalRunwayId.equals(epId) ? "" : epId;
            }
            return;
        }

        // 着陸WP [+] 追加
        List<String> arrAvailable = getAvailableWps(routeArrivalWaypoints);
        int arrAddIdx = btn.id - BTN_ARR_WP_ADD_BASE;
        if (arrAddIdx >= 0 && arrAddIdx < arrAvailable.size()) {
            routeArrivalWaypoints.add(arrAvailable.get(arrAddIdx));
            initGui(); return;
        }

        // Parking 選択
        int pIdx = btn.id - BTN_PARKING_BASE;
        if (pIdx >= 0 && pIdx < pkt.parkingMarkers.size()) {
            selectedParkingId = pkt.parkingMarkers.get(pIdx).id;
            return;
        }

        // Runway A 選択
        int rwyIdx = btn.id - BTN_RUNWAY_BASE;
        if (rwyIdx >= 0 && rwyIdx < pkt.runwayAMarkers.size()) {
            selectedRunwayId = pkt.runwayAMarkers.get(rwyIdx).id;
            return;
        }

        // Runway B 選択
        // ・RT_A が未選択なら RT_B を主エンドポイント（runwayId）として設定
        //   → CTOL着陸後 B端スタートの帰還専用ルート作成に使用
        // ・RT_A 選択済みなら従来通り runwayBId（離陸終点）に設定
        int rwyBIdx = btn.id - BTN_RUNWAY_B_BASE;
        if (rwyBIdx >= 0 && rwyBIdx < pkt.runwayBMarkers.size()) {
            String id = pkt.runwayBMarkers.get(rwyBIdx).id;
            if (selectedRunwayId.isEmpty()) {
                selectedRunwayId = id; // RT_A未選択 → RT_Bを主エンドポイントに
            } else {
                selectedRunwayBId = id; // RT_A選択済み → 離陸終点として設定
            }
            return;
        }

        // Helipad 選択
        //   selectedRunwayId を更新（ヘリパッドをルート終端に設定）。
        //   VSTOL 帰還ルート: Parking を先に選んでから Helipad を選ぶ → runwayId=HELIPAD のみ更新。
        //   純 VTOL ルート  : Parking 未選択で Helipad を選ぶ → parkingId/runwayId 両方に設定（後方互換）。
        int hpIdx = btn.id - BTN_HELIPAD_BASE;
        if (hpIdx >= 0 && hpIdx < pkt.helipads.size()) {
            String hpId = pkt.helipads.get(hpIdx).id;
            selectedRunwayId = hpId;
            if (selectedParkingId.isEmpty()) {
                selectedParkingId = hpId; // 純 VTOL: P+R 兼用
            }
            return;
        }

        // WP [^] 上移動
        int upIdx = btn.id - BTN_WP_UP_BASE;
        if (upIdx > 0 && upIdx < routeWaypoints.size()) {
            java.util.Collections.swap(routeWaypoints, upIdx, upIdx - 1);
            initGui();
            return;
        }

        // WP [v] 下移動
        int downIdx = btn.id - BTN_WP_DOWN_BASE;
        if (downIdx >= 0 && downIdx < routeWaypoints.size() - 1) {
            java.util.Collections.swap(routeWaypoints, downIdx, downIdx + 1);
            initGui();
            return;
        }

        // WP [x] 削除（ルート内から除去）
        int rmIdx = btn.id - BTN_WP_REMOVE_BASE;
        if (rmIdx >= 0 && rmIdx < routeWaypoints.size()) {
            routeWaypoints.remove(rmIdx);
            initGui();
            return;
        }

        // WP [+] 追加（利用可能リストから）
        List<String> available = getAvailableWps();
        int addIdx = btn.id - BTN_WP_ADD_BASE;
        if (addIdx >= 0 && addIdx < available.size()) {
            routeWaypoints.add(available.get(addIdx));
            initGui();
        }
    }

    private void loadRoute(PacketOpenBaseGui.RouteDto r) {
        editingRouteId         = r.routeId;
        if (fRouteId != null) fRouteId.setText(r.routeId);
        selectedParkingId      = r.parkingId;
        selectedRunwayId       = r.runwayId;
        selectedRunwayBId      = r.runwayBId;
        selectedArrivalRunwayId = r.arrivalRunwayId;
        routeWaypoints.clear();
        routeWaypoints.addAll(r.waypointIds);
        routeArrivalWaypoints.clear();
        routeArrivalWaypoints.addAll(r.arrivalWaypointIds);
        initGui(); // ボタン再構築（ハイライト反映）
    }

    private void clearRouteForm() {
        editingRouteId          = "";
        if (fRouteId != null) fRouteId.setText("");
        selectedParkingId       = "";
        selectedRunwayId        = "";
        selectedRunwayBId       = "";
        selectedArrivalRunwayId = "";
        routeWaypoints.clear();
        routeArrivalWaypoints.clear();
        editingArrivalWps       = false;
    }

    private void saveRoute() {
        String rid = fRouteId != null ? fRouteId.getText().trim() : "";
        if (rid.isEmpty()) {
            mc.player.sendMessage(new net.minecraft.util.text.TextComponentString("§cRoute ID が空です"));
            return;
        }
        if (selectedParkingId.isEmpty()) {
            mc.player.sendMessage(new net.minecraft.util.text.TextComponentString("§cParking を選択してください"));
            return;
        }
        if (selectedRunwayId.isEmpty()) {
            if (isHelipads(selectedParkingId)) {
                // ヘリパッドを駐機場に選択した場合、同一ヘリパッドをエンドポイントに自動補完
                selectedRunwayId = selectedParkingId;
            } else {
                mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "§cEndpoint (Runway A または Helipad) を選択してください"));
                return;
            }
        }

        PacketBaseAction pkt2 = new PacketBaseAction();
        pkt2.action          = PacketBaseAction.SAVE_ROUTE;
        pkt2.routeId         = rid;
        pkt2.baseId          = baseId;
        pkt2.parkingId       = selectedParkingId;
        pkt2.runwayId        = selectedRunwayId;
        pkt2.runwayBId       = selectedRunwayBId;
        pkt2.waypointsCsv        = String.join(",", routeWaypoints);
        pkt2.arrivalWaypointsCsv = String.join(",", routeArrivalWaypoints);
        pkt2.arrivalRunwayId     = selectedArrivalRunwayId;
        pkt2.parkingHeading      = -1; // 駐機方位はマーカーブロック側で設定
        pkt2.sendToServer();

        // ローカルリスト更新
        PacketOpenBaseGui.RouteDto dto = new PacketOpenBaseGui.RouteDto();
        dto.routeId          = rid;
        dto.parkingId   = selectedParkingId;
        dto.runwayId    = selectedRunwayId;
        dto.runwayBId   = selectedRunwayBId;
        dto.waypointIds.addAll(routeWaypoints);
        dto.arrivalWaypointIds.addAll(routeArrivalWaypoints);
        dto.arrivalRunwayId = selectedArrivalRunwayId;
        boolean found = false;
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).routeId.equals(rid)) { routes.set(i, dto); found = true; break; }
        }
        if (!found) { routes.add(dto); selectedRouteIdx = routes.size() - 1; }
        initGui();
    }

    private void deleteRoute() {
        if (selectedRouteIdx < 0 || selectedRouteIdx >= routes.size()) return;
        String rid = routes.get(selectedRouteIdx).routeId;
        PacketBaseAction pkt2 = new PacketBaseAction();
        pkt2.action  = PacketBaseAction.DELETE_ROUTE;
        pkt2.routeId = rid;
        pkt2.baseId  = baseId;
        pkt2.sendToServer();
        routes.remove(selectedRouteIdx);
        selectedRouteIdx = -1;
        clearRouteForm();
        initGui();
    }

    // =========================================================================
    // Mission タブ（変更なし）
    // =========================================================================

    private void initMissionTab() {
        // ── initGui() 再呼び出し時のフィールド値退避 ────────────────────
        // タブ切り替え・スクロールのたびに initGui() が呼ばれてフィールドが
        // 再生成されるため、現在の入力値をバッファ変数に退避して復元する。
        if (fTargetX      != null) _mTargetX      = fTargetX.getText();
        if (fTargetZ      != null) _mTargetZ      = fTargetZ.getText();
        if (fOrbitRadius  != null) _mOrbitRadius  = fOrbitRadius.getText();
        if (fCruiseAlt    != null) _mCruiseAlt    = fCruiseAlt.getText();
        if (fStrikePasses != null) _mStrikePasses = fStrikePasses.getText();
        if (fTimeLimit    != null) _mTimeLimit    = fTimeLimit.getText();

        int cx  = width / 2;
        int ty  = height / 4 + 12;
        int sty = ty - scrollMission; // スクロール適用後の基準Y

        // 3列グリッドを画面中央に正確に配置
        int btnW = 58, gap = 4;
        int gridW = 3 * btnW + 2 * gap; // 182px
        int gx    = cx - gridW / 2;

        // ── Mission type ボタン ─────────────────────────────────────────
        String[] mtypes = {"CAP","CAS","STRIKE","ESCORT","RECON","FERRY"};
        int[]    mids   = {BTN_M_CAP, BTN_M_CAS, BTN_M_STRIKE, BTN_M_ESCORT, BTN_M_RECON, BTN_M_FERRY};
        for (int i = 0; i < mtypes.length; i++) {
            buttonList.add(new GuiButton(mids[i],
                    gx + (i % 3) * (btnW + gap), sty + 10 + (i / 3) * 20, btnW, 16, mtypes[i]));
        }

        // ── Weapon ボタン ───────────────────────────────────────────────
        String[] wnames = {"GUN","AA Msl","AS Msl","CAS","Rocket","Bomb"};
        int[]    wids   = {BTN_W_GUN, BTN_W_AA, BTN_W_AS, BTN_W_CAS, BTN_W_ROCKET, BTN_W_BOMB};
        for (int i = 0; i < wnames.length; i++) {
            buttonList.add(new GuiButton(wids[i],
                    gx + (i % 3) * (btnW + gap), sty + 58 + (i / 3) * 20, btnW, 16, wnames[i]));
        }

        // ── パラメータフィールド（2列）──────────────────────────────────
        int fy  = sty + 112;  // Weapon buttons row1 bottom=sty+94 → label at fy-12=sty+100 でクリア
        int fw  = (gridW - 8) / 2;
        int c1  = gx, c2 = gx + fw + 8;
        fTargetX      = tf(10, c1, fy,      fw, _mTargetX);
        fTargetZ      = tf(11, c2, fy,      fw, _mTargetZ);
        fOrbitRadius  = tf(12, c1, fy + 30, fw, _mOrbitRadius);
        fCruiseAlt    = tf(13, c2, fy + 30, fw, _mCruiseAlt);
        fStrikePasses = tf(14, c1, fy + 60, fw, _mStrikePasses);
        fTimeLimit    = tf(15, c2, fy + 60, fw, _mTimeLimit);

        // ── 機体選択ボタン（2列）──────────────────────────────────────
        int ay   = sty + 204;  // 最終フィールド bottom=sty+188 → ラベル ay-12=sty+192 でクリア
        int abW  = (gridW - gap) / 2;
        List<PacketOpenBaseGui.AircraftDto> acs = pkt.nearbyAircraft;
        for (int i = 0; i < acs.size() && i < MAX_AIRCRAFT; i++) {
            String label = acs.get(i).name;
            if (label.length() > 14) label = label.substring(0, 13) + "…";
            buttonList.add(new GuiButton(BTN_AIRCRAFT_BASE + i,
                    gx + (i % 2) * (abW + gap), ay + (i / 2) * 20, abW, 16, label));
        }

        // ── タキシールート選択ボタン（2列）─────────────────────────────
        int acRows = Math.min((acs.size() + 1) / 2, MAX_AIRCRAFT / 2);
        int pky    = ay + acRows * 20 + 14;
        int pkW    = (gridW - gap) / 2;
        for (int i = 0; i < routes.size() && i < MAX_PARK_DISPATCH; i++) {
            String label = routes.get(i).routeId;
            if (label.length() > 14) label = label.substring(0, 13) + "…";
            buttonList.add(new GuiButton(BTN_PARK_DISPATCH_BASE + i,
                    gx + (i % 2) * (pkW + gap), pky + (i / 2) * 20,
                    pkW, 16, label));
        }

        // ── 着陸専用ルート選択ボタン（2列）──────────────────────────────
        int pkRows = routes.isEmpty() ? 0
                   : Math.min((routes.size() + 1) / 2, MAX_PARK_DISPATCH / 2);
        int ary  = pky + pkRows * 20 + 14;
        int arW  = (gridW - gap) / 2;
        for (int i = 0; i < routes.size() && i < MAX_ARRIVAL_ROUTE; i++) {
            String label = routes.get(i).routeId;
            if (label.length() > 14) label = label.substring(0, 13) + "…";
            buttonList.add(new GuiButton(BTN_ARRIVAL_ROUTE_BASE + i,
                    gx + (i % 2) * (arW + gap), ary + (i / 2) * 20,
                    arW, 16, label));
        }

        // ── Dispatch / Orbit Atk ────────────────────────────────────────
        int arRows = routes.isEmpty() ? 0
                   : Math.min((routes.size() + 1) / 2, MAX_ARRIVAL_ROUTE / 2);
        int dispY = ary + arRows * 20 + 6;
        buttonList.add(new GuiButton(BTN_DISPATCH,
                cx - 40, dispY, 80, 16, "Dispatch"));
        buttonList.add(new GuiButton(BTN_ORBIT_ATTACK,
                cx + 44, dispY, 70, 16, "Orbit Atk"));

        focusedField = fTargetX;

        // ── クリップ外ボタンを非表示 ────────────────────────────────────
        int clipTop = ty, clipBot = height - 30;
        for (GuiButton b : buttonList) {
            if (isMissionButton(b.id)) {
                b.visible = b.y + b.height > clipTop && b.y < clipBot;
            }
        }
    }

    private boolean isMissionButton(int id) {
        return (id >= BTN_M_CAP    && id <= BTN_M_FERRY)
            || (id >= BTN_W_GUN   && id <= BTN_W_BOMB)
            || (id >= BTN_AIRCRAFT_BASE      && id < BTN_AIRCRAFT_BASE      + MAX_AIRCRAFT)
            || (id >= BTN_PARK_DISPATCH_BASE && id < BTN_PARK_DISPATCH_BASE + MAX_PARK_DISPATCH)
            || (id >= BTN_ARRIVAL_ROUTE_BASE && id < BTN_ARRIVAL_ROUTE_BASE + MAX_ARRIVAL_ROUTE)
            || id == BTN_DISPATCH
            || id == BTN_ORBIT_ATTACK;
    }

    // ─── Mission タブ描画 ─────────────────────────────────────────────────────

    private void drawMissionTab(int cx, int ty) {
        // ── GL Scissor: スクロール領域をクリップ ─────────────────────────
        int clipTop = ty, clipBot = height - 30;
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, (height - clipBot) * sf, width * sf, (clipBot - clipTop) * sf);

        int sty  = ty - scrollMission;
        int btnW = 58, gap = 4;
        int gridW = 3 * btnW + 2 * gap;
        int gx    = cx - gridW / 2;

        // セクションラベル
        drawString(fontRenderer, "Mission Types:", gx, sty,      0xAAAAAA);
        drawString(fontRenderer, "Weapons:",       gx, sty + 48, 0xAAAAAA);

        // フィールドラベル（フィールドの上に表示）
        int fy = sty + 112;  // initMissionTab と合わせる
        int fw = (gridW - 8) / 2;
        int c1 = gx, c2 = gx + fw + 8;
        drawString(fontRenderer, "Target X:", c1, fy - 12, 0xAAAAAA);
        drawString(fontRenderer, "Z:",        c2, fy - 12, 0xAAAAAA);
        drawString(fontRenderer, "Orbit R:",  c1, fy + 18, 0xAAAAAA);
        drawString(fontRenderer, "Cruise Y:", c2, fy + 18, 0xAAAAAA);
        drawString(fontRenderer, "Passes:",   c1, fy + 48, 0xAAAAAA);
        drawString(fontRenderer, "Limit(s):", c2, fy + 48, 0xAAAAAA);

        // 機体選択ラベル
        int ay = sty + 204;  // initMissionTab と合わせる
        drawString(fontRenderer, "Select Aircraft:", gx, ay - 12, 0xAAAAAA);
        if (pkt.nearbyAircraft.isEmpty()) {
            drawString(fontRenderer, "§7(no aircraft nearby)", gx, ay + 3, 0x666666);
        }

        // 選択中機体のハイライト
        for (int i = 0; i < pkt.nearbyAircraft.size() && i < MAX_AIRCRAFT; i++) {
            if (pkt.nearbyAircraft.get(i).uuid.equals(selectedAircraftUuid)) {
                highlightBtn(BTN_AIRCRAFT_BASE + i);
            }
        }

        // タキシールート選択ラベル
        int acRowsDraw = Math.min((pkt.nearbyAircraft.size() + 1) / 2, MAX_AIRCRAFT / 2);
        int pkyDraw    = ay + acRowsDraw * 20 + 14;
        drawString(fontRenderer, "§f出発ルート §7(Departure, optional):", gx, pkyDraw - 12, 0xAAAAAA);
        if (routes.isEmpty()) {
            drawString(fontRenderer, "§7(none — Routes tab で作成)", gx, pkyDraw + 3, 0x666666);
        }
        for (int i = 0; i < routes.size() && i < MAX_PARK_DISPATCH; i++) {
            if (routes.get(i).routeId.equals(selectedRouteForDispatch)) {
                highlightBtn(BTN_PARK_DISPATCH_BASE + i);
            }
        }

        // 着陸専用ルート選択ラベル
        int pkRowsDraw = routes.isEmpty() ? 0 : Math.min((routes.size() + 1) / 2, MAX_PARK_DISPATCH / 2);
        int aryDraw    = pkyDraw + pkRowsDraw * 20 + 14;
        drawString(fontRenderer, "§f帰着ルート §7(Arrival, optional):", gx, aryDraw - 12, 0xAAAAAA);
        if (routes.isEmpty()) {
            drawString(fontRenderer, "§7(none)", gx, aryDraw + 3, 0x666666);
        }
        for (int i = 0; i < routes.size() && i < MAX_ARRIVAL_ROUTE; i++) {
            if (routes.get(i).routeId.equals(selectedArrivalRouteId)) {
                highlightBtn(BTN_ARRIVAL_ROUTE_BASE + i);
            }
        }
        if (selectedArrivalRouteId.isEmpty()) {
            drawString(fontRenderer, "§7← §f(fallback)", gx + gridW + 4, aryDraw + 3, 0x888888);
        }

        highlightToggle(BTN_M_CAP,    selectedMissionTypes.contains("CAP"));
        highlightToggle(BTN_M_CAS,    selectedMissionTypes.contains("CAS"));
        highlightToggle(BTN_M_STRIKE, selectedMissionTypes.contains("STRIKE"));
        highlightToggle(BTN_M_ESCORT, selectedMissionTypes.contains("ESCORT"));
        highlightToggle(BTN_M_RECON,  selectedMissionTypes.contains("RECON"));
        highlightToggle(BTN_M_FERRY,  selectedMissionTypes.contains("FERRY"));
        highlightToggle(BTN_W_GUN,    selectedWeapons.contains("gun"));
        highlightToggle(BTN_W_AA,     selectedWeapons.contains("aamissile"));
        highlightToggle(BTN_W_AS,     selectedWeapons.contains("asmissile"));
        highlightToggle(BTN_W_CAS,    selectedWeapons.contains("cas"));
        highlightToggle(BTN_W_ROCKET, selectedWeapons.contains("rocket"));
        highlightToggle(BTN_W_BOMB,      selectedWeapons.contains("bomb"));
        highlightToggle(BTN_ORBIT_ATTACK, orbitAttackMode);
        // VSTOL 自動判定: 帰着ルートのエンドポイントがヘリパッドなら VTOL 着陸を示す
        boolean isVtolArrival = isVtolArrivalRoute(selectedArrivalRouteId);
        if (isVtolArrival && !selectedArrivalRouteId.isEmpty()) {
            drawString(fontRenderer, "§b⬆ VTOL着陸 §7(帰着ルートから自動)", gx, height - 30, 0x55FFFF);
        }

        if (fTargetX      != null) fTargetX.drawTextBox();
        if (fTargetZ      != null) fTargetZ.drawTextBox();
        if (fOrbitRadius  != null) fOrbitRadius.drawTextBox();
        if (fCruiseAlt    != null) fCruiseAlt.drawTextBox();
        if (fStrikePasses != null) fStrikePasses.drawTextBox();
        if (fTimeLimit    != null) fTimeLimit.drawTextBox();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // スクロールバー描画（クリップ外で描く）
        drawMissionScrollbar(clipTop, clipBot);
    }

    /** Mission タブのスクロールバーを右端に描画する。 */
    private void drawMissionScrollbar(int top, int bot) {
        int visH  = bot - top;
        int maxSc = Math.max(0, MISSION_CONTENT_H - visH);
        if (maxSc <= 0) return;
        int bx   = width - 7;
        int barH = Math.max(16, visH * visH / MISSION_CONTENT_H);
        int barY = top + (int) ((long) scrollMission * (visH - barH) / maxSc);
        drawRect(bx, top, bx + 4, bot, 0x44FFFFFF);
        drawRect(bx, barY, bx + 4, barY + barH, 0xCCCCCCCC);
    }

    // ─── Mission タブ操作 ─────────────────────────────────────────────────────

    private void actionMission(GuiButton btn) {
        // 機体選択
        int aIdx = btn.id - BTN_AIRCRAFT_BASE;
        if (aIdx >= 0 && aIdx < pkt.nearbyAircraft.size()) {
            selectedAircraftUuid = pkt.nearbyAircraft.get(aIdx).uuid;
            return;
        }

        // タキシールート選択（再クリックで解除）
        int pkIdx = btn.id - BTN_PARK_DISPATCH_BASE;
        if (pkIdx >= 0 && pkIdx < routes.size()) {
            String rid = routes.get(pkIdx).routeId;
            selectedRouteForDispatch = selectedRouteForDispatch.equals(rid) ? "" : rid;
            return;
        }

        // 着陸専用ルート選択（再クリックで解除）
        int arIdx = btn.id - BTN_ARRIVAL_ROUTE_BASE;
        if (arIdx >= 0 && arIdx < routes.size()) {
            String rid = routes.get(arIdx).routeId;
            selectedArrivalRouteId = selectedArrivalRouteId.equals(rid) ? "" : rid;
            return;
        }

        switch (btn.id) {
            case BTN_M_CAP:    toggleMission("CAP",    false); return;
            case BTN_M_CAS:    toggleMission("CAS",    false); return;
            case BTN_M_STRIKE: toggleMission("STRIKE", true);  return;
            case BTN_M_ESCORT: toggleMission("ESCORT", false); return;
            case BTN_M_RECON:  toggleMission("RECON",  false); return;
            case BTN_M_FERRY:  toggleMission("FERRY",  true);  return;
            case BTN_W_GUN:    toggleWeapon("gun");       return;
            case BTN_W_AA:     toggleWeapon("aamissile"); return;
            case BTN_W_AS:     toggleWeapon("asmissile"); return;
            case BTN_W_CAS:    toggleWeapon("cas");       return;
            case BTN_W_ROCKET: toggleWeapon("rocket");    return;
            case BTN_W_BOMB:      toggleWeapon("bomb");                   return;
            case BTN_ORBIT_ATTACK: orbitAttackMode = !orbitAttackMode;  return;
            case BTN_DISPATCH:    dispatchOrder();                       return;
        }
    }

    private void toggleMission(String type, boolean exclusive) {
        if (exclusive) {
            if (selectedMissionTypes.contains(type)) selectedMissionTypes.remove(type);
            else { selectedMissionTypes.clear(); selectedMissionTypes.add(type); }
        } else {
            boolean hadExclusive = selectedMissionTypes.contains("STRIKE")
                                || selectedMissionTypes.contains("FERRY");
            if (hadExclusive) selectedMissionTypes.clear();
            if (selectedMissionTypes.contains(type)) selectedMissionTypes.remove(type);
            else selectedMissionTypes.add(type);
        }
    }

    private void toggleWeapon(String weapon) {
        if (selectedWeapons.contains(weapon)) selectedWeapons.remove(weapon);
        else selectedWeapons.add(weapon);
    }

    private void dispatchOrder() {
        String uuid = selectedAircraftUuid;
        if (uuid.isEmpty() || selectedMissionTypes.isEmpty()) return;
        PacketBaseAction pkt2 = new PacketBaseAction();
        pkt2.action           = PacketBaseAction.DISPATCH_ORDER;
        pkt2.baseId           = baseId;
        pkt2.wingmanUuid      = uuid;
        pkt2.routeId          = selectedRouteForDispatch;   // "" = 空中発進
        pkt2.missionTypesCsv  = String.join(",", selectedMissionTypes);
        pkt2.weaponsCsv       = String.join(",", selectedWeapons);
        pkt2.targetX          = parseDouble(fTargetX,      0);
        pkt2.targetZ          = parseDouble(fTargetZ,      0);
        pkt2.orbitRadius      = parseDouble(fOrbitRadius,  300);
        pkt2.cruiseAlt        = parseDouble(fCruiseAlt,    80);
        pkt2.strikePasses     = parseInt   (fStrikePasses, 2);
        pkt2.timeLimitSeconds = parseInt   (fTimeLimit,    600);
        pkt2.ferryDestBase    = "";
        pkt2.orbitAttack      = orbitAttackMode;
        // VTOL着陸: 帰着ルートのエンドポイントがヘリパッドなら自動的に useVstol=true
        pkt2.useVstol         = isVtolArrivalRoute(selectedArrivalRouteId);
        pkt2.arrivalRouteId   = selectedArrivalRouteId;
        pkt2.sendToServer();
    }

    // =========================================================================
    // Update / Draw / Input
    // =========================================================================

    @Override
    public void updateScreen() {
        if (fRouteId      != null) fRouteId.updateCursorCounter();
        if (fTargetX      != null) fTargetX.updateCursorCounter();
        if (fTargetZ      != null) fTargetZ.updateCursorCounter();
        if (fOrbitRadius  != null) fOrbitRadius.updateCursorCounter();
        if (fCruiseAlt    != null) fCruiseAlt.updateCursorCounter();
        if (fStrikePasses != null) fStrikePasses.updateCursorCounter();
        if (fTimeLimit    != null) fTimeLimit.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        int cx = width / 2;
        int ty = height / 4;

        drawCenteredString(fontRenderer, "§eBase: §f" + baseId, cx, ty - 26, 0xFFFFFF);
        highlightTab(BTN_TAB_ROUTES,  currentTab == 0);
        highlightTab(BTN_TAB_MISSION, currentTab == 1);

        if (currentTab == 0) drawRoutesTab(cx, ty + 12);
        else                  drawMissionTab(cx, ty + 12);

        super.drawScreen(mx, my, pt);
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

    private void highlightToggle(int btnId, boolean on) {
        if (!on) return;
        for (GuiButton b : buttonList) {
            if (b.id == btnId) {
                drawRect(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, 0xFFFFAA00);
                break;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        switch (btn.id) {
            case BTN_TAB_ROUTES:
                if (currentTab != 0) { currentTab = 0; scrollRoutes = 0; scrollMission = 0; initGui(); } return;
            case BTN_TAB_MISSION:
                if (currentTab != 1) { currentTab = 1; scrollRoutes = 0; scrollMission = 0; initGui(); } return;
            case BTN_CLOSE:
                mc.player.closeScreen(); return;
        }
        if (currentTab == 0) actionRoutes(btn);
        else                  actionMission(btn);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.player.closeScreen(); return; }
        if (key == Keyboard.KEY_TAB) {
            cycleFocus(); return;
        }
        if (focusedField != null && currentTab == 1) focusedField.textboxKeyTyped(c, key);
        if (fRouteId != null && fRouteId.isFocused() && currentTab == 0) fRouteId.textboxKeyTyped(c, key);
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) throws IOException {
        super.mouseClicked(mx, my, mb);
        if (fRouteId != null) { fRouteId.mouseClicked(mx, my, mb); }
        GuiTextField[] fields = missionFields();
        GuiTextField clicked = null;
        for (GuiTextField f : fields) {
            if (f != null) { f.mouseClicked(mx, my, mb); if (f.isFocused()) clicked = f; }
        }
        if (clicked != null) {
            for (GuiTextField f : fields) { if (f != null && f != clicked) f.setFocused(false); }
            focusedField = clicked;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int step = wheel > 0 ? -14 : 14;
        int visH = (height - 30) - (height / 4 + 12);
        if (currentTab == 0) {
            int maxSc = Math.max(0, ROUTES_CONTENT_H - visH);
            scrollRoutes  = Math.max(0, Math.min(scrollRoutes  + step, maxSc));
            initGui();
        } else if (currentTab == 1) {
            int maxSc = Math.max(0, MISSION_CONTENT_H - visH);
            scrollMission = Math.max(0, Math.min(scrollMission + step, maxSc));
            initGui();
        }
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private GuiTextField tf(int id, int x, int y, int w, String placeholder) {
        GuiTextField f = new GuiTextField(id, fontRenderer, x, y, w, 16);
        f.setMaxStringLength(128);
        if (f.getText().isEmpty()) f.setText(placeholder);
        return f;
    }

    private GuiTextField[] missionFields() {
        return new GuiTextField[]{ fTargetX, fTargetZ, fOrbitRadius, fCruiseAlt,
                                   fStrikePasses, fTimeLimit };
    }

    private void cycleFocus() {
        if (currentTab == 0) return; // Routes タブはフォーカス循環不要
        GuiTextField[] fields = missionFields();
        List<GuiTextField> active = new ArrayList<>();
        for (GuiTextField f : fields) { if (f != null) active.add(f); }
        if (active.isEmpty()) return;
        int cur  = active.indexOf(focusedField);
        int next = (cur + 1) % active.size();
        for (GuiTextField f : active) f.setFocused(false);
        active.get(next).setFocused(true);
        focusedField = active.get(next);
    }

    /** 指定IDがヘリパッドマーカー一覧に含まれるか判定する。 */
    private boolean isHelipads(String id) {
        for (PacketOpenBaseGui.MarkerDto m : pkt.helipads) {
            if (m.id.equals(id)) return true;
        }
        return false;
    }

    /**
     * 指定した帰着ルートのエンドポイント（runwayId）がヘリパッドであれば true。
     * VTOL着陸の自動判定に使用。空ルートIDなら false。
     */
    private boolean isVtolArrivalRoute(String routeId) {
        if (routeId.isEmpty()) return false;
        for (PacketOpenBaseGui.RouteDto r : routes) {
            if (r.routeId.equals(routeId)) {
                return isHelipads(r.runwayId);
            }
        }
        return false;
    }

    private double parseDouble(GuiTextField f, double def) {
        if (f == null) return def;
        try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return def; }
    }

    private int parseInt(GuiTextField f, int def) {
        if (f == null) return def;
        try { return Integer.parseInt(f.getText().trim()); } catch (Exception e) { return def; }
    }
}
