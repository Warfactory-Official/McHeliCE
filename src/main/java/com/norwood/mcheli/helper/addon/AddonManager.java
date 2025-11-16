package com.norwood.mcheli.helper.addon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.MCH_Utils;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AddonManager {

    public static final Pattern ZIP_PATTERN = Pattern.compile("(.+).(zip|jar)$");
    public static final String BUILTIN_ADDON_DOMAIN = "@builtin";
    private static final Map<String, AddonPack> ADDON_LIST = Maps.newLinkedHashMap();

    @Nullable
    public static AddonPack get(String addonDomain) {
        return "@builtin".equals(addonDomain) ? BuiltinAddonPack.instance() : ADDON_LIST.get(addonDomain);
    }

    public static List<AddonPack> getLoadedAddons() {
        return ImmutableList.<AddonPack>builder()
                .addAll(ADDON_LIST.values())
                .build();
    }

    public static List<AddonPack> loadAddons(File addonDir) {
        MCH_MOD.getLogger().info("Loading content from: " + addonDir.getAbsolutePath());
        checkExistAddonDir(addonDir);
        List<AddonPack> addons = Lists.newArrayList();
        File[] addonFiles = addonDir.listFiles();

        MCH_MOD.getLogger().info("Found contents: " + addonFiles.toString());
        for (File addonFile : addonFiles) {
            if (validAddonPath(addonFile)) {
                AddonPack data = loadAddon(addonFile);
                if (data != null) {
                    addons.add(data);
                }
            }
        }

        MCH_Utils.logger().info("Load complete addons. count:{}", addons.size());
        return addons;
    }

    @SideOnly(Side.CLIENT)
    public static List<AddonPack> loadAddonsAndAddResources(File addonDir) {
        checkExistAddonDir(addonDir);
        List<AddonPack> addons = Lists.newArrayList();
        File[] addonFiles = addonDir.listFiles();

        for (File addonFile : addonFiles) {
            if (validAddonPath(addonFile)) {
                AddonPack data = loadAddonAndAddResource(addonFile, MCH_MOD.class);
                if (data != null) {
                    addons.add(data);
                }
            }
        }

        checkExistAddonDir(GeneratedAddonPack.instance().getFile());
        addReloadableResource(GeneratedAddonPack.instance(), MCH_MOD.class);
        FMLClientHandler.instance().refreshResources((Predicate) null);
        MCH_Utils.logger().info("Load complete addons and add resources. count:{}", addons.size());
        return addons;
    }

    @Nullable
    private static AddonPack loadAddon(File addonFile) {
        try {
            AddonPack addonPack = AddonPack.create(addonFile);
            ADDON_LIST.put(addonPack.getDomain(), addonPack);
            return addonPack;
        } catch (Exception var2) {
            MCH_Utils.logger().error("Failed to load for pack:{} :", addonFile.getName(), var2);
            return null;
        }
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    private static AddonPack loadAddonAndAddResource(File addonFile, Class<?> clazz) {
        AddonPack addonPack = loadAddon(addonFile);
        if (addonPack == null) {
            return null;
        } else {
            addReloadableResource(addonPack, clazz);
            return addonPack;
        }
    }

    @SideOnly(Side.CLIENT)
    private static void addReloadableResource(AddonPack addonPack, Class<?> clazz) {
        Map<String, Object> descriptor = Maps.newHashMap();
        descriptor.put("modid", Tags.MODID);
        descriptor.put("name", "MCHeli#" + addonPack.getName());
        descriptor.put("version", addonPack.getVersion());
        File file = addonPack.getFile();
        FMLModContainer container = new FMLModContainer(
                clazz.getName(),
                new ModCandidate(file, file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR), descriptor);
        container.bindMetadata(MetadataCollection.from(null, ""));
        FMLClientHandler.instance().addModAsResource(container);
    }

    private static boolean validAddonPath(File addonFile) {
        return !GeneratedAddonPack.isGeneratedAddonName(addonFile) &&
                (addonFile.isDirectory() || ZIP_PATTERN.matcher(addonFile.getName()).matches());
    }

    private static void checkExistAddonDir(File addonDir) {
        if (!addonDir.exists()) {
            addonDir.mkdirs();
        }
    }
}
