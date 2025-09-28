package com.norwood.mcheli.networking.packet;

import com.google.common.io.ByteArrayDataInput;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.wrapper.W_Network;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_DraftingTableCreatePacket extends MCH_Packet {
    public IRecipe recipe;

    public static void send(IRecipe recipe) {
        if (recipe != null) {
            MCH_DraftingTableCreatePacket s = new MCH_DraftingTableCreatePacket();
            s.recipe = recipe;
            W_Network.sendToServer(s);
            MCH_Lib.DbgLog(true, "MCH_DraftingTableCreatePacket.send recipe = " + recipe.getRegistryName());
        }
    }

    @Override
    public int getMessageID() {
        return 537395216;
    }

    @Override
    public void readData(ByteArrayDataInput data) {
        try {
            this.recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation(data.readUTF()));
        } catch (Exception var3) {
        }
    }

    @Override
    public void writeData(DataOutputStream dos) {
        try {
            dos.writeUTF(this.recipe.getRegistryName().toString());
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }
}
