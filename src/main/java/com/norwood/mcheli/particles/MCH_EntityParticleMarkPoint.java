package com.norwood.mcheli.particles;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.helper.entity.ITargetMarkerObject;
import com.norwood.mcheli.multiplay.MCH_GuiTargetMarker;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MCH_EntityParticleMarkPoint extends MCH_EntityParticleBase implements ITargetMarkerObject {

    final Team team;

    public MCH_EntityParticleMarkPoint(World par1World, double x, double y, double z, Team team) {
        super(par1World, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleMaxAge(30);
        this.team = team;
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            this.setExpired();
        } else {
            player.getTeam();
            player.getTeam();
            if (!player.isOnScoreboardTeam(this.team)) {
                this.setExpired();
            }
        }
    }

    public void setExpired() {
        super.setExpired();
        MCH_Lib.DbgLog(true, "MCH_EntityParticleMarkPoint.setExpired : " + this);
    }

    @Override
    public int getFXLayer() {
        return 3;
    }

    @Override
    public void renderParticle(
                               @NotNull BufferBuilder buffer,
                               @NotNull Entity entity,
                               float partialTicks,
                               float rotationX,
                               float rotationZ,
                               float rotationYZ,
                               float rotationXY,
                               float rotationXZ) {
        GlStateManager.pushMatrix();
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;

        if (player != null) {
            double interpX = interpPosX;
            double interpY = interpPosY;
            double interpZ = interpPosZ;

            if (minecraft.gameSettings.thirdPersonView > 0) {
                double cameraDistance = W_Reflection.getThirdPersonDistance();
                float yaw = -entity.rotationYaw;
                float pitch = -entity.rotationPitch;

                Vec3d cameraOffset = MCH_Lib.RotVec3(0.0, 0.0, -cameraDistance, yaw, pitch);
                if (minecraft.gameSettings.thirdPersonView == 2) {
                    cameraOffset = new Vec3d(-cameraOffset.x, -cameraOffset.y, -cameraOffset.z);
                }

                Vec3d playerEyePosition = new Vec3d(
                        entity.posX,
                        entity.posY + entity.getEyeHeight(),
                        entity.posZ);

                RayTraceResult rayTraceResult = entity.world.rayTraceBlocks(
                        playerEyePosition,
                        playerEyePosition.add(cameraOffset));

                double effectiveDistance = cameraDistance;
                if (rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
                    effectiveDistance = playerEyePosition.distanceTo(rayTraceResult.hitVec) - 0.4;
                    if (effectiveDistance < 0.0) {
                        effectiveDistance = 0.0;
                    }
                }

                double scaleFactor = effectiveDistance / cameraDistance;
                GlStateManager.translate(
                        cameraOffset.x * scaleFactor,
                        cameraOffset.y * scaleFactor,
                        cameraOffset.z * scaleFactor);

                interpX += cameraOffset.x * scaleFactor;
                interpY += cameraOffset.y * scaleFactor;
                interpZ += cameraOffset.z * scaleFactor;
            }

            double renderX = prevPosX + (posX - prevPosX) * partialTicks - interpX;
            double renderY = prevPosY + (posY - prevPosY) * partialTicks - interpY;
            double renderZ = prevPosZ + (posZ - prevPosZ) * partialTicks - interpZ;

            double distanceScale = Math.sqrt(renderX * renderX + renderY * renderY + renderZ * renderZ) / 10.0;
            if (distanceScale < 1.0) {
                distanceScale = 1.0;
            }

            MCH_GuiTargetMarker.addMarkEntityPos(
                    100,
                    this,
                    renderX / distanceScale,
                    renderY / distanceScale,
                    renderZ / distanceScale,
                    false);

            GlStateManager.popMatrix();
        }
    }

    @Override
    public double getX() {
        return this.posX;
    }

    @Override
    public double getY() {
        return this.posY;
    }

    @Override
    public double getZ() {
        return this.posZ;
    }
}
