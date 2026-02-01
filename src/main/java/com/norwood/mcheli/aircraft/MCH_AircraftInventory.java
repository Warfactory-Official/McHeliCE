package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.parachute.MCH_ItemParachute;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Random;

public class MCH_AircraftInventory {

    public static final int SLOT_FUEL_IN = 0;
    public static final int SLOT_FUEL_OUT = 1;

    public static final int SLOT_PARACHUTE0 = 3;
    public static final int SLOT_PARACHUTE1 = 4;

    public static final int SIZE = 10;

    private final MCH_EntityAircraft aircraft;
    private final ItemStackHandler items;

    public MCH_AircraftInventory(MCH_EntityAircraft aircraft) {
        this.aircraft = aircraft;
        this.items = new ItemStackHandler(SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                aircraft.markDirty();
            }
        };
    }


    public ItemStack getFuelSlotItemStack(int index) {
        return items.getStackInSlot(index);
    }

    public void setFuelSlotItemStack(int index, ItemStack stack) {
        items.setStackInSlot(index, stack);
    }

    public ItemStack getParachuteSlotItemStack(int index) {
        return items.getStackInSlot(2 + index);
    }

    public void setParachuteSlotItemStack(int index, ItemStack stack) {
        items.setStackInSlot(3 + index, stack);
    }

    /* ----------------
     * Parachutes
     * ---------------- */

    public boolean haveParachute() {
        for (int i = 0; i < 2; i++) {
            ItemStack stack = getParachuteSlotItemStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MCH_ItemParachute) {
                return true;
            }
        }
        return false;
    }

    public void consumeParachute() {
        for (int i = 0; i < 2; i++) {
            ItemStack stack = getParachuteSlotItemStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MCH_ItemParachute) {
                setParachuteSlotItemStack(i, ItemStack.EMPTY);
                return;
            }
        }
    }


    public void dropContents() {
        if (!aircraft.dropContentsWhenDead || aircraft.world.isRemote) return;

        Random rand = new Random();

        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            float x = rand.nextFloat() * 0.8F + 0.1F;
            float y = rand.nextFloat() * 0.8F + 0.1F;
            float z = rand.nextFloat() * 0.8F + 0.1F;

            while (!stack.isEmpty()) {
                int count = Math.min(rand.nextInt(21) + 10, stack.getCount());
                ItemStack drop = stack.splitStack(count);

                EntityItem entity = new EntityItem(
                        aircraft.world,
                        aircraft.posX + x,
                        aircraft.posY + y,
                        aircraft.posZ + z,
                        drop
                );

                float f = 0.05F;
                entity.motionX = rand.nextGaussian() * f;
                entity.motionY = rand.nextGaussian() * f + 0.2F;
                entity.motionZ = rand.nextGaussian() * f;

                aircraft.world.spawnEntity(entity);
            }

            items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }


    public void writeToNBT(NBTTagCompound tag) {
        tag.setTag("ItemsAC", items.serializeNBT());
    }

    public void readFromNBT(NBTTagCompound tag) {
        items.deserializeNBT(tag.getCompoundTag("ItemsAC"));
    }


    public ItemStackHandler getItemHandler() {
        return items;
    }

    public String getInventoryName() {
        assert aircraft.getAcInfo() != null;
        return aircraft.getAcInfo().name;
    }
}

