package com.norwood.mcheli.sound;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.info.parsers.yaml.YamlParser;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundList;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SoundRegistry {

    public static final String SND = "_snd";
    public static Map<ResourceLocation, SoundList> soundMap = new HashMap<>();

    static {
        put("mcheli:a-10_snd", entry("mcheli:a-10_snd"));
        put("mcheli:a10gau8_snd", entry("mcheli:a10gau8_snd"));
        put("mcheli:alert", entry("mcheli:alert"));
        put("mcheli:boat", entry("mcheli:boat"));
        put("mcheli:bomb_snd", entry("mcheli:bomb_snd"));
        put("mcheli:cannon_1_snd", entry("mcheli:cannon_1_snd"));
        put("mcheli:cannon_2_snd", entry("mcheli:cannon_2_snd"));
        put("mcheli:cannon_3_snd", entry("mcheli:cannon_3_snd"));
        put("mcheli:cannon_4_snd", entry("mcheli:cannon_4_snd"));
        put("mcheli:chain", entry("mcheli:chain"));
        put("mcheli:chain_ct", entry("mcheli:chain_ct"));
        put("mcheli:eject_seat", entry("mcheli:eject_seat"));
        put("mcheli:fim92_reload", entry("mcheli:fim92_reload"));
        put("mcheli:fim92_snd", entry("mcheli:fim92_snd"));
        put("mcheli:gau-8_snd", entry("mcheli:gau-8_snd"));
        put("mcheli:gltd", entry("mcheli:gltd"));
        put("mcheli:gun_h1_snd", entry("mcheli:gun_h1_snd"));
        put("mcheli:gun_h2_snd", entry("mcheli:gun_h2_snd"));
        put("mcheli:gun_h3_snd", entry("mcheli:gun_h3_snd"));
        put("mcheli:gun_h4_snd", entry("mcheli:gun_h4_snd"));
        put("mcheli:gun_h5_snd", entry("mcheli:gun_h5_snd"));
        put("mcheli:gun_h6_snd", entry("mcheli:gun_h6_snd"));
        put("mcheli:gun_h7_snd", entry("mcheli:gun_h7_snd"));
        put("mcheli:gun_l1_snd", entry("mcheli:gun_l1_snd"));
        put("mcheli:gun_l2_snd", entry("mcheli:gun_l2_snd"));
        put("mcheli:gun_l3_snd", entry("mcheli:gun_l3_snd"));
        put("mcheli:gun_l4_snd", entry("mcheli:gun_l4_snd"));
        put("mcheli:hawk_snd", entry("mcheli:hawk_snd"));
        put("mcheli:heli", entry("mcheli:heli"));
        put("mcheli:helidmg", entry("mcheli:helidmg"));
        put("mcheli:heli_k", entry("mcheli:heli_k"));
        put("mcheli:hit", entry("mcheli:hit"));
        put("mcheli:locked", entry("mcheli:locked"));
        put("mcheli:lockon", entry("mcheli:lockon"));
        put("mcheli:mbtrun", entry("mcheli:mbtrun"));
        put("mcheli:mbt_run", entry("mcheli:mbt_run"));
        put("mcheli:missile_1_snd", entry("mcheli:missile_1_snd"));
        put("mcheli:missile_2_snd", entry("mcheli:missile_2_snd"));
        put("mcheli:missile_3_snd", entry("mcheli:missile_3_snd"));
        put("mcheli:missile_4_snd", entry("mcheli:missile_4_snd"));
        put("mcheli:mk19_l_snd", entry("mcheli:mk19_l_snd"));
        put("mcheli:mk19_r_snd", entry("mcheli:mk19_r_snd"));
        put("mcheli:mw-1_snd", entry("mcheli:mw-1_snd"));
        put("mcheli:ng", entry("mcheli:ng"));
        put("mcheli:pi", entry("mcheli:pi"));
        put("mcheli:plane", entry("mcheli:plane"));
        put("mcheli:plane_cc", entry("mcheli:plane_cc"));
        put("mcheli:plane_cv", entry("mcheli:plane_cv"));
        put("mcheli:plastic_bomb_snd", entry("mcheli:plastic_bomb_snd"));
        put("mcheli:prop", entry("mcheli:prop"));
        put("mcheli:r44_heli", entry("mcheli:r44_heli"));
        put("mcheli:radicon_heli", entry("mcheli:radicon_heli"));
        put("mcheli:rocket", entry("mcheli:rocket"));
        put("mcheli:rocket_snd", entry("mcheli:rocket_snd"));
        put("mcheli:rr_griffon", entry("mcheli:rr_griffon"));
        put("mcheli:rr_merlin", entry("mcheli:rr_merlin"));
        put("mcheli:sa-2_snd", entry("mcheli:sa-2_snd"));
        put("mcheli:smoke_snd", entry("mcheli:smoke_snd"));
        put("mcheli:tank", entry("mcheli:tank"));
        put("mcheli:tank_gte", entry("mcheli:tank_gte"));
        put("mcheli:turboprop", entry("mcheli:turboprop"));
        put("mcheli:vehicle_drive", entry("mcheli:vehicle_drive"));
        put("mcheli:vehicle_run", entry("mcheli:vehicle_run"));
        put("mcheli:xm301_snd", entry("mcheli:xm301_snd"));
        put("mcheli:zoom", entry("mcheli:zoom"));

        put("wrench", entry("mcheli:wrench1", "mcheli:wrench2", "mcheli:wrench3"));
    }

    @SideOnly(Side.CLIENT)
    public static void commitChanges(SoundHandler handler) {
        for (Map.Entry<ResourceLocation, SoundList> entry : soundMap.entrySet())
            handler.loadSoundResource(entry.getKey(), entry.getValue());
    }




    private static void put(String event, SoundList list) {
        soundMap.put(new ResourceLocation(event), list);
    }

    private static void put(ResourceLocation event, SoundList list) {
        soundMap.put(event, list);
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
        ResourceLocation resource = new ResourceLocation(Tags.MODID, (String) ymlEntry.get("Name"));
        if (!MCH_MOD.proxy.isRemote()) {
            return resource;
        }

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
        ResourceLocation resource = new ResourceLocation(Tags.MODID, loc.startsWith(Tags.MODID + ":") ? loc.split(":")[1] : loc);
        if (!MCH_MOD.proxy.isRemote()) return resource; //Serverside just returns
        StringBuilder snd = new StringBuilder();
        snd.append(resource.toString());
        if (!loc.endsWith(SND) && !validateSoundResource(resource))
            snd.append(SND);
        put(resource, entry(snd.toString()));
        return resource;
    }


    public static boolean validateSoundResource(ResourceLocation sound)
    {
        ResourceLocation resourcelocation =  new ResourceLocation(sound.getNamespace(),"sounds/" + sound.getPath() + ".ogg");
        IResource iresource = null;
        boolean flag;

        try
        {
            iresource = Minecraft.getMinecraft().getResourceManager().getResource(resourcelocation);
            iresource.getInputStream();
            return true;
        }
        catch (FileNotFoundException e)
        {
            flag = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally
        {
            IOUtils.closeQuietly(iresource);
        }

        return flag;
    }

    @NoArgsConstructor
    private static class SoundSpec {
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


