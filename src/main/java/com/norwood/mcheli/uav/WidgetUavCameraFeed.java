package com.norwood.mcheli.uav;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.factories.UavStationGuiData.UavEntry;
import com.norwood.mcheli.networking.packet.PacketUavPreviewSelect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Live camera-feed viewport for the selected UAV. Self-contained widget (cf.
 * {@link com.norwood.mcheli.aircraft.WidgetAircraftViewport}): in {@link #draw} it renders the world
 * from the selected, client-loaded UAV's onboard camera into an off-screen framebuffer and blits the
 * result into the widget. UAVs that are unloaded on the client show "NO SIGNAL".
 *
 * <p>The world is rendered by temporarily swapping {@code renderViewEntity} to the shared
 * {@link MCH_ViewEntityDummy} positioned at the UAV; the GUI's 2D matrices, scissor and the main
 * framebuffer are saved and restored around the pass so the rest of the panel renders normally.
 */
public class WidgetUavCameraFeed extends Widget<WidgetUavCameraFeed> {

    /** One shared full-screen FBO — only one station screen is open at a time. */
    private static Framebuffer feedBuffer;
    private static int bufferWidth;
    private static int bufferHeight;

    private final Supplier<UavEntry> selected;
    private MCH_EntityAircraft cached;
    private UUID lastSentId;
    private int heartbeat;

    public WidgetUavCameraFeed(Supplier<UavEntry> selected) {
        this.selected = selected;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int w = (int) getArea().getWidth();
        int h = (int) getArea().getHeight();
        Gui.drawRect(0, 0, w, h, 0xFF101010);

        Minecraft mc = Minecraft.getMinecraft();
        UavEntry entry = this.selected.get();
        // Ask the server to stream the selected UAV's terrain to us (heartbeat; expires on close).
        sendPreviewHeartbeat(entry);
        MCH_EntityAircraft uav = resolve(mc, entry);

        if (uav == null || !OpenGlHelper.isFramebufferEnabled()) {
            drawStatus(w, h, entry);
            return;
        }

        try {
            renderFeed(mc, uav, w, h);
        } catch (Throwable t) {
            // A render hiccup must never take down the GUI.
            drawStatus(w, h, entry);
        }
    }

    private void renderFeed(Minecraft mc, MCH_EntityAircraft uav, int w, int h) {
        int dw = mc.displayWidth;
        int dh = mc.displayHeight;
        if (feedBuffer == null || bufferWidth != dw || bufferHeight != dh) {
            if (feedBuffer != null) {
                feedBuffer.deleteFramebuffer();
            }
            feedBuffer = new Framebuffer(dw, dh, true);
            feedBuffer.setFramebufferFilter(GL11.GL_LINEAR);
            bufferWidth = dw;
            bufferHeight = dh;
        }

        float partialTicks = mc.getRenderPartialTicks();

        // Aim the shared view dummy from the UAV's onboard camera position.
        MCH_ViewEntityDummy dummy = MCH_ViewEntityDummy.getInstance(mc.world);
        double camX = uav.posX;
        double camY = uav.posY + uav.getEyeHeight();
        double camZ = uav.posZ;
        float yaw = uav.rotationYaw;
        float pitch = uav.rotationPitch;
        dummy.setLocationAndAngles(camX, camY, camZ, yaw, pitch);
        dummy.prevPosX = dummy.lastTickPosX = camX;
        dummy.prevPosY = dummy.lastTickPosY = camY;
        dummy.prevPosZ = dummy.lastTickPosZ = camZ;
        dummy.prevRotationYaw = yaw;
        dummy.prevRotationPitch = pitch;
        dummy.rotationYawHead = dummy.prevRotationYawHead = yaw;

        Entity prevView = mc.getRenderViewEntity();
        boolean scissor = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
        if (scissor) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        // Preserve the GUI's 2D projection/modelview across the 3D world pass.
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            mc.setRenderViewEntity(dummy);
            feedBuffer.framebufferClear();
            feedBuffer.bindFramebuffer(true);
            mc.entityRenderer.renderWorld(partialTicks, 0L);
        } finally {
            mc.getFramebuffer().bindFramebuffer(true);
            mc.setRenderViewEntity(prevView);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            if (scissor) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
        }

        // Restore 2D GUI render state.
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Blit the feed texture into the widget area (FBO textures are bottom-up, so flip V).
        feedBuffer.bindFramebufferTexture();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bb.pos(0.0, h, 0.0).tex(0.0, 0.0).endVertex();
        bb.pos(w, h, 0.0).tex(1.0, 0.0).endVertex();
        bb.pos(w, 0.0, 0.0).tex(1.0, 1.0).endVertex();
        bb.pos(0.0, 0.0, 0.0).tex(0.0, 1.0).endVertex();
        tess.draw();
        feedBuffer.unbindFramebufferTexture();

        // Thin "REC"-style marker so it reads as a live feed.
        FontRenderer fr = mc.fontRenderer;
        fr.drawStringWithShadow("● LIVE", 3.0f, 3.0f, 0xFFFF4040);
    }

    /**
     * Tells the server which UAV we're previewing so {@link com.norwood.mcheli.wingman.handler.UavChunkStreamer}
     * streams its terrain to the client. Sent on selection change and as a periodic heartbeat; the
     * server expires the request shortly after the screen closes (heartbeats stop).
     */
    private void sendPreviewHeartbeat(UavEntry entry) {
        if (entry == null) {
            return;
        }
        this.heartbeat++;
        if (!entry.id().equals(this.lastSentId) || this.heartbeat % 20 == 0) {
            new PacketUavPreviewSelect(true, entry.id()).sendToServer();
            this.lastSentId = entry.id();
        }
    }

    /** Resolves the selected UAV among client-loaded entities (cached between frames). */
    private MCH_EntityAircraft resolve(Minecraft mc, UavEntry entry) {
        if (entry == null || !entry.loaded() || mc.world == null) {
            this.cached = null;
            return null;
        }
        UUID id = entry.id();
        if (this.cached != null && !this.cached.isDead && this.cached.getUniqueID().equals(id)) {
            return this.cached;
        }
        for (Entity e : mc.world.loadedEntityList) {
            if (e instanceof MCH_EntityAircraft ac && ac.getUniqueID().equals(id)) {
                this.cached = ac;
                return ac;
            }
        }
        this.cached = null;
        return null;
    }

    private void drawStatus(int w, int h, UavEntry entry) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String line;
        int color;
        if (entry == null) {
            line = "NO UAV SELECTED";
            color = 0xFFAAAAAA;
        } else if (!entry.loaded()) {
            line = "NO SIGNAL";
            color = 0xFFFF5555;
        } else {
            line = "ACQUIRING…";
            color = 0xFFFFAA00;
        }
        fr.drawStringWithShadow(line, (w - fr.getStringWidth(line)) / 2.0f, h / 2.0f - 4, color);
    }
}
