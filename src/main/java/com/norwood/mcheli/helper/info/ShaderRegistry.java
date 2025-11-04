package com.norwood.mcheli.helper.info;

import lombok.SneakyThrows;
import net.minecraft.client.shader.Shader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.glUniformMatrix4;

@SideOnly(Side.CLIENT)
public class ShaderRegistry {
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
   private static final Map<String, ShaderProgram> shaderMap = new HashMap<>();

    public static void uploadMatrix4f(int uniformLoc, Matrix4f mat) {
        MATRIX_BUFFER.clear();
        mat.get(MATRIX_BUFFER);
        MATRIX_BUFFER.flip();
        GL20.glUniformMatrix4(uniformLoc, false, MATRIX_BUFFER);
    }

   public void init(){

   }

   @Nullable
   public static ShaderProgram getShader(String key){
       return shaderMap.get(key);
   }

    public class ShaderProgram {
        private final int programId;
        private final Map<String, Integer> uniformCache = new HashMap<>();

        @SneakyThrows
        public ShaderProgram(String vertexPath, String fragmentPath) {
            int vert = compile(GL20.GL_VERTEX_SHADER, new String(Files.readAllBytes(Paths.get(vertexPath)), StandardCharsets.UTF_8));
            int frag = compile(GL20.GL_FRAGMENT_SHADER,new String(Files.readAllBytes(Paths.get(fragmentPath)), StandardCharsets.UTF_8));
            programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, vert);
            GL20.glAttachShader(programId, frag);
            GL20.glLinkProgram(programId);
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
        }

        private int compile(int type, String src) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, src);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException(GL20.glGetShaderInfoLog(shader, 100));
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

    public class BasicLightingShader extends ShaderProgram {
        private final int uModelMatrix;
        private final int uLightDir;
        private final int uColor;

        public BasicLightingShader() {
            super("shaders/basic.vert", "shaders/basic.frag");
            uModelMatrix = getUniformLocation("u_ModelMatrix");
            uLightDir = getUniformLocation("u_LightDir");
            uColor = getUniformLocation("u_Color");
        }

        public void setModelMatrix(Matrix4f mat) {
            uploadMatrix4f(uModelMatrix, mat);
        }

        public void setLightDir(Vector3f dir) {
            GL20.glUniform3f(uLightDir, dir.x, dir.y, dir.z);
        }

        public void setColor(float r, float g, float b) {
            GL20.glUniform3f(uColor, r, g, b);
        }
    }



}
