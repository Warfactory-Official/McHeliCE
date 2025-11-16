package com.norwood.mcheli.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MCH_ConfigGuiContainer extends Container {

    public final EntityPlayer player;

    public MCH_ConfigGuiContainer(EntityPlayer player) {
        this.player = player;
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    public boolean canInteractWith(@NotNull EntityPlayer player) {
        return true;
    }

    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer par1EntityPlayer, int par2) {
        return ItemStack.EMPTY;
    }
}
