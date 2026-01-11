package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.norwood.mcheli.factories.AircraftGuiData;

public class AircraftGui {


    public static ModularPanel buildUI(AircraftGuiData data, PanelSyncManager syncManager, UISettings settings) {
       ModularPanel panel = ModularPanel.defaultPanel("aircraft_gui");
       panel.bindPlayerInventory();



        return null;
    }
}
