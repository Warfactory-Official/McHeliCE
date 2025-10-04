package com.norwood.mcheli.helper.info;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.aircraft.MCH_SeatRackInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
                case "CanRide" -> info.canRide = (boolean) entry.getValue();
                case "CreativeOnly" -> info.creativeOnly = (boolean) entry.getValue();
                case "Invulnerable" -> info.invulnerable = (boolean) entry.getValue();
                case "MaxFuel" -> info.maxFuel = getClamped(0, 100_000_000, (Number) entry.getValue());
                case "FuelConsumption" -> info.fuelConsumption = getClamped(0.0F, 10_000.0F, (Number) entry.getValue());
                case "FuelSupplyRange" -> info.fuelSupplyRange = getClamped(0F, 1_1000.0F, (Number) entry.getValue());
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
                    if (entry.getValue() instanceof String data) info.throttleDownFactor = info.toFloat(data, 0, 10);
                }
                case "HUDType", "WeaponGroupType" -> {
                    //Unimplemented
                }
                case "Textures" -> {
                    List<String> textures = (List<String>) entry.getValue();
                    textures.stream().map(String::trim).forEach(info::addTextureName);
                }
                case "ParticleScale" -> info.particlesScale = getClamped(50f, (Number) entry.getValue());
                case "EnableSeaSurfaceParticle" -> info.enableSeaSurfaceParticle = (boolean) entry.getValue();
                case "SplashParticles" -> {
                    List<Map<String, Object>> splashParticles = (List<Map<String, Object>>) entry.getValue();
                    splashParticles.stream().map((this::parseParticleSplash)).forEach(info.particleSplashs::add);
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
                case "Racks" -> {
                    List<Map<String, Object>> racks = (List<Map<String, Object>>) entry.getValue();
                    racks.stream().map(this::parseRacks).forEach((rack) -> {
                        if (rack instanceof MCH_SeatRackInfo r)
                            info.entityRackList.add(r);
                        else
                            info.rideRacks.add((MCH_AircraftInfo.RideRack) rack);


                    });
                }


            }
        }

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
        MCH_AircraftInfo.CameraPosition cameraPos = null;
        String[] entityNames = new String[0];
        float range = 0f;
        float openParaAlt = 0f;
        float yaw = 0f;
        float pitch = 0f;
        boolean rotSeat = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> position = parseVector((Object[]) entry.getValue());
                case "camera", "cam" -> cameraPos = parseCamera((Map<String, Object>) entry.getValue());
                case "names", "name" -> {
                    Object values  = entry.getValue();
                    if(values instanceof List<?> list)
                        entityNames = list.toArray(new String[0]);
                    else if(values instanceof String[] array)
                        entityNames =  array;
                    else if(values instanceof String name)
                        entityNames = new String[]{name}  ;
                    else throw new IllegalArgumentException("Rack name must be a string, array or list!");
                }
                case "range" -> range = ((Number) entry.getValue()).floatValue();
                case "openParaAlt" -> openParaAlt = ((Number) entry.getValue()).floatValue();
                case "yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "rotSeat" -> rotSeat = (Boolean) entry.getValue();
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat rack must have a position!");
        }
        if (cameraPos == null) {
            throw new IllegalArgumentException("Seat rack must have a camera position!");
        }

        return new MCH_SeatRackInfo(
                entityNames,
                position.x, position.y, position.z,
                cameraPos,
                range,
                openParaAlt,
                yaw,
                pitch,
                rotSeat
        );
    }



    private MCH_AircraftInfo.RideRack parseRidingRack(Map<String, Object> map) {
        int rackID = -1;//FUCK IDs
        String name = "";


        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "id" -> rackID = getClamped(1, 10_000, (Number) entry.getValue());
                case "name" -> name = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
            }
        }
        if (rackID == -1 || name.isEmpty())
            throw new IllegalArgumentException("Name nor ID can be empty!");

        return new MCH_AircraftInfo.RideRack(name, rackID);
    }

    private MCH_AircraftInfo.SearchLight parseSearchLights(Map<String, Object> map) {
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
                case "fixedDirection" -> fixedDirection = (boolean) entry.getValue();
                case "steering" -> steering = (boolean) entry.getValue();
                case "pos" -> pos = parseVector((Object[]) entry.getValue());
                case "colorStart" -> colorStart = parseHexColor((String) entry.getValue());
                case "colorEnd" -> colorEnd = parseHexColor((String) entry.getValue());
                case "height" -> height = ((Number) entry.getValue()).floatValue();
                case "width" -> width = ((Number) entry.getValue()).floatValue();
                case "yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "stRot" -> stRot = ((Number) entry.getValue()).floatValue();
            }
        }

        if (pos == null) {
            throw new IllegalArgumentException("SearchLight must have a position!");
        }

        return new MCH_AircraftInfo.SearchLight(
                pos, colorStart, colorEnd, height, width,
                fixedDirection, yaw, pitch, steering, stRot
        );


    }

    private MCH_AircraftInfo.RepellingHook parseHook(Map<String, Object> map) {
        Vec3d pos = null;
        int interval = 0;


        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> pos = parseVector((Object[]) entry.getValue());
                case "interval" -> interval = ((Number) entry.getValue()).intValue();
            }
        }

        if (pos == null) throw new IllegalArgumentException("Repelling hook must have a position!");
        return new MCH_AircraftInfo.RepellingHook(pos, interval);
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


    private MCH_AircraftInfo.ParticleSplash parseParticleSplash(Map<String, Object> map) {

        Vec3d pos = null;
        int num = 2;
        float acceleration = 1f;
        float size = 2f;
        int age = 80;
        float motionY = 0.01f;
        float gravity = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> pos = parseVector((Object[]) entry.getValue());
                case "count" -> num = getClamped(1, 100, (Number) entry.getValue());
                case "size" -> size = ((Number) entry.getValue()).floatValue();
                case "accel" -> acceleration = ((Number) entry.getValue()).floatValue();
                case "age" -> age = getClamped(1, 100_000, (Number) entry.getValue());
                case "motion" -> motionY = ((Number) entry.getValue()).floatValue();
                case "gravity" -> gravity = ((Number) entry.getValue()).floatValue();
            }
        }

        if (pos == null) throw new IllegalArgumentException("Splash particle must have a position!");

        return new MCH_AircraftInfo.ParticleSplash(num, size, acceleration, pos, age, motionY, gravity);
    }

    private MCH_AircraftInfo.CameraPosition parseCamera(Map<String, Object> map) {
        Vec3d pos = Vec3d.ZERO;
        boolean fixRot = false;
        float yaw = 0;
        float pitch = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> pos = parseVector((Object[]) entry.getValue());
                case "fixedRot" -> fixRot = (boolean) entry.getValue();
                case "yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "pitch" -> pitch = ((Number) entry.getValue()).floatValue();
            }
        }

        return new MCH_AircraftInfo.CameraPosition(pos, fixRot, yaw, pitch);
    }

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
        MCH_AircraftInfo.CameraPosition cameraPos = null;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> position = parseVector((Object[]) entry.getValue());
                case "gunner" -> isGunner = (Boolean) entry.getValue();
                case "switchGunner" -> canSwitchGunner = (Boolean) entry.getValue();
                case "fixRot" -> hasFixedRotation = (Boolean) entry.getValue();
                case "fixYaw" -> fixedYaw = ((Number) entry.getValue()).floatValue();
                case "fixPitch" -> fixedPitch = ((Number) entry.getValue()).floatValue();
                case "minPitch" -> minPitch = ((Number) entry.getValue()).floatValue();
                case "maxPitch" -> maxPitch = ((Number) entry.getValue()).floatValue();
                case "rotSeat" -> rotatableSeat = (Boolean) entry.getValue();
                case "invCamPos" -> invertCameraPos = (Boolean) entry.getValue();
                case "camera", "cam" -> cameraPos = parseCamera((Map<String, Object>) entry.getValue());
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Seat must have a position!");
        }

        return new MCH_SeatInfo(
                position,
                isGunner,
                cameraPos,
                invertCameraPos,
                canSwitchGunner,
                hasFixedRotation,
                fixedYaw,
                fixedPitch,
                minPitch,
                maxPitch,
                rotatableSeat
        );
    }


    private Vec3d parseVector(Object[] vector) {
        if (vector instanceof Number[] numbers) {
            if (numbers.length != 3)
                throw new IllegalArgumentException("The vector array must contain princely 3 numbers!");
            return new Vec3d(numbers[0].doubleValue(), numbers[1].doubleValue(), numbers[2].doubleValue());
        } else throw new IllegalArgumentException("Vector must be an array of Numbers!");


    }

    private MCH_AircraftInfo.Hatch parseHatch(MCH_AircraftInfo data, Map<String, Object> map) {
        Vec3d position = null;
        Vec3d rotation = null;
        float maxRotation = 0f;
        String partName = "light_hatch" + data.lightHatchList.size();
        boolean isSliding = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "pos" -> position = parseVector((Object[]) entry.getValue());
                case "rot" -> rotation = parseVector((Object[]) entry.getValue());
                case "name" -> partName = (String) entry.getValue();
                case "isSliding" -> isSliding = (Boolean) entry.getValue();
            }
        }

        if (position == null) {
            throw new IllegalArgumentException("Hatch must have a position!");
        }
        if (rotation == null) {
            throw new IllegalArgumentException("Hatch must have a rotation!");
        }

        return new MCH_AircraftInfo.Hatch(
                position, rotation,
                partName,
                maxRotation,
                isSliding
        );
    }


    public static enum RACK_TYPE {//Could be bool, but this makes it more extensible
        NORMAL, RIDING
    }
}
