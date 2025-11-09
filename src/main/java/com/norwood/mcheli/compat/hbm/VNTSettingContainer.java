package com.norwood.mcheli.compat.hbm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import com.norwood.mcheli.compat.ModCompatManager;

import lombok.NoArgsConstructor;

@SuppressWarnings("unchecked")
public class VNTSettingContainer {

    public static final String HBM_VANILLANT_PACKAGE = "com.hbm.explosion.vanillant.standard";
    public static Map<String, Class<?>> vanillantClassSet = null; // Simple name : Class
    private final Map<String, Object> rawEntry;
    // ONLY ASSIGNABLE AT RUNTIME
    private Object blockAllocator;
    private Object blockProcessor;
    private Object entityProcessor;
    private Object playerProcessor;
    public ExplosionEffect explosionEffect = ExplosionEffect.standard();
    // private Object[] sfx;

    public VNTSettingContainer(Map<String, Object> rawEntry) {
        this.rawEntry = rawEntry;
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

    @Optional.Method(modid = "hbm")
    public void loadRuntimeInstances() {
        if (vanillantClassSet == null || vanillantClassSet.isEmpty())
            vanillantClassSet = ModCompatManager.getClassesInPackage(HBM_VANILLANT_PACKAGE);

        blockProcessor = processMap((Map<String, Object>) rawEntry.get("BlockProcessor"));
        blockAllocator = processMap((Map<String, Object>) rawEntry.get("BlockAllocator"));
        entityProcessor = processMap((Map<String, Object>) rawEntry.get("EntityProcessor"));
        playerProcessor = processMap((Map<String, Object>) rawEntry.get("PlayerProcessor"));
        // sfx = ((List<Map<String, Object>>) rawEntry.get("SFX")).stream().map((x) ->
        // processMap(x)).collect(Collectors.toList()).toArray();
    }

    @Optional.Method(modid = "hbm")
    public void buildExplosion(World world, double x, double y, double z, float size, Entity exploder,
                               boolean effectOnly) {
        if (!effectOnly) {
            var vnt = new com.hbm.explosion.vanillant.ExplosionVNT(world, x, y, z, size, exploder);
            vnt.setBlockAllocator((com.hbm.explosion.vanillant.interfaces.IBlockAllocator) blockAllocator);
            vnt.setEntityProcessor((com.hbm.explosion.vanillant.interfaces.IEntityProcessor) entityProcessor);
            vnt.setBlockProcessor((com.hbm.explosion.vanillant.interfaces.IBlockProcessor) blockProcessor);
            vnt.setPlayerProcessor((com.hbm.explosion.vanillant.interfaces.IPlayerProcessor) playerProcessor);
            // vnt.setSFX((com.hbm.explosion.vanillant.interfaces.IExplosionSFX[]) sfx);
            vnt.explode();
        }
        explosionEffect.execute(world, x, y, z);
    }

    public Object processMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        // Get the class if specified
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
                if (ctorArgs instanceof List<?>argsList) {
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
                if (value instanceof Map<?, ?>nestedMap) {
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
        String methodName = Character.toLowerCase(key.charAt(0)) + key.substring(1); // Ideally user provides exact
                                                                                     // method name, safety if they
                                                                                     // decide to use the pascal case

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

    @NoArgsConstructor
    public static class ExplosionEffect {

        public boolean isSmall;

        public int cloudCount = 0;
        public float cloudScale = 1.0F;
        public float cloudSpeedMult = 1.0F;
        public float waveScale = 1.0F;

        public int debrisCount = 0;
        public int debrisSize = 0;
        public int debrisRetry = 0;
        public float debrisVelocity = 0.0F;
        public float debrisHorizontalDeviation = 0.0F;
        public float debrisVerticalOffset = 0.0F;

        public float soundRange = 0.0F;

        public ExplosionEffect(boolean isSmall,
                               int cloudCount,
                               float cloudScale,
                               float cloudSpeedMult,
                               float waveScale,
                               int debrisCount,
                               int debrisSize,
                               int debrisRetry,
                               float debrisVelocity,
                               float debrisHorizontalDeviation,
                               float debrisVerticalOffset,
                               float soundRange) {
            this.isSmall = isSmall;
            this.cloudCount = cloudCount;
            this.cloudScale = cloudScale;
            this.cloudSpeedMult = cloudSpeedMult;
            this.waveScale = waveScale;
            this.debrisCount = debrisCount;
            this.debrisSize = debrisSize;
            this.debrisRetry = debrisRetry;
            this.debrisVelocity = debrisVelocity;
            this.debrisHorizontalDeviation = debrisHorizontalDeviation;
            this.debrisVerticalOffset = debrisVerticalOffset;
            this.soundRange = soundRange;
        }

        @Optional.Method(modid = "hbm")
        public void execute(World world, double x, double y, double z) {
            if (isSmall) {
                com.hbm.particle.helper.ExplosionSmallCreator.composeEffect(
                        world,
                        x, y, z,
                        cloudCount,
                        cloudScale,
                        cloudSpeedMult);
            } else {
                com.hbm.particle.helper.ExplosionCreator.composeEffect(
                        world,
                        x, y, z,
                        cloudCount,
                        cloudScale,
                        cloudSpeedMult,
                        waveScale,
                        debrisCount,
                        debrisSize,
                        debrisRetry,
                        debrisVelocity,
                        debrisHorizontalDeviation,
                        debrisVerticalOffset,
                        soundRange);
            }
        }

        public static ExplosionEffect medium() {
            return new ExplosionEffect(
                    false,
                    10,
                    2F,
                    0.5F,
                    25F,
                    5,
                    8,
                    20,
                    0.75F,
                    1F,
                    -2F,
                    150F);
        }

        public static ExplosionEffect standard() {
            return new ExplosionEffect(
                    false,
                    15,
                    5F,
                    1F,
                    45F,
                    10,
                    16,
                    50,
                    1F,
                    3F,
                    -2F,
                    200F);
        }

        public static ExplosionEffect large() {
            return new ExplosionEffect(
                    false,
                    30,
                    6.5F,
                    2F,
                    65F,
                    25,
                    16,
                    50,
                    1.25F,
                    3F,
                    -2F,
                    350F);
        }
    }
}
