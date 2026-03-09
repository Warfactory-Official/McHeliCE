package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.MCH_Logger;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.IntBuffer;

final class VertexArraySupport {

    private static final Backend BACKEND;

    static {
        BACKEND = detectBackend();
        MCH_Logger.info("[VAO] Using {} backend", BACKEND);
    }

    private VertexArraySupport() {
    }

    static int glGenVertexArrays() {
        return switch (BACKEND) {
            case GL30 -> GL30.glGenVertexArrays();
            case ARB -> ARBVertexArrayObject.glGenVertexArrays();
            case APPLE -> APPLEVertexArrayObject.glGenVertexArraysAPPLE();
            case APPLE_COMPAT -> AppleVAO.glGenVertexArraysAPPLE();
        };
    }

    static void glBindVertexArray(int vao) {
        switch (BACKEND) {
            case GL30:
                GL30.glBindVertexArray(vao);
                break;
            case ARB:
                ARBVertexArrayObject.glBindVertexArray(vao);
                break;
            case APPLE:
                APPLEVertexArrayObject.glBindVertexArrayAPPLE(vao);
                break;
            case APPLE_COMPAT:
                AppleVAO.glBindVertexArrayAPPLE(vao);
                break;
        }
    }

    static void glDeleteVertexArrays(IntBuffer arrays) {
        switch (BACKEND) {
            case GL30:
                GL30.glDeleteVertexArrays(arrays);
                break;
            case ARB:
                ARBVertexArrayObject.glDeleteVertexArrays(arrays);
                break;
            case APPLE:
                APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(arrays);
                break;
            case APPLE_COMPAT:
                AppleVAO.glDeleteVertexArraysAPPLE(arrays);
                break;
        }
    }

    private static Backend detectBackend() {
        if (hasCapability("OpenGL30")) {
            return Backend.GL30;
        }

        boolean appleVAO = hasCapability("GL_APPLE_vertex_array_object");
        if (isMac() && appleVAO) {
            if (isLwjgl3Runtime()) {
                if (!AppleVAO.isAvailable()) {
                    throw new UnsupportedOperationException("Apple VAO workaround initialization failed");
                }
                return Backend.APPLE_COMPAT;
            }
            return Backend.APPLE;
        }

        if (hasCapability("GL_ARB_vertex_array_object")) {
            return Backend.ARB;
        }

        if (appleVAO) {
            return Backend.APPLE;
        }

        throw new UnsupportedOperationException("Vertex Array Objects are not supported on this system");
    }

    private static boolean hasCapability(String fieldName) {
        Object capabilities = getCapabilities();
        if (capabilities == null) {
            return false;
        }

        try {
            Field field = capabilities.getClass().getField(fieldName);
            return field.getBoolean(capabilities);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object getCapabilities() {
        Object lwjgl3Capabilities = getLwjgl3Capabilities();
        if (lwjgl3Capabilities != null) {
            return lwjgl3Capabilities;
        }

        return getLwjgl2Capabilities();
    }

    private static Object getLwjgl3Capabilities() {
        try {
            Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
            Method getCapabilities = glClass.getMethod("getCapabilities");
            return getCapabilities.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object getLwjgl2Capabilities() {
        try {
            Class<?> glContextClass = Class.forName("org.lwjgl.opengl.GLContext");
            Method getCapabilities = glContextClass.getMethod("getCapabilities");
            return getCapabilities.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean isLwjgl3Runtime() {
        return getLwjgl3Capabilities() != null && classExists("org.lwjgl.system.JNI");
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isMac() {
        String osName = System.getProperty("os.name", "");
        return osName.regionMatches(true, 0, "Mac", 0, 3);
    }

    private enum Backend {
        GL30,
        ARB,
        APPLE,
        APPLE_COMPAT
    }
}
