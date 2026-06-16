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
            GlStateManager.rotate(cameraRoll, 0.0F, 0.0F, 1.0F);
            // Compose the airframe pitch/yaw with the player's view as
            // Rx(hullPitch)*Ry(hullYaw)*Rx(viewPitch-hullPitch)*Ry(viewYaw-hullYaw), i.e. the
            // airframe orientation followed by the view offset relative to it. This is what tilts
            // the camera with the airframe (e.g. a plane diving/ascending, where the relative view
            // pitch is held near zero so only the hull pitch tilts the view).
            //
            // It is only valid while the view is an airframe-relative offset. When an independent
            // mounted weapon is being aimed (MouseInputHandler drives it as an absolute world aim
            // via player.turn), the view pitch is large and unrelated to the hull; composing it
            // here sandwiches the pitch between the two yaw rotations and gimbal-locks it relative
            // to the hull heading (correct facing south, rolls east/west, inverts north). In that
            // case let vanilla render the absolute view directly, matching the freelook path.
            if (ridingEntity.isOverridePlayerPitch() && !ridingEntity.hasIndependentMountedAim(mc.player)) {
                GlStateManager.rotate(ridingEntity.rotationPitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(ridingEntity.rotationYaw, 0.0F, 1.0F, 0.0F);
                event.setPitch(event.getPitch() - ridingEntity.rotationPitch);
                event.setYaw(event.getYaw() - ridingEntity.rotationYaw);
            }

            GlStateManager.translate(0.0F, f, 0.0F);
        }
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
