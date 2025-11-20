package com.norwood.mcheli.sound;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//NGL th
public class SoundRegistry {
    public static final String SND = "_snd";
    public static SoundRegistry INSTANCE = new SoundRegistry();

    static {
        add(rl("a-10_snd"));
        add(rl("a10gau8_snd"));
        add(rl("alert"));
        add(rl("boat"));
        add(rl("bomb_snd"));
        add(rl("cannon_1_snd"));
        add(rl("cannon_2_snd"));
        add(rl("cannon_3_snd"));
        add(rl("cannon_4_snd"));
        add(rl("chain"));
        add(rl("chain_ct"));
        add(rl("eject_seat"));
        add(rl("fim92_reload"));
        add(rl("fim92_snd"));
        add(rl("gau-8_snd"));
        add(rl("gltd"));
        add(rl("gun_h1_snd"));
        add(rl("gun_h2_snd"));
        add(rl("gun_h3_snd"));
        add(rl("gun_h4_snd"));
        add(rl("gun_h5_snd"));
        add(rl("gun_h6_snd"));
        add(rl("gun_h7_snd"));
        add(rl("gun_l1_snd"));
        add(rl("gun_l2_snd"));
        add(rl("gun_l3_snd"));
        add(rl("gun_l4_snd"));
        add(rl("hawk_snd"));
        add(rl("heli"));
        add(rl("helidmg"));
        add(rl("heli_k"));
        add(rl("hit"));
        add(rl("locked"));
        add(rl("lockon"));
        add(rl("mbtrun"));
        add(rl("mbt_run"));
        add(rl("missile_1_snd"));
        add(rl("missile_2_snd"));
        add(rl("missile_3_snd"));
        add(rl("missile_4_snd"));
        add(rl("mk19_l_snd"));
        add(rl("mk19_r_snd"));
        add(rl("mw-1_snd"));
        add(rl("ng"));
        add(rl("pi"));
        add(rl("plane"));
        add(rl("plane_cc"));
        add(rl("plane_cv"));
        add(rl("plastic_bomb_snd"));
        add(rl("prop"));
        add(rl("r44_heli"));
        add(rl("radicon_heli"));
        add(rl("rocket"));
        add(rl("rocket_snd"));
        add(rl("rr_griffon"));
        add(rl("rr_merlin"));
        add(rl("sa-2_snd"));
        add(rl("smoke_snd"));
        add(rl("tank"));
        add(rl("tank_gte"));
        add(rl("turboprop"));
        add(rl("vehicle_drive"));
        add(rl("vehicle_run"));
        add(rl("xm301_snd"));
        add(rl("zoom"));
        add(rl("wrench"));
    }
    private static ResourceLocation rl(String path) {
        return new ResourceLocation(Tags.MODID, path);
    }

    public final Set<ResourceLocation> soundSet = new HashSet<>();


    public SoundRegistry() {
        INSTANCE = this;
    }

    private static void add(ResourceLocation event) {
        INSTANCE.soundSet.add(event);
    }


    public static boolean validateSoundResource(ResourceLocation sound) {

        var resourcelocation = new ResourceLocation(sound.getNamespace(), "sounds/" + sound.getPath() + ".ogg");

        try (IResource iresource =
                     Minecraft.getMinecraft().getResourceManager().getResource(resourcelocation)) {
            iresource.getInputStream();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public ResourceLocation parseSound(Map<String, Object> ymlEntry) {
        String eventName = (String) ymlEntry.get("Name");
        ResourceLocation id = new ResourceLocation(Tags.MODID, eventName);

        soundSet.add(id);

        if (!MCH_MOD.proxy.isRemote()) {
            return id;
        }

        ClientSoundRegistry.INSTANCE.buildFromYML(id, ymlEntry);

        return id;
    }

    public ResourceLocation parseSound(String loc) {
        ResourceLocation id = new ResourceLocation(Tags.MODID,
                loc.startsWith(Tags.MODID + ":") ? loc.split(":")[1] : loc);

        soundSet.add(id);

        if (!MCH_MOD.proxy.isRemote()) return id;

        ClientSoundRegistry.INSTANCE.buildFromString(id, loc);

        return id;
    }

    @NoArgsConstructor
    protected static class SoundSpec {
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

