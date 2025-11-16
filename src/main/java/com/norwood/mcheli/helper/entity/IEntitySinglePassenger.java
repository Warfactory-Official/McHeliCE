package com.norwood.mcheli.helper.entity;

import net.minecraft.entity.Entity;

import javax.annotation.Nullable;

public interface IEntitySinglePassenger {

    @Nullable
    Entity getRiddenByEntity();
}
