package com.norwood.mcheli.multiplay;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import org.jetbrains.annotations.NotNull;

public class MCH_ContainerScoreboard extends Container {

    public final EntityPlayer thePlayer;

    public MCH_ContainerScoreboard(EntityPlayer player) {
        this.thePlayer = player;
    }

    public boolean canInteractWith(@NotNull EntityPlayer player) {
        return true;
    }
}
