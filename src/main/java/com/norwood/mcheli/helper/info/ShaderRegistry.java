package com.norwood.mcheli.helper.info;

import static org.lwjgl.opengl.GL20.glUniformMatrix4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.norwood.mcheli.Tags;

import lombok.SneakyThrows;

// Because of muh optifine compat im forced to abandon you for now :/
@SideOnly(Side.CLIENT)
public class ShaderRegistry {

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final Map<String, ShaderProgram> shaderMap = new HashMap<>();
    public static final String SHADER_PATH = "shaders/";

    public static void uploadMatrix4f(int uniformLoc, Matrix4f mat) {
        MATRIX_BUFFER.clear();
        mat.get(MATRIX_BUFFER);
        MATRIX_BUFFER.flip();
        GL20.glUniformMatrix4(uniformLoc, false, MATRIX_BUFFER);
    }

    public static void init() {
        registerShader("basic");
    }

    private static void registerShader(String name) {
        IResource shaderVert = null;
        IResource shaderFrag = null;
        ResourceLocation fragLoc = new ResourceLocation(Tags.MODID, String.format("%s%s.frag", SHADER_PATH, name));
        ResourceLocation vertLoc = new ResourceLocation(Tags.MODID, String.format("%s%s.vert", SHADER_PATH, name));
        try {
            shaderVert = Minecraft.getMinecraft().getResourceManager()
                    .getResource(vertLoc);
            shaderFrag = Minecraft.getMinecraft().getResourceManager()
                    .getResource(fragLoc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var shaderProg = new ShaderProgram(shaderVert.getInputStream(), vertLoc, shaderFrag.getInputStream(), fragLoc);
        shaderMap.put(name, shaderProg);
    }

    private static String readStream(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    @Nullable
    public static ShaderProgram getShader(String key) {
        return shaderMap.get(key);
    }

    public static class ShaderProgram {

        private final int programId;
        private final Map<String, Integer> uniformCache = new HashMap<>();

        @SneakyThrows
        public ShaderProgram(InputStream vertStream, ResourceLocation vertexPath, InputStream fragStream,
                             ResourceLocation fragPath) {
            int vert = compile(GL20.GL_VERTEX_SHADER, readStream(vertStream), vertexPath.toString());
            int frag = compile(GL20.GL_FRAGMENT_SHADER, readStream(fragStream), fragPath.toString());
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, vert);
            GL20.glAttachShader(programId, frag);
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
        }

        private int compile(int type, String src, String path) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, src);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException(
                        "Error compiling shader " + path + "\n" + GL20.glGetShaderInfoLog(shader, 100));
            return shader;
        }

        public void bind() {
            GL20.glUseProgram(programId);
        }

        public void unbind() {
            GL20.glUseProgram(0);
        }

        public void cleanup() {
            GL20.glDeleteProgram(programId);
        }

        public int getUniformLocation(String name) {
            return uniformCache.computeIfAbsent(name,
                    n -> GL20.glGetUniformLocation(programId, n));
        }

        public void setUniform(String name, float v0, float v1, float v2) {
            GL20.glUniform3f(getUniformLocation(name), v0, v1, v2);
        }

        public void setUniformMat4(String name, FloatBuffer matrix) {
            glUniformMatrix4(getUniformLocation(name), false, matrix);
        }
    }
}
