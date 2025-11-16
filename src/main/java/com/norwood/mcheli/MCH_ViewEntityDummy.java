package com.norwood.mcheli;

import com.norwood.mcheli.helper.client.MCH_CameraManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MCH_ViewEntityDummy extends EntityPlayerSP {

    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static MCH_ViewEntityDummy instance = null;
    private float zoom;

    private MCH_ViewEntityDummy(World world) {
        super(Minecraft.getMinecraft(), world, Minecraft.getMinecraft().getConnection(), new StatisticsManager(),
                new RecipeBook());
        this.hurtTime = 0;
        this.maxHurtTime = 1;
        this.setSize(1.0F, 1.0F);
    }

    public static MCH_ViewEntityDummy getInstance(World w) {
        if ((instance == null || instance.isDead) && w.isRemote) {
            instance = new MCH_ViewEntityDummy(w);
            if (Minecraft.getMinecraft().player != null) {
                instance.movementInput = Minecraft.getMinecraft().player.movementInput;
            }

            instance.setPosition(0.0, -4.0, 0.0);
            w.spawnEntity(instance);
        }

        return instance;
    }

    public static void onUnloadWorld() {
        if (instance != null) {
            instance.setDead();
            instance = null;
        }
    }

    public static void setCameraPosition(double x, double y, double z) {
        if (instance != null) {
            instance.prevPosX = x;
            instance.prevPosY = y;
            instance.prevPosZ = z;
            instance.lastTickPosX = x;
            instance.lastTickPosY = y;
            instance.lastTickPosZ = z;
            instance.posX = x;
            instance.posY = y;
            instance.posZ = z;
        }
    }

    public @NotNull AxisAlignedBB getEntityBoundingBox() {
        return ZERO_AABB;
    }

    public void onUpdate() {}

    public void update(MCH_Camera camera) {
        if (camera != null) {
            this.zoom = camera.getCameraZoom();
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
            this.rotationYaw = camera.rotationYaw;
            this.rotationPitch = camera.rotationPitch;
            this.prevPosX = camera.posX;
            this.prevPosY = camera.posY;
            this.prevPosZ = camera.posZ;
            this.posX = camera.posX;
            this.posY = camera.posY;
            this.posZ = camera.posZ;
            MCH_CameraManager.setCameraZoom(this.zoom);
        }
    }

    public float getFovModifier() {
        return super.getFovModifier() * (1.0F / this.zoom);
    }

    public float getEyeHeight() {
        return 0.0F;
    }
}
