package com.norwood.mcheli.weapon;

import com.norwood.mcheli.wrapper.W_Item;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Handheld tool that defuses MCHeli proximity mines ({@code MCH_EntityBomb} in mine mode).
 *
 * <p>The actual defuse happens in {@link MCH_EntityBomb#processInitialInteract}: sneak up to an
 * armed mine (sneaking suppresses the proximity trigger) and right-click it while holding this item.
 * No texture/model is registered for it yet, so it renders with the default missing-model icon.
 */
public class MCH_ItemMineDefuser extends W_Item {

    public MCH_ItemMineDefuser() {
        super();
        this.setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@NotNull ItemStack stack, @Nullable World world, @NotNull List<String> tooltip,
                               @NotNull ITooltipFlag flag) {
        tooltip.add("§8Sneak up to a mine and right-click it to defuse");
    }
}
