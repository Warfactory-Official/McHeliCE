package com.norwood.mcheli.hud;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;

import java.util.Date;

@Getter
public class MCH_HudItemString extends MCH_HudItem {

    private final String posX;
    private final String posY;
    private final String format;
    private final MCH_HudItemStringArgs[] args;
    private final boolean isCenteredString;
    private final Object[] prm;

    public MCH_HudItemString(int fileLine, String posx, String posy, String fmt, String[] arg, boolean centered) {
        super(fileLine);
        this.posX = toFormula(posx);
        this.posY = toFormula(posy);
        this.format = fmt;
        int len = arg.length < 3 ? 0 : arg.length - 3;
        this.args = new MCH_HudItemStringArgs[len];
         prm = new Object[this.args.length];

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
        double hp_per = ac.getMaxHP() > 0 ? (double) ac.getHP() / ac.getMaxHP() : 0.0;

        for (int i = 0; i < prm.length; i++) {
            switch (this.args[i]) {
                case NAME -> prm[i] = ac.getAcInfo().displayName;
                case ALTITUDE -> prm[i] = Altitude;
                case DATE -> prm[i] = date;
                case MC_THOR -> prm[i] = worldTime / 1000;
                case MC_TMIN -> prm[i] = worldTime % 1000 * 36 / 10 / 60;
                case MC_TSEC -> prm[i] = worldTime % 1000 * 36 / 10 % 60;
                case MAX_HP -> prm[i] = ac.getMaxHP();
                case HP -> prm[i] = ac.getHP();
                case HP_PER -> prm[i] = hp_per * 100.0;
                case POS_X -> prm[i] = ac.posX;
                case POS_Y -> prm[i] = ac.posY;
                case POS_Z -> prm[i] = ac.posZ;
                case MOTION_X -> prm[i] = ac.motionX;
                case MOTION_Y -> prm[i] = ac.motionY;
                case MOTION_Z -> prm[i] = ac.motionZ;
                case INVENTORY -> prm[i] = ac.getSizeInventory();
                case WPN_NAME -> {
                    prm[i] = WeaponName;
                    if (CurrentWeapon == null) {
                        return;
                    }
                }
                case WPN_AMMO -> {
                    prm[i] = WeaponAmmo;
                    if (CurrentWeapon == null) {
                        return;
                    }

                    if (CurrentWeapon.getMagSize() <= 0) {
                        return;
                    }
                }
                case WPN_RM_AMMO -> {
                    prm[i] = WeaponAllAmmo;
                    if (CurrentWeapon == null) {
                        return;
                    }

                    if (CurrentWeapon.getMagSize() <= 0) {
                        return;
                    }
                }
                case RELOAD_PER -> {
                    prm[i] = ReloadPer;
                    if (CurrentWeapon == null) {
                        return;
                    }
                }
                case RELOAD_SEC -> {
                    prm[i] = ReloadSec;
                    if (CurrentWeapon == null) {
                        return;
                    }
                }
                case MORTAR_DIST -> {
                    prm[i] = MortarDist;
                    if (CurrentWeapon == null) {
                        return;
                    }
                }
                case MC_VER -> prm[i] = "1.12.2";
                case MOD_VER -> prm[i] = MCH_MOD.VER;
                case MOD_NAME -> prm[i] = "MC Helicopter MOD";
                case YAW -> prm[i] = MCH_Lib.getRotate360(ac.getYaw() + 180.0F);
                case PITCH -> prm[i] = -ac.getPitch();
                case ROLL -> prm[i] = MathHelper.wrapDegrees(ac.getRoll());
                case PLYR_YAW -> prm[i] = MCH_Lib.getRotate360(player.rotationYaw + 180.0F);
                case PLYR_PITCH -> prm[i] = -player.rotationPitch;
                case TVM_POS_X -> prm[i] = TVM_PosX;
                case TVM_POS_Y -> prm[i] = TVM_PosY;
                case TVM_POS_Z -> prm[i] = TVM_PosZ;
                case TVM_DIFF -> prm[i] = TVM_Diff;
                case CAM_ZOOM -> prm[i] = ac.camera.getCameraZoom();
                case UAV_DIST -> prm[i] = UAV_Dist;
                case KEY_GUI -> prm[i] = MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt);
                case THROTTLE -> prm[i] = ac.getCurrentThrottle() * 100.0;
            }
        }

        if (this.isCenteredString) {
            this.drawCenteredString(String.format(this.format, prm), x, y, colorSetting);
        } else {
            this.drawString(String.format(this.format, prm), x, y, colorSetting);
        }
    }
}
