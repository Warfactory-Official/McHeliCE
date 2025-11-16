package com.norwood.mcheli.compat;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ModCompatManager {

    public static final Map<String, Boolean> LOADED_CACHE = new ConcurrentHashMap<>();

    public static final String MODID_HBM = "hbm";
    public static final String MODID_TOP = "theoneprobe";

    public static boolean isLoaded(String modid) {
        return LOADED_CACHE.computeIfAbsent(modid, Loader::isModLoaded);
    }

    public static @Nullable URI getModUri(String modID) {
        ModContainer mod = Loader.instance().getIndexedModList().get(modID);
        if (mod == null) return null;
        File source = mod.getSource();
        return source != null ? source.toURI() : null;
    }

    public static Map<String, Class<?>> getClassesInPackage(String packageName) {
        Map<String, Class<?>> classes = new HashMap<>();
        String path = packageName.replace('.', '/');

        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    File dir = new File(resource.toURI());
                    findClassesInDirPackage(packageName, dir, classes);
                } else if ("jar".equals(protocol)) {
                    try (JarInputStream jarStream = new JarInputStream(resource.openStream())) {
                        JarEntry entry;
                        while ((entry = jarStream.getNextJarEntry()) != null) {
                            String name = entry.getName();
                            if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.replace('/', '.').substring(0, name.length() - 6);
                                try {
                                    Class<?> clazz = Class.forName(className);
                                    classes.put(clazz.getSimpleName(), clazz);
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    private static void findClassesInDirPackage(String packageName, File directory, Map<String, Class<?>> classes) {
        if (!directory.exists()) return;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                findClassesInDirPackage(packageName + "." + file.getName(), file, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.put(clazz.getSimpleName(), clazz);
                } catch (Throwable ignored) {}
            }
        }
    }
}
