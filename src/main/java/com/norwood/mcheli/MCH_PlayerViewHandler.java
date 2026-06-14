package com.norwood.mcheli;

import net.minecraft.client.Minecraft;

public class MCH_PlayerViewHandler {


    public static float playerRecoilPitch;
    public static float playerRecoilYaw;

    public static float antiRecoilPitch;
    public static float antiRecoilYaw;

    public static Minecraft minecraft = Minecraft.getMinecraft();

    public static float recoilControl = 0.8f;

    public static void applyRecoil(float pitch, float yaw, float control) {
        playerRecoilPitch += pitch;
        playerRecoilYaw += yaw;
        recoilControl = control;
    }


    public static void onUpdate() {

        if(minecraft.player == null) {
            return;
        }

        if (playerRecoilPitch > 0) {
            playerRecoilPitch *= recoilControl;
        }

        minecraft.player.rotationPitch -= playerRecoilPitch;
        minecraft.player.rotationYaw -= playerRecoilYaw;
        antiRecoilPitch += playerRecoilPitch;
        antiRecoilYaw += playerRecoilYaw;

        minecraft.player.rotationPitch += antiRecoilPitch * 0.2F;
        minecraft.player.rotationYaw += antiRecoilYaw * 0.2F;

        antiRecoilPitch *= 0.8F;
        antiRecoilYaw *= 0.8F;

        playerRecoilYaw *= 0.8F;
    }
}

