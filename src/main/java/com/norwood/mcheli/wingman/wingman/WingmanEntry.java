package com.norwood.mcheli.wingman.wingman;

import com.norwood.mcheli.wingman.mission.AutonomousState;
import com.norwood.mcheli.wingman.mission.MissionNode;
import com.norwood.mcheli.wingman.mission.MissionOrder;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WingmanEntry {

    // Attack mode constants
    public static final int ATK_NONE   = 0;
    public static final int ATK_MANUAL = 1;
    public static final int ATK_AUTO   = 2;

    public WingmanState state;
    public Entity leader;
    public int formationSlot;

    public int attackMode = ATK_NONE;
    public UUID manualTargetId = null;
    public UUID currentAutoTarget = null;
    public int weaponSeat = 0;
    /** McHeli武器種フィルタ (null = 全種試す) */
    public String weaponType = null;

    // ─── 旧MissionNode系 自律飛行 ────────────────────────────────────────────
    public AutonomousState autoState   = AutonomousState.NONE;
    public List<MissionNode> mission   = null;
    public int missionIndex            = 0;
    public int missionNodeTimer        = 0;
    public double autoTargetX = 0, autoTargetY = 0, autoTargetZ = 0;

    // ─── 新MissionOrder系 自律飛行 ───────────────────────────────────────────

    /** 発令されたミッション（nullなら旧系またはアイドル） */
    public MissionOrder order = null;

    /** オンステーション経過tick */
    public int orderTimer = 0;

    /** 旋回角度（CAP/CAS orbit用、ラジアン） */
    public double orbitAngle = 0.0;

    /** ストライク残りパス数 */
    public int strikePassesRemaining = 0;

    /** 現在割り当てられた駐機スポットID（TAXI_OUT/IN で使用） */
    public String assignedParkingId = "";

    /**
     * 発令時に指定された出発タキシールートID。
     * initOrder / getOrderRunway で findById を使うことで、
     * 同じ parkingId を持つ複数ルートがある場合に正しい出発ルートを取得する。
     */
    public String departureRouteId = "";

    /** タキシーWPキュー（TAXI_OUT/IN フェーズで順に消化） */
    public List<String> taxiWpQueue = new ArrayList<>();

    /** タキシーWPキューの現在インデックス */
    public int taxiWpIndex = 0;

    /** 偵察フェーズで検知したMob数 */
    public int reconMobCount = 0;

    /** RTBトリガー理由（ログ用） */
    public String rtbReason = "";

    /** 診断ログ用 汎用Tickカウンタ（ハートビート間隔計算に使用） */
    public int diagTick = 0;

    /**
     * VTOL機のホバーモードフラグ。
     * true  → WingmanTickHandler が VTOL モード ON（ホバリング：離陸上昇・垂直降下）
     * false → VTOL モード OFF（固定翼飛行：巡航・接近）
     * AutonomousFlightHandler が各フェーズで設定する。
     */
    public boolean vtolHoverMode = false;

    /**
     * McHeli が返す現在の VTOL ノズル状態（WTH が毎 tick 更新）。
     * 診断ログ用。実際の制御判断には vtolOnSent / missionNodeTimer を使う。
     *   0 = ノズル格納完了（固定翼モード）
     *   1 = ノズル回転中（移行中）
     *   2 = ノズル 90° 展開完了（McHeli バージョンによっては 0/1 のみ）
     */
    public int vtolNozzleMode = 0;

    /**
     * VTOL ON/OFF コマンド送信済みフラグ。
     * true  → 今サイクルで swithVtolMode(ON) または swithVtolMode(OFF) を送信済み。
     *         再送を防ぐ（no-arg トグルを毎 tick 呼ぶと ON/OFF が交互して機能しない）。
     * false → 未送信（次 tick に WTH が送信する）。
     * vtolHoverMode が変化するたびに AFH が false にリセットする。
     */
    public boolean vtolOnSent = false;

    /** タキシング時のPhase.START直前XZ位置（McHeli物理適用前の参照点）。taxiPushPosition が使用。 */
    public double taxiPreX = 0, taxiPreZ = 0;

    /**
     * LANDING 地上ロール中の現在速度 (blocks/tick)。
     * CIRCUIT_FINAL→LANDING 遷移時にタッチダウン速度で初期化し、
     * taxiPushPosition が毎 tick 指数減衰させて RT_B まで徐々に減速する。
     * 0 = 未初期化（通常 TAXI_SPEED を使用）。
     */
    public double landingRollSpeed = 0;

    /**
     * PARKED 時に強制する機首方位（Minecraft yaw 値, 度数法）。
     * Float.NaN = 設定なし（方位強制しない）。
     * 駐機完了時 (TAXI_IN→PARKED) に TaxiRoute.parkingHeading から変換してセットされる。
     */
    public float parkingHeadingYaw = Float.NaN;

    /**
     * TAKEOFF_ROLL グランドロール中の地上拘束Y座標。
     * ALIGN→TAKEOFF_ROLL 遷移時に wingman.posY を記録し、Phase.END で
     * McHeli が揚力で機体を浮かせないよう posY をこの値に制限する。
     * ローテーションゾーン（RT_B 手前 ROTATION_DIST）に入ったら拘束を解除する。
     * 0 = 未設定（ALIGN 前または TAKEOFF_ROLL 以外）。
     */
    public double groundRollY = 0;

    // ─── 判定メソッド ─────────────────────────────────────────────────────────

    public boolean isAutonomous() {
        return (autoState != AutonomousState.NONE && mission != null) || order != null;
    }

    /** MissionOrder系で動作中か */
    public boolean hasOrder() {
        return order != null;
    }

    public MissionNode currentNode() {
        if (mission == null || missionIndex >= mission.size()) return null;
        return mission.get(missionIndex);
    }

    public void advanceMission() {
        missionIndex++;
        missionNodeTimer = 0;
    }

    // ─── コンストラクタ ───────────────────────────────────────────────────────

    public WingmanEntry(Entity leader, int slot) {
        this.state         = WingmanState.FOLLOWING;
        this.leader        = leader;
        this.formationSlot = slot;
    }

    /** 自律モード専用エントリ */
    public WingmanEntry() {
        this.state         = WingmanState.FOLLOWING;
        this.leader        = null;
        this.formationSlot = 0;
    }
}
