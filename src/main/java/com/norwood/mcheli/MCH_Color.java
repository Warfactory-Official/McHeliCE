package com.norwood.mcheli;

public class MCH_Color {
    public final float a;
    public final float r;
    public final float g;
    public final float b;

    public MCH_Color(float aa, float rr, float gg, float bb) {
        this.a = this.round(aa);
        this.r = this.round(rr);
        this.g = this.round(gg);
        this.b = this.round(bb);
    }
    public MCH_Color(int argb) {
        this.a = ((argb >> 24) & 0xFF) / 255.0F;
        this.r = ((argb >> 16) & 0xFF) / 255.0F;
        this.g = ((argb >> 8) & 0xFF) / 255.0F;
        this.b = (argb & 0xFF) / 255.0F;
    }


    public MCH_Color(float rr, float gg, float bb) {
        this(1.0F, rr, gg, bb);
    }

    public MCH_Color() {
        this(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public float round(float f) {
        return f > 1.0F ? 1.0F : (Math.max(f, 0.0F));
    }
}
