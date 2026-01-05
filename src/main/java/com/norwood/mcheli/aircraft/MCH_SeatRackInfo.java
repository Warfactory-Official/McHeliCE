package com.norwood.mcheli.aircraft;

import net.minecraft.util.math.Vec3d;

public class MCH_SeatRackInfo extends MCH_SeatInfo {

    public final float range;
    public final float openParaAlt;
    public final String[] names;

    public MCH_SeatRackInfo(
                            String[] entityNames, double x, double y, double z, Vec3d up,
                            MCH_AircraftInfo.CameraPosition ep,
                            float rng, float paraAlt, float yaw, float pitch, boolean rotSeat) {
        super(new Vec3d(x, y, z), up, false, ep, false, false, false, yaw, pitch, -30.0F, 70.0F, rotSeat);
        this.range = rng;
        this.openParaAlt = paraAlt;
        this.names = entityNames;
    }

    public MCH_SeatRackInfo(
                            String[] entityNames, double x, double y, double z, MCH_AircraftInfo.CameraPosition ep,
                            float rng, float paraAlt, float yaw, float pitch, boolean rotSeat) {
        this(entityNames, x, y, z, null, ep, rng, paraAlt, yaw, pitch, rotSeat);
    }

    public Vec3d getEntryPos() {
        return this.getCamPos().pos;
    }
}
