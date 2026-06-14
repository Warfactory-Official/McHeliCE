package com.norwood.mcheli.wingman.handler;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.weapon.MCH_WeaponParam;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wingman.McHeliWingman;
import com.norwood.mcheli.wingman.config.WingmanConfig;
import com.norwood.mcheli.wingman.mission.AutonomousState;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import com.norwood.mcheli.wingman.wingman.WingmanState;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Per-tick wingman logic.
 *
 * 設計方針:
 *   機体本来の飛行物理（McHeli）を尊重し、setRotYaw/setRotPitch をレート制限付きで操作して誘導する。
 *   velocity/motion は直接書き換えない。
 *
 * Phase.START: エンジン・姿勢角セット（McHeli更新前）
 * Phase.END  : 攻撃発射・ギア同期
 */
public class WingmanTickHandler {

    // 到達判定距離
    private static final double ARRIVAL_THRESHOLD = 5.0;
    // 停止ホールド判定距離（親機停止時にこの距離以内なら停止待機）
    private static final double HOLD_THRESHOLD = 25.0;
    // 親機停止判定速度（ブロック/tick）
    private static final double LEADER_STOP_SPEED = 0.4;
    // 自動攻撃索敵レンジ
    // NOTE: orbit半径のデフォルト300ブロックをカバーするため400に設定。
    //       旋回軌道(半径300)上から目標中心のゾンビ距離 ≈ 300 > 200 で届かなかった問題を修正。
    private static final double AUTO_ATTACK_RANGE = 400.0;
    // 発射後の逃避時間（tick）— この間は現在ヨーを維持してエスケープ
    private static final int POST_FIRE_ESCAPE_TICKS = 40;

    // 武器種別スタンドオフ距離（接敵目標水平距離）— 近づき過ぎない
    private static double standoffForWeapon(String type) {
        if (type == null) return 80.0;
        switch (type.toLowerCase()) {
            case "gun": case "machinegun": case "machinegun1": case "machinegun2":
            case "cannon":                return 50.0;   // 近距離で精密掃射
            case "cas":                   return 60.0;
            case "rocket": case "mkrocket": return 80.0; // 120→80: より近くから発射
            case "missile": case "aamissile": case "atmissile": case "tvmissile":
                                          return 200.0;
            case "asmissile":             return 280.0;
            case "bomb":                  return 180.0;
            case "torpedo":               return 40.0;
            default:                      return 80.0;
        }
    }

    // 武器種別高度オフセット（ターゲットY + この値 が目標高度）
    private static double altOffsetForWeapon(String type) {
        if (type == null) return 50.0;
        switch (type.toLowerCase()) {
            case "gun": case "machinegun": case "machinegun1": case "machinegun2":
            case "cannon":   return 40.0;
            case "cas":      return 50.0;
            case "rocket": case "mkrocket": return 60.0;
            case "missile": case "aamissile": case "atmissile": case "tvmissile":
                             return 80.0;
            case "asmissile": return 200.0;
            case "bomb":     return 200.0;   // 十分な高度から投下
            case "torpedo":  return 5.0;
            default:         return 50.0;
        }
    }

    // 武器種別発射有効レンジ（この距離以内で発射）
    private static double fireRangeForWeapon(String type) {
        if (type == null) return 150.0;
        switch (type.toLowerCase()) {
            case "gun": case "machinegun": case "machinegun1": case "machinegun2":
            case "cannon":   return 120.0;
            case "cas":      return 140.0;
            case "rocket": case "mkrocket": return 200.0;
            case "missile": case "aamissile": case "atmissile": case "tvmissile":
                             return 300.0;
            case "asmissile": return 380.0;
            case "bomb":     return 350.0;
            case "torpedo":  return 80.0;
            default:         return 150.0;
        }
    }

    // 武器種別クールダウン(tick)
    private static int fireCooldownForType(String type) {
        if (type == null) return 10;
        switch (type.toLowerCase()) {
            case "gun": case "machinegun": case "machinegun1": case "machinegun2": return 2;
            case "cas":      return 3;
            case "cannon":   return 5;
            case "rocket": case "mkrocket": return 10;
            case "aamissile": case "atmissile": case "missile": return 20;
            case "asmissile": return 60;   // 対地ミサイルは1発打ったら間隔を広く
            case "tvmissile": return 30;
            case "bomb":     return 80;    // 爆弾は爆発回避のため間隔を大きく
            case "torpedo":  return 30;
            default:         return 10;
        }
    }

    // 旋回レート制限 (°/tick) — 固定翼は緩やかに、ヘリは機動性高め
    private static final float MAX_YAW_RATE_PLANE = 3.0f;
    private static final float MAX_YAW_RATE_HELI  = 4.0f;
    // ピッチレート制限 (°/tick)
    private static final float MAX_PITCH_RATE = 3.0f;
    // ロール（バンク角）レート制限 (°/tick)
    private static final float MAX_ROLL_RATE = 5.0f;
    // 最大ピッチ角
    private static final float MAX_PITCH_UP   = 25.0f;
    private static final float MAX_PITCH_DOWN = -20.0f;

    // ファイアレートの自前管理
    private final java.util.Map<UUID, Integer> fireCooldowns = new java.util.HashMap<>();
    // 発射後エスケープカウンタ（残りtick > 0 中は現在ヨーを維持してエスケープ飛行）
    private final java.util.Map<UUID, Integer> postFireEscape = new java.util.HashMap<>();
    // デバッグログ抑制カウンタ（20tickに1回だけ出力）
    private int boostDebugTick = 0;

    // ─── Phase.START ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onWorldTickStart(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.START) return;
        WorldServer ws = (WorldServer) event.world;

        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            Entity wingman = ws.getEntityFromUuid(e.getKey());
            if (wingman == null || wingman.isDead) continue;
            WingmanEntry entry = e.getValue();

            // VTOL/固定翼: 状態に応じてVTOLモードを制御
            // ヘリコプター以外かつ VTOL 機能を持つ機体のみ処理する
            if (!isHelicopter(wingman) && com.norwood.mcheli.wingman.util.McheliReflect.isVtol(wingman)) {
                // vtolNozzleMode を毎tick更新 (0=格納完了, 1=ノズル回転中, 2=90°展開完了)
                // McHeli の getVtolMode():int を読み取り WingmanEntry に反映 → AFH が参照してスロットルを制御
                entry.vtolNozzleMode = (wingman instanceof MCH_EntityPlane p) ? p.getVtolMode() : 0;

                boolean needsVtol = entry.isAutonomous() && entry.vtolHoverMode;

                // VTOL ON/OFF 制御: vtolOnSent フラグで 1 サイクルに 1 回だけ送信する。
                // ─── 問題: swithVtolMode が no-arg トグルの場合、毎 tick 呼ぶと
                //   ON → OFF → ON → OFF ... と交互してノズルが完成しない。
                //   また getVtolMode() がノズル回転中も 0 を返す実装では
                //   vtolNozzleMode==0 条件が毎 tick 成立してしまう。
                // ─── 解決: vtolHoverMode が変わったときのみ 1 回だけコマンドを送る。
                //   vtolOnSent は AFH が vtolHoverMode を変更するたびに false にリセットする。
                if (needsVtol && !entry.vtolOnSent) {
                    forceVtolOn(wingman);
                    entry.vtolOnSent = true;
                } else if (!needsVtol && entry.vtolOnSent) {
                    forceVtolOff(wingman);
                    entry.vtolOnSent = false;
                }
            }

            if (entry.attackMode != WingmanEntry.ATK_NONE) {
                // 攻撃中: 全力スロットル
                maintainEngine(wingman, 1.0);
                steerToTarget(ws, wingman, entry);
            } else if (entry.isAutonomous()) {
                // ─── 搭乗プレイヤーの rotationYaw を目標方向に上書き ─────────────
                // McHeli の onUpdate_Server は毎 tick の冒頭で
                //   aircraft.lastRiderYaw = rider.rotationYaw
                // を実行し、その値で機体の向き（レンダリング含む）を決定する。
                // McHeli は IEntitySinglePassenger 独自実装のため getPassengers() が空。
                // → getMcheliRider() で getRiddenByEntity() を使い乗員を取得する。
                // → Phase.START（ネットワーク後・エンティティ更新前）で rider.rotationYaw を
                //   目標方向に上書きすることで、onUpdate_Server が正しい方向を読む。
                Entity mcheliRider = getMcheliRider(wingman);
                if (mcheliRider instanceof net.minecraft.entity.player.EntityPlayer) {
                    // ヨー修正: 目標方向が 1ブロック以上離れている場合のみ計算。
                    // スロットル修正: 常に毎tick送信（距離条件に依存しない）。
                    double pdx = entry.autoTargetX - wingman.posX;
                    double pdz = entry.autoTargetZ - wingman.posZ;
                    float pTargetYaw = Float.NaN; // NaN = クライアント側のヨー補間をリセット
                    if (pdx * pdx + pdz * pdz >= 1.0) {
                        pTargetYaw = (float) Math.toDegrees(Math.atan2(-pdx, pdz));
                        // サーバー側: rider.rotationYaw を上書き（lastRiderYaw 経由で物理方向を修正）
                        mcheliRider.rotationYaw = pTargetYaw;
                    }
                    // クライアント側ビジュアル修正: PacketAutopilotVisual でヨー角・スロットルを毎tick送信。
                    // McHeli の onUpdateAircraft() がキー入力 (throttleUp/Down=false) からスロットルを
                    // 再計算して上書きするため、ClientAutopilotHandler が Phase.END で打ち消す必要がある。
                    if (mcheliRider instanceof net.minecraft.entity.player.EntityPlayerMP) {
                        float curThrottle = (float) com.norwood.mcheli.wingman.util.McheliReflect.getCurrentThrottle(wingman);
                        new com.norwood.mcheli.networking.packet.PacketAutopilotVisual(
                                wingman.getEntityId(), pTargetYaw, curThrottle)
                            .sendToPlayer((net.minecraft.entity.player.EntityPlayerMP) mcheliRider);
                    }
                }

                if (entry.autoState == AutonomousState.ALIGN) {
                    // ALIGN: スロットル0 + motion0 で停止保持。
                    // steerToTarget でヨーをレート制限付きで滑走路方向へ回転させる。
                    // isTaxiingState には含めない（即時 setRotYaw でALIGN条件が早期成立するため）。
                    maintainEngine(wingman, 0.0);
                    wingman.motionX = 0;
                    wingman.motionY = 0;
                    wingman.motionZ = 0;
                    steerToTarget(ws, wingman, entry);
                    // 地上停止中はピッチ・ロールを水平に保つ（前ステートの残留姿勢を防ぐ）
                    setRotPitch(wingman, 0.0f);
                    setRotRoll(wingman,  0.0f);
                } else if (isTaxiingState(entry.autoState)) {
                    // タキシング中: スロットル0。
                    // McHeli物理更新前の位置を記録し、motion を進行方向にセットする。
                    // → McHeli が motion を適用してホイールアニメーションを動かす。
                    // → Phase.END の taxiPushPosition() が記録した参照位置から正確な位置に補正
                    //    する（McHeli 分 + taxiPushPosition 分の二重加算を防ぐ）。
                    maintainEngine(wingman, 0.0);
                    entry.taxiPreX = wingman.posX;
                    entry.taxiPreZ = wingman.posZ;
                    double tdx = entry.autoTargetX - wingman.posX;
                    double tdz = entry.autoTargetZ - wingman.posZ;
                    double tDist = Math.sqrt(tdx * tdx + tdz * tdz);
                    if (tDist > 0.5) {
                        wingman.motionX = (tdx / tDist) * TAXI_SPEED;
                        wingman.motionZ = (tdz / tDist) * TAXI_SPEED;
                        // McHeli物理更新前に機首を進行方向へ向ける
                        float tYaw = (float) Math.toDegrees(Math.atan2(-tdx, tdz));
                        setRotYaw(wingman, tYaw);
                    } else {
                        wingman.motionX = 0;
                        wingman.motionZ = 0;
                    }
                    wingman.motionY = 0; // 垂直移動を抑制（浮上・沈降防止）
                    // 地上走行中はピッチ・ロールを水平に保つ（前ステートの残留姿勢を防ぐ）
                    setRotPitch(wingman, 0.0f);
                    setRotRoll(wingman,  0.0f);
                } else if (isAfhThrottleState(entry.autoState)) {
                    // 着陸サーキット・離陸ロールなど: AFHがスロットルを精密制御するステート。
                    steerToTarget(ws, wingman, entry);
                    // TAKEOFF_ROLL / CLIMB: 常に全力スロットル。
                    // AFH の setThrottle は内部フィールドのみ更新し DataParameter を更新しない。
                    // McHeli の地上物理は DataParameter を読むため、WTH の maintainEngine
                    //（setCurrentThrottle + DataParameter の両方を設定）で補完する必要がある。
                    // ALIGN で DataParameter が 0 にセットされたまま TAKEOFF_ROLL に遷移すると
                    // 推力が発生しないため、ここで確実に 1.0 を書き込む。
                    if (entry.autoState == AutonomousState.TAKEOFF_ROLL
                            || entry.autoState == AutonomousState.CLIMB) {
                        maintainEngine(wingman, 1.0);
                    }
                    // VTOLホバー中（離陸上昇・垂直降下）はピッチを0°に固定する。
                    // steerToTarget が 3D ターゲット角度でピッチをつけると、
                    // VTOL ホバーモードではそのピッチが前後水平移動に変換されてしまう。
                    // pitch=0 にすることでスロットル（垂直推力）のみで真上に昇降させる。
                    if (entry.vtolHoverMode) {
                        setRotPitch(wingman, 0.0f);
                    }
                } else if (entry.autoState == AutonomousState.NONE
                           || entry.autoState == AutonomousState.PARKED) {
                    // NONE : AFH が同 tick の Phase.START(後半)で initOrder() を実行して状態確定。
                    //        WTH が先に動くため、初期化前に高スロットル・旋回を適用するとテレポートが発生する。
                    // PARKED: 駐機中は停止保持。
                    // → スロットル 0・motion 0 で停止。旋回なし。
                    maintainEngine(wingman, 0.0);
                    wingman.motionX = 0;
                    wingman.motionY = 0;
                    wingman.motionZ = 0;
                } else {
                    // 巡航・RtB・オンステーションなど: 目標距離に応じてスロットル調整
                    double dx = entry.autoTargetX - wingman.posX;
                    double dy = entry.autoTargetY - wingman.posY;
                    double dz = entry.autoTargetZ - wingman.posZ;
                    double distToTarget = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    maintainEngine(wingman, distToTarget > 20 ? 1.0 : 0.5);
                    steerToTarget(ws, wingman, entry);
                }
            } else if (entry.leader != null) {
                // フォーメーション追従: 指定スロットまでの距離に応じてスロットル調整
                double[] fp = formationPos(entry.leader, entry.formationSlot);
                double dx = fp[0] - wingman.posX;
                double dy = fp[1] - wingman.posY;
                double dz = fp[2] - wingman.posZ;
                double distToSlot = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (isLeaderStopped(entry.leader) && distToSlot < HOLD_THRESHOLD) {
                    holdStop(wingman, entry.leader);
                } else {
                    maintainEngineAdaptive(wingman, entry.leader, distToSlot);
                    steerToTarget(ws, wingman, entry);
                }
            }
        }
    }

    /**
     * McHeliの飛行物理を尊重した操縦制御。
     *
     * McHeli内部: onUpdate_Server は getRotYaw()/getRotPitch() を使って速度ベクトルを更新する。
     * ライダーなしUAVでは setAngles() が呼ばれないため lastRiderYaw/moveLeft 等は効果なし。
     * → setRotYaw / setRotPitch を直接レート制限付きで書き換えることで機体を誘導する。
     */
    private void steerToTarget(WorldServer ws, Entity wingman, WingmanEntry entry) {
        // 発射後エスケープ中は現在ヨーを維持（旋回しない）
        UUID wid = wingman.getUniqueID();
        int escTicks = postFireEscape.getOrDefault(wid, 0);
        if (escTicks > 0) {
            postFireEscape.put(wid, escTicks - 1);
            return;  // ヨー/ピッチ変更なし → 直進エスケープ
        }

        double[] movTarget = computeMoveTarget(ws, wingman, entry);
        double[] aimTarget = computeAimTarget(ws, wingman, entry);
        double[] aimRef    = (aimTarget != null) ? aimTarget : movTarget;

        boolean heli = isHelicopter(wingman);
        boolean inAttack = (entry.attackMode != WingmanEntry.ATK_NONE);
        // 攻撃中は旋回・ピッチレートを高めて精密追跡。固定翼は 5°/tick で目標追跡できる。
        float maxYawRate   = inAttack ? (heli ? 8.0f : 5.0f) : (heli ? MAX_YAW_RATE_HELI : MAX_YAW_RATE_PLANE);
        float maxPitchRate = inAttack ? 5.0f : MAX_PITCH_RATE;

        // 現在の機体角度を取得
        float currentYaw   = getCurrentRotYaw(wingman);
        float currentPitch = getCurrentRotPitch(wingman);

        if (aimRef != null) {
            double dx = aimRef[0] - wingman.posX;
            double dz = aimRef[2] - wingman.posZ;
            double dy = aimRef[1] - wingman.posY;
            // XZ距離を先に計算してヨー不定ゾーンを検出する
            double hDist = Math.sqrt(dx * dx + dz * dz);

            // ─── Yaw: XZ距離が1ブロック以上ある場合のみ更新 ──────────────
            // hDist < 1.0 の場合（VTOL垂直上昇・降下中など）はヨーを変えない。
            // そうしないと atan2(-dx, dz) が不定になりスピンが発生する。
            float lastYawStep = 0f; // バンク計算用にヨーステップを外部から参照できるよう保持
            if (hDist >= 1.0) {
                float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

                // フォーメーション追従中: スロットに近いほどリーダーのヨーに追随する。
                if (!inAttack && !entry.isAutonomous() && entry.leader != null) {
                    // alpha: 0(スロット密着)→1(30ブロック以上離れている)
                    float alpha = (float) Math.min(1.0, hDist / 30.0);
                    float leaderYaw = entry.leader.rotationYaw;
                    float diff = targetYaw - leaderYaw;
                    while (diff >  180f) diff -= 360f;
                    while (diff < -180f) diff += 360f;
                    targetYaw = leaderYaw + diff * alpha;
                }

                float yawDiff = targetYaw - currentYaw;
                while (yawDiff >  180f) yawDiff -= 360f;
                while (yawDiff < -180f) yawDiff += 360f;
                lastYawStep = Math.max(-maxYawRate, Math.min(maxYawRate, yawDiff));
                setRotYaw(wingman, currentYaw + lastYawStep);
                if (wingman instanceof MCH_EntityAircraft acw) acw.lastRiderYaw = targetYaw;
            }

            // ─── Roll: フォーメーション追従中はリーダーのロール角に追随 ──
            if (!inAttack && !entry.isAutonomous() && entry.leader != null) {
                float leaderRoll  = getCurrentRotRoll(entry.leader);
                float currentRoll = getCurrentRotRoll(wingman);
                float rollDiff    = leaderRoll - currentRoll;
                while (rollDiff >  180f) rollDiff -= 360f;
                while (rollDiff < -180f) rollDiff += 360f;
                float rollStep = Math.max(-MAX_ROLL_RATE, Math.min(MAX_ROLL_RATE, rollDiff));
                setRotRoll(wingman, currentRoll + rollStep);
            }

            // ─── Roll: 自律固定翼のバンク（旋回に連動）───────────────────
            // ヨーレートに比例したバンク角を設定（右旋回→内側バンク）。
            // McHeli の rotationYaw は左旋回で増加するため、lastYawStep が正 = 左旋回。
            // 左旋回で内側（左）にバンクさせるには正のロール値が必要。
            // ∴ targetRoll = +lastYawStep（符号反転なし）
            // ヘリ・VTOLホバー・TAKEOFF_ROLL・ALIGN 中はバンクなし（地上または垂直動作）。
            if (entry.isAutonomous() && !heli && !entry.vtolHoverMode
                    && entry.autoState != AutonomousState.TAKEOFF_ROLL
                    && entry.autoState != AutonomousState.ALIGN) {
                float targetRoll = lastYawStep * 7.0f;
                targetRoll = Math.max(-45f, Math.min(45f, targetRoll));
                float curRoll = getCurrentRotRoll(wingman);
                float rDiff   = targetRoll - curRoll;
                while (rDiff >  180f) rDiff -= 360f;
                while (rDiff < -180f) rDiff += 360f;
                setRotRoll(wingman, curRoll + Math.max(-MAX_ROLL_RATE, Math.min(MAX_ROLL_RATE, rDiff)));
            }

            // ─── Pitch ───────────────────────────────────────────────────
            // 固定砲（gun/cannon/cas）: 機首ごとターゲットに向ける
            // それ以外の武器（missile/bomb等）: 機体は水平維持、lastRiderPitchでエイム
            boolean fixedGun = inAttack && isFixedGunWeapon(entry.weaponType);

            float targetPitch;
            if (fixedGun) {
                // 機首でターゲットに直接エイム
                targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(hDist, 1.0)));
                targetPitch = Math.max(MAX_PITCH_DOWN, Math.min(MAX_PITCH_UP, targetPitch));
            } else {
                // 高度維持ピッチ（movTargetのY座標に向かう分だけ）
                double movDy = (movTarget != null) ? (movTarget[1] - wingman.posY) : dy;
                if (heli) {
                    targetPitch = (float) -Math.toDegrees(Math.atan2(movDy, Math.max(hDist, 0.1)));
                    targetPitch = Math.max(-20f, Math.min(20f, targetPitch));
                } else {
                    targetPitch = (float) -Math.toDegrees(Math.atan2(movDy, Math.max(hDist, 1.0)));
                    targetPitch = Math.max(MAX_PITCH_DOWN, Math.min(MAX_PITCH_UP, targetPitch));
                }
            }
            float pitchStep = Math.max(-maxPitchRate, Math.min(maxPitchRate, targetPitch - currentPitch));
            setRotPitch(wingman, currentPitch + pitchStep);

            // lastRiderPitch: 攻撃中は武器エイム用に実際のターゲットへの角度を渡す
            float aimPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(hDist, 1.0)));
            if (wingman instanceof MCH_EntityAircraft acw) acw.lastRiderPitch = aimPitch;
        }

        // 他の子機との分離（formationSideDist を最小間隔として反発ヨー補正）
        applySiblingRepulsion(wingman, entry, currentYaw, maxYawRate);
    }

    /**
     * 同一リーダーの他の子機が近すぎる場合、反発方向にヨーをオフセットする。
     * 最小間隔 = formationSideDist（0の場合は formationRearDist を使用）。
     */
    private void applySiblingRepulsion(Entity wingman, WingmanEntry entry, float currentYaw, float maxYawRate) {
        double minDist = WingmanConfig.formationSideDist > 0
                ? WingmanConfig.formationSideDist
                : WingmanConfig.formationRearDist;
        if (minDist <= 0) return;

        double repX = 0, repZ = 0;
        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            if (e.getKey().equals(wingman.getUniqueID())) continue;
            WingmanEntry other = e.getValue();
            if (other.leader != entry.leader) continue;
            // 他の子機のエンティティをワールドから取得
            Entity sibling = wingman.world.loadedEntityList.stream()
                    .filter(en -> en.getUniqueID().equals(e.getKey()))
                    .findFirst().orElse(null);
            if (sibling == null) continue;
            double sdx = wingman.posX - sibling.posX;
            double sdz = wingman.posZ - sibling.posZ;
            double dist = Math.sqrt(sdx * sdx + sdz * sdz);
            if (dist < minDist && dist > 0.01) {
                // 近すぎる: 離れる方向に反発ベクトルを積算（距離に反比例）
                double strength = (minDist - dist) / minDist;
                repX += (sdx / dist) * strength;
                repZ += (sdz / dist) * strength;
            }
        }

        if (repX == 0 && repZ == 0) return;
        float repYaw = (float) Math.toDegrees(Math.atan2(-repX, repZ));
        float yawDiff = repYaw - currentYaw;
        while (yawDiff >  180f) yawDiff -= 360f;
        while (yawDiff < -180f) yawDiff += 360f;
        // 反発は最大yawRateの半分だけ補正（通常の追従を阻害しないよう控えめに）
        float repStep = Math.max(-maxYawRate * 0.5f, Math.min(maxYawRate * 0.5f, yawDiff));
        setRotYaw(wingman, currentYaw + repStep);
    }

    /**
     * 機首方向固定の直射武器か判定。
     * gun/cannon/cas/rocket は砲身・ポッドが機体前方固定のため機首をターゲットに向ける必要がある。
     * missile/bomb は誘導または垂直投下のため機体向き不問。
     */
    private static boolean isFixedGunWeapon(String type) {
        if (type == null) return false;
        switch (type.toLowerCase()) {
            case "gun": case "machinegun": case "machinegun1": case "machinegun2":
            case "cannon": case "cas":
            case "rocket": case "mkrocket":
                return true;
            default:
                return false;
        }
    }

    // ─── Phase.END ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        WorldServer ws = (WorldServer) event.world;

        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            UUID id = e.getKey();
            WingmanEntry entry = e.getValue();

            // 自律機はleaderがなくても継続。通常follower機はleader死亡で解除。
            if (!entry.isAutonomous() && (entry.leader == null || entry.leader.isDead)) {
                WingmanRegistry.remove(id);
                McHeliWingman.logger.info("[Wingman] {} unregistered — leader gone", id);
                continue;
            }

            // プレイヤーがリーダー機から降りた場合、攻撃モードを自動的に HOLD にする。
            // 降機後もオートモードのままだと標的を追って行方不明になるため。
            if (!entry.isAutonomous() && entry.leader != null
                    && entry.attackMode != WingmanEntry.ATK_NONE) {
                Entity rider = getMcheliRider(entry.leader);
                if (!(rider instanceof net.minecraft.entity.player.EntityPlayer)) {
                    entry.attackMode = WingmanEntry.ATK_NONE;
                    entry.manualTargetId = null;
                    McHeliWingman.logger.info("[Wingman] {} auto-hold — leader has no rider", id);
                }
            }

            Entity wingman = ws.getEntityFromUuid(id);
            if (wingman == null || wingman.isDead) continue;

            // 攻撃: 発射のみ（移動はSTARTで処理済み）
            if (entry.attackMode != WingmanEntry.ATK_NONE) {
                Entity target = resolveTarget(ws, wingman, entry);
                if (target != null && !target.isDead) {
                    // 5秒に1回ターゲット情報をINFOログ（攻撃対象の特定用）
                    if (entry.diagTick % 100 == 1) {
                        McHeliWingman.logger.info(
                            "[Wingman/ATK] {} targeting: name='{}' class={} pos=({},{},{}) dist={}",
                            wingman.getUniqueID().toString().substring(0, 8),
                            target.getName(), target.getClass().getSimpleName(),
                            (int)target.posX, (int)target.posY, (int)target.posZ,
                            (int)wingman.getDistance(target));
                    }
                    double dx   = target.posX - wingman.posX;
                    double dy   = (target.posY + 1.5) - wingman.posY;
                    double dz   = target.posZ - wingman.posZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    float  hd   = (float) Math.sqrt(dx * dx + dz * dz);
                    float  yaw  = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float  pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(hd, 1.0f)));
                    if (dist <= fireRangeForWeapon(entry.weaponType)) {
                        // 発射条件を武器種別で分岐:
                        //   爆弾    : 水平距離≤80ブロックのみ投下（直上または進行方向直下）
                        //   固定銃  : 機首誤差≤20°のみ発射（gun/cannon/rocket など）
                        //   ミサイル: 方向不問（軌道旋回中の側方発射も含む）
                        boolean canFire = true;
                        if (isBombWeapon(entry.weaponType)) {
                            canFire = hd <= 80.0f;
                        } else if (isFixedGunWeapon(entry.weaponType)) {
                            float yawErr = Math.abs(yaw - getCurrentRotYaw(wingman));
                            if (yawErr > 180f) yawErr = 360f - yawErr;
                            canFire = yawErr < 20.0f;
                        }
                        if (canFire) {
                            tryFire(wingman, entry.leader, target, yaw, pitch, entry.weaponType);
                        }
                    }
                }
            }

            // スロット追従ブースト（編隊追従時のみ、McHeli物理更新後にpos加算）
            // ※ Y成分は渡さない: setPosition()でposYを直接書き換えるとMcHeliが
            //   「位置が変わった→上方補正」を繰り返す。高度はpitch/throttleに任せる。
            if (entry.attackMode == WingmanEntry.ATK_NONE && !entry.isAutonomous()
                    && entry.leader != null && !isLeaderStopped(entry.leader)) {
                double[] fp = formationPos(entry.leader, entry.formationSlot);
                double fdx = fp[0] - wingman.posX;
                double fdz = fp[2] - wingman.posZ;
                double xzDist = Math.sqrt(fdx * fdx + fdz * fdz);
                double leaderSpeed = Math.sqrt(
                    entry.leader.motionX * entry.leader.motionX +
                    entry.leader.motionY * entry.leader.motionY +
                    entry.leader.motionZ * entry.leader.motionZ);
                applySlotBoost(wingman, fdx, 0.0, fdz, xzDist, leaderSpeed);
            }

            if (entry.leader != null) {
                syncGear(wingman, entry.leader);
            } else if (entry.isAutonomous()) {
                // 自律飛行中: autoState に応じてギアを自動制御
                autoGear(wingman, entry.autoState);
            }

            // タキシング中: McHeli物理更新後に地上位置を直接移動する
            if (entry.isAutonomous() && isTaxiingState(entry.autoState)) {
                taxiPushPosition(wingman, entry);
            }

            // 自律飛行中にプレイヤーが搭乗している場合の機首方向修正。
            // ─ 根本修正: Phase.START で rider.rotationYaw を目標方向に上書き済み。
            //   McHeli の onUpdate_Server が lastRiderYaw = rider.rotationYaw を読む前に
            //   Phase.START が実行されるため、物理推力方向は正しくなっている。
            // ─ Phase.END: setRotYaw でこの tick の視覚的な機首方向をさらに確定させる。
            //   タキシング中は taxiPushPosition が対処済みなので除外。
            if (entry.isAutonomous() && !isTaxiingState(entry.autoState)) {
                Entity endRider = getMcheliRider(wingman);
                if (endRider instanceof net.minecraft.entity.player.EntityPlayer) {
                    double px = entry.autoTargetX - wingman.posX;
                    double pz = entry.autoTargetZ - wingman.posZ;
                    double phd = Math.sqrt(px * px + pz * pz);
                    if (phd >= 1.0) {
                        float targetYaw  = (float) Math.toDegrees(Math.atan2(-px, pz));
                        float currentYaw = getCurrentRotYaw(wingman);
                        float maxRate    = isHelicopter(wingman) ? MAX_YAW_RATE_HELI : MAX_YAW_RATE_PLANE;
                        float yawDiff    = targetYaw - currentYaw;
                        while (yawDiff >  180f) yawDiff -= 360f;
                        while (yawDiff < -180f) yawDiff += 360f;
                        setRotYaw(wingman, currentYaw + Math.max(-maxRate, Math.min(maxRate, yawDiff)));
                        // rider.rotationYaw も再上書き: McHeli updatePassenger が lastRiderYaw を
                        // rider に push するが、次 tick の Phase.START でまた上書きするため保険として。
                        endRider.rotationYaw = targetYaw;
                    }
                }
            }
        }
    }

    // ─── Target computation ──────────────────────────────────────────────────

    /** 移動目標座標。武器種別高度オフセット・スタンドオフ込み。 */
    private double[] computeMoveTarget(WorldServer ws, Entity wingman, WingmanEntry entry) {
        if (entry.attackMode != WingmanEntry.ATK_NONE) {
            Entity target = resolveTarget(ws, wingman, entry);
            if (target != null && !target.isDead) {
                boolean orbitFlag = (entry.order != null && entry.order.orbitAttack);
                if (isOrbitFireWeapon(entry.weaponType, orbitFlag)) {
                    return computeOrbitTarget(wingman, target, entry);
                } else if (isBombWeapon(entry.weaponType)) {
                    return computeOverflyTarget(wingman, target, entry);
                } else {
                    return computeApproachTarget(wingman, target, entry);
                }
            }
        }
        // 自律飛行中: AutonomousFlightHandlerが設定した目標座標を使う
        if (entry.isAutonomous()) {
            double[] raw = new double[]{ entry.autoTargetX, entry.autoTargetY, entry.autoTargetZ };

            // ─── 旋回中の推力逆転防止（バンクターン誘導）────────────────────────
            // 地上 / 着陸サーキット / ALIGN 中は適用しない（位置精度優先）。
            // それ以外の巡航状態で目標が現在機首方向から 60° 以上ずれている場合、
            // 目標ではなく「現在機首方向から最大 60° 先」の誘導点へ向かわせる。
            // これにより: 推力ベクトルが速度ベクトルと大きくずれず、旋回中も前進を維持できる。
            if (!isTaxiingState(entry.autoState)
                    && !isAfhThrottleState(entry.autoState)
                    && entry.autoState != AutonomousState.ALIGN
                    && !entry.vtolHoverMode) {
                double tdx = raw[0] - wingman.posX;
                double tdz = raw[2] - wingman.posZ;
                double rawDist = Math.sqrt(tdx * tdx + tdz * tdz);
                if (rawDist > 20) {
                    float curYaw = wingman.rotationYaw; // 反射不要（バニラフィールド）
                    float toTargetYaw = (float) Math.toDegrees(Math.atan2(-tdx, tdz));
                    float yawDiff = toTargetYaw - curYaw;
                    while (yawDiff >  180f) yawDiff -= 360f;
                    while (yawDiff < -180f) yawDiff += 360f;
                    // 60° を超えるヨー差: 「60° 先」の誘導点にクランプ
                    if (Math.abs(yawDiff) > 60f) {
                        float leadYaw = curYaw + Math.signum(yawDiff) * 60f;
                        double rad = Math.toRadians(leadYaw);
                        double leadX = wingman.posX + (-Math.sin(rad)) * 40.0;
                        double leadZ = wingman.posZ + Math.cos(rad) * 40.0;
                        return new double[]{ leadX, raw[1], leadZ };
                    }
                }
            }
            return raw;
        }
        if (entry.state == WingmanState.FOLLOWING && entry.leader != null) {
            return formationPos(entry.leader, entry.formationSlot);
        }
        return null;
    }

    /**
     * 移動目標がリーダー機に近づきすぎる場合、リーダーから押し出す補正を加える。
     * クリアランス半径 = max(formationSideDist, formationRearDist) / 2
     */
    private double[] applyLeaderClearance(double[] movTarget, Entity leader) {
        if (leader == null) return movTarget;
        double clearance = Math.max(WingmanConfig.formationSideDist, WingmanConfig.formationRearDist);
        if (clearance <= 0) return movTarget;
        double ldx = movTarget[0] - leader.posX;
        double ldz = movTarget[2] - leader.posZ;
        double hd  = Math.sqrt(ldx * ldx + ldz * ldz);
        if (hd < clearance && hd > 0.01) {
            // リーダーから clearance だけ離れた点に押し出す
            double scale = clearance / hd;
            return new double[]{
                leader.posX + ldx * scale,
                movTarget[1],
                leader.posZ + ldz * scale
            };
        }
        return movTarget;
    }

    /** エイム目標。攻撃中のみターゲット座標を返す。 */
    private double[] computeAimTarget(WorldServer ws, Entity wingman, WingmanEntry entry) {
        if (entry.attackMode == WingmanEntry.ATK_NONE) return null;
        Entity target = resolveTarget(ws, wingman, entry);
        if (target == null || target.isDead) return null;
        return new double[]{target.posX, target.posY + 1.5, target.posZ};
    }

    // ─── Attack path helpers ─────────────────────────────────────────────────

    /**
     * 軌道旋回しながら攻撃（ミサイル / orbitAttack=true の AC-130 モード）。
     * 機体はターゲット周囲を一定半径で旋回し、射程内に入るたびに武器を発射する。
     * ヨーはターゲット方向ではなく旋回軌道の接線方向になるため、
     * 側方射撃（AC-130）や誘導ミサイルの全周発射に対応できる。
     */
    private double[] computeOrbitTarget(Entity wingman, Entity target, WingmanEntry entry) {
        // 軌道半径: ミッションオーダーの orbitRadius を優先、なければ武器スタンドオフ距離
        double orbitRadius = (entry.order != null && entry.order.orbitRadius > 0)
                ? entry.order.orbitRadius
                : standoffForWeapon(entry.weaponType);
        double altOffset = altOffsetForWeapon(entry.weaponType);
        double idealTY   = target.posY + altOffset;
        if (WingmanConfig.minAttackAltitude > 0) idealTY = Math.max(idealTY, WingmanConfig.minAttackAltitude);
        if (WingmanConfig.maxAttackAltitude > 0) idealTY = Math.min(idealTY, WingmanConfig.maxAttackAltitude);

        // 旋回角を毎tick進める（2°/tick ≒ 180tick で一周）
        entry.orbitAngle = (entry.orbitAngle + 2.0) % 360.0;
        double rad = Math.toRadians(entry.orbitAngle);
        double[] raw = new double[]{
            target.posX + Math.sin(rad) * orbitRadius,
            idealTY,
            target.posZ + Math.cos(rad) * orbitRadius
        };
        return applyLeaderClearance(raw, entry.leader);
    }

    /**
     * スタンドオフ接近攻撃（gun / rocket）。
     * スタンドオフ距離まで直線接近し、超えたら後退する往復アプローチ。
     * 機首固定武器はこのモードで機首をターゲットに向けながら接近して発射する。
     */
    private double[] computeApproachTarget(Entity wingman, Entity target, WingmanEntry entry) {
        double standoff  = standoffForWeapon(entry.weaponType);
        double altOffset = altOffsetForWeapon(entry.weaponType);
        double dx    = target.posX - wingman.posX;
        double dz    = target.posZ - wingman.posZ;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        double idealTY = target.posY + altOffset;
        if (WingmanConfig.minAttackAltitude > 0) idealTY = Math.max(idealTY, WingmanConfig.minAttackAltitude);
        if (WingmanConfig.maxAttackAltitude > 0) idealTY = Math.min(idealTY, WingmanConfig.maxAttackAltitude);

        double[] raw;
        if (hDist > standoff) {
            double ratio = (hDist - standoff) / hDist;
            raw = new double[]{
                wingman.posX + dx * ratio,
                idealTY,
                wingman.posZ + dz * ratio
            };
        } else {
            // スタンドオフ圏内: ターゲットから離れる方向に逃げる
            double norm = Math.max(hDist, 0.1);
            raw = new double[]{
                target.posX + (-dx / norm) * standoff * 1.5,
                idealTY,
                target.posZ + (-dz / norm) * standoff * 1.5
            };
        }
        return applyLeaderClearance(raw, entry.leader);
    }

    /**
     * 直上通過投下（bomb）。
     * ターゲットを機首方向に見て、その80ブロック先を目標にする。
     * これにより機体はターゲット直上を通過する軌道を飛び、通過時に爆弾を投下する。
     * 投下条件（水平距離≤80）は Phase.END の発射チェックで別途管理する。
     */
    private double[] computeOverflyTarget(Entity wingman, Entity target, WingmanEntry entry) {
        double altOffset = altOffsetForWeapon("bomb"); // 200m
        double idealTY   = target.posY + altOffset;
        if (WingmanConfig.minAttackAltitude > 0) idealTY = Math.max(idealTY, WingmanConfig.minAttackAltitude);
        if (WingmanConfig.maxAttackAltitude > 0) idealTY = Math.min(idealTY, WingmanConfig.maxAttackAltitude);

        // 現在の進行方向にターゲットを80ブロック超えた点を目標に設定
        float facingRad = (float) Math.toRadians(getCurrentRotYaw(wingman));
        double fwdX = -Math.sin(facingRad);
        double fwdZ =  Math.cos(facingRad);
        double[] raw = new double[]{
            target.posX + fwdX * 80.0,
            idealTY,
            target.posZ + fwdZ * 80.0
        };
        return applyLeaderClearance(raw, entry.leader);
    }

    /**
     * 軌道旋回射撃を行う武器か判定する。
     * ・orbitAttack フラグ（AC-130モード）が true なら全武器で旋回射撃。
     * ・誘導ミサイル系は全周射撃可能なため常に旋回軌道を取る（接近せずに射程維持）。
     */
    private static boolean isOrbitFireWeapon(String type, boolean orbitAttack) {
        if (orbitAttack) return true;
        if (type == null) return false;
        switch (type.toLowerCase()) {
            case "missile": case "aamissile": case "atmissile":
            case "tvmissile": case "asmissile":
                return true;
            default:
                return false;
        }
    }

    /** 爆弾系武器か判定（垂直投下のため直上通過アプローチが必要）。 */
    private static boolean isBombWeapon(String type) {
        return type != null && type.toLowerCase().equals("bomb");
    }

    /**
     * フォーメーション座標。
     *   side > 0: 通常V字/ダイヤ隊形 (rank = slot/2+1, 左右交互)
     *   side = 0: 縦列 (slot 0 = 1×rear後方, slot N = (N+1)×rear後方, 親機と同方向)
     */
    private double[] formationPos(Entity leader, int slot) {
        double sideDist = WingmanConfig.formationSideDist;
        double altOff   = WingmanConfig.formationAltOffset;
        double rearDist = WingmanConfig.formationRearDist;

        double yawRad = Math.toRadians(leader.rotationYaw);
        double fwdX   = -Math.sin(yawRad);
        double fwdZ   =  Math.cos(yawRad);

        if (sideDist == 0.0) {
            // 縦列: 各機がリアに等間隔で一列
            int rank = slot + 1;
            return new double[]{
                leader.posX - fwdX * rearDist * rank,
                leader.posY + altOff,
                leader.posZ - fwdZ * rearDist * rank
            };
        }

        double rigX   =  Math.cos(yawRad);
        double rigZ   =  Math.sin(yawRad);
        int    rank     = slot / 2 + 1;
        double sideSign = (slot % 2 == 0) ? 1.0 : -1.0;

        return new double[]{
            leader.posX + rigX * sideDist * sideSign * rank - fwdX * rearDist * rank,
            leader.posY + altOff,
            leader.posZ + rigZ * sideDist * sideSign * rank - fwdZ * rearDist * rank
        };
    }

    // ─── Attack ──────────────────────────────────────────────────────────────

    private Entity resolveTarget(WorldServer ws, Entity wingman, WingmanEntry entry) {
        if (entry.attackMode == WingmanEntry.ATK_MANUAL && entry.manualTargetId != null)
            return ws.getEntityFromUuid(entry.manualTargetId);
        if (entry.attackMode == WingmanEntry.ATK_AUTO) {
            java.util.Set<UUID> taken = getTargetedUUIDs(wingman.getUniqueID(), entry.leader);
            java.util.List<Entity> siblings = getSiblings(wingman.getUniqueID(), entry.leader, wingman.world);
            Entity target = findNearestHostile(wingman, AUTO_ATTACK_RANGE, taken, siblings);
            entry.currentAutoTarget = (target != null) ? target.getUniqueID() : null;
            return target;
        }
        return null;
    }

    // ─── 敵判定（IMob + McHeli ターゲットドローン）────────────────────────────

    /** isEnemy ログ済みエンティティ UUID（ログ重複排除用）。 */
    private static final java.util.Set<UUID> enemyLoggedSet = java.util.Collections.newSetFromMap(
        new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * 敵として扱うエンティティか判定する。
     * ・IMob（Minecraft 敵対モブ）
     * ・McHeli 機体で isUAV()=true（BQM-74E 等のターゲットドローン）かつ自軍でないもの
     *
     * 注: unmanned（乗客なし）フォールバックは Phalanx 等の地上設置型兵器を誤検出するため不使用。
     *     isUAV() が BQM-74E で true を返すことを確認済み（MCH_EntityPlane）。
     */
    /**
     * 攻撃対象として有効な敵エンティティか判定する。
     *
     * 優先度:
     * 1. プレイヤー本人・プレイヤーが乗っている機体 → 常に除外（フレンドリーファイア防止）
     * 2. 自軍ウィングマン → 除外
     * 3. McHeli の IFF チェック（MCH_Multiplay.canAttackEntity）→ チームシステムを尊重
     * 4. IFF 取得失敗時フォールバック → IMob + UAV ドローン
     */
    private static boolean isEnemy(Entity wingman, Entity e) {
        // プレイヤー自身は絶対に攻撃しない
        if (e instanceof net.minecraft.entity.player.EntityPlayer) return false;
        // 自軍ウィングマンは攻撃しない
        if (WingmanRegistry.snapshot().containsKey(e.getUniqueID())) return false;
        // プレイヤーが乗っている機体は攻撃しない（フレンドリーファイア防止）
        for (Entity passenger : e.getPassengers()) {
            if (passenger instanceof net.minecraft.entity.player.EntityPlayer) return false;
        }
        // McHeli の非戦闘エンティティ（弾体・座席・ヒットボックス・フレア等）を除外する。
        // これらは canAttackEntity=true を返す可能性があるが、攻撃対象として不正。
        // 戦闘対象として有効な McHeli エンティティ: 機体・戦車・車両・ガンナーのみ。
        String eClass = e.getClass().getName();
        if (eClass.startsWith("com.norwood.mcheli.")) {
            if (!com.norwood.mcheli.wingman.util.McheliReflect.isAircraft(e)
                    && !eClass.equals("com.norwood.mcheli.tank.MCH_EntityTank")
                    && !eClass.equals("com.norwood.mcheli.vehicle.MCH_EntityVehicle")
                    && !eClass.equals("com.norwood.mcheli.mob.MCH_EntityGunner")) {
                return false; // 弾体・座席・ヒットボックス・フレア等は除外
            }
        }
        // McHeli IFF チェック: MCH_Multiplay.canAttackEntity(attacker, target)
        // チームシステムが有効な場合、味方チームのエンティティには false を返す
        boolean ok = com.norwood.mcheli.multiplay.MCH_Multiplay.canAttackEntity(wingman, e);
        if (enemyLoggedSet.add(e.getUniqueID())) {
            McHeliWingman.logger.info(
                "[Wingman/IFF] {} → canAttackEntity={}", e.getName(), ok);
        }
        return ok;
    }

    private Entity findNearestHostile(Entity wingman, double range,
                                      java.util.Set<UUID> exclude, java.util.List<Entity> siblings) {
        double minSep = WingmanConfig.formationSideDist > 0
                ? WingmanConfig.formationSideDist
                : WingmanConfig.formationRearDist;
        double bestSq = range * range;
        Entity best   = null;
        outer:
        for (Entity e : wingman.world.loadedEntityList) {
            if (e == wingman || !isEnemy(wingman, e) || e.isDead) continue;
            if (exclude != null && exclude.contains(e.getUniqueID())) continue;
            // 攻撃経路（wingman→ターゲット間の中点）が他の子機に近すぎる場合はスキップ
            if (minSep > 0 && siblings != null) {
                double midX = (wingman.posX + e.posX) * 0.5;
                double midZ = (wingman.posZ + e.posZ) * 0.5;
                for (Entity sib : siblings) {
                    double sdx = midX - sib.posX;
                    double sdz = midZ - sib.posZ;
                    if (sdx * sdx + sdz * sdz < minSep * minSep) continue outer;
                }
            }
            double dsq = wingman.getDistanceSq(e);
            if (dsq < bestSq) { bestSq = dsq; best = e; }
        }
        return best;
    }

    /** 同一リーダーに従う他の子機エンティティリストを返す。 */
    private java.util.List<Entity> getSiblings(UUID selfId, Entity leader, net.minecraft.world.World world) {
        java.util.List<Entity> result = new java.util.ArrayList<>();
        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            if (e.getKey().equals(selfId)) continue;
            if (e.getValue().leader != leader) continue;
            for (Entity en : world.loadedEntityList) {
                if (en.getUniqueID().equals(e.getKey())) { result.add(en); break; }
            }
        }
        return result;
    }

    /** 同一リーダーの他子機が攻撃中のターゲットUUIDセットを返す（分散攻撃用）。 */
    private java.util.Set<UUID> getTargetedUUIDs(UUID selfId, Entity leader) {
        java.util.Set<UUID> taken = new java.util.HashSet<>();
        for (Map.Entry<UUID, WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
            if (e.getKey().equals(selfId)) continue;
            WingmanEntry other = e.getValue();
            if (other.leader != leader) continue;
            if (other.attackMode == WingmanEntry.ATK_MANUAL && other.manualTargetId != null)
                taken.add(other.manualTargetId);
            else if (other.attackMode == WingmanEntry.ATK_AUTO && other.currentAutoTarget != null)
                taken.add(other.currentAutoTarget);
        }
        return taken;
    }

    private void tryFire(Entity aircraft, Entity leader, Entity target,
                         float yaw, float pitch, String weaponType) {
        if (!(aircraft instanceof MCH_EntityAircraft ac)) return;
        MCH_WeaponSet[] weapons = ac.getWeapons();
        if (weapons == null || weapons.length == 0) return;

        UUID id = aircraft.getUniqueID();
        int cd = fireCooldowns.getOrDefault(id, 0);
        if (cd > 0) { fireCooldowns.put(id, cd - 1); return; }

        MCH_WeaponSet chosen = pickWeapon(weapons, weaponType, true);
        if (chosen == null) chosen = pickWeapon(weapons, weaponType, false);
        if (chosen == null) return;

        String resolvedType = resolveWeaponType(chosen, weaponType);

        // Build the param the CE way: entity = the firing aircraft, user = the leader
        // (shot attribution / IFF). isInfinity lets the autonomous craft self-resupply.
        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.entity = ac;
        prm.user = leader;
        prm.isInfinity = true;
        prm.setPosAndRot(ac.posX, ac.posY, ac.posZ, yaw, pitch);

        boolean fired = chosen.use(prm);

        if (fired) {
            int cooldown = fireCooldownForType(resolvedType);
            fireCooldowns.put(id, cooldown);
            int escapeTicks = isHeavyWeapon(resolvedType) ? POST_FIRE_ESCAPE_TICKS : 0;
            if (escapeTicks > 0) postFireEscape.put(id, escapeTicks);
            McHeliWingman.logger.debug("[Wingman] fired type={} cooldown={} escape={}", resolvedType, cooldown, escapeTicks);
        } else {
            McHeliWingman.logger.debug("[Wingman] tryFire failed type={}", resolvedType);
        }
    }

    private static boolean isHeavyWeapon(String type) {
        if (type == null) return false;
        switch (type.toLowerCase()) {
            case "bomb": case "asmissile": case "missile":
            case "aamissile": case "atmissile": case "tvmissile":
                return true;
            default: return false;
        }
    }

    private MCH_WeaponSet pickWeapon(MCH_WeaponSet[] weapons, String weaponType, boolean requireCanFire) {
        for (MCH_WeaponSet ws : weapons) {
            if (ws == null) continue;
            String t = resolveWeaponType(ws, "");
            if ("dummy".equalsIgnoreCase(t)) continue;
            if ("targetingpod".equalsIgnoreCase(t) && !"targetingpod".equalsIgnoreCase(weaponType)) continue;
            if (weaponType != null && !matchesWeaponType(ws, weaponType)) continue;
            if (requireCanFire && !ws.canFire()) continue;
            return ws;
        }
        return null;
    }

    private String resolveWeaponType(MCH_WeaponSet weaponSet, String fallback) {
        com.norwood.mcheli.weapon.MCH_WeaponInfo info = weaponSet.getInfo();
        if (info == null || info.type == null) return fallback;
        return info.type;
    }

    private boolean matchesWeaponType(MCH_WeaponSet weaponSet, String expectedType) {
        String actual = resolveWeaponType(weaponSet, null);
        if (actual == null) return false;
        if (expectedType.equalsIgnoreCase(actual)) return true;
        String exp = expectedType.toLowerCase();
        String act = actual.toLowerCase();
        switch (exp) {
            case "gun":       return act.contains("gun") || act.equals("cas");
            case "cannon":    return act.contains("cannon");
            case "missile":   return act.contains("missile");
            case "asmissile": return act.equals("asmissile");
            case "rocket":    return act.contains("rocket");
            case "bomb":      return act.contains("bomb");
            case "torpedo":   return act.contains("torpedo");
            default:          return false;
        }
    }

    // ─── Engine ──────────────────────────────────────────────────────────────

    // スロットルが切り替わり始める距離（ブロック）
    private static final double THROTTLE_BLEND_FAR  = 80.0;  // この距離以上: 全力
    private static final double THROTTLE_BLEND_NEAR = 12.0;  // この距離以下: 親機同調

    // スロット追従ブースト（スロットル上限突破用motion加算）
    // McHeliの物理更新後にmotion加算するため機体固有の速度上限を超えられる
    // ブーストはリーダー速度×BOOST_LEADER_MULT を上限とする（動的計算）
    // → リーダー速度 + ブースト ≒ リーダー速度×3 を実現
    private static final double BOOST_LEADER_MULT = 2.0;  // リーダー速度に掛けた値が最大ブースト量
    private static final double BOOST_DIST_ON  = 80.0;    // ブースト開始距離
    private static final double BOOST_DIST_OFF = 20.0;    // ブースト終了距離（これ以下でゼロに）

    /**
     * フォーメーション追従用適応スロットル。
     * 遠い: 全力 / 近い: 親機スロットルに同調して追い抜き防止。
     */
    private void maintainEngineAdaptive(Entity aircraft, Entity leader, double distToSlot) {
        double leaderThrottle = getEntityThrottle(leader);

        double targetThrottle;
        if (distToSlot >= THROTTLE_BLEND_FAR) {
            targetThrottle = 1.0;
        } else if (distToSlot <= THROTTLE_BLEND_NEAR) {
            targetThrottle = leaderThrottle;
        } else {
            double t = (distToSlot - THROTTLE_BLEND_NEAR) / (THROTTLE_BLEND_FAR - THROTTLE_BLEND_NEAR);
            targetThrottle = leaderThrottle + t * (1.0 - leaderThrottle);
        }
        // 未到着時の最低スロットル: 停止中の親機でも子機は位置に向かって動き続けられる
        double minThrottle = distToSlot > ARRIVAL_THRESHOLD ? 0.25 : 0.05;
        targetThrottle = Math.max(minThrottle, Math.min(1.0, targetThrottle));

        applyThrottle(aircraft, targetThrottle);
    }

    /**
     * スロット追従ブースト。
     * McHeliが物理更新でmotionを確定した後（Phase.END）に呼び出し、
     * スロット方向へ追加velocityを加算してスロットル上限を超えた速度を実現する。
     *
     * ブースト量はスロットまでの距離で線形補間:
     *   dist >= BOOST_DIST_ON  → BOOST_MAX
     *   dist <= BOOST_DIST_OFF → 0（オーバーシュート防止）
     */
    /**
     * スロット追従ブースト。
     * 最大ブースト = leaderSpeed × BOOST_LEADER_MULT（≒ リーダー速度の3倍まで到達可能）。
     * スロット距離でtaper: BOOST_DIST_ON以上=全力、BOOST_DIST_OFF以下=ゼロ。
     */
    private void applySlotBoost(Entity wingman, double fdx, double fdy, double fdz,
                                 double distToSlot, double leaderSpeed) {
        if (distToSlot <= BOOST_DIST_OFF) return;

        double taper;
        if (distToSlot >= BOOST_DIST_ON) {
            taper = 1.0;
        } else {
            taper = (distToSlot - BOOST_DIST_OFF) / (BOOST_DIST_ON - BOOST_DIST_OFF);
        }

        double boostAmount = leaderSpeed * BOOST_LEADER_MULT * taper;

        double norm = Math.max(distToSlot, 0.01);
        double bx = (fdx / norm) * boostAmount;
        double by = (fdy / norm) * boostAmount;
        double bz = (fdz / norm) * boostAmount;

        // motionへの加算はMcHeliが次tickに上書きするため無効。
        // posX/Y/Zを直接加算してMcHeliの物理更新後に追加変位を与える。
        double newX = wingman.posX + bx;
        double newY = wingman.posY + by;
        double newZ = wingman.posZ + bz;
        wingman.setPosition(newX, newY, newZ);

        boostDebugTick++;
        if (boostDebugTick >= 20) {
            boostDebugTick = 0;
            McHeliWingman.logger.info("[Wingman/Boost] dist={} leaderSpd={} boost={} applied to pos",
                (int)distToSlot,
                String.format("%.3f", leaderSpeed),
                String.format("%.3f", boostAmount));
        }
    }

    /** 指定スロットル値を直接適用する（攻撃モード・強制指定用）。 */
    private void maintainEngine(Entity aircraft, double throttle) {
        applyThrottle(aircraft, throttle);
    }

    private void applyThrottle(Entity aircraft, double throttle) {
        if (aircraft instanceof MCH_EntityAircraft ac) ac.setCurrentThrottle(throttle);
    }

    /** McHeli aircraft の現在スロットル値を取得する。取得失敗時は 0.5 を返す。 */
    private double getEntityThrottle(Entity entity) {
        return entity instanceof MCH_EntityAircraft ac ? ac.getCurrentThrottle() : 0.5;
    }

    // ─── Gear ────────────────────────────────────────────────────────────────

    private void syncGear(Entity wingman, Entity leader) {
        if (!(leader instanceof MCH_EntityAircraft l) || !(wingman instanceof MCH_EntityAircraft w)) return;
        boolean lf = l.isLandingGearFolded();
        boolean wf = w.isLandingGearFolded();
        if (!w.canFoldLandingGear()) return;
        if (lf && !wf) w.foldLandingGear();
        else if (!lf && wf) w.unfoldLandingGear();
    }

    /**
     * 自律飛行中のギア自動制御。
     * 巡航・旋回・ダウンウィンド・ベースレグ中は格納、それ以外（地上・アプローチ・着陸）は展開。
     */
    private void autoGear(Entity wingman, com.norwood.mcheli.wingman.mission.AutonomousState state) {
        boolean retract;
        switch (state) {
            case CLIMB:
            case ENROUTE:
            case ATTACK:
            case LOITER:
            case DESCEND:
            case CIRCUIT_DOWNWIND:
            case CIRCUIT_BASE:
            // MissionOrder 系
            case TRANSIT_TO:
            case ON_STATION:
            case STRIKE_PASS:
            case RTB:
            case VTOL_TAKEOFF:  // VTOL離陸中はギア格納
                retract = true;
                break;
            default:  // TAXI_OUT, ALIGN, TAKEOFF_ROLL, CIRCUIT_FINAL, LANDING, TAXI_IN, PARKED, VTOL_LAND など
                retract = false;
                break;
        }
        if (wingman instanceof MCH_EntityAircraft w) {
            if (!w.canFoldLandingGear()) return;
            boolean folded = w.isLandingGearFolded();
            if (retract && !folded) w.foldLandingGear();
            else if (!retract && folded) w.unfoldLandingGear();
        }
    }

    // ─── Leader / hold helpers ───────────────────────────────────────────────

    private static boolean isLeaderStopped(Entity leader) {
        double spd = Math.sqrt(
            leader.motionX * leader.motionX +
            leader.motionY * leader.motionY +
            leader.motionZ * leader.motionZ);
        return spd < LEADER_STOP_SPEED;
    }

    /**
     * 停止ホールド。固定翼はスロットル0で減速、ヘリはホバー維持スロットルを使う。
     * ヘリをスロットル0にすると重力で落下→過剰上昇を繰り返す無限上昇が起きるため、
     * 少量スロットルで高度を保持する。
     */
    private void holdStop(Entity aircraft, Entity leader) {
        boolean heli = isHelicopter(aircraft);
        if (heli) {
            // ヘリ: 高度誤差に応じてスロットルを微調整してホバリング維持
            double targetY = leader.posY + WingmanConfig.formationAltOffset;
            double dy = targetY - aircraft.posY;
            // 誤差 ±3ブロック以内ならホバースロットル、それ以上は補正
            double hover = 0.35 + Math.max(-0.15, Math.min(0.15, dy * 0.03));
            applyThrottle(aircraft, hover);
        } else {
            applyThrottle(aircraft, 0.0);
        }
        // 停止中は親機と同方向を向く（地上整列）
        float leaderYaw = leader.rotationYaw;
        float childYaw  = getCurrentRotYaw(aircraft);
        float yawDiff   = leaderYaw - childYaw;
        while (yawDiff >  180f) yawDiff -= 360f;
        while (yawDiff < -180f) yawDiff += 360f;
        float yawStep = Math.max(-MAX_YAW_RATE_HELI, Math.min(MAX_YAW_RATE_HELI, yawDiff));
        setRotYaw(aircraft, childYaw + yawStep);
    }

    // ─── Taxi helpers ────────────────────────────────────────────────────────

    /** 地上タキシング中のステートか判定する。
     *  ALIGN は除外 — ALIGN は rate-limited steerToTarget でヨー回転させる別処理。*/
    private static boolean isTaxiingState(AutonomousState state) {
        return state == AutonomousState.TAXI_OUT
            || state == AutonomousState.TAXI_IN
            || state == AutonomousState.LANDING; // A端タッチダウン後のB端への地上ロール
    }

    /**
     * AFH（AutonomousFlightHandler）がスロットルを精密制御するステートか判定する。
     * WTH はこれらのステートでスロットルを上書きしない。
     * 着陸サーキット全ステートと離陸ロール（TAKEOFF_ROLL）が該当。
     */
    private static boolean isAfhThrottleState(AutonomousState state) {
        switch (state) {
            case TAKEOFF_ROLL:
            case CLIMB:
            case DESCEND:
            case CIRCUIT_DOWNWIND:
            case CIRCUIT_BASE:
            case CIRCUIT_FINAL:
            case LANDING:
            case VTOL_TAKEOFF:  // VTOL離着陸中: AFHがスロットルを精密制御
            case VTOL_LAND:
                return true;
            default:
                return false;
        }
    }

    /** タキシング速度 (blocks/tick) ≒ 5 m/s */
    private static final double TAXI_SPEED = 0.25;

    /**
     * McHeli 物理更新後（Phase.END）に機体を地上タキシング目標方向へ強制移動する。
     * スロットルは 0 に保ち、setPosition() で位置を上書きして地上走行を表現する。
     */
    private void taxiPushPosition(Entity wingman, WingmanEntry entry) {
        // Phase.START で記録した McHeli 物理更新前の XZ 位置を参照点として使う。
        // McHeli が Phase.START で設定した motion を適用して機体を動かした後に
        // この補正を掛けるため、二重加算にならず正確に TAXI_SPEED で前進できる。
        double baseX = entry.taxiPreX;
        double baseZ = entry.taxiPreZ;
        double dx = entry.autoTargetX - baseX;
        double dz = entry.autoTargetZ - baseZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5) {
            wingman.motionX = 0;
            wingman.motionZ = 0;
            return; // 目標到達済み
        }

        // LANDING 状態: タッチダウン速度から指数減衰で TAXI_SPEED まで減速する。
        // それ以外: 固定 TAXI_SPEED。
        double speed;
        if (entry.autoState == com.norwood.mcheli.wingman.mission.AutonomousState.LANDING
                && entry.landingRollSpeed > TAXI_SPEED) {
            entry.landingRollSpeed = Math.max(TAXI_SPEED, entry.landingRollSpeed * 0.965);
            speed = entry.landingRollSpeed;
        } else {
            speed = TAXI_SPEED;
        }

        double step = Math.min(speed, dist);
        double newX = baseX + (dx / dist) * step;
        double newZ = baseZ + (dz / dist) * step;
        // Y は McHeli 物理が確定した高さを維持（地面追従）
        wingman.setPosition(newX, wingman.posY, newZ);

        // 次 tick の Phase.START に備えて motion を維持（ホイールアニメーション継続）
        wingman.motionX = (dx / dist) * speed;
        wingman.motionZ = (dz / dist) * speed;

        // ヨーを進行方向に向ける（Phase.END でも再確認）、ピッチ・ロールを水平に維持
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        setRotYaw(wingman,   targetYaw);
        setRotPitch(wingman, 0.0f);
        setRotRoll(wingman,  0.0f);

        // 5秒毎にタキシー進捗ログ
        if (entry.diagTick % 100 == 0) {
            McHeliWingman.logger.info("[Taxi] {} push base=({},{}) pos=({},{}) target=({},{}) dist={} step={} yaw={}",
                wingman.getUniqueID().toString().substring(0, 8),
                String.format("%.1f", baseX), String.format("%.1f", baseZ),
                String.format("%.1f", wingman.posX), String.format("%.1f", wingman.posZ),
                String.format("%.1f", entry.autoTargetX), String.format("%.1f", entry.autoTargetZ),
                String.format("%.1f", dist), String.format("%.3f", step),
                String.format("%.1f", targetYaw));
        }
    }

    // ─── Aircraft type / VTOL ────────────────────────────────────────────────

    private boolean isHelicopter(Entity a) {
        return a instanceof MCH_EntityHeli;
    }

    private void forceVtolOff(Entity a) {
        if (a instanceof MCH_EntityPlane p && p.getVtolMode() != 0) {
            p.swithVtolMode(false);
            McHeliWingman.logger.info("[VTOL] {} McHeli VTOL=OFF (fixed-wing mode)", a.getUniqueID().toString().substring(0, 8));
        }
    }

    private void forceVtolOn(Entity a) {
        if (a instanceof MCH_EntityPlane p && p.getVtolMode() == 0) {
            p.swithVtolMode(true);
            McHeliWingman.logger.info("[VTOL] {} McHeli VTOL=ON (hover mode)", a.getUniqueID().toString().substring(0, 8));
        }
    }

    private float getCurrentRotYaw(Entity a) {
        return a instanceof MCH_EntityAircraft ac ? ac.getYaw() : a.rotationYaw;
    }

    private float getCurrentRotPitch(Entity a) {
        return a instanceof MCH_EntityAircraft ac ? ac.getPitch() : a.rotationPitch;
    }

    private void setRotYaw(Entity a, float yaw) {
        if (a instanceof MCH_EntityAircraft ac) ac.setRotYaw(yaw);
    }

    private void setRotPitch(Entity a, float pitch) {
        if (a instanceof MCH_EntityAircraft ac) ac.setRotPitch(pitch);
    }

    private float getCurrentRotRoll(Entity a) {
        return a instanceof MCH_EntityAircraft ac ? ac.getRoll() : 0f;
    }

    private void setRotRoll(Entity a, float roll) {
        if (a instanceof MCH_EntityAircraft ac) ac.setRotRoll(roll);
    }

    /**
     * McHeli の独自乗員取得。
     * McHeli は IEntitySinglePassenger を実装し getRiddenByEntity() で乗員を返す。
     * vanilla の getPassengers() は空のことが多いため、こちらを優先して使用する。
     */
    private Entity getMcheliRider(Entity aircraft) {
        // 1) McHeli の getRiddenByEntity() を試みる
        if (aircraft instanceof MCH_EntityAircraft ac) {
            Entity r = ac.getRiddenByEntity();
            if (r != null) return r;
        }
        // 2) vanilla の getPassengers() にフォールバック
        for (Entity pass : aircraft.getPassengers()) {
            return pass;
        }
        return null;
    }
}
