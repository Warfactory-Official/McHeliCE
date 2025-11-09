package com.norwood.mcheli.aircraft;

public class MCH_Rotor {

    public MCH_Blade[] blades;
    private int numBlade;
    private int invRot;
    private boolean isFoldBlade;
    private boolean isFoldBladeTarget;
    private final boolean haveFoldBladeFunc;

    public MCH_Rotor(int numBlade, int invRot, int foldSpeed, float posx, float posy, float posz, float rotx,
                     float roty, float rotz, boolean canFoldBlade) {
        this.setupBlade(numBlade, invRot, foldSpeed);
        this.isFoldBlade = false;
        this.isFoldBladeTarget = false;
        this.haveFoldBladeFunc = canFoldBlade;
        this.setPostion(posx, posy, posz);
        this.setRotation(rotx, roty, rotz);
    }

    public MCH_Rotor setPostion(float x, float y, float z) {
        return this;
    }

    public MCH_Rotor setRotation(float x, float y, float z) {
        return this;
    }

    public int getBladeNum() {
        return this.numBlade;
    }

    public void setupBlade(int num, int invRot, int foldSpeed) {
        this.invRot = invRot > 0 ? invRot : 1;
        this.numBlade = num > 0 ? num : 1;
        this.blades = new MCH_Blade[this.numBlade];

        for (int i = 0; i < this.numBlade; i++) {
            this.blades[i] = new MCH_Blade(i * this.invRot);
            this.blades[i].setFoldRotation(5 + i * 3).setFoldSpeed(foldSpeed);
        }
    }

    public boolean isFoldingOrUnfolding() {
        return this.isFoldBlade != this.isFoldBladeTarget;
    }

    public float getBladeRotaion(int bladeIndex) {
        return bladeIndex >= this.numBlade ? 0.0F : this.blades[bladeIndex].getRotation();
    }

    public void startUnfold() {
        this.isFoldBladeTarget = false;
    }

    public void startFold() {
        if (this.haveFoldBladeFunc) {
            this.isFoldBladeTarget = true;
        }
    }

    public void forceFold() {
        if (this.haveFoldBladeFunc) {
            this.isFoldBladeTarget = true;
            this.isFoldBlade = true;

            for (MCH_Blade b : this.blades) {
                b.forceFold();
            }
        }
    }

    public void update(float rot) {
        boolean isCmpFoldUnfold = true;

        for (MCH_Blade b : this.blades) {
            b.setPrevRotation(b.getRotation());
            if (!this.isFoldBlade) {
                if (!this.isFoldBladeTarget) {
                    b.setRotation(rot + b.getBaseRotation());
                    isCmpFoldUnfold = false;
                } else if (!b.folding()) {
                    isCmpFoldUnfold = false;
                }
            } else if (this.isFoldBladeTarget) {
                isCmpFoldUnfold = false;
            } else if (!b.unfolding(rot)) {
                isCmpFoldUnfold = false;
            }
        }

        if (isCmpFoldUnfold) {
            this.isFoldBlade = this.isFoldBladeTarget;
        }
    }
}
