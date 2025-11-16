package com.norwood.mcheli.helper.entity;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public interface ITargetMarkerObject {

    static ITargetMarkerObject fromEntity(Entity target) {
        return new ITargetMarkerObject.EntityWrapper(target);
    }

    double getX();

    double getY();

    double getZ();

    @Nullable
    default Entity getEntity() {
        return null;
    }

    @SideOnly(Side.CLIENT)
    class EntityWrapper implements ITargetMarkerObject {

        private final Entity target;

        public EntityWrapper(Entity entity) {
            this.target = entity;
        }

        @Override
        public double getX() {
            return this.target.posX;
        }

        @Override
        public double getY() {
            return this.target.posY;
        }

        @Override
        public double getZ() {
            return this.target.posZ;
        }

        @Override
        public Entity getEntity() {
            return this.target;
        }
    }
}
