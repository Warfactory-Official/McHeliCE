package com.norwood.mcheli.hud;

import lombok.Getter;

@Getter
public class MCH_HudItemLine extends MCH_HudItem {

    private final String[] pos;

    public MCH_HudItemLine(int fileLine, String[] position) {
        super(fileLine);
        this.pos = new String[position.length];

        for (int i = 0; i < position.length; i++) {
            this.pos[i] = toFormula(position[i]);
        }
    }

    @Override
    public void execute() {
        double[] lines = new double[this.pos.length];
        int pairLength = lines.length - (lines.length % 2);

        for (int i = 0; i < pairLength; i += 2) {
            lines[i] = resolveHudX(calc(this.pos[i]));
            lines[i + 1] = resolveHudY(calc(this.pos[i + 1]));
        }

        if (pairLength == lines.length) {
            this.drawLine(lines, colorSetting, 3);
        } else {
            double[] safeLines = new double[pairLength];
            System.arraycopy(lines, 0, safeLines, 0, pairLength);
            this.drawLine(safeLines, colorSetting, 3);
        }
    }
}
