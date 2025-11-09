package com.norwood.mcheli.aircraft;

public interface MCH_IEntityCanRideAircraft {

    boolean isSkipNormalRender();

    boolean canRideAircraft(MCH_EntityAircraft var1, int var2, MCH_SeatRackInfo var3);
}
