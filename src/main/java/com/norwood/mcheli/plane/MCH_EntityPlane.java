package com.norwood.mcheli.plane;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_Parts;
import com.norwood.mcheli.networking.packet.PacketStatusRequest;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MCH_EntityPlane extends MCH_EntityAircraft {

    public float soundVolume;
    public MCH_Parts partNozzle;
    public MCH_Parts partWing;
    public float rotationRotor;
    public float prevRotationRotor;
    public float addkeyRotValue;
    private MCH_PlaneInfo planeInfo = null;
    private boolean addKeyFlag;

    public MCH_EntityPlane(World world) {
        super(world);
        this.currentSpeed = 0.07;
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 0.7F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.weapons = this.createWeapon(0);
        this.soundVolume = 0.0F;
        this.partNozzle = null;
        this.partWing = null;
        this.stepHeight = 0.6F;
        this.rotationRotor = 0.0F;
        this.prevRotationRotor = 0.0F;
    }

    @Override
    public String getKindName() {
        return "planes";
    }

    @Override
    public String getEntityType() {
        return "Plane";
    }

    public MCH_PlaneInfo getPlaneInfo() {
        return this.planeInfo;
    }

    @Override
    public void changeType(String type) {
        MCH_Lib.DbgLog(this.world, "MCP_EntityPlane.changeType " + type + " : " + this);
        if (!type.isEmpty()) {
            this.planeInfo = MCP_PlaneInfoManager.get(type);
        }

        if (this.planeInfo == null) {
            MCH_Lib.Log(this, "##### MCP_EntityPlane changePlaneType() Plane info null %d, %s, %s",
                    W_Entity.getEntityId(this), type, this.getEntityName());
            this.setDead();
        } else {
            this.setAcInfo(this.planeInfo);
            this.newSeats(this.getAcInfo().getNumSeatAndRack());
            this.partNozzle = this.createNozzle(this.planeInfo);
            this.partWing = this.createWing(this.planeInfo);
            this.weapons = this.createWeapon(1 + this.getSeatNum());
            this.initPartRotation(this.getRotYaw(), this.getRotPitch());
        }
    }

    @Nullable
    @Override
    public Item getItem() {
        return this.getPlaneInfo() != null ? this.getPlaneInfo().item : null;
    }

    @Override
    public boolean canMountWithNearEmptyMinecart() {
        return MCH_Config.MountMinecartPlane.prmBool;
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
        if (this.planeInfo == null) {
            this.planeInfo = MCP_PlaneInfoManager.get(this.getTypeName());
            if (this.planeInfo == null) {
                MCH_Lib.Log(this, "##### MCP_EntityPlane readEntityFromNBT() Plane info null %d, %s",
                        W_Entity.getEntityId(this), this.getEntityName());
                this.setDead();
            } else {
                this.setAcInfo(this.planeInfo);
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
    }

    @Override
    public int getNumEjectionSeat() {
        if (this.getAcInfo() != null && this.getAcInfo().isEnableEjectionSeat) {
            int n = this.getSeatNum() + 1;
            return n <= 2 ? n : 0;
        } else {
            return 0;
        }
    }

    @Override
    public void onInteractFirst(EntityPlayer player) {
        this.addkeyRotValue = 0.0F;
        this.addKeyFlag = false;
    }

    @Override
    public boolean canSwitchGunnerMode() {
        if (!super.canSwitchGunnerMode()) {
            return false;
        } else {
            float roll = MathHelper.abs(MathHelper.wrapDegrees(this.getRotRoll()));
            float pitch = MathHelper.abs(MathHelper.wrapDegrees(this.getRotPitch()));
            return !(roll > 40.0F) && !(pitch > 40.0F) && this.getCurrentThrottle() > 0.6F &&
                    MCH_Lib.getBlockIdY(this, 3, -5) == 0;
        }
    }

    @Override
    public void onUpdateAircraft() {
        if (this.planeInfo == null) {
            this.changeType(this.getTypeName());
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
        } else {
            if (this.addKeyFlag) {
                this.addKeyFlag = false;
            }

            if (!this.isRequestedSyncStatus) {
                this.isRequestedSyncStatus = true;
                if (this.world.isRemote) {
                    PacketStatusRequest.requestStatus(this);
                }
            }

            if (this.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
                this.initCurrentWeapon(this.getRiddenByEntity());
            }

            this.updateWeapons();
            this.onUpdate_Seats();
            this.onUpdate_Control();
            this.prevRotationRotor = this.rotationRotor;
            this.rotationRotor = (float) (this.rotationRotor + this.getCurrentThrottle() * this.getAcInfo().rotorSpeed);
            if (this.rotationRotor > 360.0F) {
                this.rotationRotor -= 360.0F;
                this.prevRotationRotor -= 360.0F;
            }

            if (this.rotationRotor < 0.0F) {
                this.rotationRotor += 360.0F;
                this.prevRotationRotor += 360.0F;
            }

            if (this.onGround && this.getVtolMode() == 0 && this.planeInfo.isDefaultVtol) {
                this.swithVtolMode(true);
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (!this.isDestroyed() && this.isHovering() && MathHelper.abs(this.getRotPitch()) < 70.0F) {
                this.setRotPitch(this.getRotPitch() * 0.95F, "isHovering()");
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
    public boolean canUpdateYaw(Entity player) {
        return super.canUpdateYaw(player) && !this.isHovering();
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
    public float getYawFactor() {
        float yaw = this.getVtolMode() > 0 ? this.getPlaneInfo().vtolYaw : super.getYawFactor();
        return yaw * 0.8F;
    }

    @Override
    public float getPitchFactor() {
        float pitch = this.getVtolMode() > 0 ? this.getPlaneInfo().vtolPitch : super.getPitchFactor();
        return pitch * 0.8F;
    }

    @Override
    public float getRollFactor() {
        float roll = this.getVtolMode() > 0 ? this.getPlaneInfo().vtolYaw : super.getRollFactor();
        return roll * 0.8F;
    }

    @Override
    public boolean isOverridePlayerPitch() {
        return super.isOverridePlayerPitch() && !this.isHovering();
    }

    @Override
    public boolean isOverridePlayerYaw() {
        return super.isOverridePlayerYaw() && !this.isHovering();
    }

    @Override
    public float getControlRotYaw(float mouseX, float mouseY, float tick) {
        if (MCH_Config.MouseControlFlightSimMode.prmBool) {
            this.rotationByKey(tick);
            return this.addkeyRotValue * 20.0F;
        } else {
            return mouseX;
        }
    }

    @Override
    public float getControlRotPitch(float mouseX, float mouseY, float tick) {
        return mouseY;
    }

    @Override
    public float getControlRotRoll(float mouseX, float mouseY, float tick) {
        if (MCH_Config.MouseControlFlightSimMode.prmBool) {
            return mouseX * 2.0F;
        } else {
            return this.getVtolMode() == 0 ? mouseX * 0.5F : mouseX;
        }
    }

    private void rotationByKey(float partialTicks) {
        float rot = 0.2F;
        if (!MCH_Config.MouseControlFlightSimMode.prmBool && this.getVtolMode() != 0) {
            rot *= 0.0F;
        }

        if (!this.addKeyFlag) {
            this.addKeyFlag = true;
            if (this.moveLeft && !this.moveRight) {
                this.addkeyRotValue -= rot * partialTicks;
            }

            if (this.moveRight && !this.moveLeft) {
                this.addkeyRotValue += rot * partialTicks;
            }
        }
    }

    @Override
    public void onUpdateAngles(float partialTicks) {
        if (!this.isDestroyed()) {
            if (this.isGunnerMode) {
                this.setRotPitch(this.getRotPitch() * 0.95F);
                this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 0.2F);
                if (MathHelper.abs(this.getRotRoll()) > 20.0F) {
                    this.setRotRoll(this.getRotRoll() * 0.95F);
                }
            }

            boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;
            if (!isFly || this.isFreeLookMode() || this.isGunnerMode ||
                    this.getAcInfo().isFloat && this.getWaterDepth() > 0.0) {
                float gmy = 1.0F;
                if (!isFly) {
                    gmy = this.getAcInfo().mobilityYawOnGround;
                    if (!this.getAcInfo().canRotOnGround) {
                        Block block = MCH_Lib.getBlockY(this, 3, -2, false);
                        if (!W_Block.isEqual(block, W_Block.getWater()) && !W_Block.isEqual(block, Blocks.AIR)) {
                            gmy = 0.0F;
                        }
                    }
                }

                if (this.moveLeft && !this.moveRight) {
                    this.setRotYaw(this.getRotYaw() - 0.6F * gmy * partialTicks);
                }

                if (this.moveRight && !this.moveLeft) {
                    this.setRotYaw(this.getRotYaw() + 0.6F * gmy * partialTicks);
                }
            } else if (!MCH_Config.MouseControlFlightSimMode.prmBool) {
                this.rotationByKey(partialTicks);
                this.setRotRoll(this.getRotRoll() + this.addkeyRotValue * 0.5F * this.getAcInfo().mobilityRoll);
            }

            this.addkeyRotValue = (float) (this.addkeyRotValue * (1.0 - 0.1F * partialTicks));
            if (!isFly && MathHelper.abs(this.getRotPitch()) < 40.0F) {
                this.applyOnGroundPitch(0.97F);
            }

            if (this.getNozzleRotation() > 0.001F) {
                float rot = 1.0F - 0.03F * partialTicks;
                this.setRotPitch(this.getRotPitch() * rot);
                rot = 1.0F - 0.1F * partialTicks;
                this.setRotRoll(this.getRotRoll() * rot);
            }
        }
    }

    protected void onUpdate_Control() {
        if (this.isGunnerMode && !this.canUseFuel()) {
            this.switchGunnerMode(false);
        }

        this.throttleBack = (float) (this.throttleBack * 0.8);
        if (this.getRiddenByEntity() != null && !this.getRiddenByEntity().isDead && this.isCanopyClose() &&
                this.canUseWing() && this.canUseFuel() && !this.isDestroyed()) {
            this.onUpdate_ControlNotHovering();
        } else if (this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {
            this.throttleUp = true;
            this.onUpdate_ControlNotHovering();
        } else if (this.getCurrentThrottle() > 0.0) {
            this.addCurrentThrottle(-0.0025 * this.getAcInfo().throttleUpDown);
        } else {
            this.setCurrentThrottle(0.0);
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

    protected void onUpdate_ControlNotHovering() {
        if (!this.isGunnerMode) {
            float throttleUpDown = this.getAcInfo().throttleUpDown;
            boolean turn = this.moveLeft && !this.moveRight || !this.moveLeft && this.moveRight;
            boolean localThrottleUp = this.throttleUp;
            if (turn && this.getCurrentThrottle() < this.getAcInfo().pivotTurnThrottle && !localThrottleUp &&
                    !this.throttleDown) {
                localThrottleUp = true;
                throttleUpDown *= 2.0F;
            }

            if (localThrottleUp) {
                float f = throttleUpDown;
                if (this.getRidingEntity() != null) {
                    double mx = this.getRidingEntity().motionX;
                    double mz = this.getRidingEntity().motionZ;
                    f = throttleUpDown * (MathHelper.sqrt(mx * mx + mz * mz) * this.getAcInfo().throttleUpDownOnEntity);
                }

                if (this.getAcInfo().enableBack && this.throttleBack > 0.0F) {
                    this.throttleBack = (float) (this.throttleBack - 0.01 * f);
                } else {
                    this.throttleBack = 0.0F;
                    if (this.getCurrentThrottle() < 1.0) {
                        this.addCurrentThrottle(0.01 * f);
                    } else {
                        this.setCurrentThrottle(1.0);
                    }
                }
            } else if (this.throttleDown) {
                if (this.getCurrentThrottle() > 0.0) {
                    this.addCurrentThrottle(-0.01 * throttleUpDown);
                } else {
                    this.setCurrentThrottle(0.0);
                    if (this.getAcInfo().enableBack) {
                        this.throttleBack = (float) (this.throttleBack + 0.0025 * throttleUpDown);
                        if (this.throttleBack > 0.6F) {
                            this.throttleBack = 0.6F;
                        }
                    }
                }
            } else if (this.cs_planeAutoThrottleDown && this.getCurrentThrottle() > 0.0) {
                this.addCurrentThrottle(-0.005 * throttleUpDown);
                if (this.getCurrentThrottle() <= 0.0) {
                    this.setCurrentThrottle(0.0);
                }
            }
        }
    }

    protected void onUpdate_Particle() {
        if (this.world.isRemote) {
            this.onUpdate_ParticleLandingGear();
            this.onUpdate_ParticleNozzle();
        }
    }

    protected void onUpdate_Particle2() {
        if (this.world.isRemote) {
            if (!(this.getHP() >= this.getMaxHP() * 0.5)) {
                if (this.getPlaneInfo() != null) {
                    int rotorNum = this.getPlaneInfo().rotorList.size();

                    if (this.isFirstDamageSmoke) {
                        this.prevDamageSmokePos = new Vec3d[rotorNum + 1];
                    }

                    float yaw = this.getRotYaw();
                    float pitch = this.getRotPitch();
                    float roll = this.getRotRoll();
                    boolean spawnSmoke = true;

                    for (int ri = 0; ri < rotorNum; ri++) {
                        if (this.getHP() >= this.getMaxHP() * 0.2 && this.getMaxHP() > 0) {
                            int d = (int) (((double) this.getHP() / this.getMaxHP() - 0.2) / 0.3 * 15.0);
                            if (d > 0 && this.rand.nextInt(d) > 0) {
                                spawnSmoke = false;
                            }
                        }

                        Vec3d rotor_pos = this.getPlaneInfo().rotorList.get(ri).pos;
                        Vec3d pos = MCH_Lib.RotVec3(rotor_pos, -yaw, -pitch, -roll);
                        double x = this.posX + pos.x;
                        double y = this.posY + pos.y;
                        double z = this.posZ + pos.z;
                        this.onUpdate_Particle2SpawnSmoke(ri, x, y, z, 1.0F, spawnSmoke);
                    }

                    spawnSmoke = true;
                    if (this.getHP() >= this.getMaxHP() * 0.2 && this.getMaxHP() > 0) {
                        int d = (int) (((double) this.getHP() / this.getMaxHP() - 0.2) / 0.3 * 15.0);
                        if (d > 0 && this.rand.nextInt(d) > 0) {
                            spawnSmoke = false;
                        }
                    }

                    double px = this.posX;
                    double py = this.posY;
                    double pz = this.posZ;
                    if (this.getSeatInfo(0) != null && this.getSeatInfo(0).pos != null) {
                        Vec3d pos = MCH_Lib.RotVec3(0.0, this.getSeatInfo(0).pos.y, -2.0, -yaw, -pitch, -roll);
                        px += pos.x;
                        py += pos.y;
                        pz += pos.z;
                    }

                    this.onUpdate_Particle2SpawnSmoke(rotorNum, px, py, pz, rotorNum == 0 ? 2.0F : 1.0F, spawnSmoke);
                    this.isFirstDamageSmoke = false;
                }
            }
        }
    }

    public void onUpdate_Particle2SpawnSmoke(int ri, double x, double y, double z, float size, boolean spawnSmoke) {
        if (this.isFirstDamageSmoke || this.prevDamageSmokePos[ri] == null) {
            this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
        }

        Vec3d prev = this.prevDamageSmokePos[ri];
        double dx = x - prev.x;
        double dy = y - prev.y;
        double dz = z - prev.z;
        int num = (int) (MathHelper.sqrt(dx * dx + dy * dy + dz * dz) / 0.3) + 1;

        for (int i = 0; i < num; i++) {
            float c = 0.2F + this.rand.nextFloat() * 0.3F;
            MCH_ParticleParam prm = new MCH_ParticleParam(
                    this.world, "smoke", prev.x + (x - prev.x) * i / 3.0, prev.y + (y - prev.y) * i / 3.0,
                    prev.z + (z - prev.z) * i / 3.0);
            prm.motionX = size * (this.rand.nextDouble() - 0.5) * 0.3;
            prm.motionY = size * this.rand.nextDouble() * 0.1;
            prm.motionZ = size * (this.rand.nextDouble() - 0.5) * 0.3;
            prm.size = size * (this.rand.nextInt(5) + 5.0F) * 1.0F;
            prm.setColor(0.7F + this.rand.nextFloat() * 0.1F, c, c, c);
            MCH_ParticlesUtil.spawnParticle(prm);
        }

        this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
    }

    public void onUpdate_ParticleLandingGear() {
        double d = this.motionX * this.motionX + this.motionZ * this.motionZ;
        if (d > 0.01) {
            int x = MathHelper.floor(this.posX + 0.5);
            int y = MathHelper.floor(this.posY - 0.5);
            int z = MathHelper.floor(this.posZ + 0.5);
            MCH_ParticlesUtil.spawnParticleTileCrack(
                    this.world,
                    x,
                    y,
                    z,
                    this.posX + (this.rand.nextFloat() - 0.5) * this.width,
                    this.getEntityBoundingBox().minY + 0.1,
                    this.posZ + (this.rand.nextFloat() - 0.5) * this.width,
                    -this.motionX * 4.0,
                    1.5,
                    -this.motionZ * 4.0);
        }
    }

    private void onUpdate_ParticleSplash() {
        if (this.getAcInfo() != null) {
            if (this.world.isRemote) {
                double mx = this.posX - this.prevPosX;
                double mz = this.posZ - this.prevPosZ;
                double dist = mx * mx + mz * mz;
                if (dist > 1.0) {
                    dist = 1.0;
                }

                for (MCH_AircraftInfo.ParticleSplash p : this.getAcInfo().particleSplashs) {
                    for (int i = 0; i < p.num; i++) {
                        if (dist > 0.03 + this.rand.nextFloat() * 0.1) {
                            this.setParticleSplash(p.pos, -mx * p.acceleration, p.motionY, -mz * p.acceleration,
                                    p.gravity, p.size * (0.5 + dist * 0.5), p.age);
                        }
                    }
                }
            }
        }
    }

    private void setParticleSplash(Vec3d pos, double mx, double my, double mz, float gravity, double size, int age) {
        Vec3d v = this.getTransformedPosition(pos);
        v = v.add(this.rand.nextDouble() - 0.5, (this.rand.nextDouble() - 0.5) * 0.5, this.rand.nextDouble() - 0.5);
        int x = (int) (v.x + 0.5);
        int y = (int) (v.y + 0.0);
        int z = (int) (v.z + 0.5);
        if (W_WorldFunc.isBlockWater(this.world, x, y, z)) {
            float c = this.rand.nextFloat() * 0.3F + 0.7F;
            MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", v.x, v.y, v.z);
            prm.motionX = mx + (this.rand.nextFloat() - 0.5) * 0.7;
            prm.motionY = my;
            prm.motionZ = mz + (this.rand.nextFloat() - 0.5) * 0.7;
            prm.size = (float) size * (this.rand.nextFloat() * 0.2F + 0.8F);
            prm.setColor(0.9F, c, c, c);
            prm.age = age + (int) (this.rand.nextFloat() * 0.5 * age);
            prm.gravity = gravity;
            MCH_ParticlesUtil.spawnParticle(prm);
        }
    }

    public void onUpdate_ParticleNozzle() {
        if (this.planeInfo != null && this.planeInfo.haveNozzle()) {
            if (!(this.getCurrentThrottle() <= 0.1F)) {
                float yaw = this.getRotYaw();
                float pitch = this.getRotPitch();
                float roll = this.getRotRoll();
                Vec3d nozzleRot = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -yaw - 180.0F, pitch - this.getNozzleRotation(), roll);

                for (MCH_AircraftInfo.DrawnPart nozzle : this.planeInfo.nozzles) {
                    if (this.rand.nextFloat() <= this.getCurrentThrottle() * 1.5) {
                        Vec3d nozzlePos = MCH_Lib.RotVec3(nozzle.pos, -yaw, -pitch, -roll);
                        double x = this.posX + nozzlePos.x + nozzleRot.x;
                        double y = this.posY + nozzlePos.y + nozzleRot.y;
                        double z = this.posZ + nozzlePos.z + nozzleRot.z;
                        float a = 0.7F;
                        if (W_WorldFunc.getBlockId(this.world, (int) (x + nozzleRot.x * 3.0),
                                (int) (y + nozzleRot.y * 3.0), (int) (z + nozzleRot.z * 3.0)) != 0) {
                            a = 2.0F;
                        }

                        MCH_ParticleParam prm = new MCH_ParticleParam(
                                this.world,
                                "smoke",
                                x,
                                y,
                                z,
                                nozzleRot.x + (this.rand.nextFloat() - 0.5F) * a,
                                nozzleRot.y,
                                nozzleRot.z + (this.rand.nextFloat() - 0.5F) * a,
                                5.0F * this.getAcInfo().particlesScale);
                        MCH_ParticlesUtil.spawnParticle(prm);
                    }
                }
            }
        }
    }

    @Override
    public void destroyAircraft() {
        super.destroyAircraft();
        int inv = 1;
        if (this.getRotRoll() >= 0.0F) {
            if (this.getRotRoll() > 90.0F) {
                inv = -1;
            }
        } else if (this.getRotRoll() > -90.0F) {
            inv = -1;
        }

        this.rotDestroyedRoll = (0.5F + this.rand.nextFloat()) * inv;
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
            if (MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
                if (MathHelper.abs(this.getRotPitch()) < 10.0F) {
                    this.setRotPitch(this.getRotPitch() + this.rotDestroyedPitch);
                }

                float roll = MathHelper.abs(this.getRotRoll());
                if (roll < 45.0F || roll > 135.0F) {
                    this.setRotRoll(this.getRotRoll() + this.rotDestroyedRoll);
                }
            } else if (MathHelper.abs(this.getRotPitch()) > 20.0F) {
                this.setRotPitch(this.getRotPitch() * 0.99F);
            }
        }

        if (this.getRiddenByEntity() != null) {}

        this.updateSound();
        this.onUpdate_Particle();
        this.onUpdate_Particle2();
        this.onUpdate_ParticleSplash();
        this.onUpdate_ParticleSandCloud(true);
        this.updateCamera(this.posX, this.posY, this.posZ);
    }

    private void onUpdate_Server() {
        double prevMotion = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        double dp = 0.0;
        if (this.canFloatWater()) {
            dp = this.getWaterDepth();
        }

        boolean levelOff = this.isGunnerMode;
        if (dp == 0.0) {
            if (this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {
                Block block = MCH_Lib.getBlockY(this, 3, -40, true);
                if (block != null && !W_Block.isEqual(block, Blocks.AIR)) {
                    block = MCH_Lib.getBlockY(this, 3, -5, true);
                    if (block == null || W_Block.isEqual(block, Blocks.AIR)) {
                        this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 2.0F);
                        if (this.getRotPitch() > -20.0F) {
                            this.setRotPitch(this.getRotPitch() - 0.5F);
                        }
                    }
                } else {
                    this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot);
                    this.setRotPitch(this.getRotPitch() * 0.95F);
                    if (this.canFoldLandingGear()) {
                        this.foldLandingGear();
                    }

                    levelOff = true;
                }
            }

            if (!levelOff) {
                this.motionY = this.motionY +
                        (0.04 + (!this.isInWater() ? this.getAcInfo().gravity : this.getAcInfo().gravityInWater));
                this.motionY = this.motionY + -0.047 * (1.0 - this.getCurrentThrottle());
            } else {
                this.motionY *= 0.8;
            }
        } else {
            this.setRotPitch(this.getRotPitch() * 0.8F, "getWaterDepth != 0");
            if (MathHelper.abs(this.getRotRoll()) < 40.0F) {
                this.setRotRoll(this.getRotRoll() * 0.9F);
            }

            if (dp < 1.0) {
                this.motionY -= 1.0E-4;
                this.motionY = this.motionY + 0.007 * this.getCurrentThrottle();
            } else {
                if (this.motionY < 0.0) {
                    this.motionY /= 2.0;
                }

                this.motionY += 0.007;
            }
        }

        float throttle = (float) (this.getCurrentThrottle() / 10.0);
        Vec3d v;
        if (this.getNozzleRotation() > 0.001F) {
            this.setRotPitch(this.getRotPitch() * 0.95F);
            v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - this.getNozzleRotation());
            if (this.getNozzleRotation() >= 90.0F) {
                v = new Vec3d(v.x * 0.8F, v.y, v.z * 0.8F);
            }
        } else {
            v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);
        }

        if (!levelOff) {
            if (this.getNozzleRotation() <= 0.01F) {
                this.motionY = this.motionY + v.y * throttle / 2.0;
            } else {
                this.motionY = this.motionY + v.y * throttle / 8.0;
            }
        }

        boolean canMove = true;
        if (!this.getAcInfo().canMoveOnGround) {
            Block block = MCH_Lib.getBlockY(this, 3, -2, false);
            if (!W_Block.isEqual(block, W_Block.getWater()) && !W_Block.isEqual(block, Blocks.AIR)) {
                canMove = false;
            }
        }

        if (canMove) {
            if (this.getAcInfo().enableBack && this.throttleBack > 0.0F) {
                this.motionX = this.motionX - v.x * this.throttleBack;
                this.motionZ = this.motionZ - v.z * this.throttleBack;
            } else {
                this.motionX = this.motionX + v.x * throttle;
                this.motionZ = this.motionZ + v.z * throttle;
            }
        }

        double motion = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        float speedLimit = this.getMaxSpeed();
        if (motion > speedLimit) {
            this.motionX *= speedLimit / motion;
            this.motionZ *= speedLimit / motion;
            motion = speedLimit;
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

        if (this.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0) {
            this.motionX = this.motionX * this.getAcInfo().motionFactor;
            this.motionZ = this.motionZ * this.getAcInfo().motionFactor;
            if (MathHelper.abs(this.getRotPitch()) < 40.0F) {
                this.applyOnGroundPitch(0.8F);
            }
        }

        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.motionY *= 0.95;
        this.motionX = this.motionX * this.getAcInfo().motionFactor;
        this.motionZ = this.motionZ * this.getAcInfo().motionFactor;
        this.setRotation(this.getRotYaw(), this.getRotPitch());
        this.onUpdate_updateBlock();
        if (this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
            this.unmountEntity();
        }
    }

    public float getMaxSpeed() {
        float f = 0.0F;
        if (this.partWing != null && this.getPlaneInfo().isVariableSweepWing) {
            f = (this.getPlaneInfo().sweepWingSpeed - this.getPlaneInfo().speed) * this.partWing.getFactor();
        } else if (this.partHatch != null && this.getPlaneInfo().isVariableSweepWing) {
            f = (this.getPlaneInfo().sweepWingSpeed - this.getPlaneInfo().speed) * this.partHatch.getFactor();
        }

        return this.getPlaneInfo().speed + f;
    }

    @Override
    public float getSoundVolume() {
        return this.getAcInfo() != null && this.getAcInfo().throttleUpDown <= 0.0F ? 0.0F : this.soundVolume * 0.7F;
    }

    public void updateSound() {
        float target = (float) this.getCurrentThrottle();
        if (this.getRiddenByEntity() != null && (this.partCanopy == null || this.getCanopyRotation() < 1.0F)) {
            target += 0.1F;
        }

        if (this.soundVolume < target) {
            this.soundVolume += 0.02F;
            if (this.soundVolume >= target) {
                this.soundVolume = target;
            }
        } else if (this.soundVolume > target) {
            this.soundVolume -= 0.02F;
            if (this.soundVolume <= target) {
                this.soundVolume = target;
            }
        }
    }

    @Override
    public float getSoundPitch() {
        return (float) (0.6 + this.getCurrentThrottle() * 0.4);
    }

    @Override
    public String getDefaultSoundName() {
        return "plane";
    }

    @Override
    public void updateParts(int stat) {
        super.updateParts(stat);
        if (!this.isDestroyed()) {
            MCH_Parts[] parts = new MCH_Parts[] { this.partNozzle, this.partWing };

            for (MCH_Parts p : parts) {
                if (p != null) {
                    p.updateStatusClient(stat);
                    p.update();
                }
            }

            if (!this.world.isRemote && this.partWing != null && this.getPlaneInfo().isVariableSweepWing &&
                    this.partWing.isON() && this.getCurrentThrottle() >= 0.2F &&
                    (this.getCurrentThrottle() < 0.5 || MCH_Lib.getBlockIdY(this, 1, -10) != 0)) {
                this.partWing.setStatusServer(false);
            }
        }
    }

    @Override
    public float getUnfoldLandingGearThrottle() {
        return 0.7F;
    }

    public boolean canSwitchVtol() {
        if (this.planeInfo == null || !this.planeInfo.isEnableVtol) {
            return false;
        } else if (this.getModeSwitchCooldown() > 0) {
            return false;
        } else if (this.getVtolMode() == 1) {
            return false;
        } else if (MathHelper.abs(this.getRotRoll()) > 30.0F) {
            return false;
        } else if (this.onGround && this.planeInfo.isDefaultVtol) {
            return false;
        } else {
            this.setModeSwitchCooldown(20);
            return true;
        }
    }

    public boolean getNozzleStat() {
        return this.partNozzle != null && this.partNozzle.getStatus();
    }

    @Override
    public int getVtolMode() {
        if (!this.getNozzleStat()) {
            return this.getNozzleRotation() <= 0.005F ? 0 : 1;
        } else {
            return this.getNozzleRotation() >= 89.995F ? 2 : 1;
        }
    }

    public float getFuleConsumptionFactor() {
        return super.getFuelConsumptionFactor() * (1);
    }

    public float getNozzleRotation() {
        return this.partNozzle != null ? this.partNozzle.rotation : 0.0F;
    }

    public float getPrevNozzleRotation() {
        return this.partNozzle != null ? this.partNozzle.prevRotation : 0.0F;
    }

    public void swithVtolMode(boolean mode) {
        if (this.partNozzle != null) {
            if (this.planeInfo.isDefaultVtol && this.onGround && !mode) {
                return;
            }

            if (!this.world.isRemote) {
                this.partNozzle.setStatusServer(mode);
            }

            if (this.getRiddenByEntity() != null && !this.getRiddenByEntity().isDead) {
                this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch = 0.0F;
            }
        }
    }

    protected MCH_Parts createNozzle(MCH_PlaneInfo info) {
        MCH_Parts nozzle = null;
        if (info.haveNozzle() || info.haveRotor() || info.isEnableVtol) {
            nozzle = new MCH_Parts(this, 1, PART_STAT, "Nozzle");
            nozzle.rotationMax = 90.0F;
            nozzle.rotationInv = 1.5F;
            nozzle.soundStartSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
            nozzle.soundEndSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
            nozzle.soundStartSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
            nozzle.soundEndSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
            nozzle.soundSwitching.setPrm("plane_cv", 1.0F, 0.5F);
            if (info.isDefaultVtol) {
                nozzle.forceSwitch(true);
            }
        }

        return nozzle;
    }

    protected MCH_Parts createWing(MCH_PlaneInfo info) {
        MCH_Parts wing = null;
        if (this.planeInfo.haveWing()) {
            wing = new MCH_Parts(this, 3, PART_STAT, "Wing");
            wing.rotationMax = 90.0F;
            wing.rotationInv = 2.5F;
            wing.soundStartSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
            wing.soundEndSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
            wing.soundStartSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
            wing.soundEndSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
        }

        return wing;
    }

    public boolean canUseWing() {
        if (this.partWing == null) {
            return true;
        } else if (this.getPlaneInfo().isVariableSweepWing) {
            return !(this.getCurrentThrottle() < 0.2) || this.partWing.isOFF();
        } else {
            return this.partWing.isOFF();
        }
    }

    public boolean canFoldWing() {
        if (this.partWing != null && this.getModeSwitchCooldown() <= 0) {
            if (this.getPlaneInfo().isVariableSweepWing) {
                if (!this.onGround && MCH_Lib.getBlockIdY(this, 3, -20) == 0) {
                    if (this.getCurrentThrottle() < 0.7F) {
                        return false;
                    }
                } else if (this.getCurrentThrottle() > 0.1F) {
                    return false;
                }
            } else {
                if (!this.onGround && MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
                    return false;
                }

                if (this.getCurrentThrottle() > 0.01F) {
                    return false;
                }
            }

            return this.partWing.isOFF();
        } else {
            return false;
        }
    }

    public boolean canUnfoldWing() {
        return this.partWing != null && this.getModeSwitchCooldown() <= 0 && this.partWing.isON();
    }

    public void foldWing(boolean fold) {
        if (this.partWing != null && this.getModeSwitchCooldown() <= 0) {
            this.partWing.setStatusServer(fold);
            this.setModeSwitchCooldown(20);
        }
    }

    public float getWingRotation() {
        return this.partWing != null ? this.partWing.rotation : 0.0F;
    }

    public float getPrevWingRotation() {
        return this.partWing != null ? this.partWing.prevRotation : 0.0F;
    }
}
