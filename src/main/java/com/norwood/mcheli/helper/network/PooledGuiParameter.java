package com.norwood.mcheli.helper.network;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class PooledGuiParameter {

    private static Entity clientEntity;
    private static Entity serverEntity;

    public static void setEntity(EntityPlayer player, @Nullable Entity target) {
        if (player.world.isRemote) {
            clientEntity = target;
        } else {
            serverEntity = target;
        }
    }

    @Nullable
    public static Entity getEntity(EntityPlayer player) {
        return player.world.isRemote ? clientEntity : serverEntity;
    }

    public static void resetEntity(EntityPlayer player) {
        setEntity(player, null);
    }
}
