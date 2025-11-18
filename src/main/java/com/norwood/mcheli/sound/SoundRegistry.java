package com.norwood.mcheli.sound;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import lombok.NoArgsConstructor;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundList;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SoundRegistry {

    public static final String SND = "_snd";
    public static Map<ResourceLocation, SoundList> soundMap = new HashMap<>();

    static {
        put("a-10_snd", entry("mcheli:a-10_snd"));
        put("a10gau8_snd", entry("mcheli:a10gau8_snd"));
        put("alert", entry("mcheli:alert"));
        put("boat", entry("mcheli:boat"));
        put("bomb_snd", entry("mcheli:bomb_snd"));
        put("cannon_1_snd", entry("mcheli:cannon_1_snd"));
        put("cannon_2_snd", entry("mcheli:cannon_2_snd"));
        put("cannon_3_snd", entry("mcheli:cannon_3_snd"));
        put("cannon_4_snd", entry("mcheli:cannon_4_snd"));
        put("chain", entry("mcheli:chain"));
        put("chain_ct", entry("mcheli:chain_ct"));
        put("eject_seat", entry("mcheli:eject_seat"));
        put("fim92_reload", entry("mcheli:fim92_reload"));
        put("fim92_snd", entry("mcheli:fim92_snd"));
        put("gau-8_snd", entry("mcheli:gau-8_snd"));
        put("gltd", entry("mcheli:gltd"));
        put("gun_h1_snd", entry("mcheli:gun_h1_snd"));
        put("gun_h2_snd", entry("mcheli:gun_h2_snd"));
        put("gun_h3_snd", entry("mcheli:gun_h3_snd"));
        put("gun_h4_snd", entry("mcheli:gun_h4_snd"));
        put("gun_h5_snd", entry("mcheli:gun_h5_snd"));
        put("gun_h6_snd", entry("mcheli:gun_h6_snd"));
        put("gun_h7_snd", entry("mcheli:gun_h7_snd"));
        put("gun_l1_snd", entry("mcheli:gun_l1_snd"));
        put("gun_l2_snd", entry("mcheli:gun_l2_snd"));
        put("gun_l3_snd", entry("mcheli:gun_l3_snd"));
        put("gun_l4_snd", entry("mcheli:gun_l4_snd"));
        put("hawk_snd", entry("mcheli:hawk_snd"));
        put("heli", entry("mcheli:heli"));
        put("helidmg", entry("mcheli:helidmg"));
        put("heli_k", entry("mcheli:heli_k"));
        put("hit", entry("mcheli:hit"));
        put("locked", entry("mcheli:locked"));
        put("lockon", entry("mcheli:lockon"));
        put("mbtrun", entry("mcheli:mbtrun"));
        put("mbt_run", entry("mcheli:mbt_run"));
        put("missile_1_snd", entry("mcheli:missile_1_snd"));
        put("missile_2_snd", entry("mcheli:missile_2_snd"));
        put("missile_3_snd", entry("mcheli:missile_3_snd"));
        put("missile_4_snd", entry("mcheli:missile_4_snd"));
        put("mk19_l_snd", entry("mcheli:mk19_l_snd"));
        put("mk19_r_snd", entry("mcheli:mk19_r_snd"));
        put("mw-1_snd", entry("mcheli:mw-1_snd"));
        put("ng", entry("mcheli:ng"));
        put("pi", entry("mcheli:pi"));
        put("plane", entry("mcheli:plane"));
        put("plane_cc", entry("mcheli:plane_cc"));
        put("plane_cv", entry("mcheli:plane_cv"));
        put("plastic_bomb_snd", entry("mcheli:plastic_bomb_snd"));
        put("prop", entry("mcheli:prop"));
        put("r44_heli", entry("mcheli:r44_heli"));
        put("radicon_heli", entry("mcheli:radicon_heli"));
        put("rocket", entry("mcheli:rocket"));
        put("rocket_snd", entry("mcheli:rocket_snd"));
        put("rr_griffon", entry("mcheli:rr_griffon"));
        put("rr_merlin", entry("mcheli:rr_merlin"));
        put("sa-2_snd", entry("mcheli:sa-2_snd"));
        put("smoke_snd", entry("mcheli:smoke_snd"));
        put("tank", entry("mcheli:tank"));
        put("tank_gte", entry("mcheli:tank_gte"));
        put("turboprop", entry("mcheli:turboprop"));
        put("vehicle_drive", entry("mcheli:vehicle_drive"));
        put("vehicle_run", entry("mcheli:vehicle_run"));
        put("xm301_snd", entry("mcheli:xm301_snd"));
        put("zoom", entry("mcheli:zoom"));

        put("wrench", entry("mcheli:wrench1", "mcheli:wrench2", "mcheli:wrench3"));
    }

    @SideOnly(Side.CLIENT)
    public static void commitChanges(SoundHandler handler){
        for(Map.Entry<ResourceLocation,SoundList> entry : soundMap.entrySet())
            handler.loadSoundResource(entry.getKey(),entry.getValue());

    }
    public static void registerSoundEvents(){

    }


    private static void put(String event, SoundList list) {
        soundMap.put(new ResourceLocation("mcheli", event), list);
    }


    private static SoundList entry(String... paths) {
        List<Sound> entries = new ArrayList<>();
        for (String path : paths) {
            Sound e = new Sound(
                    path,
                    1.0F, // volume
                    1.0F, // pitch
                    1,    // weight
                    Sound.Type.FILE,
                    false // streaming
            );
            entries.add(e);
        }

        return new SoundList(entries, false, null);
    }


    public static ResourceLocation parseSound(Map<String, Object> ymlEntry) {
        if (!MCH_MOD.proxy.isRemote())
            return new ResourceLocation(Tags.MODID, (String) ymlEntry.get("Name"));

        String eventName = (String) ymlEntry.get("Name");
        List<SoundSpec> specs = new ArrayList<>();

        Object sounds = ymlEntry.get("Sounds");
        if (sounds instanceof String single) {
            specs.add(new SoundSpec(single));
        } else if (sounds instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof String s) {
                    specs.add(new SoundSpec(s));
                } else if (o instanceof Map<?, ?> map) {
                    specs.add(SoundSpec.fromYML((Map<String, Object>) map));
                }
            }
        }

        List<Sound> entries = specs.stream()
                .map(spec -> new Sound(spec.name, spec.volume, spec.pitch, spec.weight, Sound.Type.FILE, spec.streaming))
                .collect(Collectors.toList());

        SoundList list = new SoundList(entries, false, null);
        ResourceLocation resourceLocation = new ResourceLocation(Tags.MODID, eventName);
        soundMap.put(resourceLocation, list);

        return resourceLocation;
    }

    //TXT parser
    public static ResourceLocation parseSound(String loc) {
        if (!MCH_MOD.proxy.isRemote()) return new ResourceLocation(Tags.MODID, loc); //Serverside just returns
        StringBuilder snd = new StringBuilder();
        snd.append(Tags.MODID).append(loc);
        if (!loc.endsWith(SND))
            snd.append(SND);
        put(Tags.MODID + ":" + loc, entry(snd.toString()));
        return new ResourceLocation(Tags.MODID, loc);
    }

    @NoArgsConstructor
    public static class SoundSpec {
        public String name;
        float volume = 1f;
        float pitch = 1f;
        int weight = 1;
        boolean streaming = false;

        public SoundSpec(String name) {
            this.name = name;
        }

        public static SoundSpec fromYML(Map<String, Object> map) {
            var spec = new SoundSpec();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                switch (entry.getKey()) {
                    case "Name" -> spec.name = ((String) entry.getValue()).trim().toLowerCase();
                    case "Vol", "Volume" -> spec.volume = YamlParser.getClamped(1F, 0F, entry.getValue());
                    case "Pitch" -> spec.pitch = YamlParser.getClamped(1F, 0F, entry.getValue());
                    case "Weight" -> spec.weight = ((Number) entry.getValue()).intValue();
                    case "isStreamed", "Stream" -> spec.streaming = (Boolean) entry.getValue();
                }
            }

            return spec;
        }


    }
}


