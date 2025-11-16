package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.client._ModelFormatException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

@SideOnly(Side.CLIENT)
public class W_MetasequoiaObject extends W_ModelCustom {

    public ArrayList<W_Vertex> vertices = new ArrayList<>();
    public final ArrayList<GroupObject> groupObjects = new ArrayList<>();
    private final String fileName;
    private int vertexNum = 0;
    private int faceNum = 0;

    public ModelVBO asVBO() {
        return new ModelVBO(this);
    }

    public W_MetasequoiaObject(ResourceLocation location, IResource resource) throws _ModelFormatException {
        this.fileName = resource.toString();
        this.loadObjModel(resource.getInputStream());
    }

    public W_MetasequoiaObject(ResourceLocation resource) throws _ModelFormatException {
        this.fileName = resource.toString();

        try {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            this.loadObjModel(res.getInputStream());
        } catch (IOException var3) {
            throw new _ModelFormatException("IO Exception reading model format:" + this.fileName, var3);
        }
    }

    public W_MetasequoiaObject(String fileName, URL resource) throws _ModelFormatException {
        this.fileName = fileName;

        try {
            this.loadObjModel(resource.openStream());
        } catch (IOException var4) {
            throw new _ModelFormatException("IO Exception reading model format:" + this.fileName, var4);
        }
    }

    public W_MetasequoiaObject(String filename, InputStream inputStream) throws _ModelFormatException {
        this.fileName = filename;
        this.loadObjModel(inputStream);
    }

    private boolean isValidGroupObjectLine(String line) {
        String[] s = line.split(" ");
        return s.length >= 2 && s[0].equals("Object") && s[1].length() >= 4 && s[1].charAt(0) == '"';
    }

    private boolean isValidVertexLine(String line) {
        String[] s = line.split(" ");
        return s[0].equals("vertex");
    }

    private boolean isValidFaceLine(String line) {
        String[] s = line.split(" ");
        return s[0].equals("face");
    }

    @Override
    public boolean containsPart(String partName) {
        for (GroupObject groupObject : this.groupObjects) {
            if (partName.equalsIgnoreCase(groupObject.name)) {
                return true;
            }
        }

        return false;
    }

    private void loadObjModel(InputStream inputStream) throws _ModelFormatException {
        BufferedReader reader = null;
        String currentLine;
        int lineCount = 0;

        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));

            while ((currentLine = reader.readLine()) != null) {
                lineCount++;
                currentLine = normalizeWhitespace(currentLine);
                if (isValidGroupObjectLine(currentLine)) {
                    GroupObject group = this.parseGroupObject(currentLine, lineCount);
                    if (group != null) {
                        group.glDrawingMode = 4;
                        this.vertices.clear();
                        int vertexNum = 0;
                        boolean mirror = false;
                        double facet = Math.cos(0.785398163375);
                        boolean shading = false;

                        while ((currentLine = reader.readLine()) != null) {
                            lineCount++;
                            currentLine = normalizeWhitespace(currentLine);
                            if (currentLine.equalsIgnoreCase("mirror 1")) {
                                mirror = true;
                            }

                            if (currentLine.equalsIgnoreCase("shading 1")) {
                                shading = true;
                            }

                            String[] s = currentLine.split(" ");
                            if (s.length == 2 && s[0].equalsIgnoreCase("facet")) {
                                facet = Math.cos(Double.parseDouble(s[1]) * 3.1415926535 / 180.0);
                            }

                            if (isValidVertexLine(currentLine)) {
                                vertexNum = Integer.parseInt(currentLine.split(" ")[1]);
                                break;
                            }
                        }

                        if (vertexNum > 0) {
                            while ((currentLine = reader.readLine()) != null) {
                                lineCount++;
                                currentLine = normalizeWhitespace(currentLine);
                                String[] sx = currentLine.split(" ");
                                if (sx.length == 3) {
                                    W_Vertex v = new W_Vertex(Float.parseFloat(sx[0]) / 100.0F,
                                            Float.parseFloat(sx[1]) / 100.0F, Float.parseFloat(sx[2]) / 100.0F);
                                    this.checkMinMax(v);
                                    this.vertices.add(v);
                                    if (--vertexNum <= 0) {
                                        break;
                                    }
                                } else if (sx.length > 0) {
                                    throw new _ModelFormatException(
                                            "format error : " + this.fileName + " : line=" + lineCount);
                                }
                            }

                            int faceNum = 0;

                            while ((currentLine = reader.readLine()) != null) {
                                lineCount++;
                                currentLine = normalizeWhitespace(currentLine);
                                if (isValidFaceLine(currentLine)) {
                                    faceNum = Integer.parseInt(currentLine.split(" ")[1]);
                                    break;
                                }
                            }

                            if (faceNum > 0) {
                                while ((currentLine = reader.readLine()) != null) {
                                    lineCount++;
                                    currentLine = normalizeWhitespace(currentLine);
                                    String[] sx = currentLine.split(" ");
                                    if (sx.length <= 2) {} else {
                                        if (Integer.parseInt(sx[0]) >= 3) {
                                            W_Face[] faces = this.parseFace(currentLine, lineCount, mirror);

                                            Collections.addAll(group.faces, faces);
                                        }

                                        if (--faceNum <= 0) {
                                            break;
                                        }
                                    }
                                }

                                this.calcVerticesNormal(group, shading, facet);
                            }
                        }

                        this.vertexNum = this.vertexNum + this.vertices.size();
                        this.faceNum = this.faceNum + group.faces.size();
                        this.vertices.clear();
                        this.groupObjects.add(group);
                    }
                }
            }
        } catch (IOException var28) {
            throw new _ModelFormatException("IO Exception reading model format : " + this.fileName, var28);
        } finally {
            this.checkMinMaxFinal();
            this.vertices = null;

            try {
                reader.close();
            } catch (IOException var27) {}

            try {
                inputStream.close();
            } catch (IOException var26) {}
        }
    }

    public void calcVerticesNormal(GroupObject group, boolean shading, double facet) {
        for (W_Face f : group.faces) {
            f.vertexNormals = new W_Vertex[f.verticesID.length];

            for (int i = 0; i < f.verticesID.length; i++) {
                W_Vertex vn = this.getVerticesNormalFromFace(f.faceNormal, f.verticesID[i], group, (float) facet);
                vn.normalize();
                if (shading) {
                    if (f.faceNormal.x * vn.x + f.faceNormal.y * vn.y + f.faceNormal.z * vn.z >= facet) {
                        f.vertexNormals[i] = vn;
                    } else {
                        f.vertexNormals[i] = f.faceNormal;
                    }
                } else {
                    f.vertexNormals[i] = f.faceNormal;
                }
            }
        }
    }

    public W_Vertex getVerticesNormalFromFace(W_Vertex faceNormal, int verticesID, GroupObject group, float facet) {
        W_Vertex v = new W_Vertex(0.0F, 0.0F, 0.0F);

        for (W_Face f : group.faces) {
            for (int id : f.verticesID) {
                if (id == verticesID) {
                    if (!(f.faceNormal.x * faceNormal.x + f.faceNormal.y * faceNormal.y +
                            f.faceNormal.z * faceNormal.z < facet)) {
                        v.add(f.faceNormal);
                    }
                    break;
                }
            }
        }

        v.normalize();
        return v;
    }

    @Override
    public void renderAll() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(4, DefaultVertexFormats.POSITION_TEX_NORMAL);

        this.tessellateAll(tessellator);
        tessellator.draw();
    }

    public void tessellateAll(Tessellator tessellator) {
        for (GroupObject groupObject : this.groupObjects) {
            groupObject.render(tessellator);
        }
    }

    @Override
    public void renderOnly(String... groupNames) {
        for (GroupObject groupObject : this.groupObjects) {
            for (String groupName : groupNames) {
                if (groupName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render();
                }
            }
        }
    }

    public void tessellateOnly(Tessellator tessellator, String... groupNames) {
        for (GroupObject groupObject : this.groupObjects) {
            for (String groupName : groupNames) {
                if (groupName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render(tessellator);
                }
            }
        }
    }

    @Override
    public void renderPart(String partName) {
        if (partName.charAt(0) == '$') {
            for (int i = 0; i < this.groupObjects.size(); i++) {
                GroupObject groupObject = this.groupObjects.get(i);
                if (partName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render();
                    i++;

                    while (i < this.groupObjects.size()) {
                        groupObject = this.groupObjects.get(i);
                        if (groupObject.name.charAt(0) == '$') {
                            break;
                        }

                        groupObject.render();
                        i++;
                    }
                }
            }
        } else {
            for (GroupObject groupObject : this.groupObjects) {
                if (partName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render();
                }
            }
        }
    }

    public void tessellatePart(Tessellator tessellator, String partName) {
        for (GroupObject groupObject : this.groupObjects) {
            if (partName.equalsIgnoreCase(groupObject.name)) {
                groupObject.render(tessellator);
            }
        }
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        for (GroupObject groupObject : this.groupObjects) {
            boolean skipPart = false;

            for (String excludedGroupName : excludedGroupNames) {
                if (excludedGroupName.equalsIgnoreCase(groupObject.name)) {
                    skipPart = true;
                    break;
                }
            }

            if (!skipPart) {
                groupObject.render();
            }
        }
    }

    @Override
    public _IModelCustom toVBO() {
        return this.asVBO();
    }

    public void tessellateAllExcept(Tessellator tessellator, String... excludedGroupNames) {
        for (GroupObject groupObject : this.groupObjects) {
            boolean exclude = false;

            for (String excludedGroupName : excludedGroupNames) {
                if (excludedGroupName.equalsIgnoreCase(groupObject.name)) {
                    exclude = true;
                    break;
                }
            }

            if (!exclude) {
                groupObject.render(tessellator);
            }
        }
    }

    private W_Face[] parseFace(String line, int lineCount, boolean mirror) {
        String[] s = line.split("[ VU)(M]+");
        int vnum = Integer.parseInt(s[0]);
        if (vnum != 3 && vnum != 4) {
            return new W_Face[0];
        } else if (vnum == 3) {
            W_Face face = new W_Face();
            face.verticesID = new int[] { Integer.parseInt(s[3]), Integer.parseInt(s[2]), Integer.parseInt(s[1]) };
            face.vertices = new W_Vertex[] { this.vertices.get(face.verticesID[0]),
                    this.vertices.get(face.verticesID[1]), this.vertices.get(face.verticesID[2]) };
            if (s.length >= 11) {
                face.textureCoordinates = new W_TextureCoordinate[] {
                        new W_TextureCoordinate(Float.parseFloat(s[9]), Float.parseFloat(s[10])),
                        new W_TextureCoordinate(Float.parseFloat(s[7]), Float.parseFloat(s[8])),
                        new W_TextureCoordinate(Float.parseFloat(s[5]), Float.parseFloat(s[6]))
                };
            } else {
                face.textureCoordinates = new W_TextureCoordinate[] {
                        new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F),
                        new W_TextureCoordinate(0.0F, 0.0F)
                };
            }

            face.faceNormal = face.calculateFaceNormal();
            return new W_Face[] { face };
        } else {
            W_Face face1 = new W_Face();
            face1.verticesID = new int[] { Integer.parseInt(s[3]), Integer.parseInt(s[2]), Integer.parseInt(s[1]) };
            face1.vertices = new W_Vertex[] {
                    this.vertices.get(face1.verticesID[0]), this.vertices.get(face1.verticesID[1]),
                    this.vertices.get(face1.verticesID[2])
            };
            if (s.length >= 12) {
                face1.textureCoordinates = new W_TextureCoordinate[] {
                        new W_TextureCoordinate(Float.parseFloat(s[10]), Float.parseFloat(s[11])),
                        new W_TextureCoordinate(Float.parseFloat(s[8]), Float.parseFloat(s[9])),
                        new W_TextureCoordinate(Float.parseFloat(s[6]), Float.parseFloat(s[7]))
                };
            } else {
                face1.textureCoordinates = new W_TextureCoordinate[] {
                        new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F),
                        new W_TextureCoordinate(0.0F, 0.0F)
                };
            }

            face1.faceNormal = face1.calculateFaceNormal();
            W_Face face2 = new W_Face();
            face2.verticesID = new int[] { Integer.parseInt(s[4]), Integer.parseInt(s[3]), Integer.parseInt(s[1]) };
            face2.vertices = new W_Vertex[] {
                    this.vertices.get(face2.verticesID[0]), this.vertices.get(face2.verticesID[1]),
                    this.vertices.get(face2.verticesID[2])
            };
            if (s.length >= 14) {
                face2.textureCoordinates = new W_TextureCoordinate[] {
                        new W_TextureCoordinate(Float.parseFloat(s[12]), Float.parseFloat(s[13])),
                        new W_TextureCoordinate(Float.parseFloat(s[10]), Float.parseFloat(s[11])),
                        new W_TextureCoordinate(Float.parseFloat(s[6]), Float.parseFloat(s[7]))
                };
            } else {
                face2.textureCoordinates = new W_TextureCoordinate[] {
                        new W_TextureCoordinate(0.0F, 0.0F), new W_TextureCoordinate(0.0F, 0.0F),
                        new W_TextureCoordinate(0.0F, 0.0F)
                };
            }

            face2.faceNormal = face2.calculateFaceNormal();
            return new W_Face[] { face1, face2 };
        }
    }

    private GroupObject parseGroupObject(String line, int lineCount) throws _ModelFormatException {
        GroupObject group = null;
        if (isValidGroupObjectLine(line)) {
            String[] s = line.split(" ");
            String trimmedLine = s[1].substring(1, s[1].length() - 1);
            if (!trimmedLine.isEmpty()) {
                group = new GroupObject(trimmedLine);
            }

            return group;
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" +
                    this.fileName + "' - Incorrect format");
        }
    }

    @Override
    public String getType() {
        return "mqo";
    }

    @Override
    public void renderAllLine(int startLine, int maxLine) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(1, DefaultVertexFormats.POSITION);
        this.renderAllLine(tessellator, startLine, maxLine);
        tessellator.draw();
    }

    public void renderAllLine(Tessellator tessellator, int startLine, int maxLine) {
        int lineCnt = 0;
        BufferBuilder builder = tessellator.getBuffer();

        for (GroupObject groupObject : this.groupObjects) {
            if (!groupObject.faces.isEmpty()) {
                for (W_Face face : groupObject.faces) {
                    for (int i = 0; i < face.vertices.length / 3; i++) {
                        W_Vertex v1 = face.vertices[i * 3];
                        W_Vertex v2 = face.vertices[i * 3 + 1];
                        W_Vertex v3 = face.vertices[i * 3 + 2];
                        if (++lineCnt > maxLine) {
                            return;
                        }

                        builder.pos(v1.x, v1.y, v1.z).endVertex();
                        builder.pos(v2.x, v2.y, v2.z).endVertex();
                        if (++lineCnt > maxLine) {
                            return;
                        }

                        builder.pos(v2.x, v2.y, v2.z).endVertex();
                        builder.pos(v3.x, v3.y, v3.z).endVertex();
                        if (++lineCnt > maxLine) {
                            return;
                        }

                        builder.pos(v3.x, v3.y, v3.z).endVertex();
                        builder.pos(v1.x, v1.y, v1.z).endVertex();
                    }
                }
            }
        }
    }

    @Override
    public int getVertexNum() {
        return this.vertexNum;
    }

    @Override
    public int getFaceNum() {
        return this.faceNum;
    }

    @Override
    public void renderAll(int startFace, int maxFace) {
        if (startFace < 0) {
            startFace = 0;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(4, DefaultVertexFormats.POSITION_TEX_NORMAL);
        this.renderAll(tessellator, startFace, maxFace);
        tessellator.draw();
    }

    public void renderAll(Tessellator tessellator, int startFace, int maxLine) {
        int faceCnt = 0;

        for (GroupObject groupObject : this.groupObjects) {
            if (!groupObject.faces.isEmpty()) {
                for (W_Face face : groupObject.faces) {
                    if (++faceCnt >= startFace) {
                        if (faceCnt > maxLine) {
                            return;
                        }

                        face.addFaceForRender(tessellator);
                    }
                }
            }
        }
    }
}
