package com.norwood.mcheli.hud;

import lombok.Getter;

@Getter
public class MCH_HudItemConditional extends MCH_HudItem {

    private final boolean isEndif;
    private final String conditional;

    public MCH_HudItemConditional(int fileLine, boolean isEndif, String conditional) {
        super(fileLine);
        this.isEndif = isEndif;
        this.conditional = toFormula(conditional);
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public void execute() {
        if (!this.isEndif) {
            this.parent.isIfFalse = calc(this.conditional) == 0.0;
        } else {
            this.parent.isIfFalse = false;
        }
    }
}
