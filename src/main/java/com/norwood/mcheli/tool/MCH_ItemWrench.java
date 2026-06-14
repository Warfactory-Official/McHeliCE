package com.norwood.mcheli.tool;

import com.google.common.collect.Multimap;
import com.norwood.mcheli.MCH_ClientProxy;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.sound.MCH_SoundEvents;
import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class MCH_ItemWrench extends W_Item {

    private static final Random rand = new Random();
    private final ToolMaterial toolMaterial;
    private final float damageVsEntity;

    public MCH_ItemWrench(int itemId, ToolMaterial material) {
        super(itemId);
        this.toolMaterial = material;
        this.maxStackSize = 1;
        this.setMaxDamage(material.getMaxUses());
        this.damageVsEntity = 4.0F + material.getAttackDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.YELLOW + "Shift+Right click to change vehicle skin!");

        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

    public static float getUseAnimSmooth(ItemStack stack, float partialTicks) {
        int i = Math.abs(getUseAnimCount(stack) - 8);
        int j = Math.abs(getUseAnimPrevCount(stack) - 8);
        return j + (i - j) * partialTicks;
    }

    public static int getUseAnimPrevCount(ItemStack stack) {
        return getAnimCount(stack, "MCH_WrenchAnimPrev");
    }

    public static int getUseAnimCount(ItemStack stack) {
        return getAnimCount(stack, "MCH_WrenchAnim");
    }

    public static void setUseAnimCount(ItemStack stack, int n, int prev) {
        setAnimCount(stack, "MCH_WrenchAnim", n);
        setAnimCount(stack, "MCH_WrenchAnimPrev", prev);
    }

    public static int getAnimCount(ItemStack stack, String name) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        if (stack.getTagCompound().hasKey(name)) {
            return stack.getTagCompound().getInteger(name);
        } else {
            stack.getTagCompound().setInteger(name, 0);
            return 0;
        }
    }

    public static void setAnimCount(ItemStack stack, String name, int n) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        stack.getTagCompound().setInteger(name, n);
    }


    public boolean canHarvestBlock(IBlockState blockIn) {
        Material material = blockIn.getMaterial();
        return material == Material.IRON || material instanceof MaterialLogic;
    }

    public float getDestroySpeed(@NotNull ItemStack stack, IBlockState state) {
        Material material = state.getMaterial();
        if (material == Material.IRON) {
            return 20.5F;
        } else {
            return material instanceof MaterialLogic ? 5.5F : 2.0F;
        }
    }

    public boolean hitEntity(@NotNull ItemStack itemStack, @NotNull EntityLivingBase entity, EntityLivingBase player) {
        if (!player.world.isRemote) {
            if (rand.nextInt(40) == 0) {
                entity.entityDropItem(new ItemStack(W_Item.getItemByName("iron_ingot"), 1, 0), 0.0F);
            } else if (rand.nextInt(20) == 0) {
                entity.entityDropItem(new ItemStack(W_Item.getItemByName("gunpowder"), 1, 0), 0.0F);
            }
        }

        itemStack.damageItem(2, player);
        return true;
    }

    public void onPlayerStoppedUsing(@NotNull ItemStack stack, @NotNull World worldIn,
                                     @NotNull EntityLivingBase entityLiving, int timeLeft) {
        setUseAnimCount(stack, 0, 0);
    }

    public void onUsingTick(@NotNull ItemStack stack, EntityLivingBase player, int count) {
        if (player.world.isRemote) {
            MCH_EntityAircraft ac = MCH_ClientProxy.getMouseOverAircraft(player);
            if (ac != null) {
                int cnt = getUseAnimCount(stack);
                int prev = cnt;
                if (cnt <= 0) {
                    cnt = 16;
                } else {
                    cnt--;
                }

                setUseAnimCount(stack, cnt, prev);
            }
        }

        if (!player.world.isRemote && count < this.getMaxItemUseDuration(stack) && count % 20 == 0) {
            MCH_EntityAircraft ac = MCH_ClientProxy.getMouseOverAircraft(player);
            if (ac != null && ac.getHP() > 0 && ac.repair(10)) {
                stack.damageItem(1, player);
                float pitch = 0.9F + rand.nextFloat() * 0.2F;
                MCH_SoundEvents.playSound(player.world, (int) ac.posX, (int) ac.posY, (int) ac.posZ, "wrench", 1.0F, pitch);
            }
        }
    }

    public void onUpdate(@NotNull ItemStack item, @NotNull World world, @NotNull Entity entity, int n, boolean b) {
        if (entity instanceof EntityPlayer player) {
            ItemStack itemStack = player.getHeldItemMainhand();
            if (itemStack == item) {
                MCH_MOD.proxy.setCreativeDigDelay(0);
            }
        }
    }


    public boolean onBlockDestroyed(@NotNull ItemStack itemStack, @NotNull World world, IBlockState state,
                                    @NotNull BlockPos pos, @NotNull EntityLivingBase entity) {
        if (state.getBlockHardness(world, pos) != 0.0) {
            itemStack.damageItem(2, entity);
        }

        return true;
    }

    @SideOnly(Side.CLIENT)
    public boolean isFull3D() {
        return true;
    }

    public @NotNull EnumAction getItemUseAction(@NotNull ItemStack itemStack) {
        return EnumAction.BLOCK;
    }

    public int getMaxItemUseDuration(@NotNull ItemStack itemStack) {
        return 72000;
    }

    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, EntityPlayer player,
                                                             @NotNull EnumHand handIn) {
        player.setActiveHand(handIn);
        return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(handIn));
    }

    public int getItemEnchantability() {
        return this.toolMaterial.getEnchantability();
    }

    public String getToolMaterialName() {
        return this.toolMaterial.toString();
    }

    public boolean getIsRepairable(@NotNull ItemStack item1, @NotNull ItemStack item2) {
        ItemStack mat = this.toolMaterial.getRepairItemStack();
        return !mat.isEmpty() && OreDictionary.itemMatches(mat, item2, false) || super.getIsRepairable(item1, item2);
    }

    public @NotNull Multimap<String, AttributeModifier> getItemAttributeModifiers(@NotNull EntityEquipmentSlot equipmentSlot) {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", this.damageVsEntity, 0));
        }

        return multimap;
    }
}
