package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.FloatSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.norwood.mcheli.factories.AircraftGuiData;
import com.norwood.mcheli.networking.packet.PacketOpenScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.Arrays;
import java.util.function.Consumer;
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
        syncManager.registerServerSyncedAction("dumpFuel", (_) -> aircraft.dumpFuel());


        var slotRow = new Row()
                .size(SLOT_SIZE * 2 + 20, SLOT_SIZE)
                .margin(5)
                .child(new ItemSlot()
                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_FUEL_IN)
                                .filter(fuelPredicate(aircraft.getAcInfo()))))
                .child(new ButtonWidget<>().onMouseTapped(_ -> {
                                    syncManager.callSyncedAction("dumpFuel");
                                    return true;
                                }).size(18)
                                .overlay(GuiTextures.CROSS)
                                .tooltip(tooltip -> tooltip.addLine("Empty the tank")
                                        .add(IKey.str("Warning!").style(TextFormatting.RED, TextFormatting.BOLD))
                                        .add(IKey.str(" The contents of the tank will be lost!").style(TextFormatting.RED)))


                                .relativeToParent().center()
                )
                .child(new ItemSlot()
                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_FUEL_OUT)
                                .filter(_ -> false)

                        ).alignX(Alignment.CenterRight));

        Consumer<RichTooltip> tooltipConsumer = tooltip -> {
            tooltip
                    .addLine(IKey.str("Fuel").style(TextFormatting.GREEN))
                    .addLine(IKey.dynamic(() -> String.format("Current fuel: %s (%.2f)",
                            aircraft.getFuel() > 0
                                    && aircraft.getFuelFluid() != null
                                    && FluidRegistry.isFluidRegistered(aircraft.getFuelType()) ?
                                    new FluidStack(aircraft.getFuelFluid(), 1).getLocalizedName()
                                    : "Empty",
                            data.getInfo().getFuelConsumption(aircraft.getFuelType())
                    )))
                    .addLine(IKey.dynamic(() -> String.format("%d / %dmB", aircraft.getFuel(), aircraft.getMaxFuel())))
                    .addLine("Accepted Fuels and consumption factor:");

            data.getInfo().getFluidType().forEach((fuel, eff) ->
                    {
                        TextFormatting color = switch (eff) {
                            case Float d when d == 1f -> TextFormatting.WHITE;
                            case Float d when d < 0.5f -> TextFormatting.BLUE;
                            case Float d when d < 1.0f -> TextFormatting.GREEN;
                            case Float d when d <= 1.5f -> TextFormatting.YELLOW;
                            default -> TextFormatting.RED;
                        };

                        if (!FluidRegistry.isFluidRegistered(fuel)) return;
                        tooltip.addLine(IKey.str("– %s : %.2f", new FluidStack(FluidRegistry.getFluid(fuel), 1).getLocalizedName(), eff)
                                .style(color));

                    }
            );

        };

        var gauge = WidgetGauge.make(0.4f)
                .value(new FloatSyncValue(aircraft::getFuelPercentage)).marginBottom(2).tooltip(tooltipConsumer);

        var fuelMb = IKey.dynamic(() ->
                        String.format("%d mB", aircraft.getFuel())
                )
                .asWidget()
                .padding(6, 2)
                .width(SLOT_SIZE * 2 + 20)
                .alignX(Alignment.Center)
                .tooltip(tooltipConsumer)
                .color(() -> {
                    var fuelPercent = aircraft.getFuelPercentage();
                    if (fuelPercent >= 1.0f) return 0xFFB2FF59;
                    if (fuelPercent <= 0.0f) return 0xFFFF5252;
                    return 0xFFFFFFFF;
                })
                .background(GuiTextures.MENU_BACKGROUND.withColorOverride(0xFF000000));

        var grid = new Column().name("trunk")
                .margin(2)
                .padding(2)
                .child(IKey.str("Storage (%d)", aircraftInvHander.getSlots()).asWidget().alignX(Alignment.BottomLeft))
                .child(new Grid()
                        .margin(2)
                        .mapTo(8, aircraftInvHander.getSlots(), (slot) ->
                                new ItemSlot()
                                        .slot(new ModularSlot(aircraftInvHander, slot))
                                        .size(SLOT_SIZE)
                        )
                        .size(18 * 8 + 5, 18 * 5)
                        .scrollable(new VerticalScrollData())
                ).coverChildren().margin(2);


        var fuelSlots = new Column()
                .align(Alignment.TopRight)
                .child(gauge)
                .child(fuelMb)
                .child(slotRow)
                .coverChildren();

        var settingsButton = new ButtonWidget<>().onMouseTapped(
                        _ -> {
                            PacketOpenScreen.send(2);
                            return false;
                        }
                ).background(GuiTextures.MC_BUTTON)
                .overlay(GuiTextures.GEAR)
                .size(18);

        var inventory = new Row().invisible().child(
                        new ParentWidget<>().name("inventory_wrapper")
                                .child(
                                        SlotGroupWidget.playerInventory(false)
                                )
                                .coverChildren()
                                .padding(5)
                                .background(GuiTextures.MC_BACKGROUND)
                ).child(settingsButton.top(0))
                .coverChildren();


        var weaponList = new ListWidget<>().children(
                Arrays.asList(aircraft.getWeapons()), weaponRow -> new Row()
                        .child(
                               new ScrollingTextWidget(IKey.str(weaponRow.getName()))
                                       .tooltip(t -> t.addLine(weaponRow.getName()))
                                       .width(100)
                        )
                        .child(IKey.str("%s/%s", weaponRow.getAmmoNum(), weaponRow.getAmmoNumMax() ).asWidget())
                        .height(20).background(GuiTextures.MENU_BACKGROUND)


        ).scrollDirection(GuiAxis.Y).size(200);


        var vehicleGui = new ParentWidget<>().name("vehicle_gui")
                .child(
                        new ParentWidget<>()
                                .size(C_WIDTH - 10, 200)
                                .child(fuelSlots.relativeToParent().align(Alignment.TopRight))
                                .child(grid.relativeToParent().align(Alignment.BottomRight))
                                .child(weaponList.relativeToParent().align(Alignment.BottomLeft))
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
