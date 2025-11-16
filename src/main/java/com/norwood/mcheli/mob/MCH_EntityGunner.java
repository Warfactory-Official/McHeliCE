package com.norwood.mcheli.mob;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_SeatInfo;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponEntitySeeker;
import com.norwood.mcheli.weapon.MCH_WeaponParam;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class MCH_EntityGunner extends EntityLivingBase {

    private static final DataParameter<String> TEAM_NAME = EntityDataManager.createKey(MCH_EntityGunner.class,
            DataSerializers.STRING);
    public boolean isCreative = false;
    public String ownerUUID = "";
    public int targetType = 0;
    public int despawnCount = 0;
    public int switchTargetCount = 0;
    public Entity targetEntity = null;
    public double targetPrevPosX = 0.0;
    public double targetPrevPosY = 0.0;
    public double targetPrevPosZ = 0.0;
    public boolean waitCooldown = false;
    public int idleCount = 0;
    public int idleRotation = 0;

    public MCH_EntityGunner(World world) {
        super(world);
    }

    public MCH_EntityGunner(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y, z);
    }

    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TEAM_NAME, "");
    }

    public String getTeamName() {
        return this.dataManager.get(TEAM_NAME);
    }

    public void setTeamName(String name) {
        this.dataManager.set(TEAM_NAME, name);
    }

    public Team getTeam() {
        return this.world.getScoreboard().getTeam(this.getTeamName());
    }

    public boolean isOnSameTeam(@NotNull Entity entityIn) {
        return super.isOnSameTeam(entityIn);
    }

    public @NotNull ITextComponent getDisplayName() {
        Team team = this.getTeam();
        return team != null ?
                new TextComponentString(ScorePlayerTeam.formatPlayerName(team, team.getName() + " Gunner")) :
                new TextComponentString("");
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return this.isCreative;
    }

    public void onDeath(@NotNull DamageSource source) {
        super.onDeath(source);
    }

    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (this.world.isRemote) {
            return false;
        } else if (this.getRidingEntity() == null) {
            return false;
        } else if (player.capabilities.isCreativeMode) {
            this.removeFromAircraft(player);
            return true;
        } else if (this.isCreative) {
            player.sendMessage(new TextComponentString("Creative mode only."));
            return false;
        } else if (this.getTeam() != null && !this.isOnSameTeam(player)) {
            player.sendMessage(new TextComponentString("You are other team."));
            return false;
        } else {
            this.removeFromAircraft(player);
            return true;
        }
    }

    public void removeFromAircraft(EntityPlayer player) {
        if (!this.world.isRemote) {
            W_WorldFunc.MOD_playSoundAtEntity(player, "wrench", 1.0F, 1.0F);
            this.setDead();
            MCH_EntityAircraft ac = null;
            if (this.getRidingEntity() instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft) this.getRidingEntity();
            } else if (this.getRidingEntity() instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat) this.getRidingEntity()).getParent();
            }

            String name = "";
            if (ac != null && ac.getAcInfo() != null) {
                name = " on " + ac.getAcInfo().displayName + " seat " + (ac.getSeatIdByEntity(this) + 1);
            }

            player.sendMessage(
                    new TextComponentString(
                            "Remove gunner" + name + " by " + ScorePlayerTeam.formatPlayerName(player.getTeam(),
                                    player.getDisplayName().getFormattedText()) + "."));
            this.dismountRidingEntity();
        }
    }

    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && !this.isDead) {
            if (this.getRidingEntity() != null && this.getRidingEntity().isDead) {
                this.dismountRidingEntity();
            }

            if (this.getRidingEntity() instanceof MCH_EntityAircraft) {
                this.shotTarget((MCH_EntityAircraft) this.getRidingEntity());
            } else if (this.getRidingEntity() instanceof MCH_EntitySeat &&
                    ((MCH_EntitySeat) this.getRidingEntity()).getParent() != null) {
                        this.shotTarget(((MCH_EntitySeat) this.getRidingEntity()).getParent());
                    } else
                if (this.despawnCount < 20) {
                    this.despawnCount++;
                } else if (this.getRidingEntity() == null || this.ticksExisted > 100) {
                    this.setDead();
                }

            if (this.targetEntity == null) {
                if (this.idleCount == 0) {
                    this.idleCount = (3 + this.rand.nextInt(5)) * 20;
                    this.idleRotation = this.rand.nextInt(5) - 2;
                }

                this.rotationYaw = this.rotationYaw + this.idleRotation / 2.0F;
            } else {
                this.idleCount = 60;
            }
        }

        if (this.switchTargetCount > 0) {
            this.switchTargetCount--;
        }

        if (this.idleCount > 0) {
            this.idleCount--;
        }
    }

    public boolean canAttackEntity(EntityLivingBase entity, MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        boolean ret;
        if (this.targetType == 0) {
            ret = entity != this && !(entity instanceof EntityEnderman) && !entity.isDead &&
                    !this.isOnSameTeam(entity) && entity.getHealth() > 0.0F && !ac.isMountedEntity(entity);
        } else {
            ret = entity != this && !((EntityPlayer) entity).capabilities.isCreativeMode && !entity.isDead &&
                    !this.getTeamName().isEmpty() && !this.isOnSameTeam(entity) && entity.getHealth() > 0.0F &&
                    !ac.isMountedEntity(entity);
        }

        if (ret && ws.getCurrentWeapon().getGuidanceSystem() != null) {
            ret = ws.getCurrentWeapon().getGuidanceSystem().canLockEntity(entity);
        }

        return ret;
    }

    public void shotTarget(MCH_EntityAircraft ac) {
        if (!ac.isDestroyed()) {
            if (ac.getGunnerStatus()) {
                MCH_WeaponSet ws = ac.getCurrentWeapon(this);
                if (ws != null && ws.getInfo() != null && ws.getCurrentWeapon() != null) {
                    MCH_WeaponBase cw = ws.getCurrentWeapon();
                    if (this.targetEntity != null &&
                            (this.targetEntity.isDead || ((EntityLivingBase) this.targetEntity).getHealth() <= 0.0F) &&
                            this.switchTargetCount > 20) {
                        this.switchTargetCount = 20;
                    }

                    Vec3d pos = this.getGunnerWeaponPos(ac, ws);
                    if (this.switchTargetCount <= 0) {
                        this.switchTargetCount = 20;
                        EntityLivingBase nextTarget = null;
                        List<? extends EntityLivingBase> list;
                        if (this.targetType == 0) {
                            int rh = MCH_Config.RangeOfGunner_VsMonster_Horizontal.prmInt;
                            int rv = MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt;
                            list = this.world.getEntitiesWithinAABB(EntityLivingBase.class,
                                    this.getEntityBoundingBox().grow(rh, rv, rh), IMob.MOB_SELECTOR);
                        } else {
                            int rh = MCH_Config.RangeOfGunner_VsPlayer_Horizontal.prmInt;
                            int rv = MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt;
                            list = this.world.getEntitiesWithinAABB(EntityPlayer.class,
                                    this.getEntityBoundingBox().grow(rh, rv, rh));
                        }

                        for (EntityLivingBase entity : list) {
                            if (this.canAttackEntity(entity, ac, ws) && this.checkPitch(entity, ac, pos) &&
                                    (nextTarget == null || this.getDistance(entity) < this.getDistance(nextTarget)) &&
                                    this.canEntityBeSeen(entity) && this.isInAttackable(entity, ac, ws, pos)) {
                                nextTarget = entity;
                                this.switchTargetCount = 60;
                            }
                        }

                        if (nextTarget != null && this.targetEntity != nextTarget) {
                            this.targetPrevPosX = nextTarget.posX;
                            this.targetPrevPosY = nextTarget.posY;
                            this.targetPrevPosZ = nextTarget.posZ;
                        }

                        this.targetEntity = nextTarget;
                    }

                    if (this.targetEntity != null) {
                        float rotSpeed = 10.0F;
                        if (ac.isPilot(this)) {
                            rotSpeed = ac.getAcInfo().cameraRotationSpeed / 10.0F;
                        }

                        this.rotationPitch = MathHelper.wrapDegrees(this.rotationPitch);
                        this.rotationYaw = MathHelper.wrapDegrees(this.rotationYaw);
                        double dist = this.getDistance(this.targetEntity);
                        double tick = 1.0;
                        if (dist >= 10.0 && ws.getInfo().acceleration > 1.0F) {
                            tick = dist / ws.getInfo().acceleration;
                        }

                        if (this.targetEntity.getRidingEntity() instanceof MCH_EntitySeat ||
                                this.targetEntity.getRidingEntity() instanceof MCH_EntityAircraft) {
                            tick -= MCH_Config.HitBoxDelayTick.prmInt;
                        }

                        double dx = (this.targetEntity.posX - this.targetPrevPosX) * tick;
                        double dy = (this.targetEntity.posY - this.targetPrevPosY) * tick +
                                this.targetEntity.height * this.rand.nextDouble();
                        double dz = (this.targetEntity.posZ - this.targetPrevPosZ) * tick;
                        double d0 = this.targetEntity.posX + dx - pos.x;
                        double d1 = this.targetEntity.posY + dy - pos.y;
                        double d2 = this.targetEntity.posZ + dz - pos.z;
                        double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
                        float yaw = MathHelper.wrapDegrees((float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F);
                        float pitch = (float) (-(Math.atan2(d1, d3) * 180.0 / Math.PI));
                        if (Math.abs(this.rotationPitch - pitch) < rotSpeed &&
                                Math.abs(this.rotationYaw - yaw) < rotSpeed) {
                            float r = ac.isPilot(this) ? 0.1F : 0.5F;
                            this.rotationPitch = pitch + (this.rand.nextFloat() - 0.5F) * r - cw.fixRotationPitch;
                            this.rotationYaw = yaw + (this.rand.nextFloat() - 0.5F) * r;
                            if (!this.waitCooldown || ws.currentHeat <= 0 || ws.getInfo().maxHeatCount <= 0) {
                                this.waitCooldown = false;
                                MCH_WeaponParam prm = new MCH_WeaponParam();
                                prm.setPosition(ac.posX, ac.posY, ac.posZ);
                                prm.user = this;
                                prm.entity = ac;
                                prm.option1 = cw instanceof MCH_WeaponEntitySeeker ? this.targetEntity.getEntityId() :
                                        0;
                                if (ac.useCurrentWeapon(prm) && ws.getInfo().maxHeatCount > 0 &&
                                        ws.currentHeat > ws.getInfo().maxHeatCount * 4 / 5) {
                                    this.waitCooldown = true;
                                }
                            }
                        }

                        if (Math.abs(pitch - this.rotationPitch) >= rotSpeed) {
                            this.rotationPitch = this.rotationPitch +
                                    (pitch > this.rotationPitch ? rotSpeed : -rotSpeed);
                        }

                        if (Math.abs(yaw - this.rotationYaw) >= rotSpeed) {
                            if (Math.abs(yaw - this.rotationYaw) <= 180.0F) {
                                this.rotationYaw = this.rotationYaw + (yaw > this.rotationYaw ? rotSpeed : -rotSpeed);
                            } else {
                                this.rotationYaw = this.rotationYaw + (yaw > this.rotationYaw ? -rotSpeed : rotSpeed);
                            }
                        }

                        this.rotationYawHead = this.rotationYaw;
                        this.targetPrevPosX = this.targetEntity.posX;
                        this.targetPrevPosY = this.targetEntity.posY;
                        this.targetPrevPosZ = this.targetEntity.posZ;
                    } else {
                        this.rotationPitch *= 0.95F;
                    }
                }
            }
        }
    }

    private boolean checkPitch(EntityLivingBase entity, MCH_EntityAircraft ac, Vec3d pos) {
        try {
            double d0 = entity.posX - pos.x;
            double d1 = entity.posY - pos.y;
            double d2 = entity.posZ - pos.z;
            double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
            float pitch = (float) (-(Math.atan2(d1, d3) * 180.0 / Math.PI));
            MCH_AircraftInfo ai = ac.getAcInfo();
            if (ac instanceof MCH_EntityVehicle && ac.isPilot(this) &&
                    Math.abs(ai.minRotationPitch) + Math.abs(ai.maxRotationPitch) > 0.0F) {
                if (pitch < ai.minRotationPitch) {
                    return false;
                }

                if (pitch > ai.maxRotationPitch) {
                    return false;
                }
            }

            MCH_WeaponBase cw = ac.getCurrentWeapon(this).getCurrentWeapon();
            if (!(cw instanceof MCH_WeaponEntitySeeker)) {
                MCH_AircraftInfo.Weapon wi = ai.getWeaponById(ac.getCurrentWeaponID(this));
                if (Math.abs(wi.minPitch) + Math.abs(wi.maxPitch) > 0.0F) {
                    if (pitch < wi.minPitch) {
                        return false;
                    }

                    if (pitch > wi.maxPitch) {
                        return false;
                    }
                }
            }
        } catch (Exception var16) {}

        return true;
    }

    public Vec3d getGunnerWeaponPos(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        MCH_SeatInfo seatInfo = ac.getSeatInfo(this);
        return (seatInfo == null || !seatInfo.rotSeat) && !(ac instanceof MCH_EntityVehicle) ?
                ac.getTransformedPosition(ws.getCurrentWeapon().position) :
                ac.calcOnTurretPos(ws.getCurrentWeapon().position).add(ac.posX, ac.posY, ac.posZ);
    }

    private boolean isInAttackable(EntityLivingBase entity, MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3d pos) {
        if (ac instanceof MCH_EntityVehicle) {
            return true;
        } else {
            try {
                if (ac.getCurrentWeapon(this).getCurrentWeapon() instanceof MCH_WeaponEntitySeeker) {
                    return true;
                } else {
                    MCH_AircraftInfo.Weapon wi = ac.getAcInfo().getWeaponById(ac.getCurrentWeaponID(this));
                    Vec3d v1 = new Vec3d(0.0, 0.0, 1.0);
                    float yaw = -ac.getRotYaw() + (wi.maxYaw + wi.minYaw) / 2.0F - wi.defaultYaw;
                    v1 = v1.rotateYaw(yaw * (float) Math.PI / 180.0F);
                    Vec3d v2 = new Vec3d(entity.posX - pos.x, 0.0, entity.posZ - pos.z).normalize();
                    double dot = v1.dotProduct(v2);
                    double rad = Math.acos(dot);
                    double deg = rad * 180.0 / Math.PI;
                    return deg < Math.abs(wi.maxYaw - wi.minYaw) / 2.0F;
                }
            } catch (Exception var15) {
                return false;
            }
        }
    }

    @Nullable
    public MCH_EntityAircraft getAc() {
        if (this.getRidingEntity() == null) {
            return null;
        } else {
            return this.getRidingEntity() instanceof MCH_EntityAircraft ? (MCH_EntityAircraft) this.getRidingEntity() :
                    (this.getRidingEntity() instanceof MCH_EntitySeat ?
                            ((MCH_EntitySeat) this.getRidingEntity()).getParent() : null);
        }
    }

    public void writeEntityToNBT(@NotNull NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("Creative", this.isCreative);
        nbt.setString("OwnerUUID", this.ownerUUID);
        nbt.setString("TeamName", this.getTeamName());
        nbt.setInteger("TargetType", this.targetType);
    }

    public void readEntityFromNBT(@NotNull NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.isCreative = nbt.getBoolean("Creative");
        this.ownerUUID = nbt.getString("OwnerUUID");
        this.setTeamName(nbt.getString("TeamName"));
        this.targetType = nbt.getInteger("TargetType");
    }

    @Nullable
    public Entity changeDimension(int dimensionIn, @NotNull ITeleporter teleporter) {
        return null;
    }

    public void setDead() {
        if (!this.world.isRemote && !this.isDead && !this.isCreative) {
            if (this.targetType == 0) {
                this.dropItem(MCH_MOD.itemSpawnGunnerVsMonster, 1);
            } else {
                this.dropItem(MCH_MOD.itemSpawnGunnerVsPlayer, 1);
            }
        }

        super.setDead();
        MCH_Lib.DbgLog(this.world, "MCH_EntityGunner.setDead type=%d :" + this, this.targetType);
    }

    public boolean attackEntityFrom(@NotNull DamageSource ds, float amount) {
        if (ds == DamageSource.OUT_OF_WORLD) {
            this.setDead();
        }

        return super.attackEntityFrom(ds, amount);
    }

    public @NotNull ItemStack getItemStackFromSlot(@NotNull EntityEquipmentSlot slotIn) {
        return ItemStack.EMPTY;
    }

    public void setItemStackToSlot(@NotNull EntityEquipmentSlot slotIn, @NotNull ItemStack stack) {}

    public @NotNull Iterable<ItemStack> getArmorInventoryList() {
        return Collections.emptyList();
    }

    public @NotNull EnumHandSide getPrimaryHand() {
        return EnumHandSide.RIGHT;
    }
}
