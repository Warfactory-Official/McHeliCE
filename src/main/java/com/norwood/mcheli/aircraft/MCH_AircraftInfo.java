package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.RWRType;
import com.norwood.mcheli.RadarType;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.info.IItemContent;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.hud.MCH_HudManager;
import com.norwood.mcheli.hud.direct_drawable.*;
import com.norwood.mcheli.wrapper.W_Entity;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class MCH_AircraftInfo extends MCH_BaseInfo implements IItemContent {

    public final String name;
    public final List<IRecipe> recipe = new ArrayList<>();
    public final List<MCH_AircraftInfo.WeaponSet> weaponSetList = new ArrayList<>();
    public final List<MCH_SeatInfo> seatList = new ArrayList<>();
    public final List<Integer[]> exclusionSeatList = new ArrayList<>();
    public final List<MCH_Hud> hudList = new ArrayList<>();
    public final List<MCH_BoundingBox> extraBoundingBox = new ArrayList<>();
    public final List<MCH_AircraftInfo.Wheel> wheels = this.getDefaultWheelList();
    public final List<MCH_SeatRackInfo> entityRackList = new ArrayList<>();
    public final MCH_MobDropOption mobDropOption = new MCH_MobDropOption();
    public final List<MCH_AircraftInfo.RepellingHook> repellingHooks = new ArrayList<>();
    public final List<MCH_AircraftInfo.RideRack> rideRacks = new ArrayList<>();
    public final List<MCH_AircraftInfo.ParticleSplash> particleSplashs = new ArrayList<>();
    public final List<MCH_AircraftInfo.SearchLight> searchLights = new ArrayList<>();
    public final List<MCH_AircraftInfo.CameraPosition> cameraPosition = new ArrayList<>();
    public final List<MCH_AircraftInfo.Hatch> hatchList = new ArrayList<>();
    public final List<MCH_AircraftInfo.Camera> cameraList = new ArrayList<>();
    public final List<MCH_AircraftInfo.PartWeapon> partWeapon = new ArrayList<>();
    public final List<MCH_AircraftInfo.WeaponBay> partWeaponBay = new ArrayList<>();
    public final List<MCH_AircraftInfo.Canopy> canopyList = new ArrayList<>();
    public final List<MCH_AircraftInfo.LandingGear> landingGear = new ArrayList<>();
    public final List<MCH_AircraftInfo.Throttle> partThrottle = new ArrayList<>();
    public final List<MCH_AircraftInfo.RotPart> partRotPart = new ArrayList<>();
    public final List<MCH_AircraftInfo.CrawlerTrack> partCrawlerTrack = new ArrayList<>();
    public final List<MCH_AircraftInfo.TrackRoller> partTrackRoller = new ArrayList<>();
    public final List<MCH_AircraftInfo.PartWheel> partWheel = new ArrayList<>();
    public final List<MCH_AircraftInfo.PartWheel> partSteeringWheel = new ArrayList<>();
    public final List<MCH_AircraftInfo.Hatch> lightHatchList = new ArrayList<>();
    private final List<String> textureNameList = new ArrayList<>();
    public MCH_AircraftInfo.Flare flare = new Flare(this);
    public String displayName;
    public HashMap<String, String> displayNameLang = new HashMap<>();
    public int itemID = 0;
    public List<String> recipeString = new ArrayList<>();
    public boolean isShapedRecipe = true;
    public String category = "zzz";
    public boolean creativeOnly = false;
    public boolean invulnerable = false;
    public boolean isEnableGunnerMode = false;
    public int cameraZoom = this.getDefaultMaxZoom();
    public boolean isEnableConcurrentGunnerMode = false;
    public boolean isEnableNightVision = false;
    public boolean isEnableEntityRadar = false;
    public boolean isEnableEjectionSeat = false;
    public boolean isEnableParachuting = false;
    public float bodyHeight = 0.7F;
    public float bodyWidth = 2.0F;
    public boolean isFloat = false;
    public float floatOffset = 0.0F;
    public float gravity = -0.04F;
    public float gravityInWater = -0.04F;
    public int maxHp = 50;
    public float armorMinDamage = 0.0F;
    public float armorMaxDamage = 100000.0F;
    public float armorDamageFactor = 1.0F;
    public boolean enableBack = false;
    public int inventorySize = 0;
    public boolean isUAV = false;
    public boolean isSmallUAV = false;
    public boolean isNewUAV;
    public boolean isTargetDrone = false;
    public float autoPilotRot = -0.6F;
    public float onGroundPitch = 0.0F;
    public boolean canMoveOnGround = true;
    public boolean canRotOnGround = true;
    public MCH_Hud hudTvMissile = null;
    public float damageFactor = 0.2F;
    public float submergedDamageHeight = 0.0F;
    public boolean regeneration = false;
    public int maxFuel = 0;
    public float fuelConsumption = 1.0F;
    public float fuelSupplyRange = 0.0F;
    public float ammoSupplyRange = 0.0F;
    public float repairOtherVehiclesRange = 0.0F;
    public int repairOtherVehiclesValue = 10;
    public float stealth = 0.0F;
    public boolean canRide = true;
    public float entityWidth = 1.0F;
    public float entityHeight = 1.0F;
    public float entityPitch = 0.0F;
    public float entityRoll = 0.0F;
    public float stepHeight = this.getDefaultStepHeight();
    public int mobSeatNum = 0;
    public int entityRackNum = 0;
    public float rotorSpeed = this.getDefaultRotorSpeed();
    public boolean enableSeaSurfaceParticle = false;
    public float pivotTurnThrottle = 0.0F;
    public float trackRollerRot = 30.0F;
    public float partWheelRot = 30.0F;
    public float onGroundPitchFactor = 0.0F;
    public float onGroundRollFactor = 0.0F;
    public Vec3d turretPosition = new Vec3d(0.0, 0.0, 0.0);
    public boolean defaultFreelook = false;
    public Vec3d unmountPosition = null;
    public float thirdPersonDist = 4.0F;
    public float markerWidth = 2.0F;
    public float markerHeight = 1.0F;
    public float bbZmin = -1.0F;
    public float bbZmax = 1.0F;
    public float bbZ;
    public boolean alwaysCameraView = false;
    public float cameraRotationSpeed = 100.0F;
    public float speed = 0.1F;
    public float motionFactor = 0.96F;
    public float mobilityYaw = 1.0F;
    public float mobilityPitch = 1.0F;
    public float mobilityRoll = 1.0F;
    public float mobilityYawOnGround = 1.0F;
    public float minRotationPitch = this.getMinRotationPitch();
    public float maxRotationPitch = this.getMaxRotationPitch();
    public float minRotationRoll = this.getMinRotationPitch();
    public float maxRotationRoll = this.getMaxRotationPitch();
    public boolean limitRotation = false;
    public float throttleUpDown = 1.0F;
    public float throttleUpDownOnEntity = 2.0F;
    public int textureCount = 0;
    public float particlesScale = 1.0F;
    public boolean hideEntity = false;
    public boolean smoothShading = true;
    public String soundMove = "";
    public float soundRange = this.getDefaultSoundRange();
    public float soundVolume = 1.0F;
    public float soundPitch = 1.0F;
    public _IModelCustom model = null;
    /**
     * Radar type
     */
    public RadarType radarType = RadarType.EARLY_AA;
    /**
     * RWR type
     */
    public RWRType rwrType = RWRType.DIGITAL;
    /**
     * Display name on modern air-to-air radar
     */
    public String nameOnModernAARadar = "";
    /**
     * Display name on early air-to-air radar
     */
    public String nameOnEarlyAARadar = "";
    /**
     * Display name on modern air-to-surface radar
     */
    public String nameOnModernASRadar = "";
    /**
     * Display name on early air-to-surface radar
     */
    public String nameOnEarlyASRadar = "";
    /**
     * Explosion radius when the vehicle is destroyed
     */
    public float explosionSizeByCrash = 5;
    /**
     * Reverse speed multiplier (default 1)
     */
    public float throttleDownFactor = 1;
    /**
     * Duration of chaff effectiveness
     */
    public int chaffUseTime = 100;
    /**
     * Chaff cooldown time
     */
    public int chaffWaitTime = 400;
    /**
     * Duration of repair system (duration = heal percentage)
     */
    public int maintenanceUseTime = 20;
    /**
     * Repair system cooldown time
     */
    public int maintenanceWaitTime = 300;
    /**
     * Vehicle paralysis threshold; engine shuts down below this HP percentage
     */
    public int engineShutdownThreshold = 20;
    /**
     * APS active duration
     */
    public int apsUseTime = 100;
    /**
     * APS cooldown time
     */
    public int apsWaitTime = 400;
    /**
     * APS range
     */
    public int apsRange = 8;
    /**
     * Whether the vehicle has RWR
     */
    public boolean hasRWR = false;
    /**
     * HUD custom field for vehicle HUD type
     */
    public int hudType = 0;
    /**
     * HUD custom field for vehicle weapon group type
     */
    public int weaponGroupType = 0;
    /**
     * Explosion damage multiplier; final damage = base * multiplier
     */
    public float armorExplosionDamageMultiplier = 1.0f;
    /**
     * Whether the vehicle has a photoelectric jammer (immune to laser lock-on)
     */
    public boolean hasPhotoelectricJammer = false;

    /**
     * Scale of preview in TheOneProbe
     */
    public float oneProbeScale = 1F;

    private String lastWeaponType = "";
    private int lastWeaponIndex = -1;
    private MCH_AircraftInfo.PartWeapon lastWeaponPart = null;

    private List<DirectDrawable> hudCache = null;

    public MCH_AircraftInfo(AddonResourceLocation location, String path) {
        super(location, path);
        this.name = location == null ? "IAMTEST" : location.getPath();
        this.displayName = this.name;
        this.textureNameList.add(this.name);
    }

    public static String[] getCannotReloadItem() {
        return new String[] {
                "DisplayName", "AddDisplayName", "ItemID", "AddRecipe", "AddShapelessRecipe", "InventorySize", "Sound",
                "UAV", "SmallUAV", "TargetDrone", "Category"
        };
    }

    public List<DirectDrawable> getHudCache() {
        if (hudCache == null) {
            hudCache = buildHudCache();
        }
        return hudCache;
    }

    private List<DirectDrawable> buildHudCache() {
        var list = new ArrayList<DirectDrawable>();
        if (hasRWR)
            list.add(HudRWR.INSTANCE);

        list.add(HudGPS.INSTANCE);
        list.add(HudMortarRadar.INSTANCE);
        list.add(HudBVRLock.INSTANCE);
        return list;
    }

    public ItemStack getItemStack() {
        return new ItemStack(this.getItem());
    }

    public abstract String getDirectoryName();

    public abstract String getKindName();

    public float getDefaultSoundRange() {
        return 100.0F;
    }

    public List<MCH_AircraftInfo.Wheel> getDefaultWheelList() {
        return new ArrayList<>();
    }

    public float getDefaultRotorSpeed() {
        return 0.0F;
    }

    private float getDefaultStepHeight() {
        return 0.0F;
    }

    public boolean haveRepellingHook() {
        return !this.repellingHooks.isEmpty();
    }

    public boolean haveFlare() {
        return this.flare.types.length > 0;
    }

    public boolean haveCanopy() {
        return !this.canopyList.isEmpty();
    }

    public boolean haveLandingGear() {
        return !this.landingGear.isEmpty();
    }

    public abstract String getDefaultHudName(int var1);

    @Override
    public boolean validate() throws Exception {
        if (this.cameraPosition.isEmpty()) {
            this.cameraPosition.add(new CameraPosition(this));
        }

        this.bbZ = (this.bbZmax + this.bbZmin) / 2.0F;
        if (this.isTargetDrone) {
            this.isUAV = true;
        }

        if (this.isEnableParachuting && !this.repellingHooks.isEmpty()) {
            this.isEnableParachuting = false;
            this.repellingHooks.clear();
        }

        if (this.isUAV) {
            this.alwaysCameraView = true;
            if (this.seatList.isEmpty()) {
                MCH_SeatInfo s = new MCH_SeatInfo(new Vec3d(0.0, 0.0, 0.0), false);
                this.seatList.add(s);
            }
        }

        this.mobSeatNum = this.seatList.size();
        this.entityRackNum = this.entityRackList.size();
        if (this.getNumSeat() < 1) {
            throw new Exception("At least one seat must be set.");
        } else {
            if (this.getNumHud() < this.getNumSeat()) {
                for (int i = this.getNumHud(); i < this.getNumSeat(); i++) {
                    this.hudList.add(MCH_HudManager.get(this.getDefaultHudName(i)));
                }
            }

            if (this.getNumSeat() == 1 && this.getNumHud() == 1) {
                this.hudList.add(MCH_HudManager.get(this.getDefaultHudName(1)));
            }

            this.seatList.addAll(this.entityRackList);

            this.entityRackList.clear();
            if (this.hudTvMissile == null) {
                this.hudTvMissile = MCH_HudManager.get("tv_missile");
            }

            if (this.textureNameList.isEmpty()) {
                throw new Exception("At least one texture must be set.");
            } else {
                if (this.itemID <= 0) {}

                for (int i = 0; i < this.partWeaponBay.size(); i++) {
                    MCH_AircraftInfo.WeaponBay wb = this.partWeaponBay.get(i);
                    String[] weaponNames = wb.weaponName.split("\\s*/\\s*");
                    if (weaponNames.length == 0) {
                        this.partWeaponBay.remove(i);
                    } else {
                        List<Integer> list = new ArrayList<>();

                        for (String s : weaponNames) {
                            int id = this.getWeaponIdByName(s);
                            if (id >= 0) {
                                list.add(id);
                            }
                        }

                        if (list.isEmpty()) {
                            this.partWeaponBay.remove(i);
                        } else {
                            this.partWeaponBay.get(i).weaponIds = list.toArray(new Integer[0]);
                        }
                    }
                }

                return true;
            }
        }
    }

    public int getInfo_MaxSeatNum() {
        return 30;
    }

    public int getNumSeatAndRack() {
        return this.seatList.size();
    }

    public int getNumSeat() {
        return this.mobSeatNum;
    }

    public int getNumRack() {
        return this.entityRackNum;
    }

    public int getNumHud() {
        return this.hudList.size();
    }

    public float getMaxSpeed() {
        return 0.8F;
    }

    public float getMinRotationPitch() {
        return -89.9F;
    }

    public float getMaxRotationPitch() {
        return 80.0F;
    }

    public float getMinRotationRoll() {
        return -80.0F;
    }

    public float getMaxRotationRoll() {
        return 80.0F;
    }

    public int getDefaultMaxZoom() {
        return 1;
    }

    public boolean haveHatch() {
        return !this.hatchList.isEmpty();
    }

    public boolean havePartCamera() {
        return !this.cameraList.isEmpty();
    }

    public boolean havePartThrottle() {
        return !this.partThrottle.isEmpty();
    }

    public MCH_AircraftInfo.WeaponSet getWeaponSetById(int id) {
        return id >= 0 && id < this.weaponSetList.size() ? this.weaponSetList.get(id) : null;
    }

    public MCH_AircraftInfo.Weapon getWeaponById(int id) {
        MCH_AircraftInfo.WeaponSet ws = this.getWeaponSetById(id);
        return ws != null ? ws.weapons.get(0) : null;
    }

    public int getWeaponIdByName(String s) {
        for (int i = 0; i < this.weaponSetList.size(); i++) {
            if (this.weaponSetList.get(i).type.equalsIgnoreCase(s)) {
                return i;
            }
        }

        return -1;
    }

    public MCH_AircraftInfo.Weapon getWeaponByName(String s) {
        for (int i = 0; i < this.weaponSetList.size(); i++) {
            if (this.weaponSetList.get(i).type.equalsIgnoreCase(s)) {
                return this.getWeaponById(i);
            }
        }

        return null;
    }

    public void addTextureName(String name) {
        this.textureNameList.add(name);
    }

    public WeaponSet getOrCreateWeaponSet(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (type.compareTo(this.lastWeaponType) != 0 || this.lastWeaponIndex < 0) {
            this.weaponSetList.add(new WeaponSet(this, type));
            this.lastWeaponIndex = this.weaponSetList.size() - 1;
            this.lastWeaponType = type;
        }
        return this.weaponSetList.get(this.lastWeaponIndex);
    }

    public PartWeapon getLastWeaponPart() {
        return this.lastWeaponPart;
    }

    public void setLastWeaponPart(PartWeapon part) {
        this.lastWeaponPart = part;
    }

    public int getWeaponCount() {
        return this.weaponSetList.size();
    }

    public MCH_AircraftInfo.CrawlerTrack createCrawlerTrack(String data, String name) {
        String[] s = this.splitParam(data);
        int PC = s.length - 3;
        boolean REV = this.toBool(s[0]);
        float LEN = this.toFloat(s[1], 0.001F, 1000.0F) * 0.9F;
        float Z = this.toFloat(s[2]);
        if (PC < 4) {
            return null;
        } else {
            double[] cx = new double[PC];
            double[] cy = new double[PC];

            for (int i = 0; i < PC; i++) {
                int idx = !REV ? i : PC - i - 1;
                String[] xy = this.splitParamSlash(s[3 + idx]);
                cx[i] = this.toFloat(xy[0]);
                cy[i] = this.toFloat(xy[1]);
            }

            List<MCH_AircraftInfo.CrawlerTrackPrm> lp = new ArrayList<>();
            lp.add(new CrawlerTrackPrm(this, (float) cx[0], (float) cy[0]));
            double dist = 0.0;

            for (int i = 0; i < PC; i++) {
                double x = cx[(i + 1) % PC] - cx[i];
                double y = cy[(i + 1) % PC] - cy[i];
                dist += Math.sqrt(x * x + y * y);
                double dist2 = dist;

                for (int j = 1; dist >= LEN; j++) {
                    lp.add(new CrawlerTrackPrm(this, (float) (cx[i] + x * (LEN * j / dist2)),
                            (float) (cy[i] + y * (LEN * j / dist2))));
                    dist -= LEN;
                }
            }

            for (int i = 0; i < lp.size(); i++) {
                MCH_AircraftInfo.CrawlerTrackPrm pp = lp.get((i + lp.size() - 1) % lp.size());
                MCH_AircraftInfo.CrawlerTrackPrm cp = lp.get(i);
                MCH_AircraftInfo.CrawlerTrackPrm np = lp.get((i + 1) % lp.size());
                float pr = (float) (Math.atan2(pp.x - cp.x, pp.y - cp.y) * 180.0 / Math.PI);
                float nr = (float) (Math.atan2(np.x - cp.x, np.y - cp.y) * 180.0 / Math.PI);
                float ppr = (pr + 360.0F) % 360.0F;
                float nnr = nr + 180.0F;
                if ((nnr < ppr - 0.3 || nnr > ppr + 0.3) && nnr - ppr < 100.0F && nnr - ppr > -100.0F) {
                    nnr = (nnr + ppr) / 2.0F;
                }

                cp.r = nnr;
            }

            MCH_AircraftInfo.CrawlerTrack c = new CrawlerTrack(this, name);
            c.len = LEN;
            c.cx = cx;
            c.cy = cy;
            c.lp = lp;
            c.z = Z;
            c.side = Z >= 0.0F ? 1 : 0;
            return c;
        }
    }

    public String getTextureName() {
        String s = this.textureNameList.get(this.textureCount);
        this.textureCount = (this.textureCount + 1) % this.textureNameList.size();
        return s;
    }

    public String getNextTextureName(String base) {
        if (this.textureNameList.size() >= 2) {
            for (int i = 0; i < this.textureNameList.size(); i++) {
                String s = this.textureNameList.get(i);
                if (s.equalsIgnoreCase(base)) {
                    i = (i + 1) % this.textureNameList.size();
                    return this.textureNameList.get(i);
                }
            }
        }

        return base;
    }

    @Override
    public boolean canReloadItem(String item) {
        String[] ignoreItems = getCannotReloadItem();

        for (String s : ignoreItems) {
            if (s.equalsIgnoreCase(item)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "MCH_AircraftInfo{" +
                "name='" + name + '\'' +
                ", recipe=" + recipe +
                ", weaponSetList=" + weaponSetList +
                ", seatList=" + seatList +
                ", exclusionSeatList=" + exclusionSeatList +
                ", hudList=" + hudList +
                ", extraBoundingBox=" + extraBoundingBox +
                ", wheels=" + wheels +
                ", entityRackList=" + entityRackList +
                ", mobDropOption=" + mobDropOption +
                ", repellingHooks=" + repellingHooks +
                ", rideRacks=" + rideRacks +
                ", particleSplashs=" + particleSplashs +
                ", searchLights=" + searchLights +
                ", cameraPosition=" + cameraPosition +
                ", hatchList=" + hatchList +
                ", cameraList=" + cameraList +
                ", partWeapon=" + partWeapon +
                ", partWeaponBay=" + partWeaponBay +
                ", canopyList=" + canopyList +
                ", landingGear=" + landingGear +
                ", partThrottle=" + partThrottle +
                ", partRotPart=" + partRotPart +
                ", partCrawlerTrack=" + partCrawlerTrack +
                ", partTrackRoller=" + partTrackRoller +
                ", partWheel=" + partWheel +
                ", partSteeringWheel=" + partSteeringWheel +
                ", lightHatchList=" + lightHatchList +
                ", textureNameList=" + textureNameList +
                ", flare=" + flare +
                ", displayName='" + displayName + '\'' +
                ", displayNameLang=" + displayNameLang +
                ", itemID=" + itemID +
                ", recipeString=" + recipeString +
                ", isShapedRecipe=" + isShapedRecipe +
                ", category='" + category + '\'' +
                ", creativeOnly=" + creativeOnly +
                ", invulnerable=" + invulnerable +
                ", isEnableGunnerMode=" + isEnableGunnerMode +
                ", cameraZoom=" + cameraZoom +
                ", isEnableConcurrentGunnerMode=" + isEnableConcurrentGunnerMode +
                ", isEnableNightVision=" + isEnableNightVision +
                ", isEnableEntityRadar=" + isEnableEntityRadar +
                ", isEnableEjectionSeat=" + isEnableEjectionSeat +
                ", isEnableParachuting=" + isEnableParachuting +
                ", bodyHeight=" + bodyHeight +
                ", bodyWidth=" + bodyWidth +
                ", isFloat=" + isFloat +
                ", floatOffset=" + floatOffset +
                ", gravity=" + gravity +
                ", gravityInWater=" + gravityInWater +
                ", maxHp=" + maxHp +
                ", armorMinDamage=" + armorMinDamage +
                ", armorMaxDamage=" + armorMaxDamage +
                ", armorDamageFactor=" + armorDamageFactor +
                ", enableBack=" + enableBack +
                ", inventorySize=" + inventorySize +
                ", isUAV=" + isUAV +
                ", isSmallUAV=" + isSmallUAV +
                ", isNewUAV=" + isNewUAV +
                ", isTargetDrone=" + isTargetDrone +
                ", autoPilotRot=" + autoPilotRot +
                ", onGroundPitch=" + onGroundPitch +
                ", canMoveOnGround=" + canMoveOnGround +
                ", canRotOnGround=" + canRotOnGround +
                ", hudTvMissile=" + hudTvMissile +
                ", damageFactor=" + damageFactor +
                ", submergedDamageHeight=" + submergedDamageHeight +
                ", regeneration=" + regeneration +
                ", maxFuel=" + maxFuel +
                ", fuelConsumption=" + fuelConsumption +
                ", fuelSupplyRange=" + fuelSupplyRange +
                ", ammoSupplyRange=" + ammoSupplyRange +
                ", repairOtherVehiclesRange=" + repairOtherVehiclesRange +
                ", repairOtherVehiclesValue=" + repairOtherVehiclesValue +
                ", stealth=" + stealth +
                ", canRide=" + canRide +
                ", entityWidth=" + entityWidth +
                ", entityHeight=" + entityHeight +
                ", entityPitch=" + entityPitch +
                ", entityRoll=" + entityRoll +
                ", stepHeight=" + stepHeight +
                ", mobSeatNum=" + mobSeatNum +
                ", entityRackNum=" + entityRackNum +
                ", rotorSpeed=" + rotorSpeed +
                ", enableSeaSurfaceParticle=" + enableSeaSurfaceParticle +
                ", pivotTurnThrottle=" + pivotTurnThrottle +
                ", trackRollerRot=" + trackRollerRot +
                ", partWheelRot=" + partWheelRot +
                ", onGroundPitchFactor=" + onGroundPitchFactor +
                ", onGroundRollFactor=" + onGroundRollFactor +
                ", turretPosition=" + turretPosition +
                ", defaultFreelook=" + defaultFreelook +
                ", unmountPosition=" + unmountPosition +
                ", thirdPersonDist=" + thirdPersonDist +
                ", markerWidth=" + markerWidth +
                ", markerHeight=" + markerHeight +
                ", bbZmin=" + bbZmin +
                ", bbZmax=" + bbZmax +
                ", bbZ=" + bbZ +
                ", alwaysCameraView=" + alwaysCameraView +
                ", cameraRotationSpeed=" + cameraRotationSpeed +
                ", speed=" + speed +
                ", motionFactor=" + motionFactor +
                ", mobilityYaw=" + mobilityYaw +
                ", mobilityPitch=" + mobilityPitch +
                ", mobilityRoll=" + mobilityRoll +
                ", mobilityYawOnGround=" + mobilityYawOnGround +
                ", minRotationPitch=" + minRotationPitch +
                ", maxRotationPitch=" + maxRotationPitch +
                ", minRotationRoll=" + minRotationRoll +
                ", maxRotationRoll=" + maxRotationRoll +
                ", limitRotation=" + limitRotation +
                ", throttleUpDown=" + throttleUpDown +
                ", throttleUpDownOnEntity=" + throttleUpDownOnEntity +
                ", textureCount=" + textureCount +
                ", particlesScale=" + particlesScale +
                ", hideEntity=" + hideEntity +
                ", smoothShading=" + smoothShading +
                ", soundMove='" + soundMove + '\'' +
                ", soundRange=" + soundRange +
                ", soundVolume=" + soundVolume +
                ", soundPitch=" + soundPitch +
                ", model=" + model +
                ", nameOnModernAARadar='" + nameOnModernAARadar + '\'' +
                ", nameOnEarlyAARadar='" + nameOnEarlyAARadar + '\'' +
                ", nameOnModernASRadar='" + nameOnModernASRadar + '\'' +
                ", nameOnEarlyASRadar='" + nameOnEarlyASRadar + '\'' +
                ", explosionSizeByCrash=" + explosionSizeByCrash +
                ", throttleDownFactor=" + throttleDownFactor +
                ", chaffUseTime=" + chaffUseTime +
                ", chaffWaitTime=" + chaffWaitTime +
                ", maintenanceUseTime=" + maintenanceUseTime +
                ", maintenanceWaitTime=" + maintenanceWaitTime +
                ", engineShutdownThreshold=" + engineShutdownThreshold +
                ", apsUseTime=" + apsUseTime +
                ", apsWaitTime=" + apsWaitTime +
                ", apsRange=" + apsRange +
                ", oneProbeScale=" + oneProbeScale +
                ", lastWeaponType='" + lastWeaponType + '\'' +
                ", lastWeaponIndex=" + lastWeaponIndex +
                ", lastWeaponPart=" + lastWeaponPart +
                '}';
    }

    @Override
    public void onPostReload() {
        hudCache = null;
    }

    // mlbv: @Builder can't handle inheritance
    @SuperBuilder(toBuilder = true)
    public static class Camera extends MCH_AircraftInfo.DrawnPart {

        public final boolean yawSync;
        public final boolean pitchSync;

        public Camera(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, float rx, float ry,
                      float rz, String name, boolean ys, boolean ps) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.yawSync = ys;
            this.pitchSync = ps;
        }

        public Camera(DrawnPart base, boolean yawSync, boolean pitchSync) {
            super(base);
            this.yawSync = yawSync;
            this.pitchSync = pitchSync;
        }

        @Override
        public String toString() {
            return "Camera{" +
                    "pos=" + pos +
                    ", yawSync=" + yawSync +
                    ", pitchSync=" + pitchSync +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class CameraPosition {

        public final Vec3d pos;
        public final boolean fixRot;
        public final float yaw;
        public final float pitch;

        public CameraPosition(MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d vec3, boolean fixRot, float yaw,
                              float pitch) {
            this.pos = vec3.add(0, W_Entity.GLOBAL_Y_OFFSET, 0); // FIXME Seems like basically everthing needs a 0.35
                                                                 // offset when it comes to mcheli rendering, this is
                                                                 // temporary so for when I figure out where it belongs
            this.fixRot = fixRot;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public CameraPosition(MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d vec3) {
            this(paramMCH_AircraftInfo, vec3, false, 0.0F, 0.0F);
        }

        public CameraPosition(MCH_AircraftInfo paramMCH_AircraftInfo) {
            this(paramMCH_AircraftInfo, new Vec3d(0.0, 0.0, 0.0));
        }

        public CameraPosition(Vec3d pos, boolean fixRot, float yaw, float pitch) {
            this.pos = pos.add(0, W_Entity.GLOBAL_Y_OFFSET, 0);
            this.fixRot = fixRot;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public String toString() {
            return "CameraPosition{" +
                    "pos=" + pos +
                    ", fixRot=" + fixRot +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    '}';
        }
    }

    public static class Canopy extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final boolean isSlide;

        public Canopy(DrawnPart base, float maxRotationFactor, boolean isSlide) {
            super(base);
            this.maxRotFactor = maxRotationFactor;
            this.isSlide = isSlide;
        }

        public Canopy(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, float rx, float ry,
                      float rz, float mr, String name, boolean slide) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.maxRotFactor = mr;
            this.isSlide = slide;
        }

        @Override
        public String toString() {
            return "Canopy{" +
                    "maxRotFactor=" + maxRotFactor +
                    ", isSlide=" + isSlide +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class CrawlerTrack extends MCH_AircraftInfo.DrawnPart {

        public float len = W_Entity.GLOBAL_Y_OFFSET;
        public double[] cx;
        public double[] cy;
        public List<MCH_AircraftInfo.CrawlerTrackPrm> lp;
        public float z;
        public int side;

        public CrawlerTrack(MCH_AircraftInfo paramMCH_AircraftInfo, String name) {
            super(paramMCH_AircraftInfo, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, name);
        }

        public CrawlerTrack(String name) {
            super(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, name);
        }

        @Override
        public String toString() {
            return "CrawlerTrack{" +
                    "len=" + len +
                    ", cx=" + Arrays.toString(cx) +
                    ", cy=" + Arrays.toString(cy) +
                    ", lp=" + lp +
                    ", z=" + z +
                    ", side=" + side +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class CrawlerTrackPrm {

        public final float x;
        public final float y;
        public float nx;
        public float ny;
        public float r;

        public CrawlerTrackPrm(MCH_AircraftInfo paramMCH_AircraftInfo, float x, float y) {
            this.x = x;
            this.y = y;
        }

        public CrawlerTrackPrm(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "CrawlerTrackPrm{" +
                    "x=" + x +
                    ", y=" + y +
                    ", nx=" + nx +
                    ", ny=" + ny +
                    ", r=" + r +
                    '}';
        }
    }

    @SuperBuilder(toBuilder = true)
    public static class DrawnPart {

        public final Vec3d pos;
        public final Vec3d rot;
        public final String modelName;
        public _IModelCustom model;

        public DrawnPart(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, float rx, float ry,
                         float rz, String name) {
            this.pos = new Vec3d(px, py, pz);
            this.rot = new Vec3d(rx, ry, rz);
            this.modelName = name;
            this.model = null;
        }

        public DrawnPart(Vec3d pos, Vec3d rot, String modelName) {
            this.pos = pos;
            this.rot = rot;
            this.modelName = modelName;
        }

        public DrawnPart(DrawnPart other) {
            this.pos = other.pos;
            this.rot = other.rot;
            this.modelName = other.modelName;
            this.model = other.model;
        }

        @Override
        public String toString() {
            return "DrawnPart{" +
                    "pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    @AllArgsConstructor
    public static class Flare {

        public Vec3d pos = new Vec3d(0.0, 0.0, 0.0);
        public int[] types = new int[0];

        public Flare(MCH_AircraftInfo paramMCH_AircraftInfo) {}

        @Override
        public String toString() {
            return "Flare{" +
                    "pos=" + pos +
                    ", types=" + Arrays.toString(types) +
                    '}';
        }
    }

    public static class Hatch extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final float maxRot;
        public final boolean isSlide;

        public Hatch(Vec3d pos, Vec3d rot, String modelName, float maxRot, boolean isSlide) {
            super(pos, rot, modelName);
            this.maxRot = maxRot;
            this.maxRotFactor = this.maxRot / 90.0F;
            this.isSlide = isSlide;
        }

        public Hatch(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, float rx, float ry, float rz,
                     float mr, String name, boolean slide) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.maxRot = mr;
            this.maxRotFactor = this.maxRot / 90.0F;
            this.isSlide = slide;
        }

        public Hatch(DrawnPart base, float maxRot, boolean isSlide) {
            super(base);
            this.maxRot = maxRot;
            this.maxRotFactor = maxRot / 90.0F;
            this.isSlide = isSlide;
        }

        @Override
        public String toString() {
            return "Hatch{" +
                    "maxRotFactor=" + maxRotFactor +
                    ", maxRot=" + maxRot +
                    ", isSlide=" + isSlide +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class LandingGear extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final boolean reverse;
        public final boolean hatch;
        public Vec3d slide = null;
        public boolean enableRot2;
        public Vec3d rot2;
        public float maxRotFactor2;

        public LandingGear(DrawnPart other, float maxRotFactor, boolean reverse, boolean hatch) {
            super(other);
            this.maxRotFactor = maxRotFactor;
            this.reverse = reverse;
            this.hatch = hatch;
        }

        public LandingGear(
                           MCH_AircraftInfo paramMCH_AircraftInfo,
                           float x,
                           float y,
                           float z,
                           float rx,
                           float ry,
                           float rz,
                           String model,
                           float maxRotF,
                           boolean rev,
                           boolean isHatch) {
            super(paramMCH_AircraftInfo, x, y, z, rx, ry, rz, model);
            this.maxRotFactor = maxRotF;
            this.enableRot2 = false;
            this.rot2 = new Vec3d(0.0, 0.0, 0.0);
            this.maxRotFactor2 = 0.0F;
            this.reverse = rev;
            this.hatch = isHatch;
        }

        @Override
        public String toString() {
            return "LandingGear{" +
                    "maxRotFactor=" + maxRotFactor +
                    ", reverse=" + reverse +
                    ", hatch=" + hatch +
                    ", slide=" + slide +
                    ", enableRot2=" + enableRot2 +
                    ", rot2=" + rot2 +
                    ", maxRotFactor2=" + maxRotFactor2 +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class PartWeapon extends MCH_AircraftInfo.DrawnPart {

        public final String[] name;
        public final boolean rotBarrel;
        public final boolean isMissile;
        public final boolean hideGM;
        public final boolean yaw;
        public final boolean pitch;
        public final float recoilBuf;
        public final boolean turret;
        public final List<MCH_AircraftInfo.PartWeaponChild> child;

        public PartWeapon(
                          MCH_AircraftInfo paramMCH_AircraftInfo,
                          String[] name,
                          boolean rotBrl,
                          boolean missile,
                          boolean hgm,
                          boolean y,
                          boolean p,
                          float px,
                          float py,
                          float pz,
                          String modelName,
                          float rx,
                          float ry,
                          float rz,
                          float rb,
                          boolean turret) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, modelName);
            this.name = name;
            this.rotBarrel = rotBrl;
            this.isMissile = missile;
            this.hideGM = hgm;
            this.yaw = y;
            this.pitch = p;
            this.recoilBuf = rb;
            this.child = new ArrayList<>();
            this.turret = turret;
        }

        public PartWeapon(DrawnPart other, String[] name, boolean rotBarrel, boolean isMissile, boolean hideGM,
                          boolean yaw, boolean pitch, float recoilBuf, boolean turret) {
            super(other);
            this.name = name;
            this.rotBarrel = rotBarrel;
            this.isMissile = isMissile;
            this.hideGM = hideGM;
            this.yaw = yaw;
            this.pitch = pitch;
            this.recoilBuf = recoilBuf;
            this.turret = turret;
            this.child = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "PartWeapon{" +
                    "name=" + Arrays.toString(name) +
                    ", rotBarrel=" + rotBarrel +
                    ", isMissile=" + isMissile +
                    ", hideGM=" + hideGM +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    ", recoilBuf=" + recoilBuf +
                    ", turret=" + turret +
                    ", child=" + child +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class PartWeaponChild extends MCH_AircraftInfo.DrawnPart {

        public final String[] name;
        public final boolean yaw;
        public final boolean pitch;
        public final float recoilBuf;

        public PartWeaponChild(
                               MCH_AircraftInfo paramMCH_AircraftInfo,
                               String[] name,
                               boolean y,
                               boolean p,
                               float px,
                               float py,
                               float pz,
                               String modelName,
                               float rx,
                               float ry,
                               float rz,
                               float rb) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, modelName);
            this.name = name;
            this.yaw = y;
            this.pitch = p;
            this.recoilBuf = rb;
        }

        public PartWeaponChild(Vec3d pos, Vec3d rot, String modelName, String[] name, boolean yaw, boolean pitch,
                               float recoilBuf) {
            super(pos, rot, modelName);
            this.name = name;
            this.yaw = yaw;
            this.pitch = pitch;
            this.recoilBuf = recoilBuf;
        }

        @Override
        public String toString() {
            return "PartWeaponChild{" +
                    "name=" + Arrays.toString(name) +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    ", recoilBuf=" + recoilBuf +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class PartWheel extends MCH_AircraftInfo.DrawnPart {

        public final float rotDir;
        public final Vec3d pos2;

        public PartWheel(DrawnPart other, float rotDir, Vec3d pos2) {
            super(other);
            this.rotDir = rotDir;
            this.pos2 = pos2;
        }

        public PartWheel(
                         MCH_AircraftInfo paramMCH_AircraftInfo,
                         float px,
                         float py,
                         float pz,
                         float rx,
                         float ry,
                         float rz,
                         float rd,
                         float px2,
                         float py2,
                         float pz2,
                         String name) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.rotDir = rd;
            this.pos2 = new Vec3d(px2, py2, pz2);
        }

        public PartWheel(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, float rx, float ry,
                         float rz, float rd, String name) {
            this(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, rd, px, py, pz, name);
        }

        @Override
        public String toString() {
            return "PartWheel{" +
                    "rotDir=" + rotDir +
                    ", pos2=" + pos2 +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    @AllArgsConstructor
    public static class ParticleSplash {

        public final int num;
        public final float acceleration;
        public final float size;
        public final Vec3d pos;
        public final int age;
        public final float motionY;
        public final float gravity;

        public ParticleSplash(MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d v, int nm, float siz, float acc, int ag,
                              float my, float gr) {
            this.num = nm;
            this.pos = v;
            this.size = siz;
            this.acceleration = acc;
            this.age = ag;
            this.motionY = my;
            this.gravity = gr;
        }

        @Override
        public String toString() {
            return "ParticleSplash{" +
                    "num=" + num +
                    ", acceleration=" + acceleration +
                    ", size=" + size +
                    ", pos=" + pos +
                    ", age=" + age +
                    ", motionY=" + motionY +
                    ", gravity=" + gravity +
                    '}';
        }
    }

    @AllArgsConstructor
    public static class RepellingHook {

        public final Vec3d pos;
        public final int interval;

        public RepellingHook(MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d pos, int inv) {
            this.pos = pos;
            this.interval = inv;
        }

        @Override
        public String toString() {
            return "RepellingHook{" +
                    "pos=" + pos +
                    ", interval=" + interval +
                    '}';
        }
    }

    @AllArgsConstructor
    public static class RideRack {

        public final String name;
        public final int rackID;

        public RideRack(MCH_AircraftInfo paramMCH_AircraftInfo, String n, int id) {
            this.name = n;
            this.rackID = id;
        }

        @Override
        public String toString() {
            return "RideRack{" +
                    "name='" + name + '\'' +
                    ", rackID=" + rackID +
                    '}';
        }
    }

    public static class RotPart extends MCH_AircraftInfo.DrawnPart {

        public final float rotSpeed;
        public final boolean rotAlways;

        public RotPart(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, float rx, float ry,
                       float rz, float mr, boolean a, String name) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.rotSpeed = mr;
            this.rotAlways = a;
        }

        public RotPart(DrawnPart other, float rotSpeed, boolean rotAlways) {
            super(other);
            this.rotSpeed = rotSpeed;
            this.rotAlways = rotAlways;
        }

        @Override
        public String toString() {
            return "RotPart{" +
                    "rotSpeed=" + rotSpeed +
                    ", rotAlways=" + rotAlways +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class SearchLight {

        public final Vec3d pos;
        public final int colorStart;
        public final int colorEnd;
        public final float height;
        public final float width;
        public final float angle;
        public final boolean fixDir;
        public final float yaw;
        public final float pitch;
        public final boolean steering;
        public final float stRot;

        public SearchLight(
                           Vec3d pos, int cs, int ce, float h, float w, boolean fix, float y, float p, boolean st,
                           float stRot) {
            this.colorStart = cs;
            this.colorEnd = ce;
            this.pos = pos;
            this.height = h;
            this.width = w;
            this.angle = (float) (Math.atan2(w / 2.0F, h) * 180.0 / Math.PI);
            this.fixDir = fix;
            this.steering = st;
            this.yaw = y;
            this.pitch = p;
            this.stRot = stRot;
        }

        public SearchLight(
                           MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d pos, int cs, int ce, float h, float w,
                           boolean fix, float y, float p, boolean st, float stRot) {
            this.colorStart = cs;
            this.colorEnd = ce;
            this.pos = pos;
            this.height = h;
            this.width = w;
            this.angle = (float) (Math.atan2(w / 2.0F, h) * 180.0 / Math.PI);
            this.fixDir = fix;
            this.steering = st;
            this.yaw = y;
            this.pitch = p;
            this.stRot = stRot;
        }

        @Override
        public String toString() {
            return "SearchLight{" +
                    "pos=" + pos +
                    ", colorStart=" + colorStart +
                    ", colorEnd=" + colorEnd +
                    ", height=" + height +
                    ", width=" + width +
                    ", angle=" + angle +
                    ", fixDir=" + fixDir +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    ", steering=" + steering +
                    ", stRot=" + stRot +
                    '}';
        }
    }

    public static class Throttle extends MCH_AircraftInfo.DrawnPart {

        public final Vec3d slide;
        public final float rot2;

        public Throttle(
                        MCH_AircraftInfo paramMCH_AircraftInfo,
                        float px,
                        float py,
                        float pz,
                        float rx,
                        float ry,
                        float rz,
                        float rot,
                        String name,
                        float px2,
                        float py2,
                        float pz2) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.rot2 = rot;
            this.slide = new Vec3d(px2, py2, pz2);
        }

        public Throttle(DrawnPart other, Vec3d slide, float rot2) {
            super(other);
            this.slide = slide;
            this.rot2 = rot2;
        }

        @Override
        public String toString() {
            return "Throttle{" +
                    "slide=" + slide +
                    ", rot2=" + rot2 +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class TrackRoller extends MCH_AircraftInfo.DrawnPart {

        final int side;

        public TrackRoller(MCH_AircraftInfo paramMCH_AircraftInfo, float px, float py, float pz, String name) {
            super(paramMCH_AircraftInfo, px, py, pz, 0.0F, 0.0F, 0.0F, name);
            this.side = px >= 0.0F ? 1 : 0;
        }

        public TrackRoller(DrawnPart other) {
            super(other);
            this.side = other.pos.x >= 0 ? 1 : 0;
        }

        @Override
        public String toString() {
            return "TrackRoller{" +
                    "side=" + side +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class Weapon {

        public final Vec3d pos;
        public final float yaw;
        public final float pitch;
        public final boolean canUsePilot;
        public final int seatID;
        public final float defaultYaw;
        public final float minYaw;
        public final float maxYaw;
        public final float minPitch;
        public final float maxPitch;
        public final boolean turret;

        public Weapon(
                      MCH_AircraftInfo paramMCH_AircraftInfo,
                      float x,
                      float y,
                      float z,
                      float yaw,
                      float pitch,
                      boolean canPirot,
                      int seatId,
                      float defy,
                      float mny,
                      float mxy,
                      float mnp,
                      float mxp,
                      boolean turret) {
            this.pos = new Vec3d(x, y + W_Entity.GLOBAL_Y_OFFSET, z);
            this.yaw = yaw;
            this.pitch = pitch;
            this.canUsePilot = canPirot;
            this.seatID = seatId;
            this.defaultYaw = defy;
            this.minYaw = mny;
            this.maxYaw = mxy;
            this.minPitch = mnp;
            this.maxPitch = mxp;
            this.turret = turret;
        }

        public Weapon(
                      float x,
                      float y,
                      float z,
                      float yaw,
                      float pitch,
                      boolean canPirot,
                      int seatId,
                      float defy,
                      float mny,
                      float mxy,
                      float mnp,
                      float mxp,
                      boolean turret) {
            this.pos = new Vec3d(x, y + W_Entity.GLOBAL_Y_OFFSET, z);
            this.yaw = yaw;
            this.pitch = pitch;
            this.canUsePilot = canPirot;
            this.seatID = seatId;
            this.defaultYaw = defy;
            this.minYaw = mny;
            this.maxYaw = mxy;
            this.minPitch = mnp;
            this.maxPitch = mxp;
            this.turret = turret;
        }

        @Override
        public String toString() {
            return "Weapon{" +
                    "pos=" + pos +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    ", canUsePilot=" + canUsePilot +
                    ", seatID=" + seatID +
                    ", defaultYaw=" + defaultYaw +
                    ", minYaw=" + minYaw +
                    ", maxYaw=" + maxYaw +
                    ", minPitch=" + minPitch +
                    ", maxPitch=" + maxPitch +
                    ", turret=" + turret +
                    '}';
        }
    }

    public static class WeaponBay extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final boolean isSlide;

        public final String weaponName;

        public Integer[] weaponIds;// TODO, get rid of that shit and just use hashmaps once the fucking old parser is
                                   // dead

        public WeaponBay(
                         MCH_AircraftInfo paramMCH_AircraftInfo, String wn, float px, float py, float pz, float rx,
                         float ry, float rz, float mr, String name, boolean slide) {
            super(paramMCH_AircraftInfo, px, py, pz, rx, ry, rz, name);
            this.maxRotFactor = mr;
            this.isSlide = slide;
            this.weaponName = wn;
            this.weaponIds = new Integer[0];
        }

        public WeaponBay(DrawnPart other, float maxRotFactor, boolean isSlide, String weaponName) {
            super(other);
            this.maxRotFactor = maxRotFactor;
            this.isSlide = isSlide;
            this.weaponName = weaponName;
            this.weaponIds = new Integer[0];
        }

        @Override
        public String toString() {
            return "WeaponBay{" +
                    "maxRotFactor=" + maxRotFactor +
                    ", isSlide=" + isSlide +
                    ", weaponName='" + weaponName + '\'' +
                    ", weaponIds=" + Arrays.toString(weaponIds) +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }

    public static class WeaponSet {

        public final String type;
        public final ArrayList<MCH_AircraftInfo.Weapon> weapons;

        public WeaponSet(String t) {
            this.type = t;
            this.weapons = new ArrayList<>();
        }

        public WeaponSet(MCH_AircraftInfo paramMCH_AircraftInfo, String t) {
            this.type = t;
            this.weapons = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "WeaponSet{" +
                    "type='" + type + '\'' +
                    ", weapons=" + weapons +
                    '}';
        }
    }

    @AllArgsConstructor
    public static class Wheel {

        public final Vec3d pos;
        public final float size;

        public Wheel(MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d v, float sz) {
            this.pos = v;
            this.size = sz;
        }

        public Wheel(MCH_AircraftInfo paramMCH_AircraftInfo, Vec3d v) {
            this(paramMCH_AircraftInfo, v, 1.0F);
        }

        @Override
        public String toString() {
            return "Wheel{" +
                    "pos=" + pos +
                    ", size=" + size +
                    '}';
        }
    }
}
