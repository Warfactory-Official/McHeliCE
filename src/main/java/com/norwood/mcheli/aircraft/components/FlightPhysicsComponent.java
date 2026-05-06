package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.MCH_LowPassFilterFloat;
import com.norwood.mcheli.MCH_Queue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FlightPhysicsComponent implements IAircraftComponent {
    @Getter
    private final MCH_EntityAircraft parent;

    @Getter @Setter private double currentSpeed;
    @Getter @Setter private double velocityX, velocityY, velocityZ;
    @Getter @Setter private double aircraftX, aircraftY, aircraftZ;
    @Getter @Setter private double aircraftYaw, aircraftPitch;
    @Getter @Setter private int aircraftPosRotInc;

    @Getter @Setter private float rotationRoll;
    @Getter @Setter private float prevRotationRoll;
    @Getter @Setter private boolean aircraftRollRev, aircraftRotChanged;

    @Setter @Getter private double currentThrottle;
    @Getter private double prevCurrentThrottle;
    @Getter @Setter private float throttleBack = 0.0F;
    @Getter @Setter private double beforeHoverThrottle;
    @Getter @Setter private boolean throttleUp, throttleDown, moveLeft, moveRight;

    @Getter private final float[] rotCrawlerTrack = new float[2];
    @Getter private final float[] prevRotCrawlerTrack = new float[2];
    @Getter private final float[] throttleCrawlerTrack = new float[2];
    @Getter private final float[] rotTrackRoller = new float[2];
    @Getter private final float[] prevRotTrackRoller = new float[2];

    @Getter @Setter private float rotWheel = 0.0F, prevRotWheel = 0.0F;
    @Getter @Setter private float rotYawWheel = 0.0F, prevRotYawWheel = 0.0F;

    @Getter private final MCH_Queue<Vec3d> prevPosition = new MCH_Queue<>(10, Vec3d.ZERO);
    @Getter private final MCH_LowPassFilterFloat lowPassPartialTicks = new MCH_LowPassFilterFloat(10);

    public FlightPhysicsComponent(MCH_EntityAircraft parent) {
        this.parent = parent;
    }

    @Override
    public void init() {}

    @Override
    public MCH_EntityAircraft getParent() {
        return parent;
    }

    public void beginUpdate() {
        prevCurrentThrottle = currentThrottle;
        if (parent.getCountOnUpdate() < 2) {
            prevPosition.clear(new Vec3d(parent.posX, parent.posY, parent.posZ));
        }

        updateControl();
        checkServerNoMove();
    }

    @Override
    public void onUpdate() {
        finishUpdate();
    }

    public void finishUpdate() {
        prevPosition.put(new Vec3d(parent.posX, parent.posY, parent.posZ));
    }

    public void updateControl() {
        if (!parent.world.isRemote) {
            parent.networkSync.setCommonStatus(7, moveLeft);
            parent.networkSync.setCommonStatus(8, moveRight);
            parent.networkSync.setCommonStatus(9, throttleUp);
            parent.networkSync.setCommonStatus(10, throttleDown);
        } else if (parent.getRiddenByEntity() != MCH_MOD.proxy.getClientPlayer()) {
            moveLeft = parent.networkSync.getCommonStatus(7);
            moveRight = parent.networkSync.getCommonStatus(8);
            throttleUp = parent.networkSync.getCommonStatus(9);
            throttleDown = parent.networkSync.getCommonStatus(10);
        }
    }

    public void updatePartWheel() {
        if (!parent.world.isRemote) return;

        var acInfo = parent.getAcInfo();
        if (acInfo == null) return;

        prevRotWheel = rotWheel;
        prevRotYawWheel = rotYawWheel;

        double throttle = currentThrottle;
        if (acInfo.enableBack && throttleBack > 0.01F && throttle <= 0.0) {
            throttle = -throttleBack * 15.0;
        }

        if (moveLeft && !moveRight) {
            rotYawWheel = (float) Math.min(rotYawWheel + 0.1, 1.0);
        } else if (!moveLeft && moveRight) {
            rotYawWheel = (float) Math.max(rotYawWheel - 0.1, -1.0);
        } else {
            rotYawWheel *= 0.9F;
        }

        rotWheel += (float) (throttle * acInfo.partWheelRot);
        if (rotWheel >= 360.0F) {
            rotWheel -= 360.0F;
            prevRotWheel -= 360.0F;
        } else if (rotWheel < 0.0F) {
            rotWheel += 360.0F;
            prevRotWheel += 360.0F;
        }
    }

    public void updatePartCrawlerTrack() {
        if (!parent.world.isRemote) return;

        var info = parent.getAcInfo();
        if (info == null) return;

        prevRotTrackRoller[0] = rotTrackRoller[0];
        prevRotTrackRoller[1] = rotTrackRoller[1];
        prevRotCrawlerTrack[0] = rotCrawlerTrack[0];
        prevRotCrawlerTrack[1] = rotCrawlerTrack[1];

        double throttle = currentThrottle;
        double pivotTurnThrottle = info.pivotTurnThrottle <= 0.0 ? 1.0 : info.pivotTurnThrottle * 0.1;

        boolean turningLeft = moveLeft;
        boolean turningRight = moveRight;
        int dir = 1;

        if (info.enableBack && throttleBack > 0.0F && throttle <= 0.0) {
            throttle = -throttleBack * 5.0F;
            if (turningLeft != turningRight) {
                boolean tmp = turningLeft;
                turningLeft = turningRight;
                turningRight = tmp;
                dir = -1;
            }
        }

        if (turningLeft && !turningRight) {
            double t = 0.2 * dir;
            throttleCrawlerTrack[0] += (float) t;
            throttleCrawlerTrack[1] -= (float) (pivotTurnThrottle * t);
        } else if (!turningLeft && turningRight) {
            double t = 0.2 * dir;
            throttleCrawlerTrack[0] -= (float) (pivotTurnThrottle * t);
            throttleCrawlerTrack[1] += (float) t;
        } else {
            if (throttle > 0.2) throttle = 0.2;
            else if (throttle < -0.2) throttle = -0.2;

            throttleCrawlerTrack[0] += (float) throttle;
            throttleCrawlerTrack[1] += (float) throttle;
        }

        for (int i = 0; i < 2; i++) {
            float t = throttleCrawlerTrack[i];
            if (t < -0.72F) t = -0.72F;
            else if (t > 0.72F) t = 0.72F;
            throttleCrawlerTrack[i] = t;

            float rot = rotTrackRoller[i] + t * info.trackRollerRot;
            float prevRot = prevRotTrackRoller[i];
            if (rot >= 360.0F) {
                rot -= 360.0F;
                prevRot -= 360.0F;
            } else if (rot < 0.0F) {
                rot += 360.0F;
                prevRot += 360.0F;
            }

            rotTrackRoller[i] = rot;
            prevRotTrackRoller[i] = prevRot;

            float trackRot = rotCrawlerTrack[i] - t;
            float prevTrackRot = prevRotCrawlerTrack[i];
            while (trackRot >= 1.0F) {
                trackRot--;
                prevTrackRot--;
            }
            while (trackRot < 0.0F) trackRot++;
            while (prevTrackRot < 0.0F) prevTrackRot++;

            rotCrawlerTrack[i] = trackRot;
            prevRotCrawlerTrack[i] = prevTrackRot;
            throttleCrawlerTrack[i] = t * 0.75F;
        }
    }

    public void checkServerNoMove() {
        if (!parent.world.isRemote) {
            double motion = parent.motionX * parent.motionX + parent.motionY * parent.motionY + parent.motionZ * parent.motionZ;
            if (motion < 1.0E-4) {
                if (parent.serverNoMoveCount < 20) {
                    parent.serverNoMoveCount++;
                    if (parent.serverNoMoveCount >= 20) {
                        parent.serverNoMoveCount = 0;
                        if (parent.world instanceof WorldServer) {
                            ((WorldServer) parent.world).getEntityTracker().sendToTracking(parent, new SPacketEntityVelocity(parent.getEntityId(), 0.0, 0.0, 0.0));
                        }
                    }
                }
            } else {
                parent.serverNoMoveCount = 0;
            }
        }
    }

    public void applyServerPositionAndRotation() {
        double rpinc = aircraftPosRotInc;
        double yaw = MathHelper.wrapDegrees(aircraftYaw - parent.getYaw());
        double roll = MathHelper.wrapDegrees(parent.getServerRoll() - parent.getRoll());
        if (!parent.isDestroyed() && (!com.norwood.mcheli.wrapper.W_Lib.isClientPlayer(parent.getRiddenByEntity()) || parent.getRidingEntity() != null)) {
            parent.setRotYaw((float) (parent.getYaw() + yaw / rpinc));
            parent.setRotPitch((float) (parent.getPitch() + (aircraftPitch - parent.getPitch()) / rpinc));
            parent.setRotRoll((float) (parent.getRoll() + roll / rpinc));
        }

        parent.setPosition(
                parent.posX + (aircraftX - parent.posX) / rpinc,
                parent.posY + (aircraftY - parent.posY) / rpinc,
                parent.posZ + (aircraftZ - parent.posZ) / rpinc);
        parent.setRotYaw(parent.getYaw());
        parent.setRotPitch(parent.getPitch());
        aircraftPosRotInc--;
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double x, double y, double z) {
        velocityX = parent.motionX = x;
        velocityY = parent.motionY = y;
        velocityZ = parent.motionZ = z;
    }

    @SideOnly(Side.CLIENT)
    public void captureClientPositionAndRotation(double x, double y, double z, float yaw, float pitch, int increments) {
        aircraftPosRotInc = increments;
        aircraftX = x;
        aircraftY = y;
        aircraftZ = z;
        aircraftYaw = yaw;
        aircraftPitch = pitch;
        parent.motionX = velocityX;
        parent.motionY = velocityY;
        parent.motionZ = velocityZ;
    }

    public void resetMoveControls() {
        moveLeft = moveRight = throttleDown = throttleUp = false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        rotationRoll = compound.getFloat("AcRoll");
        prevRotationRoll = rotationRoll;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setFloat("AcRoll", rotationRoll);
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public void setVelocityComponents(double velocityX, double velocityY, double velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    public double getAircraftX() {
        return aircraftX;
    }

    public double getAircraftY() {
        return aircraftY;
    }

    public double getAircraftZ() {
        return aircraftZ;
    }

    public double getAircraftYaw() {
        return aircraftYaw;
    }

    public double getAircraftPitch() {
        return aircraftPitch;
    }

    public int getAircraftPosRotInc() {
        return aircraftPosRotInc;
    }

    public void setAircraftPosRotInc(int aircraftPosRotInc) {
        this.aircraftPosRotInc = aircraftPosRotInc;
    }

    public float getRotationRoll() {
        return rotationRoll;
    }

    public void setRotationRoll(float rotationRoll) {
        this.rotationRoll = rotationRoll;
    }

    public float getPrevRotationRoll() {
        return prevRotationRoll;
    }

    public void setPrevRotationRoll(float prevRotationRoll) {
        this.prevRotationRoll = prevRotationRoll;
    }

    public boolean isAircraftRollRev() {
        return aircraftRollRev;
    }

    public void setAircraftRollRev(boolean aircraftRollRev) {
        this.aircraftRollRev = aircraftRollRev;
    }

    public boolean isAircraftRotChanged() {
        return aircraftRotChanged;
    }

    public void setAircraftRotChanged(boolean aircraftRotChanged) {
        this.aircraftRotChanged = aircraftRotChanged;
    }

    public double getCurrentThrottle() {
        return currentThrottle;
    }

    public void setCurrentThrottle(double currentThrottle) {
        this.currentThrottle = currentThrottle;
    }

    public double getPrevCurrentThrottle() {
        return prevCurrentThrottle;
    }

    public float getThrottleBack() {
        return throttleBack;
    }

    public void setThrottleBack(float throttleBack) {
        this.throttleBack = throttleBack;
    }

    public double getBeforeHoverThrottle() {
        return beforeHoverThrottle;
    }

    public void setBeforeHoverThrottle(double beforeHoverThrottle) {
        this.beforeHoverThrottle = beforeHoverThrottle;
    }

    public boolean isThrottleUp() {
        return throttleUp;
    }

    public void setThrottleUp(boolean throttleUp) {
        this.throttleUp = throttleUp;
    }

    public boolean isThrottleDown() {
        return throttleDown;
    }

    public void setThrottleDown(boolean throttleDown) {
        this.throttleDown = throttleDown;
    }

    public boolean isMoveLeft() {
        return moveLeft;
    }

    public void setMoveLeft(boolean moveLeft) {
        this.moveLeft = moveLeft;
    }

    public boolean isMoveRight() {
        return moveRight;
    }

    public void setMoveRight(boolean moveRight) {
        this.moveRight = moveRight;
    }

    public float[] getRotCrawlerTrack() {
        return rotCrawlerTrack;
    }

    public float[] getPrevRotCrawlerTrack() {
        return prevRotCrawlerTrack;
    }

    public float[] getThrottleCrawlerTrack() {
        return throttleCrawlerTrack;
    }

    public float[] getRotTrackRoller() {
        return rotTrackRoller;
    }

    public float[] getPrevRotTrackRoller() {
        return prevRotTrackRoller;
    }

    public float getRotWheel() {
        return rotWheel;
    }

    public void setRotWheel(float rotWheel) {
        this.rotWheel = rotWheel;
    }

    public float getPrevRotWheel() {
        return prevRotWheel;
    }

    public void setPrevRotWheel(float prevRotWheel) {
        this.prevRotWheel = prevRotWheel;
    }

    public float getRotYawWheel() {
        return rotYawWheel;
    }

    public void setRotYawWheel(float rotYawWheel) {
        this.rotYawWheel = rotYawWheel;
    }

    public float getPrevRotYawWheel() {
        return prevRotYawWheel;
    }

    public void setPrevRotYawWheel(float prevRotYawWheel) {
        this.prevRotYawWheel = prevRotYawWheel;
    }

    public MCH_Queue<Vec3d> getPrevPosition() {
        return prevPosition;
    }

    public MCH_LowPassFilterFloat getLowPassPartialTicks() {
        return lowPassPartialTicks;
    }
}
