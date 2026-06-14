package com.norwood.mcheli.flare;

import com.norwood.mcheli.weapon.MCH_IEntityLockChecker;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public class MCH_EntityChaff extends W_Entity implements MCH_IEntityLockChecker {

    public static final int MAX_TICK_EXISTED = 120;
    public double gravity = -0.001D;
    public double airResistance = 0.99D;

    public MCH_EntityChaff(World world) {
        super(world);
        this.setSize(1.0F, 1.0F);
    }

    @SuppressWarnings("unused")
    public MCH_EntityChaff(World world, double x, double y, double z, double mx, double my, double mz) {
        this(world);
        this.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        this.motionX = mx;
        this.motionY = my;
        this.motionZ = mz;
    }

    @Override
    public void onUpdate() {
        if (world.isRemote) {
            setRenderDistanceWeight(500D);
        }

        if (ticksExisted > MAX_TICK_EXISTED) {
            setDead();
            return;
        }

        if (!world.isRemote && !world.isBlockLoaded(new BlockPos(posX, posY, posZ))) {
            setDead();
            return;
        }

        super.onUpdate();

        if (!world.isRemote) {
            this.onUpdateCollided();
        }

        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;

        this.motionY += this.gravity;
        this.motionX *= this.airResistance;
        this.motionZ *= this.airResistance;

        if (!world.isRemote && (this.isInWater() || this.onGround)) {
            setDead();
        }

        this.setPosition(this.posX, this.posY, this.posZ);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isInRangeToRenderDist(double distance) {
        double d0 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0D;
        if (Double.isNaN(d0)) d0 = 4.0D;
        d0 = d0 * 64.0D;
        return distance < d0 * d0;
    }

    @Override
    public boolean canLockEntity(Entity entity) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(@NotNull NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(@NotNull NBTTagCompound compound) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    protected void onUpdateCollided() {
        Vec3d start = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d end = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

        RayTraceResult result = this.world.rayTraceBlocks(start, end);
        if (result != null) {
            this.setDead();
        }
    }
}