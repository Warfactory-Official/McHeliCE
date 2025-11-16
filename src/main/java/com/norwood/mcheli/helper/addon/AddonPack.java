package com.norwood.mcheli.helper.addon;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.io.ResourceLoader;
import net.minecraft.util.JsonUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AddonPack {

    private final String addonDomain;
    private final String addonName;
    private final String addonVersion;
    protected final ImmutableMap<String, JsonElement> packMetaMap;
    private final File addonFile;
    private final String credits;
    private final List<String> authors;
    private final String description;
    private final String loaderVersion;

    public AddonPack(
                     String addonDomain,
                     String addonName,
                     String addonVersion,
                     File addonFile,
                     String credits,
                     List<String> authors,
                     String description,
                     String loaderVersion,
                     ImmutableMap<String, JsonElement> packMetaMap) {
        this.addonDomain = addonDomain;
        this.addonName = addonName;
        this.addonVersion = addonVersion;
        this.addonFile = addonFile;
        this.credits = credits;
        this.authors = authors;
        this.description = description;
        this.loaderVersion = loaderVersion;
        this.packMetaMap = packMetaMap;
    }

    public static AddonPack create(File addonFile) {
        JsonObject packMetaJson = loadPackMeta(addonFile);
        JsonObject packJson = JsonUtils.getJsonObject(packMetaJson, "pack", new JsonObject());
        JsonObject addonJson = JsonUtils.getJsonObject(packMetaJson, "addon", new JsonObject());
        String addonDomain = JsonUtils.getString(addonJson, "domain", null);
        String packName = JsonUtils.getString(packJson, "description", addonFile.getName());
        String version = JsonUtils.getString(addonJson, "version", "0.0");

        String credits = JsonUtils.getString(addonJson, "credits", "");
        String description = JsonUtils.getString(addonJson, "description", "");
        String loaderVersion = JsonUtils.getString(addonJson, "loader_version", "1");
        List<String> authors = getAuthors(addonJson);
        return new AddonPack(
                addonDomain, packName, version, addonFile, credits, authors, description, loaderVersion,
                ImmutableMap.copyOf(packMetaJson.entrySet()));
    }

    private static List<String> getAuthors(JsonObject jsonObject) {
        List<String> list = Lists.newLinkedList();
        if (jsonObject.has("authors")) {
            JsonElement jsonElement = jsonObject.get("authors");
            if (jsonElement.isJsonArray()) {
                for (JsonElement jsonElement1 : jsonElement.getAsJsonArray()) {
                    list.add(jsonElement1.getAsString());
                }
            }
        } else if (jsonObject.has("author")) {
            JsonElement jsonElement2 = jsonObject.get("author");
            if (jsonElement2.isJsonPrimitive()) {
                list.add(jsonElement2.getAsString());
            }
        }

        return list;
    }

    private static JsonObject loadPackMeta(File addonFile) {
        ResourceLoader loader = ResourceLoader.create(addonFile);
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(loader.getInputStream("pack.mcmeta"), StandardCharsets.UTF_8));
            return new JsonParser().parse(bufferedReader).getAsJsonObject();
        } catch (FileNotFoundException var8) {
            MCH_Logger.get().warn("'pack.mcmeta' does not found in '{}'", addonFile.getName());
        } catch (IOException var9) {
            var9.printStackTrace();
        } finally {
            IOUtils.closeQuietly(bufferedReader);
            IOUtils.closeQuietly(loader);
        }

        return new JsonObject();
    }

    public String getDomain() {
        return this.addonDomain;
    }

    public String getName() {
        return this.addonName;
    }

    public String getVersion() {
        return this.addonVersion;
    }

    public String getAuthorsString() {
        return Joiner.on(", ").join(this.authors);
    }

    public String getCredits() {
        return this.credits;
    }

    public String getDescription() {
        return this.description;
    }

    public String getLoaderVersion() {
        return this.loaderVersion;
    }

    public File getFile() {
        return this.addonFile;
    }

    public ImmutableMap<String, JsonElement> getPackMetaMap() {
        return this.packMetaMap;
    }
}
