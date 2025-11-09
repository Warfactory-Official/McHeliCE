package com.norwood.mcheli.aircraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.wrapper.W_Entity;

public class MCH_EntityHitBox extends W_Entity {

    public MCH_EntityAircraft parent;
    public int debugId;

    public MCH_EntityHitBox(World world) {
        super(world);
        this.setSize(1.0F, 1.0F);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.parent = null;
        this.ignoreFrustumCheck = true;
        this.isImmuneToFire = true;
    }

    public MCH_EntityHitBox(World world, MCH_EntityAircraft ac, float w, float h) {
        this(world);
        this.setPosition(ac.posX, ac.posY + 1.0, ac.posZ);
        this.prevPosX = ac.posX;
        this.prevPosY = ac.posY + 1.0;
        this.prevPosZ = ac.posZ;
        this.parent = ac;
        this.setSize(w, h);
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return par1Entity.getEntityBoundingBox();
    }

    public AxisAlignedBB getCollisionBoundingBox() {
        return this.getEntityBoundingBox();
    }

    public double getMountedYOffset() {
        return -0.3;
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return this.parent != null && this.parent.attackEntityFrom(par1DamageSource, par2);
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    public void setDead() {
        super.setDead();
    }

    public void onUpdate() {
        super.onUpdate();
    }

    protected void writeEntityToNBT(@NotNull NBTTagCompound par1NBTTagCompound) {}

    protected void readEntityFromNBT(@NotNull NBTTagCompound par1NBTTagCompound) {}

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    public boolean processInitialInteract(@NotNull EntityPlayer player, @NotNull EnumHand hand) {
        return this.parent != null && this.parent.processInitialInteract(player, hand);
    }
}
