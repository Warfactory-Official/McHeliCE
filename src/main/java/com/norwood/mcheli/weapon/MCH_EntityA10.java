package com.norwood.mcheli.weapon;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Optional;

public class MCH_EntityA10 extends W_Entity {

    private static final DataParameter<String> WEAPON_NAME = EntityDataManager.createKey(
            MCH_EntityA10.class, DataSerializers.STRING);

    public static final int DESPAWN_COUNT_MAX = 70;
    private static final int RENDER_START_TICK = 20;

    private int despawnCount = 0;
    private int shotCount = 0;

    public Entity shootingAircraft;
    public Entity shootingEntity;
    public int direction = 0; // 0: South, 1: West, 2: North, 3: East
    public int power = 32;
    public float acceleration = 4.0F;
    public int explosionPower = 1;
    public MCH_WeaponInfo weaponInfo;

    public MCH_EntityA10(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        this.preventEntitySpawning = false;
        this.setSize(5.0F, 3.0F);
        this.isImmuneToFire = true;
        this._renderDistanceWeight *= 10.0;
    }

    public MCH_EntityA10(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y, z);
        this.prevPosX = this.lastTickPosX = x;
        this.prevPosY = this.lastTickPosY = y;
        this.prevPosZ = this.lastTickPosZ = z;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(WEAPON_NAME, "");
    }

    // Modernized NBT Handling
    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setString("WeaponName", getWeaponName());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.despawnCount = 200;
        Optional.of(compound.getString("WeaponName"))
                .filter(s -> !s.isEmpty())
                .ifPresent(this::setWeaponName);
    }

    public void setWeaponName(String s) {
        if (s == null || s.isEmpty()) return;

        this.weaponInfo = MCH_WeaponInfoManager.get(s);
        if (this.weaponInfo != null && !this.world.isRemote) {
            this.dataManager.set(WEAPON_NAME, s);
        }
    }

    public String getWeaponName() {
        return this.dataManager.get(WEAPON_NAME);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.isDead) this.despawnCount++;

        // Ensure weapon info is synced
        if (this.weaponInfo == null) {
            setWeaponName(getWeaponName());
            if (this.weaponInfo == null) {
                this.setDead();
                return;
            }
        }

        if (this.world.isRemote) {
            this.shotCount += 4;
        } else {
            updateServer();
        }

        updateMotion();
    }

    private void updateServer() {
        if (this.isDead) return;

        if (this.despawnCount > DESPAWN_COUNT_MAX) {
            this.setDead();
        } else if (this.despawnCount > 0 && this.shotCount < 40) {
            // Spawn two bullets per tick
            repeat(2, () -> shotGAU8(this.shotCount++));

            if (this.shotCount == 38) {
                String sound = MCH_MOD.DOMAIN + ":gau-8_snd";
                MCH_SoundEvents.playSound(world, posX, posY, posZ, sound, 150.0F, 1.0F);
            }
        }
    }

    private void updateMotion() {
        if (this.isDead) return;

        if (this.despawnCount <= RENDER_START_TICK) {
            this.motionY = -0.3;
        } else {
            this.setPosition(posX + motionX, posY + motionY, posZ + motionZ);
            this.motionY += 0.02;
        }
    }

    /**
     * Modernized GAU-8 firing logic using Switch Expressions
     */
    protected void shotGAU8(int currentShot) {
        float yaw = 90.0F * this.direction;
        float pitch = 30.0F;

        // Use switch expression to calculate offsets based on direction
        double offset = currentShot * 0.6;

        double spawnX = this.posX;
        double spawnZ = this.posZ;

        // Target vectors
        double tX = rand.nextDouble() - 0.5;
        double tY = -2.6;
        double tZ = rand.nextDouble() - 0.5;

        switch (this.direction) {
            case 0 -> { tZ += 10.0; spawnZ += offset; } // South
            case 1 -> { tX -= 10.0; spawnX -= offset; } // West
            case 2 -> { tZ -= 10.0; spawnZ -= offset; } // North
            case 3 -> { tX += 10.0; spawnX += offset; } // East
        }

        double dist = MathHelper.sqrt(tX * tX + tY * tY + tZ * tZ);
        double velX = (tX * 4.0) / dist;
        double velY = (tY * 4.0) / dist;
        double velZ = (tZ * 4.0) / dist;

        MCH_EntityBullet bullet = new MCH_EntityBullet(world, spawnX, posY, spawnZ, velX, velY, velZ, yaw, pitch, acceleration);

        bullet.setName(getWeaponName());
        bullet.explosionPower = (currentShot % 4 == 0) ? this.explosionPower : 0;
        bullet.setPower(this.power);
        bullet.shootingEntity = this.shootingEntity;
        bullet.shootingAircraft = this.shootingAircraft;

        this.world.spawnEntity(bullet);
    }

    private void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) action.run();
    }

    public boolean isRender() {
        return this.despawnCount > RENDER_START_TICK;
    }

    @Override
    protected boolean canTriggerWalking() { return false; }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) { return false; }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() { return 10.0F; }
}
