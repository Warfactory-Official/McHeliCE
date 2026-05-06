package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.norwood.mcheli.*;
import com.norwood.mcheli.aircraft.components.*;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.factories.AircraftGuiData;
import com.norwood.mcheli.factories.MCHGuiFactories;
import com.norwood.mcheli.gui.AircraftGui;
import com.norwood.mcheli.gui.ContainerGui;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.helper.entity.ITargetMarkerObject;
import com.norwood.mcheli.mob.MCH_EntityGunner;
import com.norwood.mcheli.mob.MCH_ItemSpawnGunner;
import com.norwood.mcheli.multiplay.MCH_Multiplay;
import com.norwood.mcheli.networking.packet.*;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.UAVTracker;
import com.norwood.mcheli.weapon.*;
import com.norwood.mcheli.wrapper.*;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.*;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public abstract class MCH_EntityAircraft extends W_EntityContainer implements IGuiHolder<AircraftGuiData>, MCH_IEntityLockChecker, MCH_IEntityCanRideAircraft, IEntityAdditionalSpawnData, IEntitySinglePassenger, ITargetMarkerObject, IFluidHandler {

    public final InventoryComponent inventoryComponent;
    public final FuelComponent fuelComponent;
    @Delegate(excludes = IAircraftComponent.class)
    public final WeaponSystemComponent weaponSystem;
    @Delegate(excludes = IAircraftComponent.class)
    public final SeatManagerComponent seatManager;
    @Delegate(excludes = IAircraftComponent.class)
    public final FlightPhysicsComponent flightPhysics;
    public final MCH_Camera camera;
    public final HashMap<Entity, Integer> noCollisionEntities = new HashMap<>();
    protected final MCH_SoundUpdater soundUpdater;
    /* --- Radar & Systems --- */
    private final MCH_Radar entityRadar;
    private final MCH_EntityHitBox pilotSeat;
    /* --- Inventory & Logistics --- */
    private final MCH_AircraftInventory inventory;
    /* --- Components --- */
    @Delegate(excludes = IAircraftComponent.class)
    public NetworkSyncComponent networkSync;
    public int brightness = 240;
    public boolean keepOnRideRotation;
    public float lastRiderYaw;
    public float prevLastRiderYaw;
    public float lastRiderPitch;
    public float prevLastRiderPitch;

    /* --- Damage & State --- */
    public int repairCount;
    public int beforeDamageTaken;
    public int timeSinceHit;
    public int serverNoMoveCount = 0;
    public Entity lastAttackedEntity = null;
    public float rotDestroyedYaw, rotDestroyedPitch, rotDestroyedRoll;
    public int damageSinceDestroyed;
    public boolean isFirstDamageSmoke = true;
    public Vec3d[] prevDamageSmokePos = new Vec3d[0];
    public MCH_Parts partHatch;
    public MCH_Parts partCanopy;
    public MCH_Parts partLandingGear;
    public float[] rotPartRotation;
    public float[] prevRotPartRotation;
    public float lastSearchLightYaw, lastSearchLightPitch;
    public float rotLightHatch = 0.0F;
    public float prevRotLightHatch = 0.0F;
    /* --- Collision & Bounds --- */
    public MCH_BoundingBox[] extraBoundingBox;
    @Nullable
    public String lastBBName;
    public float lastBBDamageFactor;
    /* --- Landing & UI --- */
    public double prevLandInDistance = -1.0;
    public Vec3d impactPos = null;
    public Vec3d prevImpactPos = null;
    public float thirdPersonDist = 4.0F;
    public boolean cs_dismountAll;
    public boolean cs_heliAutoThrottleDown;
    public boolean cs_planeAutoThrottleDown;
    public boolean cs_tankAutoThrottleDown;
    protected Entity lastRiddenByEntity;
    protected Entity lastRidingEntity;
    private List<IAircraftComponent> components = new ArrayList<>();
    @Getter
    private int radarRotate;
    @Getter
    private int cameraId;
    private Entity[] partEntities;
    /* --- Specialized Equipment (UAV, Towed, Parachute) --- */
    @Setter
    private MCH_EntityChain towChainEntity;
    @Setter
    private MCH_EntityChain towedChainEntity;

    @Getter
    @Setter
    private boolean isParachuting;
    @Getter
    private boolean isHoveringMode = false;
    private int supplyAmmoWait;
    private boolean beforeSupplyAmmo;
    private double lastLandInDistance = -1.0;
    private double lastCalcLandInDistanceCount;
    /* --- Metadata & Lifecycle --- */
    private MCH_AircraftInfo acInfo;
    @Setter
    @Getter
    private String commonUniqueId;
    @Setter
    @Getter
    private int modeSwitchCooldown;
    @Setter
    @Getter
    private int despawnCount;
    @Getter
    private int countOnUpdate;


    public MCH_EntityAircraft(World world) {
        super(world);

        this.inventoryComponent = new InventoryComponent(this);
        this.fuelComponent = new FuelComponent(this);
        this.weaponSystem = new WeaponSystemComponent(this);
        this.seatManager = new SeatManagerComponent(this);
        this.flightPhysics = new FlightPhysicsComponent(this);
        this.components.add(this.inventoryComponent);
        this.components.add(this.weaponSystem);
        this.components.add(this.seatManager);
        this.components.add(this.flightPhysics);
        this.components.add(this.fuelComponent);
        MCH_Logger.debugLog(world, "MCH_EntityAircraft : " + this);
        this.setAcInfo(null);
        this.dropContentsWhenDead = false;
        this.ignoreFrustumCheck = true;
        this.entityRadar = new MCH_Radar(world);
        this.radarRotate = 0;
        this.setCurrentThrottle(0.0);
        this.cs_dismountAll = false;
        this.cs_heliAutoThrottleDown = true;
        this.cs_planeAutoThrottleDown = false;
        this._renderDistanceWeight = 2.0 * MCH_Config.RenderDistanceWeight.prmDouble;
        this.setCommonUniqueId("");
        this.pilotSeat = new MCH_EntityHitBox(world, this, 1.0F, 1.0F);
        this.pilotSeat.parent = this;
        this.partEntities = new Entity[]{this.pilotSeat};
        this.setTextureName("");
        this.camera = new MCH_Camera(world, this, this.posX, this.posY, this.posZ);
        this.setCameraId(0);
        this.lastRiddenByEntity = null;
        this.lastRidingEntity = null;
        this.soundUpdater = MCH_MOD.proxy.CreateSoundUpdater(this);
        this.countOnUpdate = 0;
        this.setTowChainEntity(null);
        this.repairCount = 0;
        this.beforeDamageTaken = 0;
        this.timeSinceHit = 0;
        this.setDespawnCount(0);
        this.modeSwitchCooldown = 0;
        this.partHatch = null;
        this.partCanopy = null;
        this.partLandingGear = null;
        this.rotPartRotation = new float[0];
        this.prevRotPartRotation = new float[0];
        this.lastRiderYaw = 0.0F;
        this.prevLastRiderYaw = 0.0F;
        this.lastRiderPitch = 0.0F;
        this.prevLastRiderPitch = 0.0F;
        this.extraBoundingBox = new MCH_BoundingBox[0];
        this.setEntityBoundingBox(new MCH_AircraftBoundingBox(this));
        this.lastBBDamageFactor = 1.0F;
        this.lastBBName = null;
        this.inventory = new MCH_AircraftInventory(this);
        this.isParachuting = false;
        this.lastSearchLightYaw = this.lastSearchLightPitch = 0.0F;
        this.forceSpawn = true;
    }

    @Nullable
    public static MCH_EntityAircraft getAircraft_RiddenOrControl(@Nullable Entity rider) {
        if (rider != null) {
            if (rider.getRidingEntity() instanceof MCH_EntityAircraft) {
                return (MCH_EntityAircraft) rider.getRidingEntity();
            }

            if (rider.getRidingEntity() instanceof MCH_EntitySeat) {
                return ((MCH_EntitySeat) rider.getRidingEntity()).getParent();
            }

            if (rider.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
                return uavStation.getControlled();
            }
        }

        return null;
    }

    private static void getCollisionBoxes(@Nullable Entity entity, AxisAlignedBB box, List<AxisAlignedBB> result) {
        final int minX = MathHelper.floor(box.minX) - 1;
        final int maxX = MathHelper.ceil(box.maxX) + 1;
        final int minY = MathHelper.floor(box.minY) - 1;
        final int maxY = MathHelper.ceil(box.maxY) + 1;
        final int minZ = MathHelper.floor(box.minZ) - 1;
        final int maxZ = MathHelper.ceil(box.maxZ) + 1;

        if (entity == null) return;
        final World world = entity.world;
        final WorldBorder border = world.getWorldBorder();

        final boolean wasOutsideBorder = entity.isOutsideBorder();
        final boolean insideWorldBorder = world.isInsideWorldBorder(entity);

        final IBlockState fallbackBlock = Blocks.STONE.getDefaultState();

        final PooledMutableBlockPos pos = PooledMutableBlockPos.retain();

        try {
            for (int x = minX; x < maxX; x++) {
                boolean edgeX = (x == minX || x == maxX - 1);

                for (int z = minZ; z < maxZ; z++) {
                    boolean edgeZ = (z == minZ || z == maxZ - 1);

                    // Skip the corner columns unless needed
                    if (edgeX && edgeZ) continue;

                    // Only check mid-height block to verify chunk is loaded
                    if (!world.isBlockLoaded(pos.setPos(x, 64, z))) continue;

                    for (int y = minY; y < maxY; y++) {
                        boolean topLayer = (y == maxY - 1);

                        // Skip unnecessary corner-top blocks
                        if ((edgeX || edgeZ) && topLayer) continue;

                        // Maintain border flag behavior
                        if (wasOutsideBorder == insideWorldBorder) {
                            entity.setOutsideBorder(!insideWorldBorder);
                        }

                        pos.setPos(x, y, z);

                        final IBlockState state = (!border.contains(pos) && insideWorldBorder) ? fallbackBlock : world.getBlockState(pos);

                        state.addCollisionBoxToList(world, pos, box, result, entity, false);
                    }
                }
            }
        } finally {
            pos.release();
        }

    }

    public static List<AxisAlignedBB> getCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb) {
        List<AxisAlignedBB> list = new ArrayList<>();
        getCollisionBoxes(entityIn, aabb, list);
        if (entityIn != null) {
            List<Entity> list1 = entityIn.world.getEntitiesWithinAABBExcludingEntity(entityIn, aabb.grow(0.25));

            for (Entity entity : list1) {
                if (!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntitySeat) && !(entity instanceof MCH_EntityHitBox)) {
                    AxisAlignedBB axisalignedbb = entity.getCollisionBoundingBox();
                    if (axisalignedbb != null && axisalignedbb.intersects(aabb)) {
                        list.add(axisalignedbb);
                    }

                    axisalignedbb = entityIn.getCollisionBox(entity);
                    if (axisalignedbb != null && axisalignedbb.intersects(aabb)) {
                        list.add(axisalignedbb);
                    }
                }
            }
        }

        return list;
    }

    public void onUpdate_RidingEntity() {
        if (!this.world.isRemote && seatManager.waitMountEntity == 0 && this.getCountOnUpdate() > 20 && this.canMountWithNearEmptyMinecart()) {
            this.mountWithNearEmptyMinecart();
        }

        if (seatManager.waitMountEntity > 0) {
            seatManager.waitMountEntity--;
        }

        if (!this.world.isRemote && this.getRidingEntity() != null) {
            this.setRotRoll(this.getRoll() * 0.9F);
            this.setRotPitch(this.getPitch() * 0.95F);
            Entity re = this.getRidingEntity();
            float target = MathHelper.wrapDegrees(re.rotationYaw + 90.0F);
            if (target - this.rotationYaw > 180.0F) {
                target -= 360.0F;
            }

            if (target - this.rotationYaw < -180.0F) {
                target += 360.0F;
            }


            float dist = 50.0F * (float) re.getDistanceSq(re.prevPosX, re.prevPosY, re.prevPosZ);
            if (dist > 0.001) {
                dist = MathHelper.sqrt(dist);
                float distYaw = MCH_Lib.RNG(target - this.rotationYaw, -dist, dist);
                this.rotationYaw += distYaw;
            }

            double bkPosX = this.posX;
            double bkPosY = this.posY;
            double bkPosZ = this.posZ;
            if (this.getRidingEntity().isDead) {
                this.dismountRidingEntity();
                seatManager.waitMountEntity = 20;
            } else if (this.getCurrentThrottle() > 0.8) {
                this.motionX = this.getRidingEntity().motionX;
                this.motionY = this.getRidingEntity().motionY;
                this.motionZ = this.getRidingEntity().motionZ;
                this.dismountRidingEntity();
                seatManager.waitMountEntity = 20;
            }

            this.posX = bkPosX;
            this.posY = bkPosY;
            this.posZ = bkPosZ;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double dist) {
        return true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.networkSync = new NetworkSyncComponent(this);
        this.networkSync.init();
        if (components == null)
            components = new ArrayList<>();
        this.components.add(0, this.networkSync);


        if (!this.world.isRemote) {
            this.setCommonStatus(3, MCH_Config.InfinityAmmo.prmBool);
            this.setCommonStatus(4, MCH_Config.InfinityFuel.prmBool);
            this.setGunnerStatus(true);
        }

        this.getEntityData().setString("EntityType", this.getEntityType());
    }

    public float getYaw() {
        return this.rotationYaw;
    }

    public void setRotYaw(float f) {
        this.rotationYaw = f;
    }

    public float getPitch() {
        return this.rotationPitch;
    }

    public void setRotPitch(float f) {
        this.rotationPitch = f;
    }

    public float getRoll() {
        return this.flightPhysics.getRotationRoll();
    }

    public void setRotRoll(float f) {
        this.flightPhysics.setRotationRoll(f);
    }

    public float getPrevRotationRoll() {
        return this.flightPhysics.getPrevRotationRoll();
    }

    public void setPrevRotationRoll(float prevRotationRoll) {
        this.flightPhysics.setPrevRotationRoll(prevRotationRoll);
    }

    public MCH_Queue<Vec3d> getPrevPositionHistory() {
        return this.flightPhysics.getPrevPosition();
    }

    public MCH_LowPassFilterFloat getLowPassPartialTicks() {
        return this.flightPhysics.getLowPassPartialTicks();
    }

    public double getCurrentThrottle() {
        return this.flightPhysics.getCurrentThrottle();
    }

    public void setCurrentThrottle(double currentThrottle) {
        this.flightPhysics.setCurrentThrottle(currentThrottle);
    }

    public double getPrevCurrentThrottle() {
        return this.flightPhysics.getPrevCurrentThrottle();
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.flightPhysics.setCurrentSpeed(currentSpeed);
    }

    public boolean isAircraftRollRev() {
        return this.flightPhysics.isAircraftRollRev();
    }

    public void setAircraftRollRev(boolean aircraftRollRev) {
        this.flightPhysics.setAircraftRollRev(aircraftRollRev);
    }

    public boolean isAircraftRotChanged() {
        return this.flightPhysics.isAircraftRotChanged();
    }

    public void setAircraftRotChanged(boolean aircraftRotChanged) {
        this.flightPhysics.setAircraftRotChanged(aircraftRotChanged);
    }

    public float getThrottleBack() {
        return this.flightPhysics.getThrottleBack();
    }

    public void setThrottleBack(float throttleBack) {
        this.flightPhysics.setThrottleBack(throttleBack);
    }

    public double getBeforeHoverThrottle() {
        return this.flightPhysics.getBeforeHoverThrottle();
    }

    public void setBeforeHoverThrottle(double beforeHoverThrottle) {
        this.flightPhysics.setBeforeHoverThrottle(beforeHoverThrottle);
    }

    public boolean isThrottleUp() {
        return this.flightPhysics.isThrottleUp();
    }

    public void setThrottleUp(boolean throttleUp) {
        this.flightPhysics.setThrottleUp(throttleUp);
    }

    public boolean isThrottleDown() {
        return this.flightPhysics.isThrottleDown();
    }

    public void setThrottleDown(boolean throttleDown) {
        this.flightPhysics.setThrottleDown(throttleDown);
    }

    public boolean isMoveLeft() {
        return this.flightPhysics.isMoveLeft();
    }

    public void setMoveLeft(boolean moveLeft) {
        this.flightPhysics.setMoveLeft(moveLeft);
    }

    public boolean isMoveRight() {
        return this.flightPhysics.isMoveRight();
    }

    public void setMoveRight(boolean moveRight) {
        this.flightPhysics.setMoveRight(moveRight);
    }

    public float[] getRotCrawlerTrack() {
        return this.flightPhysics.getRotCrawlerTrack();
    }

    public float[] getPrevRotCrawlerTrack() {
        return this.flightPhysics.getPrevRotCrawlerTrack();
    }

    public float[] getRotTrackRoller() {
        return this.flightPhysics.getRotTrackRoller();
    }

    public float[] getPrevRotTrackRoller() {
        return this.flightPhysics.getPrevRotTrackRoller();
    }

    public float getRotWheel() {
        return this.flightPhysics.getRotWheel();
    }

    public float getPrevRotWheel() {
        return this.flightPhysics.getPrevRotWheel();
    }

    public float getRotYawWheel() {
        return this.flightPhysics.getRotYawWheel();
    }

    public float getPrevRotYawWheel() {
        return this.flightPhysics.getPrevRotYawWheel();
    }

    public int getAircraftPosRotInc() {
        return this.flightPhysics.getAircraftPosRotInc();
    }

    public void setAircraftPosRotInc(int aircraftPosRotInc) {
        this.flightPhysics.setAircraftPosRotInc(aircraftPosRotInc);
    }

    public double getAircraftX() {
        return this.flightPhysics.getAircraftX();
    }

    public double getAircraftY() {
        return this.flightPhysics.getAircraftY();
    }

    public double getAircraftZ() {
        return this.flightPhysics.getAircraftZ();
    }

    public double getAircraftYaw() {
        return this.flightPhysics.getAircraftYaw();
    }

    public double getAircraftPitch() {
        return this.flightPhysics.getAircraftPitch();
    }

    public void setTextureName(@Nullable String textureName) {
        this.networkSync.setTextureName(textureName);
    }

    public String getTextureName() {
        return this.networkSync.getTextureName();
    }

    public void setCommonStatus(int bit, boolean value) {
        this.networkSync.setCommonStatus(bit, value);
    }

    public void setCommonStatus(int bit, boolean value, boolean writeClient) {
        this.networkSync.setCommonStatus(bit, value, writeClient);
    }

    public int getCommonStatus() {
        return this.networkSync.getCommonStatus();
    }

    public boolean getCommonStatus(int bit) {
        return this.networkSync.getCommonStatus(bit);
    }

    public float getServerRoll() {
        return this.networkSync.getServerRoll();
    }

    public String getTypeName() {
        return this.networkSync.getTypeName();
    }

    public void setTypeName(String typeName) {
        this.networkSync.setTypeName(typeName);
    }

    @Nullable
    public IUavStation getUavStation() {
        return this.networkSync.getUavStation();
    }

    public double getThrottle() {
        return this.networkSync.getThrottle();
    }

    public void setThrottle(double throttle) {
        this.networkSync.setThrottle(throttle);
    }

    public int getDamageTaken() {
        return this.networkSync.getDamageTaken();
    }

    public void setDamageTaken(int damageTaken) {
        this.networkSync.setDamageTaken(damageTaken);
    }

    public String getCommand() {
        return this.networkSync.getCommand();
    }

    public void setCommandForce(String command) {
        this.networkSync.setCommandForce(command);
    }

    public int getPartStatus() {
        return this.networkSync.getPartStatus();
    }

    public void setPartStatus(int partStatus) {
        this.networkSync.setPartStatus(partStatus);
    }

    public float getWeaponUserYaw(@Nullable Entity entity) {
        return this.networkSync.getWeaponUserYaw(entity);
    }

    public float getWeaponUserYaw(@Nullable Entity entity, float partialTicks) {
        return this.networkSync.getWeaponUserYaw(entity, partialTicks);
    }

    public float getWeaponUserPitch(@Nullable Entity entity) {
        return this.networkSync.getWeaponUserPitch(entity);
    }

    public float getWeaponUserPitch(@Nullable Entity entity, float partialTicks) {
        return this.networkSync.getWeaponUserPitch(entity, partialTicks);
    }

    public void setPacketWeaponUserAim(float yaw, float pitch) {
        this.networkSync.setPacketWeaponUserAim(yaw, pitch);
    }

    public void clearPacketWeaponUserAim() {
        this.networkSync.clearPacketWeaponUserAim();
    }

    public net.minecraftforge.items.ItemStackHandler getInventory() {
        return this.inventory.getItemHandler();
    }

    public int getSeatIdByEntity(@Nullable Entity entity) {
        return this.seatManager.getSeatIdByEntity(entity);
    }

    public int getSeatNum() {
        return this.seatManager.getSeatNum();
    }

    public @NotNull MCH_SeatInfo[] getSeatsInfo() {
        return this.seatManager.getSeatsInfo();
    }

    public MCH_SeatInfo getSeatInfo(@Nullable Entity entity) {
        return this.seatManager.getSeatInfo(entity);
    }

    public MCH_EntitySeat[] getSeats() {
        return this.seatManager.getSeats();
    }

    public Entity getEntityBySeatId(int id) {
        return this.seatManager.getEntityBySeatId(id);
    }

    public MCH_EntitySeat getSeat(int idx) {
        return this.seatManager.getSeat(idx);
    }

    public void setTowChainEntity(MCH_EntityChain towChainEntity) {
        this.towChainEntity = towChainEntity;
    }

    public void setTowedChainEntity(MCH_EntityChain towedChainEntity) {
        this.towedChainEntity = towedChainEntity;
    }

    public void setCommonUniqueId(String commonUniqueId) {
        this.commonUniqueId = commonUniqueId;
    }

    public String getCommonUniqueId() {
        return this.commonUniqueId;
    }

    public void setModeSwitchCooldown(int modeSwitchCooldown) {
        this.modeSwitchCooldown = modeSwitchCooldown;
    }

    public int getModeSwitchCooldown() {
        return this.modeSwitchCooldown;
    }

    public void setDespawnCount(int despawnCount) {
        this.despawnCount = despawnCount;
    }

    public int getDespawnCount() {
        return this.despawnCount;
    }

    public int getCountOnUpdate() {
        return this.countOnUpdate;
    }

    public int getCameraId() {
        return this.cameraId;
    }

    public boolean isHoveringMode() {
        return this.isHoveringMode;
    }

    public float getLastRiderYaw() {
        return this.lastRiderYaw;
    }

    public float getPrevLastRiderYaw() {
        return this.prevLastRiderYaw;
    }

    public float getLastRiderPitch() {
        return this.lastRiderPitch;
    }

    public float getPrevLastRiderPitch() {
        return this.prevLastRiderPitch;
    }

    public float getCurrentWeaponShotYaw(@Nullable Entity entity) {
        return this.weaponSystem.getCurrentWeaponShotYaw(entity);
    }

    public float getCurrentWeaponShotPitch(@Nullable Entity entity) {
        return this.weaponSystem.getCurrentWeaponShotPitch(entity);
    }

    public float getCurrentWeaponShotYaw(@Nullable Entity entity, float partialTicks) {
        return this.weaponSystem.getCurrentWeaponShotYaw(entity, partialTicks);
    }

    public float getCurrentWeaponShotPitch(@Nullable Entity entity, float partialTicks) {
        return this.weaponSystem.getCurrentWeaponShotPitch(entity, partialTicks);
    }

    public boolean supportsDetachedTurretAim() {
        return this.weaponSystem.supportsDetachedTurretAim();
    }

    public boolean isDetachedWeaponAimActive() {
        return this.weaponSystem.isDetachedWeaponAimActive();
    }

    public int getFlareTick() {
        return this.weaponSystem.getFlareTick();
    }

    public boolean isGunnerMode() {
        return this.weaponSystem.isGunnerMode();
    }

    public boolean isRidePlayer() {
        return this.seatManager.isRidePlayer();
    }

    public void unmountEntity() {
        this.seatManager.unmountEntity();
    }

    public void updateSeatsPosition(double px, double py, double pz, boolean setPrevPos) {
        this.seatManager.updateSeatsPosition(px, py, pz, setPrevPos);
    }

    public void newSeats(int seatsNum) {
        this.seatManager.newSeats(seatsNum);
    }

    public void stopUnmountCrew() {
        this.seatManager.stopUnmountCrew();
    }

    public boolean unmountCrew(boolean unmountParachute) {
        return this.seatManager.unmountCrew(unmountParachute);
    }

    public boolean interactFirstSeat(EntityPlayer player) {
        return this.seatManager.interactFirstSeat(player);
    }

    public void mountMobToSeats() {
        this.seatManager.mountMobToSeats();
    }

    public boolean isMountedEntity(@Nullable Entity entity) {
        return this.seatManager.isMountedEntity(entity);
    }

    public void onMountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
        this.seatManager.onMountPlayerSeat(seat, entity);
    }

    public void onUnmountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
        this.seatManager.onUnmountPlayerSeat(seat, entity);
    }

    public void switchNextSeat(Entity entity) {
        this.seatManager.switchNextSeat(entity);
    }

    public void switchPrevSeat(Entity entity) {
        this.seatManager.switchPrevSeat(entity);
    }

    public void unmount(Entity entity) {
        this.seatManager.unmount(entity);
    }

    public int getNumEjectionSeat() {
        return this.seatManager.getNumEjectionSeat();
    }

    public boolean canPutToRack() {
        return this.seatManager.canPutToRack();
    }

    public boolean canDownFromRack() {
        return this.seatManager.canDownFromRack();
    }

    public boolean canRideRack() {
        return this.seatManager.canRideRack();
    }

    public boolean canStartRepelling() {
        return this.seatManager.canStartRepelling();
    }

    public void checkRideRack() {
        this.seatManager.checkRideRack();
    }

    public void resetMoveControls() {
        this.flightPhysics.resetMoveControls();
    }

    public void dropEntityParachute(Entity entity) {
        this.seatManager.dropEntityParachute(entity);
    }

    public boolean isParachuting() {
        return this.isParachuting;
    }

    public void setParachuting(boolean parachuting) {
        this.isParachuting = parachuting;
    }

    public boolean haveFlare() {
        return this.weaponSystem.haveFlare();
    }

    public boolean canUseFlare() {
        return this.weaponSystem.canUseFlare();
    }

    public boolean isFlarePreparation() {
        return this.weaponSystem.isFlarePreparation();
    }

    public boolean isFlareUsing() {
        return this.weaponSystem.isFlareUsing();
    }

    public int getCurrentFlareType() {
        return this.weaponSystem.getCurrentFlareType();
    }

    public boolean useFlare(int type) {
        return this.weaponSystem.useFlare(type);
    }

    public int getChaffUseTime() {
        return this.weaponSystem.getChaffUseTime();
    }

    public MCH_EntityAircraft.WeaponBay[] getWeaponBays() {
        return this.weaponSystem.getWeaponBays();
    }

    public void setUavStation(@Nullable MCH_EntityUavStation uavStation) {
        this.networkSync.setUavStation(uavStation);
    }

    public int getRadarRotate() {
        return this.radarRotate;
    }

    public void applyOnGroundPitch(float factor) {
        if (this.getAcInfo() != null) {
            float ogp = this.getAcInfo().onGroundPitch;
            float pitch = this.getPitch();
            pitch -= ogp;
            pitch *= factor;
            pitch += ogp;
            this.setRotPitch(pitch);
        }

        this.setRotRoll(this.getRoll() * factor);
    }

    public float calcRotYaw(float partialTicks) {
        return this.prevRotationYaw + (this.getYaw() - this.prevRotationYaw) * partialTicks;
    }

    public float calcRotPitch(float partialTicks) {
        return this.prevRotationPitch + (this.getPitch() - this.prevRotationPitch) * partialTicks;
    }

    public float calcRotRoll(float partialTicks) {
        return this.getPrevRotationRoll() + (this.getRoll() - this.getPrevRotationRoll()) * partialTicks;
    }

    protected void setRotation(float y, float p) {
        this.setRotYaw(y % 360.0F);
        this.setRotPitch(p % 360.0F);
    }

    public boolean isInfinityAmmo(Entity player) {
        return this.isCreative(player) || this.getCommonStatus(3);
    }

    public String getKindName() {
        return "";
    }

    public String getEntityType() {
        return "";
    }

    public abstract void changeType(String var1);

    public boolean isTargetDrone() {
        return this.getAcInfo() != null && this.getAcInfo().isTargetDrone;
    }

    public boolean isUAV() {
        return this.getAcInfo() != null && this.getAcInfo().isUAV;
    }

    public boolean isNewUAV() {
        return (getAcInfo() != null && (getAcInfo()).isNewUAV);
    }

    public boolean isAlwaysCameraView() {
        return this.getAcInfo() == null || !this.getAcInfo().alwaysCameraView;
    }

    public float getStealth() {
        return this.getAcInfo() != null ? this.getAcInfo().stealth : 0.0F;
    }

    public MCH_AircraftInventory getGuiInventory() {
        return this.inventory;
    }

    public void openGui(EntityPlayer player) {
        if (!this.world.isRemote && getAcInfo() != null) {
            MCHGuiFactories.aircraft().openGui(player, this);
        }
    }

    public void openContainer(EntityPlayer player) {
        if (!this.world.isRemote && getAcInfo() != null && getInventory().getSlots() > 0) {
            MCHGuiFactories.aircraft().openContainer(player, this);
        }
    }

    public boolean isCreative(@Nullable Entity entity) {
        return entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode || entity instanceof MCH_EntityGunner && ((MCH_EntityGunner) entity).isCreative;
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        IUavStation uavStation = this.getUavStation();
        if (this.isUAV() && uavStation != null) {
            return uavStation.getOperator();
        } else {
            List<Entity> passengers = this.getPassengers();
            return passengers.isEmpty() ? null : passengers.getFirst();
        }
    }

    public int getMaxHP() {
        return this.getAcInfo() != null ? this.getAcInfo().maxHp : 100;
    }

    public int getHP() {
        return Math.max(this.getMaxHP() - this.getDamageTaken(), 0);
    }

    public void destroyAircraft() {
        this.setSearchLight(false);
        this.switchHoveringMode(false);
        this.switchGunnerMode(false);

        for (int i = 0; i < seatManager.getSeatNum() + 1; i++) {
            Entity e = seatManager.getEntityBySeatId(i);
            if (e instanceof EntityPlayer) {
                this.switchCameraMode((EntityPlayer) e, 0);
            }
        }

        if (this.isTargetDrone()) {
            this.setDespawnCount(20 * MCH_Config.DespawnCount.prmInt / 10);
        } else {
            this.setDespawnCount(20 * MCH_Config.DespawnCount.prmInt);
        }

        this.rotDestroyedPitch = this.rand.nextFloat() - 0.5F;
        this.rotDestroyedRoll = (this.rand.nextFloat() - 0.5F) * 0.5F;
        this.rotDestroyedYaw = 0.0F;
        if (this.isUAV() && this.getRiddenByEntity() != null) {
            this.getRiddenByEntity().dismountRidingEntity();
        }

        if (!this.world.isRemote) {
            seatManager.ejectSeat(this.getRiddenByEntity());
            Entity entity = seatManager.getEntityBySeatId(1);
            if (entity != null) {
                seatManager.ejectSeat(entity);
            }

            float dmg = MCH_Config.KillPassengersWhenDestroyed.prmBool ? 100000.0F : 0.001F;
            DamageSource dse = DamageSource.GENERIC;
            if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
                if (this.lastAttackedEntity instanceof EntityPlayer) {
                    dse = DamageSource.causePlayerDamage((EntityPlayer) this.lastAttackedEntity);
                }
            } else {
                dse = DamageSource.causeExplosionDamage(new Explosion(this.world, this.lastAttackedEntity, this.posX, this.posY, this.posZ, 1.0F, false, true));
            }

            Entity riddenByEntity = this.getRiddenByEntity();
            if (riddenByEntity != null) {
                riddenByEntity.attackEntityFrom(dse, dmg);
            }

            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() != null) {
                    seat.getRiddenByEntity().attackEntityFrom(dse, dmg);
                }
            }
        }
    }

    public boolean isDestroyed() {
        return this.getDespawnCount() > 0;
    }

    public boolean isEntityRadarMounted() {
        return this.getAcInfo() != null && this.getAcInfo().isEnableEntityRadar;
    }

    public boolean canFloatWater() {
        return this.getAcInfo() != null && this.getAcInfo().isFloat && !this.isDestroyed();
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        if (this.haveSearchLight() && this.isSearchLightON()) {
            return 240 << 16 | 240;
        }

        BlockPos pos = new BlockPos(this.posX, 0, this.posZ);
        if (!this.world.isBlockLoaded(pos)) {
            return 0;
        }

        double heightOffset = (getEntityBoundingBox().maxY - getEntityBoundingBox().minY) * 0.66;
        float floatOffset = calculateFloatOffset();

        int verticalPos = MathHelper.floor(this.posY + floatOffset + heightOffset);
        int combinedLight = this.world.getCombinedLight(new BlockPos(pos.getX(), verticalPos, pos.getZ()), 0);

        int blockLight = combinedLight & 0xFFFF;
        int skyLight = (combinedLight >> 16) & 0xFFFF;

        updateDynamicBrightness(skyLight);

        return (this.brightness << 16) | blockLight;
    }

    private float calculateFloatOffset() {
        if (this.getAcInfo() == null) return 0.0F;

        if (this.canFloatWater()) {
            return Math.abs(this.getAcInfo().floatOffset) + 1.0F;
        }

        return this.getAcInfo().submergedDamageHeight;
    }

    private void updateDynamicBrightness(int targetSkyLight) {
        if (targetSkyLight < this.brightness) {
            if (this.getCountOnUpdate() % 2 == 0) {
                this.brightness--;
            }
        } else if (targetSkyLight > this.brightness) {
            // Rapidly increase brightness to simulate eye adjustment or light activation
            this.brightness = Math.min(this.brightness + 4, 240);
        }
    }

    @Nullable
    public MCH_AircraftInfo.CameraPosition getCameraPosInfo() {
        if (this.getAcInfo() == null) {
            return null;
        } else {
            Entity player = MCH_Lib.getClientPlayer();
            int sid = this.getSeatIdByEntity(player);
            if (sid == 0 && this.canSwitchCameraPos() && this.getCameraId() > 0 && this.getCameraId() < this.getAcInfo().cameraPosition.size()) {
                return this.getAcInfo().cameraPosition.get(this.getCameraId());
            } else {
                return sid > 0 && sid < getSeatsInfo().length && seatManager.getSeatsInfo()[sid].invCamPos ? seatManager.getSeatsInfo()[sid].getCamPos() : this.getAcInfo().cameraPosition.getFirst();
            }
        }
    }

    public void setCameraId(int cameraId) {
        MCH_Logger.debugLog(true, "MCH_EntityAircraft.setCameraId %d -> %d", this.cameraId, cameraId);
        this.cameraId = cameraId;
    }

    public boolean canSwitchCameraPos() {
        return this.getCameraPosNum() >= 2;
    }

    public int getCameraPosNum() {
        return this.getAcInfo() != null ? this.getAcInfo().cameraPosition.size() : 1;
    }

    public void onAcInfoReloaded() {
        if (this.getAcInfo() != null) {
            this.setSize(this.getAcInfo().bodyWidth, this.getAcInfo().bodyHeight);
        }
    }

    public void writeSpawnData(ByteBuf buffer) {
        if (this.getAcInfo() != null) {
            buffer.writeFloat(this.getAcInfo().bodyHeight);
            buffer.writeFloat(this.getAcInfo().bodyWidth);
            buffer.writeFloat(this.getAcInfo().thirdPersonDist);
            byte[] name = this.getTypeName().getBytes();
            buffer.writeShort(name.length);
            buffer.writeBytes(name);
        } else {
            buffer.writeFloat(this.height);
            buffer.writeFloat(this.width);
            buffer.writeFloat(4.0F);
            buffer.writeShort(0);
        }
    }

    public void readSpawnData(ByteBuf data) {
        try {
            float height = data.readFloat();
            float width = data.readFloat();
            this.setSize(width, height);
            this.thirdPersonDist = data.readFloat();
            int len = data.readShort();
            if (len > 0) {
                byte[] dst = new byte[len];
                data.readBytes(dst);
                this.changeType(new String(dst));
            }
        } catch (Exception var6) {
            MCH_Logger.log(this, "readSpawnData error!");
            var6.printStackTrace();
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.setDespawnCount(nbt.getInteger("AcDespawnCount"));
        this.setTextureName(nbt.getString("TextureName"));
        this.setCommonUniqueId(nbt.getString("AircraftUniqueId"));
        this.setRotRoll(nbt.getFloat("AcRoll"));
        this.setPrevRotationRoll(this.getRoll());
        this.prevLastRiderYaw = this.lastRiderYaw = nbt.getFloat("AcLastRYaw");
        this.prevLastRiderPitch = this.lastRiderPitch = nbt.getFloat("AcLastRPitch");
        this.setPartStatus(nbt.getInteger("PartStatus"));
        this.setTypeName(nbt.getString("TypeName"));
        super.readEntityFromNBT(nbt);

        for (IAircraftComponent component : this.components) {
            component.readFromNBT(nbt);
        }
        this.getGuiInventory().readFromNBT(nbt);
        this.setCommandForce(nbt.getString("AcCommand"));
        this.setGunnerStatus(nbt.getBoolean("AcGunnerStatus"));
        int[] wa_list = nbt.getIntArray("AcWeaponsAmmo");

        for (int i = 0; i < wa_list.length; i++) {
            this.getWeapon(i).setReserveAmmo(wa_list[i]);
            this.getWeapon(i).reloadMag();
        }

        if (this.getDespawnCount() > 0) {
            this.setDamageTaken(this.getMaxHP());
        } else if (nbt.hasKey("AcDamage")) {
            this.setDamageTaken(nbt.getInteger("AcDamage"));
        }

        if (this.haveSearchLight() && nbt.hasKey("SearchLight")) {
            this.setSearchLight(nbt.getBoolean("SearchLight"));
        }

        seatManager.setDismountedUserCtrl(nbt.getBoolean("AcDismounted"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setString("TextureName", this.getTextureName());
        nbt.setString("AircraftUniqueId", this.getCommonUniqueId());
        nbt.setString("TypeName", this.getTypeName());
        nbt.setInteger("PartStatus", this.getPartStatus() & this.getLastPartStatusMask());
        nbt.setInteger("AcDespawnCount", this.getDespawnCount());
        nbt.setFloat("AcRoll", this.getRoll());
        nbt.setBoolean("SearchLight", this.isSearchLightON());

        nbt.setString("AcCommand", this.getCommand());
        if (!nbt.hasKey("AcGunnerStatus")) {
            this.setGunnerStatus(true);
        }

        nbt.setBoolean("AcGunnerStatus", this.getGunnerStatus());
        super.writeEntityToNBT(nbt);

        for (IAircraftComponent component : this.components) {
            component.writeToNBT(nbt);
        }
        this.getGuiInventory().writeToNBT(nbt);
        int[] wa_list = new int[this.getWeaponNum()];

        for (int i = 0; i < wa_list.length; i++) {
            wa_list[i] = this.getWeapon(i).getRestAllAmmoNum() + this.getWeapon(i).getAmmo();
        }

        nbt.setTag("AcWeaponsAmmo", W_NBTTag.newTagIntArray("AcWeaponsAmmo", wa_list));
        nbt.setInteger("AcDamage", this.getDamageTaken());
        nbt.setBoolean("AcDismounted", seatManager.isDismountedUserCtrl());
    }

    @Override
    public boolean attackEntityFrom(@NotNull DamageSource damageSource, float originalDamage) {
        if (this.weaponSystem.isIronCurtainActive()) {
            return false;
        }
        if (shouldIgnoreDamage(damageSource)) {
            return false;
        }

        String type = damageSource.getDamageType();
        Entity attacker = damageSource.getTrueSource();
        boolean isPlayerAttack = false;
        boolean playDamageSound = !(attacker instanceof EntityPlayer);

        // Client side just returns true
        if (this.world.isRemote) {
            return true;
        }

        // Calculate base damage
        float damage = calculateDamage(damageSource, originalDamage, this.lastBBDamageFactor);
        this.lastBBDamageFactor = 1.0F;

        if (attacker instanceof EntityLivingBase) {
            this.lastAttackedEntity = attacker;
        }

        if (attacker instanceof EntityPlayer player) {
            isPlayerAttack = evaluatePlayerAttack(player, type, damage);
            playDamageSound = true;
        }
        if (isPlayerAttack && attacker instanceof EntityPlayer player && player.capabilities.isCreativeMode) {
            this.setDead(true);
            playDamageSound();
            return true;
        }

        if (damage <= 0.0F) {
            return false;
        }

        if (!this.isDestroyed()) {
            if (!isPlayerAttack) {
                applyDamage(damageSource, damage, type, damage);
            }

            this.markVelocityChanged();
            if (this.getDamageTaken() >= this.getMaxHP() || isPlayerAttack) {
                handleDestruction(damageSource, attacker, type, isPlayerAttack);
            }
        }

        if (playDamageSound) {
            playDamageSound();
        }

        return true;
    }

    private boolean shouldIgnoreDamage(DamageSource src) {
        String type = src.getDamageType();
        return this.isEntityInvulnerable(src) || this.isDead || this.timeSinceHit > 0 || type.equalsIgnoreCase("inFire") || type.equalsIgnoreCase("cactus");
    }

    private float calculateDamage(DamageSource src, float base, float factor) {
        float damage = MCH_Config.applyDamageByExternal(this, src, base);

        if (this.getAcInfo() != null && this.getAcInfo().invulnerable) {
            return 0.0F;
        }

        if (src == DamageSource.OUT_OF_WORLD) {
            this.setDead();
        }

        if (!MCH_Multiplay.canAttackEntity(src, this)) {
            return 0.0F;
        }

        String type = src.getDamageType();

        if (type.equalsIgnoreCase("lava")) {
            damage *= this.rand.nextInt(8) + 2;
            this.timeSinceHit = 2;
        } else if (type.startsWith("explosion")) {
            this.timeSinceHit = 1;
        } else if (type.equalsIgnoreCase("onFire")) {
            this.timeSinceHit = 10;
        }

        MCH_AircraftInfo acInfo = this.getAcInfo();
        if (acInfo != null && !type.equalsIgnoreCase("lava") && !type.equalsIgnoreCase("onFire")) {
            damage = Math.min(damage, acInfo.armorMaxDamage);
            if (factor <= 1.0F) {
                damage *= factor;
            }
            damage *= acInfo.armorDamageFactor;
            damage -= acInfo.armorMinDamage;

            if (damage <= 0.0F) {
                return 0.0F;
            }

            if (factor > 1.0F) {
                damage *= factor;
            }
        }

        return damage;
    }

    private boolean evaluatePlayerAttack(EntityPlayer player, String type, float damage) {
        boolean creative = player.capabilities.isCreativeMode;

        if (type.equalsIgnoreCase("player")) {
            if (creative) {
                return true;
            }
            if (this.getAcInfo() != null && !this.getAcInfo().creativeOnly && !MCH_Config.PreventingBroken.prmBool) {
                if (MCH_Config.BreakableOnlyPickaxe.prmBool) {
                    if (!player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() instanceof ItemPickaxe) {
                        return true;
                    }
                } else {
                    return !this.isRidePlayer();
                }
            }
        }

        W_WorldFunc.playSoundAt(this, "hit", damage > 0.0F ? 1.0F : 0.5F, 1.0F);
        return false;
    }

    private void applyDamage(DamageSource src, float damage, String type, float factor) {
        MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.attackEntityFrom:damage=%.1f(factor=%.2f):%s", damage, factor, type);
        this.setDamageTaken(this.getDamageTaken() + (int) damage);
    }

    private void handleDestruction(DamageSource src, Entity attacker, String type, boolean byPlayer) {
        if (!byPlayer) {
            this.setDamageTaken(this.getMaxHP());
            this.destroyAircraft();
            this.timeSinceHit = 20;

            String cmd = this.getCommand().trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            if (!cmd.isEmpty()) {
                MCH_DummyCommandSender.execCommand(cmd);
            }

            if (type.equalsIgnoreCase("inWall")) {
                this.explosionByCrash(0.0);
                this.damageSinceDestroyed = this.getMaxHP();
            } else {
                MCH_Explosion.newExplosion(this.world, null, attacker, this.posX, this.posY, this.posZ, 2.0F, 2.0F, true, true, true,
                        true, 5, null);
            }
        } else {
            dropItemIfNeeded((EntityPlayer) attacker);
            this.setDead(true);
        }
    }

    private void dropItemIfNeeded(EntityPlayer player) {
        if (this.getAcInfo() != null && this.getAcInfo().getItem() != null) {
            boolean creative = player.capabilities.isCreativeMode;
            boolean sneaking = player.isSneaking();

            if (creative) {
                if (MCH_Config.DropItemInCreativeMode.prmBool && !sneaking) {
                    this.dropItemWithOffset(this.getAcInfo().getItem(), 1, 0.0F);
                }
                if (!MCH_Config.DropItemInCreativeMode.prmBool && sneaking) {
                    this.dropItemWithOffset(this.getAcInfo().getItem(), 1, 0.0F);
                }
            } else {
                this.dropItemWithOffset(this.getAcInfo().getItem(), 1, 0.0F);
            }
        }
    }

    private void playDamageSound() {
        W_WorldFunc.playSoundAt(this, "helidmg", 1.0F, 0.9F + this.rand.nextFloat() * 0.1F);
    }

    public boolean isExploded() {
        return this.isDestroyed() && this.damageSinceDestroyed > this.getMaxHP() / 10 + 1;
    }

    public void destruct() {
        if (this.getRiddenByEntity() != null) {
            this.getRiddenByEntity().dismountRidingEntity();
        }

        this.setDead(true);
    }

    @Nullable
    public EntityItem entityDropItem(ItemStack is, float par2) {
        if (is.getCount() == 0) {
            return null;
        } else {
            this.setAcDataToItem(is);
            return super.entityDropItem(is, par2);
        }
    }

    public void setAcDataToItem(ItemStack is) {
        if (!is.hasTagCompound()) {
            is.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound nbt = is.getTagCompound();
        if (nbt == null) return;
        nbt.setString("MCH_Command", this.getCommand());
        if (MCH_Config.ItemFuel.prmBool) {
            nbt.setInteger("MCH_Fuel", this.fuelComponent.getFuel());
        }

        if (MCH_Config.ItemDamage.prmBool) {
            is.setItemDamage(this.getDamageTaken());
        }
    }

    public void getAcDataFromItem(ItemStack is) {
        if (is.hasTagCompound()) {
            NBTTagCompound nbt = is.getTagCompound();
            if (nbt == null) return;
            this.setCommandForce(nbt.getString("MCH_Command"));
            if (MCH_Config.ItemFuel.prmBool) {
                fuelComponent.setFuel(nbt.getInteger("MCH_Fuel"));
            }

            if (MCH_Config.ItemDamage.prmBool) {
                this.setDamageTaken(is.getMetadata());
            }
        }
    }

    @Override
    public boolean isUsableByPlayer(@NotNull EntityPlayer player) {
        if (this.isUAV()) {
            return super.isUsableByPlayer(player);
        } else if (!this.isDead) {
            return this.getSeatIdByEntity(player) >= 0 ? player.getDistanceSq(this) <= 4096.0 : player.getDistanceSq(this) <= 64.0;
        } else {
            return false;
        }
    }

    public void applyEntityCollision(@NotNull Entity par1Entity) {
    }

    public void addVelocity(double par1, double par3, double par5) {
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double par1, double par3, double par5) {
        this.flightPhysics.setVelocity(par1, par3, par5);
    }

    public void onFirstUpdate() {
        if (!this.world.isRemote) {
            this.setCommonStatus(3, MCH_Config.InfinityAmmo.prmBool);
            this.setCommonStatus(4, MCH_Config.InfinityFuel.prmBool);
        }
    }

    public void onRidePilotFirstUpdate() {
        if (this.world.isRemote && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            this.updateClientSettings(0);
        }

        Entity pilot = this.getRiddenByEntity();
        if (pilot != null) {
            pilot.rotationYaw = seatManager.getLastRiderYaw();
            pilot.rotationPitch = seatManager.getLastRiderPitch();
        }

        this.keepOnRideRotation = false;
        if (this.getAcInfo() != null) {
            this.switchFreeLookModeClient(this.getAcInfo().defaultFreelook);
        }
    }

    public void addCurrentThrottle(double throttle) {
        this.setCurrentThrottle(this.getCurrentThrottle() + throttle);
    }

    public boolean canMouseRot() {
        return !this.isDead && this.getRiddenByEntity() != null && !this.isDestroyed();
    }

    public boolean canUpdateYaw(Entity player) {
        if (this.getRidingEntity() != null) {
            return false;
        } else {
            return this.getCountOnUpdate() >= 30 && MCH_Lib.getBlockIdY(this, 3, -2) == 0;
        }
    }

    public boolean canUpdatePitch(Entity player) {
        return this.getCountOnUpdate() >= 30 && MCH_Lib.getBlockIdY(this, 3, -2) == 0;
    }

    public boolean canUpdateRoll(Entity player) {
        if (this.getRidingEntity() != null) {
            return false;
        } else {
            return this.getCountOnUpdate() >= 30 && MCH_Lib.getBlockIdY(this, 3, -2) == 0;
        }
    }

    public boolean isOverridePlayerYaw() {
        return !this.isFreeLookMode();
    }

    public boolean isOverridePlayerPitch() {
        return !this.isFreeLookMode();
    }

    public double getAddRotationYawLimit() {
        return this.getAcInfo() != null ? 40.0 * this.getAcInfo().mobilityYaw : 40.0;
    }

    public double getAddRotationPitchLimit() {
        return this.getAcInfo() != null ? 40.0 * this.getAcInfo().mobilityPitch : 40.0;
    }

    public double getAddRotationRollLimit() {
        return this.getAcInfo() != null ? 40.0 * this.getAcInfo().mobilityRoll : 40.0;
    }

    public float getYawFactor() {
        return 1.0F;
    }

    public float getPitchFactor() {
        return 1.0F;
    }

    public float getRollFactor() {
        return 1.0F;
    }

    public abstract void onUpdateAngles(float var1);

    public float getControlRotYaw(float mouseX, float mouseY, float tick) {
        return 0.0F;
    }

    public float getControlRotPitch(float mouseX, float mouseY, float tick) {
        return 0.0F;
    }

    public float getControlRotRoll(float mouseX, float mouseY, float tick) {
        return 0.0F;
    }

    public void setAngles(Entity player, boolean fixRot, float fixYaw, float fixPitch, float deltaX, float deltaY, float inputX, float inputY, float deltaSeconds) {

        float prevPitch = this.getPitch();
        float prevYaw = this.getYaw();
        float prevRoll = this.getRoll();

        if (this.isFreeLookMode()) {
            inputX = 0.0F;
            inputY = 0.0F;
        }

        float yaw = 0.0F;
        float pitch = 0.0F;
        float roll = 0.0F;

        //YAW
        if (this.canUpdateYaw(player)) {
            double limit = this.getAddRotationYawLimit();
            yaw = this.getControlRotYaw(inputX, inputY, 1.0F);
            yaw = (float) MathHelper.clamp(yaw, -limit, limit);
            yaw *= this.getYawFactor() * deltaSeconds;
        }

        //PITCH
        if (this.canUpdatePitch(player)) {
            double limit = this.getAddRotationPitchLimit();
            pitch = this.getControlRotPitch(inputX, inputY, 1.0F);
            pitch = (float) MathHelper.clamp(pitch, -limit, limit);
            pitch *= -this.getPitchFactor() * deltaSeconds;
        }

        //ROLL
        if (this.canUpdateRoll(player)) {
            double limit = this.getAddRotationRollLimit();
            roll = this.getControlRotRoll(inputX, inputY, 1.0F);
            roll = (float) MathHelper.clamp(roll, -limit, limit);
            roll *= this.getRollFactor() * deltaSeconds;
        }


        MCH_Math.FMatrix m = MCH_Math.newMatrix();

        MCH_Math.MatTurnZ(m, roll * ((float) Math.PI / 180.0F));
        MCH_Math.MatTurnX(m, pitch * ((float) Math.PI / 180.0F));
        MCH_Math.MatTurnY(m, yaw * ((float) Math.PI / 180.0F));

        MCH_Math.MatTurnZ(m, this.getRoll() * ((float) Math.PI / 180.0F));
        MCH_Math.MatTurnX(m, this.getPitch() * ((float) Math.PI / 180.0F));
        MCH_Math.MatTurnY(m, this.getYaw() * ((float) Math.PI / 180.0F));

        MCH_Math.FVector3D v = MCH_Math.MatrixToEuler(m);

        //LIMITS
        assert this.getAcInfo() != null;
        if (this.getAcInfo().limitRotation) {
            v.x = MCH_Lib.RNG(v.x, this.getAcInfo().minRotationPitch, this.getAcInfo().maxRotationPitch);
            v.z = MCH_Lib.RNG(v.z, this.getAcInfo().minRotationRoll, this.getAcInfo().maxRotationRoll);
        }

        if (v.z > 180.0F) v.z -= 360.0F;
        if (v.z < -180.0F) v.z += 360.0F;

        this.setRotYaw(v.y);
        this.setRotPitch(v.x);
        this.setRotRoll(v.z);

        this.onUpdateAngles(deltaSeconds);

        //POST LIM CLAMP
        assert this.getAcInfo() != null;
        if (this.getAcInfo().limitRotation) {
            this.setRotPitch(MCH_Lib.RNG(this.getPitch(), this.getAcInfo().minRotationPitch, this.getAcInfo().maxRotationPitch));

            this.setRotRoll(MCH_Lib.RNG(this.getRoll(), this.getAcInfo().minRotationRoll, this.getAcInfo().maxRotationRoll));
        }

        //CHECKS
        if (MathHelper.abs(this.getPitch()) > 90.0F) {
            MCH_Logger.debugLog(true, "MCH_EntityAircraft.setAngles Error:Pitch=%.1f", this.getPitch());
        }

        if (this.getRoll() > 180.0F) this.setRotRoll(this.getRoll() - 360.0F);
        if (this.getRoll() < -180.0F) this.setRotRoll(this.getRoll() + 360.0F);

        this.setPrevRotationRoll(this.getRoll());
        this.prevRotationPitch = this.getPitch();
        if (this.getRidingEntity() == null) {
            this.prevRotationYaw = this.getYaw();
        }

        //SYNC
        if (!this.isOverridePlayerYaw() && !fixRot) {
            player.turn(deltaX, 0.0F);
        } else {
            if (this.getRidingEntity() == null) {
                player.prevRotationYaw = this.getYaw() + (fixRot ? fixYaw : 0.0F);
            } else {
                if (this.getYaw() - player.rotationYaw > 180.0F) player.prevRotationYaw += 360.0F;
                if (this.getYaw() - player.rotationYaw < -180.0F) player.prevRotationYaw -= 360.0F;
            }
            player.rotationYaw = this.getYaw() + (fixRot ? fixYaw : 0.0F);
        }

        if (!this.isOverridePlayerPitch() && !fixRot) {
            player.turn(0.0F, deltaY);
        } else {
            player.prevRotationPitch = this.getPitch() + (fixRot ? fixPitch : 0.0F);
            player.rotationPitch = this.getPitch() + (fixRot ? fixPitch : 0.0F);
        }

        //CHANGE FLAG
        if ((this.getRidingEntity() == null && prevYaw != this.getYaw()) || prevPitch != this.getPitch() || prevRoll != this.getRoll()) {
            this.setAircraftRotChanged(true);
        }
    }

    public boolean canSwitchSearchLight(Entity entity) {
        return this.haveSearchLight() && this.getSeatIdByEntity(entity) <= 1;
    }

    public boolean isSearchLightON() {
        return this.getCommonStatus(6);
    }

    public void setSearchLight(boolean onoff) {
        this.setCommonStatus(6, onoff);
    }

    public boolean isRadarEnabledRuntime() {
        return this.getCommonStatus(NetworkSyncComponent.CMN_ID_RADAR_ENABLED);
    }

    public void setRadarEnabledRuntime(boolean enabled) {
        this.setRadarEnabledRuntime(enabled, false);
    }

    public void setRadarEnabledRuntime(boolean enabled, boolean writeClient) {
        this.setCommonStatus(NetworkSyncComponent.CMN_ID_RADAR_ENABLED, enabled, writeClient);
    }

    public boolean isECMJammerUsing() {
        return this.weaponSystem.isECMJammerUsing();
    }

    public void setECMJammerUsing(boolean enabled) {
        this.weaponSystem.setECMJammerUsing(enabled);
    }

    public boolean useECMJammer() {
        return this.weaponSystem.useECMJammer();
    }

    public void setMortarRadarEnabledRuntime(boolean enabled) {
        this.setCommonStatus(NetworkSyncComponent.CMN_ID_MORTAR_RADAR_ENABLED, enabled, true);
    }

    public boolean haveSearchLight() {
        return this.getAcInfo() != null && !this.getAcInfo().searchLights.isEmpty();
    }

    public float getSearchLightValue(Entity target) {
        var aircraftInfo = getAcInfo();
        if (!haveSearchLight() || !isSearchLightON() || aircraftInfo == null) {
            return 0.0F;
        }

        for (MCH_AircraftInfo.SearchLight searchLight : aircraftInfo.searchLights) {
            Vec3d searchLightWorldPos = getTransformedPosition(searchLight.pos);

            double deltaX = target.posX - searchLightWorldPos.x;
            double deltaY = target.posY - searchLightWorldPos.y;
            double deltaZ = target.posZ - searchLightWorldPos.z;

            double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
            double maxDistanceSquared = searchLight.height * searchLight.height + 20.0;

            if (distanceSquared <= 2.0 || distanceSquared >= maxDistanceSquared) {
                continue;
            }

            double horizontalAngle;
            double verticalAngle;

            double angrad = Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
            if (!searchLight.fixDir) {
                Vec3d lightDirection = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -lastSearchLightYaw + searchLight.yaw, -lastSearchLightPitch + searchLight.pitch, -getRoll());

                horizontalAngle = MCH_Lib.getPosAngle(lightDirection.x, lightDirection.z, deltaX, deltaZ);
                verticalAngle = Math.abs(Math.toDegrees(angrad) + lastSearchLightPitch + searchLight.pitch);
            } else {
                float steeringRotation = searchLight.steering ? this.getRotYawWheel() * searchLight.stRot : 0.0F;

                Vec3d lightDirection = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -getYaw() + searchLight.yaw + steeringRotation, -getPitch() + searchLight.pitch, -getRoll());

                horizontalAngle = MCH_Lib.getPosAngle(lightDirection.x, lightDirection.z, deltaX, deltaZ);
                verticalAngle = Math.abs(Math.toDegrees(angrad) + getPitch() + searchLight.pitch);
            }

            float effectiveAngleLimit = searchLight.angle * 3.0F;

            if (horizontalAngle < effectiveAngleLimit && verticalAngle < effectiveAngleLimit) {
                float intensity = 0.0F;

                if (horizontalAngle + verticalAngle < effectiveAngleLimit) {
                    intensity = (float) (1440.0 * (1.0 - (horizontalAngle + verticalAngle) / effectiveAngleLimit));
                }

                return Math.min(intensity, 240.0F);
            }
        }

        return 0.0F;
    }


    public abstract void onUpdateAircraft();

    public void onUpdate() {
        this.flightPhysics.beginUpdate();
        this.lastBBDamageFactor = 1.0F;
        this.onUpdate_RidingEntity();

        //Unmount iterator
        seatManager.iterateUnmount();

        //Apply damage to passengers in destroyed vehicle
        if (this.isDestroyed() && this.getCountOnUpdate() % 20 == 0) {
            for (int sid = 0; sid < seatManager.getSeatNum() + 1; sid++) {
                Entity entity = seatManager.getEntityBySeatId(sid);
                if (entity != null && (sid != 0 || !this.isUAV()) && MCH_Config.applyDamageVsEntity(entity, DamageSource.IN_FIRE, 1.0F) > 0.0F) {
                    entity.setFire(5);
                }
            }
        }

        //Apply raotation
        if ((this.isAircraftRotChanged() || this.isAircraftRollRev()) && this.world.isRemote && this.getRiddenByEntity() != null) {
            PacketIndRotation.send(this);
            this.setAircraftRotChanged(false);
            this.setAircraftRollRev(false);
        }

        //Apply roll
        this.networkSync.syncServerRoll();
        this.setPrevRotationRoll(this.getRoll());

        //Target drone specific
        if (!this.world.isRemote && this.isTargetDrone() && !this.isDestroyed() && this.getCountOnUpdate() > 20 && !fuelComponent.canUseFuel()) {
            this.setDamageTaken(this.getMaxHP());
            this.destroyAircraft();
            MCH_Explosion.newExplosion(this.world, null, null, this.posX, this.posY, this.posZ, 2.0F, 2.0F, true, true, true, true, 5);
        }

        //Despawn logic – client
        if (this.world.isRemote && this.getAcInfo() != null && this.getHP() <= 0 && this.getDespawnCount() <= 0) {
            this.destroyAircraft();
        }

        //Despawn logic – server
        if (!this.world.isRemote && this.getDespawnCount() > 0) {
            this.setDespawnCount(this.getDespawnCount() - 1);
            if (this.getDespawnCount() <= 1) {
                this.setDead(true);
            }
        }

        super.onUpdate();


        //Per part update
        if (this.getParts() != null) {
            for (Entity e : this.getParts()) {
                if (e != null) {
                    e.onUpdate();
                }
            }
        }
        for (IAircraftComponent component : this.components) {
            component.onUpdate();
        }

        this.updateNoCollisionEntities();
        this.networkSync.updateUavStation();
        this.supplyAmmoToOtherAircraft();
        this.repairOtherAircraft();

        if (this.modeSwitchCooldown > 0) {
            this.modeSwitchCooldown--;
        }

        if (this.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
            this.onRidePilotFirstUpdate();
        }

        if (this.countOnUpdate == 0) {
            this.onFirstUpdate();
        }

        this.countOnUpdate++;
        if (this.countOnUpdate >= 1000000) {
            this.countOnUpdate = 1;
        }

        this.fallDistance = 0.0F;
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            riddenByEntity.fallDistance = 0.0F;
        }

        if (this.soundUpdater != null) {
            this.soundUpdater.update();
        }

        if (this.getTowChainEntity() != null && this.getTowChainEntity().isDead) {
            this.setTowChainEntity(null);
        }

        this.updateSupplyAmmo();
        this.autoRepair();
        int ft = this.getFlareTick();

        if (!this.world.isRemote && this.getFlareTick() == 0 && ft != 0) {
            this.setCommonStatus(0, false);
        }

        Entity ex = this.getRiddenByEntity();
        if (ex != null && !ex.isDead && !this.isDestroyed()) {
            this.lastRiderYaw = ex.rotationYaw;
            this.prevLastRiderYaw = ex.prevRotationYaw;
            this.lastRiderPitch = ex.rotationPitch;
            this.prevLastRiderPitch = ex.prevRotationPitch;
        } else if (this.getTowedChainEntity() != null || this.getRidingEntity() != null) {
            this.lastRiderYaw = this.rotationYaw;
            this.prevLastRiderYaw = this.prevRotationYaw;
            this.lastRiderPitch = this.rotationPitch;
            this.prevLastRiderPitch = this.prevRotationPitch;
        }

        this.updatePartCameraRotate();
        this.updatePartWheel();
        this.updatePartCrawlerTrack();
        this.updatePartLightHatch();
        this.regenerationMob();
        if (this.getRiddenByEntity() == null && this.lastRiddenByEntity != null) {
            this.unmountEntity();
        }

        this.updateExtraBoundingBox();
        boolean prevOnGround = this.onGround;
        double prevMotionY = this.motionY;
        this.onUpdateAircraft();
        if (this.getAcInfo() != null) {
            this.updateParts(this.getPartStatus());
        }

        if (this.weaponSystem.recoilCount > 0) {
            this.weaponSystem.recoilCount--;
        }

        if (!W_Entity.isEqual(MCH_MOD.proxy.getClientPlayer(), this.getRiddenByEntity())) {
            this.weaponSystem.updateRecoil(1.0F);
        }

        if (!this.world.isRemote && this.isDestroyed() && !this.isExploded() && !prevOnGround && this.onGround && prevMotionY < -0.2) {
            this.explosionByCrash(prevMotionY);
            this.damageSinceDestroyed = this.getMaxHP();
        }

        this.onUpdate_PartRotation();
        this.onUpdate_ParticleSmoke();
        this.updateSeatsPosition(this.posX, this.posY, this.posZ, false);
        this.updateHitBoxPosition();
        this.onUpdate_CollisionGroundDamage();
        this.onUpdate_UnmountCrew();
        seatManager.onUpdate_Repelling();
        seatManager.checkRideRack();
        if (this.lastRidingEntity == null && this.getRidingEntity() != null) {
            this.onRideEntity(this.getRidingEntity());
        }

        this.lastRiddenByEntity = this.getRiddenByEntity();
        this.lastRidingEntity = this.getRidingEntity();
    }

    private void updateNoCollisionEntities() {
        if (!this.world.isRemote) {
            if (this.getCountOnUpdate() % 10 == 0) {
                for (int i = 0; i < 1 + seatManager.getSeatNum(); i++) {
                    Entity e = seatManager.getEntityBySeatId(i);
                    if (e != null) {
                        this.noCollisionEntities.put(e, 8);
                    }
                }

                if (this.getTowChainEntity() != null && this.getTowChainEntity().towedEntity != null) {
                    this.noCollisionEntities.put(this.getTowChainEntity().towedEntity, 60);
                }

                if (this.getTowedChainEntity() != null && this.getTowedChainEntity().towEntity != null) {
                    this.noCollisionEntities.put(this.getTowedChainEntity().towEntity, 60);
                }

                if (this.getRidingEntity() instanceof MCH_EntitySeat) {
                    MCH_EntityAircraft ac = ((MCH_EntitySeat) this.getRidingEntity()).getParent();
                    if (ac != null) {
                        this.noCollisionEntities.put(ac, 60);
                    }
                } else if (this.getRidingEntity() != null) {
                    this.noCollisionEntities.put(this.getRidingEntity(), 60);
                }

                this.noCollisionEntities.replaceAll((k, _) -> this.noCollisionEntities.get(k) - 1);

                this.noCollisionEntities.values().removeIf(integer -> integer <= 0);
            }
        }
    }

    public void updateControl() {
        this.flightPhysics.updateControl();
    }

    public void updateRecoil(float partialTicks) {
        this.weaponSystem.updateRecoil(partialTicks);
    }

    private void updatePartLightHatch() {
        this.prevRotLightHatch = this.rotLightHatch;
        if (this.isSearchLightON()) {
            this.rotLightHatch = (float) (this.rotLightHatch + 0.5);
        } else {
            this.rotLightHatch = (float) (this.rotLightHatch - 0.5);
        }

        if (this.rotLightHatch > 1.0F) {
            this.rotLightHatch = 1.0F;
        }

        if (this.rotLightHatch < 0.0F) {
            this.rotLightHatch = 0.0F;
        }
    }

    public void updateExtraBoundingBox() {
        for (MCH_BoundingBox bb : this.extraBoundingBox) {
            bb.updatePosition(this.posX, this.posY, this.posZ, this.getYaw(), this.getPitch(), this.getRoll());
        }
    }

    public void updatePartWheel() {
        this.flightPhysics.updatePartWheel();
    }


    public void updatePartCrawlerTrack() {
        this.flightPhysics.updatePartCrawlerTrack();
    }


    public void checkServerNoMove() {
        this.flightPhysics.checkServerNoMove();
    }

    public boolean haveRotPart() {
        return this.world.isRemote && this.getAcInfo() != null && this.rotPartRotation.length > 0 && this.rotPartRotation.length == this.getAcInfo().partRotPart.size();
    }

    public void onUpdate_PartRotation() {
        if (this.haveRotPart() && this.getAcInfo() != null) {
            for (int i = 0; i < this.rotPartRotation.length; i++) {
                this.prevRotPartRotation[i] = this.rotPartRotation[i];
                if (!this.isDestroyed() && this.getAcInfo().partRotPart.get(i).rotAlways || this.getRiddenByEntity() != null) {
                    this.rotPartRotation[i] = this.rotPartRotation[i] + this.getAcInfo().partRotPart.get(i).rotSpeed;
                    if (this.rotPartRotation[i] < 0.0F) {
                        this.rotPartRotation[i] = this.rotPartRotation[i] + 360.0F;
                    }

                    if (this.rotPartRotation[i] >= 360.0F) {
                        this.rotPartRotation[i] = this.rotPartRotation[i] - 360.0F;
                    }
                }
            }
        }
    }

    public void onRideEntity(Entity ridingEntity) {
    }

    public boolean canRepell() {
        return this.isRepelling() && seatManager.getTickRepelling() > 50;
    }


    private void onUpdate_UnmountCrew() {
        if (this.getAcInfo() != null) {
            if (this.isParachuting) {
                if (MCH_Lib.getBlockIdY(this, 3, -10) != 0) {
                    this.stopUnmountCrew();
                } else if ((!this.haveHatch() || this.getHatchRotation() > 89.0F) && this.getCountOnUpdate() % this.getAcInfo().mobDropOption.interval == 0 && !this.unmountCrew(true)) {
                    this.stopUnmountCrew();
                }
            }
        }
    }


    public boolean canParachute(Entity entity) {
        if (this.getAcInfo() == null || !this.getAcInfo().isEnableParachuting || this.getSeatIdByEntity(entity) <= 1 || MCH_Lib.getBlockIdY(this, 3, -13) != 0) {
            return false;
        } else {
            return this.getSeatIdByEntity(entity) > 1;
        }
    }


    public void explosionByCrash(double prevMotionY) {
        float exp = this.getAcInfo() != null ? this.getAcInfo().maxFuel / 400.0F : 2.0F;
        if (exp < 1.0F) {
            exp = 1.0F;
        }

        if (exp > 15.0F) {
            exp = 15.0F;
        }

        MCH_Logger.debugLog(this.world, "OnGroundAfterDestroyed:motionY=%.3f", (float) prevMotionY);
        MCH_Explosion.newExplosion(this.world, null, null, this.posX, this.posY, this.posZ, exp, exp >= 2.0F ? exp * 0.5F : 1.0F, true, true, true, true, 5);
    }

    public void onUpdate_CollisionGroundDamage() {
        if (!this.isDestroyed()) {
            if (MCH_Lib.getBlockIdY(this, 3, -3) > 0 && !this.world.isRemote) {
                float roll = MathHelper.abs(MathHelper.wrapDegrees(this.getRoll()));
                float pitch = MathHelper.abs(MathHelper.wrapDegrees(this.getPitch()));
                if (roll > this.getGiveDamageRot() || pitch > this.getGiveDamageRot()) {
                    float dmg = MathHelper.abs(roll) + MathHelper.abs(pitch);
                    if (dmg < 90.0F) {
                        dmg *= 0.4F * (float) this.getDistance(this.prevPosX, this.prevPosY, this.prevPosZ);
                    } else {
                        dmg *= 0.4F;
                    }

                    if (dmg > 1.0F && this.rand.nextInt(4) == 0) {
                        this.attackEntityFrom(DamageSource.IN_WALL, dmg);
                    }
                }
            }

            if (this.getCountOnUpdate() % 30 == 0 && (this.getAcInfo() == null || !this.getAcInfo().isFloat) && MCH_Lib.isBlockInWater(this.world, (int) (this.posX + 0.5), (int) (this.posY + 1.5 + this.getAcInfo().submergedDamageHeight), (int) (this.posZ + 0.5))) {
                int hp = this.getMaxHP() / 10;
                if (hp <= 0) {
                    hp = 1;
                }

                this.attackEntityFrom(DamageSource.IN_WALL, hp);
            }
        }
    }

    public float getGiveDamageRot() {
        return 40.0F;
    }

    public void applyServerPositionAndRotation() {
        this.flightPhysics.applyServerPositionAndRotation();
    }

    protected void autoRepair() {
        if (this.timeSinceHit > 0) {
            this.timeSinceHit--;
        }

        if (this.getMaxHP() > 0) {
            if (!this.isDestroyed()) {
                if (this.getDamageTaken() > this.beforeDamageTaken) {
                    this.repairCount = 600;
                } else if (this.repairCount > 0) {
                    this.repairCount--;
                } else {
                    this.repairCount = 40;
                    double hpp = (double) this.getHP() / this.getMaxHP();
                    if (hpp >= MCH_Config.AutoRepairHP.prmDouble) {
                        this.repair(this.getMaxHP() / 100);
                    }
                }
            }

            this.beforeDamageTaken = this.getDamageTaken();
        }
    }

    public boolean repair(int tpd) {
        if (tpd < 1) {
            tpd = 1;
        }

        int damage = this.getDamageTaken();
        if (damage > 0) {
            if (!this.world.isRemote) {
                this.setDamageTaken(damage - tpd);
            }

            return true;
        } else {
            return false;
        }
    }

    public void repairOtherAircraft() {
        float range = this.getAcInfo() != null ? this.getAcInfo().repairOtherVehiclesRange : 0.0F;
        if (!(range <= 0.0F)) {
            if (!this.world.isRemote && this.getCountOnUpdate() % 20 == 0) {
                List<MCH_EntityAircraft> list = this.world.getEntitiesWithinAABB(MCH_EntityAircraft.class, this.getCollisionBoundingBox().grow(range, range, range));

                for (MCH_EntityAircraft ac : list) {
                    if (!W_Entity.isEqual(this, ac) && ac.getHP() < ac.getMaxHP()) {
                        ac.setDamageTaken(ac.getDamageTaken() - this.getAcInfo().repairOtherVehiclesValue);
                    }
                }
            }
        }
    }

    protected void regenerationMob() {
        if (!this.isDestroyed()) {
            if (!this.world.isRemote) {
                if (this.getAcInfo() != null && this.getAcInfo().regeneration && this.getRiddenByEntity() != null) {
                    MCH_EntitySeat[] st = this.getSeats();

                    for (MCH_EntitySeat s : st) {
                        if (s != null && !s.isDead) {
                            Entity e = s.getRiddenByEntity();
                            if (W_Lib.isEntityLivingBase(e) && !e.isDead) {
                                PotionEffect pe = W_Entity.getActivePotionEffect(e, MobEffects.REGENERATION);
                                if (pe == null || pe.getDuration() < 500) {
                                    W_Entity.addPotionEffect(e, new PotionEffect(MobEffects.REGENERATION, 250, 0, true, true));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public double getWaterDepth() {
        final int samples = 5;
        final double sampleOffset = -0.125;

        AxisAlignedBB box = getEntityBoundingBox();
        double height = box.maxY - box.minY;
        double depth = 0.0;

        MCH_AircraftInfo info = getAcInfo();
        if (info == null) {
            return 0.0;
        }

        double floatOffset = info.floatOffset;

        for (int i = 0; i < samples; i++) {
            double yMin = box.minY + height * i / samples + sampleOffset + floatOffset;
            double yMax = box.minY + height * (i + 1) / samples + sampleOffset + floatOffset;

            AxisAlignedBB slice = W_AxisAlignedBB.getAABB(box.minX, yMin, box.minZ, box.maxX, yMax, box.maxZ);

            if (world.isMaterialInBB(slice, Material.WATER)) {
                depth += 1.0 / samples;
            }
        }

        return depth;
    }


    public boolean canSupply() {
        return this.canFloatWater() ? MCH_Lib.getBlockIdY(this, 1, -3) != 0 : MCH_Lib.getBlockIdY(this, 1, -3) != 0 && !this.isInWater();
    }


    public void updateSupplyAmmo() {
        if (!this.world.isRemote) {
            boolean isReloading = this.getRiddenByEntity() instanceof EntityPlayer && !this.getRiddenByEntity().isDead && ((EntityPlayer) this.getRiddenByEntity()).openContainer instanceof MCH_AircraftGuiContainer;

            this.setCommonStatus(2, isReloading);
            if (!this.isDestroyed() && this.beforeSupplyAmmo && !isReloading) {
                this.reloadAllWeapon();
                PacketNotifyAmmoNum.sendAllAmmoNum(this, null);
            }

            this.beforeSupplyAmmo = isReloading;
        }

        if (this.getCommonStatus(2)) {
            this.supplyAmmoWait = 20;
        }

        if (this.supplyAmmoWait > 0) {
            this.supplyAmmoWait--;
        }
    }

    public void supplyAmmo(int weaponID) {
        supplyAmmo(this.getWeapon(weaponID))
        ;
    }

    public void supplyAmmo(MCH_WeaponSet ws) {
        if (this.world.isRemote) {
            ws.supplyRestAllAmmo();
        } else {
            if (this.getRiddenByEntity() instanceof EntityPlayerMP) {
                MCH_CriteriaTriggers.SUPPLY_AMMO.trigger((EntityPlayerMP) this.getRiddenByEntity());
            }

            if (this.getRiddenByEntity() instanceof EntityPlayer player) {
                if (this.canPlayerSupplyAmmo(player, ws)) {

                    for (MCH_WeaponInfo.RoundItem ri : ws.getInfo().roundItems) {
                        int num = ri.num;

                        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                            ItemStack itemStack = player.inventory.mainInventory.get(i);
                            if (!itemStack.isEmpty() && itemStack.isItemEqual(ri.itemStack)) {
                                if (itemStack.getItem() != W_Item.getItemByName("water_bucket") && itemStack.getItem() != W_Item.getItemByName("lava_bucket")) {
                                    if (itemStack.getCount() > num) {
                                        itemStack.shrink(num);
                                        num = 0;
                                    } else {
                                        num -= itemStack.getCount();
                                        itemStack.setCount(0);
                                        player.inventory.mainInventory.set(i, ItemStack.EMPTY);
                                    }
                                } else if (itemStack.getCount() == 1) {
                                    player.inventory.setInventorySlotContents(i, new ItemStack(W_Item.getItemByName("bucket"), 1));
                                    num--;
                                }
                            }

                            if (num <= 0) {
                                break;
                            }
                        }
                    }

                    ws.supplyRestAllAmmo();
                }
            }
        }
    }

    public void supplyAmmoToOtherAircraft() {
        float range = this.getAcInfo() != null ? this.getAcInfo().ammoSupplyRange : 0.0F;
        if (!(range <= 0.0F)) {
            if (!this.world.isRemote && this.getCountOnUpdate() % 40 == 0) {
                List<MCH_EntityAircraft> list = this.world.getEntitiesWithinAABB(MCH_EntityAircraft.class, this.getCollisionBoundingBox().grow(range, range, range));

                for (MCH_EntityAircraft ac : list) {
                    if (!W_Entity.isEqual(this, ac) && ac.canSupply()) {
                        for (int wid = 0; wid < ac.getWeaponNum(); wid++) {
                            MCH_WeaponSet ws = ac.getWeapon(wid);
                            int num = ws.getRestAllAmmoNum() + ws.getAmmo();
                            if (num < ws.getMaxAmmo()) {
                                int ammo = ws.getMaxAmmo() / 10;
                                if (ammo < 1) {
                                    ammo = 1;
                                }

                                ws.setReserveAmmo(num + ammo);
                                EntityPlayer player = ac.getEntityByWeaponId(wid);
                                if (num != ws.getRestAllAmmoNum() + ws.getAmmo()) {
                                    if (ws.getAmmo() <= 0) {
                                        ws.reloadMag();
                                    }

                                    PacketNotifyAmmoNum.sendAmmoNum(ac, player, wid);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canPlayerSupplyAmmo(EntityPlayer player, int weaponId) {
        return canPlayerSupplyAmmo(player, this.getWeapon(weaponId));
    }

    public boolean canPlayerSupplyAmmo(EntityPlayer player, MCH_WeaponSet ws) {
        if (!canSupply() || ws.getRestAllAmmoNum() + ws.getAmmo() >= ws.getMaxAmmo())
            return false;
        for (MCH_WeaponInfo.RoundItem ri : ws.getInfo().roundItems) {
            int num = ri.num;

            for (ItemStack itemStack : player.inventory.mainInventory) {
                if (!itemStack.isEmpty() && itemStack.isItemEqual(ri.itemStack)) {
                    num -= itemStack.getCount();
                }

                if (num <= 0) {
                    break;
                }
            }

            if (num > 0) {
                return false;
            }
        }

        return true;
    }

    public List<ItemStack> getMissingAmmo(EntityPlayer player, MCH_WeaponSet ws) {
        List<ItemStack> missingStacks = new ArrayList<>();


        for (MCH_WeaponInfo.RoundItem ri : ws.getInfo().roundItems) {
            int needed = ri.num;

            for (ItemStack itemStack : player.inventory.mainInventory) {
                if (!itemStack.isEmpty() && itemStack.isItemEqual(ri.itemStack)) {
                    needed -= itemStack.getCount();
                }
                if (needed <= 0) break;
            }

            if (needed > 0) {
                ItemStack missing = ri.itemStack.copy();
                missing.setCount(needed);
                missingStacks.add(missing);
            }
        }

        return missingStacks;
    }

    public void switchNextTextureName() {
        if (this.getAcInfo() != null) {
            this.setTextureName(this.getAcInfo().getNextTextureName(this.getTextureName()));
        }
    }

    public void zoomCamera() {
        if (this.canZoom()) {
            float z = this.camera.getCameraZoom();
            if (z >= this.getZoomMax() - 0.01) {
                z = 1.0F;
            } else {
                z *= 2.0F;
                if (z >= this.getZoomMax()) {
                    z = this.getZoomMax();
                }
            }

            this.camera.setCameraZoom(z <= this.getZoomMax() + 0.01 ? z : 1.0F);
        }
    }

    public int getZoomMax() {
        return this.getAcInfo() != null ? this.getAcInfo().cameraZoom : 1;
    }

    public boolean canZoom() {
        return this.getZoomMax() > 1;
    }

    public boolean canSwitchCameraMode() {
        return !this.isDestroyed() && this.getAcInfo() != null && this.getAcInfo().isEnableNightVision;
    }

    public boolean canSwitchCameraMode(int seatID) {
        return !this.isDestroyed() && this.canSwitchCameraMode() && this.camera.isValidUid(seatID);
    }

    public int getCameraMode(EntityPlayer player) {
        return this.camera.getMode(this.getSeatIdByEntity(player));
    }

    public void switchCameraMode(EntityPlayer player) {
        this.switchCameraMode(player, this.camera.getMode(this.getSeatIdByEntity(player)) + 1);
    }

    public void switchCameraMode(EntityPlayer player, int mode) {
        this.camera.setMode(this.getSeatIdByEntity(player), mode);
    }

    public void updateCameraViewers() {
        for (int i = 0; i < seatManager.getSeatNum() + 1; i++) {
            this.camera.updateViewer(i, seatManager.getEntityBySeatId(i));
        }
    }

    public void updateRadar(int radarSpeed) {
        if (this.isEntityRadarMounted()) {
            this.radarRotate += radarSpeed;
            if (this.radarRotate >= 360) {
                this.radarRotate = 0;
            }

            if (this.radarRotate == 0) {
                this.entityRadar.updateXZ(this, 64);
            }
        }
    }

    public void initRadar() {
        this.entityRadar.clear();
        this.radarRotate = 0;
    }

    public ArrayList<MCH_Vector2> getRadarEntityList() {
        return this.entityRadar.getEntityList();
    }

    public ArrayList<MCH_Vector2> getRadarEnemyList() {
        return this.entityRadar.getEnemyList();
    }

    public void move(@NotNull MoverType type, double moveX, double moveY, double moveZ) {
        if (this.getAcInfo() != null) {
            this.world.profiler.startSection("move");

            double originalX = moveX;
            double originalY = moveY;
            double originalZ = moveZ;

            AxisAlignedBB oldBoundingBox = this.getEntityBoundingBox();
            List<AxisAlignedBB> collisionBoxes = getCollisionBoxes(this, oldBoundingBox.expand(moveX, moveY, moveZ));

            // Vertical movement
            if (moveY != 0.0) {
                for (AxisAlignedBB box : collisionBoxes) {
                    moveY = box.calculateYOffset(this.getEntityBoundingBox(), moveY);
                }
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, moveY, 0.0));
            }

            boolean canStepUp = this.onGround || (originalY != moveY && originalY < 0.0);

            // X movement
            if (moveX != 0.0) {
                for (AxisAlignedBB box : collisionBoxes) {
                    moveX = box.calculateXOffset(this.getEntityBoundingBox(), moveX);
                }
                if (moveX != 0.0) {
                    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(moveX, 0.0, 0.0));
                }
            }

            // Z movement
            if (moveZ != 0.0) {
                for (AxisAlignedBB box : collisionBoxes) {
                    moveZ = box.calculateZOffset(this.getEntityBoundingBox(), moveZ);
                }
                if (moveZ != 0.0) {
                    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, moveZ));
                }
            }

            // Step-up logic
            if (this.stepHeight > 0.0F && canStepUp && (originalX != moveX || originalZ != moveZ)) {
                double savedX = moveX;
                double savedY = moveY;
                double savedZ = moveZ;

                AxisAlignedBB beforeStepBB = this.getEntityBoundingBox();
                this.setEntityBoundingBox(oldBoundingBox);

                moveY = this.stepHeight;
                List<AxisAlignedBB> stepCollisionBoxes = getCollisionBoxes(this, oldBoundingBox.expand(originalX, moveY, originalZ));

                AxisAlignedBB stepAttemptBB1 = this.getEntityBoundingBox().expand(originalX, 0.0, originalZ);
                double stepYOffset1 = moveY;
                for (AxisAlignedBB box : stepCollisionBoxes) {
                    stepYOffset1 = box.calculateYOffset(stepAttemptBB1, stepYOffset1);
                }
                stepAttemptBB1 = stepAttemptBB1.offset(0.0, stepYOffset1, 0.0);

                double stepX1 = originalX;
                for (AxisAlignedBB box : stepCollisionBoxes) {
                    stepX1 = box.calculateXOffset(stepAttemptBB1, stepX1);
                }
                stepAttemptBB1 = stepAttemptBB1.offset(stepX1, 0.0, 0.0);

                double stepZ1 = originalZ;
                for (AxisAlignedBB box : stepCollisionBoxes) {
                    stepZ1 = box.calculateZOffset(stepAttemptBB1, stepZ1);
                }
                stepAttemptBB1 = stepAttemptBB1.offset(0.0, 0.0, stepZ1);

                AxisAlignedBB stepAttemptBB2 = this.getEntityBoundingBox();
                double stepYOffset2 = moveY;
                for (AxisAlignedBB box : stepCollisionBoxes) {
                    stepYOffset2 = box.calculateYOffset(stepAttemptBB2, stepYOffset2);
                }
                stepAttemptBB2 = stepAttemptBB2.offset(0.0, stepYOffset2, 0.0);

                double stepX2 = originalX;
                for (AxisAlignedBB box : stepCollisionBoxes) {
                    stepX2 = box.calculateXOffset(stepAttemptBB2, stepX2);
                }
                stepAttemptBB2 = stepAttemptBB2.offset(stepX2, 0.0, 0.0);

                double stepZ2 = originalZ;
                for (AxisAlignedBB box : stepCollisionBoxes) {
                    stepZ2 = box.calculateZOffset(stepAttemptBB2, stepZ2);
                }
                stepAttemptBB2 = stepAttemptBB2.offset(0.0, 0.0, stepZ2);

                double stepDistance1 = stepX1 * stepX1 + stepZ1 * stepZ1;
                double stepDistance2 = stepX2 * stepX2 + stepZ2 * stepZ2;

                if (stepDistance1 > stepDistance2) {
                    moveX = stepX1;
                    moveZ = stepZ1;
                    moveY = -stepYOffset1;
                    this.setEntityBoundingBox(stepAttemptBB1);
                } else {
                    moveX = stepX2;
                    moveZ = stepZ2;
                    moveY = -stepYOffset2;
                    this.setEntityBoundingBox(stepAttemptBB2);
                }

                for (AxisAlignedBB box : stepCollisionBoxes) {
                    moveY = box.calculateYOffset(this.getEntityBoundingBox(), moveY);
                }
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, moveY, 0.0));

                if (savedX * savedX + savedZ * savedZ >= moveX * moveX + moveZ * moveZ) {
                    moveX = savedX;
                    moveY = savedY;
                    moveZ = savedZ;
                    this.setEntityBoundingBox(beforeStepBB);
                }
            }

            this.world.profiler.endSection();
            this.world.profiler.startSection("rest");

            this.resetPositionToBB();
            this.collidedHorizontally = originalX != moveX || originalZ != moveZ;
            this.collidedVertically = originalY != moveY;
            this.onGround = this.collidedVertically && originalY < 0.0;
            this.collided = this.collidedHorizontally || this.collidedVertically;

            int posXInt = MathHelper.floor(this.posX);
            int posYInt = MathHelper.floor(this.posY - 0.2F);
            int posZInt = MathHelper.floor(this.posZ);
            BlockPos pos = new BlockPos(posXInt, posYInt, posZInt);

            IBlockState state = this.world.getBlockState(pos);
            if (state.getMaterial() == Material.AIR) {
                BlockPos belowPos = pos.down();
                IBlockState belowState = this.world.getBlockState(belowPos);
                Block belowBlock = belowState.getBlock();
                if (belowBlock instanceof BlockFence || belowBlock instanceof BlockWall || belowBlock instanceof BlockFenceGate) {
                    state = belowState;
                    pos = belowPos;
                }
            }

            this.updateFallState(moveY, this.onGround, state, pos);

            if (originalX != moveX) {
                this.motionX = 0.0;
            }
            if (originalZ != moveZ) {
                this.motionZ = 0.0;
            }

            Block landedBlock = state.getBlock();
            if (originalY != moveY) {
                landedBlock.onLanded(this.world, this);
            }

            try {
                this.doBlockCollisions();
            } catch (Throwable throwable) {
                CrashReport crashReport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
                CrashReportCategory category = crashReport.makeCategory("Entity being checked for collision");
                this.addEntityCrashInfo(category);
                throw new ReportedException(crashReport);
            }

            this.world.profiler.endSection();
        }
    }

    protected void onUpdate_updateBlock() {
        if (!MCH_Config.Collision_DestroyBlock.prmBool) {
            return;
        }

        final int baseY = MathHelper.floor(this.posY);

        for (int corner = 0; corner < 4; corner++) {
            int x = MathHelper.floor(this.posX + (corner % 2 - 0.5) * 0.8);
            int z = MathHelper.floor(this.posZ + (corner / 2D - 0.5) * 0.8);

            for (int dy = 0; dy < 2; dy++) {
                BlockPos pos = new BlockPos(x, baseY + dy, z);
                IBlockState state = this.world.getBlockState(pos);
                Block block = state.getBlock();

                if (block == Blocks.AIR) {
                    continue; // skip empty
                }

                if (block == Blocks.SNOW_LAYER) {
                    this.world.setBlockToAir(pos);
                } else if (block == Blocks.WATERLILY || block == Blocks.CAKE) {
                    this.world.destroyBlock(pos, false); // directly call World API
                    // TODO: USE FAKE PLAYERS
                }
            }
        }
    }

    public void onUpdate_ParticleSmoke() {
        if (this.world.isRemote) {
            if (!(this.getCurrentThrottle() <= 0.1F)) {
                float yaw = this.getYaw();
                float pitch = this.getPitch();
                float roll = this.getRoll();
                MCH_WeaponSet ws = this.getCurrentWeapon(this.getRiddenByEntity());
                if (ws.getFirstWeapon() instanceof MCH_WeaponSmoke) {
                    for (int i = 0; i < ws.getWeaponsCount(); i++) {
                        MCH_WeaponBase wb = ws.getWeapon(i);
                        if (wb != null) {
                            MCH_WeaponInfo wi = wb.getInfo();
                            if (wi != null) {
                                Vec3d rot = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -yaw - 180.0F + wb.fixRotationYaw, pitch - wb.fixRotationPitch, roll);
                                if (this.rand.nextFloat() <= this.getCurrentThrottle() * 1.5) {
                                    Vec3d pos = MCH_Lib.RotVec3(wb.position, -yaw, -pitch, -roll);
                                    double x = this.posX + pos.x + rot.x;
                                    double y = this.posY + pos.y + rot.y;
                                    double z = this.posZ + pos.z + rot.z;

                                    for (int smk = 0; smk < wi.smokeNum; smk++) {
                                        float c = this.rand.nextFloat() * 0.05F;
                                        int maxAge = (int) (this.rand.nextDouble() * wi.smokeMaxAge);
                                        MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", x, y, z);
                                        prm.setMotion(rot.x * wi.acceleration + (this.rand.nextDouble() - 0.5) * 0.2, rot.y * wi.acceleration + (this.rand.nextDouble() - 0.5) * 0.2, rot.z * wi.acceleration + (this.rand.nextDouble() - 0.5) * 0.2);
                                        prm.size = (this.rand.nextInt(5) + 5.0F) * wi.smokeSize;
                                        prm.setColor(wi.color.a + this.rand.nextFloat() * 0.05F, wi.color.r + c, wi.color.g + c, wi.color.b + c);
                                        prm.age = maxAge;
                                        prm.toWhite = true;
                                        prm.diffusible = true;
                                        MCH_ParticlesUtil.spawnParticle(prm);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void onUpdate_ParticleSandCloud(boolean seaOnly) {
        if (this.getAcInfo() != null && (!seaOnly || this.getAcInfo().enableSeaSurfaceParticle)) {
            double particlePosY = (int) this.posY;
            boolean b = false;
            float scale = this.getAcInfo().particlesScale * 3.0F;
            if (seaOnly) {
                scale *= 2.0F;
            }

            double throttle = this.getCurrentThrottle();
            throttle *= 2.0;
            if (throttle > 1.0) {
                throttle = 1.0;
            }

            int count = seaOnly ? (int) (scale * 7.0F) : 0;
            int rangeY = (int) (scale * 10.0F) + 1;

            int y;
            for (y = 0; y < rangeY && !b; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Block block = W_WorldFunc.getBlock(this.world, (int) (this.posX + 0.5) + x, (int) (this.posY + 0.5) - y, (int) (this.posZ + 0.5) + z);
                        if (!b && !Block.isEqualTo(block, Blocks.AIR)) {
                            if (seaOnly && W_Block.isEqual(block, W_Block.getWater())) {
                                count--;
                            }

                            if (count <= 0) {
                                particlePosY = this.posY + 1.0 + scale / 5.0F - y;
                                b = true;
                                x += 100;
                                break;
                            }
                        }
                    }
                }
            }

            double pn = (rangeY - y + 1) / (5.0 * scale) / 2.0;
            if (b && this.getAcInfo().particlesScale > 0.01F) {
                for (int k = 0; k < (int) (throttle * 6.0 * pn); k++) {
                    float r = (float) (this.rand.nextDouble() * Math.PI * 2.0);
                    double dx = MathHelper.cos(r);
                    double dz = MathHelper.sin(r);
                    MCH_ParticleParam prm = new MCH_ParticleParam(this.world, "smoke", this.posX + dx * scale * 3.0, particlePosY + (this.rand.nextDouble() - 0.5) * scale, this.posZ + dz * scale * 3.0, scale * (dx * 0.3), scale * -0.4 * 0.05, scale * (dz * 0.3), scale * 5.0F);
                    prm.setColor(prm.a * 0.6F, prm.r, prm.g, prm.b);
                    prm.age = (int) (10.0F * scale);
                    prm.motionYUpAge = seaOnly ? 0.2F : 0.1F;
                    MCH_ParticlesUtil.spawnParticle(prm);
                }
            }
        }
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return par1Entity.getEntityBoundingBox();
    }

    public @NotNull AxisAlignedBB getCollisionBoundingBox() {
        return this.getEntityBoundingBox();
    }

    public double getMountedYOffset() {
        return 0.0;
    }

    public float getShadowSize() {
        return 2.0F;
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }


    @Nullable
    public EntityPlayer getEntityByWeaponId(int id) {
        if (id >= 0 && id < this.getWeaponNum()) {
            for (int i = 0; i < this.weaponSystem.getCurrentWeaponIds().length; i++) {
                if (this.weaponSystem.getCurrentWeaponIds()[i] == id) {
                    Entity e = seatManager.getEntityBySeatId(i);
                    if (e instanceof EntityPlayer) {
                        return (EntityPlayer) e;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public Entity getWeaponUserByWeaponName(String name) {
        if (this.getAcInfo() == null) {
            return null;
        } else {
            MCH_AircraftInfo.Weapon weapon = this.getAcInfo().getWeaponByName(name);
            Entity entity = null;
            if (weapon != null) {
                entity = seatManager.getEntityBySeatId(this.getWeaponSeatID(null, weapon));
                if (entity == null && weapon.canUsePilot) {
                    entity = this.getRiddenByEntity();
                }
            }

            return entity;
        }
    }


    public boolean isValidSeatID(int seatID) {
        return seatID >= 0 && seatID < seatManager.getSeatNum() + 1;
    }

    public void updateHitBoxPosition() {
    }


    public int getClientPositionDelayCorrection() {
        return 7;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9, boolean teleport) {
        this.flightPhysics.captureClientPositionAndRotation(par1, par3, par5, par7, par8, par9 + this.getClientPositionDelayCorrection());
    }


    public void updatePassenger(@NotNull Entity passenger) {
        seatManager.updateRiderPosition(passenger, this.posX, this.posY, this.posZ);
    }

    public Vec3d calcOnTurretPos(Vec3d pos) {
        return this.getRotSeatTransformedOffset(pos, this.getRotSeatYaw());
    }


    public Vec3d getTransformedPosition(Vec3d v) {
        return this.getTransformedPosition(v.x, v.y, v.z);
    }

    public Vec3d getTransformedPosition(double x, double y, double z) {
        return this.getTransformedPosition(x, y, z, this.posX, this.posY, this.posZ);
    }

    public Vec3d getTransformedPosition(Vec3d v, Vec3d pos) {
        return this.getTransformedPosition(v.x, v.y, v.z, pos.x, pos.y, pos.z);
    }


    public Vec3d getTransformedPosition(double x, double y, double z, double px, double py, double pz) {
        Vec3d v = MCH_Lib.RotVec3(x, y, z, -this.getYaw(), -this.getPitch(), -this.getRoll());
        return v.add(px, py, pz);
    }

    public Vec3d getTransformedPosition(double x, double y, double z, double px, double py, double pz, boolean rotSeat) {
        if (rotSeat && this.getAcInfo() != null) {
            Vec3d v = this.getRotSeatTransformedOffset(new Vec3d(x, y, z), this.getRotSeatYaw());
            return v.add(px, py, pz);
        }

        Vec3d v = MCH_Lib.RotVec3(x, y, z, -this.getYaw(), -this.getPitch(), -this.getRoll());
        return v.add(px, py, pz);
    }

    private float getRotSeatYaw() {
        if (this.weaponSystem.isDetachedWeaponAimActive()) {
            return this.weaponSystem.getDetachedWeaponAimYaw();
        }
        if (this.getRiddenByEntity() != null) {
            return this.getRiddenByEntity().rotationYaw;
        }
        return this.seatManager.getLastRiderYaw();
    }

    private Vec3d getRotSeatTransformedOffset(Vec3d localPos, float riderYaw) {
        assert this.getAcInfo() != null;
        Vec3d turretPos = this.getAcInfo().turretPosition;
        Vec3d relative = localPos.subtract(turretPos);
        Vec3d rotatedRelative = MCH_Lib.RotVec3(relative, this.getYaw() - riderYaw, 0.0F, 0.0F);
        return MCH_Lib.RotVec3(rotatedRelative.add(turretPos), -this.getYaw(), -this.getPitch(), -this.getRoll());
    }


    @Nullable
    public MCH_WeaponInfo getWeaponInfoById(int id) {
        if (id >= 0) {
            MCH_WeaponSet ws = this.getWeapon(id);
            if (ws != null) {
                return ws.getInfo();
            }
        }

        return null;
    }

    public abstract boolean canMountWithNearEmptyMinecart();

    protected void mountWithNearEmptyMinecart() {
        seatManager.onMountWithNearEmptyMinecart();
    }


    public boolean isCreatedSeats() {
        return !this.getCommonUniqueId().isEmpty();
    }


    @Override
    public void setDead() {
        this.setDead(false);
    }

    public void setDead(boolean dropItems) {
        this.dropContentsWhenDead = dropItems;
        super.setDead();
        if (this.getRiddenByEntity() != null) {
            this.getRiddenByEntity().dismountRidingEntity();
        }

        this.getGuiInventory().dropContents();

        seatManager.killSeats();

        if (this.soundUpdater != null) {
            this.soundUpdater.update();
        }

        if (this.getTowChainEntity() != null) {
            this.getTowChainEntity().setDead();
            this.setTowChainEntity(null);
        }

        for (Entity e : Objects.requireNonNull(this.getParts())) {
            if (e != null) {
                e.setDead();
            }
        }
        if (isUAV()) UAVTracker.delUAVPos(world, this);

        String format = "setDead:" + (this.getAcInfo() != null ? this.getAcInfo().name : "null");
        MCH_Logger.debugLog(this.world, format);
    }


    public Entity getRidingEntity() {
        return super.getRidingEntity();
    }


    public void startRepelling() {
        MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.startRepelling()");
        this.setRepellingStat(true);
        this.setThrottleUp(false);
        this.setThrottleDown(false);
        this.setMoveLeft(false);
        this.setMoveRight(false);
        seatManager.setTickRepelling(0);
    }


    public boolean isRepelling() {
        return this.getCommonStatus(5);
    }

    public void setRepellingStat(boolean b) {
        this.setCommonStatus(5, b);
    }

    public Vec3d getRopePos(int ropeIndex) {
        return this.getAcInfo() != null && this.getAcInfo().haveRepellingHook() && ropeIndex < this.getAcInfo().repellingHooks.size() ? this.getTransformedPosition(this.getAcInfo().repellingHooks.get(ropeIndex).pos()) : new Vec3d(this.posX, this.posY, this.posZ);
    }


    @Override
    public boolean canRideAircraft(MCH_EntityAircraft ac, int seatID, MCH_SeatRackInfo info) {
        var selfInfo = getAcInfo();
        if (selfInfo == null) return false;
        if (ac.getRidingEntity() != null) return false;
        if (getRidingEntity() != null) return false;

        var acInfo = ac.getAcInfo();

        // Check names first
        for (String name : info.names) {
            if (name.equalsIgnoreCase(selfInfo.name) || name.equalsIgnoreCase(selfInfo.getKindName())) {
                return seatManager.checkNestedRiders();
            }
        }

        // Check ride racks
        for (MCH_AircraftInfo.RideRack rr : selfInfo.rideRacks) {
            assert acInfo != null;
            int baseSeat = acInfo.getNumSeat() - 1;
            int id = baseSeat + rr.rackID() - 1;

            if (id != seatID) continue;
            if (!rr.name().equalsIgnoreCase(acInfo.name)) continue;

            MCH_EntitySeat seat = seatManager.getSeat(id);
            if (seat != null && seat.getRiddenByEntity() == null) {
                return seatManager.checkNestedRiders();
            }
        }

        // Nothing matched
        return false;
    }


    public void onInteractFirst(EntityPlayer player) {
    }

    public boolean notOnSameTeam(EntityPlayer player) {
        for (int i = 0; i < 1 + seatManager.getSeatNum(); i++) {
            Entity entity = seatManager.getEntityBySeatId(i);
            if (entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) {
                EntityLivingBase riddenEntity = (EntityLivingBase) entity;
                if (riddenEntity.getTeam() != null && !riddenEntity.isOnSameTeam(player)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean processInitialInteract(EntityPlayer player, boolean ss, EnumHand hand) {
        seatManager.setSwitchSeat(ss);
        boolean ret = this.processInitialInteract(player, hand);
        seatManager.setSwitchSeat(false);
        return ret;
    }

    @Override
    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        if (this.isDestroyed()) return false;
        if (this.getAcInfo() == null) return false;
        if (this.notOnSameTeam(player)) return false;

        ItemStack stack = player.getHeldItem(hand);

        // Item interactions
        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof MCH_ItemWrench) {
                if (!world.isRemote && player.isSneaking()) {
                    this.switchNextTextureName();
                }
                return false;
            }
            if (stack.getItem() instanceof MCH_ItemSpawnGunner) {
                return false;
            }
        }

        // Sneak interactions
        if (player.isSneaking()) {
            openContainer(player);
            return false;
        }

        // Seat/ride restrictions
        if (!this.getAcInfo().canRide) return false;
        if (this.getRiddenByEntity() != null || this.isUAV()) {
            return this.interactFirstSeat(player);
        }
        if (player.getRidingEntity() instanceof MCH_EntitySeat) return false;
        if (!seatManager.canRideSeatOrRack(0, player)) return false;

        // Canopy/mode restrictions
        if (!seatManager.isSwitchSeat()) {
            if (this.getAcInfo().haveCanopy() && this.isCanopyClose()) {
                this.openCanopy();
                return false;
            }
            if (this.getModeSwitchCooldown() > 0) return false;
        }

        // Main riding flow
        this.closeCanopy();
        this.lastRiddenByEntity = null;
        this.initRadar();

        if (!world.isRemote) {
            player.startRiding(this);
            if (!this.keepOnRideRotation) {
                this.mountMobToSeats();
            }
        } else {
            this.updateClientSettings(0);
        }

        this.setCameraId(0);
        this.initPilotWeapon();
        this.getLowPassPartialTicks().clear();

        if ("uh-1c".equalsIgnoreCase(this.getAcInfo().name) && player instanceof EntityPlayerMP) {
            MCH_CriteriaTriggers.RIDING_VALKYRIES.trigger((EntityPlayerMP) player);
        }

        this.onInteractFirst(player);
        return true;
    }


    public void updateClientSettings(int seatId) {
        this.cs_dismountAll = MCH_Config.DismountAll.prmBool;
        this.cs_heliAutoThrottleDown = MCH_Config.AutoThrottleDownHeli.prmBool;
        this.cs_planeAutoThrottleDown = MCH_Config.AutoThrottleDownPlane.prmBool;
        this.cs_tankAutoThrottleDown = MCH_Config.AutoThrottleDownTank.prmBool;
        this.camera.setShaderSupport(seatId, W_EntityRenderer.isShaderSupport());
        PacketClientSettingsSync.send();
    }

    @Override
    public boolean canLockEntity(Entity entity) {
        return !this.isMountedEntity(entity);
    }


    public Entity[] getParts() {
        return this.partEntities;
    }

    public float getSoundVolume() {
        return 1.0F;
    }

    public float getSoundPitch() {
        return 1.0F;
    }

    public abstract String getDefaultSoundName();

    public String getSoundName() {
        if (this.getAcInfo() == null) {
            return "";
        } else {
            return this.getAcInfo().soundMove != null ? this.getAcInfo().soundMove.getPath() : this.getDefaultSoundName();
        }
    }

    @Override
    public boolean isSkipNormalRender() {
        return this.getRidingEntity() instanceof MCH_EntitySeat;
    }

    public boolean isRenderBullet(Entity entity, Entity rider) {
        return !this.isCameraView(rider) || !W_Entity.isEqual(this.getTVMissile(), entity) || (this.getTVMissile() != null && !W_Entity.isEqual(this.getTVMissile().shootingEntity, rider));
    }

    public boolean isCameraView(Entity entity) {
        return this.weaponSystem.isCameraView(entity);
    }

    public void updateCamera(double x, double y, double z) {
        this.weaponSystem.updateCamera(x, y, z);
    }

    public void updateCameraRotate(float yaw, float pitch) {
        this.camera.prevRotationYaw = this.camera.rotationYaw;
        this.camera.prevRotationPitch = this.camera.rotationPitch;
        this.camera.rotationYaw = yaw;
        this.camera.rotationPitch = pitch;
    }

    public void updatePartCameraRotate() {
        if (this.world.isRemote) {
            Entity e = seatManager.getEntityBySeatId(1);
            if (e == null) {
                e = this.getRiddenByEntity();
            }

            if (e != null) {
                this.camera.partRotationYaw = e.rotationYaw;
                float pitch = e.rotationPitch;
                this.camera.prevPartRotationYaw = this.camera.partRotationYaw;
                this.camera.prevPartRotationPitch = this.camera.partRotationPitch;
                this.camera.partRotationPitch = pitch;
            }
        }
    }

    @Nullable
    public MCH_EntityTvMissile getTVMissile() {
        return this.weaponSystem.getTVMissile();
    }

    public void setTVMissile(MCH_EntityTvMissile entity) {
        this.weaponSystem.setTVMissile(entity);
    }

    public MCH_WeaponSet[] createWeapon(int seat_num) {
        return this.weaponSystem.createWeapon(seat_num);
    }

    public void switchWeapon(Entity entity, int id) {
        this.weaponSystem.switchWeapon(entity, id);
    }

    public void updateWeaponID(int sid, int id) {
        this.weaponSystem.updateWeaponID(sid, id);
    }

    @Nullable
    public MCH_WeaponSet getWeaponByName(String name) {
        return this.weaponSystem.getWeaponByName(name);
    }

    public void reloadAllWeapon() {
        this.weaponSystem.reloadAllWeapon();
    }

    public MCH_WeaponSet getFirstSeatWeapon() {
        return this.weaponSystem.getFirstSeatWeapon();
    }

    public void initCurrentWeapon(Entity entity) {
        this.weaponSystem.initCurrentWeapon(entity);
    }

    public void initPilotWeapon() {
        this.weaponSystem.initPilotWeapon();
    }

    public MCH_WeaponSet getCurrentWeapon(Entity entity) {
        return this.weaponSystem.getCurrentWeapon(entity);
    }

    public MCH_WeaponSet getWeapon(int id) {
        return this.weaponSystem.getWeapon(id);
    }

    public int getWeaponIDBySeatID(int sid) {
        return this.weaponSystem.getWeaponIDBySeatID(sid);
    }

    public double getLandInDistance(Entity user) {
        if (this.lastCalcLandInDistanceCount != this.getCountOnUpdate() && (this.world.isRemote || this.getCountOnUpdate() % 5 == 0)) {
            this.lastCalcLandInDistanceCount = this.getCountOnUpdate();
            this.prevLandInDistance = this.lastLandInDistance;
            this.prevImpactPos = this.impactPos;
            MCH_WeaponParam prm = new MCH_WeaponParam();
            prm.setPosition(this.posX, this.posY, this.posZ);
            prm.entity = this;
            prm.user = user;
            prm.isInfinity = this.isInfinityAmmo(prm.user);
            if (prm.user != null) {
                MCH_WeaponSet currentWs = this.getCurrentWeapon(prm.user);
                if (currentWs != null) {
                    int sid = this.getSeatIdByEntity(prm.user);
                    assert this.getAcInfo() != null;
                    if (this.getAcInfo().getWeaponSetById(sid) != null) {
                        prm.isTurret = this.getAcInfo().getWeaponSetById(sid).weapons.getFirst().turret;
                    }

                    Vec3d shotPos = currentWs.getFirstWeapon().getShotPos(this);
                    prm.setPosition(this.posX + shotPos.x, this.posY + shotPos.y, this.posZ + shotPos.z);
                    prm.rotYaw = MathHelper.wrapDegrees(this.getCurrentWeaponShotYaw(prm.user));
                    prm.rotPitch = MathHelper.wrapDegrees(this.getCurrentWeaponShotPitch(prm.user));

                    this.impactPos = currentWs.getCurrentWeapon().getImpactPos(prm);

                    if (this.impactPos != null) {
                        double dx = this.impactPos.x - this.posX;
                        double dz = this.impactPos.z - this.posZ;
                        this.lastLandInDistance = Math.sqrt(dx * dx + dz * dz);
                    } else {
                        this.lastLandInDistance = -1.0;
                    }

                    if (this.prevLandInDistance < 0) this.prevLandInDistance = this.lastLandInDistance;
                    if (this.prevImpactPos == null) this.prevImpactPos = this.impactPos;
                }
            }
        }

        return this.lastLandInDistance;
    }

    public boolean useCurrentWeapon(Entity user) {
        return this.weaponSystem.useCurrentWeapon(user);
    }

    public boolean prepareCurrentWeapon(Entity user) {
        return this.weaponSystem.prepareCurrentWeapon(user);
    }

    public boolean useCurrentWeapon(MCH_WeaponParam prm) {
        return this.weaponSystem.useCurrentWeapon(prm);
    }


    public void switchCurrentWeaponMode(Entity entity) {
        this.weaponSystem.switchCurrentWeaponMode(entity);
    }

    public int getWeaponNum() {
        return this.weaponSystem.getWeaponNum();
    }

    public int getCurrentWeaponID(Entity entity) {
        return this.weaponSystem.getCurrentWeaponID(entity);
    }

    public int getNextWeaponID(Entity entity, int step) {
        return this.weaponSystem.getNextWeaponID(entity, step);
    }

    public int getWeaponSeatID(MCH_WeaponInfo wi, MCH_AircraftInfo.Weapon w) {
        return this.weaponSystem.getWeaponSeatID(wi, w);
    }

    public boolean isMissileCameraMode(Entity entity) {
        return this.weaponSystem.isMissileCameraMode(entity);
    }

    public boolean isPilotReloading() {
        return this.getCommonStatus(2) || this.supplyAmmoWait > 0;
    }

    public int getUsedWeaponStat() {
        return this.weaponSystem.getUsedWeaponStat();
    }

    public boolean isWeaponOnCooldown(MCH_WeaponSet checkWs, int index) {
        return this.weaponSystem.isWeaponOnCooldown(checkWs, index);
    }

    public void updateWeapons() {
        this.weaponSystem.updateWeapons();
    }

    public void updateWeaponsRotation() {
        this.weaponSystem.updateWeaponsRotation();
    }


    public int getMaxHitStatus() {
        return 15;
    }

    public void hitBullet() {
        this.weaponSystem.hitBullet();
    }

    public void initRotationYaw(float yaw) {
        this.rotationYaw = yaw;
        this.prevRotationYaw = yaw;
        this.lastRiderYaw = yaw;
        this.lastSearchLightYaw = yaw;

        for (MCH_WeaponSet w : this.weaponSystem.getWeapons()) {
            w.setYaw(w.getDefYaw());
            w.setPitch(0.0F);
        }
    }

    @Nullable
    public MCH_AircraftInfo getAcInfo() {
        return this.acInfo;
    }


    public void setAcInfo(@Nullable MCH_AircraftInfo info) {
        this.acInfo = info;
        if (info != null) {
            this.partHatch = this.createHatch();
            this.partCanopy = this.createCanopy();
            this.partLandingGear = this.createLandingGear();
            this.weaponSystem.setWeaponBays(this.weaponSystem.createWeaponBays());
            this.rotPartRotation = new float[info.partRotPart.size()];
            this.prevRotPartRotation = new float[info.partRotPart.size()];
            this.extraBoundingBox = this.createExtraBoundingBox();
            this.partEntities = this.createParts();
            this.stepHeight = info.stepHeight;
            this.setInventorySize((short) info.inventorySize);
        }
    }

    @Nullable
    public abstract Item getItem();

    public MCH_BoundingBox[] createExtraBoundingBox() {
        // Get the list of extra bounding boxes
        MCH_AircraftInfo acInfo = this.getAcInfo();
        if (acInfo == null || acInfo.extraBoundingBox == null) {
            return new MCH_BoundingBox[0];
        }

        List<MCH_BoundingBox> boundingBoxes = acInfo.extraBoundingBox;

        // Initialize the array with the size of the list
        MCH_BoundingBox[] ar = new MCH_BoundingBox[boundingBoxes.size()];

        // Iterate over the list and copy each bounding box to the array
        int i = 0;
        for (MCH_BoundingBox bb : boundingBoxes) {
            ar[i++] = bb.copy();
        }

        return ar;
    }

    public Entity[] createParts() {
        return new Entity[]{this.partEntities[0]};
    }

    public void switchGunnerMode(boolean mode) {
        this.weaponSystem.switchGunnerMode(mode);
    }

    public boolean canSwitchGunnerMode() {
        return this.weaponSystem.canSwitchGunnerMode();
    }

    public boolean canSwitchGunnerModeOtherSeat(EntityPlayer player) {
        return this.weaponSystem.canSwitchGunnerModeOtherSeat(player);
    }

    public void switchGunnerModeOtherSeat(EntityPlayer player) {
        this.weaponSystem.switchGunnerModeOtherSeat(player);
    }

    public void switchHoveringMode(boolean mode) {
        seatManager.stopRepelling();
        if (this.canSwitchHoveringMode() && this.isHoveringMode() != mode) {
            if (mode) {
                this.setBeforeHoverThrottle(this.getCurrentThrottle());
            } else {
                this.setCurrentThrottle(this.getBeforeHoverThrottle());
            }

            this.isHoveringMode = mode;
            Entity riddenByEntity = this.getRiddenByEntity();
            if (riddenByEntity != null) {
                riddenByEntity.rotationPitch = 0.0F;
                riddenByEntity.prevRotationPitch = 0.0F;
            }
        }
    }

    public boolean canSwitchHoveringMode() {
        return this.getAcInfo() != null && !this.weaponSystem.isGunnerMode();
    }

    public boolean isHovering() {
        return this.weaponSystem.isGunnerMode() || this.isHoveringMode();
    }

    public boolean getIsGunnerMode(Entity entity) {
        return this.weaponSystem.getIsGunnerMode(entity);
    }

    public boolean isPilot(Entity player) {
        return W_Entity.isEqual(this.getRiddenByEntity(), player);
    }

    public boolean canSwitchFreeLook() {
        return true;
    }

    public boolean isFreeLookMode() {
        return this.getCommonStatus(1) || this.isRepelling();
    }

    public void switchFreeLookMode(boolean b) {
        this.setCommonStatus(1, b);
    }

    public void switchFreeLookModeClient(boolean b) {
        this.setCommonStatus(1, b, true);
    }

    public boolean canSwitchGunnerFreeLook(EntityPlayer player) {
        return this.weaponSystem.canSwitchGunnerFreeLook(player);
    }

    public boolean isGunnerLookMode(EntityPlayer player) {
        return this.weaponSystem.isGunnerLookMode(player);
    }

    public void switchGunnerFreeLookMode(boolean b) {
        this.weaponSystem.switchGunnerFreeLookMode(b);
    }

    public void switchGunnerFreeLookMode() {
        this.weaponSystem.switchGunnerFreeLookMode();
    }

    public void updateParts(int stat) {
        if (this.isDestroyed()) return;

        // Update aircraft parts
        MCH_Parts[] parts = {this.partHatch, this.partCanopy, this.partLandingGear};
        for (MCH_Parts part : parts) {
            if (part != null) {
                part.updateStatusClient(stat);
                part.update();
            }
        }

        // Landing gear logic
        if (this.isDestroyed() || this.world.isRemote || this.partLandingGear == null) return;

        float gearFactor = this.partLandingGear.getFactor();
        boolean isFolded = this.isLandingGearFolded();
        int blockId;

        if (!isFolded && gearFactor <= 0.1F) {
            // Attempt to fold landing gear
            blockId = MCH_Lib.getBlockIdY(this, 3, -20);
            boolean safeToFold = (this.getCurrentThrottle() <= 0.8F || this.onGround || blockId != 0) && this.getAcInfo() != null && this.getAcInfo().isFloat && (this.isInWater() || MCH_Lib.getBlockY(this, 3, -20, true) == W_Block.getWater());

            if (safeToFold) {
                this.partLandingGear.setStatusServer(true);
            }
        } else if (isFolded && gearFactor >= 0.9F) {
            // Attempt to unfold landing gear
            blockId = MCH_Lib.getBlockIdY(this, 3, -10);
            boolean shouldUnfold = false;

            if (this.getCurrentThrottle() < this.getUnfoldLandingGearThrottle() && blockId != 0) {
                shouldUnfold = true;

                if (this.getAcInfo() != null && this.getAcInfo().isFloat) {
                    blockId = MCH_Lib.getBlockIdY(this.world, this.posX, this.posY + 1.0 + this.getAcInfo().floatOffset, this.posZ, 1, 65386, true);
                    if (W_Block.isEqual(blockId, W_Block.getWater())) {
                        shouldUnfold = false;
                    }
                }
            } else if (this.getVtolMode() == 2 && blockId != 0) {
                shouldUnfold = true;
            }

            if (shouldUnfold) {
                this.partLandingGear.setStatusServer(false);
            }
        }
    }

    public float getUnfoldLandingGearThrottle() {
        return 0.8F;
    }

    protected void initPartRotation(float yaw, float pitch) {
        this.lastRiderYaw = yaw;
        this.prevLastRiderYaw = yaw;
        this.camera.partRotationYaw = yaw;
        this.camera.prevPartRotationYaw = yaw;
        this.lastSearchLightYaw = yaw;

        this.lastRiderPitch = pitch;
        this.prevLastRiderPitch = pitch;
        this.camera.partRotationPitch = pitch;
        this.camera.prevPartRotationPitch = pitch;
        this.lastSearchLightPitch = pitch;

    }

    public int getLastPartStatusMask() {
        return 24;
    }

    protected MCH_Parts createHatch() {
        MCH_Parts hatch = null;
        if (this.getAcInfo() != null && this.getAcInfo().haveHatch()) {
            hatch = new MCH_Parts(this, 4, NetworkSyncComponent.PART_STAT, "Hatch");
            hatch.rotationMax = 90.0F;
            hatch.rotationInv = 1.5F;
            hatch.soundEndSwichOn.setPrm("plane_cc", 1.0F, 1.0F);
            hatch.soundEndSwichOff.setPrm("plane_cc", 1.0F, 1.0F);
            hatch.soundSwitching.setPrm("plane_cv", 1.0F, 0.5F);
        }

        return hatch;
    }

    public boolean haveHatch() {
        return this.partHatch != null;
    }

    public boolean canUnfoldHatch() {
        return this.partHatch != null && this.modeSwitchCooldown <= 0 && this.partHatch.isOFF();
    }

    public boolean canFoldHatch() {
        return this.partHatch != null && this.modeSwitchCooldown <= 0 && this.partHatch.isON();
    }

    public void foldHatch(boolean fold) {
        this.foldHatch(fold, false);
    }

    public void foldHatch(boolean fold, boolean force) {
        if (this.partHatch != null) {
            if (force || this.modeSwitchCooldown <= 0) {
                this.partHatch.setStatusServer(fold);
                this.modeSwitchCooldown = 20;
                if (!fold) {
                    this.stopUnmountCrew();
                }
            }
        }
    }

    public float getHatchRotation() {
        return this.partHatch != null ? this.partHatch.rotation : 0.0F;
    }

    public float getPrevHatchRotation() {
        return this.partHatch != null ? this.partHatch.prevRotation : 0.0F;
    }

    public void foldLandingGear() {
        if (this.partLandingGear != null && this.getModeSwitchCooldown() <= 0) {
            this.partLandingGear.setStatusServer(true);
            this.setModeSwitchCooldown(20);
        }
    }

    public void unfoldLandingGear() {
        if (this.partLandingGear != null && this.getModeSwitchCooldown() <= 0) {
            if (this.isLandingGearFolded()) {
                this.partLandingGear.setStatusServer(false);
                this.setModeSwitchCooldown(20);
            }
        }
    }

    public boolean canFoldLandingGear() {
        if (this.getLandingGearRotation() >= 1.0F) {
            return false;
        } else {
            Block block = MCH_Lib.getBlockY(this, 3, -10, true);
            return !this.isLandingGearFolded() && block == Blocks.AIR;
        }
    }

    public boolean canUnfoldLandingGear() {
        return !(this.getLandingGearRotation() < 89.0F) && this.isLandingGearFolded();
    }

    public boolean isLandingGearFolded() {
        return this.partLandingGear != null && this.partLandingGear.getStatus();
    }

    protected MCH_Parts createLandingGear() {
        MCH_Parts lg = null;
        if (this.getAcInfo() != null && this.getAcInfo().haveLandingGear()) {
            lg = new MCH_Parts(this, 2, NetworkSyncComponent.PART_STAT, "LandingGear");
            lg.rotationMax = 90.0F;
            lg.rotationInv = 2.5F;
            lg.soundStartSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
            lg.soundEndSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
            lg.soundStartSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
            lg.soundEndSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
            lg.soundSwitching.setPrm("plane_cv", 1.0F, 0.75F);
        }

        return lg;
    }

    public float getLandingGearRotation() {
        return this.partLandingGear != null ? this.partLandingGear.rotation : 0.0F;
    }

    public float getPrevLandingGearRotation() {
        return this.partLandingGear != null ? this.partLandingGear.prevRotation : 0.0F;
    }

    public int getVtolMode() {
        return 0;
    }

    public void openCanopy() {
        if (this.partCanopy != null && this.getModeSwitchCooldown() <= 0) {
            this.partCanopy.setStatusServer(true);
            this.setModeSwitchCooldown(20);
        }
    }

    public void openCanopy_EjectSeat() {
        if (this.partCanopy != null) {
            this.partCanopy.setStatusServer(true, false);
            this.setModeSwitchCooldown(40);
        }
    }

    public void closeCanopy() {
        if (this.partCanopy != null && this.getModeSwitchCooldown() <= 0) {
            if (this.getCanopyStat()) {
                this.partCanopy.setStatusServer(false);
                this.setModeSwitchCooldown(20);
            }
        }
    }

    public boolean getCanopyStat() {
        return this.partCanopy != null && this.partCanopy.getStatus();
    }

    public boolean isCanopyClose() {
        return this.partCanopy == null || !this.getCanopyStat() && this.getCanopyRotation() <= 0.01F;
    }

    public float getCanopyRotation() {
        return this.partCanopy != null ? this.partCanopy.rotation : 0.0F;
    }

    public float getPrevCanopyRotation() {
        return this.partCanopy != null ? this.partCanopy.prevRotation : 0.0F;
    }

    protected MCH_Parts createCanopy() {
        MCH_Parts canopy = null;
        if (this.getAcInfo() != null && this.getAcInfo().haveCanopy()) {
            canopy = new MCH_Parts(this, 0, NetworkSyncComponent.PART_STAT, "Canopy");
            canopy.rotationMax = 90.0F;
            canopy.rotationInv = 3.5F;
            canopy.soundEndSwichOn.setPrm("plane_cc", 1.0F, 1.0F);
            canopy.soundEndSwichOff.setPrm("plane_cc", 1.0F, 1.0F);
        }

        return canopy;
    }

    public boolean hasBrake() {
        return false;
    }

    public boolean getBrake() {
        return this.getCommonStatus(11);
    }

    public void setBrake(boolean b) {
        this.setCommonStatus(11, b, this.world.isRemote);
    }

    public boolean getGunnerStatus() {
        return this.getCommonStatus(12);
    }

    public void setGunnerStatus(boolean b) {
        if (!this.world.isRemote) {
            this.setCommonStatus(12, b);
        }
    }

    @Override
    public int getSizeInventory() {
        return this.getAcInfo() != null ? this.getAcInfo().inventorySize : 0;
    }


    @Override
    public boolean hasCustomName() {
        return this.getAcInfo() != null;
    }

    @Nullable
    public MCH_EntityChain getTowChainEntity() {
        return this.towChainEntity;
    }

    @Nullable
    public MCH_EntityChain getTowedChainEntity() {
        return this.towedChainEntity;
    }

    public void setEntityBoundingBox(@NotNull AxisAlignedBB bb) {
        super.setEntityBoundingBox(new MCH_AircraftBoundingBox(this, bb));
    }


    @Override
    public double getX() {
        return this.posX;
    }

    @Override
    public double getY() {
        return this.posY;
    }

    @Override
    public double getZ() {
        return this.posZ;
    }

    @Override
    public Entity getEntity() {
        return this;
    }

    public String getNameOnMyRadar(MCH_EntityAircraft other) {
        if (other.getAcInfo() == null || this.getAcInfo() == null) return "";
        else return switch (getAcInfo().radarType) {
            case MODERN_AA -> other.getAcInfo().nameOnModernAARadar;
            case EARLY_AA -> other.getAcInfo().nameOnEarlyAARadar;
            case MODERN_AS -> other.getAcInfo().nameOnModernASRadar;
            case EARLY_AS -> other.getAcInfo().nameOnEarlyASRadar;
            case null -> "";
        };
    }

    public String getNameOnMyRadar(EntityInfo other) {
        if (this.world.getEntityByID(other.entityId) instanceof MCH_EntityAircraft aircraft) {
            return getNameOnMyRadar(aircraft);
        }
        return "?";
    }

    public boolean isSmallUAV() {
        return this.getAcInfo() != null && this.getAcInfo().isSmallUAV;
    }

    @Override
    public @NotNull AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(32);
    }

    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[]{new FluidTankProperties(fuelComponent.getFuelStack(), fuelComponent.getMaxFuel())};
    }


    public int fill(FluidStack resource, boolean doFill) {
        return fuelComponent.fill(resource, doFill);
    }

    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return fuelComponent.drain(resource, doDrain);
    }


    public boolean canUseChaff() {
        return this.weaponSystem.canUseChaff();
    }

    public boolean useChaff() {
        return this.weaponSystem.useChaff();
    }

    public boolean isChaffUsing() {
        return this.weaponSystem.isChaffUsing();
    }

    public boolean useAPS() {
        return this.weaponSystem.useAPS();
    }

    public boolean canUseAPS() {
        return this.weaponSystem.canUseAPS();
    }

    public boolean haveAPS() {
        return this.getAcInfo() != null && this.getAcInfo().haveAPS();
    }

    public ModularPanel buildUI(AircraftGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return data.isContainerOnly() ?
                ContainerGui.buildUI(data, syncManager, settings, this) : AircraftGui.buildUI(data, syncManager, settings, this);
    }

    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(AircraftGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(Tags.MODID, mainPanel);
    }

    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return fuelComponent.drain(maxDrain, doDrain);
    }

    public void resetAirburstDistance(EntityPlayer player, MCH_WeaponBase wb) {
        if (wb == null) {
            return;
        }

        int dist = 0;
        if (wb.airburstDist <= 0) {
            Vec3d start = player.getPositionEyes(1.0F);
            Vec3d look = player.getLook(1.0F);
            Vec3d end = start.add(look.x * 3000.0, look.y * 3000.0, look.z * 3000.0);
            RayTraceResult mop = this.world.rayTraceBlocks(start, end, false, true, false);
            if (mop != null && mop.typeOfHit != RayTraceResult.Type.MISS) {
                dist = (int) player.getDistance(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
            }
        }

        wb.airburstDist = dist;
        if (this.world.isRemote) {
            new PacketAirburstDistReset(this.getEntityId(), dist).sendToServer();
        }
    }

    public void manualReloadForPlayer(EntityPlayer player) {
        boolean didReload = getCurrentWeapon(player).manualReload();
        if (didReload && !world.isRemote && player instanceof EntityPlayerMP playerMP)
            new PacketSyncReload(this.getEntityId()).sendToPlayer(playerMP);
    }

    public @NotNull String getName() {
        if (this.getAcInfo() == null) return super.getName();
        return this.acInfo.name;
    }

    public String getTranslationKey() {
        if (getAcInfo() != null)
            return "item.mcheli:" + getAcInfo().name + ".name";
        else
            return "";
    }

    public double getCurrentSpeed() {
        double motionX = this.motionX;
        double motionY = this.motionY;
        double motionZ = this.motionZ;

        double tickDistance = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);

        return tickDistance * 20.0D;
    }

    public String getNameOnOtherRadar(MCH_EntityAircraft other) {
        if (other.getAcInfo() == null || this.getAcInfo() == null) return "?";
        return switch (other.getAcInfo().radarType) {
            case MODERN_AA -> getAcInfo().nameOnModernAARadar;
            case EARLY_AA -> getAcInfo().nameOnEarlyAARadar;
            case MODERN_AS -> getAcInfo().nameOnModernASRadar;
            case EARLY_AS -> getAcInfo().nameOnEarlyASRadar;
            default -> "?";
        };
    }

    public Random getRNG() {
        return this.rand;
    }

    public static class WeaponBay {

        public float rot = 0.0F;
        public float prevRot = 0.0F;

        public WeaponBay() {
        }
    }
}
