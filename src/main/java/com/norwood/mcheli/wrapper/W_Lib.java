package com.norwood.mcheli.wrapper;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import com.norwood.mcheli.MCH_MOD;

public class W_Lib {

    public static boolean isEntityLivingBase(Entity entity) {
        return entity instanceof EntityLivingBase;
    }

    public static EntityLivingBase castEntityLivingBase(Object entity) {
        return (EntityLivingBase) entity;
    }

    public static Class<EntityLivingBase> getEntityLivingBaseClass() {
        return EntityLivingBase.class;
    }

    public static double getEntityMoveDist(@Nullable Entity entity) {
        if (entity == null) {
            return 0.0;
        } else {
            return entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).moveForward : 0.0;
        }
    }

    public static boolean isClientPlayer(@Nullable Entity entity) {
        return entity instanceof EntityPlayer && entity.world.isRemote &&
                W_Entity.isEqual(MCH_MOD.proxy.getClientPlayer(), entity);
    }

    public static boolean isFirstPerson() {
        return MCH_MOD.proxy.isFirstPerson();
    }
}
