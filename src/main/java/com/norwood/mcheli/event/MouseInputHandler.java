package com.norwood.mcheli.event;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.Display;

@Mod.EventBusSubscriber(
        modid = "mcheli",
        value = {Side.CLIENT})
public class MouseInputHandler {
    private static final double MAX_STICK_LENGTH = 40.0;
    private static double prevMouseDeltaX;
    private static double prevMouseDeltaY;
    private static double mouseDeltaX = 0.0;
    private static double mouseDeltaY = 0.0;
    private static double mouseRollDeltaX = 0.0;
    private static double mouseRollDeltaY = 0.0;

    private final Minecraft mc;
    private boolean isRideAircraft = false;
    private boolean limitedMountedLookActive = false;
    private int limitedMountedLookEntityId = Integer.MIN_VALUE;
    private int limitedMountedLookSeatId = Integer.MIN_VALUE;
    private float limitedMountedCurrentYaw = 0.0F;
    private float limitedMountedCurrentPitch = 0.0F;
    private float limitedMountedTargetYaw = 0.0F;
    private float limitedMountedTargetPitch = 0.0F;
    private float prevTick = 0.0F;
    private long prevNanoTime;

    public MouseInputHandler(Minecraft mc) {
        this.mc = mc;
    }

    public static double getCurrentStickX() {
        return mouseRollDeltaX;
    }

    public static double getCurrentStickY() {
        double inv = 1.0;
        if (Minecraft.getMinecraft().gameSettings.invertMouse) {
            inv = -inv;
        }

        if (MCH_Config.InvertMouse.prmBool) {
            inv = -inv;
        }

        return mouseRollDeltaY * inv;
    }

    public static double getMaxStickLength() {
        return MAX_STICK_LENGTH;
    }

    private static boolean isStickMode(MCH_EntityAircraft ac) {
        return switch (ac) {
            case MCH_EntityPlane _ -> MCH_Config.MouseControlStickModePlane.prmBool;
            case MCH_EntityHeli _ -> MCH_Config.MouseControlStickModeHeli.prmBool;
            case null, default -> false;
        };
    }
    public void handleRenderTickPre(EntityPlayer player, float partialTicks) {
        ClientCommonTickHandler.ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        GuiTickHandler.handleWrenchUI(player);
        CameraHandler.cameraMode = getCameraMode(player);

        MCH_EntityAircraft aircraft = getControlledAircraft(player);
        MCH_EntityVehicle vehicle = getDirectRiddenVehicle(player);
        boolean stickMode = isStickMode(aircraft);
        prevTickCorrection(partialTicks);

        long now = System.nanoTime();
        if (prevNanoTime == 0) prevNanoTime = now;
        float deltaSeconds = (now - prevNanoTime) / 1_000_000_000.0F;
        if (deltaSeconds > 0.1F) deltaSeconds = 0.1F;
        prevNanoTime = now;

        if (aircraft != null && aircraft.canMouseRot()) {
            handleAircraftMouseControl(aircraft, player, stickMode, partialTicks, deltaSeconds);
        } else if (vehicle != null && vehicle.canMouseRot()) {
            handleVehicleMouseControl(vehicle, player, partialTicks, deltaSeconds);
        } else {
            if (vehicle != null) {
                vehicle.setupAllRiderRenderPosition(partialTicks, player);
            }
            handleSeatOrIdle(player, stickMode, partialTicks, deltaSeconds);
        }

        if (aircraft != null) {
            updateRiderLastPositions(aircraft, player);
        } else if (vehicle != null) {
            updateRiderLastPositions(vehicle, player);
        }
        updateViewEntityDummy(aircraft != null ? aircraft : vehicle, player);
        prevTick = partialTicks;
    }

    private int getCameraMode(EntityPlayer player) {
        if (ClientCommonTickHandler.ridingAircraft != null) {
            return ClientCommonTickHandler.ridingAircraft.getCameraMode(player);
        }
        if (player.getRidingEntity() instanceof MCH_EntityGLTD gltd) {
            return gltd.camera.getMode(0);
        }
        return 0;
    }

    private MCH_EntityAircraft getControlledAircraft(EntityPlayer player) {
        return switch (player.getRidingEntity()) {
            case MCH_EntityHeli heli -> heli;
            case MCH_EntityPlane plane -> plane;
            case MCH_EntityShip ship -> ship;
            case MCH_EntityTank tank -> tank;
            case IUavStation uav -> uav.getControlled();
            case null, default -> null;
        };
    }

    private MCH_EntityVehicle getDirectRiddenVehicle(EntityPlayer player) {
        return player.getRidingEntity() instanceof MCH_EntityVehicle vehicle ? vehicle : null;
    }

    private void prevTickCorrection(float partialTicks) {
        for (int i = 0; i < 10 && prevTick > partialTicks; i++) {
            prevTick--;
        }
    }

    public void updateMouseDelta(boolean stickMode, float partialTicks) {
        prevMouseDeltaX = mouseDeltaX;
        prevMouseDeltaY = mouseDeltaY;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        if (!mc.inGameHasFocus || !Display.isActive() || mc.currentScreen != null) {
            return;
        }

        if (!stickMode) {
            if (Math.abs(mouseRollDeltaX) < getMaxStickLength() * 0.2) {
                mouseRollDeltaX *= 1.0F - 0.15F * partialTicks;
            }

            if (Math.abs(mouseRollDeltaY) < getMaxStickLength() * 0.2) {
                mouseRollDeltaY *= 1.0F - 0.15F * partialTicks;
            }
        }

        mc.mouseHelper.mouseXYChange();
        float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f2 = f1 * f1 * f1 * 8.0F;
        double ms = MCH_Config.MouseSensitivity.prmDouble * 0.1;
        mouseDeltaX = ms * mc.mouseHelper.deltaX * f2;
        mouseDeltaY = ms * mc.mouseHelper.deltaY * f2;
        byte inv = 1;
        if (mc.gameSettings.invertMouse) {
            inv = -1;
        }

        if (MCH_Config.InvertMouse.prmBool) {
            inv *= -1;
        }

        mouseRollDeltaX += mouseDeltaX;
        mouseRollDeltaY += mouseDeltaY * inv;
        double dist = mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY;
        if (dist > 1.0) {
            dist = MathHelper.sqrt(dist);
            double d = Math.min(dist, getMaxStickLength());

            mouseRollDeltaX /= dist;
            mouseRollDeltaY /= dist;
            mouseRollDeltaX *= d;
            mouseRollDeltaY *= d;
        }
    }

    private void handleAircraftMouseControl(MCH_EntityAircraft aircraft, EntityPlayer player, boolean stickMode,
                                            float partialTicks, float deltaSeconds) {
        if (!isRideAircraft) {
            aircraft.onInteractFirst(player);
        }
        isRideAircraft = true;

        updateMouseDelta(stickMode, partialTicks);

        boolean fixRot = false;
        float fixYaw = 0.0F;
        float fixPitch = 0.0F;
        MCH_SeatInfo seatInfo = aircraft.getSeatInfo(player);

        if (seatInfo != null && seatInfo.fixRot && aircraft.getIsGunnerMode(player) &&
                aircraft.isGunnerLookMode(player)) {
            fixRot = true;
            fixYaw = seatInfo.fixYaw;
            fixPitch = seatInfo.fixPitch;
            zeroMouseDeltas();
            clearDetachedMountedAim(aircraft);
        } else if (aircraft.isPilot(player)) {
            MCH_AircraftInfo.CameraPosition cameraPosition = aircraft.getCameraPosInfo();
            if (cameraPosition != null) {
                fixYaw = cameraPosition.yaw();
                fixPitch = cameraPosition.pitch();
            }
        }

        if (isLimitedMountedLookAircraft(aircraft)) {
            player.turn((float) mouseDeltaX, (float) mouseDeltaY);
            updateDetachedMountedAim(aircraft, player, deltaSeconds);
            if (aircraft instanceof MCH_EntityTank tank) {
                tank.updateAircraftOrientation(deltaSeconds);
            }
            aircraft.setupAllRiderRenderPosition(partialTicks, player);
            dampMouseRollIfNeeded(stickMode);
            updateCameraRoll(aircraft, player);
            return;
        }

        clearDetachedMountedAim(aircraft);
        applyMouseOrAircraftRotation(aircraft, player, fixRot, fixYaw, fixPitch, partialTicks, deltaSeconds);
        dampMouseRollIfNeeded(stickMode);
        updateCameraRoll(aircraft, player);
    }

    private void handleVehicleMouseControl(MCH_EntityVehicle vehicle, EntityPlayer player, float partialTicks,
                                           float deltaSeconds) {
        if (!isRideAircraft) {
            vehicle.onInteractFirst(player);
        }
        isRideAircraft = true;

        if (shouldLockVehicleSeatView(vehicle, player)) {
            lockPlayerToVehicleView(vehicle, player);
            vehicle.setupAllRiderRenderPosition(partialTicks, player);
            W_Reflection.setCameraRoll(0.0F);
            return;
        }

        updateMouseDelta(false, partialTicks);
        player.turn((float) mouseDeltaX, (float) mouseDeltaY);
        updateDetachedMountedAim(vehicle, player, deltaSeconds);
        vehicle.setupAllRiderRenderPosition(partialTicks, player);
    }

    private void handleSeatOrIdle(EntityPlayer player, boolean stickMode, float partialTicks, float deltaSeconds) {
        MCH_EntitySeat seat = player.getRidingEntity() instanceof MCH_EntitySeat ?
                (MCH_EntitySeat) player.getRidingEntity() : null;
        if (seat != null && seat.getParent() != null) {
            handleSeatMouseControl(seat, player, stickMode, partialTicks, deltaSeconds);
        } else if (isRideAircraft) {
            W_Reflection.setCameraRoll(0.0F);
            isRideAircraft = false;
            mouseRollDeltaX = 0.0;
            mouseRollDeltaY = 0.0;
            resetLimitedMountedLook();
        }
    }

    private void handleSeatMouseControl(MCH_EntitySeat seat, EntityPlayer player, boolean stickMode,
                                        float partialTicks, float deltaSeconds) {
        updateMouseDelta(stickMode, partialTicks);
        MCH_EntityAircraft aircraft = seat.getParent();

        boolean fixRot = false;
        assert aircraft != null;
        MCH_SeatInfo seatInfo = aircraft.getSeatInfo(player);
        if (seatInfo != null && seatInfo.fixRot && aircraft.getIsGunnerMode(player) &&
                aircraft.isGunnerLookMode(player)) {
            fixRot = true;
            zeroMouseDeltas();
            clearDetachedMountedAim(aircraft);
        }

        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        if (aircraft instanceof MCH_EntityVehicle vehicle && shouldLockVehicleSeatView(vehicle, player)) {
            lockPlayerToVehicleView(vehicle, player);
            aircraft.setupAllRiderRenderPosition(partialTicks, player);
            W_Reflection.setCameraRoll(0.0F);
            return;
        }

        boolean limitedMountedLook = isLimitedMountedLookAircraft(aircraft);
        if (!limitedMountedLook) {
            clearDetachedMountedAim(aircraft);
            mouseDeltaY *= weaponSet != null && weaponSet.getInfo() != null ?
                    weaponSet.getInfo().cameraRotationSpeedPitch : 1.0;
        } else {
            player.turn((float) mouseDeltaX, (float) mouseDeltaY);
            updateDetachedMountedAim(aircraft, player, deltaSeconds);
        }

        float yaw = aircraft.getYaw();
        float pitch = aircraft.getPitch();
        float roll = aircraft.getRoll();

        if (!limitedMountedLook) {
            player.turn((float) mouseDeltaX, (float) mouseDeltaY);
        }
        aircraft.setRotYaw(aircraft.calcRotYaw(partialTicks));
        aircraft.setRotPitch(aircraft.calcRotPitch(partialTicks));
        aircraft.setRotRoll(aircraft.calcRotRoll(partialTicks));

        if (fixRot) {
            fixPlayerRotation(aircraft, seatInfo, player);
        }

        aircraft.setupAllRiderRenderPosition(partialTicks, player);
        aircraft.setRotYaw(yaw);
        aircraft.setRotPitch(pitch);
        aircraft.setRotRoll(roll);

        mouseRollDeltaX *= 0.9;
        mouseRollDeltaY *= 0.9;
        updateCameraRoll(aircraft, player);
    }

    private boolean isLimitedMountedLookAircraft(MCH_EntityAircraft aircraft) {
        return aircraft instanceof MCH_EntityTank || aircraft instanceof MCH_EntityVehicle;
    }

    private void resetLimitedMountedLook() {
        limitedMountedLookActive = false;
        limitedMountedLookEntityId = Integer.MIN_VALUE;
        limitedMountedLookSeatId = Integer.MIN_VALUE;
        limitedMountedTargetYaw = 0.0F;
        limitedMountedTargetPitch = 0.0F;
    }

    private void updateDetachedMountedAim(MCH_EntityAircraft aircraft, EntityPlayer player, float deltaSeconds) {
        float yawSpeed = getMountedLookYawSpeed(aircraft);
        float pitchSpeed = getMountedLookPitchSpeed(aircraft, player, yawSpeed);
        if (yawSpeed <= 0.0F || pitchSpeed < 0.0F) {
            clearDetachedMountedAim(aircraft);
            return;
        }

        syncLimitedMountedLookState(aircraft, player);
        limitedMountedTargetYaw = player.rotationYaw;
        limitedMountedTargetPitch = player.rotationPitch;

        float maxYawStep = Math.max(0.0F, yawSpeed * deltaSeconds);
        float maxPitchStep = Math.max(0.0F, pitchSpeed * deltaSeconds);
        float yawStep = clampWrappedDelta(limitedMountedCurrentYaw, limitedMountedTargetYaw, maxYawStep);
        float pitchStep = clampLinearDelta(limitedMountedCurrentPitch, limitedMountedTargetPitch, maxPitchStep);

        float prevYaw = limitedMountedCurrentYaw;
        float prevPitch = limitedMountedCurrentPitch;
        limitedMountedCurrentYaw = MathHelper.wrapDegrees(limitedMountedCurrentYaw + yawStep);
        limitedMountedCurrentPitch = MathHelper.clamp(limitedMountedCurrentPitch + pitchStep, -90.0F, 90.0F);
        aircraft.setDetachedWeaponAim(prevYaw, limitedMountedCurrentYaw, prevPitch, limitedMountedCurrentPitch);
    }

    private void syncLimitedMountedLookState(MCH_EntityAircraft aircraft, EntityPlayer player) {
        int entityId = aircraft.getEntityId();
        int seatId = aircraft.getSeatIdByEntity(player);
        if (!limitedMountedLookActive || limitedMountedLookEntityId != entityId || limitedMountedLookSeatId != seatId) {
            limitedMountedLookActive = true;
            limitedMountedLookEntityId = entityId;
            limitedMountedLookSeatId = seatId;
            limitedMountedCurrentYaw = player.rotationYaw;
            limitedMountedCurrentPitch = player.rotationPitch;
            limitedMountedTargetYaw = player.rotationYaw;
            limitedMountedTargetPitch = player.rotationPitch;
            aircraft.setDetachedWeaponAim(player.prevRotationYaw, player.rotationYaw, player.prevRotationPitch,
                    player.rotationPitch);
        }
    }

    private float getMountedLookYawSpeed(MCH_EntityAircraft aircraft) {
        return aircraft.getAcInfo() != null ? Math.max(0.0F, aircraft.getAcInfo().cameraRotationSpeed) : 0.0F;
    }

    private float getMountedLookPitchSpeed(MCH_EntityAircraft aircraft, EntityPlayer player, float yawSpeed) {
        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        if (weaponSet != null && weaponSet.getInfo() != null) {
            return yawSpeed * weaponSet.getInfo().cameraRotationSpeedPitch;
        }
        return yawSpeed;
    }

    private float clampWrappedDelta(float current, float target, float maxStep) {
        return MathHelper.clamp(MathHelper.wrapDegrees(target - current), -maxStep, maxStep);
    }

    private float clampLinearDelta(float current, float target, float maxStep) {
        return MathHelper.clamp(target - current, -maxStep, maxStep);
    }

    private void clearDetachedMountedAim(MCH_EntityAircraft aircraft) {
        aircraft.clearDetachedWeaponAim();
        resetLimitedMountedLook();
    }

    private void zeroMouseDeltas() {
        mouseRollDeltaX = 0.0;
        mouseRollDeltaY = 0.0;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    private void applyMouseOrAircraftRotation(MCH_EntityAircraft aircraft, EntityPlayer player, boolean fixRot,
                                              float fixYaw, float fixPitch, float partialTicks,
                                              float deltaSeconds) {
        if (aircraft.getAcInfo() == null) {
            player.turn((float) mouseDeltaX, (float) mouseDeltaY);
        } else {
            aircraft.setAngles(
                    player,
                    fixRot,
                    fixYaw,
                    fixPitch,
                    (float) (mouseDeltaX + prevMouseDeltaX) / 2.0F,
                    (float) (mouseDeltaY + prevMouseDeltaY) / 2.0F,
                    (float) mouseRollDeltaX,
                    (float) mouseRollDeltaY,
                    deltaSeconds
            );
        }
        aircraft.setupAllRiderRenderPosition(partialTicks, player);
    }

    private void dampMouseRollIfNeeded(boolean stickMode) {
        double dist = MathHelper.sqrt(mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY);
        if (!stickMode || dist < getMaxStickLength() * 0.1) {
            mouseRollDeltaX *= 0.95;
            mouseRollDeltaY *= 0.95;
        }
    }

    private void updateCameraRoll(MCH_EntityAircraft aircraft, EntityPlayer player) {
        float roll = MathHelper.wrapDegrees(aircraft.getRoll());
        float yaw = MathHelper.wrapDegrees(aircraft.getYaw() - player.rotationYaw);
        roll *= MathHelper.cos((float) (yaw * Math.PI / 180.0));

        if ((aircraft instanceof MCH_EntityTank || aircraft instanceof MCH_EntityVehicle) &&
                player.getRidingEntity() instanceof MCH_EntitySeat &&
                !aircraft.getIsGunnerMode(player)) {
            roll = 0.0F;
        }

        if (aircraft.getTVMissile() != null && W_Lib.isClientPlayer(aircraft.getTVMissile().shootingEntity) &&
                aircraft.getIsGunnerMode(player)) {
            roll = 0.0F;
        }

        W_Reflection.setCameraRoll(roll);
        correctViewEntityDummy(player);
    }

    private boolean shouldLockVehicleSeatView(MCH_EntityVehicle vehicle, EntityPlayer player) {
        return !vehicle.canSwitchFreeLook() &&
                !vehicle.getIsGunnerMode(player) &&
                vehicle.getCurrentWeaponID(player) < 0;
    }

    private void lockPlayerToVehicleView(MCH_EntityVehicle vehicle, EntityPlayer player) {
        player.prevRotationYaw = player.rotationYaw = vehicle.getYaw();
        player.prevRotationPitch = player.rotationPitch = vehicle.getPitch();
    }

    private void fixPlayerRotation(MCH_EntityAircraft aircraft, MCH_SeatInfo seatInfo, EntityPlayer player) {
        player.rotationYaw = aircraft.getYaw() + seatInfo.fixYaw;
        player.rotationPitch = aircraft.getPitch() + seatInfo.fixPitch;

        if (player.rotationPitch > 90.0F) {
            player.prevRotationPitch -= (player.rotationPitch - 90.0F) * 2.0F;
            player.rotationPitch -= (player.rotationPitch - 90.0F) * 2.0F;
            player.prevRotationYaw += 180.0F;
            player.rotationYaw += 180.0F;
        } else if (player.rotationPitch < -90.0F) {
            player.prevRotationPitch -= (player.rotationPitch - 90.0F) * 2.0F;
            player.rotationPitch -= (player.rotationPitch - 90.0F) * 2.0F;
            player.prevRotationYaw += 180.0F;
            player.rotationYaw += 180.0F;
        }
    }

    private void updateRiderLastPositions(MCH_EntityAircraft aircraft, EntityPlayer player) {
        if (aircraft.getSeatIdByEntity(player) == 0 && !aircraft.isDestroyed()) {
            if (aircraft.isDetachedWeaponAimActive()) {
                aircraft.lastRiderYaw = aircraft.getDetachedWeaponAimYaw();
                aircraft.prevLastRiderYaw = aircraft.getPrevDetachedWeaponAimYaw();
                aircraft.lastRiderPitch = aircraft.getDetachedWeaponAimPitch();
                aircraft.prevLastRiderPitch = aircraft.getPrevDetachedWeaponAimPitch();
            } else {
                aircraft.lastRiderYaw = player.rotationYaw;
                aircraft.prevLastRiderYaw = player.prevRotationYaw;
                aircraft.lastRiderPitch = player.rotationPitch;
                aircraft.prevLastRiderPitch = player.prevRotationPitch;
            }
        }
        aircraft.updateWeaponsRotation();
    }

    private void updateViewEntityDummy(MCH_EntityAircraft aircraft, EntityPlayer player) {
        if (aircraft != null || player.getRidingEntity() instanceof MCH_EntityGLTD ||
                player.getRidingEntity() instanceof IUavStation) {
            Entity viewEntity = MCH_ViewEntityDummy.getInstance(player.world);
            if (viewEntity != null) {
                viewEntity.rotationYaw = player.rotationYaw;
                viewEntity.prevRotationYaw = player.prevRotationYaw;
                if (aircraft != null) {
                    MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
                    if (weaponSet != null && weaponSet.getInfo() != null && weaponSet.getInfo().fixCameraPitch) {
                        viewEntity.rotationPitch = viewEntity.prevRotationPitch = 0.0F;
                    }
                }
            }
        }
    }

    private void correctViewEntityDummy(Entity entity) {
        Entity viewEntity = MCH_ViewEntityDummy.getInstance(entity.world);
        if (viewEntity != null) {
            if (viewEntity.rotationYaw - viewEntity.prevRotationYaw > 180.0F) {
                viewEntity.prevRotationYaw += 360.0F;
            } else if (viewEntity.rotationYaw - viewEntity.prevRotationYaw < -180.0F) {
                viewEntity.prevRotationYaw -= 360.0F;
            }
        }
    }
}
