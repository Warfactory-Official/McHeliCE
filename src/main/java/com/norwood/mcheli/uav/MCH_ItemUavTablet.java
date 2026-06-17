package com.norwood.mcheli.uav;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.factories.UavStationGuiFactory;
import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handheld "UAV tablet": a UAV station you carry instead of place. Its paired-UAV list lives in the
 * item's NBT. Right-click a deployed UAV to bind it (handled in {@code MCH_EventHook}); right-click in
 * the air to open the station screen — which on the server seats the player on a <b>temporary</b>
 * {@link MCH_EntityUavStation} (seeded from this tablet's NBT) that despawns and writes its pairings
 * back to the tablet when the player dismounts.
 *
 * <p>By default the tablet only handles small UAVs; {@link MCH_Config#UavTabletControlNormalUav} lifts
 * that to full-size UAVs too.
 */
public class MCH_ItemUavTablet extends W_Item {

    private static final String NBT_PAIRED = "PairedUavs";

    public MCH_ItemUavTablet() {
        super();
        this.setMaxStackSize(1);
    }

    /** @return whether the tablet may bind/control full-size (non-small) UAVs (config-gated). */
    public static boolean allowsNormalUav() {
        return MCH_Config.UavTabletControlNormalUav != null && MCH_Config.UavTabletControlNormalUav.prmBool;
    }

    public static List<UUID> getPairedUavs(ItemStack stack) {
        List<UUID> out = new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return out;
        }
        NBTTagList list = tag.getTagList(NBT_PAIRED, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            out.add(list.getCompoundTagAt(i).getUniqueId("ID"));
        }
        return out;
    }

    public static void setPairedUavs(ItemStack stack, java.util.Collection<UUID> ids) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (UUID id : new LinkedHashSet<>(ids)) {
            NBTTagCompound e = new NBTTagCompound();
            e.setUniqueId("ID", id);
            list.appendTag(e);
        }
        tag.setTag(NBT_PAIRED, list);
        stack.setTagCompound(tag);
    }

    /** @return true if the UAV was newly added (false if already present). */
    public static boolean addPairedUav(ItemStack stack, UUID id) {
        Set<UUID> ids = new LinkedHashSet<>(getPairedUavs(stack));
        boolean added = ids.add(id);
        if (added) {
            setPairedUavs(stack, ids);
        }
        return added;
    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull net.minecraft.world.World world,
                                                             EntityPlayer player, @NotNull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            return ActionResult.newResult(EnumActionResult.PASS, stack);
        }
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            // Seat the player on a transient station seeded from the tablet, then open its screen.
            MCH_EntityUavStation station = MCH_EntityUavStation.createHandheld(
                    world, player, hand, getPairedUavs(stack), !allowsNormalUav());
            world.spawnEntity(station);
            player.startRiding(station, true);
            UavStationGuiFactory.INSTANCE.openGui(player, station);
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@NotNull ItemStack stack, @Nullable net.minecraft.world.World world,
                               @NotNull List<String> tooltip, @NotNull ITooltipFlag flag) {
        int n = getPairedUavs(stack).size();
        tooltip.add("§7Paired UAVs: §f" + n);
        tooltip.add("§8Right-click a UAV to bind it");
        tooltip.add("§8Right-click in the air to open");
        if (!allowsNormalUav()) {
            tooltip.add("§8Small UAVs only");
        }
    }
}
