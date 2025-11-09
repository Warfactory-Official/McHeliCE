package com.norwood.mcheli.aircraft;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.mob.MCH_ItemSpawnGunner;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.wrapper.W_Entity;

public class MCH_EntitySeat extends W_Entity implements IEntitySinglePassenger {

    public static final float BB_SIZE = 1.0F;
    public String parentUniqueID;
    public int seatID;
    public int parentSearchCount;
    protected Entity lastRiddenByEntity;
    private MCH_EntityAircraft parent;

    public MCH_EntitySeat(World world) {
        super(world);
        this.setSize(1.0F, 1.0F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.seatID = -1;
        this.setParent(null);
        this.parentSearchCount = 0;
        this.lastRiddenByEntity = null;
        this.ignoreFrustumCheck = true;
        this.isImmuneToFire = true;
    }

    public MCH_EntitySeat(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y + 1.0, z);
        this.prevPosX = x;
        this.prevPosY = y + 1.0;
        this.prevPosZ = z;
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return par1Entity.getEntityBoundingBox();
    }

    public AxisAlignedBB getCollisionBoundingBox() {
        return this.getEntityBoundingBox();
    }

    public double getMountedYOffset() {
        return -0.3;
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return this.getParent() != null && this.getParent().attackEntityFrom(par1DamageSource, par2);
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch,
                                             int posRotationIncrements, boolean teleport) {}

    public void setDead() {
        super.setDead();
    }

    public void onUpdate() {
        super.onUpdate();
        this.fallDistance = 0.0F;
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            riddenByEntity.fallDistance = 0.0F;
        }

        if (this.lastRiddenByEntity == null && riddenByEntity != null) {
            if (this.getParent() != null) {
                MCH_Lib.DbgLog(this.world, "MCH_EntitySeat.onUpdate:SeatID=%d", this.seatID, riddenByEntity.toString());
                this.getParent().onMountPlayerSeat(this, riddenByEntity);
            }
        } else if (this.lastRiddenByEntity != null && riddenByEntity == null && this.getParent() != null) {
            MCH_Lib.DbgLog(this.world, "MCH_EntitySeat.onUpdate:SeatID=%d", this.seatID,
                    this.lastRiddenByEntity.toString());
            this.getParent().onUnmountPlayerSeat(this, this.lastRiddenByEntity);
        }

        if (this.world.isRemote) {
            this.onUpdate_Client();
        } else {
            this.onUpdate_Server();
        }

        this.lastRiddenByEntity = this.getRiddenByEntity();
    }

    private void onUpdate_Client() {
        this.checkDetachmentAndDelete();
    }

    private void onUpdate_Server() {
        this.checkDetachmentAndDelete();
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null && riddenByEntity.isDead) {}
    }

    public void updatePassenger(@NotNull Entity passenger) {
        this.updatePosition(passenger);
    }

    public void updatePosition(@Nullable Entity ridEnt) {
        if (ridEnt != null) {
            ridEnt.setPosition(this.posX, this.posY, this.posZ);
            ridEnt.motionX = ridEnt.motionY = ridEnt.motionZ = 0.0;
        }
    }

    public void updateRotation(@Nullable Entity ridEnt, float yaw, float pitch) {
        if (ridEnt != null) {
            ridEnt.rotationYaw = yaw;
            ridEnt.rotationPitch = pitch;
        }
    }

    protected void checkDetachmentAndDelete() {
        if (!this.isDead && (this.seatID < 0 || this.getParent() == null || this.getParent().isDead)) {
            if (this.getParent() != null && this.getParent().isDead) {
                this.parentSearchCount = 100000000;
            }

            if (this.parentSearchCount >= 1200) {
                this.setDead();
                if (!this.world.isRemote) {
                    Entity riddenByEntity = this.getRiddenByEntity();
                    if (riddenByEntity != null) {
                        riddenByEntity.dismountRidingEntity();
                    }
                }

                this.setParent(null);
                MCH_Lib.DbgLog(this.world, "[Error]座席エンティティは本体が見つからないため削除 seat=%d, parentUniqueID=%s", this.seatID,
                        this.parentUniqueID);
            } else {
                this.parentSearchCount++;
            }
        } else {
            this.parentSearchCount = 0;
        }
    }

    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setInteger("SeatID", this.seatID);
        par1NBTTagCompound.setString("ParentUniqueID", this.parentUniqueID);
    }

    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        this.seatID = par1NBTTagCompound.getInteger("SeatID");
        this.parentUniqueID = par1NBTTagCompound.getString("ParentUniqueID");
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    public boolean canRideMob(Entity entity) {
        return this.getParent() != null && this.seatID >= 0 &&
                !(this.getParent().getSeatInfo(this.seatID + 1) instanceof MCH_SeatRackInfo);
    }

    public boolean isGunnerMode() {
        Entity riddenByEntity = this.getRiddenByEntity();
        return riddenByEntity != null && this.getParent() != null && this.getParent().getIsGunnerMode(riddenByEntity);
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        if (this.getParent() != null && !this.getParent().isDestroyed()) {
            if (!this.getParent().checkTeam(player)) {
                return false;
            } else {
                ItemStack itemStack = player.getHeldItem(hand);
                if (!itemStack.isEmpty() && itemStack.getItem() instanceof MCH_ItemWrench) {
                    return this.getParent().processInitialInteract(player, hand);
                } else if (!itemStack.isEmpty() && itemStack.getItem() instanceof MCH_ItemSpawnGunner) {
                    return this.getParent().processInitialInteract(player, hand);
                } else {
                    Entity riddenByEntity = this.getRiddenByEntity();
                    if (riddenByEntity != null) {
                        return false;
                    } else if (player.getRidingEntity() != null) {
                        return false;
                    } else if (!this.canRideMob(player)) {
                        return false;
                    } else {
                        player.startRiding(this);
                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    @Nullable
    public MCH_EntityAircraft getParent() {
        return this.parent;
    }

    public void setParent(MCH_EntityAircraft parent) {
        this.parent = parent;
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            MCH_Lib.DbgLog(this.world, "MCH_EntitySeat.setParent:SeatID=%d %s : " + this.getParent(), this.seatID,
                    riddenByEntity.toString());
            if (this.getParent() != null) {
                this.getParent().onMountPlayerSeat(this, riddenByEntity);
            }
        }
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        List<Entity> passengers = this.getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
