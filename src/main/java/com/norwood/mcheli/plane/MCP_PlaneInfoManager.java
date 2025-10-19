package com.norwood.mcheli.plane;

import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MCP_PlaneInfoManager extends MCH_AircraftInfoManager<MCP_PlaneInfo> {
    private static final MCP_PlaneInfoManager instance = new MCP_PlaneInfoManager();
    public static HashMap map = new LinkedHashMap();


    public static MCP_PlaneInfo get(String name) {
        return ContentRegistries.plane().get(name);
    }

    public static MCP_PlaneInfoManager getInstance() {
        return instance;
    }

    @Nullable
    public static MCP_PlaneInfo getFromItem(@Nullable Item item) {
        return getInstance().getAcInfoFromItem(item);
    }

    public MCP_PlaneInfo newInfo(AddonResourceLocation name, String filepath) {
        return new MCP_PlaneInfo(name, filepath);
    }

    @Nullable
    public MCP_PlaneInfo getAcInfoFromItem(@Nullable Item item) {
        return ContentRegistries.plane().findFirst(info -> info.item == item);
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
