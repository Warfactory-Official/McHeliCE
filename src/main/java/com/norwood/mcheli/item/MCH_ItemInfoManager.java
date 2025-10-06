package com.norwood.mcheli.item;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentParseException;
import com.norwood.mcheli.helper.info.parsers.txt.TxtParser;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import net.minecraft.item.Item;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MCH_ItemInfoManager {
    private static final Map<String, MCH_ItemInfo> MAP = new LinkedHashMap<>();

    static {
        TxtParser.register();
        YamlParser.register();
    }

    public static boolean load(String path) {
        path = path.replace('\\', '/');
        File dir = new File(path);
        File[] files = dir.listFiles(pathname -> {
            String s = pathname.getName().toLowerCase();
            return pathname.isFile() && s.length() >= 5 && s.substring(s.length() - 4).equals(".txt");
        });

        if (files == null || files.length == 0) {
            return false;
        }

        for (File file : files) {
            String name = file.getName().toLowerCase();
            name = name.substring(0, name.length() - 4);
            if (MAP.containsKey(name)) {
                continue;
            }

            try {
                AddonResourceLocation location = MCH_Utils.buildinAddon(name);
                java.util.List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                MCH_ItemInfo info = TxtParser.INSTANCE.parseItem(location, file.getCanonicalPath(), lines, false);
                if (info != null && info.validate()) {
                    MAP.put(name, info);
                }
            } catch (ContentParseException ex) {
                MCH_Lib.Log("### Load failed %s : line=%d", file.getName(), ex.getLineNo());
                ex.printStackTrace();
            } catch (Exception ex) {
                MCH_Lib.Log("### Load failed %s", file.getName());
                ex.printStackTrace();
            }
        }

        MCH_Lib.Log("Read %d item", MAP.size());
        return !MAP.isEmpty();
    }

    public static MCH_ItemInfo get(String name) {
        return MAP.get(name);
    }

    public static MCH_ItemInfo get(Item item) {
        for (MCH_ItemInfo info : MAP.values()) {
            if (info.item == item) {
                return info;
            }
        }
        return null;
    }

    public static boolean contains(String name) {
        return MAP.containsKey(name);
    }

    public static Set<String> getKeySet() {
        return MAP.keySet();
    }

    public static Collection<MCH_ItemInfo> getValues() {
        return MAP.values();
    }
}
