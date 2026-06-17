package com.norwood.mcheli.compat.hbm;

import com.norwood.mcheli.compat.ModCompatManager;
import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class VNTSettingContainer {

    public static final String HBM_VANILLANT_PACKAGE = "com.hbm.explosion.vanillant.standard";
    private static final String C_EXPLOSION_VNT = "com.hbm.explosion.vanillant.ExplosionVNT";
    private static final String C_EXPLOSION_CREATOR = "com.hbm.particle.helper.ExplosionCreator";
    private static final String C_EXPLOSION_SMALL_CREATOR = "com.hbm.particle.helper.ExplosionSmallCreator";
    private static final String C_IEXPLOSION_SFX = "com.hbm.explosion.vanillant.interfaces.IExplosionSFX";
    public static Map<String, Class<?>> vanillantClassSet = null; // Simple name : Class
    private final Map<String, Object> rawEntry;
    // ONLY ASSIGNABLE AT RUNTIME
    private Object blockAllocator;
    private Object blockProcessor;
    private Object entityProcessor;
    private Object playerProcessor;
    private Object[] sfx;
    public String preset;
    public ExplosionEffect explosionEffect = ExplosionEffect.standard();

    public VNTSettingContainer(Map<String, Object> rawEntry) {
        this.rawEntry = rawEntry;
    }

    public void loadRuntimeInstances() {
        if (vanillantClassSet == null || vanillantClassSet.isEmpty())
            vanillantClassSet = ModCompatManager.getClassesInPackage(HBM_VANILLANT_PACKAGE);

        blockProcessor = processMap((Map<String, Object>) rawEntry.get("BlockProcessor"));
        blockAllocator = processMap((Map<String, Object>) rawEntry.get("BlockAllocator"));
        entityProcessor = processMap((Map<String, Object>) rawEntry.get("EntityProcessor"));
        playerProcessor = processMap((Map<String, Object>) rawEntry.get("PlayerProcessor"));

        Object sfxRaw = rawEntry.get("SFX");
        if (sfxRaw instanceof List<?> sfxList) {
            List<Object> built = new ArrayList<>();
            for (Object e : sfxList) {
                if (e instanceof Map<?, ?> m) {
                    Object o = processMap((Map<String, Object>) m);
                    if (o != null) built.add(o);
                }
            }
            sfx = built.isEmpty() ? null : built.toArray();
        }
    }

    public void buildExplosion(World world, double x, double y, double z, float size, Entity exploder,
                               boolean effectOnly) {
        if (!effectOnly && HBMReflect.available()) {
            // ExplosionVNT(World, double, double, double, float size, Entity exploder)
            Object vnt = HBMReflect.construct(C_EXPLOSION_VNT, world, x, y, z, size, exploder);
            if (vnt != null) {
                if (preset != null) {
                    HBMReflect.tryCall(vnt, presetMethod(preset));
                }
                // Processor objects are built reflectively from config in processMap(); the
                // setters accept HBM interface types, which the reflective matcher resolves.
                if (blockAllocator != null) HBMReflect.call(vnt, "setBlockAllocator", blockAllocator);
                if (entityProcessor != null) HBMReflect.call(vnt, "setEntityProcessor", entityProcessor);
                if (blockProcessor != null) HBMReflect.call(vnt, "setBlockProcessor", blockProcessor);
                if (playerProcessor != null) HBMReflect.call(vnt, "setPlayerProcessor", playerProcessor);
                if (sfx != null && sfx.length > 0) {
                    // setSFX(IExplosionSFX...) needs a typed IExplosionSFX[] for the varargs slot.
                    Object sfxArray = HBMReflect.typedArray(C_IEXPLOSION_SFX, java.util.Arrays.asList(sfx));
                    if (sfxArray != null) HBMReflect.call(vnt, "setSFX", sfxArray);
                }
                HBMReflect.call(vnt, "explode");
            }
        }
        explosionEffect.execute(world, x, y, z);
    }

    /** Maps a preset name to the corresponding {@code ExplosionVNT} factory method. */
    private static String presetMethod(String preset) {
        return switch (preset.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "AMAT", "ANTIMATTER" -> "makeAmat";
            default -> "makeStandard";
        };
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

            // Constructor args if present. Both paths go through HBMReflect, which matches by
            // arity + assignability and guards setAccessible (safe under strict modern JVMs).
            Object ctorArgs = map.get("Constructor");
            if (ctorArgs instanceof List<?> argsList) {
                instance = HBMReflect.constructFrom(clazz, argsList.toArray());
            } else {
                instance = HBMReflect.constructFrom(clazz);
            }
            if (instance == null) return null;
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

        // Lower-camel from PascalCase config keys; users may also give the exact method name.
        String methodName = Character.toLowerCase(key.charAt(0)) + key.substring(1);

        // Boolean true selects a no-arg builder toggle (e.g. "NoDrop: true" -> setNoDrop(),
        // "AllowSelfDamage: true" -> allowSelfDamage()). Boolean false is a no-op.
        if (value instanceof Boolean flag) {
            if (!flag) return;
            if (HBMReflect.tryCall(instance, methodName)) return;
            String setName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            if (HBMReflect.tryCall(instance, setName)) return;
            // otherwise fall through and treat it as a boolean-valued setter / field
        }

        // Single-argument builder/setter matched by name + assignability. This is what makes
        // interface-typed setters work -- e.g. withBlockEffect(IBlockMutator) fed a concrete
        // BlockMutatorFire, or withDamageMod(ICustomDamageHandler) fed a CustomDamageHandlerAmat.
        if (HBMReflect.tryCall(instance, methodName, value)) return;

        // Last resort: assign a field of that name (public, then declared up the hierarchy).
        HBMReflect.setField(instance, key, value);
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

        public void execute(World world, double x, double y, double z) {
            if (!HBMReflect.available()) return;
            if (isSmall) {
                HBMReflect.callStatic(C_EXPLOSION_SMALL_CREATOR, "composeEffect",
                        world, x, y, z,
                        cloudCount, cloudScale, cloudSpeedMult);
            } else {
                HBMReflect.callStatic(C_EXPLOSION_CREATOR, "composeEffect",
                        world, x, y, z,
                        cloudCount, cloudScale, cloudSpeedMult, waveScale,
                        debrisCount, debrisSize, debrisRetry, debrisVelocity,
                        debrisHorizontalDeviation, debrisVerticalOffset, soundRange);
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
