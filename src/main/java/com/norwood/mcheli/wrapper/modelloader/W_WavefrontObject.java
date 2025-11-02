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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class W_WavefrontObject extends W_ModelCustom {
    private static final Pattern vertexPattern = Pattern.compile("(v( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(v( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
    private static final Pattern vertexNormalPattern = Pattern.compile("(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
    private static final Pattern textureCoordinatePattern = Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *$)");
    private static final Pattern face_V_VT_VN_Pattern = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
    private static final Pattern face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
    private static final Pattern face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
    private static final Pattern face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");
    private static final Pattern groupObjectPattern = Pattern.compile("([go]( [-\\$\\w\\d]+) *\\n)|([go]( [-\\$\\w\\d]+) *$)");
    private static Matcher vertexMatcher;
    private static Matcher vertexNormalMatcher;
    private static Matcher textureCoordinateMatcher;
    private static Matcher face_V_VT_VN_Matcher;
    private static Matcher face_V_VT_Matcher;
    private static Matcher face_V_VN_Matcher;
    private static Matcher face_V_Matcher;
    private static Matcher groupObjectMatcher;
    public final ArrayList<W_Vertex> vertices = new ArrayList<>();
    public final ArrayList<W_Vertex> vertexNormals = new ArrayList<>();
    public final ArrayList<W_TextureCoordinate> textureCoordinates = new ArrayList<>();
    public final ArrayList<W_GroupObject> groupObjects = new ArrayList<>();
    private final String fileName;
    private W_GroupObject currentGroupObject;

    public W_WavefrontObject(ResourceLocation location, IResource resource) throws _ModelFormatException {
        this.fileName = resource.toString();
        this.loadObjModel(resource.getInputStream());
    }

    public W_WavefrontObject(ResourceLocation resource) throws _ModelFormatException {
        this.fileName = resource.toString();

        try {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            this.loadObjModel(res.getInputStream());
        } catch (IOException var3) {
            throw new _ModelFormatException("IO Exception reading model format", var3);
        }
    }

    public W_WavefrontObject(String fileName, URL resource) throws _ModelFormatException {
        this.fileName = fileName;

        try {
            this.loadObjModel(resource.openStream());
        } catch (IOException var4) {
            throw new _ModelFormatException("IO Exception reading model format", var4);
        }
    }

    public W_WavefrontObject(String filename, InputStream inputStream) throws _ModelFormatException {
        this.fileName = filename;
        this.loadObjModel(inputStream);
    }

    private static boolean isValidVertexLine(String line) {
        if (vertexMatcher != null) {
            vertexMatcher.reset();
        }

        vertexMatcher = vertexPattern.matcher(line);
        return vertexMatcher.matches();
    }

    private static boolean isValidVertexNormalLine(String line) {
        if (vertexNormalMatcher != null) {
            vertexNormalMatcher.reset();
        }

        vertexNormalMatcher = vertexNormalPattern.matcher(line);
        return vertexNormalMatcher.matches();
    }

    private static boolean isValidTextureCoordinateLine(String line) {
        if (textureCoordinateMatcher != null) {
            textureCoordinateMatcher.reset();
        }

        textureCoordinateMatcher = textureCoordinatePattern.matcher(line);
        return textureCoordinateMatcher.matches();
    }

    private static boolean isValidFace_V_VT_VN_Line(String line) {
        if (face_V_VT_VN_Matcher != null) {
            face_V_VT_VN_Matcher.reset();
        }

        face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line);
        return face_V_VT_VN_Matcher.matches();
    }

    private static boolean isValidFace_V_VT_Line(String line) {
        if (face_V_VT_Matcher != null) {
            face_V_VT_Matcher.reset();
        }

        face_V_VT_Matcher = face_V_VT_Pattern.matcher(line);
        return face_V_VT_Matcher.matches();
    }

    private static boolean isValidFace_V_VN_Line(String line) {
        if (face_V_VN_Matcher != null) {
            face_V_VN_Matcher.reset();
        }

        face_V_VN_Matcher = face_V_VN_Pattern.matcher(line);
        return face_V_VN_Matcher.matches();
    }

    private static boolean isValidFace_V_Line(String line) {
        if (face_V_Matcher != null) {
            face_V_Matcher.reset();
        }

        face_V_Matcher = face_V_Pattern.matcher(line);
        return face_V_Matcher.matches();
    }

    private static boolean isValidFaceLine(String line) {
        return isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line);
    }

    private static boolean isValidGroupObjectLine(String line) {
        if (groupObjectMatcher != null) {
            groupObjectMatcher.reset();
        }

        groupObjectMatcher = groupObjectPattern.matcher(line);
        return groupObjectMatcher.matches();
    }

    public ModelVBO asVBO() {
        return new ModelVBO(this);
    }

    @Override
    public boolean containsPart(String partName) {
        for (W_GroupObject groupObject : this.groupObjects) {
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
                        if (currentGroupObject == null) currentGroupObject = new W_GroupObject("Default");
                        W_Face face = parseFace(currentLine, lineCount);
                        currentGroupObject.faces.add(face);
                        break;

                    case 'g':
                        // group
                        if (currentLine.length() > 2 && currentLine.charAt(2) == '$') {
                            W_GroupObject group = parseGroupObject(currentLine, lineCount);
                            if (group != null && currentGroupObject != null) groupObjects.add(currentGroupObject);
                            currentGroupObject = group;
                        }
                        break;

                    case 'o':
                        if (currentLine.length() > 2 && currentLine.charAt(2) == '$') {
                            W_GroupObject group2 = parseGroupObject(currentLine, lineCount);
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


    @Override
    public void renderAll() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        if (this.currentGroupObject != null) {
            builder.begin(this.currentGroupObject.glDrawingMode, DefaultVertexFormats.POSITION_TEX_NORMAL);
        } else {
            builder.begin(4, DefaultVertexFormats.POSITION_TEX_NORMAL);
        }

        this.tessellateAll(tessellator);
        tessellator.draw();
    }

    public void tessellateAll(Tessellator tessellator) {
        for (W_GroupObject groupObject : this.groupObjects) {
            groupObject.render(tessellator);
        }
    }

    @Override
    public void renderOnly(String... groupNames) {
        for (W_GroupObject groupObject : this.groupObjects) {
            for (String groupName : groupNames) {
                if (groupName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render();
                }
            }
        }
    }


    @Override
    public void renderPart(String partName) {
        for (W_GroupObject groupObject : this.groupObjects) {
            if (partName.equalsIgnoreCase(groupObject.name)) {
                groupObject.render();
            }
        }
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        for (W_GroupObject groupObject : this.groupObjects) {
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


    private W_Vertex parseVertex(String line, int lineCount) throws _ModelFormatException {
        W_Vertex vertex = null;
        if (isValidVertexLine(line)) {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");

            try {
                if (tokens.length == 2) {
                    return new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
                } else {
                    return tokens.length == 3 ? new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])) : vertex;
                }
            } catch (NumberFormatException var6) {
                throw new _ModelFormatException(String.format("Number formatting error at line %d", lineCount), var6);
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
    }

    private W_Vertex parseVertexNormal(String line, int lineCount) throws _ModelFormatException {
        W_Vertex vertexNormal = null;
        if (isValidVertexNormalLine(line)) {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");

            try {
                return tokens.length == 3 ? new W_Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])) : vertexNormal;
            } catch (NumberFormatException var6) {
                throw new _ModelFormatException(String.format("Number formatting error at line %d", lineCount), var6);
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
    }

    private W_TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws _ModelFormatException {
        W_TextureCoordinate textureCoordinate = null;
        if (isValidTextureCoordinateLine(line)) {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");

            try {
                if (tokens.length == 2) {
                    return new W_TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]));
                } else {
                    return tokens.length == 3
                            ? new W_TextureCoordinate(Float.parseFloat(tokens[0]), 1.0F - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]))
                            : textureCoordinate;
                }
            } catch (NumberFormatException var6) {
                throw new _ModelFormatException(String.format("Number formatting error at line %d", lineCount), var6);
            }
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
    }

    private W_Face parseFace(String line, int lineCount) throws _ModelFormatException {
        W_Face face;
        if (isValidFaceLine(line)) {
            face = new W_Face();
            String trimmedLine = line.substring(line.indexOf(" ") + 1);
            String[] tokens = trimmedLine.split(" ");
            String[] subTokens;
            if (tokens.length == 3) {
                if (this.currentGroupObject.glDrawingMode == -1) {
                    this.currentGroupObject.glDrawingMode = 4;
                } else if (this.currentGroupObject.glDrawingMode != 4) {
                    throw new _ModelFormatException(
                            "Error parsing entry ('"
                                    + line
                                    + "', line "
                                    + lineCount
                                    + ") in file '"
                                    + this.fileName
                                    + "' - Invalid number of points for face (expected 4, found "
                                    + tokens.length
                                    + ")"
                    );
                }
            } else if (tokens.length == 4) {
                if (this.currentGroupObject.glDrawingMode == -1) {
                    this.currentGroupObject.glDrawingMode = 7;
                } else if (this.currentGroupObject.glDrawingMode != 7) {
                    throw new _ModelFormatException(
                            "Error parsing entry ('"
                                    + line
                                    + "', line "
                                    + lineCount
                                    + ") in file '"
                                    + this.fileName
                                    + "' - Invalid number of points for face (expected 3, found "
                                    + tokens.length
                                    + ")"
                    );
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
            } else {
                if (!isValidFace_V_Line(line)) {
                    throw new _ModelFormatException(
                            "Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format"
                    );
                }

                face.vertices = new W_Vertex[tokens.length];

                for (int i = 0; i < tokens.length; i++) {
                    face.vertices[i] = this.vertices.get(Integer.parseInt(tokens[i]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();
            }

            return face;
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
    }

    private W_GroupObject parseGroupObject(String line, int lineCount) throws _ModelFormatException {
        W_GroupObject group = null;
        if (isValidGroupObjectLine(line)) {
            String trimmedLine = line.substring(line.indexOf(" ") + 1);
            if (!trimmedLine.isEmpty()) {
                group = new W_GroupObject(trimmedLine);
            }

            return group;
        } else {
            throw new _ModelFormatException("Error parsing entry ('" + line + "', line " + lineCount + ") in file '" + this.fileName + "' - Incorrect format");
        }
    }

    @Override
    public String getType() {
        return "obj";
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

        for (W_GroupObject groupObject : this.groupObjects) {
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
        return this.vertices.size();
    }

    @Override
    public int getFaceNum() {
        return this.getVertexNum() / 3;
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

        for (W_GroupObject groupObject : this.groupObjects) {
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
