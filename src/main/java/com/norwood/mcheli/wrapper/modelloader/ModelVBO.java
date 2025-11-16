package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.client._IModelCustom;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

public class ModelVBO extends W_ModelCustom implements _IModelCustom {

    private static final int FLOAT_SIZE = 4;
    private static final int STRIDE = 9 * FLOAT_SIZE;
    static int VERTEX_SIZE = 3;
    List<ModelVBO.VBOBufferData> groups = new ArrayList<>();

    public ModelVBO(W_WavefrontObject obj) {
        uploadVBO(obj.groupObjects);
    }

    public ModelVBO(W_MetasequoiaObject obj) {
        uploadVBO(cleanUpMsqMess(obj.groupObjects));
    }

    // Metasequoia users seem to not understand what proper mesh grouping is, so we need to do it for them
    private static List<GroupObject> cleanUpMsqMess(ArrayList<GroupObject> groupObjects) {
        List<GroupObject> result = new ArrayList<>();
        GroupObject currentKey = null;

        for (GroupObject obj : groupObjects) {
            String name = obj.name;

            if (name.isEmpty()) continue;

            if (name.charAt(0) == '$') {
                // Start a new key group
                currentKey = new GroupObject(name);
                currentKey.faces.addAll(obj.faces); // keep existing faces if any
                result.add(currentKey);
            } else if (currentKey != null) {
                // Merge into the most recent $ group
                currentKey.faces.addAll(obj.faces);
            } else {
                // Ungrouped before any $, keep standalone
                result.add(obj);
            }
        }

        return result;
    }

    private void uploadVBO(List<GroupObject> obj) {
        for (GroupObject g : obj) {
            VBOBufferData data = new VBOBufferData();
            data.name = g.name;

            List<Float> vertexData = new ArrayList<>(g.faces.size() * 3 * VERTEX_SIZE);
            List<Float> uvwData = new ArrayList<>(g.faces.size() * 3 * VERTEX_SIZE);
            List<Float> normalData = new ArrayList<>(g.faces.size() * 3 * VERTEX_SIZE);

            for (W_Face face : g.faces) {
                for (int i = 0; i < face.vertices.length; i++) {
                    W_Vertex vert = face.vertices[i];
                    W_TextureCoordinate tex = new W_TextureCoordinate(0, 0);
                    W_Vertex normal = face.vertexNormals != null ?  face.vertexNormals[i] : new W_Vertex(0,0,0);//Oh yeah, sometimes models just miss those... What the fuck

                    if (face.textureCoordinates != null && face.textureCoordinates.length > 0) {
                        tex = face.textureCoordinates[i];
                    }

                    data.vertices++;
                    vertexData.add(vert.x);
                    vertexData.add(vert.y);
                    vertexData.add(vert.z);

                    uvwData.add(tex.u);
                    uvwData.add(tex.v);
                    uvwData.add(tex.w);

                    normalData.add(normal.x);
                    normalData.add(normal.y);
                    normalData.add(normal.z);
                }
            }
            float[] combinedData = new float[data.vertices * 9];
            int dst = 0;
            for (int i = 0; i < data.vertices; i++) {
                combinedData[dst++] = vertexData.get(i * 3);
                combinedData[dst++] = vertexData.get(i * 3 + 1);
                combinedData[dst++] = vertexData.get(i * 3 + 2);

                combinedData[dst++] = uvwData.get(i * 3);
                combinedData[dst++] = uvwData.get(i * 3 + 1);
                combinedData[dst++] = uvwData.get(i * 3 + 2);

                combinedData[dst++] = normalData.get(i * 3);
                combinedData[dst++] = normalData.get(i * 3 + 1);
                combinedData[dst++] = normalData.get(i * 3 + 2);
            }

            FloatBuffer buffer = BufferUtils.createFloatBuffer(combinedData.length);
            buffer.put(combinedData);
            buffer.flip();

            data.vaoHandle = GL30.glGenVertexArrays();
            data.vboHandle = glGenBuffers();
            GL30.glBindVertexArray(data.vaoHandle);
            glBindBuffer(GL_ARRAY_BUFFER, data.vboHandle);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

            GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, 0L);
            glEnableClientState(GL_VERTEX_ARRAY);

            GL11.glTexCoordPointer(3, GL11.GL_FLOAT, STRIDE, 3L * Float.BYTES);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);

            GL11.glNormalPointer(GL11.GL_FLOAT, STRIDE, 6L * Float.BYTES);
            glEnableClientState(GL_NORMAL_ARRAY);

            GL30.glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            groups.add(data);
        }
    }

    public void delete(){
        var vaoIDBuffer = BufferUtils.createIntBuffer(groups.size());
        var vboIDBuffer = BufferUtils.createIntBuffer(groups.size());
        for(VBOBufferData data : groups){
            vaoIDBuffer.put(data.vaoHandle);
            vboIDBuffer.put(data.vboHandle);
        }
        vaoIDBuffer.flip();
        vboIDBuffer.flip();

        GL30.glDeleteVertexArrays(vaoIDBuffer);
        GL15.glDeleteBuffers(vboIDBuffer);
    }

    private void renderVBO(ModelVBO.VBOBufferData data) {
        GL30.glBindVertexArray(data.vaoHandle);
        GlStateManager.glDrawArrays(GL11.GL_TRIANGLES, 0, data.vertices);
        GL30.glBindVertexArray(0);
    }

    @Override
    public String getType() {
        return "obj_vbo";
    }

    @Override
    public void renderAll() {
        for (ModelVBO.VBOBufferData data : groups) {
            renderVBO(data);
        }
    }

    @Override
    public void renderOnly(String... groupNames) {
        for (ModelVBO.VBOBufferData data : groups) {
            for (String name : groupNames) {
                if (data.name.equalsIgnoreCase(name)) {
                    renderVBO(data);
                }
            }
        }
    }

    @Override
    public void renderPart(String partName) {
        for (ModelVBO.VBOBufferData data : groups) {
            if (data.name.equalsIgnoreCase(partName)) {
                renderVBO(data);
            }
        }
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        for (ModelVBO.VBOBufferData data : groups) {
            boolean skip = false;
            for (String name : excludedGroupNames) {
                if (data.name.equalsIgnoreCase(name)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                renderVBO(data);
            }
        }
    }

    @Override
    public _IModelCustom toVBO() {
        return this;
    }

    @Override
    public boolean containsPart(String partName) {
        for (ModelVBO.VBOBufferData data : groups) {
            if (data.name.equalsIgnoreCase(partName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void renderAll(int var1, int var2) {}

    @Override
    public void renderAllLine(int var1, int var2) {}

    @Override
    public int getVertexNum() {
        return 0;
    }

    @Override
    public int getFaceNum() {
        return 0;
    }

    class VBOBufferData {

        String name;
        int vertices = 0;
        int vboHandle;
        int vaoHandle;
    }
}
