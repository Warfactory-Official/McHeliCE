package com.norwood.mcheli.helper.addon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.io.ResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BuiltinAddonPack extends AddonPack {

    public static final String EXTRACTED_FOLDER_NAME = "default";
    private static final String PACK_META = """
            {
              "pack": {
                "pack_format": 3,
                "description": "MCHeli default content pack"
              },
              "addon": {
                "domain": "@builtin",
                "version": "1.0",
                "loader_version": "2",
                "credits": "EMB4-MCHeli",
                "authors": ["EMB4", "Murachiki27"],
                "description": "Builtin addon extracted to disk"
              }
            }
            """;
    private static BuiltinAddonPack instance = null;

    private BuiltinAddonPack() {
        super("@builtin", "MCHeli-Builtin", "1.0", null, "EMB4-MCHeli", ImmutableList.of("EMB4", "Murachiki27"),
                "Builtin addon", "1", ImmutableMap.of());
    }

    public static BuiltinAddonPack instance() {
        if (instance == null) {
            instance = new BuiltinAddonPack();
        }

        return instance;
    }

    public static boolean isDefaultAddonName(File file) {
        return EXTRACTED_FOLDER_NAME.equals(file.getName());
    }

    @Override
    public File getFile() {
        return new File(MCH_MOD.getAddonDir(), EXTRACTED_FOLDER_NAME);
    }

    public boolean ensureExtracted(MCH_Config config) {
        File targetDir = this.getFile();
        if (config != null && MCH_Config.ExtractDefaultContentPack.prmBool && targetDir.exists()) {
            return false;
        }

        try {
            extractTo(targetDir);
            if (config != null) {
                config.ExtractDefaultContentPack.setPrm(true);
            }
            return true;
        } catch (IOException e) {
            MCH_Logger.error("Failed to extract builtin content pack to '{}'", targetDir.getAbsolutePath(), e);
            return false;
        }
    }

    private void extractTo(File targetDir) throws IOException {
        deleteRecursively(targetDir);
        if (!targetDir.mkdirs() && !targetDir.isDirectory()) {
            throw new IOException("Failed to create builtin addon dir: " + targetDir.getAbsolutePath());
        }

        writePackMeta(targetDir);
        int copied = 0;
        try (ResourceLoader loader = ResourceLoader.create(MCH_Utils.getSource())) {
            for (ResourceLoader.ResourceEntry entry : loader.loadAll(resource ->
                    !resource.isDirectory() && resource.getPath().startsWith("assets/mcheli/"))) {
                copyEntry(loader, entry, targetDir);
                copied++;
            }
        }

        MCH_Logger.get().info("Extracted {} builtin content files to '{}'", copied, targetDir.getAbsolutePath());
    }

    private void writePackMeta(File targetDir) throws IOException {
        File packMeta = new File(targetDir, "pack.mcmeta");
        try (OutputStream out = new FileOutputStream(packMeta)) {
            out.write(PACK_META.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void copyEntry(ResourceLoader loader, ResourceLoader.ResourceEntry entry, File targetDir) throws IOException {
        File outFile = new File(targetDir, entry.getPath());
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Failed to create directory: " + parent.getAbsolutePath());
        }

        try (InputStream in = loader.getInputStreamFromEntry(entry);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    private void deleteRecursively(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete: " + file.getAbsolutePath());
        }
    }

}
