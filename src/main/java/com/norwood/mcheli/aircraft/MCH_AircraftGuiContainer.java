package com.norwood.mcheli.aircraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.parachute.MCH_ItemParachute;
import com.norwood.mcheli.uav.MCH_EntityUavStation;

public class MCH_AircraftGuiContainer extends Container {

    public final EntityPlayer player;
    public final MCH_EntityAircraft aircraft;

    public MCH_AircraftGuiContainer(EntityPlayer player, MCH_EntityAircraft ac) {
        this.player = player;
        this.aircraft = ac;
        MCH_AircraftInventory iv = this.aircraft.getGuiInventory();
        this.addSlotToContainer(new Slot(iv, 0, 10, 30));
        this.addSlotToContainer(new Slot(iv, 1, 10, 48));
        this.addSlotToContainer(new Slot(iv, 2, 10, 66));
        int num = this.aircraft.getNumEjectionSeat();

        for (int i = 0; i < num; i++) {
            this.addSlotToContainer(new Slot(iv, 3 + i, 10 + 18 * i, 105));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlotToContainer(new Slot(player.inventory, 9 + x + y * 9, 25 + x * 18, 135 + y * 18));
            }
        }

        for (int x = 0; x < 9; x++) {
            this.addSlotToContainer(new Slot(player.inventory, x, 25 + x * 18, 195));
        }
    }

    public int getInventoryStartIndex() {
        return this.aircraft == null ? 3 : 3 + this.aircraft.getNumEjectionSeat();
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    public boolean canInteractWith(@NotNull EntityPlayer player) {
        if (this.aircraft.getGuiInventory().isUsableByPlayer(player)) {
            return true;
        } else {
            if (this.aircraft.isUAV()) {
                MCH_EntityUavStation us = this.aircraft.getUavStation();
                if (us != null) {
                    double x = us.posX + us.posUavX;
                    double z = us.posZ + us.posUavZ;
                    return this.aircraft.posX < x + 10.0 && this.aircraft.posX > x - 10.0 &&
                            this.aircraft.posZ < z + 10.0 && this.aircraft.posZ > z - 10.0;
                }
            }

            return false;
        }
    }

    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int slotIndex) {
        MCH_AircraftInventory iv = this.aircraft.getGuiInventory();
        Slot slot = this.inventorySlots.get(slotIndex);
        if (slot == null) {
            return null;
        } else {
            ItemStack itemStack = slot.getStack();
            MCH_Lib.DbgLog(player.world, "transferStackInSlot : %d :" + itemStack, slotIndex);
            if (itemStack.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                if (slotIndex < this.getInventoryStartIndex()) {
                    for (int i = this.getInventoryStartIndex(); i < this.inventorySlots.size(); i++) {
                        Slot playerSlot = this.inventorySlots.get(i);
                        if (playerSlot.getStack().isEmpty()) {
                            playerSlot.putStack(itemStack);
                            slot.putStack(ItemStack.EMPTY);
                            return itemStack;
                        }
                    }
                } else if (itemStack.getItem() instanceof MCH_ItemFuel) {
                    for (int ix = 0; ix < 3; ix++) {
                        if (iv.getFuelSlotItemStack(ix).isEmpty()) {
                            iv.setInventorySlotContents(ix, itemStack);
                            slot.putStack(ItemStack.EMPTY);
                            return itemStack;
                        }
                    }
                } else if (itemStack.getItem() instanceof MCH_ItemParachute) {
                    int num = this.aircraft.getNumEjectionSeat();

                    for (int ixx = 0; ixx < num; ixx++) {
                        if (iv.getParachuteSlotItemStack(ixx).isEmpty()) {
                            iv.setInventorySlotContents(3 + ixx, itemStack);
                            slot.putStack(ItemStack.EMPTY);
                            return itemStack;
                        }
                    }
                }

                return ItemStack.EMPTY;
            }
        }
    }

    public void onContainerClosed(@NotNull EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            MCH_AircraftInventory iv = this.aircraft.getGuiInventory();

            for (int i = 0; i < 3; i++) {
                ItemStack is = iv.getFuelSlotItemStack(i);
                if (!is.isEmpty() && !(is.getItem() instanceof MCH_ItemFuel)) {
                    this.dropPlayerItem(player, i);
                }
            }

            for (int ix = 0; ix < 2; ix++) {
                ItemStack is = iv.getParachuteSlotItemStack(ix);
                if (!is.isEmpty() && !(is.getItem() instanceof MCH_ItemParachute)) {
                    this.dropPlayerItem(player, 3 + ix);
                }
            }
        }
    }

    public void dropPlayerItem(EntityPlayer player, int slotID) {
        if (!player.world.isRemote) {
            ItemStack itemstack = this.aircraft.getGuiInventory().removeStackFromSlot(slotID);
            if (!itemstack.isEmpty()) {
                player.dropItem(itemstack, false);
            }
        }
    }
}
