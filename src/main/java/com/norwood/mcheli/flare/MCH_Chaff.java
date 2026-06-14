package com.norwood.mcheli.flare;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketChaffUse;
import com.norwood.mcheli.wrapper.W_McClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

public class MCH_Chaff {
    public int tick;
    public int useTick;
    public int chaffUseTime;
    public int chaffWaitTime;
    public World world;
    public MCH_EntityAircraft aircraft;
    private int spawnChaffEntityIntervalTick;
    public final Random rand = new Random();

    public MCH_Chaff(World w, MCH_EntityAircraft ac) {
        this.world = w;
        this.aircraft = ac;
    }

    public boolean onUse() {
        boolean result = false;
        if (world.isRemote) {
            if (tick == 0) {
                tick = chaffWaitTime;
                useTick = chaffUseTime;
                spawnChaffEntityIntervalTick = 0;
                result = true;
            }
        } else {
            result = true;
            tick = chaffWaitTime;
            useTick = chaffUseTime;
            spawnChaffEntityIntervalTick = 0;
            new PacketChaffUse(aircraft.getEntityId(), useTick).sendToDimension(world);
            aircraft.getEntityData().setBoolean("ChaffUsing", true);
        }
        return result;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) --this.tick;
            if (this.useTick > 0) --this.useTick;

            if (this.useTick > 0) {
                this.onUsing();
            }

            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ChaffUsing")) {
                this.aircraft.getEntityData().setBoolean("ChaffUsing", false);
            }
        }
    }

    private void onUsing() {
        if (spawnChaffEntityIntervalTick == 0) {
            spawnChaffEntityIntervalTick = chaffUseTime / 10;
            if (!world.isRemote) {
                spawnChaffEntity();
            }
            if (world.isRemote) {
                //FIXME:CHAFF SOUND
//                W_McClient.MOD_playSoundFX("chaff", 10.0F, 1.0F);
            }
        }
        if (spawnChaffEntityIntervalTick > 0) {
            spawnChaffEntityIntervalTick--;
        }
    }

    private void spawnChaffEntity() {
        float yaw = this.aircraft.rotationYaw;
        float rad = (float) Math.toRadians(yaw);

        double forwardX = -MathHelper.sin(rad);
        double forwardZ = MathHelper.cos(rad);

        double leftX = -MathHelper.sin(rad + (float) Math.PI / 2F);
        double leftZ = MathHelper.cos(rad + (float) Math.PI / 2F);
        double rightX = -MathHelper.sin(rad - (float) Math.PI / 2F);
        double rightZ = MathHelper.cos(rad - (float) Math.PI / 2F);

        double baseX = this.aircraft.prevPosX - forwardX * 20D;
        double baseY = this.aircraft.prevPosY - 10D;
        double baseZ = this.aircraft.prevPosZ - forwardZ * 20D;

        double sideOffset = 1.5D;

        double leftPosX = baseX + leftX * sideOffset;
        double leftPosZ = baseZ + leftZ * sideOffset;
        double rightPosX = baseX + rightX * sideOffset;
        double rightPosZ = baseZ + rightZ * sideOffset;

        double sideSpeed = 0.2D;

        double leftVelX = this.aircraft.motionX + leftX * sideSpeed;
        double leftVelY = this.aircraft.motionY;
        double leftVelZ = this.aircraft.motionZ + leftZ * sideSpeed;

        double rightVelX = this.aircraft.motionX + rightX * sideSpeed;
        double rightVelY = this.aircraft.motionY;
        double rightVelZ = this.aircraft.motionZ + rightZ * sideSpeed;

        MCH_EntityChaff leftChaff = new MCH_EntityChaff(world, leftPosX, baseY, leftPosZ, leftVelX, leftVelY, leftVelZ);
        MCH_EntityChaff rightChaff = new MCH_EntityChaff(world, rightPosX, baseY, rightPosZ, rightVelX, rightVelY, rightVelZ);

        this.world.spawnEntity(leftChaff);
        this.world.spawnEntity(rightChaff);
    }

    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
