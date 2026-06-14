package com.norwood.mcheli.wingman.client;

import com.norwood.mcheli.networking.packet.PacketWingmanPanelOpen;
import com.norwood.mcheli.wingman.util.McheliReflect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * クライアント専用: ウィングマンパネルのキーバインドと HUD ヒント描画。
 *
 * McHeli の他のキー説明と同じスタイルで画面右側に描画する。
 * MCH_KeyName.getDescOrName(KeyBinding) を直接呼んでキー名を取得する。
 */
@SideOnly(Side.CLIENT)
public class WingmanKeyHandler {

    // ─── Key binding ─────────────────────────────────────────────────────────

    public static final KeyBinding KEY_PANEL = new KeyBinding(
            "key.wingman.panel",
            Keyboard.KEY_NUMPAD0,
            "key.categories.wingman");

    /** FMLInitializationEvent から呼ぶ（クライアント側のみ）。 */
    public static void registerClient() {
        ClientRegistry.registerKeyBinding(KEY_PANEL);
    }

    // ─── Client tick: キー押下検出 → サーバーへリクエスト ───────────────────

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) return;

        Entity riding = mc.player.getRidingEntity();
        if (!McheliReflect.isAircraft(riding)) return;

        if (KEY_PANEL.isPressed()) {
            new PacketWingmanPanelOpen().sendToServer();
        }
    }

    // ─── HUD ヒント描画 ───────────────────────────────────────────────────────

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        Entity riding = player.getRidingEntity();
        if (!McheliReflect.isAircraft(riding)) return;

        // GUIが開いているときは描画しない
        if (mc.currentScreen != null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        drawKeyHint(mc, sr);
    }

    /**
     * McHeli のキー説明と同じ視覚スタイルで [KEY] 説明 を描画する。
     *
     * McHeli は画面右端から右揃えでキーヒントを描いている。
     * MCH_AircraftCommonGui.drawKeyBind() の位置（右辺 x = width, y 約 40px〜）
     * に合わせて、末尾（= 一番下）に追加する形で描画する。
     *
     * 実際の McHeli ヒント数が不明なため固定オフセット (Y_OFFSET) を使う。
     * 必要に応じて config で調整できるようにしてもよい。
     */
    private static void drawKeyHint(Minecraft mc, ScaledResolution sr) {
        // キー名: MCH_KeyName.getDescOrName() があれば使う（McHeli 流の名前）
        String keyName = getMcheliKeyName();

        FontRenderer fr = mc.fontRenderer;
        String keyStr  = "[" + keyName + "]";
        String descStr = " Wingman Panel";

        int keyW  = fr.getStringWidth(keyStr);
        int descW = fr.getStringWidth(descStr);
        int totalW = keyW + descW;

        // 右揃え、Y は McHeli のキーヒント群の末尾付近（固定値）
        int sw = sr.getScaledWidth();
        int x  = sw - totalW - 4;
        int y  = Y_OFFSET;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        // 半透明の黒い帯
        net.minecraft.client.gui.Gui.drawRect(
                x - 2, y - 1,
                x + totalW + 2, y + fr.FONT_HEIGHT + 1,
                0x88000000);

        // [KEY] 部分をオレンジ（McHeli の強調色）で描画
        fr.drawStringWithShadow(keyStr,  x,         y, 0xFFFFAA00);
        // 説明文を白で描画
        fr.drawStringWithShadow(descStr, x + keyW,  y, 0xFFFFFFFF);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * McHeli が HUD キーヒントを描き始める Y 座標に合わせた固定オフセット。
     *
     * McHeli のヘリコプター HUD は右側に約 12 個のキーヒントを描く。
     * 1 行 = fontRenderer.FONT_HEIGHT + 2px = 10px 程度。
     * 本実装では McHeli のヒント群の「下」に続けて表示するため、
     * 基準 Y = 40 (McHeli 先頭) + ヒント数 × 10 とする。
     * ゲーム内でずれる場合は下の値を調整すること。
     */
    private static final int Y_OFFSET = 150;

    // ─── MCH_KeyName.getDescOrName() ────────────────────────────────────────

    private static String getMcheliKeyName() {
        // MCH_KeyName で McHeli 流のキー名を取得（McHeli 流の名前）
        String result = com.norwood.mcheli.MCH_KeyName.getDescOrName(KEY_PANEL);
        if (result != null && !result.isEmpty()) return result;
        // フォールバック: Minecraft 標準のキー名
        return net.minecraft.client.settings.GameSettings.getKeyDisplayString(
                KEY_PANEL.getKeyCode());
    }
}
