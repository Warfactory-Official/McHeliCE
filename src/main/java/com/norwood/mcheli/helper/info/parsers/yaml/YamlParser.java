package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.RWRType;
import com.norwood.mcheli.RadarType;
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
import com.norwood.mcheli.hud.MCH_HudManager;
import com.norwood.mcheli.item.MCH_ItemInfo;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfoManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

import static com.norwood.mcheli.aircraft.MCH_AircraftInfo.*;

// TODO:Reforged Fields
@SuppressWarnings({ "unchecked", "unboxing" })
public class YamlParser implements IParser {

    public static final Yaml YAML_INSTANCE = new Yaml();
    public static final YamlParser INSTANCE = new YamlParser();
    public static final Set<String> DRAWN_PART_ARGS = new HashSet<>(
            Arrays.asList("Type", "Position", "Rotation", "PartName", "Rot", "Pos"));

    private YamlParser() {}

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

    public static float getClamped(float min, float max, Object value) {
        return Math.max(min, Math.min(max, ((Number) value).floatValue()));
    }

    public static int getClamped(int min, int max, Object value) {
        return Math.max(min, Math.min(max, ((Number) value).intValue()));
    }

    public static double getClamped(double min, double max, Number value) {
        return Math.max(min, Math.min(max, ((Number) value).doubleValue()));
    }

    public static float getClamped(float max, Object value) {
        return getClamped(0, max, value);
    }

    public static int getClamped(int max, Object value) {
        return getClamped(0, max, value);
    }

    public static double getClamped(double max, Number value) {
        return getClamped(0, max, value);
    }

    public static Vec3d parseVector(Object vector) {
        if (vector == null) throw new IllegalArgumentException("Vector value is null");
        if (vector instanceof List<?>list) {
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
    public @Nullable MCH_HeliInfo parseHelicopter(AddonResourceLocation location, String filepath, List<String> lines,
                                                  boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_HeliInfo(location, filepath);
        mapToAircraft(info, root);
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "HeliFeatures" -> parseHeliFeatures((Map<String, Object>) entry.getValue(), info);
                case "Components" -> {
                    var components = (Map<String, List<Map<String, Object>>>) entry.getValue();
                    ComponentParser.parseComponentsHeli(components, info);
                }
            }

        }
        info.validate();
        return info;
    }

    private void parseHeliFeatures(Map<String, Object> heliFeat, MCH_HeliInfo info) {
        for (Map.Entry<String, Object> mobEntry : heliFeat.entrySet()) {
            switch (mobEntry.getKey()) {
                case "IsFoldableBlade" -> info.isEnableFoldBlade = (Boolean) mobEntry.getValue();
                default -> logUnkownEntry(mobEntry, "HeliFeatures");
            }
        }
    }

    @Override
    public @Nullable MCH_PlaneInfo parsePlane(AddonResourceLocation location, String filepath, List<String> lines,
                                              boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_PlaneInfo(location, filepath);
        mapToAircraft(info, root);
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "PlaneFeatures" -> parsePlaneFeatures((Map<String, Object>) entry.getValue(), info);
                case "Components" -> {
                    var components = (Map<String, List<Map<String, Object>>>) entry.getValue();
                    ComponentParser.parseComponentsPlane(components, info);
                }
            }
        }
        info.validate();
        return info;
    }

    @Override
    public @Nullable MCH_ShipInfo parseShip(AddonResourceLocation location, String filepath, List<String> lines,
                                            boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_ShipInfo(location, filepath);
        mapToAircraft(info, root);
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "ShipFeatures" -> parseShipFeatures((Map<String, Object>) entry.getValue(), info);
                case "Components" -> {
                    var components = (Map<String, List<Map<String, Object>>>) entry.getValue();
                    ComponentParser.parseComponentsShip(components, info);
                }
            }
        }
        info.validate();
        return info;
    }

    @Override
    public @Nullable MCH_TankInfo parseTank(AddonResourceLocation location, String filepath, List<String> lines,
                                            boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_TankInfo(location, filepath);
        mapToAircraft(info, root);
        if (root.containsKey("TankFeatures"))
            parseTankFeat((Map<String, Object>) root.get("TankFeatures"), info);
        info.validate();
        return info;
    }

    private void parseTankFeat(Map<String, Object> tankFeatures, MCH_TankInfo info) {
        for (Map.Entry<String, Object> entry : tankFeatures.entrySet()) {
            switch (entry.getKey()) {
                case "WeightType" -> {
                    try {
                        info.weightType = TankWeight
                                .valueOf(((String) entry.getValue()).toUpperCase(Locale.ROOT).trim()).ordinal();
                    } catch (RuntimeException e) {
                        throw new IllegalArgumentException("Invalid Weight type: " + entry.getValue() +
                                ". Allowed values: " +
                                Arrays.stream(TankWeight.values()).map(Enum::name).collect(Collectors.joining(", ")));
                    }
                }
                case "WeightedCenterZ", "CenterZ" -> getClamped(-1000F, 1000F, entry.getValue());
                default -> logUnkownEntry(entry, "TankFeatures");
            }

        }
    }

    @Override
    public @Nullable MCH_VehicleInfo parseVehicle(AddonResourceLocation location, String filepath, List<String> lines,
                                                  boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_VehicleInfo(location, filepath);
        mapToAircraft(info, root);
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "VehicleFeatures" -> parseVehicleFeatures((Map<String, Object>) entry.getValue(), info);
                case "Components" -> {
                    var components = (Map<String, List<Map<String, Object>>>) entry.getValue();
                    ComponentParser.parseComponentVehicle(components, info);
                }
            }
        }

        return info;
    }

    private void parseVehicleFeatures(Map<String, Object> value, MCH_VehicleInfo info) {
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "CanMove" -> info.isEnableMove = (Boolean) entry.getValue();
                case "CanRotate" -> info.isEnableRot = (Boolean) entry.getValue();
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }
    }

    @Override
    public @Nullable MCH_WeaponInfo parseWeapon(AddonResourceLocation location, String filepath, List<String> lines,
                                                boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_WeaponInfo(location, filepath);
        WeaponParser.parse(info, root);
        info.validate();
        return info;
    }

    @Override
    public @Nullable MCH_ThrowableInfo parseThrowable(AddonResourceLocation location, String filepath,
                                                      List<String> lines, boolean reload) throws Exception {
        Map<String, Object> root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var throwable = new MCH_ThrowableInfo(location, filepath);
        ThrowableParser.parse(throwable, root);
        throwable.validate();
        return throwable;
    }

    @Override
    public @Nullable MCH_Hud parseHud(AddonResourceLocation location, String filepath, List<String> lines,
                                      boolean reload) throws Exception {
        Object root = YAML_INSTANCE.load(lines.stream().collect(Collectors.joining("\n")));
        var info = new MCH_Hud(location, filepath);
        HUDParser.parse(info, root);
        info.validate();
        return info;
    }

    @Override
    public @Nullable MCH_ItemInfo parseItem(AddonResourceLocation location, String filepath, List<String> lines,
                                            boolean reload) throws Exception {
        return null;
    }

    @SuppressWarnings("unchecked")
    private void parsePlaneFeatures(Map<String, Object> map, MCH_PlaneInfo info) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "VariableSweepWing" -> info.isVariableSweepWing = (Boolean) entry.getValue();
                case "SweepWingSpeed" -> info.sweepWingSpeed = getClamped(5.0F, entry.getValue());
                case "EnableVtol" -> {
                    Object vtol = entry.getValue();
                    if (vtol instanceof Boolean)
                        info.isEnableVtol = (Boolean) entry.getValue();
                    else if (vtol instanceof Map<?, ?>) {
                        info.isEnableVtol = true;
                        for (Map.Entry<String, Object> vtolEntry : map.entrySet()) {
                            switch (vtolEntry.getKey()) {
                                case "IsDefault" -> info.isDefaultVtol = (Boolean) entry.getValue();
                                case "Yaw" -> info.vtolYaw = getClamped(1.0F, entry.getValue());
                                case "Pitch" -> info.vtolPitch = getClamped(0.01F, 1.0F, entry.getValue());
                            }
                        }
                    }

                }
                case "EnableAutoPilot" -> info.isEnableAutoPilot = (Boolean) entry.getValue();
                default -> logUnkownEntry(entry, "PlaneFeatures");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseShipFeatures(Map<String, Object> map, MCH_ShipInfo info) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "VariableSweepWing" -> info.isVariableSweepWing = (Boolean) entry.getValue();
                case "SweepWingSpeed" -> info.sweepWingSpeed = getClamped(5.0F, entry.getValue());
                case "EnableVtol" -> {
                    Object vtol = entry.getValue();
                    if (vtol instanceof Boolean)
                        info.isEnableVtol = (Boolean) entry.getValue();
                    else if (vtol instanceof Map<?, ?>) {
                        info.isEnableVtol = true;
                        for (Map.Entry<String, Object> vtolEntry : map.entrySet()) {
                            switch (vtolEntry.getKey()) {
                                case "IsDefault" -> info.isDefaultVtol = (Boolean) entry.getValue();
                                case "Yaw" -> info.vtolYaw = getClamped(1.0F, entry.getValue());
                                case "Pitch" -> info.vtolPitch = getClamped(0.01F, 1.0F, entry.getValue());
                            }
                        }
                    }

                }
                case "EnableAutoPilot" -> info.isEnableAutoPilot = (Boolean) entry.getValue();
                default -> logUnkownEntry(entry, "ShipFeatures");
            }
        }
    }

    @SuppressWarnings("unboxing")
    private void mapToAircraft(MCH_AircraftInfo info, Map<String, Object> root) {
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "DisplayName" -> {
                    Object nameObject = entry.getValue();
                    if (nameObject instanceof String name) info.displayName = name.trim();
                    else if (nameObject instanceof Map<?, ?>translationNames) {
                        var userNameMap = (Map<String, String>) translationNames;
                        if (userNameMap.containsKey("DEFAULT")) {
                            info.displayName = userNameMap.get("DEFAULT");
                            userNameMap.remove("DEFAULT");
                        }
                        info.displayNameLang = (HashMap<String, String>) userNameMap;
                    } else throw new ClassCastException();
                }
                case "Author" -> {
                    // Proposal: would allow content creators to put their signature
                }
                // Depricated on 1,12, around for 1.7 compat
                case "ItemID" -> {
                    info.itemID = (int) entry.getValue();
                }
                case "Category" -> {
                    if (entry.getValue() instanceof String category)
                        info.category = category.toUpperCase(Locale.ROOT).trim();
                    else if (entry.getValue() instanceof List<?>categories) {
                        List<String> list = (List<String>) categories;
                        info.category = list.stream().map(String::trim).map(String::toUpperCase)
                                .collect(Collectors.joining(","));
                    } else throw new RuntimeException();

                }
                case "Recepie" -> {
                    Map<String, Object> map = (Map<String, Object>) entry.getValue();
                    for (Map.Entry<String, Object> recMapEntry : map.entrySet()) {
                        switch (recMapEntry.getKey()) {
                            case "isShaped" -> info.isShapedRecipe = (Boolean) recMapEntry.getValue();
                            case "Pattern" -> info.recipeString = ((List<String>) recMapEntry.getValue()).stream()
                                    .map(String::toUpperCase).map(String::trim).collect(Collectors.toList());
                        }
                    }

                }
                case "CanRide" -> info.canRide = (Boolean) entry.getValue();
                case "RotorSpeed" -> {
                    info.rotorSpeed = getClamped(-10000.0F, 10000.0F, entry.getValue());
                    if (info.rotorSpeed > 0.01F) info.rotorSpeed -= 0.01F;
                    if (info.rotorSpeed < -0.01F) info.rotorSpeed += 0.01F; // Interesting
                }

                case "TurretPosition" -> info.turretPosition = parseVector(entry.getValue());
                case "CreativeOnly" -> info.creativeOnly = (Boolean) entry.getValue();
                case "Regeneration" -> info.regeneration = (Boolean) entry.getValue();
                case "Invulnerable" -> info.invulnerable = (Boolean) entry.getValue();
                case "MaxFuel" -> info.maxFuel = getClamped(100_000_000, entry.getValue());
                case "MaxHP" -> info.maxHp = getClamped(1, 1000_000_000, entry.getValue());
                case "Stealth" -> info.stealth = getClamped(1F, entry.getValue());
                case "FuelConsumption" -> info.fuelConsumption = getClamped(10_000.0F, entry.getValue());
                case "FuelSupplyRange" -> info.fuelSupplyRange = getClamped(1_000.0F, entry.getValue());
                case "AmmoSupplyRange" -> info.ammoSupplyRange = getClamped(1000, entry.getValue());
                case "RepairOtherVehicles" -> {
                    Map<String, Number> repairMap = (HashMap<String, Number>) entry.getValue();
                    if (repairMap.containsKey("Range"))
                        info.repairOtherVehiclesRange = getClamped(1_000.0F, (Object) repairMap.get("Range"));
                    if (repairMap.containsKey("Value"))
                        info.repairOtherVehiclesValue = getClamped(10_000_000, (Object) repairMap.get("Value"));
                }

                case "RadarType" -> {
                    if (entry.getValue() instanceof String data) {
                        try {
                            info.radarType = RadarType.valueOf(data);
                        } catch (IllegalArgumentException e) {
                            info.radarType = RadarType.MODERN_AA;
                        }
                    }
                }
                case "RWRType" -> {
                    if (entry.getValue() instanceof String data) {
                        try {
                            info.rwrType = RWRType.valueOf(data);
                        } catch (IllegalArgumentException e) {
                            info.rwrType = RWRType.DIGITAL;
                        }
                    }
                }
                case "NameOnModernAARadar" -> info.nameOnModernAARadar = ((String) entry.getValue()).trim();
                case "NameOnEarlyAARadar" -> info.nameOnEarlyAARadar = ((String) entry.getValue()).trim();
                case "NameOnModernASRadar" -> info.nameOnModernASRadar = ((String) entry.getValue()).trim();
                case "NameOnEarlyASRadar" -> info.nameOnEarlyASRadar = ((String) entry.getValue()).trim();
                case "ExplosionSizeByCrash" -> info.explosionSizeByCrash = getClamped(100, entry.getValue());
                case "ThrottleDownFactor" -> info.throttleDownFactor = getClamped(10F, entry.getValue());
                case "HUDType", "WeaponGroupType" -> {
                    // Unimplemented
                }

                case "Weapons" -> {
                    List<Map<String, Object>> weapons = (List<Map<String, Object>>) entry.getValue();
                    weapons.forEach(map -> parseWeapon(map, info));
                }

                case "GlobalUnmountPos" -> info.unmountPosition = parseVector(entry.getValue());
                case "PhysicalProperties" -> {
                    Map<String, Object> physicalProperties = (Map<String, Object>) entry.getValue();
                    physicalProperties.entrySet().forEach((physEntry) -> parsePhisProperties(physEntry, info));
                }
                case "Render" -> {
                    Map<String, Object> renderProperties = (Map<String, Object>) entry.getValue();
                    parseRender(renderProperties, info);
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
                case "Racks" -> {
                    int seatCount = 0;
                    if (root.containsKey("Seats")) {
                        seatCount = ((List<?>) root.get("Seats")).size();
                    }
                    List<Map<String, Object>> racks = (List<Map<String, Object>>) entry.getValue();
                    for (Map<String, Object> rack : racks) {
                        parseSeatRackInfo(rack, info, seatCount, racks.size());
                    }
                }

                case "RideRack" -> {
                    parseRideRacks((Map<String, Integer>) entry.getValue(), info);
                }

                case "Wheels" -> {
                    Map<String, Object> wheel = (Map<String, Object>) entry.getValue();
                    parseWheels(wheel, info);
                }
                case "Components" -> {
                    var components = (Map<String, List<Map<String, Object>>>) entry.getValue();
                    ComponentParser.parseComponents(components, info);
                }
                case "Sound" -> {
                    Map<String, Object> soundSettings = (Map<String, Object>) entry.getValue();
                    parseSound(soundSettings, info);
                }

                case "Seats" -> {
                    List<Map<String, Object>> seatList = (List<Map<String, Object>>) entry.getValue();
                    seatList.stream().forEachOrdered(seat -> parseSeatInfo(seat, info, seatList.size()));

                }

                case "Uav" -> {
                    Map<String, Object> uav = (Map<String, Object>) entry.getValue();
                    parseUAV(uav, info);
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

                        maxAbsXZ = Math.max(maxAbsXZ,
                                Math.max(Math.max(Math.abs((float) aabb.maxX), Math.abs((float) aabb.minX)),
                                        Math.max(Math.abs((float) aabb.maxZ), Math.abs((float) aabb.minZ))));

                        zMin = Math.min(zMin, (float) aabb.minZ);
                        zMax = Math.max(zMax, (float) aabb.maxZ);
                    }
                    info.markerHeight = maxY;
                    info.markerWidth = maxAbsXZ / 2.0F;
                    info.bbZmin = zMin;
                    info.bbZmax = zMax;
                }

                case "PlaneFeatures", "TankFeatures", "HeliFeatures", "VehicleFeatures" -> {}
                default -> logUnkownEntry(entry, "AircraftInfo");
            }
        }
    }

    private void parseWheels(Map<String, Object> wheel, MCH_AircraftInfo info) {
        for (Map.Entry<String, Object> entry : wheel.entrySet()) {
            switch (entry.getKey()) {
                case "Hitboxes" -> {
                    List<Map<String, Object>> wheels = (List<Map<String, Object>>) entry.getValue();
                    info.wheels.clear();
                    info.wheels.addAll(wheels.stream().map(this::parseWheel)
                            .sorted((o1, o2) -> o1.pos.z > o2.pos.z ? -1 : 1).collect(Collectors.toList()));
                }
                case "WheelRotation" -> info.partWheelRot = getClamped(-10000.0F, 10000.0F, entry.getValue());
                case "TrackRotation" -> info.trackRollerRot = getClamped(-10000.0F, 10000.0F, entry.getValue());

                default -> logUnkownEntry(entry, "Wheels");
            }
        }
    }

    private void parseUAV(Map<String, Object> uav, MCH_AircraftInfo info) {
        for (Map.Entry<String, Object> entry : uav.entrySet()) {
            switch (entry.getKey()) {
                case "IsUav" -> info.isUAV = (Boolean) entry.getValue();
                case "IsSmallUav" -> info.isSmallUAV = (Boolean) entry.getValue();
                case "IsNewUav" -> info.isNewUAV = (Boolean) entry.getValue();
                case "IsTargetDrone" -> info.isTargetDrone = (Boolean) entry.getValue();
                default -> logUnkownEntry(entry, "Uav");
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
                        type = MCH_BoundingBox.EnumBoundingBoxType
                                .valueOf(((String) entry.getValue()).toUpperCase(Locale.ROOT).trim());
                    } catch (RuntimeException r) {
                        throw new IllegalArgumentException("Invalid bounding box type: " + entry.getValue() +
                                ". Allowed values: " + Arrays.stream(MCH_BoundingBox.EnumBoundingBoxType.values())
                                        .map(Enum::name).collect(Collectors.joining(", ")));
                    }
                }

                default -> logUnkownEntry(entry, "BoundingBox");
            }
        }

        if (pos == null) throw new IllegalArgumentException("Bounding box must have a position!");
        if (size == null) throw new IllegalArgumentException("Bounding box must have a size!");

        var parsedBox = new MCH_BoundingBox(pos.x, pos.y, pos.z, (float) size.x, (float) size.y, (float) size.z,
                damageFact);
        parsedBox.setBoundingBoxType(type);
        info.extraBoundingBox.add(parsedBox);
    }

    private void parseSound(Map<String, Object> soundSettings, MCH_AircraftInfo info) {
        for (Map.Entry<String, Object> entry : soundSettings.entrySet()) {
            switch (entry.getKey()) {
                case "MoveSound" -> info.soundMove = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "Volume", "Vol" -> info.soundVolume = getClamped(10F, entry.getValue());
                case "Pitch" -> info.soundPitch = getClamped(1F, 10F, entry.getValue());
                case "Range" -> info.soundRange = getClamped(1F, 1000.0F, entry.getValue());
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
                case "SmoothShading" -> info.smoothShading = (Boolean) entry.getValue();
                case "HideRiders" -> info.hideEntity = (Boolean) entry.getValue();
                case "ModelWidth" -> info.entityWidth = ((Number) entry.getValue()).floatValue();
                case "ModelHeight" -> info.entityHeight = ((Number) entry.getValue()).floatValue();
                case "ModelPitch" -> info.entityPitch = ((Number) entry.getValue()).floatValue();
                case "ModelRoll" -> info.entityRoll = ((Number) entry.getValue()).floatValue();
                case "ParticleScale" -> info.particlesScale = getClamped(50f, entry.getValue());
                case "OneProbeScale" -> info.oneProbeScale = ((Number) entry.getValue()).floatValue();
                case "EnableSeaSurfaceParticle" -> info.enableSeaSurfaceParticle = (Boolean) entry.getValue();
                case "SplashParticles" -> {
                    List<Map<String, Object>> splashParticles = (List<Map<String, Object>>) entry.getValue();
                    splashParticles.stream().map((this::parseParticleSplash)).forEach(info.particleSplashs::add);
                }
                default -> logUnkownEntry(entry, "Render");
            }

        }
    }

    private void parsePhisProperties(Map.Entry<String, Object> entry, MCH_AircraftInfo info) {
        switch (entry.getKey()) {
            case "Speed" -> info.speed = getClamped(info.getMaxSpeed(), entry.getValue());
            case "CanFloat" -> info.isFloat = (Boolean) entry.getValue();
            case "FloatOffset" -> info.floatOffset = -((Number) entry.getValue()).floatValue();
            case "MotionFactor" -> info.motionFactor = getClamped(1F, entry.getValue());
            case "Gravity" -> info.gravity = getClamped(-50F, 50F, entry.getValue());
            case "RotationSnapValue" -> info.autoPilotRot = getClamped(-5F, 5F, entry.getValue());
            case "GravityInWater" -> info.gravityInWater = getClamped(-50F, 50F, entry.getValue());
            case "StepHeight" -> info.stepHeight = getClamped(0F, 1000F, entry.getValue());
            case "CanRotOnGround" -> info.canRotOnGround = (Boolean) entry.getValue();
            case "CanMoveOnGround" -> info.canMoveOnGround = (Boolean) entry.getValue();
            case "OnGroundPitch" -> info.onGroundPitch = -getClamped(-90F, 90F, entry.getValue());
            case "PivotTurnThrottle" -> info.pivotTurnThrottle = getClamped(1F, entry.getValue());

            case "Mobility" -> {
                Map<String, Object> mob = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> mobEntry : mob.entrySet()) {
                    switch (mobEntry.getKey()) {
                        case "Yaw" -> info.mobilityYaw = getClamped(100F, mobEntry.getValue());
                        case "Pitch" -> info.mobilityPitch = getClamped(100F, mobEntry.getValue());
                        case "Roll" -> info.mobilityRoll = getClamped(100F, mobEntry.getValue());
                        case "YawOnGround" -> info.mobilityYawOnGround = getClamped(100F, mobEntry.getValue());
                        default -> logUnkownEntry(mobEntry, "Mobility");
                    }
                }
            }

            case "GroundPitchFactors" -> {
                Map<String, Object> factors = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> rotEntry : factors.entrySet()) {
                    switch (rotEntry.getKey()) {
                        case "Pitch" -> info.onGroundPitchFactor = getClamped(0F, 180F, rotEntry.getValue());
                        case "Roll" -> info.onGroundRollFactor = getClamped(0F, 180F, rotEntry.getValue());
                        default -> logUnkownEntry(rotEntry, "GroundPitchFactors");
                    }
                }
            }
            case "BodySize" -> {
                Map<String, Object> factors = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> bodyEntry : factors.entrySet()) {
                    switch (bodyEntry.getKey()) {
                        case "Height" -> info.bodyHeight = getClamped(0.1F, 1000F, bodyEntry.getValue());
                        case "Width" -> info.bodyWidth = getClamped(0.1F, 1000F, bodyEntry.getValue());
                        default -> logUnkownEntry(bodyEntry, "GroundPitchFactors");
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
                            if (pitchMap.containsKey("Min"))
                                info.minRotationPitch = getClamped(info.getMinRotationPitch(), 0F, pitchMap.get("Min"));
                            if (pitchMap.containsKey("Max"))
                                info.maxRotationPitch = getClamped(0F, info.getMaxRotationPitch(), pitchMap.get("Max"));
                        }
                        case "Roll" -> {
                            Map<String, Object> rollMap = (Map<String, Object>) rotEntry.getValue();
                            if (rollMap.containsKey("Min"))
                                info.minRotationRoll = getClamped(info.getMinRotationRoll(), 0F, rollMap.get("Min"));
                            if (rollMap.containsKey("Max"))
                                info.maxRotationRoll = getClamped(0F, info.getMaxRotationRoll(), rollMap.get("Max"));
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
            case "ArmorDamageFactor" -> info.armorDamageFactor = getClamped(10_000F, entry.getValue());
            case "ArmorMinDamage" -> info.armorMinDamage = getClamped(1_000_000F, entry.getValue());
            case "ArmorMaxDamage" -> info.armorMaxDamage = getClamped(1_000_000F, entry.getValue());
            case "DamageFactor" -> info.damageFactor = getClamped(1F, entry.getValue());
            case "SubmergedDamageHeight" -> info.submergedDamageHeight = getClamped(-1000F, 1000F, entry.getValue());
            default -> logUnkownEntry(entry, "Armor");
        }
    }

    @SuppressWarnings("unboxing")
    private void parseAircraftFeatures(Map<String, Object> map, MCH_AircraftInfo info) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "GunnerMode" -> info.isEnableGunnerMode = ((Boolean) entry.getValue()).booleanValue();
                case "InventorySize" -> info.inventorySize = getClamped(54, entry.getValue()); // FIXME: Capped due to
                                                                                               // inventory code being
                                                                                               // fucking ass
                case "NightVision" -> info.isEnableNightVision = ((Boolean) entry.getValue()).booleanValue();
                case "EntityRadar" -> info.isEnableEntityRadar = ((Boolean) entry.getValue()).booleanValue();
                case "CanReverse" -> info.enableBack = ((Boolean) entry.getValue()).booleanValue();
                case "CanRotateOnGround", "CanRotOnGround" -> info.canRotOnGround = ((Boolean) entry.getValue())
                        .booleanValue();
                case "ConcurrentGunner" -> info.isEnableConcurrentGunnerMode = ((Boolean) entry.getValue())
                        .booleanValue();
                case "EjectionSeat" -> info.isEnableEjectionSeat = ((Boolean) entry.getValue()).booleanValue();
                case "ThrottleUpDown" -> info.throttleUpDown = getClamped(3F, entry.getValue());
                case "ThrottleUpDownEntity" -> info.throttleUpDownOnEntity = getClamped(100_000F, entry.getValue());
                case "Parachuting" -> parseParachuting(entry, info);
                case "Flare" -> info.flare = parseFlare((Map<String, Object>) entry.getValue());
                default -> logUnkownEntry(entry, "AircraftFeatures");
            }
        }
    }

    private void parseParachuting(Map.Entry<String, Object> entry, MCH_AircraftInfo info) {
        // I have no idea what im doing
        Object value = entry.getValue();
        if (value instanceof Boolean bool) {
            info.isEnableParachuting = bool;
            return;
        }
        if (value instanceof Map<?, ?>parachuteMapRaw) {
            info.isEnableParachuting = true;
            for (Map.Entry<String, Object> mobEntry : ((Map<String, Object>) parachuteMapRaw).entrySet()) {
                switch (mobEntry.getKey()) {
                    case "Pos", "Position" -> info.mobDropOption.pos = parseVector(mobEntry.getValue());
                    case "Interval" -> info.mobDropOption.interval = ((Number) mobEntry.getValue()).intValue();
                    default -> logUnkownEntry(mobEntry, "Parachuting");
                }
            }
            return;
        }
        throw new IllegalArgumentException("Parachuting type must be a boolean or map, got: " + value.getClass());
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
                    } else if (entry.getValue() instanceof String[]typeArray) {
                        typeStrings.addAll(Arrays.asList(typeArray));
                    } else if (entry.getValue() instanceof List<?>typeList) {
                        for (Object obj : typeList) {
                            if (obj instanceof String s) typeStrings.add(s);
                            else
                                throw new IllegalArgumentException(
                                        "Flare type must be a string, got: " + obj.getClass());
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported type value: " + entry.getValue().getClass());
                    }

                    for (String typeRaw : typeStrings) {
                        try {
                            FlareType type = FlareType.valueOf(typeRaw.trim().toUpperCase(Locale.ROOT));
                            flareTypes.add(type);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid flare type: " + typeRaw + ". Allowed values: " +
                                    Arrays.stream(FlareType.values()).map(Enum::name)
                                            .collect(Collectors.joining(", ")));
                        }
                    }
                }

                default -> logUnkownEntry(entry, "Flare");
            }
        }

        if (flareTypes.isEmpty()) {
            flareTypes.add(FlareType.NONE);
        }

        return new Flare(pos,
                flareTypes.stream().map(FlareType::getLegacyMapping).mapToInt(Integer::intValue).toArray());
    }

    @SuppressWarnings("unboxing")
    private void parseGlobalCamera(MCH_AircraftInfo info, Map.Entry<String, Object> entry) {
        switch (entry.getKey()) {
            case "ThirdPersonDist" -> info.thirdPersonDist = getClamped(4f, 100f, entry.getValue());
            case "Zoom", "CameraZoom" -> info.cameraZoom = getClamped(1, 10, entry.getValue());
            case "DefaultFreeLook" -> info.defaultFreelook = ((Boolean) entry.getValue()).booleanValue();
            case "RotationSpeed" -> info.cameraRotationSpeed = getClamped(10000.0F, entry.getValue());
            case "AlwaysCameraView" -> info.alwaysCameraView = (Boolean) entry.getValue();
            case "Pos", "Positons" -> {
                List<Map<String, Object>> cameraList = (List<Map<String, Object>>) entry.getValue();
                info.cameraPosition.addAll(cameraList.stream().map(camera -> parseCameraPosition(camera, info))
                        .collect(Collectors.toList()));
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

    private void parseRideRacks(Map<String, Integer> map, MCH_AircraftInfo info) {
        map.entrySet().forEach(stringIntegerEntry -> {
            if (stringIntegerEntry.getKey().isEmpty())
                throw new IllegalArgumentException("RideRack vehicle entry cannot be empty!");
            if (stringIntegerEntry.getValue() < 0)
                throw new IllegalArgumentException("RideRack rackID cannot be negative!");
            info.rideRacks.add(new RideRack(stringIntegerEntry.getKey(), stringIntegerEntry.getValue()));
        });
    }

    private void parseSeatRackInfo(Map<String, Object> map, MCH_AircraftInfo info, int seatCount, int rackCount) {
        Vec3d position = null;
        CameraPosition cameraPos = null;
        String[] entityNames = new String[0];
        float range = 0f;
        float openParaAlt = 0f;
        float yaw = 0f;
        float pitch = 0f;
        boolean rotSeat = false;
        List<Integer> exclusionList = null;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> position = parseVector(entry.getValue());
                case "Camera", "Cam" -> cameraPos = parseCameraPosition((Map<String, Object>) entry.getValue());
                case "Names", "Name" -> {
                    Object values = entry.getValue();
                    if (values instanceof List<?>list) entityNames = list.toArray(new String[0]);
                    else if (values instanceof String[]array) entityNames = array;
                    else if (values instanceof String name) entityNames = new String[] { name };
                    else throw new IllegalArgumentException("Rack name must be a string, array or list!");
                }
                case "Range" -> range = ((Number) entry.getValue()).floatValue();
                case "OpenParaAlt" -> openParaAlt = ((Number) entry.getValue()).floatValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "RotSeat" -> rotSeat = ((Boolean) entry.getValue()).booleanValue();
                case "ExcludeWith" -> {
                    exclusionList = new ArrayList<>();
                    var exclusionMap = (Map<String, List<Integer>>) entry.getValue();
                    var seatYaml = exclusionMap.get("Seats");
                    if (seatYaml != null) {
                        for (Integer n : seatYaml) {
                            if (n == null) continue;
                            int idx0 = n - 1; // 1-based -> 0-based seat
                            if (idx0 >= 0) exclusionList.add(idx0);
                        }
                    }
                    var rackYaml = exclusionMap.get("Racks");
                    if (rackYaml != null) {
                        for (Integer n : rackYaml) {
                            if (n == null) continue;
                            int idx0 = seatCount + (n - 1); // 1-based -> 0-based combined
                            if (idx0 >= seatCount) exclusionList.add(idx0);
                        }
                    }
                }

                default -> logUnkownEntry(entry, "Racks");
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat rack must have a position!");
        }
        if (cameraPos == null) {
            throw new IllegalArgumentException("Seat rack must have a camera position!");
        }

        info.entityRackList.add(new MCH_SeatRackInfo(entityNames, position.x, position.y, position.z, cameraPos, range,
                openParaAlt, yaw, pitch, rotSeat));
        final int rackIndex0 = info.entityRackList.size() - 1;

        if (exclusionList != null) {
            for (Integer t : exclusionList) {
                if (t == null) continue;
                info.exclusionSeatList.add(new Integer[] { seatCount + rackIndex0, t });
            }
        }
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
                case "Count" -> num = getClamped(1, 100, entry.getValue());
                case "Size" -> size = ((Number) entry.getValue()).floatValue();
                case "Accel", "Acceleration" -> acceleration = ((Number) entry.getValue()).floatValue();
                case "Age" -> age = getClamped(1, 100_000, entry.getValue());
                case "Motion" -> motionY = ((Number) entry.getValue()).floatValue();
                case "Gravity" -> gravity = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "SplashParticles");
            }
        }

        if (pos == null) throw new IllegalArgumentException("Splash particle must have a position!");

        return new ParticleSplash(num, acceleration, size, pos, age, motionY, gravity);
    }

    private CameraPosition parseCameraPosition(Map<String, Object> map, MCH_AircraftInfo info) {
        Vec3d pos = Vec3d.ZERO;
        boolean fixRot = false;
        float yaw = 0;
        float pitch = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "FixedRot" -> fixRot = (Boolean) entry.getValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                default -> logUnkownEntry(entry, "CameraPosition");
            }
        }

        return new CameraPosition(pos, fixRot, yaw, pitch);
    }

    private CameraPosition parseCameraPosition(Map<String, Object> map) {
        return parseCameraPosition(map, null);
    }

    private void parseWeapon(Map<String, Object> map, MCH_AircraftInfo info) {
        String type = null;
        Vec3d pos = Vec3d.ZERO;
        float yaw = 0.0F;
        float pitch = 0.0F;
        boolean canUsePilot = true;
        int seatID = 0;
        float dfy = 0.0F;
        float mny = 0.0F;
        float mxy = 0.0F;
        float mnp = 0.0F;
        float mxp = 0.0F;
        boolean turret = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Type", "WeaponType" -> type = ((String) entry.getValue()).toLowerCase();
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "CanUsePilot" -> canUsePilot = (Boolean) entry.getValue();
                case "SeatID" -> seatID = ((Number) entry.getValue()).intValue();
                case "DefaultYaw" -> dfy = ((Number) entry.getValue()).floatValue();
                case "MinYaw" -> mny = ((Number) entry.getValue()).floatValue();
                case "MaxYaw" -> mxy = ((Number) entry.getValue()).floatValue();
                case "MinPitch" -> mnp = ((Number) entry.getValue()).floatValue();
                case "MaxPitch" -> mxp = ((Number) entry.getValue()).floatValue();
                case "Turret" -> turret = (Boolean) entry.getValue();
                default -> logUnkownEntry(entry, "Weapon");
            }
        }

        if (type == null || !MCH_WeaponInfoManager.contains(type)) {
            throw new IllegalArgumentException("Weapon " + type + " type is unknown or missing!");
        }

        if (seatID <= 0) {
            canUsePilot = true;
        }

        dfy = MathHelper.wrapDegrees(dfy);
        seatID = Math.max(0, seatID - 1);

        MCH_AircraftInfo.Weapon weapon = new MCH_AircraftInfo.Weapon(info, (float) pos.x, (float) pos.y, (float) pos.z,
                yaw, pitch, canUsePilot, seatID, dfy, mny, mxy, mnp, mxp, turret);

        WeaponSet set = info.getOrCreateWeaponSet(type);
        set.weapons.add(weapon);
    }

    @SuppressWarnings("unboxing")
    private void parseSeatInfo(Map<String, Object> map, MCH_AircraftInfo info, int seatCount) {
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
        String hudName = "none";
        List<Integer> exclusionList = null; // 0-based

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
                case "Hud" -> hudName = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                case "Camera", "Cam" -> cameraPos = parseCameraPosition((Map<String, Object>) entry.getValue());
                case "ExcludeWith" -> {
                    exclusionList = new ArrayList<>();
                    var exclusionMap = (Map<String, List<Integer>>) entry.getValue();

                    var seatYaml = exclusionMap.get("Seats");
                    if (seatYaml != null) {
                        for (Integer n : seatYaml) {
                            if (n == null) continue;
                            int idx0 = n - 1; // 1-based -> 0-based seat
                            if (idx0 >= 0) exclusionList.add(idx0);
                        }
                    }
                    var rackYaml = exclusionMap.get("Racks");
                    if (rackYaml != null) {
                        for (Integer n : rackYaml) {
                            if (n == null) continue;
                            int idx0 = seatCount + (n - 1); // 1-based -> 0-based combined
                            if (idx0 >= seatCount) exclusionList.add(idx0);
                        }
                    }
                }

                default -> logUnkownEntry(entry, "Seats");
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat must have a position!");
        }

        info.seatList.add(new MCH_SeatInfo(position, isGunner, cameraPos, invertCameraPos, canSwitchGunner,
                hasFixedRotation, fixedYaw, fixedPitch, minPitch, maxPitch, rotatableSeat));

        if (MCH_MOD.proxy.isRemote()) {
            info.hudList.add(MCH_HudManager.get(hudName) != null ? MCH_HudManager.get(hudName) : MCH_Hud.NoDisp);
        }
        final int seatIndex0 = info.seatList.size() - 1;

        if (exclusionList != null) {
            for (Integer t : exclusionList) {
                if (t == null) continue;
                info.exclusionSeatList.add(new Integer[] { seatIndex0, t });
            }
        }
    }

    public static enum TankWeight {
        UNKNOWN,
        CAR,
        TANK
    }

    public static enum FlareType {

        NONE(0),
        NORMAL(1),
        LARGE_AIRCRAFT(2),
        SIDE(3),
        FRONT(4),
        DOWN(5),
        SMOKE_LAUNCHER(10);

        final byte legacyMapping;

        FlareType(int legacyMapping) {
            this.legacyMapping = (byte) legacyMapping;
        }

        public int getLegacyMapping() {
            return legacyMapping;
        }
    }
}
