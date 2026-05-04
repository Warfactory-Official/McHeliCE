package com.norwood.mcheli.event;

import com.norwood.mcheli.*;
import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.gltd.MCH_ClientGLTDTickHandler;
import com.norwood.mcheli.gui.MCH_Gui;
import com.norwood.mcheli.helicopter.MCH_ClientHeliTickHandler;
import com.norwood.mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.plane.MCP_ClientPlaneTickHandler;
import com.norwood.mcheli.ship.MCH_ClientShipTickHandler;
import com.norwood.mcheli.tank.MCH_ClientTankTickHandler;
import com.norwood.mcheli.tool.MCH_ClientToolTickHandler;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.vehicle.MCH_ClientVehicleTickHandler;
import com.norwood.mcheli.weapon.GPSPosition;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_Reflection;
import com.norwood.mcheli.wrapper.W_TickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;

import static com.norwood.mcheli.event.ImageTransferHandler.handleImageDataSending;

@SideOnly(Side.CLIENT)
public class ClientCommonTickHandler extends W_TickHandler {


    public static ClientCommonTickHandler instance;
    public static MCH_EntityAircraft ridingAircraft = null;
    public static boolean isDrawScoreboard = false;
    public final MCH_ClientTickHandlerBase[] ticks;
    public final KeyboardInputHandler kbInput;
    public final MouseInputHandler mouseInput;
    public final GuiTickHandler guiTickHandler;
    public final CameraHandler cameraHandler;

    public ClientCommonTickHandler(Minecraft minecraft, MCH_Config config) {
        super(minecraft);
        this.ticks = new MCH_ClientTickHandlerBase[]{new MCH_ClientHeliTickHandler(minecraft, config), new MCP_ClientPlaneTickHandler(minecraft, config), new MCH_ClientShipTickHandler(minecraft, config), new MCH_ClientTankTickHandler(minecraft, config), new MCH_ClientGLTDTickHandler(minecraft, config), new MCH_ClientVehicleTickHandler(minecraft, config), new MCH_ClientLightWeaponTickHandler(minecraft, config), new MCH_ClientSeatTickHandler(minecraft, config), new MCH_ClientToolTickHandler(minecraft, config)};
        this.guiTickHandler = new GuiTickHandler(this);
        this.kbInput = new KeyboardInputHandler(minecraft, this.ticks);
        this.mouseInput = new MouseInputHandler(minecraft);
        this.cameraHandler = new CameraHandler(this);

        kbInput.updateKeybind(config);
    }

    public static double getCurrentStickX() {
        return MouseInputHandler.getCurrentStickX();
    }

    public static double getCurrentStickY() {
        return MouseInputHandler.getCurrentStickY();
    }

    public static double getMaxStickLength() {
        return MouseInputHandler.getMaxStickLength();
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

    public void onTickPost() {
        if(super.mc.player != null && super.mc.world != null) {
            MCH_GuiTargetMarker.onClientTick();
            clearMountedArmSwing(super.mc.player);
        }
        MCH_PlayerViewHandler.onUpdate();
    }


    public void onTick() {
        MCH_ClientTickHandlerBase.initRotLimit();
        kbInput.onTick();
        EntityPlayer player = mc.player;
        boolean inGui = mc.currentScreen != null;

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

        mouseInput.handleRenderTickPre(player, partialTicks);
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
            clearMountedArmSwing(this.mc.player);
            MCH_ClientTickHandlerBase.applyRotLimit(this.mc.player);
            if (ridingAircraft != null) {
                Entity e = MCH_ViewEntityDummy.getInstance(this.mc.player.world);
                if (e != null) {
                    e.rotationPitch = this.mc.player.rotationPitch;
                    e.rotationYaw = this.mc.player.rotationYaw;
                    e.prevRotationPitch = this.mc.player.prevRotationPitch;
                    e.prevRotationYaw = this.mc.player.prevRotationYaw;
                }
            }
        }
        guiTickHandler.handleGui(partialTicks);
    }

    private void clearMountedArmSwing(EntityPlayer player) {
        if (MCH_EntityAircraft.getAircraft_RiddenOrControl(player) == null) {
            return;
        }

        player.swingProgress = 0.0F;
        player.prevSwingProgress = 0.0F;
        player.swingProgressInt = 0;
    }

}
