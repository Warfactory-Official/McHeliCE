package com.norwood.mcheli.tank;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntityHitBox;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MCH_EntityWheel extends W_Entity {

    public Vec3d pos;
    boolean isPlus;
    private MCH_EntityAircraft parents;

    public MCH_EntityWheel(World w) {
        super(w);
        this.setSize(1.0F, 1.0F);
        this.stepHeight = 1.5F;
        this.isImmuneToFire = true;
        this.isPlus = false;
    }

    public void setWheelPos(Vec3d pos, Vec3d weightedCenter) {
        this.pos = pos;
        this.isPlus = pos.z >= weightedCenter.z;
    }

    public void travelToDimension(int dimensionId) {}

    public MCH_EntityAircraft getParents() {
        return this.parents;
    }

    public void setParents(MCH_EntityAircraft parents) {
        this.parents = parents;
    }

    protected void readEntityFromNBT(@NotNull NBTTagCompound compound) {
        this.setDead();
    }

    protected void writeEntityToNBT(@NotNull NBTTagCompound compound) {}

    public void move(@NotNull MoverType type, double x, double y, double z) {
        this.world.profiler.startSection("move");
        double d2 = x;
        double d3 = y;
        double d4 = z;
        List<AxisAlignedBB> list1 = this.getCollisionBoxes(this, this.getEntityBoundingBox().expand(x, y, z));
        AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
        if (y != 0.0) {
            for (AxisAlignedBB axisAlignedBB : list1) {
                y = axisAlignedBB.calculateYOffset(this.getEntityBoundingBox(), y);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
        }

        boolean flag = this.onGround || d3 != y && d3 < 0.0;
        if (x != 0.0) {
            for (AxisAlignedBB axisAlignedBB : list1) {
                x = axisAlignedBB.calculateXOffset(this.getEntityBoundingBox(), x);
            }

            if (x != 0.0) {
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0, 0.0));
            }
        }

        if (z != 0.0) {
            for (AxisAlignedBB axisAlignedBB : list1) {
                z = axisAlignedBB.calculateZOffset(this.getEntityBoundingBox(), z);
            }

            if (z != 0.0) {
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, z));
            }
        }

        if (this.stepHeight > 0.0F && flag && (d2 != x || d4 != z)) {
            double d14 = x;
            double d6 = y;
            double d7 = z;
            AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
            this.setEntityBoundingBox(axisalignedbb);
            y = this.stepHeight;
            List<AxisAlignedBB> list = this.getCollisionBoxes(this, this.getEntityBoundingBox().expand(d2, y, d4));
            AxisAlignedBB axisalignedbb2 = this.getEntityBoundingBox();
            AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0, d4);
            double d8 = y;

            for (AxisAlignedBB axisAlignedBB1 : list) {
                d8 = axisAlignedBB1.calculateYOffset(axisalignedbb3, d8);
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0, d8, 0.0);
            double d18 = d2;

            for (AxisAlignedBB element : list) {
                d18 = element.calculateXOffset(axisalignedbb2, d18);
            }

            axisalignedbb2 = axisalignedbb2.offset(d18, 0.0, 0.0);
            double d19 = d4;

            for (AxisAlignedBB item : list) {
                d19 = item.calculateZOffset(axisalignedbb2, d19);
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0, 0.0, d19);
            AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
            double d20 = y;

            for (AxisAlignedBB value : list) {
                d20 = value.calculateYOffset(axisalignedbb4, d20);
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0, d20, 0.0);
            double d21 = d2;

            for (AxisAlignedBB bb : list) {
                d21 = bb.calculateXOffset(axisalignedbb4, d21);
            }

            axisalignedbb4 = axisalignedbb4.offset(d21, 0.0, 0.0);
            double d22 = d4;

            for (AxisAlignedBB alignedBB : list) {
                d22 = alignedBB.calculateZOffset(axisalignedbb4, d22);
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0, 0.0, d22);
            double d23 = d18 * d18 + d19 * d19;
            double d9 = d21 * d21 + d22 * d22;
            if (d23 > d9) {
                x = d18;
                z = d19;
                y = -d8;
                this.setEntityBoundingBox(axisalignedbb2);
            } else {
                x = d21;
                z = d22;
                y = -d20;
                this.setEntityBoundingBox(axisalignedbb4);
            }

            for (AxisAlignedBB axisAlignedBB : list) {
                y = axisAlignedBB.calculateYOffset(this.getEntityBoundingBox(), y);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
            if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                x = d14;
                y = d6;
                z = d7;
                this.setEntityBoundingBox(axisalignedbb1);
            }
        }

        this.world.profiler.endSection();
        this.world.profiler.startSection("rest");
        this.resetPositionToBB();
        this.collidedHorizontally = d2 != x || d4 != z;
        this.collidedVertically = d3 != y;
        this.onGround = this.collidedVertically && d3 < 0.0;
        this.collided = this.collidedHorizontally || this.collidedVertically;
        int j6 = MathHelper.floor(this.posX);
        int i1 = MathHelper.floor(this.posY - 0.2F);
        int k6 = MathHelper.floor(this.posZ);
        BlockPos blockpos = new BlockPos(j6, i1, k6);
        IBlockState iblockstate = this.world.getBlockState(blockpos);
        if (iblockstate.getMaterial() == Material.AIR) {
            BlockPos blockpos1 = blockpos.down();
            IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
            Block block1 = iblockstate1.getBlock();
            if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
                iblockstate = iblockstate1;
                blockpos = blockpos1;
            }
        }

        this.updateFallState(y, this.onGround, iblockstate, blockpos);
        if (d2 != x) {
            this.motionX = 0.0;
        }

        if (d4 != z) {
            this.motionZ = 0.0;
        }

        Block block = iblockstate.getBlock();
        if (d3 != y) {
            block.onLanded(this.world, this);
        }

        try {
            this.doBlockCollisions();
        } catch (Throwable var45) {
            CrashReport crashreport = CrashReport.makeCrashReport(var45, "Checking entity block collision");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
            this.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
        }

        this.world.profiler.endSection();
    }

    public List<AxisAlignedBB> getCollisionBoxes(Entity entityIn, AxisAlignedBB aabb) {
        ArrayList<AxisAlignedBB> collidingBoundingBoxes = new ArrayList<>();
        this.getCollisionBoxes(entityIn, aabb, collidingBoundingBoxes);
        double d0 = 0.25;
        List<Entity> list = entityIn.world.getEntitiesWithinAABBExcludingEntity(entityIn, aabb.grow(d0, d0, d0));

        for (Entity entity : list) {
            if (!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntitySeat) &&
                    !(entity instanceof MCH_EntityHitBox) && entity != this.parents) {
                AxisAlignedBB axisalignedbb1 = entity.getCollisionBoundingBox();
                if (axisalignedbb1 != null && axisalignedbb1.intersects(aabb)) {
                    collidingBoundingBoxes.add(axisalignedbb1);
                }

                axisalignedbb1 = entityIn.getCollisionBox(entity);
                if (axisalignedbb1 != null && axisalignedbb1.intersects(aabb)) {
                    collidingBoundingBoxes.add(axisalignedbb1);
                }
            }
        }

        return collidingBoundingBoxes;
    }

    private boolean getCollisionBoxes(Entity entityIn, AxisAlignedBB aabb, List<AxisAlignedBB> outList) {
        int i = MathHelper.floor(aabb.minX) - 1;
        int j = MathHelper.ceil(aabb.maxX) + 1;
        int k = MathHelper.floor(aabb.minY) - 1;
        int l = MathHelper.ceil(aabb.maxY) + 1;
        int i1 = MathHelper.floor(aabb.minZ) - 1;
        int j1 = MathHelper.ceil(aabb.maxZ) + 1;
        WorldBorder worldborder = entityIn.world.getWorldBorder();
        boolean flag = entityIn.isOutsideBorder();
        boolean flag1 = entityIn.world.isInsideWorldBorder(entityIn);
        IBlockState iblockstate = Blocks.STONE.getDefaultState();
        PooledMutableBlockPos blockpos = PooledMutableBlockPos.retain();

        try {
            for (int k1 = i; k1 < j; k1++) {
                for (int l1 = i1; l1 < j1; l1++) {
                    boolean flag2 = k1 == i || k1 == j - 1;
                    boolean flag3 = l1 == i1 || l1 == j1 - 1;
                    if ((!flag2 || !flag3) && entityIn.world.isBlockLoaded(blockpos.setPos(k1, 64, l1))) {
                        for (int i2 = k; i2 < l; i2++) {
                            if (!flag2 && !flag3 || i2 != l - 1) {
                                if (flag == flag1) {
                                    entityIn.setOutsideBorder(!flag1);
                                }

                                blockpos.setPos(k1, i2, l1);
                                IBlockState iblockstate1;
                                if (!worldborder.contains(blockpos) && flag1) {
                                    iblockstate1 = iblockstate;
                                } else {
                                    iblockstate1 = entityIn.world.getBlockState(blockpos);
                                }

                                iblockstate1.addCollisionBoxToList(entityIn.world, blockpos, aabb, outList, entityIn,
                                        false);
                            }
                        }
                    }
                }
            }
        } finally {
            blockpos.release();
        }

        return !outList.isEmpty();
    }
}
