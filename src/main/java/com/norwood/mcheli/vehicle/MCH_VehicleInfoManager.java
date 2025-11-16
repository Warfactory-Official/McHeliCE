package com.norwood.mcheli.vehicle;

import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import com.norwood.mcheli.helper.info.ContentRegistries;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

public class MCH_VehicleInfoManager extends MCH_AircraftInfoManager<MCH_VehicleInfo> {

    private static final MCH_VehicleInfoManager instance = new MCH_VehicleInfoManager();

    @Nullable
    public static MCH_VehicleInfo get(String name) {
        return ContentRegistries.vehicle().get(name);
    }

    public static MCH_VehicleInfoManager getInstance() {
        return instance;
    }

    @Nullable
    public static MCH_VehicleInfo getFromItem(Item item) {
        return getInstance().getAcInfoFromItem(item);
    }

    @Nullable
    public MCH_VehicleInfo getAcInfoFromItem(Item item) {
        return ContentRegistries.vehicle().findFirst(info -> info.item == item);
    }

    @Override
    protected boolean contains(String name) {
        return ContentRegistries.vehicle().contains(name);
    }

    @Override
    protected int size() {
        return ContentRegistries.vehicle().size();
    }
}
