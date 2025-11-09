package com.norwood.mcheli.chain;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_EntityHitBox;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.parachute.MCH_EntityParachute;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_Lib;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_ItemChain extends W_Item {

    public MCH_ItemChain(int par1) {
        super(par1);
        this.setMaxStackSize(1);
    }

    public static void interactEntity(ItemStack item, @Nullable Entity entity, EntityPlayer player, World world) {
        if (!world.isRemote && entity != null && !entity.isDead) {
            if (entity instanceof EntityItem) {
                return;
            }

            if (entity instanceof MCH_EntityChain) {
                return;
            }

            if (entity instanceof MCH_EntityHitBox) {
                return;
            }

            if (entity instanceof MCH_EntitySeat) {
                return;
            }

            if (entity instanceof MCH_EntityUavStation) {
                return;
            }

            if (entity instanceof MCH_EntityParachute) {
                return;
            }

            if (W_Lib.isEntityLivingBase(entity)) {
                return;
            }

            if (MCH_Config.FixVehicleAtPlacedPoint.prmBool && entity instanceof MCH_EntityVehicle) {
                return;
            }

            MCH_EntityChain towingChain = getTowedEntityChain(entity);
            if (towingChain != null) {
                towingChain.setDead();
                return;
            }

            Entity entityTowed = getTowedEntity(item, world);
            if (entityTowed == null) {
                playConnectTowedEntity(entity);
                setTowedEntity(item, entity);
            } else {
                if (W_Entity.isEqual(entityTowed, entity)) {
                    return;
                }

                double diff = entity.getDistance(entityTowed);
                if (diff < 2.0 || diff > 16.0) {
                    return;
                }

                MCH_EntityChain chain = new MCH_EntityChain(
                        world, (entityTowed.posX + entity.posX) / 2.0, (entityTowed.posY + entity.posY) / 2.0,
                        (entityTowed.posZ + entity.posZ) / 2.0);
                chain.setChainLength((int) diff);
                chain.setTowEntity(entityTowed, entity);
                chain.prevPosX = chain.posX;
                chain.prevPosY = chain.posY;
                chain.prevPosZ = chain.posZ;
                world.spawnEntity(chain);
                playConnectTowingEntity(entity);
                setTowedEntity(item, null);
            }
        }
    }

    public static void playConnectTowingEntity(Entity e) {
        W_WorldFunc.MOD_playSoundEffect(e.world, e.posX, e.posY, e.posZ, "chain_ct", 1.0F, 1.0F);
    }

    public static void playConnectTowedEntity(Entity e) {
        W_WorldFunc.MOD_playSoundEffect(e.world, e.posX, e.posY, e.posZ, "chain", 1.0F, 1.0F);
    }

    @Nullable
    public static MCH_EntityChain getTowedEntityChain(Entity entity) {
        List<MCH_EntityChain> list = entity.world.getEntitiesWithinAABB(MCH_EntityChain.class,
                entity.getEntityBoundingBox().grow(25.0, 25.0, 25.0));
        return null;
    }

    public static void setTowedEntity(ItemStack item, @Nullable Entity entity) {
        NBTTagCompound nbt = item.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            item.setTagCompound(nbt);
        }

        if (entity != null && !entity.isDead) {
            nbt.setInteger("TowedEntityId", W_Entity.getEntityId(entity));
            nbt.setString("TowedEntityUUID", entity.getPersistentID().toString());
        } else {
            nbt.setInteger("TowedEntityId", 0);
            nbt.setString("TowedEntityUUID", "");
        }
    }

    @Nullable
    public static Entity getTowedEntity(ItemStack item, World world) {
        NBTTagCompound nbt = item.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            item.setTagCompound(nbt);
        } else if (nbt.hasKey("TowedEntityId") && nbt.hasKey("TowedEntityUUID")) {
            int id = nbt.getInteger("TowedEntityId");
            String uuid = nbt.getString("TowedEntityUUID");
            Entity entity = world.getEntityByID(id);
            if (entity != null && !entity.isDead && uuid.compareTo(entity.getPersistentID().toString()) == 0) {
                return entity;
            }
        }

        return null;
    }

    public void onCreated(@NotNull ItemStack par1ItemStack, @NotNull World par2World,
                          @NotNull EntityPlayer par3EntityPlayer) {}
}
