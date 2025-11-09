package com.norwood.mcheli.helper.info.parsers.yaml;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;

@SuppressWarnings("unchecked")
public class ThrowableParser {

    private ThrowableParser() {}

    public static void parse(MCH_ThrowableInfo info, Map<String, Object> root) {
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
                        info.displayNameLang.putAll((Map<String, String>) userNameMap);
                    } else throw new ClassCastException();
                }
                case "Author" -> {
                    // Proposal: would allow content creators to put their signature
                }
                // Depricated on 1,12, around for 1.7 compat
                case "ItemID" -> {
                    info.itemID = (int) entry.getValue();
                }

                case "Sound" -> {
                    Map<String, Object> soundSettings = (Map<String, Object>) entry.getValue();
                    parseSound(soundSettings, info);
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
                case "Power" -> info.power = getClamped(0, Integer.MAX_VALUE, entry.getValue());

                case "Acceleration" -> info.acceleration = getClamped(100.0F, entry.getValue());

                case "AccelerationInWater" -> info.accelerationInWater = getClamped(100.0F, entry.getValue());

                case "DispenseAcceleration" -> info.dispenseAcceleration = getClamped(1000.0F, entry.getValue());

                case "Explosion" -> info.explosion = getClamped(0, 50, entry.getValue());

                case "DelayFuse" -> info.delayFuse = getClamped(0, 100000, entry.getValue());

                case "Bound" -> info.bound = getClamped(100000.0F, entry.getValue());

                case "TimeFuse" -> info.timeFuse = getClamped(0, 100000, entry.getValue());

                case "Flaming" -> info.flaming = (Boolean) entry.getValue();

                case "StackSize" -> info.stackSize = getClamped(1, 64, entry.getValue());

                case "ProximityFuseDist" -> info.proximityFuseDist = getClamped(20.0F, entry.getValue());

                case "Accuracy" -> info.accuracy = getClamped(1000.0F, entry.getValue());

                case "AliveTime" -> info.aliveTime = getClamped(0, 1_000_000, entry.getValue());

                case "Bomblet" -> info.bomblet = getClamped(0, 1000, entry.getValue());

                case "BombletSpread" -> info.bombletDiff = getClamped(1000.0F, entry.getValue());

                case "Gravity" -> info.gravity = getClamped(-50.0F, 50.0F, entry.getValue());

                case "GravityInWater" -> info.gravityInWater = getClamped(-50.0F, 50.0F, entry.getValue());

                case "Particle" -> info.particleName = ((String) entry.getValue()).trim().toLowerCase();

                case "Smoke" -> parseSmoke(info, (Map<String, Object>) entry.getValue());
                default -> logUnkownEntry(entry, "ThrowableInfo");
            }
        }
    }

    private static void parseSmoke(MCH_ThrowableInfo info, Map<String, Object> soundSettings) {
        for (Map.Entry<String, Object> entry : soundSettings.entrySet()) {
            switch (entry.getKey()) {
                case "DisableSmoke" -> info.disableSmoke = (Boolean) entry.getValue();
                case "Size" -> info.smokeSize = getClamped(1000.0F, entry.getValue());

                case "Count" -> info.smokeNum = getClamped(0, 1000, entry.getValue());

                case "Color" -> {
                    info.smokeColor = new MCH_Color(parseHexColor(((String) entry.getValue()).trim()));
                }
                case "Velocity" -> {
                    var velMap = (Map<String, Object>) entry.getValue();

                    for (Map.Entry<String, Object> velEntry : velMap.entrySet()) {
                        switch (entry.getKey()) {
                            case "Vertical", "Y" -> info.smokeVelocityVertical = getClamped(-100.0F, 100.0F,
                                    velEntry.getValue());
                            case "Horizontal", "X" -> info.smokeVelocityHorizontal = getClamped(1000.0F,
                                    velEntry.getValue());
                        }

                    }
                }

                default -> logUnkownEntry(entry, "Smoke");
            }

        }
    }

    private static void parseSound(Map<String, Object> soundSettings, MCH_ThrowableInfo info) {
        for (Map.Entry<String, Object> entry : soundSettings.entrySet()) {
            switch (entry.getKey()) {
                case "Volume", "Vol" -> info.soundVolume = getClamped(10F, entry.getValue());
                case "Pitch" -> info.soundPitch = getClamped(1F, 10F, entry.getValue());
                default -> logUnkownEntry(entry, "Sound");
            }

        }
    }
}
