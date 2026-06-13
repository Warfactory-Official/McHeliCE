package com.norwood.mcheli.helper.client;

import com.norwood.mcheli.MCH_3rdCamera;
import com.norwood.mcheli.MCH_ViewEntityDummy;
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

import javax.annotation.Nullable;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.*;

@EventBusSubscriber(
        modid = "mcheli",
        value = {Side.CLIENT})
public class MCH_CameraManager {

    private static final float DEF_THIRD_CAMERA_DIST = 4.0F;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static float cameraRoll = 0.0F;
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
            GlStateManager.translate(0.0F, -f, 0.0F);
            if (ridingEntity.isOverridePlayerPitch()) {
                GlStateManager.rotate(ridingEntity.rotationPitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(ridingEntity.rotationYaw, 0.0F, 1.0F, 0.0F);
                event.setPitch(event.getPitch() - ridingEntity.rotationPitch);
                event.setYaw(event.getYaw() - ridingEntity.rotationYaw);
            } else {
                float partialTicks = (float) event.getRenderPartialTicks();
                            float roll = computeFreeLookCameraRoll(
                                    event.getYaw(),
                                    event.getPitch(),
                                    ridingEntity.calcRotYaw(partialTicks),
                                    ridingEntity.calcRotPitch(partialTicks),
                                     ridingEntity.calcRotRoll(partialTicks));
                     GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
            }

            GlStateManager.translate(0.0F, f, 0.0F);
        }
    }


    private static float computeFreeLookCameraRoll(float camYaw, float camPitch,
                                                       float acYaw, float acPitch, float acRoll) {
           double psi = Math.toRadians(camYaw);
           double pit = Math.toRadians(camPitch);
           double sinPsi = Math.sin(psi), cosPsi = Math.cos(psi);
           double sinPit = Math.sin(pit), cosPit = Math.cos(pit);

           double ax = sinPit * sinPsi, ay = cosPit, az = -sinPit * cosPsi;
           double bx = cosPsi, bz = sinPsi;

              double yr = Math.toRadians(acYaw), pr = Math.toRadians(acPitch), rr = Math.toRadians(acRoll);
           double sinR = Math.sin(rr), cosR = Math.cos(rr);
           double sinP = Math.sin(pr), cosP = Math.cos(pr);
           double sinY = Math.sin(yr), cosY = Math.cos(yr);
           double ux = -sinR * cosY - cosR * sinP * sinY;
           double uy = cosR * cosP;
           double uz = -sinR * sinY + cosR * sinP * cosY;

           double dotA = ax * ux + ay * uy + az * uz;
           double dotB = bx * ux + bz * uz;
           return (float) Math.toDegrees(Math.atan2(dotB, dotA));
       }

    @SubscribeEvent
    static void onFOVModifierEvent(FOVModifier event) {
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

    public static float getThirdPeasonCameraDistance() {
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

        Entity riding = player.getRidingEntity();
        boolean inVehicle = riding instanceof MCH_EntityAircraft ||
                riding instanceof MCH_EntitySeat seat && seat.getParent() instanceof MCH_EntityAircraft;
        if (!inVehicle) {
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
