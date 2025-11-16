package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.hud.*;
import net.minecraft.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.logUnkownEntry;
import static com.norwood.mcheli.hud.MCH_HudItem.toFormula;

// Dude this is so fucking stupid, we should just move to groovy but muh 1.7 compat... ugh
@SuppressWarnings("unchecked")
public class HUDParser {

    private HUDParser() {}

    public static Tuple<String, String> setTuple(List<String> mapKeys, Object object) {
        if (object instanceof List<?>tupleList) {
            if (tupleList.size() != 2) throw new IllegalArgumentException("Tuple list must have exactly 2 variables");
            List<String> tupleListTyped = toStringList(tupleList);
            return new Tuple<>(tupleListTyped.get(0), tupleListTyped.get(1));
        } else if (object instanceof Map<?, ?>map) {
            Map<String, String> tupleMapTyped = toStringMap((Map<String, ?>) map);
            return new Tuple<>(tupleMapTyped.get(mapKeys.get(0)), tupleMapTyped.get(mapKeys.get(1)));
        } else {
            throw new IllegalArgumentException("Tuple parsing failed, the values must be either string or a number!");
        }
    }

    public static List<String> toStringList(List<?> list) {
        if (list.get(0) instanceof String) return (List<String>) list;
        if (list.get(0) instanceof Number)
            return ((List<Number>) list).stream().map(Number::toString).collect(Collectors.toList());
        throw new ClassCastException();
    }

    public static Map<String, String> toStringMap(Map<String, ?> map) {
        if (map.isEmpty()) return new HashMap<>();
        Object firstValue = map.values().iterator().next();
        if (firstValue instanceof String) {
            return (Map<String, String>) map;
        }
        if (firstValue instanceof Number) {
            return map.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> ((Number) e.getValue()).toString()));
        }
        throw new ClassCastException("Map values are not String or Number");
    }

    public static void parse(MCH_Hud info, Object rootObj) {
        if (rootObj instanceof List<?>) {
            for (Object item : (List<?>) rootObj) {
                parseHudItem(info, item);
            }
        } else {
            throw new IllegalArgumentException("Root object must be a List");
        }
    }

    private static MCH_HudItem parseHUDCommands(Map.Entry<String, Object> entry) {
        switch (entry.getKey()) {

            case "Exit" -> {
                return new MCH_HudItemExit(0);

            }
            case "Color" -> {
                return new MCH_HudItemColor(0, toFormula((String) entry.getValue()));

            }
            case "DrawTexture" -> {
                return parseDrawTexture((Map<String, Object>) entry.getValue());
            }

            case "DrawString" -> {
                return parseDrawString((Map<String, Object>) entry.getValue());
            }

            case "CameraRot", "CameraRotation" -> {
                return parseCameraRot((Map<String, Object>) entry.getValue());
            }

            case "DrawRectangle", "DrawRect" -> {
                return parseRact((Map<String, Object>) entry.getValue());
            }

            case "DrawLine" -> {
                return parseLine((Map<String, Object>) entry.getValue());
            }

            case "Call" -> {
                return new MCH_HudItemCall(0, (String) entry.getValue());
            }
            case "DrawRadar" -> {
                return parseRadar((Map<String, Object>) entry.getValue());
            }

            case "DrawGraduation" -> {
                return parseGraduation((Map<String, Object>) entry.getValue());
            }

            default -> {
                return null;
            }
        }
    }

    private static MCH_HudItem parseGraduation(Map<String, Object> value) {
        GraduationType type = null;
        String xCoord = null;
        String yCoord = null;
        String rot = "0";
        String roll = "0";

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Type" -> type = GraduationType
                        .valueOf(((String) entry.getValue()).toUpperCase(Locale.ROOT).trim());
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Rotation", "Rot" -> rot = (String) entry.getValue();
                case "Roll" -> roll = (String) entry.getValue();
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Texture, Pos fields are required for drawTexture element.");
        return new MCH_HudItemGraduation(0, type == null ? -1 : type.ordinal(), rot, roll, xCoord, yCoord);
    }

    private static MCH_HudItemRadar parseRadar(Map<String, Object> value) {
        boolean isEntityRadar = false;
        String xCoord = null;
        String yCoord = null;
        String width = null;
        String height = null;
        String rot = "0";

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "EntityRadar" -> isEntityRadar = (Boolean) entry.getValue();
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Size" -> {
                    Tuple<String, String> size = setTuple(Arrays.asList("width", "height"), entry.getValue());
                    width = MCH_HudItem.toFormula(size.getFirst());
                    height = MCH_HudItem.toFormula(size.getSecond());
                }
                case "Rotation", "Rot" -> rot = toFormula((String) entry.getValue());
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Texture, Pos fields are required for drawTexture element.");

        return new MCH_HudItemRadar(0, isEntityRadar, rot, xCoord, yCoord, width, height);
    }

    private static MCH_HudItem parseLine(Map<String, Object> value) {
        boolean isStriped = false;
        String pattern = "0";
        String factor = "0";
        List<List<String>> positions = new ArrayList<>();

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Striped" -> isStriped = (Boolean) entry.getValue();
                case "Fac", "Factor" -> factor = toFormula((String) entry.getValue());
                case "Pat", "Pattern" -> pattern = toFormula((String) entry.getValue());
                case "StartPos", "Position", "Start" -> {
                    Object raw = entry.getValue();

                    if (raw instanceof List<?>rawList) {
                        for (Object pairObj : rawList) {
                            if (pairObj instanceof List<?>pair && pair.size() == 2) {
                                String x = MCH_HudItem.toFormula(pair.get(0).toString());
                                String y = MCH_HudItem.toFormula(pair.get(1).toString());
                                positions.add(Arrays.asList(x, y));
                            } else {
                                throw new IllegalArgumentException(
                                        "Each position must be a list of exactly 2 elements.");
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Position field must be a list of lists.");
                    }
                }
                default -> logUnkownEntry(entry, "Line");
            }
        }

        if (positions.isEmpty()) {
            throw new IllegalArgumentException("At least one position is required for Line element.");
        }

        // Flatten the list of lists into a single String[] array
        String[] coordsArr = positions.stream()
                .flatMap(List::stream)
                .toArray(String[]::new);

        return isStriped ? new MCH_HudItemLineStipple(0, pattern, factor, coordsArr) :
                new MCH_HudItemLine(0, coordsArr);
    }

    private static MCH_HudItem parseCameraRot(Map<String, Object> value) {
        String xCoord = null;
        String yCoord = null;

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Pos fields are required for drawTexture element.");

        return new MCH_HudItemCameraRot(0, xCoord, yCoord);
    }

    private static MCH_HudItemRect parseRact(Map<String, Object> value) {
        String xCoord = null;
        String yCoord = null;
        String width = null;
        String height = null;

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Size" -> {
                    Tuple<String, String> size = setTuple(Arrays.asList("width", "height"), entry.getValue());
                    width = MCH_HudItem.toFormula(size.getFirst());
                    height = MCH_HudItem.toFormula(size.getSecond());
                }
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Pos fields are required for drawTexture element.");

        return new MCH_HudItemRect(0, xCoord, yCoord, width, height);
    }

    private static MCH_HudItemString parseDrawString(Map<String, Object> map) {
        String text = null;
        List<String> varSubstitute = null;
        String xCoord = null;
        String yCoord = null;
        boolean center = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Text" -> {
                    var formatEntry = (Map<String, Object>) entry.getValue();
                    text = (String) MCH_Utils.getAny(formatEntry, Arrays.asList("Fmt", "Format"), null);
                    varSubstitute = (List<String>) MCH_Utils.getAny(formatEntry, Arrays.asList("Vars", "Variables"),
                            null);
                }
                case "Center" -> center = (Boolean) entry.getValue();
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    xCoord = pos.getFirst();
                    yCoord = pos.getSecond();
                }
                default -> logUnkownEntry(entry, "DrawString");

            }
        }

        if (xCoord == null || yCoord == null || text == null)
            throw new IllegalArgumentException("Pos, Format, fields are required for drawTexture element.");

        String[] args = Stream.concat(
                Stream.of(xCoord, yCoord, text),
                varSubstitute != null ? varSubstitute.stream() : Stream.empty()).toArray(String[]::new);

        return new MCH_HudItemString(0, xCoord, yCoord, text, args, center);
    }

    private static MCH_HudItemTexture parseDrawTexture(Map<String, Object> map) {
        String name = null;
        String xCoord = null;
        String yCoord = null;
        String width = null;
        String height = null;
        String uLeft = null;
        String vTop = null;
        String uWidth = null;
        String vHeight = null;
        String rot = "0";

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Texture", "Name" -> name = (String) entry.getValue();
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Size" -> {
                    Tuple<String, String> size = setTuple(Arrays.asList("width", "height"), entry.getValue());
                    width = MCH_HudItem.toFormula(size.getFirst());
                    height = MCH_HudItem.toFormula(size.getSecond());
                }
                case "UVPos" -> {
                    Tuple<String, String> uvPos = setTuple(Arrays.asList("x", "y"), entry.getValue());
                    uLeft = MCH_HudItem.toFormula(uvPos.getFirst());
                    vTop = MCH_HudItem.toFormula(uvPos.getSecond());
                }
                case "UVSize" -> {
                    Tuple<String, String> uvSize = setTuple(Arrays.asList("width", "height"), entry.getValue());
                    uWidth = MCH_HudItem.toFormula(uvSize.getFirst());
                    vHeight = MCH_HudItem.toFormula(uvSize.getSecond());
                }
                case "Rotation", "Rot" -> rot = toFormula((String) entry.getValue());
                default -> logUnkownEntry(entry, "DrawTexture");
            }
        }

        if (name == null || xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Texture, Pos fields are required for drawTexture element.");

        return new MCH_HudItemTexture(0, name, xCoord, yCoord, width, height, uLeft, vTop, uWidth, vHeight, rot);
    }

    private static void parseHudItem(MCH_Hud info, Object obj) {
        if (obj instanceof Map<?, ?>map) {
            if (map.containsKey("If")) {
                parseConditional(info, (LinkedHashMap<String, Object>) map);
            } else {
                Map.Entry<String, Object> entry = ((Map<String, Object>) map).entrySet().iterator().next();
                var element = parseHUDCommands(entry);
                if (element == null) logUnkownEntry(entry, "Hud");
                else info.list.add(element);
            }
        } else {
            throw new IllegalArgumentException("Each HUD item must be a Map");
        }
    }

    private static void parseConditional(MCH_Hud info, Map<String, Object> map) {
        String condition = ((String) map.get("If")).trim();
        if (condition == null || condition.isEmpty())
            throw new IllegalArgumentException("Condition cannot be blank!");

        var conditional = new MCH_HudItemConditional(0, false, condition);
        info.list.add(conditional);

        List<Object> doBlock = (List<Object>) map.get("Do");
        if (doBlock.isEmpty())
            MCH_Logger.get().warn("Hud " + info.getLocation().getPath() + " contains empty conditionals!");

        for (Object obj : doBlock) {
            if (obj instanceof Map<?, ?>objMap) {
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) objMap).entrySet()) {
                    MCH_HudItem element = parseHUDCommands(entry);
                    if (element != null) info.list.add(element);
                    else logUnkownEntry(entry, "Hud");
                }
            } else {
                throw new IllegalArgumentException("Each Do block item must be a Map");
            }
        }

        info.list.add(new MCH_HudItemConditional(0, true, null));
    }

    public static enum GraduationType {
        YAW,
        PITCH,
        PITCH_ROLL,
        PITCH_ROLL_ALT;
    }
}
