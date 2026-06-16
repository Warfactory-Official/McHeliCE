package com.norwood.mcheli.factories;

import com.cleanroommc.modularui.factory.GuiManager;

public class MCHGuiFactories {

    private MCHGuiFactories() {
    }

    public static AircraftGuiFactory aircraft() {
        return AircraftGuiFactory.INSTANCE;
    }

    public static UavStationGuiFactory uavStation() {
        return UavStationGuiFactory.INSTANCE;
    }


    public static void init() {
        GuiManager.registerFactory(aircraft());
        GuiManager.registerFactory(uavStation());
    }
}
