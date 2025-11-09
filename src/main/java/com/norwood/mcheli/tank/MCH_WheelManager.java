package com.norwood.mcheli.tank;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.MoverType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_Lib;

// 1.12.2

public class MCH_WheelManager {

    private static final Random rand = new Random();
    public final MCH_EntityAircraft parent;
    public MCH_EntityWheel[] wheels;
    public Vec3d weightedCenter;
    public float targetPitch;
    public float targetRoll;
    public float prevYaw;
    private double minZ;
    private double maxZ;
    private double avgZ;

    public MCH_WheelManager(MCH_EntityAircraft ac) {
        this.parent = ac;
        this.wheels = new MCH_EntityWheel[0];
        this.weightedCenter = Vec3d.ZERO;
    }

    public void createWheels(World w, List<MCH_AircraftInfo.Wheel> list, Vec3d weightedCenter) {
        this.wheels = new MCH_EntityWheel[list.size() * 2];
        this.minZ = 999999.0;
        this.maxZ = -999999.0;
        this.weightedCenter = weightedCenter;

        for (int i = 0; i < this.wheels.length; i++) {
            MCH_EntityWheel wheel = new MCH_EntityWheel(w);
            wheel.setParents(this.parent);
            Vec3d wp = list.get(i / 2).pos;
            wheel.setWheelPos(new Vec3d(i % 2 == 0 ? wp.x : -wp.x, wp.y, wp.z), this.weightedCenter);
            Vec3d v = this.parent.getTransformedPosition(wheel.pos.x, wheel.pos.y, wheel.pos.z);
            wheel.setLocationAndAngles(v.x, v.y + 1.0, v.z, 0.0F, 0.0F);
            this.wheels[i] = wheel;
            if (wheel.pos.z <= this.minZ) {
                this.minZ = wheel.pos.z;
            }

            if (wheel.pos.z >= this.maxZ) {
                this.maxZ = wheel.pos.z;
            }
        }

        this.avgZ = this.maxZ - this.minZ;
    }

    public void move(double x, double y, double z) {
        MCH_EntityAircraft ac = this.parent;
        if (ac.getAcInfo() == null) return;

        // Local tunables (feel free to tweak)
        final double WHEEL_Y_COMP = 0.35D;           // small lift to keep wheels out of collision fudge zone
        final double GROUND_HORZ_SPEED_EPS = 0.06D;  // treat below this as basically stationary
        final float MAX_DOWN_PITCH_STATIONARY = 3.5F;
        final float GROUND_HIGH_THROTTLE_BLEND = 0.12F;
        final float GROUND_IDLE_BLEND = 0.55F;
        final float AIR_BLEND = 0.9F;
        final float MAX_DELTA_PER_TICK = 2.5F;       // clamp pitch/roll per tick

        // 1) Update wheels' prev positions and compute motion target
        for (MCH_EntityWheel wheel : this.wheels) {
            wheel.prevPosX = wheel.posX;
            wheel.prevPosY = wheel.posY;
            wheel.prevPosZ = wheel.posZ;

            // get transformed position for this wheel and add Y compensation
            Vec3d tv = ac.getTransformedPosition(wheel.pos.x, wheel.pos.y, wheel.pos.z);
            double tvx = tv.x;
            double tvy = tv.y + WHEEL_Y_COMP; // lift to avoid tiny intersection with ground
            double tvz = tv.z;

            wheel.motionX = tvx - wheel.posX + x;
            wheel.motionY = tvy - wheel.posY;
            wheel.motionZ = tvz - wheel.posZ + z;
        }

        // 2) Move wheels with slight vertical damping
        for (MCH_EntityWheel wheel : this.wheels) {
            wheel.motionY *= 0.15;
            wheel.move(MoverType.SELF, wheel.motionX, wheel.motionY, wheel.motionZ);
            // small downward settle after moving
            double f = 1.0;
            wheel.move(MoverType.SELF, 0.0, -0.1 * f, 0.0);
        }

        // 3) Determine special on-ground flags (mirrors original logic)
        int zmog = -1;
        for (int i = 0; i < this.wheels.length / 2; i++) {
            zmog = i;
            MCH_EntityWheel w1 = this.wheels[i * 2];
            MCH_EntityWheel w2 = this.wheels[i * 2 + 1];
            if (!w1.isPlus && (w1.onGround || w2.onGround)) {
                zmog = -1;
                break;
            }
        }
        if (zmog >= 0) {
            this.wheels[zmog * 2].onGround = true;
            this.wheels[zmog * 2 + 1].onGround = true;
        }

        zmog = -1;
        for (int ix = this.wheels.length / 2 - 1; ix >= 0; ix--) {
            zmog = ix;
            MCH_EntityWheel w1 = this.wheels[ix * 2];
            MCH_EntityWheel w2 = this.wheels[ix * 2 + 1];
            if (w1.isPlus && (w1.onGround || w2.onGround)) {
                zmog = -1;
                break;
            }
        }
        if (zmog >= 0) {
            this.wheels[zmog * 2].onGround = true;
            this.wheels[zmog * 2 + 1].onGround = true;
        }

        // 4) Compute aggregated normal-like vector 'rv' from wheel pairs
        Vec3d rv = Vec3d.ZERO;
        Vec3d wc = ac.getTransformedPosition(this.weightedCenter);
        wc = new Vec3d(wc.x - ac.posX, this.weightedCenter.y + WHEEL_Y_COMP, wc.z - ac.posZ); // use compensated
                                                                                              // weighted center

        for (int ixx = 0; ixx < this.wheels.length / 2; ixx++) {
            MCH_EntityWheel w1 = this.wheels[ixx * 2];
            MCH_EntityWheel w2 = this.wheels[ixx * 2 + 1];

            Vec3d v1 = new Vec3d(w1.posX - (ac.posX + wc.x), w1.posY - (ac.posY + wc.y), w1.posZ - (ac.posZ + wc.z));
            Vec3d v2 = new Vec3d(w2.posX - (ac.posX + wc.x), w2.posY - (ac.posY + wc.y), w2.posZ - (ac.posZ + wc.z));

            Vec3d cross;
            if (w1.pos.z >= 0.0) {
                cross = v2.crossProduct(v1);
            } else {
                cross = v1.crossProduct(v2);
            }

            double clen = Math.sqrt(cross.x * cross.x + cross.y * cross.y + cross.z * cross.z);
            if (clen == 0.0) continue; // skip degenerate pair

            cross = cross.normalize();
            double f = Math.abs(w1.pos.z / this.avgZ);
            if (!w1.onGround && !w2.onGround) {
                f = 0.0;
            }

            rv = rv.add(cross.x * f, cross.y * f, cross.z * f);
        }

        // 5) If rv is zero-length, skip normal-based corrections
        double rvlen = Math.sqrt(rv.x * rv.x + rv.y * rv.y + rv.z * rv.z);
        if (rvlen > 0.0) {
            rv = rv.normalize();

            // small lateral nudge from wheel normals
            if (rv.y > 0.01 && rv.y < 0.7) {
                ac.motionX += rv.x / 50.0;
                ac.motionZ += rv.z / 50.0;
            }

            // rotate into world space relative to vehicle yaw
            rv = rv.rotateYaw((float) (ac.getRotYaw() * Math.PI / 180.0));

            // compute pitch/roll from rv (same formula as original)
            float pitch = (float) (90.0 - Math.atan2(rv.y, rv.z) * 180.0 / Math.PI);
            float roll = -((float) (90.0 - Math.atan2(rv.y, rv.x) * 180.0 / Math.PI));

            // Limit change per ac's configured on-ground factors (same as original)
            float ogpf = ac.getAcInfo().onGroundPitchFactor;
            if (pitch - ac.getRotPitch() > ogpf) pitch = ac.getRotPitch() + ogpf;
            if (pitch - ac.getRotPitch() < -ogpf) pitch = ac.getRotPitch() - ogpf;

            float ogrf = ac.getAcInfo().onGroundRollFactor;
            if (roll - ac.getRotRoll() > ogrf) roll = ac.getRotRoll() + ogrf;
            if (roll - ac.getRotRoll() < -ogrf) roll = ac.getRotRoll() - ogrf;

            this.targetPitch = pitch;
            this.targetRoll = roll;

            // DEFENSIVE: when essentially stationary on ground, avoid nose digging
            boolean groundLike = ac.onGround || MCH_Lib.getBlockIdY(ac, 1, -2) > 0;
            double horizSpeed = Math.sqrt(ac.motionX * ac.motionX + ac.motionZ * ac.motionZ);
            if (groundLike && horizSpeed < GROUND_HORZ_SPEED_EPS) {
                if (pitch - ac.getRotPitch() < -MAX_DOWN_PITCH_STATIONARY) {
                    pitch = ac.getRotPitch() - MAX_DOWN_PITCH_STATIONARY;
                }
                // damp the correction somewhat so it doesn't instantly change
                pitch = ac.getRotPitch() + (pitch - ac.getRotPitch()) * 0.45F;
            }

            // Blended application: stronger in air, much weaker on ground when throttling/moving
            float blend;
            double throttle = ac.getCurrentThrottle();
            if (!groundLike) {
                blend = AIR_BLEND;
            } else {
                if (throttle > 0.05D || horizSpeed > 0.08D) {
                    blend = GROUND_HIGH_THROTTLE_BLEND;
                } else {
                    blend = GROUND_IDLE_BLEND;
                }
            }

            float newPitch = ac.getRotPitch() + (pitch - ac.getRotPitch()) * blend;
            float newRoll = ac.getRotRoll() + (roll - ac.getRotRoll()) * blend;

            // clamp per-tick delta
            float dpitch = newPitch - ac.getRotPitch();
            float droll = newRoll - ac.getRotRoll();
            if (dpitch > MAX_DELTA_PER_TICK) dpitch = MAX_DELTA_PER_TICK;
            if (dpitch < -MAX_DELTA_PER_TICK) dpitch = -MAX_DELTA_PER_TICK;
            if (droll > MAX_DELTA_PER_TICK) droll = MAX_DELTA_PER_TICK;
            if (droll < -MAX_DELTA_PER_TICK) droll = -MAX_DELTA_PER_TICK;

            if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
                ac.setRotPitch(ac.getRotPitch() + dpitch);
                ac.setRotRoll(ac.getRotRoll() + droll);
            }
        }

        // 6) Reposition wheels to follow targetPitch/targetRoll (respecting movement limits)
        for (MCH_EntityWheel wheel : this.wheels) {
            Vec3d vx = this.getTransformedPosition(wheel.pos.x, wheel.pos.y, wheel.pos.z, ac, ac.getRotYaw(),
                    this.targetPitch, this.targetRoll);
            double rangeH = 2.0;
            double poy = wheel.stepHeight / 2.0F;
            if (wheel.posX > vx.x + rangeH) {
                wheel.posX = vx.x + rangeH;
                wheel.posY = vx.y + poy;
            }
            if (wheel.posX < vx.x - rangeH) {
                wheel.posX = vx.x - rangeH;
                wheel.posY = vx.y + poy;
            }
            if (wheel.posZ > vx.z + rangeH) {
                wheel.posZ = vx.z + rangeH;
                wheel.posY = vx.y + poy;
            }
            if (wheel.posZ < vx.z - rangeH) {
                wheel.posZ = vx.z - rangeH;
                wheel.posY = vx.y + poy;
            }
            wheel.setPositionAndRotation(wheel.posX, wheel.posY, wheel.posZ, 0.0F, 0.0F);
        }
    }

    public Vec3d getTransformedPosition(double x, double y, double z, MCH_EntityAircraft ac, float yaw, float pitch,
                                        float roll) {
        Vec3d v = MCH_Lib.RotVec3(x, y, z, -yaw, -pitch, -roll);
        return v.add(ac.posX, ac.posY, ac.posZ);
    }

    public void updateBlock() {
        if (MCH_Config.Collision_DestroyBlock.prmBool) {
            MCH_EntityAircraft ac = this.parent;

            for (MCH_EntityWheel w : this.wheels) {
                Vec3d v = ac.getTransformedPosition(w.pos);
                int x = (int) (v.x + 0.5);
                int y = (int) (v.y + 0.5);
                int z = (int) (v.z + 0.5);
                BlockPos blockpos = new BlockPos(x, y, z);
                IBlockState iblockstate = ac.world.getBlockState(blockpos);
                if (iblockstate.getBlock() == W_Block.getSnowLayer()) {
                    ac.world.setBlockToAir(blockpos);
                }

                if (iblockstate.getBlock() == Blocks.WATERLILY || iblockstate.getBlock() == Blocks.CAKE) {
                    BlockPos blockpos1 = new BlockPos(x, y, z);
                    ac.world.destroyBlock(blockpos1, false);
                }
            }
        }
    }

    public void particleLandingGear() {
        if (this.wheels.length > 0) {
            MCH_EntityAircraft ac = this.parent;
            double d = ac.motionX * ac.motionX + ac.motionZ * ac.motionZ + Math.abs(this.prevYaw - ac.getRotYaw());
            this.prevYaw = ac.getRotYaw();
            if (d > 0.001) {
                for (int i = 0; i < 2; i++) {
                    MCH_EntityWheel w = this.wheels[rand.nextInt(this.wheels.length)];
                    Vec3d v = ac.getTransformedPosition(w.pos);
                    int x = MathHelper.floor(v.x + 0.5);
                    int y = MathHelper.floor(v.y - 0.5);
                    int z = MathHelper.floor(v.z + 0.5);
                    BlockPos blockpos = new BlockPos(x, y, z);
                    IBlockState iblockstate = ac.world.getBlockState(blockpos);
                    if (Block.isEqualTo(iblockstate.getBlock(), Blocks.AIR)) {
                        y = MathHelper.floor(v.y + 0.5);
                        blockpos = new BlockPos(x, y, z);
                        iblockstate = ac.world.getBlockState(blockpos);
                    }

                    if (!Block.isEqualTo(iblockstate.getBlock(), Blocks.AIR)) {
                        MCH_ParticlesUtil.spawnParticleTileCrack(
                                ac.world,
                                x,
                                y,
                                z,
                                v.x + (rand.nextFloat() - 0.5),
                                v.y + 0.1,
                                v.z + (rand.nextFloat() - 0.5),
                                -ac.motionX * 4.0 + (rand.nextFloat() - 0.5) * 0.1,
                                rand.nextFloat() * 0.5,
                                -ac.motionZ * 4.0 + (rand.nextFloat() - 0.5) * 0.1);
                    }
                }
            }
        }
    }
}
