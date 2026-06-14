package com.norwood.mcheli;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helper.client.MCH_CameraManager;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MCH_3rdCamera extends EntityPlayerSP {

    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    public static float lerpAmount = 0.3F;
    private static MCH_3rdCamera instance;

    private MCH_EntityAircraft aircraft;

    private MCH_3rdCamera(World world) {
        super(Minecraft.getMinecraft(), world, Objects.requireNonNull(Minecraft.getMinecraft().getConnection()),
                new StatisticsManager(), new RecipeBook());
        this.noClip = true;
        this.setSize(0.0F, 0.0F);
        this.movementInput = new MovementInput();
    }

    public static MCH_3rdCamera getInstance(World world, MCH_EntityAircraft ac) {
        if (instance != null && instance.world != world) {
            clear();
        }
        if ((instance == null || instance.isDead) && world.isRemote) {
            instance = new MCH_3rdCamera(world);
            instance.setPosition(ac.getX(), ac.getY()+4, ac.getZ());
        }
        return instance;
    }

    public static void clear() {
        if (instance != null) {
            instance.setDead();
            instance.aircraft = null;
            instance = null;
        }
    }

    public void update(MCH_EntityAircraft aircraft, EntityPlayerSP player) {
        if (aircraft == null || aircraft.isDead || player == null) {
            setDead();
            return;
        }

        this.aircraft = aircraft;

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;

        Vec3d cameraOffset = getCameraOffset(aircraft);
        Vec3d upRoll = getRolledUpVector(aircraft);
        Vec3d target = new Vec3d(aircraft.posX, aircraft.posY, aircraft.posZ).add(upRoll.scale(cameraOffset.y));

        this.setPosition(
                this.posX + (target.x - this.posX) * lerpAmount,
                this.posY + (target.y - this.posY) * lerpAmount,
                this.posZ + (target.z - this.posZ) * lerpAmount);

        this.rotationYaw = player.rotationYaw;
        this.rotationPitch = clampPitch(player.rotationPitch + 2.0F);

        if (this.rotationYaw - this.prevRotationYaw >= 180.0F) {
            this.prevRotationYaw += 360.0F;
        } else if (this.rotationYaw - this.prevRotationYaw < -180.0F) {
            this.prevRotationYaw -= 360.0F;
        }

        MCH_CameraManager.setCameraZoom(aircraft.camera.getCameraZoom());
    }

    private static Vec3d getCameraOffset(MCH_EntityAircraft aircraft) {
        if (aircraft instanceof MCH_EntityPlane) {
            return new Vec3d(0.0, 10.0, 0.0);
        }
        if (aircraft instanceof MCH_EntityHeli) {
            return new Vec3d(0.0, 5.0, 0.0);
        }
        if (aircraft instanceof MCH_EntityTank) {
            return new Vec3d(0.0, 4.0, 0.0);
        }
        if (aircraft instanceof MCH_EntityVehicle) {
            return new Vec3d(0.0, 2.0, 0.0);
        }
        if (aircraft instanceof MCH_EntityShip) {
            return new Vec3d(0.0, 4.0, 0.0);
        }
        return Vec3d.ZERO;
    }

    private static Vec3d getRolledUpVector(MCH_EntityAircraft aircraft) {
        double yawRad = Math.toRadians(aircraft.rotationYaw);
        double pitchRad = Math.toRadians(aircraft.rotationPitch);
        double rollRad = Math.toRadians(aircraft.getRoll());
        double cy = Math.cos(yawRad);
        double sy = Math.sin(yawRad);
        double cp = Math.cos(pitchRad);
        double sp = Math.sin(pitchRad);

        Vec3d forward = new Vec3d(-sy * cp, -sp, cy * cp).normalize();
        Vec3d worldUp = new Vec3d(0.0, 1.0, 0.0);
        Vec3d right = forward.crossProduct(worldUp);
        if (right.lengthSquared() < 1.0E-12) {
            right = new Vec3d(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3d up = right.crossProduct(forward).normalize();
        return rotateAroundAxis(up, forward, rollRad).normalize();
    }

    private static Vec3d rotateAroundAxis(Vec3d vector, Vec3d axisUnit, double angle) {
        Vec3d axis = axisUnit.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3d cross = axis.crossProduct(vector);
        double dot = axis.dotProduct(vector);
        double x = vector.x * cos + cross.x * sin + axis.x * dot * (1.0 - cos);
        double y = vector.y * cos + cross.y * sin + axis.y * dot * (1.0 - cos);
        double z = vector.z * cos + cross.z * sin + axis.z * dot * (1.0 - cos);
        return new Vec3d(x, y, z);
    }

    private static float clampPitch(float pitch) {
        return MathHelper.clamp(pitch, -89.9F, 89.9F);
    }

    @Override
    public void onUpdate() {
        if (this.aircraft == null || this.aircraft.isDead) {
            setDead();
        }
    }

    @Override
    public boolean isEntityInvulnerable(@NotNull DamageSource source) {
        return true;
    }

    @Override
    public @NotNull AxisAlignedBB getEntityBoundingBox() {
        return ZERO_AABB;
    }

    @Override
    public float getEyeHeight() {
        return 0.0F;
    }
}
