package com.norwood.mcheli.uav;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface IUavStation {



    Vec3d getPos();
    @Nullable
    MCH_EntityAircraft getControlled();

    void setControlled(@Nullable MCH_EntityAircraft aircraft);

    @NotNull StationType getType();

    default void setType(StationType type){};
    default void setUavPosition(double x, double y, double z){}


    enum StationType {
        DEFAULT,
        SMALL
    }
   @Nullable
    Entity getOperator();

}
