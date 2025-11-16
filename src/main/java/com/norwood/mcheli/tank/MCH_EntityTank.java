package com.norwood.mcheli.tank;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.MCH_Math;
import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.networking.packet.PacketStatusRequest;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class MCH_EntityTank extends MCH_EntityAircraft {

    public final MCH_WheelManager WheelMng;
    public float soundVolume;
    public float soundVolumeTarget;
    public float rotationRotor;
    public float prevRotationRotor;
    public float addkeyRotValue;
    private MCH_TankInfo tankInfo = null;

    public MCH_EntityTank(World world) {
        super(world);
        this.currentSpeed = 0.07;
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 0.7F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.weapons = this.createWeapon(0);
        this.soundVolume = 0.0F;
        this.stepHeight = 0.6F;
        this.rotationRotor = 0.0F;
        this.prevRotationRotor = 0.0F;
        this.WheelMng = new MCH_WheelManager(this);
    }

    @Override
    public String getKindName() {
        return "tanks";
    }

    @Override
    public String getEntityType() {
        return "Vehicle";
    }

    @Nullable
    public MCH_TankInfo getTankInfo() {
        return this.tankInfo;
    }

    @Override
    public void changeType(String type) {
        MCH_Lib.DbgLog(this.world, "MCH_EntityTank.changeType " + type + " : " + this);
        if (!type.isEmpty()) {
            this.tankInfo = MCH_TankInfoManager.get(type);
        }

        if (this.tankInfo == null) {
            MCH_Lib.Log(this, "##### MCH_EntityTank changeTankType() Tank info null %d, %s, %s",
                    W_Entity.getEntityId(this), type, this.getEntityName());
            this.setDead();
        } else {
            this.setAcInfo(this.tankInfo);
            this.newSeats(this.getAcInfo().getNumSeatAndRack());
            this.switchFreeLookModeClient(this.getAcInfo().defaultFreelook);
            this.weapons = this.createWeapon(1 + this.getSeatNum());
            this.initPartRotation(this.getRotYaw(), this.getRotPitch());
            this.WheelMng.createWheels(this.world, this.getAcInfo().wheels,
                    new Vec3d(0.0, -GLOBAL_Y_OFFSET, this.getTankInfo().weightedCenterZ));
        }
    }

    @Nullable
    @Override
    public Item getItem() {
        return this.getTankInfo() != null ? this.getTankInfo().item : null;
    }

    @Override
    public boolean canMountWithNearEmptyMinecart() {
        return MCH_Config.MountMinecartTank.prmBool;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    public float getGiveDamageRot() {
        return 91.0F;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        super.writeEntityToNBT(par1NBTTagCompound);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        super.readEntityFromNBT(par1NBTTagCompound);
        if (this.tankInfo == null) {
            this.tankInfo = MCH_TankInfoManager.get(this.getTypeName());
            if (this.tankInfo == null) {
                MCH_Lib.Log(this, "##### MCH_EntityTank readEntityFromNBT() Tank info null %d, %s",
                        W_Entity.getEntityId(this), this.getEntityName());
                this.setDead();
            } else {
                this.setAcInfo(this.tankInfo);
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
    }

    @Override
    public void onInteractFirst(EntityPlayer player) {
        this.addkeyRotValue = 0.0F;
        player.rotationYawHead = player.prevRotationYawHead = this.getLastRiderYaw();
        player.prevRotationYaw = player.rotationYaw = this.getLastRiderYaw();
        player.rotationPitch = this.getLastRiderPitch();
    }

    @Override
    public boolean canSwitchGunnerMode() {
        return false;
    }

    @Override
    public void onUpdateAircraft() {
        if (this.tankInfo == null) {
            this.changeType(this.getTypeName());
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
        } else {
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

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
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

    @SideOnly(Side.CLIENT)
    public boolean canRenderOnFire() {
        return this.isDestroyed() || super.canRenderOnFire();
    }

    @Override
    public void updateExtraBoundingBox() {
        if (this.world.isRemote) {
            super.updateExtraBoundingBox();
        } else if (this.getCountOnUpdate() <= 1) {
            super.updateExtraBoundingBox();
            super.updateExtraBoundingBox();
        }
    }

    public MCH_EntityTank.ClacAxisBB calculateXOffset(List<AxisAlignedBB> list, AxisAlignedBB bb, double x) {
        for (AxisAlignedBB axisAlignedBB : list) {
            x = axisAlignedBB.calculateXOffset(bb, x);
        }

        return new MCH_EntityTank.ClacAxisBB(x, bb.offset(x, 0.0, 0.0));
    }

    public MCH_EntityTank.ClacAxisBB calculateYOffset(List<AxisAlignedBB> list, AxisAlignedBB bb, double y) {
        return this.calculateYOffset(list, bb, bb, y);
    }

    public MCH_EntityTank.ClacAxisBB calculateYOffset(List<AxisAlignedBB> list, AxisAlignedBB calcBB,
                                                      AxisAlignedBB offsetBB, double y) {
        for (AxisAlignedBB axisAlignedBB : list) {
            y = axisAlignedBB.calculateYOffset(calcBB, y);
        }

        return new MCH_EntityTank.ClacAxisBB(y, offsetBB.offset(0.0, y, 0.0));
    }

    public MCH_EntityTank.ClacAxisBB calculateZOffset(List<AxisAlignedBB> list, AxisAlignedBB bb, double z) {
        for (AxisAlignedBB axisAlignedBB : list) {
            z = axisAlignedBB.calculateZOffset(bb, z);
        }

        return new MCH_EntityTank.ClacAxisBB(z, bb.offset(0.0, 0.0, z));
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
        this.world.profiler.startSection("move");
        double d2 = x;
        double d3 = y;
        double d4 = z;
        List<AxisAlignedBB> list1 = getCollisionBoxes(this, this.getEntityBoundingBox().expand(x, y, z));
        AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
        if (y != 0.0) {
            MCH_EntityTank.ClacAxisBB v = this.calculateYOffset(list1, this.getEntityBoundingBox(), y);
            y = v.value;
            this.setEntityBoundingBox(v.bb);
        }

        boolean flag = this.onGround || d3 != y && d3 < 0.0;

        for (MCH_BoundingBox ebb : this.extraBoundingBox) {
            ebb.updatePosition(this.posX, this.posY, this.posZ, this.getRotYaw(), this.getRotPitch(),
                    this.getRotRoll());
        }

        if (x != 0.0) {
            MCH_EntityTank.ClacAxisBB v = this.calculateXOffset(list1, this.getEntityBoundingBox(), x);
            x = v.value;
            if (x != 0.0) {
                this.setEntityBoundingBox(v.bb);
            }
        }

        if (z != 0.0) {
            MCH_EntityTank.ClacAxisBB v = this.calculateZOffset(list1, this.getEntityBoundingBox(), z);
            z = v.value;
            if (z != 0.0) {
                this.setEntityBoundingBox(v.bb);
            }
        }

        if (this.stepHeight > 0.0F && flag && (d2 != x || d4 != z)) {
            double d14 = x;
            double d6 = y;
            double d7 = z;
            AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
            this.setEntityBoundingBox(axisalignedbb);
            y = this.stepHeight;
            List<AxisAlignedBB> list = getCollisionBoxes(this, this.getEntityBoundingBox().expand(d2, y, d4));
            AxisAlignedBB axisalignedbb2 = this.getEntityBoundingBox();
            AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0, d4);
            MCH_EntityTank.ClacAxisBB v = this.calculateYOffset(list, axisalignedbb3, axisalignedbb2, y);
            double d8 = v.value;
            axisalignedbb2 = v.bb;
            v = this.calculateXOffset(list, axisalignedbb2, d2);
            double d18 = v.value;
            axisalignedbb2 = v.bb;
            v = this.calculateZOffset(list, axisalignedbb2, d4);
            double d19 = v.value;
            axisalignedbb2 = v.bb;
            AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
            v = this.calculateYOffset(list, axisalignedbb4, y);
            double d20 = v.value;
            axisalignedbb4 = v.bb;
            v = this.calculateXOffset(list, axisalignedbb4, d2);
            double d21 = v.value;
            axisalignedbb4 = v.bb;
            v = this.calculateZOffset(list, axisalignedbb4, d4);
            double d22 = v.value;
            axisalignedbb4 = v.bb;
            double d23 = d18 * d18 + d19 * d19;
            double d9 = d21 * d21 + d22 * d22;
            if (d23 > d9) {
                x = d18;
                z = d19;
                y = -d8;
                this.setEntityBoundingBox(axisalignedbb2);
            } else {
                x = d21;
                z = d22;
                y = -d20;
                this.setEntityBoundingBox(axisalignedbb4);
            }

            v = this.calculateYOffset(list, this.getEntityBoundingBox(), y);
            y = v.value;
            this.setEntityBoundingBox(v.bb);
            if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                x = d14;
                y = d6;
                z = d7;
                this.setEntityBoundingBox(axisalignedbb1);
            }
        }

        this.world.profiler.endSection();
        this.world.profiler.startSection("rest");
        this.resetPositionToBB();
        this.collidedHorizontally = d2 != x || d4 != z;
        this.collidedVertically = d3 != y;
        this.onGround = this.collidedVertically && d3 < 0.0;
        this.collided = this.collidedHorizontally || this.collidedVertically;
        int j6 = MathHelper.floor(this.posX);
        int i1 = MathHelper.floor(this.posY - 0.2F);
        int k6 = MathHelper.floor(this.posZ);
        BlockPos blockpos = new BlockPos(j6, i1, k6);
        IBlockState iblockstate = this.world.getBlockState(blockpos);
        if (iblockstate.getMaterial() == Material.AIR) {
            BlockPos blockpos1 = blockpos.down();
            IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
            Block block1 = iblockstate1.getBlock();
            if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
                iblockstate = iblockstate1;
                blockpos = blockpos1;
            }
        }

        this.updateFallState(y, this.onGround, iblockstate, blockpos);
        if (d2 != x) {
            this.motionX = 0.0;
        }

        if (d4 != z) {
            this.motionZ = 0.0;
        }

        Block block = iblockstate.getBlock();
        if (d3 != y) {
            block.onLanded(this.world, this);
        }

        try {
            this.doBlockCollisions();
        } catch (Throwable var45) {
            CrashReport crashreport = CrashReport.makeCrashReport(var45, "Checking entity block collision");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
            this.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
        }

        this.world.profiler.endSection();
    }

    private void rotationByKey(float partialTicks) {
        float rot = 0.2F;
        if (this.moveLeft && !this.moveRight) {
            this.addkeyRotValue -= rot * partialTicks;
        }

        if (this.moveRight && !this.moveLeft) {
            this.addkeyRotValue += rot * partialTicks;
        }
    }

    /**
     * public void onUpdateAngles(float partialTicks) {
     * if(!this.isDestroyed()) {
     * if(super.isGunnerMode) {
     * this.setRotPitch(this.getRotPitch() * 0.95F);
     * this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 0.2F);
     * if(MathHelper.abs(this.getRotRoll()) > 20.0F) {
     * this.setRotRoll(this.getRotRoll() * 0.95F);
     * }
     * }
     * <p>
     * this.updateRecoil(partialTicks);
     * this.setRotPitch(this.getRotPitch() + (this.WheelMng.targetPitch - this.getRotPitch()) * partialTicks);
     * this.setRotRoll(this.getRotRoll() + (this.WheelMng.targetRoll - this.getRotRoll()) * partialTicks);
     * boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;
     * //System.out.println("isfly" + isFly);
     * <p>
     * //logic for like rotation
     * if(!isFly || this.getAcInfo().isFloat && this.getWaterDepth() > 0.0D) {
     * float rotonground = 1.0F;
     * if(!isFly) {
     * rotonground = this.getAcInfo().mobilityYawOnGround;
     * if(!this.getAcInfo().canRotOnGround) {
     * Block pivotTurnThrottle = MCH_Lib.getBlockY(this, 3, -2, false);
     * if(!W_Block.isEqual(pivotTurnThrottle, W_Block.getWater()) && !W_Block.isEqual(pivotTurnThrottle, Blocks.air)) {
     * rotonground = 0.0F;
     * }
     * }
     * }
     * <p>
     * float pivotTurnThrottle1 = this.getAcInfo().pivotTurnThrottle;
     * double dx = super.posX - super.prevPosX;
     * double dz = super.posZ - super.prevPosZ;
     * double dist = dx * dx + dz * dz;
     * <p>
     * if(pivotTurnThrottle1 <= 0.0F || this.getCurrentThrottle() >= (double)pivotTurnThrottle1 || super.throttleBack >=
     * pivotTurnThrottle1 / 10.0F || dist > (double)super.throttleBack * 0.01D) {
     * float sf = (float)Math.sqrt(dist <= 1.0D?dist:1.0D);
     * if(pivotTurnThrottle1 <= 0.0F) {
     * sf = 1.0F;
     * }
     * <p>
     * float flag = !super.throttleUp && super.throttleDown && this.getCurrentThrottle() < (double)pivotTurnThrottle1 +
     * 0.05D?-1.0F:1.0F;
     * if(super.moveLeft && !super.moveRight) {
     * this.setRotYaw(this.getRotYaw() - 0.6F * rotonground * partialTicks * flag * sf);
     * }
     * <p>
     * if(super.moveRight && !super.moveLeft) {
     * this.setRotYaw(this.getRotYaw() + 0.6F * rotonground * partialTicks * flag * sf);
     * }
     * <p>
     * }
     * }
     * <p>
     * this.addkeyRotValue = (float)((double)this.addkeyRotValue * (1.0D - (double)(0.1F * partialTicks)));
     * }
     * }
     * 1.7.10 bullshit
     */

    @Override
    public void onUpdateAngles(float partialTicks) {
        if (!this.isDestroyed()) {

            // keep gunner damping / autopilot unchanged
            if (this.isGunnerMode) {
                this.setRotPitch(this.getRotPitch() * 0.95F);
                this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 0.2F);
                if (MathHelper.abs(this.getRotRoll()) > 20.0F) {
                    this.setRotRoll(this.getRotRoll() * 0.95F);
                }
            }

            this.updateRecoil(partialTicks);

            // --- SAFER PITCH INTERPOLATION (prevents tiny spikes from producing visible pitch jumps)
            // Compute base interpolation from WheelMng.targetPitch but scale/clamp it when on ground or under throttle
            float currentPitch = this.getRotPitch();
            float targetPitch = this.WheelMng.targetPitch;

            // base interpolation
            float baseInterp = partialTicks;

            // if on ground (not flying) reduce interpolation strength so wheel pitch adjustments are much slower
            boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;
            boolean nearWaterFloat = this.getAcInfo().isFloat && this.getWaterDepth() > 0.0;
            if (!isFly || nearWaterFloat) {
                // mobility factor from acInfo (like original code)
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
                // reduce pitch interpolation while on ground. multiply by gmy too so immobile vehicles remain locked.
                baseInterp *= Math.max(0.12F, 0.35F * gmy); // small fraction on ground (12%..35% of normal)
            }

            // also reduce interpolation when throttle is being applied strongly (prevents throttle spikes causing
            // pitch)
            double throttle = this.getCurrentThrottle();
            if (throttle > 0.05D) {
                // scale down interpolation by up to half when throttle high
                float throttleFactor = 1.0F - Math.min(0.8F, (float) throttle) * 0.35F;
                baseInterp *= throttleFactor;
            }

            // compute the desired new pitch and clamp the per-tick delta to avoid jumps
            float desiredPitch = currentPitch + (targetPitch - currentPitch) * baseInterp;

            // clamp per-tick pitch change (in degrees) to avoid sudden jumps from small impulses
            final float MAX_PITCH_DELTA_PER_TICK = 0.9F; // tweak this smaller if you want even gentler motion
            float pitchDelta = desiredPitch - currentPitch;
            if (pitchDelta > MAX_PITCH_DELTA_PER_TICK) {
                pitchDelta = MAX_PITCH_DELTA_PER_TICK;
            } else if (pitchDelta < -MAX_PITCH_DELTA_PER_TICK) {
                pitchDelta = -MAX_PITCH_DELTA_PER_TICK;
            }
            this.setRotPitch(currentPitch + pitchDelta);

            // --- ROLL interpolation (kept similar to original, but also slightly damped on ground)
            float currentRoll = this.getRotRoll();
            float targetRoll = this.WheelMng.targetRoll;
            float rollInterp = partialTicks;
            if (!isFly || nearWaterFloat) {
                rollInterp *= 0.5F; // damp roll interpolation on ground
            }
            this.setRotRoll(currentRoll + (targetRoll - currentRoll) * rollInterp);

            // --- YAW (pivot turning) logic follows original but using computed gmy/pivot checks
            if (!isFly || nearWaterFloat) {
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

                float pivotTurnThrottle = this.getAcInfo().pivotTurnThrottle;
                double dx = this.posX - this.prevPosX;
                double dz = this.posZ - this.prevPosZ;
                double dist = dx * dx + dz * dz;

                if (pivotTurnThrottle <= 0.0F || this.getCurrentThrottle() >= pivotTurnThrottle ||
                        this.throttleBack >= pivotTurnThrottle / 10.0F || dist > this.throttleBack * 0.01) {
                    float sf = (float) Math.sqrt(Math.min(dist, 1.0));
                    if (pivotTurnThrottle <= 0.0F) {
                        sf = 1.0F;
                    }

                    float flag = !this.throttleUp && this.throttleDown &&
                            this.getCurrentThrottle() < pivotTurnThrottle + 0.05 ? -1.0F : 1.0F;
                    if (this.moveLeft && !this.moveRight) {
                        this.setRotYaw(this.getRotYaw() - 0.6F * gmy * partialTicks * flag * sf);
                    }

                    if (this.moveRight && !this.moveLeft) {
                        this.setRotYaw(this.getRotYaw() + 0.6F * gmy * partialTicks * flag * sf);
                    }
                }
            } else {
                // when flying, yaw is allowed normally (original behavior)
                float pivotTurnThrottle = this.getAcInfo().pivotTurnThrottle;
                double dx = this.posX - this.prevPosX;
                double dz = this.posZ - this.prevPosZ;
                double dist = dx * dx + dz * dz;

                if (pivotTurnThrottle <= 0.0F || this.getCurrentThrottle() >= pivotTurnThrottle ||
                        this.throttleBack >= pivotTurnThrottle / 10.0F || dist > this.throttleBack * 0.01) {
                    float sf = (float) Math.sqrt(Math.min(dist, 1.0));
                    if (pivotTurnThrottle <= 0.0F) {
                        sf = 1.0F;
                    }

                    float flag = !this.throttleUp && this.throttleDown &&
                            this.getCurrentThrottle() < pivotTurnThrottle + 0.05 ? -1.0F : 1.0F;
                    if (this.moveLeft && !this.moveRight) {
                        this.setRotYaw(this.getRotYaw() - 0.6F * partialTicks * flag * sf);
                    }

                    if (this.moveRight && !this.moveLeft) {
                        this.setRotYaw(this.getRotYaw() + 0.6F * partialTicks * flag * sf);
                    }
                }
            }

            this.addkeyRotValue = (float) (this.addkeyRotValue * (1.0 - 0.1F * partialTicks));
        }
    }

    protected void onUpdate_Control() {
        if (this.isGunnerMode && !this.canUseFuel()) {
            this.switchGunnerMode(false);
        }

        this.throttleBack = (float) (this.throttleBack * 0.8);
        if (this.getBrake()) {
            this.throttleBack = (float) (this.throttleBack * 0.5);
            if (this.getCurrentThrottle() > 0.0) {
                this.addCurrentThrottle(-0.02 * this.getAcInfo().throttleUpDown);
            } else {
                this.setCurrentThrottle(0.0);
            }
        }

        if (this.getRiddenByEntity() != null && !this.getRiddenByEntity().isDead && this.isCanopyClose() &&
                this.canUseFuel() && !this.isDestroyed()) {
            this.onUpdate_ControlSub();
        } else if (this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {
            this.throttleUp = true;
            this.onUpdate_ControlSub();
        } else if (this.getCurrentThrottle() > 0.0) {
            this.addCurrentThrottle(-0.0025 * this.getAcInfo().throttleUpDown);
        } else {
            this.setCurrentThrottle(0.0);
        }

        if (this.getCurrentThrottle() < 0.0) {
            this.setCurrentThrottle(0.0);
        }

        if (this.world.isRemote) {
            if (!W_Lib.isClientPlayer(this.getRiddenByEntity()) || this.getCountOnUpdate() % 200 == 0) {
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

    protected void onUpdate_ControlSub() {
        if (!super.isGunnerMode) {
            float throttleUpDown = this.getAcInfo().throttleUpDown;
            if (super.throttleUp) {
                float f = throttleUpDown;
                if (this.getRidingEntity() != null) {
                    double mx = this.getRidingEntity().motionX;
                    double mz = this.getRidingEntity().motionZ;
                    f = throttleUpDown * MathHelper.sqrt(mx * mx + mz * mz) * this.getAcInfo().throttleUpDownOnEntity;
                }

                if (this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
                    super.throttleBack = (float) ((double) super.throttleBack - 0.01D * (double) f);
                } else {
                    super.throttleBack = 0.0F;
                    if (this.getCurrentThrottle() < 1.0D) {
                        this.addCurrentThrottle(0.01D * (double) f);
                        // here as well?
                    } else {
                        this.setCurrentThrottle(1.0D);
                        // implement a new variable here to add throttle control for specific vehicles specifically 1.8D
                    }
                }

            } else if (super.throttleDown) {

                if (this.getCurrentThrottle() > 0.0D) {
                    this.addCurrentThrottle(-0.01D * (double) throttleUpDown);
                } else {
                    this.setCurrentThrottle(0.0D);
                    if (this.getAcInfo().enableBack) {
                        // super.throttleBack = (float)((double)super.throttleBack + 0.0025D * (double)throttleUpDown);
                        super.throttleBack = (float) ((double) super.throttleBack +
                                0.0025D * (double) throttleUpDown * getAcInfo().throttleDownFactor);
                        // if(super.throttleBack > 0.6F) { //todno: add a new variable here for reversespeed
                        // super.throttleBack = 0.6F;
                        // }
                        float pivotTurnThrottle1 = this.getAcInfo().pivotTurnThrottle;
                        if (pivotTurnThrottle1 > 0) {
                            if (super.throttleBack > 0) {
                                double dx = super.posX - super.prevPosX;
                                double dz = super.posZ - super.prevPosZ;
                                double dist = dx * dx + dz * dz;
                                float sf = (float) Math.sqrt(dist <= 1.0D ? dist : 1.0D);
                                if (pivotTurnThrottle1 <= 0.0F) {
                                    sf = 1.0F;
                                }

                                float rotonground = 1.0F;
                                boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;

                                if (!isFly) {
                                    rotonground = this.getAcInfo().mobilityYawOnGround;
                                    if (!this.getAcInfo().canRotOnGround) {
                                        Block pivotTurnThrottle = MCH_Lib.getBlockY(this, 3, -2, false);
                                        if (!W_Block.isEqual(pivotTurnThrottle, W_Block.getWater()) &&
                                                !W_Block.isEqual(pivotTurnThrottle, Blocks.AIR)) {
                                            rotonground = 0.0F;
                                        }
                                    }
                                }

                                float flag = !super.throttleUp && super.throttleDown &&
                                        this.getCurrentThrottle() < (double) pivotTurnThrottle1 + 0.05D ? -1.0F : 1.0F;
                                if (super.moveLeft && !super.moveRight) {
                                    this.setRotYaw(this.getRotYaw() + 0.6F * rotonground * flag * sf);
                                }

                                if (super.moveRight && !super.moveLeft) {
                                    this.setRotYaw(this.getRotYaw() - 0.6F * rotonground * flag * sf);
                                }
                            }
                        }
                    }
                }
            } else if (super.cs_tankAutoThrottleDown && this.getCurrentThrottle() > 0.0D) {
                this.addCurrentThrottle(-0.005D * (double) throttleUpDown);
                if (this.getCurrentThrottle() <= 0.0D) {
                    this.setCurrentThrottle(0.0D);
                }
            }
        }
    }

    protected void onUpdate_Particle2() {
        if (this.world.isRemote) {
            if (!(this.getHP() >= this.getMaxHP() * 0.5)) {
                if (this.getTankInfo() != null) {
                    int bbNum = this.getTankInfo().extraBoundingBox.size();

                    if (this.isFirstDamageSmoke || this.prevDamageSmokePos.length != bbNum + 1) {
                        this.prevDamageSmokePos = new Vec3d[bbNum + 1];
                    }

                    float yaw = this.getRotYaw();
                    float pitch = this.getRotPitch();
                    float roll = this.getRotRoll();

                    for (int ri = 0; ri < bbNum; ri++) {
                        if (this.getHP() >= this.getMaxHP() * 0.2 && this.getMaxHP() > 0) {
                            int d = (int) (((double) this.getHP() / this.getMaxHP() - 0.2) / 0.3 * 15.0);
                            if (d > 0 && this.rand.nextInt(d) > 0) {}
                        } else {
                            MCH_BoundingBox bb = this.getTankInfo().extraBoundingBox.get(ri);
                            Vec3d pos = this.getTransformedPosition(bb.offsetX, bb.offsetY, bb.offsetZ);
                            double x = pos.x;
                            double y = pos.y;
                            double z = pos.z;
                            this.onUpdate_Particle2SpawnSmoke(ri, x, y, z, 1.0F);
                        }
                    }

                    boolean b = true;
                    if (this.getHP() >= this.getMaxHP() * 0.2 && this.getMaxHP() > 0) {
                        int d = (int) (((double) this.getHP() / this.getMaxHP() - 0.2) / 0.3 * 15.0);
                        if (d > 0 && this.rand.nextInt(d) > 0) {
                            b = false;
                        }
                    }

                    if (b) {
                        double px = this.posX;
                        double py = this.posY;
                        double pz = this.posZ;
                        if (this.getSeatInfo(0) != null && this.getSeatInfo(0).pos != null) {
                            Vec3d pos = MCH_Lib.RotVec3(0.0, this.getSeatInfo(0).pos.y, -2.0, -yaw, -pitch, -roll);
                            px += pos.x;
                            py += pos.y;
                            pz += pos.z;
                        }

                        this.onUpdate_Particle2SpawnSmoke(bbNum, px, py, pz, bbNum == 0 ? 2.0F : 1.0F);
                    }

                    this.isFirstDamageSmoke = false;
                }
            }
        }
    }

    public void onUpdate_Particle2SpawnSmoke(int ri, double x, double y, double z, float size) {
        if (this.isFirstDamageSmoke || this.prevDamageSmokePos[ri] == null) {
            this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
        }

        int num = 1;

        for (int i = 0; i < num; i++) {
            float c = 0.2F + this.rand.nextFloat() * 0.3F;
            MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", x, y, z);
            prm.motionX = size * (this.rand.nextDouble() - 0.5) * 0.3;
            prm.motionY = size * this.rand.nextDouble() * 0.1;
            prm.motionZ = size * (this.rand.nextDouble() - 0.5) * 0.3;
            prm.size = size * (this.rand.nextInt(5) + 5.0F) * 1.0F;
            prm.setColor(0.7F + this.rand.nextFloat() * 0.1F, c, c, c);
            MCH_ParticlesUtil.spawnParticle(prm);
        }

        this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
    }

    public void onUpdate_Particle2SpawnSmode(int ri, double x, double y, double z, float size) {
        if (this.isFirstDamageSmoke) {
            this.prevDamageSmokePos[ri] = new Vec3d(x, y, z);
        }

        Vec3d prev = this.prevDamageSmokePos[ri];
        double dx = x - prev.x;
        double dy = y - prev.y;
        double dz = z - prev.z;
        int num = (int) (MathHelper.sqrt(dx * dx + dy * dy + dz * dz) / 0.3) + 1;

        for (int i = 0; i < num; i++) {
            float c = 0.2F + this.rand.nextFloat() * 0.3F;
            MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", x, y, z);
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
        this.WheelMng.particleLandingGear();
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

    @Override
    public void destroyAircraft() {
        super.destroyAircraft();
        this.rotDestroyedPitch = 0.0F;
        this.rotDestroyedRoll = 0.0F;
        this.rotDestroyedYaw = 0.0F;
    }

    @Override
    public int getClientPositionDelayCorrection() {
        return this.getTankInfo().weightType == 1 ? 2 : (7);
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
                // todo marker
            }

            if (this.isInWater()) {
                this.motionX *= 0.99;
                this.motionZ *= 0.99;
            }
        }

        this.updateWheels();
        this.onUpdate_Particle2();
        this.updateSound();
        if (this.world.isRemote) {
            this.onUpdate_ParticleLandingGear();
            this.onUpdate_ParticleSplash();
            this.onUpdate_ParticleSandCloud(true);
        }

        this.updateCamera(this.posX, this.posY, this.posZ);
    }

    @Override
    public void applyOnGroundPitch(float factor) {}

    /***
     * 1.7.10 method - for reference
     *
     * private void onUpdate_Server() {
     * 
     * //1.7.10
     * 
     * //todo gear shifts here
     * Entity rdnEnt = this.getRiddenByEntity();
     * double prevMotion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
     * double dp = 0.0D;
     * if(this.canFloatWater()) {
     * dp = this.getWaterDepth();
     * }
     * 
     * boolean levelOff = super.isGunnerMode;
     * if(dp == 0.0D) {
     * if(!levelOff) {
     * super.motionY += 0.04D + (double)(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater);
     * super.motionY += -0.047D * (1.0D - this.getCurrentThrottle());
     * } else {
     * super.motionY *= 0.8D;
     * }
     * } else {
     * if(MathHelper.abs(this.getRotRoll()) < 40.0F) {
     * ;
     * }
     * 
     * if(dp < 1.0D) {
     * super.motionY -= 1.0E-4D;
     * super.motionY += 0.007D * this.getCurrentThrottle();
     * } else {
     * if(super.motionY < 0.0D) {
     * super.motionY /= 2.0D;
     * }
     * 
     * super.motionY += 0.007D;
     * }
     * }
     * 
     * float throttle = (float)(this.getCurrentThrottle() / 10.0D);
     * 
     * Vec3 v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);
     * if(!levelOff) {
     * super.motionY += v.yCoord * (double)throttle / 8.0D;
     * }
     * 
     * boolean canMove = true;
     * if(!this.getAcInfo().canMoveOnGround) {
     * Block motion = MCH_Lib.getBlockY(this, 3, -2, false);
     * if(!W_Block.isEqual(motion, W_Block.getWater()) && !W_Block.isEqual(motion, Blocks.air)) {
     * canMove = false;
     * }
     * }
     * 
     * if(canMove) {
     * if(this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
     * super.motionX -= v.xCoord * (double)super.throttleBack;
     * super.motionZ -= v.zCoord * (double)super.throttleBack;
     * } else {
     * super.motionX += v.xCoord * (double)throttle;
     * super.motionZ += v.zCoord * (double)throttle;
     * }
     * }
     * 
     * double motion1 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
     * float speedLimit = this.getMaxSpeed();
     * //todo maybe this is causing the 1.15 max speed hardcap?
     * // we know mcheli is capable of handling way faster speeds so why are ground vehicles limited to 1.15?
     * if(motion1 > (double)speedLimit) {
     * super.motionX *= (double)speedLimit / motion1;
     * super.motionZ *= (double)speedLimit / motion1;
     * motion1 = speedLimit;
     * }
     * 
     * if(motion1 > prevMotion && super.currentSpeed < (double)speedLimit) {
     * super.currentSpeed += ((double)speedLimit - super.currentSpeed) / 35.0D;
     * if(super.currentSpeed > (double)speedLimit) {
     * super.currentSpeed = (double)speedLimit;
     * }
     * } else {
     * super.currentSpeed -= (super.currentSpeed - 0.07D) / 35.0D;
     * if(super.currentSpeed < 0.07D) {
     * super.currentSpeed = 0.07D;
     * }
     * }
     * 
     * if(super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0) {
     * super.motionX *= (double)this.getAcInfo().motionFactor;
     * super.motionZ *= (double)this.getAcInfo().motionFactor;
     * if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
     * this.applyOnGroundPitch(0.8F);
     * }
     * }
     * 
     * this.updateWheels();
     * this.moveEntity(super.motionX, super.motionY, super.motionZ);
     * super.motionY *= 0.95D;
     * super.motionX *= (double)this.getAcInfo().motionFactor;
     * super.motionZ *= (double)this.getAcInfo().motionFactor;
     * this.setRotation(this.getRotYaw(), this.getRotPitch());
     * this.onUpdate_updateBlock();
     * this.updateCollisionBox();
     * if(this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
     * this.unmountEntity();
     * super.riddenByEntity = null;
     * }
     * 
     * //if(this.) todo gear check
     * 
     * }
     **/

    private void onUpdate_Server() {
        // 1.12.2 - patched: normalize thrust, avoid vertical throttle on-ground,
        // and clamp pitch changes from applyOnGroundPitch to prevent wobble.
        double prevMotion = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        double dp = 0.0;
        if (this.canFloatWater()) {
            dp = this.getWaterDepth();
        }

        boolean levelOff = this.isGunnerMode;
        if (dp == 0.0) {
            if (!levelOff) {
                this.motionY = this.motionY +
                        (0.04 + (!this.isInWater() ? this.getAcInfo().gravity : this.getAcInfo().gravityInWater));
                this.motionY = this.motionY + -0.047 * (1.0 - this.getCurrentThrottle());
            } else {
                this.motionY *= 0.8;
            }
        } else if (!(MathHelper.abs(this.getRotRoll()) >= 40.0F) && !(dp < 1.0)) {
            if (this.motionY < 0.0) {
                this.motionY /= 2.0;
            }
            this.motionY += 0.007;
        } else {
            this.motionY -= 1.0E-4;
            this.motionY = this.motionY + 0.007 * this.getCurrentThrottle();
        }

        // compute throttle & direction vector
        float throttle = (float) (this.getCurrentThrottle() / 10.0);
        Vec3d v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);

        // normalize vector (ensure consistent magnitude like 1.7 behaviour)
        double vx = v.x;
        double vy = v.y;
        double vz = v.z;
        double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vlen != 0.0D) {
            vx /= vlen;
            vy /= vlen;
            vz /= vlen;
            v = new Vec3d(vx, vy, vz);
        }

        // thresholds
        final double AIRBORNE_SPEED_MIN = 0.05D;    // require a small horizontal speed before throttle gives lift
        final double SMALL_MOTIONY_CLAMP = 0.03D;   // zero small vertical motion when on ground
        final float MAX_PITCH_DELTA = 0.6F;         // maximum pitch change allowed per tick from applyOnGroundPitch

        // apply vertical thrust: only meaningful vertical lift while airborne and moving
        if (!levelOff) {
            double horizSpeed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            if (!this.onGround && horizSpeed > AIRBORNE_SPEED_MIN) {
                this.motionY += v.y * throttle / 8.0;
            } else {
                // intentionally skip tiny vertical kicks while grounded to avoid pitch wobble
                // If you want a tiny bump for climbing small obstacles, change the line below:
                // this.motionY += v.y * throttle / 32.0;
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

            // zero very small vertical movement before pitch adjustments so applyOnGroundPitch isn't triggered by
            // micro-spikes
            if (Math.abs(this.motionY) < SMALL_MOTIONY_CLAMP) {
                this.motionY = 0.0;
            }

            // call applyOnGroundPitch but clamp any sudden rotation delta it creates
            float prevPitch = this.getRotPitch();
            this.applyOnGroundPitch(0.8F);
            float afterPitch = this.getRotPitch();
            float delta = afterPitch - prevPitch;
            if (delta > MAX_PITCH_DELTA) {
                delta = MAX_PITCH_DELTA;
            } else if (delta < -MAX_PITCH_DELTA) {
                delta = -MAX_PITCH_DELTA;
            }
            // only update rotation to a clamped pitch
            if (delta != (afterPitch - prevPitch)) {
                this.setRotation(this.getRotYaw(), prevPitch + delta);
            }
        }

        this.updateWheels();
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.motionY *= 0.95;
        this.motionX = this.motionX * this.getAcInfo().motionFactor;
        this.motionZ = this.motionZ * this.getAcInfo().motionFactor;
        this.setRotation(this.getRotYaw(), this.getRotPitch());
        this.onUpdate_updateBlock();
        this.updateCollisionBox();
        if (this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
            this.unmountEntity();
        }
    }

    private void collisionEntity(AxisAlignedBB bb) {
        if (bb != null) {
            double speed = Math
                    .sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
            if (!(speed <= 0.05)) {
                Entity rider = this.getRiddenByEntity();
                float damage = (float) (speed * 15.0);
                MCH_EntityAircraft rideAc = this.getRidingEntity() instanceof MCH_EntitySeat ?
                        ((MCH_EntitySeat) this.getRidingEntity()).getParent() :
                        (this.getRidingEntity() instanceof MCH_EntityAircraft ?
                                (MCH_EntityAircraft) this.getRidingEntity() : null);
                List<Entity> list = this.world
                        .getEntitiesInAABBexcluding(
                                this,
                                bb.grow(0.3, 0.3, 0.3),
                                ex -> {
                                    if (ex != rideAc && !(ex instanceof EntityItem) && !(ex instanceof EntityXPOrb) &&
                                            !(ex instanceof MCH_EntityBaseBullet) && !(ex instanceof MCH_EntityChain) &&
                                            !(ex instanceof MCH_EntitySeat)) {
                                        if (ex instanceof MCH_EntityTank tank) {
                                            if (tank.getTankInfo() != null && tank.getTankInfo().weightType == 2) {
                                                return MCH_Config.Collision_EntityTankDamage.prmBool;
                                            }
                                        }

                                        return MCH_Config.Collision_EntityDamage.prmBool;
                                    } else {
                                        return false;
                                    }
                                });

                for (Entity e : list) {
                    if (this.shouldCollisionDamage(e)) {
                        double dx = e.posX - this.posX;
                        double dz = e.posZ - this.posZ;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 5.0) {
                            dist = 5.0;
                        }

                        damage = (float) (damage + (5.0 - dist));
                        DamageSource ds;
                        if (rider instanceof EntityLivingBase) {
                            ds = DamageSource.causeMobDamage((EntityLivingBase) rider);
                        } else {
                            ds = DamageSource.GENERIC;
                        }

                        MCH_Lib.applyEntityHurtResistantTimeConfig(e);
                        e.attackEntityFrom(ds, damage);
                        if (e instanceof MCH_EntityAircraft) {
                            e.motionX = e.motionX + this.motionX * 0.05;
                            e.motionZ = e.motionZ + this.motionZ * 0.05;
                        } else if (e instanceof EntityArrow) {
                            e.setDead();
                        } else {
                            e.motionX = e.motionX + this.motionX * 1.5;
                            e.motionZ = e.motionZ + this.motionZ * 1.5;
                        }

                        if (this.getTankInfo().weightType != 2 && (e.width >= 1.0F || e.height >= 1.5)) {
                            if (e instanceof EntityLivingBase) {
                                ds = DamageSource.causeMobDamage((EntityLivingBase) e);
                            } else {
                                ds = DamageSource.GENERIC;
                            }

                            this.attackEntityFrom(ds, damage / 3.0F);
                        }

                        MCH_Lib.DbgLog(this.world, "MCH_EntityTank.collisionEntity damage=%.1f %s", damage,
                                e.toString());
                    }
                }
            }
        }
    }

    private boolean shouldCollisionDamage(Entity e) {
        if (this.getSeatIdByEntity(e) >= 0) {
            return false;
        } else if (this.noCollisionEntities.containsKey(e)) {
            return false;
        } else {
            if (e instanceof MCH_EntityHitBox && ((MCH_EntityHitBox) e).parent != null) {
                MCH_EntityAircraft ac = ((MCH_EntityHitBox) e).parent;
                if (this.noCollisionEntities.containsKey(ac)) {
                    return false;
                }
            }

            return (!(e.getRidingEntity() instanceof MCH_EntityAircraft) ||
                    !this.noCollisionEntities.containsKey(e.getRidingEntity())) &&
                    (!(e.getRidingEntity() instanceof MCH_EntitySeat) ||
                            ((MCH_EntitySeat) e.getRidingEntity()).getParent() == null ||
                            !this.noCollisionEntities.containsKey(((MCH_EntitySeat) e.getRidingEntity()).getParent()));
        }
    }

    public void updateCollisionBox() {
        if (this.getAcInfo() != null) {
            this.WheelMng.updateBlock();

            for (MCH_BoundingBox bb : this.extraBoundingBox) {
                if (this.rand.nextInt(3) == 0) {
                    if (MCH_Config.Collision_DestroyBlock.prmBool) {
                        Vec3d v = this.getTransformedPosition(bb.offsetX, bb.offsetY, bb.offsetZ);
                        this.destoryBlockRange(v, bb.width, bb.height);
                    }

                    this.collisionEntity(bb.getBoundingBox());
                }
            }

            if (MCH_Config.Collision_DestroyBlock.prmBool) {
                this.destoryBlockRange(this.getTransformedPosition(0.0, 0.0, 0.0), this.width * 1.5,
                        this.height * 2.0F);
            }

            this.collisionEntity(this.getCollisionBoundingBox());
        }
    }

    public void destoryBlockRange(Vec3d v, double w, double h) {
        if (this.getAcInfo() != null) {
            List<Block> destroyBlocks = MCH_Config.getBreakableBlockListFromType(this.getTankInfo().weightType);
            List<Block> noDestroyBlocks = MCH_Config.getNoBreakableBlockListFromType(this.getTankInfo().weightType);
            List<Material> destroyMaterials = MCH_Config
                    .getBreakableMaterialListFromType(this.getTankInfo().weightType);
            int ws = (int) (w + 2.0) / 2;
            int hs = (int) (h + 2.0) / 2;

            for (int x = -ws; x <= ws; x++) {
                for (int z = -ws; z <= ws; z++) {
                    for (int y = -hs; y <= hs + 1; y++) {
                        int bx = (int) (v.x + x - 0.5);
                        int by = (int) (v.y + y - 1.0);
                        int bz = (int) (v.z + z - 0.5);
                        BlockPos blockpos = new BlockPos(bx, by, bz);
                        IBlockState iblockstate = this.world.getBlockState(blockpos);
                        Block block = by >= 0 && by < 256 ? iblockstate.getBlock() : Blocks.AIR;
                        Material mat = iblockstate.getMaterial();
                        if (!Block.isEqualTo(block, Blocks.AIR)) {
                            for (Block c : noDestroyBlocks) {
                                if (Block.isEqualTo(block, c)) {
                                    block = null;
                                    break;
                                }
                            }

                            if (block == null) {
                                break;
                            }

                            for (Block cx : destroyBlocks) {
                                if (Block.isEqualTo(block, cx)) {
                                    this.destroyBlock(blockpos);
                                    mat = null;
                                    break;
                                }
                            }

                            if (mat == null) {
                                break;
                            }

                            for (Material m : destroyMaterials) {
                                if (iblockstate.getMaterial() == m) {
                                    this.destroyBlock(blockpos);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void destroyBlock(BlockPos blockpos) {
        if (this.rand.nextInt(8) == 0) {
            this.world.destroyBlock(blockpos, true);
        } else {
            this.world.setBlockToAir(blockpos);
        }
    }

    private void updateWheels() {
        this.WheelMng.move(this.motionX, this.motionY, this.motionZ);
    }

    public float getMaxSpeed() {
        return this.getTankInfo().speed;
    }

    @Override
    public void setAngles(Entity player, boolean fixRot, float fixYaw, float fixPitch, float deltaX, float deltaY,
                          float x, float y, float partialTicks) {
        if (partialTicks < 0.03F) {
            partialTicks = 0.4F;
        }

        if (partialTicks > 0.9F) {
            partialTicks = 0.6F;
        }

        this.lowPassPartialTicks.put(partialTicks);
        partialTicks = this.lowPassPartialTicks.getAvg();
        float ac_pitch = this.getRotPitch();
        float ac_yaw = this.getRotYaw();
        float ac_roll = this.getRotRoll();
        if (this.isFreeLookMode()) {
            y = 0.0F;
            x = 0.0F;
        }

        float yaw = 0.0F;
        float pitch = 0.0F;
        float roll = 0.0F;
        MCH_Math.FMatrix m_add = MCH_Math.newMatrix();
        MCH_Math.MatTurnZ(m_add, roll / 180.0F * (float) Math.PI);
        MCH_Math.MatTurnX(m_add, pitch / 180.0F * (float) Math.PI);
        MCH_Math.MatTurnY(m_add, yaw / 180.0F * (float) Math.PI);
        MCH_Math.MatTurnZ(m_add, (float) (this.getRotRoll() / 180.0F * Math.PI));
        MCH_Math.MatTurnX(m_add, (float) (this.getRotPitch() / 180.0F * Math.PI));
        MCH_Math.MatTurnY(m_add, (float) (this.getRotYaw() / 180.0F * Math.PI));
        MCH_Math.FVector3D v = MCH_Math.MatrixToEuler(m_add);
        v.x = MCH_Lib.RNG(v.x, -90.0F, 90.0F);
        v.z = MCH_Lib.RNG(v.z, -90.0F, 90.0F);
        if (v.z > 180.0F) {
            v.z -= 360.0F;
        }

        if (v.z < -180.0F) {
            v.z += 360.0F;
        }

        this.setRotYaw(v.y);
        this.setRotPitch(v.x);
        this.setRotRoll(v.z);
        this.onUpdateAngles(partialTicks);
        if (this.getAcInfo().limitRotation) {
            v.x = MCH_Lib.RNG(this.getRotPitch(), -90.0F, 90.0F);
            v.z = MCH_Lib.RNG(this.getRotRoll(), -90.0F, 90.0F);
            this.setRotPitch(v.x);
            this.setRotRoll(v.z);
        }

        if (MathHelper.abs(this.getRotPitch()) > 90.0F) {
            MCH_Lib.DbgLog(true, "MCH_EntityAircraft.setAngles Error:Pitch=%.1f", this.getRotPitch());
            this.setRotPitch(0.0F);
        }

        if (this.getRotRoll() > 180.0F) {
            this.setRotRoll(this.getRotRoll() - 360.0F);
        }

        if (this.getRotRoll() < -180.0F) {
            this.setRotRoll(this.getRotRoll() + 360.0F);
        }

        this.prevRotationRoll = this.getRotRoll();
        this.prevRotationPitch = this.getRotPitch();
        if (this.getRidingEntity() == null) {
            this.prevRotationYaw = this.getRotYaw();
        }

        float deltaLimit = this.getAcInfo().cameraRotationSpeed * partialTicks;
        MCH_WeaponSet ws = this.getCurrentWeapon(player);
        deltaLimit *= ws != null && ws.getInfo() != null ? ws.getInfo().cameraRotationSpeedPitch : 1.0F;
        if (deltaX > deltaLimit) {
            deltaX = deltaLimit;
        }

        if (deltaX < -deltaLimit) {
            deltaX = -deltaLimit;
        }

        if (deltaY > deltaLimit) {
            deltaY = deltaLimit;
        }

        if (deltaY < -deltaLimit) {
            deltaY = -deltaLimit;
        }

        if (!this.isOverridePlayerYaw() && !fixRot) {
            player.turn(deltaX, 0.0F);
        } else {
            if (this.getRidingEntity() == null) {
                player.prevRotationYaw = this.getRotYaw() + fixYaw;
            } else {
                if (this.getRotYaw() - player.rotationYaw > 180.0F) {
                    player.prevRotationYaw += 360.0F;
                }

                if (this.getRotYaw() - player.rotationYaw < -180.0F) {
                    player.prevRotationYaw -= 360.0F;
                }
            }

            player.rotationYaw = this.getRotYaw() + fixYaw;
        }

        if (!this.isOverridePlayerPitch() && !fixRot) {
            player.turn(0.0F, deltaY);
        } else {
            player.prevRotationPitch = this.getRotPitch() + fixPitch;
            player.rotationPitch = this.getRotPitch() + fixPitch;
        }

        float playerYaw = MathHelper.wrapDegrees(this.getRotYaw() - player.rotationYaw);
        float playerPitch = this.getRotPitch() * MathHelper.cos((float) (playerYaw * Math.PI / 180.0)) +
                -this.getRotRoll() * MathHelper.sin((float) (playerYaw * Math.PI / 180.0));
        if (MCH_MOD.proxy.isFirstPerson()) {
            player.rotationPitch = MCH_Lib.RNG(
                    player.rotationPitch, playerPitch + this.getAcInfo().minRotationPitch,
                    playerPitch + this.getAcInfo().maxRotationPitch);
            player.rotationPitch = MCH_Lib.RNG(player.rotationPitch, -90.0F, 90.0F);
        }

        player.prevRotationPitch = player.rotationPitch;
        if (this.getRidingEntity() == null && ac_yaw != this.getRotYaw() || ac_pitch != this.getRotPitch() ||
                ac_roll != this.getRotRoll()) {
            this.aircraftRotChanged = true;
        }
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

        if (!this.moveLeft && !this.moveRight && !this.throttleDown) {
            this.soundVolumeTarget *= 0.8F;
        } else {
            this.soundVolumeTarget += 0.1F;
            if (this.soundVolumeTarget > 0.75F) {
                this.soundVolumeTarget = 0.75F;
            }
        }

        if (target < this.soundVolumeTarget) {
            target = this.soundVolumeTarget;
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
        float target1 = (float) (0.5 + this.getCurrentThrottle() * 0.5);
        float target2 = (float) (0.5 + this.soundVolumeTarget * 0.5);
        return Math.max(target1, target2);
    }

    @Override
    public String getDefaultSoundName() {
        return "prop";
    }

    @Override
    public boolean hasBrake() {
        return true;
    }

    @Override
    public void updateParts(int stat) {
        super.updateParts(stat);
        if (!this.isDestroyed()) {
            MCH_Parts[] parts = new MCH_Parts[0];

            for (MCH_Parts p : parts) {
                if (p != null) {
                    p.updateStatusClient(stat);
                    p.update();
                }
            }
        }
    }

    @Override
    public float getUnfoldLandingGearThrottle() {
        return 0.7F;
    }

    public static class ClacAxisBB {

        public final double value;
        public final AxisAlignedBB bb;

        public ClacAxisBB(double value, AxisAlignedBB bb) {
            this.value = value;
            this.bb = bb;
        }
    }
}
