package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.command.MCH_Command;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.uav.IUavStation;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.W_Entity;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public class NetworkSyncComponent implements IAircraftComponent {
    @Getter
    private final MCH_EntityAircraft parent;

    public static final DataParameter<Integer> PART_STAT = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> DAMAGE = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> UAV_STATION = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> STATUS = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> USE_WEAPON = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> FUEL = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> ROT_ROLL = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<Integer> THROTTLE = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.VARINT);
    public static final DataParameter<String> ID_TYPE = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    public static final DataParameter<String> TEXTURE_NAME = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    public static final DataParameter<String> FUEL_FF = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);
    public static final DataParameter<String> COMMAND = EntityDataManager.createKey(MCH_EntityAircraft.class, DataSerializers.STRING);

    public static final int CMN_ID_GUNNER_STATUS = 12;
    public static final int CMN_ID_RADAR_ENABLED = 13;
    public static final int CMN_ID_MORTAR_RADAR_ENABLED = 14;
    public static final int CMN_ID_ECM_JAMMER_ENABLED = 15;

    private int commonStatus;
    private boolean requestedSyncStatus;
    @Nullable
    private IUavStation uavStation;
    private boolean packetWeaponUserAimActive;
    private float packetWeaponUserYaw;
    private float packetWeaponUserPitch;

    public NetworkSyncComponent(MCH_EntityAircraft parent) {
        this.parent = parent;
    }

    @Override
    public MCH_EntityAircraft getParent() {
        return parent;
    }

    @Override
    public void init() {
        parent.getDataManager().register(ID_TYPE, "");
        parent.getDataManager().register(DAMAGE, 0);
        parent.getDataManager().register(STATUS, 0);
        parent.getDataManager().register(USE_WEAPON, 0);
        parent.getDataManager().register(FUEL, 0);
        parent.getDataManager().register(FUEL_FF, "");
        parent.getDataManager().register(TEXTURE_NAME, "");
        parent.getDataManager().register(UAV_STATION, 0);
        parent.getDataManager().register(ROT_ROLL, 0);
        parent.getDataManager().register(COMMAND, "");
        parent.getDataManager().register(THROTTLE, 0);
        parent.getDataManager().register(PART_STAT, 0);
    }

    @Override
    public void onUpdate() {
        if (parent.world.isRemote) {
            commonStatus = parent.getDataManager().get(STATUS);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        // Handled by EntityDataManager properties
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        // Handled by EntityDataManager properties
    }

    public int getCommonStatus() {
        return commonStatus;
    }

    public boolean getCommonStatus(int bit) {
        return (commonStatus >> bit & 1) != 0;
    }

    public void setCommonStatus(int bit, boolean value) {
        setCommonStatus(bit, value, false);
    }

    public void setCommonStatus(int bit, boolean value, boolean writeClient) {
        if (!parent.world.isRemote || writeClient) {
            int before = commonStatus;
            int mask = 1 << bit;
            if (value) {
                commonStatus |= mask;
            } else {
                commonStatus &= ~mask;
            }

            if (before != commonStatus) {
                Object[] data = new Object[]{parent.getDataManager().get(STATUS), commonStatus};
                MCH_Logger.debugLog(parent.world, "setCommonStatus : %08X -> %08X ", data);
                parent.getDataManager().set(STATUS, commonStatus);
            }
        }
    }

    public boolean isRequestedSyncStatus() {
        return requestedSyncStatus;
    }

    public boolean markSyncStatusRequested() {
        if (requestedSyncStatus) {
            return false;
        }

        requestedSyncStatus = true;
        return true;
    }

    public void setRequestedSyncStatus(boolean requestedSyncStatus) {
        this.requestedSyncStatus = requestedSyncStatus;
    }

    public float getServerRoll() {
        return parent.getDataManager().get(ROT_ROLL).shortValue();
    }

    public void syncServerRoll() {
        if (!parent.world.isRemote && (int) parent.getPrevRotationRoll() != (int) parent.getRoll()) {
            float roll = MathHelper.wrapDegrees(parent.getRoll());
            parent.getDataManager().set(ROT_ROLL, (int) roll);
        }
    }

    public void setCommand(String command, EntityPlayer player) {
        if (!parent.world.isRemote && MCH_Command.canUseCommand(player)) {
            setCommandForce(command);
        }
    }

    public void setCommandForce(String command) {
        if (!parent.world.isRemote) {
            parent.getDataManager().set(COMMAND, command);
        }
    }

    public String getCommand() {
        return parent.getDataManager().get(COMMAND);
    }

    public void setTypeName(String typeName) {
        String beforeType = getTypeName();
        if (typeName != null && !typeName.isEmpty() && typeName.compareTo(beforeType) != 0) {
            parent.getDataManager().set(ID_TYPE, typeName);
            parent.changeType(typeName);
            parent.initRotationYaw(parent.getYaw());
        }
    }

    public String getTypeName() {
        return parent.getDataManager().get(ID_TYPE);
    }

    @Nullable
    public IUavStation getUavStation() {
        return parent.isUAV() ? uavStation : null;
    }

    public void setUavStation(@Nullable MCH_EntityUavStation uavStation) {
        this.uavStation = uavStation;
        if (!parent.world.isRemote) {
            if (uavStation != null) {
                parent.getDataManager().set(UAV_STATION, W_Entity.getEntityId(uavStation));
            } else {
                parent.getDataManager().set(UAV_STATION, 0);
            }
        }
    }

    public void updateUavStation() {
        if (!parent.isUAV()) {
            return;
        }

        if (parent.world.isRemote) {
            updateClientUavStation();
        } else {
            updateServerUavStation();
        }

        if (uavStation instanceof Entity entityUav && entityUav.isDead) {
            uavStation = null;
        }
    }

    private void updateClientUavStation() {
        int eid = parent.getDataManager().get(UAV_STATION);
        if (eid > 0) {
            if (uavStation == null) {
                Entity entity = parent.world.getEntityByID(eid);
                if (entity instanceof IUavStation) {
                    uavStation = (IUavStation) entity;
                    uavStation.setControlled(parent);
                }
            }
        } else if (uavStation != null) {
            uavStation.setControlled(null);
            uavStation = null;
        }
    }

    private void updateServerUavStation() {
        if (uavStation == null) {
            return;
        }

        double rangeSq = uavStation.getType() == IUavStation.StationType.SMALL ? 2500.0 : 15129.0;
        Vec3d stationPos = uavStation.getPos();
        double udx = parent.posX - stationPos.x;
        double udz = parent.posZ - stationPos.z;

        if (udx * udx + udz * udz > rangeSq) {
            uavStation.setControlled(null);
            setUavStation(null);
            parent.attackEntityFrom(DamageSource.OUT_OF_WORLD, parent.getMaxHP() + 10);
        }
    }

    public double getThrottle() {
        return 0.05 * parent.getDataManager().get(THROTTLE);
    }

    public void setThrottle(double throttle) {
        int value = (int) (throttle * 20.0);
        if (value == 0 && throttle > 0.0) {
            value = 1;
        }

        parent.getDataManager().set(THROTTLE, value);
    }

    public int getDamageTaken() {
        return parent.getDataManager().get(DAMAGE);
    }

    public void setDamageTaken(int damageTaken) {
        if (damageTaken < 0) {
            damageTaken = 0;
        }

        if (damageTaken > parent.getMaxHP()) {
            damageTaken = parent.getMaxHP();
        }

        parent.getDataManager().set(DAMAGE, damageTaken);
    }

    public int getUseWeaponStat() {
        return parent.getDataManager().get(USE_WEAPON);
    }

    public void setUseWeaponStat(int useWeaponStat) {
        parent.getDataManager().set(USE_WEAPON, useWeaponStat);
    }

    public String getTextureName() {
        return parent.getDataManager().get(TEXTURE_NAME);
    }

    public void setTextureName(@Nullable String textureName) {
        if (textureName != null && !textureName.isEmpty()) {
            parent.getDataManager().set(TEXTURE_NAME, textureName);
        }
    }

    public int getPartStatus() {
        return parent.getDataManager().get(PART_STAT);
    }

    public void setPartStatus(int partStatus) {
        parent.getDataManager().set(PART_STAT, partStatus);
    }

    public void setPacketWeaponUserAim(float yaw, float pitch) {
        packetWeaponUserAimActive = true;
        packetWeaponUserYaw = yaw;
        packetWeaponUserPitch = pitch;
    }

    public void clearPacketWeaponUserAim() {
        packetWeaponUserAimActive = false;
    }

    public float getWeaponUserYaw(@Nullable Entity entity) {
        if (packetWeaponUserAimActive) {
            return packetWeaponUserYaw;
        }
        if (parent.weaponSystem.isDetachedWeaponAimActive() && parent.supportsDetachedTurretAim()) {
            return parent.weaponSystem.getDetachedWeaponAimYaw();
        }
        if (entity != null) {
            return entity.rotationYaw;
        }
        return parent.getLastRiderYaw();
    }

    public float getWeaponUserYaw(@Nullable Entity entity, float partialTicks) {
        if (packetWeaponUserAimActive) {
            return packetWeaponUserYaw;
        }
        if (parent.weaponSystem.isDetachedWeaponAimActive() && parent.supportsDetachedTurretAim()) {
            float prev = parent.weaponSystem.getPrevDetachedWeaponAimYaw();
            float curr = parent.weaponSystem.getDetachedWeaponAimYaw();
            if (curr - prev > 180.0F) {
                prev += 360.0F;
            } else if (curr - prev < -180.0F) {
                prev -= 360.0F;
            }
            return MathHelper.wrapDegrees(prev + (curr - prev) * partialTicks);
        }
        if (entity != null) {
            float prev = entity.prevRotationYaw;
            float curr = entity.rotationYaw;
            if (curr - prev > 180.0F) {
                prev += 360.0F;
            } else if (curr - prev < -180.0F) {
                prev -= 360.0F;
            }
            return MathHelper.wrapDegrees(prev + (curr - prev) * partialTicks);
        }
        float prev = parent.getPrevLastRiderYaw();
        float curr = parent.getLastRiderYaw();
        if (curr - prev > 180.0F) {
            prev += 360.0F;
        } else if (curr - prev < -180.0F) {
            prev -= 360.0F;
        }
        return MathHelper.wrapDegrees(prev + (curr - prev) * partialTicks);
    }

    public float getWeaponUserPitch(@Nullable Entity entity) {
        if (packetWeaponUserAimActive) {
            return packetWeaponUserPitch;
        }
        if (parent.weaponSystem.isDetachedWeaponAimActive() && parent.supportsDetachedTurretAim()) {
            return parent.weaponSystem.getDetachedWeaponAimPitch();
        }
        if (entity != null) {
            return entity.rotationPitch;
        }
        return parent.getLastRiderPitch();
    }

    public float getWeaponUserPitch(@Nullable Entity entity, float partialTicks) {
        if (packetWeaponUserAimActive) {
            return packetWeaponUserPitch;
        }
        if (parent.weaponSystem.isDetachedWeaponAimActive() && parent.supportsDetachedTurretAim()) {
            return parent.weaponSystem.getPrevDetachedWeaponAimPitch() + (parent.weaponSystem.getDetachedWeaponAimPitch() - parent.weaponSystem.getPrevDetachedWeaponAimPitch()) * partialTicks;
        }
        if (entity != null) {
            return entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        }
        return parent.getPrevLastRiderPitch() + (parent.getLastRiderPitch() - parent.getPrevLastRiderPitch()) * partialTicks;
    }
}
