package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.EntityInfo;
import com.norwood.mcheli.MCH_EntityInfoClientTracker;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.util.List;

@Getter
@AllArgsConstructor
@ElegantPacket
public class PacketEntityInfoSync implements ServerToClientPacket {

    public final List<EntityInfo> entities;
    public final long snapshotSeq;

    @Override
    public void onReceive(Minecraft mc) {
        MCH_EntityInfoClientTracker.updateEntities(entities, snapshotSeq);
    }
}
