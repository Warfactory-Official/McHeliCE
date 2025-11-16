package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.helper.addon.GeneratedAddonPack;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class W_LanguageRegistry {

    private static HashMap<String, ArrayList<String>> map = new HashMap<>();

    public static void addName(Object objectToName, String name) {
        addNameForObject(objectToName, "en_us", name);
    }

    public static void addNameForObject(Object o, String lang, String name) {
        addNameForObject(o, lang, name, "", "");
    }

    public static void addNameForObject(Object o, String lang, String name, String key, String desc) {
        if (o != null) {
            lang = lang.toLowerCase(Locale.ROOT);
            if (!map.containsKey(lang)) {
                map.put(lang, new ArrayList<>());
            }

            if (o instanceof Item) {
                map.get(lang).add(((Item) o).getTranslationKey() + ".name=" + name);
            }

            if (o instanceof Block) {
                map.get(lang).add(((Block) o).getTranslationKey() + ".name=" + name);
            } else if (o instanceof Advancement) {
                map.get(lang).add("advancement." + key + "=" + name);
                map.get(lang).add("advancement." + key + ".desc=" + desc);
            }
        }
    }

    public static void clear() {
        map.clear();
        map = null;
    }

    public static void updateGeneratedLang() {
        GeneratedAddonPack.instance().checkMkdirsAssets("lang");

        for (String key : map.keySet()) {
            ArrayList<String> list = map.get(key);
            GeneratedAddonPack.instance().updateAssetFile("lang/" + key + ".lang", list);
        }

        clear();
    }
}
