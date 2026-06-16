package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.UAVTracker;
import com.norwood.mcheli.wingman.config.WingmanConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ModularUI factory for the UAV station pairing/control screen. Mirrors
 * {@link AircraftGuiFactory}: the paired-UAV snapshot is computed on the server in
 * {@link #openGui} and serialized to the client through {@link #writeGuiData}/{@link #readGuiData}.
 */
public class UavStationGuiFactory extends AbstractUIFactory<UavStationGuiData> {

    public static final UavStationGuiFactory INSTANCE = new UavStationGuiFactory();

    protected UavStationGuiFactory() {
        super("mcheli:uav_station");
    }

    public void openGui(EntityPlayer player, MCH_EntityUavStation station) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(station);
        List<UavStationGuiData.UavEntry> entries = buildSnapshot(player, station);
        GuiManager.open(this, new UavStationGuiData(player, station, entries), (EntityPlayerMP) player);
    }

    /** Server-side: gather display info for each paired UAV (loaded entity, or last-known position). */
    private static List<UavStationGuiData.UavEntry> buildSnapshot(EntityPlayer player, MCH_EntityUavStation station) {
        List<UavStationGuiData.UavEntry> list = new ArrayList<>();
        if (!(player.world instanceof WorldServer sw)) {
            return list;
        }
        for (UUID id : station.getPairedUavs()) {
            Entity e = sw.getEntityFromUuid(id);
            if (e instanceof MCH_EntityAircraft ac && !ac.isDead) {
                MCH_AircraftInfo info = ac.getAcInfo();
                String itemName = "";
                if (info != null && info.getItem() != null && info.getItem().getRegistryName() != null) {
                    itemName = info.getItem().getRegistryName().toString();
                }
                String name = info != null ? info.displayName : "UAV";
                boolean reachable = withinReach(station, ac.posX, ac.posZ,
                        info != null ? info.uavRange : -1);
                list.add(new UavStationGuiData.UavEntry(id, true, itemName, name,
                        (int) ac.posX, (int) ac.posY, (int) ac.posZ, ac.getHP(), ac.getMaxHP(), reachable));
            } else {
                ChunkPos cp = UAVTracker.getUAVPos(sw, id);
                int x = cp != null ? cp.x * 16 + 8 : 0;
                int z = cp != null ? cp.z * 16 + 8 : 0;
                boolean reachable = cp != null && withinReach(station, x, z, -1);
                list.add(new UavStationGuiData.UavEntry(id, false, "", "UAV (offline)",
                        x, 0, z, 0, 0, reachable));
            }
        }
        return list;
    }

    private static boolean withinReach(MCH_EntityUavStation station, double x, double z, int uavRange) {
        int range = uavRange >= 0 ? uavRange : WingmanConfig.uavControllerRange;
        if (range < 0 || range >= WingmanConfig.UAV_UNLIMITED_THRESHOLD) {
            return true;
        }
        double dx = x - station.posX;
        double dz = z - station.posZ;
        return dx * dx + dz * dz <= (double) range * range;
    }

    @Override
    public @NotNull IGuiHolder<UavStationGuiData> getGuiHolder(UavStationGuiData guiData) {
        return Objects.requireNonNull(castGuiHolder(guiData.getStation()), "UAV station is not a gui holder!");
    }

    @Override
    public void writeGuiData(UavStationGuiData guiData, PacketBuffer buf) {
        buf.writeInt(guiData.getStation().getEntityId());
        List<UavStationGuiData.UavEntry> entries = guiData.getEntries();
        buf.writeInt(entries.size());
        for (UavStationGuiData.UavEntry e : entries) {
            buf.writeLong(e.id().getMostSignificantBits());
            buf.writeLong(e.id().getLeastSignificantBits());
            buf.writeBoolean(e.loaded());
            buf.writeString(e.itemName());
            buf.writeString(e.displayName());
            buf.writeInt(e.x());
            buf.writeInt(e.y());
            buf.writeInt(e.z());
            buf.writeInt(e.hp());
            buf.writeInt(e.maxHp());
            buf.writeBoolean(e.reachable());
        }
    }

    @Override
    public @NotNull UavStationGuiData readGuiData(EntityPlayer player, PacketBuffer buf) {
        MCH_EntityUavStation station = (MCH_EntityUavStation) player.world.getEntityByID(buf.readInt());
        int count = buf.readInt();
        List<UavStationGuiData.UavEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = new UUID(buf.readLong(), buf.readLong());
            boolean loaded = buf.readBoolean();
            String itemName = buf.readString(256);
            String displayName = buf.readString(256);
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            int hp = buf.readInt();
            int maxHp = buf.readInt();
            boolean reachable = buf.readBoolean();
            entries.add(new UavStationGuiData.UavEntry(id, loaded, itemName, displayName, x, y, z, hp, maxHp, reachable));
        }
        return new UavStationGuiData(player, station, entries);
    }

    @Override
    public ModularScreen createScreen(UavStationGuiData guiData, ModularPanel mainPanel) {
        IGuiHolder<UavStationGuiData> holder = Objects.requireNonNull(getGuiHolder(guiData), "Gui holder must not be null!");
        return holder.createScreen(guiData, mainPanel);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, UavStationGuiData guiData) {
        MCH_EntityUavStation station = guiData.getStation();
        return super.canInteractWith(player, guiData)
                && station != null
                && !station.isDead
                && player.world == station.world
                && player.getDistanceSq(station.posX, station.posY, station.posZ) <= 64;
    }
}
