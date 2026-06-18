package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketClientSound;
import com.norwood.mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MCH_EntityBomb extends MCH_EntityBaseBullet {

    private static final DataParameter<Byte> MINE_FLAGS =
            EntityDataManager.createKey(MCH_EntityBomb.class, DataSerializers.BYTE);
    private static final byte LANDED_BIT = 1;
    private static final double MINE_SNEAK_TRIGGER_DIST_SQ = 1.0D;

    private boolean mineArmed;
    private int mineArmTimer;
    private int mineTick;

    public MCH_EntityBomb(World par1World) {
        super(par1World);
    }

    public MCH_EntityBomb(
                          World par1World, double posX, double posY, double posZ, double targetX, double targetY,
                          double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(MINE_FLAGS, (byte) 0);
    }

    @Override
    public void onUpdate() {
        MCH_WeaponInfo info = this.getInfo();

        if (info != null && info.isMine()) {
            if (this.isMineLanded()) {
                this.onUpdateLandedMine(info);
            } else {
                super.onUpdate();
            }
            return;
        }

        super.onUpdate();
        if (!this.world.isRemote && info != null) {
            this.motionX *= 0.999;
            this.motionZ *= 0.999;
            if (this.isInWater()) {
                this.motionX = this.motionX * info.velocityInWater;
                this.motionY = this.motionY * info.velocityInWater;
                this.motionZ = this.motionZ * info.velocityInWater;
            }

            float dist = info.proximityFuseDist;
            if (dist > 0.1F && this.getCountOnUpdate() % 10 == 0) {
                List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                        this.getEntityBoundingBox().grow(dist, dist, dist));
                for (Entity entity : list) {
                    if (W_Lib.isEntityLivingBase(entity) && this.canBeCollidedEntity(entity)) {
                        RayTraceResult m = new RayTraceResult(
                                new Vec3d(this.posX, this.posY, this.posZ), EnumFacing.DOWN,
                                new BlockPos(this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5));
                        this.onImpact(m, 1.0F);
                        break;
                    }
                }
            }
        }

        this.onUpdateBomblet();
    }


    private boolean isMineLanded() {
        return (this.dataManager.get(MINE_FLAGS) & LANDED_BIT) != 0;
    }

    private void setMineFlag(byte bit, boolean on) {
        byte cur = this.dataManager.get(MINE_FLAGS);
        byte next = (byte) (on ? (cur | bit) : (cur & ~bit));
        if (next != cur) {
            this.dataManager.set(MINE_FLAGS, next);
        }
    }

    private void landMine(RayTraceResult m) {
        if (m != null && m.hitVec != null) {
            this.setPosition(m.hitVec.x, m.hitVec.y, m.hitVec.z);
        }
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.mineArmed = false;
        this.mineArmTimer = 0;
        this.mineTick = 0;
        this.setMineFlag(LANDED_BIT, true);
    }

    private void onUpdateLandedMine(MCH_WeaponInfo info) {
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        if (this.world.isRemote) {
            return;
        }

        if (!this.mineArmed) {
            if (++this.mineArmTimer >= info.mineArmDelay) {
                this.mineArmed = true;
            }
            return;
        }

        if (++this.mineTick % 5 == 0) {
            this.scanAndTrigger(info);
        }
    }

    private void scanAndTrigger(MCH_WeaponInfo info) {
        double range = info.mineRange;
        if (range <= 0.0D) {
            return;
        }
        AxisAlignedBB box = this.getEntityBoundingBox().grow(range);
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, box);
        for (Entity e : list) {
            if (!this.triggersMine(e, info)) {
                continue;
            }
            double cx = e.posX - this.posX;
            double cy = (e.posY + e.height * 0.5D) - (this.posY + this.height * 0.5D);
            double cz = e.posZ - this.posZ;
            double distSq = cx * cx + cy * cy + cz * cz;
            if (distSq > range * range) {
                continue;
            }
            if (info.mineFuze == MCH_MineFuze.PROXIMITY_PLAYER
                    && e instanceof EntityPlayer p && p.isSneaking()
                    && distSq > MINE_SNEAK_TRIGGER_DIST_SQ) {
                continue;
            }
            this.detonateMine(info);
            return;
        }
    }

    private boolean triggersMine(Entity e, MCH_WeaponInfo info) {
        if (e == null || e.isDead) {
            return false;
        }
        return switch (info.mineFuze) {
            case PROXIMITY_PLAYER -> e instanceof EntityPlayer p && !p.isSpectator()
                    && !p.capabilities.isCreativeMode;
            case PROXIMITY_VEHICLE -> e instanceof MCH_EntityAircraft;
            default -> false;
        };
    }

    private void detonateMine(MCH_WeaponInfo info) {
        if (this.world.isRemote) {
            return;
        }
        if (this.isInWater() && this.explosionPowerInWater > 0) {
            this.newExplosion(this.posX, this.posY, this.posZ, this.explosionPowerInWater,
                    this.explosionPowerInWater, true);
        } else if (this.explosionPower > 0) {
            this.newExplosion(this.posX, this.posY, this.posZ, this.explosionPower, info.explosionBlock, false);
        } else {
            this.playExplosionSound();
        }
        PacketClientSound.sendSoundPacket(this.posX, this.posY, this.posZ, info.hitSoundRange, this.world,
                info.hitSound != null ? info.hitSound.toString() : null, true);
        this.setDead();
    }

    @Override
    protected void onImpact(RayTraceResult m, float damageFactor) {
        MCH_WeaponInfo info = this.getInfo();
        if (!this.world.isRemote && info != null && info.isMine() && !this.isMineLanded()) {
            if (m != null && m.typeOfHit == RayTraceResult.Type.BLOCK) {
                this.landMine(m);
            }
            return;
        }
        super.onImpact(m, damageFactor);
    }

    @Override
    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        MCH_WeaponInfo info = this.getInfo();
        if (info != null && info.isMine()) {
            ItemStack held = player.getHeldItem(hand);
            if (!held.isEmpty() && held.getItem() instanceof MCH_ItemMineDefuser) {
                if (!this.world.isRemote) {
                    this.world.playSound(null, this.posX, this.posY, this.posZ,
                            SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6F, 1.6F);
                    this.setDead();
                }
                player.swingArm(hand);
                return true;
            }
        }
        return super.processInitialInteract(player, hand);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        MCH_WeaponInfo info = this.getInfo();
        if (info != null && info.isMine()) {
            nbt.setBoolean("MchMine", true);
            nbt.setBoolean("MineLanded", this.isMineLanded());
            nbt.setBoolean("MineArmed", this.mineArmed);
            nbt.setInteger("MineArmTimer", this.mineArmTimer);
        }
    }

    @Override
    public void readEntityFromNBT(@NotNull NBTTagCompound nbt) {
        if (nbt.getBoolean("MchMine")) {
            this.setName(nbt.getString("WeaponName"));
            this.mineArmed = nbt.getBoolean("MineArmed");
            this.mineArmTimer = nbt.getInteger("MineArmTimer");
            this.setMineFlag(LANDED_BIT, nbt.getBoolean("MineLanded"));
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            return;
        }
        super.readEntityFromNBT(nbt);
    }

    @Override
    public void sprinkleBomblet() {
        if (!this.world.isRemote) {
            MCH_EntityBomb e = new MCH_EntityBomb(
                    this.world, this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ,
                    this.rand.nextInt(360), 0.0F, this.acceleration);
            e.setParameterFromWeapon(this, this.shootingAircraft, this.shootingEntity);
            e.setName(this.getName());
            float RANDOM = this.getInfo().bombletDiff;
            e.motionX = this.motionX + (this.rand.nextFloat() - 0.5F) * RANDOM;
            e.motionY = this.motionY / 2.0 + (this.rand.nextFloat() - 0.5F) * RANDOM / 2.0F;
            e.motionZ = this.motionZ + (this.rand.nextFloat() - 0.5F) * RANDOM;
            e.setBomblet();
            this.world.spawnEntity(e);
        }
    }

    @Override
    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.Bomb;
    }
}
