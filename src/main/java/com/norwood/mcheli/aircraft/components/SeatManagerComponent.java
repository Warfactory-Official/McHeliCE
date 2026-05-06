package com.norwood.mcheli.aircraft.components;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ViewEntityDummy;
import com.norwood.mcheli.aircraft.*;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.mob.MCH_EntityGunner;
import com.norwood.mcheli.networking.packet.PacketRequestSeatList;
import com.norwood.mcheli.parachute.MCH_EntityParachute;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.wrapper.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

import static com.norwood.mcheli.wrapper.W_Entity.isEqual;

public class SeatManagerComponent implements IAircraftComponent {
    public final List<UnmountReserve> listUnmountReserve = new ArrayList<>();
    private final MCH_EntityAircraft parent;
    @Getter
    private final MCH_EntityHitBox pilotSeat;
    public boolean canRideRackStatus;
    public int waitMountEntity = 0;
    @Getter
    public float lastRiderYaw, prevLastRiderYaw;
    @Getter
    public float lastRiderPitch, prevLastRiderPitch;
    protected Entity lastRiddenByEntity;
    protected Entity lastRidingEntity;
    @Setter
    @Getter
    private MCH_EntitySeat[] seats = new MCH_EntitySeat[0];
    @Getter
    private MCH_SeatInfo[] seatsInfo;
    @Getter
    @Setter
    private boolean switchSeat = false;
    @Getter
    private int seatSearchCount;
    @Setter@Getter
    private int tickRepelling;
    @Getter@Setter
    private int lastUsedRopeIndex;
    @Getter@Setter
    private float ropesLength = 0.0F;
    @Setter@Getter
    private boolean dismountedUserCtrl;
    public void onMountWithNearEmptyMinecart() {
        if (parent.getRidingEntity() == null) {
            int d = 2;
            if (this.dismountedUserCtrl) {
                d = 6;
            }

            List<Entity> list = parent.world.getEntitiesWithinAABBExcludingEntity(parent, parent.getEntityBoundingBox().grow(d, d, d));
            if (!list.isEmpty()) {
                for (Entity entity : list) {
                    if (entity instanceof EntityMinecartEmpty) {
                        if (this.dismountedUserCtrl) {
                            return;
                        }

                        if (!entity.isBeingRidden() && entity.canBePushed()) {
                            waitMountEntity = 20;
                            MCH_Logger.debugLog(parent.world.isRemote, "MCH_EntityAircraft.mountWithNearEmptyMinecart:" + entity);
                            parent.startRiding(entity);
                            return;
                        }
                    }
                }
            }

            this.dismountedUserCtrl = false;
        }
    }


    public SeatManagerComponent(MCH_EntityAircraft parent) {
        this.parent = parent;
        this.pilotSeat = new MCH_EntityHitBox(parent.world, parent, 1.0F, 1.0F);
        this.pilotSeat.parent = parent;
        this.seatsInfo = null;
        this.seats = new MCH_EntitySeat[0];
        this.canRideRackStatus = false;
        this.seatSearchCount = 0;
    }
    public void onUpdate_Repelling() {
        if (this.getAcInfo() != null && this.getAcInfo().haveRepellingHook()) {
            if (parent.isRepelling()) {
                int alt = this.getAlt(parent.posX, parent.posY, parent.posZ);
                if (ropesLength > -50.0F && this.ropesLength > -alt) {
                    ropesLength = (float) (this.ropesLength - (parent.world.isRemote ? 0.3F : 0.25));
                }
            } else {
                this.ropesLength = 0.0F;
            }
        }

        this.onUpdate_UnmountCrewRepelling();
    }

    public void unmountAircraft() {
        Vec3d v = new Vec3d(parent.posX, parent.posY, parent.posZ);
        if (parent.getRidingEntity() instanceof MCH_EntitySeat) {
            MCH_EntityAircraft ac = ((MCH_EntitySeat) parent.getRidingEntity()).getParent();
            assert ac != null;
            MCH_SeatInfo seatInfo = ac.getSeatInfo(parent);
            if (seatInfo instanceof MCH_SeatRackInfo) {
                if (seatInfo.unmountPos != null) {
                    v = seatInfo.unmountPos;
                } else {
                    v = ((MCH_SeatRackInfo) seatInfo).getEntryPos();
                }
                v = ac.getTransformedPosition(v);
            }
        } else if (parent.getRidingEntity() instanceof EntityMinecartEmpty) {
            dismountedUserCtrl = true;
        }

        parent.setLocationAndAngles(v.x, v.y, v.z, parent.getYaw(), parent.getPitch());
        parent.dismountRidingEntity();
        parent.setLocationAndAngles(v.x, v.y, v.z, parent.getYaw(), parent.getPitch());
    }



    public int getAlt(double posX, double posY, double posZ) {
        int i = 0;
        while (i < 256 && !(posY - i <= 0.0) && (!(posY - i < 256.0) || 0 == W_WorldFunc.getBlockId(parent.world, (int) posX, (int) posY - i, (int) posZ))) {
            i++;
        }
        return i;
    }


    @Override
    public MCH_EntityAircraft getParent() {
        return parent;
    }

    public void killSeats() {
        for (MCH_EntitySeat s : this.seats) {
            if (s != null) {
                s.setDead();
            }
        }
    }

    public boolean isRidePlayer() {
        if (this.getRiddenByEntity() instanceof EntityPlayer) {
            return true;
        } else {
            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() instanceof EntityPlayer) {
                    return true;
                }
            }

            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public void setupAllRiderRenderPosition(float tick, EntityPlayer player) {
        double x = parent.lastTickPosX + (parent.posX - parent.lastTickPosX) * tick;
        double y = parent.lastTickPosY + (parent.posY - parent.lastTickPosY) * tick;
        double z = parent.lastTickPosZ + (parent.posZ - parent.lastTickPosZ) * tick;
        updateRiderPosition(this.getRiddenByEntity(), x, y, z);
        this.updateSeatsPosition(x, y, z, true);

        for (int i = 0; i < getSeatNum() + 1; i++) {
            Entity e = getEntityBySeatId(i);
            if (e != null) {
                e.lastTickPosX = e.posX;
                e.lastTickPosY = e.posY;
                e.lastTickPosZ = e.posZ;
            }
        }

        if (parent.getTVMissile() != null && W_Lib.isClientPlayer(parent.getTVMissile().shootingEntity)) {
            Entity tv = parent.getTVMissile();
            assert tv != null;
            x = tv.prevPosX + (tv.posX - tv.prevPosX) * tick;
            y = tv.prevPosY + (tv.posY - tv.prevPosY) * tick;
            z = tv.prevPosZ + (tv.posZ - tv.prevPosZ) * tick;
            MCH_ViewEntityDummy.setCameraPosition(x, y, z);
        } else {
            MCH_AircraftInfo.CameraPosition cpi = parent.getCameraPosInfo();
            if (cpi != null && cpi.pos() != null) {
                MCH_SeatInfo seatInfo = this.getSeatInfo(player);
                Vec3d v;
                if (seatInfo != null && seatInfo.rotSeat) {
                    v = parent.calcOnTurretPos(cpi.pos());
                } else {
                    v = MCH_Lib.RotVec3(cpi.pos(), -parent.getYaw(), -parent.getPitch(), -parent.getRoll());
                }

                MCH_ViewEntityDummy.setCameraPosition(x + v.x, y + v.y, z + v.z);

            }
        }
    }

    public void updateSeatsPosition(double px, double py, double pz, boolean setPrevPos) {
        MCH_SeatInfo[] info = this.getSeatsInfo();
        py += W_Entity.GLOBAL_SEAT_OFFSET;
        if (this.pilotSeat != null && !this.pilotSeat.isDead) {
            this.pilotSeat.prevPosX = this.pilotSeat.posX;
            this.pilotSeat.prevPosY = this.pilotSeat.posY;
            this.pilotSeat.prevPosZ = this.pilotSeat.posZ;
            this.pilotSeat.setPosition(px, py, pz);
            if (info != null && info.length > 0 && info[0] != null) {
                Vec3d v = parent.getTransformedPosition(info[0].pos.x, info[0].pos.y, info[0].pos.z, px, py, pz, info[0].rotSeat);
                this.pilotSeat.setPosition(v.x, v.y, v.z);
            }

            this.pilotSeat.rotationPitch = parent.getPitch();
            this.pilotSeat.rotationYaw = parent.getYaw();
            if (setPrevPos) {
                this.pilotSeat.prevPosX = this.pilotSeat.posX;
                this.pilotSeat.prevPosY = this.pilotSeat.posY;
                this.pilotSeat.prevPosZ = this.pilotSeat.posZ;
            }
        }

        int i = 0;

        for (MCH_EntitySeat seat : this.seats) {
            i++;
            if (seat != null && !seat.isDead) {
                float offsetY = -0.5F;


                seat.prevPosX = seat.posX;
                seat.prevPosY = seat.posY;
                seat.prevPosZ = seat.posZ;
                MCH_SeatInfo si = i < Objects.requireNonNull(info).length ? info[i] : info[0];
                Vec3d v = parent.getTransformedPosition(si.pos.x, si.pos.y + offsetY, si.pos.z, px, py, pz, si.rotSeat);
                seat.setPosition(v.x, v.y, v.z);
                seat.rotationPitch = parent.getPitch();
                seat.rotationYaw = parent.getYaw();
                if (setPrevPos) {
                    seat.prevPosX = seat.posX;
                    seat.prevPosY = seat.posY;
                    seat.prevPosZ = seat.posZ;
                }

                if (si instanceof MCH_SeatRackInfo) {
                    seat.updateRotation(seat.getRiddenByEntity(), si.fixYaw + parent.getYaw(), si.fixPitch);
                }

                seat.updatePosition(seat.getRiddenByEntity());
            }
        }
    }





    public boolean canUnmount(Entity entity) {
        if (this.getAcInfo() == null) {
            return false;
        } else if (!this.getAcInfo().isEnableParachuting) {
            return false;
        } else {
            return this.getSeatIdByEntity(entity) > 1 && (!parent.haveHatch() || !(parent.getHatchRotation() < 89.0F));
        }
    }



    //TODO: Per seat dismount, raycast dismount tests
    public void unmount(Entity entity) {
        final var info = getAcInfo();
        if (info == null) return;

        final boolean repelling = parent.canRepell() && info.haveRepellingHook();
        final boolean normalUnmount = !repelling && canUnmount(entity);

        if (!repelling && !normalUnmount) return;

        final MCH_EntitySeat seat = getSeatByEntity(entity);
        if (seat == null) {
            MCH_Logger.log(parent, "Error:MCH_EntityAircraft.unmount seat=null : " + entity);
            return;
        }


        // Determine position to drop the entity
        final Vec3d hookPos;
        final Vec3d dropPos;

        if (repelling) {
            // Select next rope hook
            lastUsedRopeIndex = (lastUsedRopeIndex + 1) % info.repellingHooks.size();
            hookPos = info.repellingHooks.get(lastUsedRopeIndex).pos();

            dropPos = parent.getTransformedPosition(hookPos, parent.getPrevPositionHistory().oldest()).add(0.0, -2.0, 0.0);

        } else {
            MCH_SeatInfo seatInfo = getSeatInfo(seat.seatID + 1);
            if (seatInfo != null && seatInfo.unmountPos != null) {
                hookPos = seatInfo.unmountPos;
            } else {
                hookPos = info.mobDropOption.pos;
            }
            dropPos = parent.getTransformedPosition(hookPos, parent.getPrevPositionHistory().oldest());
        }

        // Update seat position
        seat.posX = dropPos.x;
        seat.posY = dropPos.y;
        seat.posZ = dropPos.z;

        // Dismount and place entity
        entity.dismountRidingEntity();
        entity.posX = dropPos.x;
        entity.posY = dropPos.y;
        entity.posZ = dropPos.z;

        if (repelling) {
            unmountEntityRepelling(entity, dropPos, lastUsedRopeIndex);
        } else {
            dropEntityParachute(entity);
        }
    }
    public void setSeat(int idx, MCH_EntitySeat seat) {
        if (idx < this.seats.length) {
            String format = "MCH_EntityAircraft.setSeat SeatID=" + idx + " / seat[]" + (this.seats[idx] != null) + " / " + (seat.getRiddenByEntity() != null);
            MCH_Logger.debugLog(parent.world, format);
            if (this.seats[idx] != null) {
                this.seats[idx].getRiddenByEntity();
            }

            this.seats[idx] = seat;
        }
    }

    public void dropEntityParachute(Entity entity) {
        entity.motionX = parent.motionX;
        entity.motionY = parent.motionY;
        entity.motionZ = parent.motionZ;
        MCH_EntityParachute parachute = new MCH_EntityParachute(parent.world, entity.posX, entity.posY, entity.posZ);
        parachute.rotationYaw = entity.rotationYaw;
        parachute.motionX = entity.motionX;
        parachute.motionY = entity.motionY;
        parachute.motionZ = entity.motionZ;
        parachute.fallDistance = entity.fallDistance;
        parachute.user = entity;
        parachute.setType(3);
        parent.world.spawnEntity(parachute);
    }


    public void unmountEntityRepelling(Entity entity, Vec3d dropPos, int ropeIdx) {
        entity.posX = dropPos.x;
        entity.posY = dropPos.y - 2.0;
        entity.posZ = dropPos.z;
        MCH_EntityHide hideEntity = new MCH_EntityHide(parent.world, entity.posX, entity.posY, entity.posZ);
        hideEntity.setParent(parent, entity, ropeIdx);
        hideEntity.motionX = entity.motionX = 0.0;
        hideEntity.motionY = entity.motionY = 0.0;
        hideEntity.motionZ = entity.motionZ = 0.0;
        hideEntity.fallDistance = entity.fallDistance = 0.0F;
        parent.world.spawnEntity(hideEntity);
    }


    private void onUpdate_UnmountCrewRepelling() {
        if (this.getAcInfo() != null) {
            if (!parent.isRepelling()) {
                this.tickRepelling = 0;
            } else if (this.tickRepelling < 60) {
                this.tickRepelling++;
            } else if (!parent.world.isRemote) {
                for (int ropeIdx = 0; ropeIdx < this.getAcInfo().repellingHooks.size(); ropeIdx++) {
                    MCH_AircraftInfo.RepellingHook hook = this.getAcInfo().repellingHooks.get(ropeIdx);
                    if (parent.getCountOnUpdate() % hook.interval() == 0) {
                        for (int i = 1; i < this.getSeatNum(); i++) {
                            MCH_EntitySeat seat = this.getSeat(i);
                            if (seat != null && seat.getRiddenByEntity() != null && !W_EntityPlayer.isPlayer(seat.getRiddenByEntity()) && !(seat.getRiddenByEntity() instanceof MCH_EntityGunner) && !(this.getSeatInfo(i + 1) instanceof MCH_SeatRackInfo)) {
                                Entity entity = seat.getRiddenByEntity();
                                Vec3d dropPos = parent.getTransformedPosition(hook.pos(), parent.getPrevPositionHistory().oldest());
                                seat.posX = dropPos.x;
                                seat.posY = dropPos.y - 2.0;
                                seat.posZ = dropPos.z;
                                entity.dismountRidingEntity();
                                this.unmountEntityRepelling(entity, dropPos, ropeIdx);
                                this.lastUsedRopeIndex = ropeIdx;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    public Entity getEntityBySeatId(int id) {
        if (id == 0) {
            return parent.getRiddenByEntity();
        } else {
            id--;
            if (id >= 0 && id < this.getSeats().length) {
                return this.seats[id] != null ? this.seats[id].getRiddenByEntity() : null;
            } else {
                return null;
            }
        }
    }

    public void newSeats(int seatsNum) {
        if (seatsNum >= 2) {
            if (this.seats != null) {
                for (int i = 0; i < this.seats.length; i++) {
                    if (this.seats[i] != null) {
                        this.seats[i].setDead();
                        this.seats[i] = null;
                    }
                }
            }

            this.seats = new MCH_EntitySeat[seatsNum - 1];
        }
    }

    public void switchPrevSeat(Entity entity) {
        if (entity != null) {
            if (this.seats != null && this.seats.length > 0) {
                if (parent.isMountedEntity(entity)) {
                    boolean isFound = false;

                    for (int i = this.seats.length - 1; i >= 0; i--) {
                        MCH_EntitySeat seat = this.seats[i];
                        if (seat != null) {
                            if (isEqual(seat.getRiddenByEntity(), entity)) {
                                isFound = true;
                            } else if (isFound && seat.getRiddenByEntity() == null) {
                                entity.startRiding(seat);
                                return;
                            }
                        }
                    }

                    for (int ix = this.seats.length - 1; ix >= 0; ix--) {
                        MCH_EntitySeat seat = this.seats[ix];
                        if (!(parent.getSeatInfo(ix + 1) instanceof MCH_SeatRackInfo) && seat != null && seat.getRiddenByEntity() == null) {
                            entity.startRiding(seat);
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean interactFirstSeat(EntityPlayer player) {
        if (this.getSeats() == null) {
            return false;
        } else {
            int seatId = 1;

            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() == null && !parent.isMountedEntity(player) && this.canRideSeatOrRack(seatId, player)) {
                    if (!parent.world.isRemote) {
                        player.startRiding(seat);
                    }
                    break;
                }

                seatId++;
            }

            return true;
        }
    }

    @Nullable
    public MCH_SeatInfo getSeatInfo(int index) {
        MCH_SeatInfo[] seats = this.getSeatsInfo();
        return index >= 0 && seats != null && index < seats.length ? seats[index] : null;
    }

    @Nullable
    public MCH_SeatInfo getSeatInfo(@Nullable Entity entity) {
        return this.getSeatInfo(this.getSeatIdByEntity(entity));
    }

    public int getSeatIdByEntity(@Nullable Entity entity) {
        if (entity == null) {
            return -1;
        } else if (isEqual(this.getRiddenByEntity(), entity)) {
            return 0;
        } else {
            for (int i = 0; i < this.getSeats().length; i++) {
                MCH_EntitySeat seat = this.getSeats()[i];
                if (seat != null && isEqual(seat.getRiddenByEntity(), entity)) {
                    return i + 1;
                }
            }

            return -1;
        }
    }

    private Entity getRiddenByEntity() {
        return parent.getRiddenByEntity();
    }

    public void createSeats(String uuid) {
        if (!parent.world.isRemote) {
            if (!uuid.isEmpty()) {
                parent.setCommonUniqueId(uuid);
                this.seats = new MCH_EntitySeat[this.getSeatNum()];

                for (int i = 0; i < this.seats.length; i++) {
                    this.seats[i] = new MCH_EntitySeat(parent.world, parent.posX, parent.posY, parent.posZ);
                    this.seats[i].parentUniqueID = parent.getCommonUniqueId();
                    this.seats[i].seatID = i;
                    this.seats[i].setParent(parent);
                    parent.world.spawnEntity(this.seats[i]);
                }
            }
        }
    }

    protected void newSeatsPos() {
        if (this.getAcInfo() != null) {
            MCH_SeatInfo[] v = new MCH_SeatInfo[this.getAcInfo().getNumSeatAndRack()];

            for (int i = 0; i < v.length; i++) {
                v[i] = parent.getAcInfo().seatList.get(i);
            }

            this.setSeatsInfo(v);
        }
    }

    public @NotNull MCH_SeatInfo[] getSeatsInfo() {
        if (this.seatsInfo == null) {
            this.newSeatsPos();
        }
        return this.seatsInfo;
    }

    protected void setSeatsInfo(MCH_SeatInfo[] v) {
        this.seatsInfo = v;
    }

    @Nullable
    public MCH_EntitySeat getSeatByEntity(@Nullable Entity entity) {
        int idx = this.getSeatIdByEntity(entity);
        return idx > 0 ? this.getSeat(idx - 1) : null;
    }

    public void updateRiderPosition(Entity passenger, double px, double py, double pz) {
        MCH_SeatInfo[] info = this.getSeatsInfo();
        if (parent.isPassenger(passenger) && !passenger.isDead) {
            float riddenEntityYOffset = 0.0F;

            Vec3d v;
            if (info != null && info.length > 0) {
                v = parent.getTransformedPosition(info[0].pos.x, info[0].pos.y + riddenEntityYOffset - 0.5, info[0].pos.z, px, py + W_Entity.GLOBAL_Y_OFFSET, pz, info[0].rotSeat);
            } else {
                v = parent.getTransformedPosition(0.0, riddenEntityYOffset - 1.0F, 0.0);
            }

            passenger.setPosition(v.x, v.y, v.z);
        }
    }

    public int getSeatNum() {
        if (this.getAcInfo() == null) {
            return 0;
        } else {
            int s = this.getAcInfo().getNumSeatAndRack();
            return s >= 1 ? s - 1 : 1;
        }
    }

    public void onMountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
        if (seat != null) {
            if (parent.world.isRemote && MCH_Lib.getClientPlayer() == entity) {
                parent.switchGunnerFreeLookMode(false);
            }

            parent.initCurrentWeapon(entity);
            Object[] data1 = new Object[]{W_Entity.getEntityId(entity)};
            MCH_Logger.debugLog(parent.world, "onMountEntitySeat:%d", data1);
            Entity pilot = this.getRiddenByEntity();
            int sid = this.getSeatIdByEntity(entity);
            if (sid == 1 && (this.getAcInfo() == null || !this.getAcInfo().isEnableConcurrentGunnerMode)) {
                parent.switchGunnerMode(false);
            }

            if (sid > 0) {
                parent.weaponSystem.setGunnerModeOtherSeat(true);
            }

            if (pilot != null && this.getAcInfo() != null) {
                int cwid = parent.getCurrentWeaponID(pilot);
                MCH_AircraftInfo.Weapon w = this.getAcInfo().getWeaponById(cwid);
                if (w != null && parent.getWeaponSeatID(parent.getWeaponInfoById(cwid), w) == sid) {
                    int next = parent.getNextWeaponID(pilot, 1);
                    Object[] data = new Object[]{W_Entity.getEntityId(pilot), next};
                    MCH_Logger.debugLog(parent.world, "onMountEntitySeat:%d:->%d", data);
                    if (next >= 0) {
                        parent.switchWeapon(pilot, next);
                    }
                }
            }

            if (parent.world.isRemote) {
                parent.updateClientSettings(sid);
            }
        }
    }

    public void searchSeat() {
        List<MCH_EntitySeat> list = parent.world.getEntitiesWithinAABB(MCH_EntitySeat.class, parent.getEntityBoundingBox().grow(60.0, 60.0, 60.0));

        for (MCH_EntitySeat seat : list) {
            if (!seat.isDead && seat.parentUniqueID.equals(parent.getCommonUniqueId()) && seat.seatID >= 0 && seat.seatID < this.getSeatNum() && this.seats[seat.seatID] == null) {
                this.seats[seat.seatID] = seat;
                seat.setParent(parent);
            }
        }
    }

    public boolean checkNestedRiders() {
        for (MCH_EntitySeat seat : getSeats()) {
            if (seat != null && seat.getRiddenByEntity() instanceof MCH_IEntityCanRideAircraft) {
                return false;
            }
        }
        return true;
    }

    public void onUnmountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
        Object[] data = new Object[]{W_Entity.getEntityId(entity)};
        MCH_Logger.debugLog(parent.world, "onUnmountPlayerSeat:%d", data);
        int sid = this.getSeatIdByEntity(entity);
        parent.camera.initCamera(sid, entity);
        MCH_SeatInfo seatInfo = this.getSeatInfo(seat.seatID + 1);
        if (seatInfo != null) {
            this.setUnmountPosition(entity, seatInfo);
        }

        if (!parent.isRidePlayer()) {
            parent.switchGunnerMode(false);
            parent.switchHoveringMode(false);
        }
    }

    @Override
    public void init() {
    }

    public void checkRideRack() {
        if (parent.getCountOnUpdate() % 10 != 0 || parent.getRidingEntity() != null) {
            return;
        }

        this.canRideRackStatus = false;
        AxisAlignedBB searchBox = parent.getCollisionBoundingBox().grow(60.0);
        List<MCH_EntityAircraft> aircraftList = parent.world.getEntitiesWithinAABB(MCH_EntityAircraft.class, searchBox);

        for (MCH_EntityAircraft ac : aircraftList) {
            if (ac.getAcInfo() == null) {
                continue;
            }

            for (int sid = 0; sid < ac.getSeatNum(); sid++) {
                // MCHeli seats are often 1-indexed in info but 0-indexed in entity arrays
                MCH_SeatInfo seatInfo = ac.getSeatInfo(1 + sid);

                if (seatInfo instanceof MCH_SeatRackInfo info) {
                    MCH_EntitySeat seat = ac.getSeat(sid);

                    if (seat != null && !seat.isBeingRidden()) {
                        Vec3d entryPos = ac.getTransformedPosition(info.getEntryPos());
                        float r = info.range;
                        AxisAlignedBB rangeBox = new AxisAlignedBB(entryPos.x - r, entryPos.y - r, entryPos.z - r, entryPos.x + r, entryPos.y + r, entryPos.z + r);

                        if (rangeBox.contains(new Vec3d(parent.posX, parent.posY, parent.posZ)) && parent.canRideAircraft(ac, sid, info)) {
                            this.canRideRackStatus = true;
                            return;
                        }
                    }
                }
            }
        }
    }

    public void rideRack() {
        if (parent.getRidingEntity() == null) {
            AxisAlignedBB bb = parent.getCollisionBoundingBox();
            List<Entity> list = parent.world.getEntitiesWithinAABBExcludingEntity(parent, bb.grow(60.0, 60.0, 60.0));

            for (Entity entity : list) {
                if (entity instanceof MCH_EntityAircraft ac) {
                    if (ac.getAcInfo() != null) {
                        for (int sid = 0; sid < ac.seatManager.getSeatNum(); sid++) {
                            MCH_SeatInfo seatInfo = ac.getSeatInfo(1 + sid);
                            if (seatInfo instanceof MCH_SeatRackInfo info && ac.canRideSeatOrRack(1 + sid, entity)) {
                                MCH_EntitySeat seat = ac.getSeat(sid);
                                if (seat != null && seat.getRiddenByEntity() == null) {
                                    Vec3d v = ac.getTransformedPosition(info.getEntryPos());
                                    float r = info.range;
                                    if (parent.posX >= v.x - r && parent.posX <= v.x + r && parent.posY >= v.y - r && parent.posY <= v.y + r && parent.posZ >= v.z - r && parent.posZ <= v.z + r && parent.canRideAircraft(ac, sid, info)) {
                                        MCH_SoundEvents.playSound(parent.world, parent.posX, parent.posY, parent.posZ, "random.click", 1.0F, 1.0F);
                                        parent.startRiding(seat);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canRideSeatOrRack(int seatId, Entity entity) {
        if (this.getAcInfo() == null) {
            return false;
        } else {
            for (Integer[] a : this.getAcInfo().exclusionSeatList) {
                if (Arrays.asList(a).contains(seatId)) {

                    for (int id : a) {
                        if (this.getEntityBySeatId(id) != null) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    public boolean canDownFromRack() {
        for (int i = 0; i < this.getSeatNum(); i++) {
            MCH_EntitySeat seat = this.getSeat(i);
            MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
            if (seat != null && seat.getRiddenByEntity() != null && seatInfo instanceof MCH_SeatRackInfo) {
                return true;
            }
        }

        return false;
    }

    public void switchNextSeat(Entity entity) {
        if (entity != null) {
            if (this.seats != null && this.seats.length > 0) {
                if (this.isMountedEntity(entity)) {
                    boolean isFound = false;
                    int sid = 1;

                    for (MCH_EntitySeat seat : this.seats) {
                        if (seat != null) {
                            if (this.getSeatInfo(sid) instanceof MCH_SeatRackInfo) {
                                break;
                            }

                            if (isEqual(seat.getRiddenByEntity(), entity)) {
                                isFound = true;
                            } else if (isFound && seat.getRiddenByEntity() == null) {
                                entity.startRiding(seat);
                                return;
                            }

                            sid++;
                        }
                    }

                    sid = 1;

                    for (MCH_EntitySeat seatx : this.seats) {
                        if (seatx != null && seatx.getRiddenByEntity() == null) {
                            if (!(this.getSeatInfo(sid) instanceof MCH_SeatRackInfo)) {
                                this.onMountPlayerSeat(seatx, entity);
                                return;
                            }
                            break;
                        }

                        sid++;
                    }
                }
            }
        }
    }

    // returns local position in seat(rack) configs
    protected Vec3d getUnmountPos(MCH_SeatInfo seatInfo) {
        if (this.getAcInfo() == null || seatInfo == null) return Vec3d.ZERO;

        if (seatInfo.unmountPos != null) {
            return seatInfo.unmountPos;
        }

        if (this.getAcInfo().unmountPosition != null) {
            return this.getAcInfo().unmountPosition;
        }

        double x = seatInfo.pos.x;
        x = x >= 0.0 ? x + 3.0 : x - 3.0;
        return new Vec3d(x, 2.0, seatInfo.pos.z);
    }

    public void setUnmountPosition(@Nullable Entity rByEntity, @Nullable MCH_SeatInfo seatInfo) {
        if (rByEntity != null && seatInfo != null) {
            Vec3d localPos = this.getUnmountPos(seatInfo);
            Vec3d desired = parent.getTransformedPosition(localPos);
            RayTraceResult trace = parent.world.rayTraceBlocks(rByEntity.getPositionVector(), desired, false, true, true);
            Vec3d dir = desired.subtract(rByEntity.getPositionVector()).normalize();
            var finPos = trace == null ? desired : trace.hitVec.subtract(dir);


            rByEntity.setPosition(finPos.x, finPos.y, finPos.z);
            this.listUnmountReserve.add(new UnmountReserve(rByEntity, finPos.x, finPos.y, finPos.z));
        }
    }

    public void unmountEntityFromSeat(@Nullable Entity entity) {
        if (entity != null && this.seats != null) {
            for (MCH_EntitySeat seat : this.seats) {
                if (seat != null && seat.getRiddenByEntity() != null && isEqual(seat.getRiddenByEntity(), entity)) {
                    entity.dismountRidingEntity();
                }
            }

        }
    }

    public void ejectSeat(@Nullable Entity entity) {
        int sid = this.getSeatIdByEntity(entity);
        if (sid >= 0 && sid <= 1) {
            if (parent.getGuiInventory().haveParachute()) {
                if (sid == 0) {
                    parent.getGuiInventory().consumeParachute();
                    this.unmountEntity();
                    this.ejectSeatSub(entity, 0);
                    entity = this.getEntityBySeatId(1);
                    if (entity instanceof EntityPlayer) {
                        entity = null;
                    }
                }

                if (parent.getGuiInventory().haveParachute() && entity != null) {
                    parent.getGuiInventory().consumeParachute();
                    this.unmountEntityFromSeat(entity);
                    this.ejectSeatSub(entity, 1);
                }
            }
        }
    }

    public void ejectSeatSub(Entity entity, int sid) {
        Vec3d pos = this.getSeatInfo(sid) != null ? Objects.requireNonNull(this.getSeatInfo(sid)).pos : null;
        if (pos != null) {
            Vec3d v = parent.getTransformedPosition(pos.x, pos.y + 2.0, pos.z);
            entity.setPosition(v.x, v.y, v.z);
        }

        Vec3d v = MCH_Lib.RotVec3(0.0, 2.0, 0.0, -parent.getYaw(), -parent.getPitch(), -parent.getRoll());
        entity.motionX = parent.motionX + v.x + (parent.getRNG().nextFloat() - 0.5) * 0.1;
        entity.motionY = parent.motionY + v.y;
        entity.motionZ = parent.motionZ + v.z + (parent.getRNG().nextFloat() - 0.5) * 0.1;
        MCH_EntityParachute parachute = new MCH_EntityParachute(parent.world, entity.posX, entity.posY, entity.posZ);
        parachute.rotationYaw = entity.rotationYaw;
        parachute.motionX = entity.motionX;
        parachute.motionY = entity.motionY;
        parachute.motionZ = entity.motionZ;
        parachute.fallDistance = entity.fallDistance;
        parachute.user = entity;
        parachute.setType(2);
        parent.world.spawnEntity(parachute);
        assert this.getAcInfo() != null;
        if (this.getAcInfo().haveCanopy() && parent.isCanopyClose()) {
            parent.openCanopy_EjectSeat();
        }

        W_WorldFunc.playSoundAt(entity, "eject_seat", 5.0F, 1.0F);
    }

    public boolean canEjectSeat(@Nullable Entity entity) {
        int sid = this.getSeatIdByEntity(entity);
        return (sid != 0 || !parent.isUAV()) && sid >= 0 && sid < 2 && this.getAcInfo() != null && this.getAcInfo().isEnableEjectionSeat;
    }

    public int getNumEjectionSeat() {
        return 0;
    }

    public boolean unmountCrew(boolean unmountParachute) {
        boolean anyUnmounted = false;
        MCH_SeatInfo[] seatInfos = getSeatsInfo();

        for (int i = 0; i < seats.length; i++) {
            var seat = seats[i];
            if (seat == null) continue;

            var occupant = seat.getRiddenByEntity();
            if (occupant == null) continue;
            if (occupant instanceof EntityPlayer) continue;
            if (seatInfos[i + 1] instanceof MCH_SeatRackInfo) continue;

            MCH_SeatInfo seatInfo = seatInfos[i + 1];
            anyUnmounted = true;

            if (unmountParachute && getSeatIdByEntity(occupant) > 1) {
                Vec3d localUnmountPos = (seatInfo != null && seatInfo.unmountPos != null) ? seatInfo.unmountPos : Objects.requireNonNull(getAcInfo()).mobDropOption.pos; // acInfo assumed non-null

                Vec3d worldDropPos = parent.getTransformedPosition(localUnmountPos, parent.getPrevPositionHistory().oldest());

                seat.posX = worldDropPos.x;
                seat.posY = worldDropPos.y;
                seat.posZ = worldDropPos.z;

                occupant.dismountRidingEntity();
                occupant.posX = worldDropPos.x;
                occupant.posY = worldDropPos.y;
                occupant.posZ = worldDropPos.z;

                parent.dropEntityParachute(occupant);
                break; // stop after first parachute drop
            } else {
                setUnmountPosition(seat, seatInfo);
                occupant.dismountRidingEntity();
                setUnmountPosition(occupant, seatInfo);
            }
        }

        return anyUnmounted;
    }

    public void unmountCrew() {
        if (this.getAcInfo() != null) {
            if (this.getAcInfo().haveRepellingHook()) {
                if (!parent.isRepelling()) {
                    if (MCH_Lib.getBlockIdY(parent, 3, -4) > 0) {
                        this.unmountCrew(false);
                    } else if (this.canStartRepelling()) {
                        parent.startRepelling();
                    }
                } else {
                    this.stopRepelling();
                }
            } else if (parent.isParachuting()) {
                parent.stopUnmountCrew();
            } else if (this.getAcInfo().isEnableParachuting && MCH_Lib.getBlockIdY(parent, 3, -10) == 0) {
                this.startUnmountCrew();
            } else {
                this.unmountCrew(false);
            }
        }
    }

    public void stopUnmountCrew() {
        parent.setParachuting(false);
    }

    public void stopRepelling() {
        MCH_Logger.debugLog(parent.world, "MCH_EntityAircraft.stopRepelling()");
        parent.setRepellingStat(false);
    }

    public boolean canStartRepelling() {
        assert this.getAcInfo() != null;
        if (this.getAcInfo().haveRepellingHook() && parent.isHovering() && MCH_Lib.abs(parent.getPitch()) < 3.0F && MCH_Lib.abs(parent.getRoll()) < 3.0F) {
            Vec3d v = parent.getPrevPositionHistory().oldest().add(-parent.posX, parent.posY, -parent.posZ);
            return v.length() < 0.3;
        }

        return false;
    }

    public void mountMobToSeats() {
        List<EntityLivingBase> list = parent.world.getEntitiesWithinAABB(W_Lib.getEntityLivingBaseClass(), parent.getEntityBoundingBox().grow(3.0, 2.0, 3.0));

        for (Entity entity : list) {
            if (!(entity instanceof EntityPlayer) && entity.getRidingEntity() == null) {
                int sid = 1;

                for (MCH_EntitySeat seat : this.getSeats()) {
                    if (seat != null && seat.getRiddenByEntity() == null && !this.isMountedEntity(entity) && this.canRideSeatOrRack(sid, entity)) {
                        if (this.getSeatInfo(sid) instanceof MCH_SeatRackInfo) {
                            break;
                        }

                        entity.startRiding(seat);
                    }

                    sid++;
                }
            }
        }
    }

    public void mountEntityToRack() {
        if (!MCH_Config.EnablePutRackInFlying.prmBool) {
            if (parent.getCurrentThrottle() > 0.3) {
                return;
            }

            Block block = MCH_Lib.getBlockY(parent, 1, -3, true);
            if (block == null || W_Block.isEqual(block, Blocks.AIR)) {
                return;
            }
        }

        int countRideEntity = 0;

        for (int sid = 0; sid < this.getSeatNum(); sid++) {
            MCH_EntitySeat seat = this.getSeat(sid);
            if (this.getSeatInfo(1 + sid) instanceof MCH_SeatRackInfo info && seat != null && seat.getRiddenByEntity() == null) {
                Vec3d v = MCH_Lib.RotVec3(info.getEntryPos().x, info.getEntryPos().y, info.getEntryPos().z, -parent.getYaw(), -parent.getPitch(), -parent.getRoll());
                v = v.add(parent.posX, parent.posY, parent.posZ);
                AxisAlignedBB bb = new AxisAlignedBB(v.x, v.y, v.z, v.x, v.y, v.z);
                float range = info.range;
                List<Entity> list = parent.world.getEntitiesWithinAABBExcludingEntity(parent, bb.grow(range, range, range));

                for (Entity entity : list) {
                    if (this.canRideSeatOrRack(1 + sid, entity)) {
                        if (entity instanceof MCH_IEntityCanRideAircraft) {
                            if (((MCH_IEntityCanRideAircraft) entity).canRideAircraft(parent, sid, info)) {
                                MCH_Logger.debugLog(parent.world, "MCH_EntityAircraft.mountEntityToRack:%d:%s", sid, entity);
                                entity.startRiding(seat);
                                countRideEntity++;
                                break;
                            }
                        } else if (entity.getRidingEntity() == null) {
                            NBTTagCompound nbt = entity.getEntityData();
                            if (nbt.hasKey("CanMountEntity") && nbt.getBoolean("CanMountEntity")) {
                                MCH_Logger.debugLog(parent.world, "MCH_EntityAircraft.mountEntityToRack:%d:%s:%s", sid, entity, entity.getClass());
                                entity.startRiding(seat);
                                countRideEntity++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (countRideEntity > 0) {
            MCH_SoundEvents.playSound(parent.world, parent.posX, parent.posY, parent.posZ, "random.click", 1.0F, 1.0F);
        }
    }

    public void unmountEntityFromRack() {
        for (int sid = this.getSeatNum() - 1; sid >= 0; sid--) {
            MCH_EntitySeat seat = this.getSeat(sid);
            if (this.getSeatInfo(sid + 1) instanceof MCH_SeatRackInfo info && seat != null && seat.getRiddenByEntity() != null) {
                Entity entity = seat.getRiddenByEntity();
                Vec3d pos = info.unmountPos;
                if (pos == null) {
                    pos = info.getEntryPos();
                    if (entity instanceof MCH_EntityAircraft) {
                        assert this.getAcInfo() != null;
                        if (pos.z >= this.getAcInfo().bbZ) {
                            pos = pos.add(0.0, 0.0, 12.0);
                        } else {
                            pos = pos.add(0.0, 0.0, -12.0);
                        }
                    }
                }

                Vec3d v = MCH_Lib.RotVec3(pos.x, pos.y, pos.z, -parent.getYaw(), -parent.getPitch(), -parent.getRoll());
                seat.posX = entity.posX = parent.posX + v.x;
                seat.posY = entity.posY = parent.posY + v.y;
                seat.posZ = entity.posZ = parent.posZ + v.z;
                UnmountReserve ur = new UnmountReserve(entity, entity.posX, entity.posY, entity.posZ);
                ur.cnt = 8;
                this.listUnmountReserve.add(ur);
                entity.dismountRidingEntity();
                if (MCH_Lib.getBlockIdY(parent, 3, -20) > 0) {
                    MCH_Logger.debugLog(parent.world, "MCH_EntityAircraft.unmountEntityFromRack:%d:%s", sid, entity);
                } else {
                    MCH_Logger.debugLog(parent.world, "MCH_EntityAircraft.unmountEntityFromRack:%d Parachute:%s", sid, entity);
                    parent.dropEntityParachute(entity);
                }
                break;
            }
        }
    }

    public boolean canPutToRack() {
        for (int i = 0; i < this.getSeatNum(); i++) {
            MCH_EntitySeat seat = this.getSeat(i);
            MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
            if (seat != null && seat.getRiddenByEntity() == null && seatInfo instanceof MCH_SeatRackInfo) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public MCH_EntitySeat getSeat(int idx) {
        return idx < this.seats.length ? this.seats[idx] : null;
    }

    public boolean canRideRack() {
        return parent.getRidingEntity() == null && this.canRideRackStatus;
    }

    public boolean isMountedEntity(@Nullable Entity entity) {
        return entity != null && this.isMountedEntity(W_Entity.getEntityId(entity));
    }

    @Nullable
    public EntityPlayer getFirstMountPlayer() {
        if (this.getRiddenByEntity() instanceof EntityPlayer) {
            return (EntityPlayer) this.getRiddenByEntity();
        } else {
            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() instanceof EntityPlayer) {
                    return (EntityPlayer) seat.getRiddenByEntity();
                }
            }

            return null;
        }
    }

    public boolean isMountedSameTeamEntity(@Nullable EntityLivingBase player) {
        if (player != null && player.getTeam() != null) {
            if (this.getRiddenByEntity() instanceof EntityLivingBase && player.isOnSameTeam(this.getRiddenByEntity())) {
                return true;
            } else {
                for (MCH_EntitySeat seat : this.getSeats()) {
                    if (seat != null && seat.getRiddenByEntity() instanceof EntityLivingBase && player.isOnSameTeam(seat.getRiddenByEntity())) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isMountedEntity(int entityId) {
        if (W_Entity.getEntityId(this.getRiddenByEntity()) == entityId) {
            return true;
        } else {
            for (MCH_EntitySeat seat : this.getSeats()) {
                if (seat != null && seat.getRiddenByEntity() != null && W_Entity.getEntityId(seat.getRiddenByEntity()) == entityId) {
                    return true;
                }
            }

            return false;
        }
    }

    public void unmountEntity() {
        if (!parent.isRidePlayer()) {
            parent.switchHoveringMode(false);
        }

        parent.resetMoveControls();
        Entity rByEntity = null;
        if (this.getRiddenByEntity() != null) {
            rByEntity = this.getRiddenByEntity();
            parent.camera.initCamera(0, rByEntity);
            if (!parent.world.isRemote) {
                this.getRiddenByEntity().dismountRidingEntity();
            }
        } else if (this.lastRiddenByEntity != null) {
            rByEntity = this.lastRiddenByEntity;
            if (rByEntity instanceof EntityPlayer) {
                parent.camera.initCamera(0, rByEntity);
            }
        }

        MCH_Logger.debugLog(parent.world, "unmountEntity:" + rByEntity);
        if (!parent.isRidePlayer()) {
            parent.switchGunnerMode(false);
        }

        parent.setCommonStatus(1, false);
        if (!parent.isUAV()) {
            this.setUnmountPosition(rByEntity, this.getSeatInfo(0));
        } else if (rByEntity != null && rByEntity.getRidingEntity() instanceof MCH_EntityUavStation) {
            rByEntity.dismountRidingEntity();
        }

        this.lastRiddenByEntity = null;
        if (parent.cs_dismountAll) {
            this.unmountCrew(false);
        }
    }

    public void startUnmountCrew() {
        parent.setParachuting(true);
        if (parent.haveHatch()) {
            parent.foldHatch(true, true);
        }
    }

    public void onUpdate_Seats() {
        boolean b = false;

        for (MCH_EntitySeat seat : this.seats) {
            if (seat != null) {
                if (!seat.isDead) {
                    seat.fallDistance = 0.0F;
                }
            } else {
                b = true;
            }
        }

        if (b) {
            if (this.seatSearchCount > 40) {
                if (parent.world.isRemote) {
                    PacketRequestSeatList.requestSeatList(parent);
                } else {
                    this.searchSeat();
                }

                this.seatSearchCount = 0;
            }

            this.seatSearchCount++;
        }
    }

    public void iterateUnmount() {
        Iterator<UnmountReserve> itr = this.listUnmountReserve.iterator();
        while (itr.hasNext()) {
            UnmountReserve ur = itr.next();
            if (ur.entity != null && !ur.entity.isDead) {
                ur.entity.setPosition(ur.posX, ur.posY, ur.posZ);
                ur.entity.fallDistance = parent.fallDistance;
            }

            if (ur.cnt > 0) {
                ur.cnt--;
            }

            if (ur.cnt == 0) {
                itr.remove();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (parent.getRiddenByEntity() == null && lastRiddenByEntity != null) {
            parent.unmountEntity();
        }
        lastRiddenByEntity = parent.getRiddenByEntity();
        lastRidingEntity = parent.getRidingEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        prevLastRiderYaw = lastRiderYaw = compound.getFloat("AcLastRYaw");
        prevLastRiderPitch = lastRiderPitch = compound.getFloat("AcLastRPitch");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setFloat("AcLastRYaw", lastRiderYaw);
        compound.setFloat("AcLastRPitch", lastRiderPitch);
    }

    public MCH_EntitySeat[] getSeats() {
        return seats != null ? seats : new MCH_EntitySeat[0];
    }

    public float getLastRiderYaw() {
        return lastRiderYaw;
    }

    public float getLastRiderPitch() {
        return lastRiderPitch;
    }

    public int getTickRepelling() {
        return tickRepelling;
    }

    public void setTickRepelling(int tickRepelling) {
        this.tickRepelling = tickRepelling;
    }

    public boolean isSwitchSeat() {
        return switchSeat;
    }

    public void setSwitchSeat(boolean switchSeat) {
        this.switchSeat = switchSeat;
    }

    public void setDismountedUserCtrl(boolean dismountedUserCtrl) {
        this.dismountedUserCtrl = dismountedUserCtrl;
    }

    public boolean isDismountedUserCtrl() {
        return dismountedUserCtrl;
    }

    public static class UnmountReserve {

        final Entity entity;
        final double posX;
        final double posY;
        final double posZ;
        int cnt = 5;

        public UnmountReserve(Entity e, double x, double y, double z) {
            this.entity = e;
            this.posX = x;
            this.posY = y;
            this.posZ = z;
        }
    }


}
