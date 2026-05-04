package com.norwood.mcheli.aircraft;

import hohserg.elegant.networking.api.IByteBufSerializable;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class for RWR threat table.
 * Ported from MCHeli Reforged.
 */
public class MCH_RWRThreatTable implements IByteBufSerializable {
    public int receiverEntityId;
    public long snapshotSeq;
    public List<MCH_RWRThreatEvent> events;

    public MCH_RWRThreatTable() {
        this.events = new ArrayList<>();
    }

    public MCH_RWRThreatTable(int receiverEntityId, long snapshotSeq, List<MCH_RWRThreatEvent> events) {
        this.receiverEntityId = receiverEntityId;
        this.snapshotSeq = snapshotSeq;
        this.events = events != null ? events : new ArrayList<>();
    }

    public MCH_RWRThreatTable(ByteBuf buf) {
        this.receiverEntityId = buf.readInt();
        this.snapshotSeq = buf.readLong();
        int size = buf.readInt();
        this.events = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.events.add(new MCH_RWRThreatEvent(buf));
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(receiverEntityId);
        buf.writeLong(snapshotSeq);
        buf.writeInt(events.size());
        for (MCH_RWRThreatEvent event : events) {
            event.serialize(buf);
        }
    }
}
