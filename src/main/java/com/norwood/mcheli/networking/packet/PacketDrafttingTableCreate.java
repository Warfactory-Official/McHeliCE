package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.block.MCH_DraftingTableGuiContainer;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@ElegantPacket
public class PacketDrafttingTableCreate implements ClientToServerPacket {

    public IRecipe recipe;

    @SuppressWarnings("unused")
    public PacketDrafttingTableCreate(ByteBuf buffer) {
        try {
            String text = buffer.toString(buffer.readerIndex(), buffer.readableBytes(), StandardCharsets.UTF_8);
            this.recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation(text));
        } catch (Exception exception) {
            exception.printStackTrace();
            recipe = null;
        }

    }

    public static void send(IRecipe recipe) {
        if (recipe != null) {
            var packet = new PacketDrafttingTableCreate();
            packet.recipe = recipe;
            packet.sendToServer();
            MCH_Lib.DbgLog(true, "MCH_DraftingTableCreatePacket.send recipe = " + recipe.getRegistryName());
        }
    }

    @Override
    public void serialize(ByteBuf acc) {
        acc.writeCharSequence(this.recipe.getRegistryName().toString(), StandardCharsets.UTF_8);
    }


    @Override
    public void onReceive(EntityPlayerMP player) {
        if (recipe == null) return;
        boolean openScreen = player.openContainer instanceof MCH_DraftingTableGuiContainer;
        MCH_Lib.DbgLog(false, "MCH_DraftingTablePacketHandler.onPacketCreate : " + openScreen);
        if (openScreen) {
            ((MCH_DraftingTableGuiContainer) player.openContainer).createRecipeItem(recipe);
        }

    }
}

