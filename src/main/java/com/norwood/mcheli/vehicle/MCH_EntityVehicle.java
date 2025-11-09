package com.norwood.mcheli.vehicle;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketStatusRequest;
import com.norwood.mcheli.weapon.MCH_WeaponParam;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_WorldFunc;

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
        MCH_Lib.DbgLog(this.world, "MCH_EntityVehicle.changeType " + type + " : " + this);
        if (!type.isEmpty()) {
            this.vehicleInfo = MCH_VehicleInfoManager.get(type);
        }

        if (this.vehicleInfo == null) {
            MCH_Lib.Log(this, "##### MCH_EntityVehicle changeVehicleType() Vehicle info null %d, %s, %s",
                    W_Entity.getEntityId(this), type, this.getEntityName());
            this.setDead();
        } else {
            this.setAcInfo(this.vehicleInfo);
            this.newSeats(this.getAcInfo().getNumSeatAndRack());
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
                MCH_Lib.Log(this, "##### MCH_EntityVehicle readEntityFromNBT() Vehicle info null %d, %s",
                        W_Entity.getEntityId(this), this.getEntityName());
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
        if (prm.user != null) {
            MCH_WeaponSet currentWs = this.getCurrentWeapon(prm.user);
            if (currentWs != null) {
                MCH_AircraftInfo.Weapon w = this.getAcInfo().getWeaponByName(currentWs.getInfo().name);
                if (w != null && w.maxYaw != 0.0F && w.minYaw != 0.0F) {
                    return super.useCurrentWeapon(prm);
                }
            }
        }

        float breforeUseWeaponPitch = this.rotationPitch;
        float breforeUseWeaponYaw = this.rotationYaw;
        this.rotationPitch = prm.user.rotationPitch;
        this.rotationYaw = prm.user.rotationYaw;
        boolean result = super.useCurrentWeapon(prm);
        this.rotationPitch = breforeUseWeaponPitch;
        this.rotationYaw = breforeUseWeaponYaw;
        return result;
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
        } else if (this.getVehicleInfo().isEnableMove || this.getVehicleInfo().isEnableRot) {
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
            if (this.getVehicleInfo().isEnableMove) {
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

    protected void onUpdate_Particle() {
        double particlePosY = this.posY;
        boolean b = false;

        int y;
        for (y = 0; y < 5 && !b; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    int block = W_WorldFunc.getBlockId(this.world, (int) (this.posX + 0.5) + x,
                            (int) (this.posY + 0.5) - y, (int) (this.posZ + 0.5) + z);
                    if (block != 0 && !b) {
                        particlePosY = (int) (this.posY + 1.0) - y;
                        b = true;
                    }
                }
            }

            for (int x = -3; b && x <= 3; x++) {
                for (int zx = -3; zx <= 3; zx++) {
                    if (W_WorldFunc.isBlockWater(this.world, (int) (this.posX + 0.5) + x, (int) (this.posY + 0.5) - y,
                            (int) (this.posZ + 0.5) + zx)) {
                        for (int i = 0; i < 7.0 * this.getCurrentThrottle(); i++) {
                            this.world
                                    .spawnParticle(
                                            EnumParticleTypes.WATER_SPLASH,
                                            this.posX + 0.5 + x + (this.rand.nextDouble() - 0.5) * 2.0,
                                            particlePosY + this.rand.nextDouble(),
                                            this.posZ + 0.5 + zx + (this.rand.nextDouble() - 0.5) * 2.0,
                                            x + (this.rand.nextDouble() - 0.5) * 2.0,
                                            -0.3,
                                            zx + (this.rand.nextDouble() - 0.5) * 2.0);
                        }
                    }
                }
            }
        }

        double pn = (5 - y + 1) / 5.0;
        if (b) {
            for (int k = 0; k < (int) (this.getCurrentThrottle() * 6.0 * pn); k++) {
                this.world
                        .spawnParticle(
                                EnumParticleTypes.EXPLOSION_NORMAL,
                                this.posX + (this.rand.nextDouble() - 0.5),
                                particlePosY + (this.rand.nextDouble() - 0.5),
                                this.posZ + (this.rand.nextDouble() - 0.5),
                                (this.rand.nextDouble() - 0.5) * 2.0,
                                -0.4,
                                (this.rand.nextDouble() - 0.5) * 2.0);
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

        if (this.getRiddenByEntity() != null) {}

        this.updateCamera(this.posX, this.posY, this.posZ);
    }

    private void onUpdate_Server() {
        this.updateCameraViewers();

        // Gravity / buoyancy
        if (this.canFloatWater()) {
            double dp = this.getWaterDepth();
            if (dp == 0.0) {
                this.motionY += this.isInWater() ? this.getAcInfo().gravityInWater : this.getAcInfo().gravity;
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
            this.motionY += this.getAcInfo().gravity;
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
        float speedLimit = this.getAcInfo().speed;
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
