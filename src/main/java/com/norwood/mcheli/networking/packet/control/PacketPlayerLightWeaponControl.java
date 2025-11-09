package com.norwood.mcheli.networking.packet.control;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.Vec3d;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.lweapon.MCH_ItemLightWeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponBase;
import com.norwood.mcheli.weapon.MCH_WeaponCreator;
import com.norwood.mcheli.weapon.MCH_WeaponParam;
import com.norwood.mcheli.wrapper.W_EntityPlayer;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;

@ElegantPacket
public class PacketPlayerLightWeaponControl implements ClientToServerPacket {

    public boolean useWeapon = false;
    public int useWeaponOption1 = 0;
    public int useWeaponOption2 = 0;
    public double useWeaponPosX = 0.0;
    public double useWeaponPosY = 0.0;
    public double useWeaponPosZ = 0.0;
    public int cmpReload = 0;
    public int camMode = 0;

    private void handleWeaponFire(EntityPlayer player, ItemStack weaponStack) {
        String weaponName = MCH_ItemLightWeaponBase.getName(weaponStack);
        MCH_WeaponBase weapon = MCH_WeaponCreator.createWeapon(
                player.world, weaponName, Vec3d.ZERO, 0.0F, 0.0F, null, false);

        MCH_WeaponParam prm = new MCH_WeaponParam();
        prm.entity = player;
        prm.user = player;
        prm.setPosAndRot(useWeaponPosX, useWeaponPosY, useWeaponPosZ, player.rotationYaw, player.rotationPitch);
        prm.option1 = useWeaponOption1;
        prm.option2 = useWeaponOption2;

        weapon.shot(prm);

        if (!player.capabilities.isCreativeMode) {
            if (weaponStack.getMaxDamage() == 1) {
                weaponStack.shrink(1);
            } else if (weaponStack.getMaxDamage() > 1) {
                weaponStack.setItemDamage(weaponStack.getMaxDamage());
            }
        }
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        final ItemStack held = player.getHeldItemMainhand();

        if (camMode == 1) {
            player.removePotionEffect(MobEffects.NIGHT_VISION);
        } else if (camMode == 2 && MCH_ItemLightWeaponBase.isHeld(player)) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 255, 0, false, false));
        }

        if (camMode > 0) {
            MCH_Lib.DbgLog(false, "MCH_LightWeaponPacketHandler NV=%s", camMode == 2 ? "ON" : "OFF");
        }

        if (held.isEmpty() || !(held.getItem() instanceof MCH_ItemLightWeaponBase lweapon)) {
            return;
        }

        if (useWeapon && held.getMetadata() < held.getMaxDamage()) {
            handleWeaponFire(player, held);
            return;
        }

        if (cmpReload > 0 && held.getMetadata() > 1 && W_EntityPlayer.hasItem(player, lweapon.bullet)) {
            if (!player.capabilities.isCreativeMode) {
                W_EntityPlayer.consumeInventoryItem(player, lweapon.bullet);
            }
            held.setItemDamage(0);
        }
    }
}
