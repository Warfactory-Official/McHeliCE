package com.norwood.mcheli.lweapon;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.norwood.mcheli.MCH_ClientTickHandlerBase;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Key;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.networking.packet.control.PacketPlayerLightWeaponControl;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponCreator;
import com.norwood.mcheli.weapon.MCH_WeaponGuidanceSystem;
import com.norwood.mcheli.wrapper.*;

public class MCH_ClientLightWeaponTickHandler extends MCH_ClientTickHandlerBase {

    public static int reloadCount;
    public static int lockonSoundCount;
    public static int weaponMode;
    public static int selectedZoom;
    public static Entity markEntity = null;
    public static Vec3d markPos = Vec3d.ZERO;
    public static MCH_WeaponGuidanceSystem gs = new MCH_WeaponGuidanceSystem();
    public static double lockRange = 120.0;
    protected static MCH_WeaponBase weapon;
    private static final FloatBuffer screenPos = BufferUtils.createFloatBuffer(3);
    private static final FloatBuffer screenPosBB = BufferUtils.createFloatBuffer(3);
    private static final FloatBuffer matModel = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer matProjection = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer matViewport = BufferUtils.createIntBuffer(16);
    public MCH_Key KeyAttack;
    public MCH_Key KeyUseWeapon;
    public MCH_Key KeySwWeaponMode;
    public MCH_Key KeyZoom;
    public MCH_Key KeyCameraMode;
    public MCH_Key[] Keys;
    protected boolean isHeldItem = false;
    protected boolean isBeforeHeldItem = false;
    protected EntityPlayer prevThePlayer = null;
    protected ItemStack prevItemStack = ItemStack.EMPTY;

    public MCH_ClientLightWeaponTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.updateKeybind(config);
        gs.canLockInAir = false;
        gs.canLockOnGround = false;
        gs.canLockInWater = false;
        gs.setLockCountMax(40);
        gs.lockRange = 120.0;
        lockonSoundCount = 0;
        this.initWeaponParam(null);
    }

    public static void markEntity(Entity entity, double x, double y, double z) {
        if (gs.getLockingEntity() == entity) {
            GL11.glGetFloat(2982, matModel);
            GL11.glGetFloat(2983, matProjection);
            GL11.glGetInteger(2978, matViewport);
            GLU.gluProject((float) x, (float) y, (float) z, matModel, matProjection, matViewport, screenPos);
            MCH_AircraftInfo i = entity instanceof MCH_EntityAircraft ? ((MCH_EntityAircraft) entity).getAcInfo() :
                    null;
            float w = entity.width > entity.height ? entity.width : (i != null ? i.markerWidth : entity.height);
            float h = i != null ? i.markerHeight : entity.height;
            GLU.gluProject((float) x + w, (float) y + h, (float) z + w, matModel, matProjection, matViewport,
                    screenPosBB);
            markEntity = entity;
        }
    }

    @Nullable
    public static Vec3d getMartEntityPos() {
        return gs.getLockingEntity() == markEntity && markEntity != null ?
                new Vec3d(screenPos.get(0), screenPos.get(1), screenPos.get(2)) : null;
    }

    @Nullable
    public static Vec3d getMartEntityBBPos() {
        return gs.getLockingEntity() == markEntity && markEntity != null ?
                new Vec3d(screenPosBB.get(0), screenPosBB.get(1), screenPosBB.get(2)) : null;
    }

    public static int getPotionNightVisionDuration(EntityPlayer player) {
        PotionEffect cpe = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
        return cpe != null ? cpe.getDuration() : 0;
    }

    public void initWeaponParam(EntityPlayer player) {
        reloadCount = 0;
        weaponMode = 0;
        selectedZoom = 0;
    }

    @Override
    public void updateKeybind(MCH_Config config) {
        this.KeyAttack = new MCH_Key(MCH_Config.KeyAttack.prmInt);
        this.KeyUseWeapon = new MCH_Key(MCH_Config.KeyUseWeapon.prmInt);
        this.KeySwWeaponMode = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
        this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
        this.KeyCameraMode = new MCH_Key(MCH_Config.KeyCameraMode.prmInt);
        this.Keys = new MCH_Key[] { this.KeyAttack, this.KeyUseWeapon, this.KeySwWeaponMode, this.KeyZoom,
                this.KeyCameraMode };
    }

    @Override
    protected void onTick(boolean inGUI) {
        for (MCH_Key k : this.Keys) {
            k.update();
        }

        this.isBeforeHeldItem = this.isHeldItem;
        EntityPlayer player = this.mc.player;
        if (this.prevThePlayer == null || this.prevThePlayer != player) {
            this.initWeaponParam(player);
            this.prevThePlayer = player;
        }

        ItemStack is = player != null ? player.getHeldItemMainhand() : ItemStack.EMPTY;
        if (player == null || player.getRidingEntity() instanceof MCH_EntityGLTD ||
                player.getRidingEntity() instanceof MCH_EntityAircraft) {
            is = ItemStack.EMPTY;
        }

        if (gs.getLockingEntity() == null) {
            markEntity = null;
        }

        if (!is.isEmpty() && is.getItem() instanceof MCH_ItemLightWeaponBase lweapon) {
            if (this.prevItemStack.isEmpty() || !this.prevItemStack.isItemEqual(is) &&
                    !this.prevItemStack.getTranslationKey().equals(is.getTranslationKey())) {
                this.initWeaponParam(player);
                weapon = MCH_WeaponCreator.createWeapon(player.world, MCH_ItemLightWeaponBase.getName(is), Vec3d.ZERO,
                        0.0F, 0.0F, null, false);
                if (weapon != null && weapon.getInfo() != null && weapon.getGuidanceSystem() != null) {
                    gs = weapon.getGuidanceSystem();
                }
            }

            if (weapon == null || gs == null) {
                return;
            }

            gs.setWorld(player.world);
            gs.lockRange = lockRange;
            if (player.getItemInUseMaxCount() > 10) {
                selectedZoom = selectedZoom % weapon.getInfo().zoom.length;
                W_Reflection.setCameraZoom(weapon.getInfo().zoom[selectedZoom]);
            } else {
                W_Reflection.restoreCameraZoom();
            }

            if (is.getMetadata() < is.getMaxDamage()) {
                if (player.getItemInUseMaxCount() > 10) {
                    gs.lock(player);
                    if (gs.getLockCount() > 0) {
                        if (lockonSoundCount > 0) {
                            lockonSoundCount--;
                        } else {
                            lockonSoundCount = 7;
                            lockonSoundCount = (int) (lockonSoundCount *
                                    (1.0 - (float) gs.getLockCount() / gs.getLockCountMax()));
                            if (lockonSoundCount < 3) {
                                lockonSoundCount = 2;
                            }

                            W_McClient.MOD_playSoundFX("lockon", 1.0F, 1.0F);
                        }
                    }
                } else {
                    W_Reflection.restoreCameraZoom();
                    gs.clearLock();
                }

                reloadCount = 0;
            } else {
                lockonSoundCount = 0;
                if (W_EntityPlayer.hasItem(player, lweapon.bullet) && player.getItemInUseCount() <= 0) {
                    if (reloadCount == 10) {
                        W_McClient.MOD_playSoundFX("fim92_reload", 1.0F, 1.0F);
                    }

                    if (reloadCount < 40) {
                        reloadCount++;
                        if (reloadCount == 40) {
                            this.onCompleteReload();
                        }
                    }
                } else {
                    reloadCount = 0;
                }

                gs.clearLock();
            }

            if (!inGUI) {
                this.playerControl(player, is, (MCH_ItemLightWeaponBase) is.getItem());
            }

            this.isHeldItem = MCH_ItemLightWeaponBase.isHeld(player);
        } else {
            lockonSoundCount = 0;
            reloadCount = 0;
            this.isHeldItem = false;
        }

        if (this.isBeforeHeldItem != this.isHeldItem) {
            MCH_Lib.DbgLog(true, "LWeapon cancel");
            if (!this.isHeldItem) {
                if (getPotionNightVisionDuration(player) < 250) {
                    PacketPlayerLightWeaponControl packet = new PacketPlayerLightWeaponControl();
                    packet.camMode = 1;
                    packet.sendToServer();
                    player.removePotionEffect(MobEffects.NIGHT_VISION);
                }

                W_Reflection.restoreCameraZoom();
            }
        }

        this.prevItemStack = is;
        gs.update();
    }

    protected void onCompleteReload() {
        PacketPlayerLightWeaponControl packet = new PacketPlayerLightWeaponControl();
        packet.cmpReload = 1;
        packet.sendToServer();
    }

    protected void playerControl(EntityPlayer player, ItemStack is, MCH_ItemLightWeaponBase item) {
        PacketPlayerLightWeaponControl packet = new PacketPlayerLightWeaponControl();
        boolean send = false;
        boolean autoShot = MCH_Config.LWeaponAutoFire.prmBool && is.getMetadata() < is.getMaxDamage() &&
                gs.isLockComplete();

        if (this.KeySwWeaponMode.isKeyDown() && weapon.numMode > 1) {
            weaponMode = (weaponMode + 1) % weapon.numMode;
            W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
        }

        if (this.KeyAttack.isKeyPress() || autoShot) {
            boolean result = false;
            if (is.getMetadata() < is.getMaxDamage() && gs.isLockComplete()) {
                boolean canFire = true;
                if (weaponMode > 0 && gs.getTargetEntity() != null) {
                    double dx = gs.getTargetEntity().posX - player.posX;
                    double dz = gs.getTargetEntity().posZ - player.posZ;
                    canFire = Math.sqrt(dx * dx + dz * dz) >= 40.0;
                }

                if (canFire) {
                    packet.useWeapon = true;
                    packet.useWeaponOption1 = W_Entity.getEntityId(gs.lastLockEntity);
                    packet.useWeaponOption2 = weaponMode;
                    packet.useWeaponPosX = player.posX;
                    packet.useWeaponPosY = player.posY + player.getEyeHeight();
                    packet.useWeaponPosZ = player.posZ;
                    gs.clearLock();
                    send = true;
                    result = true;
                }
            }

            if (this.KeyAttack.isKeyDown() && !result && player.getItemInUseMaxCount() > 5) {
                playSoundNG();
            }
        }

        if (this.KeyZoom.isKeyDown()) {
            int prevZoom = selectedZoom;
            selectedZoom = (selectedZoom + 1) % weapon.getInfo().zoom.length;
            if (prevZoom != selectedZoom) {
                playSound("zoom", 0.5F, 1.0F);
            }
        }

        if (this.KeyCameraMode.isKeyDown()) {
            PotionEffect pe = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
            MCH_Lib.DbgLog(true, "LWeapon NV %s", pe != null ? "ON->OFF" : "OFF->ON");
            if (pe != null) {
                player.removePotionEffect(MobEffects.NIGHT_VISION);
                packet.camMode = 1;
                send = true;
                W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
            } else if (player.getItemInUseMaxCount() > 60) {
                packet.camMode = 2;
                send = true;
                W_McClient.MOD_playSoundFX("pi", 0.5F, 0.9F);
            } else {
                playSoundNG();
            }
        }

        if (send) {
            packet.sendToServer();
        }
    }
}
