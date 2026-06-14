package com.norwood.mcheli.wingman.handler;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.block.MarkerType;
import com.norwood.mcheli.wingman.mission.AutonomousState;
import com.norwood.mcheli.wingman.mission.MissionNode;
import com.norwood.mcheli.wingman.mission.MissionOrder;
import com.norwood.mcheli.wingman.mission.MissionType;
import com.norwood.mcheli.wingman.mission.TaxiRoute;
import com.norwood.mcheli.wingman.registry.MarkerRegistry;
import com.norwood.mcheli.wingman.registry.TaxiRouteRegistry;
import com.norwood.mcheli.wingman.util.McheliReflect;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 自律飛行の状態機械。WingmanTickHandler と役割分担:
 *   WingmanTickHandler  — スロットル・ヨー・ピッチ・武器（毎tick低レベル制御）
 *   AutonomousFlightHandler — ミッションノードの進行管理（高レベル指示）
 *
 * このハンドラはミッションの「何をするか」を決定し、
 * WingmanEntry の leader / attackMode / formationSlot などを書き換えることで
 * WingmanTickHandler に実際の飛行制御を委譲する。
 */
public class AutonomousFlightHandler {

    // 各フェーズの到達判定距離
    private static final double ARRIVE_DIST        = 30.0;  // FLY_TO 到達判定（XZ水平距離）
    private static final double TAXI_DIST          = 30.0;  // 地上滑走 到達判定（高速固定翼でも検出）
    private static final double PARK_DIST          = 8.0;   // 駐機到達判定（小さくして正確に停止）
    private static final double CRUISE_ALT         = 80.0;  // デフォルト巡航高度（Y座標）
    private static final double TAKEOFF_SPEED      = 1.5;   // 離陸判定速度 (blocks/tick)
    private static final double LANDING_SPEED      = 0.1;   // 着陸完了判定速度

    // 離陸整列・中心線追従
    private static final double ALIGN_TOLERANCE    = 5.0;   // 機首整列許容角度 (°) — 小さくしてロール中のヨードリフトを防ぐ
    private static final double LOOKAHEAD_DIST     = 40.0;  // 滑走路中心線先読み距離 (blocks)
    private static final double ROTATION_DIST      = 50.0;  // ローテーション開始距離（RT_B 手前何 blocks でピッチアップ開始）

    // 燃料
    private static final double BINGO_FUEL_RATIO   = 0.20;  // 残燃料がこの比率以下でRTBトリガー

    // 着陸サーキット
    private static final double CIRCUIT_OFFSET     = 80.0;  // 滑走路中心からの横距離 (blocks)
    private static final double CIRCUIT_FINAL_DIST = 250.0; // ファイナル進入開始距離 — 大きすぎるとチャンク外逸脱
    private static final double TOUCHDOWN_DIST     = 20.0;  // 接地判定距離

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.START) return;
        WorldServer ws = (WorldServer) event.world;

        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            WingmanEntry entry = e.getValue();
            if (!entry.isAutonomous()) continue;

            Entity wingman = ws.getEntityFromUuid(e.getKey());
            if (wingman == null || wingman.isDead) continue;

            tickMission(ws, wingman, entry);
        }
    }

    /**
     * Phase.END: McHeli 物理更新後にも forceYaw を再適用。
     * McHeli がエンティティtick内でヨートルクを加算する場合でも、
     * クライアントへのパケット送信前にヨーを正確な値に戻す。
     */
    @SubscribeEvent
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        WorldServer ws = (WorldServer) event.world;

        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            WingmanEntry entry = e.getValue();
            if (!entry.isAutonomous()) continue;

            Entity wingman = ws.getEntityFromUuid(e.getKey());
            if (wingman == null || wingman.isDead) continue;

            // ─── VTOL 水平位置補正（McHeli 物理更新後）─────────────────────────────
            // VTOL_TAKEOFF 待機フェーズ (missionNodeTimer < 80):
            //   ノズル回転中、機体をパッド中心へ誘導する。motionX/Z を使うことで McHeli の
            //   ホイールアニメーションも機能する。motionY=0 で VTOL ホバー物理の浮き上がりを防ぐ。
            // VTOL_LAND ホバー・降下フェーズ (vtolHoverMode=true):
            //   接近フェーズ終了時 hDist=0〜20 ブロックの任意点から降下を開始するため、
            //   パッド中心方向へゆっくり移動させながら垂直降下することで正確な着地を実現する。
            boolean vtolPadCorrect =
                (entry.autoState == AutonomousState.VTOL_TAKEOFF && entry.missionNodeTimer < 180)
                || (entry.autoState == AutonomousState.VTOL_LAND   && entry.vtolHoverMode);
            if (vtolPadCorrect) {
                String _padBaseId = (entry.order != null) ? entry.order.baseId : "";
                MarkerRegistry.MarkerInfo padInfo = findMarkerInfoById(ws, entry.assignedParkingId, _padBaseId);
                if (padInfo != null) {
                    double px = padInfo.pos.getX() + 0.5 - wingman.posX;
                    double pz = padInfo.pos.getZ() + 0.5 - wingman.posZ;
                    double d  = Math.sqrt(px * px + pz * pz);

                    // ─── VTOL_LAND: 水平補正は着地30ブロック以内のみ ─────────────────
                    // 高高度でのホバー中に水平補正を適用すると機体が空中を横移動し
                    // 「空中タキシング」のように見える。パッドに近い高度でのみ補正する。
                    boolean applyHorizCorrect = true;
                    if (entry.autoState == AutonomousState.VTOL_LAND) {
                        double gyCheck = (padInfo.type == com.norwood.mcheli.wingman.block.MarkerType.HELIPAD)
                            ? padInfo.pos.getY() + 1.0
                            : wingman.world.getHeight(padInfo.pos.getX(), padInfo.pos.getZ());
                        applyHorizCorrect = wingman.posY <= gyCheck + 30.0;
                    }

                    if (applyHorizCorrect) {
                        if (d > 0.2) {
                            // 距離に比例した速度（上限 0.5 blocks/tick）でパッド中心へ向かう。
                            // hDist=20 → 0.5/tick → 40 tick で中心到達。
                            // hDist=2  → 0.1/tick → スムーズに停止。
                            double spd = Math.min(0.5, d * 0.05);
                            wingman.motionX = (px / d) * spd;
                            wingman.motionZ = (pz / d) * spd;
                        } else {
                            wingman.motionX = 0;
                            wingman.motionZ = 0;
                        }
                    }

                    // VTOL_TAKEOFF 待機中: motionY=0 で VTOL ホバー物理の浮き上がりを防止。
                    // McHeli 物理後（onWorldTickEnd）に 0 にすることで次 tick への適用を保証。
                    if (entry.autoState == AutonomousState.VTOL_TAKEOFF) {
                        wingman.motionY = 0;
                    } else if (entry.autoState == AutonomousState.VTOL_LAND) {
                        // 降下速度の上限を -0.12 blocks/tick (≒2.4ブロック/秒) に制限。
                        // ホバースロットルが機体ごとに異なっても、この上限で降下が速くなりすぎない。
                        wingman.motionY = Math.max(wingman.motionY, -0.12);
                    }
                } else {
                    // マーカーが見つからない → ドリフトのみ防止
                    wingman.motionX = 0;
                    wingman.motionZ = 0;
                }
            }

            // ─── VTOL_LAND ホバー降下: McHeli 物理後にピッチ・ロールを 0° に再強制 ─────
            // VTOL ホバーモード中、McHeli の物理更新が速度ベクトルからピッチを計算し
            // 機首下げ（負のピッチ）を付与することがある。これにより機体がヘリパッドで
            // バウンドしたり、垂直降下ではなく前傾降下になったりする。
            // Phase.END で forcePitch(0) することで確実に水平姿勢を維持する。
            // ─── VTOL_LAND ホバー降下 / 着地後待機: McHeli 物理後にピッチ・ロールを 0° に再強制 ─────
            // vtolHoverMode=true: ホバー降下中の機首下げを防ぐ
            // missionNodeTimer < 0: 着地後待機中（VTOL OFF 後も McHeli が機首下げを付与する）
            if (entry.autoState == AutonomousState.VTOL_LAND
                    && (entry.vtolHoverMode || entry.missionNodeTimer < 0)) {
                forcePitch(wingman, 0.0f);
                forceRoll(wingman, 0.0f);
                continue;
            }

            // ─── VTOL_TAKEOFF 地上フェーズ: McHeli 地上物理後にヨーを再強制 ─────────
            // TAKEOFF_ROLL と同様に、McHeli の地上トルク物理が WTH のヨー設定を
            // 毎 tick 上書きするため、Phase.END でヨーを再適用する。
            // missionNodeTimer < 180: ノズル待機(0-79) + アイドル(80-139) + ランプアップ前半(140-179)
            // autoTargetX/Z は tickOrderVtolTakeoff が毎 tick「パッド中心から50ブロック先」に更新済み。
            if (entry.autoState == AutonomousState.VTOL_TAKEOFF && entry.missionNodeTimer < 180) {
                double vtolDx = entry.autoTargetX - wingman.posX;
                double vtolDz = entry.autoTargetZ - wingman.posZ;
                double vtolHd = Math.sqrt(vtolDx * vtolDx + vtolDz * vtolDz);
                if (vtolHd >= 1.0) {
                    float vtolYaw = (float) Math.toDegrees(Math.atan2(-vtolDx, vtolDz));
                    forceYaw(wingman, vtolYaw);
                }
                continue;
            }

            // ─── PARKED: 駐機方位強制（設定がある場合のみ）────────────────────────────
            // 機体が停止した後も McHeli が地上トルク等でヨーを動かす場合があるため
            // Phase.END で毎 tick 再強制する。parkingHeadingYaw が NaN なら何もしない。
            if (entry.autoState == AutonomousState.PARKED
                    && !Float.isNaN(entry.parkingHeadingYaw)) {
                forceYaw(wingman, entry.parkingHeadingYaw);
                continue;
            }

            // ─── LANDING: タッチダウン後地上ロール中はピッチ・ロールを 0° に強制 ──────────
            // LANDING は isTaxiingState=true で taxiPushPosition が位置を制御するが、
            // McHeli が速度ベクトルからピッチを再計算し着陸姿勢（機首上げ等）が維持される。
            // Phase.END で毎 tick 水平姿勢に上書きすることで後輪の地面めり込みを防ぐ。
            if (entry.autoState == AutonomousState.LANDING) {
                forcePitch(wingman, 0.0f);
                forceRoll(wingman, 0.0f);
                continue;
            }

            // ─── CIRCUIT_FINAL: グライドスロープ追従 (空中) + RT_A 地上ロール (接地後) ────
            // 【問題】Phase.START のスロットル制御だけでは機体が降下しすぎ、RT_A より
            //         手前でタッチダウンする。
            // 【対策】Phase.END で毎 tick motionY を glideAlt に向けて P 制御することで、
            //         McHeli 物理後も正確にグライドスロープを追従させる。
            // 【接地後】RT_A まで setPosition で位置を強制移動し、ピッチ・ロールを 0° に。
            if (entry.autoState == AutonomousState.CIRCUIT_FINAL && entry.hasOrder()) {
                MarkerRegistry.MarkerInfo[] cfRwy = getOrderRunway(ws, entry, entry.order);
                if (cfRwy != null && cfRwy[0] != null && cfRwy[1] != null) {
                    double cfAx = cfRwy[0].pos.getX() + 0.5, cfAy = cfRwy[0].pos.getY(),
                           cfAz = cfRwy[0].pos.getZ() + 0.5;
                    double cfBx = cfRwy[1].pos.getX() + 0.5, cfBy = cfRwy[1].pos.getY(),
                           cfBz = cfRwy[1].pos.getZ() + 0.5;
                    double cfRdx = cfBx - cfAx, cfRdz = cfBz - cfAz;
                    double cfRlen = Math.sqrt(cfRdx * cfRdx + cfRdz * cfRdz);
                    if (cfRlen >= 1) {
                        double cfDirX = cfRdx / cfRlen, cfDirZ = cfRdz / cfRlen;
                        double cfCircuitY = cfAy + 80; // RT_A の Y 座標を基準に 80 ブロック上空
                        double cfTouchdown = cfAy + 1;
                        double cfProj = (wingman.posX - cfAx) * (-cfDirX)
                                      + (wingman.posZ - cfAz) * (-cfDirZ);
                        double cfGlide = cfTouchdown + Math.max(0, cfProj)
                                * ((cfCircuitY - cfTouchdown) / CIRCUIT_FINAL_DIST);
                        cfGlide = Math.max(cfTouchdown, Math.min(cfCircuitY, cfGlide));

                        // 接地判定: RT_A 高さ +2 以下を接地とみなす
                        boolean cfGround = wingman.posY <= cfAy + 2;

                        if (!cfGround) {
                            // ── 空中: motionY を P 制御でグライドスロープに追従 ────────
                            // glideError > 0: 高すぎる → 降下。< 0: 低すぎる → 緩やかに上昇。
                            double glideError = wingman.posY - cfGlide;
                            double tMotionY = -glideError * 0.15;
                            // 上昇は 0.1/tick 以内、降下は 0.4/tick 以内
                            tMotionY = Math.max(-0.4, Math.min(0.1, tMotionY));
                            wingman.motionY = tMotionY;
                        } else {
                            // ── 接地後: RT_A へ向かって位置を強制移動 ─────────────────
                            double dx = cfAx - wingman.posX, dz = cfAz - wingman.posZ;
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            if (dist > 1.0) {
                                double spd = Math.min(0.4, dist * 0.05);
                                wingman.setPosition(
                                    wingman.posX + (dx / dist) * spd,
                                    wingman.posY,
                                    wingman.posZ + (dz / dist) * spd);
                            }
                            // 接地後の姿勢水平化
                            forcePitch(wingman, 0.0f);
                            forceRoll(wingman, 0.0f);
                        }
                    }
                }
                continue; // TAKEOFF_ROLL/CLIMB 処理をスキップ
            }

            // ─── TAKEOFF_ROLL / CLIMB: McHeli 物理後にヨー・ピッチを再強制 ────────────
            // McHeli の onUpdate_Server が毎 tick 実速度ベクトルからヨーとピッチを
            // 再計算して上書きする。Phase.START で WTH が設定した機首上げ角度が
            // McHeli 物理後には 0° にリセットされるため、Phase.END でピッチを再強制しないと
            // 揚力が発生せず離陸できない（地上滑走のまま 200 tick タイムアウトになる）。
            if (entry.autoState != AutonomousState.TAKEOFF_ROLL
                    && entry.autoState != AutonomousState.CLIMB) continue;

            // ─── CLIMB ピッチ強制（McHeli 物理後）────────────────────────────────────
            // McHeli は毎 tick 速度ベクトルからピッチを再計算して 0° にリセットする。
            // Phase.END で forcePitch しないとアフターバーナー全開でも水平飛行になる。
            // autoTargetY は centerlineTarget が「min(targetAlt, posY+15)」に毎 tick 更新済み。
            if (entry.autoState == AutonomousState.CLIMB) {
                double dyToTarget = entry.autoTargetY - wingman.posY;
                // dy > 0（目標が上）→ atan2 正 → 先頭 - で機首上げ（負値）
                float climbPitch = -(float) Math.toDegrees(Math.atan2(dyToTarget, LOOKAHEAD_DIST));
                // -25° ～ -3°（0° に近すぎると水平飛行になるので最低 -3° を保証）
                climbPitch = Math.max(-25f, Math.min(-3f, climbPitch));
                forcePitch(wingman, climbPitch);
                McHeliWingman.logger.debug("[Order] {} CLIMB forcePitch={} dyToTarget={}",
                    shortId(wingman), String.format("%.1f", climbPitch),
                    String.format("%.1f", dyToTarget));
                continue;  // 以下の TAKEOFF_ROLL ヨー・地上拘束処理はスキップ
            }

            MarkerRegistry.MarkerInfo rwyA = null, rwyB = null;

            if (entry.hasOrder()) {
                // MissionOrder 系: TaxiRoute / baseId から滑走路を解決
                MarkerRegistry.MarkerInfo[] rwy = getOrderRunway(ws, entry, entry.order);
                if (rwy != null) { rwyA = rwy[0]; rwyB = rwy[1]; }
            } else {
                // MissionNode 系
                MissionNode node = entry.currentNode();
                if (node == null || node.type != MissionNode.Type.TAKEOFF) continue;
                rwyA = MarkerRegistry.resolveMarker(ws, node.runwayId, MarkerType.RUNWAY_A);
                rwyB = MarkerRegistry.resolveMarker(ws, node.runwayId, MarkerType.RUNWAY_B);
            }
            if (rwyA == null || rwyB == null) continue;

            double ax = rwyA.pos.getX() + 0.5, az = rwyA.pos.getZ() + 0.5;
            double bx = rwyB.pos.getX() + 0.5, bz = rwyB.pos.getZ() + 0.5;
            double rdx = bx - ax, rdz = bz - az;
            double rlen = Math.sqrt(rdx * rdx + rdz * rdz);
            if (rlen < 1) continue;
            double dirX = rdx / rlen, dirZ = rdz / rlen;
            float runwayYaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));

            // ヨーを全 TAKEOFF_ROLL 期間で強制（直進保証）
            forceYaw(wingman, runwayYaw);

            double rollDist = (wingman.posX - ax) * dirX + (wingman.posZ - az) * dirZ;
            double distToB  = rlen - rollDist;

            // ─── グランドロール地面拘束（タイマーベース）──────────────────────────────
            // distToB ではなくタイマーで判定。滑走路が短く最初から ROTATION_DIST 内に
            // ある場合でも、最初の60tick(3秒)は確実に機体を地上に拘束する。
            // Phase.END（McHeli物理後）で setPosition を毎tick上書きすることで、
            // McHeli が揚力で機体を浮かせても次の Phase.END で地上へ戻す。
            if (entry.missionNodeTimer <= 60 && entry.groundRollY > 0) {
                if (wingman.posY > entry.groundRollY + 0.3) {
                    wingman.setPosition(wingman.posX, entry.groundRollY, wingman.posZ);
                    wingman.motionY = 0;
                }
            }

            // ─── ローテーションゾーン: ピッチ強制（グランドロール60tick経過後のみ）──────
            // McHeli が 0° にリセットするピッチを Phase.END で再上書きし、
            // 次 tick の McHeli 物理更新で機首上げ → 揚力発生 → 離陸を実現する。
            // グランドロール中（timer≤60）はピッチ強制しない（地上拘束と矛盾するため）。
            if (distToB <= ROTATION_DIST && entry.missionNodeTimer > 60) {
                // factor: 0.0 (RT_B-ROTATION_DIST) → 1.0 (RT_B 到達/通過後)
                double factor  = Math.max(0.0, Math.min(1.0, 1.0 - distToB / ROTATION_DIST));
                // McHeli 規約: 負 = 機首上げ。最大 -20°。
                float rotPitch = -(float)(factor * 20.0);
                forcePitch(wingman, rotPitch);
                McHeliWingman.logger.debug("[Order] {} TAKEOFF_ROLL rotZone distToB={} factor={} pitch={}",
                    shortId(wingman), String.format("%.1f", distToB),
                    String.format("%.2f", factor), String.format("%.1f", rotPitch));
            }
        }
    }

    private void tickMission(WorldServer ws, Entity wingman, WingmanEntry entry) {
        // MissionOrder 系（新）を優先
        if (entry.hasOrder()) {
            tickOrder(ws, wingman, entry);
            return;
        }

        // MissionNode 系（旧）
        MissionNode node = entry.currentNode();
        if (node == null) {
            entry.autoState = AutonomousState.PARKED;
            entry.mission   = null;
            McHeliWingman.logger.info("[Auto] {} mission complete", shortId(wingman));
            return;
        }

        switch (node.type) {
            case FLY_TO:    tickFlyTo(ws, wingman, entry, node);    break;
            case TAKEOFF:   tickTakeoff(ws, wingman, entry, node);  break;
            case LAND:      tickLand(ws, wingman, entry, node);     break;
            case ATTACK:    tickAttack(ws, wingman, entry, node);   break;
            case LOITER:    tickLoiter(ws, wingman, entry, node);   break;
            case PARK:      tickPark(ws, wingman, entry, node);     break;
        }
    }

    // ─── FLY_TO ──────────────────────────────────────────────────────────────

    private void tickFlyTo(WorldServer ws, Entity wingman, WingmanEntry entry, MissionNode node) {
        entry.autoState = AutonomousState.ENROUTE;
        setThrottle(wingman, 1.0);  // 巡航は常に全スロットル
        // 目標高度は指定値 or 巡航高度のどちらか高い方（地面レベルの waypoint で急降下しない）
        double targetY = Math.max(node.y, CRUISE_ALT);
        entry.autoTargetX = node.x;
        entry.autoTargetY = targetY;
        entry.autoTargetZ = node.z;

        // 到達判定は XZ 水平距離のみ（高度差で永遠に到達しない問題を防ぐ）
        double dx = node.x - wingman.posX;
        double dz = node.z - wingman.posZ;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist < ARRIVE_DIST) {
            McHeliWingman.logger.info("[Auto] {} FLY_TO reached ({},{},{})", shortId(wingman), (int)node.x, (int)node.y, (int)node.z);
            entry.advanceMission();
        }
    }

    // ─── TAKEOFF ─────────────────────────────────────────────────────────────

    private void tickTakeoff(WorldServer ws, Entity wingman, WingmanEntry entry, MissionNode node) {
        MarkerRegistry.MarkerInfo rwyA = MarkerRegistry.resolveMarker(ws, node.runwayId, MarkerType.RUNWAY_A);
        MarkerRegistry.MarkerInfo rwyB = MarkerRegistry.resolveMarker(ws, node.runwayId, MarkerType.RUNWAY_B);
        if (rwyA == null || rwyB == null) {
            McHeliWingman.logger.warn("[Auto] {} TAKEOFF: runway '{}' A or B not found, skipping", shortId(wingman), node.runwayId);
            entry.advanceMission();
            return;
        }

        double ax = rwyA.pos.getX() + 0.5, ay = rwyA.pos.getY(), az = rwyA.pos.getZ() + 0.5;
        double bx = rwyB.pos.getX() + 0.5,                       bz = rwyB.pos.getZ() + 0.5;

        double rdx = bx - ax, rdz = bz - az;
        double rlen = Math.sqrt(rdx * rdx + rdz * rdz);
        if (rlen < 1) { entry.advanceMission(); return; }
        double dirX = rdx / rlen, dirZ = rdz / rlen;

        // 滑走路方向の Minecraft yaw (atan2(-dx, dz))
        float runwayYaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));

        switch (entry.autoState) {
            case TAXI_OUT: {
                // A 端へ直接向かう（低速タクシー）—— MissionNode 系のシンプルな動作
                double dist = Math.sqrt(Math.pow(ax - wingman.posX, 2) + Math.pow(az - wingman.posZ, 2));
                entry.autoTargetX = ax;
                entry.autoTargetY = ay;
                entry.autoTargetZ = az;
                setThrottle(wingman, 0.0); // WTH の taxiPushPosition() が地上移動を担う
                if (entry.missionNodeTimer++ % 40 == 0) {
                    McHeliWingman.logger.info("[Auto] {} TAXI_OUT dist={}", shortId(wingman), (int)dist);
                }
                if (dist < TAXI_DIST) {
                    entry.autoState = AutonomousState.ALIGN;
                    entry.missionNodeTimer = 0;
                }
                break;
            }
            case ALIGN:
            case TAKEOFF_ROLL:
            case CLIMB: {
                // 共通ヘルパーに委譲
                boolean done = tickAlignToClimb(ws, wingman, entry, rwyA, rwyB, CRUISE_ALT);
                if (done) {
                    McHeliWingman.logger.info("[Auto] {} reached cruise alt", shortId(wingman));
                    entry.advanceMission();
                }
                break;
            }
            default:
                entry.autoState = AutonomousState.TAXI_OUT;
                entry.missionNodeTimer = 0;
                McHeliWingman.logger.info("[Auto] {} TAKEOFF start, runway='{}' A=({},{},{}) B=({},{},{})",
                    shortId(wingman), node.runwayId,
                    (int)ax, (int)ay, (int)az, (int)bx, (int)rwyB.pos.getY(), (int)bz);
        }
    }

    // ─── LAND ────────────────────────────────────────────────────────────────

    private void tickLand(WorldServer ws, Entity wingman, WingmanEntry entry, MissionNode node) {
        MarkerRegistry.MarkerInfo rwyA = MarkerRegistry.resolveMarker(ws, node.runwayId, MarkerType.RUNWAY_A);
        MarkerRegistry.MarkerInfo rwyB = MarkerRegistry.resolveMarker(ws, node.runwayId, MarkerType.RUNWAY_B);
        if (rwyA == null || rwyB == null) {
            McHeliWingman.logger.warn("[Auto] {} LAND: runway '{}' A or B not found, skipping", shortId(wingman), node.runwayId);
            entry.advanceMission();
            return;
        }

        double ax = rwyA.pos.getX() + 0.5, ay = rwyA.pos.getY(), az = rwyA.pos.getZ() + 0.5;
        double bx = rwyB.pos.getX() + 0.5, by = rwyB.pos.getY(), bz = rwyB.pos.getZ() + 0.5;

        // 共通ヘルパーに委譲（MissionNode系でも MissionOrder系と同じ実装を使う）
        boolean landed = tickLandingCircuit(ws, wingman, entry, rwyA, rwyB);
        if (landed) {
            McHeliWingman.logger.info("[Auto] {} stopped", shortId(wingman));
            entry.advanceMission();
        }
    }

    /**
     * 滑走路中心線追従ターゲット。
     * 機体を軸に射影し、LOOKAHEAD 先の中心線上の点を autoTarget に設定する。
     * 横ズレが生じても連続的に補正される。
     *
     * @param ax/az  滑走路 A 端の XZ 座標（軸の起点）
     * @param targetY  目標高度（地上滑走時は ay、上昇時は CRUISE_ALT など）
     * @param dirX/dirZ  A→B 方向の単位ベクトル
     * @param lookahead  先読み距離 (blocks)
     */
    private static void centerlineTarget(WingmanEntry entry, Entity wingman,
                                          double ax, double targetY, double az,
                                          double dirX, double dirZ, double lookahead) {
        double proj = (wingman.posX - ax) * dirX + (wingman.posZ - az) * dirZ;
        double t = proj + lookahead;
        entry.autoTargetX = ax + dirX * t;
        entry.autoTargetY = targetY;
        entry.autoTargetZ = az + dirZ * t;
    }

    // =========================================================================
    // MissionOrder 系 — 新ミッション実行エンジン
    // =========================================================================

    private void tickOrder(WorldServer ws, Entity wingman, WingmanEntry entry) {
        MissionOrder order = entry.order;
        entry.diagTick++;

        // ─── ハートビートログ（10秒=200tick毎）─────────────────────────────
        if (entry.diagTick % 200 == 1) {
            double fuel    = McheliReflect.getFuel(wingman);
            double maxFuel = McheliReflect.getMaxFuel(wingman);
            String fuelStr = (maxFuel > 0)
                ? String.format("%.0f/%.0f(%.0f%%)", fuel, maxFuel, fuel / maxFuel * 100)
                : String.valueOf((int) fuel);
            double dx = entry.autoTargetX - wingman.posX;
            double dz = entry.autoTargetZ - wingman.posZ;
            double distToTarget = Math.sqrt(dx * dx + dz * dz);
            McHeliWingman.logger.info(
                "[Order/HB] {} state={} vtol={} timer={}/{} fuel={} pos=({},{},{}) target=({},{},{}) dist={}",
                shortId(wingman), entry.autoState,
                entry.vtolHoverMode ? "ON" : "off",
                entry.orderTimer, order.timeLimitSeconds + "sec",
                fuelStr,
                (int) wingman.posX, (int) wingman.posY, (int) wingman.posZ,
                (int) entry.autoTargetX, (int) entry.autoTargetY, (int) entry.autoTargetZ,
                (int) distToTarget);
        }

        // ─── RTBトリガー判定（ON_STATION中のみ）─────────────────────────────
        if (entry.autoState == AutonomousState.ON_STATION
                || entry.autoState == AutonomousState.STRIKE_PASS) {
            entry.orderTimer++;
            // 時間切れ
            if (order.timeLimitSeconds > 0 && entry.orderTimer >= order.timeLimitTicks()) {
                triggerRtb(wingman, entry, "time limit");
                return;
            }
            // ビンゴ燃料
            if (isBingo(wingman, order, ws)) {
                triggerRtb(wingman, entry, "bingo fuel");
                return;
            }
        }

        switch (entry.autoState) {

            // ─── 初期化 ────────────────────────────────────────────────────
            case NONE:
            case PARKED:
                initOrder(ws, wingman, entry, order);
                break;

            // ─── 地上タキシー（出発） ──────────────────────────────────────
            case TAXI_OUT:
                tickOrderTaxiOut(ws, wingman, entry, order);
                break;

            // ─── 離陸フェーズ（既存ヘルパー流用）─────────────────────────
            case ALIGN:
            case TAKEOFF_ROLL:
            case CLIMB: {
                MarkerRegistry.MarkerInfo[] rwy = getOrderRunway(ws, entry, order);
                if (rwy == null) {
                    // 滑走路情報なし → 直接巡航へ
                    entry.autoState = AutonomousState.TRANSIT_TO;
                    break;
                }
                boolean climbDone = tickAlignToClimb(ws, wingman, entry, rwy[0], rwy[1], order.cruiseAlt);
                if (climbDone) {
                    entry.autoState = AutonomousState.TRANSIT_TO;
                    McHeliWingman.logger.info("[Order] {} cruise altitude reached, TRANSIT_TO ({},{})",
                        shortId(wingman), (int)order.targetX, (int)order.targetZ);
                }
                break;
            }

            // ─── 任務エリアへ直行 ──────────────────────────────────────────
            case TRANSIT_TO:
                tickOrderTransit(ws, wingman, entry, order);
                break;

            // ─── オンステーション ──────────────────────────────────────────
            case ON_STATION:
                tickOrderOnStation(ws, wingman, entry, order);
                break;

            // ─── ストライクパス ────────────────────────────────────────────
            case STRIKE_PASS:
                tickOrderStrikePass(ws, wingman, entry, order);
                break;

            // ─── 帰投 ──────────────────────────────────────────────────────
            case RTB:
                tickOrderRtb(ws, wingman, entry, order);
                break;

            // ─── VTOL 離陸 / 着陸 ──────────────────────────────────────
            case VTOL_TAKEOFF:
                tickOrderVtolTakeoff(ws, wingman, entry, order);
                break;

            case VTOL_LAND:
                tickOrderVtolLand(ws, wingman, entry, order);
                break;

            // ─── 着陸サーキット（既存ヘルパー流用）───────────────────────
            case DESCEND:
            case CIRCUIT_DOWNWIND:
            case CIRCUIT_BASE:
            case CIRCUIT_FINAL:
            case LANDING: {
                MarkerRegistry.MarkerInfo[] rwy = getOrderRunway(ws, entry, order);
                if (rwy == null) {
                    // 滑走路情報なし → 直接駐機完了（runwayAId=null でフォールバック検索）
                    finishLanding(ws, wingman, entry, order, null, null);
                    break;
                }
                boolean landed = tickLandingCircuit(ws, wingman, entry, rwy[0], rwy[1]);
                if (landed) {
                    // A端タッチダウン/B端停止のため: runwayAId=rwy[0].id, rwyBId=rwy[1].id
                    finishLanding(ws, wingman, entry, order, rwy[0].id, rwy[1].id);
                }
                break;
            }

            // ─── 地上タキシー（帰還） ──────────────────────────────────────
            case TAXI_IN:
                tickOrderTaxiIn(ws, wingman, entry, order);
                break;
        }
    }

    /** MissionOrder 発令時の初期化。地上駐機中ならTAXI_OUTまたはVTOL_TAKEOFFへ、空中ならTRANSIT_TOへ。 */
    private void initOrder(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        if (!entry.assignedParkingId.isEmpty()) {
            // 駐機スポットのマーカー種別を確認
            MarkerRegistry.MarkerInfo parkingMarker = findMarkerInfoById(ws, entry.assignedParkingId, order.baseId);

            if (parkingMarker != null && parkingMarker.type == MarkerType.HELIPAD) {
                // ヘリパッドから直接VTOLで離陸（ヘリ・VTOL機のみ）
                if (McheliReflect.canUseHelipad(wingman)) {
                    entry.autoState        = AutonomousState.VTOL_TAKEOFF;
                    entry.vtolHoverMode    = true;
                    entry.vtolOnSent       = false;  // WTH が次 tick に 1 回だけ swithVtolMode を送る
                    entry.missionNodeTimer = 0;       // ノズル待機タイマーをリセット
                    entry.autoTargetX    = parkingMarker.pos.getX() + 0.5;
                    entry.autoTargetY    = order.cruiseAlt;
                    entry.autoTargetZ    = parkingMarker.pos.getZ() + 0.5;
                    McHeliWingman.logger.info("[Order] {} VTOL_TAKEOFF from helipad='{}'",
                        shortId(wingman), entry.assignedParkingId);
                    return;
                } else {
                    McHeliWingman.logger.warn("[Order] {} assigned to HELIPAD '{}' but cannot use helipad, airborne start",
                        shortId(wingman), entry.assignedParkingId);
                }
            } else {
                // 通常駐機スポット: タキシールートを検索
                // departureRouteId が設定されていれば findById で確実に出発ルートを取得する。
                // findByParking は同じ parkingId を持つ複数ルートがある場合に誤ルートを返す可能性がある。
                TaxiRoute route = !entry.departureRouteId.isEmpty()
                    ? TaxiRouteRegistry.findById(ws, entry.departureRouteId)
                    : TaxiRouteRegistry.findByParking(ws, entry.assignedParkingId);
                if (route != null) {
                    entry.taxiWpQueue = new ArrayList<>(route.fullDeparture());
                    // fullDeparture()[0] は parkingId 自身。
                    // 機体がパーキング以外の場所にいる場合（スポーン直後など）、
                    // 最初のWPとしてパーキングへ戻らせる必要はない。
                    // インデックス1（最初のタキシーWP or 滑走路A端）から開始することで、
                    // 複数機が同一パーキングへ集中する問題を解消する。
                    entry.taxiWpIndex = (entry.taxiWpQueue.size() > 1) ? 1 : 0;
                    entry.autoState   = AutonomousState.TAXI_OUT;
                    McHeliWingman.logger.info("[Order] {} TAXI_OUT start, parking='{}' route='{}' startIdx={} queue={} waypoints={} runwayId='{}'",
                        shortId(wingman), entry.assignedParkingId, route.routeId,
                        entry.taxiWpIndex, entry.taxiWpQueue, route.waypointIds, route.runwayId);
                    return;
                } else {
                    McHeliWingman.logger.warn("[Order] {} no TaxiRoute found for parking='{}', falling back to airborne start",
                        shortId(wingman), entry.assignedParkingId);
                }
            }
        } else {
            McHeliWingman.logger.info("[Order] {} no parking assigned, airborne start", shortId(wingman));
        }
        // 空中 or タキシールートなし → 直接巡航開始
        entry.autoState = AutonomousState.TRANSIT_TO;
        if (order.targetX == 0 && order.targetZ == 0) {
            McHeliWingman.logger.warn("[Order] {} WARNING: target is (0,0) — GUIでTarget X/Zを設定してください。" +
                " 機体は世界原点へ向かいます。", shortId(wingman));
        }
        McHeliWingman.logger.info("[Order] {} TRANSIT_TO ({},{}) baseId='{}' timeLimitSec={}",
            shortId(wingman), (int)order.targetX, (int)order.targetZ,
            order.baseId, order.timeLimitSeconds);
    }

    /** タキシーWPキューに沿って地上移動（出発用）。 */
    private void tickOrderTaxiOut(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        if (entry.taxiWpIndex >= entry.taxiWpQueue.size()) {
            // キュー完了 → タキシーエンドポイントに到達
            // 最終WPがヘリパッドなら VTOL_TAKEOFF、それ以外は通常 ALIGN
            String lastWpId = entry.taxiWpQueue.isEmpty() ? ""
                : entry.taxiWpQueue.get(entry.taxiWpQueue.size() - 1);
            MarkerRegistry.MarkerInfo lastMarker = findMarkerInfoById(ws, lastWpId, order.baseId);
            if (lastMarker != null && lastMarker.type == MarkerType.HELIPAD
                    && McheliReflect.canUseHelipad(wingman)) {
                entry.autoState        = AutonomousState.VTOL_TAKEOFF;
                entry.vtolHoverMode    = true;
                entry.vtolOnSent       = false;  // リセット: WTH が 1 回だけ ON コマンドを送る
                entry.missionNodeTimer = 0;       // ノズル待機タイマーをリセット
                entry.assignedParkingId = lastWpId;  // パーキングIDをヘリパッドIDに更新（vtolPadCorrect用）
                entry.autoTargetX    = lastMarker.pos.getX() + 0.5;
                entry.autoTargetY    = order.cruiseAlt;
                entry.autoTargetZ    = lastMarker.pos.getZ() + 0.5;
                McHeliWingman.logger.info("[Order] {} TAXI_OUT complete → VTOL_TAKEOFF from helipad '{}'",
                    shortId(wingman), lastWpId);
            } else {
                // ヘリコプターはCTOLルート（滑走路離陸）不可 → PARKED に戻す
                // VTOL固定翼機はCTOL離陸が可能（useVstol=false で選択） → このまま ALIGN へ進む
                if (McheliReflect.isHelicopter(wingman)) {
                    McHeliWingman.logger.error(
                        "[Order] {} ERROR: helicopter on CTOL runway route '{}' — " +
                        "assign a helipad route instead. Aborting to PARKED.",
                        shortId(wingman), entry.departureRouteId);
                    entry.autoState = AutonomousState.PARKED;
                    entry.order     = null;
                    return;
                }
                entry.autoState        = AutonomousState.ALIGN;
                entry.missionNodeTimer = 0;
                McHeliWingman.logger.info("[Order] {} reached runway, starting ALIGN", shortId(wingman));
            }
            return;
        }
        String wpId = entry.taxiWpQueue.get(entry.taxiWpIndex);
        BlockPosXZ wp = resolveAnyMarker(ws, wpId, order.baseId);
        if (wp == null) {
            McHeliWingman.logger.warn("[Order] {} TAXI_OUT WP {}/{} id='{}' NOT FOUND in MarkerRegistry — skipping! (WP marker missing or ID mismatch?)",
                shortId(wingman), entry.taxiWpIndex + 1, entry.taxiWpQueue.size(), wpId);
            entry.taxiWpIndex++; return;
        }

        double tx = wp.x + 0.5, tz = wp.z + 0.5;
        entry.autoTargetX = tx;
        entry.autoTargetY = wingman.posY;
        entry.autoTargetZ = tz;
        setThrottle(wingman, 0.0); // WTH の taxiPushPosition() が位置移動を担う

        double dist = Math.sqrt(Math.pow(tx - wingman.posX, 2) + Math.pow(tz - wingman.posZ, 2));
        if (entry.diagTick % 100 == 5) {
            McHeliWingman.logger.info("[Order] {} TAXI_OUT WP {}/{} id={} dist={} pos=({},{})",
                shortId(wingman), entry.taxiWpIndex + 1, entry.taxiWpQueue.size(),
                wpId, (int) dist, (int) wingman.posX, (int) wingman.posZ);
        }
        if (dist < 1.0) {  // XZ完全一致（taxiPushPosition停止閾値0.5に合わせて1.0）
            McHeliWingman.logger.info("[Order] {} TAXI_OUT reached WP {}/{}: {} dist={}",
                shortId(wingman), entry.taxiWpIndex + 1, entry.taxiWpQueue.size(), wpId, (int) dist);
            entry.taxiWpIndex++;
        }
    }

    /** 任務エリアへの直行飛行。 */
    private void tickOrderTransit(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        setThrottle(wingman, 1.0);
        entry.autoTargetX = order.targetX;
        entry.autoTargetY = order.cruiseAlt;
        entry.autoTargetZ = order.targetZ;

        double dx = order.targetX - wingman.posX;
        double dz = order.targetZ - wingman.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (entry.diagTick % 200 == 3) {
            McHeliWingman.logger.info("[Order] {} TRANSIT_TO dist={} target=({},{}) alt={}",
                shortId(wingman), (int) dist,
                (int) order.targetX, (int) order.targetZ, (int) order.cruiseAlt);
        }

        if (dist < ARRIVE_DIST) {
            entry.autoState  = AutonomousState.ON_STATION;
            entry.orderTimer = 0;
            McHeliWingman.logger.info("[Order] {} ON_STATION start types={} target=({},{}) timeLimitSec={}",
                shortId(wingman), order.missionTypes,
                (int) order.targetX, (int) order.targetZ, order.timeLimitSeconds);
        }
    }

    /** オンステーション実行。ミッション種別に応じた行動。 */
    private void tickOrderOnStation(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        // STRIKE は即パスへ移行
        if (order.hasType(MissionType.STRIKE) && entry.strikePassesRemaining > 0) {
            entry.autoState        = AutonomousState.STRIKE_PASS;
            entry.missionNodeTimer = 0;
            return;
        }
        // FERRY: 到着後 100tick 待機してRTB
        if (order.hasType(MissionType.FERRY)) {
            if (entry.orderTimer >= 100) triggerRtb(wingman, entry, "ferry complete");
            return;
        }
        // RECON: 旋回しながらMob数カウント
        if (order.hasType(MissionType.RECON) && !order.hasType(MissionType.CAP)
                && !order.hasType(MissionType.CAS) && !order.hasType(MissionType.ESCORT)) {
            tickRecon(ws, wingman, entry, order);
            return;
        }
        // CAP / CAS / ESCORT: 旋回 + 自動攻撃
        orbit(entry, wingman, order);
        entry.attackMode = WingmanEntry.ATK_AUTO;

        // RECON を追加していた場合はMob数もカウント
        if (order.hasType(MissionType.RECON)) {
            countMobs(wingman, order, entry);
        }
    }

    /** ストライクパス実行。 */
    private void tickOrderStrikePass(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        entry.missionNodeTimer++;
        entry.attackMode = WingmanEntry.ATK_AUTO;
        setThrottle(wingman, 1.0); // ストライクパス中は全力
        // ターゲットへ飛び込む（orbit で代替: 狭い半径=100 で攻撃パス）
        double angle = entry.orbitAngle + Math.PI; // 逆側から突入
        double r = Math.min(order.orbitRadius * 0.3, 100.0);
        entry.autoTargetX = order.targetX + Math.cos(angle) * r;
        entry.autoTargetY = order.cruiseAlt;
        entry.autoTargetZ = order.targetZ + Math.sin(angle) * r;
        entry.orbitAngle += 0.012;

        // 1パス = 300tick (15秒)
        if (entry.missionNodeTimer >= 300) {
            entry.strikePassesRemaining--;
            entry.missionNodeTimer = 0;
            McHeliWingman.logger.info("[Order] {} strike pass done, remaining={}", shortId(wingman),
                entry.strikePassesRemaining);
            if (entry.strikePassesRemaining <= 0) {
                triggerRtb(wingman, entry, "strike complete");
            } else {
                entry.autoState = AutonomousState.ON_STATION; // 次パスまで旋回
            }
        }
    }

    /** 偵察専用行動：旋回しながらMob数を集計し、任務時間後にRTBレポート。 */
    private void tickRecon(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        orbit(entry, wingman, order);
        entry.attackMode = WingmanEntry.ATK_NONE;
        countMobs(wingman, order, entry);
    }

    /**
     * 帰投飛行。到着マーカーを探して着陸フェーズへ遷移する。
     *   1. ヘリ/VTOL機 かつ ヘリパッドがあれば → VTOL_LAND
     *   2. RUNWAY_B があれば → 降下サーキット
     *   3. ヘリ/VTOL機 でヘリパッドもRUNWAY_Bもなければ → その場VTOL_LAND（Bug③ fix）
     *   4. 固定翼でRUNWAY_Bなし → 警告してPARKED
     */
    private void tickOrderRtb(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        setThrottle(wingman, 1.0);
        entry.attackMode = WingmanEntry.ATK_NONE;

        boolean isHeli        = McheliReflect.isHelicopter(wingman);
        boolean isVtolCapable = McheliReflect.isVtol(wingman);
        // ヘリは常にヘリパッド優先。VTOL固定翼は order.useVstol=true のときのみ VTOL 着陸。
        // VSTOL を選択していない場合は固定翼として RUNWAY_B サーキット着陸を行う。
        boolean canVtol = isHeli || (isVtolCapable && order.useVstol);

        // ヘリ/VTOL機: ヘリパッドを優先チェック
        if (canVtol) {
            // 1. baseId で HELIPAD を検索（正規ルート）
            MarkerRegistry.MarkerInfo helipad = MarkerRegistry.findByBase(ws, order.baseId, MarkerType.HELIPAD);
            // 2. フォールバック: assignedParkingId が HELIPAD を指す場合。
            //    マーカーの baseId が order.baseId と不一致でも離陸元ヘリパッドへ着陸できるようにする。
            if (helipad == null && !entry.assignedParkingId.isEmpty()) {
                MarkerRegistry.MarkerInfo candidate =
                    findMarkerInfoById(ws, entry.assignedParkingId, order.baseId);
                if (candidate != null && candidate.type == MarkerType.HELIPAD) {
                    helipad = candidate;
                    McHeliWingman.logger.debug(
                        "[Order] {} RTB: HELIPAD resolved via assignedParkingId='{}' (baseId fallback)",
                        shortId(wingman), entry.assignedParkingId);
                }
            }
            if (helipad != null) {
                entry.assignedParkingId = helipad.id;
                entry.autoTargetX    = helipad.pos.getX() + 0.5;
                entry.autoTargetZ    = helipad.pos.getZ() + 0.5;
                entry.autoState      = AutonomousState.VTOL_LAND;
                entry.missionNodeTimer = 0;  // ホバー安定タイマーをリセット
                entry.vtolHoverMode  = false;  // 接近フェーズは固定翼モードで飛ぶ（tickOrderVtolLandが管理）
                McHeliWingman.logger.info("[Order] {} RTB: VTOL_LAND at helipad='{}'",
                    shortId(wingman), helipad.id);
                return;
            }
        }

        // RUNWAY_B を検索
        MarkerRegistry.MarkerInfo rwyB = MarkerRegistry.findByBase(ws, order.baseId, MarkerType.RUNWAY_B);
        if (rwyB == null) rwyB = MarkerRegistry.findById(ws, MarkerType.RUNWAY_B, order.baseId);

        if (rwyB == null) {
            if (canVtol) {
                // RUNWAY_B もヘリパッドもない → その場でVTOL着陸（Bug③ fix: 永久ホバリング防止）
                entry.autoTargetX   = wingman.posX;
                entry.autoTargetZ   = wingman.posZ;
                entry.autoState     = AutonomousState.VTOL_LAND;
                entry.vtolHoverMode = true;  // その場VTOL_LAND: 既にヘリパッド上空なので即ホバー降下
                McHeliWingman.logger.warn("[Order] {} RTB: no runway/helipad for base='{}' → VTOL_LAND in place",
                    shortId(wingman), order.baseId);
            } else {
                // 固定翼: 着陸できないので警告してアボート
                entry.order     = null;
                entry.autoState = AutonomousState.PARKED;
                McHeliWingman.logger.warn("[Order] {} RTB: RUNWAY_B not found for base='{}' — aborting mission",
                    shortId(wingman), order.baseId);
            }
            return;
        }

        // 通常の滑走路着陸
        double bx = rwyB.pos.getX() + 0.5;
        double bz = rwyB.pos.getZ() + 0.5;
        entry.autoTargetX = bx;
        entry.autoTargetY = order.cruiseAlt;
        entry.autoTargetZ = bz;

        double dx = bx - wingman.posX, dz = bz - wingman.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (entry.diagTick % 400 == 2) {
            McHeliWingman.logger.info("[Order] {} RTB in progress: dist={} to RUNWAY_B({},{}) base='{}'",
                shortId(wingman), (int) dist, (int) bx, (int) bz, order.baseId);
        }

        // 滑走路から200ブロック以内になったら降下サーキット開始
        if (dist < 200.0) {
            entry.autoState        = AutonomousState.DESCEND;
            entry.missionNodeTimer = 0;
            McHeliWingman.logger.info("[Order] {} entering landing circuit, dist={} base='{}'",
                shortId(wingman), (int) dist, order.baseId);
        }
    }

    /**
     * CTOL着陸後の後処理。タキシールートがあれば TAXI_IN へ、なければ PARKED。
     *
     * @param runwayAId  着陸に使用した RUNWAY_A マーカーの ID（null の場合はフォールバック検索）。
     *                   呼び出し側で rwy[0].id を渡すことで、同一 parkingId を持つ複数ルート
     *                   （CTOL出発ルートと VTOL到着ルートなど）を一意に区別する。
     * @param rwyBId     停止位置の RUNWAY_B マーカー ID（null の場合は通常の fullArrival を使用）。
     *                   A端タッチダウン/B端停止のとき、TAXI_IN キューをB端スタートに差し替えるために使用。
     */
    private void finishLanding(WorldServer ws, Entity wingman, WingmanEntry entry,
                               MissionOrder order, String runwayAId, String rwyBId) {
        entry.attackMode = WingmanEntry.ATK_NONE;

        // 空き駐機スポットを探す
        MarkerRegistry.MarkerInfo parking = MarkerRegistry.findAvailableParking(
            ws, order.baseId, ws.loadedEntityList);
        if (parking == null && !entry.assignedParkingId.isEmpty()) {
            // 以前の駐機スポットへ戻る（PARKING 優先、次に HELIPAD）
            parking = MarkerRegistry.findById(ws, MarkerType.PARKING, entry.assignedParkingId, order.baseId);
            if (parking == null) {
                parking = MarkerRegistry.findById(ws, MarkerType.HELIPAD, entry.assignedParkingId, order.baseId);
            }
        }

        if (parking != null) {
            TaxiRoute route = null;

            // ★ 優先: arrivalRouteId が指定されていればそれを直接使用（着陸専用ルート）
            if (!order.arrivalRouteId.isEmpty()) {
                route = TaxiRouteRegistry.findById(ws, order.arrivalRouteId);
                if (route != null) {
                    McHeliWingman.logger.info("[Order] {} finishLanding: using arrivalRouteId='{}'",
                        shortId(wingman), route.routeId);
                } else {
                    McHeliWingman.logger.warn("[Order] {} finishLanding: arrivalRouteId='{}' not found, falling back",
                        shortId(wingman), order.arrivalRouteId);
                }
            }

            // ① RUNWAY_A ID が判明している場合: findByRunway で一意に検索
            //    （CTOL出発ルートとVTOL到着ルートが同一 parkingId を共有する場合でも正しく選択）
            if (route == null && runwayAId != null && !runwayAId.isEmpty()) {
                route = TaxiRouteRegistry.findByRunway(ws, runwayAId);
                if (route != null && !runwayAId.equals(route.runwayId)) {
                    McHeliWingman.logger.warn(
                        "[Order] {} finishLanding: findByRunway('{}') returned route='{}' runwayId='{}' — MISMATCH, discarding",
                        shortId(wingman), runwayAId, route.routeId, route.runwayId);
                    route = null;
                }
            }

            // ② フォールバック: departureRouteId を findById で直接取得（findByRunway は同一runwayIdを持つ
            //    複数ルートが存在する場合に誤ルートを返す可能性があるため使用しない）
            if (route == null && !entry.departureRouteId.isEmpty()) {
                route = TaxiRouteRegistry.findById(ws, entry.departureRouteId);
                if (route != null) {
                    McHeliWingman.logger.info("[Order] {} finishLanding: using departureRoute '{}' directly",
                        shortId(wingman), route.routeId);
                }
            }

            // ③ 最終フォールバック: parkingId で検索（後方互換）
            if (route == null) {
                route = TaxiRouteRegistry.findByParking(ws, parking.id);
            }

            if (route != null) {
                entry.assignedParkingId = parking.id;
                // B端停止の場合（rwyBId が指定されている）: TAXI_IN キューをB端スタートに差し替え
                // fullArrival() = [runwayA, wps..., parking] → [rwyB, wps..., parking] に変換
                if (rwyBId != null && !rwyBId.isEmpty()) {
                    List<String> arrivalFromA = route.fullArrival();
                    List<String> queue = new ArrayList<>();
                    queue.add(rwyBId); // B端からスタート
                    if (arrivalFromA.size() > 1) {
                        queue.addAll(arrivalFromA.subList(1, arrivalFromA.size())); // WPs + parking
                    }
                    entry.taxiWpQueue = queue;
                    McHeliWingman.logger.info("[Order] {} TAXI_IN from B-end: rwyBId='{}' queue={}",
                        shortId(wingman), rwyBId, queue);
                } else {
                    entry.taxiWpQueue = new ArrayList<>(route.fullArrival());
                }
                entry.taxiWpIndex = 0;
                entry.autoState   = AutonomousState.TAXI_IN;
                McHeliWingman.logger.info("[Order] {} TAXI_IN to parking '{}' via route '{}' queue={}",
                    shortId(wingman), parking.id, route.routeId, entry.taxiWpQueue);
                return;
            }
        }

        // タキシールートなし → 駐機完了
        entry.autoState = AutonomousState.PARKED;
        entry.order     = null;
        applyParkingHeading(ws, entry);
        McHeliWingman.logger.info("[Order] {} mission complete (no taxi route). parking={} pos=({},{},{})",
            shortId(wingman), parking != null ? parking.id : "none",
            (int) wingman.posX, (int) wingman.posY, (int) wingman.posZ);
    }

    /** タキシーWPキューに沿って地上移動（帰還用）。 */
    private void tickOrderTaxiIn(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        if (entry.taxiWpIndex >= entry.taxiWpQueue.size()) {
            // 駐機スポットに到達 → 完了
            setThrottle(wingman, 0.0);
            entry.autoState = AutonomousState.PARKED;
            entry.order     = null;
            applyParkingHeading(ws, entry);
            McHeliWingman.logger.info("[Order] {} PARKED at '{}' headingYaw={}",
                shortId(wingman), entry.assignedParkingId,
                Float.isNaN(entry.parkingHeadingYaw) ? "any" : String.format("%.0f", entry.parkingHeadingYaw));
            return;
        }
        String wpId = entry.taxiWpQueue.get(entry.taxiWpIndex);
        BlockPosXZ wp = resolveAnyMarker(ws, wpId, order.baseId);
        if (wp == null) {
            McHeliWingman.logger.warn("[Order] {} TAXI_IN WP {}/{} id='{}' NOT FOUND in MarkerRegistry — skipping! (WP marker missing or ID mismatch?)",
                shortId(wingman), entry.taxiWpIndex + 1, entry.taxiWpQueue.size(), wpId);
            entry.taxiWpIndex++; return;
        }

        double tx = wp.x + 0.5, tz = wp.z + 0.5;
        entry.autoTargetX = tx;
        entry.autoTargetY = wingman.posY;
        entry.autoTargetZ = tz;
        setThrottle(wingman, 0.0); // WTH の taxiPushPosition() が位置移動を担う

        double dist = Math.sqrt(Math.pow(tx - wingman.posX, 2) + Math.pow(tz - wingman.posZ, 2));
        if (entry.diagTick % 100 == 5) {
            McHeliWingman.logger.info("[Order] {} TAXI_IN WP {}/{} id={} dist={} pos=({},{})",
                shortId(wingman), entry.taxiWpIndex + 1, entry.taxiWpQueue.size(),
                wpId, (int) dist, (int) wingman.posX, (int) wingman.posZ);
        }
        if (dist < 1.0) {  // XZ完全一致（taxiPushPosition停止閾値0.5に合わせて1.0）
            McHeliWingman.logger.info("[Order] {} TAXI_IN reached WP {}/{}: {} dist={}",
                shortId(wingman), entry.taxiWpIndex + 1, entry.taxiWpQueue.size(), wpId, (int) dist);
            entry.taxiWpIndex++;
        }
    }

    // ─── VTOL 離着陸 ─────────────────────────────────────────────────────────

    /**
     * VTOL離陸。ヘリパッドXZを保持しながら巡航高度まで上昇し、完了後 TRANSIT_TO へ。
     * initOrder() が autoTargetX/Z にヘリパッド座標をセット済みの前提で動作する。
     */
    /**
     * vtolHoverMode を変更し、値が変化したときのみログを出力するヘルパー。
     * WingmanTickHandler はこのフラグを見て McHeli の VTOL モードを ON/OFF する。
     */
    private void setVtolHoverMode(Entity wingman, WingmanEntry entry, boolean mode, String reason) {
        if (entry.vtolHoverMode != mode) {
            McHeliWingman.logger.info("[VTOL] {} hoverMode {} → {} ({})",
                shortId(wingman), entry.vtolHoverMode ? "ON" : "OFF", mode ? "ON" : "OFF", reason);
            entry.vtolHoverMode = mode;
        }
    }

    private void tickOrderVtolTakeoff(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        // VTOLホバーモードON: WTH が McHeli ノズルを ON にする (変化時のみログ)
        setVtolHoverMode(wingman, entry, true, "VTOL_TAKEOFF ascending");

        // ─── パッド座標とヨー目標を先に確定 ─────────────────────────────────────
        // ノズル待機フェーズ・上昇フェーズ共通:
        //   autoTargetX/Z = パッド中心から「離陸向き方向」へ 50 ブロックの固定点。
        //   wingman.posX でなく padX (固定) を基点にすることで毎 tick 変動しない。
        //   hDist ≈ 0 (パッド真上) でも WTH の "hDist≥1.0 でのみヨー更新" 条件を満たす。
        //
        //   方向決定の優先順位:
        //     1. HELIPAD_B マーカーがあれば HELIPAD → HELIPAD_B の固定方向（推奨）
        //     2. なければミッションターゲット方向（ミッションごとに変わる）
        double padX = wingman.posX, padZ = wingman.posZ; // フォールバック
        MarkerRegistry.MarkerInfo pad = findMarkerInfoById(ws, entry.assignedParkingId, order.baseId);
        if (pad != null) {
            padX = pad.pos.getX() + 0.5;
            padZ = pad.pos.getZ() + 0.5;
        }

        // HELIPAD_B を検索（同じ baseId を持つ方向指示マーカー）
        MarkerRegistry.MarkerInfo padB = (pad != null && !pad.baseId.isEmpty())
            ? MarkerRegistry.findByBase(ws, pad.baseId, MarkerType.HELIPAD_B) : null;

        // ─── 離陸向きの autoTargetX/Z を先に決定（全フェーズ共通）────────────────
        // padX/padZ から離陸向き方向に 50 ブロック離れた固定点を autoTargetX/Z にセット。
        // hDist が常に 50 になるため WTH の "hDist≥1.0 でのみヨー更新" 条件を常に満たす。
        //
        // ※ McHeli の setRotYaw は atan2 基準と 180° ずれているため、
        //   autoTarget を「HELIPAD_B 反対側」に置くことで機首が HELIPAD_B 方向を向く。
        if (padB != null) {
            double ddx = padB.pos.getX() + 0.5 - padX;
            double ddz = padB.pos.getZ() + 0.5 - padZ;
            double dlen = Math.sqrt(ddx * ddx + ddz * ddz);
            if (dlen > 1.0) {
                entry.autoTargetX = padX - (ddx / dlen) * 50.0;  // 反転: 機首→HELIPAD_B方向
                entry.autoTargetZ = padZ - (ddz / dlen) * 50.0;
            } else {
                entry.autoTargetX = padX;
                entry.autoTargetZ = padZ + 50.0; // フォールバック: 南方向
            }
        } else {
            // HELIPAD_B なし: ミッションターゲット方向（従来動作）
            if (entry.diagTick % 200 == 11 && pad != null) {
                McHeliWingman.logger.warn(
                    "[VTOL_TAKEOFF] {} HELIPAD_B not found (baseId='{}') → using mission-target direction. " +
                    "Place a HELIPAD_B marker with baseId='{}' to fix heading.",
                    shortId(wingman), pad.baseId, pad.baseId);
            }
            double toTargetDx = order.targetX - padX;
            double toTargetDz = order.targetZ - padZ;
            double toTargetLen = Math.sqrt(toTargetDx * toTargetDx + toTargetDz * toTargetDz);
            if (toTargetLen > 1.0) {
                entry.autoTargetX = padX - (toTargetDx / toTargetLen) * 50.0;
                entry.autoTargetZ = padZ - (toTargetDz / toTargetLen) * 50.0;
            } else {
                entry.autoTargetX = padX;
                entry.autoTargetZ = padZ + 50.0; // フォールバック: 南方向
            }
        }

        // ─── フェーズタイマー ─────────────────────────────────────────────────────
        // timer   0- 79: ノズル待機    (80tick = 4秒)  throttle=0
        //               McHeli ノズル回転 1.5°/tick × 60tick = 90°。余裕を持って 80tick 待つ。
        // timer  80-139: アイドル      (60tick = 3秒)  throttle=0.1
        //               エンジン音を出しつつ地上待機。vtolPadCorrect でパッド中心へ誘導継続。
        // timer 140-239: ランプアップ  (100tick = 5秒) throttle 0.1→0.9
        // timer  240+:   垂直上昇      throttle=0.9
        entry.missionNodeTimer++;
        if (entry.missionNodeTimer < 80) {
            // ノズル待機: throttle=0 で静止
            setThrottle(wingman, 0.0);
            entry.autoTargetY = wingman.posY;
            if (entry.diagTick % 20 == 7) {
                McHeliWingman.logger.info("[VTOL_TAKEOFF] {} nozzle-wait timer={}/80 nozzle={} yaw-target=({},{}) throttle=0",
                    shortId(wingman), entry.missionNodeTimer, entry.vtolNozzleMode,
                    (int) entry.autoTargetX, (int) entry.autoTargetZ);
            }
            return;
        }

        if (entry.missionNodeTimer < 140) {
            // アイドル: throttle=0.1 でエンジン音・ノズル固定、まだ離陸しない
            setThrottle(wingman, 0.1);
            entry.autoTargetY = wingman.posY;
            if (entry.diagTick % 20 == 7) {
                McHeliWingman.logger.info("[VTOL_TAKEOFF] {} idle timer={}/140 throttle=0.10",
                    shortId(wingman), entry.missionNodeTimer);
            }
            return;
        }

        if (entry.missionNodeTimer < 240) {
            // ランプアップ: 100tick で throttle 0.1→0.9 に滑らかに上昇
            double ramp = 0.1 + (entry.missionNodeTimer - 140) / 100.0 * 0.8;
            setThrottle(wingman, ramp);
            entry.autoTargetY = wingman.posY;
            if (entry.diagTick % 20 == 7) {
                McHeliWingman.logger.info("[VTOL_TAKEOFF] {} ramp-up timer={}/240 throttle={}",
                    shortId(wingman), entry.missionNodeTimer, String.format("%.2f", ramp));
            }
            return;
        }

        // ─── 垂直上昇フェーズ ────────────────────────────────────────────────────
        setThrottle(wingman, 0.9);
        entry.autoTargetY = order.cruiseAlt;

        if (entry.diagTick % 20 == 7) {
            McHeliWingman.logger.info("[VTOL_TAKEOFF] {} ascending alt={}/{} nozzle=2 padXZ=({},{}) targetXZ=({},{})",
                shortId(wingman),
                String.format("%.1f", wingman.posY), (int) order.cruiseAlt,
                (int) padX, (int) padZ,
                (int) entry.autoTargetX, (int) entry.autoTargetZ);
        }

        if (wingman.posY >= order.cruiseAlt - 5) {
            setVtolHoverMode(wingman, entry, false, "VTOL_TAKEOFF complete → TRANSIT_TO");
            entry.autoState = AutonomousState.TRANSIT_TO;
            McHeliWingman.logger.info("[VTOL_TAKEOFF] {} complete alt={} → TRANSIT_TO target=({},{})",
                shortId(wingman), String.format("%.1f", wingman.posY),
                (int) order.targetX, (int) order.targetZ);
        }
    }

    /**
     * VTOL着陸。
     * - 接近フェーズ (hDist > 20): 高度を保ちながらヘリパッドXZへ移動
     * - 降下フェーズ (hDist <= 20): 垂直降下して接地
     * RTB からの遷移時に entry.autoTargetX/Z = ヘリパッドXZ、entry.assignedParkingId = ヘリパッドID がセット済み。
     */
    private void tickOrderVtolLand(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        // FIXED: padX/padZ をマーカーレジストリから毎tick直接取得する。
        // entry.autoTargetX/Z は降下フェーズで上書きされるため、それを参照すると
        // 「padX = entry.autoTargetX; entry.autoTargetX = padX + offset;」
        // が毎tick累積し、ヘリパッドから10ブロック/tick ずつ無限にドリフトする。
        MarkerRegistry.MarkerInfo pad = findMarkerInfoById(ws, entry.assignedParkingId, order.baseId);
        double padX, padZ, groundY;
        if (pad != null) {
            padX    = pad.pos.getX() + 0.5;
            padZ    = pad.pos.getZ() + 0.5;
            groundY = (pad.type == MarkerType.HELIPAD) ? pad.pos.getY() + 1.0
                    : wingman.world.getHeight((int) padX, (int) padZ);
        } else {
            // マーカーが見つからない場合のフォールバック（初回のRTB直後のみ正確）
            padX    = entry.autoTargetX;
            padZ    = entry.autoTargetZ;
            groundY = wingman.world.getHeight((int) padX, (int) padZ);
        }

        // ─── 着地後待機フェーズ (missionNodeTimer < 0) ──────────────────────────
        // 降下フェーズで着地を検出すると missionNodeTimer = -40 にセットされる。
        // 毎 tick +1 して 0 に達したら finishVtolLanding を呼ぶ（2秒 = 40tick）。
        // この間はスロットル 0・VTOL OFF・motion 0 で完全静止する。
        if (entry.missionNodeTimer < 0) {
            setThrottle(wingman, 0.0);
            setVtolHoverMode(wingman, entry, false, "post-landing wait");
            wingman.motionX = 0;
            wingman.motionY = 0;
            wingman.motionZ = 0;
            // WTH Phase.START がこの autoTarget を使って機体を動かすため、
            // パッド中心に固定しないと待機中に遠方へドリフトする。
            entry.autoTargetX = padX;
            entry.autoTargetY = groundY;
            entry.autoTargetZ = padZ;
            if (entry.diagTick % 20 == 9) {
                McHeliWingman.logger.info("[VTOL_LAND] {} POST-LAND wait {}/100 anchor=({},{})",
                    shortId(wingman), 100 + entry.missionNodeTimer,
                    String.format("%.1f", padX), String.format("%.1f", padZ));
            }
            entry.missionNodeTimer++;
            if (entry.missionNodeTimer >= 0) {
                finishVtolLanding(ws, wingman, entry, order);
            }
            return;
        }

        double dx = padX - wingman.posX;
        double dz = padZ - wingman.posZ;
        double hDist = Math.sqrt(dx * dx + dz * dz);

        if (hDist > 20) {
            // 接近フェーズ: VTOL OFF (固定翼モード) で巡航高度を保ちながらヘリパッドへ向かう。
            // VTOL モード ON のままでは F-35B が前進できず後退・沈降してしまうため OFF が必須。
            // throttle=0.6: 0.8 では速すぎてパッドへの侵入が荒くなるため抑制。
            setVtolHoverMode(wingman, entry, false, "VTOL_LAND approach hDist=" + (int) hDist);
            setThrottle(wingman, 0.6);
            entry.autoTargetX = padX;
            entry.autoTargetZ = padZ;
            entry.autoTargetY = order.cruiseAlt;
            // 1秒毎に接近ログ
            if (entry.diagTick % 20 == 8) {
                McHeliWingman.logger.info("[VTOL_LAND] {} APPROACH hDist={} alt={}/{} padXZ=({},{})",
                    shortId(wingman), (int) hDist,
                    String.format("%.1f", wingman.posY), (int) order.cruiseAlt,
                    (int) padX, (int) padZ);
            }
        } else {
            // ─── ホバー＋降下フェーズ (hDist <= 20) ──────────────────────────────
            setVtolHoverMode(wingman, entry, true, "VTOL_LAND hDist=" + (int) hDist);

            // ヨー安定用XZターゲット (ホバー・降下両フェーズ共通)
            MarkerRegistry.MarkerInfo padB = (pad != null && !pad.baseId.isEmpty())
                ? MarkerRegistry.findByBase(ws, pad.baseId, MarkerType.HELIPAD_B) : null;
            if (padB != null) {
                double ddx = padB.pos.getX() + 0.5 - padX;
                double ddz = padB.pos.getZ() + 0.5 - padZ;
                double dlen = Math.sqrt(ddx * ddx + ddz * ddz);
                if (dlen > 1.0) {
                    entry.autoTargetX = padX - (ddx / dlen) * 50.0;
                    entry.autoTargetZ = padZ - (ddz / dlen) * 50.0;
                }
            } else {
                double facingRad = Math.toRadians(wingman.rotationYaw);
                entry.autoTargetX = padX + (-Math.sin(facingRad)) * 10.0;
                entry.autoTargetZ = padZ +   Math.cos(facingRad)  * 10.0;
            }

            entry.missionNodeTimer++;

            if (entry.missionNodeTimer < 80) {
                // ─── ホバー安定フェーズ: 80tick(4秒) 高度維持してから降下 ───────
                setThrottle(wingman, 0.5);
                entry.autoTargetY = wingman.posY;
                if (entry.diagTick % 20 == 8) {
                    McHeliWingman.logger.info("[VTOL_LAND] {} HOVER timer={}/80 hDist={} alt={}",
                        shortId(wingman), entry.missionNodeTimer, (int) hDist,
                        String.format("%.1f", wingman.posY));
                }
            } else {
                // ─── 垂直降下フェーズ ────────────────────────────────────────────
                // 高度比例スロットル制御: 地面に近づくほど throttle を上げて降下を緩める。
                //   30ブロック以上: 0.45（緩やかな降下開始）
                //   0ブロック:      0.57（ほぼホバー、極低速着地）
                // 機体ごとにホバースロットルが異なるため、motionY クランプと併用して確実に遅くする。
                double altAboveGround = Math.max(0, wingman.posY - groundY);
                double descentThrottle;
                if (altAboveGround > 30) {
                    descentThrottle = 0.45;
                } else {
                    descentThrottle = 0.45 + (30 - altAboveGround) / 30.0 * 0.12;
                }
                setThrottle(wingman, descentThrottle);
                entry.autoTargetY = groundY;
                if (entry.diagTick % 20 == 8) {
                    McHeliWingman.logger.info("[VTOL_LAND] {} DESCEND hDist={} alt={} groundY={} throttle={}",
                        shortId(wingman), (int) hDist,
                        String.format("%.1f", wingman.posY), (int) groundY,
                        String.format("%.2f", descentThrottle));
                }
                // 着地完了判定 → 5秒（100tick）待機後に TAXI_IN へ
                // onGround: McHeli 地上接地フラグ（より確実）
                // posY <= groundY + 1: 接地直前の余裕（onGround が遅延する場合の保険）
                boolean isOnGround = wingman.onGround || wingman.posY <= groundY + 1.0;
                if (isOnGround && hDist < 15) {
                    entry.missionNodeTimer = -100;  // 負値で着地後待機フェーズを開始
                    setThrottle(wingman, 0.0);
                    McHeliWingman.logger.info("[VTOL_LAND] {} LANDED at ({},{},{}) → waiting 5s before taxi",
                        shortId(wingman), (int) wingman.posX, (int) wingman.posY, (int) wingman.posZ);
                }
            }
        }
    }

    /**
     * VTOL着陸完了処理。
     * ヘリパッドID（assignedParkingId）を runwayId として TaxiRoute を逆引きし、
     * 帰還ルートがあれば TAXI_IN でパーキングへ戻る。なければ PARKED で完了。
     */
    private void finishVtolLanding(WorldServer ws, Entity wingman, WingmanEntry entry, MissionOrder order) {
        setThrottle(wingman, 0.0);
        // VTOL モード OFF: ノズルを前方に戻してタキシー可能な状態にする
        setVtolHoverMode(wingman, entry, false, "VTOL landing complete");

        // ヘリパッドID (= TaxiRoute.runwayId) でルートを逆引き
        String padId = entry.assignedParkingId;
        TaxiRoute route = (!padId.isEmpty()) ? TaxiRouteRegistry.findByRunway(ws, padId) : null;

        if (route != null) {
            // 安全チェック: 見つかったルートの runwayId が padId（ヘリパッドID）と一致しているか確認する。
            // 不一致の場合、findByRunway が出発ルートや別ルートを誤って返した可能性がある。
            if (!padId.equals(route.runwayId)) {
                McHeliWingman.logger.warn(
                    "[Order] {} finishVtolLanding: findByRunway('{}') returned route='{}' with runwayId='{}' — ID MISMATCH! "
                    + "Helipad and RUNWAY_A may share the same marker ID, or arrival route not registered. "
                    + "Falling back to PARKED. Fix: ensure arrival route (vtol-in) has runwayId='{}'.",
                    shortId(wingman), padId, route.routeId, route.runwayId, padId);
                route = null; // フォールバック: そのまま PARKED
            }
        }

        if (route != null) {
            // fullArrival() = [helipadId, wp..., parkingId] → そのまま TAXI_IN キューに積む
            entry.assignedParkingId = route.parkingId;      // 最終目的地をパーキングに更新
            entry.taxiWpQueue       = new ArrayList<>(route.fullArrival());
            entry.taxiWpIndex       = 0;
            // taxiPreX/Z を現在地に初期化しないと、Phase.START で WTH が taxiPreX を記録する前に
            // Phase.END の taxiPushPosition が古い値（前回 TAXI_OUT の滑走路付近）を参照し
            // 機体が瞬時に遠距離へテレポートするバグが発生する。
            entry.taxiPreX          = wingman.posX;
            entry.taxiPreZ          = wingman.posZ;
            entry.autoState         = AutonomousState.TAXI_IN;
            // order は TAXI_IN 完了時 (tickOrderTaxiIn) に null にされるため、ここでは保持する
            McHeliWingman.logger.info("[Order] {} VTOL landing complete → TAXI_IN route='{}' queue={} parking='{}' (pad='{}')",
                shortId(wingman), route.routeId, entry.taxiWpQueue, route.parkingId, padId);
            return;
        }

        // タキシールートなし → その場で駐機完了
        entry.autoState = AutonomousState.PARKED;
        entry.order     = null;
        applyParkingHeading(ws, entry);
        McHeliWingman.logger.info("[Order] {} VTOL landing complete (no taxi route). pos=({},{},{}) pad='{}'",
            shortId(wingman), (int) wingman.posX, (int) wingman.posY, (int) wingman.posZ, padId);
    }

    // ─── MissionOrder 共通ヘルパー ────────────────────────────────────────────

    /** 軌道旋回目標を設定する（CAP/CAS/ESCORT/RECON）。 */
    private void orbit(WingmanEntry entry, Entity wingman, MissionOrder order) {
        setThrottle(wingman, 1.0);
        entry.orbitAngle += 0.007;  // rad/tick → 約 900 tick で一周
        double r = order.orbitRadius;
        entry.autoTargetX = order.targetX + Math.cos(entry.orbitAngle) * r;
        entry.autoTargetY = order.cruiseAlt;
        entry.autoTargetZ = order.targetZ + Math.sin(entry.orbitAngle) * r;
    }

    /** 旋回エリア内の IMob 数をカウントして reconMobCount に累積する。 */
    private void countMobs(Entity wingman, MissionOrder order, WingmanEntry entry) {
        if (entry.orderTimer % 100 != 0) return; // 5秒ごとにカウント
        int count = 0;
        double r2 = order.orbitRadius * order.orbitRadius;
        for (Entity e : wingman.world.loadedEntityList) {
            if (!(e instanceof IMob) || e.isDead) continue;
            double dx = e.posX - order.targetX, dz = e.posZ - order.targetZ;
            if (dx * dx + dz * dz < r2) count++;
        }
        entry.reconMobCount = count;
    }

    /** RTBをトリガーして理由をログに残す。 */
    private void triggerRtb(Entity wingman, WingmanEntry entry, String reason) {
        entry.rtbReason    = reason;
        entry.autoState    = AutonomousState.RTB;
        entry.attackMode   = WingmanEntry.ATK_NONE;
        McHeliWingman.logger.info("[Order] {} RTB triggered: {}", shortId(wingman), reason);
        // RECON の場合は近くのプレイヤーに報告
        if (entry.order != null && entry.order.hasType(MissionType.RECON)) {
            for (net.minecraft.entity.player.EntityPlayer p : wingman.world.playerEntities) {
                if (p.getDistance(wingman) < 500) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§e[Recon] " + shortId(wingman) + ": §f" + entry.reconMobCount
                        + " mobs detected in target area (RTB: " + reason + ")"));
                }
            }
        }
    }

    /** ビンゴ燃料チェック。残燃料が BINGO_FUEL_RATIO 以下でtrue。 */
    private boolean isBingo(Entity wingman, MissionOrder order, WorldServer ws) {
        if (McheliReflect.hasInfiniteFuel(wingman)) {
            McHeliWingman.logger.debug("[Bingo] {} infinite fuel, skip", shortId(wingman));
            return false;
        }
        double fuel    = McheliReflect.getFuel(wingman);
        double maxFuel = McheliReflect.getMaxFuel(wingman);
        if (fuel < 0 || maxFuel <= 0) {
            McHeliWingman.logger.debug("[Bingo] {} fuel unreadable fuel={} max={}", shortId(wingman), fuel, maxFuel);
            return false;
        }
        double ratio = fuel / maxFuel;
        boolean bingo = ratio < BINGO_FUEL_RATIO;
        if (bingo) {
            // bingo 到達時のみ出力（毎tick spam を防ぐ）
            McHeliWingman.logger.info("[Bingo] {} fuel={}/{} ({}%) bingo=true",
                shortId(wingman), (int) fuel, (int) maxFuel,
                String.format("%.1f", ratio * 100));
        }
        return bingo;
    }

    /**
     * MissionOrder 用の RUNWAY_A/B を取得する。[0]=A, [1]=B。見つからなければ null。
     *
     * RUNWAY_A はタキシールートの runwayId から取得する（ルートが一意に指定するため）。
     * RUNWAY_B は TaxiRoute.runwayBId を優先し、空文字の場合は order.baseId で自動検索する。
     *   → TaxiRoute に runwayBId が設定されていれば findById で確実に取得できる。
     *   → 見つからない場合は warnログを出力して null を返す（呼び出し元が TRANSIT_TO に移行）。
     */
    private MarkerRegistry.MarkerInfo[] getOrderRunway(WorldServer ws,
            WingmanEntry entry, MissionOrder order) {
        // TaxiRoute から runwayId / runwayBId を使う
        // departureRouteId が設定されていれば findById で確実に出発ルートを取得する。
        TaxiRoute route = null;
        if (!entry.departureRouteId.isEmpty()) {
            route = TaxiRouteRegistry.findById(ws, entry.departureRouteId);
        } else if (!entry.assignedParkingId.isEmpty()) {
            route = TaxiRouteRegistry.findByParking(ws, entry.assignedParkingId);
        }
        MarkerRegistry.MarkerInfo rwyA, rwyB;
        String logCtx = "base='" + order.baseId + "' route='" + entry.departureRouteId + "'";
        if (route != null) {
            rwyA = MarkerRegistry.findById(ws, MarkerType.RUNWAY_A, route.runwayId);
            if (rwyA == null) {
                McHeliWingman.logger.warn("[Order] getOrderRunway: RUNWAY_A '{}' not found ({})",
                    route.runwayId, logCtx);
            }
        } else {
            rwyA = MarkerRegistry.findByBase(ws, order.baseId, MarkerType.RUNWAY_A);
            if (rwyA == null) {
                McHeliWingman.logger.warn("[Order] getOrderRunway: RUNWAY_A not found by baseId ({})", logCtx);
            }
        }
        // RUNWAY_B: TaxiRoute.runwayBId が設定されていれば ID で直接取得、なければ baseId 検索にフォールバック
        if (route != null && !route.runwayBId.isEmpty()) {
            rwyB = MarkerRegistry.findById(ws, MarkerType.RUNWAY_B, route.runwayBId);
            if (rwyB == null) {
                McHeliWingman.logger.warn("[Order] getOrderRunway: RUNWAY_B '{}' not found ({}); falling back to baseId search",
                    route.runwayBId, logCtx);
                rwyB = MarkerRegistry.findByBase(ws, order.baseId, MarkerType.RUNWAY_B);
            }
        } else {
            rwyB = MarkerRegistry.findByBase(ws, order.baseId, MarkerType.RUNWAY_B);
        }
        if (rwyB == null) {
            McHeliWingman.logger.warn(
                "[Order] getOrderRunway: RUNWAY_B not found ({}). " +
                "Set runwayBId in the route (Taxi Routes GUI) or set baseId='{}' on a RUNWAY_B marker. " +
                "Takeoff will be skipped → airborne start.",
                logCtx, order.baseId);
        }
        if (rwyA == null || rwyB == null) return null;
        return new MarkerRegistry.MarkerInfo[]{rwyA, rwyB};
    }

    /**
     * ALIGN → TAKEOFF_ROLL → CLIMB を実行する共有ヘルパー。
     * CLIMBが目標高度に到達したら true を返す。
     * entry.autoState は内部で書き換える（ALIGN→TAKEOFF_ROLL→CLIMB）。
     */
    private boolean tickAlignToClimb(WorldServer ws, Entity wingman, WingmanEntry entry,
            MarkerRegistry.MarkerInfo rwyA, MarkerRegistry.MarkerInfo rwyB, double targetAlt) {

        double ax = rwyA.pos.getX() + 0.5, ay = rwyA.pos.getY(), az = rwyA.pos.getZ() + 0.5;
        double bx = rwyB.pos.getX() + 0.5,                       bz = rwyB.pos.getZ() + 0.5;
        double rdx = bx - ax, rdz = bz - az;
        double rlen = Math.sqrt(rdx * rdx + rdz * rdz);
        if (rlen < 1) return true;
        double dirX = rdx / rlen, dirZ = rdz / rlen;
        float runwayYaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));

        switch (entry.autoState) {
            case ALIGN: {
                setThrottle(wingman, 0.0);
                entry.autoTargetX = wingman.posX + dirX * 1000;
                entry.autoTargetY = ay;
                entry.autoTargetZ = wingman.posZ + dirZ * 1000;

                // ─── 接地待ち ────────────────────────────────────────────────────
                // TAXI_OUT→ALIGN 移行直後は機体が空中にある場合がある（スポーン位置が高い、
                // VTOL物理で浮いている等）。posY が滑走路面（ay+2）より高い間は
                // タイマーを進めず、motionY を負にして McHeli 物理より強制降下させる。
                // AFH は WTH より後に Phase.START を処理するため motionY を上書きできる。
                double heightAboveRwy = wingman.posY - ay;
                if (heightAboveRwy > 2.5) {
                    wingman.motionY = -0.25;   // 強制降下（約 5 blocks/sec）
                    if (entry.diagTick % 20 == 3) {
                        McHeliWingman.logger.info("[ALIGN] {} waiting for landing: aboveRwy={}",
                            shortId(wingman), String.format("%.1f", heightAboveRwy));
                    }
                    // タイマーは進めない（接地するまで ALIGN を継続）
                    return false;
                }

                entry.missionNodeTimer++;
                float yawDiff = runwayYaw - wingman.rotationYaw;
                while (yawDiff >  180f) yawDiff -= 360f;
                while (yawDiff < -180f) yawDiff += 360f;
                boolean aligned  = Math.abs(yawDiff) < ALIGN_TOLERANCE;
                double  alignSpd = Math.sqrt(wingman.motionX * wingman.motionX + wingman.motionZ * wingman.motionZ);
                boolean slow     = alignSpd < 0.4;
                // 接地確認後、最低 20tick(1秒) 静止してからタイムアウト判定。
                // 1tick で即 TAKEOFF_ROLL に遷移するのを防ぎ、McHeli 物理が機体を
                // 地面に落ち着かせる時間を確保する。
                boolean settled  = entry.missionNodeTimer >= 20;
                boolean timedOut = entry.missionNodeTimer > 80;
                if (settled && ((aligned && slow) || timedOut)) {
                    forceYaw(wingman, runwayYaw);
                    entry.missionNodeTimer = 0;
                    entry.groundRollY = wingman.posY; // 地上高さを記録（Phase.END で拘束に使用）
                    entry.autoState = AutonomousState.TAKEOFF_ROLL;
                    McHeliWingman.logger.info(
                        "[Order] {} ALIGN→TAKEOFF_ROLL aligned={} timedOut={} yawDiff={} posY={} rwyY={} aboveRwy={} groundRollY={}",
                        shortId(wingman), aligned, timedOut, String.format("%.1f", yawDiff),
                        String.format("%.1f", wingman.posY), String.format("%.1f", ay),
                        String.format("%.1f", heightAboveRwy),
                        String.format("%.1f", entry.groundRollY));
                }
                return false;
            }
            case TAKEOFF_ROLL: {
                // 実機再現離陸:
                //   RT_A から全力加速し、RT_B 手前 ROTATION_DIST (50m) に入ったら
                //   徐々にピッチアップ。RT_B 付近で離陸する。
                //
                //   [グランドロール]          [ローテーション]    [離陸]
                //   RT_A ─── 全力加速・ピッチ0° ─── RT_B-50m ─── RT_B ───→
                //                                     └─ 徐々にピッチアップ ─┘
                setThrottle(wingman, 1.0);
                forceYaw(wingman, runwayYaw);
                entry.missionNodeTimer++;

                // RT_B までの残り距離
                double projFromA = (wingman.posX - ax) * dirX + (wingman.posZ - az) * dirZ;
                double distToB   = rlen - projFromA;

                // グランドロールフェーズ: 最初の60tick(3秒)は目標高度を滑走路面に維持。
                // distToB ではなくタイマーで判定することで、滑走路が短く最初から
                // ROTATION_DIST 内にある場合でも確実に地上滑走フェーズを確保する。
                boolean inGroundRollPhase = entry.missionNodeTimer <= 60;
                if (distToB > ROTATION_DIST || inGroundRollPhase) {
                    // グランドロール: 目標高度 = 滑走路面 (ピッチアップしない)
                    centerlineTarget(entry, wingman, ax, ay, az, dirX, dirZ, LOOKAHEAD_DIST);
                } else {
                    // ローテーション: RT_B に近づくほど目標高度が上がる
                    // factor: 0.0（RT_B まで ROTATION_DIST）→ 1.0（RT_B 到達）
                    double factor      = Math.max(0.0, Math.min(1.0, 1.0 - distToB / ROTATION_DIST));
                    double rotTargetY  = ay + factor * (targetAlt - ay);
                    centerlineTarget(entry, wingman, ax, rotTargetY, az, dirX, dirZ, LOOKAHEAD_DIST);
                }

                // 診断ログ（20tickごと）: 滑走中か空中かを確認
                if (entry.missionNodeTimer % 20 == 1) {
                    double spd = Math.sqrt(wingman.motionX * wingman.motionX + wingman.motionZ * wingman.motionZ);
                    McHeliWingman.logger.info(
                        "[TAKEOFF_ROLL] {} timer={} posY={} rwyY={} aboveRwy={} distToB={} speed={}",
                        shortId(wingman), entry.missionNodeTimer,
                        String.format("%.1f", wingman.posY), String.format("%.1f", ay),
                        String.format("%.1f", wingman.posY - ay),
                        String.format("%.1f", distToB),
                        String.format("%.2f", spd));
                }

                // 離陸判定: ローテーション開始後に 3m 以上浮いたら CLIMB へ。
                // minRollTicks: 最低60tick(3秒)は地上滑走させる。
                //   → F-35Bのような高推力機でも可視的な地上滑走フェーズを確保。
                //   → timer<60 の間は inRotation かつ浮いていても CLIMB に遷移しない。
                // タイムアウト: 200tick（10秒）で強制 CLIMB（滑走路が極端に短い場合など）。
                boolean inRotation  = distToB <= ROTATION_DIST;
                boolean minRollTime = entry.missionNodeTimer >= 60;
                if ((inRotation && wingman.posY > ay + 3 && minRollTime) || entry.missionNodeTimer > 200) {
                    entry.autoState = AutonomousState.CLIMB;
                    McHeliWingman.logger.info("[Order] {} TAKEOFF_ROLL→CLIMB posY={} ay={} distToB={} timer={}",
                        shortId(wingman), String.format("%.1f", wingman.posY),
                        String.format("%.1f", ay), String.format("%.1f", distToB),
                        entry.missionNodeTimer);
                }
                return false;
            }
            case CLIMB: {
                setThrottle(wingman, 1.0);
                double climbTargetY = Math.min(targetAlt, wingman.posY + 15.0);
                centerlineTarget(entry, wingman, ax, climbTargetY, az, dirX, dirZ, LOOKAHEAD_DIST);
                if (wingman.posY >= targetAlt - 5) {
                    return true; // 上昇完了
                }
                return false;
            }
            default:
                // 予期しない状態 → ALIGN にリセット
                entry.autoState = AutonomousState.ALIGN;
                entry.missionNodeTimer = 0;
                return false;
        }
    }

    /**
     * DESCEND → CIRCUIT_* → LANDING を実行する共有ヘルパー。
     * LANDING で速度が LANDING_SPEED 以下になったら true を返す。
     */
    private boolean tickLandingCircuit(WorldServer ws, Entity wingman, WingmanEntry entry,
            MarkerRegistry.MarkerInfo rwyA, MarkerRegistry.MarkerInfo rwyB) {

        double ax = rwyA.pos.getX() + 0.5, ay = rwyA.pos.getY(), az = rwyA.pos.getZ() + 0.5;
        double bx = rwyB.pos.getX() + 0.5, by = rwyB.pos.getY(), bz = rwyB.pos.getZ() + 0.5;
        double rdx = bx - ax, rdz = bz - az;
        double rlen = Math.sqrt(rdx * rdx + rdz * rdz);
        if (rlen < 1) return true;
        double dirX = rdx / rlen, dirZ = rdz / rlen;
        double perpX = -dirZ, perpZ = dirX;
        double circuitY = ay + 80; // RT_A の Y 座標を基準に 80 ブロック上空

        switch (entry.autoState) {
            case DESCEND: {
                // 降下目標: B端横（B→Aアプローチのため、まずB端に降下してから折り返す）
                double descThr = 0.65 + (circuitY - wingman.posY) * 0.02;
                setThrottle(wingman, Math.max(0.35, Math.min(0.9, descThr)));
                double epX = bx + perpX * CIRCUIT_OFFSET;
                double epZ = bz + perpZ * CIRCUIT_OFFSET;
                entry.autoTargetX = epX; entry.autoTargetY = circuitY; entry.autoTargetZ = epZ;
                double descDist = Math.sqrt(Math.pow(epX - wingman.posX, 2) + Math.pow(epZ - wingman.posZ, 2));
                if (entry.diagTick % 200 == 10) McHeliWingman.logger.info(
                    "[Circuit] {} DESCEND dist={} alt={} circuitY={}", shortId(wingman),
                    (int)descDist, String.format("%.1f", wingman.posY), (int)circuitY);
                if (descDist < 30) {
                    entry.autoState = AutonomousState.CIRCUIT_DOWNWIND;
                    McHeliWingman.logger.info("[Circuit] {} → CIRCUIT_DOWNWIND", shortId(wingman));
                }
                return false;
            }
            case CIRCUIT_DOWNWIND: {
                // ダウンウィンド: A端手前250m横（A→Bの逆方向に250m）
                double dwThr = 0.65 + (circuitY - wingman.posY) * 0.02;
                setThrottle(wingman, Math.max(0.4, Math.min(0.9, dwThr)));
                double dwX = ax - dirX * CIRCUIT_FINAL_DIST + perpX * CIRCUIT_OFFSET;
                double dwZ = az - dirZ * CIRCUIT_FINAL_DIST + perpZ * CIRCUIT_OFFSET;
                entry.autoTargetX = dwX; entry.autoTargetY = circuitY; entry.autoTargetZ = dwZ;
                entry.missionNodeTimer++;
                double dwDist = Math.sqrt(Math.pow(dwX - wingman.posX, 2) + Math.pow(dwZ - wingman.posZ, 2));
                if (entry.diagTick % 200 == 10) McHeliWingman.logger.info(
                    "[Circuit] {} DOWNWIND dist={} alt={}", shortId(wingman),
                    (int)dwDist, String.format("%.1f", wingman.posY));
                if (dwDist < 30) {
                    entry.missionNodeTimer = 0;
                    entry.autoState = AutonomousState.CIRCUIT_BASE;
                    McHeliWingman.logger.info("[Circuit] {} → CIRCUIT_BASE", shortId(wingman));
                }
                return false;
            }
            case CIRCUIT_BASE: {
                // ベース: A端手前250mのセンターライン（ファイナルへの折り返し点）
                double bsThr = 0.65 + (circuitY - wingman.posY) * 0.02;
                setThrottle(wingman, Math.max(0.4, Math.min(0.9, bsThr)));
                double fX = ax - dirX * CIRCUIT_FINAL_DIST;
                double fZ = az - dirZ * CIRCUIT_FINAL_DIST;
                entry.autoTargetX = fX; entry.autoTargetY = circuitY; entry.autoTargetZ = fZ;
                entry.missionNodeTimer++;
                double bsDist = Math.sqrt(Math.pow(fX - wingman.posX, 2) + Math.pow(fZ - wingman.posZ, 2));
                if (entry.diagTick % 200 == 10) McHeliWingman.logger.info(
                    "[Circuit] {} BASE dist={} alt={}", shortId(wingman),
                    (int)bsDist, String.format("%.1f", wingman.posY));
                if (bsDist < 40) {
                    entry.missionNodeTimer = 0;
                    entry.autoState = AutonomousState.CIRCUIT_FINAL;
                    McHeliWingman.logger.info("[Circuit] {} → CIRCUIT_FINAL", shortId(wingman));
                }
                return false;
            }
            case CIRCUIT_FINAL: {
                // ファイナル: A端に向かってアプローチ（B→A方向）。タッチダウンはA端。
                // projFromA: A端より「B→A方向」にどれだけ離れているか（正 = A端手前 = アプローチ中）
                if (entry.diagTick % 200 == 10) McHeliWingman.logger.info(
                    "[Circuit] {} FINAL alt={} spd=({},{},{})", shortId(wingman),
                    String.format("%.1f", wingman.posY),
                    String.format("%.2f", wingman.motionX),
                    String.format("%.2f", wingman.motionY),
                    String.format("%.2f", wingman.motionZ));
                double projFromA = (wingman.posX - ax) * (-dirX) + (wingman.posZ - az) * (-dirZ);
                double tProj     = Math.max(0, projFromA - LOOKAHEAD_DIST);
                double touchdownAlt = ay + 1;
                double glideAlt = touchdownAlt + Math.max(0, projFromA)
                    * ((circuitY - touchdownAlt) / CIRCUIT_FINAL_DIST);
                glideAlt = Math.max(touchdownAlt, Math.min(circuitY, glideAlt));
                boolean onGround = wingman.posY <= ay + 1;
                double gsError   = wingman.posY - glideAlt;
                // 目標: A端 + B→A方向にtProj先 (= ax + (-dirX)*tProj)
                if (onGround) {
                    setThrottle(wingman, 0.0);
                    entry.autoTargetX = ax + (-dirX) * tProj;
                    entry.autoTargetY = ay - 10;
                    entry.autoTargetZ = az + (-dirZ) * tProj;
                } else {
                    // グライドスロープ誤差に基づくスロットル制御
                    // Phase.END の motionY 追従と組み合わせて機体を精密に誘導する。
                    // 旧: projFromA < 80 でスロットルを強制的に下げていたが、
                    //     これが過剰降下の原因だったため廃止。
                    double thr = 0.55 - gsError * 0.02;
                    setThrottle(wingman, Math.max(0.1, Math.min(0.75, thr)));
                    entry.autoTargetX = ax + (-dirX) * tProj;
                    entry.autoTargetY = glideAlt;
                    entry.autoTargetZ = az + (-dirZ) * tProj;
                }
                entry.missionNodeTimer++;
                double hDist = Math.sqrt(Math.pow(ax - wingman.posX, 2) + Math.pow(az - wingman.posZ, 2));
                // タッチダウン位置の厳格化: RT_A の 8ブロック以内（かつ滑走路高さ +6 以下）に
                // 到達したときのみ LANDING へ遷移する。
                // 旧来の「接地したら 60 ブロック以内で即遷移」を廃止し、
                // 手前タッチダウン後も RT_A まで地上ロールで転がるようにする。
                if ((hDist < 8.0 && wingman.posY <= ay + 6) || projFromA <= 0) {
                    entry.autoState = AutonomousState.LANDING;
                    // タッチダウン時の水平速度を landingRollSpeed に記録し、
                    // WTH の taxiPushPosition が指数減衰で徐々に減速する。
                    double hSpd = Math.sqrt(wingman.motionX * wingman.motionX
                                          + wingman.motionZ * wingman.motionZ);
                    entry.landingRollSpeed = Math.max(hSpd, 0.5);
                    McHeliWingman.logger.info("[Circuit] {} LANDING at A-end hDist={} posY={} projFromA={} initSpd={}",
                        shortId(wingman), String.format("%.1f", hDist),
                        String.format("%.1f", wingman.posY), String.format("%.1f", projFromA),
                        String.format("%.3f", entry.landingRollSpeed));
                }
                return false;
            }
            case LANDING: {
                // タッチダウン後: B端（固定座標）を目標に taxiPushPosition で転がる。
                // isTaxiingState(LANDING)=true なので WTH が taxiPushPosition を呼ぶ。
                setThrottle(wingman, 0.0);
                entry.autoTargetX = bx;
                entry.autoTargetY = ay - 10;
                entry.autoTargetZ = bz;
                double distToB = Math.sqrt(Math.pow(bx - wingman.posX, 2) + Math.pow(bz - wingman.posZ, 2));
                if (entry.diagTick % 100 == 10) McHeliWingman.logger.info(
                    "[Circuit] {} LANDING distToB={} pos=({},{})",
                    shortId(wingman), String.format("%.1f", distToB),
                    String.format("%.1f", wingman.posX), String.format("%.1f", wingman.posZ));
                if (distToB < TOUCHDOWN_DIST) {
                    McHeliWingman.logger.info("[Order] {} stopped near B-end at ({},{},{})",
                        shortId(wingman), (int)wingman.posX, (int)wingman.posY, (int)wingman.posZ);
                    return true;
                }
                return false;
            }
            default:
                entry.autoState = AutonomousState.DESCEND;
                return false;
        }
    }

    /** マーカーIDからブロック座標を返す（型問わず最初にIDが一致したもの）。 */
    private BlockPosXZ resolveAnyMarker(WorldServer ws, String markerId) {
        return resolveAnyMarker(ws, markerId, "");
    }

    /**
     * マーカーIDからXZ座標を返す（型問わず）。
     * baseId が空でない場合、まず baseId が一致するものを優先する。
     */
    private BlockPosXZ resolveAnyMarker(WorldServer ws, String markerId, String baseId) {
        BlockPosXZ fallback = null;
        for (MarkerRegistry.MarkerInfo m : MarkerRegistry.snapshot(ws)) {
            if (!markerId.equals(m.id)) continue;
            if (!baseId.isEmpty() && baseId.equals(m.baseId))
                return new BlockPosXZ(m.pos.getX(), m.pos.getZ()); // 完全一致
            if (fallback == null)
                fallback = new BlockPosXZ(m.pos.getX(), m.pos.getZ()); // フォールバック候補
        }
        return fallback;
    }

    /** マーカーIDから MarkerInfo を返す（型問わず）。見つからなければ null。 */
    private MarkerRegistry.MarkerInfo findMarkerInfoById(WorldServer ws, String markerId) {
        return findMarkerInfoById(ws, markerId, "");
    }

    /**
     * マーカーIDから MarkerInfo を返す（型問わず）。
     * baseId が空でない場合、まず baseId が一致するものを優先する（完全一致優先）。
     * 一致しなければ baseId を無視した最初のヒットを返す（後方互換フォールバック）。
     */
    private MarkerRegistry.MarkerInfo findMarkerInfoById(WorldServer ws, String markerId, String baseId) {
        if (markerId == null || markerId.isEmpty()) return null;
        MarkerRegistry.MarkerInfo fallback = null;
        for (MarkerRegistry.MarkerInfo m : MarkerRegistry.snapshot(ws)) {
            if (!markerId.equals(m.id)) continue;
            if (!baseId.isEmpty() && baseId.equals(m.baseId)) return m; // 完全一致
            if (fallback == null) fallback = m;                          // フォールバック候補
        }
        return fallback;
    }

    /** XZ のみの軽量な座標ペア。 */
    private static class BlockPosXZ {
        final int x, z;
        BlockPosXZ(int x, int z) { this.x = x; this.z = z; }
    }

    // ─── ATTACK ──────────────────────────────────────────────────────────────

    private void tickAttack(WorldServer ws, Entity wingman, WingmanEntry entry, MissionNode node) {
        entry.autoState = AutonomousState.ATTACK;
        // 半径内に敵がいる間は ATK_AUTO を維持、いなくなったら次ノードへ
        boolean hasTarget = false;
        for (Entity e : wingman.world.loadedEntityList) {
            if (!(e instanceof IMob) || e.isDead) continue;
            if (wingman.getDistanceSq(e) < node.attackRadius * node.attackRadius) { hasTarget = true; break; }
        }
        entry.attackMode = hasTarget ? WingmanEntry.ATK_AUTO : WingmanEntry.ATK_NONE;
        if (!hasTarget) {
            McHeliWingman.logger.info("[Auto] {} ATTACK node complete (no targets in radius)", shortId(wingman));
            entry.advanceMission();
        }
    }

    // ─── LOITER ──────────────────────────────────────────────────────────────

    private void tickLoiter(WorldServer ws, Entity wingman, WingmanEntry entry, MissionNode node) {
        entry.autoState = AutonomousState.LOITER;
        entry.missionNodeTimer++;
        // 旋回: 現在位置を中心に半径20ブロックで旋回するため、目標点を回転させる
        double angle = (entry.missionNodeTimer * 2.0) * Math.PI / 200.0;  // 200tickで一周
        entry.autoTargetX = wingman.posX + Math.cos(angle) * 20;
        entry.autoTargetY = wingman.posY;
        entry.autoTargetZ = wingman.posZ + Math.sin(angle) * 20;
        if (entry.missionNodeTimer >= node.durationTicks) {
            McHeliWingman.logger.info("[Auto] {} LOITER complete", shortId(wingman));
            entry.advanceMission();
        }
    }

    // ─── PARK ────────────────────────────────────────────────────────────────

    private void tickPark(WorldServer ws, Entity wingman, WingmanEntry entry, MissionNode node) {
        entry.autoState = AutonomousState.TAXI_IN;
        MarkerRegistry.MarkerInfo parking = MarkerRegistry.resolveMarker(ws, node.parkingId, MarkerType.PARKING);
        if (parking == null) { entry.advanceMission(); return; }
        double tx = parking.pos.getX() + 0.5;
        double tz = parking.pos.getZ() + 0.5;
        entry.autoTargetX = tx;
        entry.autoTargetY = wingman.posY;
        entry.autoTargetZ = tz;
        setThrottle(wingman, 0.0); // WTH の taxiPushPosition() が地上移動を担う
        double dist = Math.sqrt(Math.pow(tx - wingman.posX, 2) + Math.pow(tz - wingman.posZ, 2));
        if (dist < PARK_DIST) {
            entry.autoState = AutonomousState.PARKED;
            McHeliWingman.logger.info("[Auto] {} PARKED at {}", shortId(wingman), node.parkingId);
            entry.advanceMission();
        }
    }

    // ─── Throttle helper ─────────────────────────────────────────────────────

    // ─── Yaw / Pitch force helpers ───────────────────────────────────────────

    /** ヨーを直接セットして WingmanTickHandler のレートリミットを bypass する。 */
    private void forceYaw(Entity aircraft, float yaw) {
        if (aircraft instanceof MCH_EntityAircraft ac) ac.setRotYaw(yaw);
    }

    /**
     * ピッチを直接セットして McHeli 物理後のリセットを上書きする。
     * TAKEOFF_ROLL ローテーションゾーンで Phase.END に呼び出すことで、
     * McHeli が次 tick の onUpdate_Server で機首上げを認識して揚力を発生させる。
     * McHeli 規約: 負 = 機首上げ（pitch = -20 で 20° ノーズアップ）。
     */
    private void forcePitch(Entity aircraft, float pitch) {
        if (aircraft instanceof MCH_EntityAircraft ac) ac.setRotPitch(pitch);
    }

    /**
     * ロールを 0° に強制する。LANDING / CIRCUIT_FINAL 接地後の姿勢水平化に使用。
     */
    private void forceRoll(Entity aircraft, float roll) {
        if (aircraft instanceof MCH_EntityAircraft ac) ac.setRotRoll(roll);
    }

    // ─── Throttle helper ─────────────────────────────────────────────────────

    private void setThrottle(Entity aircraft, double throttle) {
        if (aircraft instanceof MCH_EntityAircraft ac) ac.setCurrentThrottle(throttle);
    }

    private static String shortId(Entity e) {
        return e.getUniqueID().toString().substring(0, 8);
    }

    // ─── 駐機方位ヘルパー ─────────────────────────────────────────────────────

    /**
     * TaxiRoute の parkingHeading を WingmanEntry.parkingHeadingYaw (Minecraft yaw 度数) に変換して設定。
     * departureRouteId → findById → parkingId → findByParking の順で検索。
     * heading が -1（任意）なら NaN のまま（強制なし）。
     */
    /**
     * 駐機スポットマーカーの parkingHeading を WingmanEntry.parkingHeadingYaw に変換して設定。
     * PARKING マーカーの parkingHeading を直接参照（マーカーブロック GUI で設定）。
     * heading が -1（任意）なら NaN のまま（強制なし）。
     */
    private void applyParkingHeading(WorldServer ws, WingmanEntry entry) {
        if (!entry.assignedParkingId.isEmpty()) {
            for (MarkerRegistry.MarkerInfo m : MarkerRegistry.snapshot(ws)) {
                if (m.type == MarkerType.PARKING && entry.assignedParkingId.equals(m.id)) {
                    if (m.parkingHeading >= 0) {
                        entry.parkingHeadingYaw = compassToYaw(m.parkingHeading);
                        return;
                    }
                    break;
                }
            }
        }
        entry.parkingHeadingYaw = Float.NaN;
    }

    /**
     * コンパス方位インデックス → Minecraft yaw (度数) 変換。
     * Minecraft yaw: 0=南(+Z), 90=西(-X), 180=北(-Z), -90=東(+X)
     */
    private static float compassToYaw(int heading) {
        switch (heading) {
            case 0: return 180f;  // 北 → -Z
            case 1: return -90f;  // 東 → +X
            case 2: return   0f;  // 南 → +Z
            case 3: return  90f;  // 西 → -X
            default: return 0f;
        }
    }
}
