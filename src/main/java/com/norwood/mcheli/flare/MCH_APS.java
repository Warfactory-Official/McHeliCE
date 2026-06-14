package com.norwood.mcheli.flare;

import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketIronCurtainUse;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * Active Protection System (APS) for aircraft.
 * Ported from MCHeli Reforged and adapted for CE.
 */
public class MCH_APS {
    public int tick;
    public int useTick;
    public int useTime;
    public int waitTime;
    public int range;

    public final World world;
    public final MCH_EntityAircraft aircraft;
    private final Random rand = new Random();

    public MCH_APS(World world, MCH_EntityAircraft aircraft) {
        this.world = world;
        this.aircraft = aircraft;
    }

    /**
     * Triggers the APS.
     * @return true if successfully activated.
     */
    public boolean onUse() {
        if (tick != 0) {
            return false;
        }

        tick = waitTime;
        useTick = useTime;

        if (world.isRemote) {
            if (range == 100) {
                MCH_SoundEvents.playSound(world, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 3.0F, 1.0F);
                aircraft.ironCurtainRunningTick = useTick;
            } else {
                MCH_SoundEvents.playSound(world, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 3.0F, 1.0F);
            }
        } else {
            aircraft.getEntityData().setBoolean("APSUsing", true);
            if (range == 100) {
                MCH_SoundEvents.playSound(world, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 10.0F, 1.0F);
                aircraft.ironCurtainRunningTick = useTick;
                new PacketIronCurtainUse(aircraft.getEntityId(), useTick).sendToDimension(world);
            } else {
                MCH_SoundEvents.playSound(world, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 3.0F, 1.0F);
            }
        }
        return true;
    }

    public void onUpdate() {
        if (aircraft == null || aircraft.isDead) {
            return;
        }

        if (tick > 0) {
            tick--;
        }

        if (useTick > 0) {
            useTick--;
            if (useTick == 0) {
                MCH_SoundEvents.playSound(world, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_deactivate", 3.0F, 1.0F);
                onEnd();
            } else {
                onUsing();
            }
        }

        if (!isUsing() && aircraft.getEntityData().getBoolean("APSUsing")) {
            aircraft.getEntityData().setBoolean("APSUsing", false);
        }
    }

    private void onUsing() {
        if (range == 100) {
            // Iron Curtain doesn't use standard APS logic
            return;
        }

        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(aircraft, aircraft.getEntityBoundingBox().grow(range, range, range));
        for (Entity entity : list) {
            if (entity.isDead) continue;

            boolean isBullet = entity.getClass().getName().contains("EntityBullet") || entity.getClass().getName().contains("EntityBaseBullet");
            boolean isGrenade = entity.getClass().getName().contains("EntityGrenade");
            boolean isMissile = entity instanceof MCH_EntityBaseBullet;

            if (!isBullet && !isGrenade && !isMissile) continue;

            // Basic team check
            if (isMissile) {
             MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) entity;
                if (bullet.shootingEntity instanceof EntityLivingBase shooter && aircraft.getRiddenByEntity() instanceof EntityLivingBase pilot) {
                    if (shooter.isOnSameTeam(pilot)) continue;
                }
            }

            if (world.isRemote) {
                spawnFlameLine(aircraft.posX, aircraft.posY, aircraft.posZ, entity.posX, entity.posY, entity.posZ);
            } else {
                MCH_SoundEvents.playSound(world, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 5.0F, 1.0F);
                entity.setDead();
                if (isMissile || isGrenade) {
                  MCH_Explosion.newExplosion(world, null, null, entity.posX, entity.posY, entity.posZ, 2.0F, 0.0F, true, true, false, true, 0);
                }
            }
        }
    }

    private void spawnFlameLine(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = (int) (dist * 5);
        for (int i = 0; i < count; i++) {
            double r = (double) i / (double) count;
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.FLAME, ax + dx * r, ay + dy * r, az + dz * r, 0.0D, 0.0D, 0.0D);
        }
    }

    private void onEnd() {
        if (range == 100) {
            aircraft.ironCurtainRunningTick = 0;
            aircraft.ironCurtainWaveTimer = 0;
            aircraft.ironCurtainCurrentFactor = 0.5f;
            aircraft.ironCurtainLastFactor = 0.5f;
        }
    }

    public boolean isInPreparation() {
        return tick != 0;
    }

    public boolean isUsing() {
        return useTick > 0;
    }
}
