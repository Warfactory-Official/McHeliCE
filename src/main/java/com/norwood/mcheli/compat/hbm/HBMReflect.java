package com.norwood.mcheli.compat.hbm;

import com.norwood.mcheli.compat.ModCompatManager;
import com.norwood.mcheli.helper.MCH_Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflective bridge to HBM-NTM (modid {@code "hbm"}).
 *
 * <p>McHeliCE depends on HBM only at runtime ({@code runtimeOnly}), so the compat layer must
 * <b>not</b> reference {@code com.hbm.*} types directly. Doing so coupled our build to HBM's
 * internal package layout -- which moves between versions -- and silently broke compilation
 * (the old direct-import layer only ever "compiled" off stale class files). Everything here
 * resolves classes / methods / constructors / fields by name at runtime, caches the handles,
 * and degrades to a logged no-op when HBM is absent or a member has moved/renamed, rather than
 * crashing or failing the build.
 *
 * <p>Members are matched by name + argument arity + assignability rather than exact parameter
 * types, so benign HBM signature changes (e.g. {@code int}->{@code float} arguments, or a field
 * whose type changed from {@code String} to an enum) do not break the integration as long as a
 * compatible value is supplied. Every failure path logs at most once per distinct target.
 */
public final class HBMReflect {

    private HBMReflect() {}

    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MISSING_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Map<String, Method[]> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /** True only when the HBM mod is actually loaded; gate every effect on this. */
    public static boolean available() {
        return ModCompatManager.isLoaded(ModCompatManager.MODID_HBM);
    }

    /** Resolve (and cache) an HBM class by fully-qualified name. Null + one-time warn if missing. */
    @Nullable
    public static Class<?> cls(String name) {
        if (MISSING_CLASSES.contains(name)) return null;
        Class<?> c = CLASS_CACHE.get(name);
        if (c != null) return c;
        try {
            c = Class.forName(name);
            CLASS_CACHE.put(name, c);
            return c;
        } catch (Throwable t) {
            MISSING_CLASSES.add(name);
            warnOnce("class:" + name, "HBM class not found (moved or HBM absent): " + name);
            return null;
        }
    }

    /** Construct an HBM object by class name, matching the constructor by argument arity/types. */
    @Nullable
    public static Object construct(String clsName, Object... args) {
        return constructFrom(cls(clsName), args);
    }

    /** Construct from an already-resolved class, matching the constructor by arity + assignability. */
    @Nullable
    public static Object constructFrom(Class<?> c, Object... args) {
        if (c == null) return null;
        try {
            for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length == args.length && argsMatch(p, args)) {
                    trySetAccessible(ctor);
                    return ctor.newInstance(args);
                }
            }
            warnOnce("ctor:" + c.getName() + ":" + args.length,
                    "No matching HBM constructor for " + c.getName() + " with " + args.length + " args");
        } catch (Throwable t) {
            warnOnce("ctor!" + c.getName(), "Failed constructing HBM " + c.getName() + ": " + t);
        }
        return null;
    }

    /** Invoke an instance method matched by name + arity + assignability. Returns its result (or null). */
    @Nullable
    public static Object call(Object target, String method, Object... args) {
        if (target == null) return null;
        Method m = findMethod(target.getClass(), method, args, false);
        if (m == null) {
            warnOnce("m:" + target.getClass().getName() + "." + method + ":" + args.length,
                    "HBM method not found: " + target.getClass().getName() + "." + method + "/" + args.length);
            return null;
        }
        try {
            return m.invoke(target, args);
        } catch (Throwable t) {
            warnOnce("m!" + target.getClass().getName() + "." + method, "Failed invoking HBM " + method + ": " + t);
            return null;
        }
    }

    /**
     * Like {@link #call} but returns whether a matching method was found and invoked, and never
     * warns -- for speculative calls where "no such method" is an expected, non-error outcome
     * (e.g. probing for an optional no-arg builder toggle).
     */
    public static boolean tryCall(Object target, String method, Object... args) {
        if (target == null) return false;
        Method m = findMethod(target.getClass(), method, args, false);
        if (m == null) return false;
        try {
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Invoke a static method on an HBM class, matched by name + arity + assignability. */
    @Nullable
    public static Object callStatic(String clsName, String method, Object... args) {
        Class<?> c = cls(clsName);
        if (c == null) return null;
        Method m = findMethod(c, method, args, true);
        if (m == null) {
            warnOnce("sm:" + clsName + "." + method + ":" + args.length,
                    "HBM static method not found: " + clsName + "." + method + "/" + args.length);
            return null;
        }
        try {
            return m.invoke(null, args);
        } catch (Throwable t) {
            warnOnce("sm!" + clsName + "." + method, "Failed invoking HBM static " + clsName + "." + method + ": " + t);
            return null;
        }
    }

    /** Assign a field (public or, failing that, any declared field up the hierarchy). */
    public static boolean setField(Object target, String field, Object value) {
        if (target == null) return false;
        // Fast path: public field (incl. inherited).
        try {
            Field f = target.getClass().getField(field);
            f.set(target, value);
            return true;
        } catch (NoSuchFieldException ignored) {
            // fall through to declared-field walk
        } catch (Throwable t) {
            warnOnce("f!" + target.getClass().getName() + "." + field,
                    "Failed setting HBM field " + field + ": " + t);
            return false;
        }
        // Walk the class hierarchy for a declared (possibly protected/private) field.
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(field);
                f.setAccessible(true);
                f.set(target, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                // try superclass
            } catch (Throwable t) {
                warnOnce("f!" + k.getName() + "." + field, "Failed setting HBM field " + field + ": " + t);
                return false;
            }
        }
        warnOnce("f?" + target.getClass().getName() + "." + field,
                "HBM field not found: " + target.getClass().getName() + "." + field);
        return false;
    }

    /** Resolve an enum constant by exact name. */
    @Nullable
    public static Object enumValue(String enumClass, String constName) {
        Class<?> c = cls(enumClass);
        if (c == null || !c.isEnum() || constName == null) return null;
        for (Object e : c.getEnumConstants()) {
            if (((Enum<?>) e).name().equals(constName)) return e;
        }
        return null;
    }

    /** Resolve an enum constant case-insensitively (HBM enum names are PascalCase; configs vary). */
    @Nullable
    public static Object enumValueIgnoreCase(String enumClass, String constName) {
        Class<?> c = cls(enumClass);
        if (c == null || !c.isEnum() || constName == null) return null;
        for (Object e : c.getEnumConstants()) {
            if (((Enum<?>) e).name().equalsIgnoreCase(constName)) return e;
        }
        return null;
    }

    /** Build a {@code List} of enum constants for the given names (skips unknown names). */
    public static List<Object> enumList(String enumClass, List<String> names) {
        List<Object> out = new ArrayList<>();
        if (names == null) return out;
        for (String n : names) {
            Object e = enumValue(enumClass, n);
            if (e != null) out.add(e);
        }
        return out;
    }

    /** Build a typed enum array (e.g. {@code ExAttrib[]}) from a list of enum constants. */
    @Nullable
    public static Object enumArray(String enumClass, List<Object> values) {
        return typedArray(enumClass, values);
    }

    /** Build a typed array of {@code componentClass} from the values (e.g. {@code IExplosionSFX[]}). */
    @Nullable
    public static Object typedArray(String componentClass, List<Object> values) {
        Class<?> c = cls(componentClass);
        if (c == null) return null;
        Object arr = Array.newInstance(c, values.size());
        for (int i = 0; i < values.size(); i++) Array.set(arr, i, values.get(i));
        return arr;
    }


    @Nullable
    private static Method findMethod(Class<?> c, String name, Object[] args, boolean isStatic) {
        Method[] methods = METHOD_CACHE.computeIfAbsent(c.getName(), k -> c.getMethods());
        for (Method m : methods) {
            if (!m.getName().equals(name)) continue;
            if (Modifier.isStatic(m.getModifiers()) != isStatic) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != args.length) continue;
            if (argsMatch(p, args)) {
                trySetAccessible(m);
                return m;
            }
        }
        return null;
    }

    /**
     * {@code setAccessible(true)} that never propagates. Our targets are classpath classes
     * (HBM / Minecraft, unnamed module), so this succeeds on both Java 8 and modern JVMs; but a
     * stricter environment (module system / SecurityManager) may refuse it. If so we skip it --
     * public members still invoke fine, and for non-public members the caller's guarded
     * invoke()/newInstance() turns the resulting IllegalAccessException into a logged no-op.
     */
    private static void trySetAccessible(java.lang.reflect.AccessibleObject ao) {
        try {
            ao.setAccessible(true);
        } catch (Throwable ignored) {
            // strict JVM refused; proceed without it
        }
    }

    private static boolean argsMatch(Class<?>[] params, Object[] args) {
        for (int i = 0; i < params.length; i++) {
            Object a = args[i];
            if (a == null) continue; // null is assignable to any reference parameter
            if (!isAssignable(params[i], a.getClass())) return false;
        }
        return true;
    }

    /** Assignability that accounts for primitive parameters being passed boxed values. */
    private static boolean isAssignable(Class<?> target, Class<?> value) {
        if (target.isPrimitive()) {
            if (target == int.class) return value == Integer.class;
            if (target == long.class) return value == Long.class;
            if (target == float.class) return value == Float.class;
            if (target == double.class) return value == Double.class;
            if (target == boolean.class) return value == Boolean.class;
            if (target == char.class) return value == Character.class;
            if (target == byte.class) return value == Byte.class;
            if (target == short.class) return value == Short.class;
            return false;
        }
        return target.isAssignableFrom(value);
    }

    private static void warnOnce(String key, String msg) {
        if (WARNED.add(key)) {
            MCH_Logger.get().warn("[MCHeli/HBM] {}", msg);
        }
    }
}
