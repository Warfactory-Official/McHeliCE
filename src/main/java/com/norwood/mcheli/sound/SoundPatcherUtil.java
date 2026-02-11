package com.norwood.mcheli.sound;


import com.google.gson.*;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.MCH_Logger;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

//Terrible issues require terrible solutions
public class SoundPatcherUtil {

    @SneakyThrows
    public static void patchSounds() {
        List<Path> jsonSounds = discoverSoundJsons();
        if (jsonSounds.isEmpty()) return;
        var soundJsonList = serializeSoundJsons(jsonSounds);
        MCH_Logger.log("Patching sound jsons...");
        patchSoundJsons(soundJsonList);
        saveJsonObject(soundJsonList);
    }

    private static void patchSoundJsons(List<SoundJson> soundJsons) {
        var iterator = soundJsons.iterator();
        while (iterator.hasNext()) {
            var jsound = iterator.next();

            if (jsound.object.has("mchce_patched")) {
                iterator.remove();
                continue;
            }

            for (var entry : jsound.object.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    JsonObject soundData = entry.getValue().getAsJsonObject();

                    if (soundData.has("sounds") && soundData.get("sounds").isJsonArray()) {
                        JsonArray soundsArray = soundData.getAsJsonArray("sounds");

                        for (int i = 0; i < soundsArray.size(); i++) {
                            String currentSound = soundsArray.get(i).getAsString();
                            if (!currentSound.contains(":")) {
                                soundsArray.set(i, new JsonPrimitive(Tags.MODID + ":" + currentSound));
                            }
                        }
                    }
                }
            }

            JsonObject markerEvent = new JsonObject();
            markerEvent.addProperty("category", "master");
            JsonArray fakeSounds = new JsonArray();
            fakeSounds.add("patched_marker");
            markerEvent.add("sounds", fakeSounds);
            markerEvent.addProperty("_note", "Patched by " + Tags.MODNAME + " " + Tags.VERSION);

            jsound.object.add("mchce_patched", markerEvent);
        }
    }

    public static void saveJsonObject(List<SoundJson> soundJsons) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        for (var jsound : soundJsons) {
            try (BufferedWriter writer = Files.newBufferedWriter(jsound.path)) {
                gson.toJson(jsound.object, writer);
            }
        }
    }

    private static List<SoundJson> serializeSoundJsons(List<Path> paths) throws IOException {
        List<SoundJson> soundJsons = new ArrayList<>(paths.size());
        var parser = new JsonParser();

        for (var path : paths) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                JsonObject jsonObject = parser.parse(reader).getAsJsonObject();
                soundJsons.add(new SoundJson(path, jsonObject));
            }
        }
        return soundJsons;
    }

    private static List<Path> discoverSoundJsons() throws IOException {
        Path addonPath = Minecraft.getMinecraft().gameDir.toPath().resolve("mcheli_addons");
        List<Path> jsonPaths = new ArrayList<>();
        if (!Files.exists(addonPath)) return jsonPaths;

        try (Stream<Path> stream = Files.walk(addonPath, 4)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("sounds.json"))
                    .forEach(jsonPaths::add);
        }
        return jsonPaths;
    }

    record SoundJson(Path path, JsonObject object) {}
}