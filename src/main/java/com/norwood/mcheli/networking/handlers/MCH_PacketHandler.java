package com.norwood.mcheli.networking.handlers;

import com.google.common.io.ByteArrayDataInput;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.lweapon.MCH_LightWeaponPacketHandler;
import com.norwood.mcheli.multiplay.MCH_MultiplayPacketHandler;
import com.norwood.mcheli.wrapper.W_PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MCH_PacketHandler extends W_PacketHandler {
    @Override
    public void onPacket(ByteArrayDataInput data, EntityPlayer entityPlayer, MessageContext ctx) {
        int msgid = this.getMessageId(data);
        IThreadListener handler = FMLCommonHandler.instance().getWorldThread(ctx.netHandler);
        switch (msgid) {
            case 268437520 -> MCH_CommonPacketHandler.onPacketEffectExplosion(entityPlayer, data, handler);
            case 268437761 -> MCH_MultiplayPacketHandler.onPacket_NotifySpotedEntity(entityPlayer, data, handler);
            case 268437762 -> MCH_MultiplayPacketHandler.onPacket_NotifyMarkPoint(entityPlayer, data, handler);
            case 268438032 -> MCH_MultiplayPacketHandler.onPacket_IndClient(entityPlayer, data, handler);
            case 536873088 -> MCH_MultiplayPacketHandler.onPacket_Command(entityPlayer, data, handler);
            case 536873472 -> MCH_MultiplayPacketHandler.onPacket_LargeData(entityPlayer, data, handler);
            case 536873473 -> MCH_MultiplayPacketHandler.onPacket_ModList(entityPlayer, data, handler);
//            case 536879120 -> MCH_HeliPacketHandler.onPacket_PlayerControl(entityPlayer, data, handler);
//            case 536903696 -> MCP_PlanePacketHandler.onPacket_PlayerControl(entityPlayer, data, handler);
            case 536936464 -> MCH_LightWeaponPacketHandler.onPacket_PlayerControl(entityPlayer, data, handler);
            case 537002000 -> MCH_VehiclePacketHandler.onPacket_PlayerControl(entityPlayer, data, handler);
            case 537919504 -> MCH_TankPacketHandler.onPacket_PlayerControl(entityPlayer, data, handler);
            case 536903698 -> MCH_ShipPacketHandler.onPacket_PlayerControl(entityPlayer, data, handler);
            default ->
                    MCH_Lib.DbgLog(entityPlayer.world, "MCH_PacketHandler.onPacket invalid MSGID=0x%X(%d)", msgid, msgid);
        }
    }

    protected int getMessageId(ByteArrayDataInput data) {
        try {
            return data.readInt();
        } catch (Exception var3) {
            var3.printStackTrace();
            return 0;
        }
    }
}
