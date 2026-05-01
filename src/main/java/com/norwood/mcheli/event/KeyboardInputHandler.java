package com.norwood.mcheli.event;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.MCH_ServerSettings;
import com.norwood.mcheli.networking.packet.PacketOpenScreen;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;

import static com.norwood.mcheli.event.ClientCommonTickHandler.isDrawScoreboard;

public class KeyboardInputHandler {
    private final Minecraft mc;
    private final MCH_ClientTickHandlerBase[] ticks;
    public MCH_Key[] Keys;
    public MCH_Key KeyCamDistUp;
    public MCH_Key KeyCamDistDown;
    public MCH_Key KeyScoreboard;
    public MCH_Key KeyMultiplayManager;

    protected void updateKeys() {
        for (MCH_Key key : Keys) {
            key.update();
        }
    }

    protected KeyboardInputHandler(Minecraft mc, MCH_ClientTickHandlerBase[] ticks) {
        this.mc = mc;
        this.ticks = ticks;
    }

    public void updateKeybind(MCH_Config config) {
        this.KeyCamDistUp = new MCH_Key(MCH_Config.KeyCameraDistUp.prmInt);
        this.KeyCamDistDown = new MCH_Key(MCH_Config.KeyCameraDistDown.prmInt);
        this.KeyScoreboard = new MCH_Key(MCH_Config.KeyScoreboard.prmInt);
        this.KeyMultiplayManager = new MCH_Key(MCH_Config.KeyMultiplayManager.prmInt);
        this.Keys = new MCH_Key[]{this.KeyCamDistUp, this.KeyCamDistDown, this.KeyScoreboard, this.KeyMultiplayManager};

        for (MCH_ClientTickHandlerBase t : ticks) {
            t.updateKeybind(config);
        }
    }

    protected void onTick() {
        updateKeys();

        if (mc.player != null && mc.currentScreen == null) {
            handleCameraDistance();
            handleScoreboardAndMultiplayer();
        }
    }

    protected void handleScoreboardAndMultiplayer() {
        if (mc.isSingleplayer() && !MCH_Config.DebugLog) return;

        isDrawScoreboard = KeyScoreboard.isKeyPress();

        if (!isDrawScoreboard && KeyMultiplayManager.isKeyDown()) {
            PacketOpenScreen.send(5);
        }
    }

    protected void handleCameraDistance() {
        if (!MCH_ServerSettings.enableCamDistChange) return;

        boolean up = KeyCamDistUp.isKeyDown();
        boolean down = KeyCamDistDown.isKeyDown();
        if (!up && !down) return;

        int camDist = (int) W_Reflection.getThirdPersonDistance();

        if (up) {
            camDist = Math.min(camDist + CAM_DIST_STEP, CAM_DIST_MAX);
        } else {
            camDist = Math.max(camDist - CAM_DIST_STEP, CAM_DIST_MIN);
        }

        W_Reflection.setThirdPersonDistance(camDist);
    }


    private static final int CAM_DIST_STEP = 4;
    private static final int CAM_DIST_MIN = 4;
    private static final int CAM_DIST_MAX = 72;

}
