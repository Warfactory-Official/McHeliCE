package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.weapon.MCH_RenderLaser;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;

/**
 * Server -> client: tells nearby clients to render a laser beam (and impact sparks).
 * The beam is purely cosmetic; all damage/raytracing is resolved server-side in
 * {@link com.norwood.mcheli.weapon.MCH_WeaponLaser}.
 */
@ElegantPacket
@AllArgsConstructor
public class PacketWeaponLaserShooting implements ServerToClientPacket {

    public final double srcX;
    public final double srcY;
    public final double srcZ;
    public final double destX;
    public final double destY;
    public final double destZ;
    public final int argb;
    public final float width;
    public final int life;
    public final boolean pulsate;
    public final double renderStartDist;
    public final boolean hitSomething;

    @Override
    public void onReceive(Minecraft mc) {
        MCH_RenderLaser.addBeam(new Vec3d(this.srcX, this.srcY, this.srcZ), new Vec3d(this.destX, this.destY, this.destZ),
                this.argb, this.width, this.life, this.pulsate, this.renderStartDist);
        if (this.hitSomething && mc.world != null) {
            this.spawnImpactParticles(mc);
        }
    }

    private void spawnImpactParticles(Minecraft mc) {
        ParticleManager pm = mc.effectRenderer;
        for (int i = 0; i < 6; i++) {
            pm.spawnEffectParticle(EnumParticleTypes.CLOUD.getParticleID(),
                    this.destX + (mc.world.rand.nextFloat() - 0.5D), this.destY + 0.1D, this.destZ + (mc.world.rand.nextFloat() - 0.5D),
                    mc.world.rand.nextGaussian() * 0.02D, Math.abs(mc.world.rand.nextGaussian()) * 0.04D, mc.world.rand.nextGaussian() * 0.02D);
        }
        for (int i = 0; i < 4; i++) {
            pm.spawnEffectParticle(EnumParticleTypes.FLAME.getParticleID(),
                    this.destX + (mc.world.rand.nextFloat() - 0.5D) * 0.5D, this.destY + 0.1D, this.destZ + (mc.world.rand.nextFloat() - 0.5D) * 0.5D,
                    0.0D, 0.02D, 0.0D);
        }
    }
}
