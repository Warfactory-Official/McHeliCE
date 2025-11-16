package com.norwood.mcheli.tank;

import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import com.norwood.mcheli.helper.info.ContentRegistries;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

public class MCH_TankInfoManager extends MCH_AircraftInfoManager<MCH_TankInfo> {

    private static final MCH_TankInfoManager instance = new MCH_TankInfoManager();

    @Nullable
    public static MCH_TankInfo get(String name) {
        return ContentRegistries.tank().get(name);
    }

    public static MCH_TankInfoManager getInstance() {
        return instance;
    }

    @Nullable
    public static MCH_TankInfo getFromItem(Item item) {
        return getInstance().getAcInfoFromItem(item);
    }

    @Nullable
    public MCH_TankInfo getAcInfoFromItem(Item item) {
        return ContentRegistries.tank().findFirst(info -> info.item == item);
    }

    @Override
    protected boolean contains(String name) {
        return ContentRegistries.tank().contains(name);
    }

    @Override
    protected int size() {
        return ContentRegistries.tank().size();
    }
}
