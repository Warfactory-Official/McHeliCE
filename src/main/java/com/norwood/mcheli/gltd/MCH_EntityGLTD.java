package com.norwood.mcheli.gltd;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Camera;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.multiplay.MCH_Multiplay;
import com.norwood.mcheli.weapon.MCH_WeaponCAS;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfoManager;
import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_EntityGLTD extends W_Entity implements IEntitySinglePassenger {

    public static final float Y_OFFSET = 0.25F;
    private static final DataParameter<Integer> TIME_SINCE_HIT = EntityDataManager.createKey(MCH_EntityGLTD.class,
            DataSerializers.VARINT);
    private static final DataParameter<Integer> FORWARD_DIR = EntityDataManager.createKey(MCH_EntityGLTD.class,
            DataSerializers.VARINT);
    private static final DataParameter<Integer> DAMAGE_TAKEN = EntityDataManager.createKey(MCH_EntityGLTD.class,
            DataSerializers.VARINT);
    public final MCH_Camera camera;
    public final MCH_WeaponCAS weaponCAS;
    public boolean zoomDir;
    public int countWait;
    public boolean isUsedPlayer;
    public float renderRotaionYaw;
    public float renderRotaionPitch;
    public int retryRiddenByEntityCheck;
    public Entity lastRiddenByEntity;
    private boolean isBoatEmpty = true;
    private double speedMultiplier = 0.07;
    private int gltdPosRotInc;
    private double gltdX;
    private double gltdY;
    private double gltdZ;
    private double gltdYaw;
    private double gltdPitch;
    @SideOnly(Side.CLIENT)
    private double velocityX;
    @SideOnly(Side.CLIENT)
    private double velocityY;
    @SideOnly(Side.CLIENT)
    private double velocityZ;

    public MCH_EntityGLTD(World world) {
        super(world);
        this.preventEntitySpawning = true;
        this.setSize(0.5F, 0.5F);
        this.camera = new MCH_Camera(world, this);
        MCH_WeaponInfo wi = MCH_WeaponInfoManager.get("a10gau8");
        this.weaponCAS = new MCH_WeaponCAS(world, Vec3d.ZERO, 0.0F, 0.0F, "a10gau8", wi);
        this.weaponCAS.interval = this.weaponCAS.interval + (this.weaponCAS.interval > 0 ? 150 : 'ｪ');
        this.weaponCAS.displayName = "A-10 GAU-8 Avenger";
        this.ignoreFrustumCheck = true;
        this.countWait = 0;
        this.retryRiddenByEntityCheck = 0;
        this.lastRiddenByEntity = null;
        this.isUsedPlayer = false;
        this.renderRotaionYaw = 0.0F;
        this.renderRotaionYaw = 0.0F;
        this.renderRotaionPitch = 0.0F;
        this.zoomDir = true;
        this._renderDistanceWeight = 2.0;
    }

    public MCH_EntityGLTD(World par1World, double x, double y, double z) {
        this(par1World);
        this.setPosition(x, y, z);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
        this.camera.setPosition(x, y, z);
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TIME_SINCE_HIT, 0);
        this.dataManager.register(FORWARD_DIR, 1);
        this.dataManager.register(DAMAGE_TAKEN, 0);
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return par1Entity.getEntityBoundingBox();
    }

    public AxisAlignedBB getCollisionBoundingBox() {
        return this.getEntityBoundingBox();
    }

    public double getMountedYOffset() {
        return this.height * 0.0 - 0.3;
    }

    @Override
    public boolean attackEntityFrom(DamageSource ds, float damage) {
        if (this.isEntityInvulnerable(ds)) {
            return false;
        } else if (!this.world.isRemote && !this.isDead) {
            damage = MCH_Config.applyDamageByExternal(this, ds, damage);
            if (!MCH_Multiplay.canAttackEntity(ds, this)) {
                return false;
            } else {
                this.setForwardDirection(-this.getForwardDirection());
                this.setTimeSinceHit(10);
                this.setDamageTaken((int) (this.getDamageTaken() + damage * 100.0F));
                this.markVelocityChanged();
                boolean flag = ds.getTrueSource() instanceof EntityPlayer &&
                        ((EntityPlayer) ds.getTrueSource()).capabilities.isCreativeMode;
                if (flag || this.getDamageTaken() > 40.0F) {
                    Entity riddenByEntity = this.getRiddenByEntity();
                    this.camera.initCamera(0, riddenByEntity);
                    if (riddenByEntity != null) {
                        riddenByEntity.startRiding(this);
                    }

                    if (!flag) {
                        this.dropItemWithOffset(MCH_MOD.itemGLTD, 1, 0.0F);
                    }

                    W_WorldFunc.MOD_playSoundEffect(this.world, this.posX, this.posY, this.posZ, "hit", 1.0F, 1.0F);
                    this.setDead();
                }

                return true;
            }
        } else {
            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    public void performHurtAnimation() {}

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
        if (this.isBoatEmpty) {
            this.gltdPosRotInc = par9 + 5;
        } else {
            double x = par1 - this.posX;
            double y = par3 - this.posY;
            double z = par5 - this.posZ;
            if (x * x + y * y + z * z <= 1.0) {
                return;
            }

            this.gltdPosRotInc = 3;
        }

        this.gltdX = par1;
        this.gltdY = par3;
        this.gltdZ = par5;
        this.gltdYaw = par7;
        this.gltdPitch = par8;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double x, double y, double z) {
        this.velocityX = this.motionX = x;
        this.velocityY = this.motionY = y;
        this.velocityZ = this.motionZ = z;
    }

    public void onUpdate() {
        super.onUpdate();
        if (this.getTimeSinceHit() > 0) {
            this.setTimeSinceHit(this.getTimeSinceHit() - 1);
        }

        if (this.getDamageTaken() > 0.0F) {
            this.setDamageTaken(this.getDamageTaken() - 1);
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        double d3 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            this.camera.updateViewer(0, riddenByEntity);
        }

        if (!this.world.isRemote || !this.isBoatEmpty) {
            this.motionY -= 0.04;
            double d4 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            if (d4 > 0.35) {
                double d5 = 0.35 / d4;
                this.motionX *= d5;
                this.motionZ *= d5;
                d4 = 0.35;
            }

            if (d4 > d3 && this.speedMultiplier < 0.35) {
                this.speedMultiplier = this.speedMultiplier + (0.35 - this.speedMultiplier) / 35.0;
                if (this.speedMultiplier > 0.35) {
                    this.speedMultiplier = 0.35;
                }
            } else {
                this.speedMultiplier = this.speedMultiplier - (this.speedMultiplier - 0.07) / 35.0;
                if (this.speedMultiplier < 0.07) {
                    this.speedMultiplier = 0.07;
                }
            }

            if (this.onGround) {
                this.motionX *= 0.5;
                this.motionY *= 0.5;
                this.motionZ *= 0.5;
            }

            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.99;
            this.motionY *= 0.95;
            this.motionZ *= 0.99;
            this.rotationPitch = 0.0F;
            double d5 = this.rotationYaw;
            double d11 = this.prevPosX - this.posX;
            double d10 = this.prevPosZ - this.posZ;
            if (d11 * d11 + d10 * d10 > 0.001) {
                d5 = (float) (Math.atan2(d10, d11) * 180.0 / Math.PI);
            }

            double d12 = MathHelper.wrapDegrees(d5 - this.rotationYaw);
            if (d12 > 20.0) {
                d12 = 20.0;
            }

            if (d12 < -20.0) {
                d12 = -20.0;
            }

            this.rotationYaw = (float) (this.rotationYaw + d12);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            if (!this.world.isRemote) {
                if (MCH_Config.Collision_DestroyBlock.prmBool) {
                    for (int l = 0; l < 4; l++) {
                        int i1 = MathHelper.floor(this.posX + (l % 2 - 0.5) * 0.8);
                        int j1 = MathHelper.floor(this.posZ + ((double) l / 2D - 0.5) * 0.8);

                        for (int k1 = 0; k1 < 2; k1++) {
                            int l1 = MathHelper.floor(this.posY) + k1;
                            if (W_WorldFunc.isEqualBlock(this.world, i1, l1, j1, W_Block.getSnowLayer())) {
                                this.world.setBlockToAir(new BlockPos(i1, l1, j1));
                            }
                        }
                    }
                }

                riddenByEntity = this.getRiddenByEntity();
                if (riddenByEntity != null && riddenByEntity.isDead) {
                    riddenByEntity.dismountRidingEntity();
                }
            }
        } else if (this.gltdPosRotInc > 0) {
            double d4x = this.posX + (this.gltdX - this.posX) / this.gltdPosRotInc;
            double d5x = this.posY + (this.gltdY - this.posY) / this.gltdPosRotInc;
            double d11x = this.posZ + (this.gltdZ - this.posZ) / this.gltdPosRotInc;
            double d10x = MathHelper.wrapDegrees(this.gltdYaw - this.rotationYaw);
            this.rotationYaw = (float) (this.rotationYaw + d10x / this.gltdPosRotInc);
            this.rotationPitch = (float) (this.rotationPitch +
                    (this.gltdPitch - this.rotationPitch) / this.gltdPosRotInc);
            this.gltdPosRotInc--;
            this.setPosition(d4x, d5x, d11x);
            this.setRotation(this.rotationYaw, this.rotationPitch);
        } else {
            double d4x = this.posX + this.motionX;
            double d5x = this.posY + this.motionY;
            double d11x = this.posZ + this.motionZ;
            this.setPosition(d4x, d5x, d11x);
            if (this.onGround) {
                this.motionX *= 0.5;
                this.motionY *= 0.5;
                this.motionZ *= 0.5;
            }

            this.motionX *= 0.99;
            this.motionY *= 0.95;
            this.motionZ *= 0.99;
        }

        this.updateCameraPosition(false);
        if (this.countWait > 0) {
            this.countWait--;
        }

        if (this.countWait < 0) {
            this.countWait++;
        }

        this.weaponCAS.update(this.countWait);
        riddenByEntity = this.getRiddenByEntity();
        if (this.lastRiddenByEntity == null || riddenByEntity != null) {
            this.retryRiddenByEntityCheck = 0;
        } else if (this.retryRiddenByEntityCheck < 3) {
            this.retryRiddenByEntityCheck++;
            this.setUnmoundPosition(this.lastRiddenByEntity);
        } else {
            this.unmountEntity();
        }

        riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            this.lastRiddenByEntity = riddenByEntity;
        }
    }

    public void setUnmoundPosition(Entity e) {
        if (e != null) {
            float yaw = this.rotationYaw;
            double d0 = Math.sin(yaw * Math.PI / 180.0) * 1.2;
            double d1 = -Math.cos(yaw * Math.PI / 180.0) * 1.2;
            e.setPosition(this.posX + d0, this.posY + this.getMountedYOffset() + e.getYOffset() + 1.0, this.posZ + d1);
            e.lastTickPosX = e.prevPosX = e.posX;
            e.lastTickPosY = e.prevPosY = e.posY;
            e.lastTickPosZ = e.prevPosZ = e.posZ;
        }
    }

    public void unmountEntity() {
        this.camera.setMode(0, 0);
        this.camera.setCameraZoom(1.0F);
        if (!this.world.isRemote) {
            Entity riddenByEntity = this.getRiddenByEntity();
            if (riddenByEntity != null) {
                if (!riddenByEntity.isDead) {
                    riddenByEntity.dismountRidingEntity();
                }
            } else if (this.lastRiddenByEntity != null && !this.lastRiddenByEntity.isDead) {
                this.camera.updateViewer(0, this.lastRiddenByEntity);
                this.setUnmoundPosition(this.lastRiddenByEntity);
            }
        }

        this.lastRiddenByEntity = null;
    }

    public void updateCameraPosition(boolean foreceUpdate) {
        Entity riddenByEntity = this.getRiddenByEntity();
        if (foreceUpdate || riddenByEntity != null && this.camera != null) {
            double x = -Math.sin(this.rotationYaw * Math.PI / 180.0) * 0.6;
            double z = Math.cos(this.rotationYaw * Math.PI / 180.0) * 0.6;
            this.camera.setPosition(this.posX + x, this.posY + 0.7, this.posZ + z);
        }
    }

    @SideOnly(Side.CLIENT)
    public void zoomCamera(float f) {
        float z = this.camera.getCameraZoom();
        z += f;
        if (z < 1.0F) {
            z = 1.0F;
        }

        if (z > 10.0F) {
            z = 10.0F;
        }

        this.camera.setCameraZoom(z);
    }

    public void updatePassenger(@NotNull Entity passenger) {
        if (this.isPassenger(passenger)) {
            double x = Math.sin(this.rotationYaw * Math.PI / 180.0) * 0.5;
            double z = -Math.cos(this.rotationYaw * Math.PI / 180.0) * 0.5;
            passenger.setPosition(this.posX + x, this.posY + this.getMountedYOffset() + passenger.getYOffset(),
                    this.posZ + z);
        }
    }

    public void switchWeapon(int id) {}

    public boolean useCurrentWeapon(int option1, int option2) {
        Entity riddenByEntity = this.getRiddenByEntity();
        if (this.countWait == 0 && riddenByEntity != null && this.weaponCAS.shot(riddenByEntity, this.camera.posX,
                this.camera.posY, this.camera.posZ, option1, option2)) {
            this.countWait = this.weaponCAS.interval;
            if (this.world.isRemote) {
                this.countWait = this.countWait + (this.countWait > 0 ? 10 : -10);
            } else {
                W_WorldFunc.MOD_playSoundEffect(this.world, this.posX, this.posY, this.posZ, "gltd", 0.5F, 1.0F);
            }

            return true;
        } else {
            return false;
        }
    }

    protected void writeEntityToNBT(@NotNull NBTTagCompound par1NBTTagCompound) {}

    protected void readEntityFromNBT(@NotNull NBTTagCompound par1NBTTagCompound) {}

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        Entity riddenByEntity = this.getRiddenByEntity();
        if (!(riddenByEntity instanceof EntityPlayer) || riddenByEntity == player) {
            player.rotationYaw = MathHelper.wrapDegrees(this.rotationYaw);
            player.rotationPitch = MathHelper.wrapDegrees(this.rotationPitch);
            if (!this.world.isRemote) {
                player.startRiding(this);
            } else {
                this.zoomDir = true;
                this.camera.setCameraZoom(1.0F);
                if (this.countWait > 0) {
                    this.countWait = -this.countWait;
                }

                if (this.countWait > -60) {
                    this.countWait = -60;
                }
            }

            this.updateCameraPosition(true);
        }
        return true;
    }

    public int getDamageTaken() {
        return this.dataManager.get(DAMAGE_TAKEN);
    }

    public void setDamageTaken(int par1) {
        this.dataManager.set(DAMAGE_TAKEN, par1);
    }

    public int getTimeSinceHit() {
        return this.dataManager.get(TIME_SINCE_HIT);
    }

    public void setTimeSinceHit(int par1) {
        this.dataManager.set(TIME_SINCE_HIT, par1);
    }

    public int getForwardDirection() {
        return 0;
    }

    public void setForwardDirection(int par1) {
        this.dataManager.set(FORWARD_DIR, par1);
    }

    @SideOnly(Side.CLIENT)
    public void setIsBoatEmpty(boolean par1) {
        this.isBoatEmpty = par1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        double size = 150;
        return new AxisAlignedBB(
                this.posX - size, this.posY - size, this.posZ - size,
                this.posX + size, this.posY + size, this.posZ + size);
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        List<Entity> passengers = this.getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
