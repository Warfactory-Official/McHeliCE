package com.norwood.mcheli;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;
import com.norwood.mcheli.wrapper.W_Item;

public class MCH_CreativeTabs extends CreativeTabs {

    private final List<ItemStack> iconItems;
    private ItemStack lastItem;
    private int currentIconIndex;
    private int switchItemWait;
    private Item fixedItem = null;

    public MCH_CreativeTabs(String label) {
        super(label);
        this.iconItems = new ArrayList<>();
        this.currentIconIndex = 0;
        this.switchItemWait = 0;
        this.lastItem = ItemStack.EMPTY;
    }

    public void setFixedIconItem(String itemName) {
        if (itemName.indexOf(58) >= 0) {
            this.fixedItem = W_Item.getItemByName(itemName);
        } else {
            this.fixedItem = W_Item.getItemByName("mcheli:" + itemName);
            if (this.fixedItem != null) {}
        }
    }

    public @NotNull ItemStack createIcon() {
        if (this.iconItems.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.currentIconIndex = (this.currentIconIndex + 1) % this.iconItems.size();
            return this.iconItems.get(this.currentIconIndex);
        }
    }

    public @NotNull ItemStack getIcon() {
        if (this.fixedItem != null) {
            return new ItemStack(this.fixedItem, 1, 0);
        } else {
            if (this.switchItemWait > 0) {
                this.switchItemWait--;
            } else {
                this.lastItem = this.createIcon();
                this.switchItemWait = 60;
            }

            if (this.lastItem.isEmpty()) {
                this.lastItem = new ItemStack(W_Item.getItemByName("iron_block"));
            }

            return this.lastItem;
        }
    }

    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(@NotNull NonNullList<ItemStack> list) {
        super.displayAllRelevantItems(list);
        Comparator<ItemStack> cmp = (i1, i2) -> {
            if (i1.getItem() instanceof MCH_ItemAircraft && i2.getItem() instanceof MCH_ItemAircraft) {
                MCH_AircraftInfo info1 = ((MCH_ItemAircraft) i1.getItem()).getAircraftInfo();
                MCH_AircraftInfo info2 = ((MCH_ItemAircraft) i2.getItem()).getAircraftInfo();
                if (info1 != null && info2 != null) {
                    String s1 = info1.category + "." + info1.name;
                    String s2 = info2.category + "." + info2.name;
                    return s1.compareTo(s2);
                }
            }

            return i1.getItem().getTranslationKey().compareTo(i2.getItem().getTranslationKey());
        };
        list.sort(cmp);
    }

    public void addIconItem(Item i) {
        if (i != null) {
            this.iconItems.add(new ItemStack(i));
        }
    }

    public @NotNull String getTranslationKey() {
        return this.getTabLabel();
    }
}
