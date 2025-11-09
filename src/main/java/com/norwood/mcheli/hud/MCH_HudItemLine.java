package com.norwood.mcheli.hud;

import lombok.Getter;

@Getter
public class MCH_HudItemLine extends MCH_HudItem {

    private final String[] pos;

    public MCH_HudItemLine(int fileLine, String[] position) {
        super(fileLine);
        this.pos = new String[position.length];

        for (int i = 0; i < position.length; i++) {
            this.pos[i] = position[i].toLowerCase();
        }
    }

    @Override
    public void execute() {
        double[] lines = new double[this.pos.length];

        for (int i = 0; i < lines.length; i += 2) {
            lines[i] = centerX + calc(this.pos[i]);
            lines[i + 1] = centerY + calc(this.pos[i + 1]);
        }

        this.drawLine(lines, colorSetting, 3);
    }
}
