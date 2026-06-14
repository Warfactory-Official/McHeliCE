package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_PlayerViewHandler;
import com.norwood.mcheli.MCH_PotionEffect;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntityHide;
import com.norwood.mcheli.aircraft.MCH_EntityHitBox;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.networking.packet.PacketNotifyHit;
import com.norwood.mcheli.networking.packet.PacketWeaponLaserShooting;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Hitscan directed-energy weapon (ported from MCHeli Reforged 1.7.10).
 *
 * <p>CE refactor notes vs. the 1.7 original:
 * <ul>
 *   <li>Tank yaw/pitch limiting is delegated to {@link MCH_WeaponBase#calculateShotRotation}
 *       (the 1.7 version inlined ~30 lines of duplicated tank math).</li>
 *   <li>The hit/damage resolution is now strictly server-authoritative. The 1.7 version
 *       raytraced on both sides; here the server raytraces, applies damage, and broadcasts a
 *       single {@link PacketWeaponLaserShooting} for the cosmetic beam, so every client renders
 *       exactly one consistent beam and there is no client/server divergence.</li>
 *   <li>Beam rendering uses the VBO pipeline ({@link MCH_RenderLaser}); no GL11 immediate mode.</li>
 * </ul>
 */
public class MCH_WeaponLaser extends MCH_WeaponBase {

    private static final double BROADCAST_RANGE = 1000.0;

    public MCH_WeaponLaser(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.power = 8;
        this.acceleration = 4.0F;
        this.explosionPower = 0;
        this.interval = 0;
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        Vec2f rot = calculateShotRotation(prm, true, false);
        Vec3d look = Vec3d.fromPitchYaw(rot.y, rot.x);

        if (!this.world.isRemote) {
            fireLaser(prm, look);
            this.playSound(prm.entity);
        } else {
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
        }
        return true;
    }

    private void fireLaser(MCH_WeaponParam prm, Vec3d look) {
        double range = this.weaponInfo.length > 0.0F ? this.weaponInfo.length : 256.0;
        Vec3d start = new Vec3d(prm.posX, prm.posY, prm.posZ);
        Vec3d end = start.add(look.scale(range));

        RayTraceResult result = laserRaytrace(prm, start, look, end);
        Vec3d hitVec = (result != null && result.hitVec != null) ? result.hitVec : end;
        boolean hitSomething = result != null;

        if (result != null && result.entityHit != null) {
            applyDamage(prm, result.entityHit, start, hitVec);
        }

        int argb = this.weaponInfo.color != null ? this.weaponInfo.color.toARGB() : 0xFFFF0000;
        float width = this.weaponInfo.radius > 0.0F ? this.weaponInfo.radius : 0.1F;
        int life = Math.max(1, this.weaponInfo.timeFuse);
        new PacketWeaponLaserShooting(start.x, start.y, start.z, hitVec.x, hitVec.y, hitVec.z,
                argb, width, life, true, this.weaponInfo.turningFactor, hitSomething)
                .sendPacketToAllAround(this.world, prm.posX, prm.posY, prm.posZ, BROADCAST_RANGE);
    }

    private void applyDamage(MCH_WeaponParam prm, Entity hit, Vec3d start, Vec3d hitVec) {
        MCH_Lib.applyEntityHurtResistantTimeConfig(hit);
        DamageSource ds = DamageSource.causeThrownDamage(prm.entity, prm.user);
        float damage = MCH_Config.applyDamageVsEntity(hit, ds, this.power);
        damage *= getInfo() != null ? getInfo().getDamageFactor(hit) : 1.0F;

        float dist = (float) start.distanceTo(hitVec);
        if (this.weaponInfo.enableBulletDecay && this.weaponInfo.bulletDecay != null) {
            float decayFactor = 1.0F;
            for (MCH_IBulletDecay decay : this.weaponInfo.bulletDecay) {
                decayFactor = decay.calculateDecayFactor(dist);
            }
            damage *= decayFactor;
        }

        List<EntityLivingBase> living = new ArrayList<>();
        if (hit instanceof EntityLivingBase lb) {
            hit.setFire(5);
            living.add(lb);
        }
        if (hit instanceof MCH_EntityAircraft ac) {
            if (ac.getRiddenByEntity() instanceof EntityLivingBase rb) {
                living.add(rb);
            }
            if (ac.getSeats() != null) {
                for (MCH_EntitySeat seat : ac.getSeats()) {
                    if (seat != null && seat.getRiddenByEntity() instanceof EntityLivingBase sr) {
                        living.add(sr);
                    }
                }
            }
        }
        if (getInfo() != null && getInfo().potionEffect != null) {
            for (EntityLivingBase lb : living) {
                for (MCH_PotionEffect effect : getInfo().potionEffect) {
                    if ((effect.startDist < 0 && effect.endDist < 0) || (effect.startDist <= dist && dist < effect.endDist)) {
                        lb.addPotionEffect(new PotionEffect(effect.potionEffect));
                    }
                }
            }
        }

        hit.attackEntityFrom(ds, damage);

        if (prm.user instanceof EntityPlayerMP && prm.entity instanceof MCH_EntityAircraft) {
            PacketNotifyHit.send((MCH_EntityAircraft) prm.entity, (EntityPlayerMP) prm.user);
        }
    }

    public static boolean canHitByLaser(Entity entity, MCH_WeaponParam prm) {
        if (entity == null || entity.isDead) {
            return false;
        }
        if (entity instanceof MCH_EntityChain) {
            return false;
        }
        if (entity instanceof MCH_EntityBaseBullet b) {
            if (b.getInfo() != null && !b.getInfo().canBeIntercepted) {
                return false;
            }
            if (W_Entity.isEqual(b.shootingAircraft, prm.entity)) {
                return false;
            }
            if (W_Entity.isEqual(b.shootingEntity, prm.user)) {
                return false;
            }
        }
        if (entity instanceof MCH_EntitySeat) {
            return false;
        }
        if (entity instanceof MCH_EntityHide) {
            return false;
        }
        if (entity instanceof MCH_EntityHitBox hb) {
            if (W_Entity.isEqual(hb.parent, prm.entity)) {
                return false;
            }
        }
        if (W_Entity.isEqual(entity, prm.user)) {
            return false;
        }
        if (W_Entity.isEqual(entity, prm.user.getRidingEntity())) {
            return false;
        }
        if (prm.entity instanceof MCH_EntityAircraft ac) {
            if (W_Entity.isEqual(entity, prm.entity)) {
                return false;
            }
            return !ac.isMountedEntity(entity);
        }
        return true;
    }

    /**
     * Raytrace blocks first, then entities, returning the nearest hit (or the block hit / null).
     * Mirrors the 1.7 entity-sweep logic using 1.12 {@link RayTraceResult}/{@link AxisAlignedBB}.
     */
    public static RayTraceResult laserRaytrace(MCH_WeaponParam prm, Vec3d start, Vec3d look, Vec3d end) {
        Entity user = prm.user;
        double maxDist = start.distanceTo(end);

        RayTraceResult result = user.world.rayTraceBlocks(start, end, false, true, true);
        double blockDist = result != null ? result.hitVec.distanceTo(start) : maxDist;

        Entity pointed = null;
        Vec3d pointedVec = null;
        double closest = blockDist;

        AxisAlignedBB searchBox = user.getEntityBoundingBox()
                .expand(look.x * maxDist, look.y * maxDist, look.z * maxDist)
                .grow(1.0);
        List<Entity> list = user.world.getEntitiesWithinAABBExcludingEntity(user, searchBox);

        for (Entity entity : list) {
            if (!entity.canBeCollidedWith() || !canHitByLaser(entity, prm)) {
                continue;
            }
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(0.3);
            RayTraceResult intercept = box.calculateIntercept(start, end);
            if (box.contains(start)) {
                pointed = entity;
                pointedVec = intercept == null ? start : intercept.hitVec;
                closest = 0.0;
            } else if (intercept != null) {
                double d = start.distanceTo(intercept.hitVec);
                if (d < closest || closest == 0.0) {
                    pointed = entity;
                    pointedVec = intercept.hitVec;
                    closest = d;
                }
            }
        }

        if (pointed != null && (closest < blockDist || result == null)) {
            result = new RayTraceResult(pointed, pointedVec);
        }
        return result;
    }
}
