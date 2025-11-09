package com.norwood.mcheli.hud;

import java.util.Date;

import net.minecraft.util.math.MathHelper;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;

import lombok.Getter;

@Getter
public class MCH_HudItemString extends MCH_HudItem {

    private final String posX;
    private final String posY;
    private final String format;
    private final MCH_HudItemStringArgs[] args;
    private final boolean isCenteredString;

    public MCH_HudItemString(int fileLine, String posx, String posy, String fmt, String[] arg, boolean centered) {
        super(fileLine);
        this.posX = posx.toLowerCase();
        this.posY = posy.toLowerCase();
        this.format = fmt;
        int len = arg.length < 3 ? 0 : arg.length - 3;
        this.args = new MCH_HudItemStringArgs[len];

        for (int i = 0; i < len; i++) {
            this.args[i] = MCH_HudItemStringArgs.toArgs(arg[3 + i]);
        }

        this.isCenteredString = centered;
    }

    @Override
    public void execute() {
        int x = (int) (centerX + calc(this.posX));
        int y = (int) (centerY + calc(this.posY));
        int worldTime = (int) ((ac.world.getWorldTime() + 6000L) % 24000L);
        Date date = new Date();
        Object[] prm = new Object[this.args.length];
        double hp_per = ac.getMaxHP() > 0 ? (double) ac.getHP() / ac.getMaxHP() : 0.0;

        for (int i = 0; i < prm.length; i++) {
            switch (this.args[i]) {
                case NAME:
                    prm[i] = ac.getAcInfo().displayName;
                    break;
                case ALTITUDE:
                    prm[i] = Altitude;
                    break;
                case DATE:
                    prm[i] = date;
                    break;
                case MC_THOR:
                    prm[i] = worldTime / 1000;
                    break;
                case MC_TMIN:
                    prm[i] = worldTime % 1000 * 36 / 10 / 60;
                    break;
                case MC_TSEC:
                    prm[i] = worldTime % 1000 * 36 / 10 % 60;
                    break;
                case MAX_HP:
                    prm[i] = ac.getMaxHP();
                    break;
                case HP:
                    prm[i] = ac.getHP();
                    break;
                case HP_PER:
                    prm[i] = hp_per * 100.0;
                    break;
                case POS_X:
                    prm[i] = ac.posX;
                    break;
                case POS_Y:
                    prm[i] = ac.posY;
                    break;
                case POS_Z:
                    prm[i] = ac.posZ;
                    break;
                case MOTION_X:
                    prm[i] = ac.motionX;
                    break;
                case MOTION_Y:
                    prm[i] = ac.motionY;
                    break;
                case MOTION_Z:
                    prm[i] = ac.motionZ;
                    break;
                case INVENTORY:
                    prm[i] = ac.getSizeInventory();
                    break;
                case WPN_NAME:
                    prm[i] = WeaponName;
                    if (CurrentWeapon == null) {
                        return;
                    }
                    break;
                case WPN_AMMO:
                    prm[i] = WeaponAmmo;
                    if (CurrentWeapon == null) {
                        return;
                    }

                    if (CurrentWeapon.getAmmoNumMax() <= 0) {
                        return;
                    }
                    break;
                case WPN_RM_AMMO:
                    prm[i] = WeaponAllAmmo;
                    if (CurrentWeapon == null) {
                        return;
                    }

                    if (CurrentWeapon.getAmmoNumMax() <= 0) {
                        return;
                    }
                    break;
                case RELOAD_PER:
                    prm[i] = ReloadPer;
                    if (CurrentWeapon == null) {
                        return;
                    }
                    break;
                case RELOAD_SEC:
                    prm[i] = ReloadSec;
                    if (CurrentWeapon == null) {
                        return;
                    }
                    break;
                case MORTAR_DIST:
                    prm[i] = MortarDist;
                    if (CurrentWeapon == null) {
                        return;
                    }
                    break;
                case MC_VER:
                    prm[i] = "1.12.2";
                    break;
                case MOD_VER:
                    prm[i] = MCH_MOD.VER;
                    break;
                case MOD_NAME:
                    prm[i] = "MC Helicopter MOD";
                    break;
                case YAW:
                    prm[i] = MCH_Lib.getRotate360(ac.getRotYaw() + 180.0F);
                    break;
                case PITCH:
                    prm[i] = -ac.getRotPitch();
                    break;
                case ROLL:
                    prm[i] = MathHelper.wrapDegrees(ac.getRotRoll());
                    break;
                case PLYR_YAW:
                    prm[i] = MCH_Lib.getRotate360(player.rotationYaw + 180.0F);
                    break;
                case PLYR_PITCH:
                    prm[i] = -player.rotationPitch;
                    break;
                case TVM_POS_X:
                    prm[i] = TVM_PosX;
                    break;
                case TVM_POS_Y:
                    prm[i] = TVM_PosY;
                    break;
                case TVM_POS_Z:
                    prm[i] = TVM_PosZ;
                    break;
                case TVM_DIFF:
                    prm[i] = TVM_Diff;
                    break;
                case CAM_ZOOM:
                    prm[i] = ac.camera.getCameraZoom();
                    break;
                case UAV_DIST:
                    prm[i] = UAV_Dist;
                    break;
                case KEY_GUI:
                    prm[i] = MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt);
                    break;
                case THROTTLE:
                    prm[i] = ac.getCurrentThrottle() * 100.0;
            }
        }

        if (this.isCenteredString) {
            this.drawCenteredString(String.format(this.format, prm), x, y, colorSetting);
        } else {
            this.drawString(String.format(this.format, prm), x, y, colorSetting);
        }
    }
}
