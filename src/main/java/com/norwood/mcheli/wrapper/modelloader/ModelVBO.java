package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.client._IModelCustom;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModelVBO extends W_ModelCustom implements _IModelCustom {

    static int VERTEX_SIZE = 3;
    static int UV_SIZE = 3;
    List<ModelVBO.VBOBufferData> groups = new ArrayList<>();

    public ModelVBO(W_WavefrontObject obj) {
        uploadVBO(obj.groupObjects);

    }


    public ModelVBO(W_MetasequoiaObject obj) {
        uploadVBO(cleanUpMsqMess(obj.groupObjects));
    }

    //Metasequoia users seem to not understand what proper mesh grouping is, so we need to do it for them
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
            ModelVBO.VBOBufferData data = new ModelVBO.VBOBufferData();
            data.name = g.name;

            FloatBuffer vertexData = BufferUtils.createFloatBuffer(g.faces.size() * 3 * VERTEX_SIZE);
            FloatBuffer uvData = BufferUtils.createFloatBuffer(g.faces.size() * 3 * UV_SIZE);
            FloatBuffer normalData = BufferUtils.createFloatBuffer(g.faces.size() * 3 * VERTEX_SIZE);

            for (W_Face face : g.faces) {
                for (int i = 0; i < face.vertices.length; i++) {
                    W_Vertex vert = face.vertices[i];
                    W_TextureCoordinate tex = new W_TextureCoordinate(0, 0);
                    W_Vertex normal = face.vertexNormals[i];

                    if (face.textureCoordinates != null && face.textureCoordinates.length > 0) {
                        tex = face.textureCoordinates[i];
                    }

                    data.vertices++;
                    vertexData.put(new float[]{vert.x, vert.y, vert.z});
                    uvData.put(new float[]{tex.u, tex.v, tex.w});
                    normalData.put(new float[]{normal.x, normal.y, normal.z});
                }
            }
            vertexData.flip();
            uvData.flip();
            normalData.flip();

            data.vertexHandle = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.vertexHandle);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            data.uvHandle = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.uvHandle);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvData, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            data.normalHandle = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.normalHandle);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalData, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);


            groups.add(data);
        }

    }

    @Override
    public String getType() {
        return "obj_vbo";
    }

   private void prepare(ModelVBO.VBOBufferData data) {
       GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.vertexHandle);
       GL11.glVertexPointer(VERTEX_SIZE, GL11.GL_FLOAT, 0, 0l);

       GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.uvHandle);
       GL11.glTexCoordPointer(UV_SIZE, GL11.GL_FLOAT, 0, 0l);

       GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.normalHandle);
       GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0l);

       GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
       GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
       GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
   }
   private void clean(){
       GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
       GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
       GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
       GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
   }

    private void renderVBO(ModelVBO.VBOBufferData data) {
        prepare(data);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, data.vertices);
        clean();
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
    public void renderAll(int var1, int var2) {

    }

    @Override
    public void renderAllLine(int var1, int var2) {

    }

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
        int vertexHandle;
        int uvHandle;
        int normalHandle;
        int vaoHandle;

    }

}
