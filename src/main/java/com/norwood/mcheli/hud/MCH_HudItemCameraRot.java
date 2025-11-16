package com.norwood.mcheli.hud;

import com.norwood.mcheli.MCH_Camera;
import com.norwood.mcheli.MCH_Lib;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
public class MCH_HudItemCameraRot extends MCH_HudItem {

    private final String drawPosX;
    private final String drawPosY;

    public MCH_HudItemCameraRot(int fileLine, String posx, String posy) {
        super(fileLine);
        this.drawPosX = toFormula(posx);
        this.drawPosY = toFormula(posy);
    }

    @Override
    public void execute() {
        this.drawCommonGunnerCamera(ac, ac.camera, colorSetting, centerX + calc(this.drawPosX),
                centerY + calc(this.drawPosY));
    }

    private void drawCommonGunnerCamera(Entity ac, MCH_Camera camera, int color, double posX, double posY) {
        if (camera != null) {
            double[] line = new double[] { posX - 21.0, posY - 11.0, posX + 21.0, posY - 11.0, posX + 21.0, posY + 11.0,
                    posX - 21.0, posY + 11.0 };
            this.drawLine(line, color, 2);
            line = new double[] { posX - 21.0, posY, posX, posY, posX + 21.0, posY, posX, posY, posX, posY - 11.0, posX,
                    posY, posX, posY + 11.0, posX, posY };
            this.drawLineStipple(line, color, 1, 52428);
            float pitch = camera.rotationPitch;
            if (pitch < -30.0F) {
                pitch = -30.0F;
            }

            if (pitch > 70.0F) {
                pitch = 70.0F;
            }

            pitch -= 20.0F;
            pitch = (float) (pitch * 0.16);
            float yaw = (float) MCH_Lib.getRotateDiff(ac.rotationYaw, camera.rotationYaw);
            yaw *= 2.0F;
            if (yaw < -50.0F) {
                yaw = -50.0F;
            }

            if (yaw > 50.0F) {
                yaw = 50.0F;
            }

            yaw = (float) (yaw * 0.34);
            line = new double[] {
                    posX + yaw - 3.0,
                    posY + pitch - 2.0,
                    posX + yaw + 3.0,
                    posY + pitch - 2.0,
                    posX + yaw + 3.0,
                    posY + pitch + 2.0,
                    posX + yaw - 3.0,
                    posY + pitch + 2.0
            };
            this.drawLine(line, color, 2);
        }
    }
}
