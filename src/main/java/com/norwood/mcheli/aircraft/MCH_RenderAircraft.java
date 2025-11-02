package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_ClientCommonTickHandler;
import com.norwood.mcheli.MCH_ClientEventHook;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.helper.MCH_ColorInt;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.client.renderer.MCH_Verts;
import com.norwood.mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.weapon.MCH_WeaponGuidanceSystem;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_EntityRenderer;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_Render;
import com.norwood.mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.Iterator;

public abstract class MCH_RenderAircraft<T extends MCH_EntityAircraft> extends W_Render<T> {
    public static boolean renderingEntity = false;
    public static _IModelCustom debugModel = null;

    protected MCH_RenderAircraft(RenderManager renderManager) {
        super(renderManager);
    }

    public static boolean shouldSkipRender(Entity entity) {
        if (entity instanceof MCH_IEntityCanRideAircraft e) {
            if (e.isSkipNormalRender()) {
                return !renderingEntity;
            }
        } else if ((entity.getClass().toString().indexOf("flansmod.common.driveables.EntityPlane") > 0 || entity.getClass().toString().indexOf("flansmod.common.driveables.EntityVehicle") > 0) && entity.getRidingEntity() instanceof MCH_EntitySeat) {
            return !renderingEntity;
        }

        return false;
    }

    public static void renderLight(double x, double y, double z, float tickTime, MCH_EntityAircraft ac, MCH_AircraftInfo info) {
        if (ac.haveSearchLight()) {
            if (ac.isSearchLightON()) {
                Entity entity = ac.getEntityBySeatId(1);
                if (entity != null) {
                    ac.lastSearchLightYaw = entity.rotationYaw;
                    ac.lastSearchLightPitch = entity.rotationPitch;
                } else {
                    entity = ac.getEntityBySeatId(0);
                    if (entity != null) {
                        ac.lastSearchLightYaw = entity.rotationYaw;
                        ac.lastSearchLightPitch = entity.rotationPitch;
                    }
                }

                float yaw = ac.lastSearchLightYaw;
                float pitch = ac.lastSearchLightPitch;
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableTexture2D();
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GlStateManager.disableAlpha();
                GlStateManager.disableCull();
                GlStateManager.depthMask(false);
                float rot = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;

                for (MCH_AircraftInfo.SearchLight sl : info.searchLights) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(sl.pos.x, sl.pos.y, sl.pos.z);
                    if (!sl.fixDir) {
                        GlStateManager.rotate(yaw - ac.getRotYaw() + sl.yaw, 0.0F, -1.0F, 0.0F);
                        GlStateManager.rotate(pitch + 90.0F - ac.getRotPitch() + sl.pitch, 1.0F, 0.0F, 0.0F);
                    } else {
                        float stRot = 0.0F;
                        if (sl.steering) {
                            stRot = -rot * sl.stRot;
                        }

                        GlStateManager.rotate(0.0F + sl.yaw + stRot, 0.0F, -1.0F, 0.0F);
                        GlStateManager.rotate(90.0F + sl.pitch, 1.0F, 0.0F, 0.0F);
                    }

                    float height = sl.height;
                    float width = sl.width / 2.0F;
                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder builder = tessellator.getBuffer();
                    builder.begin(6, DefaultVertexFormats.POSITION_COLOR);
                    MCH_ColorInt cs = new MCH_ColorInt(sl.colorStart);
                    MCH_ColorInt ce = new MCH_ColorInt(sl.colorEnd);
                    builder.pos(0.0, 0.0, 0.0).color(cs.r, cs.g, cs.b, cs.a).endVertex();

                    for (int i = 0; i < 25; i++) {
                        float angle = (float) (15.0 * i / 180.0 * Math.PI);
                        builder.pos(MathHelper.sin(angle) * width, height, MathHelper.cos(angle) * width).color(ce.r, ce.g, ce.b, ce.a).endVertex();
                    }

                    tessellator.draw();
                    GlStateManager.popMatrix();
                }

                GlStateManager.depthMask(true);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                RenderHelper.enableStandardItemLighting();
            }
        }
    }

    public static void renderBody(@Nullable _IModelCustom model) {
        if (model != null) {
            if (model instanceof W_ModelCustom) {
                if (((W_ModelCustom) model).containsPart("$body")) {
                    model.renderPart("$body");
                } else {
                    model.renderAll();
                }
            } else {
                model.renderAll();
            }
        }
    }

    public static void renderPart(@Nullable _IModelCustom model, @Nullable _IModelCustom modelBody, String partName) {
        if (model != null) {
            model.renderAll();
        } else if (modelBody instanceof W_ModelCustom && ((W_ModelCustom) modelBody).containsPart("$" + partName)) {
            modelBody.renderPart("$" + partName);
        }
    }

    public static void renderLightHatch(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (!info.lightHatchList.isEmpty()) {
            float rot = ac.prevRotLightHatch + (ac.rotLightHatch - ac.prevRotLightHatch) * tickTime;

            for (MCH_AircraftInfo.Hatch t : info.lightHatchList) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(t.pos.x, t.pos.y, t.pos.z);
                GL11.glRotated(rot * t.maxRot, t.rot.x, t.rot.y, t.rot.z);
                GlStateManager.translate(-t.pos.x, -t.pos.y, -t.pos.z);
                renderPart(t.model, info.model, t.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderSteeringWheel(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (!info.partSteeringWheel.isEmpty()) {
            float rot = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;

            for (MCH_AircraftInfo.PartWheel t : info.partSteeringWheel) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(t.pos.x, t.pos.y, t.pos.z);
                GL11.glRotated(rot * t.rotDir, t.rot.x, t.rot.y, t.rot.z);
                GlStateManager.translate(-t.pos.x, -t.pos.y, -t.pos.z);
                renderPart(t.model, info.model, t.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderWheel(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (!info.partWheel.isEmpty()) {
            float yaw = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;

            for (MCH_AircraftInfo.PartWheel t : info.partWheel) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(t.pos2.x, t.pos2.y, t.pos2.z);
                GL11.glRotated(yaw * t.rotDir, t.rot.x, t.rot.y, t.rot.z);
                GlStateManager.translate(-t.pos2.x, -t.pos2.y, -t.pos2.z);
                GlStateManager.translate(t.pos.x, t.pos.y, t.pos.z);
                GlStateManager.rotate(ac.prevRotWheel + (ac.rotWheel - ac.prevRotWheel) * tickTime, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(-t.pos.x, -t.pos.y, -t.pos.z);
                renderPart(t.model, info.model, t.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderRotPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (ac.haveRotPart()) {
            for (int i = 0; i < ac.rotPartRotation.length; i++) {
                float rot = ac.rotPartRotation[i];
                float prevRot = ac.prevRotPartRotation[i];
                if (prevRot > rot) {
                    rot += 360.0F;
                }

                rot = MCH_Lib.smooth(rot, prevRot, tickTime);
                MCH_AircraftInfo.RotPart h = info.partRotPart.get(i);
                GlStateManager.pushMatrix();
                GlStateManager.translate(h.pos.x, h.pos.y, h.pos.z);
                GlStateManager.rotate(rot, (float) h.rot.x, (float) h.rot.y, (float) h.rot.z);
                GlStateManager.translate(-h.pos.x, -h.pos.y, -h.pos.z);
                renderPart(h.model, info.model, h.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderWeapon(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        MCH_WeaponSet beforeWs = null;
        Entity e = ac.getRiddenByEntity();
        int weaponIndex = 0;
        Iterator<MCH_AircraftInfo.PartWeapon> var6 = info.partWeapon.iterator();

        while (true) {
            MCH_AircraftInfo.PartWeapon w;
            MCH_WeaponSet ws;
            while (true) {
                if (!var6.hasNext()) {
                    return;
                }

                w = (MCH_AircraftInfo.PartWeapon) var6.next();
                ws = ac.getWeaponByName(w.name[0]);
                if (ws != beforeWs) {
                    weaponIndex = 0;
                    beforeWs = ws;
                }

                float rotYaw = 0.0F;
                float prevYaw = 0.0F;
                float rotPitch = 0.0F;
                float prevPitch = 0.0F;
                if (!w.hideGM || !W_Lib.isFirstPerson()) {
                    break;
                }

                if (ws == null) {
                    if (ac.isMountedEntity(MCH_Lib.getClientPlayer())) {
                        continue;
                    }
                    break;
                } else {
                    boolean hide = false;

                    for (String s : w.name) {
                        if (W_Lib.isClientPlayer(ac.getWeaponUserByWeaponName(s))) {
                            hide = true;
                            break;
                        }
                    }

                    if (!hide) {
                        break;
                    }
                }
            }

            GlStateManager.pushMatrix();
            if (w.turret) {
                GlStateManager.translate(info.turretPosition.x, info.turretPosition.y, info.turretPosition.z);
                float ty = MCH_Lib.smooth(ac.getLastRiderYaw() - ac.getRotYaw(), ac.prevLastRiderYaw - ac.prevRotationYaw, tickTime);
                GlStateManager.rotate(ty, 0.0F, -1.0F, 0.0F);
                GlStateManager.translate(-info.turretPosition.x, -info.turretPosition.y, -info.turretPosition.z);
            }

            GlStateManager.translate(w.pos.x, w.pos.y, w.pos.z);
            if (w.yaw) {
                float var20;
                float var21;
                if (ws != null) {
                    var20 = ws.rotationYaw - ws.defaultRotationYaw;
                    var21 = ws.prevRotationYaw - ws.defaultRotationYaw;
                } else if (e != null) {
                    var20 = e.rotationYaw - ac.getRotYaw();
                    var21 = e.prevRotationYaw - ac.prevRotationYaw;
                } else {
                    var20 = ac.getLastRiderYaw() - ac.rotationYaw;
                    var21 = ac.prevLastRiderYaw - ac.prevRotationYaw;
                }

                if (var20 - var21 > 180.0F) {
                    var21 += 360.0F;
                } else if (var20 - var21 < -180.0F) {
                    var21 -= 360.0F;
                }

                GlStateManager.rotate(var21 + (var20 - var21) * tickTime, 0.0F, -1.0F, 0.0F);
            }

            if (w.turret) {
                float ty = MCH_Lib.smooth(ac.getLastRiderYaw() - ac.getRotYaw(), ac.prevLastRiderYaw - ac.prevRotationYaw, tickTime);
                ty -= ws.rotationTurretYaw;
                GlStateManager.rotate(-ty, 0.0F, -1.0F, 0.0F);
            }

            boolean rev_sign = false;
            if (ws != null && (int) ws.defaultRotationYaw != 0) {
                float t = MathHelper.wrapDegrees(ws.defaultRotationYaw);
                rev_sign = t >= 45.0F && t <= 135.0F || t <= -45.0F && t >= -135.0F;
                GlStateManager.rotate(-ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
            }

            if (w.pitch) {
                float var22;
                float var23;
                if (ws != null) {
                    var22 = ws.rotationPitch;
                    var23 = ws.prevRotationPitch;
                } else if (e != null) {
                    var22 = e.rotationPitch;
                    var23 = e.prevRotationPitch;
                } else {
                    var22 = ac.getLastRiderPitch();
                    var23 = ac.prevLastRiderPitch;
                }

                if (rev_sign) {
                    var22 = -var22;
                    var23 = -var23;
                }

                GlStateManager.rotate(var23 + (var22 - var23) * tickTime, 1.0F, 0.0F, 0.0F);
            }

            if (ws != null && w.recoilBuf != 0.0F) {
                MCH_WeaponSet.Recoil r = ws.recoilBuf[0];
                if (w.name.length > 1) {
                    for (String wnm : w.name) {
                        MCH_WeaponSet tws = ac.getWeaponByName(wnm);
                        if (tws != null && tws.recoilBuf[0].recoilBuf > r.recoilBuf) {
                            r = tws.recoilBuf[0];
                        }
                    }
                }

                float recoilBuf = r.prevRecoilBuf + (r.recoilBuf - r.prevRecoilBuf) * tickTime;
                GlStateManager.translate(0.0, 0.0, w.recoilBuf * recoilBuf);
            }

            if (ws != null) {
                GlStateManager.rotate(ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
                if (w.rotBarrel) {
                    float rotBrl = ws.prevRotBarrel + (ws.rotBarrel - ws.prevRotBarrel) * tickTime;
                    GlStateManager.rotate(rotBrl, (float) w.rot.x, (float) w.rot.y, (float) w.rot.z);
                }
            }

            GlStateManager.translate(-w.pos.x, -w.pos.y, -w.pos.z);
            if (!w.isMissile || !ac.isWeaponNotCooldown(ws, weaponIndex)) {
                renderPart(w.model, info.model, w.modelName);

                for (MCH_AircraftInfo.PartWeaponChild wc : w.child) {
                    GlStateManager.pushMatrix();
                    renderWeaponChild(ac, info, wc, ws, e, tickTime);
                    GlStateManager.popMatrix();
                }
            }

            GlStateManager.popMatrix();
            weaponIndex++;
        }
    }

    public static void renderWeaponChild(MCH_EntityAircraft ac, MCH_AircraftInfo info, MCH_AircraftInfo.PartWeaponChild w, MCH_WeaponSet ws, Entity e, float tickTime) {
        float rotYaw;
        float prevYaw;
        float rotPitch;
        float prevPitch;
        GlStateManager.translate(w.pos.x, w.pos.y, w.pos.z);
        if (w.yaw) {
            if (ws != null) {
                rotYaw = ws.rotationYaw - ws.defaultRotationYaw;
                prevYaw = ws.prevRotationYaw - ws.defaultRotationYaw;
            } else if (e != null) {
                rotYaw = e.rotationYaw - ac.getRotYaw();
                prevYaw = e.prevRotationYaw - ac.prevRotationYaw;
            } else {
                rotYaw = ac.getLastRiderYaw() - ac.rotationYaw;
                prevYaw = ac.prevLastRiderYaw - ac.prevRotationYaw;
            }

            if (rotYaw - prevYaw > 180.0F) {
                prevYaw += 360.0F;
            } else if (rotYaw - prevYaw < -180.0F) {
                prevYaw -= 360.0F;
            }

            GlStateManager.rotate(prevYaw + (rotYaw - prevYaw) * tickTime, 0.0F, -1.0F, 0.0F);
        }

        boolean rev_sign = false;
        if (ws != null && (int) ws.defaultRotationYaw != 0) {
            float t = MathHelper.wrapDegrees(ws.defaultRotationYaw);
            rev_sign = t >= 45.0F && t <= 135.0F || t <= -45.0F && t >= -135.0F;
            GlStateManager.rotate(-ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
        }

        if (w.pitch) {
            if (ws != null) {
                rotPitch = ws.rotationPitch;
                prevPitch = ws.prevRotationPitch;
            } else if (e != null) {
                rotPitch = e.rotationPitch;
                prevPitch = e.prevRotationPitch;
            } else {
                rotPitch = ac.getLastRiderPitch();
                prevPitch = ac.prevLastRiderPitch;
            }

            if (rev_sign) {
                rotPitch = -rotPitch;
                prevPitch = -prevPitch;
            }

            GlStateManager.rotate(prevPitch + (rotPitch - prevPitch) * tickTime, 1.0F, 0.0F, 0.0F);
        }

        if (ws != null && w.recoilBuf != 0.0F) {
            MCH_WeaponSet.Recoil r = ws.recoilBuf[0];
            if (w.name.length > 1) {
                for (String wnm : w.name) {
                    MCH_WeaponSet tws = ac.getWeaponByName(wnm);
                    if (tws != null && tws.recoilBuf[0].recoilBuf > r.recoilBuf) {
                        r = tws.recoilBuf[0];
                    }
                }
            }

            float recoilBuf = r.prevRecoilBuf + (r.recoilBuf - r.prevRecoilBuf) * tickTime;
            GlStateManager.translate(0.0, 0.0, -w.recoilBuf * recoilBuf);
        }

        if (ws != null) {
            GlStateManager.rotate(ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
        }

        GlStateManager.translate(-w.pos.x, -w.pos.y, -w.pos.z);
        renderPart(w.model, info.model, w.modelName);
    }

    public static void renderTrackRoller(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (!info.partTrackRoller.isEmpty()) {
            float[] rot = ac.rotTrackRoller;
            float[] prevRot = ac.prevRotTrackRoller;

            for (MCH_AircraftInfo.TrackRoller t : info.partTrackRoller) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(t.pos.x, t.pos.y, t.pos.z);
                GlStateManager.rotate(prevRot[t.side] + (rot[t.side] - prevRot[t.side]) * tickTime, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(-t.pos.x, -t.pos.y, -t.pos.z);
                renderPart(t.model, info.model, t.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderCrawlerTrack(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (!info.partCrawlerTrack.isEmpty()) {
            int prevWidth = GL11.glGetInteger(2833);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder builder = tessellator.getBuffer();

            for (MCH_AircraftInfo.CrawlerTrack c : info.partCrawlerTrack) {
                GL11.glPointSize(c.len * 20.0F);
                if (MCH_Config.TestMode.prmBool) {
                    GlStateManager.disableTexture2D();
                    GlStateManager.disableBlend();
                    builder.begin(0, DefaultVertexFormats.POSITION_COLOR);

                    for (int i = 0; i < c.cx.length; i++) {
                        builder.pos(c.z, c.cx[i], c.cy[i]).color((int) (255.0F / c.cx.length * i), 80, 255 - (int) (255.0F / c.cx.length * i), 255).endVertex();
                    }

                    tessellator.draw();
                }

                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                int L = c.lp.size() - 1;
                double rc = ac != null ? ac.rotCrawlerTrack[c.side] : 0.0;
                double pc = ac != null ? ac.prevRotCrawlerTrack[c.side] : 0.0;

                for (int i = 0; i < L; i++) {
                    MCH_AircraftInfo.CrawlerTrackPrm cp = c.lp.get(i);
                    MCH_AircraftInfo.CrawlerTrackPrm np = c.lp.get((i + 1) % L);
                    double x1 = cp.x;
                    double x2 = np.x;
                    double r1 = cp.r;
                    double y1 = cp.y;
                    double y2 = np.y;
                    double r2 = np.r;
                    if (r2 - r1 < -180.0) {
                        r2 += 360.0;
                    }

                    if (r2 - r1 > 180.0) {
                        r2 -= 360.0;
                    }

                    double sx = x1 + (x2 - x1) * rc;
                    double sy = y1 + (y2 - y1) * rc;
                    double sr = r1 + (r2 - r1) * rc;
                    double ex = x1 + (x2 - x1) * pc;
                    double ey = y1 + (y2 - y1) * pc;
                    double er = r1 + (r2 - r1) * pc;
                    double x = sx + (ex - sx) * pc;
                    double y = sy + (ey - sy) * pc;
                    double r = sr + (er - sr) * pc;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(0.0, x, y);
                    GlStateManager.rotate((float) r, -1.0F, 0.0F, 0.0F);
                    renderPart(c.model, info.model, c.modelName);
                    GlStateManager.popMatrix();
                }
            }

            GlStateManager.enableBlend();
            GL11.glPointSize(prevWidth);
        }
    }

    public static void renderHatch(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (info.haveHatch() && ac.partHatch != null) {
            float rot = ac.getHatchRotation();
            float prevRot = ac.getPrevHatchRotation();

            for (MCH_AircraftInfo.Hatch h : info.hatchList) {
                GlStateManager.pushMatrix();
                if (h.isSlide) {
                    float r = ac.partHatch.rotation / ac.partHatch.rotationMax;
                    float pr = ac.partHatch.prevRotation / ac.partHatch.rotationMax;
                    float f = pr + (r - pr) * tickTime;
                    GlStateManager.translate(h.pos.x * f, h.pos.y * f, h.pos.z * f);
                } else {
                    GlStateManager.translate(h.pos.x, h.pos.y, h.pos.z);
                    GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * h.maxRotFactor, (float) h.rot.x, (float) h.rot.y, (float) h.rot.z);
                    GlStateManager.translate(-h.pos.x, -h.pos.y, -h.pos.z);
                }

                renderPart(h.model, info.model, h.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderThrottle(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (info.havePartThrottle()) {
            float throttle = MCH_Lib.smooth((float) ac.getCurrentThrottle(), (float) ac.getPrevCurrentThrottle(), tickTime);

            for (MCH_AircraftInfo.Throttle h : info.partThrottle) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(h.pos.x, h.pos.y, h.pos.z);
                GlStateManager.rotate(throttle * h.rot2, (float) h.rot.x, (float) h.rot.y, (float) h.rot.z);
                GlStateManager.translate(-h.pos.x, -h.pos.y, -h.pos.z);
                GlStateManager.translate(h.slide.x * throttle, h.slide.y * throttle, h.slide.z * throttle);
                renderPart(h.model, info.model, h.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderWeaponBay(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        for (int i = 0; i < info.partWeaponBay.size(); i++) {
            MCH_AircraftInfo.WeaponBay w = info.partWeaponBay.get(i);
            MCH_EntityAircraft.WeaponBay ws = ac.weaponBays[i];
            GlStateManager.pushMatrix();
            if (w.isSlide) {
                float r = ws.rot / 90.0F;
                float pr = ws.prevRot / 90.0F;
                float f = pr + (r - pr) * tickTime;
                GlStateManager.translate(w.pos.x * f, w.pos.y * f, w.pos.z * f);
            } else {
                GlStateManager.translate(w.pos.x, w.pos.y, w.pos.z);
                GlStateManager.rotate((ws.prevRot + (ws.rot - ws.prevRot) * tickTime) * w.maxRotFactor, (float) w.rot.x, (float) w.rot.y, (float) w.rot.z);
                GlStateManager.translate(-w.pos.x, -w.pos.y, -w.pos.z);
            }

            renderPart(w.model, info.model, w.modelName);
            GlStateManager.popMatrix();
        }
    }

    public static void renderCamera(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (info.havePartCamera()) {
            float rotYaw = ac.camera.partRotationYaw;
            float prevRotYaw = ac.camera.prevPartRotationYaw;
            float rotPitch = ac.camera.partRotationPitch;
            float prevRotPitch = ac.camera.prevPartRotationPitch;
            float yaw = prevRotYaw + (rotYaw - prevRotYaw) * tickTime - ac.getRotYaw();
            float pitch = prevRotPitch + (rotPitch - prevRotPitch) * tickTime - ac.getRotPitch();

            for (MCH_AircraftInfo.Camera c : info.cameraList) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(c.pos.x, c.pos.y, c.pos.z);
                if (c.yawSync) {
                    GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
                }

                if (c.pitchSync) {
                    GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                }

                GlStateManager.translate(-c.pos.x, -c.pos.y, -c.pos.z);
                renderPart(c.model, info.model, c.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderCanopy(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (info.haveCanopy() && ac.partCanopy != null) {
            float rot = ac.getCanopyRotation();
            float prevRot = ac.getPrevCanopyRotation();

            for (MCH_AircraftInfo.Canopy c : info.canopyList) {
                GlStateManager.pushMatrix();
                if (c.isSlide) {
                    float r = ac.partCanopy.rotation / ac.partCanopy.rotationMax;
                    float pr = ac.partCanopy.prevRotation / ac.partCanopy.rotationMax;
                    float f = pr + (r - pr) * tickTime;
                    GlStateManager.translate(c.pos.x * f, c.pos.y * f, c.pos.z * f);
                } else {
                    GlStateManager.translate(c.pos.x, c.pos.y, c.pos.z);
                    GlStateManager.rotate((prevRot + (rot - prevRot) * tickTime) * c.maxRotFactor, (float) c.rot.x, (float) c.rot.y, (float) c.rot.z);
                    GlStateManager.translate(-c.pos.x, -c.pos.y, -c.pos.z);
                }

                renderPart(c.model, info.model, c.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderLandingGear(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
        if (info.haveLandingGear() && ac.partLandingGear != null) {
            float rot = ac.getLandingGearRotation();
            float prevRot = ac.getPrevLandingGearRotation();
            float revR = 90.0F - rot;
            float revPr = 90.0F - prevRot;
            float rot1 = prevRot + (rot - prevRot) * tickTime;
            float rot1Rev = revPr + (revR - revPr) * tickTime;
            float rotHatch = 90.0F * MathHelper.sin(rot1 * 2.0F * (float) Math.PI / 180.0F) * 3.0F;
            if (rotHatch > 90.0F) {
                rotHatch = 90.0F;
            }

            for (MCH_AircraftInfo.LandingGear n : info.landingGear) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(n.pos.x, n.pos.y, n.pos.z);
                if (!n.reverse) {
                    if (!n.hatch) {
                        GlStateManager.rotate(rot1 * n.maxRotFactor, (float) n.rot.x, (float) n.rot.y, (float) n.rot.z);
                    } else {
                        GlStateManager.rotate(rotHatch * n.maxRotFactor, (float) n.rot.x, (float) n.rot.y, (float) n.rot.z);
                    }
                } else {
                    GlStateManager.rotate(rot1Rev * n.maxRotFactor, (float) n.rot.x, (float) n.rot.y, (float) n.rot.z);
                }

                if (n.enableRot2) {
                    if (!n.reverse) {
                        GlStateManager.rotate(rot1 * n.maxRotFactor2, (float) n.rot2.x, (float) n.rot2.y, (float) n.rot2.z);
                    } else {
                        GlStateManager.rotate(rot1Rev * n.maxRotFactor2, (float) n.rot2.x, (float) n.rot2.y, (float) n.rot2.z);
                    }
                }

                GlStateManager.translate(-n.pos.x, -n.pos.y, -n.pos.z);
                if (n.slide != null) {
                    float f = rot / 90.0F;
                    if (n.reverse) {
                        f = 1.0F - f;
                    }

                    GlStateManager.translate(f * n.slide.x, f * n.slide.y, f * n.slide.z);
                }

                renderPart(n.model, info.model, n.modelName);
                GlStateManager.popMatrix();
            }
        }
    }

    public static void renderEntityMarker(Entity entity) {
        Entity player = Minecraft.getMinecraft().player;
        if (player != null) {
            if (!W_Entity.isEqual(player, entity)) {
                MCH_EntityAircraft ac = null;
                if (player.getRidingEntity() instanceof MCH_EntityAircraft) {
                    ac = (MCH_EntityAircraft) player.getRidingEntity();
                } else if (player.getRidingEntity() instanceof MCH_EntitySeat) {
                    ac = ((MCH_EntitySeat) player.getRidingEntity()).getParent();
                } else if (player.getRidingEntity() instanceof MCH_EntityUavStation) {
                    ac = ((MCH_EntityUavStation) player.getRidingEntity()).getControlAircract();
                }

                if (ac != null) {
                    if (!W_Entity.isEqual(ac, entity)) {
                        MCH_WeaponGuidanceSystem gs = ac.getCurrentWeapon(player).getCurrentWeapon().getGuidanceSystem();
                        if (gs != null && gs.canLockEntity(entity)) {
                            RenderManager rm = Minecraft.getMinecraft().getRenderManager();
                            double dist = entity.getDistanceSq(rm.renderViewEntity);
                            double x = entity.posX - TileEntityRendererDispatcher.staticPlayerX;
                            double y = entity.posY - TileEntityRendererDispatcher.staticPlayerY;
                            double z = entity.posZ - TileEntityRendererDispatcher.staticPlayerZ;
                            if (dist < 10000.0) {
                                GlStateManager.pushMatrix();
                                GlStateManager.translate((float) x, (float) y + entity.height + 0.5F, (float) z);
                                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                                GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                                GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                                GlStateManager.scale(-0.02666667F, -0.02666667F, 0.02666667F);
                                GlStateManager.disableLighting();
                                GlStateManager.translate(0.0F, 9.374999F, 0.0F);
                                GlStateManager.depthMask(false);
                                GlStateManager.enableBlend();
                                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                                GlStateManager.disableTexture2D();
                                int prevWidth = GL11.glGetInteger(2849);
                                float size = Math.max(entity.width, entity.height) * 20.0F;
                                if (entity instanceof MCH_EntityAircraft) {
                                    size *= 2.0F;
                                }

                                Tessellator tessellator = Tessellator.getInstance();
                                BufferBuilder builder = tessellator.getBuffer();
                                builder.begin(2, MCH_Verts.POS_COLOR_LMAP);
                                boolean isLockEntity = gs.isLockingEntity(entity);
                                if (isLockEntity) {
                                    GL11.glLineWidth(MCH_Gui.scaleFactor * 1.5F);
                                    builder.pos(-size - 1.0F, 0.0, 0.0).color(1.0F, 0.0F, 0.0F, 1.0F).lightmap(0, 240).endVertex();
                                    builder.pos(-size - 1.0F, size * 2.0F, 0.0).color(1.0F, 0.0F, 0.0F, 1.0F).lightmap(0, 240).endVertex();
                                    builder.pos(size + 1.0F, size * 2.0F, 0.0).color(1.0F, 0.0F, 0.0F, 1.0F).lightmap(0, 240).endVertex();
                                    builder.pos(size + 1.0F, 0.0, 0.0).color(1.0F, 0.0F, 0.0F, 1.0F).lightmap(0, 240).endVertex();
                                } else {
                                    GL11.glLineWidth(MCH_Gui.scaleFactor);
                                    builder.pos(-size - 1.0F, 0.0, 0.0).color(1.0F, 0.3F, 0.0F, 8.0F).lightmap(0, 240).endVertex();
                                    builder.pos(-size - 1.0F, size * 2.0F, 0.0).color(1.0F, 0.3F, 0.0F, 8.0F).lightmap(0, 240).endVertex();
                                    builder.pos(size + 1.0F, size * 2.0F, 0.0).color(1.0F, 0.3F, 0.0F, 8.0F).lightmap(0, 240).endVertex();
                                    builder.pos(size + 1.0F, 0.0, 0.0).color(1.0F, 0.3F, 0.0F, 8.0F).lightmap(0, 240).endVertex();
                                }

                                tessellator.draw();
                                GlStateManager.popMatrix();
                                if (!ac.isUAV() && isLockEntity && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                                    GlStateManager.pushMatrix();
                                    builder.begin(1, MCH_Verts.POS_COLOR_LMAP);
                                    GL11.glLineWidth(1.0F);
                                    builder.pos(x, y + entity.height / 2.0F, z).color(1.0F, 0.0F, 0.0F, 1.0F).lightmap(0, 240).endVertex();
                                    builder.pos(ac.lastTickPosX - TileEntityRendererDispatcher.staticPlayerX, ac.lastTickPosY - TileEntityRendererDispatcher.staticPlayerY - 1.0, ac.lastTickPosZ - TileEntityRendererDispatcher.staticPlayerZ).color(1.0F, 0.0F, 0.0F, 1.0F).lightmap(0, 240).endVertex();
                                    tessellator.draw();
                                    GlStateManager.popMatrix();
                                }

                                GL11.glLineWidth(prevWidth);
                                GlStateManager.enableTexture2D();
                                GlStateManager.depthMask(true);
                                GlStateManager.enableLighting();
                                GlStateManager.disableBlend();
                                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void renderRope(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
        GlStateManager.pushMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        if (ac.isRepelling()) {
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();

            for (int i = 0; i < info.repellingHooks.size(); i++) {
                builder.begin(3, DefaultVertexFormats.POSITION_COLOR);
                builder.pos(info.repellingHooks.get(i).pos.x, info.repellingHooks.get(i).pos.y, info.repellingHooks.get(i).pos.z).color(0, 0, 0, 255).endVertex();
                builder.pos(info.repellingHooks.get(i).pos.x, info.repellingHooks.get(i).pos.y + ac.ropesLength, info.repellingHooks.get(i).pos.z).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
            }

            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
        }

        GlStateManager.popMatrix();
    }

    public void doRender(T entity, double posX, double posY, double posZ, float par8, float tickTime) {
        MCH_AircraftInfo info = entity.getAcInfo();
        if (info != null) {
            GlStateManager.pushMatrix();
            float yaw = this.calcRot(entity.getRotYaw(), entity.prevRotationYaw, tickTime);
            float pitch = entity.calcRotPitch(tickTime);
            float roll = this.calcRot(entity.getRotRoll(), entity.prevRotationRoll, tickTime);
            if (MCH_Config.EnableModEntityRender.prmBool) {
                this.renderRiddenEntity(entity, tickTime, yaw, pitch + info.entityPitch, roll + info.entityRoll, info.entityWidth, info.entityHeight);
            }

            if (!shouldSkipRender(entity)) {
                this.setCommonRenderParam(info.smoothShading, entity.getBrightnessForRender());
                if (entity.isDestroyed()) {
                    GlStateManager.color(0.15F, 0.15F, 0.15F, 1.0F);
                } else {
                    GlStateManager.color(0.75F, 0.75F, 0.75F, (float) MCH_Config.__TextureAlpha.prmDouble);
                }

                this.renderAircraft(entity, posX, posY, posZ, yaw, pitch, roll, tickTime);
                this.renderCommonPart(entity, info, posX, posY, posZ, tickTime);
                renderLight(posX, posY, posZ, tickTime, entity, info);
                this.restoreCommonRenderParam();
            }

            GlStateManager.popMatrix();
            MCH_GuiTargetMarker.addMarkEntityPos(1, entity, posX, posY + info.markerHeight, posZ);
            MCH_ClientLightWeaponTickHandler.markEntity(entity, posX, posY, posZ);
            renderEntityMarker(entity);
            if (MCH_Config.TestMode.prmBool) {
                com.norwood.mcheli.helper.debug.WeaponPointRenderer.renderWeaponPoints(entity, info, posX, posY, posZ);
            }
        }
    }


    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity.canRenderOnFire()) {
            this.renderEntityOnFire(entity, x, y, z, partialTicks);
        }
    }

    private void renderEntityOnFire(Entity entity, double x, double y, double z, float tick) {
        GlStateManager.disableLighting();
        TextureMap texturemap = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite textureatlassprite = texturemap.getAtlasSprite("minecraft:blocks/fire_layer_0");
        TextureAtlasSprite textureatlassprite1 = texturemap.getAtlasSprite("minecraft:blocks/fire_layer_1");
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        float f1 = entity.width * 1.4F;
        GlStateManager.scale(f1 * 2.0F, f1 * 2.0F, f1 * 2.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        float f2 = 1.5F;
        float f3 = 0.0F;
        float f4 = entity.height / f1;
        float f5 = (float) (entity.posY + entity.getEntityBoundingBox().minY);
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, -0.3F + (int) f4 * 0.02F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float f6 = 0.0F;
        int i = 0;
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);

        while (f4 > 0.0F) {
            TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
            this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            float f7 = textureatlassprite2.getMinU();
            float f8 = textureatlassprite2.getMinV();
            float f9 = textureatlassprite2.getMaxU();
            float f10 = textureatlassprite2.getMaxV();
            if (i / 2 % 2 == 0) {
                float f11 = f9;
                f9 = f7;
                f7 = f11;
            }

            bufferbuilder.pos(f2 - f3, 0.0F - f5, f6).tex(f9, f10).endVertex();
            bufferbuilder.pos(-f2 - f3, 0.0F - f5, f6).tex(f7, f10).endVertex();
            bufferbuilder.pos(-f2 - f3, 1.4F - f5, f6).tex(f7, f8).endVertex();
            bufferbuilder.pos(f2 - f3, 1.4F - f5, f6).tex(f9, f8).endVertex();
            f4 -= 0.45F;
            f5 -= 0.45F;
            f2 *= 0.9F;
            f6 += 0.03F;
            i++;
        }

        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }

    protected void bindTexture(String path, MCH_EntityAircraft ac) {
        if (ac == MCH_ClientCommonTickHandler.ridingAircraft) {
            int bk = MCH_ClientCommonTickHandler.cameraMode;
            MCH_ClientCommonTickHandler.cameraMode = 0;
            super.bindTexture(MCH_Utils.suffix(path));
            MCH_ClientCommonTickHandler.cameraMode = bk;
        } else {
            super.bindTexture(MCH_Utils.suffix(path));
        }
    }

    public void renderRiddenEntity(MCH_EntityAircraft ac, float tickTime, float yaw, float pitch, float roll, float width, float height) {
        MCH_ClientEventHook.setCancelRender(false);
        GlStateManager.pushMatrix();
        this.renderEntitySimple(ac, ac.getRiddenByEntity(), tickTime, yaw, pitch, roll, width, height);

        for (MCH_EntitySeat s : ac.getSeats()) {
            if (s != null) {
                this.renderEntitySimple(ac, s.getRiddenByEntity(), tickTime, yaw, pitch, roll, width, height);
            }
        }

        GlStateManager.popMatrix();
        MCH_ClientEventHook.setCancelRender(true);
    }

    public void renderEntitySimple(MCH_EntityAircraft ac, Entity entity, float tickTime, float yaw, float pitch, float roll, float width, float height) {
        if (entity != null) {
            boolean isPilot = ac.isPilot(entity);
            boolean isClientPlayer = W_Lib.isClientPlayer(entity);
            if (!isClientPlayer || !W_Lib.isFirstPerson() || isPilot && ac.getCameraId() > 0) {
                GlStateManager.pushMatrix();
                if (entity.ticksExisted == 0) {
                    entity.lastTickPosX = entity.posX;
                    entity.lastTickPosY = entity.posY;
                    entity.lastTickPosZ = entity.posZ;
                }

                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * tickTime;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * tickTime;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * tickTime;
                float f1 = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickTime;
                int i = entity.getBrightnessForRender();
                if (entity.isBurning()) {
                    i = 15728880;
                }

                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                double dx = x - TileEntityRendererDispatcher.staticPlayerX;
                double dy = y - TileEntityRendererDispatcher.staticPlayerY;
                double dz = z - TileEntityRendererDispatcher.staticPlayerZ;
                GlStateManager.translate(dx, dy, dz);
                GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(width, height, width);
                GlStateManager.rotate(-yaw, 0.0F, -1.0F, 0.0F);
                GlStateManager.translate(-dx, -dy, -dz);
                boolean bk = renderingEntity;
                renderingEntity = true;
                Entity ridingEntity = entity.getRidingEntity();
                if (!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_IEntityCanRideAircraft)) {
                    entity.dismountRidingEntity();
                }

                EntityLivingBase entityLiving = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
                float bkPitch = 0.0F;
                float bkPrevPitch = 0.0F;
                if (isPilot && entityLiving != null) {
                    entityLiving.renderYawOffset = ac.getRotYaw();
                    entityLiving.prevRenderYawOffset = ac.getRotYaw();
                    if (ac.getCameraId() > 0) {
                        entityLiving.rotationYawHead = ac.getRotYaw();
                        entityLiving.prevRotationYawHead = ac.getRotYaw();
                        bkPitch = entityLiving.rotationPitch;
                        bkPrevPitch = entityLiving.prevRotationPitch;
                        entityLiving.rotationPitch = ac.getRotPitch();
                        entityLiving.prevRotationPitch = ac.getRotPitch();
                    }
                }

                W_EntityRenderer.renderEntityWithPosYaw(this.renderManager, entity, dx, dy, dz, f1, tickTime, false);
                if (isPilot && entityLiving != null && ac.getCameraId() > 0) {
                    entityLiving.rotationPitch = bkPitch;
                    entityLiving.prevRotationPitch = bkPrevPitch;
                }

                entity.startRiding(ridingEntity);
                renderingEntity = bk;
                GlStateManager.popMatrix();
            }
        }
    }

    public abstract void renderAircraft(MCH_EntityAircraft var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11);

    public float calcRot(float rot, float prevRot, float tickTime) {
        rot = MathHelper.wrapDegrees(rot);
        prevRot = MathHelper.wrapDegrees(prevRot);
        if (rot - prevRot < -180.0F) {
            prevRot -= 360.0F;
        } else if (prevRot - rot < -180.0F) {
            prevRot += 360.0F;
        }

        return prevRot + (rot - prevRot) * tickTime;
    }

    public void renderDebugHitBox(MCH_EntityAircraft e, double x, double y, double z, float yaw, float pitch) {
        if (MCH_Config.TestMode.prmBool && debugModel != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(e.width, e.height, e.width);
            this.bindTexture("textures/hit_box.png");
            debugModel.renderAll();
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);

            for (MCH_BoundingBox bb : e.extraBoundingBox) {
                GlStateManager.pushMatrix();

                if (bb.rotatedOffset != null)
                    GlStateManager.translate(bb.rotatedOffset.x, bb.rotatedOffset.y, bb.rotatedOffset.z);
                float yAngle = bb.rotationYaw;
                float pAngle = bb.rotationPitch;
                float rAngle = bb.rotationRoll;
                if (bb.boundingBoxType == MCH_BoundingBox.EnumBoundingBoxType.TURRET) {
                    yAngle += bb.localRotYaw;
                    pAngle += bb.localRotPitch;
                    rAngle += bb.localRotRoll;
                }

                GlStateManager.rotate(-yAngle, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(pAngle, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(rAngle, 0.0F, 0.0F, 1.0F);

                GlStateManager.scale(bb.width, bb.height, bb.width);

                this.bindTexture("textures/bounding_box.png");
                debugModel.renderAll();
                GlStateManager.popMatrix();
                this.drawHitBoxDetail(bb);
            }

            GlStateManager.popMatrix();
            GlStateManager.color(1,1,1,1);
        }
    }

    public void drawHitBoxDetail(MCH_BoundingBox bb) {
        String s = String.format("%.2f", bb.damageFactor);
        float scale = 0.08F;

        GlStateManager.pushMatrix();

        Vec3d offset = bb.rotatedOffset == null ? Vec3d.ZERO : bb.rotatedOffset;
        GlStateManager.translate(offset.x, offset.y, offset.z);
        GlStateManager.translate(0.0F, 0.5F + bb.height, 0.0F);

        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        FontRenderer font = this.getFontRendererFromRenderManager();

        int strWidth = font.getStringWidth(s) / 2;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        GlStateManager.disableTexture2D();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(-strWidth - 1, -1.0D, 0.0D).color(0F, 0F, 0F, 0.4F).endVertex();
        buf.pos(-strWidth - 1, 8.0D, 0.0D).color(0F, 0F, 0F, 0.4F).endVertex();
        buf.pos(strWidth + 1, 8.0D, 0.0D).color(0F, 0F, 0F, 0.4F).endVertex();
        buf.pos(strWidth + 1, -1.0D, 0.0D).color(0F, 0F, 0F, 0.4F).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();

        int color = bb.damageFactor < 1.0F ? 0xFFFFFFFF : (bb.damageFactor > 1.0F ? 0xFFFF0000 : 0xFFFFFF);
        font.drawString(s, -font.getStringWidth(s) / 2, 0, color);

        GlStateManager.popMatrix();
    }

    public final boolean shouldRender(MCH_EntityAircraft livingEntity, ICamera camera, double camX, double camY, double camZ) {
        return false;
    }

    public void renderDebugPilotSeat(MCH_EntityAircraft e, double x, double y, double z, float yaw, float pitch, float roll) {
        if (MCH_Config.TestMode.prmBool && debugModel != null) {
            GlStateManager.pushMatrix();
            MCH_SeatInfo seat = e.getSeatInfo(0);
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(seat.pos.x, seat.pos.y, seat.pos.z);
            GlStateManager.scale(1.0F, 1.0F, 1.0F);
            this.bindTexture("textures/seat_pilot.png");
            debugModel.renderAll();
            GlStateManager.popMatrix();
        }
    }

    public void renderCommonPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
        renderRope(ac, info, x, y, z, tickTime);
        renderWeapon(ac, info, tickTime);
        renderRotPart(ac, info, tickTime);
        renderHatch(ac, info, tickTime);
        renderTrackRoller(ac, info, tickTime);
        renderCrawlerTrack(ac, info, tickTime);
        renderSteeringWheel(ac, info, tickTime);
        renderLightHatch(ac, info, tickTime);
        renderWheel(ac, info, tickTime);
        renderThrottle(ac, info, tickTime);
        renderCamera(ac, info, tickTime);
        renderLandingGear(ac, info, tickTime);
        renderWeaponBay(ac, info, tickTime);
        renderCanopy(ac, info, tickTime);
    }
}
