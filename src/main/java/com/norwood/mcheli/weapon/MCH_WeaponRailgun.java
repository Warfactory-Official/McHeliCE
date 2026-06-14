package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_PlayerViewHandler;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Charge-up kinetic weapon (ported from MCHeli Reforged 1.7.10).
 *
 * <p>CE refactor notes vs. the 1.7 original:
 * <ul>
 *   <li>Tank yaw/pitch limiting delegated to {@link MCH_WeaponBase#calculateShotRotation}.</li>
 *   <li>Fires the existing {@link MCH_EntityBullet} (no new projectile entity needed).</li>
 *   <li>The charge "spin-up" (lock sound + visual charge) is client-side only, faithful to
 *       Reforged; the server fires immediately. See the note on {@link #shot} — this is a known
 *       Reforged quirk left intact so gameplay matches the source.</li>
 *   <li>Charge sound uses {@link PositionedSoundRecord} (the {@code W_Sound} wrapper's
 *       constructors are package-private), resolved through {@link MCH_SoundEvents#getSound}.</li>
 * </ul>
 */
public class MCH_WeaponRailgun extends MCH_WeaponBase {

    private int lockCount;
    private int prevLockCount;
    @SideOnly(Side.CLIENT)
    private ISound chargeSound;

    public MCH_WeaponRailgun(World w, Vec3d v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
        super(w, v, yaw, pitch, nm, wi);
        this.power = 32;
        this.acceleration = 10.0F;
        this.explosionPower = 2;
        this.interval = 0;
    }

    @Override
    public void update(int countWait) {
        super.update(countWait);
        if (this.world != null && this.world.isRemote) {
            // If the charge isn't advancing (player stopped firing), reset and silence it.
            if (this.lockCount != this.prevLockCount) {
                this.prevLockCount = this.lockCount;
            } else {
                this.lockCount = this.prevLockCount = 0;
                stopChargeSound();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void stopChargeSound() {
        if (this.chargeSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(this.chargeSound);
            this.chargeSound = null;
        }
    }

    @SideOnly(Side.CLIENT)
    private void playChargeSound(double x, double y, double z) {
        if (this.chargeSound == null && getInfo() != null && getInfo().railgunSound != null) {
            SoundEvent sound = MCH_SoundEvents.getSound(getInfo().railgunSound);
            if (sound != null) {
                this.chargeSound = new PositionedSoundRecord(sound, SoundCategory.PLAYERS, 10.0F, 1.0F, (float) x, (float) y, (float) z);
                Minecraft.getMinecraft().getSoundHandler().playSound(this.chargeSound);
            }
        }
    }

    @Override
    public boolean shot(MCH_WeaponParam prm) {
        if (!this.world.isRemote) {
            return railGunShot(prm);
        }

        // Client: spin up over lockTime ticks, then play fire effects. (Reforged behavior: the
        // server has already fired by now; the charge is purely a client-side spin-up.)
        int lockTime = this.weaponInfo.lockTime;
        if (lockTime <= 0) {
            return railGunShot(prm);
        }
        if (this.lockCount <= lockTime) {
            if (this.lockCount == 1) {
                playChargeSound(prm.user.posX, prm.user.posY, prm.user.posZ);
            }
            this.lockCount++;
            if (this.lockCount >= lockTime) {
                this.lockCount = 0;
                return railGunShot(prm);
            }
        }
        return false;
    }

    private boolean railGunShot(MCH_WeaponParam prm) {
        Vec2f rot = calculateShotRotation(prm, true, false);
        Vec3d motion = Vec3d.fromPitchYaw(rot.y, rot.x);

        if (!this.world.isRemote) {
            MCH_EntityBullet e = new MCH_EntityBullet(this.world, prm.posX, prm.posY, prm.posZ,
                    motion.x, motion.y, motion.z, rot.x, rot.y, this.acceleration);
            e.setName(this.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.setAirburstDist(this.airburstDist);
            e.posX += e.motionX * 0.5D;
            e.posY += e.motionY * 0.5D;
            e.posZ += e.motionZ * 0.5D;
            this.world.spawnEntity(e);
            this.playSound(prm.entity);
        } else {
            MCH_PlayerViewHandler.applyRecoil(getInfo().getRecoilPitch(), getInfo().getRecoilYaw(), getInfo().recoilRecoverFactor);
        }
        return true;
    }

    public float getRailgunTime() {
        return this.weaponInfo.lockTime > 0 ? (float) this.lockCount / this.weaponInfo.lockTime : 1.0F;
    }
}
