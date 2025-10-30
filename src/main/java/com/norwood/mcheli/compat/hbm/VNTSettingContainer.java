package com.norwood.mcheli.compat.hbm;


import com.norwood.mcheli.compat.ModCompatManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VNTSettingContainer {

    public static final String HBM_VANILLANT_PACKAGE = "com.hbm.explosion.vanillant.standard";
    public static Map<String,Class<?>> vanillantClassSet = null; //Simple name : Class
    private final Map<String, Object> rawEntry;
    //ONLY ASSIGNABLE AT RUNTIME
    private Object blockAllocator;
    private Object blockProcessor;
    private Object entityProcessor;
    private Object playerProcessor;
    private Object[] sfx;


    public VNTSettingContainer(Map<String, Object> rawEntry) {
        this.rawEntry = rawEntry;
    }

    @Optional.Method(modid = "hbm")
    public void loadRuntimeInstances() {
        if (vanillantClassSet == null || vanillantClassSet.isEmpty())
            vanillantClassSet = ModCompatManager.getClassesInPackage(HBM_VANILLANT_PACKAGE);

        blockProcessor = processMap((Map<String, Object>) rawEntry.get("BlockProcessor"));
        blockAllocator = processMap((Map<String, Object>) rawEntry.get("BlockAllocator"));
        entityProcessor = processMap((Map<String, Object>) rawEntry.get("EntityProcessor"));
        playerProcessor = processMap((Map<String, Object>) rawEntry.get("PlayerProcessor"));
        sfx = ((List<Map<String,Object>>) rawEntry.get("SFX")).stream().map((x)-> processMap(x)).collect(Collectors.toList()).toArray();
    }


    @Optional.Method(modid = "hbm")
    public void buildExplosion(World world, double x, double y, double z, float size, Entity exploder) {
        var vnt = new com.hbm.explosion.vanillant.ExplosionVNT(world, x, y, z, size, exploder);
        vnt.setBlockAllocator((com.hbm.explosion.vanillant.interfaces.IBlockAllocator) blockAllocator);
        vnt.setEntityProcessor((com.hbm.explosion.vanillant.interfaces.IEntityProcessor) entityProcessor);
        vnt.setBlockProcessor((com.hbm.explosion.vanillant.interfaces.IBlockProcessor) blockProcessor);
        vnt.setPlayerProcessor((com.hbm.explosion.vanillant.interfaces.IPlayerProcessor) playerProcessor);
        vnt.setSFX((com.hbm.explosion.vanillant.interfaces.IExplosionSFX[]) sfx);
        vnt.explode();
    }

    public Object processMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        //Get the class if specified
        Object instance = null;
        if (map.containsKey("Type")) {
            String typeName = (String) map.get("Type");
            Class<?> clazz = vanillantClassSet.get(typeName);
            if (clazz == null) {
                System.out.println("Unknown class type: " + typeName);
                return null;
            }

            // Constructor args if present
            Object ctorArgs = map.get("Constructor");
            try {
                if (ctorArgs instanceof List<?> argsList) {
                    Constructor<?> ctor = findMatchingConstructor(clazz, argsList);
                    instance = ctor.newInstance(argsList.toArray());
                } else {
                    instance = clazz.getDeclaredConstructor().newInstance();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Process all other keys
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equals("Type") || key.equals("Constructor")) continue;

            try {
                if (value instanceof Map<?, ?> nestedMap) {
                    Object nestedObj = processMap((Map<String, Object>) nestedMap);
                    invokeSetter(instance, key, nestedObj);
                } else {
                    // Scalar value - call setter or assign field
                    invokeSetter(instance, key, value);
                }
            } catch (Exception e) {
                System.out.println("Failed to process key: " + key);
                e.printStackTrace();
            }
        }

        return instance;
    }

    private void invokeSetter(Object instance, String key, Object value) {
        if (instance == null || value == null) return;

        Class<?> clazz = instance.getClass();
        String methodName =   Character.toLowerCase(key.charAt(0)) + key.substring(1); // Ideally user provides exact method name, safety if they decide to use the pascal case

        try {
            Method m = clazz.getMethod(methodName, value.getClass());
            m.invoke(instance, value);
        } catch (NoSuchMethodException e) {
            try {
                Field f = clazz.getDeclaredField(key);
                f.setAccessible(true);
                f.set(instance, value);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Constructor<?> findMatchingConstructor(Class<?> clazz, List<?> args) throws NoSuchMethodException {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        outer:
        for (Constructor<?> ctor : constructors) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            if (paramTypes.length != args.size()) continue;

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                Object arg = args.get(i);
                if (arg == null) continue; // null can go anywhere

                if (!isAssignable(paramType, arg.getClass())) {
                    continue outer;
                }
            }

            ctor.setAccessible(true);
            return ctor;
        }

        throw new NoSuchMethodException("No matching constructor found for " + clazz.getSimpleName());
    }

    // Fixes possible issues with boxing
    private static boolean isAssignable(Class<?> targetType, Class<?> valueType) {
        if (targetType.isPrimitive()) {
            if (targetType == int.class) return valueType == Integer.class;
            if (targetType == long.class) return valueType == Long.class;
            if (targetType == float.class) return valueType == Float.class;
            if (targetType == double.class) return valueType == Double.class;
            if (targetType == boolean.class) return valueType == Boolean.class;
            if (targetType == char.class) return valueType == Character.class;
            if (targetType == byte.class) return valueType == Byte.class;
            if (targetType == short.class) return valueType == Short.class;
            return false;
        }
        return targetType.isAssignableFrom(valueType);
    }



}
