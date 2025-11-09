package com.norwood.mcheli.helper.info;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.addon.AddonManager;
import com.norwood.mcheli.helper.addon.AddonPack;
import com.norwood.mcheli.helper.addon.BuiltinAddonPack;
import com.norwood.mcheli.hud.MCH_Hud;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;

public class ContentRegistries {

    private static final Pattern PATH_SPLIT = Pattern.compile("[/\\\\]+");
    private static ContentRegistry<MCH_HeliInfo> REGISTORY_HELI = null;
    private static ContentRegistry<MCH_PlaneInfo> REGISTORY_PLANE = null;
    private static ContentRegistry<MCH_ShipInfo> REGISTORY_SHIP = null;
    private static ContentRegistry<MCH_TankInfo> REGISTORY_TANK = null;
    private static ContentRegistry<MCH_VehicleInfo> REGISTORY_VEHICLE = null;
    private static ContentRegistry<MCH_WeaponInfo> REGISTORY_WEAPON = null;
    private static ContentRegistry<MCH_ThrowableInfo> REGISTORY_THROWABLE = null;
    private static ContentRegistry<MCH_Hud> REGISTORY_HUD = null;

    private static String[] splitPath(String filepath) {
        String norm = filepath.replace('\\', '/');
        while (norm.startsWith("/")) norm = norm.substring(1);
        return PATH_SPLIT.split(norm);
    }

    private static int indexOf(String[] parts, String needle) {
        for (int i = 0; i < parts.length; i++) {
            if (needle.equals(parts[i])) return i;
        }
        return -1;
    }

    public static ContentRegistry<MCH_HeliInfo> heli() {
        return REGISTORY_HELI;
    }

    public static ContentRegistry<MCH_PlaneInfo> plane() {
        return REGISTORY_PLANE;
    }

    public static ContentRegistry<MCH_ShipInfo> ship() {
        return REGISTORY_SHIP;
    }

    public static ContentRegistry<MCH_TankInfo> tank() {
        return REGISTORY_TANK;
    }

    public static ContentRegistry<MCH_VehicleInfo> vehicle() {
        return REGISTORY_VEHICLE;
    }

    public static ContentRegistry<MCH_WeaponInfo> weapon() {
        return REGISTORY_WEAPON;
    }

    public static ContentRegistry<MCH_ThrowableInfo> throwable() {
        return REGISTORY_THROWABLE;
    }

    public static ContentRegistry<MCH_Hud> hud() {
        return REGISTORY_HUD;
    }

    @SuppressWarnings("unchecked")
    public static <T extends MCH_BaseInfo> ContentRegistry<T> get(Class<T> clazz) {
        if (clazz == MCH_HeliInfo.class) {
            return (ContentRegistry<T>) REGISTORY_HELI;
        } else if (clazz == MCH_PlaneInfo.class) {
            return (ContentRegistry<T>) REGISTORY_PLANE;
        } else if (clazz == MCH_ShipInfo.class) {
            return (ContentRegistry<T>) REGISTORY_SHIP;
        } else if (clazz == MCH_TankInfo.class) {
            return (ContentRegistry<T>) REGISTORY_TANK;
        } else if (clazz == MCH_VehicleInfo.class) {
            return (ContentRegistry<T>) REGISTORY_VEHICLE;
        } else if (clazz == MCH_WeaponInfo.class) {
            return (ContentRegistry<T>) REGISTORY_WEAPON;
        } else if (clazz == MCH_ThrowableInfo.class) {
            return (ContentRegistry<T>) REGISTORY_THROWABLE;
        } else if (clazz == MCH_Hud.class) {
            return (ContentRegistry<T>) REGISTORY_HUD;
        } else {
            throw new RuntimeException("Unknown type: " + clazz);
        }
    }

    public static void loadContents(File addonDir) {
        Multimap<ContentType, ContentLoader.ContentEntry> contents = LinkedHashMultimap.create();
        List<AddonPack> addons = MCH_MOD.proxy.loadAddonPacks(addonDir);
        MCH_Lib.Log("Registered content packs: " + addons.toString());

        MCH_MOD.proxy.onLoadStartAddons(addons.size());
        contents.putAll(loadAddonContents(BuiltinAddonPack.instance()));

        for (AddonPack pack : addons) {
            contents.putAll(loadAddonContents(pack));
        }

        MCH_MOD.proxy.onLoadFinishAddons();
        REGISTORY_HUD = parseContents(MCH_Hud.class, "hud", contents.get(ContentType.HUD));
        REGISTORY_WEAPON = parseContents(MCH_WeaponInfo.class, "weapons", contents.get(ContentType.WEAPON));
        REGISTORY_HELI = parseContents(MCH_HeliInfo.class, "helicopters", contents.get(ContentType.HELICOPTER));
        REGISTORY_PLANE = parseContents(MCH_PlaneInfo.class, "planes", contents.get(ContentType.PLANE));
        REGISTORY_SHIP = parseContents(MCH_ShipInfo.class, "ships", contents.get(ContentType.SHIP));
        REGISTORY_TANK = parseContents(MCH_TankInfo.class, "tanks", contents.get(ContentType.TANK));
        REGISTORY_VEHICLE = parseContents(MCH_VehicleInfo.class, "vehicles", contents.get(ContentType.VEHICLE));
        REGISTORY_THROWABLE = parseContents(MCH_ThrowableInfo.class, "throwable", contents.get(ContentType.THROWABLE));
    }

    public static IContentData reparseContent(IContentData content, String dir) {
        AddonPack addonPack = AddonManager.get(content.getLocation().getAddonDomain());
        if (addonPack == null) {
            return content;
        } else {
            ContentLoader packLoader = getDefaultPackLoader(addonPack);
            return packLoader.reloadAndParseSingle(content, dir);
        }
    }

    static <T extends MCH_BaseInfo> List<T> reloadAllAddonContents(ContentRegistry<T> registry) {
        List<T> list = Lists.newLinkedList();

        for (AddonPack addon : AddonManager.getLoadedAddons()) {
            ContentLoader packLoader = getPackLoader(addon, getFilterOnly(registry.getDirectoryName()));

            ContentType type = ContentFactories.getType(registry.getDirectoryName());
            if (type != null) {
                list.addAll(packLoader.reloadAndParse(registry.getType(), registry.values(), type));
            }
        }

        return list;
    }

    private static Multimap<ContentType, ContentLoader.ContentEntry> loadAddonContents(AddonPack pack) {
        ContentLoader packLoader = getDefaultPackLoader(pack);
        MCH_MOD.proxy.onLoadStepAddon(pack.getDomain());
        return packLoader.load();
    }

    private static <T extends MCH_BaseInfo> ContentRegistry<T> parseContents(Class<T> clazz, String dir,
                                                                             Collection<ContentLoader.ContentEntry> values) {
        ContentRegistry.Builder<T> builder = ContentRegistry.builder(clazz, dir);
        MCH_MOD.proxy.onLoadStartContents(dir, values.size());

        values.stream().map(ContentLoader.ContentEntry::parse).filter(Objects::nonNull).map(clazz::cast)
                .forEach(builder::put);

        MCH_MOD.proxy.onLoadFinishContents(dir);
        return builder.build();
    }

    public static ContentLoader getDefaultPackLoader(AddonPack pack) {
        return getPackLoader(pack, ContentRegistries::filter);
    }

    public static ContentLoader getPackLoader(AddonPack pack, Predicate<String> fileFilter) {
        String loaderVersion = pack.getLoaderVersion();
        return pack.getFile().isDirectory() ?
                new FolderContentLoader(pack.getDomain(), pack.getFile(), loaderVersion, fileFilter) :
                new FileContentLoader(pack.getDomain(), pack.getFile(), loaderVersion, fileFilter);
    }

    private static boolean filter(String filepath) {
        String[] parts = splitPath(filepath);
        int i = indexOf(parts, "assets");
        if (i < 0) return false;
        if (parts.length < i + 4) return false;
        String modDir = parts[i + 1];
        String infoDir = parts[i + 2];
        return Tags.MODID.equals(modDir) && MCH_MOD.proxy.canLoadContentDirName(infoDir);
    }

    private static Predicate<String> getFilterOnly(String dir) {
        return filepath -> {
            String[] parts = splitPath(filepath);
            int i = indexOf(parts, "assets");
            if (i < 0) return false;
            if (parts.length < i + 4) return false;
            String modDir = parts[i + 1];
            String infoDir = parts[i + 2];
            return Tags.MODID.equals(modDir) && dir.equals(infoDir);
        };
    }
}
