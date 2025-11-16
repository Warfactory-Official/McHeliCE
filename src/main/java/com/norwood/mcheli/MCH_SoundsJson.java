package com.norwood.mcheli;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.addon.GeneratedAddonPack;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class MCH_SoundsJson {

    public static void updateGenerated() {
        File soundsDir = new File(MCH_MOD.getSource().getPath() + "/assets/mcheli/sounds");
        File[] soundFiles = soundsDir.listFiles(f -> {
            String s = f.getName().toLowerCase();
            return f.isFile() && s.length() >= 5 && s.substring(s.length() - 4).compareTo(".ogg") == 0;
        });
        Multimap<String, String> multimap = Multimaps.newListMultimap(Maps.newLinkedHashMap(), Lists::newLinkedList);
        List<String> lines = Lists.newLinkedList();
        int cnt = 0;
        if (soundFiles != null) {
            for (File f : soundFiles) {
                String name = f.getName().toLowerCase();
                int ei = name.lastIndexOf(".");
                name = name.substring(0, ei);
                String key = name;
                char c = name.charAt(name.length() - 1);
                if (c >= '0' && c <= '9') {
                    key = name.substring(0, name.length() - 1);
                }

                multimap.put(key, name);
            }

            lines.add("{");

            for (String key : multimap.keySet()) {
                cnt++;
                String sounds = Joiner.on(",")
                        .join(multimap.get(key).stream().map(namex -> '"' + MCH_Utils.suffix(namex).toString() + '"')
                                .collect(Collectors.toList()));
                String line = "\"" + key + "\": {\"category\": \"master\",\"sounds\": [" + sounds + "]}";
                if (cnt < multimap.keySet().size()) {
                    line = line + ",";
                }

                lines.add(line);
            }

            lines.add("}");
            lines.add("");
        }

        GeneratedAddonPack.instance().updateAssetFile("sounds.json", lines);
        MCH_Utils.logger().info("Update sounds.json, %d sounds.", cnt);
    }
}
