package com.norwood.mcheli.lweapon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.weapon.MCH_WeaponGuidanceSystem;
import com.norwood.mcheli.wrapper.W_McClient;

@SideOnly(Side.CLIENT)
public class MCH_GuiLightWeapon extends MCH_Gui {

    public MCH_GuiLightWeapon(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        if (MCH_ItemLightWeaponBase.isHeld(player)) {
            Entity re = player.getRidingEntity();
            return !(re instanceof MCH_EntityAircraft) && !(re instanceof MCH_EntityGLTD);
        }

        return false;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView) {
            GL11.glLineWidth(scaleFactor);
            if (this.isDrawGui(player)) {
                MCH_WeaponGuidanceSystem gs = MCH_ClientLightWeaponTickHandler.gs;
                if (gs != null && MCH_ClientLightWeaponTickHandler.weapon != null &&
                        MCH_ClientLightWeaponTickHandler.weapon.getInfo() != null) {
                    PotionEffect pe = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
                    if (pe != null) {
                        this.drawNightVisionNoise();
                    }

                    GlStateManager.enableBlend();
                    GlStateManager.color(0.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    double dist = 0.0;
                    if (gs.getTargetEntity() != null) {
                        double dx = gs.getTargetEntity().posX - player.posX;
                        double dz = gs.getTargetEntity().posZ - player.posZ;
                        dist = Math.sqrt(dx * dx + dz * dz);
                    }

                    boolean canFire = MCH_ClientLightWeaponTickHandler.weaponMode == 0 || dist >= 40.0 ||
                            gs.getLockCount() <= 0;
                    if ("fgm148".equalsIgnoreCase(MCH_ItemLightWeaponBase.getName(player.getHeldItemMainhand()))) {
                        this.drawGuiFGM148(player, gs, canFire, player.getHeldItemMainhand());
                        this.drawKeyBind(-805306369, true);
                    } else {
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        W_McClient.MOD_bindTexture("textures/gui/stinger.png");
                        double size = 512.0;

                        while (size < this.width || size < this.height) {
                            size *= 2.0;
                        }

                        this.drawTexturedModalRectRotate(-(size - this.width) / 2.0, -(size - this.height) / 2.0 - 20.0,
                                size, size, 0.0, 0.0, 256.0, 256.0, 0.0F);
                        this.drawKeyBind(-805306369, false);
                    }

                    GlStateManager.disableBlend();
                    this.drawLock(-14101432, -2161656, gs.getLockCount(), gs.getLockCountMax());
                    this.drawRange(player, gs, canFire, -14101432, -2161656);
                }
            }
        }
    }

    public void drawNightVisionNoise() {
        GlStateManager.enableBlend();
        GlStateManager.color(0.0F, 1.0F, 0.0F, 0.3F);
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        W_McClient.MOD_bindTexture("textures/gui/alpha.png");
        this.drawTexturedModalRectRotate(0.0, 0.0, this.width, this.height, this.rand.nextInt(256),
                this.rand.nextInt(256), 256.0, 256.0, 0.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    void drawLock(int color, int colorLock, int cntLock, int cntMax) {
        int posX = this.centerX;
        int posY = this.centerY + 20;
        drawRect(posX - 20, posY + 20 + 1, posX - 20 + 40, posY + 20 + 1 + 1 + 3 + 1, color);
        float lock = (float) cntLock / cntMax;
        drawRect(posX - 20 + 1, posY + 20 + 1 + 1, posX - 20 + 1 + (int) (38.0 * lock), posY + 20 + 1 + 1 + 3,
                -2161656);
    }

    void drawRange(EntityPlayer player, MCH_WeaponGuidanceSystem gs, boolean canFire, int color1, int color2) {
        String msgLockDist = "[--.--]";
        int color = color2;
        if (gs.getLockCount() > 0) {
            Entity target = gs.getLockingEntity();
            if (target != null) {
                double dx = target.posX - player.posX;
                double dz = target.posZ - player.posZ;
                msgLockDist = String.format("[%.2f]", Math.sqrt(dx * dx + dz * dz));
                color = canFire ? color1 : color2;
                if (!MCH_Config.HideKeybind.prmBool && gs.isLockComplete()) {
                    String k = MCH_KeyName.getDescOrName(MCH_Config.KeyAttack.prmInt);
                    this.drawCenteredString("Shot : " + k, this.centerX, this.centerY + 65, -805306369);
                }
            }
        }

        this.drawCenteredString(msgLockDist, this.centerX, this.centerY + 50, color);
    }

    void drawGuiFGM148(EntityPlayer player, MCH_WeaponGuidanceSystem gs, boolean canFire, ItemStack itemStack) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        double fac = Math.min(this.width / 800.0, this.height / 700.0);
        int size = (int) (1024.0 * fac);
        size = size / 64 * 64;
        fac = size / 1024.0;
        double left = (double) -(size - this.width) / 2;
        double top = (double) -(size - this.height) / 2 - 20;
        double right = left + size;
        double bottom = top + size;
        Vec3d pos = MCH_ClientLightWeaponTickHandler.getMartEntityPos();
        if (gs.getLockCount() > 0) {
            int scale = scaleFactor > 0 ? scaleFactor : 2;
            if (pos == null) {
                pos = new Vec3d((double) this.width / 2 * scale, (double) this.height / 2 * scale, 0.0);
            }

            double IX = 280.0 * fac;
            double IY = 370.0 * fac;
            double cx = pos.x / scale;
            double cy = this.height - pos.y / scale;
            double sx = MCH_Lib.RNG(cx, left + IX, right - IX);
            double sy = MCH_Lib.RNG(cy, top + IY, bottom - IY);
            if (gs.getLockCount() >= gs.getLockCountMax() / 2) {
                this.drawLine(new double[] { -1.0, sy, this.width + 1, sy, sx, -1.0, sx, this.height + 1 },
                        -1593835521);
            }

            if (player.ticksExisted % 6 >= 3) {
                pos = MCH_ClientLightWeaponTickHandler.getMartEntityBBPos();
                if (pos == null) {
                    pos = new Vec3d(((double) this.width / 2 - 65) * scale, ((double) this.height / 2 + 50) * scale,
                            0.0);
                }

                double bx = pos.x / scale;
                double by = this.height - pos.y / scale;
                double dx = Math.abs(cx - bx);
                double dy = Math.abs(cy - by);
                double p = 1.0 - (double) gs.getLockCount() / gs.getLockCountMax();
                dx = MCH_Lib.RNG(dx, 25.0, 70.0);
                dy = MCH_Lib.RNG(dy, 15.0, 70.0);
                dx += (70.0 - dx) * p;
                dy += (70.0 - dy) * p;
                int lx = 10;
                int ly = 6;
                this.drawLine(new double[] { sx - dx, sy - dy + ly, sx - dx, sy - dy, sx - dx + lx, sy - dy },
                        -1593835521, 3);
                this.drawLine(new double[] { sx + dx, sy - dy + ly, sx + dx, sy - dy, sx + dx - lx, sy - dy },
                        -1593835521, 3);
                dy /= 6.0;
                this.drawLine(new double[] { sx - dx, sy + dy - ly, sx - dx, sy + dy, sx - dx + lx, sy + dy },
                        -1593835521, 3);
                this.drawLine(new double[] { sx + dx, sy + dy - ly, sx + dx, sy + dy, sx + dx - lx, sy + dy },
                        -1593835521, 3);
            }
        }

        drawRect(-1, -1, (int) left + 1, this.height + 1, -16777216);
        drawRect((int) right - 1, -1, this.width + 1, this.height + 1, -16777216);
        drawRect(-1, -1, this.width + 1, (int) top + 1, -16777216);
        drawRect(-1, (int) bottom - 1, this.width + 1, this.height + 1, -16777216);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        W_McClient.MOD_bindTexture("textures/gui/javelin.png");
        this.drawTexturedModalRectRotate(left, top, size, size, 0.0, 0.0, 256.0, 256.0, 0.0F);
        W_McClient.MOD_bindTexture("textures/gui/javelin2.png");
        PotionEffect pe = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
        if (pe == null) {
            double x = 247.0;
            double y = 211.0;
            double w = 380.0;
            double h = 350.0;
            this.drawTexturedRect(left + x * fac, top + y * fac, (w - x) * fac, (h - y) * fac, x, y, w - x, h - y,
                    1024.0, 1024.0);
        }

        if (player.getItemInUseMaxCount() <= 60) {
            double x = 130.0;
            double y = 334.0;
            double w = 257.0;
            double h = 455.0;
            this.drawTexturedRect(left + x * fac, top + y * fac, (w - x) * fac, (h - y) * fac, x, y, w - x, h - y,
                    1024.0, 1024.0);
        }

        if (MCH_ClientLightWeaponTickHandler.selectedZoom == 0) {
            double x = 387.0;
            double y = 211.0;
            double w = 510.0;
            double h = 350.0;
            this.drawTexturedRect(left + x * fac, top + y * fac, (w - x) * fac, (h - y) * fac, x, y, w - x, h - y,
                    1024.0, 1024.0);
        }

        if (MCH_ClientLightWeaponTickHandler.selectedZoom ==
                MCH_ClientLightWeaponTickHandler.weapon.getInfo().zoom.length - 1) {
            double x = 511.0;
            double y = 211.0;
            double w = 645.0;
            double h = 350.0;
            this.drawTexturedRect(left + x * fac, top + y * fac, (w - x) * fac, (h - y) * fac, x, y, w - x, h - y,
                    1024.0, 1024.0);
        }

        if (gs.getLockCount() > 0) {
            double x = 643.0;
            double y = 211.0;
            double w = 775.0;
            double h = 350.0;
            this.drawTexturedRect(left + x * fac, top + y * fac, (w - x) * fac, (h - y) * fac, x, y, w - x, h - y,
                    1024.0, 1024.0);
        }

        double x = 768.0;
        double y;
        double w = 890.0;
        double h;
        if (MCH_ClientLightWeaponTickHandler.weaponMode == 1) {
            y = 340.0;
            h = 455.0;
        } else {
            y = 456.0;
            h = 565.0;
        }
        this.drawTexturedRect(left + x * fac, top + y * fac, (w - x) * fac, (h - y) * fac, x, y, w - x, h - y, 1024.0,
                1024.0);

        if (!canFire) {
            double var53 = 379.0;
            double var64 = 670.0;
            double var75 = 511.0;
            double var86 = 810.0;
            this.drawTexturedRect(
                    left + var53 * fac, top + var64 * fac, (var75 - var53) * fac, (var86 - var64) * fac, var53, var64,
                    var75 - var53, var86 - var64, 1024.0, 1024.0);
        }

        if (itemStack.getMetadata() >= itemStack.getMaxDamage()) {
            double var54 = 512.0;
            double var65 = 670.0;
            double var76 = 645.0;
            double var87 = 810.0;
            this.drawTexturedRect(
                    left + var54 * fac, top + var65 * fac, (var76 - var54) * fac, (var87 - var65) * fac, var54, var65,
                    var76 - var54, var87 - var65, 1024.0, 1024.0);
        }

        if (gs.getLockCount() < gs.getLockCountMax()) {
            double var55 = 646.0;
            double var66 = 670.0;
            double var77 = 776.0;
            double var88 = 810.0;
            this.drawTexturedRect(
                    left + var55 * fac, top + var66 * fac, (var77 - var55) * fac, (var88 - var66) * fac, var55, var66,
                    var77 - var55, var88 - var66, 1024.0, 1024.0);
        }

        if (pe != null) {
            double var56 = 768.0;
            double var67 = 562.0;
            double var78 = 890.0;
            double var89 = 694.0;
            this.drawTexturedRect(
                    left + var56 * fac, top + var67 * fac, (var78 - var56) * fac, (var89 - var67) * fac, var56, var67,
                    var78 - var56, var89 - var67, 1024.0, 1024.0);
        }
    }

    public void drawKeyBind(int color, boolean canSwitchMode) {
        int OffX = this.centerX + 55;
        int OffY = this.centerY + 40;
        this.drawString("CAM MODE :", OffX, OffY + 10, color);
        this.drawString("ZOOM      :", OffX, OffY + 20, color);
        if (canSwitchMode) {
            this.drawString("MODE      :", OffX, OffY + 30, color);
        }

        OffX += 60;
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt), OffX, OffY + 10, color);
        this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt), OffX, OffY + 20, color);
        if (canSwitchMode) {
            this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt), OffX, OffY + 30, color);
        }
    }
}
