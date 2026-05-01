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
    public float lastRiderYaw;
    public float lastRiderPitch;
    public double fixPosX;
    public double fixPosY;
    public double fixPosZ;
    private MCH_VehicleInfo vehicleInfo = null;

    public MCH_EntityVehicle(World world) {
        super(world);
        this.currentSpeed = 0.07;
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 0.7F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.isUsedPlayer = false;
        this.lastRiderYaw = 0.0F;
        this.lastRiderPitch = 0.0F;
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

            if (this.getVehicleInfo().isEnableMove) {
                if (this.moveLeft && !this.moveRight) {
                    this.rotationYaw = (float) (this.rotationYaw - 0.5);
                }

                if (this.moveRight && !this.moveLeft) {
                    this.rotationYaw = (float) (this.rotationYaw + 0.5);
                }
            }

            if (move) {
                double d = Math.sqrt(x * x + z * z);
                this.motionX -= x / d * 0.03F;
                this.motionZ += z / d * 0.03F;
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
            this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (this.onGround) {
                this.motionX *= 0.95;
                this.motionZ *= 0.95;
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

        // Horizontal aceleration
        if (this.getCurrentThrottle() > 0.0) {
            double yawRad = Math.toRadians(this.rotationYaw);
            double accel = 0.03 * this.getCurrentThrottle(); // scale throttle → accel
            this.motionX -= Math.sin(yawRad) * accel;
            this.motionZ += Math.cos(yawRad) * accel;
        }

        // Speed
        double motionH = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        float speedLimit = Objects.requireNonNull(this.getAcInfo()).speed;
        if (motionH > speedLimit) {
            double scale = speedLimit / motionH;
            this.motionX *= scale;
            this.motionZ *= scale;
        }

        // Friction
        double groundFriction = this.onGround ? 0.91 : 0.99;
        this.motionX *= groundFriction;
        this.motionZ *= groundFriction;
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
    public void onUpdateAngles(float partialTicks) {}

    @Override
    public boolean canSwitchFreeLook() {
        return false;
    }
}
