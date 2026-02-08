package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.client._IModelCustom;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.ARBCopyBuffer.GL_COPY_READ_BUFFER;
import static org.lwjgl.opengl.ARBCopyBuffer.glCopyBufferSubData;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

public class ModelVBO extends W_ModelCustom implements _IModelCustom {

    private static final int FLOAT_SIZE = 4;
    private static final int STRIDE = 9 * FLOAT_SIZE;
    static int VERTEX_SIZE = 3;
    List<ModelVBO.VBOBufferData> groups = new ArrayList<>();

    private int staticVAO = -1;
    private int staticVBO = -1;
    private int bakedTracksVAO = -1;
    private int bakedTracksVBO = -1;

    private int staticVerts = -1;
    private float[] treadBuffer;
    private int trackVerts;

    public ModelVBO(W_WavefrontObject obj) {
        uploadVBO(obj.groupObjects);

        this.min = obj.min;
        this.minX = obj.minX;
        this.minY = obj.minY;
        this.minZ = obj.minZ;

        this.max = obj.max;
        this.maxX = obj.maxX;
        this.maxY = obj.maxY;
        this.maxZ = obj.maxZ;

        this.size = obj.size;
        this.sizeX = obj.sizeX;
        this.sizeY = obj.sizeY;
        this.sizeZ = obj.sizeZ;
    }

    public ModelVBO(W_MetasequoiaObject obj) {
        uploadVBO(cleanUpMsqMess(obj.groupObjects));

        this.min = obj.min;
        this.minX = obj.minX;
        this.minY = obj.minY;
        this.minZ = obj.minZ;

        this.max = obj.max;
        this.maxX = obj.maxX;
        this.maxY = obj.maxY;
        this.maxZ = obj.maxZ;

        this.size = obj.size;
        this.sizeX = obj.sizeX;
        this.sizeY = obj.sizeY;
        this.sizeZ = obj.sizeZ;
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

            float[] combinedData = prepareGroupData(g, data); // helper method to flatten vertices/uv/normals
            FloatBuffer buffer = BufferUtils.createFloatBuffer(combinedData.length);
            buffer.put(combinedData).flip();
            if (g.name.contains("crawler_track")) { //Really fragile but oh well
                treadBuffer = combinedData;
            }

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

    public void uploadStatic(MCH_AircraftInfo info) {
        int airframeVerts = groups.stream()
                .filter(g -> !g.name.contains("crawler_track"))
                .mapToInt(g -> g.vertices)
                .sum();

        staticVAO = GL30.glGenVertexArrays();
        staticVBO = glGenBuffers();

        GL30.glBindVertexArray(staticVAO);
        glBindBuffer(GL_ARRAY_BUFFER, staticVBO);
        glBufferData(GL_ARRAY_BUFFER, (long) airframeVerts * 9 * Float.BYTES, GL_STATIC_DRAW);

        glVertexPointer(3, GL_FLOAT, STRIDE, 0L);
        glEnableClientState(GL_VERTEX_ARRAY);
        glTexCoordPointer(3, GL_FLOAT, STRIDE, 3L * Float.BYTES);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glNormalPointer(GL_FLOAT, STRIDE, 6L * Float.BYTES);
        glEnableClientState(GL_NORMAL_ARRAY);

        long offset = 0;
        for (VBOBufferData g : groups) {
            if (g.name.contains("crawler_track")) continue;

            glBindBuffer(GL_COPY_READ_BUFFER, g.vboHandle);
            long size = g.vertices * 9L * Float.BYTES;
            glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_ARRAY_BUFFER, 0, offset, size);
            offset += size;
        }
        this.staticVerts = airframeVerts;


        if (treadBuffer != null && !info.partCrawlerTrack.isEmpty()) {
            uploadTracks(info);
        } else {

            GL30.glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindBuffer(GL_COPY_READ_BUFFER, 0);
        }
    }

    private void uploadTracks(MCH_AircraftInfo info) {
        float[] bakedData = bakeCrawlerTrack(info, treadBuffer);
        this.trackVerts = bakedData.length / 9;

        bakedTracksVAO = GL30.glGenVertexArrays();
        bakedTracksVBO = glGenBuffers();

        GL30.glBindVertexArray(bakedTracksVAO);
        glBindBuffer(GL_ARRAY_BUFFER, bakedTracksVBO);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(bakedData.length);
        buffer.put(bakedData).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexPointer(3, GL_FLOAT, STRIDE, 0L);
        glEnableClientState(GL_VERTEX_ARRAY);
        glTexCoordPointer(3, GL_FLOAT, STRIDE, 3L * Float.BYTES);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glNormalPointer(GL_FLOAT, STRIDE, 6L * Float.BYTES);
        glEnableClientState(GL_NORMAL_ARRAY);


        GL30.glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
    }


    private float[] prepareGroupData(GroupObject g, VBOBufferData data) {
        List<Float> vertexData = new ArrayList<>(g.faces.size() * 3 * VERTEX_SIZE);
        List<Float> uvwData = new ArrayList<>(g.faces.size() * 3 * VERTEX_SIZE);
        List<Float> normalData = new ArrayList<>(g.faces.size() * 3 * VERTEX_SIZE);

        for (W_Face face : g.faces) {
            for (int i = 0; i < face.vertices.length; i++) {
                W_Vertex vert = face.vertices[i];
                W_TextureCoordinate tex = (face.textureCoordinates != null && face.textureCoordinates.length > 0)
                        ? face.textureCoordinates[i]
                        : new W_TextureCoordinate(0, 0);
                W_Vertex normal = (face.vertexNormals != null) ? face.vertexNormals[i] : new W_Vertex(0, 0, 0);

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
        return combinedData;
    }

    public void delete() {
        var vaoIDBuffer = BufferUtils.createIntBuffer(groups.size());
        var vboIDBuffer = BufferUtils.createIntBuffer(groups.size());
        for (VBOBufferData data : groups) {
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

    public void renderStatic(MCH_AircraftInfo info) {
        if (staticVAO == -1)
            uploadStatic(info);
        GL30.glBindVertexArray(this.staticVAO);
        GlStateManager.glDrawArrays(GL11.GL_TRIANGLES, 0, staticVerts);
        if (this.bakedTracksVAO != -1) {
            GL30.glBindVertexArray(this.bakedTracksVAO);
            GlStateManager.glDrawArrays(GL11.GL_TRIANGLES, 0, trackVerts);
        }

        GL30.glBindVertexArray(0);
    }

    public void renderTracksBuffer(MCH_AircraftInfo info) {
        if (treadBuffer == null || info.partCrawlerTrack.isEmpty()) {
            MCH_Logger.error("Attempted to render tracks for a vehicle that does not have them!");
            return;
        }

        if (bakedTracksVAO == -1)
            uploadTracks(info);
        GL30.glBindVertexArray(this.bakedTracksVAO);
        GlStateManager.glDrawArrays(GL11.GL_TRIANGLES, 0, trackVerts);

        GL30.glBindVertexArray(0);
    }


    public float[] bakeCrawlerTrack(MCH_AircraftInfo info, float[] treadTemplate) {
        if (info.partCrawlerTrack.isEmpty()) return new float[0];

        MCH_AircraftInfo.CrawlerTrack track = info.partCrawlerTrack.get(0);
        int pointCount = track.lp.size() - 1;
        int singleTrackSize = pointCount * treadTemplate.length;

        float[] baked = new float[singleTrackSize * 2];
        int cursor = 0;

        for (int i = 0; i < pointCount; i++) {
            MCH_AircraftInfo.CrawlerTrackPrm current = track.lp.get(i);

            float rad = (float) Math.toRadians(current.r);
            float cosR = (float) Math.cos(rad);
            float sinR = (float) Math.sin(rad);

            for (int j = 0; j < treadTemplate.length; j += 9) {
                float vx = treadTemplate[j];
                float vy = treadTemplate[j + 1];
                float vz = treadTemplate[j + 2];

                float nx = treadTemplate[j + 6];
                float ny = treadTemplate[j + 7];
                float nz = treadTemplate[j + 8];

                baked[cursor++] = vx;

                baked[cursor++] = (vy * cosR + vz * sinR) + (float)current.x;
                baked[cursor++] = (-vy * sinR + vz * cosR) + (float)current.y;

                baked[cursor++] = treadTemplate[j + 3];
                baked[cursor++] = treadTemplate[j + 4];
                baked[cursor++] = treadTemplate[j + 5];

                baked[cursor++] = nx;
                baked[cursor++] = (ny * cosR + nz * sinR);
                baked[cursor++] = (-ny * sinR + nz * cosR);
            }
        }

        for (int i = 0; i < singleTrackSize; i += 27) {
            for (int v = 0; v < 3; v++) {
                int srcOffset = i + (v * 9);
                int dstOffset = singleTrackSize + i + ((2 - v) * 9);

                baked[dstOffset]     = -baked[srcOffset];
                baked[dstOffset + 1] =  baked[srcOffset + 1];
                baked[dstOffset + 2] =  baked[srcOffset + 2];

                baked[dstOffset + 3] =  baked[srcOffset + 3];
                baked[dstOffset + 4] =  baked[srcOffset + 4];
                baked[dstOffset + 5] =  baked[srcOffset + 5];

                baked[dstOffset + 6] = -baked[srcOffset + 6];
                baked[dstOffset + 7] =  baked[srcOffset + 7];
                baked[dstOffset + 8] =  baked[srcOffset + 8];
            }
        }

        return baked;
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

    static class VBOBufferData {

        String name;
        int vertices = 0;
        int vboHandle;
        int vaoHandle;
    }
}
