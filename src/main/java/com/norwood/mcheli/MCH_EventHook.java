package com.norwood.mcheli;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;
import com.norwood.mcheli.chain.MCH_ItemChain;
import com.norwood.mcheli.command.MCH_Command;
import com.norwood.mcheli.networking.packet.PacketSyncServerSettings;
import com.norwood.mcheli.weapon.MCH_EntityBaseBullet;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_EntityPlayer;
import com.norwood.mcheli.wrapper.W_EventHook;
import com.norwood.mcheli.wrapper.W_Lib;

public class MCH_EventHook extends W_EventHook {

    @Override
    public void commandEvent(CommandEvent event) {
        MCH_Command.onCommandEvent(event);
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        MCH_MOD.proxy.registerParticleTextures(event);
    }

    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        MCH_MOD.proxy.registerShaders(event);
    }

    @Override
    public void entitySpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        // Mob spawn: adjust render distance
        if (W_Lib.isEntityLivingBase(entity) && !W_EntityPlayer.isPlayer(entity)) {
            MCH_MOD.proxy.setRenderEntityDistanceWeight(MCH_Config.MobRenderDistanceWeight.prmDouble);
            return;
        }

        // Aircraft spawn: create seats
        if (entity instanceof MCH_EntityAircraft aircraft) {
            if (!aircraft.world.isRemote && !aircraft.isCreatedSeats()) {
                aircraft.createSeats(UUID.randomUUID().toString());
            }
            return;
        }

        // Player spawn: fix invalid rotation, send server settings
        if (W_EntityPlayer.isPlayer(entity)) {
            correctInvalidRotation(entity);

            if (!entity.world.isRemote && entity instanceof EntityPlayerMP) {
                MCH_Lib.DbgLog(false, "EntityJoinWorldEvent: " + entity);
                PacketSyncServerSettings.send((EntityPlayerMP) entity);
            }
        }
    }

    private void correctInvalidRotation(Entity e) {
        boolean invalidPitch = Float.isNaN(e.rotationPitch) || Float.isNaN(e.prevRotationPitch) ||
                Float.isInfinite(e.rotationPitch) || Float.isInfinite(e.prevRotationPitch);

        if (invalidPitch) {
            MCH_Lib.Log(e, "### EntityJoinWorldEvent Error: Player invalid rotation pitch (" + e.rotationPitch + ")");
            e.rotationPitch = 0.0F;
            e.prevRotationPitch = 0.0F;
        }

        boolean invalidYaw = Float.isNaN(e.rotationYaw) || Float.isNaN(e.prevRotationYaw) ||
                Float.isInfinite(e.rotationYaw) || Float.isInfinite(e.prevRotationYaw);

        if (invalidYaw) {
            MCH_Lib.Log(e, "### EntityJoinWorldEvent Error: Player invalid rotation yaw (" + e.rotationYaw + ")");
            e.rotationYaw = 0.0F;
            e.prevRotationYaw = 0.0F;
        }
    }

    @Override
    public void livingAttackEvent(LivingAttackEvent event) {
        MCH_EntityAircraft ac = this.getRiddenAircraft(event.getEntity());
        if (ac != null) {
            if (ac.getAcInfo() != null) {
                if (!ac.isDestroyed()) {
                    if (!(ac.getAcInfo().damageFactor > 0.0F)) {
                        Entity attackEntity = event.getSource().getTrueSource();
                        if (attackEntity == null) {
                            event.setCanceled(true);
                        } else if (W_Entity.isEqual(attackEntity, event.getEntity())) {
                            event.setCanceled(true);
                        } else if (ac.isMountedEntity(attackEntity)) {
                            event.setCanceled(true);
                        } else {
                            MCH_EntityAircraft atkac = this.getRiddenAircraft(attackEntity);
                            if (W_Entity.isEqual(atkac, ac)) {
                                event.setCanceled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void livingHurtEvent(LivingHurtEvent event) {
        Entity entity = event.getEntity();
        MCH_EntityAircraft aircraft = this.getRiddenAircraft(entity);

        if (aircraft == null || aircraft.isDestroyed() || aircraft.getAcInfo() == null) {
            return;
        }

        Entity attacker = event.getSource().getTrueSource();
        float damage = event.getAmount();
        float factor = aircraft.getAcInfo().damageFactor;

        boolean selfDamage = (attacker == null) || W_Entity.isEqual(attacker, entity);
        boolean isRiderAttacking = aircraft.isMountedEntity(attacker);
        boolean isSameAircraft = W_Entity.isEqual(this.getRiddenAircraft(attacker), aircraft);

        if (isRiderAttacking || isSameAircraft) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }

        aircraft.attackEntityFrom(event.getSource(), damage * 2.0F);
        event.setAmount(damage * factor);
    }

    public MCH_EntityAircraft getRiddenAircraft(Entity entity) {
        if (entity == null) return null;
        MCH_EntityAircraft ac = null;
        Entity ridden = entity.getRidingEntity();
        if (ridden == null) {
            return null;
        }
        if (ridden instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft) ridden;
        } else if (ridden instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat) ridden).getParent();
        }

        if (ac == null) {
            List<MCH_EntityAircraft> list = entity.world.getEntitiesWithinAABB(MCH_EntityAircraft.class,
                    entity.getEntityBoundingBox().grow(50.0, 50.0, 50.0));
            for (MCH_EntityAircraft tmp : list) {
                if (tmp.isMountedEntity(entity)) {
                    return tmp;
                }
            }
        }

        return ac;
    }

    @Override
    public void entityInteractEvent(EntityInteract event) {
        ItemStack item = event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND);
        if (!item.isEmpty()) {
            if (item.getItem() instanceof MCH_ItemChain) {
                MCH_ItemChain.interactEntity(item, event.getTarget(), event.getEntityPlayer(),
                        event.getEntityPlayer().world);
                event.setCanceled(true);
            } else if (item.getItem() instanceof MCH_ItemAircraft) {
                ((MCH_ItemAircraft) item.getItem()).rideEntity(item, event.getTarget(), event.getEntityPlayer());
            }
        }
    }

    @Override
    public void entityCanUpdate(CanUpdate event) {
        if (event.getEntity() instanceof MCH_EntityBaseBullet bullet) {
            bullet.setDead();
        }
    }
}
