package com.norwood.mcheli.uav;

import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Handheld device for binding UAVs to UAV stations.
 *
 * <p>Right-click a deployed UAV to store its entity UUID on the device, then right-click a UAV
 * station to add that UAV to the station's paired list (see {@link IUavPairingHolder}). The actual
 * interaction is handled in {@code MCH_EventHook#onUavPairingInteract} because UAV/station entities
 * are not {@code EntityLivingBase} (so {@code itemInteractionForEntity} never fires for them).
 */
public class MCH_ItemUavPairingDevice extends W_Item {

    public static final String NBT_BOUND_UAV = "BoundUav";

    public MCH_ItemUavPairingDevice() {
        super();
        this.setMaxStackSize(1);
    }

    @Nullable
    public static UUID getBoundUav(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return (tag != null && tag.hasUniqueId(NBT_BOUND_UAV)) ? tag.getUniqueId(NBT_BOUND_UAV) : null;
    }

    public static void setBoundUav(ItemStack stack, @Nullable UUID id) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        if (id == null) {
            tag.removeTag(NBT_BOUND_UAV + "Most");
            tag.removeTag(NBT_BOUND_UAV + "Least");
        } else {
            tag.setUniqueId(NBT_BOUND_UAV, id);
        }
        stack.setTagCompound(tag);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@NotNull ItemStack stack, @Nullable World world, @NotNull List<String> tooltip,
                               @NotNull ITooltipFlag flag) {
        UUID bound = getBoundUav(stack);
        if (bound != null) {
            tooltip.add("§7Bound UAV: §f" + bound.toString().substring(0, 8));
            tooltip.add("§8Right-click a UAV station to pair");
        } else {
            tooltip.add("§8Right-click a UAV to bind it");
        }
    }
}
