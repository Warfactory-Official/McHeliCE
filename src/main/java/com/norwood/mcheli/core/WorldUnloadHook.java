package com.norwood.mcheli.core;


import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;

import java.util.Collection;

@SuppressWarnings("unused")
public class WorldUnloadHook {

    public static Collection<Entity> onEntityUnload(Collection<Entity> entities) {

        return MCH_MOD.proxy.isRemote() ? entities.stream().filter(e -> !(e instanceof W_Entity)).toList() : entities;
    }


}
