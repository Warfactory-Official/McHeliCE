package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.parachute.MCH_ItemParachute;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MCH_AircraftGuiContainer extends Container {

    private static final int FUEL_SLOTS = 3;
    private static final int PLAYER_INV_START_Y = 135;
    private static final int HOTBAR_Y = 195;
    private static final int PARACHUTE_SLOT_START = 3;

    public final EntityPlayer player;
    public final MCH_EntityAircraft aircraft;
    private final ItemStackHandler inventory;

    public MCH_AircraftGuiContainer(EntityPlayer player, MCH_EntityAircraft aircraft) {
        this.player = player;
        this.aircraft = aircraft;
        this.inventory = aircraft.getGuiInventory().getItemHandler();

        addAircraftSlots();
        addPlayerInventory(player.inventory);
    }


    private void addAircraftSlots() {
        // Fuel slots
        for (int i = 0; i < FUEL_SLOTS; i++) {
            addSlotToContainer(new SlotItemHandler(inventory, i, 10, 30 + i * 18));
        }

        // Parachute / ejection seats
        int seats = aircraft.getNumEjectionSeat();
        for (int i = 0; i < seats; i++) {
            addSlotToContainer(new SlotItemHandler(
                    inventory,
                    PARACHUTE_SLOT_START + i,
                    10 + i * 18,
                    105
            ));
        }
    }


    private void addPlayerInventory(InventoryPlayer inv) {
        // Main inventory
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(
                        inv,
                        9 + x + y * 9,
                        25 + x * 18,
                        PLAYER_INV_START_Y + y * 18
                ));
            }
        }

        // Hotbar
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(
                    inv,
                    x,
                    25 + x * 18,
                    HOTBAR_Y
            ));
        }
    }


    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player) {
        if (aircraft.isUsableByPlayer(player)) {
            return true;
        }

        //FIXME: That will crash someday
        if (aircraft.isUAV()) {
            MCH_EntityUavStation station = (MCH_EntityUavStation) aircraft.getUavStation();
            if (station != null) {
                double x = station.posX + station.offsetX;
                double z = station.posZ + station.offsetZ;
                return aircraft.posX > x - 10 && aircraft.posX < x + 10
                        && aircraft.posZ > z - 10 && aircraft.posZ < z + 10;
            }
        }

        return false;
    }


    @Override
    @NotNull
    public ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        int aircraftSlots = PARACHUTE_SLOT_START + aircraft.getNumEjectionSeat();

        if (index < aircraftSlots) {
            // Aircraft → player
            if (!mergeItemStack(stack, aircraftSlots, inventorySlots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // Player → aircraft
            if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY,null)) {
                if (!mergeItemStack(stack, 0, FUEL_SLOTS, false))
                    return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof MCH_ItemParachute) {
                if (!mergeItemStack(
                        stack,
                        PARACHUTE_SLOT_START,
                        aircraftSlots,
                        false))
                    return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        return original;
    }


    @Override
    public void onContainerClosed(@NotNull EntityPlayer player) {
        super.onContainerClosed(player);
        if (player.world.isRemote) return;

        // Reject invalid fuel
        for (int i = 0; i < FUEL_SLOTS; i++) {
            ejectIfInvalid(player, i, CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
        }

        // Reject invalid parachutes
        int seats = aircraft.getNumEjectionSeat();
        for (int i = 0; i < seats; i++) {
            ejectIfInvalid(player, PARACHUTE_SLOT_START + i, MCH_ItemParachute.class);
        }
    }

    private void ejectIfInvalid(EntityPlayer player, int slot, Class<?> allowed) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty() && !allowed.isInstance(stack.getItem())) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            player.dropItem(stack, false);
        }
    }
    private void ejectIfInvalid(EntityPlayer player, int slot, Capability<?> capability) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty() && stack.hasCapability(capability,null)) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            player.dropItem(stack, false);
        }
    }
}

