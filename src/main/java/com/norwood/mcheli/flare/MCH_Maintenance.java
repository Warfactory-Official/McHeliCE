package com.norwood.mcheli.flare;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.world.World;

public class MCH_Maintenance {

    public int tick;
    public int useTick;
    public int useTime;
    public int waitTime;

    public final World worldObj;
    public final MCH_EntityAircraft aircraft;

    public MCH_Maintenance(World world, MCH_EntityAircraft aircraft) {
        this.worldObj = world;
        this.aircraft = aircraft;
    }

    public boolean onUse() {
        if (this.tick != 0) {
            return false;
        }

        this.tick = this.waitTime;
        this.useTick = this.useTime;

        if (this.worldObj.isRemote) {
            W_McClient.playSound("wrench", 10.0F, 1.0F);
        } else {
            this.aircraft.getEntityData().setBoolean("MaintenanceUsing", true);
            this.aircraft.recoverERAByMaintenance(); // Reforged: maintenance restores some ERA tiles
        }

        return true;
    }

    public void onUpdate() {
        if (this.aircraft == null || this.aircraft.isDead) {
            return;
        }

        if (this.tick > 0) {
            --this.tick;
        }

        if (this.useTick > 0) {
            --this.useTick;
        }

        if (this.useTick > 0) {
            this.onUsing();
        }

        if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("MaintenanceUsing")) {
            this.aircraft.getEntityData().setBoolean("MaintenanceUsing", false);
        }
    }

    private void onUsing() {
        if (!this.aircraft.isDead) {
            this.aircraft.repair(this.aircraft.getMaxHP() / 100);
        }
    }

    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
