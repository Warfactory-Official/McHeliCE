package com.norwood.mcheli;

import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_EntityRenderer;
import com.norwood.mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MCH_Camera {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_NIGHTVISION = 1;
    public static final int MODE_THERMALVISION = 2;

    private final World worldObj;
    public double posX;
    public double posY;
    public double posZ;
    public float rotationYaw;
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;
    public float partRotationYaw;
    public float partRotationPitch;
    public float prevPartRotationYaw;
    public float prevPartRotationPitch;
    private float zoom;
    private final int[] mode;
    private final boolean[] canUseShader;
    private final int[] lastMode;
    private int lastZoomDir;

    public MCH_Camera(World w, Entity p) {
        this.worldObj = w;
        this.mode = new int[] { MODE_NORMAL, MODE_NORMAL };
        this.zoom = 1.0F;
        this.lastMode = new int[this.getUserMax()];
        this.lastZoomDir = 0;
        this.canUseShader = new boolean[this.getUserMax()];
    }

    public MCH_Camera(World w, Entity p, double x, double y, double z) {
        this(w, p);
        this.setPosition(x, y, z);
        this.setCameraZoom(1.0F);
    }

    public int getUserMax() {
        return this.mode.length;
    }

    public void initCamera(int uid, @Nullable Entity viewer) {
        this.setCameraZoom(1.0F);
        this.setMode(uid, MODE_NORMAL);
        this.updateViewer(uid, viewer);
    }

    public void setMode(int uid, int m) {
        if (this.isValidUid(uid)) {
            this.mode[uid] = m < 0 ? 0 : m % this.getModeNum(uid);

            switch (this.mode[uid]) {

                case MODE_NIGHTVISION:
                    if (this.worldObj.isRemote) {
                        W_EntityRenderer.currentShader = new ResourceLocation(Tags.MODID,
                                "shaders/post/nightvision.json");
                    }
                    break;

                case MODE_THERMALVISION:
                    if (this.worldObj.isRemote) {
                        W_EntityRenderer.currentShader = new ResourceLocation(Tags.MODID, "shaders/post/thermal.json");
                    }
                    break;
                case MODE_NORMAL:

                default:
                    if (this.worldObj.isRemote) {
                        W_EntityRenderer.currentShader = null;
                        W_EntityRenderer.deactivateShader();
                    }
                    break;
            }
        }
    }

    public void setShaderSupport(int uid, Boolean b) {
        if (this.isValidUid(uid)) {
            this.setMode(uid, MODE_NORMAL);
            this.canUseShader[uid] = b;
        }
    }

    public boolean isValidUid(int uid) {
        return uid >= 0 && uid < this.getUserMax();
    }

    public int getModeNum(int uid) {
        if (!this.isValidUid(uid)) {
            return 2;
        } else {
            return this.canUseShader[uid] ? 3 : 2;
        }
    }

    public int getMode(int uid) {
        return this.isValidUid(uid) ? this.mode[uid] : MODE_NORMAL;
    }

    public String getModeName(int uid) {
        return switch (this.getMode(uid)) {
            case MODE_NIGHTVISION -> "NIGHT VISION";
            case MODE_THERMALVISION -> "THERMAL VISION";
            default -> "";
        };
    }

    public void updateViewer(int uid, @Nullable Entity viewer) {
        if (this.isValidUid(uid) && viewer != null) {
            if (W_Lib.isEntityLivingBase(viewer) && !viewer.isDead) {

                // Remove night vision when leaving special modes
                if (this.getMode(uid) == MODE_NORMAL && this.lastMode[uid] != MODE_NORMAL) {
                    PotionEffect pe = W_Entity.getActivePotionEffect(viewer, MobEffects.NIGHT_VISION);
                    if (pe != null && pe.getDuration() > 0 && pe.getDuration() < 500) {
                        if (viewer.world.isRemote) {
                            W_Entity.removePotionEffectClient(viewer, MobEffects.NIGHT_VISION);
                        } else {
                            W_Entity.removePotionEffect(viewer, MobEffects.NIGHT_VISION);
                        }
                    }
                }

                // Apply night vision when in NV or thermal mode
                if (this.getMode(uid) == MODE_NIGHTVISION || this.getMode(uid) == MODE_THERMALVISION) {
                    PotionEffect pe = W_Entity.getActivePotionEffect(viewer, MobEffects.NIGHT_VISION);
                    if ((pe == null || pe.getDuration() < 500) && !viewer.world.isRemote) {
                        W_Entity.addPotionEffect(viewer,
                                new PotionEffect(MobEffects.NIGHT_VISION, 250, 0, true, false));
                    }
                }
            }

            this.lastMode[uid] = this.getMode(uid);
        }
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public float getCameraZoom() {
        return this.zoom;
    }

    public void setCameraZoom(float z) {
        float prevZoom = this.zoom;
        this.zoom = Math.max(z, 1.0F);
        this.lastZoomDir = Float.compare(this.zoom, prevZoom);
    }

    public int getLastZoomDir() {
        return this.lastZoomDir;
    }
}
