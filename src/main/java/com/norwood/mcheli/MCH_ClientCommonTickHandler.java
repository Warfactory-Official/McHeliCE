package com.norwood.mcheli;

import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.command.MCH_GuiTitle;
import com.norwood.mcheli.gltd.MCH_ClientGLTDTickHandler;
import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.gltd.MCH_GuiGLTD;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.helicopter.MCH_ClientHeliTickHandler;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helicopter.MCH_GuiHeli;
import com.norwood.mcheli.helper.client.MCH_CameraManager;
import com.norwood.mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import com.norwood.mcheli.lweapon.MCH_GuiLightWeapon;
import com.norwood.mcheli.mob.MCH_GuiSpawnGunner;
import com.norwood.mcheli.multiplay.MCH_GuiScoreboard;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.multiplay.MCH_MultiplayClient;
import com.norwood.mcheli.networking.packet.PacketOpenScreen;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.plane.MCP_ClientPlaneTickHandler;
import com.norwood.mcheli.plane.MCP_GuiPlane;
import com.norwood.mcheli.ship.MCH_ClientShipTickHandler;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.ship.MCH_GuiShip;
import com.norwood.mcheli.tank.MCH_ClientTankTickHandler;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.tank.MCH_GuiTank;
import com.norwood.mcheli.tool.MCH_ClientToolTickHandler;
import com.norwood.mcheli.tool.MCH_GuiWrench;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.tool.rangefinder.MCH_GuiRangeFinder;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.vehicle.MCH_ClientVehicleTickHandler;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.vehicle.MCH_GuiVehicle;
import com.norwood.mcheli.weapon.GPSPosition;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class MCH_ClientCommonTickHandler extends W_TickHandler {

    public static MCH_ClientCommonTickHandler instance;
    public static int cameraMode = 0;
    public static MCH_EntityAircraft ridingAircraft = null;
    public static boolean isDrawScoreboard = false;
    public static int sendLDCount = 0;
    public static boolean isLocked = false;
    public static int lockedSoundCount = 0;
    private static double prevMouseDeltaX;
    private static double prevMouseDeltaY;
    private static double mouseDeltaX = 0.0;
    private static double mouseDeltaY = 0.0;
    private static double mouseRollDeltaX = 0.0;
    private static double mouseRollDeltaY = 0.0;
    private static boolean isRideAircraft = false;
    private static float prevTick = 0.0F;
    public final MCH_GuiCommon gui_Common;
    public final MCH_Gui gui_Heli;
    public final MCH_Gui gui_Plane;
    public final MCH_Gui gui_Ship;
    public final MCH_Gui gui_Tank;
    public final MCH_Gui gui_GLTD;
    public final MCH_Gui gui_Vehicle;
    public final MCH_Gui gui_LWeapon;
    public final MCH_Gui gui_Wrench;
    public final MCH_Gui gui_EMarker;
    public final MCH_Gui gui_SwnGnr;
    public final MCH_Gui gui_RngFndr;
    public final MCH_Gui gui_Title;
    public final MCH_Gui[] guis;
    public final MCH_Gui[] guiTicks;
    public final MCH_ClientTickHandlerBase[] ticks;
    public MCH_Key[] Keys;
    public MCH_Key KeyCamDistUp;
    public MCH_Key KeyCamDistDown;
    public MCH_Key KeyScoreboard;
    public MCH_Key KeyMultiplayManager;
    int debugcnt;

    public MCH_ClientCommonTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.gui_Common = new MCH_GuiCommon(minecraft);
        this.gui_Heli = new MCH_GuiHeli(minecraft);
        this.gui_Plane = new MCP_GuiPlane(minecraft);
        this.gui_Ship = new MCH_GuiShip(minecraft);
        this.gui_Tank = new MCH_GuiTank(minecraft);
        this.gui_GLTD = new MCH_GuiGLTD(minecraft);
        this.gui_Vehicle = new MCH_GuiVehicle(minecraft);
        this.gui_LWeapon = new MCH_GuiLightWeapon(minecraft);
        this.gui_Wrench = new MCH_GuiWrench(minecraft);
        this.gui_SwnGnr = new MCH_GuiSpawnGunner(minecraft);
        this.gui_RngFndr = new MCH_GuiRangeFinder(minecraft);
        this.gui_EMarker = new MCH_GuiTargetMarker(minecraft);
        this.gui_Title = new MCH_GuiTitle(minecraft);
        this.guis = new MCH_Gui[] { this.gui_RngFndr, this.gui_LWeapon, this.gui_Heli, this.gui_Plane, this.gui_Ship,
                this.gui_Tank, this.gui_GLTD, this.gui_Vehicle };
        this.guiTicks = new MCH_Gui[] {
                this.gui_Common,
                this.gui_Heli,
                this.gui_Plane,
                this.gui_Ship,
                this.gui_Tank,
                this.gui_GLTD,
                this.gui_Vehicle,
                this.gui_LWeapon,
                this.gui_Wrench,
                this.gui_SwnGnr,
                this.gui_RngFndr,
                this.gui_EMarker,
                this.gui_Title
        };
        this.ticks = new MCH_ClientTickHandlerBase[] {
                new MCH_ClientHeliTickHandler(minecraft, config),
                new MCP_ClientPlaneTickHandler(minecraft, config),
                new MCH_ClientShipTickHandler(minecraft, config),
                new MCH_ClientTankTickHandler(minecraft, config),
                new MCH_ClientGLTDTickHandler(minecraft, config),
                new MCH_ClientVehicleTickHandler(minecraft, config),
                new MCH_ClientLightWeaponTickHandler(minecraft, config),
                new MCH_ClientSeatTickHandler(minecraft, config),
                new MCH_ClientToolTickHandler(minecraft, config)
        };
        this.updatekeybind(config);
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

    public void updatekeybind(MCH_Config config) {
        this.KeyCamDistUp = new MCH_Key(MCH_Config.KeyCameraDistUp.prmInt);
        this.KeyCamDistDown = new MCH_Key(MCH_Config.KeyCameraDistDown.prmInt);
        this.KeyScoreboard = new MCH_Key(MCH_Config.KeyScoreboard.prmInt);
        this.KeyMultiplayManager = new MCH_Key(MCH_Config.KeyMultiplayManager.prmInt);
        this.Keys = new MCH_Key[] { this.KeyCamDistUp, this.KeyCamDistDown, this.KeyScoreboard,
                this.KeyMultiplayManager };

        for (MCH_ClientTickHandlerBase t : this.ticks) {
            t.updateKeybind(config);
        }
    }

    public String getLabel() {
        return null;
    }

    public void onTick() {
        MCH_ClientTickHandlerBase.initRotLimit();

        for (MCH_Key k : this.Keys) {
            k.update();
        }

        EntityPlayer player = this.mc.player;
        if (player != null && this.mc.currentScreen == null) {
            if (MCH_ServerSettings.enableCamDistChange &&
                    (this.KeyCamDistUp.isKeyDown() || this.KeyCamDistDown.isKeyDown())) {
                int camdist = (int) W_Reflection.getThirdPersonDistance();
                if (this.KeyCamDistUp.isKeyDown() && camdist < 72) {
                    camdist += 4;
                    if (camdist > 72) {
                        camdist = 72;
                    }

                    W_Reflection.setThirdPersonDistance(camdist);
                } else if (this.KeyCamDistDown.isKeyDown()) {
                    camdist -= 4;
                    if (camdist < 4) {
                        camdist = 4;
                    }

                    W_Reflection.setThirdPersonDistance(camdist);
                }
            }

            if (this.mc.currentScreen == null && (!this.mc.isSingleplayer() || MCH_Config.DebugLog)) {
                isDrawScoreboard = this.KeyScoreboard.isKeyPress();
                if (!isDrawScoreboard && this.KeyMultiplayManager.isKeyDown()) {
                    PacketOpenScreen.send(5);
                }
            }
        }

        if (sendLDCount < 10) {
            sendLDCount++;
        } else {
            MCH_MultiplayClient.sendImageData();
            sendLDCount = 0;
        }

        boolean inOtherGui = this.mc.currentScreen != null;

        for (MCH_ClientTickHandlerBase t : this.ticks) {
            t.onTick(inOtherGui);
        }

        for (MCH_Gui g : this.guiTicks) {
            g.onTick();
        }

        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (player != null && ac != null && !ac.isDestroyed()) {
            if (isLocked && lockedSoundCount == 0) {
                isLocked = false;
                lockedSoundCount = 20;
                MCH_ClientTickHandlerBase.playSound("locked");
            }

            MCH_CameraManager.setRidingAircraft(ac);
        } else {
            lockedSoundCount = 0;
            isLocked = false;
            MCH_CameraManager.setRidingAircraft(ac);
        }

        if (lockedSoundCount > 0) {
            lockedSoundCount--;
        }

        // GPS
        if (mc.player.getRidingEntity() == null) {
            GPSPosition.currentClientGPSPosition.isActive = false;
        }
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
        if (this.mc.inGameHasFocus && Display.isActive() && this.mc.currentScreen == null) {
            if (stickMode) {
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
    }

    @Override
    public void onRenderTickPre(float partialTicks) {
        MCH_GuiTargetMarker.clearMarkEntityPos();
        if (!MCH_ServerSettings.enableDebugBoundingBox) {
            Minecraft.getMinecraft().getRenderManager().setDebugBoundingBox(false);
        }

        MCH_ClientEventHook.haveSearchLightAircraft.clear();
        if (this.mc != null && this.mc.world != null) {
            for (Object o : new ArrayList<>(Minecraft.getMinecraft().world.loadedEntityList)) {
                if (o instanceof MCH_EntityAircraft && ((MCH_EntityAircraft) o).haveSearchLight()) {
                    MCH_ClientEventHook.haveSearchLightAircraft.add((MCH_EntityAircraft) o);
                }
            }
        }

        if (!W_McClient.isGamePaused()) {
            EntityPlayer player = this.mc.player;
            if (player != null) {
                ItemStack currentItemstack = player.getHeldItem(EnumHand.MAIN_HAND);
                if (currentItemstack.getItem() instanceof MCH_ItemWrench && player.getItemInUseCount() > 0) {
                    W_Reflection.setItemRendererMainProgress(1.0F);
                }

                ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
                if (ridingAircraft != null) {
                    cameraMode = ridingAircraft.getCameraMode(player);
                } else if (player.getRidingEntity() instanceof MCH_EntityGLTD gltd) {
                    cameraMode = gltd.camera.getMode(0);
                } else {
                    cameraMode = 0;
                }

                MCH_EntityAircraft ac = null;
                if (player.getRidingEntity() instanceof MCH_EntityHeli ||
                        player.getRidingEntity() instanceof MCH_EntityPlane ||
                        player.getRidingEntity() instanceof MCH_EntityShip ||
                        player.getRidingEntity() instanceof MCH_EntityTank) {
                    ac = (MCH_EntityAircraft) player.getRidingEntity();
                } else if (player.getRidingEntity() instanceof MCH_EntityUavStation) {
                    ac = ((MCH_EntityUavStation) player.getRidingEntity()).getControlAircract();
                } else if (player.getRidingEntity() instanceof MCH_EntityVehicle vehicle) {
                    vehicle.setupAllRiderRenderPosition(partialTicks, player);
                }

                boolean stickMode = false;
                if (ac instanceof MCH_EntityHeli) {
                    stickMode = MCH_Config.MouseControlStickModeHeli.prmBool;
                }

                if (ac instanceof MCH_EntityPlane || ac instanceof MCH_EntityShip) { // do the stanky leg
                    stickMode = MCH_Config.MouseControlStickModePlane.prmBool;
                }

                for (int i = 0; i < 10 && prevTick > partialTicks; i++) {
                    prevTick--;
                }

                if (ac != null && ac.canMouseRot()) {
                    if (!isRideAircraft) {
                        ac.onInteractFirst(player);
                    }

                    isRideAircraft = true;
                    this.updateMouseDelta(stickMode, partialTicks);
                    boolean fixRot = false;
                    float fixYaw = 0.0F;
                    float fixPitch = 0.0F;
                    MCH_SeatInfo seatInfo = ac.getSeatInfo(player);
                    if (seatInfo != null && seatInfo.fixRot && ac.getIsGunnerMode(player) &&
                            !ac.isGunnerLookMode(player)) {
                        fixRot = true;
                        fixYaw = seatInfo.fixYaw;
                        fixPitch = seatInfo.fixPitch;
                        mouseRollDeltaX *= 0.0;
                        mouseRollDeltaY *= 0.0;
                        mouseDeltaX *= 0.0;
                        mouseDeltaY *= 0.0;
                    } else if (ac.isPilot(player)) {
                        MCH_AircraftInfo.CameraPosition cp = ac.getCameraPosInfo();
                        if (cp != null) {
                            fixYaw = cp.yaw;
                            fixPitch = cp.pitch;
                        }
                    }

                    if (ac.getAcInfo() == null) {
                        player.turn((float) mouseDeltaX, (float) mouseDeltaY);
                    } else {
                        ac.setAngles(
                                player,
                                fixRot,
                                fixYaw,
                                fixPitch,
                                (float) (mouseDeltaX + prevMouseDeltaX) / 2.0F,
                                (float) (mouseDeltaY + prevMouseDeltaY) / 2.0F,
                                (float) mouseRollDeltaX,
                                (float) mouseRollDeltaY,
                                partialTicks - prevTick);
                    }

                    ac.setupAllRiderRenderPosition(partialTicks, player);
                    double dist = MathHelper
                            .sqrt(mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY);
                    if (!stickMode || dist < getMaxStickLength() * 0.1) {
                        mouseRollDeltaX *= 0.95;
                        mouseRollDeltaY *= 0.95;
                    }

                    float roll = MathHelper.wrapDegrees(ac.getRotRoll());
                    float yaw = MathHelper.wrapDegrees(ac.getRotYaw() - player.rotationYaw);
                    roll *= MathHelper.cos((float) (yaw * Math.PI / 180.0));
                    if (ac.getTVMissile() != null && W_Lib.isClientPlayer(ac.getTVMissile().shootingEntity) &&
                            ac.getIsGunnerMode(player)) {
                        roll = 0.0F;
                    }

                    W_Reflection.setCameraRoll(roll);
                    this.correctViewEntityDummy(player);
                } else {
                    MCH_EntitySeat seat = player.getRidingEntity() instanceof MCH_EntitySeat ?
                            (MCH_EntitySeat) player.getRidingEntity() : null;
                    if (seat != null && seat.getParent() != null) {
                        this.updateMouseDelta(stickMode, partialTicks);
                        ac = seat.getParent();
                        boolean fixRotx = false;
                        MCH_SeatInfo seatInfox = ac.getSeatInfo(player);
                        if (seatInfox != null && seatInfox.fixRot && ac.getIsGunnerMode(player) &&
                                !ac.isGunnerLookMode(player)) {
                            fixRotx = true;
                            mouseRollDeltaX *= 0.0;
                            mouseRollDeltaY *= 0.0;
                            mouseDeltaX *= 0.0;
                            mouseDeltaY *= 0.0;
                        }

                        Vec3d v = new Vec3d(mouseDeltaX, mouseRollDeltaY, 0.0);
                        v = W_Vec3.rotateRoll((float) (ac.calcRotRoll(partialTicks) / 180.0F * Math.PI), v);
                        MCH_WeaponSet ws = ac.getCurrentWeapon(player);
                        mouseDeltaY = mouseDeltaY *
                                (ws != null && ws.getInfo() != null ? ws.getInfo().cameraRotationSpeedPitch : 1.0);
                        player.turn((float) mouseDeltaX, (float) mouseDeltaY);
                        float y = ac.getRotYaw();
                        float p = ac.getRotPitch();
                        float r = ac.getRotRoll();
                        ac.setRotYaw(ac.calcRotYaw(partialTicks));
                        ac.setRotPitch(ac.calcRotPitch(partialTicks));
                        ac.setRotRoll(ac.calcRotRoll(partialTicks));
                        float revRoll = 0.0F;
                        if (fixRotx) {
                            player.rotationYaw = ac.getRotYaw() + seatInfox.fixYaw;
                            player.rotationPitch = ac.getRotPitch() + seatInfox.fixPitch;
                            if (player.rotationPitch > 90.0F) {
                                player.prevRotationPitch = player.prevRotationPitch -
                                        (player.rotationPitch - 90.0F) * 2.0F;
                                player.rotationPitch = player.rotationPitch - (player.rotationPitch - 90.0F) * 2.0F;
                                player.prevRotationYaw += 180.0F;
                                player.rotationYaw += 180.0F;
                                revRoll = 180.0F;
                            } else if (player.rotationPitch < -90.0F) {
                                player.prevRotationPitch = player.prevRotationPitch -
                                        (player.rotationPitch - 90.0F) * 2.0F;
                                player.rotationPitch = player.rotationPitch - (player.rotationPitch - 90.0F) * 2.0F;
                                player.prevRotationYaw += 180.0F;
                                player.rotationYaw += 180.0F;
                                revRoll = 180.0F;
                            }
                        }

                        ac.setupAllRiderRenderPosition(partialTicks, player);
                        ac.setRotYaw(y);
                        ac.setRotPitch(p);
                        ac.setRotRoll(r);
                        mouseRollDeltaX *= 0.9;
                        mouseRollDeltaY *= 0.9;
                        float roll = MathHelper.wrapDegrees(ac.getRotRoll());
                        float yaw = MathHelper.wrapDegrees(ac.getRotYaw() - player.rotationYaw);
                        roll *= MathHelper.cos((float) (yaw * Math.PI / 180.0));
                        if (ac.getTVMissile() != null && W_Lib.isClientPlayer(ac.getTVMissile().shootingEntity) &&
                                ac.getIsGunnerMode(player)) {
                            roll = 0.0F;
                        }

                        W_Reflection.setCameraRoll(roll + revRoll);
                        this.correctViewEntityDummy(player);
                    } else {
                        if (isRideAircraft) {
                            W_Reflection.setCameraRoll(0.0F);
                            isRideAircraft = false;
                        }

                        mouseRollDeltaX = 0.0;
                        mouseRollDeltaY = 0.0;
                    }
                }

                if (ac != null) {
                    if (ac.getSeatIdByEntity(player) == 0 && !ac.isDestroyed()) {
                        ac.lastRiderYaw = player.rotationYaw;
                        ac.prevLastRiderYaw = player.prevRotationYaw;
                        ac.lastRiderPitch = player.rotationPitch;
                        ac.prevLastRiderPitch = player.prevRotationPitch;
                    }

                    ac.updateWeaponsRotation();
                }

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

                prevTick = partialTicks;
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
            if (!currentItemstack.isEmpty() && currentItemstack.getItem() instanceof MCH_ItemWrench &&
                    player.getItemInUseCount() > 0 && player.getActiveItemStack() != currentItemstack) {
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

        if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat ||
                this.mc.currentScreen.getClass().toString().contains("GuiDriveableController")) {
            for (MCH_Gui gui : this.guis) {
                if (this.drawGui(gui, partialTicks)) {
                    break;
                }
            }

            this.drawGui(this.gui_Common, partialTicks);
            this.drawGui(this.gui_Wrench, partialTicks);
            this.drawGui(this.gui_SwnGnr, partialTicks);
            this.drawGui(this.gui_EMarker, partialTicks);
            if (isDrawScoreboard) {
                MCH_GuiScoreboard.drawList(this.mc, this.mc.fontRenderer, false);
            }

            this.drawGui(this.gui_Title, partialTicks);
        }
    }

    public boolean drawGui(MCH_Gui gui, float partialTicks) {
        if (gui.isDrawGui(this.mc.player)) {
            gui.drawScreen(0, 0, partialTicks);
            return true;
        } else {
            return false;
        }
    }
}
