package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
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
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.factories.AircraftGuiData;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.networking.packet.PacketOpenScreen;
import com.norwood.mcheli.networking.packet.PacketRequestResupply;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.modelloader.ModelVBO;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

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

    public static boolean canResupply(MCH_WeaponSet ws, MCH_EntityAircraft aircraft, EntityPlayer player) {
        return aircraft.canPlayerSupplyAmmo(player, ws) && ws.getAmmo() < ws.getMagSize();
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
                        .size(18 * 8 + 5, 18 * 4)
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


        record WeaponEntry(int id, MCH_WeaponSet set) {
        }

        var weaponList = new ListWidget<>().children(
                IntStream.range(0, aircraft.getWeapons().length)
                        .mapToObj(i -> new WeaponEntry(i, aircraft.getWeapon(i)))
                        .filter(entry -> entry.set().getMagSize() > 0)
                        .toList(),
                entry -> {
                    MCH_WeaponSet ws = entry.set();
                    int weaponId = entry.id();
                    return new Row()
                            .child(new ScrollingTextWidget(IKey.str(ws.getDisplayName()))
                                    .tooltip(t -> t.addLine(ws.getDisplayName()))
                                    .padding(4)
                                    .widthRel(0.30f))
                            .child(IKey.dynamic(() -> String.format("%d/%d", ws.getAmmo(), ws.getMagSize())).asWidget().padding(4))
                            .child(new ListWidget<>().children(
                                    ws.getInfo().roundItems, round -> new Row()
                                            .child(IKey.str("%dx", round.num).asWidget())
                                            .child(new IngredientDrawable(round.itemStack).asWidget().size(12))
                                            .coverChildrenWidth().padding(4)
                            ).scrollDirection(GuiAxis.X).maxSizeRel(0.35f))
                            .child(IKey.dynamic(() -> String.format("(%d)", ws.getAmmoReserve())).asWidget().padding(4))
                            .child(new ButtonWidget<>().onMouseTapped(_ -> {
                                        if (canResupply(ws, aircraft, data.getPlayer())) {
                                            PacketRequestResupply.send(aircraft, weaponId);
                                            aircraft.supplyAmmo(ws);
                                            return true;
                                        } else return false;
                                    })
                                    .background(canResupply(ws, aircraft, data.getPlayer()) ? GuiTextures.MC_BUTTON : GuiTextures.MC_BUTTON_DISABLED)
                                    .hoverBackground(canResupply(ws, aircraft, data.getPlayer()) ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON_DISABLED)
                                    .size(18).right(2).top(1))

                            .height(20).marginTop(4).padding(4).background(GuiTextures.MENU_BACKGROUND);
                }
        ).scrollDirection(GuiAxis.Y).size(300, 110);


        var vehicleGui = new ParentWidget<>().name("vehicle_gui")
                .child(
                        new ParentWidget<>()
                                .size(C_WIDTH - 10, 200)
                                .child(fuelSlots.relativeToParent().align(Alignment.TopRight))
                                .child(grid.relativeToParent().align(Alignment.BottomLeft))
                                .child(weaponList.relativeToParent().align(Alignment.TopLeft))
                                .child(new WidgetAircraftViewport((ModelVBO) data.getInfo().model, getTexturePath(aircraft), aircraft.getAcInfo()).size(100).align(Alignment.BottomRight))
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

    public static ResourceLocation getTexturePath(MCH_EntityAircraft ac){

       String path = switch (ac) {
           case MCH_EntityHeli heli -> "helicopters/" + heli.getTextureName();
           case MCH_EntityPlane plane -> "planes/" + plane.getTextureName();
           case MCH_EntityVehicle vehicle -> "vehicles/" + vehicle.getTextureName();
           case MCH_EntityShip ship -> "ships/" + ship.getTextureName();
           case MCH_EntityTank tank -> "tanks/" + tank.getTextureName();
           default -> throw new IllegalStateException("Unexpected value: " + ac);
       };
       return new ResourceLocation(Tags.MODID, "textures/"+path+".png");


    }
}
