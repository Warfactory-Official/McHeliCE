package com.norwood.mcheli;

import com.norwood.mcheli.helper.client.MCH_CameraManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MCH_ViewEntityDummy extends EntityPlayerSP {

    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static MCH_ViewEntityDummy instance = null;
    private float zoom = 1.0F;

    private MCH_ViewEntityDummy(World world) {
        super(Minecraft.getMinecraft(), world, Minecraft.getMinecraft().getConnection(),
                new StatisticsManager(), new RecipeBook());
        this.noClip = true;
        this.setSize(0.0F, 0.0F);
    }

    public static MCH_ViewEntityDummy getInstance(World w) {
        if ((instance == null || instance.isDead) && w.isRemote) {
            instance = new MCH_ViewEntityDummy(w);
            instance.movementInput = new MovementInput();
            instance.setPosition(0.0, -4.0, 0.0);
            w.spawnEntity(instance);
        }
        return instance;
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

    public void update(MCH_Camera camera) {
        if (camera == null) return;

        this.zoom = camera.getCameraZoom();

        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        this.rotationYaw = camera.rotationYaw;
        this.rotationPitch = camera.rotationPitch;
        this.posX = camera.posX;
        this.posY = camera.posY;
        this.posZ = camera.posZ;

        this.lastTickPosX = this.prevPosX;
        this.lastTickPosY = this.prevPosY;
        this.lastTickPosZ = this.prevPosZ;

        MCH_CameraManager.setCameraZoom(this.zoom);
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public boolean isEntityInvulnerable(@NotNull DamageSource source) { return true; }

    @Override
    public @NotNull AxisAlignedBB getEntityBoundingBox() { return ZERO_AABB; }

    @Override
    public float getEyeHeight() { return 0.0F; }
}
