package com.norwood.mcheli.hud;

import javax.annotation.Nullable;

import com.norwood.mcheli.helper.info.ContentRegistries;

public class MCH_HudManager {

    private MCH_HudManager() {}

    @Nullable
    public static MCH_Hud get(String name) {
        return ContentRegistries.hud().get(name);
    }

    public static boolean contains(String name) {
        return ContentRegistries.hud().contains(name);
    }

    public static int size() {
        return ContentRegistries.hud().size();
    }
}
