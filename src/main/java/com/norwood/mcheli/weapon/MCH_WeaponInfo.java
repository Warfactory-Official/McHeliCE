package com.norwood.mcheli.weapon;

import com.github.bsideup.jabel.Desugar;
import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.MCH_DamageFactor;
import com.norwood.mcheli.compat.hbm.VNTSettingContainer;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MCH_WeaponInfo extends MCH_BaseInfo {

    //HBM compat
    public Payload payloadNTM = Payload.NONE;
    public boolean effectOnly = false;
    public String fluidTypeNTM = null;
    public VNTSettingContainer vntSettingContainer = null;

    public final String name;
    public static Random rand = new Random();
    public String explosionType;
    public String displayName;
    public String type;
    public int power;
    public float acceleration;
    public float accelerationInWater;
    public int explosion;
    public int explosionBlock;
    public int explosionInWater;
    public int explosionAltitude;
    public int delayFuse;
    public float bound;
    public int timeFuse;
    public boolean flaming;
    public MCH_SightType sight;
    public float[] zoom;
    public int delay;
    public int reloadTime;
    public int round;
    public int suppliedNum;
    public int maxAmmo;
    public List<RoundItem> roundItems;
    public int soundDelay;
    public float soundVolume;
    public float soundPitch;
    public float soundPitchRandom;
    public int soundPattern;
    public int lockTime;
    public boolean ridableOnly;
    public float proximityFuseDist;
    public int rigidityTime;
    public float accuracy;
    public int bomblet;
    public int bombletSTime;
    public float bombletDiff;
    public int modeNum;
    public int fixMode;
    public int piercing;
    public int heatCount;
    public int maxHeatCount;
    public boolean isFAE;
    public boolean isGuidedTorpedo;
    public float gravity;
    public float gravityInWater;
    public float velocityInWater;
    public boolean destruct;
    public String trajectoryParticleName;
    public int trajectoryParticleStartTick;
    public boolean disableSmoke;
    public MCH_Cartridge cartridge;
    public MCH_Color color;
    public MCH_Color colorInWater;
    public String soundFileName;
    public float smokeSize;
    public int smokeNum;
    public int smokeMaxAge;
    public String dispenseItemLoc = null;
    public Item dispenseItem;
    public int dispenseDamege;
    public int dispenseRange;
    public int recoilBufCount;
    public int recoilBufCountSpeed;
    public float length;
    public float radius;
    public float angle;
    public boolean displayMortarDistance;
    public boolean fixCameraPitch;
    public float cameraRotationSpeedPitch;
    public int target;
    public int markTime;
    public float recoil;
    public String bulletModelName;
    public MCH_BulletModel bulletModel;
    public String bombletModelName;
    public MCH_BulletModel bombletModel;
    public MCH_DamageFactor damageFactor;
    public String group;
    public List<MuzzleFlash> listMuzzleFlash;
    public List<MuzzleFlash> listMuzzleFlashSmoke;

    /**
     * Number of block-breaking particles generated
     */
    public int flakParticlesCrack = 10;
    /**
     * Number of white smoke particles generated
     */
    public int numParticlesFlak = 3;
    /**
     * Spread of block-breaking particles. Recommended values: 0.1 (rifle bullet) ~ 0.6 (anti-tank rifle)
     */
    public float flakParticlesDiff = 0.3F;
    public String hitSound = "";
    public String hitSoundIron = "hit_metal";
    public String railgunSound = "railgun";
    public float hitSoundRange = 100;
    /**
     * Whether this is an infrared missile (affected by flares)
     */
    public boolean isHeatSeekerMissile = true;
    /**
     * Whether this is a radar-guided missile (affected by chaff)
     */
    public boolean isRadarMissile = false;
    // Maximum homing angle of the missile seeker
    public int maxDegreeOfMissile = 60;
    // Unlock delay; -1 means permanent lock
    public int tickEndHoming = -1;
    /**
     * Maximum lock-on range
     */
    public int maxLockOnRange = 300;
    /**
     * Maximum lock-on angle for aircraft radar
     */
    public int maxLockOnAngle = 10;
    /**
     * Maximum angle for pulse-Doppler radar; beyond this, the missile will lose lock (can be used for IR missile rear attacks)
     */
    public float pdHDNMaxDegree = 1000f;
    /**
     * Unlock interval for pulse-Doppler radar; after exceeding max angle, missile unlocks after this many ticks
     */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /**
     * Duration of flare resistance; -1 means no resistance
     */
    public int antiFlareCount = -1;
    /**
     * Radar missile multipath clutter detection height — aircraft below this height will cause radar lock loss
     */
    public int lockMinHeight = 25;
    /**
     * Semi-active radar missile requires continuous guidance
     */
    public boolean passiveRadar = false;

    /**
     * Unlock countdown after semi-active radar missile loses guidance
     */
    public int passiveRadarLockOutCount = 20;

    /**
     * Enable laser guidance for TV missiles
     */
    public boolean laserGuidance = false;

    /**
     * Whether the aircraft has a laser targeting pod
     */
    public boolean hasLaserGuidancePod = true;

    /// Allow off-boresight firing (for AA missiles)
    public boolean enableOffAxis = true;

    /// Missile maneuverability factor — smaller = smoother. 1 means vanilla missile movement. Recommended: 0.1
    public double turningFactor = 0.5;

    /// Enable chunk loader (experimental feature)
    public boolean enableChunkLoader = false;

    /// Active radar missile (BVR) will automatically track target after launch
    public boolean activeRadar = false;

    /// Scan interval for active radar missile
    public int scanInterval = 20;

    /// Weapon switch cooldown
    public int weaponSwitchCount = 0;

    /// Weapon switch sound effect
    public String weaponSwitchSound = "";

    /// Vertical weapon recoil
    public float recoilPitch = 0.0F;
    /**
     * Horizontal weapon recoil (fixed direction)
     */
    public float recoilYaw = 0.0F;
    /**
     * Random vertical recoil (Recoil 2 + rndRecoil 0.5 == 1.5–2.5 recoil range)
     */
    public float recoilPitchRange = 0.0F;
    /**
     * Random horizontal recoil
     */
    public float recoilYawRange = 0.0F;
    /**
     * Weapon recoil recovery speed
     */
    public float recoilRecoverFactor = 0.8F;

    /**
     * Per-tick speed increment. Negative = deceleration, positive = acceleration
     */
    public float speedFactor = 0F;
    /**
     * Start tick for per-tick speed multiplier
     */
    public int speedFactorStartTick = 0;
    /**
     * End tick for per-tick speed multiplier
     */
    public int speedFactorEndTick = 0;
    /**
     * Whether missile speed depends on aircraft velocity (final speed = aircraft speed + projectile speed)
     */
    public boolean speedDependsAircraft = false;
    /**
     * Whether missile can lock onto other missile entities
     */
    public boolean canLockMissile = false;
    /**
     * Allow beyond visual range (BVR) target acquisition
     */
    public boolean enableBVR = false;
    /**
     * Minimum activation distance for BVR functionality
     */
    public int minRangeBVR = 300;
    /**
     * Predict target position
     */
    public boolean predictTargetPos = true;

    /**
     * Maximum number of chaff locks — beyond this, the missile will fly straight
     */
    public int numLockedChaffMax = 2;

    /**
     * Explosion damage multipliers for different entity types
     */
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
    /**
     * HUD custom field, used to indicate reticle type
     */
    public int crossType = 0;
    /**
     * Whether the weapon has a mortar radar
     */
    public boolean hasMortarRadar = false;
    /**
     * Maximum display distance for mortar radar — should be greater than the indirect-fire weapon’s max range
     */
    public double mortarRadarMaxDist = -1;

    /**
     * Marker rocket parameters
     */
    public int markerRocketSpawnNum = 5;
    public int markerRocketSpawnDiff = 15;
    public int markerRocketSpawnHeight = 200;
    public int markerRocketSpawnSpeed = 5;

    public MCH_WeaponInfo(AddonResourceLocation location, String path) {
        super(location, path);
        this.name = location.getPath();
        this.displayName = this.name;
        this.type = "";
        this.power = 0;
        this.acceleration = 1.0F;
        this.accelerationInWater = 1.0F;
        this.explosion = 0;
        this.explosionBlock = -1;
        this.explosionInWater = 0;
        this.explosionAltitude = 0;
        this.delayFuse = 0;
        this.timeFuse = 0;
        this.flaming = false;
        this.sight = MCH_SightType.NONE;
        this.zoom = new float[]{1.0F};
        this.delay = 10;
        this.reloadTime = 30;
        this.round = 0;
        this.suppliedNum = 1;
        this.roundItems = new ArrayList<>();
        this.maxAmmo = 0;
        this.soundDelay = 0;
        this.soundPattern = 0;
        this.soundVolume = 1.0F;
        this.soundPitch = 1.0F;
        this.soundPitchRandom = 0.1F;
        this.lockTime = 30;
        this.ridableOnly = false;
        this.proximityFuseDist = 0.0F;
        this.rigidityTime = 7;
        this.accuracy = 0.0F;
        this.bomblet = 0;
        this.bombletSTime = 10;
        this.bombletDiff = 0.3F;
        this.modeNum = 0;
        this.fixMode = 0;
        this.piercing = 0;
        this.heatCount = 0;
        this.maxHeatCount = 0;
        this.bulletModelName = "";
        this.bombletModelName = "";
        this.bulletModel = null;
        this.bombletModel = null;
        this.isFAE = false;
        this.isGuidedTorpedo = false;
        this.gravity = 0.0F;
        this.gravityInWater = 0.0F;
        this.velocityInWater = 0.999F;
        this.destruct = false;
        this.trajectoryParticleName = "explode";
        this.trajectoryParticleStartTick = 0;
        this.cartridge = null;
        this.disableSmoke = false;
        this.color = new MCH_Color();
        this.colorInWater = new MCH_Color();
        this.soundFileName = this.name + "_snd";
        this.smokeMaxAge = 100;
        this.smokeNum = 1;
        this.smokeSize = 2.0F;
        this.dispenseItem = null;
        this.dispenseDamege = 0;
        this.dispenseRange = 1;
        this.recoilBufCount = 2;
        this.recoilBufCountSpeed = 3;
        this.length = 0.0F;
        this.radius = 0.0F;
        this.target = 1;
        this.recoil = 0.0F;
        this.damageFactor = null;
        this.group = "";
        this.listMuzzleFlash = null;
        this.listMuzzleFlashSmoke = null;
        this.displayMortarDistance = false;
        this.fixCameraPitch = false;
        this.cameraRotationSpeedPitch = 1.0F;
    }

    @Override
    public void onPostReload() {
        MCH_WeaponInfoManager.setRoundItems();
        if(dispenseItemLoc != null){
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(dispenseItemLoc));
            if(item != null) dispenseItem = item;
        }
    }


    public void checkData() {
        if (this.explosionBlock < 0) {
            this.explosionBlock = this.explosion;
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
    }

    public float getDamageFactor(Entity e) {
        return this.damageFactor != null ? this.damageFactor.getDamageFactor(e) : 1.0F;
    }

    //TODO:Enumify
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
        NTM_EXP_SMALL,
        NTM_EXP_LARGE,
        NTM_MINI_NUKE,
        NTM_NUKE,
        NTM_CHLORINE,
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
    public static class MuzzleFlashRaw
     {
        float Distance;
        float Size;
        float Range;
        int Age;
        int Count;
        String Color; //ARGB
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
