package com.norwood.mcheli.networking.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.multiplay.MCH_Multiplay;
import com.norwood.mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import com.norwood.mcheli.wrapper.W_WorldFunc;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.RequiredArgsConstructor;

@ElegantPacket
@RequiredArgsConstructor
public class PacketRequestSpotEnemy extends PacketBase implements ClientToServerPacket {

    final public int targetFilter;

    @Override
    public void onReceive(EntityPlayerMP player) {
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !(held.getItem() instanceof MCH_ItemRangeFinder)) {
            return;
        }

        double eyeY = player.posY + player.getEyeHeight();
        int filter = this.targetFilter;

        boolean success = false;
        if (filter == 0) {
            success = MCH_Multiplay.markPoint(player, player.posX, eyeY, player.posZ);
        } else if (held.getMetadata() < held.getMaxDamage()) {
            if (MCH_Config.RangeFinderConsume.prmBool) {
                held.damageItem(1, player);
            }

            int time = (filter & 252) == 0 ? 60 : MCH_Config.RangeFinderSpotTime.prmInt;
            success = MCH_Multiplay.spotEntity(
                    player,
                    null,
                    player.posX,
                    eyeY,
                    player.posZ,
                    filter,
                    MCH_Config.RangeFinderSpotDist.prmInt,
                    time,
                    20.0F);
        }

        W_WorldFunc.MOD_playSoundAtEntity(player, success ? "pi" : "ng", 1.0F, 1.0F);
    }
}
