package com.norwood.mcheli.wrapper;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

public class W_EntityPlayerSP {

    public static void closeScreen(Entity p) {
        if (p instanceof EntityPlayerSP) {
            ((EntityPlayerSP) p).closeScreen();
        }
    }
}
