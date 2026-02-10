package com.norwood.mcheli.aircraft;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
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
import com.norwood.mcheli.parachute.MCH_ItemParachute;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class AircraftGui {
    public static final int C_WIDTH = 410;
    public static final int C_HEIGHT = 300;
    public static final int SLOT_SIZE = 18;

    public static final ResourceLocation WEAPON_ICONS = new ResourceLocation(Tags.MODID, "gui/wepon_icons");
    public static final UITexture PARACHUTE_SLOT =
            UITexture.builder()
                    .location(new ResourceLocation(Tags.MODID, "gui/parachute_slot"))
                    .imageSize(18, 18)
                    .name("parachute_slot")
                    .build();

    public static final UITexture FUEL_SLOT =
            UITexture.builder()
                    .location(new ResourceLocation(Tags.MODID, "gui/fuel_slot"))
                    .imageSize(18, 18)
                    .name("fuel_slot")
                    .build();
    public static final UITexture FUEL =
            UITexture.builder()
                    .location(new ResourceLocation(Tags.MODID, "gui/fuel_"))
                    .imageSize(32, 32)
                    .name("fuel")
                    .build();

    public static final UITexture APFSDS_SHELL = createWeaponIcon("apfsds", 0, 0);
    public static final UITexture HE_SHELL = createWeaponIcon("he_shell", 18, 0);
    public static final UITexture HEAT_SHELL = createWeaponIcon("heat_shell", 36, 0);
    public static final UITexture CANISTER_SHELL = createWeaponIcon("canister", 54, 0);

    public static final UITexture LARGE_CALIBER_MG = createWeaponIcon("large_mg", 0, 18);
    public static final UITexture MORTAR_SHOT = createWeaponIcon("mortar", 18, 18);

    public static final UITexture BOMB = createWeaponIcon("bomb", 0, 36);
    public static final UITexture CLUSTER_BOMB = createWeaponIcon("cluster_bomb", 18, 36);
    public static final UITexture GATLING_BULLET = createWeaponIcon("gatling_bullet", 36, 36);
    public static final UITexture AAM_ROCKET = createWeaponIcon("aam_rocket", 54, 36);
    public static final UITexture TV_ROCKET = createWeaponIcon("tv_rocket", 72, 36);
    public static final UITexture SAM_ROCKET = createWeaponIcon("sam_rocket", 90, 36);
    public static final UITexture NUC_WARHEAD = createWeaponIcon("nuc_warhead", 108, 36);
    public static final UITexture MG_7_62MM = createWeaponIcon("mg_7_62", 126, 36);
    public static final UITexture MG_12_7MM = createWeaponIcon("mg_12_7", 144, 36);
    public static Map<String, UITexture> name2WSIcon = new HashMap<>();
    static UITexture RELOAD_LIGHT = UITexture.builder()
            .location(Tags.MODID, "gui/reload_light")
            .imageSize(18, 18)
            .name("rel_light")
            .defaultColorType()
            .build();
    static UITexture RELOAD_DARK = UITexture.builder()
            .location(Tags.MODID, "gui/reload_dark")
            .imageSize(18, 18)
            .name("rel_light")
            .defaultColorType()
            .build();

    private static UITexture createWeaponIcon(String name, int x, int y) {
        return UITexture.builder()
                .location(WEAPON_ICONS)
                .imageSize(160, 53)
                .subAreaXYWH(x, y, 16, 16)
                .name(name)
                .build();
    }

    @Nullable
    private static UITexture determineIcon(@NotNull String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        // Shells
        if (lowerCase.contains("apfsds")) return APFSDS_SHELL;
        if (lowerCase.contains("heat")) return HEAT_SHELL;
        if ((name.contains("HE") && lowerCase.contains("cannon")) || lowerCase.contains("high explosive") || lowerCase.contains("hesh"))
            return HE_SHELL;
        if (lowerCase.contains("canister")) return CANISTER_SHELL;

        // Machine Guns
        if (lowerCase.contains("machine gun") || lowerCase.contains("browning") || lowerCase.contains("mg")) {
            if (lowerCase.contains("12.7mm")) return MG_12_7MM;
            if (lowerCase.contains("7.62mm")) return MG_7_62MM;
            if (lowerCase.contains("gatling")) return GATLING_BULLET;
            return LARGE_CALIBER_MG;
        }

        // Rockets / Missiles
        if (lowerCase.contains("aam")) return AAM_ROCKET;
        if (lowerCase.contains("sam")) return SAM_ROCKET;
        if (name.contains("TV")) return TV_ROCKET;

        // Bombs & Heavy Ordnance
        if (lowerCase.contains("nuc") || lowerCase.contains("nuclear")) return NUC_WARHEAD;
        if (lowerCase.contains("cluster")) return CLUSTER_BOMB;
        if (lowerCase.contains("bomb")) return BOMB;

        // Other
        if (lowerCase.contains("mortar")) return MORTAR_SHOT;

        return null;
    }

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
        boolean showParachuteSlot = data.getInfo().isEnableParachuting || data.getInfo().isEnableEjectionSeat;


        var slotRow = new Row()
                .size(SLOT_SIZE * 2 + 20, SLOT_SIZE)
                .margin(5)
                .child(new ItemSlot()
                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_FUEL_IN)
                                .filter(fuelPredicate(aircraft.getAcInfo()))).background(FUEL_SLOT))
                .child(new ButtonWidget<>().onMouseTapped(_ -> {
                                    syncManager.callSyncedAction("dumpFuel");
                                    return true;
                                }).size(18)
                                .overlay(FUEL.asIcon().size(16))
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
                ).coverChildren().margin(2).setEnabledIf(_ -> data.getInfo().inventorySize > 0);


        var fuelSlots = new Column()
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

        List<WeaponEntry> weaponEntries = IntStream.range(0, aircraft.getWeapons().length)
                .mapToObj(i -> new WeaponEntry(i, aircraft.getWeapon(i)))
                .filter(entry -> entry.set().getMagSize() > 0)
                .toList();
        var weaponList = new ListWidget<>().children(
                        weaponEntries,
                        entry -> {
                            MCH_WeaponSet ws = entry.set();
                            int weaponId = entry.id();
                            var row = new Row();
                            if (name2WSIcon.computeIfAbsent(ws.getDisplayName(), AircraftGui::determineIcon) != null)
                                row.child(name2WSIcon.get(ws.getDisplayName()).asWidget());

                            row.child(new ScrollingTextWidget(IKey.str(ws.getDisplayName()))
                                            .tooltip(t -> t.addLine(ws.getDisplayName()))
                                            .padding(4)
                                            .height(8)
                                            .widthRel(0.30f))
                                    .child(IKey.dynamic(() -> String.format("%d/%d", ws.getAmmo(), ws.getMagSize())).asWidget().padding(4))
                                    .child(new ListWidget<>().children(
                                            ws.getInfo().roundItems, round -> new Row()
                                                    .child(IKey.str("%dx", round.num).asWidget())
                                                    .child(new IngredientDrawable(round.itemStack).asWidget().size(12).tooltip(tooltip -> tooltip.addLine(round.itemStack.getDisplayName())))
                                                    .coverChildrenWidth().padding(4).tooltip(tooltip -> tooltip.addLine(round.itemStack.getDisplayName()))
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
                                            .overlay(canResupply(ws, aircraft, data.getPlayer()) ? RELOAD_LIGHT.asIcon().size(16) : RELOAD_DARK.asIcon().size(16))
                                            .hoverBackground(canResupply(ws, aircraft, data.getPlayer()) ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON_DISABLED)
                                            .tooltip(tooltip -> {
                                                tooltip.addLine("Reload");
                                                List<ItemStack> missing = aircraft.getMissingAmmo(data.getPlayer(), ws);
                                                if (!missing.isEmpty()) {
                                                    tooltip.addLine(TextFormatting.RED + "Missing Ammo:");
                                                    for (ItemStack itemStack : missing) {
                                                        tooltip.addLine(TextFormatting.GRAY + " - " + itemStack.getCount() + "x " + itemStack.getDisplayName());
                                                    }
                                                }
                                            })
                                            .size(18).right(2).top(1))

                                    .height(20).marginTop(4).padding(4).background(GuiTextures.MENU_BACKGROUND);
                            return row;
                        }
                ).scrollDirection(GuiAxis.Y)
                .size(300, 110)
                .setEnabledIf(_ -> !weaponEntries.isEmpty());


        var viewport = new Column().name("viewport")
                .size(110, showParachuteSlot ? 80 : 62)
                .child(new WidgetAircraftViewport((ModelVBO) data.getInfo().model, getTexturePath(aircraft), aircraft.getAcInfo())
                        .size(110, 50)
                        .background(GuiTextures.SLOT_ITEM.withColorOverride(0xFF000000))
                )
                .child(new Row()
                        .child(IKey.dynamic(() -> String.format("%.0f m/s", aircraft.getCurrentSpeed()))
                                .style(TextFormatting.WHITE).asWidget()
                                .margin(2)
                        )
                        .child(IKey.dynamic(() -> String.format("HP: %d/%d", aircraft.getHP(), aircraft.getMaxHP()))
                                .style(TextFormatting.GREEN).asWidget()
                                .margin(2)
                        )
                        .padding(2)
                        .height(10)
                        .background(GuiTextures.SLOT_ITEM.withColorOverride(0xFF000000))
                )
                .child(
                        new Row()
                                .child(new ItemSlot()
                                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_PARACHUTE0)
                                                .filter(stack -> stack.getItem() instanceof MCH_ItemParachute))
                                        .background(PARACHUTE_SLOT)
                                )
                                .child(new ItemSlot()
                                        .slot(new ModularSlot(aircraftGuiInv, MCH_AircraftInventory.SLOT_PARACHUTE1)
                                                .filter(stack -> stack.getItem() instanceof MCH_ItemParachute))
                                        .background(PARACHUTE_SLOT)
                                )
                                .setEnabledIf(_ -> showParachuteSlot)
                                .size(SLOT_SIZE * 2, SLOT_SIZE)
                )
                .background(GuiTextures.MENU_BACKGROUND);

        var gui = new Column()
                .size(C_WIDTH - 10, 200)
                .child(new Row()
                        .child(fuelSlots)
                        .child(viewport)
                        .child(grid)
                        .coverChildren()
                )
                .child(weaponList);


        var vehicleGui = new ParentWidget<>().name("vehicle_gui")
                .child(
                        gui
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
                                .child(IKey.str(aircraft.getName()).asWidget().padding(4, 2).background(GuiTextures.MC_BACKGROUND))
                                .child(vehicleGui)
                                .child(inventory)
                );
    }

    public static ResourceLocation getTexturePath(MCH_EntityAircraft ac) {

        String path = switch (ac) {
            case MCH_EntityHeli heli -> "helicopters/" + heli.getTextureName();
            case MCH_EntityPlane plane -> "planes/" + plane.getTextureName();
            case MCH_EntityVehicle vehicle -> "vehicles/" + vehicle.getTextureName();
            case MCH_EntityShip ship -> "ships/" + ship.getTextureName();
            case MCH_EntityTank tank -> "tanks/" + tank.getTextureName();
            default -> throw new IllegalStateException("Unexpected value: " + ac);
        };
        return new ResourceLocation(Tags.MODID, "textures/" + path + ".png");


    }
}
