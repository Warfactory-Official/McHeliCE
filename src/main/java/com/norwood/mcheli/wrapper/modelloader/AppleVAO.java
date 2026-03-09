package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.MCH_Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

final class AppleVAO {

    private static final IntBuffer TMP = ByteBuffer.allocateDirect(Integer.BYTES)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer();

    private static final boolean AVAILABLE;
    private static final MethodHandle MH_MEM_ADDRESS;
    private static final MethodHandle MH_GL_BIND_VERTEX_ARRAY_APPLE;
    private static final MethodHandle MH_GL_GEN_VERTEX_ARRAYS_APPLE;
    private static final MethodHandle MH_GL_DELETE_VERTEX_ARRAYS_APPLE;
    private static final MethodHandle MH_GL_IS_VERTEX_ARRAY_APPLE;

    static {
        boolean available = false;
        MethodHandle mhMemAddress = null;
        MethodHandle mhGlBindVertexArrayAPPLE = null;
        MethodHandle mhGlGenVertexArraysAPPLE = null;
        MethodHandle mhGlDeleteVertexArraysAPPLE = null;
        MethodHandle mhGlIsVertexArrayAPPLE = null;

        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
            Class<?> functionProviderClass = Class.forName("org.lwjgl.system.FunctionProvider");
            Class<?> jniClass = Class.forName("org.lwjgl.system.JNI");
            Class<?> memoryUtilClass = Class.forName("org.lwjgl.system.MemoryUtil");

            mhMemAddress = findMemAddressHandle(lookup, memoryUtilClass);

            MethodHandle mhGetFunctionProvider = lookup.findStatic(glClass, "getFunctionProvider",
                    MethodType.methodType(functionProviderClass)).asType(MethodType.methodType(Object.class));
            MethodHandle mhGetFunctionAddress = lookup.findVirtual(functionProviderClass, "getFunctionAddress",
                    MethodType.methodType(long.class, CharSequence.class))
                    .asType(MethodType.methodType(long.class, Object.class, CharSequence.class));

            MethodHandle mhInvokeV = lookup.findStatic(jniClass, "invokeV",
                    MethodType.methodType(void.class, int.class, long.class));
            MethodHandle mhInvokePV = lookup.findStatic(jniClass, "invokePV",
                    MethodType.methodType(void.class, int.class, long.class, long.class));
            MethodHandle mhInvokeZ = lookup.findStatic(jniClass, "invokeZ",
                    MethodType.methodType(boolean.class, int.class, long.class));

            Object functionProvider = mhGetFunctionProvider.invokeExact();
            if (functionProvider == null) {
                MCH_Logger.error("[AppleVAO] Failed to get FunctionProvider");
            } else {
                long bindPtr = getFunctionPointer(mhGetFunctionAddress, functionProvider, "glBindVertexArrayAPPLE");
                long genPtr = getFunctionPointer(mhGetFunctionAddress, functionProvider, "glGenVertexArraysAPPLE");
                long deletePtr = getFunctionPointer(mhGetFunctionAddress, functionProvider, "glDeleteVertexArraysAPPLE");
                long isPtr = getFunctionPointer(mhGetFunctionAddress, functionProvider, "glIsVertexArrayAPPLE");

                if (bindPtr != 0L && genPtr != 0L && deletePtr != 0L && isPtr != 0L) {
                    mhGlBindVertexArrayAPPLE = MethodHandles.insertArguments(mhInvokeV, 1, bindPtr)
                            .asType(MethodType.methodType(void.class, int.class));
                    mhGlGenVertexArraysAPPLE = MethodHandles.insertArguments(mhInvokePV, 2, genPtr)
                            .asType(MethodType.methodType(void.class, int.class, long.class));
                    mhGlDeleteVertexArraysAPPLE = MethodHandles.insertArguments(mhInvokePV, 2, deletePtr)
                            .asType(MethodType.methodType(void.class, int.class, long.class));
                    mhGlIsVertexArrayAPPLE = MethodHandles.insertArguments(mhInvokeZ, 1, isPtr)
                            .asType(MethodType.methodType(boolean.class, int.class));
                    available = true;
                }
            }
        } catch (Throwable t) {
            MCH_Logger.error("[AppleVAO] Failed to initialize Apple VAO workaround", t);
        }

        AVAILABLE = available;
        MH_MEM_ADDRESS = mhMemAddress;
        MH_GL_BIND_VERTEX_ARRAY_APPLE = mhGlBindVertexArrayAPPLE;
        MH_GL_GEN_VERTEX_ARRAYS_APPLE = mhGlGenVertexArraysAPPLE;
        MH_GL_DELETE_VERTEX_ARRAYS_APPLE = mhGlDeleteVertexArraysAPPLE;
        MH_GL_IS_VERTEX_ARRAY_APPLE = mhGlIsVertexArrayAPPLE;
    }

    private AppleVAO() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static int glGenVertexArraysAPPLE() {
        TMP.put(0, 0);
        try {
            MH_GL_GEN_VERTEX_ARRAYS_APPLE.invokeExact(1, memAddress(TMP));
            return TMP.get(0);
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    static void glGenVertexArraysAPPLE(IntBuffer arrays) {
        try {
            MH_GL_GEN_VERTEX_ARRAYS_APPLE.invokeExact(arrays.remaining(), memAddress(arrays));
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    static void glBindVertexArrayAPPLE(int vao) {
        try {
            MH_GL_BIND_VERTEX_ARRAY_APPLE.invokeExact(vao);
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    static void glDeleteVertexArraysAPPLE(int vao) {
        TMP.put(0, vao);
        try {
            MH_GL_DELETE_VERTEX_ARRAYS_APPLE.invokeExact(1, memAddress(TMP));
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    static void glDeleteVertexArraysAPPLE(IntBuffer arrays) {
        try {
            MH_GL_DELETE_VERTEX_ARRAYS_APPLE.invokeExact(arrays.remaining(), memAddress(arrays));
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    static boolean glIsVertexArrayAPPLE(int vao) {
        try {
            return (boolean) MH_GL_IS_VERTEX_ARRAY_APPLE.invokeExact(vao);
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    private static MethodHandle findMemAddressHandle(MethodHandles.Lookup lookup, Class<?> memoryUtilClass)
            throws IllegalAccessException, NoSuchMethodException {
        for (Method method : memoryUtilClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!method.getName().equals("memAddress")) {
                continue;
            }
            if (method.getReturnType() != long.class || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (parameterType.isAssignableFrom(IntBuffer.class)) {
                return lookup.unreflect(method).asType(MethodType.methodType(long.class, IntBuffer.class));
            }
        }

        throw new NoSuchMethodException("org.lwjgl.system.MemoryUtil.memAddress(IntBuffer)");
    }

    private static long getFunctionPointer(MethodHandle getFunctionAddress, Object functionProvider, String name)
            throws Throwable {
        long address = (long) getFunctionAddress.invokeExact(functionProvider, (CharSequence) name);
        if (address == 0L) {
            MCH_Logger.error("[AppleVAO] Failed to get function pointer for {}", name);
        }
        return address;
    }

    private static long memAddress(IntBuffer buffer) {
        try {
            return (long) MH_MEM_ADDRESS.invokeExact(buffer);
        } catch (Throwable t) {
            throw propagate(t);
        }
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(t);
    }
}
