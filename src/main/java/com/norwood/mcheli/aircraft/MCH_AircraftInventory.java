package com.norwood.mcheli.aircraft;

import java.util.Arrays;
import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.parachute.MCH_ItemParachute;
import com.norwood.mcheli.wrapper.W_NBTTag;

public class MCH_AircraftInventory implements IInventory {

    public final int SLOT_FUEL0 = 0;
    public final int SLOT_FUEL1 = 1;
    public final int SLOT_FUEL2 = 2;
    public final int SLOT_PARACHUTE0 = 3;
    public final int SLOT_PARACHUTE1 = 4;
    final MCH_EntityAircraft aircraft;
    private ItemStack[] containerItems = new ItemStack[this.getSizeInventory()];

    public MCH_AircraftInventory(MCH_EntityAircraft ac) {
        Arrays.fill(this.containerItems, ItemStack.EMPTY);
        this.aircraft = ac;
    }

    public ItemStack getFuelSlotItemStack(int i) {
        return this.getStackInSlot(i);
    }

    public ItemStack getParachuteSlotItemStack(int i) {
        return this.getStackInSlot(3 + i);
    }

    public boolean haveParachute() {
        for (int i = 0; i < 2; i++) {
            ItemStack item = this.getParachuteSlotItemStack(i);
            if (!item.isEmpty() && item.getItem() instanceof MCH_ItemParachute) {
                return true;
            }
        }

        return false;
    }

    public void consumeParachute() {
        for (int i = 0; i < 2; i++) {
            ItemStack item = this.getParachuteSlotItemStack(i);
            if (!item.isEmpty() && item.getItem() instanceof MCH_ItemParachute) {
                this.setInventorySlotContents(3 + i, ItemStack.EMPTY);
                break;
            }
        }
    }

    public int getSizeInventory() {
        return 10;
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.containerItems) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public @NotNull ItemStack getStackInSlot(int var1) {
        return this.containerItems[var1];
    }

    public void setDead() {
        Random rand = new Random();
        if (this.aircraft.dropContentsWhenDead && !this.aircraft.world.isRemote) {
            for (int i = 0; i < this.getSizeInventory(); i++) {
                ItemStack itemstack = this.getStackInSlot(i);
                if (!itemstack.isEmpty()) {
                    float x = rand.nextFloat() * 0.8F + 0.1F;
                    float y = rand.nextFloat() * 0.8F + 0.1F;
                    float z = rand.nextFloat() * 0.8F + 0.1F;

                    while (itemstack.getCount() > 0) {
                        int j = rand.nextInt(21) + 10;
                        if (j > itemstack.getCount()) {
                            j = itemstack.getCount();
                        }

                        itemstack.shrink(j);
                        EntityItem entityitem = new EntityItem(
                                this.aircraft.world,
                                this.aircraft.posX + x,
                                this.aircraft.posY + y,
                                this.aircraft.posZ + z,
                                new ItemStack(itemstack.getItem(), j, itemstack.getMetadata()));
                        if (itemstack.hasTagCompound()) {
                            entityitem.getItem().setTagCompound(itemstack.getTagCompound().copy());
                        }

                        float f3 = 0.05F;
                        entityitem.motionX = (float) rand.nextGaussian() * f3;
                        entityitem.motionY = (float) rand.nextGaussian() * f3 + 0.2F;
                        entityitem.motionZ = (float) rand.nextGaussian() * f3;
                        this.aircraft.world.spawnEntity(entityitem);
                    }
                }
            }
        }
    }

    public @NotNull ItemStack decrStackSize(int par1, int par2) {
        if (!this.containerItems[par1].isEmpty()) {
            ItemStack itemstack;
            if (this.containerItems[par1].getCount() <= par2) {
                itemstack = this.containerItems[par1];
                this.containerItems[par1] = ItemStack.EMPTY;
            } else {
                itemstack = this.containerItems[par1].splitStack(par2);
                if (this.containerItems[par1].getCount() == 0) {
                    this.containerItems[par1] = ItemStack.EMPTY;
                }

            }
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public @NotNull ItemStack removeStackFromSlot(int par1) {
        if (!this.containerItems[par1].isEmpty()) {
            ItemStack itemstack = this.containerItems[par1];
            this.containerItems[par1] = ItemStack.EMPTY;
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void setInventorySlotContents(int par1, ItemStack par2ItemStack) {
        this.containerItems[par1] = par2ItemStack;
        if (!par2ItemStack.isEmpty() && par2ItemStack.getCount() > this.getInventoryStackLimit()) {
            par2ItemStack.setCount(this.getInventoryStackLimit());
        }
    }

    public String getInventoryName() {
        return this.getInvName();
    }

    public @NotNull String getName() {
        return this.getInvName();
    }

    public String getInvName() {
        if (this.aircraft.getAcInfo() == null) {
            return "";
        } else {
            String s = this.aircraft.getAcInfo().displayName;
            return s.length() <= 32 ? s : s.substring(0, 31);
        }
    }

    public boolean isInvNameLocalized() {
        return this.aircraft.getAcInfo() != null;
    }

    public @NotNull ITextComponent getDisplayName() {
        return new TextComponentString(this.getInvName());
    }

    public boolean hasCustomName() {
        return this.isInvNameLocalized();
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public void markDirty() {}

    public boolean isUsableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(this.aircraft) <= 144.0;
    }

    public boolean isItemValidForSlot(int par1, @NotNull ItemStack par2ItemStack) {
        return true;
    }

    public boolean isStackValidForSlot(int par1, ItemStack par2ItemStack) {
        return true;
    }

    public void openInventory(@NotNull EntityPlayer player) {}

    public void closeInventory(@NotNull EntityPlayer player) {}

    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.containerItems.length; i++) {
            if (!this.containerItems[i].isEmpty()) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("SlotAC", (byte) i);
                this.containerItems[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        par1NBTTagCompound.setTag("ItemsAC", nbttaglist);
    }

    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        NBTTagList nbttaglist = W_NBTTag.getTagList(par1NBTTagCompound, "ItemsAC", 10);
        this.containerItems = new ItemStack[this.getSizeInventory()];
        Arrays.fill(this.containerItems, ItemStack.EMPTY);

        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = W_NBTTag.tagAt(nbttaglist, i);
            int j = nbttagcompound1.getByte("SlotAC") & 255;
            if (j < this.containerItems.length) {
                this.containerItems[j] = new ItemStack(nbttagcompound1);
            }
        }
    }

    public void onInventoryChanged() {}

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {}

    public int getFieldCount() {
        return 0;
    }

    public void clear() {
        for (int i = 0; i < this.getSizeInventory(); i++) {
            this.containerItems[i] = ItemStack.EMPTY;
        }
    }
}
