package com.norwood.mcheli.tank;

import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MCH_TankInfoManager extends MCH_AircraftInfoManager<MCH_TankInfo> {
    private static final MCH_TankInfoManager instance = new MCH_TankInfoManager();
    public static HashMap map = new LinkedHashMap();


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

    public MCH_TankInfo newInfo(AddonResourceLocation name, String filepath) {
        return new MCH_TankInfo(name, filepath);
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
