package com.norwood.mcheli;

import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.IContentData;
import net.minecraft.util.math.Vec3d;

public abstract class MCH_BaseInfo implements IContentData {

    public final String filePath;
    public final AddonResourceLocation location;

    public MCH_BaseInfo(AddonResourceLocation location, String filePath) {
        this.location = location;
        this.filePath = filePath;
    }

    public boolean toBool(String s) {
        return s.equalsIgnoreCase("true");
    }

    public boolean toBool(String s, boolean defaultValue) {
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else {
            return !s.equalsIgnoreCase("false") && defaultValue;
        }
    }

    public float toFloat(String s) {
        return Float.parseFloat(s);
    }

    public float toFloat(String s, float min, float max) {
        float f = Float.parseFloat(s);
        return f > max ? max : (Math.max(f, min));
    }

    public double toDouble(String s) {
        return Double.parseDouble(s);
    }

    public Vec3d toVec3(String x, String y, String z) {
        return new Vec3d(this.toDouble(x), this.toDouble(y), this.toDouble(z));
    }

    public int toInt(String s) {
        return Integer.parseInt(s);
    }

    public int toInt(String s, int min, int max) {
        int f = Integer.parseInt(s);
        return f > max ? max : (Math.max(f, min));
    }

    public int hex2dec(String s) {
        return !s.startsWith("0x") && !s.startsWith("0X") && s.indexOf(0) != 35 ? (int) (Long.decode("0x" + s) & -1L) :
                (int) (Long.decode(s) & -1L);
    }

    public String[] splitParam(String data) {
        return data.split("\\s*,\\s*");
    }

    public String[] splitParamSlash(String data) {
        return data.split("\\s*/\\s*");
    }

    @Override
    public boolean validate() throws Exception {
        return true;
    }

    public boolean canReloadItem(String item) {
        return false;
    }

    @Override
    public AddonResourceLocation getLocation() {
        return this.location;
    }

    @Override
    public String getContentPath() {
        return this.filePath;
    }
}
