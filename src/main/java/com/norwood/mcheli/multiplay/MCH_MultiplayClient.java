package com.norwood.mcheli.multiplay;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.CoreModManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_FileSearch;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_OStream;
import com.norwood.mcheli.networking.packet.PacketImgDataChunk;
import com.norwood.mcheli.networking.packet.PacketSendModlist;

public class MCH_MultiplayClient {

    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;
    private static MCH_OStream dataOutputStream;
    private static List<String> modList = new ArrayList<>();
    private static int packetIndex;

    public static void startSendImageData() {
        Minecraft mc = Minecraft.getMinecraft();
        sendScreenShot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    public static void sendScreenShot(int displayWidth, int displayHeight, Framebuffer framebufferMc) {
        try {
            if (OpenGlHelper.isFramebufferEnabled()) {
                displayWidth = framebufferMc.framebufferTextureWidth;
                displayHeight = framebufferMc.framebufferTextureHeight;
            }

            int k = displayWidth * displayHeight;
            if (pixelBuffer == null || pixelBuffer.capacity() < k) {
                pixelBuffer = BufferUtils.createIntBuffer(k);
                pixelValues = new int[k];
            }

            GL11.glPixelStorei(3333, 1);
            GL11.glPixelStorei(3317, 1);
            pixelBuffer.clear();
            if (OpenGlHelper.isFramebufferEnabled()) {
                GL11.glBindTexture(3553, framebufferMc.framebufferTexture);
                GL11.glGetTexImage(3553, 0, 32993, 33639, pixelBuffer);
            } else {
                GL11.glReadPixels(0, 0, displayWidth, displayHeight, 32993, 33639, pixelBuffer);
            }

            pixelBuffer.get(pixelValues);
            TextureUtil.processPixelValues(pixelValues, displayWidth, displayHeight);
            BufferedImage bufferedimage;
            if (!OpenGlHelper.isFramebufferEnabled()) {
                bufferedimage = new BufferedImage(displayWidth, displayHeight, 1);
                bufferedimage.setRGB(0, 0, displayWidth, displayHeight, pixelValues, 0, displayWidth);
            } else {
                bufferedimage = new BufferedImage(framebufferMc.framebufferWidth, framebufferMc.framebufferHeight, 1);
                int l = framebufferMc.framebufferTextureHeight - framebufferMc.framebufferHeight;

                for (int i1 = l; i1 < framebufferMc.framebufferTextureHeight; i1++) {
                    for (int j1 = 0; j1 < framebufferMc.framebufferWidth; j1++) {
                        bufferedimage.setRGB(j1, i1 - l, pixelValues[i1 * framebufferMc.framebufferTextureWidth + j1]);
                    }
                }
            }

            dataOutputStream = new MCH_OStream();
            ImageIO.write(bufferedimage, "png", dataOutputStream);
        } catch (Exception var8) {}
    }

    public static void readImageData(DataOutputStream dos) {
        dataOutputStream.write(dos);
    }

    public static void sendImageData() {
        if (dataOutputStream != null) {
            byte[] buffer = dataOutputStream.toByteArray();
            int totalSize = dataOutputStream.size();
            int chunkSize = 128;

            int start = packetIndex * chunkSize;
            if (start < totalSize) {
                int length = Math.min(chunkSize, totalSize - start);

                PacketImgDataChunk packet = new PacketImgDataChunk();
                packet.imageDataIndex = start;
                packet.buf = Arrays.copyOfRange(buffer, start, start + length);

                packet.sendToServer();

                packetIndex++;
            }

            if (packetIndex * chunkSize >= totalSize) {
                dataOutputStream = null;
                packetIndex = 0;
            }
        }
    }

    public static double getPerData() {
        return (double) dataOutputStream.index / dataOutputStream.size();
    }

    public static void readModList(String playerName, String commandSenderName) {
        modList = new ArrayList<>();
        modList.add(TextFormatting.RED + "###### Name:" + commandSenderName + " ######");
        modList.add(TextFormatting.RED + "###### ID  :" + playerName + " ######");
        String[] classFileNameList = System.getProperty("java.class.path").split(File.pathSeparator);

        for (String classFileName : classFileNameList) {
            MCH_Lib.DbgLog(true, "java.class.path=" + classFileName);
            if (classFileName.length() > 1) {
                File javaClassFile = new File(classFileName);
                if (javaClassFile.getAbsolutePath().toLowerCase().contains("versions")) {
                    modList.add(TextFormatting.AQUA + "# Client class=" + javaClassFile.getName() + " : file size= " +
                            javaClassFile.length());
                }
            }
        }

        modList.add(TextFormatting.YELLOW + "=== ActiveModList ===");

        for (ModContainer mod : Loader.instance().getActiveModList()) {
            modList.add(mod + "  [" + mod.getModId() + "]  " + mod.getName() + "[" + mod.getDisplayVersion() + "]  " +
                    mod.getSource().getName());
        }

        if (!CoreModManager.getAccessTransformers().isEmpty()) {
            modList.add(TextFormatting.YELLOW + "=== AccessTransformers ===");

            modList.addAll(CoreModManager.getAccessTransformers());
        }

        if (!CoreModManager.getIgnoredMods().isEmpty()) {
            modList.add(TextFormatting.YELLOW + "=== LoadedCoremods ===");

            modList.addAll(CoreModManager.getIgnoredMods());
        }

        if (!CoreModManager.getReparseableCoremods().isEmpty()) {
            modList.add(TextFormatting.YELLOW + "=== ReparseableCoremods ===");

            modList.addAll(CoreModManager.getReparseableCoremods());
        }

        Minecraft mc = Minecraft.getMinecraft();
        MCH_FileSearch search = new MCH_FileSearch();
        File[] files = search.listFiles(new File(mc.gameDir, "mods").getAbsolutePath(), "*.jar");
        modList.add(TextFormatting.YELLOW + "=== Manifest ===");

        for (File file : files) {
            try {
                String jarPath = file.getCanonicalPath();
                JarFile jarFile = new JarFile(jarPath);
                Enumeration<JarEntry> jarEntries = jarFile.entries();
                StringBuilder manifest = new StringBuilder();

                while (jarEntries.hasMoreElements()) {
                    ZipEntry zipEntry = jarEntries.nextElement();
                    if (zipEntry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF") && !zipEntry.isDirectory()) {
                        InputStream is = jarFile.getInputStream(zipEntry);
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));

                        for (String line = br.readLine(); line != null; line = br.readLine()) {
                            line = line.replace(" ", "").trim();
                            if (!line.isEmpty()) {
                                manifest.append(" [").append(line).append("]");
                            }
                        }

                        is.close();
                        break;
                    }
                }

                jarFile.close();
                if (manifest.length() > 0) {
                    modList.add(file.getName() + manifest);
                }
            } catch (Exception var20) {
                modList.add(file.getName() + " : Read Manifest failed.");
            }
        }

        search = new MCH_FileSearch();
        files = search.listFiles(new File(mc.gameDir, "mods").getAbsolutePath(), "*.litemod");
        modList.add(TextFormatting.LIGHT_PURPLE + "=== LiteLoader ===");

        for (File file : files) {
            try {
                String jarPath = file.getCanonicalPath();
                JarFile jarFile = new JarFile(jarPath);
                Enumeration<JarEntry> jarEntries = jarFile.entries();
                StringBuilder litemod_json = new StringBuilder();

                while (jarEntries.hasMoreElements()) {
                    ZipEntry zipEntry = jarEntries.nextElement();
                    String fname = zipEntry.getName().toLowerCase();
                    if (!zipEntry.isDirectory()) {
                        if (fname.equals("litemod.json")) {
                            InputStream is = jarFile.getInputStream(zipEntry);
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));

                            for (String linex = br.readLine(); linex != null; linex = br.readLine()) {
                                linex = linex.replace(" ", "").trim();
                                if (linex.toLowerCase().contains("name")) {
                                    litemod_json.append(" [").append(linex).append("]");
                                    break;
                                }
                            }

                            is.close();
                        } else {
                            int index = fname.lastIndexOf("/");
                            if (index >= 0) {
                                fname = fname.substring(index + 1);
                            }

                            if (fname.contains("litemod") && fname.endsWith("class")) {
                                fname = zipEntry.getName();
                                if (index >= 0) {
                                    fname = fname.substring(index + 1);
                                }

                                litemod_json.append(" [").append(fname).append("]");
                            }
                        }
                    }
                }

                jarFile.close();
                if (litemod_json.length() > 0) {
                    modList.add(file.getName() + litemod_json);
                }
            } catch (Exception var19) {
                modList.add(file.getName() + " : Read LiteLoader failed.");
            }
        }
    }

    public static void sendModsInfo(String playerName, String commandSenderName, int id) {
        if (MCH_Config.DebugLog) {
            modList.clear();
            readModList(playerName, commandSenderName);
        }

        PacketSendModlist.send(modList, id);
    }
}
