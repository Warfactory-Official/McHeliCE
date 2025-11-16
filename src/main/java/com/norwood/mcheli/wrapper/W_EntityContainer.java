package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.MCH_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public abstract class W_EntityContainer extends W_Entity implements IInventory {

    public static final int MAX_INVENTORY_SIZE = 54;
    public boolean dropContentsWhenDead = true;
    private ItemStack[] containerItems;

    public W_EntityContainer(World par1World) {
        super(par1World);
        this.containerItems = new ItemStack[54];
        Arrays.fill(this.containerItems, ItemStack.EMPTY);
    }

    public @NotNull ItemStack getStackInSlot(int par1) {
        return this.containerItems[par1];
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public int getUsingSlotNum() {
        int numUsingSlot;
        if (this.containerItems == null) {
            numUsingSlot = 0;
        } else {
            int n = this.getSizeInventory();
            numUsingSlot = 0;

            for (int i = 0; i < n && i < this.containerItems.length; i++) {
                if (!this.getStackInSlot(i).isEmpty()) {
                    numUsingSlot++;
                }
            }
        }

        return numUsingSlot;
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.containerItems) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
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
            return null;
        }
    }

    public void setInventorySlotContents(int par1, ItemStack par2ItemStack) {
        this.containerItems[par1] = par2ItemStack;
        if (!par2ItemStack.isEmpty() && par2ItemStack.getCount() > this.getInventoryStackLimit()) {
            par2ItemStack.setCount(this.getInventoryStackLimit());
        }

        this.markDirty();
    }

    public void onInventoryChanged() {}

    public boolean isUsableByPlayer(@NotNull EntityPlayer par1EntityPlayer) {
        return !this.isDead;
    }

    public boolean isItemValidForSlot(int par1, @NotNull ItemStack par2ItemStack) {
        return true;
    }

    public boolean isStackValidForSlot(int par1, ItemStack par2ItemStack) {
        return true;
    }

    public String getInvName() {
        return "Inventory";
    }

    public @NotNull String getName() {
        return this.getInvName();
    }

    public String getInventoryName() {
        return this.getInvName();
    }

    public @NotNull ITextComponent getDisplayName() {
        return new TextComponentString(this.getInventoryName());
    }

    public boolean isInvNameLocalized() {
        return false;
    }

    public boolean hasCustomName() {
        return this.isInvNameLocalized();
    }

    public void setDead() {
        if (this.dropContentsWhenDead && !this.world.isRemote) {
            for (int i = 0; i < this.getSizeInventory(); i++) {
                ItemStack itemstack = this.getStackInSlot(i);
                if (!itemstack.isEmpty()) {
                    float x = this.rand.nextFloat() * 0.8F + 0.1F;
                    float y = this.rand.nextFloat() * 0.8F + 0.1F;
                    float z = this.rand.nextFloat() * 0.8F + 0.1F;

                    while (itemstack.getCount() > 0) {
                        int j = this.rand.nextInt(21) + 10;
                        if (j > itemstack.getCount()) {
                            j = itemstack.getCount();
                        }

                        itemstack.shrink(j);
                        EntityItem entityitem = new EntityItem(
                                this.world, this.posX + x, this.posY + y, this.posZ + z,
                                new ItemStack(itemstack.getItem(), j, itemstack.getMetadata()));
                        if (itemstack.hasTagCompound()) {
                            entityitem.getItem().setTagCompound(itemstack.getTagCompound().copy());
                        }

                        float f3 = 0.05F;
                        entityitem.motionX = (float) this.rand.nextGaussian() * f3;
                        entityitem.motionY = (float) this.rand.nextGaussian() * f3 + 0.2F;
                        entityitem.motionZ = (float) this.rand.nextGaussian() * f3;
                        this.world.spawnEntity(entityitem);
                    }
                }
            }
        }

        super.setDead();
    }

    protected void writeEntityToNBT(@NotNull NBTTagCompound par1NBTTagCompound) {
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.containerItems.length; i++) {
            if (!this.containerItems[i].isEmpty()) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                this.containerItems[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        par1NBTTagCompound.setTag("Items", nbttaglist);
    }

    protected void readEntityFromNBT(@NotNull NBTTagCompound par1NBTTagCompound) {
        NBTTagList nbttaglist = W_NBTTag.getTagList(par1NBTTagCompound, "Items", 10);
        this.containerItems = new ItemStack[this.getSizeInventory()];
        Arrays.fill(this.containerItems, ItemStack.EMPTY);
        MCH_Lib.DbgLog(this.world, "W_EntityContainer.readEntityFromNBT.InventorySize = %d", this.getSizeInventory());

        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = W_NBTTag.tagAt(nbttaglist, i);
            int j = nbttagcompound1.getByte("Slot") & 255;
            if (j < this.containerItems.length) {
                this.containerItems[j] = new ItemStack(nbttagcompound1);
            }
        }
    }

    public Entity changeDimension(int dimensionIn, @NotNull ITeleporter teleporter) {
        this.dropContentsWhenDead = false;
        return super.changeDimension(dimensionIn, teleporter);
    }

    public boolean displayInventory(EntityPlayer player) {
        if (!this.world.isRemote && this.getSizeInventory() > 0) {
            player.displayGUIChest(this);
            return true;
        } else {
            return false;
        }
    }

    public void openInventory(@NotNull EntityPlayer player) {}

    public void closeInventory(@NotNull EntityPlayer player) {}

    public void markDirty() {}

    public int getSizeInventory() {
        return 0;
    }

    public void clear() {}

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {}

    public int getFieldCount() {
        return 0;
    }
}
