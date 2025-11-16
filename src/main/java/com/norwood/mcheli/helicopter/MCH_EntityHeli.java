package com.norwood.mcheli.helicopter;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ServerSettings;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_Rotor;
import com.norwood.mcheli.networking.packet.PacketStatusRequest;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MCH_EntityHeli extends MCH_EntityAircraft {

    private static final DataParameter<Byte> FOLD_STAT = EntityDataManager.createKey(MCH_EntityHeli.class,
            DataSerializers.BYTE);
    public double prevRotationRotor = 0.0;
    public double rotationRotor = 0.0;
    public MCH_Rotor[] rotors;
    public byte lastFoldBladeStat;
    public int foldBladesCooldown;
    public float prevRollFactor = 0.0F;
    private MCH_HeliInfo heliInfo;

    public MCH_EntityHeli(World world) {
        super(world);
        this.heliInfo = null;
        this.currentSpeed = 0.07;
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 0.7F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.weapons = this.createWeapon(0);
        this.rotors = new MCH_Rotor[0];
        this.lastFoldBladeStat = -1;
        if (this.world.isRemote) {
            this.foldBladesCooldown = 40;
        }
    }

    @Override
    public String getKindName() {
        return "helicopters";
    }

    @Override
    public String getEntityType() {
        return "Plane";
    }

    public MCH_HeliInfo getHeliInfo() {
        return this.heliInfo;
    }

    @Override
    public void changeType(String type) {
        MCH_Lib.DbgLog(this.world, "MCH_EntityHeli.changeType " + type + " : " + this);
        if (!type.isEmpty()) {
            this.heliInfo = MCH_HeliInfoManager.get(type);
        }

        if (this.heliInfo == null) {
            MCH_Lib.Log(this, "##### MCH_EntityHeli changeHeliType() Heli info null %d, %s, %s",
                    W_Entity.getEntityId(this), type, this.getEntityName());
            this.setDead(true);
        } else {
            this.setAcInfo(this.heliInfo);
            this.newSeats(this.getAcInfo().getNumSeatAndRack());
            this.createRotors();
            this.weapons = this.createWeapon(1 + this.getSeatNum());
            this.initPartRotation(this.getRotYaw(), this.getRotPitch());
        }
    }

    @Nullable
    @Override
    public Item getItem() {
        return this.getHeliInfo() != null ? this.getHeliInfo().item : null;
    }

    @Override
    public boolean canMountWithNearEmptyMinecart() {
        return MCH_Config.MountMinecartHeli.prmBool;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FOLD_STAT, (byte) 2);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        super.writeEntityToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setDouble("RotorSpeed", this.getCurrentThrottle());
        par1NBTTagCompound.setDouble("rotetionRotor", this.rotationRotor);
        par1NBTTagCompound.setBoolean("FoldBlade", this.getFoldBladeStat() == 0);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        super.readEntityFromNBT(par1NBTTagCompound);
        boolean beforeFoldBlade = this.getFoldBladeStat() == 0;
        if (this.getCommonUniqueId().isEmpty()) {
            this.setCommonUniqueId(par1NBTTagCompound.getString("HeliUniqueId"));
            MCH_Lib.Log(
                    this,
                    "# MCH_EntityHeli readEntityFromNBT() " + W_Entity.getEntityId(this) + ", " + this.getEntityName() +
                            ", AircraftUniqueId=null, HeliUniqueId=" + this.getCommonUniqueId());
        }

        if (this.getTypeName().isEmpty()) {
            this.setTypeName(par1NBTTagCompound.getString("HeliType"));
            MCH_Lib.Log(
                    this,
                    "# MCH_EntityHeli readEntityFromNBT() " + W_Entity.getEntityId(this) + ", " + this.getEntityName() +
                            ", TypeName=null, HeliType=" + this.getTypeName());
        }

        this.setCurrentThrottle(par1NBTTagCompound.getDouble("RotorSpeed"));
        this.rotationRotor = par1NBTTagCompound.getDouble("rotetionRotor");
        this.setFoldBladeStat((byte) (par1NBTTagCompound.getBoolean("FoldBlade") ? 0 : 2));
        if (this.heliInfo == null) {
            this.heliInfo = MCH_HeliInfoManager.get(this.getTypeName());
            if (this.heliInfo == null) {
                MCH_Lib.Log(this, "##### MCH_EntityHeli readEntityFromNBT() Heli info null %d, %s",
                        W_Entity.getEntityId(this), this.getEntityName());
                this.setDead(true);
            } else {
                this.setAcInfo(this.heliInfo);
            }
        }

        if (!beforeFoldBlade && this.getFoldBladeStat() == 0) {
            this.forceFoldBlade();
        }

        this.prevRotationRotor = this.rotationRotor;
    }

    @Override
    public float getSoundVolume() {
        return this.getAcInfo() != null && this.getAcInfo().throttleUpDown <= 0.0F ? 0.0F :
                (float) this.getCurrentThrottle() * 2.0F;
    }

    @Override
    public float getSoundPitch() {
        return (float) (0.2 + this.getCurrentThrottle() * 0.2);
    }

    @Override
    public String getDefaultSoundName() {
        return "heli";
    }

    @Override
    public float getUnfoldLandingGearThrottle() {
        double x = this.posX - this.prevPosX;
        double y = this.posY - this.prevPosY;
        double z = this.posZ - this.prevPosZ;
        float s = this.getAcInfo().speed / 3.5F;
        return x * x + y * y + z * z <= s ? 0.8F : 0.3F;
    }

    protected void createRotors() {
        if (this.heliInfo != null) {
            this.rotors = new MCH_Rotor[this.heliInfo.rotorList.size()];
            int i = 0;

            for (MCH_HeliInfo.Rotor r : this.heliInfo.rotorList) {
                this.rotors[i] = new MCH_Rotor(
                        r.bladeNum,
                        r.bladeRot,
                        2,
                        (float) r.pos.x,
                        (float) r.pos.y,
                        (float) r.pos.z,
                        (float) r.rot.x,
                        (float) r.rot.y,
                        (float) r.rot.z,
                        r.haveFoldFunc);
                i++;
            }
        }
    }

    protected void forceFoldBlade() {
        if (this.heliInfo != null && this.rotors.length > 0 && this.heliInfo.isEnableFoldBlade) {
            for (MCH_Rotor r : this.rotors) {
                r.update((float) this.rotationRotor);
                this.foldBlades();
                r.forceFold();
            }
        }
    }

    public boolean isFoldBlades() {
        return this.heliInfo != null && this.rotors.length > 0 && this.getFoldBladeStat() == 0;
    }

    protected boolean canSwitchFoldBlades() {
        return this.heliInfo != null && this.rotors.length > 0 && this.heliInfo.isEnableFoldBlade &&
                this.getCurrentThrottle() <= 0.01 && this.foldBladesCooldown == 0 &&
                (this.getFoldBladeStat() == 2 || this.getFoldBladeStat() == 0);
    }

    protected boolean canUseBlades() {
        if (this.heliInfo == null) {
            return false;
        } else if (this.rotors.length == 0) {
            return true;
        } else if (this.getFoldBladeStat() == 2) {
            for (MCH_Rotor r : this.rotors) {
                if (r.isFoldingOrUnfolding()) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    protected void foldBlades() {
        if (this.heliInfo != null && this.rotors.length > 0) {
            this.setCurrentThrottle(0.0);

            for (MCH_Rotor r : this.rotors) {
                r.startFold();
            }
        }
    }

    public void unfoldBlades() {
        if (this.heliInfo != null) {
            for (MCH_Rotor r : this.rotors) {
                r.startUnfold();
            }
        }
    }

    @Override
    public void onRideEntity(Entity ridingEntity) {
        if (ridingEntity instanceof MCH_EntitySeat) {
            if (this.heliInfo == null || this.rotors.length == 0) {
                return;
            }

            if (this.heliInfo.isEnableFoldBlade) {
                this.forceFoldBlade();
                this.setFoldBladeStat((byte) 0);
            }
        }
    }

    protected byte getFoldBladeStat() {
        return this.dataManager.get(FOLD_STAT);
    }

    // huh
    public void setFoldBladeStat(byte b) {
        if (!this.world.isRemote && b >= 0 && b <= 3) {
            this.dataManager.set(FOLD_STAT, b);
        }
    }

    @Override
    public boolean canSwitchGunnerMode() {
        if (super.canSwitchGunnerMode() && this.canUseBlades()) {
            float roll = MathHelper.abs(MathHelper.wrapDegrees(this.getRotRoll()));
            float pitch = MathHelper.abs(MathHelper.wrapDegrees(this.getRotPitch()));
            return roll < 40.0F && pitch < 40.0F;
        }

        return false;
    }

    @Override
    public boolean canSwitchHoveringMode() {
        if (super.canSwitchHoveringMode() && this.canUseBlades()) {
            float roll = MathHelper.abs(MathHelper.wrapDegrees(this.getRotRoll()));
            float pitch = MathHelper.abs(MathHelper.wrapDegrees(this.getRotPitch()));
            return roll < 40.0F && pitch < 40.0F;
        }

        return false;
    }

    @Override
    public void onUpdateAircraft() {
        if (this.heliInfo == null) {
            this.changeType(this.getTypeName());
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
        } else {
            if (!this.isRequestedSyncStatus) {
                this.isRequestedSyncStatus = true;
                if (this.world.isRemote) {
                    int stat = this.getFoldBladeStat();
                    if (stat == 1 || stat == 0) {
                        this.forceFoldBlade();
                    }

                    PacketStatusRequest.requestStatus(this);
                }
            }

            if (this.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
                this.initCurrentWeapon(this.getRiddenByEntity());
            }

            this.updateWeapons();
            this.onUpdate_Seats();
            this.onUpdate_Control();
            this.onUpdate_Rotor();
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (!this.isDestroyed() && this.isHovering() && MathHelper.abs(this.getRotPitch()) < 70.0F) {
                this.setRotPitch(this.getRotPitch() * 0.95F);
            }

            if (this.isDestroyed() && this.getCurrentThrottle() > 0.0) {
                if (MCH_Lib.getBlockIdY(this, 3, -2) > 0) {
                    this.setCurrentThrottle(this.getCurrentThrottle() * 0.8);
                }

                if (this.isExploded()) {
                    this.setCurrentThrottle(this.getCurrentThrottle() * 0.98);
                }
            }

            this.updateCameraViewers();
            if (this.world.isRemote) {
                this.onUpdate_Client();
            } else {
                this.onUpdate_Server();
            }
        }
    }

    @Override
    public boolean canMouseRot() {
        return super.canMouseRot();
    }

    @Override
    public boolean canUpdatePitch(Entity player) {
        return super.canUpdatePitch(player) && !this.isHovering();
    }

    @Override
    public boolean canUpdateRoll(Entity player) {
        return super.canUpdateRoll(player) && !this.isHovering();
    }

    @Override
    public boolean isOverridePlayerPitch() {
        return super.isOverridePlayerPitch() && !this.isHovering();
    }

    @Override
    public float getRollFactor() {
        float roll = super.getRollFactor();
        double d = this.getDistanceSq(this.prevPosX, this.posY, this.prevPosZ);
        double s = this.getAcInfo().speed;
        if (s > 0.1) {
            double var10000 = d / s;
        } else {
            double var7 = 0.0;
        }

        float f = this.prevRollFactor;
        this.prevRollFactor = roll;
        return (roll + f) / 2.0F;
    }

    @Override
    public float getControlRotYaw(float mouseX, float mouseY, float tick) {
        return mouseX;
    }

    @Override
    public float getControlRotPitch(float mouseX, float mouseY, float tick) {
        return mouseY;
    }

    @Override
    public float getControlRotRoll(float mouseX, float mouseY, float tick) {
        return mouseX;
    }

    @Override
    public void onUpdateAngles(float partialTicks) {
        if (!this.isDestroyed()) {
            float rotRoll = !this.isHovering() ? 0.04F : 0.07F;
            rotRoll = 1.0F - rotRoll * partialTicks;
            if (MCH_ServerSettings.enableRotationLimit) {
                if (this.getRotPitch() > MCH_ServerSettings.pitchLimitMax) {
                    this.setRotPitch(this.getRotPitch() -
                            Math.abs((this.getRotPitch() - MCH_ServerSettings.pitchLimitMax) * 0.1F * partialTicks));
                }

                if (this.getRotPitch() < MCH_ServerSettings.pitchLimitMin) {
                    this.setRotPitch(this.getRotPitch() +
                            Math.abs((this.getRotPitch() - MCH_ServerSettings.pitchLimitMin) * 0.2F * partialTicks));
                }

                if (this.getRotRoll() > MCH_ServerSettings.rollLimit) {
                    this.setRotRoll(this.getRotRoll() -
                            Math.abs((this.getRotRoll() - MCH_ServerSettings.rollLimit) * 0.03F * partialTicks));
                }

                if (this.getRotRoll() < -MCH_ServerSettings.rollLimit) {
                    this.setRotRoll(this.getRotRoll() +
                            Math.abs((this.getRotRoll() + MCH_ServerSettings.rollLimit) * 0.03F * partialTicks));
                }
            }

            if (this.getRotRoll() > 0.1 && this.getRotRoll() < 65.0F) {
                this.setRotRoll(this.getRotRoll() * rotRoll);
            }

            if (this.getRotRoll() < -0.1 && this.getRotRoll() > -65.0F) {
                this.setRotRoll(this.getRotRoll() * rotRoll);
            }

            if (MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
                if (this.moveLeft && !this.moveRight) {
                    this.setRotRoll(this.getRotRoll() - 1.2F * partialTicks);
                }

                if (this.moveRight && !this.moveLeft) {
                    this.setRotRoll(this.getRotRoll() + 1.2F * partialTicks);
                }
            } else {
                if (MathHelper.abs(this.getRotPitch()) < 40.0F) {
                    this.applyOnGroundPitch(0.97F);
                }

                if (this.heliInfo.isEnableFoldBlade && this.rotors.length > 0 && this.getFoldBladeStat() == 0 &&
                        !this.isDestroyed()) {
                    if (this.moveLeft && !this.moveRight) {
                        this.setRotYaw(this.getRotYaw() - 0.5F * partialTicks);
                    }

                    if (this.moveRight && !this.moveLeft) {
                        this.setRotYaw(this.getRotYaw() + 0.5F * partialTicks);
                    }
                }
            }
        }
    }

    protected void onUpdate_Rotor() {
        byte stat = this.getFoldBladeStat();
        boolean isEndSwitch = true;
        if (stat != this.lastFoldBladeStat) {
            if (stat == 1) {
                this.foldBlades();
            } else if (stat == 3) {
                this.unfoldBlades();
            }

            if (this.world.isRemote) {
                this.foldBladesCooldown = 40;
            }

            this.lastFoldBladeStat = stat;
        } else if (this.foldBladesCooldown > 0) {
            this.foldBladesCooldown--;
        }

        for (MCH_Rotor r : this.rotors) {
            r.update((float) this.rotationRotor);
            if (r.isFoldingOrUnfolding()) {
                isEndSwitch = false;
            }
        }

        if (isEndSwitch) {
            if (stat == 1) {
                this.setFoldBladeStat((byte) 0);
            } else if (stat == 3) {
                this.setFoldBladeStat((byte) 2);
            }
        }
    }

    protected void onUpdate_Control() {
        if (this.isHoveringMode() && !this.canUseFuel(true)) {
            this.switchHoveringMode(false);
        }

        if (this.isGunnerMode && !this.canUseFuel()) {
            this.switchGunnerMode(false);
        }

        if (!this.isDestroyed() && (this.getRiddenByEntity() != null || this.isHoveringMode()) && this.canUseBlades() &&
                this.isCanopyClose() && this.canUseFuel(true)) {
            if (!this.isHovering()) {
                this.onUpdate_ControlNotHovering();
            } else {
                this.onUpdate_ControlHovering();
            }
        } else {
            if (this.getCurrentThrottle() > 0.0) {
                this.addCurrentThrottle(-0.00125);
            } else {
                this.setCurrentThrottle(0.0);
            }

            if (this.heliInfo.isEnableFoldBlade && this.rotors.length > 0 && this.getFoldBladeStat() == 0 &&
                    this.onGround && !this.isDestroyed()) {
                this.onUpdate_ControlFoldBladeAndOnGround();
            }
        }

        if (this.world.isRemote) {
            if (!W_Lib.isClientPlayer(this.getRiddenByEntity())) {
                double ct = this.getThrottle();
                if (this.getCurrentThrottle() >= ct - 0.02) {
                    this.addCurrentThrottle(-0.01);
                } else if (this.getCurrentThrottle() < ct) {
                    this.addCurrentThrottle(0.01);
                }
            }
        } else {
            this.setThrottle(this.getCurrentThrottle());
        }

        if (this.getCurrentThrottle() < 0.0) {
            this.setCurrentThrottle(0.0);
        }

        this.prevRotationRotor = this.rotationRotor;
        this.rotationRotor = this.rotationRotor +
                (1.0 - Math.pow(1.0 - this.getCurrentThrottle(), 5.0)) * this.getAcInfo().rotorSpeed;
        this.rotationRotor %= 360.0;
    }

    protected void onUpdate_ControlNotHovering() {
        float throttleUpDown = this.getAcInfo().throttleUpDown;
        if (this.throttleUp) {
            if (this.getCurrentThrottle() < 1.0) {
                this.addCurrentThrottle(0.02 * throttleUpDown);
            } else {
                this.setCurrentThrottle(1.0);
            }
        } else if (this.throttleDown) {
            if (this.getCurrentThrottle() > 0.0) {
                this.addCurrentThrottle(-0.014285714285714285 * throttleUpDown);
            } else {
                this.setCurrentThrottle(0.0);
            }
        } else if ((!this.world.isRemote || W_Lib.isClientPlayer(this.getRiddenByEntity())) &&
                this.cs_heliAutoThrottleDown) {
                    if (this.getCurrentThrottle() > 0.52) {
                        this.addCurrentThrottle(-0.01 * throttleUpDown);
                    } else if (this.getCurrentThrottle() < 0.48) {
                        this.addCurrentThrottle(0.01 * throttleUpDown);
                    }
                }

        if (!this.world.isRemote) {
            boolean move = false;
            float yaw;
            double x = 0.0;
            double z = 0.0;
            if (this.moveLeft && !this.moveRight) {
                yaw = this.getRotYaw() - 90.0F;
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (this.moveRight && !this.moveLeft) {
                yaw = this.getRotYaw() + 90.0F;
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (move) {
                double f = 1.0;
                double d = Math.sqrt(x * x + z * z);
                this.motionX = this.motionX - x / d * 0.02F * f * this.getAcInfo().speed;
                this.motionZ = this.motionZ + z / d * 0.02F * f * this.getAcInfo().speed;
            }
        }
    }

    protected void onUpdate_ControlHovering() {
        if (this.getCurrentThrottle() < 1.0) {
            this.addCurrentThrottle(0.03333333333333333);
        } else {
            this.setCurrentThrottle(1.0);
        }

        if (!this.world.isRemote) {
            boolean move = false;
            float yaw;
            double x = 0.0;
            double z = 0.0;
            if (this.throttleUp) {
                yaw = this.getRotYaw();
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (this.throttleDown) {
                yaw = this.getRotYaw() - 180.0F;
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (this.moveLeft && !this.moveRight) {
                yaw = this.getRotYaw() - 90.0F;
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (this.moveRight && !this.moveLeft) {
                yaw = this.getRotYaw() + 90.0F;
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (move) {
                double d = Math.sqrt(x * x + z * z);
                this.motionX = this.motionX - x / d * 0.01F * this.getAcInfo().speed;
                this.motionZ = this.motionZ + z / d * 0.01F * this.getAcInfo().speed;
            }
        }
    }

    protected void onUpdate_ControlFoldBladeAndOnGround() {
        if (!this.world.isRemote) {
            boolean move = false;
            float yaw;
            double x = 0.0;
            double z = 0.0;
            if (this.throttleUp) {
                yaw = this.getRotYaw();
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (this.throttleDown) {
                yaw = this.getRotYaw() - 180.0F;
                x += Math.sin(yaw * Math.PI / 180.0);
                z += Math.cos(yaw * Math.PI / 180.0);
                move = true;
            }

            if (move) {
                double d = Math.sqrt(x * x + z * z);
                this.motionX -= x / d * 0.03F;
                this.motionZ += z / d * 0.03F;
            }
        }
    }

    protected void onUpdate_Particle2() {
        if (this.world.isRemote) {
            if (!(this.getHP() > this.getMaxHP() * 0.5)) {
                if (this.getHeliInfo() != null) {
                    int rotorNum = this.getHeliInfo().rotorList.size();
                    if (rotorNum > 0) {
                        if (this.isFirstDamageSmoke) {
                            this.prevDamageSmokePos = new Vec3d[rotorNum];
                        }

                        for (int ri = 0; ri < rotorNum; ri++) {
                            Vec3d rotor_pos = this.getHeliInfo().rotorList.get(ri).pos;
                            float yaw = this.getRotYaw();
                            float pitch = this.getRotPitch();
                            Vec3d pos = MCH_Lib.RotVec3(rotor_pos, -yaw, -pitch, -this.getRotRoll());
                            double x = this.posX + pos.x;
                            double y = this.posY + pos.y;
                            double z = this.posZ + pos.z;
                            if (this.isFirstDamageSmoke) {
                                this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
                            }

                            Vec3d prev = this.prevDamageSmokePos[ri];
                            double dx = x - prev.x;
                            double dy = y - prev.y;
                            double dz = z - prev.z;
                            int num = (int) (MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 2.0F) + 1;

                            for (double i = 0.0; i < num; i++) {
                                double p = (double) (double) this.getHP() / this.getMaxHP();
                                if (p < this.rand.nextFloat() / 2.0F) {
                                    float c = 0.2F + this.rand.nextFloat() * 0.3F;
                                    MCH_ParticleParam prm = new MCH_ParticleParam(
                                            this.world, "smoke", prev.x + (x - prev.x) * (i / num),
                                            prev.y + (y - prev.y) * (i / num), prev.z + (z - prev.z) * (i / num));
                                    prm.motionX = (this.rand.nextDouble() - 0.5) * 0.3;
                                    prm.motionY = this.rand.nextDouble() * 0.1;
                                    prm.motionZ = (this.rand.nextDouble() - 0.5) * 0.3;
                                    prm.size = (this.rand.nextInt(5) + 5.0F);
                                    prm.setColor(0.7F + this.rand.nextFloat() * 0.1F, c, c, c);
                                    MCH_ParticlesUtil.spawnParticle(prm);
                                    int ebi = this.rand.nextInt(1 + this.extraBoundingBox.length);
                                    if (p < 0.3 && ebi > 0) {
                                        AxisAlignedBB bb = this.extraBoundingBox[ebi - 1].getBoundingBox();
                                        double bx = (bb.maxX + bb.minX) / 2.0;
                                        double by = (bb.maxY + bb.minY) / 2.0;
                                        double bz = (bb.maxZ + bb.minZ) / 2.0;
                                        prm.posX = bx;
                                        prm.posY = by;
                                        prm.posZ = bz;
                                        MCH_ParticlesUtil.spawnParticle(prm);
                                    }
                                }
                            }

                            this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
                        }

                        this.isFirstDamageSmoke = false;
                    }
                }
            }
        }
    }

    protected void onUpdate_Client() {
        if (this.getRiddenByEntity() != null && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch;
        }

        if (this.aircraftPosRotInc > 0) {
            this.applyServerPositionAndRotation();
        } else {
            this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (!this.isDestroyed() && (this.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0)) {
                this.motionX *= 0.95;
                this.motionZ *= 0.95;
                this.applyOnGroundPitch(0.95F);
            }

            if (this.isInWater()) {
                this.motionX *= 0.99;
                this.motionZ *= 0.99;
            }
        }

        if (this.isDestroyed()) {
            if (this.rotDestroyedYaw < 15.0F) {
                this.rotDestroyedYaw += 0.3F;
            }

            this.setRotYaw(this.getRotYaw() + this.rotDestroyedYaw * (float) this.getCurrentThrottle());
            if (MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
                if (MathHelper.abs(this.getRotPitch()) < 10.0F) {
                    this.setRotPitch(this.getRotPitch() + this.rotDestroyedPitch);
                }

                this.setRotRoll(this.getRotRoll() + this.rotDestroyedRoll);
            }
        }

        if (this.getRiddenByEntity() != null) {}

        this.onUpdate_ParticleSandCloud(false);
        this.onUpdate_Particle2();
        this.updateCamera(this.posX, this.posY, this.posZ);
    }

    private void onUpdate_Server() {
        double prevMotion = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        float ogp = this.getAcInfo().onGroundPitch;
        if (!this.isHovering()) {
            double dp = 0.0;
            if (this.canFloatWater()) {
                dp = this.getWaterDepth();
            }

            if (dp == 0.0) {
                this.motionY = this.motionY +
                        (!this.isInWater() ? this.getAcInfo().gravity : this.getAcInfo().gravityInWater);
                float yaw = this.getRotYaw() / 180.0F * (float) Math.PI;
                float pitch = this.getRotPitch();
                if (MCH_Lib.getBlockIdY(this, 3, -3) > 0) {
                    pitch -= ogp;
                }

                this.motionX = this.motionX + 0.1 * MathHelper.sin(yaw) * this.currentSpeed *
                        -(pitch * pitch * pitch / 20000.0F) * this.getCurrentThrottle();
                this.motionZ = this.motionZ + 0.1 * MathHelper.cos(yaw) * this.currentSpeed *
                        (pitch * pitch * pitch / 20000.0F) * this.getCurrentThrottle();
                double y = 0.0;
                if (MathHelper.abs(this.getRotPitch()) + MathHelper.abs(this.getRotRoll() * 0.9F) <= 40.0F) {
                    y = 1.0 - y / 40.0;
                }

                double throttle = this.getCurrentThrottle();
                if (this.isDestroyed()) {
                    throttle *= -0.65;
                }

                this.motionY += (y * 0.025 + 0.03) * throttle;
            } else {
                if (MathHelper.abs(this.getRotPitch()) < 40.0F) {
                    float pitchx = this.getRotPitch();
                    pitchx -= ogp;
                    pitchx *= 0.9F;
                    pitchx += ogp;
                    this.setRotPitch(pitchx);
                }

                if (MathHelper.abs(this.getRotRoll()) < 40.0F) {
                    this.setRotRoll(this.getRotRoll() * 0.9F);
                }

                if (dp < 1.0) {
                    this.motionY -= 1.0E-4;
                    this.motionY = this.motionY + 0.007 * this.getCurrentThrottle();
                } else {
                    if (this.motionY < 0.0) {
                        this.motionY *= 0.7;
                    }

                    this.motionY += 0.007;
                }
            }
        } else {
            if (this.rand.nextInt(50) == 0) {
                this.motionX = this.motionX + (this.rand.nextDouble() - 0.5) / 30.0;
            }

            if (this.rand.nextInt(50) == 0) {
                this.motionY = this.motionY + (this.rand.nextDouble() - 0.5) / 50.0;
            }

            if (this.rand.nextInt(50) == 0) {
                this.motionZ = this.motionZ + (this.rand.nextDouble() - 0.5) / 30.0;
            }
        }

        double motion = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        float speedLimit = this.getAcInfo().speed;
        if (motion > speedLimit) {
            this.motionX *= speedLimit / motion;
            this.motionZ *= speedLimit / motion;
            motion = speedLimit;
        }

        if (this.isDestroyed()) {
            this.motionX *= 0.0;
            this.motionZ *= 0.0;
            this.currentSpeed = 0.0;
        }

        if (motion > prevMotion && this.currentSpeed < speedLimit) {
            this.currentSpeed = this.currentSpeed + (speedLimit - this.currentSpeed) / 35.0;
            if (this.currentSpeed > speedLimit) {
                this.currentSpeed = speedLimit;
            }
        } else {
            this.currentSpeed = this.currentSpeed - (this.currentSpeed - 0.07) / 35.0;
            if (this.currentSpeed < 0.07) {
                this.currentSpeed = 0.07;
            }
        }

        if (this.onGround) {
            this.motionX *= 0.5;
            this.motionZ *= 0.5;
            if (MathHelper.abs(this.getRotPitch()) < 40.0F) {
                float pitchx = this.getRotPitch();
                pitchx -= ogp;
                pitchx *= 0.9F;
                pitchx += ogp;
                this.setRotPitch(pitchx);
            }

            if (MathHelper.abs(this.getRotRoll()) < 40.0F) {
                this.setRotRoll(this.getRotRoll() * 0.9F);
            }
        }

        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.motionY *= 0.95;
        this.motionX *= 0.99;
        this.motionZ *= 0.99;
        this.setRotation(this.getRotYaw(), this.getRotPitch());
        this.onUpdate_updateBlock();
        if (this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
            this.unmountEntity();
        }
    }
}
