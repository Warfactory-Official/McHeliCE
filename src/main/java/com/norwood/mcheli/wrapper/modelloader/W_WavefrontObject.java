package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.client._ModelFormatException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class W_WavefrontObject extends W_ModelCustom {

    public ArrayList<W_Vertex> vertices = new ArrayList<>();
    public ArrayList<W_Vertex> vertexNormals = new ArrayList<>();
    public ArrayList<W_TextureCoordinate> textureCoordinates = new ArrayList<>();
    public ArrayList<GroupObject> groupObjects = new ArrayList<>();
    private Pattern vertexPattern = Pattern.compile("(v( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(v( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
    private Pattern vertexNormalPattern = Pattern.compile("(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
    private Pattern textureCoordinatePattern = Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *$)");
    private Pattern face_V_VT_VN_Pattern = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
    private Pattern face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
    private Pattern face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
    private Pattern face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");
    private Pattern groupObjectPattern = Pattern.compile("([go]( [-\\$\\w\\d]+) *\\n)|([go]( [-\\$\\w\\d]+) *$)");
    private Matcher vertexMatcher;
    private Matcher vertexNormalMatcher;
    private Matcher textureCoordinateMatcher;
    private Matcher face_V_VT_VN_Matcher;
    private Matcher face_V_VT_Matcher;
    private Matcher face_V_VN_Matcher;
    private Matcher face_V_Matcher;
    private Matcher groupObjectMatcher;
    private GroupObject currentGroupObject;

    private String fileName;

    public W_WavefrontObject(ResourceLocation location, IResource resource) throws _ModelFormatException {
        this.fileName = resource.toString();
        loadObjModel(resource.getInputStream());
    }

    public W_WavefrontObject(String fileName, URL resource) throws _ModelFormatException {
        this.fileName = fileName;
        try {
            loadObjModel(resource.openStream());
        } catch (IOException e) {
            throw new _ModelFormatException("IO Exception reading model format", e);
        }
    }

    public W_WavefrontObject(String filename, InputStream inputStream) throws _ModelFormatException {
        this.fileName = filename;
        loadObjModel(inputStream);
    }

    private boolean isValidVertexLine(String line) {
        if (vertexMatcher != null) vertexMatcher.reset();
        vertexMatcher = vertexPattern.matcher(line);
        return vertexMatcher.matches();
    }

    private boolean isValidVertexNormalLine(String line) {
        if (vertexNormalMatcher != null) vertexNormalMatcher.reset();
        vertexNormalMatcher = vertexNormalPattern.matcher(line);
        return vertexNormalMatcher.matches();
    }

    private boolean isValidTextureCoordinateLine(String line) {
        if (textureCoordinateMatcher != null) textureCoordinateMatcher.reset();
        textureCoordinateMatcher = textureCoordinatePattern.matcher(line);
        return textureCoordinateMatcher.matches();
    }

    private boolean isValidFace_V_VT_VN_Line(String line) {
        if (face_V_VT_VN_Matcher != null) face_V_VT_VN_Matcher.reset();
        face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line);
        return face_V_VT_VN_Matcher.matches();
    }

    private boolean isValidFace_V_VT_Line(String line) {
        if (face_V_VT_Matcher != null) face_V_VT_Matcher.reset();
        face_V_VT_Matcher = face_V_VT_Pattern.matcher(line);
        return face_V_VT_Matcher.matches();
    }

    private boolean isValidFace_V_VN_Line(String line) {
        if (face_V_VN_Matcher != null) face_V_VN_Matcher.reset();
        face_V_VN_Matcher = face_V_VN_Pattern.matcher(line);
        return face_V_VN_Matcher.matches();
    }

    private boolean isValidFace_V_Line(String line) {
        if (face_V_Matcher != null) face_V_Matcher.reset();
        face_V_Matcher = face_V_Pattern.matcher(line);
        return face_V_Matcher.matches();
    }

    private boolean isValidFaceLine(String line) {
        return (isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line));
    }

    private boolean isValidGroupObjectLine(String line) {
        if (groupObjectMatcher != null) groupObjectMatcher.reset();
        groupObjectMatcher = groupObjectPattern.matcher(line);
        return groupObjectMatcher.matches();
    }

    public boolean containsPart(String partName) {
        for (GroupObject groupObject : this.groupObjects) {
            if (partName.equalsIgnoreCase(groupObject.name)) return true;
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
                if (currentLine.startsWith("#") || currentLine.isEmpty()) continue;

                char c0 = currentLine.charAt(0);

                switch (c0) {
                    case 'v':
                        if (currentLine.length() > 1) {
                            char c1 = currentLine.charAt(1);
                            switch (c1) {
                                case ' ' -> {
                                    // vertex
                                    W_Vertex vertex = parseVertex(currentLine, lineCount);
                                    if (vertex != null) {
                                        checkMinMax(vertex);
                                        vertices.add(vertex);
                                    }
                                }
                                case 'n' -> {
                                    // vertex normal
                                    W_Vertex normal = parseVertexNormal(currentLine, lineCount);
                                    if (normal != null) vertexNormals.add(normal);
                                }
                                case 't' -> {
                                    // texture coordinate
                                    W_TextureCoordinate tex = parseTextureCoordinate(currentLine, lineCount);
                                    if (tex != null) textureCoordinates.add(tex);
                                }
                            }
                        }
                        break;

                    case 'f':
                        if (currentGroupObject == null) currentGroupObject = new GroupObject("Default");
                        W_Face face = parseFace(currentLine, lineCount);
                        currentGroupObject.faces.add(face);
                        break;

                    case 'g':
                        // group
                        if (currentLine.length() > 2 && currentLine.charAt(2) == '$') {
                            GroupObject group = parseGroupObject(currentLine, lineCount);
                            if (group != null && currentGroupObject != null) groupObjects.add(currentGroupObject);
                            currentGroupObject = group;
                        }
                        break;

                    case 'o':
                        if (currentLine.length() > 2 && currentLine.charAt(2) == '$') {
                            GroupObject group2 = parseGroupObject(currentLine, lineCount);
                            if (group2 != null && currentGroupObject != null) groupObjects.add(currentGroupObject);
                            currentGroupObject = group2;
                        }
                        break;

                    default:
                        // ignore unknown lines
                        break;
                }
            }

            this.groupObjects.add(this.currentGroupObject);
        } catch (IOException var16) {
            throw new _ModelFormatException("IO Exception reading model format", var16);
        } finally {
            this.checkMinMaxFinal();

            try {
                reader.close();
            } catch (IOException var15) {
            }

            try {
                inputStream.close();
            } catch (IOException var14) {
            }
        }
    }



    public void renderAll() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        if (this.currentGroupObject != null) {
            builder.begin(this.currentGroupObject.glDrawingMode, DefaultVertexFormats.POSITION_TEX_NORMAL);
        } else {
            builder.begin(4, DefaultVertexFormats.POSITION_TEX_NORMAL);
        }
        tessellateAll(tessellator);
        tessellator.draw();
    }

    public void tessellateAll(Tessellator tessellator) {
        for (GroupObject groupObject : this.groupObjects)
            groupObject.render(tessellator);
    }

    public void renderOnly(String... groupNames) {
        for (GroupObject groupObject : this.groupObjects) {
            for (String groupName : groupNames) {
                if (groupName.equalsIgnoreCase(groupObject.name)) groupObject.render();
            }
        }
    }

    public void renderPart(String partName) {
        for (GroupObject groupObject : this.groupObjects) {
            if (partName.equalsIgnoreCase(groupObject.name)) groupObject.render();
        }
    }

    public void renderAllExcept(String... excludedGroupNames) {
        for (GroupObject groupObject : this.groupObjects) {
            boolean skipPart = false;
            for (String excludedGroupName : excludedGroupNames) {
                if (excludedGroupName.equalsIgnoreCase(groupObject.name)) skipPart = true;
            }
            if (!skipPart) groupObject.render();
        }
    }

    @Override
    public _IModelCustom toVBO() {
        return new ModelVBO(this);
    }

    private W_Vertex parseVertex(String line, int lineCount) throws _ModelFormatException {
        W_Vertex vertex = null;
        if (isValidVertexLine(line)) {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");
            try {
                if (tokens.length == 2) return new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
                if (tokens.length == 3)
                    return new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            } catch (NumberFormatException e) {
                throw new _ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), e);
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
        return vertex;
    }

    private W_Vertex parseVertexNormal(String line, int lineCount) throws _ModelFormatException {
        W_Vertex vertexNormal = null;
        if (isValidVertexNormalLine(line)) {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");
            try {
                if (tokens.length == 3)
                    return new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            } catch (NumberFormatException e) {
                throw new _ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), e);
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
        return vertexNormal;
    }

    private W_TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws _ModelFormatException {
        W_TextureCoordinate textureCoordinate = null;
        if (isValidTextureCoordinateLine(line)) {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");
            try {
                if (tokens.length == 2)
                    return new W_TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]));
                if (tokens.length == 3)
                    return new W_TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            } catch (NumberFormatException e) {
                throw new _ModelFormatException(String.format("Number formatting error at line %d", new Object[]{Integer.valueOf(lineCount)}), e);
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
        return textureCoordinate;
    }

    private W_Face parseFace(String line, int lineCount) throws _ModelFormatException {
        W_Face face = null;
        if (isValidFaceLine(line)) {
            face = new W_Face();
            String trimmedLine = line.substring(line.indexOf(" ") + 1);
            String[] tokens = trimmedLine.split(" ");
            String[] subTokens = null;
            if (tokens.length == 3) {
                if (this.currentGroupObject.glDrawingMode == -1) {
                    this.currentGroupObject.glDrawingMode = 4;
                } else if (this.currentGroupObject.glDrawingMode != 4) {
                    throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Invalid number of points for face (expected 4, found " + tokens.length + ")");
                }
            } else if (tokens.length == 4) {
                if (this.currentGroupObject.glDrawingMode == -1) {
                    this.currentGroupObject.glDrawingMode = 7;
                } else if (this.currentGroupObject.glDrawingMode != 7) {
                    throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Invalid number of points for face (expected 3, found " + tokens.length + ")");
                }
            }
            if (isValidFace_V_VT_VN_Line(line)) {
                face.vertices = new W_Vertex[tokens.length];
                face.textureCoordinates = new W_TextureCoordinate[tokens.length];
                face.vertexNormals = new W_Vertex[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    subTokens = tokens[i].split("/");
                    face.vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                    face.vertexNormals[i] = this.vertexNormals.get(Integer.parseInt(subTokens[2]) - 1);
                }
                face.faceNormal = face.calculateFaceNormal();
            } else if (isValidFace_V_VT_Line(line)) {
                face.vertices = new W_Vertex[tokens.length];
                face.textureCoordinates = new W_TextureCoordinate[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    subTokens = tokens[i].split("/");
                    face.vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                }
                face.faceNormal = face.calculateFaceNormal();
            } else if (isValidFace_V_VN_Line(line)) {
                face.vertices = new W_Vertex[tokens.length];
                face.vertexNormals = new W_Vertex[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    subTokens = tokens[i].split("//");
                    face.vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.vertexNormals[i] = this.vertexNormals.get(Integer.parseInt(subTokens[1]) - 1);
                }
                face.faceNormal = face.calculateFaceNormal();
            } else if (isValidFace_V_Line(line)) {
                face.vertices = new W_Vertex[tokens.length];
                for (int i = 0; i < tokens.length; i++)
                    face.vertices[i] = this.vertices.get(Integer.parseInt(tokens[i]) - 1);
                face.faceNormal = face.calculateFaceNormal();
            } else {
                throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
        return face;
    }

    private GroupObject parseGroupObject(String line, int lineCount) throws _ModelFormatException {
        GroupObject group = null;
        if (isValidGroupObjectLine(line)) {
            String trimmedLine = line.substring(line.indexOf(" ") + 1);
            if (trimmedLine.length() > 0) group = new GroupObject(trimmedLine);
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
        return group;
    }

    public String getType() {
        return "obj";
    }

    public void renderAllLine(int startLine, int maxLine) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(1, DefaultVertexFormats.POSITION);
        renderAllLine(tessellator, startLine, maxLine);
        tessellator.draw();
    }

    public void renderAllLine(Tessellator tessellator, int startLine, int maxLine) {
        int lineCnt = 0;
        BufferBuilder builder = tessellator.getBuffer();
        for (GroupObject groupObject : this.groupObjects) {
            if (groupObject.faces.size() > 0) for (W_Face face : groupObject.faces) {
                for (int i = 0; i < face.vertices.length / 3; i++) {
                    W_Vertex v1 = face.vertices[i * 3 + 0];
                    W_Vertex v2 = face.vertices[i * 3 + 1];
                    W_Vertex v3 = face.vertices[i * 3 + 2];
                    lineCnt++;
                    if (lineCnt > maxLine) return;
                    builder.pos(v1.x, v1.y, v1.z).endVertex();
                    builder.pos(v2.x, v2.y, v2.z).endVertex();
                    lineCnt++;
                    if (lineCnt > maxLine) return;
                    builder.pos(v2.x, v2.y, v2.z).endVertex();
                    builder.pos(v3.x, v3.y, v3.z).endVertex();
                    lineCnt++;
                    if (lineCnt > maxLine) return;
                    builder.pos(v3.x, v3.y, v3.z).endVertex();
                    builder.pos(v1.x, v1.y, v1.z).endVertex();
                }
            }
        }
    }

    public int getVertexNum() {
        return this.vertices.size();
    }

    public int getFaceNum() {
        return getVertexNum() / 3;
    }

    public void renderAll(int startFace, int maxFace) {
        if (startFace < 0) startFace = 0;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(4, DefaultVertexFormats.POSITION_TEX_NORMAL);
        renderAll(tessellator, startFace, maxFace);
        tessellator.draw();
    }

    public void renderAll(Tessellator tessellator, int startFace, int maxLine) {
        int faceCnt = 0;
        for (GroupObject groupObject : this.groupObjects) {
            if (groupObject.faces.size() > 0) for (W_Face face : groupObject.faces) {
                faceCnt++;
                if (faceCnt >= startFace) {
                    if (faceCnt > maxLine) return;
                    face.addFaceForRender(tessellator);
                }
            }
        }
    }
}
