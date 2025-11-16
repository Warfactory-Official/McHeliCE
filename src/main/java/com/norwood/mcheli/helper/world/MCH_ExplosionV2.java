package com.norwood.mcheli.helper.world;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_DamageFactor;
import com.norwood.mcheli.MCH_Explosion;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.flare.MCH_EntityFlare;
import com.norwood.mcheli.particles.MCH_ParticleParam;
import com.norwood.mcheli.particles.MCH_ParticlesUtil;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wrapper.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MCH_ExplosionV2 extends Explosion {

    private static final Random explosionRNG = new Random();
    public final int field_77289_h = 16;
    public final Entity exploder;
    public final double x;
    public final double y;
    public final double z;
    public final float size;
    public final boolean causesFire;
    public final boolean damagesTerrain;
    public final World world;
    public boolean isDestroyBlock;
    public int countSetFireEntity;
    public boolean isPlaySound;
    public boolean isInWater;
    public final EntityPlayer explodedPlayer;
    public float explosionSizeBlock;
    public MCH_DamageFactor damageFactor = null;
    private final MCH_Explosion.ExplosionResult result;

    @SideOnly(Side.CLIENT)
    public MCH_ExplosionV2(World worldIn, double x, double y, double z, float size, List<BlockPos> affectedPositions) {
        this(worldIn, null, null, x, y, z, size, false, true);
        this.getAffectedBlockPositions().addAll(affectedPositions);
        this.isPlaySound = false;
    }

    public MCH_ExplosionV2(
                           World worldIn, @Nullable Entity exploderIn, @Nullable Entity player, double x, double y,
                           double z, float size, boolean flaming, boolean damagesTerrain) {
        super(worldIn, exploderIn, x, y, z, size, flaming, damagesTerrain);
        this.world = worldIn;
        this.exploder = exploderIn;
        this.explodedPlayer = player instanceof EntityPlayer ? (EntityPlayer) player : null;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.causesFire = flaming;
        this.damagesTerrain = damagesTerrain;
        this.isDestroyBlock = false;
        this.explosionSizeBlock = size;
        this.countSetFireEntity = 0;
        this.isPlaySound = true;
        this.isInWater = false;
        this.result = new MCH_Explosion.ExplosionResult();
    }

    public static void playExplosionSound(World world, double x, double y, double z) {
        world.playSound(
                x,
                y,
                z,
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS,
                4.0F,
                (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F,
                false);
    }

    public static void effectMODExplosion(World world, double x, double y, double z, float size,
                                          List<BlockPos> affectedPositions) {
        MCH_ExplosionV2 explosion = new MCH_ExplosionV2(world, x, y, z, size, affectedPositions);
        explosion.doExplosionA();
        explosion.doExplosionB(true, false);
    }

    public static void effectVanillaExplosion(World world, double x, double y, double z, float size,
                                              List<BlockPos> affectedPositions) {
        MCH_ExplosionV2 explosion = new MCH_ExplosionV2(world, x, y, z, size, affectedPositions);
        explosion.doExplosionA();
        explosion.doExplosionB(true, true);
    }

    public static void effectExplosionInWater(World world, double x, double y, double z, float size) {
        if (!(size <= 0.0F)) {
            int range = (int) (size + 0.5);
            int ex = (int) (x + 0.5);
            int ey = (int) (y + 0.5);
            int ez = (int) (z + 0.5);

            for (int i1 = -range; i1 <= range; i1++) {
                if (ey + i1 >= 1) {
                    for (int j1 = -range; j1 <= range; j1++) {
                        for (int k1 = -range; k1 <= range; k1++) {
                            int d = j1 * j1 + i1 * i1 + k1 * k1;
                            if (d < range * range && W_Block.isEqualTo(
                                    W_WorldFunc.getBlock(world, ex + j1, ey + i1, ez + k1), W_Block.getWater())) {
                                int n = explosionRNG.nextInt(2);

                                for (int i = 0; i < n; i++) {
                                    MCH_ParticleParam prm = new MCH_ParticleParam(
                                            world,
                                            "splash",
                                            ex + j1,
                                            ey + i1,
                                            ez + k1,
                                            (double) j1 / range * (explosionRNG.nextFloat() - 0.2),
                                            1.0 - Math.sqrt(j1 * j1 + k1 * k1) / range +
                                                    explosionRNG.nextFloat() * 0.4 * range * 0.4,
                                            (double) k1 / range * (explosionRNG.nextFloat() - 0.2),
                                            explosionRNG.nextInt(range) * 3 + range);
                                    MCH_ParticlesUtil.spawnParticle(prm);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void doExplosionA() {
        HashSet<BlockPos> hashset = new HashSet<>();

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15) {
                        double d3 = i / 15.0F * 2.0F - 1.0F;
                        double d4 = j / 15.0F * 2.0F - 1.0F;
                        double d5 = k / 15.0F * 2.0F - 1.0F;
                        double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                        d3 /= d6;
                        d4 /= d6;
                        d5 /= d6;
                        float f1 = this.explosionSizeBlock * (0.7F + this.world.rand.nextFloat() * 0.6F);
                        double d0 = this.x;
                        double d1 = this.y;

                        for (double d2 = this.z; f1 > 0.0F; f1 -= 0.22500001F) {
                            int l = MathHelper.floor(d0);
                            int i1 = MathHelper.floor(d1);
                            int j1 = MathHelper.floor(d2);
                            int k1 = W_WorldFunc.getBlockId(this.world, l, i1, j1);
                            BlockPos blockpos = new BlockPos(l, i1, j1);
                            IBlockState iblockstate = this.world.getBlockState(blockpos);
                            Block block = iblockstate.getBlock();
                            if (k1 > 0) {
                                float f3;
                                if (this.exploder != null) {
                                    f3 = W_Entity.getBlockExplosionResistance(this.exploder, this, this.world, l, i1,
                                            j1, block);
                                } else {
                                    f3 = block.getExplosionResistance(this.world, blockpos, this.exploder, this);
                                }

                                if (this.isInWater) {
                                    f3 *= this.world.rand.nextFloat() * 0.2F + 0.2F;
                                }

                                f1 -= (f3 + 0.3F) * 0.3F;
                            }

                            if (f1 > 0.0F && (this.exploder == null ||
                                    W_Entity.shouldExplodeBlock(this.exploder, this, this.world, l, i1, j1, k1, f1))) {
                                hashset.add(blockpos);
                            }

                            d0 += d3 * 0.3F;
                            d1 += d4 * 0.3F;
                            d2 += d5 * 0.3F;
                        }
                    }
                }
            }
        }

        float f = this.size * 2.0F;
        this.getAffectedBlockPositions().addAll(hashset);
        int i = MathHelper.floor(this.x - f - 1.0);
        int j = MathHelper.floor(this.x + f + 1.0);
        int kx = MathHelper.floor(this.y - f - 1.0);
        int l1 = MathHelper.floor(this.y + f + 1.0);
        int i2 = MathHelper.floor(this.z - f - 1.0);
        int j2 = MathHelper.floor(this.z + f + 1.0);
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this.exploder,
                W_AxisAlignedBB.getAABB(i, kx, i2, j, l1, j2));
        Vec3d vec3 = new Vec3d(this.x, this.y, this.z);

        for (Entity entity : list) {
            double d7 = entity.getDistance(this.x, this.y, this.z) / f;
            if (d7 <= 1.0) {
                double d0 = entity.posX - this.x;
                double d1 = entity.posY + entity.getEyeHeight() - this.y;
                double d2 = entity.posZ - this.z;
                double d8 = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                if (d8 != 0.0) {
                    d0 /= d8;
                    d1 /= d8;
                    d2 /= d8;
                    double d9 = this.getBlockDensity(vec3, entity.getEntityBoundingBox());
                    double d10 = (1.0 - d7) * d9;
                    float damage = (int) ((d10 * d10 + d10) / 2.0 * 8.0 * f + 1.0);
                    if (damage > 0.0F && !(entity instanceof EntityItem) && !(entity instanceof EntityExpBottle) &&
                            !(entity instanceof EntityXPOrb) && !W_Entity.isEntityFallingBlock(entity)) {
                        if (!(entity instanceof MCH_EntityBaseBullet) ||
                                !(this.explodedPlayer instanceof EntityPlayer)) {
                            MCH_Lib.DbgLog(this.world,
                                    "MCH_Explosion.doExplosionA:Damage=%.1f:HitEntity=" + entity.getClass(), damage);
                            this.result.hitEntity = true;
                        } else if (!W_Entity.isEqual(((MCH_EntityBaseBullet) entity).shootingEntity,
                                this.explodedPlayer)) {
                                    this.result.hitEntity = true;
                                    MCH_Lib.DbgLog(this.world,
                                            "MCH_Explosion.doExplosionA:Damage=%.1f:HitEntityBullet=" +
                                                    entity.getClass(),
                                            damage);
                                }
                    }

                    MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
                    DamageSource ds = DamageSource.causeExplosionDamage(this);
                    damage = MCH_Config.applyDamageVsEntity(entity, ds, damage);
                    damage *= this.damageFactor != null ? this.damageFactor.getDamageFactor(entity) : 1.0F;
                    W_Entity.attackEntityFrom(entity, ds, damage);
                    double d11 = d10;
                    if (entity instanceof EntityLivingBase) {
                        d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, d10);
                    }

                    if (!(entity instanceof MCH_EntityBaseBullet)) {
                        entity.motionX += d0 * d11 * 0.4;
                        entity.motionY += d1 * d11 * 0.1;
                        entity.motionZ += d2 * d11 * 0.4;
                    }

                    if (entity instanceof EntityPlayer) {
                        this.getPlayerKnockbackMap().put((EntityPlayer) entity,
                                new Vec3d(d0 * d10, d1 * d10, d2 * d10));
                    }

                    if (damage > 0.0F && this.countSetFireEntity > 0) {
                        double fireFactor = 1.0 - d8 / f;
                        if (fireFactor > 0.0) {
                            entity.setFire((int) (fireFactor * this.countSetFireEntity));
                        }
                    }
                }
            }
        }
    }

    private double getBlockDensity(Vec3d vec3, AxisAlignedBB bb) {
        double d0 = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
        double d1 = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
        double d2 = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
        if (d0 >= 0.0 && d1 >= 0.0 && d2 >= 0.0) {
            int i = 0;
            int j = 0;

            for (float f = 0.0F; f <= 1.0F; f = (float) (f + d0)) {
                for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float) (f1 + d1)) {
                    for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float) (f2 + d2)) {
                        double d3 = bb.minX + (bb.maxX - bb.minX) * f;
                        double d4 = bb.minY + (bb.maxY - bb.minY) * f1;
                        double d5 = bb.minZ + (bb.maxZ - bb.minZ) * f2;
                        if (this.world.rayTraceBlocks(new Vec3d(d3, d4, d5), vec3, false, true, false) == null) {
                            i++;
                        }

                        j++;
                    }
                }
            }

            return (double) i / j;
        } else {
            return 0.0;
        }
    }

    public void doExplosionB(boolean spawnParticles) {
        this.doExplosionB(spawnParticles, false);
    }

    private void doExplosionB(boolean spawnParticles, boolean vanillaMode) {
        if (this.isPlaySound) {
            this.world
                    .playSound(
                            null,
                            this.x,
                            this.y,
                            this.z,
                            SoundEvents.ENTITY_GENERIC_EXPLODE,
                            SoundCategory.BLOCKS,
                            4.0F,
                            (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
        }

        if (this.damagesTerrain) {
            Iterator<BlockPos> iterator = this.getAffectedBlockPositions().iterator();
            int cnt = 0;
            int flareCnt = (int) this.size;

            while (iterator.hasNext()) {
                BlockPos chunkposition = iterator.next();
                int i = W_ChunkPosition.getChunkPosX(chunkposition);
                int j = W_ChunkPosition.getChunkPosY(chunkposition);
                int k = W_ChunkPosition.getChunkPosZ(chunkposition);
                int l = W_WorldFunc.getBlockId(this.world, i, j, k);
                cnt++;
                if (spawnParticles) {
                    if (vanillaMode) {
                        this.spawnVanillaExlosionEffect(i, j, k);
                    } else if (this.spawnExlosionEffect(cnt, i, j, k, flareCnt > 0)) {
                        flareCnt--;
                    }
                }

                if (l > 0 && this.isDestroyBlock && this.explosionSizeBlock > 0.0F &&
                        MCH_Config.Explosion_DestroyBlock.prmBool) {
                    Block block = W_Block.getBlockById(l);
                    if (block.canDropFromExplosion(this)) {
                        block.dropBlockAsItemWithChance(this.world, chunkposition,
                                this.world.getBlockState(chunkposition), 1.0F / this.explosionSizeBlock, 0);
                    }

                    block.onBlockExploded(this.world, chunkposition, this);
                }
            }
        }

        if (this.causesFire && MCH_Config.Explosion_FlamingBlock.prmBool) {
            for (BlockPos chunkpositionx : this.getAffectedBlockPositions()) {
                int ix = W_ChunkPosition.getChunkPosX(chunkpositionx);
                int jx = W_ChunkPosition.getChunkPosY(chunkpositionx);
                int kx = W_ChunkPosition.getChunkPosZ(chunkpositionx);
                int lx = W_WorldFunc.getBlockId(this.world, ix, jx, kx);
                IBlockState iblockstate = this.world.getBlockState(chunkpositionx.down());
                Block b = iblockstate.getBlock();
                if (lx == 0 && iblockstate.isOpaqueCube() && explosionRNG.nextInt(3) == 0) {
                    BlockPos blockpos = new BlockPos(ix, jx, kx);
                    this.world.setBlockState(blockpos, Blocks.FIRE.getDefaultState());
                }
            }
        }
    }

    private boolean spawnExlosionEffect(int cnt, int i, int j, int k, boolean spawnFlare) {
        boolean spawnedFlare = false;
        double d0 = i + this.world.rand.nextFloat();
        double d1 = j + this.world.rand.nextFloat();
        double d2 = k + this.world.rand.nextFloat();
        double mx = d0 - this.x;
        double my = d1 - this.y;
        double mz = d2 - this.z;
        double d6 = MathHelper.sqrt(mx * mx + my * my + mz * mz);
        mx /= d6;
        my /= d6;
        mz /= d6;
        double d7 = 0.5 / (d6 / this.size + 0.1);
        d7 *= this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F;
        mx *= d7 * 0.5;
        my *= d7 * 0.5;
        mz *= d7 * 0.5;
        double px = (d0 + this.x) / 2.0;
        double py = (d1 + this.y) / 2.0;
        double pz = (d2 + this.z) / 2.0;
        double r = Math.PI * this.world.rand.nextInt(360) / 180.0;
        if (this.size >= 4.0F && spawnFlare) {
            double a = Math.min(this.size / 12.0F, 0.6) * (0.5F + this.world.rand.nextFloat() * 0.5F);
            this.world.spawnEntity(new MCH_EntityFlare(this.world, px, py + 2.0, pz, Math.sin(r) * a,
                    (1.0 + my / 5.0) * a, Math.cos(r) * a, 2.0F, 0));
            spawnedFlare = true;
        }

        if (cnt % 4 == 0) {
            float bdf = Math.min(this.size / 3.0F, 2.0F) * (0.5F + this.world.rand.nextFloat() * 0.5F);
            MCH_ParticlesUtil.spawnParticleTileDust(
                    this.world,
                    (int) (px + 0.5),
                    (int) (py - 0.5),
                    (int) (pz + 0.5),
                    px,
                    py + 1.0,
                    pz,
                    Math.sin(r) * bdf,
                    0.5 + my / 5.0 * bdf,
                    Math.cos(r) * bdf,
                    Math.min(this.size / 2.0F, 3.0F) * (0.5F + this.world.rand.nextFloat() * 0.5F));
        }

        int es = (int) (Math.max(this.size, 4.0F));
        if (this.size <= 1.0F || cnt % es == 0) {
            if (this.world.rand.nextBoolean()) {
                my *= 3.0;
                mx *= 0.1;
                mz *= 0.1;
            } else {
                my *= 0.2;
                mx *= 3.0;
                mz *= 3.0;
            }

            MCH_ParticleParam prm = new MCH_ParticleParam(
                    this.world, "explode", px, py, pz, mx, my, mz, this.size < 8.0F ? this.size * 2.0F : 16.0F);
            prm.r = prm.g = prm.b = 0.3F + this.world.rand.nextFloat() * 0.4F;
            prm.r += 0.1F;
            prm.g += 0.05F;
            prm.b += 0.0F;
            prm.age = 10 + this.world.rand.nextInt(30);
            prm.age = (int) (prm.age * (Math.min(this.size, 6.0F)));
            prm.age = prm.age * 2 / 3;
            prm.diffusible = true;
            MCH_ParticlesUtil.spawnParticle(prm);
        }

        return spawnedFlare;
    }

    private void spawnVanillaExlosionEffect(int i, int j, int k) {
        double d0 = i + this.world.rand.nextFloat();
        double d1 = j + this.world.rand.nextFloat();
        double d2 = k + this.world.rand.nextFloat();
        double d3 = d0 - this.x;
        double d4 = d1 - this.y;
        double d5 = d2 - this.z;
        double d6 = MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
        d3 /= d6;
        d4 /= d6;
        d5 /= d6;
        double d7 = 0.5 / (d6 / this.size + 0.1);
        d7 *= this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F;
        d3 *= d7;
        d4 *= d7;
        d5 *= d7;
        MCH_ParticlesUtil.DEF_spawnParticle("explode", (d0 + this.x) / 2.0, (d1 + this.y) / 2.0, (d2 + this.z) / 2.0,
                d3, d4, d5, 10.0F);
        MCH_ParticlesUtil.DEF_spawnParticle("smoke", d0, d1, d2, d3, d4, d5, 10.0F);
    }

    public EntityLivingBase getExplosivePlacedBy() {
        return this.explodedPlayer != null ? this.explodedPlayer : super.getExplosivePlacedBy();
    }

    public MCH_Explosion.ExplosionResult getResult() {
        return this.result;
    }
}
