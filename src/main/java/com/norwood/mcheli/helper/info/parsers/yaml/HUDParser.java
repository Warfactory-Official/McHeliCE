package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.hud.*;
import net.minecraft.util.Tuple;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.logUnkownEntry;
import static com.norwood.mcheli.hud.MCH_HudItem.toFormula;

//Dude this is so fucking stupid, we should just move to groovy but muh 1.7 compat... ugh
@SuppressWarnings("unchecked")
public class HUDParser {

    public static Tuple<String, String> setTuple(List<String> mapKeys, Object object) {
        if (object instanceof List<?> tupleList) {
            if (tupleList.size() != 2) throw new IllegalArgumentException("Tuple list must have exactly 2 variables");
            List<String> tupleListTyped = toStringList(tupleList);
            return new Tuple<>(tupleListTyped.get(0), tupleListTyped.get(1));
        } else if (object instanceof Map<?, ?> map) {
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
        if (map.isEmpty()) return Map.of();
        Object firstValue = map.values().iterator().next();
        if (firstValue instanceof String) {
            return (Map<String, String>) map;
        }
        if (firstValue instanceof Number) {
            return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> ((Number) e.getValue()).toString()));
        }
        throw new ClassCastException("Map values are not String or Number");
    }

    public void parse(MCH_Hud info, LinkedHashMap<String, Object> root) {
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (entry.getKey().equals("Conditional")) {
                parseConditional(info, (LinkedHashMap<String, Object>) entry.getValue());
            } else {
                var element  = parseHUDCommands(entry);
                if(element == null)
                    logUnkownEntry(entry, "Hud");
                else info.list.add(element);
            }
        }


    }

    private MCH_HudItem parseHUDCommands(Map.Entry<String, Object> entry) {
        switch (entry.getKey()) {
            case "Color" -> {
                return new MCH_HudItemColor(0, toFormula((String) entry.getValue()));

            }
            case "DrawTexture" -> {
                return parseDrawTexture((Map<String, Object>) entry.getValue());
            }

            case "DrawString" -> {
                return parseDrawString((Map<String, Object>) entry.getValue());
            }

            case "CameraRot" -> {
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
                return parseRadar((Map<String,Object>) entry.getValue());
            }

            case "DrawGraduation" -> {
                return parseGraduation((Map<String,Object>) entry.getValue());
            }

            default -> {
                return null;
            }
        }

    }

    private MCH_HudItem parseGraduation(Map<String, Object> value) {
        GraduationType type = null;
        String xCoord = null;
        String yCoord = null;
        String rot = "0";
        String roll = "0";

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Type" -> type = GraduationType.valueOf(((String) entry.getValue()).toUpperCase(Locale.ROOT).trim());
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Rotation", "Rot" -> rot = (String) entry.getValue();
                case "Roll"  -> roll = (String) entry.getValue();
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Texture, Pos fields are required for drawTexture element.");
        return new MCH_HudItemGraduation(0, type == null? -1:type.ordinal(), rot,roll,xCoord,yCoord);
    }

    public static enum GraduationType{
        YAW,PITCH,PITCH_ROLL,PITCH_ROLL_ALT;
    }

    private MCH_HudItemRadar parseRadar(Map<String, Object> value) {
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
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Size" -> {
                    Tuple<String, String> size = setTuple(List.of("width", "height"), entry.getValue());
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

    private MCH_HudItem parseLine(Map<String, Object> value) {
        String xCoord = null;
        String yCoord = null;
        boolean isStriped = false;

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Striped" -> isStriped = (Boolean) entry.getValue();
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Pos fields are required for Line element.");
        String[] coordsArr = new String[]{xCoord,yCoord};
       if(isStriped) return new MCH_HudItemLineStipple(0, coordsArr);
       else return new MCH_HudItemLine(0, coordsArr);
    }



    private MCH_HudItem parseCameraRot(Map<String, Object> value) {
        String xCoord = null;
        String yCoord = null;

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
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

    private MCH_HudItemRect parseRact(Map<String, Object> value) {
        String xCoord = null;
        String yCoord = null;
        String width = null;
        String height = null;

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            switch (entry.getKey()) {
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Size" -> {
                    Tuple<String, String> size = setTuple(List.of("width", "height"), entry.getValue());
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

    private MCH_HudItemString parseDrawString(Map<String, Object> map) {
        String format = null;
        String xCoord = null;
        String yCoord = null;
        String[] args = null;
        boolean center = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "Format" -> format = toFormula((String) entry.getValue());
                case "Arguments", "Arg" -> args = ((List<String>) entry.getValue()).toArray(String[]::new);
                case "Center" -> center = (Boolean) entry.getValue();
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
                    xCoord = pos.getFirst();
                    yCoord = pos.getSecond();
                }
                default -> logUnkownEntry(entry, "VehicleFeatures");

            }
        }

        if (xCoord == null || yCoord == null || args == null)
            throw new IllegalArgumentException("Pos, Arguments fields are required for drawTexture element.");

        return new MCH_HudItemString(0, xCoord, yCoord, format, args, center);

    }

    private MCH_HudItemTexture parseDrawTexture(Map<String, Object> map) {
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
                case "Texture" -> name = (String) entry.getValue();
                case "Pos", "Position" -> {
                    Tuple<String, String> pos = setTuple(List.of("x", "y"), entry.getValue());
                    xCoord = MCH_HudItem.toFormula(pos.getFirst());
                    yCoord = MCH_HudItem.toFormula(pos.getSecond());
                }
                case "Size" -> {
                    Tuple<String, String> size = setTuple(List.of("width", "height"), entry.getValue());
                    width = MCH_HudItem.toFormula(size.getFirst());
                    height = MCH_HudItem.toFormula(size.getSecond());
                }
                case "UVPos" -> {
                    Tuple<String, String> uvPos = setTuple(List.of("x", "y"), entry.getValue());
                    uLeft = MCH_HudItem.toFormula(uvPos.getFirst());
                    vTop = MCH_HudItem.toFormula(uvPos.getSecond());
                }
                case "UVSize" -> {
                    Tuple<String, String> uvSize = setTuple(List.of("width", "height"), entry.getValue());
                    uWidth = MCH_HudItem.toFormula(uvSize.getFirst());
                    vHeight = MCH_HudItem.toFormula(uvSize.getSecond());
                }
                case "Rotation", "Rot" -> rot = toFormula((String) entry.getValue());
                default -> logUnkownEntry(entry, "VehicleFeatures");
            }
        }

        if (name == null || xCoord == null || yCoord == null)
            throw new IllegalArgumentException("Texture, Pos fields are required for drawTexture element.");

        return new MCH_HudItemTexture(0, name, xCoord, yCoord, width, height, uLeft, vTop, uWidth, vHeight, rot);

    }

    private void parseConditional(MCH_Hud info, LinkedHashMap<String, Object> map) {
        String condition = ((String) map.get("If")).trim();
        if (condition == null || condition.isEmpty()) throw new IllegalArgumentException("Condition cannot be blank!");
        var conditional = new MCH_HudItemConditional(0, false, condition);
        var doBlock = (LinkedHashMap<String, Object>) map.get("Do");
        if (doBlock.isEmpty()) throw new IllegalArgumentException("Conditional cannot do nothing!");

        List<MCH_HudItem> parsedDo = doBlock.entrySet().stream().map(this::parseHUDCommands).collect(Collectors.toList());

        info.list.add(conditional);
        info.list.addAll(parsedDo);
        info.list.add(new MCH_HudItemConditional(0, true, null));
    }
}
