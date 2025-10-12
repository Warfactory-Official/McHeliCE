package com.norwood.mcheli.helper;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MCH_Utils {
    public static ResourceLocation suffix(String name) {
        return new ResourceLocation(Tags.MODID, name);
    }

    public static AddonResourceLocation addon(String domain, String path) {
        return new AddonResourceLocation(suffix(path), domain);
    }

    public static AddonResourceLocation buildinAddon(String path) {
        return new AddonResourceLocation(suffix(path), "@builtin");
    }

    public static <K, V> V getAny(Map<K, V> map, Collection<K> keys, V defReturn) {
        for (K key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return defReturn;
    }



    public static File getSource() {
        return MCH_MOD.getSource();
    }

    public static boolean isClient() {
        return MCH_MOD.proxy.isRemote();
    }

    public static MinecraftServer getServer() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public static Logger logger() {
        return MCH_Logger.get();
    }

    public static <T> boolean inArray(T[] objArray, @Nullable T target) {
        for (T obj : objArray) {
            if (Objects.equals(target, obj)) {
                return true;
            }
        }

        return false;
    }
}
