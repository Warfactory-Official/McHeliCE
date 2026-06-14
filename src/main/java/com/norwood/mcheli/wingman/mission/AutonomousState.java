package com.norwood.mcheli.wingman.mission;

/** 自律飛行の状態機械ステート。 */
public enum AutonomousState {
    NONE,             // 自律モードでない（通常follow/idle）
    TAXI_OUT,         // 駐機場 → 滑走路端 地上滑走中
    ALIGN,            // 離陸前 機首整列待ち（滑走路方向に揃うまで）
    TAKEOFF_ROLL,     // 滑走路上 加速中
    CLIMB,            // 上昇中（目標高度まで）
    ENROUTE,          // 巡航中（FLY_TOノード実行中）
    ATTACK,           // ATTACKノード実行中
    LOITER,           // LOITERノード実行中
    DESCEND,          // サーキット入口へ降下
    CIRCUIT_DOWNWIND, // ダウンウィンドレグ（滑走路と平行、逆方向）
    CIRCUIT_BASE,     // ベースレグ（ファイナル軸へ向かう）
    CIRCUIT_FINAL,    // ファイナルアプローチ（グライドスロープ降下）
    LANDING,          // 着陸滑走中（スロットルゼロ・停止待ち）
    TAXI_IN,          // 滑走路 → 駐機場 地上滑走中
    PARKED,           // 駐機中

    // ─── MissionOrder 系 ────────────────────────────────────────────────────
    TRANSIT_TO,       // 任務エリアへ直行中
    ON_STATION,       // オンステーション（CAP/CAS 旋回・護衛・偵察）
    STRIKE_PASS,      // ストライクパス実行中
    RTB,              // 帰投中（任務エリア → 帰投基地）

    // ─── VTOL 系 ────────────────────────────────────────────────────────────
    VTOL_TAKEOFF,     // ヘリ・VTOL機のヘリパッドからの垂直離陸（cruiseAlt まで上昇）
    VTOL_LAND         // ヘリ・VTOL機のヘリパッドへの垂直着陸（降下→接地）
}
