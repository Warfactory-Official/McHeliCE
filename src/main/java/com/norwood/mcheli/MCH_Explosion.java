package com.norwood.mcheli;

import com.norwood.mcheli.helper.world.MCH_ExplosionV2;
import com.norwood.mcheli.networking.data.DataExplosionParameters;
import com.norwood.mcheli.networking.packet.PacketParticleEffect;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

@Deprecated// Piece of shit, im axing it soon
public class MCH_Explosion {

    public static MCH_Explosion.ExplosionResult newExplosion(
                                                             World w,
                                                             @Nullable Entity entityExploded,
                                                             @Nullable Entity player,
                                                             double x,
                                                             double y,
                                                             double z,
                                                             float size,
                                                             float sizeBlock,
                                                             boolean playSound,
                                                             boolean isSmoking,
                                                             boolean isFlaming,
                                                             boolean isDestroyBlock,
                                                             int countSetFireEntity) {
        return newExplosion(w, entityExploded, player, x, y, z, size, sizeBlock, size, sizeBlock, playSound, isSmoking,
                isFlaming, isDestroyBlock, countSetFireEntity, null);
    }

    public static MCH_Explosion.ExplosionResult newExplosion(
                                                             World world,
                                                             @Nullable Entity entityExploded,
                                                             @Nullable Entity player,
                                                             double x,
                                                             double y,
                                                             double z,
                                                             float size,
                                                             float sizeBlock,
                                                             boolean playSound,
                                                             boolean isSmoking,
                                                             boolean isFlaming,
                                                             boolean isDestroyBlock,
                                                             int countSetFireEntity,
                                                             @Nullable MCH_DamageFactor df) {
        return newExplosion(world, entityExploded, player, x, y, z, size, sizeBlock, size, sizeBlock, playSound, isSmoking,
                isFlaming, isDestroyBlock, countSetFireEntity, df);
    }

    public static MCH_Explosion.ExplosionResult newExplosion(
                                                             World world,
                                                             @Nullable Entity entityExploded,
                                                             @Nullable Entity player,
                                                             double x,
                                                             double y,
                                                             double z,
                                                             float size,
                                                             float sizeBlock,
                                                             float damagePower,
                                                             float blockPower,
                                                             boolean playSound,
                                                             boolean isSmoking,
                                                             boolean isFlaming,
                                                             boolean isDestroyBlock,
                                                             int countSetFireEntity) {
        return newExplosion(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, blockPower,
                playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, null);
    }

                                    public static MCH_Explosion.ExplosionResult newExplosion(
                                                                 World world,
                                                                 @Nullable Entity entityExploded,
                                                                 @Nullable Entity player,
                                                                 double x,
                                                                 double y,
                                                                 double z,
                                                                 float size,
                                                                 float sizeBlock,
                                                                 float damagePower,
                                                                 float blockPower,
                                                                 float damageRadius,
                                                                 boolean playSound,
                                                                 boolean isSmoking,
                                                                 boolean isFlaming,
                                                                 boolean isDestroyBlock,
                                                                 int countSetFireEntity) {
                                    return newExplosion(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, blockPower,
                                        damageRadius, playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, null);
                                    }

    public static MCH_Explosion.ExplosionResult newExplosion(
                                                             World world,
                                                             @Nullable Entity entityExploded,
                                                             @Nullable Entity player,
                                                             double x,
                                                             double y,
                                                             double z,
                                                             float size,
                                                             float sizeBlock,
                                                             float damagePower,
                                                             boolean playSound,
                                                             boolean isSmoking,
                                                             boolean isFlaming,
                                                             boolean isDestroyBlock,
                                                             int countSetFireEntity) {
        return newExplosion(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, sizeBlock,
                playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, null);
    }

    public static MCH_Explosion.ExplosionResult newExplosion(
                                                             World world,
                                                             @Nullable Entity entityExploded,
                                                             @Nullable Entity player,
                                                             double x,
                                                             double y,
                                                             double z,
                                                             float size,
                                                             float sizeBlock,
                                                             float damagePower,
                                                             float blockPower,
                                                             boolean playSound,
                                                             boolean isSmoking,
                                                             boolean isFlaming,
                                                             boolean isDestroyBlock,
                                                             int countSetFireEntity,
                                                             MCH_DamageFactor df) {
                                    return newExplosion(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, blockPower,
                                        size, playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, df);
                                    }

                                    public static MCH_Explosion.ExplosionResult newExplosion(
                                                                 World world,
                                                                 @Nullable Entity entityExploded,
                                                                 @Nullable Entity player,
                                                                 double x,
                                                                 double y,
                                                                 double z,
                                                                 float size,
                                                                 float sizeBlock,
                                                                 float damagePower,
                                                                 float blockPower,
                                                                 float damageRadius,
                                                                 boolean playSound,
                                                                 boolean isSmoking,
                                                                 boolean isFlaming,
                                                                 boolean isDestroyBlock,
                                                                 int countSetFireEntity,
                                                                 MCH_DamageFactor df) {
        if (world.isRemote) {
            return null;
        } else {
            MCH_ExplosionV2 exp = new MCH_ExplosionV2(world, entityExploded, player, x, y, z, size, isFlaming,
                    world.getGameRules().getBoolean("mobGriefing"));
            exp.isDestroyBlock = isDestroyBlock;
            exp.explosionSizeBlock = sizeBlock;
            exp.explosionBlockPower = blockPower;
            exp.explosionDamagePower = damagePower;
                                        exp.explosionDamageRadius = damageRadius;
            exp.countSetFireEntity = countSetFireEntity;
            exp.isPlaySound = playSound;
            exp.isInWater = false;
            exp.damageFactor = df;
            exp.doExplosionA();
            exp.doExplosionB(false);
            DataExplosionParameters param = new DataExplosionParameters();
            param.exploderID = W_Entity.getEntityId(entityExploded);
            param.x = x;
            param.y = y;
            param.z = z;
            param.size = size;
            param.inWater = false;
            param.setAffectedPositions(exp.getAffectedBlockPositions());
            new PacketParticleEffect(param).sendAround(world);
            return exp.getResult();
        }
    }

    @Nullable
    public static MCH_Explosion.ExplosionResult newExplosionInWater(
                                                                    World world,
                                                                    @Nullable Entity entityExploded,
                                                                    @Nullable Entity player,
                                                                    double x,
                                                                    double y,
                                                                    double z,
                                                                    float size,
                                                                    float sizeBlock,
                                                                    boolean playSound,
                                                                    boolean isSmoking,
                                                                    boolean isFlaming,
                                                                    boolean isDestroyBlock,
                                                                    int countSetFireEntity,
                                                                    MCH_DamageFactor df) {
        return newExplosionInWater(world, entityExploded, player, x, y, z, size, sizeBlock, size, sizeBlock, playSound,
                isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, df);
    }

    public static MCH_Explosion.ExplosionResult newExplosionInWater(
                                                                    World world,
                                                                    @Nullable Entity entityExploded,
                                                                    @Nullable Entity player,
                                                                    double x,
                                                                    double y,
                                                                    double z,
                                                                    float size,
                                                                    float sizeBlock,
                                                                    float damagePower,
                                                                    float blockPower,
                                                                    boolean playSound,
                                                                    boolean isSmoking,
                                                                    boolean isFlaming,
                                                                    boolean isDestroyBlock,
                                                                    int countSetFireEntity) {
        return newExplosionInWater(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, blockPower,
                playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, null);
    }

                                        public static MCH_Explosion.ExplosionResult newExplosionInWater(
                                                                        World world,
                                                                        @Nullable Entity entityExploded,
                                                                        @Nullable Entity player,
                                                                        double x,
                                                                        double y,
                                                                        double z,
                                                                        float size,
                                                                        float sizeBlock,
                                                                        float damagePower,
                                                                        float blockPower,
                                                                        float damageRadius,
                                                                        boolean playSound,
                                                                        boolean isSmoking,
                                                                        boolean isFlaming,
                                                                        boolean isDestroyBlock,
                                                                        int countSetFireEntity) {
                                        return newExplosionInWater(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, blockPower,
                                            damageRadius, playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, null);
                                        }

    public static MCH_Explosion.ExplosionResult newExplosionInWater(
                                                                    World world,
                                                                    @Nullable Entity entityExploded,
                                                                    @Nullable Entity player,
                                                                    double x,
                                                                    double y,
                                                                    double z,
                                                                    float size,
                                                                    float sizeBlock,
                                                                    float damagePower,
                                                                    float blockPower,
                                                                    boolean playSound,
                                                                    boolean isSmoking,
                                                                    boolean isFlaming,
                                                                    boolean isDestroyBlock,
                                                                    int countSetFireEntity,
                                                                    MCH_DamageFactor df) {
                                        return newExplosionInWater(world, entityExploded, player, x, y, z, size, sizeBlock, damagePower, blockPower,
                                            size, playSound, isSmoking, isFlaming, isDestroyBlock, countSetFireEntity, df);
                                        }

                                        public static MCH_Explosion.ExplosionResult newExplosionInWater(
                                                                        World world,
                                                                        @Nullable Entity entityExploded,
                                                                        @Nullable Entity player,
                                                                        double x,
                                                                        double y,
                                                                        double z,
                                                                        float size,
                                                                        float sizeBlock,
                                                                        float damagePower,
                                                                        float blockPower,
                                                                        float damageRadius,
                                                                        boolean playSound,
                                                                        boolean isSmoking,
                                                                        boolean isFlaming,
                                                                        boolean isDestroyBlock,
                                                                        int countSetFireEntity,
                                                                        MCH_DamageFactor df) {
        if (world.isRemote) {
            return null;
        } else {
            MCH_ExplosionV2 exp = new MCH_ExplosionV2(world, entityExploded, player, x, y, z, size, isFlaming,
                    world.getGameRules().getBoolean("mobGriefing"));
            exp.isDestroyBlock = isDestroyBlock;
            exp.explosionSizeBlock = sizeBlock;
            exp.explosionBlockPower = blockPower;
            exp.explosionDamagePower = damagePower;
                                            exp.explosionDamageRadius = damageRadius;
            exp.countSetFireEntity = countSetFireEntity;
            exp.isPlaySound = playSound;
            exp.isInWater = true;
            exp.damageFactor = df;
            exp.doExplosionA();
            exp.doExplosionB(false);
            DataExplosionParameters param = new DataExplosionParameters();
            param.exploderID = W_Entity.getEntityId(entityExploded);
            param.x = x;
            param.y = y;
            param.z = z;
            param.size = size;
            param.inWater = true;
            param.setAffectedPositions(exp.getAffectedBlockPositions());
            new PacketParticleEffect(param).sendAround(world);
            return exp.getResult();
        }
    }

    public static class ExplosionResult {

        public boolean hitEntity = false;
    }
}
