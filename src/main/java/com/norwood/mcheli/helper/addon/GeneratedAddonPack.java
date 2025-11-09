package com.norwood.mcheli.helper.addon;

import java.io.File;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.MCH_OutputFile;
import com.norwood.mcheli.helper.MCH_Utils;

public class GeneratedAddonPack extends AddonPack {

    private static GeneratedAddonPack instance = null;
    private static File generatedDir = null;

    private GeneratedAddonPack() {
        super(
                "@generated",
                "Generated",
                "1.0",
                null,
                "EMB4-MCHeli",
                ImmutableList.of("EMB4", "Murachiki27"),
                "Generated addon(auto generate or update files)",
                "1",
                ImmutableMap.of());
    }

    public static GeneratedAddonPack instance() {
        if (instance == null) {
            instance = new GeneratedAddonPack();
        }

        return instance;
    }

    public static boolean isGeneratedAddonName(File file) {
        return "generated".equals(file.getName());
    }

    @Override
    public File getFile() {
        if (generatedDir == null) {
            generatedDir = new File(MCH_MOD.getAddonDir(), "/generated/");
        }

        return generatedDir;
    }

    public boolean updateAssetFile(String targetAssetPath, List<String> lines) {
        File assets = this.checkExistAssets();
        MCH_OutputFile file = new MCH_OutputFile();
        if (!file.openUTF8(assets.getPath() + "/" + targetAssetPath)) {
            return false;
        } else {
            for (String s : lines) {
                file.writeLine(s);
            }

            file.close();
            MCH_Utils.logger().info("Update file:{}", file.file.getAbsolutePath());
            return true;
        }
    }

    public File checkMkdirsAssets(String dir) {
        File assets = new File(this.getFile(), "/assets/mcheli/" + dir + "/");
        if (!assets.exists()) {
            assets.mkdirs();
        }

        return assets;
    }

    private File checkExistAssets() {
        File assets = new File(this.getFile(), "/assets/mcheli/");
        if (!assets.exists()) {
            assets.mkdirs();
        }

        return assets;
    }
}
