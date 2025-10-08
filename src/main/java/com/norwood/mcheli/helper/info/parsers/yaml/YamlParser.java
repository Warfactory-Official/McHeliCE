package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_BoundingBox;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentParsers;
import com.norwood.mcheli.helper.info.parsers.IParser;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.plane.MCP_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static com.norwood.mcheli.aircraft.MCH_AircraftInfo.*;

@SuppressWarnings({"unchecked", "unboxing"})
public class YamlParser implements IParser {

    public static final Yaml YAML_INSTANCE = new Yaml();
    public static final YamlParser INSTANCE = new YamlParser();
    public static final Set<String> DRAWN_PART_ARGS = new HashSet<>(Arrays.asList("Type", "Position", "Rotation", "PartName"));
    public static final ComponentParser COMPONENT_PARSER = new ComponentParser();

    private YamlParser() {
    }

    public static void register() {
        ContentParsers.register("yml", INSTANCE);
    }

    public static void logUnkownEntry(Map.Entry<String, Object> entry, String caller) {
        MCH_Logger.get().warn("Uknown argument:" + entry.getKey() + " for " + caller);
    }

    public static int parseHexColor(String s) {
        // Accepts "0xRRGGBB", "#RRGGBB", "RRGGBB"
        if (s == null || s.isEmpty()) throw new IllegalArgumentException("Color string is empty");
        String t = s.trim();
        if (t.startsWith("#")) t = "0x" + t.substring(1);
        if (!t.startsWith("0x") && !t.startsWith("0X")) t = "0x" + t;
        return (int) (Long.decode(t).longValue());
    }

    public static float getClamped(float min, float max, Number value) {
        return Math.max(min, Math.min(max, value.floatValue()));
    }

    public static int getClamped(int min, int max, Number value) {
        return Math.max(min, Math.min(max, value.intValue()));
    }

    public static double getClamped(double min, double max, Number value) {
        return Math.max(min, Math.min(max, value.doubleValue()));
    }

    public static float getClamped(float max, Number value) {
        return getClamped(0, max, value);
    }

    public static int getClamped(int max, Number value) {
        return getClamped(0, max, value);
    }

    public static double getClamped(double max, Number value) {
        return getClamped(0, max, value);
    }

    public static Vec3d parseVector(Object vector) {
        if (vector == null) throw new IllegalArgumentException("Vector value is null");
        if (vector instanceof List<?> list) {
            if (list.size() != 3) {
                throw new IllegalArgumentException("Vector list must have exactly 3 elements, got " + list.size());
            }
            return new Vec3d(asDouble(list.get(0)), asDouble(list.get(1)), asDouble(list.get(2)));
        }
        throw new IllegalArgumentException("Unsupported vector value type: " + vector.getClass());
    }

    private static double asDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) return Double.parseDouble(s.trim());
        throw new IllegalArgumentException("Vector component must be numeric, got: " + o.getClass());
    }

    @Override
    public @Nullable MCH_HeliInfo parseHelicopter(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        InputStream input = Files.newInputStream(Paths.get(filepath), StandardOpenOption.READ);
        Map<String, Object> root = YAML_INSTANCE.load(input);
        var info = new MCH_HeliInfo(location, filepath);
        mapToAircraft(info, root);
        //TODO: Do heli specific parsing
        return info;
    }

    @Override
    public @Nullable MCP_PlaneInfo parsePlane(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_ShipInfo parseShip(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_TankInfo parseTank(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_VehicleInfo parseVehicle(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_WeaponInfo parseWeapon(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_ThrowableInfo parseThrowable(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_Hud parseHud(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @Override
    public @Nullable MCH_ItemInfo parseItem(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        return null;
    }

    @SuppressWarnings("unboxing")
    private void mapToAircraft(MCH_AircraftInfo info, Map<String, Object> root) {


        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "DisplayName" -> {
                    Object nameObject = entry.getValue();
                    if (nameObject instanceof String name) info.displayName = name.trim();
                    else if (nameObject instanceof Map<?, ?> translationNames)
                        info.displayNameLang = (HashMap<String, String>) translationNames;
                    else throw new ClassCastException();
                }
                case "Author" -> {
                    //Proposal: would allow content creators to put their signature
                }
                //Depricated on 1,12, around for 1.7 compat
                case "ItemID" -> {
                    info.itemID = (int) entry.getValue();
                }
                case "Category" -> {
                    if (entry.getValue() instanceof String category)
                        info.category = category.toUpperCase(Locale.ROOT).trim();
                    else if (entry.getValue() instanceof List<?> categories) {
                        List<String> list = (List<String>) categories;
                        info.category = list.stream().map(String::trim).map(String::toUpperCase).collect(Collectors.joining(","));
                    } else throw new RuntimeException();


                }
                case "Recepie" -> {
                    Map<String, Object> map = (Map<String, Object>) entry.getValue();
                    for (Map.Entry<String, Object> recMapEntry : map.entrySet()) {
                        switch (recMapEntry.getKey()) {
                            case "isShaped" -> info.isShapedRecipe = (Boolean) recMapEntry.getValue();
                            case "Pattern" -> info.recipeString = ((List<String>) recMapEntry.getValue())
                                    .stream()
                                    .map(String::toUpperCase)
                                    .map(String::trim)
                                    .collect(Collectors.toList());
                        }
                    }

                }
                case "CanRide" -> info.canRide = ((Boolean) entry.getValue()).booleanValue();
                case "RotorSpeed" -> {
                    info.rotorSpeed = getClamped(-10000.0F, 10000.0F, (Number) entry.getValue());
                    if (info.rotorSpeed > 0.01F) info.rotorSpeed -= 0.01F;
                    if (info.rotorSpeed < -0.01F) info.rotorSpeed += 0.01F; //Interesting
                }
                case "CreativeOnly" -> info.creativeOnly = ((Boolean) entry.getValue()).booleanValue();
                case "Invulnerable" -> info.invulnerable = ((Boolean) entry.getValue()).booleanValue();
                case "MaxFuel" -> info.maxFuel = getClamped(100_000_000, (Number) entry.getValue());
                case "MaxHP" -> info.maxHp = getClamped(1, 1000_000_000, (Number) entry.getValue());
                case "Stealth" -> info.stealth = getClamped(1F, (Number) entry.getValue());
                case "FuelConsumption" -> info.fuelConsumption = getClamped(10_000.0F, (Number) entry.getValue());
                case "FuelSupplyRange" -> info.fuelSupplyRange = getClamped(1_000.0F, (Number) entry.getValue());
                case "AmmoSupplyRange" -> info.ammoSupplyRange = getClamped(1000, (Number) entry.getValue());
                case "RepairOtherVehicles" -> {
                    Map<String, Number> repairMap = (HashMap<String, Number>) entry.getValue();
                    if (repairMap.containsKey("range"))
                        info.repairOtherVehiclesRange = getClamped(1_000.0F, repairMap.get("range"));
                    if (repairMap.containsKey("value"))
                        info.repairOtherVehiclesValue = getClamped(10_000_000, repairMap.get("value"));
                }

                //UNUSED in reforged too,
//                case "RadarType" -> {
//                    if (entry.getValue() instanceof String data) {
//                        try {
//                            info.radarType = EnumRadarType.valueOf(data);
//                        } catch (IllegalArgumentException e) {
//                            info.radarType = EnumRadarType.MODERN_AA;
//                        }
//                    }
//                }
//                case "RWRType" -> {
//                    if (entry.getValue() instanceof String data) {
//                        try {
//                            info.rwrType = EnumRWRType.valueOf(data);
//                        } catch (IllegalArgumentException e) {
//                            info.rwrType = EnumRWRType.DIGITAL;
//                        }
//                    }
//                }
                case "NameOnModernAARadar" -> info.nameOnModernAARadar = ((String) entry.getValue()).trim();
                case "NameOnEarlyAARadar" -> info.nameOnEarlyAARadar = ((String) entry.getValue()).trim();
                case "NameOnModernASRadar" -> info.nameOnModernASRadar = ((String) entry.getValue()).trim();
                case "NameOnEarlyASRadar" -> info.nameOnEarlyASRadar = ((String) entry.getValue()).trim();
                case "ExplosionSizeByCrash" -> info.explosionSizeByCrash = getClamped(100, (Number) entry.getValue());
                case "ThrottleDownFactor" -> {
                    info.throttleDownFactor = getClamped(10F, (Number) entry.getValue());
                }
                case "HUDType", "WeaponGroupType" -> {
                    //Unimplemented
                }
                case "PhysicalProperties" -> {
                    Map<String, Object> phisicalProperties = (Map<String, Object>) entry.getValue();
                    phisicalProperties.entrySet().forEach((armorEntry) -> parsePhisProperties(armorEntry, info));
                }
                case "Render" -> {
                    Map<String, Object> renderProperties = (Map<String, Object>) entry.getValue();
                    parseRender(renderProperties, info);
                }
                case "ParticleScale" -> info.particlesScale = getClamped(50f, (Number) entry.getValue());
                case "EnableSeaSurfaceParticle" ->
                        info.enableSeaSurfaceParticle = ((Boolean) entry.getValue()).booleanValue();
                case "SplashParticles" -> {
                    List<Map<String, Object>> splashParticles = (List<Map<String, Object>>) entry.getValue();
                    splashParticles.stream().map((this::parseParticleSplash)).forEach(info.particleSplashs::add);
                }
                case "Armor" -> {
                    Map<String, Object> armorSettings = (Map<String, Object>) entry.getValue();
                    armorSettings.entrySet().forEach((armorEntry) -> parseArmor(armorEntry, info));
                }

                case "Camera" -> {
                    Map<String, Object> cameraSettings = (Map<String, Object>) entry.getValue();
                    cameraSettings.entrySet().forEach(((camEntry) -> parseGlobalCamera(info, camEntry)));

                }
                case "AircraftFeatures" -> {
                    Map<String, Object> feats = (Map<String, Object>) entry.getValue();
                    parseAircraftFeatures(feats, info);
                }
                case "Racks" -> { //TODO:Move to components
                    List<Map<String, Object>> racks = (List<Map<String, Object>>) entry.getValue();
                    racks.stream().map(this::parseRacks).forEach((rack) -> {
                        if (rack instanceof MCH_SeatRackInfo r) info.entityRackList.add(r);
                        else info.rideRacks.add((RideRack) rack);
                    });
                }
                case "WheelsHitbox" -> {
                    List<Map<String, Object>> wheel = (List<Map<String, Object>>) entry.getValue();
                    info.wheels.addAll(wheel.stream().map(this::parseWheel).sorted((o1, o2) -> o1.pos.z > o2.pos.z ? -1 : 1).collect(Collectors.toList()));
                }
                case "Components" -> {
                    var components = (Map<String, List<Map<String, Object>>>) entry.getValue();
                    COMPONENT_PARSER.parseComponents(components, info);
                }
                case "Sound" -> {
                    Map<String, Object> soundSettings = (Map<String, Object>) entry.getValue();
                    parseSound(soundSettings, info);
                }

                case "Seats" -> {
                    List<Map<String, Object>> seatList = (List<Map<String, Object>>) entry.getValue();
                    seatList.stream().forEachOrdered(seat -> parseSeatInfo(seat, info));

                }

                case "BoundingBoxes" -> {
                    List<Map<String, Object>> boxList = (List<Map<String, Object>>) entry.getValue();
                    boxList.stream().forEachOrdered(box -> parseBoxes(box, info));
                    float maxY = Float.NEGATIVE_INFINITY;
                    float maxAbsXZ = 0.0F;
                    float zMin = Float.POSITIVE_INFINITY;
                    float zMax = Float.NEGATIVE_INFINITY;
                    for (MCH_BoundingBox box : info.extraBoundingBox) {
                        AxisAlignedBB aabb = box.boundingBox;
                        maxY = Math.max(maxY, (float) aabb.maxY);

                        maxAbsXZ = Math.max(maxAbsXZ, Math.max(
                                Math.max(Math.abs((float) aabb.maxX), Math.abs((float) aabb.minX)),
                                Math.max(Math.abs((float) aabb.maxZ), Math.abs((float) aabb.minZ))
                        ));

                        zMin = Math.min(zMin, (float) aabb.minZ);
                        zMax = Math.max(zMax, (float) aabb.maxZ);
                    }
                    info.markerHeight = maxY;
                    info.markerWidth = maxAbsXZ / 2.0F;
                    info.bbZmin = zMin;
                    info.bbZmax = zMax;
                }


                default -> logUnkownEntry(entry, "AircraftInfo");
            }
        }

    }

    private void parseBoxes(Map<String, Object> box, MCH_AircraftInfo info) {
        Vec3d pos = null;
        Vec3d size = null;
        float damageFact = 1f;
        String name = "";
        MCH_BoundingBox.EnumBoundingBoxType type = MCH_BoundingBox.EnumBoundingBoxType.DEFAULT;
        for (Map.Entry<String, Object> entry : box.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "Size" -> size = parseVector(entry.getValue());
                case "DamageFactor", "DmgFact" -> damageFact = ((Number) entry.getValue()).floatValue();
                case "Name" -> name = name.trim();
                case "Type" -> {
                    try {
                        type = MCH_BoundingBox.EnumBoundingBoxType.valueOf(((String) entry.getValue()).toUpperCase(Locale.ROOT).trim());
                    } catch (RuntimeException r) {
                        throw new IllegalArgumentException("Invalid bounding box type: " + entry.getValue() + ". Allowed values: " + Arrays.stream(MCH_BoundingBox.EnumBoundingBoxType.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    }
                }

                default -> logUnkownEntry(entry, "BoundingBox");
            }

            if (pos == null)
                throw new IllegalArgumentException("Bounding box must have a position!");
            if (size == null)
                throw new IllegalArgumentException("Bounding box must have a size!");

            var parsedBox = new MCH_BoundingBox(pos.x, pos.y, pos.z, (float) size.x, (float) size.y, (float) size.z, damageFact);
            parsedBox.setBoundingBoxType(type);
            info.extraBoundingBox.add(parsedBox);
        }

    }

    private void parseSound(Map<String, Object> soundSettings, MCH_AircraftInfo info) {

        for (Map.Entry<String, Object> entry : soundSettings.entrySet()) {
            switch (entry.getKey()) {
                case "MoveSound" -> info.soundMove = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "Volume", "Vol" -> info.soundVolume = getClamped(10F, (Number) entry.getValue());
                case "Pitch" -> info.soundVolume = getClamped(1F, 10F, (Number) entry.getValue());
                case "Range" -> info.soundRange = getClamped(1F, 1000.0F, (Number) entry.getValue());
            }

        }
    }

    private void parseRender(Map<String, Object> renderProperties, MCH_AircraftInfo info) {
        for (Map.Entry<String, Object> entry : renderProperties.entrySet()) {
            switch (entry.getKey()) {
                case "Textures" -> {
                    List<String> textures = (List<String>) entry.getValue();
                    textures.stream().map(String::trim).forEach(info::addTextureName);
                }
                case "SmoothShading" -> info.smoothShading = ((Boolean) entry.getValue()).booleanValue();
                case "HideRiders" -> info.hideEntity = ((Boolean) entry.getValue()).booleanValue();
                case "ModelWidth" -> info.entityWidth = ((Number) entry.getValue()).floatValue();
                case "ModelHeight" -> info.entityHeight = ((Number) entry.getValue()).floatValue();
                case "ModelPitch" -> info.entityPitch = ((Number) entry.getValue()).floatValue();
                case "ModelRoll" -> info.entityRoll = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "Render");
            }

        }
    }

    private void parsePhisProperties(Map.Entry<String, Object> entry, MCH_AircraftInfo info) {
        switch (entry.getKey()) {
            case "Speed" -> info.armorDamageFactor = getClamped(info.getMaxSpeed(), (Number) entry.getValue());
            case "CanFloat" -> info.isFloat = (Boolean) entry.getValue();
            case "FloatOffset" -> info.floatOffset = -((Number) entry.getValue()).floatValue();
            case "MotionFactor" -> info.motionFactor = getClamped(1F, (Number) entry.getValue());
            case "Gravity" -> info.gravity = getClamped(-50F, 50F, (Number) entry.getValue());
            case "RotationSnapValue" -> info.autoPilotRot = getClamped(-5F, 5F, (Number) entry.getValue());
            case "GravityInWater" -> info.gravityInWater = getClamped(-50F, 50F, (Number) entry.getValue());
            case "StepHeight" -> info.stepHeight = getClamped(0F, 1000F, (Number) entry.getValue());
            case "CanRotOnGround" -> info.canMoveOnGround = (Boolean) entry.getValue();

            case "Mobility" -> {
                Map<String, Object> mob = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> mobEntry : mob.entrySet()) {
                    switch (mobEntry.getKey()) {
                        case "Yaw" -> info.mobilityYaw = getClamped(100F, (Number) mobEntry.getValue());
                        case "Pitch" -> info.mobilityPitch = getClamped(100F, (Number) mobEntry.getValue());
                        case "Roll" -> info.mobilityRoll = getClamped(100F, (Number) mobEntry.getValue());
                        case "YawOnGround" -> info.mobilityYawOnGround = getClamped(100F, (Number) mobEntry.getValue());
                        default -> logUnkownEntry(mobEntry, "Mobility");
                    }
                }
            }

            case "GroundPitchFactors" -> {
                Map<String, Object> factors = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> rotEntry : factors.entrySet()) {
                    switch (rotEntry.getKey()) {
                        case "Pitch" -> info.onGroundPitchFactor = getClamped(0F, 180F, (Number) rotEntry.getValue());
                        case "Roll" -> info.onGroundRollFactor = getClamped(0F, 180F, (Number) rotEntry.getValue());
                        default -> logUnkownEntry(rotEntry, "GroundPitchFactors");
                    }
                }
            }

            case "RotationLimits" -> {
                info.limitRotation = true;
                Map<String, Object> rotationLimits = (Map<String, Object>) entry.getValue();

                for (Map.Entry<String, Object> rotEntry : rotationLimits.entrySet()) {
                    switch (rotEntry.getKey()) {
                        case "Pitch" -> {
                            Map<String, Object> pitchMap = (Map<String, Object>) rotEntry.getValue();
                            if (pitchMap.containsKey("min"))
                                info.minRotationPitch = getClamped(info.getMinRotationPitch(), 0F, (Number) pitchMap.get("min"));
                            if (pitchMap.containsKey("max"))
                                info.maxRotationPitch = getClamped(0F, info.getMaxRotationPitch(), (Number) pitchMap.get("max"));
                        }
                        case "Roll" -> {
                            Map<String, Object> rollMap = (Map<String, Object>) rotEntry.getValue();
                            if (rollMap.containsKey("min"))
                                info.minRotationRoll = getClamped(info.getMinRotationRoll(), 0F, (Number) rollMap.get("min"));
                            if (rollMap.containsKey("max"))
                                info.maxRotationRoll = getClamped(0F, info.getMaxRotationRoll(), (Number) rollMap.get("max"));
                        }
                        default -> logUnkownEntry(rotEntry, "RotationLimits");
                    }
                }
            }

            default -> logUnkownEntry(entry, "PhysicalProperties");
        }
    }


    private void parseArmor(Map.Entry<String, Object> entry, MCH_AircraftInfo info) {
        switch (entry.getKey()) {
            case "ArmorDamageFactor" -> info.armorDamageFactor = getClamped(10_000F, (Number) entry.getValue());
            case "ArmorMinDamage" -> info.armorMinDamage = getClamped(1_000_000F, (Number) entry.getValue());
            case "ArmorMaxDamage" -> info.armorMaxDamage = getClamped(1_000_000F, (Number) entry.getValue());
            default -> logUnkownEntry(entry, "Armor");
        }

    }

    @SuppressWarnings("unboxing")
    private void parseAircraftFeatures(Map<String, Object> map, MCH_AircraftInfo info) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "GunnerMode" -> info.isEnableGunnerMode = ((Boolean) entry.getValue()).booleanValue();
                case "InventorySize" ->
                        info.inventorySize = getClamped(54, (Number) entry.getValue()); //FIXME: Capped due to inventory code being fucking ass
                case "NightVision" -> info.isEnableNightVision = ((Boolean) entry.getValue()).booleanValue();
                case "EntityRadar" -> info.isEnableEntityRadar = ((Boolean) entry.getValue()).booleanValue();
                case "CanReverse" -> info.enableBack = ((Boolean) entry.getValue()).booleanValue();
                case "ConcurrentGunner" ->
                        info.isEnableConcurrentGunnerMode = ((Boolean) entry.getValue()).booleanValue();
                case "EjectionSeat" -> info.isEnableEjectionSeat = ((Boolean) entry.getValue()).booleanValue();
                case "Parachuting" -> {
                    if (entry.getValue() instanceof Boolean bool) info.isEnableParachuting = bool.booleanValue();
                    else if (entry.getValue() instanceof Map<?, ?> parachuteMapRaw) {
                        info.isEnableParachuting = true;
                        for (Map.Entry<String, Object> mobEntry : ((Map<String, Object>) parachuteMapRaw).entrySet()) {
                            switch (mobEntry.getKey()) {
                                case "Pos", "Position" -> info.mobDropOption.pos = parseVector(mobEntry.getValue());
                                case "Interval" ->
                                        info.mobDropOption.interval = ((Number) mobEntry.getValue()).intValue();
                                default -> logUnkownEntry(mobEntry, "Parachuting");
                            }
                        }
                    } else
                        throw new IllegalArgumentException("Parachuting type must be a boolean or map, got: " + entry.getValue().getClass());
                }
                case "Flare" -> info.flare = parseFlare((Map<String, Object>) entry.getValue());
                default -> logUnkownEntry(entry, "AircraftFeatures");
            }
        }
    }

    private Flare parseFlare(Map<String, Object> value) {
        Vec3d pos = Vec3d.ZERO;
        List<FlareType> flareTypes = new ArrayList<>();

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Positions" -> pos = parseVector(entry.getValue());
                case "Type", "Types" -> {
                    List<String> typeStrings = new ArrayList<>();
                    if (entry.getValue() instanceof String singleType) {
                        typeStrings.add(singleType);
                    } else if (entry.getValue() instanceof String[] typeArray) {
                        typeStrings.addAll(Arrays.asList(typeArray));
                    } else if (entry.getValue() instanceof List<?> typeList) {
                        for (Object obj : typeList) {
                            if (obj instanceof String s) typeStrings.add(s);
                            else
                                throw new IllegalArgumentException("Flare type must be a string, got: " + obj.getClass());
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported type value: " + entry.getValue().getClass());
                    }

                    for (String typeRaw : typeStrings) {
                        try {
                            FlareType type = FlareType.valueOf(typeRaw.trim().toUpperCase(Locale.ROOT));
                            flareTypes.add(type);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid flare type: " + typeRaw + ". Allowed values: " + Arrays.stream(FlareType.values()).map(Enum::name).collect(Collectors.joining(", ")));
                        }
                    }
                }

                default -> logUnkownEntry(entry, "Flare");
            }
        }

        if (flareTypes.isEmpty()) {
            flareTypes.add(FlareType.NONE);
        }

        return new Flare(pos, flareTypes.stream().map(FlareType::getLegacyMapping).mapToInt(Integer::intValue).toArray());
    }

    @SuppressWarnings("unboxing")
    private void parseGlobalCamera(MCH_AircraftInfo info, Map.Entry<String, Object> entry) {
        switch (entry.getKey()) {
            case "ThirdPersonDist" -> info.thirdPersonDist = getClamped(4f, 100f, (Number) entry.getValue());
            case "Zoom", "CameraZoom" -> info.cameraZoom = getClamped(1, 10, (Number) entry.getValue());
            case "DefaultFreeLook" -> info.defaultFreelook = ((Boolean) entry.getValue()).booleanValue();
            case "RotationSpeed" -> info.cameraRotationSpeed = getClamped(10000.0F, (Number) entry.getValue());
            case "Pos", "Positons" -> {
                List<Map<String, Object>> cameraList = (List<Map<String, Object>>) entry.getValue();
                info.cameraPosition.addAll(cameraList.stream().map(this::parseCameraPosition).collect(Collectors.toList()));
            }
            default -> logUnkownEntry(entry, "Camera");
        }


    }

    private Wheel parseWheel(Map<String, Object> map) {
        Vec3d wheelPos = null;
        float scale = 1;


        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> wheelPos = parseVector(entry.getValue());
                case "Scale" -> scale = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "Wheels");
            }
        }


        if (wheelPos == null) throw new IllegalArgumentException("Wheel must have a position!");
        return new Wheel(wheelPos, scale);
    }

    private Object parseRacks(Map<String, Object> map) {
        if (map.containsKey("Type")) {
            RACK_TYPE type = RACK_TYPE.valueOf((String) map.get("Type"));
            return switch (type) {
                case NORMAL -> parseSeatRackInfo(map);
                case RIDING -> parseRidingRack(map);
                default -> throw new UnsupportedOperationException();
            };

        } else {
            return parseSeatRackInfo(map);
        }
    }

    private MCH_SeatRackInfo parseSeatRackInfo(Map<String, Object> map) {
        Vec3d position = null;
        CameraPosition cameraPos = null;
        String[] entityNames = new String[0];
        float range = 0f;
        float openParaAlt = 0f;
        float yaw = 0f;
        float pitch = 0f;
        boolean rotSeat = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> position = parseVector(entry.getValue());
                case "Camera", "Cam" -> cameraPos = parseCameraPosition((Map<String, Object>) entry.getValue());
                case "Names", "Name" -> {
                    Object values = entry.getValue();
                    if (values instanceof List<?> list) entityNames = list.toArray(new String[0]);
                    else if (values instanceof String[] array) entityNames = array;
                    else if (values instanceof String name) entityNames = new String[]{name};
                    else throw new IllegalArgumentException("Rack name must be a string, array or list!");
                }
                case "Range" -> range = ((Number) entry.getValue()).floatValue();
                case "OpenParaAlt" -> openParaAlt = ((Number) entry.getValue()).floatValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "RotSeat" -> rotSeat = ((Boolean) entry.getValue()).booleanValue();
                default -> logUnkownEntry(entry, "Racks");
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat rack must have a position!");
        }
        if (cameraPos == null) {
            throw new IllegalArgumentException("Seat rack must have a camera position!");
        }

        return new MCH_SeatRackInfo(entityNames, position.x, position.y, position.z, cameraPos, range, openParaAlt, yaw, pitch, rotSeat);
    }

    private RideRack parseRidingRack(Map<String, Object> map) {
        int rackID = -1;//FUCK IDs
        String name = "";


        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "id" -> rackID = getClamped(1, 10_000, (Number) entry.getValue());
                case "name" -> name = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
            }
        }
        if (rackID == -1 || name.isEmpty()) throw new IllegalArgumentException("Name nor ID can be empty!");

        return new RideRack(name, rackID);
    }

    private ParticleSplash parseParticleSplash(Map<String, Object> map) {
        Vec3d pos = null;
        int num = 2;
        float acceleration = 1f;
        float size = 2f;
        int age = 80;
        float motionY = 0.01f;
        float gravity = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "Count" -> num = getClamped(1, 100, (Number) entry.getValue());
                case "Size" -> size = ((Number) entry.getValue()).floatValue();
                case "Accel" -> acceleration = ((Number) entry.getValue()).floatValue();
                case "Age" -> age = getClamped(1, 100_000, (Number) entry.getValue());
                case "Motion" -> motionY = ((Number) entry.getValue()).floatValue();
                case "Gravity" -> gravity = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "SplashParticles");
            }
        }

        if (pos == null) throw new IllegalArgumentException("Splash particle must have a position!");

        return new ParticleSplash(num, acceleration, size, pos, age, motionY, gravity);
    }

    private CameraPosition parseCameraPosition(Map<String, Object> map) {
        Vec3d pos = Vec3d.ZERO;
        boolean fixRot = false;
        float yaw = 0;
        float pitch = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "FixedRot" -> fixRot = ((Boolean) entry.getValue()).booleanValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "CameraPosition");
            }
        }

        return new CameraPosition(pos, fixRot, yaw, pitch);
    }

    @SuppressWarnings("unboxing")
    private void parseSeatInfo(Map<String, Object> map, MCH_AircraftInfo info) {
        Vec3d position = null;
        boolean isGunner = false;
        boolean canSwitchGunner = false;
        boolean hasFixedRotation = false;
        float fixedYaw = 0f;
        float fixedPitch = 0f;
        float minPitch = -30f;
        float maxPitch = 70f;
        boolean rotatableSeat = false;
        boolean invertCameraPos = false;
        CameraPosition cameraPos = null;
        List<Integer> exclusionList = null;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> position = parseVector(entry.getValue());
                case "Gunner" -> isGunner = ((Boolean) entry.getValue()).booleanValue();
                case "SwitchGunner" -> canSwitchGunner = ((Boolean) entry.getValue()).booleanValue();
                case "FixRot" -> hasFixedRotation = ((Boolean) entry.getValue()).booleanValue();
                case "FixYaw" -> fixedYaw = ((Number) entry.getValue()).floatValue();
                case "FixPitch" -> fixedPitch = ((Number) entry.getValue()).floatValue();
                case "MinPitch" -> minPitch = ((Number) entry.getValue()).floatValue();
                case "MaxPitch" -> maxPitch = ((Number) entry.getValue()).floatValue();
                case "RotSeat" -> rotatableSeat = ((Boolean) entry.getValue()).booleanValue();
                case "InvCamPos" -> invertCameraPos = ((Boolean) entry.getValue()).booleanValue();
                case "Camera", "Cam" -> cameraPos = parseCameraPosition((Map<String, Object>) entry.getValue());
                case "ExcludeWith" -> exclusionList = ((List<Number>) entry.getValue())
                        .stream()
                        .mapToInt(Number::intValue)
                        .boxed()
                        .collect(Collectors.toList());


                default -> logUnkownEntry(entry, "Seats");
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat must have a position!");
        }

        info.seatList.add(new MCH_SeatInfo(position, isGunner, cameraPos, invertCameraPos, canSwitchGunner, hasFixedRotation, fixedYaw, fixedPitch, minPitch, maxPitch, rotatableSeat));

        final int seatIndex = info.seatList.size();
        if (exclusionList != null)
            exclusionList.stream().map(integers -> new Integer[]{seatIndex, integers}).forEachOrdered(info.exclusionSeatList::add);
    }

    public static enum RACK_TYPE {//Could be bool, but this makes it more extensible
        NORMAL, RIDING
    }

    public static enum FlareType {
        NONE(0), NORMAL(1), LARGE_AIRCRAFT(2), SIDE(3), FRONT(4), DOWN(5), SMOKE_LAUNCHER(10);

        final byte legacyMapping;

        FlareType(int legacyMapping) {

            this.legacyMapping = (byte) legacyMapping;
        }

        public int getLegacyMapping() {
            return legacyMapping;
        }

    }
}
