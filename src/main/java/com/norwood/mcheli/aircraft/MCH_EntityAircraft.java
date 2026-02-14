package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.norwood.mcheli.*;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.command.MCH_Command;
import com.norwood.mcheli.factories.AircraftGuiData;
import com.norwood.mcheli.factories.MCHGuiFactories;
import com.norwood.mcheli.flare.MCH_Flare;
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
import com.norwood.mcheli.parachute.MCH_EntityParachute;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.UAVTracker;
import com.norwood.mcheli.weapon.*;
import com.norwood.mcheli.wrapper.*;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.*;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public abstract class MCH_EntityAircraft extends W_EntityContainer implements IGuiHolder<AircraftGuiData>, MCH_IEntityLockChecker, MCH_IEntityCanRideAircraft, IEntityAdditionalSpawnData, IEntitySinglePassenger, ITargetMarkerObject, IFluidHandler {


    protected static final DataParameter<Integer> PART_STAT = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DAMAGE = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<String> ID_TYPE = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    private static final DataParameter<String> TEXTURE_NAME = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    private static final DataParameter<Integer> UAV_STATION = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> STATUS = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> USE_WEAPON = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> FUEL = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<String> FUEL_FF = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    private static final DataParameter<Integer> ROT_ROLL = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final DataParameter<String> COMMAND = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    private static final DataParameter<Integer> THROTTLE = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    private static final MCH_EntitySeat[] seatsDummy = new MCH_EntitySeat[0];
    public final MCH_MissileDetector missileDetector;
    public final HashMap<Entity, Integer> noCollisionEntities = new HashMap<>();
    public final MCH_LowPassFilterFloat lowPassPartialTicks;
    public final List<MCH_EntityAircraft.UnmountReserve> listUnmountReserve = new ArrayList<>();
    public final MCH_Camera camera;
    public final float[] rotCrawlerTrack = new float[2];
    public final float[] prevRotCrawlerTrack = new float[2];
    public final float[] throttleCrawlerTrack = new float[2];
    public final float[] rotTrackRoller = new float[2];
    public final float[] prevRotTrackRoller = new float[2];
    protected final MCH_SoundUpdater soundUpdater;
    protected final MCH_WeaponSet dummyWeapon;
    private final MCH_AircraftInventory inventory;
    private final MCH_EntityHitBox pilotSeat;
    private final MCH_Radar entityRadar;
    private final MCH_Flare flareDv;
    private final MCH_Queue<Vec3d> prevPosition;
    public boolean isRequestedSyncStatus;
    public boolean keepOnRideRotation;
    public boolean aircraftRollRev;
    public boolean aircraftRotChanged;
    public float rotationRoll;
    public float prevRotationRoll;
    public double currentSpeed;
    public float throttleBack = 0.0F;
    public double beforeHoverThrottle;
    public int waitMountEntity = 0;
    public boolean throttleUp = false;
    public boolean throttleDown = false;
    public boolean moveLeft = false;
    public boolean moveRight = false;
    @Getter
    public float lastRiderYaw;
    public float prevLastRiderYaw;
    @Getter
    public float lastRiderPitch;
    public float prevLastRiderPitch;
    public int serverNoMoveCount = 0;
    public int repairCount;
    public int beforeDamageTaken;
    public int timeSinceHit;
    public float rotDestroyedYaw;
    public float rotDestroyedPitch;
    public float rotDestroyedRoll;
    public int damageSinceDestroyed;
    public boolean isFirstDamageSmoke = true;
    public Vec3d[] prevDamageSmokePos = new Vec3d[0];
    public boolean cs_dismountAll;
    public boolean cs_heliAutoThrottleDown;
    public boolean cs_planeAutoThrottleDown;
    public boolean cs_tankAutoThrottleDown;
    public MCH_Parts partHatch;
    public MCH_Parts partCanopy;
    public MCH_Parts partLandingGear;
    public boolean canRideRackStatus;
    public MCH_BoundingBox[] extraBoundingBox;
    @Nullable
    public String lastBBName;
    public float lastBBDamageFactor;
    public MCH_EntityAircraft.WeaponBay[] weaponBays;
    public float[] rotPartRotation;
    public float[] prevRotPartRotation;
    public float rotWheel = 0.0F;
    public float prevRotWheel = 0.0F;
    public float rotYawWheel = 0.0F;
    public float prevRotYawWheel = 0.0F;
    public float ropesLength = 0.0F;
    public float lastSearchLightYaw;
    public float lastSearchLightPitch;
    public float rotLightHatch = 0.0F;
    public float prevRotLightHatch = 0.0F;
    public int recoilCount = 0;
    public float recoilYaw = 0.0F;
    public float recoilValue = 0.0F;
    public int brightness = 240;
    public float thirdPersonDist = 4.0F;
    public Entity lastAttackedEntity = null;
    protected double velocityX;
    protected double velocityY;
    protected double velocityZ;
    protected int aircraftPosRotInc;
    protected double aircraftX;
    protected double aircraftY;
    protected double aircraftZ;
    protected double aircraftYaw;
    protected double aircraftPitch;
    @Getter
    protected MCH_WeaponSet[] weapons;
    protected int[] currentWeaponID;
    protected int useWeaponStat;
    @Getter
    protected int hitStatus;
    protected Entity lastRiddenByEntity;
    protected Entity lastRidingEntity;
    protected boolean isGunnerMode = false;
    protected boolean isGunnerModeOtherSeat = false;
    protected boolean isGunnerFreeLookMode = false;
    private MCH_AircraftInfo acInfo;
    private int commonStatus;
    private Entity[] partEntities;
    private MCH_EntitySeat[] seats;
    private MCH_SeatInfo[] seatsInfo;
    @Setter
    @Getter
    private String commonUniqueId;
    private int seatSearchCount;
    @Setter
    @Getter
    private double currentThrottle;
    @Getter
    private double prevCurrentThrottle;
    @Getter
    private int radarRotate;
    private int currentFlareIndex;
    @Getter
    private int countOnUpdate;
    @Setter
    private MCH_EntityChain towChainEntity;
    @Setter
    private MCH_EntityChain towedChainEntity;
    @Getter
    private int cameraId;
    @Getter
    private boolean isHoveringMode = false;
    private MCH_EntityTvMissile TVmissile;
    @Setter
    @Getter
    private int despawnCount;
    private MCH_EntityUavStation uavStation;
    @Setter
    @Getter
    private int modeSwitchCooldown;
    private double fuelConsumption;
    private int fuelSuppliedCount;
    private int supplyAmmoWait;
    private boolean beforeSupplyAmmo;
    private boolean isParachuting;
    private int tickRepelling;
    private int lastUsedRopeIndex;
    private boolean dismountedUserCtrl;
    private double lastCalcLandInDistanceCount;
    private double lastLandInDistance;
    private boolean switchSeat = false;

    public MCH_EntityAircraft(World world) {
        super(world);
        MCH_Logger.debugLog(world, "MCH_EntityAircraft : " + this);
        this.isRequestedSyncStatus = false;
        this.setAcInfo(null);
        this.dropContentsWhenDead = false;
        this.ignoreFrustumCheck = true;
        this.flareDv = new MCH_Flare(world, this);
        this.currentFlareIndex = 0;
        this.entityRadar = new MCH_Radar(world);
        this.radarRotate = 0;
        this.currentWeaponID = new int[0];
        this.aircraftPosRotInc = 0;
        this.aircraftX = 0.0;
        this.aircraftY = 0.0;
        this.aircraftZ = 0.0;
        this.aircraftYaw = 0.0;
        this.aircraftPitch = 0.0;
        this.currentSpeed = 0.0;
        this.setCurrentThrottle(0.0);
        this.cs_dismountAll = false;
        this.cs_heliAutoThrottleDown = true;
        this.cs_planeAutoThrottleDown = false;
        this._renderDistanceWeight = 2.0 * MCH_Config.RenderDistanceWeight.prmDouble;
        this.setCommonUniqueId("");
        this.seatSearchCount = 0;
        this.seatsInfo = null;
        this.seats = new MCH_EntitySeat[0];
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
        this.dummyWeapon = new MCH_WeaponSet(new MCH_WeaponDummy(this.world, Vec3d.ZERO, 0.0F, 0.0F, "", null));
        this.useWeaponStat = 0;
        this.hitStatus = 0;
        this.repairCount = 0;
        this.beforeDamageTaken = 0;
        this.timeSinceHit = 0;
        this.setDespawnCount(0);
        this.missileDetector = new MCH_MissileDetector(this, world);
        this.uavStation = null;
        this.modeSwitchCooldown = 0;
        this.partHatch = null;
        this.partCanopy = null;
        this.partLandingGear = null;
        this.weaponBays = new MCH_EntityAircraft.WeaponBay[0];
        this.rotPartRotation = new float[0];
        this.prevRotPartRotation = new float[0];
        this.lastRiderYaw = 0.0F;
        this.prevLastRiderYaw = 0.0F;
        this.lastRiderPitch = 0.0F;
        this.prevLastRiderPitch = 0.0F;
        this.rotationRoll = 0.0F;
        this.prevRotationRoll = 0.0F;
        this.lowPassPartialTicks = new MCH_LowPassFilterFloat(10);
        this.extraBoundingBox = new MCH_BoundingBox[0];
        this.setEntityBoundingBox(new MCH_AircraftBoundingBox(this));
        this.lastBBDamageFactor = 1.0F;
        this.lastBBName = null;
        this.inventory = new MCH_AircraftInventory(this);
        this.fuelConsumption = 0.0;
        this.fuelSuppliedCount = 0;
        this.canRideRackStatus = false;
        this.isParachuting = false;
        this.prevPosition = new MCH_Queue<>(10, Vec3d.ZERO);
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
                return uavStation.getControlAircract();
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

    public static float abs(float value) {
        return value >= 0.0F ? value : -value;
    }

    public static double abs(double value) {
        return value >= 0.0 ? value : -value;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double dist) {
        return true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ID_TYPE, "");
        this.dataManager.register(DAMAGE, 0);
        this.dataManager.register(STATUS, 0);
        this.dataManager.register(USE_WEAPON, 0);
        this.dataManager.register(FUEL, 0);
        this.dataManager.register(FUEL_FF, "");
        this.dataManager.register(TEXTURE_NAME, "");
        this.dataManager.register(UAV_STATION, 0);
        this.dataManager.register(ROT_ROLL, 0);
        this.dataManager.register(COMMAND, "");
        this.dataManager.register(THROTTLE, 0);
        this.dataManager.register(PART_STAT, 0);
        if (!this.world.isRemote) {
            this.setCommonStatus(3, MCH_Config.InfinityAmmo.prmBool);
            this.setCommonStatus(4, MCH_Config.InfinityFuel.prmBool);
            this.setGunnerStatus(true);
        }

        this.getEntityData().setString("EntityType", this.getEntityType());
    }

    public float getServerRoll() {
        return this.dataManager.get(ROT_ROLL).shortValue();
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
        return this.rotationRoll;
    }

    public void setRotRoll(float f) {
        this.rotationRoll = f;
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
        return this.prevRotationRoll + (this.getRoll() - this.prevRotationRoll) * partialTicks;
    }

    protected void setRotation(float y, float p) {
        this.setRotYaw(y % 360.0F);
        this.setRotPitch(p % 360.0F);
    }

    public boolean isInfinityAmmo(Entity player) {
        return this.isCreative(player) || this.getCommonStatus(3);
    }

    public boolean isInfinityFuel(Entity player, boolean checkOtherSeet) {
        if (!this.isCreative(player) && !this.getCommonStatus(4)) {
            if (checkOtherSeet) {
                for (MCH_EntitySeat seat : this.getSeats()) {
                    if (seat != null && this.isCreative(seat.getRiddenByEntity())) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public void setCommand(String s, EntityPlayer player) {
        if (!this.world.isRemote && MCH_Command.canUseCommand(player)) {
            this.setCommandForce(s);
        }
    }

    public void setCommandForce(String s) {
        if (!this.world.isRemote) {
            this.dataManager.set(COMMAND, s);
        }
    }

    public String getCommand() {
        return this.dataManager.get(COMMAND);
    }

    public String getKindName() {
        return "";
    }

    public String getEntityType() {
        return "";
    }

    public String getTypeName() {
        return this.dataManager.get(ID_TYPE);
    }

    public void setTypeName(String s) {
        String beforeType = this.getTypeName();
        if (s != null && !s.isEmpty() && s.compareTo(beforeType) != 0) {
            this.dataManager.set(ID_TYPE, s);
            this.changeType(s);
            this.initRotationYaw(this.getYaw());
        }
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
        return this.getAcInfo() != null && this.getAcInfo().alwaysCameraView;
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

    @Nullable
    public MCH_EntityUavStation getUavStation() {
        return this.isUAV() ? this.uavStation : null;
    }

    public void setUavStation(MCH_EntityUavStation uavSt) {
        this.uavStation = uavSt;
        if (!this.world.isRemote) {
            if (uavSt != null) {
                this.dataManager.set(UAV_STATION, W_Entity.getEntityId(uavSt));
            } else {
                this.dataManager.set(UAV_STATION, 0);
            }
        }
    }

    public boolean isCreative(@Nullable Entity entity) {
        return entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode || entity instanceof MCH_EntityGunner && ((MCH_EntityGunner) entity).isCreative;
    }

    @Nullable
    @Override
    public Entity getRiddenByEntity() {
        if (this.isUAV() && this.uavStation != null) {
            return this.uavStation.getRiddenByEntity();
        } else {
            List<Entity> passengers = this.getPassengers();
            return passengers.isEmpty() ? null : passengers.getFirst();
        }
    }

    public boolean getCommonStatus(int bit) {
        return (this.commonStatus >> bit & 1) != 0;
    }

    public void setCommonStatus(int bit, boolean b) {
        this.setCommonStatus(bit, b, false);
    }

    public void setCommonStatus(int bit, boolean b, boolean writeClient) {
        if (!this.world.isRemote || writeClient) {
            int bofore = this.commonStatus;
            int mask = 1 << bit;
            if (b) {
                this.commonStatus |= mask;
            } else {
                this.commonStatus &= ~mask;
            }

            if (bofore != this.commonStatus) {
                Object[] data = new Object[]{this.dataManager.get(STATUS), this.commonStatus};
                MCH_Logger.debugLog(this.world, "setCommonStatus : %08X -> %08X ", data);
                this.dataManager.set(STATUS, this.commonStatus);
            }
        }
    }

    public double getThrottle() {
        return 0.05 * this.dataManager.get(THROTTLE);
    }

    public void setThrottle(double t) {
        int n = (int) (t * 20.0);
        if (n == 0 && t > 0.0) {
            n = 1;
        }

        this.dataManager.set(THROTTLE, n);
    }

    public int getMaxHP() {
        return this.getAcInfo() != null ? this.getAcInfo().maxHp : 100;
    }

    public int getHP() {
        return Math.max(this.getMaxHP() - this.getDamageTaken(), 0);
    }

    public int getDamageTaken() {
        return this.dataManager.get(DAMAGE);
    }

    public void setDamageTaken(int par1) {
        if (par1 < 0) {
            par1 = 0;
        }

        if (par1 > this.getMaxHP()) {
            par1 = this.getMaxHP();
        }

        this.dataManager.set(DAMAGE, par1);
    }

    public void destroyAircraft() {
        this.setSearchLight(false);
        this.switchHoveringMode(false);
        this.switchGunnerMode(false);

        for (int i = 0; i < this.getSeatNum() + 1; i++) {
            Entity e = this.getEntityBySeatId(i);
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
            this.ejectSeat(this.getRiddenByEntity());
            Entity entity = this.getEntityBySeatId(1);
            if (entity != null) {
                this.ejectSeat(entity);
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
            return 15728880;
        } else {
            int i = MathHelper.floor(this.posX);
            int j = MathHelper.floor(this.posZ);
            if (this.world.isBlockLoaded(new BlockPos(i, 0, j))) {
                double d0 = (this.getEntityBoundingBox().maxY - this.getEntityBoundingBox().minY) * 0.66;
                float fo = this.getAcInfo() != null ? this.getAcInfo().submergedDamageHeight : 0.0F;
                if (this.canFloatWater()) {
                    fo = this.getAcInfo().floatOffset;
                    if (fo < 0.0F) {
                        fo = -fo;
                    }

                    fo++;
                }

                int k = MathHelper.floor(this.posY + fo + d0);
                int val = this.world.getCombinedLight(new BlockPos(i, k, j), 0);
                int low = val & 65535;
                int high = val >> 16 & 65535;
                if (high < this.brightness) {
                    if (this.getCountOnUpdate() % 2 == 0) {
                        this.brightness--;
                    }
                } else if (high > this.brightness) {
                    this.brightness += 4;
                    if (this.brightness > 240) {
                        this.brightness = 240;
                    }
                }

                return this.brightness << 16 | low;
            } else {
                return 0;
            }
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
                return sid > 0 && sid < this.getSeatsInfo().length && this.getSeatsInfo()[sid].invCamPos ? this.getSeatsInfo()[sid].getCamPos() : this.getAcInfo().cameraPosition.getFirst();
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
        this.prevRotationRoll = this.getRoll();
        this.prevLastRiderYaw = this.lastRiderYaw = nbt.getFloat("AcLastRYaw");
        this.prevLastRiderPitch = this.lastRiderPitch = nbt.getFloat("AcLastRPitch");
        this.setPartStatus(nbt.getInteger("PartStatus"));
        this.setTypeName(nbt.getString("TypeName"));
        super.readEntityFromNBT(nbt);
        this.getGuiInventory().readFromNBT(nbt);
        this.setCommandForce(nbt.getString("AcCommand"));
        this.setGunnerStatus(nbt.getBoolean("AcGunnerStatus"));
        this.setFuel(nbt.getInteger("AcFuel"));
        this.setFuelType(nbt.hasKey("AcFuelType") ? nbt.getString("AcFuelType") : "mch_fuel"); //Default to the default mcheli fuel
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

        this.dismountedUserCtrl = nbt.getBoolean("AcDismounted");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setString("TextureName", this.getTextureName());
        nbt.setString("AircraftUniqueId", this.getCommonUniqueId());
        nbt.setString("TypeName", this.getTypeName());
        nbt.setInteger("PartStatus", this.getPartStatus() & this.getLastPartStatusMask());
        nbt.setInteger("AcFuel", this.getFuel());
        nbt.setString("AcFuelType", this.getFuelType());
        nbt.setInteger("AcDespawnCount", this.getDespawnCount());
        nbt.setFloat("AcRoll", this.getRoll());
        nbt.setBoolean("SearchLight", this.isSearchLightON());
        nbt.setFloat("AcLastRYaw", this.getLastRiderYaw());
        nbt.setFloat("AcLastRPitch", this.getLastRiderPitch());
        nbt.setString("AcCommand", this.getCommand());
        if (!nbt.hasKey("AcGunnerStatus")) {
            this.setGunnerStatus(true);
        }

        nbt.setBoolean("AcGunnerStatus", this.getGunnerStatus());
        super.writeEntityToNBT(nbt);
        this.getGuiInventory().writeToNBT(nbt);
        int[] wa_list = new int[this.getWeaponNum()];

        for (int i = 0; i < wa_list.length; i++) {
            wa_list[i] = this.getWeapon(i).getRestAllAmmoNum() + this.getWeapon(i).getAmmo();
        }

        nbt.setTag("AcWeaponsAmmo", W_NBTTag.newTagIntArray("AcWeaponsAmmo", wa_list));
        nbt.setInteger("AcDamage", this.getDamageTaken());
        nbt.setBoolean("AcDismounted", this.dismountedUserCtrl);
    }

    @Override
    public boolean attackEntityFrom(@NotNull DamageSource damageSource, float originalDamage) {
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
                MCH_Explosion.newExplosion(this.world, null, attacker, this.posX, this.posY, this.posZ, 2.0F, 2.0F, true, true, true, true, 5);
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
            nbt.setInteger("MCH_Fuel", this.getFuel());
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
                this.setFuel(nbt.getInteger("MCH_Fuel"));
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
        this.velocityX = this.motionX = par1;
        this.velocityY = this.motionY = par3;
        this.velocityZ = this.motionZ = par5;
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
            pilot.rotationYaw = this.getLastRiderYaw();
            pilot.rotationPitch = this.getLastRiderPitch();
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

        this.prevRotationRoll = this.getRoll();
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
            this.aircraftRotChanged = true;
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
                float steeringRotation = searchLight.steering ? rotYawWheel * searchLight.stRot : 0.0F;

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
        if (this.getCountOnUpdate() < 2) {
            this.prevPosition.clear(new Vec3d(this.posX, this.posY, this.posZ));
        }

        this.prevCurrentThrottle = this.getCurrentThrottle();
        this.lastBBDamageFactor = 1.0F;
        this.updateControl();
        this.checkServerNoMove();
        this.onUpdate_RidingEntity();

        //Unmount iterator
        Iterator<MCH_EntityAircraft.UnmountReserve> itr = this.listUnmountReserve.iterator();
        while (itr.hasNext()) {
            MCH_EntityAircraft.UnmountReserve ur = itr.next();
            if (ur.entity != null && !ur.entity.isDead) {
                ur.entity.setPosition(ur.posX, ur.posY, ur.posZ);
                ur.entity.fallDistance = this.fallDistance;
            }

            if (ur.cnt > 0) {
                ur.cnt--;
            }

            if (ur.cnt == 0) {
                itr.remove();
            }
        }

        //Apply damage to passengers in destroyed vehicle
        if (this.isDestroyed() && this.getCountOnUpdate() % 20 == 0) {
            for (int sid = 0; sid < this.getSeatNum() + 1; sid++) {
                Entity entity = this.getEntityBySeatId(sid);
                if (entity != null && (sid != 0 || !this.isUAV()) && MCH_Config.applyDamageVsEntity(entity, DamageSource.IN_FIRE, 1.0F) > 0.0F) {
                    entity.setFire(5);
                }
            }
        }

        //Apply raotation
        if ((this.aircraftRotChanged || this.aircraftRollRev) && this.world.isRemote && this.getRiddenByEntity() != null) {
            PacketIndRotation.send(this);
            this.aircraftRotChanged = false;
            this.aircraftRollRev = false;
        }

        //Apply roll
        if (!this.world.isRemote && (int) this.prevRotationRoll != (int) this.getRoll()) {
            float roll = MathHelper.wrapDegrees(this.getRoll());
            this.dataManager.set(ROT_ROLL, (int) roll);
        }
        this.prevRotationRoll = this.getRoll();

        //Target drone specific
        if (!this.world.isRemote && this.isTargetDrone() && !this.isDestroyed() && this.getCountOnUpdate() > 20 && !this.canUseFuel()) {
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

        this.updateNoCollisionEntities();
        this.updateUAV();
        this.supplyFuel();
        this.supplyAmmoToOtherAircraft();
        this.updateFuel();
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

        if (this.world.isRemote) {
            this.commonStatus = this.dataManager.get(STATUS);
        }

        this.fallDistance = 0.0F;
        Entity riddenByEntity = this.getRiddenByEntity();
        if (riddenByEntity != null) {
            riddenByEntity.fallDistance = 0.0F;
        }

        if (this.missileDetector != null) {
            this.missileDetector.update();
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
        this.flareDv.update();
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

        if (this.recoilCount > 0) {
            this.recoilCount--;
        }

        if (!W_Entity.isEqual(MCH_MOD.proxy.getClientPlayer(), this.getRiddenByEntity())) {
            this.updateRecoil(1.0F);
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
        this.onUpdate_Repelling();
        this.checkRideRack();
        if (this.lastRidingEntity == null && this.getRidingEntity() != null) {
            this.onRideEntity(this.getRidingEntity());
        }

        this.lastRiddenByEntity = this.getRiddenByEntity();
        this.lastRidingEntity = this.getRidingEntity();
        this.prevPosition.put(new Vec3d(this.posX, this.posY, this.posZ));
    }

    private void updateNoCollisionEntities() {
        if (!this.world.isRemote) {
            if (this.getCountOnUpdate() % 10 == 0) {
                for (int i = 0; i < 1 + this.getSeatNum(); i++) {
                    Entity e = this.getEntityBySeatId(i);
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
        if (!this.world.isRemote) {
            this.setCommonStatus(7, this.moveLeft);
            this.setCommonStatus(8, this.moveRight);
            this.setCommonStatus(9, this.throttleUp);
            this.setCommonStatus(10, this.throttleDown);
        } else if (MCH_MOD.proxy.getClientPlayer() != this.getRiddenByEntity()) {
            this.moveLeft = this.getCommonStatus(7);
            this.moveRight = this.getCommonStatus(8);
            this.throttleUp = this.getCommonStatus(9);
            this.throttleDown = this.getCommonStatus(10);
        }
    }

    public void updateRecoil(float partialTicks) {
        if (this.recoilCount > 0 && this.recoilCount >= 12) {
            float pitch = MathHelper.cos((float) ((this.recoilYaw - this.getRoll()) * Math.PI / 180.0));
            float roll = MathHelper.sin((float) ((this.recoilYaw - this.getRoll()) * Math.PI / 180.0));
            float recoil = MathHelper.cos((float) (this.recoilCount * 6 * Math.PI / 180.0)) * this.recoilValue;
            this.setRotPitch(this.getPitch() + recoil * pitch * partialTicks);
            this.setRotRoll(this.getRoll() + recoil * roll * partialTicks);
        }
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
        if (!world.isRemote) return;

        var acInfo = getAcInfo();
        if (acInfo == null) return;

        prevRotWheel = rotWheel;
        prevRotYawWheel = rotYawWheel;

        // Compute throttle
        double throttle = getCurrentThrottle();
        double pivotThrottle = acInfo.pivotTurnThrottle;
//        pivotThrottle = pivotThrottle <= 0.0 ? 1.0 : pivotThrottle * 0.1;

        // Handle backward throttle
        if (acInfo.enableBack && throttleBack > 0.01 && throttle <= 0.0) {
            throttle = -throttleBack * 15.0;
        }

        // Handle wheel yaw based on input
        if (moveLeft && !moveRight) {
            rotYawWheel = (float) Math.min(rotYawWheel + 0.1, 1.0);
        } else if (!moveLeft && moveRight) {
            rotYawWheel = (float) Math.max(rotYawWheel - 0.1, -1.0);
        } else {
            rotYawWheel *= 0.9F;
        }

        // Update wheel rotation
        rotWheel += (float) (throttle * acInfo.partWheelRot);

        // Normalize to [0, 360)
        if (rotWheel >= 360.0) {
            rotWheel -= 360.0F;
            prevRotWheel -= 360.0F;
        } else if (rotWheel < 0.0) {
            rotWheel += 360.0F;
            prevRotWheel += 360.0F;
        }
    }


    public void updatePartCrawlerTrack() {
        if (!world.isRemote) return;

        final var info = this.getAcInfo();
        if (info == null) return;

        // Save previous rotation states
        prevRotTrackRoller[0] = rotTrackRoller[0];
        prevRotTrackRoller[1] = rotTrackRoller[1];
        prevRotCrawlerTrack[0] = rotCrawlerTrack[0];
        prevRotCrawlerTrack[1] = rotCrawlerTrack[1];

        double throttle = getCurrentThrottle();
        double pivotTurnThrottle = info.pivotTurnThrottle <= 0.0 ? 1.0 : info.pivotTurnThrottle * 0.1;

        boolean moveLeft = this.moveLeft;
        boolean moveRight = this.moveRight;
        int dir = 1;

        // Handle backward throttle
        if (info.enableBack && throttleBack > 0.0F && throttle <= 0.0) {
            throttle = -throttleBack * 5.0F;

            // Swap track directions when steering backward
            if (moveLeft != moveRight) {
                boolean tmp = moveLeft;
                moveLeft = moveRight;
                moveRight = tmp;
                dir = -1;
            }
        }

        // Apply steering or normal movement throttle
        if (moveLeft && !moveRight) {
            // Pivot: left-track forward, right-track delayed/negative
            double t = 0.2 * dir;
            throttleCrawlerTrack[0] += (float) t;
            throttleCrawlerTrack[1] -= (float) (pivotTurnThrottle * t);
        } else if (!moveLeft && moveRight) {
            // Pivot: right-track forward, left-track delayed/negative
            double t = 0.2 * dir;
            throttleCrawlerTrack[0] -= (float) (pivotTurnThrottle * t);
            throttleCrawlerTrack[1] += (float) t;
        } else {
            // Normal driving
            if (throttle > 0.2) throttle = 0.2;
            else if (throttle < -0.2) throttle = -0.2;

            throttleCrawlerTrack[0] += (float) throttle;
            throttleCrawlerTrack[1] += (float) throttle;
        }

        // Per-track update loop
        for (int i = 0; i < 2; i++) {
            // Clamp throttle range
            float t = throttleCrawlerTrack[i];
            if (t < -0.72F) t = -0.72F;
            else if (t > 0.72F) t = 0.72F;
            throttleCrawlerTrack[i] = t;

            // Roller rotation update
            float rot = rotTrackRoller[i] + t * info.trackRollerRot;
            float prevRot = prevRotTrackRoller[i];

            // Wrap 0–360
            if (rot >= 360.0F) {
                rot -= 360.0F;
                prevRot -= 360.0F;
            } else if (rot < 0.0F) {
                rot += 360.0F;
                prevRot += 360.0F;
            }

            rotTrackRoller[i] = rot;
            prevRotTrackRoller[i] = prevRot;

            // Track movement rotation (0–1 cycling)
            float trackRot = rotCrawlerTrack[i] - t;
            float prevTrackRot = prevRotCrawlerTrack[i];

            while (trackRot >= 1.0F) {
                trackRot--;
                prevTrackRot--;
            }
            while (trackRot < 0.0F) trackRot++;
            while (prevTrackRot < 0.0F) prevTrackRot++;

            rotCrawlerTrack[i] = trackRot;
            prevRotCrawlerTrack[i] = prevTrackRot;

            // Decay throttle
            throttleCrawlerTrack[i] = t * 0.75F;
        }
    }


    public void checkServerNoMove() {
        if (!this.world.isRemote) {
            double moti = this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ;
            if (moti < 1.0E-4) {
                if (this.serverNoMoveCount < 20) {
                    this.serverNoMoveCount++;
                    if (this.serverNoMoveCount >= 20) {
                        this.serverNoMoveCount = 0;
                        if (this.world instanceof WorldServer) {
                            ((WorldServer) this.world).getEntityTracker().sendToTracking(this, new SPacketEntityVelocity(this.getEntityId(), 0.0, 0.0, 0.0));
                        }
                    }
                }
            } else {
                this.serverNoMoveCount = 0;
            }
        }
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

    public int getAlt(double posX, double posY, double posZ) {
        int i = 0;
        while (i < 256 && !(posY - i <= 0.0) && (!(posY - i < 256.0) || 0 == W_WorldFunc.getBlockId(this.world, (int) posX, (int) posY - i, (int) posZ))) {
            i++;
        }
        return i;
    }

    public boolean canRepell() {
        return this.isRepelling() && this.tickRepelling > 50;
    }

    private void onUpdate_Repelling() {
        if (this.getAcInfo() != null && this.getAcInfo().haveRepellingHook()) {
            if (this.isRepelling()) {
                int alt = this.getAlt(this.posX, this.posY, this.posZ);
                if (this.ropesLength > -50.0F && this.ropesLength > -alt) {
                    this.ropesLength = (float) (this.ropesLength - (this.world.isRemote ? 0.3F : 0.25));
                }
            } else {
                this.ropesLength = 0.0F;
            }
        }

        this.onUpdate_UnmountCrewRepelling();
    }

    private void onUpdate_UnmountCrewRepelling() {
        if (this.getAcInfo() != null) {
            if (!this.isRepelling()) {
                this.tickRepelling = 0;
            } else if (this.tickRepelling < 60) {
                this.tickRepelling++;
            } else if (!this.world.isRemote) {
                for (int ropeIdx = 0; ropeIdx < this.getAcInfo().repellingHooks.size(); ropeIdx++) {
                    MCH_AircraftInfo.RepellingHook hook = this.getAcInfo().repellingHooks.get(ropeIdx);
                    if (this.getCountOnUpdate() % hook.interval() == 0) {
                        for (int i = 1; i < this.getSeatNum(); i++) {
                            MCH_EntitySeat seat = this.getSeat(i);
                            if (seat != null && seat.getRiddenByEntity() != null && !W_EntityPlayer.isPlayer(seat.getRiddenByEntity()) && !(seat.getRiddenByEntity() instanceof MCH_EntityGunner) && !(this.getSeatInfo(i + 1) instanceof MCH_SeatRackInfo)) {
                                Entity entity = seat.getRiddenByEntity();
                                Vec3d dropPos = this.getTransformedPosition(hook.pos(), this.prevPosition.oldest());
                                seat.posX = dropPos.x;
                                seat.posY = dropPos.y - 2.0;
                                seat.posZ = dropPos.z;
                                entity.dismountRidingEntity();
                                this.unmountEntityRepelling(entity, dropPos, ropeIdx);
                                this.lastUsedRopeIndex = ropeIdx;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void unmountEntityRepelling(Entity entity, Vec3d dropPos, int ropeIdx) {
        entity.posX = dropPos.x;
        entity.posY = dropPos.y - 2.0;
        entity.posZ = dropPos.z;
        MCH_EntityHide hideEntity = new MCH_EntityHide(this.world, entity.posX, entity.posY, entity.posZ);
        hideEntity.setParent(this, entity, ropeIdx);
        hideEntity.motionX = entity.motionX = 0.0;
        hideEntity.motionY = entity.motionY = 0.0;
        hideEntity.motionZ = entity.motionZ = 0.0;
        hideEntity.fallDistance = entity.fallDistance = 0.0F;
        this.world.spawnEntity(hideEntity);
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

    public void unmountAircraft() {
        Vec3d v = new Vec3d(this.posX, this.posY, this.posZ);
        if (this.getRidingEntity() instanceof MCH_EntitySeat) {
            MCH_EntityAircraft ac = ((MCH_EntitySeat) this.getRidingEntity()).getParent();
            assert ac != null;
            MCH_SeatInfo seatInfo = ac.getSeatInfo(this);
            if (seatInfo instanceof MCH_SeatRackInfo) {
                if (seatInfo.unmountPos != null) {
                    v = seatInfo.unmountPos;
                } else {
                    v = ((MCH_SeatRackInfo) seatInfo).getEntryPos();
                }
                v = ac.getTransformedPosition(v);
            }
        } else if (this.getRidingEntity() instanceof EntityMinecartEmpty) {
            this.dismountedUserCtrl = true;
        }

        this.setLocationAndAngles(v.x, v.y, v.z, this.getYaw(), this.getPitch());
        this.dismountRidingEntity();
        this.setLocationAndAngles(v.x, v.y, v.z, this.getYaw(), this.getPitch());
    }

    public boolean canUnmount(Entity entity) {
        if (this.getAcInfo() == null) {
            return false;
        } else if (!this.getAcInfo().isEnableParachuting) {
            return false;
        } else {
            return this.getSeatIdByEntity(entity) > 1 && (!this.haveHatch() || !(this.getHatchRotation() < 89.0F));
        }
    }

    //TODO: Per seat dismount, raycast dismount tests
    public void unmount(Entity entity) {
        final var info = getAcInfo();
        if (info == null) return;

        final boolean repelling = canRepell() && info.haveRepellingHook();
        final boolean normalUnmount = !repelling && canUnmount(entity);

        if (!repelling && !normalUnmount) return;

        final MCH_EntitySeat seat = getSeatByEntity(entity);
        if (seat == null) {
            MCH_Logger.log(this, "Error:MCH_EntityAircraft.unmount seat=null : " + entity);
            return;
        }

        // Determine position to drop the entity
        final Vec3d hookPos;
        final Vec3d dropPos;

        if (repelling) {
            // Select next rope hook
            lastUsedRopeIndex = (lastUsedRopeIndex + 1) % info.repellingHooks.size();
            hookPos = info.repellingHooks.get(lastUsedRopeIndex).pos();

            dropPos = getTransformedPosition(hookPos, prevPosition.oldest()).add(0.0, -2.0, 0.0);

        } else {
            MCH_SeatInfo seatInfo = getSeatInfo(seat.seatID + 1);
            if (seatInfo != null && seatInfo.unmountPos != null) {
                hookPos = seatInfo.unmountPos;
            } else {
                hookPos = info.mobDropOption.pos;
            }
            dropPos = getTransformedPosition(hookPos, prevPosition.oldest());
        }

        // Update seat position
        seat.posX = dropPos.x;
        seat.posY = dropPos.y;
        seat.posZ = dropPos.z;

        // Dismount and place entity
        entity.dismountRidingEntity();
        entity.posX = dropPos.x;
        entity.posY = dropPos.y;
        entity.posZ = dropPos.z;

        if (repelling) {
            unmountEntityRepelling(entity, dropPos, lastUsedRopeIndex);
        } else {
            dropEntityParachute(entity);
        }
    }


    public boolean canParachute(Entity entity) {
        if (this.getAcInfo() == null || !this.getAcInfo().isEnableParachuting || this.getSeatIdByEntity(entity) <= 1 || MCH_Lib.getBlockIdY(this, 3, -13) != 0) {
            return false;
        } else {
            return this.getSeatIdByEntity(entity) > 1;
        }
    }

    public void onUpdate_RidingEntity() {
        if (!this.world.isRemote && this.waitMountEntity == 0 && this.getCountOnUpdate() > 20 && this.canMountWithNearEmptyMinecart()) {
            this.mountWithNearEmptyMinecart();
        }

        if (this.waitMountEntity > 0) {
            this.waitMountEntity--;
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
                this.waitMountEntity = 20;
            } else if (this.getCurrentThrottle() > 0.8) {
                this.motionX = this.getRidingEntity().motionX;
                this.motionY = this.getRidingEntity().motionY;
                this.motionZ = this.getRidingEntity().motionZ;
                this.dismountRidingEntity();
                this.waitMountEntity = 20;
            }

            this.posX = bkPosX;
            this.posY = bkPosY;
            this.posZ = bkPosZ;
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
        double rpinc = this.aircraftPosRotInc;
        double yaw = MathHelper.wrapDegrees(this.aircraftYaw - this.getYaw());
        double roll = MathHelper.wrapDegrees(this.getServerRoll() - this.getRoll());
        if (!this.isDestroyed() && (!W_Lib.isClientPlayer(this.getRiddenByEntity()) || this.getRidingEntity() != null)) {
            this.setRotYaw((float) (this.getYaw() + yaw / rpinc));
            this.setRotPitch((float) (this.getPitch() + (this.aircraftPitch - this.getPitch()) / rpinc));
            this.setRotRoll((float) (this.getRoll() + roll / rpinc));
        }

        this.setPosition(this.posX + (this.aircraftX - this.posX) / rpinc, this.posY + (this.aircraftY - this.posY) / rpinc, this.posZ + (this.aircraftZ - this.posZ) / rpinc);
        this.setRotation(this.getYaw(), this.getPitch());
        this.aircraftPosRotInc--;
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

    public int getFuel() {
        return this.dataManager.get(FUEL);
    }

    public void setFuel(int fuel) {
        if (this.world.isRemote) return;
        if (fuel < 0) fuel = 0;
        if (fuel > this.getMaxFuel()) fuel = this.getMaxFuel();
        if (fuel != this.getFuel()) this.dataManager.set(FUEL, fuel);
    }

    @Nullable
    public FluidStack getFuelStack() {
        if (this.getFuel() == 0 || getFuelType().isEmpty()) return null;

        if (!FluidRegistry.isFluidRegistered(getFuelType())) {
            MCH_Logger.warn("Fluid: {} does not exist, make sure required fluids do exist. Fluid will be cleared");
            return null;
        }
        return new FluidStack(FluidRegistry.getFluid(getFuelType()), getFuel());
    }

    @Nullable
    public Fluid getFuelFluid() {
        if (getFuel() < 0)
            return null;
        String fluidName = getFuelType();

        var fluid = fluidName.isBlank() ? null : FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            MCH_Logger.warn("Retrieved fluid: {} does not exist, make sure required fluids do exist.", fluidName);
        }

        return fluid;
    }

    public String getFuelType() {
        return this.dataManager.get(FUEL_FF);
    }

    public void setFuelType(String name) {
        if (!name.isBlank() && FluidRegistry.getFluid(name) == null) {
            MCH_Logger.warn("Set fluid: {} does not exist, make sure required fluids do exist. Fluid will be set to default", name);
            this.dataManager.set(FUEL_FF, "mch_fuel");
        } else this.dataManager.set(FUEL_FF, name);
    }

    public void setFuelType(Fluid fluid) {
        if (fluid == null) return;
        this.dataManager.set(FUEL_FF, fluid.getName());
    }

    public float getFuelPercentage() {
        int m = this.getMaxFuel();
        return m == 0 ? 0.0F : (float) this.getFuel() / m;
    }

    public boolean canUseFuel(boolean checkOtherSeet) {
        return this.getMaxFuel() <= 0 || this.getFuel() > 1 || this.isInfinityFuel(this.getRiddenByEntity(), checkOtherSeet);
    }

    public boolean canUseFuel() {
        return this.canUseFuel(false);
    }

    public int getMaxFuel() {
        return this.getAcInfo() != null ? this.getAcInfo().maxFuel : 0;
    }

    public void supplyFuel() {
        MCH_AircraftInfo info = this.getAcInfo();
        if (info == null || info.fuelSupplyRange <= 0.0F) return;
        if (this.world.isRemote || this.getCountOnUpdate() % 10 != 0) return;

        Fluid fluid;
        if (info.fuelSupplyInfinite) {
            fluid = FluidRegistry.getFluid(info.fuelSupplyType);
        } else {
            fluid = this.getFuelFluid();
        }

        if (fluid == null) return;
        if (!info.fuelSupplyInfinite && this.getFuel() <= 0) return;

        float range = info.fuelSupplyRange;
        List<MCH_EntityAircraft> list = this.world.getEntitiesWithinAABB(
                MCH_EntityAircraft.class,
                this.getCollisionBoundingBox().grow(range, range, range)
        );

        for (MCH_EntityAircraft ac : list) {
            if (W_Entity.isEqual(this, ac)) continue;

            if (ac.canAcceptFuel(fluid)) {
                if ((!this.onGround || ac.canSupply()) && ac.getFuel() < ac.getMaxFuel()) {
                    int amount = Math.min(ac.getMaxFuel() - ac.getFuel(), 30);

                    if (ac.getFuel() <= 0) {
                        ac.setFuelType(fluid);
                    }

                    ac.setFuel(ac.getFuel() + amount);

                    if (!info.fuelSupplyInfinite) {
                        this.setFuel(Math.max(0, this.getFuel() - amount));
                    }
                }
                ac.fuelSuppliedCount = 40;
            }
        }
    }

    public boolean canAcceptFuel(Fluid fluid) {
        var info = getAcInfo();
        if (info == null || fluid == null) return false;

        if (!info.isFuelValid(new FluidStack(fluid, 1))) return false;

        Fluid currentType = getFuelFluid();
        return getFuel() <= 0 || currentType == null || currentType == fluid;
    }

    public void updateFuel() {
        if (getMaxFuel() == 0 || isDestroyed() || world.isRemote) return;

        if (fuelSuppliedCount > 0) fuelSuppliedCount--;

        //Consumption Logic
        if (getCountOnUpdate() % 20 == 0 && getFuel() > 1 && getThrottle() > 0.0 && fuelSuppliedCount <= 0) {
            var acInfo = getAcInfo();
            if (acInfo != null) {
                double throttleFactor = Math.min(getThrottle() * 1.4, 1.0);
                fuelConsumption += throttleFactor * acInfo.fuelConsumption * getFuelConsumptionFactor();

                if (fuelConsumption > 1.0) {
                    int toConsume = (int) fuelConsumption;
                    fuelConsumption -= toConsume;
                    setFuel(getFuel() - toConsume);
                }
            }
        }

        //Refueling Logic
        int currentFuel = getFuel();
        int maxFuel = getMaxFuel();

        if (canSupply() && getCountOnUpdate() % 10 == 0 && currentFuel < maxFuel) {
            MCH_AircraftInventory inventory = getGuiInventory();
            ItemStack inputStack = inventory.getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_IN);
            if (inputStack.isEmpty()) return;

            if (inputStack.getItem() instanceof MCH_ItemFuel) {
                handleItemFuelRefuel(inventory, inputStack, currentFuel, maxFuel);
                return;
            }

            IFluidHandlerItem handler = inputStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (handler == null) return;

            IFluidTankProperties tank = Arrays.stream(handler.getTankProperties())
                    .filter(p -> p.canDrain() && p.getContents() != null && canAcceptFuel(p.getContents().getFluid()))
                    .findFirst().orElse(null);

            if (tank == null) return;

            int fuelNeeded = maxFuel - currentFuel;
            Fluid fluid = tank.getContents().getFluid();

            FluidStack drained = handler.drain(new FluidStack(fluid, fuelNeeded), true);
            if (drained == null || drained.amount <= 0) return;

            ItemStack emptyContainer = handler.getContainer();
            ItemStack outputStack = inventory.getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_OUT);

            if (manageOutputSlot(inventory, outputStack, emptyContainer)) {
                inputStack.shrink(1);
                setFuelType(fluid);
                setFuel(currentFuel + drained.amount);
                triggerRefuelCriteria();
            }
        }
    }

    private void handleItemFuelRefuel(MCH_AircraftInventory inventory, ItemStack stack, int current, int max) {
        Fluid fuelType = FluidRegistry.getFluid("mch_fuel");
        if (!canAcceptFuel(fuelType)) return;

        int fuelInItem = stack.getMaxDamage() - stack.getItemDamage();
        if (fuelInItem <= 0) return;

        int needed = max - current;
        int toTransfer = Math.min(needed, fuelInItem);

        stack.setItemDamage(stack.getItemDamage() + toTransfer);
        setFuelType(fuelType);
        setFuel(current + toTransfer);

        if (stack.getItemDamage() >= stack.getMaxDamage()) {
            ItemStack empty = new ItemStack(stack.getItem());
            empty.setItemDamage(stack.getMaxDamage());

            ItemStack outputStack = inventory.getFuelSlotItemStack(MCH_AircraftInventory.SLOT_FUEL_OUT);
            if (manageOutputSlot(inventory, outputStack, empty)) {
                stack.shrink(1);
            }
        }
        triggerRefuelCriteria();
    }

    private void triggerRefuelCriteria() {
        if (getRiddenByEntity() instanceof EntityPlayerMP player) {
            MCH_CriteriaTriggers.SUPPLY_FUEL.trigger(player);
        }
    }

    private boolean manageOutputSlot(MCH_AircraftInventory inv, ItemStack out, ItemStack container) {
        if (out.isEmpty()) {
            inv.getItemHandler().setStackInSlot(MCH_AircraftInventory.SLOT_FUEL_OUT, container.copy());
            return true;
        }
        if (ItemStack.areItemsEqual(out, container) && ItemStack.areItemStackTagsEqual(out, container)
                && out.getCount() < out.getMaxStackSize()) {
            out.grow(container.getCount());
            return true;
        }
        return false;
    }

    public void dumpFuel() {
        if (world.isRemote) return;
        this.dataManager.set(FUEL, 0);
        this.dataManager.set(FUEL_FF, "");
    }

    public float getFuelConsumptionFactor() {
        var ac = getAcInfo();
        if (ac == null) return 1F;
        return ac.getFuelConsumption(this.getFuelType());
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

    public String getTextureName() {
        return this.dataManager.get(TEXTURE_NAME);
    }

    public void setTextureName(@Nullable String name) {
        if (name != null && !name.isEmpty()) {
            this.dataManager.set(TEXTURE_NAME, name);
        }

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
        for (int i = 0; i < this.getSeatNum() + 1; i++) {
            this.camera.updateViewer(i, this.getEntityBySeatId(i));
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

    public boolean useFlare(int type) {
        if (this.getAcInfo() != null && this.getAcInfo().haveFlare()) {
            for (int i : this.getAcInfo().flare.types) {
                if (i == type) {
                    this.setCommonStatus(0, true);
                    if (this.flareDv.use(type)) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public int getCurrentFlareType() {
        if (!this.haveFlare()) {
            return 0;
        } else {
            assert this.getAcInfo() != null;
            return this.getAcInfo().flare.types[this.currentFlareIndex];
        }
    }

    public void nextFlareType() {
        if (this.haveFlare()) {
            assert this.getAcInfo() != null;
            this.currentFlareIndex = (this.currentFlareIndex + 1) % this.getAcInfo().flare.types.length;
        }
    }

    public boolean canUseFlare() {
        if (this.getAcInfo() == null || !this.getAcInfo().haveFlare()) {
            return false;
        } else {
            return !this.getCommonStatus(0) && this.flareDv.tick == 0;
        }
    }

    public boolean isFlarePreparation() {
        return this.flareDv.isInPreparation();
    }

    public boolean isFlareUsing() {
        return this.flareDv.isUsing();
    }

    public int getFlareTick() {
        return this.flareDv.tick;
    }

    public boolean haveFlare() {
        return this.getAcInfo() != null && this.getAcInfo().haveFlare();
    }

    public MCH_EntitySeat[] getSeats() {
        return this.seats != null ? this.seats : seatsDummy;
    }

    public int getSeatIdByEntity(@Nullable Entity entity) {
        if (entity == null) {
            return -1;
        } else if (isEqual(this.getRiddenByEntity(), entity)) {
            return 0;
        } else {
            for (int i = 0; i < this.getSeats().length; i++) {
                MCH_EntitySeat seat = this.getSeats()[i];
                if (seat != null && isEqual(seat.getRiddenByEntity(), entity)) {
                    return i + 1;
                }
            }

            return -1;
        }
    }

    @Nullable
    public MCH_EntitySeat getSeatByEntity(@Nullable Entity entity) {
        int idx = this.getSeatIdByEntity(entity);
        return idx > 0 ? this.getSeat(idx - 1) : null;
    }

    @Nullable
    public Entity getEntityBySeatId(int id) {
        if (id == 0) {
            return this.getRiddenByEntity();
        } else {
            id--;
            if (id >= 0 && id < this.getSeats().length) {
                return this.seats[id] != null ? this.seats[id].getRiddenByEntity() : null;
            } else {
                return null;
            }
        }
    }

    @Nullable
    public EntityPlayer getEntityByWeaponId(int id) {
        if (id >= 0 && id < this.getWeaponNum()) {
            for (int i = 0; i < this.currentWeaponID.length; i++) {
                if (this.currentWeaponID[i] == id) {
                    Entity e = this.getEntityBySeatId(i);
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
                entity = this.getEntityBySeatId(this.getWeaponSeatID(null, weapon));
                if (entity == null && weapon.canUsePilot) {
                    entity = this.getRiddenByEntity();
                }
            }

            return entity;
        }
    }

    protected void newSeats(int seatsNum) {
        if (seatsNum >= 2) {
            if (this.seats != null) {
                for (int i = 0; i < this.seats.length; i++) {
                    if (this.seats[i] != null) {
                        this.seats[i].setDead();
                        this.seats[i] = null;
                    }
                }
            }

            this.seats = new MCH_EntitySeat[seatsNum - 1];
        }
    }

    @Nullable
    public MCH_EntitySeat getSeat(int idx) {
        return idx < this.seats.length ? this.seats[idx] : null;
    }

    public void setSeat(int idx, MCH_EntitySeat seat) {
        if (idx < this.seats.length) {
            String format = "MCH_EntityAircraft.setSeat SeatID=" + idx + " / seat[]" + (this.seats[idx] != null) + " / " + (seat.getRiddenByEntity() != null);
            MCH_Logger.debugLog(this.world, format);
            if (this.seats[idx] != null) {
                this.seats[idx].getRiddenByEntity();
            }

            this.seats[idx] = seat;
        }
    }

    public boolean isValidSeatID(int seatID) {
        return seatID >= 0 && seatID < this.getSeatNum() + 1;
    }

    public void updateHitBoxPosition() {
    }

    public void updateSeatsPosition(double px, double py, double pz, boolean setPrevPos) {
        MCH_SeatInfo[] info = this.getSeatsInfo();
        py += GLOBAL_SEAT_OFFSET;
        if (this.pilotSeat != null && !this.pilotSeat.isDead) {
            this.pilotSeat.prevPosX = this.pilotSeat.posX;
            this.pilotSeat.prevPosY = this.pilotSeat.posY;
            this.pilotSeat.prevPosZ = this.pilotSeat.posZ;
            this.pilotSeat.setPosition(px, py, pz);
            if (info != null && info.length > 0 && info[0] != null) {
                Vec3d v = this.getTransformedPosition(info[0].pos.x, info[0].pos.y, info[0].pos.z, px, py, pz, info[0].rotSeat);
                this.pilotSeat.setPosition(v.x, v.y, v.z);
            }

            this.pilotSeat.rotationPitch = this.getPitch();
            this.pilotSeat.rotationYaw = this.getYaw();
            if (setPrevPos) {
                this.pilotSeat.prevPosX = this.pilotSeat.posX;
                this.pilotSeat.prevPosY = this.pilotSeat.posY;
                this.pilotSeat.prevPosZ = this.pilotSeat.posZ;
            }
        }

        int i = 0;

        for (MCH_EntitySeat seat : this.seats) {
            i++;
            if (seat != null && !seat.isDead) {
                float offsetY = -0.5F;


                seat.prevPosX = seat.posX;
                seat.prevPosY = seat.posY;
                seat.prevPosZ = seat.posZ;
                MCH_SeatInfo si = i < info.length ? info[i] : info[0];
                Vec3d v = this.getTransformedPosition(si.pos.x, si.pos.y + offsetY, si.pos.z, px, py, pz, si.rotSeat);
                seat.setPosition(v.x, v.y, v.z);
                seat.rotationPitch = this.getPitch();
                seat.rotationYaw = this.getYaw();
                if (setPrevPos) {
                    seat.prevPosX = seat.posX;
                    seat.prevPosY = seat.posY;
                    seat.prevPosZ = seat.posZ;
                }

                if (si instanceof MCH_SeatRackInfo) {
                    seat.updateRotation(seat.getRiddenByEntity(), si.fixYaw + this.getYaw(), si.fixPitch);
                }

                seat.updatePosition(seat.getRiddenByEntity());
            }
        }
    }

    public int getClientPositionDelayCorrection() {
        return 7;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9, boolean teleport) {
        this.aircraftPosRotInc = par9 + this.getClientPositionDelayCorrection();
        this.aircraftX = par1;
        this.aircraftY = par3;
        this.aircraftZ = par5;
        this.aircraftYaw = par7;
        this.aircraftPitch = par8;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }

    public void updateRiderPosition(Entity passenger, double px, double py, double pz) {
        MCH_SeatInfo[] info = this.getSeatsInfo();
        if (this.isPassenger(passenger) && !passenger.isDead) {
            float riddenEntityYOffset = 0.0F;

            Vec3d v;
            if (info != null && info.length > 0) {
                v = this.getTransformedPosition(info[0].pos.x, info[0].pos.y + riddenEntityYOffset - 0.5, info[0].pos.z, px, py + W_Entity.GLOBAL_Y_OFFSET, pz, info[0].rotSeat);
            } else {
                v = this.getTransformedPosition(0.0, riddenEntityYOffset - 1.0F, 0.0);
            }

            passenger.setPosition(v.x, v.y, v.z);
        }
    }

    public void updatePassenger(@NotNull Entity passenger) {
        this.updateRiderPosition(passenger, this.posX, this.posY, this.posZ);
    }

    public Vec3d calcOnTurretPos(Vec3d pos) {
        float ry = this.getLastRiderYaw();
        if (this.getRiddenByEntity() != null) {
            ry = this.getRiddenByEntity().rotationYaw;
        }

        assert this.getAcInfo() != null;
        Vec3d tpos = this.getAcInfo().turretPosition.add(0.0, pos.y, 0.0);
        Vec3d v = pos.add(-tpos.x, -tpos.y, -tpos.z);
        v = MCH_Lib.RotVec3(v, -ry, 0.0F, 0.0F);
        Vec3d vv = MCH_Lib.RotVec3(tpos, -this.getYaw(), -this.getPitch(), -this.getRoll());
        return v.add(vv);
    }

    @SideOnly(Side.CLIENT)
    public void setupAllRiderRenderPosition(float tick, EntityPlayer player) {
        double x = this.lastTickPosX + (this.posX - this.lastTickPosX) * tick;
        double y = this.lastTickPosY + (this.posY - this.lastTickPosY) * tick;
        double z = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * tick;
        this.updateRiderPosition(this.getRiddenByEntity(), x, y, z);
        this.updateSeatsPosition(x, y, z, true);

        for (int i = 0; i < this.getSeatNum() + 1; i++) {
            Entity e = this.getEntityBySeatId(i);
            if (e != null) {
                e.lastTickPosX = e.posX;
                e.lastTickPosY = e.posY;
                e.lastTickPosZ = e.posZ;
            }
        }

        if (this.getTVMissile() != null && W_Lib.isClientPlayer(this.getTVMissile().shootingEntity)) {
            Entity tv = this.getTVMissile();
            assert tv != null;
            x = tv.prevPosX + (tv.posX - tv.prevPosX) * tick;
            y = tv.prevPosY + (tv.posY - tv.prevPosY) * tick;
            z = tv.prevPosZ + (tv.posZ - tv.prevPosZ) * tick;
            MCH_ViewEntityDummy.setCameraPosition(x, y, z);
        } else {
            MCH_AircraftInfo.CameraPosition cpi = this.getCameraPosInfo();
            if (cpi != null && cpi.pos() != null) {
                MCH_SeatInfo seatInfo = this.getSeatInfo(player);
                Vec3d v;
                if (seatInfo != null && seatInfo.rotSeat) {
                    v = this.calcOnTurretPos(cpi.pos());
                } else {
                    v = MCH_Lib.RotVec3(cpi.pos(), -this.getYaw(), -this.getPitch(), -this.getRoll());
                }

                MCH_ViewEntityDummy.setCameraPosition(x + v.x, y + v.y, z + v.z);

            }
        }
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
            MCH_AircraftInfo info = this.getAcInfo();
            Vec3d tv = MCH_Lib.RotVec3(x - info.turretPosition.x, y - info.turretPosition.y, z - info.turretPosition.z, -this.getLastRiderYaw() + this.getYaw(), 0.0F, 0.0F);
            x = tv.x + info.turretPosition.x;
            y = tv.y + info.turretPosition.x;
            z = tv.z + info.turretPosition.x;
        }

        Vec3d v = MCH_Lib.RotVec3(x, y, z, -this.getYaw(), -this.getPitch(), -this.getRoll());
        return v.add(px, py, pz);
    }

    protected @NotNull MCH_SeatInfo[] getSeatsInfo() {
        if (this.seatsInfo == null) {
            this.newSeatsPos();
        }
        return this.seatsInfo;
    }

    protected void setSeatsInfo(MCH_SeatInfo[] v) {
        this.seatsInfo = v;
    }

    @Nullable
    public MCH_SeatInfo getSeatInfo(int index) {
        MCH_SeatInfo[] seats = this.getSeatsInfo();
        return index >= 0 && seats != null && index < seats.length ? seats[index] : null;
    }

    @Nullable
    public MCH_SeatInfo getSeatInfo(@Nullable Entity entity) {
        return this.getSeatInfo(this.getSeatIdByEntity(entity));
    }

    public int getSeatNum() {
        if (this.getAcInfo() == null) {
            return 0;
        } else {
            int s = this.getAcInfo().getNumSeatAndRack();
            return s >= 1 ? s - 1 : 1;
        }
    }

    protected void newSeatsPos() {
        if (this.getAcInfo() != null) {
            MCH_SeatInfo[] v = new MCH_SeatInfo[this.getAcInfo().getNumSeatAndRack()];

            for (int i = 0; i < v.length; i++) {
                v[i] = this.getAcInfo().seatList.get(i);
            }

            this.setSeatsInfo(v);
        }
    }

    public void createSeats(String uuid) {
        if (!this.world.isRemote) {
            if (!uuid.isEmpty()) {
                this.setCommonUniqueId(uuid);
                this.seats = new MCH_EntitySeat[this.getSeatNum()];

                for (int i = 0; i < this.seats.length; i++) {
                    this.seats[i] = new MCH_EntitySeat(this.world, this.posX, this.posY, this.posZ);
                    this.seats[i].parentUniqueID = this.getCommonUniqueId();
                    this.seats[i].seatID = i;
                    this.seats[i].setParent(this);
                    this.world.spawnEntity(this.seats[i]);
                }
            }
        }
    }

    public boolean interactFirstSeat(EntityPlayer player) {
        if (this.getSeats() == null) {
            return false;
        } else {
            int seatId = 1;

            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() == null && !this.isMountedEntity(player) && this.canRideSeatOrRack(seatId, player)) {
                    if (!this.world.isRemote) {
                        player.startRiding(seat);
                    }
                    break;
                }

                seatId++;
            }

            return true;
        }
    }

    public void onMountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
        if (seat != null) {
            if (this.world.isRemote && MCH_Lib.getClientPlayer() == entity) {
                this.switchGunnerFreeLookMode(false);
            }

            this.initCurrentWeapon(entity);
            Object[] data1 = new Object[]{W_Entity.getEntityId(entity)};
            MCH_Logger.debugLog(this.world, "onMountEntitySeat:%d", data1);
            Entity pilot = this.getRiddenByEntity();
            int sid = this.getSeatIdByEntity(entity);
            if (sid == 1 && (this.getAcInfo() == null || !this.getAcInfo().isEnableConcurrentGunnerMode)) {
                this.switchGunnerMode(false);
            }

            if (sid > 0) {
                this.isGunnerModeOtherSeat = true;
            }

            if (pilot != null && this.getAcInfo() != null) {
                int cwid = this.getCurrentWeaponID(pilot);
                MCH_AircraftInfo.Weapon w = this.getAcInfo().getWeaponById(cwid);
                if (w != null && this.getWeaponSeatID(this.getWeaponInfoById(cwid), w) == sid) {
                    int next = this.getNextWeaponID(pilot, 1);
                    Object[] data = new Object[]{W_Entity.getEntityId(pilot), next};
                    MCH_Logger.debugLog(this.world, "onMountEntitySeat:%d:->%d", data);
                    if (next >= 0) {
                        this.switchWeapon(pilot, next);
                    }
                }
            }

            if (this.world.isRemote) {
                this.updateClientSettings(sid);
            }
        }
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
        if (this.getRidingEntity() == null) {
            int d = 2;
            if (this.dismountedUserCtrl) {
                d = 6;
            }

            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(d, d, d));
            if (!list.isEmpty()) {
                for (Entity entity : list) {
                    if (entity instanceof EntityMinecartEmpty) {
                        if (this.dismountedUserCtrl) {
                            return;
                        }

                        if (!entity.isBeingRidden() && entity.canBePushed()) {
                            this.waitMountEntity = 20;
                            MCH_Logger.debugLog(this.world.isRemote, "MCH_EntityAircraft.mountWithNearEmptyMinecart:" + entity);
                            this.startRiding(entity);
                            return;
                        }
                    }
                }
            }

            this.dismountedUserCtrl = false;
        }
    }

    public boolean isRidePlayer() {
        if (this.getRiddenByEntity() instanceof EntityPlayer) {
            return true;
        } else {
            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() instanceof EntityPlayer) {
                    return true;
                }
            }

            return false;
        }
    }

    public void onUnmountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
        Object[] data = new Object[]{W_Entity.getEntityId(entity)};
        MCH_Logger.debugLog(this.world, "onUnmountPlayerSeat:%d", data);
        int sid = this.getSeatIdByEntity(entity);
        this.camera.initCamera(sid, entity);
        MCH_SeatInfo seatInfo = this.getSeatInfo(seat.seatID + 1);
        if (seatInfo != null) {
            this.setUnmountPosition(entity, seatInfo);
        }

        if (!this.isRidePlayer()) {
            this.switchGunnerMode(false);
            this.switchHoveringMode(false);
        }
    }

    public boolean isCreatedSeats() {
        return !this.getCommonUniqueId().isEmpty();
    }

    public void onUpdate_Seats() {
        boolean b = false;

        for (MCH_EntitySeat seat : this.seats) {
            if (seat != null) {
                if (!seat.isDead) {
                    seat.fallDistance = 0.0F;
                }
            } else {
                b = true;
            }
        }

        if (b) {
            if (this.seatSearchCount > 40) {
                if (this.world.isRemote) {
                    PacketRequestSeatList.requestSeatList(this);
                } else {
                    this.searchSeat();
                }

                this.seatSearchCount = 0;
            }

            this.seatSearchCount++;
        }
    }

    public void searchSeat() {
        List<MCH_EntitySeat> list = this.world.getEntitiesWithinAABB(MCH_EntitySeat.class, this.getEntityBoundingBox().grow(60.0, 60.0, 60.0));

        for (MCH_EntitySeat seat : list) {
            if (!seat.isDead && seat.parentUniqueID.equals(this.getCommonUniqueId()) && seat.seatID >= 0 && seat.seatID < this.getSeatNum() && this.seats[seat.seatID] == null) {
                this.seats[seat.seatID] = seat;
                seat.setParent(this);
            }
        }
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

        for (MCH_EntitySeat s : this.seats) {
            if (s != null) {
                s.setDead();
            }
        }

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
        if(isUAV()) UAVTracker.delUAVPos(world, this);

        String format = "setDead:" + (this.getAcInfo() != null ? this.getAcInfo().name : "null");
        MCH_Logger.debugLog(this.world, format);
    }

    public void unmountEntity() {
        if (!this.isRidePlayer()) {
            this.switchHoveringMode(false);
        }

        this.moveLeft = this.moveRight = this.throttleDown = this.throttleUp = false;
        Entity rByEntity = null;
        if (this.getRiddenByEntity() != null) {
            rByEntity = this.getRiddenByEntity();
            this.camera.initCamera(0, rByEntity);
            if (!this.world.isRemote) {
                this.getRiddenByEntity().dismountRidingEntity();
            }
        } else if (this.lastRiddenByEntity != null) {
            rByEntity = this.lastRiddenByEntity;
            if (rByEntity instanceof EntityPlayer) {
                this.camera.initCamera(0, rByEntity);
            }
        }

        MCH_Logger.debugLog(this.world, "unmountEntity:" + rByEntity);
        if (!this.isRidePlayer()) {
            this.switchGunnerMode(false);
        }

        this.setCommonStatus(1, false);
        if (!this.isUAV()) {
            this.setUnmountPosition(rByEntity, this.getSeatInfo(0));
        } else if (rByEntity != null && rByEntity.getRidingEntity() instanceof MCH_EntityUavStation) {
            rByEntity.dismountRidingEntity();
        }

        this.lastRiddenByEntity = null;
        if (this.cs_dismountAll) {
            this.unmountCrew(false);
        }
    }

    public Entity getRidingEntity() {
        return super.getRidingEntity();
    }

    public void startUnmountCrew() {
        this.isParachuting = true;
        if (this.haveHatch()) {
            this.foldHatch(true, true);
        }
    }

    public void stopUnmountCrew() {
        this.isParachuting = false;
    }

    public void unmountCrew() {
        if (this.getAcInfo() != null) {
            if (this.getAcInfo().haveRepellingHook()) {
                if (!this.isRepelling()) {
                    if (MCH_Lib.getBlockIdY(this, 3, -4) > 0) {
                        this.unmountCrew(false);
                    } else if (this.canStartRepelling()) {
                        this.startRepelling();
                    }
                } else {
                    this.stopRepelling();
                }
            } else if (this.isParachuting) {
                this.stopUnmountCrew();
            } else if (this.getAcInfo().isEnableParachuting && MCH_Lib.getBlockIdY(this, 3, -10) == 0) {
                this.startUnmountCrew();
            } else {
                this.unmountCrew(false);
            }
        }
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

    private void startRepelling() {
        MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.startRepelling()");
        this.setRepellingStat(true);
        this.throttleUp = false;
        this.throttleDown = false;
        this.moveLeft = false;
        this.moveRight = false;
        this.tickRepelling = 0;
    }

    private void stopRepelling() {
        MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.stopRepelling()");
        this.setRepellingStat(false);
    }

    public boolean canStartRepelling() {
        assert this.getAcInfo() != null;
        if (this.getAcInfo().haveRepellingHook() && this.isHovering() && abs(this.getPitch()) < 3.0F && abs(this.getRoll()) < 3.0F) {
            Vec3d v = this.prevPosition.oldest().add(-this.posX, -this.posY, -this.posZ);
            return v.length() < 0.3;
        }

        return false;
    }

    public boolean unmountCrew(boolean unmountParachute) {
        boolean anyUnmounted = false;
        MCH_SeatInfo[] seatInfos = getSeatsInfo();

        for (int i = 0; i < seats.length; i++) {
            var seat = seats[i];
            if (seat == null) continue;

            var occupant = seat.getRiddenByEntity();
            if (occupant == null) continue;
            if (occupant instanceof EntityPlayer) continue;
            if (seatInfos[i + 1] instanceof MCH_SeatRackInfo) continue;

            MCH_SeatInfo seatInfo = seatInfos[i + 1];
            anyUnmounted = true;

            if (unmountParachute && getSeatIdByEntity(occupant) > 1) {
                Vec3d localUnmountPos = (seatInfo != null && seatInfo.unmountPos != null) ? seatInfo.unmountPos : getAcInfo().mobDropOption.pos; // acInfo assumed non-null

                Vec3d worldDropPos = getTransformedPosition(localUnmountPos, prevPosition.oldest());

                seat.posX = worldDropPos.x;
                seat.posY = worldDropPos.y;
                seat.posZ = worldDropPos.z;

                occupant.dismountRidingEntity();
                occupant.posX = worldDropPos.x;
                occupant.posY = worldDropPos.y;
                occupant.posZ = worldDropPos.z;

                dropEntityParachute(occupant);
                break; // stop after first parachute drop
            } else {
                setUnmountPosition(seat, seatInfo);
                occupant.dismountRidingEntity();
                setUnmountPosition(occupant, seatInfo);
            }
        }

        return anyUnmounted;
    }


    // returns local position in seat(rack) configs
    protected Vec3d getUnmountPos(MCH_SeatInfo seatInfo) {
        if (this.getAcInfo() == null || seatInfo == null) return Vec3d.ZERO;

        if (seatInfo.unmountPos != null) {
            return seatInfo.unmountPos;
        }

        if (this.getAcInfo().unmountPosition != null) {
            return this.getAcInfo().unmountPosition;
        }

        double x = seatInfo.pos.x;
        x = x >= 0.0 ? x + 3.0 : x - 3.0;
        return new Vec3d(x, 2.0, seatInfo.pos.z);
    }

    public void setUnmountPosition(@Nullable Entity rByEntity, @Nullable MCH_SeatInfo seatInfo) {
        if (rByEntity != null && seatInfo != null) {
            Vec3d localPos = this.getUnmountPos(seatInfo);
            Vec3d desired = this.getTransformedPosition(localPos);
            RayTraceResult trace = world.rayTraceBlocks(rByEntity.getPositionVector(), desired, false, true, true);
            Vec3d dir = desired.subtract(rByEntity.getPositionVector()).normalize();
            var finPos = trace == null ? desired : trace.hitVec.subtract(dir);


            rByEntity.setPosition(finPos.x, finPos.y, finPos.z);
            this.listUnmountReserve.add(new UnmountReserve(rByEntity, finPos.x, finPos.y, finPos.z));
        }
    }

    public void unmountEntityFromSeat(@Nullable Entity entity) {
        if (entity != null && this.seats != null) {
            for (MCH_EntitySeat seat : this.seats) {
                if (seat != null && seat.getRiddenByEntity() != null && W_Entity.isEqual(seat.getRiddenByEntity(), entity)) {
                    entity.dismountRidingEntity();
                }
            }

        }
    }

    public void ejectSeat(@Nullable Entity entity) {
        int sid = this.getSeatIdByEntity(entity);
        if (sid >= 0 && sid <= 1) {
            if (this.getGuiInventory().haveParachute()) {
                if (sid == 0) {
                    this.getGuiInventory().consumeParachute();
                    this.unmountEntity();
                    this.ejectSeatSub(entity, 0);
                    entity = this.getEntityBySeatId(1);
                    if (entity instanceof EntityPlayer) {
                        entity = null;
                    }
                }

                if (this.getGuiInventory().haveParachute() && entity != null) {
                    this.getGuiInventory().consumeParachute();
                    this.unmountEntityFromSeat(entity);
                    this.ejectSeatSub(entity, 1);
                }
            }
        }
    }

    public void ejectSeatSub(Entity entity, int sid) {
        Vec3d pos = this.getSeatInfo(sid) != null ? Objects.requireNonNull(this.getSeatInfo(sid)).pos : null;
        if (pos != null) {
            Vec3d v = this.getTransformedPosition(pos.x, pos.y + 2.0, pos.z);
            entity.setPosition(v.x, v.y, v.z);
        }

        Vec3d v = MCH_Lib.RotVec3(0.0, 2.0, 0.0, -this.getYaw(), -this.getPitch(), -this.getRoll());
        entity.motionX = this.motionX + v.x + (this.rand.nextFloat() - 0.5) * 0.1;
        entity.motionY = this.motionY + v.y;
        entity.motionZ = this.motionZ + v.z + (this.rand.nextFloat() - 0.5) * 0.1;
        MCH_EntityParachute parachute = new MCH_EntityParachute(this.world, entity.posX, entity.posY, entity.posZ);
        parachute.rotationYaw = entity.rotationYaw;
        parachute.motionX = entity.motionX;
        parachute.motionY = entity.motionY;
        parachute.motionZ = entity.motionZ;
        parachute.fallDistance = entity.fallDistance;
        parachute.user = entity;
        parachute.setType(2);
        this.world.spawnEntity(parachute);
        assert this.getAcInfo() != null;
        if (this.getAcInfo().haveCanopy() && this.isCanopyClose()) {
            this.openCanopy_EjectSeat();
        }

        W_WorldFunc.playSoundAt(entity, "eject_seat", 5.0F, 1.0F);
    }

    public boolean canEjectSeat(@Nullable Entity entity) {
        int sid = this.getSeatIdByEntity(entity);
        return (sid != 0 || !this.isUAV()) && sid >= 0 && sid < 2 && this.getAcInfo() != null && this.getAcInfo().isEnableEjectionSeat;
    }

    public int getNumEjectionSeat() {
        return 0;
    }

    public void mountMobToSeats() {
        List<EntityLivingBase> list = this.world.getEntitiesWithinAABB(W_Lib.getEntityLivingBaseClass(), this.getEntityBoundingBox().grow(3.0, 2.0, 3.0));

        for (Entity entity : list) {
            if (!(entity instanceof EntityPlayer) && entity.getRidingEntity() == null) {
                int sid = 1;

                for (MCH_EntitySeat seat : this.getSeats()) {
                    if (seat != null && seat.getRiddenByEntity() == null && !this.isMountedEntity(entity) && this.canRideSeatOrRack(sid, entity)) {
                        if (this.getSeatInfo(sid) instanceof MCH_SeatRackInfo) {
                            break;
                        }

                        entity.startRiding(seat);
                    }

                    sid++;
                }
            }
        }
    }

    public void mountEntityToRack() {
        if (!MCH_Config.EnablePutRackInFlying.prmBool) {
            if (this.getCurrentThrottle() > 0.3) {
                return;
            }

            Block block = MCH_Lib.getBlockY(this, 1, -3, true);
            if (block == null || W_Block.isEqual(block, Blocks.AIR)) {
                return;
            }
        }

        int countRideEntity = 0;

        for (int sid = 0; sid < this.getSeatNum(); sid++) {
            MCH_EntitySeat seat = this.getSeat(sid);
            if (this.getSeatInfo(1 + sid) instanceof MCH_SeatRackInfo info && seat != null && seat.getRiddenByEntity() == null) {
                Vec3d v = MCH_Lib.RotVec3(info.getEntryPos().x, info.getEntryPos().y, info.getEntryPos().z, -this.getYaw(), -this.getPitch(), -this.getRoll());
                v = v.add(this.posX, this.posY, this.posZ);
                AxisAlignedBB bb = new AxisAlignedBB(v.x, v.y, v.z, v.x, v.y, v.z);
                float range = info.range;
                List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, bb.grow(range, range, range));

                for (Entity entity : list) {
                    if (this.canRideSeatOrRack(1 + sid, entity)) {
                        if (entity instanceof MCH_IEntityCanRideAircraft) {
                            if (((MCH_IEntityCanRideAircraft) entity).canRideAircraft(this, sid, info)) {
                                MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.mountEntityToRack:%d:%s", sid, entity);
                                entity.startRiding(seat);
                                countRideEntity++;
                                break;
                            }
                        } else if (entity.getRidingEntity() == null) {
                            NBTTagCompound nbt = entity.getEntityData();
                            if (nbt.hasKey("CanMountEntity") && nbt.getBoolean("CanMountEntity")) {
                                MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.mountEntityToRack:%d:%s:%s", sid, entity, entity.getClass());
                                entity.startRiding(seat);
                                countRideEntity++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (countRideEntity > 0) {
            MCH_SoundEvents.playSound(this.world, this.posX, this.posY, this.posZ, "random.click", 1.0F, 1.0F);
        }
    }

    public void unmountEntityFromRack() {
        for (int sid = this.getSeatNum() - 1; sid >= 0; sid--) {
            MCH_EntitySeat seat = this.getSeat(sid);
            if (this.getSeatInfo(sid + 1) instanceof MCH_SeatRackInfo info && seat != null && seat.getRiddenByEntity() != null) {
                Entity entity = seat.getRiddenByEntity();
                Vec3d pos = info.unmountPos;
                if (pos == null) {
                    pos = info.getEntryPos();
                    if (entity instanceof MCH_EntityAircraft) {
                        assert this.getAcInfo() != null;
                        if (pos.z >= this.getAcInfo().bbZ) {
                            pos = pos.add(0.0, 0.0, 12.0);
                        } else {
                            pos = pos.add(0.0, 0.0, -12.0);
                        }
                    }
                }

                Vec3d v = MCH_Lib.RotVec3(pos.x, pos.y, pos.z, -this.getYaw(), -this.getPitch(), -this.getRoll());
                seat.posX = entity.posX = this.posX + v.x;
                seat.posY = entity.posY = this.posY + v.y;
                seat.posZ = entity.posZ = this.posZ + v.z;
                MCH_EntityAircraft.UnmountReserve ur = new UnmountReserve(entity, entity.posX, entity.posY, entity.posZ);
                ur.cnt = 8;
                this.listUnmountReserve.add(ur);
                entity.dismountRidingEntity();
                if (MCH_Lib.getBlockIdY(this, 3, -20) > 0) {
                    MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.unmountEntityFromRack:%d:%s", sid, entity);
                } else {
                    MCH_Logger.debugLog(this.world, "MCH_EntityAircraft.unmountEntityFromRack:%d Parachute:%s", sid, entity);
                    this.dropEntityParachute(entity);
                }
                break;
            }
        }
    }

    public void dropEntityParachute(Entity entity) {
        entity.motionX = this.motionX;
        entity.motionY = this.motionY;
        entity.motionZ = this.motionZ;
        MCH_EntityParachute parachute = new MCH_EntityParachute(this.world, entity.posX, entity.posY, entity.posZ);
        parachute.rotationYaw = entity.rotationYaw;
        parachute.motionX = entity.motionX;
        parachute.motionY = entity.motionY;
        parachute.motionZ = entity.motionZ;
        parachute.fallDistance = entity.fallDistance;
        parachute.user = entity;
        parachute.setType(3);
        this.world.spawnEntity(parachute);
    }

    public void rideRack() {
        if (this.getRidingEntity() == null) {
            AxisAlignedBB bb = this.getCollisionBoundingBox();
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, bb.grow(60.0, 60.0, 60.0));

            for (Entity entity : list) {
                if (entity instanceof MCH_EntityAircraft ac) {
                    if (ac.getAcInfo() != null) {
                        for (int sid = 0; sid < ac.getSeatNum(); sid++) {
                            MCH_SeatInfo seatInfo = ac.getSeatInfo(1 + sid);
                            if (seatInfo instanceof MCH_SeatRackInfo info && ac.canRideSeatOrRack(1 + sid, entity)) {
                                MCH_EntitySeat seat = ac.getSeat(sid);
                                if (seat != null && seat.getRiddenByEntity() == null) {
                                    Vec3d v = ac.getTransformedPosition(info.getEntryPos());
                                    float r = info.range;
                                    if (this.posX >= v.x - r && this.posX <= v.x + r && this.posY >= v.y - r && this.posY <= v.y + r && this.posZ >= v.z - r && this.posZ <= v.z + r && this.canRideAircraft(ac, sid, info)) {
                                        MCH_SoundEvents.playSound(this.world, this.posX, this.posY, this.posZ, "random.click", 1.0F, 1.0F);
                                        this.startRiding(seat);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canPutToRack() {
        for (int i = 0; i < this.getSeatNum(); i++) {
            MCH_EntitySeat seat = this.getSeat(i);
            MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
            if (seat != null && seat.getRiddenByEntity() == null && seatInfo instanceof MCH_SeatRackInfo) {
                return true;
            }
        }

        return false;
    }

    public boolean canDownFromRack() {
        for (int i = 0; i < this.getSeatNum(); i++) {
            MCH_EntitySeat seat = this.getSeat(i);
            MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
            if (seat != null && seat.getRiddenByEntity() != null && seatInfo instanceof MCH_SeatRackInfo) {
                return true;
            }
        }

        return false;
    }

    public void checkRideRack() {
        if (this.getCountOnUpdate() % 10 == 0) {
            this.canRideRackStatus = false;
            if (this.getRidingEntity() == null) {
                AxisAlignedBB bb = this.getCollisionBoundingBox();
                List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, bb.grow(60.0, 60.0, 60.0));

                for (Entity entity : list) {
                    if (entity instanceof MCH_EntityAircraft ac) {
                        if (ac.getAcInfo() != null) {
                            for (int sid = 0; sid < ac.getSeatNum(); sid++) {
                                MCH_SeatInfo seatInfo = ac.getSeatInfo(1 + sid);
                                if (seatInfo instanceof MCH_SeatRackInfo info) {
                                    MCH_EntitySeat seat = ac.getSeat(sid);
                                    if (seat != null && seat.getRiddenByEntity() == null) {
                                        Vec3d v = ac.getTransformedPosition(info.getEntryPos());
                                        float r = info.range;
                                        if (this.posX >= v.x - r && this.posX <= v.x + r && this.posY >= v.y - r && this.posY <= v.y + r && this.posZ >= v.z - r && this.posZ <= v.z + r && this.canRideAircraft(ac, sid, info)) {
                                            this.canRideRackStatus = true;
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canRideRack() {
        return this.getRidingEntity() == null && this.canRideRackStatus;
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
                return checkNestedRiders();
            }
        }

        // Check ride racks
        for (MCH_AircraftInfo.RideRack rr : selfInfo.rideRacks) {
            assert acInfo != null;
            int baseSeat = acInfo.getNumSeat() - 1;
            int id = baseSeat + rr.rackID() - 1;

            if (id != seatID) continue;
            if (!rr.name().equalsIgnoreCase(acInfo.name)) continue;

            MCH_EntitySeat seat = ac.getSeat(id);
            if (seat != null && seat.getRiddenByEntity() == null) {
                return checkNestedRiders();
            }
        }

        // Nothing matched
        return false;
    }

    private boolean checkNestedRiders() {
        for (MCH_EntitySeat seat : getSeats()) {
            if (seat != null && seat.getRiddenByEntity() instanceof MCH_IEntityCanRideAircraft) {
                return false;
            }
        }
        return true;
    }


    public boolean isMountedEntity(@Nullable Entity entity) {
        return entity != null && this.isMountedEntity(W_Entity.getEntityId(entity));
    }

    @Nullable
    public EntityPlayer getFirstMountPlayer() {
        if (this.getRiddenByEntity() instanceof EntityPlayer) {
            return (EntityPlayer) this.getRiddenByEntity();
        } else {
            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() instanceof EntityPlayer) {
                    return (EntityPlayer) seat.getRiddenByEntity();
                }
            }

            return null;
        }
    }

    public boolean isMountedSameTeamEntity(@Nullable EntityLivingBase player) {
        if (player != null && player.getTeam() != null) {
            if (this.getRiddenByEntity() instanceof EntityLivingBase && player.isOnSameTeam(this.getRiddenByEntity())) {
                return true;
            } else {
                for (MCH_EntitySeat seat : this.getSeats()) {
                    if (seat != null && seat.getRiddenByEntity() instanceof EntityLivingBase && player.isOnSameTeam(seat.getRiddenByEntity())) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isMountedEntity(int entityId) {
        if (W_Entity.getEntityId(this.getRiddenByEntity()) == entityId) {
            return true;
        } else {
            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() != null && W_Entity.getEntityId(seat.getRiddenByEntity()) == entityId) {
                    return true;
                }
            }

            return false;
        }
    }

    public void onInteractFirst(EntityPlayer player) {
    }

    public boolean notOnSameTeam(EntityPlayer player) {
        for (int i = 0; i < 1 + this.getSeatNum(); i++) {
            Entity entity = this.getEntityBySeatId(i);
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
        this.switchSeat = ss;
        boolean ret = this.processInitialInteract(player, hand);
        this.switchSeat = false;
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
        if (!this.canRideSeatOrRack(0, player)) return false;

        // Canopy/mode restrictions
        if (!this.switchSeat) {
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
        this.lowPassPartialTicks.clear();

        if ("uh-1c".equalsIgnoreCase(this.getAcInfo().name) && player instanceof EntityPlayerMP) {
            MCH_CriteriaTriggers.RIDING_VALKYRIES.trigger((EntityPlayerMP) player);
        }

        this.onInteractFirst(player);
        return true;
    }

    public boolean canRideSeatOrRack(int seatId, Entity entity) {
        if (this.getAcInfo() == null) {
            return false;
        } else {
            for (Integer[] a : this.getAcInfo().exclusionSeatList) {
                if (Arrays.asList(a).contains(seatId)) {

                    for (int id : a) {
                        if (this.getEntityBySeatId(id) != null) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
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

    public void switchNextSeat(Entity entity) {
        if (entity != null) {
            if (this.seats != null && this.seats.length > 0) {
                if (this.isMountedEntity(entity)) {
                    boolean isFound = false;
                    int sid = 1;

                    for (MCH_EntitySeat seat : this.seats) {
                        if (seat != null) {
                            if (this.getSeatInfo(sid) instanceof MCH_SeatRackInfo) {
                                break;
                            }

                            if (W_Entity.isEqual(seat.getRiddenByEntity(), entity)) {
                                isFound = true;
                            } else if (isFound && seat.getRiddenByEntity() == null) {
                                entity.startRiding(seat);
                                return;
                            }

                            sid++;
                        }
                    }

                    sid = 1;

                    for (MCH_EntitySeat seatx : this.seats) {
                        if (seatx != null && seatx.getRiddenByEntity() == null) {
                            if (!(this.getSeatInfo(sid) instanceof MCH_SeatRackInfo)) {
                                this.onMountPlayerSeat(seatx, entity);
                                return;
                            }
                            break;
                        }

                        sid++;
                    }
                }
            }
        }
    }

    public void switchPrevSeat(Entity entity) {
        if (entity != null) {
            if (this.seats != null && this.seats.length > 0) {
                if (this.isMountedEntity(entity)) {
                    boolean isFound = false;

                    for (int i = this.seats.length - 1; i >= 0; i--) {
                        MCH_EntitySeat seat = this.seats[i];
                        if (seat != null) {
                            if (W_Entity.isEqual(seat.getRiddenByEntity(), entity)) {
                                isFound = true;
                            } else if (isFound && seat.getRiddenByEntity() == null) {
                                entity.startRiding(seat);
                                return;
                            }
                        }
                    }

                    for (int ix = this.seats.length - 1; ix >= 0; ix--) {
                        MCH_EntitySeat seat = this.seats[ix];
                        if (!(this.getSeatInfo(ix + 1) instanceof MCH_SeatRackInfo) && seat != null && seat.getRiddenByEntity() == null) {
                            entity.startRiding(seat);
                            return;
                        }
                    }
                }
            }
        }
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
        return this.getIsGunnerMode(entity) || this.isUAV();
    }

    public void updateCamera(double x, double y, double z) {
        if (this.world.isRemote) {
            if (this.getTVMissile() != null) {
                this.camera.setPosition(this.TVmissile.posX, this.TVmissile.posY, this.TVmissile.posZ);
                this.camera.setCameraZoom(1.0F);
                this.TVmissile.isSpawnParticle = !this.isMissileCameraMode(this.TVmissile.shootingEntity);
            } else {
                this.setTVMissile(null);
                MCH_AircraftInfo.CameraPosition cpi = this.getCameraPosInfo();
                Vec3d cp = cpi != null ? cpi.pos() : Vec3d.ZERO;
                Vec3d v = MCH_Lib.RotVec3(cp, -this.getYaw(), -this.getPitch(), -this.getRoll());
                this.camera.setPosition(x + v.x, y + v.y, z + v.z);
            }
        }
    }

    public void updateCameraRotate(float yaw, float pitch) {
        this.camera.prevRotationYaw = this.camera.rotationYaw;
        this.camera.prevRotationPitch = this.camera.rotationPitch;
        this.camera.rotationYaw = yaw;
        this.camera.rotationPitch = pitch;
    }

    public void updatePartCameraRotate() {
        if (this.world.isRemote) {
            Entity e = this.getEntityBySeatId(1);
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
        return this.TVmissile != null && !this.TVmissile.isDead ? this.TVmissile : null;
    }

    public void setTVMissile(MCH_EntityTvMissile entity) {
        this.TVmissile = entity;
    }

    public MCH_WeaponSet[] createWeapon(int seat_num) {
        this.currentWeaponID = new int[seat_num];

        Arrays.fill(this.currentWeaponID, -1);

        if (this.getAcInfo() != null && !this.getAcInfo().weaponSetList.isEmpty() && seat_num > 0) {
            MCH_WeaponSet[] weaponSetArray = new MCH_WeaponSet[this.getAcInfo().weaponSetList.size()];

            for (int i = 0; i < this.getAcInfo().weaponSetList.size(); i++) {
                MCH_AircraftInfo.WeaponSet ws = this.getAcInfo().weaponSetList.get(i);
                MCH_WeaponBase[] wb = new MCH_WeaponBase[ws.weapons.size()];

                for (int j = 0; j < ws.weapons.size(); j++) {
                    wb[j] = MCH_WeaponCreator.createWeapon(this.world, ws.type, ws.weapons.get(j).pos, ws.weapons.get(j).yaw, ws.weapons.get(j).pitch, this, ws.weapons.get(j).turret);
                    assert wb[j] != null;
                    wb[j].aircraft = this;
                }

                if (wb.length > 0 && wb[0] != null) {
                    float defYaw = ws.weapons.getFirst().defaultYaw;
                    weaponSetArray[i] = new MCH_WeaponSet(wb);
                    weaponSetArray[i].setPrevYaw(defYaw);
                    weaponSetArray[i].setYaw(defYaw);
                    weaponSetArray[i].setDefYaw(defYaw);
                }
            }

            return weaponSetArray;
        } else {
            return new MCH_WeaponSet[]{this.dummyWeapon};
        }
    }

    public void switchWeapon(Entity entity, int id) {
        int sid = this.getSeatIdByEntity(entity);
        if (this.isValidSeatID(sid)) {
            if (this.getWeaponNum() > 0 && this.currentWeaponID.length > 0) {
                if (id < 0) {
                    this.currentWeaponID[sid] = -1;
                }

                if (id >= this.getWeaponNum()) {
                    id = this.getWeaponNum() - 1;
                }

                String format = "switchWeapon:" + W_Entity.getEntityId(entity) + " -> " + id;
                MCH_Logger.debugLog(this.world, format);
                this.getCurrentWeapon(entity).reload();
                this.currentWeaponID[sid] = id;
                MCH_WeaponSet ws = this.getCurrentWeapon(entity);
                ws.onSwitchWeapon(this.world.isRemote, this.isInfinityAmmo(entity));
                if (!this.world.isRemote) {
                    new PacketSyncWeapon(getEntityId(this), sid, id, (short) ws.getAmmo(), (short) ws.getRestAllAmmoNum()).sendPacketToAllAround(world, posX, posY, posZ, 150);
                }
            }
        }
    }

    public void updateWeaponID(int sid, int id) {
        if (sid >= 0 && sid < this.currentWeaponID.length) {
            if (this.getWeaponNum() > 0 && this.currentWeaponID.length > 0) {
                if (id < 0) {
                    this.currentWeaponID[sid] = -1;
                }

                if (id >= this.getWeaponNum()) {
                    id = this.getWeaponNum() - 1;
                }

                MCH_Logger.debugLog(this.world, "switchWeapon:seatID=" + sid + ", WeaponID=" + id);
                this.currentWeaponID[sid] = id;
            }
        }
    }

    @Nullable
    public MCH_WeaponSet getWeaponByName(String name) {
        for (MCH_WeaponSet ws : this.weapons) {
            if (ws.isName(name)) {
                return ws;
            }
        }

        return null;
    }

    public void reloadAllWeapon() {
        for (int i = 0; i < this.getWeaponNum(); i++) {
            this.getWeapon(i).reloadMag();
        }
    }

    public MCH_WeaponSet getFirstSeatWeapon() {
        return this.currentWeaponID != null && this.currentWeaponID.length > 0 && this.currentWeaponID[0] >= 0 ? this.getWeapon(this.currentWeaponID[0]) : this.getWeapon(0);
    }

    public void initCurrentWeapon(Entity entity) {
        int sid = this.getSeatIdByEntity(entity);
        String format = "initCurrentWeapon:" + W_Entity.getEntityId(entity) + ":%d";
        MCH_Logger.debugLog(this.world, format, sid);
        if (sid >= 0 && sid < this.currentWeaponID.length) {
            this.currentWeaponID[sid] = -1;
            if (entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) {
                this.currentWeaponID[sid] = this.getNextWeaponID(entity, 1);
                this.switchWeapon(entity, this.getCurrentWeaponID(entity));
                if (this.world.isRemote) {
                    PacketIndNotifyAmmoNum.send(this, -1);
                }
            }
        }
    }

    public void initPilotWeapon() {
        this.currentWeaponID[0] = -1;
    }

    public MCH_WeaponSet getCurrentWeapon(Entity entity) {
        return this.getWeapon(this.getCurrentWeaponID(entity));
    }

    public MCH_WeaponSet getWeapon(int id) {
        return id >= 0 && this.weapons.length > 0 && id < this.weapons.length ? this.weapons[id] : this.dummyWeapon;
    }

    public int getWeaponIDBySeatID(int sid) {
        return sid >= 0 && sid < this.currentWeaponID.length ? this.currentWeaponID[sid] : -1;
    }

    public double getLandInDistance(Entity user) {
        if (this.lastCalcLandInDistanceCount != this.getCountOnUpdate() && this.getCountOnUpdate() % 5 == 0) {
            this.lastCalcLandInDistanceCount = this.getCountOnUpdate();
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

                    this.lastLandInDistance = currentWs.getImpactDistance(prm);
                }
            }
        }

        return this.lastLandInDistance;
    }

    public boolean useCurrentWeapon(Entity user) {
        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.setPosition(this.posX, this.posY, this.posZ);
        prm.entity = this;
        prm.user = user;
        return this.useCurrentWeapon(prm);
    }

    public boolean useCurrentWeapon(MCH_WeaponParam prm) {
        prm.isInfinity = this.isInfinityAmmo(prm.user);
        if (prm.user != null) {
            MCH_WeaponSet currentWs = this.getCurrentWeapon(prm.user);
            if (currentWs != null && currentWs.canFire()) {
                int sid = this.getSeatIdByEntity(prm.user);
                assert this.getAcInfo() != null;
                if (this.getAcInfo().getWeaponSetById(sid) != null) {
                    prm.isTurret = this.getAcInfo().getWeaponSetById(sid).weapons.getFirst().turret;
                }

                int lastUsedIndex = currentWs.getCurrentWeaponIndex();
                if (currentWs.use(prm)) {
                    for (MCH_WeaponSet ws : this.weapons) {
                        if (ws != currentWs && !ws.getInfo().group.isEmpty() && ws.getInfo().group.equals(currentWs.getInfo().group)) {
                            ws.waitAndReloadByOther(prm.reload);
                        }
                    }

                    if (!this.world.isRemote) {
                        int shift = 0;

                        for (MCH_WeaponSet wsx : this.weapons) {
                            if (wsx == currentWs) {
                                break;
                            }

                            shift += wsx.getWeaponsCount();
                        }

                        shift += lastUsedIndex;
                        this.useWeaponStat |= shift < 32 ? 1 << shift : 0;
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public void switchCurrentWeaponMode(Entity entity) {
        this.getCurrentWeapon(entity).switchMode();
    }

    public int getWeaponNum() {
        return this.weapons.length;
    }

    public int getCurrentWeaponID(Entity entity) {
        if (!(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
            return -1;
        } else {
            int id = this.getSeatIdByEntity(entity);
            return id >= 0 && id < this.currentWeaponID.length ? this.currentWeaponID[id] : -1;
        }
    }

    public int getNextWeaponID(Entity entity, int step) {
        if (this.getAcInfo() == null) {
            return -1;
        } else {
            int sid = this.getSeatIdByEntity(entity);
            if (sid < 0) {
                return -1;
            } else {
                int id = this.getCurrentWeaponID(entity);

                int i;
                for (i = 0; i < this.getWeaponNum(); i++) {
                    if (step >= 0) {
                        id = (id + 1) % this.getWeaponNum();
                    } else {
                        id = id > 0 ? id - 1 : this.getWeaponNum() - 1;
                    }

                    MCH_AircraftInfo.Weapon w = this.getAcInfo().getWeaponById(id);
                    if (w != null) {
                        MCH_WeaponInfo wi = this.getWeaponInfoById(id);
                        int wpsid = this.getWeaponSeatID(wi, w);
                        if (wpsid < this.getSeatNum() + 1 + 1 && (wpsid == sid || sid == 0 && w.canUsePilot && !(this.getEntityBySeatId(wpsid) instanceof EntityPlayer) && !(this.getEntityBySeatId(wpsid) instanceof MCH_EntityGunner))) {
                            break;
                        }
                    }
                }

                if (i >= this.getWeaponNum()) {
                    return -1;
                } else {
                    Object[] data = new Object[]{W_Entity.getEntityId(entity), id};
                    MCH_Logger.debugLog(this.world, "getNextWeaponID:%d:->%d", data);
                    return id;
                }
            }
        }
    }

    public int getWeaponSeatID(MCH_WeaponInfo wi, MCH_AircraftInfo.Weapon w) {
        return wi == null || (wi.target & 195) != 0 || !wi.type.isEmpty() || !MCH_MOD.proxy.isSinglePlayer() && !MCH_Config.TestMode.prmBool ? w.seatID : 1000;
    }

    public boolean isMissileCameraMode(Entity entity) {
        return this.getTVMissile() != null && this.isCameraView(entity);
    }

    public boolean isPilotReloading() {
        return this.getCommonStatus(2) || this.supplyAmmoWait > 0;
    }

    public int getUsedWeaponStat() {
        if (this.getAcInfo() == null) {
            return 0;
        } else if (this.getAcInfo().getWeaponCount() <= 0) {
            return 0;
        } else {
            int stat = 0;
            int i = 0;

            for (MCH_WeaponSet w : this.weapons) {
                if (i >= 32) {
                    break;
                }

                for (int wi = 0; wi < w.getWeaponsCount() && i < 32; wi++) {
                    stat |= w.isUsed(wi) ? 1 << i : 0;
                    i++;
                }
            }

            return stat;
        }
    }

    public boolean isWeaponNotCooldown(MCH_WeaponSet checkWs, int index) {
        if (this.getAcInfo() == null || this.getAcInfo().getWeaponCount() <= 0) {
            return false;
        } else if (this.getAcInfo().getWeaponCount() <= 0) {
            return false;
        } else {
            int shift = 0;

            for (MCH_WeaponSet ws : this.weapons) {
                if (ws == checkWs) {
                    break;
                }

                shift += ws.getWeaponsCount();
            }

            shift += index;
            return (this.useWeaponStat & 1 << shift) != 0;
        }
    }

    public void updateWeapons() {
        if (this.getAcInfo() != null) {
            if (this.getAcInfo().getWeaponCount() > 0) {
                int prevUseWeaponStat = this.useWeaponStat;
                if (!this.world.isRemote) {
                    this.useWeaponStat = this.useWeaponStat | this.getUsedWeaponStat();
                    this.dataManager.set(USE_WEAPON, this.useWeaponStat);
                    this.useWeaponStat = 0;
                } else {
                    this.useWeaponStat = this.dataManager.get(USE_WEAPON);
                }

                float yaw = MathHelper.wrapDegrees(this.getYaw());
                float pitch = MathHelper.wrapDegrees(this.getPitch());
                int id = 0;

                for (int wid = 0; wid < this.weapons.length; wid++) {
                    MCH_WeaponSet w = this.weapons[wid];
                    boolean isLongDelay = false;
                    if (w.getFirstWeapon() != null) {
                        isLongDelay = w.hasLongDelay();
                    }

                    boolean isSelected = false;

                    for (int swid : this.currentWeaponID) {
                        if (swid == wid) {
                            isSelected = true;
                            break;
                        }
                    }

                    boolean isWpnUsed = false;

                    for (int index = 0; index < w.getWeaponsCount(); index++) {
                        boolean isPrevUsed = id < 32 && (prevUseWeaponStat & 1 << id) != 0;
                        boolean isUsed = id < 32 && (this.useWeaponStat & 1 << id) != 0;
                        if (isLongDelay && isPrevUsed && isUsed) {
                            isUsed = false;
                        }

                        isWpnUsed |= isUsed;
                        if (!isPrevUsed && isUsed) {
                            float recoil = w.getInfo().recoil;
                            if (recoil > 0.0F) {
                                this.recoilCount = 30;
                                this.recoilValue = recoil;
                                this.recoilYaw = w.getYaw();
                            }
                        }

                        if (this.world.isRemote && isUsed) {
                            Vec3d wrv = MCH_Lib.RotVec3(0.0, 0.0, -1.0, -w.getYaw() - yaw, -w.getPitch());
                            Vec3d spv = w.getCurrentWeapon().getShotPos(this);
                            this.spawnParticleMuzzleFlash(this.world, w.getInfo(), this.posX + spv.x, this.posY + spv.y, this.posZ + spv.z, wrv);
                        }

                        w.updateWeapon(this, isUsed, index);
                        id++;
                    }

                    w.update(this, isSelected, isWpnUsed);
                    MCH_AircraftInfo.Weapon wi = this.getAcInfo().getWeaponById(wid);
                    if (wi != null && !this.isDestroyed()) {
                        Entity entity = this.getEntityBySeatId(this.getWeaponSeatID(this.getWeaponInfoById(wid), wi));
                        if (wi.canUsePilot && !(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
                            entity = this.getEntityBySeatId(0);
                        }

                        if (!(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
                            w.setTurretYaw(this.getLastRiderYaw() - this.getYaw());
                            if (this.getTowedChainEntity() != null || this.getRidingEntity() != null) {
                                w.setYaw(0.0F);
                            }
                        } else {
                            if ((int) wi.minYaw != 0 || (int) wi.maxYaw != 0) {
                                float ty = wi.turret ? MathHelper.wrapDegrees(this.getLastRiderYaw()) - yaw : 0.0F;
                                float ey = MathHelper.wrapDegrees(entity.rotationYaw - yaw - wi.defaultYaw - ty);
                                if (Math.abs((int) wi.minYaw) < 360 && Math.abs((int) wi.maxYaw) < 360) {
                                    float targetYaw = MCH_Lib.RNG(ey, wi.minYaw, wi.maxYaw);
                                    float wy = w.getYaw() - wi.defaultYaw - ty;
                                    if (targetYaw < wy) {
                                        if (wy - targetYaw > 15.0F) {
                                            wy -= 15.0F;
                                        } else {
                                            wy = targetYaw;
                                        }
                                    } else if (targetYaw > wy) {
                                        if (targetYaw - wy > 15.0F) {
                                            wy += 15.0F;
                                        } else {
                                            wy = targetYaw;
                                        }
                                    }

                                    w.setYaw(wy + wi.defaultYaw + ty);
                                } else {
                                    w.setYaw(ey + ty);
                                }
                            }

                            float ep = MathHelper.wrapDegrees(entity.rotationPitch - pitch);
                            w.setPitch(MCH_Lib.RNG(ep, wi.minPitch, wi.maxPitch));
                            w.setTurretYaw(0.0F);
                        }
                    }
                }

                this.updateWeaponBay();
                if (this.hitStatus > 0) {
                    this.hitStatus--;
                }
            }
        }
    }

    public void updateWeaponsRotation() {
        if (this.getAcInfo() != null) {
            if (this.getAcInfo().getWeaponCount() > 0) {
                if (!this.isDestroyed()) {
                    float yaw = MathHelper.wrapDegrees(this.getYaw());
                    float pitch = MathHelper.wrapDegrees(this.getPitch());

                    for (int wid = 0; wid < this.weapons.length; wid++) {
                        MCH_WeaponSet w = this.weapons[wid];
                        MCH_AircraftInfo.Weapon wi = this.getAcInfo().getWeaponById(wid);
                        if (wi != null) {
                            Entity entity = this.getEntityBySeatId(this.getWeaponSeatID(this.getWeaponInfoById(wid), wi));
                            if (wi.canUsePilot && !(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
                                entity = this.getEntityBySeatId(0);
                            }

                            if (!(entity instanceof EntityPlayer) && !(entity instanceof MCH_EntityGunner)) {
                                w.setTurretYaw(this.getLastRiderYaw() - this.getYaw());
                            } else {
                                if ((int) wi.minYaw != 0 || (int) wi.maxYaw != 0) {
                                    float ty = wi.turret ? MathHelper.wrapDegrees(this.getLastRiderYaw()) - yaw : 0.0F;
                                    float ey = MathHelper.wrapDegrees(entity.rotationYaw - yaw - wi.defaultYaw - ty);
                                    if (Math.abs((int) wi.minYaw) < 360 && Math.abs((int) wi.maxYaw) < 360) {
                                        float targetYaw = MCH_Lib.RNG(ey, wi.minYaw, wi.maxYaw);
                                        float wy = w.getYaw() - wi.defaultYaw - ty;
                                        if (targetYaw < wy) {
                                            if (wy - targetYaw > 15.0F) {
                                                wy -= 15.0F;
                                            } else {
                                                wy = targetYaw;
                                            }
                                        } else if (targetYaw > wy) {
                                            if (targetYaw - wy > 15.0F) {
                                                wy += 15.0F;
                                            } else {
                                                wy = targetYaw;
                                            }
                                        }

                                        w.setYaw(wy + wi.defaultYaw + ty);
                                    } else {
                                        w.setYaw(ey + ty);
                                    }
                                }

                                float ep = MathHelper.wrapDegrees(entity.rotationPitch - pitch);
                                w.setPitch(MCH_Lib.RNG(ep, wi.minPitch, wi.maxPitch));
                                w.setTurretYaw(0.0F);
                            }
                        }

                        w.setPrevYaw(w.getYaw());
                    }
                }
            }
        }
    }

    private void spawnParticleMuzzleFlash(World w, MCH_WeaponInfo wi, double px, double py, double pz, Vec3d wrv) {
        if (wi.listMuzzleFlashSmoke != null) {
            for (MCH_WeaponInfo.MuzzleFlash mf : wi.listMuzzleFlashSmoke) {
                double x = px + -wrv.x * mf.dist;
                double y = py + -wrv.y * mf.dist;
                double z = pz + -wrv.z * mf.dist;
                MCH_ParticleParam p = new MCH_ParticleParam(w, "smoke", px, py, pz);
                p.size = mf.size;

                for (int i = 0; i < mf.num; i++) {
                    p.a = mf.a * 0.9F + w.rand.nextFloat() * 0.1F;
                    float color = w.rand.nextFloat() * 0.1F;
                    p.r = color + mf.r * 0.9F;
                    p.g = color + mf.g * 0.9F;
                    p.b = color + mf.b * 0.9F;
                    p.age = (int) (mf.age + 0.1 * mf.age * w.rand.nextFloat());
                    p.posX = x + (w.rand.nextDouble() - 0.5) * mf.range;
                    p.posY = y + (w.rand.nextDouble() - 0.5) * mf.range;
                    p.posZ = z + (w.rand.nextDouble() - 0.5) * mf.range;
                    p.motionX = w.rand.nextDouble() * (p.posX < x ? -0.2 : 0.2);
                    p.motionY = w.rand.nextDouble() * (p.posY < y ? -0.03 : 0.03);
                    p.motionZ = w.rand.nextDouble() * (p.posZ < z ? -0.2 : 0.2);
                    MCH_ParticlesUtil.spawnParticle(p);
                }
            }
        }

        if (wi.listMuzzleFlash != null) {
            for (MCH_WeaponInfo.MuzzleFlash mf : wi.listMuzzleFlash) {
                float color = this.rand.nextFloat() * 0.1F + 0.9F;
                MCH_ParticlesUtil.spawnParticleMuzzleFlash(this.world, px + -wrv.x * mf.dist, py + -wrv.y * mf.dist, pz + -wrv.z * mf.dist, mf.size, color * mf.r, color * mf.g, color * mf.b, mf.a, mf.age + w.rand.nextInt(3));
            }
        }
    }

    private void updateWeaponBay() {
        for (int i = 0; i < this.weaponBays.length; i++) {
            MCH_EntityAircraft.WeaponBay wb = this.weaponBays[i];
            assert this.getAcInfo() != null;
            MCH_AircraftInfo.WeaponBay info = this.getAcInfo().partWeaponBay.get(i);
            boolean isSelected = false;
            Integer[] arr$ = info.weaponIds;

            for (int wid : arr$) {
                for (int sid = 0; sid < this.currentWeaponID.length; sid++) {
                    if (wid == this.currentWeaponID[sid] && this.getEntityBySeatId(sid) != null) {
                        isSelected = true;
                    }
                }
            }

            wb.prevRot = wb.rot;
            if (isSelected) {
                if (wb.rot < 90.0F) {
                    wb.rot += 3.0F;
                }

                if (wb.rot >= 90.0F) {
                    wb.rot = 90.0F;
                }
            } else {
                if (wb.rot > 0.0F) {
                    wb.rot -= 3.0F;
                }

                if (wb.rot <= 0.0F) {
                    wb.rot = 0.0F;
                }
            }
        }
    }

    public int getMaxHitStatus() {
        return 15;
    }

    public void hitBullet() {
        this.hitStatus = this.getMaxHitStatus();
    }

    public void initRotationYaw(float yaw) {
        this.rotationYaw = yaw;
        this.prevRotationYaw = yaw;
        this.lastRiderYaw = yaw;
        this.lastSearchLightYaw = yaw;

        for (MCH_WeaponSet w : this.weapons) {
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
            this.weaponBays = this.createWeaponBays();
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

    public void updateUAV() {
        if (this.isUAV()) {
            if (this.world.isRemote) {
                int eid = this.dataManager.get(UAV_STATION);
                if (eid > 0) {
                    if (this.uavStation == null) {
                        Entity uavEntity = this.world.getEntityByID(eid);
                        if (uavEntity instanceof MCH_EntityUavStation) {
                            this.uavStation = (MCH_EntityUavStation) uavEntity;
                            this.uavStation.setControlAircract(this);
                        }
                    }
                } else if (this.uavStation != null) {
                    this.uavStation.setControlAircract(null);
                    this.uavStation = null;
                }
            } else if (this.uavStation != null) {
                double udx = this.posX - this.uavStation.posX;
                double udz = this.posZ - this.uavStation.posZ;
                if (udx * udx + udz * udz > 15129.0) {
                    this.uavStation.setControlAircract(null);
                    this.setUavStation(null);
                    this.attackEntityFrom(DamageSource.OUT_OF_WORLD, this.getMaxHP() + 10);
                }
            }

            if (this.uavStation != null && this.uavStation.isDead) {
                this.uavStation = null;
            }
        }
    }

    public void switchGunnerMode(boolean mode) {
        boolean debug_bk_mode = this.isGunnerMode;
        Entity pilot = this.getEntityBySeatId(0);
        if (!mode || this.canSwitchGunnerMode()) {
            if (this.isGunnerMode && !mode) {
                this.setCurrentThrottle(this.beforeHoverThrottle);
                this.isGunnerMode = false;
                this.camera.setCameraZoom(1.0F);
                this.getCurrentWeapon(pilot).onSwitchWeapon(this.world.isRemote, this.isInfinityAmmo(pilot));
            } else if (!this.isGunnerMode && mode) {
                this.beforeHoverThrottle = this.getCurrentThrottle();
                this.isGunnerMode = true;
                this.camera.setCameraZoom(1.0F);
                this.getCurrentWeapon(pilot).onSwitchWeapon(this.world.isRemote, this.isInfinityAmmo(pilot));
            }
        }

        Object[] data = new Object[]{debug_bk_mode ? "ON" : "OFF", mode ? "ON" : "OFF"};
        MCH_Logger.debugLog(this.world, "switchGunnerMode %s->%s", data);
    }

    public boolean canSwitchGunnerMode() {
        if (this.getAcInfo() == null || !this.getAcInfo().isEnableGunnerMode) {
            return false;
        }

        if (!this.isCanopyClose()) {
            return false;
        }

        boolean seatOccupied = this.getEntityBySeatId(1) instanceof EntityPlayer;
        boolean concurrentAllowed = this.getAcInfo().isEnableConcurrentGunnerMode;

        return (concurrentAllowed || !seatOccupied) && !this.isHoveringMode();
    }

    public boolean canSwitchGunnerModeOtherSeat(EntityPlayer player) {
        int sid = this.getSeatIdByEntity(player);
        if (sid > 0) {
            MCH_SeatInfo info = this.getSeatInfo(sid);
            if (info != null) {
                return info.gunner && info.switchgunner;
            }
        }

        return false;
    }

    public void switchGunnerModeOtherSeat(EntityPlayer player) {
        this.isGunnerModeOtherSeat = !this.isGunnerModeOtherSeat;
    }

    public void switchHoveringMode(boolean mode) {
        this.stopRepelling();
        if (this.canSwitchHoveringMode() && this.isHoveringMode() != mode) {
            if (mode) {
                this.beforeHoverThrottle = this.getCurrentThrottle();
            } else {
                this.setCurrentThrottle(this.beforeHoverThrottle);
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
        return this.getAcInfo() != null && !this.isGunnerMode;
    }

    public boolean isHovering() {
        return this.isGunnerMode || this.isHoveringMode();
    }

    public boolean getIsGunnerMode(Entity entity) {
        if (this.getAcInfo() == null) {
            return false;
        } else {
            int id = this.getSeatIdByEntity(entity);
            if (id < 0) {
                return false;
            } else if (id == 0 && this.getAcInfo().isEnableGunnerMode) {
                return this.isGunnerMode;
            } else {
                MCH_SeatInfo[] st = this.getSeatsInfo();
                if (id >= st.length || !st[id].gunner) {
                    return false;
                } else {
                    return !this.world.isRemote || !st[id].switchgunner || this.isGunnerModeOtherSeat;
                }
            }
        }
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
        MCH_SeatInfo seatInfo = this.getSeatInfo(player);
        return seatInfo != null && seatInfo.fixRot && this.getIsGunnerMode(player);
    }

    public boolean isGunnerLookMode(EntityPlayer player) {
        return !this.isPilot(player) && this.isGunnerFreeLookMode;
    }

    public void switchGunnerFreeLookMode(boolean b) {
        this.isGunnerFreeLookMode = b;
    }

    public void switchGunnerFreeLookMode() {
        this.switchGunnerFreeLookMode(!this.isGunnerFreeLookMode);
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

    private int getPartStatus() {
        return this.dataManager.get(PART_STAT);
    }

    private void setPartStatus(int n) {
        this.dataManager.set(PART_STAT, n);
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

    protected MCH_EntityAircraft.WeaponBay[] createWeaponBays() {
        assert this.getAcInfo() != null;
        MCH_EntityAircraft.WeaponBay[] wbs = new MCH_EntityAircraft.WeaponBay[this.getAcInfo().partWeaponBay.size()];

        for (int i = 0; i < wbs.length; i++) {
            wbs[i] = new WeaponBay();
        }

        return wbs;
    }

    protected MCH_Parts createHatch() {
        MCH_Parts hatch = null;
        if (this.getAcInfo() != null && this.getAcInfo().haveHatch()) {
            hatch = new MCH_Parts(this, 4, PART_STAT, "Hatch");
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
            lg = new MCH_Parts(this, 2, PART_STAT, "LandingGear");
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
            canopy = new MCH_Parts(this, 0, PART_STAT, "Canopy");
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
        if (!this.world.isRemote) {
            this.setCommonStatus(11, b);
        }
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
        return new IFluidTankProperties[]{new FluidTankProperties(getFuelStack(), getMaxFuel())};
    }


    public int fill(FluidStack resource, boolean doFill) {
        var ac = this.getAcInfo();
        if (ac == null) return 0;
        int maxFill = getMaxFuel();
        int contents = getFuel();
        String type = getTypeName();
        String fluidName = resource.getFluid().getName();
        boolean sameFuel = fluidName.equalsIgnoreCase(type);

        //can't overfill or mix fuels
        if (contents >= maxFill || (contents > 0 && !sameFuel)) return 0;

        int accepted = Math.min(resource.amount, maxFill - contents);
        if (doFill) {
            this.dataManager.set(FUEL, contents + accepted);
        }
        return accepted;
    }

    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;

        int contents = getFuel();
        if (contents <= 0) return null;

        String type = getTypeName();
        String fluidName = resource.getFluid().getName();

        if (!fluidName.equalsIgnoreCase(type)) return null;

        int drained = Math.min(resource.amount, contents);

        if (doDrain) {
            this.dataManager.set(FUEL, contents - drained);
        }

        return new FluidStack(resource.getFluid(), drained);
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
        if (maxDrain <= 0) return null;

        int contents = getFuel();
        if (contents <= 0) return null;

        int drained = Math.min(maxDrain, contents);

        if (doDrain) {
            this.dataManager.set(FUEL, contents - drained);
        }

        Fluid fluid = FluidRegistry.getFluid(getTypeName());
        if (fluid == null) return null;

        return new FluidStack(fluid, drained);
    }

    public void manualReloadForPlayer(EntityPlayer player) {
        boolean didReload = getCurrentWeapon(player).manualReload();
        if (didReload && !world.isRemote && player instanceof EntityPlayerMP playerMP)
            new PacketSyncReload(this.getEntityId()).sendToPlayer(playerMP);
    }

    public String getName() {
        if (this.getAcInfo() == null) return super.getName();
        return I18n.hasKey("item.mcheli:" + acInfo.name + ".name") ?
                I18n.format("item.mcheli:" + acInfo.name + ".name") : acInfo.displayName;

    }

    public double getCurrentSpeed() {
        double motionX = this.motionX;
        double motionY = this.motionY;
        double motionZ = this.motionZ;

        double tickDistance = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);

        return tickDistance * 20.0D;
    }

    public static class UnmountReserve {

        final Entity entity;
        final double posX;
        final double posY;
        final double posZ;
        int cnt = 5;

        public UnmountReserve(Entity e, double x, double y, double z) {
            this.entity = e;
            this.posX = x;
            this.posY = y;
            this.posZ = z;
        }
    }

    public static class WeaponBay {

        public float rot = 0.0F;
        public float prevRot = 0.0F;

        public WeaponBay() {
        }
    }
}
