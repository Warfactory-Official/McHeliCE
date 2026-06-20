package com.norwood.mcheli.helper.client;

import com.norwood.mcheli.MCH_3rdCamera;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.hud.direct_drawable.DirectDrawable;
import com.norwood.mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import com.norwood.mcheli.uav.IUavStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.EntityViewRenderEvent.FOVModifier;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.*;

@EventBusSubscriber(
        modid = "mcheli",
        value = {Side.CLIENT})
public class MCH_CameraManager {

    private static final float DEF_THIRD_CAMERA_DIST = 4.0F;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static float cameraRoll = 0.0F;
    private static int dbgFcamFrame = 0; // [DEBUG FCAM] throttle counter
    private static float cameraDistance = 4.0F;
    private static float cameraZoom = 1.0F;
    private static MCH_EntityAircraft ridingAircraft = null;

    @Nullable
    private static Tuple<EntityPlayer, MCH_EntityAircraft> getActivePilotContext() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null)
            return null;

        MCH_EntityAircraft ac = getRiddenAircraft(mc, player);
        if (ac == null)
            return null;

        return new Tuple<>(player, ac);
    }

    @Nullable
    private static MCH_EntityAircraft getRiddenAircraft(Minecraft mc, EntityPlayer player) {
        Entity riding = player.getRidingEntity();
        return switch (riding) {
            case MCH_EntityAircraft aircraft -> aircraft;
            case MCH_EntitySeat mchEntitySeat -> mchEntitySeat.getParent();
            case IUavStation iUavStation -> iUavStation.getControlled();
            case null, default -> null;
        };
    }

    @SubscribeEvent
    static void onCameraSetupEvent(CameraSetup event) {
        float f = event.getEntity().getEyeHeight();
        if (ridingAircraft != null && mc.gameSettings.thirdPersonView > 0) {
            if (mc.gameSettings.thirdPersonView == 2) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.translate(0.0F, 0.0F, -(cameraDistance - 4.0F));
            if (mc.gameSettings.thirdPersonView == 2) {
                GlStateManager.rotate(-180.0F, 0.0F, 1.0F, 0.0F);
            }
        }

        MCH_EntityAircraft ridingEntity = ridingAircraft;
        if (ridingEntity != null && ridingEntity.canSwitchFreeLook() && ridingEntity.isPilot(mc.player)) {
            boolean quatCam = MCH_Config.ExperimentalQuaternionRotation.prmBool
                    && ridingEntity.orientation != null
                    && ridingEntity.isOverridePlayerPitch()
                    && !ridingEntity.hasIndependentMountedAim(mc.player);

            // Cockpit-relative quaternion free-look: the eye = orientation * freeLookOffset, so the
            // view is bolted to the airframe and the mouse pans in the screen plane (no roll-skew,
            // no gimbal over the top). Active only in free-look (where quatCam above is off, since it
            // requires isOverridePlayerPitch()).
            boolean freeLookQuatCam = MCH_Config.ExperimentalQuaternionRotation.prmBool
                    && ridingEntity.isFreeLookMode()
                    && ridingEntity.orientation != null
                    && ridingEntity.freeLookOffset != null
                    && !ridingEntity.hasIndependentMountedAim(mc.player);

            // [DEBUG FCAM] Which camera branch is the CAMERA actually taking, and what does it see?
            // Tells us whether free-look uses the cockpit-relative quaternion (orientation*offset) or
            // falls through to the world-upright legacy path. Throttled; gated by the debug flag.
            if (MCH_Config.ExperimentalQuaternionRotation.prmBool && (dbgFcamFrame++ % 15 == 0)) {
                org.joml.Quaternionf ori = ridingEntity.orientation;
                org.joml.Quaternionf off = ridingEntity.freeLookOffset;
                String branch = quatCam ? "quatCam" : (freeLookQuatCam ? "freeLookQuatCam" : "legacy");
                String viewE = "n/a";
                if (ori != null && off != null) {
                    float[] e = MCH_Lib.worldEuler(new Quaternionf(ori).mul(off));
                    viewE = String.format("yaw=%.1f,pitch=%.1f,roll=%.1f", e[1], e[0], e[2]);
                }
                MCH_Logger.info("[FCAM] branch={} freeLook={} oriNull={} offNull={} mountedAim={} overridePitch={} camRoll={} ac(y={},p={},r={}) view({})",
                        branch, ridingEntity.isFreeLookMode(), ori == null, off == null,
                        ridingEntity.hasIndependentMountedAim(mc.player), ridingEntity.isOverridePlayerPitch(),
                        cameraRoll, ridingEntity.getYaw(), ridingEntity.getPitch(), ridingEntity.getRoll(), viewE);
            }

            if (quatCam) {
                // Airframe-locked view. KEEP the eye-height bracket: with the airframe attitude
                // driving camRot, T(0,-f,0)*camRot*T(0,+f,0) makes the eye orbit with the aircraft
                // (the cockpit-attached feel). This is the original, working behavior.
                GlStateManager.translate(0.0F, -f, 0.0F);
                applyAirframeQuatCamera(new Quaternionf(ridingEntity.orientation));
                event.setRoll(0.0F);
                event.setPitch(0.0F);
                event.setYaw(0.0F);
                GlStateManager.translate(0.0F, f, 0.0F);
            } else if (freeLookQuatCam) {
                // Free-look head movement. NO bracket: vanilla applies its own translate(0,-f,0)
                // after this handler, giving R * T(0,-f,0) * W -> the head pivots about the FIXED
                // eye. The bracket here would orbit the eye about a point f below it (the "pivot
                // around the seat" artifact). The eye is still carried by the airframe via the rider
                // position, so only the head rotation is decoupled from that orbit.
                applyAirframeQuatCamera(new Quaternionf(ridingEntity.orientation).mul(ridingEntity.freeLookOffset));
                event.setRoll(0.0F);
                event.setPitch(0.0F);
                event.setYaw(0.0F);
            } else {
                GlStateManager.translate(0.0F, -f, 0.0F);
                GlStateManager.rotate(cameraRoll, 0.0F, 0.0F, 1.0F);

                if (ridingEntity.isOverridePlayerPitch() && !ridingEntity.hasIndependentMountedAim(mc.player)) {
                    GlStateManager.rotate(ridingEntity.rotationPitch, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(ridingEntity.rotationYaw, 0.0F, 1.0F, 0.0F);
                    event.setPitch(event.getPitch() - ridingEntity.rotationPitch);
                    event.setYaw(event.getYaw() - ridingEntity.rotationYaw);
                }
                GlStateManager.translate(0.0F, f, 0.0F);
            }
        }
    }

    /** Apply the quaternion camera rotation for a body->world view orientation: camRot = Ry(180) * view^-1. */
    private static void applyAirframeQuatCamera(Quaternionf view) {
        Quaternionf camRot = new Quaternionf()
                .rotateY((float) Math.toRadians(180.0))
                .mul(view.conjugate());
        AxisAngle4f aa = new AxisAngle4f().set(camRot);
        if (aa.angle != 0.0F && !Float.isNaN(aa.angle)) {
            GlStateManager.rotate((float) Math.toDegrees(aa.angle), aa.x, aa.y, aa.z);
        }
    }

    @SubscribeEvent
    static void onFOVModifierEvent(FOVModifier event) {
        // The UAV camera feed widget always renders at a fixed FOV, independent of the player's FOV
        if (com.norwood.mcheli.uav.WidgetUavCameraFeed.RENDERING_FEED) {
            event.setFOV(com.norwood.mcheli.uav.WidgetUavCameraFeed.FEED_FOV);
            return;
        }
        if (MCH_ItemRangeFinder.isUsingScope(mc.player)) {
            event.setFOV(event.getFOV() * (1.0F / cameraZoom));
            return;
        }
        if (ridingAircraft != null) {
            MCH_ViewEntityDummy viewer = MCH_ViewEntityDummy.getInstance(mc.world);
            if (viewer == event.getEntity() || event.getEntity() instanceof MCH_3rdCamera) {
                event.setFOV(event.getFOV() * (1.0F / cameraZoom));
            }
        }
    }

    public static void setCameraRoll(float roll) {
        roll = MathHelper.wrapDegrees(roll);
        cameraRoll = roll;
    }

    public static void setCameraZoom(float zoom) {
        cameraZoom = zoom;
    }

    public static float getThirdPersonCameraDistance() {
        return cameraDistance;
    }

    public static void setThirdPeasonCameraDistance(float distance) {
        distance = MathHelper.clamp(distance, 4.0F, 60.0F);
        cameraDistance = distance;
    }

    public static void setRidingAircraft(@Nullable MCH_EntityAircraft aircraft) {
        if (aircraft == null && ridingAircraft != null) {
            cameraDistance = DEF_THIRD_CAMERA_DIST;
            cameraZoom = 1.0F;
            cameraRoll = 0.0F;
        }
        ridingAircraft = aircraft;
    }

    @SubscribeEvent
    public void onRenderOverlay(Pre event) {
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }

        if (getRiddenAircraft(mc, player) == null) {
            return;
        }

        ElementType type = event.getType();
        if (type == ElementType.BOSSHEALTH ||
                type == ElementType.HEALTH ||
                type == ElementType.ARMOR ||
                type == ElementType.CROSSHAIRS ||
                type == ElementType.FOOD ||
                type == ElementType.BOSSINFO ||
                type == ElementType.VIGNETTE ||
                type == ElementType.AIR ||
                type == ElementType.HOTBAR

        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(Post event) {
        if (!DirectDrawable.shouldRender(event)) return;
        Tuple<EntityPlayer, MCH_EntityAircraft> ctx = getActivePilotContext();
        if (ctx == null) return;
        if (ctx.getSecond().getAcInfo() == null) return;
        ctx.getSecond().getAcInfo().getHudCache().forEach(drawable -> drawable.renderHud(event, ctx));
    }
}
