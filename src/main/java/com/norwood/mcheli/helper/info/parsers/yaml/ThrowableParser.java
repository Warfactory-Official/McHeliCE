package com.norwood.mcheli.helper.info.parsers.yaml;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_BoundingBox;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.getClamped;
import static com.norwood.mcheli.helper.info.parsers.yaml.YamlParser.logUnkownEntry;

public class ThrowableParser {

    public void parse(MCH_ThrowableInfo info, Map<String,Object> root){
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "DisplayName" -> {
                    Object nameObject = entry.getValue();
                    if (nameObject instanceof String name) info.displayName = name.trim();
                    else if (nameObject instanceof Map<?, ?> translationNames) {
                        var userNameMap = (Map<String, String>) translationNames;
                        if (userNameMap.containsKey("DEFAULT")) {
                            info.displayName = userNameMap.get("DEFAULT");
                            userNameMap.remove("DEFAULT");
                        }
                        info.displayNameLang.putAll( (Map<String, String>) userNameMap);
                    } else throw new ClassCastException();
                }
                case "Author" -> {
                    //Proposal: would allow content creators to put their signature
                }
                //Depricated on 1,12, around for 1.7 compat
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
                            case "Pattern" ->
                                    info.recipeString = ((List<String>) recMapEntry.getValue()).stream().map(String::toUpperCase).map(String::trim).collect(Collectors.toList());
                        }
                    }

                }

                default -> logUnkownEntry(entry, "ThrowableInfo");
            }
        }



    }

    private void parseSound(Map<String, Object> soundSettings, MCH_ThrowableInfo info) {

        for (Map.Entry<String, Object> entry : soundSettings.entrySet()) {
            switch (entry.getKey()) {
                case "Volume", "Vol" -> info.soundVolume = getClamped(10F, entry.getValue());
                case "Pitch" -> info.soundVolume = getClamped(1F, 10F, entry.getValue());
            }

        }
    }
}
