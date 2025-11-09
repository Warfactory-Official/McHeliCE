package com.norwood.mcheli.helper.network;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import com.google.common.io.ByteArrayDataInput;

import io.netty.handler.codec.EncoderException;

public class PacketHelper {

    public static void writeCompoundTag(DataOutputStream dos, @Nullable NBTTagCompound nbt) throws IOException {
        if (nbt == null) {
            dos.writeByte(0);
        } else {
            dos.writeByte(1);

            try {
                CompressedStreamTools.write(nbt, dos);
            } catch (IOException var3) {
                throw new EncoderException(var3);
            }
        }
    }

    @Nullable
    public static NBTTagCompound readCompoundTag(ByteArrayDataInput data) {
        byte b0 = data.readByte();
        if (b0 == 0) {
            return null;
        } else {
            try {
                return CompressedStreamTools.read(data, new NBTSizeTracker(2097152L));
            } catch (IOException var3) {
                throw new EncoderException(var3);
            }
        }
    }

    public static void writeItemStack(DataOutputStream dos, ItemStack itemstack) throws IOException {
        if (itemstack.isEmpty()) {
            dos.writeShort(-1);
        } else {
            dos.writeShort(Item.getIdFromItem(itemstack.getItem()));
            dos.writeByte(itemstack.getCount());
            dos.writeShort(itemstack.getMetadata());
            NBTTagCompound nbttagcompound = null;
            if (itemstack.getItem().isDamageable() || itemstack.getItem().getShareTag()) {
                nbttagcompound = itemstack.getItem().getNBTShareTag(itemstack);
            }

            writeCompoundTag(dos, nbttagcompound);
        }
    }

    public static ItemStack readItemStack(ByteArrayDataInput data) throws IOException {
        int i = data.readShort();
        if (i < 0) {
            return ItemStack.EMPTY;
        } else {
            int j = data.readByte();
            int k = data.readShort();
            ItemStack itemstack = new ItemStack(Item.getItemById(i), j, k);
            itemstack.getItem().readNBTShareTag(itemstack, readCompoundTag(data));
            return itemstack;
        }
    }
}
