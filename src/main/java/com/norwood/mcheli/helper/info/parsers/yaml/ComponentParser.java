package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.*;

@SuppressWarnings("unchecked")
public class ComponentParser {

    private ComponentParser() {}

    public static void parseComponentsHeli(Map<String, List<Map<String, Object>>> components, MCH_HeliInfo info) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
            String type = entry.getKey();
            var componentList = entry.getValue();
            switch (type) {
                case "HeliRotor" -> componentList.stream()
                        .map(component -> parseDrawnPart("blade", component, drawnPart -> {
                            int bladeNum = 1;
                            int bladeRot = 0;
                            boolean haveFoldFunc = false;
                            boolean oldRenderMethod = false;

                            for (Map.Entry<String, Object> bladeEntry : component.entrySet()) {
                                switch (bladeEntry.getKey()) {
                                    case "Count", "BladeCount" -> bladeNum = ((Number) bladeEntry.getValue())
                                            .intValue();
                                    case "BladeRotation", "BladeRot" -> bladeRot = ((Number) bladeEntry.getValue())
                                            .intValue();
                                    case "CanFold" -> haveFoldFunc = (Boolean) bladeEntry.getValue();
                                    case "OldRenderer" -> oldRenderMethod = (Boolean) bladeEntry.getValue();
                                }
                            }

                            return new MCH_HeliInfo.Rotor(drawnPart, bladeNum, bladeRot, haveFoldFunc, oldRenderMethod);
                        }, info.rotorList, new HashSet<>(Arrays.asList("Count", "BladeCount", "BladeRotation",
                                "BladeRot", "CanFold", "OldRenderer"))))
                        .forEachOrdered(info.rotorList::add);
            }
        }
    }

    public static void parseComponents(Map<String, List<Map<String, Object>>> components, MCH_AircraftInfo info) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
            String type = entry.getKey();
            var componentList = entry.getValue();
            switch (type) {
                case "Camera" -> componentList.stream()
                        .map(component -> parseDrawnPart(
                                MCH_AircraftInfo.Camera.class,
                                component,
                                drawnPart -> new MCH_AircraftInfo.Camera(drawnPart,
                                        (Boolean) component.getOrDefault("YawSync", true),
                                        (Boolean) component.getOrDefault("PitchSync", false)),
                                info.cameraList,
                                new HashSet<>(Arrays.asList("YawSync", "PitchSync"))))
                        .forEachOrdered(info.cameraList::add);

                case "Canopy" -> componentList.stream()
                        .map(component -> parseDrawnPart(MCH_AircraftInfo.Canopy.class, component,
                                drawnPart -> new MCH_AircraftInfo.Canopy(drawnPart,
                                        getClamped(-180F, 180F, component.getOrDefault("MaxRotation", 90F)),
                                        (Boolean) component.getOrDefault("IsSliding", false)),
                                info.canopyList, new HashSet<>(Arrays.asList("MaxRotation", "IsSliding"))))
                        .forEachOrdered(info.canopyList::add);

                case "Hatch" -> componentList.stream()
                        .map(component -> parseDrawnPart(MCH_AircraftInfo.Hatch.class, component,
                                drawnPart -> new MCH_AircraftInfo.Hatch(drawnPart,
                                        getClamped(-180F, 180F, component.getOrDefault("MaxRotation", 90F)),
                                        (Boolean) component.getOrDefault("IsSliding", false)),
                                info.hatchList, new HashSet<>(Arrays.asList("MaxRotation", "IsSliding"))))
                        .forEachOrdered(info.hatchList::add);

                case "LightHatch" -> componentList.stream()
                        .map(component -> parseDrawnPart("light_hatch", component,
                                drawnPart -> new MCH_AircraftInfo.Hatch(drawnPart,
                                        getClamped(-180F, 180F, component.getOrDefault("MaxRotation", 90F)),
                                        (Boolean) component.getOrDefault("IsSliding", false)),
                                info.hatchList, new HashSet<>(Arrays.asList("MaxRotation", "IsSliding"))))
                        .forEachOrdered(info.lightHatchList::add);

                case "WeaponBay" -> componentList.stream().map(component -> {
                    String weaponName = ((String) component.get("WeaponName")).trim();
                    if (weaponName == null) throw new IllegalArgumentException("WeaponName is required!");
                    return parseDrawnPart("wb", component,
                            drawnPart -> new MCH_AircraftInfo.WeaponBay(drawnPart,
                                    getClamped(-180F, 180F, component.getOrDefault("MaxRotation", 90F)),
                                    (Boolean) component.getOrDefault("IsSliding", false), weaponName),
                            info.partWeaponBay, new HashSet<>(Arrays.asList("MaxRotation", "IsSliding", "WeaponName")));
                }).forEachOrdered(info.partWeaponBay::add);

                case "RepelHook" -> componentList.stream().map(ComponentParser::parseHook)
                        .forEachOrdered(info.repellingHooks::add);

                case "Rotation" -> componentList.stream()
                        .map(component -> parseDrawnPart(MCH_AircraftInfo.RotPart.class, component,
                                drawnPart -> new MCH_AircraftInfo.RotPart(drawnPart,
                                        ((Number) component.getOrDefault("Speed", 0)).floatValue(),
                                        ((Boolean) component.getOrDefault("AlwaysRotate", false))),
                                info.partRotPart, new HashSet<>(Arrays.asList("Speed", "AlwaysRotate"))))
                        .forEachOrdered(info.partRotPart::add);

                case "SteeringWheel" -> componentList.stream().map(component -> parseDrawnPart("steering_wheel",
                        component,
                        drawnPart -> new MCH_AircraftInfo.PartWheel(drawnPart,
                                ((Number) component.getOrDefault("Direction", 0F)).floatValue(),
                                component.containsKey("Pivot") ? parseVector(component.get("Pivot")) : Vec3d.ZERO),
                        info.partSteeringWheel, new HashSet<>(Arrays.asList("Direction", "Pivot"))))
                        .forEachOrdered(info.partSteeringWheel::add);

                case "Wheel" -> componentList.stream().map(component -> {
                    // Wheels of being a drawn part, have their own constraints and defaults
                    Vec3d pos = null;
                    Vec3d rot = new Vec3d(0, 1, 0);
                    Vec3d pivot = Vec3d.ZERO;
                    String name = "wheel" + info.partWheel.size();
                    float dir = 0;

                    for (Map.Entry<String, Object> wheelEntry : component.entrySet()) {
                        switch (wheelEntry.getKey()) {
                            case "Position" -> pos = parseVector(wheelEntry.getValue());
                            case "Rotation" -> rot = parseVector(wheelEntry.getValue());
                            case "Direction" -> dir = getClamped(-1800.0F, 1800.0F, wheelEntry.getValue());
                            case "Pivot" -> pivot = parseVector(wheelEntry.getValue());
                            case "PartName" -> name = ((String) wheelEntry.getValue()).toLowerCase(Locale.ROOT).trim();
                            default -> logUnkownEntry(wheelEntry, "PartWheel");
                        }
                    }
                    if (pos == null) throw new IllegalArgumentException("Part wheel must have a Position!");
                    return (new MCH_AircraftInfo.PartWheel(new MCH_AircraftInfo.DrawnPart(pos, rot, name), dir, pivot));
                }).forEachOrdered(info.partWheel::add);

                case "LandingGear" -> componentList.stream()
                        .map(component -> parseDrawnPart("lg", component, drawnPart -> {
                            float maxRot = getClamped(-180F, 180F, component.getOrDefault("MaxRotation", 90F)) / 90F;
                            boolean reverse = (Boolean) component.getOrDefault("IsReverse", false);
                            boolean hatch = (Boolean) component.getOrDefault("IsHatch", false);
                            MCH_AircraftInfo.LandingGear gear = new MCH_AircraftInfo.LandingGear(drawnPart, maxRot,
                                    reverse, hatch);

                            if (component.containsKey("ArticulatedRotation")) {
                                gear.enableRot2 = true;
                                gear.rot2 = parseVector(component.get("ArticulatedRotation"));
                                gear.maxRotFactor2 = getClamped(-180F, 180F,
                                        component.getOrDefault("MaxArticulatedRotation", 90F)) / 90F;
                            }

                            if (component.containsKey("SlideVec")) {
                                gear.slide = parseVector(component.get("SlideVec"));
                            }

                            return gear;
                        }, info.landingGear,
                                new HashSet<>(Arrays.asList("MaxRotation", "IsReverse", "IsHatch",
                                        "ArticulatedRotation", "MaxArticulatedRotation", "SlideVec"))))
                        .forEachOrdered(info.landingGear::add);

                case "Weapon" -> componentList.stream()
                        .map(component -> parseDrawnPart("weapon", component, drawnPart -> {
                            String[] weaponNames = component.containsKey("WeaponNames") ?
                                    info.splitParamSlash(((String) component.get("WeaponNames")).toLowerCase().trim()) :
                                    new String[] { "unnamed" };

                            boolean isRotatingWeapon = (Boolean) component.getOrDefault("BarrelRot", false);
                            boolean isMissile = (Boolean) (component.getOrDefault("IsMissile", false));
                            boolean hideGM = (Boolean) (component.getOrDefault("HideGM", false));
                            boolean yaw = (Boolean) (component.getOrDefault("Yaw", false));
                            boolean pitch = (Boolean) (component.getOrDefault("Pitch", false));
                            float recoilBuf = component.containsKey("RecoilBuf") ?
                                    ((Number) component.get("RecoilBuf")).floatValue() : 0.0F;
                            boolean turret = (Boolean) (component.getOrDefault("Turret", false));

                            MCH_AircraftInfo.PartWeapon weapon = new MCH_AircraftInfo.PartWeapon(drawnPart, weaponNames,
                                    isRotatingWeapon, isMissile, hideGM, yaw, pitch, recoilBuf, turret);

                            // Parse child weapons if present
                            if (component.containsKey("Children") && component.get("Children") instanceof List) {
                                List<Map<String, Object>> children = (List<Map<String, Object>>) component
                                        .get("Children");
                                for (Map<String, Object> childPart : children) {
                                    boolean childYaw = Boolean.TRUE.equals(childPart.getOrDefault("Yaw", false));
                                    boolean childPitch = Boolean.TRUE.equals(childPart.getOrDefault("Pitch", false));
                                    Vec3d childPos = childPart.containsKey("Position") ?
                                            parseVector(childPart.get("Position")) : Vec3d.ZERO;

                                    Vec3d childRot = childPart.containsKey("Rotation") ?
                                            parseVector(childPart.get("Rotation")) : Vec3d.ZERO;
                                    float childRecoil = childPart.containsKey("RecoilBuf") ?
                                            ((Number) childPart.get("RecoilBuf")).floatValue() : 0.0F;

                                    MCH_AircraftInfo.PartWeaponChild child = new MCH_AircraftInfo.PartWeaponChild(
                                            childPos, childRot, weapon.modelName + "_" + weapon.child.size(),
                                            weapon.name, childYaw, childPitch, childRecoil);
                                    weapon.child.add(child);

                                    for (Map.Entry<String, Object> argument : childPart.entrySet()) {
                                        if (!Arrays.asList("Position", "Yaw", "Pitch", "RecoilBuf", "Rotation")
                                                .contains(argument.getKey())) {
                                            logUnkownEntry(argument, "PartWeaponChild");
                                        }
                                    }
                                }
                            }

                            return weapon;
                        }, info.partWeapon,
                                new HashSet<>(Arrays.asList("HideGM", "WeaponNames", "RotBarrel", "IsMissile", "Yaw",
                                        "Pitch", "RecoilBuf", "Turret", "Children", "BarrelRot"))))
                        .forEachOrdered(info.partWeapon::add);

                case "SearchLight" -> componentList.stream().map(component -> parseSearchLights(component))
                        .forEach(info.searchLights::add);
                // info.searchLights.add(parseSearchLights((Map<String, Object>) entry));

                case "TrackRoller" -> componentList
                        .stream().map(component -> parseDrawnPart("track_roller", component,
                                MCH_AircraftInfo.TrackRoller::new, info.partTrackRoller, new HashSet<>()))
                        .forEachOrdered(info.partTrackRoller::add);
                case "Throttle" -> componentList.stream()
                        .map(component -> parseDrawnPart(MCH_AircraftInfo.Throttle.class, component, drawnPart -> {
                            Vec3d slidePos = component.containsKey("SlidePos") ?
                                    parseVector(component.get("SlidePos")) : Vec3d.ZERO;
                            float animAngle = component.containsKey("MaxAngle") ?
                                    ((Number) component.get("MaxAngle")).floatValue() : 0.0F;

                            return new MCH_AircraftInfo.Throttle(drawnPart, slidePos, animAngle);
                        }, info.partThrottle, new HashSet<>(Arrays.asList("MaxAngle", "SlidePos"))))
                        .forEachOrdered(info.partThrottle::add);
                case "CrawlerTrack" -> componentList.stream().map((component -> parseCrawlerTrack(component, info)))
                        .forEachOrdered(info.partCrawlerTrack::add);
            }
        }
    }

    public static MCH_AircraftInfo.CrawlerTrack parseCrawlerTrack(Map<String, Object> component,
                                                                  MCH_AircraftInfo info) {
        boolean isReverse = (Boolean) component.getOrDefault("IsReverse", false);
        float segmentLength = component.containsKey("SegmentLength") ?
                ((Number) component.get("SegmentLength")).floatValue() : 1F;
        float zOffset = component.containsKey("ZOffset") ? ((Number) component.get("ZOffset")).floatValue() : 0F;

        List<float[]> trackList = new ArrayList<>();
        if (component.containsKey("TrackList") && component.get("TrackList") instanceof List<?>list) {
            for (Object elem : list) {
                if (!(elem instanceof List<?>pair) || pair.size() != 2)
                    throw new IllegalArgumentException("Each TrackList entry must be a 2-element list!");
                float x = ((Number) pair.get(0)).floatValue();
                float y = ((Number) pair.get(1)).floatValue();
                trackList.add(new float[] { x, y });
            }
        }

        int trackCount = trackList.size();
        if (trackCount < 4) throw new IllegalArgumentException("CrawlerTrack must have at least 4 track coordinates");

        double[] cx = new double[trackCount];
        double[] cy = new double[trackCount];
        for (int i = 0; i < trackCount; i++) {
            int idx = !isReverse ? i : trackCount - i - 1;
            float[] pair = trackList.get(idx);
            cx[i] = pair[0];
            cy[i] = pair[1];
        }

        List<MCH_AircraftInfo.CrawlerTrackPrm> trackParams = new ArrayList<>();
        trackParams.add(new MCH_AircraftInfo.CrawlerTrackPrm((float) cx[0], (float) cy[0]));

        double carry = 0.0;
        float len = segmentLength * 0.9F;
        for (int i = 0; i < trackCount; i++) {
            int j = (i + 1) % trackCount;
            double dx = cx[j] - cx[i];
            double dy = cy[j] - cy[i];
            final double dist = Math.hypot(dx, dy);
            double ux = dx / dist, uy = dy / dist;
            double px = cx[i], py = cy[i];
            double need = len - carry;
            double left = dist;
            while (left >= need) {
                px += ux * need;
                py += uy * need;
                trackParams.add(new MCH_AircraftInfo.CrawlerTrackPrm((float) px, (float) py));
                left -= need;
                need = len;
            }
            carry = left;
        }

        int n = trackParams.size();
        for (int i = 0; i < n; i++) {
            var prev = trackParams.get((i + n - 1) % n);
            var curr = trackParams.get(i);
            var next = trackParams.get((i + 1) % n);

            float rotPrev = (float) Math.toDegrees(Math.atan2(prev.x - curr.x, prev.y - curr.y));
            float rotNext = (float) Math.toDegrees(Math.atan2(next.x - curr.x, next.y - curr.y));
            float ppr = (rotPrev + 360.0F) % 360.0F;
            float nextAdj = rotNext + 180.0F;

            if ((nextAdj < ppr - 0.3F || nextAdj > ppr + 0.3F) && nextAdj - ppr < 100.0F && nextAdj - ppr > -100.0F)
                nextAdj = (nextAdj + ppr) / 2.0F;

            curr.r = nextAdj;
        }

        MCH_AircraftInfo.CrawlerTrack crawlerTrack = new MCH_AircraftInfo.CrawlerTrack(
                "crawler_track" + info.partCrawlerTrack.size());
        crawlerTrack.len = len;
        crawlerTrack.cx = cx;
        crawlerTrack.cy = cy;
        crawlerTrack.lp = trackParams;
        crawlerTrack.z = zOffset;
        crawlerTrack.side = zOffset >= 0.0F ? 1 : 0;

        return crawlerTrack;
    }

    private static <
            Y extends MCH_AircraftInfo.DrawnPart> Y parseDrawnPart(String defaultName, Map<String, Object> map,
                                                                   Function<MCH_AircraftInfo.DrawnPart, Y> fillChildFields,
                                                                   List<Y> partList, Set<String> knownKeys) {
        Vec3d pos = map.containsKey("Position") ? parseVector(map.get("Position")) : null;
        Vec3d rot = map.containsKey("Rotation") ? parseVector(map.get("Rotation")) : Vec3d.ZERO;
        if (pos == null) pos = map.containsKey("Pos") ? parseVector(map.get("Pos")) : null;
        if (rot == Vec3d.ZERO) rot = map.containsKey("Rot") ? parseVector(map.get("Rot")) : Vec3d.ZERO;

        String modelName = (String) map.getOrDefault("PartName", defaultName + partList.size());
        if (pos == null) throw new IllegalArgumentException("Part Position must be set!");

        var base = new MCH_AircraftInfo.DrawnPart(pos, rot, modelName);
        var built = fillChildFields.apply(base);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!YamlParser.DRAWN_PART_ARGS.contains(key) && !knownKeys.contains(key))
                logUnkownEntry(entry, built.getClass().getSimpleName());

        }
        return built;
    }

    private static <
            Y extends MCH_AircraftInfo.DrawnPart> Y parseDrawnPart(Class<? extends MCH_AircraftInfo.DrawnPart> clazz,
                                                                   Map<String, Object> map,
                                                                   Function<MCH_AircraftInfo.DrawnPart, Y> fillChildFields,
                                                                   List<Y> partList, Set<String> knownKeys) {
        return parseDrawnPart(clazz.getSimpleName().toLowerCase(Locale.ROOT).trim(), map, fillChildFields, partList,
                knownKeys);
    }

    private static MCH_AircraftInfo.RepellingHook parseHook(Map<String, Object> map) {
        Vec3d pos = null;
        int interval = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "Interval" -> interval = ((Number) entry.getValue()).intValue();
                default -> logUnkownEntry(entry, "RepellingHooks");
            }
        }

        if (pos == null) throw new IllegalArgumentException("Repelling hook must have a position!");
        return new MCH_AircraftInfo.RepellingHook(pos, interval);
    }

    private static MCH_AircraftInfo.SearchLight parseSearchLights(Map<String, Object> map) {
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
                case "Pos", "Position" -> pos = parseVector(entry.getValue());
                case "ColorStart" -> colorStart = parseHexColor((String) entry.getValue());
                case "ColorEnd" -> colorEnd = parseHexColor((String) entry.getValue());
                case "Height" -> height = ((Number) entry.getValue()).floatValue();
                case "Width" -> width = ((Number) entry.getValue()).floatValue();
                case "Yaw" -> yaw = ((Number) entry.getValue()).floatValue();
                case "Pitch" -> pitch = ((Number) entry.getValue()).floatValue();
                case "StRot" -> stRot = ((Number) entry.getValue()).floatValue();
                case "Type" -> {}
                default -> logUnkownEntry(entry, "SearchLights");
            }
        }

        if (pos == null) {
            throw new IllegalArgumentException("SearchLight must have a position!");
        }

        return new MCH_AircraftInfo.SearchLight(pos, colorStart, colorEnd, height, width, fixedDirection, yaw, pitch,
                steering, stRot);
    }

    // Yeah yeah its code duplication... Whatever. Ragex made it and now I need to deal with it
    // TODO: Turn it into something thats worthy of being more than a plane rename
    public static void parseComponentsShip(Map<String, List<Map<String, Object>>> components, MCH_ShipInfo info) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
            String type = entry.getKey();
            var componentList = entry.getValue();
            switch (type) {
                case "ShipRotor" -> componentList.stream()
                        .map(component -> parseDrawnPart("rotor", component, drawnPart -> {

                            float rotFactor = component.containsKey("RotFactor") ?
                                    getClamped(-180F, 180F, component.get("RotFactor")) / 90F : 1F;

                            var rotor = new MCH_ShipInfo.Rotor(drawnPart, rotFactor);

                            if (component.containsKey("Blades")) {
                                var rawBladeList = (List<Map<String, Object>>) component.get("Blades");

                                rawBladeList.stream()
                                        .map(partBlade -> parseDrawnPart("blade", partBlade, drawnPartBlade -> {
                                            int bladeNum = 1;
                                            Integer bladeRot = null;

                                            for (Map.Entry<String, Object> partBladeEntry : partBlade.entrySet()) {
                                                switch (partBladeEntry.getKey()) {
                                                    case "BladeNum" -> bladeNum = ((Number) partBladeEntry.getValue())
                                                            .intValue();
                                                    case "BladeRot" -> bladeRot = ((Number) partBladeEntry.getValue())
                                                            .intValue();
                                                }
                                            }
                                            if (bladeRot != null)
                                                return new MCH_ShipInfo.Blade(drawnPartBlade, bladeNum, bladeRot);
                                            return null;
                                        }, rotor.blades, new HashSet<>(Arrays.asList("BladeNum", "BladeRot"))))
                                        .forEach(rotor.blades::add);
                            }

                            return rotor;
                        }, info.rotorList, new HashSet<>())).forEachOrdered(info.rotorList::add);
                case "Wing" -> componentList.stream().map(component -> parseDrawnPart("wing", component, drawnPart -> {
                    float maxRot = 0;
                    List<MCH_ShipInfo.Pylon> pylons = new ArrayList<>();

                    for (Map.Entry<String, Object> entryWing : component.entrySet()) {
                        switch (entryWing.getKey()) {

                            case "MaxRotation", "MaxRot" -> maxRot = getClamped(-180F, 180F, entryWing.getValue());
                            case "Pylons" -> {
                                var pylonList = (List<Map<String, Object>>) entryWing.getValue();
                                pylonList.stream()
                                        .map(pylonMap -> parseDrawnPart("wing" + info.wingList.size() + "_pylon",
                                                pylonMap,
                                                drawnPylon -> new MCH_ShipInfo.Pylon(drawnPylon,
                                                        (Float) MCH_Utils.getAny(pylonMap,
                                                                Arrays.asList("MaxRot", "MaxRotation"), 0F)),
                                                pylons, new HashSet<>(Arrays.asList("MaxRotation", "MaxRot"))))
                                        .forEach(pylons::add);
                            }
                        }
                    }
                    var wing = new MCH_ShipInfo.Wing(drawnPart, maxRot);
                    if (!pylons.isEmpty()) wing.pylonList = pylons;
                    return wing;

                }, info.wingList, new HashSet<>(Arrays.asList("MaxRotation", "MaxRot")))

                    ).forEach(info.wingList::add);
                case "Nozzle" -> componentList.stream().map(component -> parseDrawnPart("nozzle", component,
                        drawnPart -> drawnPart, info.nozzles, new HashSet<>())).forEach(info.nozzles::add);

            }

        }
    }

    public static void parseComponentsPlane(Map<String, List<Map<String, Object>>> components, MCH_PlaneInfo info) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
            String type = entry.getKey();
            var componentList = entry.getValue();
            switch (type) {
                case "PlaneRotor" -> componentList.stream()
                        .map(component -> parseDrawnPart("rotor", component, drawnPart -> {

                            float rotFactor = component.containsKey("RotFactor") ?
                                    getClamped(-180F, 180F, component.get("RotFactor")) / 90F : 1F;

                            var rotor = new MCH_PlaneInfo.Rotor(drawnPart, rotFactor);

                            if (component.containsKey("Blades")) {
                                var rawBladeList = (List<Map<String, Object>>) component.get("Blades");

                                rawBladeList.stream()
                                        .map(partBlade -> parseDrawnPart("blade", partBlade, drawnPartBlade -> {
                                            int bladeNum = 1;
                                            Integer bladeRot = null;

                                            for (Map.Entry<String, Object> partBladeEntry : partBlade.entrySet()) {
                                                switch (partBladeEntry.getKey()) {
                                                    case "BladeNum" -> bladeNum = ((Number) partBladeEntry.getValue())
                                                            .intValue();
                                                    case "BladeRot" -> bladeRot = ((Number) partBladeEntry.getValue())
                                                            .intValue();
                                                }
                                            }

                                            if (bladeRot != null)
                                                return new MCH_PlaneInfo.Blade(drawnPartBlade, bladeNum, bladeRot);
                                            return null;

                                        }, rotor.blades,
                                                new HashSet<>(Arrays.asList("BladeNum", "BladeRot", "RotFactor"))))
                                        .filter(Objects::nonNull)
                                        .forEach(rotor.blades::add);
                            }

                            return rotor;

                        }, info.rotorList, new HashSet<>(Arrays.asList("Blades", "RotFactor"))))
                        .filter(Objects::nonNull)
                        .forEachOrdered(info.rotorList::add);

                case "Wing" -> componentList.stream().map(component -> parseDrawnPart("wing", component, drawnPart -> {
                    float maxRot = 0;
                    List<MCH_PlaneInfo.Pylon> pylons = new ArrayList<>();

                    for (Map.Entry<String, Object> entryWing : component.entrySet()) {
                        switch (entryWing.getKey()) {

                            case "MaxRotation", "MaxRot" -> maxRot = getClamped(-180F, 180F, entryWing.getValue());
                            case "Pylons" -> {
                                var pylonList = (List<Map<String, Object>>) entryWing.getValue();
                                pylonList.stream().map(pylonMap -> parseDrawnPart(
                                        "wing" + info.wingList.size() + "_pylon", pylonMap,
                                        drawnPylon -> new MCH_PlaneInfo.Pylon(drawnPylon,
                                                ((Number) MCH_Utils.getAny(pylonMap,
                                                        Arrays.asList("MaxRot", "MaxRotation"), 0F)).floatValue()),
                                        pylons, new HashSet<>(Arrays.asList("MaxRotation", "MaxRot"))))
                                        .forEach(pylons::add);
                            }
                        }
                    }
                    var wing = new MCH_PlaneInfo.Wing(drawnPart, maxRot);
                    if (!pylons.isEmpty()) wing.pylonList = pylons;
                    return wing;

                }, info.wingList, new HashSet<>(Arrays.asList("MaxRotation", "MaxRot", "Pylons")))

                    ).forEach(info.wingList::add);
                case "Nozzle" -> componentList.stream().map(component -> parseDrawnPart("nozzle", component,
                        drawnPart -> drawnPart, info.nozzles, new HashSet<>())).forEach(info.nozzles::add);

            }

        }
    }

    public static void parseComponentVehicle(Map<String, List<Map<String, Object>>> components, MCH_VehicleInfo info) {
        if (!components.containsKey("Vpart")) return;

        var vparts = components.get("Vpart");
        vparts.stream()
                .map(component -> parseDrawnPart("part", component, drawnPart -> parseVPart(component, drawnPart, info),
                        info.partList,
                        new HashSet<>(Arrays.asList(
                                "DrawFP", "DrawFirstPerson",
                                "UnlockYaw", "CanYaw",
                                "UnlockPitch", "CanPitch",
                                "Type", "RecoilBuff", "Children"))))
                .forEach(info.partList::add);
    }

    private static MCH_VehicleInfo.VPart parseVPart(Map<String, Object> component, Object drawnPart,
                                                    MCH_VehicleInfo info) {
        boolean drawFP = true;
        boolean yaw = false;
        boolean pitch = false;
        VpartType type = VpartType.NORMAL;
        float recoilBuff = 0;
        List<MCH_VehicleInfo.VPart> childList = new ArrayList<>();

        for (var entry : component.entrySet()) {
            switch (entry.getKey()) {
                case "DrawFP", "DrawFirstPerson" -> drawFP = (Boolean) entry.getValue();
                case "UnlockYaw", "CanYaw" -> yaw = (Boolean) entry.getValue();
                case "UnlockPitch", "CanPitch" -> pitch = (Boolean) entry.getValue();
                case "Type" -> {
                    try {
                        type = VpartType.valueOf(((String) entry.getValue())
                                .toUpperCase(Locale.ROOT).trim());
                    } catch (RuntimeException s) {
                        throw new IllegalArgumentException(
                                "Invalid Vpart type: " + entry.getValue() +
                                        ". Allowed values: " +
                                        Arrays.stream(VpartType.values())
                                                .map(Enum::name)
                                                .collect(Collectors.joining(", ")));
                    }
                }
                case "RecoilBuff" -> recoilBuff = ((Number) entry.getValue()).floatValue();
                case "Children" -> {
                    // recursive handling
                    var children = (List<Map<String, Object>>) entry.getValue();
                    for (var childComponent : children) {
                        var childPart = parseDrawnPart("child", childComponent,
                                childDrawn -> parseVPart(childComponent, childDrawn, info),
                                info.partList,
                                new HashSet<>(Arrays.asList(
                                        "DrawFP", "DrawFirstPerson",
                                        "UnlockYaw", "CanYaw",
                                        "UnlockPitch", "CanPitch",
                                        "Type", "RecoilBuff", "Children")));
                        childList.add(childPart);
                    }
                }
                default -> {}
            }
        }

        var vpart = new MCH_VehicleInfo.VPart((MCH_AircraftInfo.DrawnPart) drawnPart, pitch, yaw, type.ordinal(),
                drawFP, recoilBuff);
        vpart.child = childList;
        return vpart;
    }

    public enum VpartType {
        NORMAL,
        ROTATES_WEAPON,
        RECOILS_WEAPON,
        TYPE_3
    }
}
