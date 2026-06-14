package com.norwood.mcheli.hud;

import lombok.Getter;

@Getter
public class MCH_HudItemCall extends MCH_HudItem {

    private final String hudName;

    public MCH_HudItemCall(int fileLine, String name) {
        super(fileLine);
        this.hudName = name;
    }

    @Override
    public void execute() {
        MCH_Hud hud = MCH_HudManager.get(this.hudName);
        if (hud != null) {
            boolean previousAutoScaled = hudAutoScaled;
            boolean previousAutoScaleRequested = hudAutoScaleRequested;
            boolean calledAutoScaled = previousAutoScaleRequested && !hud.ignoreAutoScale;

            if (calledAutoScaled != previousAutoScaled) {
                configureCanvas(calledAutoScaled, previousAutoScaleRequested);
            }

            hud.drawItems();

            if (calledAutoScaled != previousAutoScaled) {
                configureCanvas(previousAutoScaled, previousAutoScaleRequested);
            }
        }
    }
}
