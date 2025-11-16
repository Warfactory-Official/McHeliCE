package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.block.MCH_DraftingTableGuiContainer;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

@RequiredArgsConstructor
@ElegantPacket
public class PacketDrafttingTableCreate implements ClientToServerPacket {

    public String recipe;

    public static void send(IRecipe recipe) {
        if (recipe != null) {
            var packet = new PacketDrafttingTableCreate();
            packet.recipe = packet.getRecipeString(recipe);
            packet.sendToServer();
            MCH_Lib.DbgLog(true, "MCH_DraftingTableCreatePacket.send recipe = " + recipe.getRegistryName());
        }
    }

    public String getRecipeString(IRecipe recipe) {
        return recipe.getRegistryName().toString();
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        var recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation(this.recipe));
        if (recipe == null) return;
        boolean openScreen = player.openContainer instanceof MCH_DraftingTableGuiContainer;
        MCH_Lib.DbgLog(false, "MCH_DraftingTablePacketHandler.onPacketCreate : " + openScreen);
        if (openScreen) {
            ((MCH_DraftingTableGuiContainer) player.openContainer).createRecipeItem(recipe);
        }
    }
}
