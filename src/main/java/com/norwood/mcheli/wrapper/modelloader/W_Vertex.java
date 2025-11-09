package com.norwood.mcheli.wrapper.modelloader;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class W_Vertex {

    public float x;
    public float y;
    public float z;

    public W_Vertex(float x, float y) {
        this(x, y, 0.0F);
    }

    public W_Vertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void normalize() {
        double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        this.x = (float) (this.x / d);
        this.y = (float) (this.y / d);
        this.z = (float) (this.z / d);
    }

    public void add(W_Vertex v) {
        this.x = this.x + v.x;
        this.y = this.y + v.y;
        this.z = this.z + v.z;
    }

    public boolean equal(W_Vertex v) {
        return this.x == v.x && this.y == v.y && this.z == v.z;
    }
}
