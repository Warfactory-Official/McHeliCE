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
import org.lwjgl.opengl.Display;

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
            case MCH_EntityPlane plane -> MCH_Config.MouseControlStickModePlane.prmBool;
            case MCH_EntityHeli heli -> MCH_Config.MouseControlStickModeHeli.prmBool;
            case null, default -> false;
        };
    }

    public void handleRenderTickPre(EntityPlayer player, float partialTicks) {
        ClientCommonTickHandler.ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        GuiTickHandler.handleWrenchUI(player);
        CameraHandler.cameraMode = getCameraMode(player);

        MCH_EntityAircraft aircraft = getControlledAircraft(player, partialTicks);
        boolean stickMode = isStickMode(aircraft);
        prevTickCorrection(partialTicks);

        long now = System.nanoTime();
        float deltaSeconds = (now - prevNanoTime) / 1_000_000_000.0F;
        prevNanoTime = now;

        if (aircraft != null && aircraft.canMouseRot()) {
            handleAircraftMouseControl(aircraft, player, stickMode, partialTicks, deltaSeconds);
        } else {
            handleSeatOrIdle(player, stickMode, partialTicks);
        }

        if (aircraft != null) {
            updateRiderLastPositions(aircraft, player);
        }
        updateViewEntityDummy(aircraft, player);
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

    private MCH_EntityAircraft getControlledAircraft(EntityPlayer player, float partialTicks) {
        return switch (player.getRidingEntity()) {
            case MCH_EntityHeli heli -> heli;
            case MCH_EntityPlane plane -> plane;
            case MCH_EntityShip ship -> ship;
            case MCH_EntityTank tank -> tank;
            case IUavStation uav -> uav.getControlled();
            case MCH_EntityVehicle vehicle -> {
                vehicle.setupAllRiderRenderPosition(partialTicks, player);
                yield null;
            }
            case null, default -> null;
        };
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
                !aircraft.isGunnerLookMode(player)) {
            fixRot = true;
            fixYaw = seatInfo.fixYaw;
            fixPitch = seatInfo.fixPitch;
            zeroMouseDeltas();
        } else if (aircraft.isPilot(player)) {
            MCH_AircraftInfo.CameraPosition cameraPosition = aircraft.getCameraPosInfo();
            if (cameraPosition != null) {
                fixYaw = cameraPosition.yaw();
                fixPitch = cameraPosition.pitch();
            }
        }

        applyMouseOrAircraftRotation(aircraft, player, fixRot, fixYaw, fixPitch, partialTicks, deltaSeconds);
        dampMouseRollIfNeeded(stickMode);
        updateCameraRoll(aircraft, player);
    }

    private void handleSeatOrIdle(EntityPlayer player, boolean stickMode, float partialTicks) {
        MCH_EntitySeat seat = player.getRidingEntity() instanceof MCH_EntitySeat ?
                (MCH_EntitySeat) player.getRidingEntity() : null;
        if (seat != null && seat.getParent() != null) {
            handleSeatMouseControl(seat, player, stickMode, partialTicks);
        } else if (isRideAircraft) {
            W_Reflection.setCameraRoll(0.0F);
            isRideAircraft = false;
            mouseRollDeltaX = 0.0;
            mouseRollDeltaY = 0.0;
        }
    }

    private void handleSeatMouseControl(MCH_EntitySeat seat, EntityPlayer player, boolean stickMode,
                                        float partialTicks) {
        updateMouseDelta(stickMode, partialTicks);
        MCH_EntityAircraft aircraft = seat.getParent();

        boolean fixRot = false;
        MCH_SeatInfo seatInfo = aircraft.getSeatInfo(player);
        if (seatInfo != null && seatInfo.fixRot && aircraft.getIsGunnerMode(player) &&
                !aircraft.isGunnerLookMode(player)) {
            fixRot = true;
            zeroMouseDeltas();
        }

        MCH_WeaponSet weaponSet = aircraft.getCurrentWeapon(player);
        mouseDeltaY *= weaponSet != null && weaponSet.getInfo() != null ?
                weaponSet.getInfo().cameraRotationSpeedPitch : 1.0;

        float yaw = aircraft.getYaw();
        float pitch = aircraft.getPitch();
        float roll = aircraft.getRoll();

        player.turn((float) mouseDeltaX, (float) mouseDeltaY);
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

        if (aircraft.getTVMissile() != null && W_Lib.isClientPlayer(aircraft.getTVMissile().shootingEntity) &&
                aircraft.getIsGunnerMode(player)) {
            roll = 0.0F;
        }

        W_Reflection.setCameraRoll(roll);
        correctViewEntityDummy(player);
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
            aircraft.lastRiderYaw = player.rotationYaw;
            aircraft.prevLastRiderYaw = player.prevRotationYaw;
            aircraft.lastRiderPitch = player.rotationPitch;
            aircraft.prevLastRiderPitch = player.prevRotationPitch;
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
