package com.norwood.mcheli.helicopter;

import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import com.norwood.mcheli.helper.info.ContentRegistries;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

public class MCH_HeliInfoManager extends MCH_AircraftInfoManager<MCH_HeliInfo> {

    private static final MCH_HeliInfoManager instance = new MCH_HeliInfoManager();

    public static MCH_HeliInfoManager getInstance() {
        return instance;
    }

    @Nullable
    public static MCH_HeliInfo get(String name) {
        return ContentRegistries.heli().get(name);
    }

    @Nullable
    public static MCH_HeliInfo getFromItem(Item item) {
        return getInstance().getAcInfoFromItem(item);
    }

    @Nullable
    public MCH_HeliInfo getAcInfoFromItem(Item item) {
        return ContentRegistries.heli().findFirst(info -> info.item == item);
    }

    @Override
    protected boolean contains(String name) {
        return ContentRegistries.heli().contains(name);
    }

    @Override
    protected int size() {
        return ContentRegistries.heli().size();
    }
}
