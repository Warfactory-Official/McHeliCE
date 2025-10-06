package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Function;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.*;

public class ComponentParser {
    //TODO: add the whole default handling thing into it
    public void parseComponents(List<Map<String, Object>> components, MCH_AircraftInfo info) {
        for (Map<String, Object> component : components) {
            if (!component.containsKey("Type") || !(component.get("Type") instanceof String))
                throw new IllegalArgumentException("Part must contain a Type string!");

            String type = ((String) component.get("Type")).trim();
            switch (type) {
                case "Camera" -> parseDrawnPart(
                        MCH_AircraftInfo.Camera.class,
                        component,
                        drawnPart -> new MCH_AircraftInfo.Camera(
                                drawnPart,
                                (Boolean) component.getOrDefault("yawSync", true),
                                (Boolean) component.getOrDefault("pitchSync", false)
                        ),
                        info.cameraList,
                        new HashSet<>(Arrays.asList("yawSync", "pitchSync"))
                );

                case "Canopy" -> parseDrawnPart(
                        MCH_AircraftInfo.Canopy.class,
                        component,
                        drawnPart -> new MCH_AircraftInfo.Canopy(
                                drawnPart,
                               getClamped(-180F, 180F, (Number) component.getOrDefault("maxRotation", 90F)),
                                (Boolean) component.getOrDefault("isSliding", false)
                        ),
                        info.canopyList,
                        new HashSet<>(Arrays.asList("maxRotation", "isSliding"))
                );

                case "Hatch" -> parseDrawnPart(
                        MCH_AircraftInfo.Hatch.class,
                        component,
                        drawnPart -> new MCH_AircraftInfo.Hatch(
                                drawnPart,
                               getClamped(-180F, 180F, (Number) component.getOrDefault("maxRotation", 90F)),
                                (Boolean) component.getOrDefault("isSliding", false)
                        ),
                        info.hatchList,
                        new HashSet<>(Arrays.asList("maxRotation", "isSliding"))
                );

                case "LightHatch" -> parseDrawnPart(
                        "light_hatch",
                        component,
                        drawnPart -> new MCH_AircraftInfo.Hatch(
                                drawnPart,
                                getClamped(-180F, 180F, (Number) component.getOrDefault("maxRotation", 90F)),
                                (Boolean) component.getOrDefault("isSliding", false)
                        ),
                        info.lightHatchList,
                        new HashSet<>(Arrays.asList("maxRotation", "isSliding"))
                );

                case "WeaponBay" -> {
                    String weaponName = component.containsKey("WeaponName")
                            ? ((String) component.get("WeaponName")).trim()
                            : null;
                    if (weaponName == null)
                        throw new IllegalArgumentException("WeaponName is required!");
                    parseDrawnPart(
                            "wb",
                            component,
                            drawnPart -> new MCH_AircraftInfo.WeaponBay(
                                    drawnPart,
                                   getClamped(-180F, 180F, (Number) component.getOrDefault("maxRotation", 90F)),
                                    (Boolean) component.getOrDefault("isSliding", false),
                                    weaponName
                            ),
                            info.partWeaponBay,
                            new HashSet<>(Arrays.asList("maxRotation", "isSliding", "WeaponName"))
                    );
                }
                case "RepelHook" -> info.repellingHooks.add(parseHook(component));


                case "Rotation" -> parseDrawnPart(
                        MCH_AircraftInfo.RotPart.class,
                        component,
                        drawnPart -> new MCH_AircraftInfo.RotPart(
                                drawnPart,
                                ((Number) component.getOrDefault("Speed", 0)).floatValue(),
                                ((Boolean) component.getOrDefault("AlwaysRotate", false))
                        ),
                        info.partRotPart,
                        new HashSet<>(Arrays.asList("Speed", "AlwaysRotate"))
                );

                case "SteeringWheel" -> parseDrawnPart(
                        "steering_wheel",
                        component,
                        drawnPart -> new MCH_AircraftInfo.PartWheel(
                                drawnPart,
                                ((Number) component.getOrDefault("Direction", 0F)).floatValue(),
                                component.containsKey("Pivot") ? parseVector((Object[]) component.get("Pivot")) : Vec3d.ZERO
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

                    for (Map.Entry<String, Object> entry : component.entrySet()) {
                        switch (entry.getKey()) {
                            case "Position" -> pos = parseVector((Object[]) entry.getValue());
                            case "Rotation" -> rot = parseVector((Object[]) entry.getValue());
                            case "Direction" ->
                                    dir = getClamped(-1800.0F, 1800.0F, (Number) entry.getValue());
                            case "Pivot" -> pivot = parseVector((Object[]) entry.getValue());
                            case "PartName" -> name = ((String) entry.getValue()).toLowerCase(Locale.ROOT).trim();
                            case "Type" -> {
                            }
                            default -> logUnkownEntry(entry, "PartWheel");
                        }
                    }
                    if (pos == null)
                        throw new IllegalArgumentException("Part wheel must have a Position!");
                    info.partWheel.add(new MCH_AircraftInfo.PartWheel(new MCH_AircraftInfo.DrawnPart(pos, rot, name), dir, pivot));
                }

                case "LandingGear" -> parseDrawnPart(
                        "lg",
                        component,
                        drawnPart -> {
                            float maxRot =getClamped(-180F, 180F, (Number) component.getOrDefault("maxRotation", 90F)) / 90F;
                            boolean reverse = (Boolean) component.getOrDefault("isReverse", false);
                            boolean hatch = (Boolean) component.getOrDefault("isHatch", false);
                            MCH_AircraftInfo.LandingGear gear = new MCH_AircraftInfo.LandingGear(drawnPart, maxRot, reverse, hatch);

                            if (component.containsKey("ArticulatedRotation")) {
                                gear.enableRot2 = true;
                                gear.rot2 = parseVector((Object[]) component.get("ArticulatedRotation"));
                                gear.maxRotFactor2 =getClamped(
                                        -180F, 180F, (Number) component.getOrDefault("MaxArticulatedRotation", 90F)
                                ) / 90F;
                            }

                            if (component.containsKey("SlideVec")) {
                                gear.slide = parseVector((Object[]) component.get("SlideVec"));
                            }

                            return gear;
                        },
                        info.landingGear,
                        new HashSet<>(Arrays.asList("maxRotation", "isReverse", "isHatch", "ArticulatedRotation", "MaxArticulatedRotation", "SlideVec"))
                );

                case "Weapon" -> parseDrawnPart(
                        "weapon",
                        component,
                        drawnPart -> {
                            String[] weaponNames = component.containsKey("WeaponNames")
                                    ? info.splitParamSlash(((String) component.get("WeaponNames")).toLowerCase().trim())
                                    : new String[]{"unnamed"};

                            boolean isRotatingWeapon = (Boolean) component.getOrDefault("BarrelRot", false);
                            boolean isMissile = (Boolean) (component.getOrDefault("IsMissile", false));
                            boolean hideGM = (Boolean) (component.getOrDefault("hideGM", false));
                            boolean yaw = (Boolean) (component.getOrDefault("Yaw", false));
                            boolean pitch = (Boolean) (component.getOrDefault("Pitch", false));
                            float recoilBuf = component.containsKey("RecoilBuf") ? ((Number) component.get("RecoilBuf")).floatValue() : 0.0F;
                            boolean turret = (Boolean) (component.getOrDefault("Turret", false));

                            MCH_AircraftInfo.PartWeapon weapon = new MCH_AircraftInfo.PartWeapon(
                                    drawnPart,
                                    weaponNames,
                                    isRotatingWeapon,
                                    isMissile,
                                    hideGM,
                                    yaw,
                                    pitch,
                                    recoilBuf,
                                    turret
                            );

                            // Parse child weapons if present
                            if (component.containsKey("Children") && component.get("Children") instanceof List) {
                                List<Map<String, Object>> children = (List<Map<String, Object>>) component.get("Children");
                                for (Map<String, Object> childPart : children) {
                                    boolean childYaw = Boolean.TRUE.equals(childPart.getOrDefault("Yaw", false));
                                    boolean childPitch = Boolean.TRUE.equals(childPart.getOrDefault("Pitch", false));
                                    Vec3d childPos = childPart.containsKey("Position")
                                            ? parseVector((Object[]) childPart.get("Position"))
                                            : Vec3d.ZERO;

                                    Vec3d childRot = childPart.containsKey("Rotation")
                                            ? parseVector((Object[]) childPart.get("Rotation"))
                                            : Vec3d.ZERO;
                                    float childRecoil = childPart.containsKey("RecoilBuf")
                                            ? ((Number) childPart.get("RecoilBuf")).floatValue()
                                            : 0.0F;

                                    MCH_AircraftInfo.PartWeaponChild child = new MCH_AircraftInfo.PartWeaponChild(
                                            childPos,
                                            childRot,
                                            weapon.modelName,
                                            weapon.name,
                                            childYaw,
                                            childPitch,
                                            childRecoil
                                    );
                                    weapon.child.add(child);

                                    for (Map.Entry<String, Object> entry : childPart.entrySet()) {
                                        if (!Arrays.asList("Position", "Yaw", "Pitch", "RecoilBuf").contains(entry.getKey())) {
                                           logUnkownEntry(entry, "PartWeaponChild");
                                        }
                                    }
                                }
                            }

                            return weapon;
                        },
                        info.partWeapon,
                        new HashSet<>(Arrays.asList("WeaponNames", "RotBarrel", "IsMissile", "Yaw", "Pitch", "RecoilBuf", "Turret", "Children"))
                );

                case "SearchLight" -> info.searchLights.add(parseSearchLights((Map<String, Object>) component));

                case "TrackRoller" -> parseDrawnPart(
                        "track_roller",
                        component,
                        MCH_AircraftInfo.TrackRoller::new,
                        info.partTrackRoller,
                        new HashSet<>()
                );
                case "Throttle" -> parseDrawnPart(
                        MCH_AircraftInfo.Throttle.class,
                        component,
                        drawnPart -> {
                            Vec3d slidePos = component.containsKey("SlidePos") ? parseVector((Object[]) component.get("SlidePos")) : Vec3d.ZERO;
                            float animAngle = component.containsKey("MaxAngle")
                                    ? ((Number) component.get("MaxAngle")).floatValue()
                                    : 0.0F;

                            return new MCH_AircraftInfo.Throttle(drawnPart, slidePos, animAngle);
                        },
                        info.partThrottle,
                        new HashSet<>(Arrays.asList("MaxAngle", "SlidePos"))
                );
                //TODO:Extract to its own method and document, holy shit it's weird
                case "CrawlerTrack" -> {


                    for (Map.Entry<String, Object> entry : component.entrySet()) {
                        boolean isReverse = false;//Controls whenever vertex order is reversed
                        float segmentLenght = 1F;
                        float zOffset = 0F;
                        List<float[]> trackList = new ArrayList<>();


                        switch (entry.getKey()) {
                            case "IsReverse" -> isReverse = (Boolean) entry.getValue();
                            case "SegmentLength" ->
                                    segmentLenght =getClamped(0.001F, 1000.0F, (Number) entry.getValue());
                            case "ZOffset" -> zOffset = ((Number) entry.getValue()).floatValue();
                            case "TrackList" -> {
                                Object raw = entry.getValue();
                                if (!(raw instanceof List<?> list))
                                    throw new IllegalArgumentException("TrackList must be a list of coordinate pairs!");

                                for (Object elem : list) {
                                    if (!(elem instanceof List<?> pair) || pair.size() != 2)
                                        throw new IllegalArgumentException("Each TrackList entry must be a 2-element list!");

                                    float x = ((Number) pair.get(0)).floatValue();
                                    float y = ((Number) pair.get(1)).floatValue();

                                    trackList.add(new float[]{x, y});
                                }
                            }
                            case "Type" -> {
                            }
                            default ->logUnkownEntry(entry, "CrawlerTrack");
                        }

                        int trackCount = trackList.size();
                        float length = segmentLenght * 0.9F;
                        if (trackCount < 4)
                            throw new IllegalArgumentException("CrawlerTrack must have at least 4 track list coordinates");

                        // Prepare ordered coordinate arrays
                        double[] cx = new double[trackCount];
                        double[] cy = new double[trackCount];
                        for (int i = 0; i < trackCount; i++) {
                            int idx = !isReverse ? i : trackCount - i - 1;
                            float[] pair = trackList.get(idx);
                            cx[i] = pair[0];
                            cy[i] = pair[1];
                        }

                        // Build interpolated track parameter list
                        List<MCH_AircraftInfo.CrawlerTrackPrm> trackParams = new ArrayList<>();
                        trackParams.add(new MCH_AircraftInfo.CrawlerTrackPrm((float) cx[0], (float) cy[0]));

                        double accLen = 0.0D;
                        for (int i = 0; i < trackCount; i++) {
                            double dx = cx[(i + 1) % trackCount] - cx[i];
                            double dy = cy[(i + 1) % trackCount] - cy[i];
                            double dist = Math.sqrt(dx * dx + dy * dy);
                            accLen += dist;

                            while (accLen >= length) {
                                trackParams.add(new MCH_AircraftInfo.CrawlerTrackPrm(
                                        (float) (cx[i] + dx * (length / dist)),
                                        (float) (cy[i] + dy * (length / dist))
                                ));
                                accLen -= length;
                            }
                        }

                        // Compute rotation between adjacent track points
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

                        // Construct and add final track
                        MCH_AircraftInfo.CrawlerTrack crawlerTrack = new MCH_AircraftInfo.CrawlerTrack(
                                "crawler_track" + info.partCrawlerTrack.size()
                        );
                        crawlerTrack.len = length;
                        crawlerTrack.cx = cx;
                        crawlerTrack.cy = cy;
                        crawlerTrack.lp = trackParams;
                        crawlerTrack.z = zOffset;
                        crawlerTrack.side = zOffset >= 0.0F ? 1 : 0;

                        info.partCrawlerTrack.add(crawlerTrack);


                    }
                }


            }
        }
    }


    private <Y extends MCH_AircraftInfo.DrawnPart> void parseDrawnPart(
            String defaultName,
            Map<String, Object> map,
            Function<MCH_AircraftInfo.DrawnPart, Y> fillChildFields,
            List<Y> partList,
            Set<String> knownKeys)  {

        Vec3d pos = map.containsKey("Position") ? parseVector((Object[]) map.get("Position")) : null;
        Vec3d rot = map.containsKey("Rotation") ? parseVector((Object[]) map.get("Rotation")) : Vec3d.ZERO;

        String modelName = (String) map.getOrDefault("PartName", defaultName + partList.size());
        if (pos == null)
            throw new IllegalArgumentException("Part Position must be set!");

        var base = new MCH_AircraftInfo.DrawnPart(pos, rot, modelName);
        var built = fillChildFields.apply(base);
        partList.add(built);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!knownKeys.contains(key) && !YamlParser.DRAWN_PART_ARGS.contains(key))
                logUnkownEntry(entry, built.getClass().getSimpleName());

        }
    }

    private <Y extends MCH_AircraftInfo.DrawnPart> void parseDrawnPart(
            Class<? extends MCH_AircraftInfo.DrawnPart> clazz,
            Map<String, Object> map,
            Function<MCH_AircraftInfo.DrawnPart, Y> fillChildFields,
            List<Y> partList,
            Set<String> knownKeys) {
        parseDrawnPart(clazz.getSimpleName().toLowerCase(Locale.ROOT).trim(), map, fillChildFields, partList, knownKeys);
    }

    private MCH_AircraftInfo.RepellingHook parseHook(Map<String, Object> map) {
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
        return new MCH_AircraftInfo.RepellingHook(pos, interval);
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
                case "Type" ->{}
                default -> logUnkownEntry(entry, "SearchLights");
            }
        }

        if (pos == null) {
            throw new IllegalArgumentException("SearchLight must have a position!");
        }

        return new MCH_AircraftInfo.SearchLight(pos, colorStart, colorEnd, height, width, fixedDirection, yaw, pitch, steering, stRot);


    }
}
