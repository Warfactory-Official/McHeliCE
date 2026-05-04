package com.norwood.mcheli.weapon;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.networking.packet.PacketNotifyTVMissileEntity;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class MCH_WeaponTvMissile extends MCH_WeaponBase {

    protected MCH_EntityTvMissile lastShotTvMissile;
    protected Entity lastShotEntity;
    protected boolean isTVGuided;
    public MCH_LaserGuidanceSystem guidanceSystem;

    public MCH_WeaponTvMissile(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        super.power = 32;
        super.acceleration = 2.0F;
        super.explosionPower = 4;
        super.interval = -100;
        if(w.isRemote) {
            super.interval -= 10;
        }

        super.numMode = 2;
        this.lastShotEntity = null;
        this.lastShotTvMissile = null;
        this.isTVGuided = false;

        if (getInfo().laserGuidance) {
            this.guidanceSystem = new MCH_LaserGuidanceSystem();
            guidanceSystem.worldObj = w;
            guidanceSystem.hasLaserGuidancePod = wi.hasLaserGuidancePod;
            if (w.isRemote) {
                initGuidanceSystemClient();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void initGuidanceSystemClient() {
        guidanceSystem.user = Minecraft.getMinecraft().player;
    }



    @Override
    public String getName() {
        String opt = "";
        if (this.getCurrentMode() == 0) {
            opt = " [TV]";
        }

        if (this.getCurrentMode() == 2) {
            opt = " [TA]";
        }

        return super.getName() + opt;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
        if (!this.world.isRemote) {
            if (this.isTVGuided && this.tick <= 9) {
                if (this.tick % 3 == 0 && this.lastShotTvMissile != null && !this.lastShotTvMissile.isDead &&
                        this.lastShotEntity != null && !this.lastShotEntity.isDead) {
                    int heliEntityID = W_Entity.getEntityId(this.lastShotEntity);
                    int missileID = W_Entity.getEntityId(this.lastShotTvMissile);
                    var packet = new PacketNotifyTVMissileEntity(heliEntityID, missileID);
                            packet.sendToClients();
                }

                if (this.tick == 9) {
                    this.lastShotEntity = null;
                    this.lastShotTvMissile = null;
                }
            }

            if (this.tick <= 2 && this.lastShotEntity instanceof MCH_EntityAircraft) {
                ((MCH_EntityAircraft) this.lastShotEntity).setTVMissile(this.lastShotTvMissile);
            }
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        return this.world.isRemote ? this.shotClient(prm.entity, prm.user) : this.shotServer(prm);
    }

    protected boolean shotClient(Entity entity, Entity user) {
        this.optionParameter2 = 0;
        this.optionParameter1 = this.getCurrentMode();
        return true;
    }

    protected boolean shotServer(MCH_WeaponParam prm) {
        float yaw, pitch;

        // Determine initial orientation based on weapon config
        if (getInfo().enableOffAxis) {
            yaw = prm.user.rotationYaw + fixRotationYaw;
            pitch = prm.user.rotationPitch + fixRotationPitch;
        } else {
            yaw = prm.entity.rotationYaw + fixRotationYaw;
            pitch = prm.entity.rotationPitch + fixRotationPitch;
        }

        if (prm.entity instanceof MCH_EntityTank tank) {
            yaw = prm.user.rotationYaw + prm.randYaw;
            pitch = prm.user.rotationPitch + prm.randPitch;

            int weaponId = tank.getCurrentWeaponID(prm.user);
            var weapon = Objects.requireNonNull(tank.getAcInfo()).getWeaponById(weaponId);

            // Determine pitch limits
            float minP = (weapon == null) ? tank.getAcInfo().minRotationPitch : weapon.minPitch;
            float maxP = (weapon == null) ? tank.getAcInfo().maxRotationPitch : weapon.maxPitch;

            // Calculate relative rotation adjusted for tank tilt (Roll/Pitch)
            float playerYaw = MathHelper.wrapDegrees(tank.getYaw() - yaw);
            float radYaw = (float) Math.toRadians(playerYaw);

            float playerPitch = tank.getPitch() * MathHelper.cos(radYaw) +
                    -tank.getRoll() * MathHelper.sin(radYaw);

            // Apply Yaw constraints
            float yawLimit = (weapon == null) ? 360.0F : weapon.maxYaw;
            float playerYawRel = MathHelper.wrapDegrees(yaw - tank.getYaw());
            float relativeYaw = MathHelper.clamp(playerYawRel, -yawLimit, yawLimit);

            yaw = MathHelper.wrapDegrees(tank.getYaw() + relativeYaw);
            pitch = MathHelper.clamp(pitch, playerPitch + minP, playerPitch + maxP);
            pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        }

        // Convert Euler angles to directional vector components
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);

        double tX = -Math.sin(radYaw) * Math.cos(radPitch);
        double tZ = Math.cos(radYaw) * Math.cos(radPitch);
        double tY = -Math.sin(radPitch);

        this.isTVGuided = prm.option1 == 0;
        float finalAcceleration = isTVGuided ? acceleration : acceleration * 1.5F;

        var missile = new MCH_EntityTvMissile(world, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, finalAcceleration);
        missile.setName(this.name);
        missile.setParameterFromWeapon(this, prm.entity, prm.user);

        this.lastShotEntity = prm.entity;
        this.lastShotTvMissile = missile;

        world.spawnEntity(missile);
        this.playSound(prm.entity);

        return true;
    }

}
