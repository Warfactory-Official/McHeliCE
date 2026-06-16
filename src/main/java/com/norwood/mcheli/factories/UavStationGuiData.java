package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.factory.GuiData;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.UUID;

/**
 * GUI data for the UAV station pairing screen. Carries the station entity plus a server-computed
 * snapshot of its paired UAVs (serialized to the client by {@link UavStationGuiFactory}), so the
 * client can render the list even for UAVs that are unloaded on the client.
 */
public class UavStationGuiData extends GuiData {

    @Getter
    final MCH_EntityUavStation station;
    @Getter
    final List<UavEntry> entries;

    public UavStationGuiData(EntityPlayer player, MCH_EntityUavStation station, List<UavEntry> entries) {
        super(player);
        this.station = station;
        this.entries = entries;
    }

    /** One paired-UAV row in the station list. */
    public record UavEntry(UUID id, boolean loaded, String itemName, String displayName,
                           int x, int y, int z, int hp, int maxHp, boolean reachable) {
    }
}
