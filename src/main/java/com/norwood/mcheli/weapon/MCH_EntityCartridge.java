package com.norwood.mcheli.weapon;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_MovingObjectPosition;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_EntityCartridge extends W_Entity {

    public final String texture_name;
    public final _IModelCustom model;
    private final float bound;
    private final float gravity;
    private final float scale;
    public float targetYaw;
    public float targetPitch;
    private int countOnUpdate;

    public MCH_EntityCartridge(World par1World, MCH_Cartridge c, double x, double y, double z, double mx, double my,
                               double mz) {
        super(par1World);
        this.setPositionAndRotation(x, y, z, 0.0F, 0.0F);
        this.motionX = mx;
        this.motionY = my;
        this.motionZ = mz;
        this.texture_name = c.name;
        this.model = c.model;
        this.bound = c.bound;
        this.gravity = c.gravity;
        this.scale = c.scale;
        this.countOnUpdate = 0;
    }

    @SideOnly(Side.CLIENT)
    public static void spawnCartridge(
                                      World world, MCH_Cartridge cartridge, double x, double y, double z, double mx,
                                      double my, double mz, float yaw, float pitch) {
        if (cartridge != null) {
            MCH_EntityCartridge entityFX = new MCH_EntityCartridge(
                    world, cartridge, x, y, z, mx + (world.rand.nextFloat() - 0.5) * 0.07, my,
                    mz + (world.rand.nextFloat() - 0.5) * 0.07);
            entityFX.prevRotationYaw = yaw;
            entityFX.rotationYaw = yaw;
            entityFX.targetYaw = yaw;
            entityFX.prevRotationPitch = pitch;
            entityFX.rotationPitch = pitch;
            entityFX.targetPitch = pitch;
            float cy = yaw + cartridge.yaw;
            float cp = pitch + cartridge.pitch;
            double tX = -MathHelper.sin(cy / 180.0F * (float) Math.PI) * MathHelper.cos(cp / 180.0F * (float) Math.PI);
            double tZ = MathHelper.cos(cy / 180.0F * (float) Math.PI) * MathHelper.cos(cp / 180.0F * (float) Math.PI);
            double tY = -MathHelper.sin(cp / 180.0F * (float) Math.PI);
            double d = MathHelper.sqrt(tX * tX + tY * tY + tZ * tZ);
            if (Math.abs(d) > 0.001) {
                entityFX.motionX = entityFX.motionX + tX * cartridge.acceleration / d;
                entityFX.motionY = entityFX.motionY + tY * cartridge.acceleration / d;
                entityFX.motionZ = entityFX.motionZ + tZ * cartridge.acceleration / d;
            }

            world.spawnEntity(entityFX);
        }
    }

    public float getScale() {
        return this.scale;
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        if (this.countOnUpdate < MCH_Config.AliveTimeOfCartridge.prmInt) {
            this.countOnUpdate++;
        } else {
            this.setDead();
        }

        this.motionX *= 0.98;
        this.motionZ *= 0.98;
        this.motionY = this.motionY + this.gravity;
        this.move();
    }

    public void rotation() {
        if (this.rotationYaw < this.targetYaw - 3.0F) {
            this.rotationYaw += 10.0F;
            if (this.rotationYaw > this.targetYaw) {
                this.rotationYaw = this.targetYaw;
            }
        } else if (this.rotationYaw > this.targetYaw + 3.0F) {
            this.rotationYaw -= 10.0F;
            if (this.rotationYaw < this.targetYaw) {
                this.rotationYaw = this.targetYaw;
            }
        }

        if (this.rotationPitch < this.targetPitch) {
            this.rotationPitch += 10.0F;
            if (this.rotationPitch > this.targetPitch) {
                this.rotationPitch = this.targetPitch;
            }
        } else if (this.rotationPitch > this.targetPitch) {
            this.rotationPitch -= 10.0F;
            if (this.rotationPitch < this.targetPitch) {
                this.rotationPitch = this.targetPitch;
            }
        }
    }

    public void move() {
        Vec3d vec1 = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d vec2 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult m = W_WorldFunc.clip(this.world, vec1, vec2);
        double d = Math.max(Math.abs(this.motionX), Math.abs(this.motionY));
        d = Math.max(d, Math.abs(this.motionZ));
        if (W_MovingObjectPosition.isHitTypeTile(m)) {
            this.setPosition(m.hitVec.x, m.hitVec.y, m.hitVec.z);
            this.motionX = this.motionX + d * (this.rand.nextFloat() - 0.5F) * 0.1F;
            this.motionY = this.motionY + d * (this.rand.nextFloat() - 0.5F) * 0.1F;
            this.motionZ = this.motionZ + d * (this.rand.nextFloat() - 0.5F) * 0.1F;
            if (d > 0.1F) {
                this.targetYaw = this.targetYaw + (float) (d * (this.rand.nextFloat() - 0.5F) * 720.0);
                this.targetPitch = (float) (d * (this.rand.nextFloat() - 0.5F) * 720.0);
            } else {
                this.targetPitch = 0.0F;
            }

            switch (m.sideHit) {
                case DOWN:
                    if (this.motionY > 0.0) {
                        this.motionY = -this.motionY * this.bound;
                    }
                    break;
                case UP:
                    if (this.motionY < 0.0) {
                        this.motionY = -this.motionY * this.bound;
                    }

                    this.targetPitch *= 0.3F;
                    break;
                case NORTH:
                    if (this.motionZ > 0.0) {
                        this.motionZ = -this.motionZ * this.bound;
                    } else {
                        this.posZ = this.posZ + this.motionZ;
                    }
                    break;
                case SOUTH:
                    if (this.motionZ < 0.0) {
                        this.motionZ = -this.motionZ * this.bound;
                    } else {
                        this.posZ = this.posZ + this.motionZ;
                    }
                    break;
                case WEST:
                    if (this.motionX > 0.0) {
                        this.motionX = -this.motionX * this.bound;
                    } else {
                        this.posX = this.posX + this.motionX;
                    }
                    break;
                case EAST:
                    if (this.motionX < 0.0) {
                        this.motionX = -this.motionX * this.bound;
                    } else {
                        this.posX = this.posX + this.motionX;
                    }
            }
        } else {
            this.posX = this.posX + this.motionX;
            this.posY = this.posY + this.motionY;
            this.posZ = this.posZ + this.motionZ;
            if (d > 0.05F) {
                this.rotation();
            }
        }
    }

    protected void readEntityFromNBT(@NotNull NBTTagCompound var1) {}

    protected void writeEntityToNBT(@NotNull NBTTagCompound var1) {}
}
