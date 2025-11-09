package com.norwood.mcheli.aircraft;

import net.minecraft.util.math.Vec3d;

public class MCH_SeatRackInfo extends MCH_SeatInfo {

    public final float range;
    public final float openParaAlt;
    public final String[] names;

    public MCH_SeatRackInfo(
                            String[] entityNames, double x, double y, double z, MCH_AircraftInfo.CameraPosition ep,
                            float rng, float paraAlt, float yaw, float pitch, boolean rotSeat) {
        super(new Vec3d(x, y, z), ep, yaw, pitch, rotSeat);
        this.range = rng;
        this.openParaAlt = paraAlt;
        this.names = entityNames;
    }

    public Vec3d getEntryPos() {
        return this.getCamPos().pos;
    }
}
