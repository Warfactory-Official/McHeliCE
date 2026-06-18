package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.MCH_DamageFactor;
import com.norwood.mcheli.MCH_PotionEffect;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.compat.hbm.*;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import com.norwood.mcheli.sound.SoundRegistry;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.norwood.mcheli.compat.ModCompatManager.MODID_HBM;
import static com.norwood.mcheli.compat.ModCompatManager.isLoaded;

public class MCH_WeaponInfo extends MCH_BaseInfo {

    public static Random rand = new Random();
    public final String name;
    // HBM compat
    public boolean useHBM = false;
    public Payload payloadNTM = Payload.NONE;
    public boolean effectOnly = false;
    public VNTSettingContainer vntSettingContainer = null;
    public ChemicalContainer chemicalContainer = null;
    public MistContainer mistContainer = null;
    public MukeContainer mukeContainer = null;
    public NTSettingContainer ntSettingContainer = null;
    public FireContainer fireContainer = null;
    public String explosionType;
    public String displayName;
    public String type = "";
    public int power = 0;
    public float acceleration = 1.0F;
    public float accelerationInWater = 1.0F;
    public int explosion = 0;
    public int explosionBlock = -1;
    public int explosionInWater = 0;
    public int explosionAltitude = 0;
    public int delayFuse = 0;
    public float bound;
    public int timeFuse = 0;
    public boolean flaming = false;
    public MCH_SightType sight = MCH_SightType.NONE;
    public float[] zoom = new float[]{1.0F};
    public int delay = 10;
    public int reloadTime = 30;
    public int round = 0;
    public int suppliedNum = 1;
    public int maxAmmo = 0;
    public List<RoundItem> roundItems = new ArrayList<>();
    public int soundDelay = 0;
    public float soundVolume = 1.0F;
    public float soundPitch = 1.0F;
    public float soundPitchRandom = 0.1F;
    public int soundPattern = 0;
    public int lockTime = 30;
    public boolean ridableOnly = false;
    public float proximityFuseDist = 0.0F;
    public int rigidityTime = 7;
    public float accuracy = 0.0F;
    public int bomblet = 0;
    public int bombletSTime = 10;
    public float bombletDiff = 0.3F;
    public int modeNum = 0;
    public int fixMode = 0;
    public int piercing = 0;
    public int heatCount = 0;
    public int maxHeatCount = 0;
    public boolean isFAE = false;
    public boolean isGuidedTorpedo = false;
    public float gravity = 0.0F;
    public float gravityInWater = 0.0F;
    public float velocityInWater = 0.999F;
    public boolean destruct = false;
    public String trajectoryParticleName = "explode";
    public int trajectoryParticleStartTick = 0;
    public int trajectoryParticleEndTick = -1; // Reforged field
    public boolean disableSmoke = false;
    public MCH_Cartridge cartridge = null;
    public MCH_Color color = new MCH_Color();
    public MCH_Color colorInWater = new MCH_Color();
    public float smokeSize = 2.0F;
    public int smokeNum = 1;
    public int smokeMaxAge = 100;
    public String dispenseItemLoc = null;
    public Item dispenseItem = null;
    public int dispenseDamege = 0;
    public int dispenseRange = 1;
    public int recoilBufCount = 2;
    public int recoilBufCountSpeed = 3;
    public float length = 0.0F;
    public float radius = 0.0F;
    public float angle;
    public boolean displayMortarDistance = false;
    public boolean fixCameraPitch = false;
    public float cameraRotationSpeedPitch = 1.0F;
    public int target = 1;
    public int markTime;
    public float recoil = 0.0F;
    public String bulletModelName = "";
    public MCH_BulletModel bulletModel = null;
    public int bulletModelEndTick = -1; // Reforged field
    public String bulletModelNameEnd = ""; // Reforged field
    public MCH_BulletModel bulletModelEnd = null; // Reforged field
    public String bombletModelName = "";
    public MCH_BulletModel bombletModel = null;
    public MCH_DamageFactor damageFactor = null;
    public String group = "";
    public List<MuzzleFlash> listMuzzleFlash = null;
    public List<MuzzleFlash> listMuzzleFlashSmoke = null;
    @NotNull
    public ResourceLocation fireSound;
    @Nullable
    public  ResourceLocation hitSound;

    public ResourceLocation hitSoundIron = new ResourceLocation(Tags.MODID, "hit_metal");
    public ResourceLocation railgunSound = new ResourceLocation(Tags.MODID, "railgun");
    @Nullable
    public ResourceLocation weaponSwitchSound = null;

    /** Reforged field: block break particle count. */
    public int flakParticlesCrack = 10;
    /** Reforged field: white smoke particle count. */
    public int numParticlesFlak = 3;
    /** Reforged field: block break particle spread (0.1 rifle ~ 0.6 anti-tank). */
    public float flakParticlesDiff = 0.3F;
    /** Reforged field: infrared missile; affected by flares. */
    public boolean isHeatSeekerMissile = true;
    /** Reforged field: radar missile; affected by chaff. */
    public boolean isRadarMissile = false;
    /** Reforged field: maximum seeker tracking angle. */
    public int maxDegreeOfMissile = 60;
    /** Reforged field: initial seeker-angle phase duration. */
    public int initMaxDegreeTick = 0;
    /** Reforged field: seeker angle used during the initial phase. */
    public int initMaxDegreeOfMissile = 60;
    /** Reforged field: lost-lock delay; -1 for infinite lock. */
    public int tickEndHoming = -1;
    /** Reforged field: maximum lock-on range. */
    public int maxLockOnRange = 300;
    /** Reforged field: maximum aircraft radar lock angle. */
    public int maxLockOnAngle = 10;
    /** Reforged field: pulse-Doppler/rear-aspect max angle. */
    public float pdHDNMaxDegree = 1000f;
    /** Reforged field: delay ticks before lock break after exceeding max angle. */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /** Reforged field: missile jamming resistance duration; -1 to disable. */
    public int antiFlareCount = -1;
    /** Reforged field: multipath clutter detection height. */
    public int lockMinHeight = 25;
    /** Reforged field: semi-active radar missile. */
    public boolean passiveRadar = false;
    /** Reforged field: semi-active radar missile that can data-link without self-search. */
    public boolean semiActiveRadar = false;
    /** Reforged field: lock-out timer after semi-active guidance is lost. */
    public int passiveRadarLockOutCount = 20;
    /** Reforged field: enables laser guidance for TV missiles. */
    public boolean laserGuidance = false;
    /** Reforged field: presence of a laser guidance pod. */
    public boolean hasLaserGuidancePod = true;
    /** Reforged field: allows off-boresight firing for AA missiles. */
    public boolean enableOffAxis = true;
    /** Reforged field: maneuverability factor; 1.0 is vanilla, 0.1 recommended. */
    public double turningFactor = 0.5;
    /** Reforged field: initial turning-factor phase duration. */
    public int initTurningFactorTick = 0;
    /** Reforged field: turning factor used during the initial phase. */
    public double initTurningFactor = 0.5;
    /** Reforged field: enables chunk loader. */
    public boolean enableChunkLoader = false;
    /** Reforged field: active radar missile (BVR); autonomous tracking after launch. */
    public boolean activeRadar = false;
    /** Reforged field: active radar scan interval. */
    public int scanInterval = 20;
    /** Reforged field: weapon switch cooldown. */
    public int weaponSwitchCount = 0;
    /** Weapon switch sound effect */
    /** Reforged field: vertical recoil. */
    public float recoilPitch = 0.0F;
    /** Reforged field: horizontal recoil (fixed direction). */
    public float recoilYaw = 0.0F;
    /** Reforged field: random vertical recoil range. */
    public float recoilPitchRange = 0.0F;
    /** Reforged field: random horizontal recoil range. */
    public float recoilYawRange = 0.0F;
    /** Reforged field: recoil recovery speed. */
    public float recoilRecoverFactor = 0.8F;
    /** Reforged field: velocity change per tick; positive to accelerate, negative to decelerate. */
    public float speedFactor = 0F;
    /** Reforged field: start tick for velocity multiplier. */
    public int speedFactorStartTick = 0;
    /** Reforged field: end tick for velocity multiplier. */
    public int speedFactorEndTick = 0;
    /** Reforged field: inherit aircraft velocity (Final speed = Aircraft + Bullet). */
    public boolean speedDependsAircraft = false;
    /** Reforged field: allows locking onto missile entities. */
    public boolean canLockMissile = false;
    /** Reforged: allow guidance to lock targets sitting in water (e.g. ships/boats). */
    public boolean canLockInWater = false;
    /** Reforged field: enables Beyond Visual Range (BVR) targeting. */
    public boolean enableBVR = false;
    /** Reforged field: minimum distance to enable BVR. */
    public int minRangeBVR = 300;
    /** Reforged field: enables target position prediction. */
    public boolean predictTargetPos = true;
    /** Reforged field: max chaff locks before missile reverts to unguided flight. */
    public int numLockedChaffMax = 2;
    /** Reforged field: explosion damage multipliers by entity type. */
    public float explosionDamageVsLiving = 1f;
    public float explosionDamageVsPlayer = 1f;
    public float explosionDamageVsPlane = 1f;
    public float explosionDamageVsVehicle = 1f;
    public float explosionDamageVsTank = 1f;
    public float explosionDamageVsHeli = 1f;
    public float explosionDamageVsShip = 1f; // Reforged field
    public boolean explosionThroughWall = false; // Reforged field
    public float explosionThroughWallFactor = 1.0f; // Reforged field
    public boolean isNewExplosionBreak = true; // Reforged field
    public boolean disableDestroyBlock = true; // Reforged field
    public boolean canBeIntercepted = false; // Reforged field
    public boolean canAirburst = false; // Reforged field
    public int explosionAirburst = 0; // Reforged field
    /** Reforged field: CCIP support flag. */
    public boolean ccip = false;
    /** Reforged field: CCIP texture name. */
    public String ccipTexture = "CCIP";
    /** Reforged field: CCIP render scale factor. */
    public float ccipFactor = 1.0F;
    /** Reforged field: HUD custom field for crosshair indicators. */
    public int crossType = 0;
    /** Reforged field: presence of mortar radar. */
    public boolean hasMortarRadar = false;
    /** Reforged field: mortar radar range; should exceed weapon range. */
    public double mortarRadarMaxDist = -1;
    /** Reforged field: marker rocket parameters. */
    public int markerRocketSpawnNum = 5;
    public int markerRocketSpawnDiff = 15;
    public int markerRocketSpawnHeight = 200;
    public int markerRocketSpawnSpeed = 5;
    /** Reforged field: hit sound broadcast range. */
    public int hitSoundRange = 100;
    public boolean enableExhaustFlare = false; // Reforged field
    public boolean spawnBulletInAir = false; // Reforged field
    public int spawnBulletMaxNum = 1; // Reforged field
    public int spawnBulletIntervalTick = 20; // Reforged field
    public int spawnBulletPerNum = 1; // Reforged field
    public boolean spawnBulletInheritSpeed = false; // Reforged field
    public boolean destructAfterSpawnBullet = false; // Reforged field
    public boolean ahead = false; // Reforged field
    public int aheadSolveIntervalTick = 2; // Reforged field
    public List<MCH_IBulletDecay> bulletDecay = new ArrayList<>(); // Reforged field
    public boolean enableBulletDecay = false; // Reforged field
    public List<MCH_PotionEffect> potionEffect = new ArrayList<>(); // Reforged field
    public boolean isGPSMissile = false; // Reforged field
    public boolean ballisticMissile = false; // Reforged field
    public double ballisticArcFactor = 0.20D; // Reforged field
    public double ballisticArcMinHeight = 20.0D; // Reforged field
    public double ballisticArcMaxHeight = 400.0D; // Reforged field
    public double ballisticMinDistance = 80.0D; // Reforged field
    public boolean ballisticLateralSine = false; // Reforged field
    public double ballisticLateralAmplitude = 12.0D; // Reforged field
    public double ballisticLateralWaves = 1.5D; // Reforged field
    public double ballisticLateralPhaseDeg = 0.0D; // Reforged field
    public double ballisticLateralStartRatio = 0.20D; // Reforged field
    public double ballisticLateralEndRatio = 0.85D; // Reforged field
    public double ballisticTerminalNoWeaveDist = 80.0D; // Reforged field
    public double ballisticTerminalCylinderRadius = 40.0D; // Reforged field
    public int canister = -1; // Reforged field
    public int canisterType = 0; // Reforged field
    public double dragInAir = 0.0D; // Reforged field
    public int buckshotCount = 6; // Reforged field
    public BuckshotPayload buckshotPayload = BuckshotPayload.BULLET; // MCHCE
    public boolean lockEntity = false; // Reforged field
    public boolean cameraFollowLockEntity = false; // Reforged field
    public float cameraFollowStrength = 0.3f; // Reforged field
    public boolean antiRadiationMissile = false; // Reforged field
    public int armEmitterLostGraceTick = 10; // Reforged field
    public int armMemoryTimeTick = 100; // Reforged field
    public boolean armCruiseEnable = false; // Reforged field
    public double armCruiseStartDistance = 150.0D; // Reforged field
    public double armCruiseTerminalRadius = 50.0D; // Reforged field
    public double armCruiseTerminalHeight = 1024.0D; // Reforged field
    public boolean enableDataLink = false; // Reforged field
    public boolean onlyDataLink = false; // Reforged field
    public boolean enableHMS = true; // Reforged field
    public String nameOnRWR = "MSL"; // Reforged field
    public float rcsFrontFactor = 1.0F; // Reforged field
    public float rcsSideFactor = 1.0F; // Reforged field
    public float rcsRearFactor = 1.0F; // Reforged field
    public float rcsTimeFactor = 1.0F; // Reforged field
    public int proximityFuseTick = -1; // Reforged field
    public float proximityFuseDamage = 0.0F; // Reforged field
    public int proximityFuseHeight = 20; // Reforged field

    public MCH_MineFuze mineFuze = MCH_MineFuze.NONE;
    public float mineRange = 4.0F;
    public int mineArmDelay = 40;

    public MCH_WeaponInfo(AddonResourceLocation location, String path, String parser) {
        super(location, path, parser);
        this.name = location.getPath();
        this.displayName = this.name;
        this.fireSound = new ResourceLocation(Tags.MODID, this.name + "_snd");
    }

    @Override
    public void onPostReload() {
        MCH_WeaponInfoManager.setRoundItems();
        if (dispenseItemLoc != null) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(dispenseItemLoc));
            if (item != null) dispenseItem = item;
        }
        loadNTMFunctionality();
    }

    public void loadNTMFunctionality() {
        if (useHBM && isLoaded(MODID_HBM)) {
            if (vntSettingContainer != null)
                vntSettingContainer.loadRuntimeInstances();
            if (ntSettingContainer != null) {
                ntSettingContainer.loadRuntimeInstances();
            }
            if (mukeContainer != null) {
                mukeContainer.loadRuntimeInstances();
            }
        } else if (useHBM && !isLoaded(MODID_HBM))
            MCH_Logger.get().warn(
                    "Weapon:\"{}\" uses HBM capabilities, to use it please install HBM:NTM Community Edition", name);
    }

    public boolean validate() {
        if (this.explosionBlock < 0) {
            this.explosionBlock = this.explosion;
        }

        if (this.explosionAirburst <= 0) {
            this.explosionAirburst = this.explosion;
        }

        if (this.fixMode >= this.modeNum) {
            this.fixMode = 0;
        }

        if (this.round <= 0) {
            this.round = this.maxAmmo;
        }

        if (this.round > this.maxAmmo) {
            this.round = this.maxAmmo;
        }

        if (this.explosion <= 0) {
            this.isFAE = false;
        }

        if (this.delayFuse <= 0) {
            this.bound = 0.0F;
        }

        if (this.isFAE) {
            this.explosionInWater = 0;
        }

        if (this.bomblet > 0 && this.bombletSTime < 1) {
            this.bombletSTime = 1;
        }

        if (this.destruct) {
            this.delay = 1000000;
        }

        if (this.spawnBulletIntervalTick < 1) {
            this.spawnBulletIntervalTick = 1;
        }

        if (this.aheadSolveIntervalTick < 1) {
            this.aheadSolveIntervalTick = 1;
        }

        if (this.trajectoryParticleEndTick >= 0 && this.trajectoryParticleEndTick < this.trajectoryParticleStartTick) {
            this.trajectoryParticleEndTick = this.trajectoryParticleStartTick;
        }

        if (this.bulletModelEndTick < -1) {
            this.bulletModelEndTick = -1;
        }

        if (this.mineFuze != MCH_MineFuze.NONE) {
            if (this.mineRange <= 0.0F) {
                this.mineRange = 4.0F;
            }
            if (this.mineArmDelay < 0) {
                this.mineArmDelay = 0;
            }
        }

        this.ballisticArcFactor = Math.max(0.0D, this.ballisticArcFactor);
        this.ballisticArcMinHeight = Math.max(0.0D, this.ballisticArcMinHeight);
        this.ballisticArcMaxHeight = Math.max(this.ballisticArcMinHeight, this.ballisticArcMaxHeight);
        this.ballisticMinDistance = Math.max(0.0D, this.ballisticMinDistance);
        this.ballisticLateralAmplitude = Math.max(0.0D, this.ballisticLateralAmplitude);
        this.ballisticLateralWaves = Math.max(0.0D, this.ballisticLateralWaves);
        this.ballisticLateralStartRatio = Math.max(0.0D, Math.min(1.0D, this.ballisticLateralStartRatio));
        this.ballisticLateralEndRatio = Math.max(this.ballisticLateralStartRatio,
                Math.min(1.0D, this.ballisticLateralEndRatio));
        this.ballisticTerminalNoWeaveDist = Math.max(0.0D, this.ballisticTerminalNoWeaveDist);
        this.ballisticTerminalCylinderRadius = Math.max(0.0D, this.ballisticTerminalCylinderRadius);
        this.armCruiseStartDistance = Math.max(0.0D, this.armCruiseStartDistance);
        this.armCruiseTerminalRadius = Math.max(0.0D, this.armCruiseTerminalRadius);
        this.armCruiseTerminalHeight = Math.max(0.0D, this.armCruiseTerminalHeight);

        if (!this.enableDataLink) {
            this.onlyDataLink = false;
        }

        if (!isCCIPSupportedType(this.type)) {
            this.ccip = false;
        }

        this.angle = (float) (Math.atan2(this.radius, this.length) * 180.0D / 3.141592653589793D);

        SoundRegistry.INSTANCE.parseSound(this.fireSound.getPath());
        if (this.hitSound != null) SoundRegistry.INSTANCE.parseSound(this.hitSound.getPath());
        SoundRegistry.INSTANCE.parseSound(this.hitSoundIron.getPath());
        SoundRegistry.INSTANCE.parseSound(this.railgunSound.getPath());
        if (this.weaponSwitchSound != null) SoundRegistry.INSTANCE.parseSound(this.weaponSwitchSound.getPath());

        return true;
    }

    public float getDamageFactor(Entity e) {
        return this.damageFactor != null ? this.damageFactor.getDamageFactor(e) : 1.0F;
    }

    /** @return true if this bomb is configured as a persistent proximity mine. */
    public boolean isMine() {
        return this.mineFuze != MCH_MineFuze.NONE;
    }

    // TODO:Enumify
    public String getWeaponTypeName() {
        return switch (this.type.toLowerCase()) {
            case "machinegun1", "machinegun2", "railgun" -> "MachineGun";
            case "torpedo" -> "Torpedo";
            case "cas" -> "CAS";
            case "rocket" -> "Rocket";
            case "buckshot" -> "Buckshot";
            case "asmissile" -> "AS Missile";
            case "aamissile" -> "AA Missile";
            case "tvmissile" -> "TV Missile";
            case "atmissile" -> "AT Missile";
            case "bomb" -> "Bomb";
            case "mkrocket" -> "Mk Rocket";
            case "dummy" -> "Dummy";
            case "smoke" -> "Smoke";
            case "dispenser" -> "Dispenser";
            case "targetingpod" -> "Targeting Pod";
            default -> "";
        };
    }

    public float getRecoilPitch() {
        return this.recoilPitch + (rand.nextFloat() * this.recoilPitchRange);
    }

    public float getRecoilYaw() {
        return this.recoilYaw + ((rand.nextFloat() - 0.5F) * this.recoilYawRange);
    }

    public int getCurrentMaxDegree(int missileTick) {
        if (this.initMaxDegreeTick > 0 && missileTick >= 0 && missileTick <= this.initMaxDegreeTick) {
            return this.initMaxDegreeOfMissile;
        }
        return this.maxDegreeOfMissile;
    }

    public double getCurrentTurningFactor(int missileTick) {
        if (this.initTurningFactorTick > 0 && missileTick >= 0 && missileTick <= this.initTurningFactorTick) {
            return this.initTurningFactor;
        }
        return this.turningFactor;
    }

    public static boolean isCCIPSupportedType(String type) {
        if (type == null) {
            return false;
        }
        return type.equalsIgnoreCase("rocket")
                || type.equalsIgnoreCase("atmissile")
                || type.equalsIgnoreCase("tvmissile");
    }

    public static enum Payload {
        NONE,
        NTM_VNT,
        NTM_NT,
        NTM_MINI_NUKE,
        NTM_NUKE,
        NTM_CHEMICAL,
        NTM_MIST,
        NTM_FIRE
    }

    // Reforged field - selects which entity each buckshot pellet spawns as
    public static enum BuckshotPayload {
        BULLET,
        ROCKET
    }

    public static class RoundItem {

        public final int num;
        public final ResourceLocation itemName;
        public final int damage;
        public ItemStack itemStack = ItemStack.EMPTY;

        public RoundItem(int n, String name, int damage) {
            this.num = n;
            this.itemName = new ResourceLocation(name);
            this.damage = damage;
        }
    }

    @Getter
    @Setter
    public static class MuzzleFlashRaw {

        float Distance;
        float Size;
        float Range;
        int Age;
        int Count;
        String Color; // ARGB
    }

    public static class MuzzleFlash {

        public final float dist;
        public final float size;
        public final float range;
        public final int age;
        public final float a;
        public final float r;
        public final float g;
        public final float b;
        public final int num;

        public MuzzleFlash(MuzzleFlashRaw raw) {
            this.dist = raw.getDistance();
            this.size = raw.getSize();
            this.range = raw.getRange();
            this.age = raw.getAge();
            this.num = raw.getCount();

            int color = YamlParser.parseHexColor(raw.getColor());
            this.a = ((color >> 24) & 0xFF) / 255.0F;
            this.r = ((color >> 16) & 0xFF) / 255.0F;
            this.g = ((color >> 8) & 0xFF) / 255.0F;
            this.b = (color & 0xFF) / 255.0F;
        }

        @Deprecated
        public MuzzleFlash(float dist, float size, float range, int age, float a, float r, float g, float b, int num) {
            this.dist = dist;
            this.size = size;
            this.range = range;
            this.age = age;
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
            this.num = num;
        }
    }
}
