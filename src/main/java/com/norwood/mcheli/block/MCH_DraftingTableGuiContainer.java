package com.norwood.mcheli.block;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_IRecipeList;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.MCH_Recipes;
import com.norwood.mcheli.wrapper.W_Block;
import com.norwood.mcheli.wrapper.W_EntityPlayer;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_DraftingTableGuiContainer extends Container {

    public final EntityPlayer player;
    public final int posX;
    public final int posY;
    public final int posZ;
    public final int outputSlotIndex;
    private final IInventory outputSlot = new InventoryCraftResult();

    public MCH_DraftingTableGuiContainer(EntityPlayer player, int posX, int posY, int posZ) {
        this.player = player;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlotToContainer(new Slot(player.inventory, 9 + x + y * 9, 30 + x * 18, 140 + y * 18));
            }
        }

        for (int x = 0; x < 9; x++) {
            this.addSlotToContainer(new Slot(player.inventory, x, 30 + x * 18, 198));
        }

        this.outputSlotIndex = this.inventoryItemStacks.size();
        Slot a = new Slot(this.outputSlot, this.outputSlotIndex, 178, 90) {

            public boolean isItemValid(@NotNull ItemStack stack) {
                return false;
            }
        };
        this.addSlotToContainer(a);
        MCH_Lib.DbgLog(player.world, "MCH_DraftingTableGuiContainer.MCH_DraftingTableGuiContainer");
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    public boolean canInteractWith(EntityPlayer player) {
        Block block = W_WorldFunc.getBlock(player.world, this.posX, this.posY, this.posZ);
        return (W_Block.isEqual(block, MCH_MOD.blockDraftingTable) ||
                W_Block.isEqual(block, MCH_MOD.blockDraftingTableLit)) &&
                player.getDistanceSq(this.posX, this.posY, this.posZ) <= 144.0;
    }

    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (slotIndex != this.outputSlotIndex) {
                return ItemStack.EMPTY;
            }

            if (!this.mergeItemStack(itemstack1, 0, 36, true)) {
                return ItemStack.EMPTY;
            }

            slot.onSlotChange(itemstack1, itemstack);
            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    public void onContainerClosed(@NotNull EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            ItemStack itemstack = this.getSlot(this.outputSlotIndex).getStack();
            if (!itemstack.isEmpty()) {
                W_EntityPlayer.dropPlayerItemWithRandomChoice(player, itemstack, false, false);
            }
        }

        MCH_Lib.DbgLog(player.world, "MCH_DraftingTableGuiContainer.onContainerClosed");
    }

    public void createRecipeItem(@Nullable IRecipe recipe) {
        boolean isCreativeMode = this.player.capabilities.isCreativeMode;
        if (this.getSlot(this.outputSlotIndex).getHasStack() && !isCreativeMode) {
            MCH_Lib.DbgLog(this.player.world, "MCH_DraftingTableGuiContainer.createRecipeItem:OutputSlot is not empty");
        } else if (recipe == null) {
            MCH_Lib.DbgLog(this.player.world, "Error:MCH_DraftingTableGuiContainer.createRecipeItem:recipe is null : ");
        } else {
            boolean result = false;
            if (isCreativeMode || MCH_Recipes.canCraft(this.player, recipe)) {
                if (!isCreativeMode) {
                    MCH_Recipes.consumeInventory(this.player, recipe);
                }

                this.getSlot(this.outputSlotIndex).putStack(recipe.getRecipeOutput().copy());
                result = true;
            }

            MCH_Lib.DbgLog(this.player.world,
                    "MCH_DraftingTableGuiContainer:Result=" + result + ":Recipe=" + recipe.getRegistryName());
        }
    }

    public int searchRecipeFromList(MCH_IRecipeList list, ItemStack item) {
        for (int i = 0; i < list.getRecipeListSize(); i++) {
            if (list.getRecipe(i).getRecipeOutput().isItemEqual(item)) {
                return i;
            }
        }

        return -1;
    }
}
