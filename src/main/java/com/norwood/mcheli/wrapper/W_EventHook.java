package com.norwood.mcheli.wrapper;

import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class W_EventHook {

    @SubscribeEvent
    public void onEvent_entitySpawn(EntityJoinWorldEvent event) {
        this.entitySpawn(event);
    }

    public void entitySpawn(EntityJoinWorldEvent event) {}

    @SubscribeEvent
    public void onEvent_livingHurtEvent(LivingHurtEvent event) {
        this.livingHurtEvent(event);
    }

    public void livingHurtEvent(LivingHurtEvent event) {}

    @SubscribeEvent
    public void onEvent_livingAttackEvent(LivingAttackEvent event) {
        this.livingAttackEvent(event);
    }

    public void livingAttackEvent(LivingAttackEvent event) {}

    @SubscribeEvent
    public void onEvent_entityInteractEvent(EntityInteract event) {
        this.entityInteractEvent(event);
    }

    public void entityInteractEvent(EntityInteract event) {}

    @SubscribeEvent
    public void onEvent_entityCanUpdate(CanUpdate event) {
        this.entityCanUpdate(event);
    }

    public void entityCanUpdate(CanUpdate event) {}

    @SubscribeEvent
    public void onEvent_commandEvent(CommandEvent event) {
        this.commandEvent(event);
    }

    public void commandEvent(CommandEvent event) {}
}
