package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MCH_LaserGuidanceSystem implements MCH_IGuidanceSystem {

    public World worldObj;
    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public boolean targeting = false;
    public boolean hasLaserGuidancePod = true;

    //TODO: Just do proper fucking projection, not clientside entity hacks
//    @SideOnly(Side.CLIENT)
//    public MCH_EntityLockBox lockBox;
    protected Entity user;

    @Override
    public double getLockPosX() {
        return targetPosX;
    }

    @Override
    public double getLockPosY() {
        return targetPosY;
    }

    @Override
    public double getLockPosZ() {
        return targetPosZ;
    }

    @Override
    public void update() {
        if (!worldObj.isRemote || !targeting) return;

        MCH_EntityAircraft ac = switch (user.getRidingEntity()) {
            case MCH_EntityAircraft a -> a;
            case MCH_EntitySeat s -> s.getParent();
            case MCH_EntityUavStation u -> u.getControlled();
            default -> null;
        };

        if (!hasLaserGuidancePod && ac == null) return;

        float yaw = hasLaserGuidancePod ? user.rotationYaw : ac.rotationYaw;
        float pitch = hasLaserGuidancePod ? user.rotationPitch : ac.rotationPitch;

        // Convert Euler angles to a normalized direction vector directly
        Vec3d lookDir = Vec3d.fromPitchYaw(pitch, yaw);

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        Vec3d src = new Vec3d(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ);

        double maxDist = 1500.0;
        Vec3d dst = src.add(lookDir.scale(maxDist));

        RayTraceResult hitResult = worldObj.rayTraceBlocks(src, dst, false, true, true);

        Vec3d hitVec = (hitResult != null && hitResult.typeOfHit != RayTraceResult.Type.MISS)
                ? hitResult.hitVec
                : dst;

        this.targetPosX = hitVec.x;
        this.targetPosY = hitVec.y;
        this.targetPosZ = hitVec.z;

//        updateLockBox();
    }

//    @SideOnly(Side.CLIENT)
//    private void updateLockBox() {
//        if (this.lockBox != null) {
//            this.lockBox.setPosition(targetPosX, targetPosY, targetPosZ);
//        } else {
//            this.lockBox = new MCH_EntityLockBox(worldObj);
//            this.lockBox.setPosition(targetPosX, targetPosY, targetPosZ);
//            this.worldObj.spawnEntity(this.lockBox);
//        }
//    }
}
