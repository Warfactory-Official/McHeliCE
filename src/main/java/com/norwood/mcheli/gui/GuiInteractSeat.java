package com.norwood.mcheli.gui;

import com.norwood.mcheli.MCH_ClientProxy;
import com.norwood.mcheli.MCH_KeyName;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntityHitBox;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.mob.MCH_ItemSpawnGunner;
import com.norwood.mcheli.tool.MCH_ItemWrench;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class GuiInteractSeat extends MCH_Gui {

    private static final int COLOR_HP_HEALTHY = 0xFF28A745;
    private static final int COLOR_BACKGROUND = 0xFF14161C;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_PADDING = 1;

    private static final int OFFSET_Y_BASE = 20;
    private static final int OFFSET_Y_TEXT = 30;

    private static final int PROMPT_PADDING_X = 6;
    private static final int PROMPT_PADDING_Y = 4;
    private static final String TRANSLATION_ENTER_SEAT = "gui.mcheli.interact_seat.enter";

    public GuiInteractSeat(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean isDrawGui(EntityPlayer player) {
        return getTargetSeatId(player) >= 0;
    }

    @Override
    public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
        if (!isThirdPersonView) {
            GL11.glLineWidth(scaleFactor);
            int seatId = getTargetSeatId(player);
            if (seatId < 0) {
                return;
            }

            GlStateManager.disableBlend();
            String keyName = MCH_KeyName.getDescOrName(this.mc.gameSettings.keyBindUseItem);
            drawPrompt(I18n.format(TRANSLATION_ENTER_SEAT, keyName, seatId));
        }

    }

    private int getTargetSeatId(EntityPlayer player) {
        if (player == null || player.world == null || player.getRidingEntity() != null || player.isSneaking()) {
            return -1;
        }

        if (isBlockedHeldItem(player.getHeldItemMainhand())) {
            return -1;
        }

        W_Entity target = MCH_ClientProxy.getMouseOverMCHEntity(player);
        if (target instanceof MCH_EntitySeat seat) {
            return getTargetSeatId(seat, player);
        }

        if (target instanceof MCH_EntityHitBox hitBox) {
            return getTargetSeatId(hitBox.parent, player);
        }

        if (target instanceof MCH_EntityAircraft aircraft) {
            return getTargetSeatId(aircraft, player);
        }

        return -1;
    }

    private int getTargetSeatId(MCH_EntitySeat seat, EntityPlayer player) {
        MCH_EntityAircraft parent = seat.getParent();
        if (parent == null || parent.isDestroyed() || parent.notOnSameTeam(player)) {
            return -1;
        }

        if (seat.getRiddenByEntity() != null || !seat.canRideMob(player)) {
            return -1;
        }

        return seat.seatID + 1;
    }

    private int getTargetSeatId(MCH_EntityAircraft aircraft, EntityPlayer player) {
        if (aircraft == null || aircraft.isDestroyed() || aircraft.getAcInfo() == null ||
                aircraft.notOnSameTeam(player) || !aircraft.getAcInfo().canRide) {
            return -1;
        }

        if (aircraft.getRiddenByEntity() != null || aircraft.isUAV()) {
            return getFirstAvailablePassengerSeatId(aircraft, player);
        }

        if (!aircraft.canRideSeatOrRack(0, player)) {
            return -1;
        }

        if (aircraft.getAcInfo().haveCanopy() && aircraft.isCanopyClose()) {
            return -1;
        }

        if (aircraft.getModeSwitchCooldown() > 0) {
            return -1;
        }

        return 0;
    }

    private int getFirstAvailablePassengerSeatId(MCH_EntityAircraft aircraft, EntityPlayer player) {
        int seatId = 1;
        for (MCH_EntitySeat seat : aircraft.getSeats()) {
            if (seat != null && seat.getRiddenByEntity() == null && !aircraft.isMountedEntity(player) &&
                    aircraft.canRideSeatOrRack(seatId, player)) {
                return seatId;
            }

            seatId++;
        }

        return -1;
    }

    private boolean isBlockedHeldItem(ItemStack stack) {
        return !stack.isEmpty() &&
                (stack.getItem() instanceof MCH_ItemWrench || stack.getItem() instanceof MCH_ItemSpawnGunner);
    }

    private void drawPrompt(String text) {
        int posX = this.centerX;
        int posY = this.centerY + OFFSET_Y_BASE + OFFSET_Y_TEXT;
        int textWidth = this.mc.fontRenderer.getStringWidth(text);
        int textY = posY + BAR_HEIGHT + 2;

        int left = posX - (textWidth / 2) - PROMPT_PADDING_X;
        int top = posY - PROMPT_PADDING_Y;
        int right = posX + (textWidth / 2) + PROMPT_PADDING_X;
        int bottom = textY + this.mc.fontRenderer.FONT_HEIGHT + PROMPT_PADDING_Y;

        drawRect(left, top, right, bottom, COLOR_BACKGROUND);
        drawRect(left + BAR_PADDING, top + BAR_PADDING, right - BAR_PADDING, top + BAR_PADDING + BAR_HEIGHT,
                COLOR_HP_HEALTHY);
        this.drawCenteredString(text, posX, textY, COLOR_HP_HEALTHY);
    }

}
