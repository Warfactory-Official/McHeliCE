package com.norwood.mcheli.hud;

import lombok.Getter;

@Getter
public class MCH_HudItemRect extends MCH_HudItem {

    private final String left;
    private final String top;
    private final String width;
    private final String height;

    public MCH_HudItemRect(int fileLine, String left, String top, String width, String height) {
        super(fileLine);
        this.left = toFormula(left);
        this.top = toFormula(top);
        this.width = toFormula(width);
        this.height = toFormula(height);
    }

    @Override
    public void execute() {
        double x2 = centerX + calc(this.left);
        double y2 = centerY + calc(this.top);
        double x1 = x2 + (int) calc(this.width);
        double y1 = y2 + (int) calc(this.height);
        drawRect(x1, y1, x2, y2, colorSetting);
    }
}
