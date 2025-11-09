package com.norwood.mcheli.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

import com.norwood.mcheli.MCH_MOD;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.RequiredArgsConstructor;

@ElegantPacket
@RequiredArgsConstructor
public class PacketTitle extends PacketBase implements ServerToClientPacket {

    final public String chatComponent;
    final public int showTime;
    final public int position;

    @Override
    public void onReceive(Minecraft mc) {
        if (mc.player != null) {
            MCH_MOD.proxy.printChatMessage(ITextComponent.Serializer.jsonToComponent(chatComponent), this.showTime,
                    this.position);
        }
    }
}
