package com.norwood.mcheli.helper.info;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.plane.MCP_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.norwood.mcheli.aircraft.MCH_AircraftInfo.*;

@SuppressWarnings({"unchecked", "unboxing"})
public class YamlParser implements IParser {

    public static final Yaml YAML_INSTANCE = new Yaml();
    public static final YamlParser INSTANCE = new YamlParser();

    private YamlParser() {
    }

    public static void register() {
        ContentParsers.register("yml", INSTANCE);
    }

    @Override
    public @Nullable MCH_HeliInfo parseHelicopter(AddonResourceLocation location, String filepath, List<String> lines, boolean reload) throws Exception {
        InputStream input = Files.newInputStream(Paths.get(filepath), StandardOpenOption.READ);
        Map<String, Object> root = YAML_INSTANCE.load(input);


        return null;
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
                case "CanRide" -> info.canRide = ((Boolean) entry.getValue()).booleanValue();
                case "CreativeOnly" -> info.creativeOnly = ((Boolean) entry.getValue()).booleanValue();
                case "Invulnerable" -> info.invulnerable = ((Boolean) entry.getValue()).booleanValue();
                case "MaxFuel" -> info.maxFuel = getClamped(100_000_000, (Number) entry.getValue());
                case "Stealth" -> info.stealth = getClamped(1F, (Number) entry.getValue());
                case "FuelConsumption" -> info.fuelConsumption = getClamped(10_000.0F, (Number) entry.getValue());
                case "FuelSupplyRange" -> info.fuelSupplyRange = getClamped(1_1000.0F, (Number) entry.getValue());
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
                    phisicalProperties.entrySet().stream().forEach((armorEntry) -> parsePhisProperties(armorEntry, info));
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
                    armorSettings.entrySet().stream().forEach((armorEntry) -> parseArmor(armorEntry, info));
                }
                case "SearchLights" -> {
                    List<Map<String, Object>> searchLights = (List<Map<String, Object>>) entry.getValue();
                    searchLights.stream().map((this::parseSearchLights)).forEach(info.searchLights::add);
                }
                case "LightHatch" -> {
                    List<Map<String, Object>> lightHatchEntries = (List<Map<String, Object>>) entry.getValue();
                    lightHatchEntries.stream().map((hatchEntry) -> parseHatch(info, hatchEntry)).forEach(info.lightHatchList::add);
                }
                case "RepellingHooks" -> {
                    List<Map<String, Object>> repellingHooks = (List<Map<String, Object>>) entry.getValue();
                    repellingHooks.stream().map(this::parseHook).forEach(info.repellingHooks::add);
                }
                case "Camera" -> {
                    Map<String, Object> cameraSettings = (Map<String, Object>) entry.getValue();
                    cameraSettings.entrySet().stream().forEach(((camEntry) -> parseGlobalCamera(info, camEntry)));

                }
                case "AircraftFeatures" -> {
                    Map<String, Object> feats = (Map<String, Object>) entry.getValue();
                    parseAircraftFeatures(feats, info);
                }
                case "Racks" -> {
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
                case "Parts" -> {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) entry.getValue();
                    parseParts(parts, info);
                }

                case "Seats" -> {

                }


                default -> logUnkownEntry(entry, "AircraftInfo");
            }
        }

    }

    //TODO: add the whole default handling thing into it
    private void parseParts(List<Map<String, Object>> parts, MCH_AircraftInfo info) {
        for (Map<String, Object> part : parts) {
            if (!part.containsKey("Type") || !(part.get("Type") instanceof String))
                throw new IllegalArgumentException("Part must contain a Type string!");

            String type = ((String) part.get("Type")).trim();
            switch (type) {
                case "Camera" -> parseDrawnPart(
                        Camera.class,
                        part,
                        drawnPart -> new Camera(
                                drawnPart,
                                (Boolean) part.getOrDefault("yawSync", true),
                                (Boolean) part.getOrDefault("pitchSync", false)
                        ),
                        info.cameraList,
                        new HashSet<>(Arrays.asList("yawSync", "pitchSync"))
                );

                case "Canopy" -> parseDrawnPart(
                        Canopy.class,
                        part,
                        drawnPart -> new Canopy(
                                drawnPart,
                                getClamped(-180F, 180F, (Number) part.getOrDefault("maxRotation", 90F)),
                                (Boolean) part.getOrDefault("isSliding", false)
                        ),
                        info.canopyList,
                        new HashSet<>(Arrays.asList("maxRotation", "isSliding"))
                );

                case "Hatch" -> parseDrawnPart(
                        Hatch.class,
                        part,
                        drawnPart -> new Hatch(
                                drawnPart,
                                getClamped(-180F, 180F, (Number) part.getOrDefault("maxRotation", 90F)),
                                (Boolean) part.getOrDefault("isSliding", false)
                        ),
                        info.hatchList,
                        new HashSet<>(Arrays.asList("maxRotation", "isSliding"))
                );

                case "WeaponBay" -> {
                    String weaponName = part.containsKey("WeaponName")
                            ? ((String) part.get("WeaponName")).trim()
                            : null;
                    if (weaponName == null)
                        throw new IllegalArgumentException("WeaponName is required!");
                    parseDrawnPart(
                            "wb",
                            part,
                            drawnPart -> new WeaponBay(
                                    drawnPart,
                                    getClamped(-180F, 180F, (Number) part.getOrDefault("maxRotation", 90F)),
                                    (Boolean) part.getOrDefault("isSliding", false),
                                    weaponName
                            ),
                            info.partWeaponBay,
                            new HashSet<>(Arrays.asList("maxRotation", "isSliding", "WeaponName"))
                    );
                }

                case "Rotation" -> parseDrawnPart(
                        RotPart.class,
                        part,
                        drawnPart -> new RotPart(
                                drawnPart,
                                ((Number) part.getOrDefault("Speed", 0)).floatValue(),
                                ((Boolean) part.getOrDefault("AlwaysRotate", false))
                        ),
                        info.partRotPart,
                        new HashSet<>(Arrays.asList("Speed", "AlwaysRotate"))
                );

                case "SteeringWheel" -> parseDrawnPart(
                        "steering_wheel",
                        part,
                        drawnPart -> new PartWheel(
                                drawnPart,
                                ((Number) part.getOrDefault("Direction", 0F)).floatValue(),
                                part.containsKey("Pivot") ? parseVector((Object[]) part.get("Pivot")) : Vec3d.ZERO
                        ),
                        info.partSteeringWheel,
                        new HashSet<>(Arrays.asList("Direction", "Pivot"))
                );

                case "Wheel" -> {
                    Vec3d pos = null;
                    Vec3d rot = new Vec3d(0, 1, 0);
                    Vec3d pivot = Vec3d.ZERO;
                    String name = "wheel" + info.partWheel.size();
                    float dir = 0;

                    for (Map.Entry<String, Object> entry : part.entrySet()) {
                        switch (entry.getKey()) {
                            case "Position" -> pos = parseVector((Object[]) entry.getValue());
                            case "Rotation" -> rot = parseVector((Object[]) entry.getValue());
                            case "Direction" -> dir = getClamped(-1800.0F, 1800.0F, (Number) entry.getValue());
                            case "Pivot" -> pivot = parseVector((Object[]) entry.getValue());
                            case "PartName" -> name = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                            case "Type" -> {}
                            default -> logUnkownEntry(entry, "PartWheel");
                        }
                    }
                    if (pos == null)
                        throw new IllegalArgumentException("Part wheel must have a Position!");
                    info.partWheel.add(new PartWheel(new DrawnPart(pos, rot, name), dir, pivot));
                }

                case "LandingGear" -> parseDrawnPart(
                        "lg",
                        part,
                        drawnPart -> {
                            float maxRot = getClamped(-180F, 180F, (Number) part.getOrDefault("maxRotation", 90F)) / 90F;
                            boolean reverse = (Boolean) part.getOrDefault("isReverse", false);
                            boolean hatch = (Boolean) part.getOrDefault("isHatch", false);
                            LandingGear gear = new LandingGear(drawnPart, maxRot, reverse, hatch);

                            if (part.containsKey("ArticulatedRotation")) {
                                gear.enableRot2 = true;
                                gear.rot2 = parseVector((Object[]) part.get("ArticulatedRotation"));
                                gear.maxRotFactor2 = getClamped(
                                        -180F, 180F, (Number) part.getOrDefault("MaxArticulatedRotation", 90F)
                                ) / 90F;
                            }

                            if (part.containsKey("SlideVec")) {
                                gear.slide = parseVector((Object[]) part.get("SlideVec"));
                            }

                            return gear;
                        },
                        info.landingGear,
                        new HashSet<>(Arrays.asList("maxRotation", "isReverse", "isHatch", "ArticulatedRotation", "MaxArticulatedRotation", "SlideVec"))
                );

                case "Weapon" -> { /* TODO */ }
            }
        }
    }

    private <Y extends DrawnPart> void parseDrawnPart(
            String defaultName,
            Map<String, Object> map,
            Function<DrawnPart, Y> fillChildFields,
            List<Y> partList,
            Set<String> knownKeys) {

        Vec3d pos = map.containsKey("Position") ? parseVector((Object[]) map.get("Position")) : null;
        Vec3d rot = map.containsKey("Rotation") ? parseVector((Object[]) map.get("Rotation")) : null;

        String modelName = (String) map.getOrDefault("PartName", defaultName + partList.size());
        if (pos == null || rot == null)
            throw new IllegalArgumentException("Part Rotation and Position must be set!");

        var base = new DrawnPart(pos, rot, modelName);
        var built = fillChildFields.apply(base);
        partList.add(built);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!knownKeys.contains(key) &&
                    !Set.of("Type", "Position", "Rotation", "PartName").contains(key)) {
                logUnkownEntry(entry, built.getClass().getSimpleName());
            }
        }
    }

    private <Y extends DrawnPart> void parseDrawnPart(
            Class<? extends DrawnPart> clazz,
            Map<String, Object> map,
            Function<DrawnPart, Y> fillChildFields,
            List<Y> partList,
            Set<String> knownKeys) {
        parseDrawnPart(clazz.getSimpleName().toLowerCase(Locale.ROOT).trim(), map, fillChildFields, partList, knownKeys);
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
            }

        }
    }

    private void parsePhisProperties(Map.Entry<String, Object> entry, MCH_AircraftInfo info) {
        switch (entry.getKey()) {
            case "Speed" -> info.armorDamageFactor = getClamped(info.getMaxSpeed(), (Number) entry.getValue());
            case "MotionFactor" -> info.motionFactor = getClamped(1F, (Number) entry.getValue());
            case "Gravity" -> info.gravity = getClamped(-50F, 50F, (Number) entry.getValue());
            case "GravityInWater" -> info.gravityInWater = getClamped(-50F, 50F, (Number) entry.getValue());
            case "StepHeight" -> info.stepHeight = getClamped(1000F, (Number) entry.getValue());

            case "MobilityYawOnGround" -> info.mobilityYawOnGround = getClamped(100F, (Number) entry.getValue());
            case "MobilityYaw" -> info.mobilityYaw = getClamped(100F, (Number) entry.getValue());
            case "MobilityPitch" -> info.mobilityPitch = getClamped(100F, (Number) entry.getValue());
            case "MobilityRoll" -> info.mobilityRoll = getClamped(100F, (Number) entry.getValue());
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
                case "NightVision" -> info.isEnableNightVision = ((Boolean) entry.getValue()).booleanValue();
                case "EntityRadar" -> info.isEnableEntityRadar = ((Boolean) entry.getValue()).booleanValue();
                case "ConcurrentGunner" ->
                        info.isEnableConcurrentGunnerMode = ((Boolean) entry.getValue()).booleanValue();
                case "EjectionSeat" -> info.isEnableEjectionSeat = ((Boolean) entry.getValue()).booleanValue();
                case "Parachuting" -> {
                    if (entry.getValue() instanceof Boolean bool) info.isEnableParachuting = bool.booleanValue();
                    else if (entry.getValue() instanceof Map<?, ?> parachuteMapRaw) {
                        info.isEnableParachuting = true;
                        for (Map.Entry<String, Object> mobEntry : ((Map<String, Object>) parachuteMapRaw).entrySet()) {
                            switch (mobEntry.getKey()) {
                                case "Pos", "Position" ->
                                        info.mobDropOption.pos = parseVector((Object[]) mobEntry.getValue());
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
                case "Pos", "Positions" -> pos = parseVector((Object[]) entry.getValue());

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
                case "pos", "position" -> wheelPos = parseVector((Object[]) entry.getValue());
                case "scale" -> scale = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "Wheels");
            }
        }


        if (wheelPos == null) throw new IllegalArgumentException("Wheel must have a position!");
        return new Wheel(wheelPos, scale);
    }

    private Object parseRacks(Map<String, Object> map) {
        if (map.containsKey("type")) {
            RACK_TYPE type = RACK_TYPE.valueOf((String) map.get("type"));
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
                case "Pos" -> position = parseVector((Object[]) entry.getValue());
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

    private void logUnkownEntry(Map.Entry<String, Object> entry, String caller) {
        MCH_Logger.get().warn("Uknown argument:" + entry.getKey() + " for " + caller);
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

    private SearchLight parseSearchLights(Map<String, Object> map) {
        Vec3d pos = null;
        int colorStart = 0xFFFFFF; // default white
        int colorEnd = 0xFFFFFF;
        float height = 1.0f;
        float width = 1.0f;
        float yaw = 0.0f;
        float pitch = 0.0f;
        float stRot = 0.0f;

        boolean fixedDirection = false;
        boolean steering = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "type" -> {

                }
                case "FixedDirection" -> fixedDirection = ((Boolean) entry.getValue()).booleanValue();
                case "Steering" -> steering = ((Boolean) entry.getValue()).booleanValue();
                case "Pos", "Position" -> pos = parseVector((Object[]) entry.getValue());
                case "ColorStart" -> colorStart = parseHexColor((String) entry.getValue());
                case "ColorEnd" -> colorEnd = parseHexColor((String) entry.getValue());
                case "Height" -> height = ((Number) entry.getValue()).floatValue();
                case "Width" -> width = ((Number) entry.getValue()).floatValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "StRot" -> stRot = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "SearchLights");
            }
        }

        if (pos == null) {
            throw new IllegalArgumentException("SearchLight must have a position!");
        }

        return new SearchLight(pos, colorStart, colorEnd, height, width, fixedDirection, yaw, pitch, steering, stRot);


    }

    private RepellingHook parseHook(Map<String, Object> map) {
        Vec3d pos = null;
        int interval = 0;


        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> pos = parseVector((Object[]) entry.getValue());
                case "interval" -> interval = ((Number) entry.getValue()).intValue();
                default -> logUnkownEntry(entry, "RepellingHooks");
            }
        }

        if (pos == null) throw new IllegalArgumentException("Repelling hook must have a position!");
        return new RepellingHook(pos, interval);
    }

    public int parseHexColor(String s) {
        return !s.startsWith("0x") && !s.startsWith("0X") && s.indexOf(0) != 35 ? (int) (Long.decode("0x" + s).longValue()) : (int) (Long.decode(s).longValue());
    }

    private float getClamped(float min, float max, Number value) {
        return Math.max(min, Math.min(max, value.floatValue()));
    }

    private int getClamped(int min, int max, Number value) {
        return Math.max(min, Math.min(max, value.intValue()));
    }

    private double getClamped(double min, double max, Number value) {
        return Math.max(min, Math.min(max, value.doubleValue()));
    }

    private float getClamped(float max, Number value) {
        return getClamped(0, max, value);
    }

    private int getClamped(int max, Number value) {
        return getClamped(0, max, value);
    }

    private double getClamped(double max, Number value) {
        return getClamped(0, max, value);
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
                case "Pos" -> pos = parseVector((Object[]) entry.getValue());
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

        return new ParticleSplash(num, size, acceleration, pos, age, motionY, gravity);
    }

    private CameraPosition parseCameraPosition(Map<String, Object> map) {
        Vec3d pos = Vec3d.ZERO;
        boolean fixRot = false;
        float yaw = 0;
        float pitch = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos" -> pos = parseVector((Object[]) entry.getValue());
                case "FixedRot" -> fixRot = ((Boolean) entry.getValue()).booleanValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "CameraPosition");
            }
        }

        return new CameraPosition(pos, fixRot, yaw, pitch);
    }

    @SuppressWarnings("unboxing")
    private MCH_SeatInfo parseSeatInfo(Map<String, Object> map) {
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

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos" -> position = parseVector((Object[]) entry.getValue());
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
                default -> logUnkownEntry(entry, "Seats");
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat must have a position!");
        }

        return new MCH_SeatInfo(position, isGunner, cameraPos, invertCameraPos, canSwitchGunner, hasFixedRotation, fixedYaw, fixedPitch, minPitch, maxPitch, rotatableSeat);
    }


    private Vec3d parseVector(Object[] vector) {
        if (vector instanceof Number[] numbers) {
            if (numbers.length != 3)
                throw new IllegalArgumentException("The vector array must contain princely 3 numbers!");
            return new Vec3d(numbers[0].doubleValue(), numbers[1].doubleValue(), numbers[2].doubleValue());
        } else throw new IllegalArgumentException("Vector must be an array of Numbers!");


    }

    private Hatch parseHatch(MCH_AircraftInfo data, Map<String, Object> map) {
        Vec3d position = null;
        Vec3d rotation = null;
        float maxRotation = 0f;
        String partName = "light_hatch" + data.lightHatchList.size();
        boolean isSliding = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos" -> position = parseVector((Object[]) entry.getValue());
                case "Rot" -> rotation = parseVector((Object[]) entry.getValue());
                case "Name" -> partName = (String) entry.getValue();
                case "IsSliding" -> isSliding = ((Boolean) entry.getValue()).booleanValue();
                default -> logUnkownEntry(entry, "Hatches");
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Hatch must have a position!");
        }
        if (rotation == null) {
            throw new IllegalArgumentException("Hatch must have a rotation!");
        }

        return new Hatch(position, rotation, partName, maxRotation, isSliding);
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
