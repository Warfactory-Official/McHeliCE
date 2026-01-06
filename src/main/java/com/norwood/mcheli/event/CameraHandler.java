package com.norwood.mcheli.event;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.client.MCH_CameraManager;
import net.minecraft.entity.player.EntityPlayer;

public class CameraHandler {
    public final ClientCommonTickHandler HANDLER;
    public static boolean isLocked = false;
    public static int lockedSoundCount = 0;
    public static int cameraMode = 0;

    private static final int LOCKED_SOUND_COOLDOWN = 20;

    public CameraHandler(ClientCommonTickHandler HANDLER) {
        this.HANDLER = HANDLER;
    }

    protected void handleAircraftCamera(EntityPlayer player) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);

        boolean ridingValidAircraft = player != null && ac != null && !ac.isDestroyed();

        if (ridingValidAircraft) {
            if (isLocked && lockedSoundCount == 0) {
                isLocked = false;
                lockedSoundCount = LOCKED_SOUND_COOLDOWN;
                MCH_ClientTickHandlerBase.playSound("locked");
            }
        } else {
            isLocked = false;
            lockedSoundCount = 0;
        }

        MCH_CameraManager.setRidingAircraft(ac);

        if (lockedSoundCount > 0) {
            lockedSoundCount--;
        }
    }
}
