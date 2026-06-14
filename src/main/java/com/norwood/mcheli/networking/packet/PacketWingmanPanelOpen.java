package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.wingman.config.WingmanConfig;
import com.norwood.mcheli.wingman.util.McheliReflect;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;

/**
 * Client → Server: request the wingman panel. The server gathers the data and
 * replies with {@link PacketWingmanPanelData}.
 */
@ElegantPacket
public class PacketWingmanPanelOpen implements ClientToServerPacket {

    public PacketWingmanPanelOpen() {}

    @Override
    public void onReceive(EntityPlayerMP player) {
        WorldServer ws = (WorldServer) player.world;
        ws.addScheduledTask(() -> buildData(player, ws).sendToPlayer(player));
    }

    private static PacketWingmanPanelData buildData(EntityPlayerMP player, WorldServer ws) {
        PacketWingmanPanelData pkt = new PacketWingmanPanelData();

        pkt.sideDist  = WingmanConfig.formationSideDist;
        pkt.altOffset = WingmanConfig.formationAltOffset;
        pkt.rearDist  = WingmanConfig.formationRearDist;
        pkt.maxWings  = WingmanConfig.maxWingmen;
        pkt.minAlt    = WingmanConfig.minAttackAltitude;
        pkt.maxAlt    = WingmanConfig.maxAttackAltitude;

        Entity leader = player.getRidingEntity();
        if (!McheliReflect.isAircraft(leader)) return pkt;

        Map<UUID, WingmanEntry> registry = WingmanRegistry.snapshot();

        for (Map.Entry<UUID, WingmanEntry> e : registry.entrySet()) {
            WingmanEntry entry = e.getValue();
            if (entry.leader != leader) continue;

            Entity wEnt = ws.getEntityFromUuid(e.getKey());
            PacketWingmanPanelData.WingmanDto dto = new PacketWingmanPanelData.WingmanDto();
            dto.uuid       = e.getKey().toString();
            dto.name       = McheliReflect.getAircraftName(wEnt);
            dto.slot       = entry.formationSlot;
            dto.state      = entry.leader != null ? "FOLLOWING" : entry.autoState.name();
            dto.attackMode = entry.attackMode;
            dto.weaponType = entry.weaponType != null ? entry.weaponType : "";
            pkt.wingmen.add(dto);
        }

        for (Entity e : ws.loadedEntityList) {
            if (!McheliReflect.isAircraft(e)) continue;
            if (e == leader) continue;
            if (registry.containsKey(e.getUniqueID())) continue;
            if (leader.getDistanceSq(e) > 512.0 * 512.0) continue;

            PacketWingmanPanelData.AircraftDto dto = new PacketWingmanPanelData.AircraftDto();
            dto.uuid = e.getUniqueID().toString();
            dto.name = McheliReflect.getAircraftName(e);
            pkt.nearby.add(dto);
            if (pkt.nearby.size() >= 8) break;
        }

        return pkt;
    }
}
