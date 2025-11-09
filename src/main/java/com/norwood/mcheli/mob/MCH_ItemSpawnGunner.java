package com.norwood.mcheli.mob;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.wrapper.W_Item;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_ItemSpawnGunner extends W_Item {

    public int primaryColor = 16777215;
    public int secondaryColor = 16777215;
    public int targetType = 0;

    public MCH_ItemSpawnGunner() {
        this.maxStackSize = 1;
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @SideOnly(Side.CLIENT)
    public static int getColorFromItemStack(ItemStack stack, int tintIndex) {
        MCH_ItemSpawnGunner item = (MCH_ItemSpawnGunner) stack.getItem();
        return tintIndex == 0 ? item.primaryColor : item.secondaryColor;
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        ItemStack itemstack = player.getHeldItem(handIn);
        float f = 1.0F;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double dx = player.prevPosX + (player.posX - player.prevPosX) * f;
        double dy = player.prevPosY + (player.posY - player.prevPosY) * f + player.getEyeHeight();
        double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
        Vec3d vec3 = new Vec3d(dx, dy, dz);
        float f3 = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f5 = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
        float f6 = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = 5.0;
        Vec3d vec31 = vec3.add(f7 * d3, f6 * d3, f8 * d3);
        List<MCH_EntityGunner> list = world.getEntitiesWithinAABB(MCH_EntityGunner.class,
                player.getEntityBoundingBox().grow(5.0, 5.0, 5.0));
        Entity target = null;

        for (MCH_EntityGunner gunner : list) {
            if (gunner.getEntityBoundingBox().calculateIntercept(vec3, vec31) != null &&
                    (target == null || player.getDistanceSq(gunner) < player.getDistanceSq(target))) {
                target = gunner;
            }
        }

        if (target == null) {
            List<MCH_EntitySeat> list1 = world.getEntitiesWithinAABB(MCH_EntitySeat.class,
                    player.getEntityBoundingBox().grow(5.0, 5.0, 5.0));

            for (MCH_EntitySeat seat : list1) {
                if (seat.getParent() != null && seat.getParent().getAcInfo() != null &&
                        seat.getEntityBoundingBox().calculateIntercept(vec3, vec31) != null &&
                        (target == null || player.getDistanceSq(seat) < player.getDistanceSq(target))) {
                    if (seat.getRiddenByEntity() instanceof MCH_EntityGunner) {
                        target = seat.getRiddenByEntity();
                    } else {
                        target = seat;
                    }
                }
            }
        }

        if (target == null) {
            List<MCH_EntityAircraft> list2 = world.getEntitiesWithinAABB(MCH_EntityAircraft.class,
                    player.getEntityBoundingBox().grow(5.0, 5.0, 5.0));

            for (MCH_EntityAircraft ac : list2) {
                if (!ac.isUAV() && ac.getAcInfo() != null &&
                        ac.getEntityBoundingBox().calculateIntercept(vec3, vec31) != null &&
                        (target == null || player.getDistanceSq(ac) < player.getDistanceSq(target))) {
                    if (ac.getRiddenByEntity() instanceof MCH_EntityGunner) {
                        target = ac.getRiddenByEntity();
                    } else {
                        target = ac;
                    }
                }
            }
        }

        if (target instanceof MCH_EntityGunner) {
            target.processInitialInteract(player, handIn);
            return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
        } else {
            if (this.targetType == 1 && !world.isRemote) {
                player.getTeam();
            }
            if (target == null) {
                if (!world.isRemote) {
                    player.sendMessage(new TextComponentString("Right click to seat."));
                }

                return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
            } else {
                if (!world.isRemote) {
                    MCH_EntityGunner gunner = new MCH_EntityGunner(world, target.posX, target.posY, target.posZ);
                    gunner.rotationYaw = ((MathHelper.floor(player.rotationYaw * 4.0F / 360.0F + 0.5) & 3) - 1) * 90;
                    gunner.isCreative = player.capabilities.isCreativeMode;
                    gunner.targetType = this.targetType;
                    gunner.ownerUUID = player.getUniqueID().toString();
                    ScorePlayerTeam team = world.getScoreboard()
                            .getPlayersTeam(player.getDisplayName().getFormattedText());
                    if (team != null) {
                        gunner.setTeamName(team.getName());
                    }

                    world.spawnEntity(gunner);
                    gunner.startRiding(target);
                    W_WorldFunc.MOD_playSoundAtEntity(gunner, "wrench", 1.0F, 3.0F);
                    MCH_EntityAircraft ac = target instanceof MCH_EntityAircraft ? (MCH_EntityAircraft) target :
                            ((MCH_EntitySeat) target).getParent();
                    player.sendMessage(
                            new TextComponentString(
                                    "The gunner was put on " + TextFormatting.GOLD + ac.getAcInfo().displayName +
                                            TextFormatting.RESET + " seat " + (ac.getSeatIdByEntity(gunner) + 1) +
                                            " by " + ScorePlayerTeam.formatPlayerName(player.getTeam(),
                                                    player.getDisplayName().getFormattedText())));
                }

                if (!player.capabilities.isCreativeMode) {
                    itemstack.shrink(1);
                }

                return ActionResult.newResult(EnumActionResult.SUCCESS, itemstack);
            }
        }
    }
}
