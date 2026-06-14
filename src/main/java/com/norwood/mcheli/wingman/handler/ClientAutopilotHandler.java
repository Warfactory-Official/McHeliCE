package com.norwood.mcheli.wingman.handler;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketAutopilotVisual;
import com.norwood.mcheli.wingman.util.McheliReflect;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;
import java.util.Map;

/**
 * CLIENT ONLY.
 * 自律飛行中に機体ビジュアルヘディングを修正するクライアントTickハンドラ。
 *
 * 問題: McHeli の func_70071_h_() (onUpdate) はクライアント側でも実行され、
 *       毎tick lastRiderYaw = localPlayer.rotationYaw (= プレイヤーの向き) を上書きする。
 *       このためサーバー側でいくら修正しても機体のビジュアルはプレイヤーの向きに固定される。
 *
 * 解決: サーバーが PacketAutopilotVisual で目標ヨー角を毎tick送信。
 *       クライアントは ClientTickEvent.Phase.END（エンティティ更新後）に
 *       指数平滑補間で setRotYaw を呼び出し、onUpdateAircraft() の結果を上書きする。
 *       prevRotationYaw も明示的に設定することでレンダリング補間を正確に制御する。
 *       プレイヤーの rotationYaw には触れないため、見回しは自由。
 */
@SideOnly(Side.CLIENT)
public class ClientAutopilotHandler {

    /**
     * 指数平滑係数 (0〜1)。
     * 高い値: より速く targetYaw に近づく（レスポンシブだが急峻）
     * 低い値: よりゆっくり近づく（滑らか）
     * 0.35 ≒ 1tick で差の35%を解消。90°のターンは約6tickで視覚的に追いつく。
     */
    private static final float SMOOTH_ALPHA = 0.35f;

    /** 上限ヨーレート (°/tick)。大きな角度差でも急激に動かないようにする。 */
    private static final float MAX_VISUAL_YAW_RATE = 6.0f;

    /** 前回適用したビジュアルヨー角。補間の起点として使用。null = 未初期化。 */
    private Float lastAppliedYaw = null;
    /** 前回適用した機体エンティティID。機体が変わったときにリセットするため保持。 */
    private int   lastEntityId   = -1;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        boolean hasYaw      = !PacketAutopilotVisual.CLIENT_HEADINGS.isEmpty();
        boolean hasThrottle = !PacketAutopilotVisual.CLIENT_THROTTLES.isEmpty();
        if (!hasYaw && !hasThrottle) return;

        // プレイヤーが乗っていない場合は全エントリを削除してリセット
        Entity vehicle = mc.player.getRidingEntity();
        if (vehicle == null || !McheliReflect.isAircraft(vehicle)) {
            PacketAutopilotVisual.CLIENT_HEADINGS.clear();
            PacketAutopilotVisual.CLIENT_THROTTLES.clear();
            lastAppliedYaw = null;
            return;
        }

        // ヨー補間適用
        Iterator<Map.Entry<Integer, Float>> it =
                PacketAutopilotVisual.CLIENT_HEADINGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Float> e = it.next();
            if (e.getKey() != vehicle.getEntityId()) {
                it.remove();
                continue;
            }
            applyHeading(vehicle, e.getValue());
        }

        // スロットル上書き: McHeli の onUpdateAircraft() がキー入力から再計算した値を打ち消す
        Iterator<Map.Entry<Integer, Float>> it2 =
                PacketAutopilotVisual.CLIENT_THROTTLES.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<Integer, Float> e = it2.next();
            if (e.getKey() != vehicle.getEntityId()) {
                it2.remove();
                continue;
            }
            applyThrottle(vehicle, e.getValue());
        }
    }

    // ─── Apply heading with exponential smoothing ────────────────────────────

    private void applyHeading(Entity aircraft, float targetYaw) {
        // 初回 or エンティティ変更時: aircraft の現在のビジュアルヨーを起点にする
        if (lastAppliedYaw == null || lastEntityId != aircraft.getEntityId()) {
            lastAppliedYaw = aircraft.rotationYaw;
            lastEntityId   = aircraft.getEntityId();
        }

        // 最短経路での角度差を計算（-180°〜+180°に正規化）
        float diff = targetYaw - lastAppliedYaw;
        while (diff >  180f) diff -= 360f;
        while (diff < -180f) diff += 360f;

        // 指数平滑化: diff の SMOOTH_ALPHA 分だけ近づく（自然な加減速）
        // さらに MAX_VISUAL_YAW_RATE で上限を設けて急激な回転を防止
        float step   = diff * SMOOTH_ALPHA;
        step = Math.max(-MAX_VISUAL_YAW_RATE, Math.min(MAX_VISUAL_YAW_RATE, step));

        float prevYaw = lastAppliedYaw;
        float newYaw  = lastAppliedYaw + step;
        lastAppliedYaw = newYaw;

        // prevRotationYaw も明示的に設定: McHeli が func_70071_h_ 内で更新した prevRotYaw は
        // playerLook ベースで不正確なため、自分たちの補間値で上書きしてレンダリングを安定させる。
        aircraft.prevRotationYaw = prevYaw;

        // rotationYaw を設定（onUpdateAircraft() の結果を上書き）
        if (aircraft instanceof MCH_EntityAircraft ac) {
            ac.setRotYaw(newYaw);
        } else {
            aircraft.rotationYaw = newYaw;
        }
    }

    // ─── Apply throttle ──────────────────────────────────────────────────────

    private void applyThrottle(Entity aircraft, float targetThrottle) {
        if (aircraft instanceof MCH_EntityAircraft ac) {
            ac.setCurrentThrottle((double) targetThrottle);
        }
    }
}
