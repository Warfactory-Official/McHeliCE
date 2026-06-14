package com.norwood.mcheli;

import java.util.Objects;

public final class RWRResult {
    public String name;
    public int color;

    public RWRResult(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String name() {
        return name;
    }

    public int color() {
        return color;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RWRResult) obj;
        return Objects.equals(this.name, that.name) &&
                this.color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color);
    }

    @Override
    public String toString() {
        return "RWRResult[" +
                "name=" + name + ", " +
                "color=" + color + ']';
    }

}
