package com.norwood.mcheli.throwable;

import com.norwood.mcheli.helper.info.ContentRegistries;
import net.minecraft.item.Item;

public class MCH_ThrowableInfoManager {

    public static MCH_ThrowableInfo get(String name) {
        return ContentRegistries.throwable().get(name);
    }

    public static MCH_ThrowableInfo get(Item item) {
        return ContentRegistries.throwable().findFirst(info -> info.item == item);
    }
}
