package com.norwood.mcheli.tool.rangefinder;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.networking.packet.PacketRequestSpotEnemy;
import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.W_Reflection;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public class MCH_ItemRangeFinder extends W_Item {

    public static int rangeFinderUseCooldown = 0;
    public static boolean continueUsingItem = false;
    public static float zoom = 2.0F;
    public static int mode = 0;

    public MCH_ItemRangeFinder(int itemId) {
        super(itemId);
        this.maxStackSize = 1;
        this.setMaxDamage(10);
    }

    public static boolean canUse(EntityPlayer player) {
        if (player == null) {
            return false;
        } else if (player.world == null) {
            return false;
        } else if (player.getHeldItemMainhand().isEmpty()) {
            return false;
        } else if (!(player.getHeldItemMainhand().getItem() instanceof MCH_ItemRangeFinder)) {
            return false;
        } else if (player.getRidingEntity() instanceof MCH_EntityAircraft) {
            return false;
        } else {
            if (player.getRidingEntity() instanceof MCH_EntitySeat) {
                MCH_EntityAircraft ac = ((MCH_EntitySeat) player.getRidingEntity()).getParent();
                return ac == null ||
                        (!ac.getIsGunnerMode(player) && ac.getWeaponIDBySeatID(ac.getSeatIdByEntity(player)) < 0);
            }

            return true;
        }
    }

    public static boolean isUsingScope(EntityPlayer player) {
        return player.getItemInUseMaxCount() > 8 || continueUsingItem;
    }

    public static void onStartUseItem() {
        zoom = 2.0F;
        W_Reflection.setCameraZoom(2.0F);
        continueUsingItem = true;
    }

    public static void onStopUseItem() {
        W_Reflection.restoreCameraZoom();
        continueUsingItem = false;
    }

    @SideOnly(Side.CLIENT)
    public void spotEntity(EntityPlayer player, ItemStack itemStack) {
        if (player != null && player.world.isRemote && rangeFinderUseCooldown == 0 &&
                player.getItemInUseMaxCount() > 8) {
            if (mode == 2) {
                rangeFinderUseCooldown = 60;
                new PacketRequestSpotEnemy(0).sendToServer();
            } else if (itemStack.getMetadata() < itemStack.getMaxDamage()) {
                rangeFinderUseCooldown = 60;
                new PacketRequestSpotEnemy(mode == 0 ? 60 : 3).sendToServer();
            } else {
                W_McClient.MOD_playSoundFX("ng", 1.0F, 1.0F);
            }
        }
    }

    public void onPlayerStoppedUsing(@NotNull ItemStack stack, World worldIn, @NotNull EntityLivingBase entityLiving,
                                     int timeLeft) {
        if (worldIn.isRemote) {
            onStopUseItem();
        }
    }

    public @NotNull ItemStack onItemUseFinish(@NotNull ItemStack stack, @NotNull World worldIn,
                                              @NotNull EntityLivingBase entityLiving) {
        return stack;
    }

    @SideOnly(Side.CLIENT)
    public boolean isFull3D() {
        return true;
    }

    public @NotNull EnumAction getItemUseAction(@NotNull ItemStack itemStack) {
        return EnumAction.BOW;
    }

    public int getMaxItemUseDuration(@NotNull ItemStack itemStack) {
        return 72000;
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = player.getHeldItem(handIn);
        if (canUse(player)) {
            player.setActiveHand(handIn);
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
    }
}
