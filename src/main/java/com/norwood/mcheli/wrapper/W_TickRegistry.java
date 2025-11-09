package com.norwood.mcheli.wrapper;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

public class W_TickRegistry {

    public static void registerTickHandler(W_TickHandler handler, Side side) {
        MinecraftForge.EVENT_BUS.register(handler);
    }
}
