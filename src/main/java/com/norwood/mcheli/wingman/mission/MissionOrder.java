package com.norwood.mcheli.wingman.mission;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import java.util.HashSet;
import java.util.Set;

/**
 * ミッション発令データ。GUIから生成されサーバーに送信される。
 * WingmanEntry に格納され AutonomousFlightHandler が参照する。
 */
public class MissionOrder {

    /** 選択された任務種別（排他チェック済み） */
    public Set<MissionType> missionTypes = new HashSet<>();

    /** 選択された武装セット */
    public Set<String> weapons = new HashSet<>();

    /** 目標エリア中心座標（XZ） */
    public double targetX = 0;
    public double targetZ = 0;

    /** 任務時間制限（秒、0=無制限）。デフォルト600秒（10分）。 */
    public int timeLimitSeconds = 600;

    /** 出発・帰投基地ID */
    public String baseId = "";

    /** 旋回半径（CAP/CAS用、blocks） */
    public double orbitRadius = 300.0;

    /** 巡航高度（Y） */
    public double cruiseAlt = 80.0;

    /** ストライクパス回数 */
    public int strikePasses = 2;

    /** フェリー目的地基地ID（FERRY専用） */
    public String ferryDestBase = "";

    /**
     * 軌道旋回しながら側方射撃するモード（AC-130 等）。
     * true の場合、武器種が gun/rocket など機首固定でも軌道を維持したまま射撃する。
     * false（デフォルト）はスタンドオフアプローチ（通常の攻撃パス）。
     */
    public boolean orbitAttack = false;

    /**
     * VSTOL 離陸モード。
     * true  : TAKEOFF_ROLL 中にノズルを 45° に固定し、VTOL 機が短距離離陸できるようにする。
     * false : 通常の CTOL 離陸（ノズル 0°）。VTOL 機も通常の固定翼として離陸する。
     */
    public boolean useVstol = false;

    /**
     * 着陸専用タキシールート ID（オプション）。
     * 設定されている場合、finishLanding() で出発ルートの逆順ではなくこのルートを使用する。
     * 空文字列の場合は通常のフォールバック検索（departureRouteId の逆順など）を使用。
     */
    public String arrivalRouteId = "";

    public MissionOrder() {}

    /** 時間制限（tick換算）。timeLimitSeconds * 20 ticks。 */
    public int timeLimitTicks() {
        return timeLimitSeconds * 20;
    }

    public boolean hasType(MissionType type) {
        return missionTypes.contains(type);
    }

    public boolean isExclusive() {
        for (MissionType t : missionTypes) {
            if (t.isExclusive()) return true;
        }
        return false;
    }
}
