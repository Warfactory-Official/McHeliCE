package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.factory.GuiManager;

public class MCHGuiFactories {

    private MCHGuiFactories() {
    }

    public static AircraftGuiFactory aircraft() {
        return AircraftGuiFactory.INSTANCE;
    }

    public static void init() {
        GuiManager.registerFactory(aircraft());
    }
}
