package com.norwood.mcheli.uav;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfoManager;
import com.norwood.mcheli.helicopter.MCH_ItemHeli;
import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.helper.network.PooledGuiParameter;
import com.norwood.mcheli.multiplay.MCH_Multiplay;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.plane.MCP_ItemPlane;
import com.norwood.mcheli.plane.MCP_PlaneInfoManager;
import com.norwood.mcheli.ship.MCH_ItemShip;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.ship.MCH_ShipInfoManager;
import com.norwood.mcheli.tank.MCH_ItemTank;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.tank.MCH_TankInfoManager;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_EntityContainer;
import com.norwood.mcheli.wrapper.W_EntityPlayer;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

import javax.annotation.Nullable;
import java.util.List;

public class MCH_EntityUavStation extends W_EntityContainer implements IEntitySinglePassenger {

    private static final DataParameter<Byte> STATUS = EntityDataManager.createKey(MCH_EntityUavStation.class,
            DataSerializers.BYTE);
    private static final DataParameter<Integer> LAST_AC_ID = EntityDataManager.createKey(MCH_EntityUavStation.class,
            DataSerializers.VARINT);
    private static final DataParameter<BlockPos> UAV_POS = EntityDataManager.createKey(MCH_EntityUavStation.class,
            DataSerializers.BLOCK_POS);
    public boolean isRequestedSyncStatus;
    public int posUavX;
    public int posUavY;
    public int posUavZ;
    public float rotCover;
    public float prevRotCover;
    protected Entity lastRiddenByEntity;
    @SideOnly(Side.CLIENT)
    protected double velocityX;
    @SideOnly(Side.CLIENT)
    protected double velocityY;
    @SideOnly(Side.CLIENT)
    protected double velocityZ;
    protected int aircraftPosRotInc;
    protected double aircraftX;
    protected double aircraftY;
    protected double aircraftZ;
    protected double aircraftYaw;
    protected double aircraftPitch;
    private MCH_EntityAircraft controlAircraft;
    private MCH_EntityAircraft lastControlAircraft;
    private String loadedLastControlAircraftGuid;

    public MCH_EntityUavStation(World world) {
        super(world);
        this.dropContentsWhenDead = false;
        this.preventEntitySpawning = true;
        this.setSize(2.0F, 0.7F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.ignoreFrustumCheck = true;
        this.lastRiddenByEntity = null;
        this.aircraftPosRotInc = 0;
        this.aircraftX = 0.0;
        this.aircraftY = 0.0;
        this.aircraftZ = 0.0;
        this.aircraftYaw = 0.0;
        this.aircraftPitch = 0.0;
        this.posUavX = 0;
        this.posUavY = 0;
        this.posUavZ = 0;
        this.rotCover = 0.0F;
        this.prevRotCover = 0.0F;
        this.setControlAircract(null);
        this.setLastControlAircraft(null);
        this.loadedLastControlAircraftGuid = "";
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATUS, (byte) 0);
        this.dataManager.register(LAST_AC_ID, 0);
        this.dataManager.register(UAV_POS, BlockPos.ORIGIN);
        this.setOpen(true);
    }

    public int getStatus() {
        return this.dataManager.get(STATUS);
    }

    public void setStatus(int n) {
        if (!this.world.isRemote) {
            MCH_Lib.DbgLog(this.world, "MCH_EntityUavStation.setStatus(%d)", n);
            this.dataManager.set(STATUS, (byte) n);
        }
    }

    public int getKind() {
        return 127 & this.getStatus();
    }

    public void setKind(int n) {
        this.setStatus(this.getStatus() & 128 | n);
    }

    public boolean isOpen() {
        return (this.getStatus() & 128) != 0;
    }

    public void setOpen(boolean b) {
        this.setStatus((b ? 128 : 0) | this.getStatus() & 127);
    }

    @Nullable
    public MCH_EntityAircraft getControlAircract() {
        return this.controlAircraft;
    }

    public void setControlAircract(@Nullable MCH_EntityAircraft ac) {
        this.controlAircraft = ac;
        if (ac != null && !ac.isDead) {
            this.setLastControlAircraft(ac);
        }
    }

    public void setUavPosition(int x, int y, int z) {
        if (!this.world.isRemote) {
            this.posUavX = x;
            this.posUavY = y;
            this.posUavZ = z;
            this.dataManager.set(UAV_POS, new BlockPos(x, y, z));
        }
    }

    public void updateUavPosition() {
        BlockPos uavPos = this.dataManager.get(UAV_POS);
        this.posUavX = uavPos.getX();
        this.posUavY = uavPos.getY();
        this.posUavZ = uavPos.getZ();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("UavStatus", this.getStatus());
        nbt.setInteger("PosUavX", this.posUavX);
        nbt.setInteger("PosUavY", this.posUavY);
        nbt.setInteger("PosUavZ", this.posUavZ);
        String s = "";
        if (this.getLastControlAircraft() != null && !this.getLastControlAircraft().isDead) {
            s = this.getLastControlAircraft().getCommonUniqueId();
        }

        if (s.isEmpty()) {
            s = this.loadedLastControlAircraftGuid;
        }

        nbt.setString("LastCtrlAc", s);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.setUavPosition(nbt.getInteger("PosUavX"), nbt.getInteger("PosUavY"), nbt.getInteger("PosUavZ"));
        if (nbt.hasKey("UavStatus")) {
            this.setStatus(nbt.getInteger("UavStatus"));
        } else {
            this.setKind(1);
        }

        this.loadedLastControlAircraftGuid = nbt.getString("LastCtrlAc");
    }

    public void initUavPostion() {
        int rt = (int) (MCH_Lib.getRotate360(this.rotationYaw + 45.0F) / 90.0);
        this.posUavX = rt != 0 && rt != 3 ? -12 : 12;
        this.posUavZ = rt != 0 && rt != 1 ? -12 : 12;
        this.posUavY = 2;
        this.setUavPosition(this.posUavX, this.posUavY, this.posUavZ);
    }

    @Override
    public void setDead() {
        super.setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        if (this.isEntityInvulnerable(damageSource)) {
            return false;
        } else if (this.isDead) {
            return true;
        } else if (this.world.isRemote) {
            return true;
        } else {
            String dmt = damageSource.getDamageType();
            damage = MCH_Config.applyDamageByExternal(this, damageSource, damage);
            if (!MCH_Multiplay.canAttackEntity(damageSource, this)) {
                return false;
            } else {
                boolean isCreative = false;
                Entity entity = damageSource.getTrueSource();
                boolean isDamegeSourcePlayer = false;
                if (entity instanceof EntityPlayer) {
                    isCreative = ((EntityPlayer) entity).capabilities.isCreativeMode;
                    if (dmt.compareTo("player") == 0) {
                        isDamegeSourcePlayer = true;
                    }

                    W_WorldFunc.MOD_playSoundAtEntity(this, "hit", 1.0F, 1.0F);
                } else {
                    W_WorldFunc.MOD_playSoundAtEntity(this, "helidmg", 1.0F, 0.9F + this.rand.nextFloat() * 0.1F);
                }

                this.markVelocityChanged();
                if (damage > 0.0F) {
                    Entity riddenByEntity = this.getRiddenByEntity();
                    if (riddenByEntity != null) {
                        riddenByEntity.startRiding(this);
                    }

                    this.dropContentsWhenDead = true;
                    this.setDead();
                    if (!isDamegeSourcePlayer) {
                        MCH_Explosion.newExplosion(this.world, null, riddenByEntity, this.posX, this.posY, this.posZ,
                                1.0F, 0.0F, true, true, false, false, 0);
                    }

                    if (!isCreative) {
                        int kind = this.getKind();
                        if (kind > 0) {
                            this.dropItemWithOffset(MCH_MOD.itemUavStation[kind - 1], 1, 0.0F);
                        }
                    }
                }

                return true;
            }
        }
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
        Entity riddenByEntity = this.getRiddenByEntity();
        if (this.getKind() == 2 && riddenByEntity != null) {
            double px = -Math.sin(this.rotationYaw * Math.PI / 180.0) * 0.9;
            double pz = Math.cos(this.rotationYaw * Math.PI / 180.0) * 0.9;
            int x = (int) (this.posX + px);
            int y = (int) (this.posY - 0.5);
            int z = (int) (this.posZ + pz);
            BlockPos blockpos = new BlockPos(x, y, z);
            IBlockState iblockstate = this.world.getBlockState(blockpos);
            return iblockstate.isOpaqueCube() ? -0.4 : -0.9;
        } else {
            return 0.35;
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 2.0F;
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    public void applyEntityCollision(@NotNull Entity par1Entity) {}

    public void addVelocity(double par1, double par3, double par5) {}

    @SideOnly(Side.CLIENT)
    public void setVelocity(double par1, double par3, double par5) {
        this.velocityX = this.motionX = par1;
        this.velocityY = this.motionY = par3;
        this.velocityZ = this.motionZ = par5;
    }

    public void onUpdate() {
        super.onUpdate();
        this.prevRotCover = this.rotCover;
        if (this.isOpen()) {
            if (this.rotCover < 1.0F) {
                this.rotCover += 0.1F;
            } else {
                this.rotCover = 1.0F;
            }
        } else if (this.rotCover > 0.0F) {
            this.rotCover -= 0.1F;
        } else {
            this.rotCover = 0.0F;
        }

        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity == null) {
            if (this.lastRiddenByEntity != null) {
                this.unmountEntity(true);
            }

            this.setControlAircract(null);
        }

        int uavStationKind = this.getKind();
        if (this.ticksExisted >= 30 || uavStationKind != 2 || this.world.isRemote && !this.isRequestedSyncStatus) {
            this.isRequestedSyncStatus = true;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.getControlAircract() != null && this.getControlAircract().isDead) {
            this.setControlAircract(null);
        }

        if (this.getLastControlAircraft() != null && this.getLastControlAircraft().isDead) {
            this.setLastControlAircraft(null);
        }

        if (this.world.isRemote) {
            this.onUpdate_Client();
        } else {
            this.onUpdate_Server();
        }

        this.lastRiddenByEntity = this.getRiddenByEntity();
    }

    @Nullable
    public MCH_EntityAircraft getLastControlAircraft() {
        return this.lastControlAircraft;
    }

    public void setLastControlAircraft(MCH_EntityAircraft ac) {
        MCH_Lib.DbgLog(this.world, "MCH_EntityUavStation.setLastControlAircraft:" + ac);
        this.lastControlAircraft = ac;
    }

    public MCH_EntityAircraft getAndSearchLastControlAircraft() {
        if (this.getLastControlAircraft() == null) {
            int id = this.getLastControlAircraftEntityId();
            if (id > 0) {
                Entity entity = this.world.getEntityByID(id);
                if (entity instanceof MCH_EntityAircraft ac) {
                    if (ac.isUAV()) {
                        this.setLastControlAircraft(ac);
                    }
                }
            }
        }

        return this.getLastControlAircraft();
    }

    public Integer getLastControlAircraftEntityId() {
        return this.dataManager.get(LAST_AC_ID);
    }

    public void setLastControlAircraftEntityId(int s) {
        if (!this.world.isRemote) {
            this.dataManager.set(LAST_AC_ID, s);
        }
    }

    public void searchLastControlAircraft() {
        if (!this.loadedLastControlAircraftGuid.isEmpty()) {
            List<MCH_EntityAircraft> list = this.world.getEntitiesWithinAABB(MCH_EntityAircraft.class,
                    this.getCollisionBoundingBox().grow(120.0, 120.0, 120.0));
            for (MCH_EntityAircraft ac : list) {
                if (ac.getCommonUniqueId().equals(this.loadedLastControlAircraftGuid)) {
                    String n = "no info : " + ac;
                    MCH_Lib.DbgLog(this.world, "MCH_EntityUavStation.searchLastControlAircraft:found" + n);
                    this.setLastControlAircraft(ac);
                    this.setLastControlAircraftEntityId(W_Entity.getEntityId(ac));
                    this.loadedLastControlAircraftGuid = "";
                    return;
                }
            }
        }
    }

    protected void onUpdate_Client() {
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
            this.motionY *= 0.96;
            this.motionX = 0.0;
            this.motionZ = 0.0;
        }

        this.updateUavPosition();
    }

    private void onUpdate_Server() {
        this.motionY -= 0.03;
        this.move(MoverType.SELF, 0.0, this.motionY, 0.0);
        this.motionY *= 0.96;
        this.motionX = 0.0;
        this.motionZ = 0.0;
        this.setRotation(this.rotationYaw, this.rotationPitch);
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            if (riddenByEntity.isDead) {
                this.unmountEntity(true);
            } else {
                ItemStack item = this.getStackInSlot(0);
                if (!item.isEmpty()) {
                    this.handleItem(riddenByEntity, item);
                    if (item.getCount() == 0) {
                        this.setInventorySlotContents(0, ItemStack.EMPTY);
                    }
                }
            }
        }

        if (this.getLastControlAircraft() == null && this.ticksExisted % 40 == 0) {
            this.searchLastControlAircraft();
        }
    }

    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9,
                                             boolean teleport) {
        this.aircraftPosRotInc = par9 + 8;
        this.aircraftX = par1;
        this.aircraftY = par3;
        this.aircraftZ = par5;
        this.aircraftYaw = par7;
        this.aircraftPitch = par8;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    public void updatePassenger(@NotNull Entity passenger) {
        if (this.isPassenger(passenger)) {
            double x = -Math.sin(this.rotationYaw * Math.PI / 180.0) * 0.9;
            double z = Math.cos(this.rotationYaw * Math.PI / 180.0) * 0.9;
            passenger.setPosition(this.posX + x,
                    this.posY + this.getMountedYOffset() + passenger.getYOffset() + W_Entity.GLOBAL_Y_OFFSET,
                    this.posZ + z);
        }
    }

    public void controlLastAircraft(Entity user) {
        if (this.getLastControlAircraft() != null && !this.getLastControlAircraft().isDead) {
            this.getLastControlAircraft().setUavStation(this);
            this.setControlAircract(this.getLastControlAircraft());
            W_EntityPlayer.closeScreen(user);
        }
    }

    public void handleItem(@Nullable Entity user, ItemStack itemStack) {
        if (user != null && !user.isDead && !itemStack.isEmpty() && itemStack.getCount() == 1) {
            if (!this.world.isRemote) {
                MCH_EntityAircraft ac = null;
                double x = this.posX + this.posUavX;
                double y = this.posY + this.posUavY;
                double z = this.posZ + this.posUavZ;
                if (y <= 1.0) {
                    y = 2.0;
                }

                Item item = itemStack.getItem();
                if (item instanceof MCP_ItemPlane) {
                    MCH_PlaneInfo pi = MCP_PlaneInfoManager.getFromItem(item);
                    if (pi != null && pi.isUAV) {
                        if (!pi.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCP_ItemPlane) item).createAircraft(this.world, x, y, z, itemStack);
                        }
                    }
                }

                if (item instanceof MCH_ItemShip) {
                    MCH_ShipInfo si = MCH_ShipInfoManager.getFromItem(item);
                    if (si != null && si.isUAV) {
                        if (!si.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCH_ItemShip) item).createAircraft(this.world, x, y, z, itemStack);
                        }
                    }
                }

                if (item instanceof MCH_ItemHeli) {
                    MCH_HeliInfo hi = MCH_HeliInfoManager.getFromItem(item);
                    if (hi != null && hi.isUAV) {
                        if (!hi.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCH_ItemHeli) item).createAircraft(this.world, x, y, z, itemStack);
                        }
                    }
                }

                if (item instanceof MCH_ItemTank) {
                    MCH_TankInfo hi = MCH_TankInfoManager.getFromItem(item);
                    if (hi != null && hi.isUAV) {
                        if (!hi.isSmallUAV && this.getKind() == 2) {
                            ac = null;
                        } else {
                            ac = ((MCH_ItemTank) item).createAircraft(this.world, x, y, z, itemStack);
                        }
                    }
                }

                if (ac != null) {
                    ac.rotationYaw = this.rotationYaw - 180.0F;
                    ac.prevRotationYaw = ac.rotationYaw;
                    user.rotationYaw = this.rotationYaw - 180.0F;
                    if (this.world.getCollisionBoxes(ac, ac.getEntityBoundingBox().grow(-0.1, -0.1, -0.1)).isEmpty()) {
                        itemStack.shrink(1);
                        MCH_Lib.DbgLog(this.world, "Create UAV: %s : %s", item.getTranslationKey(), item);
                        user.rotationYaw = this.rotationYaw - 180.0F;
                        if (!ac.isTargetDrone()) {
                            ac.setUavStation(this);
                            this.setControlAircract(ac);
                        }

                        this.world.spawnEntity(ac);
                        if (!ac.isTargetDrone()) {
                            ac.setFuel((int) (ac.getMaxFuel() * 0.05F));
                            W_EntityPlayer.closeScreen(user);
                        } else {
                            ac.setFuel(ac.getMaxFuel());
                        }
                    } else {
                        ac.setDead();
                    }
                }
            }
        }
    }

    public void _setInventorySlotContents(int par1, ItemStack itemStack) {
        super.setInventorySlotContents(par1, itemStack);
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) {
            return false;
        } else {
            int kind = this.getKind();
            if (kind <= 0) {
                return false;
            } else if (this.getRiddenByEntity() != null) {
                return false;
            } else {
                if (kind == 2) {
                    if (player.isSneaking()) {
                        this.setOpen(!this.isOpen());
                        return false;
                    }

                    if (!this.isOpen()) {
                        return false;
                    }
                }

                this.lastRiddenByEntity = null;
                PooledGuiParameter.setEntity(player, this);
                if (!this.world.isRemote) {
                    player.startRiding(this);
                    player.openGui(MCH_MOD.instance, 0, player.world, (int) this.posX, (int) this.posY,
                            (int) this.posZ);
                }

                return true;
            }
        }
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    public void unmountEntity(boolean unmountAllEntity) {
        Entity rByEntity = null;
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            if (!this.world.isRemote) {
                rByEntity = riddenByEntity;
                riddenByEntity.dismountRidingEntity();
            }
        } else if (this.lastRiddenByEntity != null) {
            rByEntity = this.lastRiddenByEntity;
        }

        if (this.getControlAircract() != null) {
            this.getControlAircract().setUavStation(null);
        }

        this.setControlAircract(null);
        if (this.world.isRemote) {
            W_EntityPlayer.closeScreen(rByEntity);
        }

        this.lastRiddenByEntity = null;
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        List<Entity> passengers = this.getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
