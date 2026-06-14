package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.networking.packet.PacketNotifyLock;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_MissileDetector {

    public static final int SEARCH_RANGE = 60;
    private final MCH_EntityAircraft ac;
    private final World world;
    private int alertCount;

    public MCH_MissileDetector(MCH_EntityAircraft aircraft, World w) {
        this.world = w;
        this.ac = aircraft;
        this.alertCount = 0;
    }

    public void update() {
        // Reforged ECM (type 2): continuously decoy incoming radar / anti-radiation missiles,
        // independent of the flare countermeasure path below.
        if (this.ac.isECMJammerUsing()) {
            this.destroyMissileECM();
        }
        if (this.ac.haveFlare()) {
            if (this.alertCount > 0) {
                this.alertCount--;
            }

            boolean isLocked = this.ac.getEntityData().getBoolean("Tracking");
            if (isLocked) {
                this.ac.getEntityData().setBoolean("Tracking", false);
            }

            if (this.ac.getEntityData().getBoolean("LockOn")) {
                if (this.alertCount == 0) {
                    this.alertCount = 10;
                    if (this.ac.haveFlare() && !this.ac.isDestroyed()) {
                        for (int i = 0; i < 2; i++) {
                            Entity entity = this.ac.getEntityBySeatId(i);
                            if (entity instanceof EntityPlayerMP entityPlayerMP) {
                                new PacketNotifyLock().sendToPlayer(entityPlayerMP);
                            }
                        }
                    }
                }

                this.ac.getEntityData().setBoolean("LockOn", false);
            }

            if (!this.ac.isDestroyed()) {
                Entity rider = this.ac.getRiddenByEntity();
                if (rider == null) {
                    rider = this.ac.getEntityBySeatId(1);
                }

                if (rider != null) {
                    if (this.ac.isFlareUsing()) {
                        this.destroyMissile();
                    } else if (!this.ac.isUAV() && !this.world.isRemote) {
                        if (this.alertCount == 0 && (isLocked || this.isLockedByMissile())) {
                            this.alertCount = 20;
                            W_WorldFunc.playSoundAt(this.ac, "alert", 1.0F, 1.0F);
                        }
                    } else if (this.ac.isUAV() && this.world.isRemote && this.alertCount == 0 &&
                            (isLocked || this.isLockedByMissile())) {
                                this.alertCount = 20;
                                if (W_Lib.isClientPlayer(rider)) {
                                    W_McClient.playSound("alert", 1.0F, 1.0F);
                                }
                            }
                }
            }
        }
    }

    public boolean destroyMissile() {
        List<MCH_EntityBaseBullet> list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class,
                this.ac.getEntityBoundingBox().grow(60.0, 60.0, 60.0));
        for (MCH_EntityBaseBullet msl : list) {
            if (msl.targetEntity != null &&
                    (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                msl.targetEntity = null;
                msl.setDead();
            }
        }

        return false;
    }

    /**
     * Reforged ECM soft-kill: for a type-2 ECM jammer, strip the lock from any radar-guided or
     * anti-radiation missile homing on this aircraft within 80 blocks (the missile then flies
     * ballistically instead of tracking). Server-side only.
     */
    public void destroyMissileECM() {
        if (this.world.isRemote) {
            return;
        }
        if (this.ac.getAcInfo() == null || this.ac.getAcInfo().ecmJammerType != 2) {
            return;
        }
        List<MCH_EntityBaseBullet> list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class,
                this.ac.getEntityBoundingBox().grow(80.0, 80.0, 80.0));
        for (MCH_EntityBaseBullet msl : list) {
            if (msl.targetEntity != null &&
                    (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                if (msl.getInfo() != null && (msl.getInfo().isRadarMissile || msl.getInfo().antiRadiationMissile)) {
                    msl.setTargetEntity(null);
                }
            }
        }
    }

    public boolean isLockedByMissile() {
        List<MCH_EntityBaseBullet> list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class,
                this.ac.getEntityBoundingBox().grow(60.0, 60.0, 60.0));
        for (MCH_EntityBaseBullet msl : list) {
            if (msl.targetEntity != null &&
                    (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                return true;
            }
        }

        return false;
    }
}
