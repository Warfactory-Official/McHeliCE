package com.norwood.mcheli.ship;

import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MCH_ShipInfoManager extends MCH_AircraftInfoManager<MCH_ShipInfo> {
    private static final MCH_ShipInfoManager instance = new MCH_ShipInfoManager();
    public static HashMap map = new LinkedHashMap();

    public static MCH_ShipInfo get(String name) {
        return ContentRegistries.ship().get(name);
    }

    public static MCH_ShipInfoManager getInstance() {
        return instance;
    }

    @Nullable
    public static MCH_ShipInfo getFromItem(@Nullable Item item) {
        return getInstance().getAcInfoFromItem(item);
    }

    public MCH_ShipInfo newInfo(AddonResourceLocation name, String filepath) {
        return new MCH_ShipInfo(name, filepath);
    }

    @Nullable
    public MCH_ShipInfo getAcInfoFromItem(@Nullable Item item) {
        return ContentRegistries.ship().findFirst(info -> info.item == item);
    }

    @Override
    protected boolean contains(String name) {
        return ContentRegistries.plane().contains(name);
    }

    @Override
    protected int size() {
        return ContentRegistries.plane().size();
    }
}
