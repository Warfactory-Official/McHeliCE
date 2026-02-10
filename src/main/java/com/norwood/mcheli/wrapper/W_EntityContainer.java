package com.norwood.mcheli.wrapper;

import com.cleanroommc.modularui.api.IGuiHolder;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class W_EntityContainer extends W_Entity
        implements IInventory, ICapabilitySerializable<NBTTagCompound>
        {

    public boolean dropContentsWhenDead = true;
    protected short inventorySize = 54;

    @Getter
    protected ItemStackHandler inventory = new AircraftStackHandler(inventorySize);

    public W_EntityContainer(World world) {
        super(world);
    }


    @Override
    public int getSizeInventory() {
        return inventory.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack decrStackSize(int slot, int amount) {
        return inventory.extractItem(slot, amount, false);
    }

    @Override
    public @NotNull ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        inventory.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, @NotNull ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(@NotNull EntityPlayer player) {
        return !isDead && player.getDistanceSq(this) <= 64 * 64;
    }

    @Override
    public boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void openInventory(@NotNull EntityPlayer player) {
    }

    @Override
    public void closeInventory(@NotNull EntityPlayer player) {
    }

    @Override
    public void markDirty() {
    }

    @Override
    public String getName() {
        return "Inventory";
    }





    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public @NotNull ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }


    @Override
    public boolean hasCapability(@NotNull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }


    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setTag("items", inventory.serializeNBT());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("items")) {
            inventory.deserializeNBT(tag.getCompoundTag("items"));
        }
    }


    @Override
    public void setDead() {
        if (dropContentsWhenDead && !world.isRemote) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(
                            world, posX, posY, posZ, stack
                    );
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.setDead();
    }

    @Override
    public Entity changeDimension(int dimension, @NotNull ITeleporter teleporter) {
        dropContentsWhenDead = false;
        return super.changeDimension(dimension, teleporter);
    }


    public void displayInventory(EntityPlayer player) {
        if (!world.isRemote && getSizeInventory() > 0) {

        }
    }

    public void setInventorySize(short newSize) {
        if (newSize == this.inventorySize) return;
        if(newSize < 0)return;

        ItemStackHandler oldHandler = this.inventory;
        int oldSize = oldHandler != null ? oldHandler.getSlots() : 0;

        ItemStackHandler newHandler = new AircraftStackHandler(newSize);

        int copySlots = Math.min(oldSize, newSize);

        for (int i = 0; i < copySlots; i++) {
            ItemStack stack = oldHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                newHandler.setStackInSlot(i, stack.copy());
            }
        }


        this.inventory = newHandler;
        this.inventorySize = newSize;

        markDirty();
    }


    private class AircraftStackHandler extends ItemStackHandler {
        public AircraftStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    }
}

