package com.norwood.mcheli.vehicle;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.networking.packet.PacketStatusRequest;
import com.norwood.mcheli.weapon.MCH_WeaponParam;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Objects;

public class MCH_EntityVehicle extends MCH_EntityAircraft {

    public boolean isUsedPlayer;
    public double fixPosX;
    public double fixPosY;
    public double fixPosZ;
    private MCH_VehicleInfo vehicleInfo = null;

    private float steerAngle = 0.0F;
    // Tuning constants -- edit + rebuild to taste. TODO: promote to YAML vehicle config.
    private static final float STEER_ATTACK  = 0.14F;  // per-tick ramp toward a held steer key
    private static final float STEER_RELEASE = 0.20F;  // per-tick return to center when released
    private static final float STEER_COUNTER = 0.30F;  // per-tick when flicking to the opposite lock
    private static final float STEER_MAX_DEG = 34.0F;  // front-wheel angle at full lock
    private static final float WHEELBASE     = 3.0F;   // effective front->rear axle distance (blocks)

    public MCH_EntityVehicle(World world) {
        super(world);
        this.currentSpeed = 0.07;
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 0.7F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.isUsedPlayer = false;
        this.weapons = this.createWeapon(0);
    }

    @Override
    public String getKindName() {
        return "vehicles";
    }

    @Override
    public String getEntityType() {
        return "Vehicle";
    }

    @Nullable
    public MCH_VehicleInfo getVehicleInfo() {
        return this.vehicleInfo;
    }

    @Override
    public void changeType(String type) {
        MCH_Logger.debugLog(this.world, "MCH_EntityVehicle.changeType " + type + " : " + this);
        if (!type.isEmpty()) {
            this.vehicleInfo = MCH_VehicleInfoManager.get(type);
        }

        if (this.vehicleInfo == null) {
            MCH_Logger.log(this, "##### MCH_EntityVehicle changeVehicleType() Vehicle info null %d, %s, %s", W_Entity.getEntityId(this), type, this.getEntityName());
            this.setDead();
        } else {
            this.setAcInfo(this.vehicleInfo);
            this.newSeats(Objects.requireNonNull(this.getAcInfo()).getNumSeatAndRack());
            this.weapons = this.createWeapon(1 + this.getSeatNum());
            this.initPartRotation(this.rotationYaw, this.rotationPitch);
        }
    }

    @Override
    public boolean canMountWithNearEmptyMinecart() {
        return MCH_Config.MountMinecartVehicle.prmBool;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        super.writeEntityToNBT(par1NBTTagCompound);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        super.readEntityFromNBT(par1NBTTagCompound);
        if (this.vehicleInfo == null) {
            this.vehicleInfo = MCH_VehicleInfoManager.get(this.getTypeName());
            if (this.vehicleInfo == null) {
                MCH_Logger.log(this, "##### MCH_EntityVehicle readEntityFromNBT() Vehicle info null %d, %s", W_Entity.getEntityId(this), this.getEntityName());
                this.setDead();
            } else {
                this.setAcInfo(this.vehicleInfo);
            }
        }
    }

    @Override
    public Item getItem() {
        return this.getVehicleInfo() != null ? this.getVehicleInfo().item : null;
    }

    @Override
    public void setDead() {
        super.setDead();
    }

    private static Vec3d getFlatForwardVector(float yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        return new Vec3d(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
    }

    private double getSignedForwardSpeed() {
        Vec3d forward = getFlatForwardVector(this.rotationYaw);
        return this.motionX * forward.x + this.motionZ * forward.z;
    }

    /** Move {@code current} toward {@code target} by at most {@code maxStep} (linear, no overshoot). */
    private static float approachValue(float current, float target, float maxStep) {
        float delta = target - current;
        if (delta > maxStep) {
            return current + maxStep;
        }
        if (delta < -maxStep) {
            return current - maxStep;
        }
        return target;
    }

    private void applyGroundGrip(double lateralGrip, double rollingFriction) {
        Vec3d forward = getFlatForwardVector(this.rotationYaw);
        double forwardSpeed = this.motionX * forward.x + this.motionZ * forward.z;
        double lateralSpeed = this.motionX * -forward.z + this.motionZ * forward.x;
        lateralSpeed *= lateralGrip;
        forwardSpeed *= rollingFriction;
        this.motionX = forward.x * forwardSpeed - forward.z * lateralSpeed;
        this.motionZ = forward.z * forwardSpeed + forward.x * lateralSpeed;
    }

    @Override
    public float getSoundVolume() {
        return (float) this.getCurrentThrottle() * 2.0F;
    }

    @Override
    public float getSoundPitch() {
        return (float) (this.getCurrentThrottle() * 0.5);
    }

    @Override
    public String getDefaultSoundName() {
        return "";
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void zoomCamera() {
        if (this.canZoom()) {
            float z = this.camera.getCameraZoom();
            z++;
            this.camera.setCameraZoom(z <= this.getZoomMax() + 0.01 ? z : 1.0F);
        }
    }

    @Override
    public boolean isCameraView(Entity entity) {
        return true;
    }

    @Override
    public boolean useCurrentWeapon(MCH_WeaponParam prm) {
        float beforeUseWeaponPitch = this.rotationPitch;
        float beforeUseWeaponYaw = this.rotationYaw;
        this.rotationPitch = this.getWeaponUserPitch(prm.user);
        this.rotationYaw = this.getWeaponUserYaw(prm.user);
        boolean result = super.useCurrentWeapon(prm);
        this.rotationPitch = beforeUseWeaponPitch;
        this.rotationYaw = beforeUseWeaponYaw;
        return result;
    }

    @Override
    public float getCurrentWeaponShotYaw(Entity user) {
        MCH_WeaponSet currentWs = user != null ? this.getCurrentWeapon(user) : null;
        if (currentWs != null) {
            return MathHelper.wrapDegrees(this.getWeaponUserYaw(user) + currentWs.getCurrentWeapon().fixRotationYaw);
        }
        return super.getCurrentWeaponShotYaw(user);
    }

    @Override
    public float getCurrentWeaponShotPitch(Entity user) {
        MCH_WeaponSet currentWs = user != null ? this.getCurrentWeapon(user) : null;
        if (currentWs != null) {
            return MathHelper.wrapDegrees(this.getWeaponUserPitch(user) + currentWs.getCurrentWeapon().fixRotationPitch);
        }
        return super.getCurrentWeaponShotPitch(user);
    }

    public Vec3d getCurrentWeaponShotPos(Vec3d localPos, Entity user) {
        return com.norwood.mcheli.MCH_Lib.RotVec3(
                localPos,
                -this.getWeaponUserYaw(user),
                -this.getWeaponUserPitch(user),
                -this.getRoll());
    }

    public Vec3d getCurrentWeaponShotPos(Vec3d localPos, Entity user, float partialTicks) {
        return com.norwood.mcheli.MCH_Lib.RotVec3(
                localPos,
                -this.getWeaponUserYaw(user, partialTicks),
                -this.getWeaponUserPitch(user, partialTicks),
                -(this.prevRotationRoll + (this.rotationRoll - this.prevRotationRoll) * partialTicks));
    }

    @Override
    protected void mountWithNearEmptyMinecart() {
        if (!MCH_Config.FixVehicleAtPlacedPoint.prmBool) {
            super.mountWithNearEmptyMinecart();
        }
    }

    @Override
    public void onUpdateAircraft() {
        if (this.vehicleInfo == null) {
            this.changeType(this.getTypeName());
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
        } else {
            if (this.ticksExisted >= 200 && MCH_Config.FixVehicleAtPlacedPoint.prmBool) {
                this.dismountRidingEntity();
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
                if (this.world.isRemote && this.ticksExisted % 4 == 0) {
                    this.fixPosY = this.posY;
                }

                this.setPosition((this.posX + this.fixPosX) / 2.0, (this.posY + this.fixPosY) / 2.0,
                        (this.posZ + this.fixPosZ) / 2.0);
            } else {
                this.fixPosX = this.posX;
                this.fixPosY = this.posY;
                this.fixPosZ = this.posZ;
            }

            if (!this.isRequestedSyncStatus) {
                this.isRequestedSyncStatus = true;
                if (this.world.isRemote) {
                    PacketStatusRequest.requestStatus(this);
                }
            }

            if (this.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
                this.getRiddenByEntity().rotationPitch = 0.0F;
                this.getRiddenByEntity().prevRotationPitch = 0.0F;
                this.initCurrentWeapon(this.getRiddenByEntity());
            }

            this.updateWeapons();
            this.onUpdate_Seats();
            this.onUpdate_Control();
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.isInWater()) {
                this.rotationPitch *= 0.9F;
            }

            if (this.world.isRemote) {
                this.onUpdate_Client();
            } else {
                this.onUpdate_Server();
            }
        }
    }

    protected void onUpdate_Control() {
        if (this.getRiddenByEntity() == null || this.getRiddenByEntity().isDead) {
            if (this.getCurrentThrottle() > 0.0) {
                this.addCurrentThrottle(-0.00125);
            } else {
                this.setCurrentThrottle(0.0);
            }
        } else if (Objects.requireNonNull(this.getVehicleInfo()).isEnableMove || this.getVehicleInfo().isEnableRot) {
            this.onUpdate_ControlOnGround();
        }

        if (this.getCurrentThrottle() < 0.0) {
            this.setCurrentThrottle(0.0);
        }

        if (this.world.isRemote) {
            if (!W_Lib.isClientPlayer(this.getRiddenByEntity())) {
                double ct = this.getThrottle();
                if (this.getCurrentThrottle() > ct) {
                    this.addCurrentThrottle(-0.005);
                }

                if (this.getCurrentThrottle() < ct) {
                    this.addCurrentThrottle(0.005);
                }
            }
        } else {
            this.setThrottle(this.getCurrentThrottle());
        }
    }

    protected void onUpdate_ControlOnGround() {
        if (!this.world.isRemote) {
            boolean move = false;
            float yaw;
            double x = 0.0;
            double z = 0.0;
            if (Objects.requireNonNull(this.getVehicleInfo()).isEnableMove) {
                if (this.throttleUp) {
                    yaw = this.rotationYaw;
                    x += Math.sin(yaw * Math.PI / 180.0);
                    z += Math.cos(yaw * Math.PI / 180.0);
                    move = true;
                }

                if (this.throttleDown) {
                    yaw = this.rotationYaw - 180.0F;
                    x += Math.sin(yaw * Math.PI / 180.0);
                    z += Math.cos(yaw * Math.PI / 180.0);
                    move = true;
                }
            }

            if (this.getVehicleInfo().isEnableRot) {

                float target = 0.0F;
                if (this.moveLeft && !this.moveRight) {
                    target = -1.0F;
                } else if (this.moveRight && !this.moveLeft) {
                    target = 1.0F;
                }
                float rate;
                if (target == 0.0F) {
                    rate = STEER_RELEASE;                  // released -> self-center
                } else if (this.steerAngle * target < 0.0F) {
                    rate = STEER_COUNTER;                  // flicking to the opposite lock
                } else {
                    rate = STEER_ATTACK;                   // building toward the held lock
                }
                this.steerAngle = approachValue(this.steerAngle, target, rate);


                double v = getSignedForwardSpeed();        // blocks/tick, signed
                if (Math.abs(this.steerAngle) > 1.0e-4F && Math.abs(v) > 1.0e-4D) {
                    double yawRateRad = (v / WHEELBASE)
                            * Math.tan(Math.toRadians(this.steerAngle * STEER_MAX_DEG));
                    this.rotationYaw += (float) Math.toDegrees(yawRateRad);
                }
            }

            if (move) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 0.0D) {
                    this.motionX -= x / d * 0.035F;
                    this.motionZ += z / d * 0.035F;
                }
            }
        }
    }

    protected void onUpdate_Client() {
        this.updateCameraViewers();
        if (this.getRiddenByEntity() != null && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch;
        }

        if (this.aircraftPosRotInc > 0) {
            double rpinc = this.aircraftPosRotInc;
            double yaw = MathHelper.wrapDegrees(this.aircraftYaw - this.rotationYaw);
            this.rotationYaw = (float) (this.rotationYaw + yaw / rpinc);
            this.rotationPitch = (float) (this.rotationPitch + (this.aircraftPitch - this.rotationPitch) / rpinc);
            this.setPosition(
                    this.posX + (this.aircraftX - this.posX) / rpinc,
                    this.posY + (this.aircraftY - this.posY) / rpinc,
                    this.posZ + (this.aircraftZ - this.posZ) / rpinc);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            this.aircraftPosRotInc--;
        } else {
            boolean localPilot = W_Lib.isClientPlayer(this.getRiddenByEntity());
            this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (this.onGround && !localPilot) {
                applyGroundGrip(0.12D, 0.992D);
            }

            if (this.isInWater()) {
                this.motionX *= 0.99;
                this.motionZ *= 0.99;
            }
        }

        this.getRiddenByEntity();

        this.updateCamera(this.posX, this.posY, this.posZ);
    }

    private void onUpdate_Server() {
        this.updateCameraViewers();

        // Gravity / buoyancy
        if (this.canFloatWater()) {
            double dp = this.getWaterDepth();
            if (dp == 0.0) {
                this.motionY += this.isInWater() ? this.getAcInfo() != null ? this.getAcInfo().gravityInWater : 0 : this.getAcInfo() != null ? this.getAcInfo().gravity : 0;
            } else if (dp < 1.0) {
                this.motionY -= 1.0E-4;
                this.motionY += 0.007 * this.getCurrentThrottle();
            } else {
                if (this.motionY < 0.0) {
                    this.motionY *= 0.5;
                }
                this.motionY += 0.007;
            }
        } else {
            this.motionY += Objects.requireNonNull(this.getAcInfo()).gravity;
        }

        // Horizontal acceleration
        if (this.getCurrentThrottle() > 0.0) {
            Vec3d forward = getFlatForwardVector(this.rotationYaw);
            double accel = 0.035 * this.getCurrentThrottle();
            this.motionX += forward.x * accel;
            this.motionZ += forward.z * accel;
        }

        // Speed
        double motionH = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        float speedLimit = Objects.requireNonNull(this.getAcInfo()).speed;
        if (motionH > speedLimit) {
            double scale = speedLimit / motionH;
            this.motionX *= scale;
            this.motionZ *= scale;
        }

        // Friction / grip
        if (this.onGround) {
            applyGroundGrip(0.08D, 0.985D);
        } else {
            this.motionX *= 0.99;
            this.motionZ *= 0.99;
        }
        this.motionY *= 0.95;

        // Move
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

        // Cleanup
        this.onUpdate_updateBlock();
        if (this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
            this.unmountEntity();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9,
                                             boolean teleport) {
        int delay = par9 + this.getClientPositionDelayCorrection();
        if (W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            delay = Math.max(1, delay - 1);
        }
        this.aircraftPosRotInc = delay;
        this.aircraftX = par1;
        this.aircraftY = par3;
        this.aircraftZ = par5;
        this.aircraftYaw = par7;
        this.aircraftPitch = par8;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    @Override
    public int getClientPositionDelayCorrection() {
        return 5;
    }

    @Override
    public void onUpdateAngles(float partialTicks) {}

    @Override
    public boolean canSwitchFreeLook() {
        return false;
    }
}
