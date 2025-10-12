package com.norwood.mcheli.plane;

import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import com.norwood.mcheli.helper.info.ContentRegistries;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

public class MCP_PlaneInfoManager extends MCH_AircraftInfoManager<MCH_PlaneInfo> {
    private static final MCP_PlaneInfoManager instance = new MCP_PlaneInfoManager();

    public static MCH_PlaneInfo get(String name) {
        return ContentRegistries.plane().get(name);
    }

    public static MCP_PlaneInfoManager getInstance() {
        return instance;
    }

    @Nullable
    public static MCH_PlaneInfo getFromItem(@Nullable Item item) {
        return getInstance().getAcInfoFromItem(item);
    }

    @Nullable
    public MCH_PlaneInfo getAcInfoFromItem(@Nullable Item item) {
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
