package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.MCH_Lib;

public class MCH_Blade {

    private float baseRotation;
    private float rotation = 0.0F;
    private float prevRotation = 0.0F;
    private float foldSpeed;
    private float foldRotation;

    public MCH_Blade(float baseRotation) {
        this.baseRotation = baseRotation;
        this.foldSpeed = 3.0F;
        this.foldRotation = 5.0F;
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = (float) MCH_Lib.getRotate360(rotation);
    }

    public float getPrevRotation() {
        return this.prevRotation;
    }

    public void setPrevRotation(float rotation) {
        this.prevRotation = (float) MCH_Lib.getRotate360(rotation);
    }

    public float getBaseRotation() {
        return this.baseRotation;
    }

    public MCH_Blade setBaseRotation(float rotation) {
        if (rotation >= 0.0) {
            this.baseRotation = rotation;
        }

        return this;
    }

    public MCH_Blade setFoldSpeed(float speed) {
        if (speed > 0.1) {
            this.foldSpeed = speed;
        }

        return this;
    }

    public MCH_Blade setFoldRotation(float rotation) {
        if (rotation > this.foldSpeed * 2.0F) {
            this.foldRotation = rotation;
        }

        return this;
    }

    public void forceFold() {
        if (this.rotation > this.foldRotation && this.rotation < 360.0F - this.foldRotation) {
            if (this.rotation < 180.0F) {
                this.rotation = this.foldRotation;
            } else {
                this.rotation = 360.0F - this.foldRotation;
            }

            this.rotation %= 360.0F;
            this.prevRotation = this.rotation;
        }
    }

    public boolean folding() {
        boolean isCmpFolding = false;
        if (this.rotation > this.foldRotation && this.rotation < 360.0F - this.foldRotation) {
            if (this.rotation < 180.0F) {
                this.rotation = this.rotation - this.foldSpeed;
            } else {
                this.rotation = this.rotation + this.foldSpeed;
            }

            this.rotation %= 360.0F;
        } else {
            isCmpFolding = true;
        }

        return isCmpFolding;
    }

    public boolean unfolding(float rot) {
        boolean isCmpUnfolding = false;
        float targetRot = (float) MCH_Lib.getRotate360(rot + this.baseRotation);
        float prevRot = this.rotation;
        if (targetRot <= 180.0F) {
            this.rotation = (float) MCH_Lib.getRotate360(this.rotation + this.foldSpeed);
            if (this.rotation >= targetRot && prevRot <= targetRot) {
                this.rotation = targetRot;
                isCmpUnfolding = true;
            }
        } else {
            this.rotation = (float) MCH_Lib.getRotate360(this.rotation - this.foldSpeed);
            if (this.rotation <= targetRot && prevRot >= targetRot) {
                this.rotation = targetRot;
                isCmpUnfolding = true;
            }
        }

        return isCmpUnfolding;
    }
}
