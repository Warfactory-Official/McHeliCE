package com.norwood.mcheli.uav;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfoManager;
import com.norwood.mcheli.helicopter.MCH_ItemHeli;
import com.norwood.mcheli.helper.MCH_Logger;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class MCH_EntityUavStation extends W_EntityContainer implements IEntitySinglePassenger, IUavStation {

    private static final DataParameter<Byte> STATUS = EntityDataManager.createKey(MCH_EntityUavStation.class,
            DataSerializers.BYTE);
    private static final DataParameter<Integer> LAST_AC_ID = EntityDataManager.createKey(MCH_EntityUavStation.class,
            DataSerializers.VARINT);
    private static final DataParameter<BlockPos> UAV_POS = EntityDataManager.createKey(MCH_EntityUavStation.class,
            DataSerializers.BLOCK_POS);
    public boolean isRequestedSyncStatus;
    public int offsetX, offsetY, offsetZ;
    public float coverRotation, prevCoverRotation;
    @SideOnly(Side.CLIENT)
    protected double velocityX, velocityY, velocityZ;
    protected Entity lastRiddenByEntity;
    @SideOnly(Side.CLIENT)
    protected int aircraftPosRotInc;
    protected double aircraftX, aircraftY, aircraftZ;
    protected double aircraftYaw, aircraftPitch;
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
        this.offsetX = 0;
        this.offsetY = 0;
        this.offsetZ = 0;
        this.coverRotation = 0.0F;
        this.prevCoverRotation = 0.0F;
        this.setControlled(null);
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
            MCH_Logger.debugLog(this.world, "MCH_EntityUavStation.setStatus(%d)", n);
            this.dataManager.set(STATUS, (byte) n);
        }
    }

    public @NotNull StationType getType() {
        int kind = 127 & this.getStatus();
        return switch (kind) {
            case 2  -> StationType.SMALL;
            default -> StationType.DEFAULT;
        };
    }

    public void setType(@NotNull StationType type) {
        int kind = (type == StationType.SMALL) ? 2 : 1;
        this.setStatus((this.getStatus() & 128) | (kind & 127));
    }

    @Override
    @Nullable
    public Entity getOperator() {
        return getRiddenByEntity();
    }

    public boolean isOpen() {
        return (this.getStatus() & 128) != 0;
    }

    public void setOpen(boolean b) {
        this.setStatus((b ? 128 : 0) | this.getStatus() & 127);
    }


    @Override
    public Vec3d getPos() {
       return new Vec3d(posX,posY,posZ)
        ;
    }

    @Nullable
    public MCH_EntityAircraft getControlled() {
        return this.controlAircraft;
    }

    public void setControlled(@Nullable MCH_EntityAircraft aircraft) {
        this.controlAircraft = aircraft;
        if (aircraft != null && !aircraft.isDead) {
            this.setLastControlAircraft(aircraft);
        }
    }

    public void setUavPosition(int x, int y, int z) {
        if (!this.world.isRemote) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            this.dataManager.set(UAV_POS, new BlockPos(x, y, z));
        }
    }

    public void updateUavPosition() {
        BlockPos uavPos = this.dataManager.get(UAV_POS);
        this.offsetX = uavPos.getX();
        this.offsetY = uavPos.getY();
        this.offsetZ = uavPos.getZ();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);

        nbt.setInteger("UavStatus", this.getStatus());

        nbt.setInteger("PosUavX", this.offsetX);
        nbt.setInteger("PosUavY", this.offsetY);
        nbt.setInteger("PosUavZ", this.offsetZ);

        String guid = (this.getLastControlAircraft() instanceof MCH_EntityAircraft ac && !ac.isDead)
                ? ac.getCommonUniqueId()
                : this.loadedLastControlAircraftGuid;

        nbt.setString("LastCtrlAc", guid);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);

        this.setUavPosition(
                nbt.getInteger("PosUavX"),
                nbt.getInteger("PosUavY"),
                nbt.getInteger("PosUavZ")
        );

        if (nbt.hasKey("UavStatus")) {
            this.setStatus(nbt.getInteger("UavStatus"));
        } else {
            this.setType(StationType.DEFAULT);
        }

        this.loadedLastControlAircraftGuid = nbt.getString("LastCtrlAc");
    }

    public void initUavPostion() {
        int rt = (int) (MCH_Lib.getRotate360(this.rotationYaw + 45.0F) / 90.0);
        this.offsetX = rt != 0 && rt != 3 ? -12 : 12;
        this.offsetZ = rt != 0 && rt != 1 ? -12 : 12;
        this.offsetY = 2;
        this.setUavPosition(this.offsetX, this.offsetY, this.offsetZ);
    }

    @Override
    public void setDead() {
        super.setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        if (isEntityInvulnerable(source)) return false;
        if (isDead || world.isRemote) return true;

        final float finalDamage = MCH_Config.applyDamageByExternal(this, source, damage);
        if (!MCH_Multiplay.canAttackEntity(source, this)) return false;

        String sound = switch (source.getTrueSource()) {
            case EntityPlayer _ -> "hit";
            default -> "helidmg";
        };

        float pitch = sound.equals("hit") ? 1.0F : 0.9F + rand.nextFloat() * 0.1F;
        W_WorldFunc.playSoundAt(this, sound, 1.0F, pitch);

        markVelocityChanged();

        if (finalDamage > 0.0F) {
            processDestruction(source);
        }

        return true;
    }

    private void processDestruction(DamageSource source) {
        Entity pilot = getRiddenByEntity();
        if (pilot != null) pilot.startRiding(this);

        this.dropContentsWhenDead = true;
        this.setDead();

        switch (source.getTrueSource()) {
            case EntityPlayer p when source.getDamageType().equals("player") -> {
                if (!p.capabilities.isCreativeMode) dropUavStation();
            }
            case Entity _ -> {
                MCH_Explosion.newExplosion(world, null, pilot, posX, posY, posZ, 1.0F, 0.0F, true, true, false, false, 0);
                dropUavStation();
            }
            case null -> {
                MCH_Explosion.newExplosion(world, null, pilot, posX, posY, posZ, 1.0F, 0.0F, true, true, false, false, 0);
                dropUavStation();
            }
        }
    }
    private void dropUavStation() {
        int kind = switch (getType()) {
            case DEFAULT -> 1;
            case SMALL   -> 2;
        };

        if (kind > 0 && kind <= MCH_MOD.itemUavStation.length) {
            this.dropItemWithOffset(MCH_MOD.itemUavStation[kind - 1], 1, 0.0F);
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
        if (this.getType() == StationType.SMALL && riddenByEntity != null) {
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

    public void applyEntityCollision(@NotNull Entity par1Entity) {
    }

    public void addVelocity(double par1, double par3, double par5) {
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double par1, double par3, double par5) {
        this.velocityX = this.motionX = par1;
        this.velocityY = this.motionY = par3;
        this.velocityZ = this.motionZ = par5;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        this.prevCoverRotation = this.coverRotation;
        float delta = this.isOpen() ? 0.1F : -0.1F;
        this.coverRotation = Math.clamp(this.coverRotation + delta, 0.0F, 1.0F);

        Entity currentRider = this.getRiddenByEntity();
        if (currentRider == null) {
            if (this.lastRiddenByEntity != null) {
                this.unmountEntity(true);
            }
            this.setControlled(null);
        }

        if (!this.isRequestedSyncStatus) {
            this.isRequestedSyncStatus = this.ticksExisted >= 30
                    || this.getType() != StationType.SMALL
                    || this.world.isRemote;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.getControlled() instanceof MCH_EntityAircraft ac && ac.isDead) {
            this.setControlled(null);
        }

        if (this.getLastControlAircraft() instanceof MCH_EntityAircraft lastAc && lastAc.isDead) {
            this.setLastControlAircraft(null);
        }

        if (this.world.isRemote) {
            this.onUpdate_Client();
        } else {
            this.onUpdate_Server();
        }

        this.lastRiddenByEntity = currentRider;
    }

    @Nullable
    public MCH_EntityAircraft getLastControlAircraft() {
        return this.lastControlAircraft;
    }

    public void setLastControlAircraft(MCH_EntityAircraft ac) {
        MCH_Logger.debugLog(this.world, "MCH_EntityUavStation.setLastControlAircraft:" + ac);
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
                    MCH_Logger.debugLog(this.world, "MCH_EntityUavStation.searchLastControlAircraft:found" + n);
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
            this.setControlled(this.getLastControlAircraft());
            W_EntityPlayer.closeScreen(user);
        }
    }

    public void handleItem(@Nullable Entity user, ItemStack itemStack) {
        if (user == null || user.isDead || itemStack.isEmpty() || itemStack.getCount() != 1 || world.isRemote) {
            return;
        }

        double x = posX + offsetX;
        double y = Math.max(posY + offsetY, 2.0);
        double z = posZ + offsetZ;

        MCH_EntityAircraft ac = switch (itemStack.getItem()) {
            case MCP_ItemPlane p when getInfo(p) instanceof MCH_PlaneInfo pi && pi.isUAV ->
                    (pi.isSmallUAV || getType() != StationType.SMALL) ? p.createAircraft(world, x, y, z, itemStack) : null;
            case MCH_ItemShip s when getInfo(s) instanceof MCH_ShipInfo si && si.isUAV ->
                    (si.isSmallUAV || getType() != StationType.SMALL) ? s.createAircraft(world, x, y, z, itemStack) : null;
            case MCH_ItemHeli h when getInfo(h) instanceof MCH_HeliInfo hi && hi.isUAV ->
                    (hi.isSmallUAV || getType() != StationType.SMALL) ? h.createAircraft(world, x, y, z, itemStack) : null;
            case MCH_ItemTank t when getInfo(t) instanceof MCH_TankInfo ti && ti.isUAV ->
                    (ti.isSmallUAV || getType() != StationType.SMALL) ? t.createAircraft(world, x, y, z, itemStack) : null;

            default -> null;
        };

        if (ac != null) {
            finalizeUavSpawn(ac, user, itemStack);
        }
    }

    private MCH_AircraftInfo getInfo(Item item) {
        return switch (item) {
            case MCP_ItemPlane p -> MCP_PlaneInfoManager.getFromItem(p);
            case MCH_ItemHeli h -> MCH_HeliInfoManager.getFromItem(h);
            case MCH_ItemTank t -> MCH_TankInfoManager.getFromItem(t);
            case MCH_ItemShip s -> MCH_ShipInfoManager.getFromItem(s);
            default -> null;
        };
    }

    private void finalizeUavSpawn(MCH_EntityAircraft ac, Entity user, ItemStack stack) {
        float yaw = rotationYaw - 180.0F;
        ac.rotationYaw = ac.prevRotationYaw = yaw;

        if (world.getCollisionBoxes(ac, ac.getEntityBoundingBox().grow(-0.1)).isEmpty()) {
            stack.shrink(1);
            if (ac.isTargetDrone()) {
                ac.setFuel(ac.getMaxFuel());
            } else {
                ac.setUavStation(this);
                setControlled(ac);
                W_EntityPlayer.closeScreen(user);
            }
            user.rotationYaw = yaw;
            world.spawnEntity(ac);
        } else {
            ac.setDead();
        }
    }


    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND || this.getRidingEntity() != null) return false;

        if (getType() == StationType.SMALL) {
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

        if (this.getControlled() != null) {
            this.getControlled().setUavStation(null);
        }

        this.setControlled(null);
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
