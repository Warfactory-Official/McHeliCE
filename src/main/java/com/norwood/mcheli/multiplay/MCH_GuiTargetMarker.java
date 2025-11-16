package com.norwood.mcheli.multiplay;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MarkEntityPos;
import com.norwood.mcheli.MCH_ServerSettings;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.helper.entity.ITargetMarkerObject;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

@SideOnly(Side.CLIENT)
public class MCH_GuiTargetMarker extends MCH_Gui {

    private static final FloatBuffer matModel = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer matProjection = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer matViewport = BufferUtils.createIntBuffer(16);
    private static final ArrayList<MCH_MarkEntityPos> entityPos = new ArrayList<>();
    private static final HashMap<Integer, Integer> spotedEntity = new HashMap<>();
    private static Minecraft s_minecraft;
    private static int spotedEntityCountdown = 0;

    public MCH_GuiTargetMarker(Minecraft minecraft) {
        super(minecraft);
        s_minecraft = minecraft;
    }

    public static void onClientTick() {
        if (!Minecraft.getMinecraft().isGamePaused()) {
            spotedEntityCountdown++;
        }

        if (spotedEntityCountdown >= 20) {
            spotedEntityCountdown = 0;

            for (Integer key : spotedEntity.keySet()) {
                int count = spotedEntity.get(key);
                if (count > 0) {
                    spotedEntity.put(key, count - 1);
                }
            }

            spotedEntity.values().removeIf(integer -> integer <= 0);
        }
    }

    public static boolean isSpotedEntity(@Nullable Entity entity) {
        if (entity != null) {
            int entityId = entity.getEntityId();

            for (int key : spotedEntity.keySet()) {
                if (key == entityId) {
                    return true;
                }
            }

        }
        return false;
    }

    public static void addSpotedEntity(int entityId, int count) {
        if (spotedEntity.containsKey(entityId)) {
            int now = spotedEntity.get(entityId);
            if (count > now) {
                spotedEntity.put(entityId, count);
            }
        } else {
            spotedEntity.put(entityId, count);
        }
    }

    public static void addMarkEntityPos(int reserve, ITargetMarkerObject target, double x, double y, double z) {
        addMarkEntityPos(reserve, target, x, y, z, false);
    }

    public static void addMarkEntityPos(int reserve, ITargetMarkerObject target, double x, double y, double z,
                                        boolean nazo) {
        if (isEnableEntityMarker()) {
            MCH_TargetType spotType = MCH_TargetType.NONE;
            EntityPlayer clientPlayer = s_minecraft.player;
            Entity entity = target.getEntity();
            if (entity instanceof MCH_EntityAircraft ac) {
                if (ac.isMountedEntity(clientPlayer)) {
                    return;
                }

                if (ac.isMountedSameTeamEntity(clientPlayer)) {
                    spotType = MCH_TargetType.SAME_TEAM_PLAYER;
                }
            } else if (entity instanceof EntityPlayer) {
                if (entity == clientPlayer || entity.getRidingEntity() instanceof MCH_EntitySeat ||
                        entity.getRidingEntity() instanceof MCH_EntityAircraft) {
                    return;
                }

                clientPlayer.getTeam();
                if (clientPlayer.isOnSameTeam(entity)) {
                    spotType = MCH_TargetType.SAME_TEAM_PLAYER;
                }
            }

            if (spotType == MCH_TargetType.NONE && isSpotedEntity(entity)) {
                spotType = MCH_Multiplay.canSpotEntity(
                        clientPlayer, clientPlayer.posX, clientPlayer.posY + clientPlayer.getEyeHeight(),
                        clientPlayer.posZ, entity, false);
            }

            if (reserve == 100) {
                spotType = MCH_TargetType.POINT;
            }

            if (spotType != MCH_TargetType.NONE) {
                MCH_MarkEntityPos e = new MCH_MarkEntityPos(spotType.ordinal(), target);
                GL11.glGetFloat(2982, matModel);
                GL11.glGetFloat(2983, matProjection);
                GL11.glGetInteger(2978, matViewport);
                if (nazo) {
                    GLU.gluProject((float) z, (float) y, (float) x, matModel, matProjection, matViewport, e.pos);
                    float yy = e.pos.get(1);
                    GLU.gluProject((float) x, (float) y, (float) z, matModel, matProjection, matViewport, e.pos);
                    e.pos.put(1, yy);
                } else {
                    GLU.gluProject((float) x, (float) y, (float) z, matModel, matProjection, matViewport, e.pos);
                }

                entityPos.add(e);
            }
        }
    }

    public static void clearMarkEntityPos() {
        entityPos.clear();
    }

    public static boolean isEnableEntityMarker() {
        return MCH_Config.DisplayEntityMarker.prmBool &&
                (Minecraft.getMinecraft().isSingleplayer() || MCH_ServerSettings.enableEntityMarker) &&
                MCH_Config.EntityMarkerSize.prmDouble > 0.0;
    }

    public static void drawRhombus(BufferBuilder builder, int dir, double x, double y, double z, double size,
                                   int color) {
        size *= 2.0;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color >> 0 & 0xFF;
        int alpha = color >> 24 & 0xFF;
        double M = size / 3.0;
        if ((dir & 1) != 0) {
            builder.pos(x - size, y, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x - size + M, y - M, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x - size, y, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x - size + M, y + M, z).color(red, green, blue, alpha).endVertex();
        }

        if ((dir & 4) != 0) {
            builder.pos(x + size, y, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x + size - M, y - M, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x + size, y, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x + size - M, y + M, z).color(red, green, blue, alpha).endVertex();
        }

        if ((dir & 8) != 0) {
            builder.pos(x, y - size, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x + M, y - size + M, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x, y - size, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x - M, y - size + M, z).color(red, green, blue, alpha).endVertex();
        }

        if ((dir & 2) != 0) {
            builder.pos(x, y + size, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x + M, y + size - M, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x, y + size, z).color(red, green, blue, alpha).endVertex();
            builder.pos(x - M, y + size - M, z).color(red, green, blue, alpha).endVertex();
        }
    }

    public static void markPoint(int px, int py, int pz) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null && player.world != null) {
            if (py < 1000) {
                MCH_ParticlesUtil.spawnMarkPoint(player, 0.5 + px, 1.0 + py, 0.5 + pz);
            } else {
                MCH_ParticlesUtil.clearMarkPoint();
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return player != null && player.world != null;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        GL11.glLineWidth(scaleFactor * 2);
        if (this.isDrawGui(player)) {
            GlStateManager.disableBlend();
            if (isEnableEntityMarker()) {
                this.drawMark();
            }
        }
    }

    void drawMark() {
        int[] COLOR_TABLE = new int[] { 0, -808464433, -805371904, -805306624, -822018049, -805351649, -65536, 0 };
        int scale = scaleFactor > 0 ? scaleFactor : 2;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color((byte) -1, (byte) -1, (byte) -1, (byte) -1);
        GlStateManager.depthMask(false);
        int DW = this.mc.displayWidth;
        int DSW = this.mc.displayWidth / scale;
        int DSH = this.mc.displayHeight / scale;
        double x;
        double z;
        double y;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                builder.begin(4, DefaultVertexFormats.POSITION_COLOR);
            }

            for (MCH_MarkEntityPos e : entityPos) {
                int color = COLOR_TABLE[e.type];
                x = e.pos.get(0) / scale;
                z = e.pos.get(2);
                y = e.pos.get(1) / scale;
                if (z < 1.0) {
                    y = DSH - y;
                } else if (x < (double) DW / 2) {
                    x = 10000.0;
                } else if (x >= (double) DW / 2) {
                    x = -10000.0;
                }

                if (i == 0) {
                    double size = MCH_Config.EntityMarkerSize.prmDouble;
                    if (e.type < MCH_TargetType.POINT.ordinal() && z < 1.0 && x >= 0.0 && x <= DSW && y >= 0.0 &&
                            y <= DSH) {
                        this.drawTriangle1(builder, x, y, size, color);
                    }
                } else if (e.type == MCH_TargetType.POINT.ordinal() && e.getTarget() != null) {
                    ITargetMarkerObject target = e.getTarget();
                    double MARK_SIZE = MCH_Config.BlockMarkerSize.prmDouble;
                    if (z < 1.0 && x >= 0.0 && x <= DSW - 20 && y >= 0.0 && y <= DSH - 40) {
                        double dist = this.mc.player.getDistance(target.getX(), target.getY(), target.getZ());
                        GlStateManager.enableTexture2D();
                        this.drawCenteredString(String.format("%.0fm", dist), (int) x,
                                (int) (y + MARK_SIZE * 1.1 + 16.0), color);
                        if (x >= (double) DSW / 2 - 20 && x <= (double) DSW / 2 + 20 && y >= (double) DSH / 2 - 20 &&
                                y <= (double) DSH / 2 + 20) {
                            this.drawString(String.format("x : %.0f", target.getX()), (int) (x + MARK_SIZE + 18.0),
                                    (int) y - 12, color);
                            this.drawString(String.format("y : %.0f", target.getY()), (int) (x + MARK_SIZE + 18.0),
                                    (int) y - 4, color);
                            this.drawString(String.format("z : %.0f", target.getZ()), (int) (x + MARK_SIZE + 18.0),
                                    (int) y + 4, color);
                        }

                        GlStateManager.disableTexture2D();
                        builder.begin(1, DefaultVertexFormats.POSITION_COLOR);
                        drawRhombus(builder, 15, x, y, this.zLevel, MARK_SIZE, color);
                    } else {
                        builder.begin(1, DefaultVertexFormats.POSITION_COLOR);
                        double S = 30.0;
                        if (x < S) {
                            drawRhombus(builder, 1, S, (double) DSH / 2, this.zLevel, MARK_SIZE, color);
                        } else if (x > DSW - S) {
                            drawRhombus(builder, 4, DSW - S, (double) DSH / 2, this.zLevel, MARK_SIZE, color);
                        }

                        if (y < S) {
                            drawRhombus(builder, 8, (double) DSW / 2, S, this.zLevel, MARK_SIZE, color);
                        } else if (y > DSH - S * 2.0) {
                            drawRhombus(builder, 2, (double) DSW / 2, DSH - S * 2.0, this.zLevel, MARK_SIZE, color);
                        }
                    }

                    tessellator.draw();
                }
            }

            if (i == 0) {
                tessellator.draw();
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public void drawTriangle1(BufferBuilder builder, double x, double y, double size, int color) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color >> 0 & 0xFF;
        int alpha = color >> 24 & 0xFF;
        builder.pos(x + size / 2.0, y - 10.0 - size, this.zLevel).color(red, green, blue, alpha).endVertex();
        builder.pos(x - size / 2.0, y - 10.0 - size, this.zLevel).color(red, green, blue, alpha).endVertex();
        builder.pos(x + 0.0, y - 10.0, this.zLevel).color(red, green, blue, alpha).endVertex();
    }
}
