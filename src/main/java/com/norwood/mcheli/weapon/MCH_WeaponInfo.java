package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.MCH_DamageFactor;
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

    /** Block break particle count */
    public int flakParticlesCrack = 10;
    /** White smoke particle count */
    public int numParticlesFlak = 3;
    /** Block break particle spread (0.1 rifle ~ 0.6 anti-tank) */
    public float flakParticlesDiff = 0.3F;
    /** Infrared missile; affected by flares */
    public boolean isHeatSeekerMissile = true;
    /** Radar missile; affected by chaff */
    public boolean isRadarMissile = false;
    /** Maximum seeker tracking angle */
    public int maxDegreeOfMissile = 60;
    /** Lost-lock delay; -1 for infinite lock */
    public int tickEndHoming = -1;
    /** Maximum lock-on range */
    public int maxLockOnRange = 300;
    /** Maximum aircraft radar lock angle */
    public int maxLockOnAngle = 10;
    /** Pulse Doppler/Rear-aspect max angle; breaks lock beyond this */
    public float pdHDNMaxDegree = 1000f;
    /** Delay ticks before lock break after exceeding max angle */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /** Missile jamming resistance duration; -1 to disable */
    public int antiFlareCount = -1;
    /** Multipath clutter detection height; below this, radar lock breaks */
    public int lockMinHeight = 25;
    /** Semi-active radar missile (requires continuous guidance) */
    public boolean passiveRadar = false;
    /** Lock-out timer after semi-active guidance is lost */
    public int passiveRadarLockOutCount = 20;
    /** Enables laser guidance for TV missiles */
    public boolean laserGuidance = false;
    /** Presence of a laser guidance pod */
    public boolean hasLaserGuidancePod = true;
    /** Allows off-boresight firing for AA missiles */
    public boolean enableOffAxis = true;
    /** Maneuverability factor; 1.0 is vanilla, 0.1 recommended */
    public double turningFactor = 0.5;
    /** Enables chunk loader (experimental) */
    public boolean enableChunkLoader = false;
    /** Active radar missile (BVR); autonomous tracking after launch */
    public boolean activeRadar = false;
    /** Active radar scan interval */
    public int scanInterval = 20;
    /** Weapon switch cooldown */
    public int weaponSwitchCount = 0;
    /** Weapon switch sound effect */
    /** Vertical recoil */
    public float recoilPitch = 0.0F;
    /** Horizontal recoil (fixed direction) */
    public float recoilYaw = 0.0F;
    /** Random vertical recoil range */
    public float recoilPitchRange = 0.0F;
    /** Random horizontal recoil range */
    public float recoilYawRange = 0.0F;
    /** Recoil recovery speed */
    public float recoilRecoverFactor = 0.8F;
    /** Velocity change per tick; positive to accelerate, negative to decelerate */
    public float speedFactor = 0F;
    /** Start tick for velocity multiplier */
    public int speedFactorStartTick = 0;
    /** End tick for velocity multiplier */
    public int speedFactorEndTick = 0;
    /** Inherit aircraft velocity (Final speed = Aircraft + Bullet) */
    public boolean speedDependsAircraft = false;
    /** Allows locking onto missile entities */
    public boolean canLockMissile = false;
    /** Enables Beyond Visual Range (BVR) targeting */
    public boolean enableBVR = false;
    /** Minimum distance to enable BVR */
    public int minRangeBVR = 300;
    /** Enables target position prediction */
    public boolean predictTargetPos = true;
    /** Max chaff locks before missile reverts to unguided flight */
    public int numLockedChaffMax = 2;
    /** Explosion damage multipliers by entity type */
    public float explosionDamageVsLiving = 1f;
    public float explosionDamageVsPlayer = 1f;
    public float explosionDamageVsPlane = 1f;
    public float explosionDamageVsVehicle = 1f;
    public float explosionDamageVsTank = 1f;
    public float explosionDamageVsHeli = 1f;
    public boolean disableDestroyBlock = true;
    public boolean canBeIntercepted = false;
    public boolean canAirburst = false;
    public int explosionAirburst;
    /** HUD custom field for crosshair indicators */
    public int crossType = 0;
    /** Presence of mortar radar */
    public boolean hasMortarRadar = false;
    /** Mortar radar range; should exceed weapon range */
    public double mortarRadarMaxDist = -1;
    /** Marker Rocket parameters */
    public int markerRocketSpawnNum = 5;
    public int markerRocketSpawnDiff = 15;
    public int markerRocketSpawnHeight = 200;
    public int markerRocketSpawnSpeed = 5;
    public int hitSoundRange = 100;

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

    // TODO:Enumify
    public String getWeaponTypeName() {
        return switch (this.type.toLowerCase()) {
            case "machinegun1", "machinegun2", "railgun" -> "MachineGun";
            case "torpedo" -> "Torpedo";
            case "cas" -> "CAS";
            case "rocket" -> "Rocket";
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

    public static enum Payload {
        NONE,
        NTM_VNT,
        NTM_NT,
        NTM_MINI_NUKE,
        NTM_NUKE,
        NTM_CHEMICAL,
        NTM_MIST
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
