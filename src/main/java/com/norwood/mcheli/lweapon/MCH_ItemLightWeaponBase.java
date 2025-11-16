package com.norwood.mcheli.lweapon;

import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MCH_ItemLightWeaponBase extends W_Item {

    public final MCH_ItemLightWeaponBullet bullet;

    public MCH_ItemLightWeaponBase(int par1, MCH_ItemLightWeaponBullet bullet) {
        super(par1);
        this.setMaxDamage(10);
        this.setMaxStackSize(1);
        this.bullet = bullet;
    }

    public static String getName(ItemStack itemStack) {
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof MCH_ItemLightWeaponBase) {
            String name = itemStack.getTranslationKey();
            int li = name.lastIndexOf(":");
            if (li >= 0) {
                name = name.substring(li + 1);
            }

            return name;
        } else {
            return "";
        }
    }

    public static boolean isHeld(@Nullable EntityPlayer player) {
        ItemStack is = player != null ? player.getHeldItemMainhand() : ItemStack.EMPTY;
        return !is.isEmpty() && is.getItem() instanceof MCH_ItemLightWeaponBase && player.getItemInUseMaxCount() > 10;
    }

    public void onUsingTick(@NotNull ItemStack stack, EntityLivingBase player, int count) {
        PotionEffect pe = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
        if (pe != null && pe.getDuration() < 220) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 250, 0, false, false));
        }
    }

    public boolean onEntitySwing(@NotNull EntityLivingBase entityLiving, @NotNull ItemStack stack) {
        return true;
    }

    public @NotNull EnumAction getItemUseAction(@NotNull ItemStack par1ItemStack) {
        return EnumAction.BOW;
    }

    public int getMaxItemUseDuration(@NotNull ItemStack par1ItemStack) {
        return 72000;
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World worldIn, EntityPlayer playerIn,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if (!itemstack.isEmpty()) {
            playerIn.setActiveHand(handIn);
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
    }
}
