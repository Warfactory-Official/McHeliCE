package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.FloatSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.norwood.mcheli.factories.AircraftGuiData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.function.Predicate;

public class AircraftGui {

    public static final int C_WIDTH = 410;
    public static final int C_HEIGHT = 300;
    public static final int SLOT_SIZE = 18;


    public static Predicate<ItemStack> fuelPredicate(MCH_AircraftInfo info) {
        return stack -> {
            if (stack.isEmpty() || info == null) return false;

            if (!stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null))
                return false;

            IFluidHandlerItem handler =
                    stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

            if (handler == null) return false;

            for (IFluidTankProperties prop : handler.getTankProperties()) {
                if (prop.canDrain() && info.isFuelValid(prop.getContents())) {
                    return true;
                }
            }
            return false;
        };
    }


    public static ModularPanel buildUI(AircraftGuiData data, PanelSyncManager syncManager, UISettings settings, MCH_EntityAircraft aircraft) {

        syncManager.bindPlayerInventory(data.getPlayer());
        var aircraftInvHander = aircraft.getInventory();
        var aircraftGuiInv = aircraft.getGuiInventory().getItemHandler();


        var slotRow = new Row()
                .size(SLOT_SIZE * 2 + 20, SLOT_SIZE)
                .margin(5)
                .child(new ItemSlot()
                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_FUEL_IN)
                                .filter(fuelPredicate(aircraft.getAcInfo()))))
                .child(new ItemSlot()
                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_FUEL_OUT)
                                .filter(_ -> false)

                        ).alignX(Alignment.CenterRight));

        var gauge = WidgetGauge.make()
                .value(new FloatSyncValue(aircraft::getFuelPercentage))
                .size(101, 55);

        var fuelMb = IKey.dynamic(() -> Integer.toString(aircraft.getFuel()))
                .asWidget()
                .padding(6, 2)
                .width(SLOT_SIZE*2+20)
                .alignX(Alignment.Center)
                .color(() -> {
                    var fuelPercent = aircraft.getFuelPercentage();
                    if(fuelPercent >= 1.0f) return 0xFFB2FF59;
                    if(fuelPercent <= 0.0f) return 0xFFFF5252;
                    return 0xFFFFFFFF;
                })
                .background(GuiTextures.MENU_BACKGROUND.withColorOverride(0xFF000000));


        var fuelSlots = new Column()
                .padding(5)
                .margin(5)
                .align(Alignment.Center)
                .child(gauge)
                .child(fuelMb)
                .child(slotRow)
                .coverChildren();

        var inventory = new ParentWidget<>().name("inventory_wrapper")
                .child(
                        SlotGroupWidget.playerInventory(false)
                )
                .coverChildren()
                .padding(5)
                .background(GuiTextures.MC_BACKGROUND);

        var vehicleGui = new ParentWidget<>().name("vehicle_gui")
                .child(
                        new Column()
                                .size(C_WIDTH - 10, 200)
                                .child(fuelSlots)
                )
                .coverChildren()
                .padding(5)
                .background(GuiTextures.MC_BACKGROUND);


        return new ModularPanel("container")
                .size(C_WIDTH, C_HEIGHT)
                .invisible()
                .child(
                        new Column()
                                .bottom(0)
                                .child(vehicleGui)
                                .child(inventory)
                );
    }

}
