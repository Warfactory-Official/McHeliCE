package com.norwood.mcheli.container;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_IEntityCanRideAircraft;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.multiplay.MCH_Multiplay;
import com.norwood.mcheli.wrapper.*;

public class MCH_EntityContainer extends W_EntityContainer implements MCH_IEntityCanRideAircraft {

    public static final float Y_OFFSET = 0.5F;
    private static final DataParameter<Integer> TIME_SINCE_HIT = EntityDataManager.createKey(MCH_EntityContainer.class,
            DataSerializers.VARINT);
    private static final DataParameter<Integer> FORWARD_DIR = EntityDataManager.createKey(MCH_EntityContainer.class,
            DataSerializers.VARINT);
    private static final DataParameter<Integer> DAMAGE_TAKEN = EntityDataManager.createKey(MCH_EntityContainer.class,
            DataSerializers.VARINT);
    private double speedMultiplier = 0.07;
    private int boatPosRotationIncrements;
    private double boatX;
    private double boatY;
    private double boatZ;
    private double boatYaw;
    private double boatPitch;
    @SideOnly(Side.CLIENT)
    private double velocityX;
    @SideOnly(Side.CLIENT)
    private double velocityY;
    @SideOnly(Side.CLIENT)
    private double velocityZ;

    public MCH_EntityContainer(World par1World) {
        super(par1World);
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 1.0F);
        this.stepHeight = 0.6F;
        this.isImmuneToFire = true;
        this._renderDistanceWeight = 2.0;
    }

    public MCH_EntityContainer(World par1World, double par2, double par4, double par6) {
        this(par1World);
        this.setPosition(par2, par4 + 0.5, par6);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.prevPosX = par2;
        this.prevPosY = par4;
        this.prevPosZ = par6;
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

    public boolean canBePushed() {
        return true;
    }

    @Override
    public int getSizeInventory() {
        return 54;
    }

    @Override
    public String getInvName() {
        return "Container " + super.getInvName();
    }

    public double getMountedYOffset() {
        return -0.3;
    }

    @Override
    public boolean attackEntityFrom(DamageSource ds, float damage) {
        if (this.isEntityInvulnerable(ds)) {
            return false;
        } else if (!this.world.isRemote && !this.isDead) {
            damage = MCH_Config.applyDamageByExternal(this, ds, damage);
            if (!MCH_Multiplay.canAttackEntity(ds, this)) {
                return false;
            } else if (ds.getTrueSource() instanceof EntityPlayer && ds.getDamageType().equalsIgnoreCase("player")) {
                MCH_Lib.DbgLog(this.world, "MCH_EntityContainer.attackEntityFrom:damage=%.1f:%s", damage,
                        ds.getDamageType());
                W_WorldFunc.MOD_playSoundAtEntity(this, "hit", 1.0F, 1.3F);
                this.setDamageTaken(this.getDamageTaken() + (int) (damage * 20.0F));
                this.setForwardDirection(-this.getForwardDirection());
                this.setTimeSinceHit(10);
                this.markVelocityChanged();
                boolean flag = ds.getTrueSource() instanceof EntityPlayer &&
                        ((EntityPlayer) ds.getTrueSource()).capabilities.isCreativeMode;
                if (flag || this.getDamageTaken() > 40.0F) {
                    if (!flag) {
                        this.dropItemWithOffset(MCH_MOD.itemContainer, 1, 0.0F);
                    }

                    this.setDead();
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public void performHurtAnimation() {
        this.setForwardDirection(-this.getForwardDirection());
        this.setTimeSinceHit(10);
        this.setDamageTaken(this.getDamageTaken() * 11);
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
        this.boatPosRotationIncrements = par9 + 10;
        this.boatX = par1;
        this.boatY = par3;
        this.boatZ = par5;
        this.boatYaw = par7;
        this.boatPitch = par8;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double par1, double par3, double par5) {
        this.velocityX = this.motionX = par1;
        this.velocityY = this.motionY = par3;
        this.velocityZ = this.motionZ = par5;
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
        byte b0 = 5;
        double d0 = 0.0;

        for (int i = 0; i < b0; i++) {
            AxisAlignedBB boundingBox = this.getEntityBoundingBox();
            double d1 = boundingBox.minY + (boundingBox.maxY - boundingBox.minY) * (i) / b0 - 0.125;
            double d2 = boundingBox.minY + (boundingBox.maxY - boundingBox.minY) * (i + 1) / b0 - 0.125;
            AxisAlignedBB axisalignedbb = W_AxisAlignedBB.getAABB(boundingBox.minX, d1, boundingBox.minZ,
                    boundingBox.maxX, d2, boundingBox.maxZ);
            if (this.world.isMaterialInBB(axisalignedbb, Material.WATER)) {
                d0 += 1.0 / b0;
            } else if (this.world.isMaterialInBB(axisalignedbb, Material.LAVA)) {
                d0 += 1.0 / b0;
            }
        }

        double d3 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        if (d3 > 0.2625) {}

        if (this.world.isRemote) {
            if (this.boatPosRotationIncrements > 0) {
                double d4 = this.posX + (this.boatX - this.posX) / this.boatPosRotationIncrements;
                double d5 = this.posY + (this.boatY - this.posY) / this.boatPosRotationIncrements;
                double d11 = this.posZ + (this.boatZ - this.posZ) / this.boatPosRotationIncrements;
                double d10 = MathHelper.wrapDegrees(this.boatYaw - this.rotationYaw);
                this.rotationYaw = (float) (this.rotationYaw + d10 / this.boatPosRotationIncrements);
                this.rotationPitch = (float) (this.rotationPitch +
                        (this.boatPitch - this.rotationPitch) / this.boatPosRotationIncrements);
                this.boatPosRotationIncrements--;
                this.setPosition(d4, d5, d11);
                this.setRotation(this.rotationYaw, this.rotationPitch);
            } else {
                double d4 = this.posX + this.motionX;
                double d5 = this.posY + this.motionY;
                double d11 = this.posZ + this.motionZ;
                this.setPosition(d4, d5, d11);
                if (this.onGround) {
                    this.motionX *= 0.9F;
                    this.motionZ *= 0.9F;
                }

                this.motionX *= 0.99;
                this.motionY *= 0.95;
                this.motionZ *= 0.99;
            }
        } else {
            if (d0 < 1.0) {
                double d4 = d0 * 2.0 - 1.0;
                this.motionY += 0.04 * d4;
            } else {
                if (this.motionY < 0.0) {
                    this.motionY /= 2.0;
                }

                this.motionY += 0.007;
            }

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
                this.motionX *= 0.9F;
                this.motionZ *= 0.9F;
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
            if (d12 > 5.0) {
                d12 = 5.0;
            }

            if (d12 < -5.0) {
                d12 = -5.0;
            }

            this.rotationYaw = (float) (this.rotationYaw + d12);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            if (!this.world.isRemote) {
                List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                        this.getEntityBoundingBox().grow(0.2, 0.0, 0.2));
                if (!list.isEmpty()) {
                    for (Entity entity : list) {
                        if (entity.canBePushed() && entity instanceof MCH_EntityContainer) {
                            entity.applyEntityCollision(this);
                        }
                    }
                }

                if (MCH_Config.Collision_DestroyBlock.prmBool) {
                    for (int lx = 0; lx < 4; lx++) {
                        int i1 = MathHelper.floor(this.posX + (lx % 2 - 0.5) * 0.8);
                        int j1 = MathHelper.floor(this.posZ + ((double) lx / 2D - 0.5) * 0.8);

                        for (int k1 = 0; k1 < 2; k1++) {
                            int l1 = MathHelper.floor(this.posY) + k1;
                            if (W_WorldFunc.isEqualBlock(this.world, i1, l1, j1, W_Block.getSnowLayer())) {
                                this.world.setBlockToAir(new BlockPos(i1, l1, j1));
                            } else if (W_WorldFunc.isEqualBlock(this.world, i1, l1, j1, Blocks.WATERLILY)) {
                                BlockPos blockpos = new BlockPos(i1, l1, j1);
                                this.world.destroyBlock(blockpos, true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        super.writeEntityToNBT(par1NBTTagCompound);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        super.readEntityFromNBT(par1NBTTagCompound);
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 2.0F;
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        this.displayInventory(player);

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
        return this.dataManager.get(FORWARD_DIR);
    }

    public void setForwardDirection(int par1) {
        this.dataManager.set(FORWARD_DIR, par1);
    }

    @Override
    public boolean canRideAircraft(MCH_EntityAircraft ac, int seatID, MCH_SeatRackInfo info) {
        for (String s : info.names) {
            if (s.equalsIgnoreCase("container")) {
                return ac.getRidingEntity() == null && this.getRidingEntity() == null;
            }
        }

        return false;
    }

    @Override
    public boolean isSkipNormalRender() {
        return this.getRidingEntity() instanceof MCH_EntitySeat;
    }
}
