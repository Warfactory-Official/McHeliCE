package com.norwood.mcheli.event;

import com.norwood.mcheli.MCH_ClientEventHook;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_ServerSettings;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.gltd.MCH_ClientGLTDTickHandler;
import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.helicopter.MCH_ClientHeliTickHandler;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.plane.MCP_ClientPlaneTickHandler;
import com.norwood.mcheli.ship.MCH_ClientShipTickHandler;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.tank.MCH_ClientTankTickHandler;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.tool.MCH_ClientToolTickHandler;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.vehicle.MCH_ClientVehicleTickHandler;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.weapon.GPSPosition;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_Reflection;
import com.norwood.mcheli.wrapper.W_TickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;
import java.util.Arrays;

import static com.norwood.mcheli.event.ImageTransferHandler.handleImageDataSending;

@SideOnly(Side.CLIENT)
public class ClientCommonTickHandler extends W_TickHandler {


    public static ClientCommonTickHandler instance;
    public static MCH_EntityAircraft ridingAircraft = null;
    public static boolean isDrawScoreboard = false;
    private static double prevMouseDeltaX;
    private static double prevMouseDeltaY;
    private static double mouseDeltaX = 0.0;
    private static double mouseDeltaY = 0.0;
    private static double mouseRollDeltaX = 0.0;
    private static double mouseRollDeltaY = 0.0;
    private static boolean isRideAircraft = false;
    private static float prevTick = 0.0F;
    public final MCH_ClientTickHandlerBase[] ticks;
    public final KeyboardInputHandler kbInput;
    public final GuiTickHandler guiTickHandler;
    public final CameraHandler cameraHandler;


    public ClientCommonTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.ticks = new MCH_ClientTickHandlerBase[]{new MCH_ClientHeliTickHandler(minecraft, config), new MCP_ClientPlaneTickHandler(minecraft, config), new MCH_ClientShipTickHandler(minecraft, config), new MCH_ClientTankTickHandler(minecraft, config), new MCH_ClientGLTDTickHandler(minecraft, config), new MCH_ClientVehicleTickHandler(minecraft, config), new MCH_ClientLightWeaponTickHandler(minecraft, config), new MCH_ClientSeatTickHandler(minecraft, config), new MCH_ClientToolTickHandler(minecraft, config)};
        this.guiTickHandler = new GuiTickHandler(this);
        this.kbInput = new KeyboardInputHandler(this);
        this.cameraHandler = new CameraHandler(this);

        kbInput.updateKeybind(config);
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
        return 40.0;
    }

    private static boolean isStickMode(MCH_EntityAircraft ac) {
        return switch (ac) {
            case MCH_EntityPlane m -> MCH_Config.MouseControlStickModePlane.prmBool;
            case MCH_EntityHeli m -> MCH_Config.MouseControlStickModeHeli.prmBool;
            case null, default -> false;
        };
    }

    private void handleGps(EntityPlayer player) {
        if (player != null && player.getRidingEntity() == null) {
            GPSPosition.currentClientGPSPosition.isActive = false;
        }
    }


    private void handleTickHandlers(boolean inOtherGui) {
        for (MCH_ClientTickHandlerBase tick : ticks) {
            tick.onTick(inOtherGui);
        }
        Arrays.stream(guiTickHandler.guiTicks).forEach(MCH_Gui::onTick);
    }


    @Override
    public void onTickPre() {
        if (this.mc.player != null && this.mc.world != null) {
            this.onTick();
        }
    }

    @Override
    public void onTickPost() {
        if (this.mc.player != null && this.mc.world != null) {
            MCH_GuiTargetMarker.onClientTick();
        }
    }


    public void updateMouseDelta(boolean stickMode, float partialTicks) {
        prevMouseDeltaX = mouseDeltaX;
        prevMouseDeltaY = mouseDeltaY;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        if (!this.mc.inGameHasFocus || !Display.isActive() || this.mc.currentScreen != null) return;

        if (!stickMode) {
            if (Math.abs(mouseRollDeltaX) < getMaxStickLength() * 0.2) {
                mouseRollDeltaX *= 1.0F - 0.15F * partialTicks;
            }

            if (Math.abs(mouseRollDeltaY) < getMaxStickLength() * 0.2) {
                mouseRollDeltaY *= 1.0F - 0.15F * partialTicks;
            }
        }

        this.mc.mouseHelper.mouseXYChange();
        float f1 = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f2 = f1 * f1 * f1 * 8.0F;
        double ms = MCH_Config.MouseSensitivity.prmDouble * 0.1;
        mouseDeltaX = ms * this.mc.mouseHelper.deltaX * f2;
        mouseDeltaY = ms * this.mc.mouseHelper.deltaY * f2;
        byte inv = 1;
        if (this.mc.gameSettings.invertMouse) {
            inv = -1;
        }

        if (MCH_Config.InvertMouse.prmBool) {
            inv *= -1;
        }

        mouseRollDeltaX = mouseRollDeltaX + mouseDeltaX;
        mouseRollDeltaY = mouseRollDeltaY + mouseDeltaY * inv;
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

    public void onTick() {
        MCH_ClientTickHandlerBase.initRotLimit();

        kbInput.updateKeys();

        EntityPlayer player = mc.player;
        boolean inGui = mc.currentScreen != null;

        if (player != null && !inGui) {
            kbInput.handleCameraDistance();
            kbInput.handleScoreboardAndMultiplayer();
        }

        handleImageDataSending();
        handleTickHandlers(inGui);
        cameraHandler.handleAircraftCamera(player);
        handleGps(player);
    }

    @Override
    public void onRenderTickPre(float partialTicks) {
        clearMarkersAndDebug();
        updateSearchLightAircraftList();

        EntityPlayer player = mc.player;
        if (W_McClient.isGamePaused() || player == null) return;

        updateRidingAircraft(player);
        CameraHandler.cameraMode = getCameraMode(player);

        MCH_EntityAircraft ac = getControlledAircraft(player, partialTicks);
        boolean stickMode = isStickMode(ac);
        prevTickCorrection(partialTicks);

        if (ac != null && ac.canMouseRot()) {
            handleAircraftMouseControl(ac, player, stickMode, partialTicks);
        } else {
            handleSeatOrIdle(player, stickMode, partialTicks);
        }

        if (ac != null) updateRiderLastPositions(ac, player);
        updateViewEntityDummy(ac, player);

        prevTick = partialTicks;
    }

// --- Helper Methods ---

    private void clearMarkersAndDebug() {
        MCH_GuiTargetMarker.clearMarkEntityPos();
        if (!MCH_ServerSettings.enableDebugBoundingBox) {
            Minecraft.getMinecraft().getRenderManager().setDebugBoundingBox(false);
        }
    }

    private void updateSearchLightAircraftList() {
        MCH_ClientEventHook.haveSearchLightAircraft.clear();
        if (mc != null && mc.world != null) {
            for (Object o : new ArrayList<>(mc.world.loadedEntityList)) {
                if (o instanceof MCH_EntityAircraft ac && ac.haveSearchLight()) {
                    MCH_ClientEventHook.haveSearchLightAircraft.add(ac);
                }
            }
        }
    }

    private void updateRidingAircraft(EntityPlayer player) {
        ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        GuiTickHandler.handleWrenchUI(player);
    }

    private int getCameraMode(EntityPlayer player) {
        if (ridingAircraft != null) return ridingAircraft.getCameraMode(player);
        if (player.getRidingEntity() instanceof MCH_EntityGLTD gltd) return gltd.camera.getMode(0);
        return 0;
    }

    private MCH_EntityAircraft getControlledAircraft(EntityPlayer player, float partialTicks) {
        return switch (player.getRidingEntity()) {
            case MCH_EntityHeli heli -> heli;
            case MCH_EntityPlane plane -> plane;
            case MCH_EntityShip ship -> ship;
            case MCH_EntityTank tank -> tank;
            case MCH_EntityUavStation uav -> uav.getControlAircract();
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

    private void handleAircraftMouseControl(MCH_EntityAircraft ac, EntityPlayer player, boolean stickMode, float partialTicks) {
        if (!isRideAircraft) ac.onInteractFirst(player);
        isRideAircraft = true;

        updateMouseDelta(stickMode, partialTicks);

        boolean fixRot = false;
        float fixYaw = 0.0F;
        float fixPitch = 0.0F;
        MCH_SeatInfo seatInfo = ac.getSeatInfo(player);

        if (seatInfo != null && seatInfo.fixRot && ac.getIsGunnerMode(player) && !ac.isGunnerLookMode(player)) {
            fixRot = true;
            fixYaw = seatInfo.fixYaw;
            fixPitch = seatInfo.fixPitch;
            zeroMouseDeltas();
        } else if (ac.isPilot(player)) {
            MCH_AircraftInfo.CameraPosition cp = ac.getCameraPosInfo();
            if (cp != null) {
                fixYaw = cp.yaw;
                fixPitch = cp.pitch;
            }
        }

        applyMouseOrAircraftRotation(ac, player, fixRot, fixYaw, fixPitch, partialTicks);
        dampMouseRollIfNeeded(ac, stickMode);
        updateCameraRoll(ac, player);
    }

    private void handleSeatOrIdle(EntityPlayer player, boolean stickMode, float partialTicks) {
        MCH_EntitySeat seat = player.getRidingEntity() instanceof MCH_EntitySeat ? (MCH_EntitySeat) player.getRidingEntity() : null;
        if (seat != null && seat.getParent() != null) {
            handleSeatMouseControl(seat, player, stickMode, partialTicks);
        } else if (isRideAircraft) {
            W_Reflection.setCameraRoll(0.0F);
            isRideAircraft = false;
            mouseRollDeltaX = 0.0;
            mouseRollDeltaY = 0.0;
        }
    }

    private void handleSeatMouseControl(MCH_EntitySeat seat, EntityPlayer player, boolean stickMode, float partialTicks) {
        updateMouseDelta(stickMode, partialTicks);
        MCH_EntityAircraft ac = seat.getParent();

        boolean fixRotx = false;
        MCH_SeatInfo seatInfox = ac.getSeatInfo(player);
        if (seatInfox != null && seatInfox.fixRot && ac.getIsGunnerMode(player) && !ac.isGunnerLookMode(player)) {
            fixRotx = true;
            zeroMouseDeltas();
        }

        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
        mouseDeltaY *= ws != null && ws.getInfo() != null ? ws.getInfo().cameraRotationSpeedPitch : 1.0;

        float y = ac.getRotYaw();
        float p = ac.getRotPitch();
        float r = ac.getRotRoll();

        player.turn((float) mouseDeltaX, (float) mouseDeltaY);
        ac.setRotYaw(ac.calcRotYaw(partialTicks));
        ac.setRotPitch(ac.calcRotPitch(partialTicks));
        ac.setRotRoll(ac.calcRotRoll(partialTicks));

        if (fixRotx) fixPlayerRotation(ac, seatInfox, player);

        ac.setupAllRiderRenderPosition(partialTicks, player);
        ac.setRotYaw(y);
        ac.setRotPitch(p);
        ac.setRotRoll(r);

        mouseRollDeltaX *= 0.9;
        mouseRollDeltaY *= 0.9;
        updateCameraRoll(ac, player);
    }

    private void zeroMouseDeltas() {
        mouseRollDeltaX = 0.0;
        mouseRollDeltaY = 0.0;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    private void applyMouseOrAircraftRotation(MCH_EntityAircraft ac, EntityPlayer player, boolean fixRot, float fixYaw, float fixPitch, float partialTicks) {
        if (ac.getAcInfo() == null) {
            player.turn((float) mouseDeltaX, (float) mouseDeltaY);
        } else {
            ac.setAngles(player, fixRot, fixYaw, fixPitch,
                    (float) (mouseDeltaX + prevMouseDeltaX) / 2.0F,
                    (float) (mouseDeltaY + prevMouseDeltaY) / 2.0F,
                    (float) mouseRollDeltaX,
                    (float) mouseRollDeltaY,
                    partialTicks - prevTick);
        }
        ac.setupAllRiderRenderPosition(partialTicks, player);
    }

    private void dampMouseRollIfNeeded(MCH_EntityAircraft ac, boolean stickMode) {
        double dist = MathHelper.sqrt(mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY);
        if (!stickMode || dist < getMaxStickLength() * 0.1) {
            mouseRollDeltaX *= 0.95;
            mouseRollDeltaY *= 0.95;
        }
    }

    private void updateCameraRoll(MCH_EntityAircraft ac, EntityPlayer player) {
        float roll = MathHelper.wrapDegrees(ac.getRotRoll());
        float yaw = MathHelper.wrapDegrees(ac.getRotYaw() - player.rotationYaw);
        roll *= MathHelper.cos((float) (yaw * Math.PI / 180.0));

        if (ac.getTVMissile() != null && W_Lib.isClientPlayer(ac.getTVMissile().shootingEntity) && ac.getIsGunnerMode(player)) {
            roll = 0.0F;
        }

        W_Reflection.setCameraRoll(roll);
        correctViewEntityDummy(player);
    }

    private void fixPlayerRotation(MCH_EntityAircraft ac, MCH_SeatInfo seatInfo, EntityPlayer player) {
        player.rotationYaw = ac.getRotYaw() + seatInfo.fixYaw;
        player.rotationPitch = ac.getRotPitch() + seatInfo.fixPitch;

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

    private void updateRiderLastPositions(MCH_EntityAircraft ac, EntityPlayer player) {
        if (ac.getSeatIdByEntity(player) == 0 && !ac.isDestroyed()) {
            ac.lastRiderYaw = player.rotationYaw;
            ac.prevLastRiderYaw = player.prevRotationYaw;
            ac.lastRiderPitch = player.rotationPitch;
            ac.prevLastRiderPitch = player.prevRotationPitch;
        }
        ac.updateWeaponsRotation();
    }

    private void updateViewEntityDummy(MCH_EntityAircraft ac, EntityPlayer player) {
        Entity de = MCH_ViewEntityDummy.getInstance(player.world);
        if (de != null) {
            de.rotationYaw = player.rotationYaw;
            de.prevRotationYaw = player.prevRotationYaw;
            if (ac != null) {
                MCH_WeaponSet wi = ac.getCurrentWeapon(player);
                if (wi != null && wi.getInfo() != null && wi.getInfo().fixCameraPitch) {
                    de.rotationPitch = de.prevRotationPitch = 0.0F;
                }
            }
        }
    }


    public void correctViewEntityDummy(Entity entity) {
        Entity de = MCH_ViewEntityDummy.getInstance(entity.world);
        if (de != null) {
            if (de.rotationYaw - de.prevRotationYaw > 180.0F) {
                de.prevRotationYaw += 360.0F;
            } else if (de.rotationYaw - de.prevRotationYaw < -180.0F) {
                de.prevRotationYaw -= 360.0F;
            }
        }
    }

    @Override
    public void onPlayerTickPre(EntityPlayer player) {
        if (player.world.isRemote) {
            ItemStack currentItemstack = player.getHeldItem(EnumHand.MAIN_HAND);
            if (!currentItemstack.isEmpty() && currentItemstack.getItem() instanceof MCH_ItemWrench && player.getItemInUseCount() > 0 && player.getActiveItemStack() != currentItemstack) {
                int maxdm = currentItemstack.getMaxDamage();
                int dm = currentItemstack.getMetadata();
                if (dm <= maxdm && dm > 0) {
                    player.setActiveHand(EnumHand.MAIN_HAND);
                }
            }
        }
    }

    @Override
    public void onRenderTickPost(float partialTicks) {
        if (this.mc.player != null) {
            MCH_ClientTickHandlerBase.applyRotLimit(this.mc.player);
            Entity e = MCH_ViewEntityDummy.getInstance(this.mc.player.world);
            if (e != null) {
                e.rotationPitch = this.mc.player.rotationPitch;
                e.rotationYaw = this.mc.player.rotationYaw;
                e.prevRotationPitch = this.mc.player.prevRotationPitch;
                e.prevRotationYaw = this.mc.player.prevRotationYaw;
            }
        }
        guiTickHandler.handleGui(partialTicks);
    }

}
