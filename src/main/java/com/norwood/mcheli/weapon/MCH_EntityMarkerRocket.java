package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import net.minecraft.init.Blocks;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class MCH_EntityMarkerRocket extends MCH_EntityBaseBullet {

    private static final DataParameter<Byte> MARKER_STATUS = EntityDataManager.createKey(MCH_EntityMarkerRocket.class,
            DataSerializers.BYTE);
    public int countDown;

    public MCH_EntityMarkerRocket(World par1World) {
        super(par1World);
        this.setMarkerStatus(0);
        this.countDown = 0;
    }

    public MCH_EntityMarkerRocket(
                                  World par1World, double posX, double posY, double posZ, double targetX,
                                  double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
        this.setMarkerStatus(0);
        this.countDown = 0;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(MARKER_STATUS, (byte) 0);
    }

    public int getMarkerStatus() {
        return this.dataManager.get(MARKER_STATUS);
    }

    public void setMarkerStatus(int n) {
        if (!this.world.isRemote) {
            this.dataManager.set(MARKER_STATUS, (byte) n);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return false;
    }

    @Override
    public void onUpdate() {
        int status = this.getMarkerStatus();
        if (this.world.isRemote) {
            if (this.getInfo() == null) {
                super.onUpdate();
            }

            if (this.getInfo() != null && !this.getInfo().disableSmoke && status != 0) {
                if (status == 1) {
                    super.onUpdate();
                    this.spawnParticle(this.getInfo().trajectoryParticleName, 3,
                            5.0F * this.getInfo().smokeSize * 0.5F);
                } else {
                    float gb = this.rand.nextFloat() * 0.3F;
                    this.spawnParticle(
                            "explode",
                            5,
                            10 + this.rand.nextInt(4),
                            this.rand.nextFloat() * 0.2F + 0.8F,
                            gb,
                            gb,
                            (this.rand.nextFloat() - 0.5F) * 0.7F,
                            0.3F + this.rand.nextFloat() * 0.3F,
                            (this.rand.nextFloat() - 0.5F) * 0.7F);
                }
            }
        } else if (status == 0 || this.isInWater()) {
            this.setDead();
        } else if (status == 1) {
            super.onUpdate();
        } else if (this.countDown > 0) {
            this.countDown--;
            if (this.countDown == 40) {
                int num = 6 + this.rand.nextInt(2);

                for (int i = 0; i < num; i++) {
                    MCH_EntityBomb e = new MCH_EntityBomb(
                            this.world,
                            this.posX + (this.rand.nextFloat() - 0.5F) * 15.0F,
                            260.0F + this.rand.nextFloat() * 10.0F + i * 30,
                            this.posZ + (this.rand.nextFloat() - 0.5F) * 15.0F,
                            0.0,
                            -0.5,
                            0.0,
                            0.0F,
                            90.0F,
                            4.0);
                    e.setName(this.getName());
                    e.explosionPower = 3 + this.rand.nextInt(2);
                    e.explosionPowerInWater = 0;
                    e.setPower(30);
                    e.piercing = 0;
                    e.shootingAircraft = this.shootingAircraft;
                    e.shootingEntity = this.shootingEntity;
                    this.world.spawnEntity(e);
                }
            }
        } else {
            this.setDead();
        }
    }

    public void spawnParticle(String name, int num, float size, float r, float g, float b, float mx, float my,
                              float mz) {
        if (this.world.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (this.posX - this.prevPosX) / num;
            double y = (this.posY - this.prevPosY) / num;
            double z = (this.posZ - this.prevPosZ) / num;

            for (int i = 0; i < num; i++) {
                MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", this.prevPosX + x * i,
                        this.prevPosY + y * i, this.prevPosZ + z * i);
                prm.motionX = mx;
                prm.motionY = my;
                prm.motionZ = mz;
                prm.size = size + this.rand.nextFloat();
                prm.setColor(1.0F, r, g, b);
                prm.isEffectWind = true;
                MCH_ParticlesUtil.spawnParticle(prm);
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult m, float damageFactor) {
        if (!this.world.isRemote) {
            if (m.entityHit == null && !W_MovingObjectPosition.isHitTypeEntity(m)) {
                BlockPos blockpos = m.getBlockPos();
                blockpos = blockpos.offset(m.sideHit);
                if (this.world.isAirBlock(blockpos)) {
                    if (MCH_Config.Explosion_FlamingBlock.prmBool) {
                        this.world.setBlockState(blockpos, Blocks.FIRE.getDefaultState());
                    }

                    int noAirBlockCount = 0;

                    for (int i = 1; i < 256; i++) {
                        if (!this.world.isAirBlock(blockpos.up(i))) {
                            if (++noAirBlockCount >= 5) {
                                break;
                            }
                        }
                    }

                    if (noAirBlockCount < 5) {
                        this.setMarkerStatus(2);
                        this.setPosition(blockpos.getX() + 0.5, blockpos.getY() + 1.1, blockpos.getZ() + 0.5);
                        this.prevPosX = this.posX;
                        this.prevPosY = this.posY;
                        this.prevPosZ = this.posZ;
                        this.countDown = 100;
                    } else {
                        this.setDead();
                    }
                } else {
                    this.setDead();
                }
            }
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Rocket;
    }
}
