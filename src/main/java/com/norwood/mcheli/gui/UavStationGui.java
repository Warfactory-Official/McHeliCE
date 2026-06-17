package com.norwood.mcheli.gui;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.norwood.mcheli.factories.UavStationGuiData;
import com.norwood.mcheli.factories.UavStationGuiData.UavEntry;
import com.norwood.mcheli.networking.packet.PacketUavConnect;
import com.norwood.mcheli.networking.packet.PacketUavDrop;
import com.norwood.mcheli.networking.packet.PacketUavStatus;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import com.norwood.mcheli.uav.WidgetUavCameraFeed;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.wrapper.InvWrapper;

public class UavStationGui {

    private UavStationGui() {
    }

    public static ModularPanel buildUI(UavStationGuiData data, PanelSyncManager syncManager, UISettings settings,
                                       MCH_EntityUavStation station) {
        syncManager.bindPlayerInventory(data.getPlayer());

        final UavEntry[] selected = {data.getEntries().isEmpty() ? null : data.getEntries().get(0)};

        var list = new ListWidget<>()
                .children(data.getEntries(), entry -> {
                    var row = Flow.row().height(20).marginBottom(2).padding(2)
                            .background(GuiTextures.MENU_BACKGROUND);
                    row.child(new ButtonWidget<>()
                            .onMouseTapped(_ -> {
                                selected[0] = entry;
                                return true;
                            })
                            .overlay(IKey.str("▶"))
                            .size(12).marginRight(2));

                    ItemStack icon = stackFor(entry.itemName());
                    if (!icon.isEmpty()) {
                        row.child(new IngredientDrawable(icon).asWidget().size(16).marginRight(2));
                    }
                    row.child(new ScrollingTextWidget(IKey.str(entry.displayName()))
                            .widthRel(0.32f).height(8).padding(2, 0));
                    row.child(IKey.str("(%d, %d, %d)", entry.x(), entry.y(), entry.z()).asWidget().padding(2));
                    row.child(IKey.str(entry.loaded() ? entry.hp() + "/" + entry.maxHp() : "?")
                            .style(entry.destroyed() ? TextFormatting.DARK_RED : TextFormatting.GREEN)
                            .asWidget().padding(2));
                    if (entry.destroyed()) {
                        row.child(IKey.str("✖ DESTROYED")
                                .style(TextFormatting.DARK_RED).asWidget().padding(2)
                                .tooltip(t -> t.addLine("Destroyed — cannot connect")));
                    } else {
                        row.child(IKey.str("●")
                                .style(entry.reachable() ? TextFormatting.GREEN : TextFormatting.RED)
                                .asWidget().padding(2)
                                .tooltip(t -> t.addLine(entry.reachable() ? "In range" : "Out of range / offline")));
                    }
                    return row;
                })
                .scrollDirection(GuiAxis.Y)
                .size(230, 130);

        var viewport = new WidgetUavCameraFeed(() -> selected[0])
                .size(110, 90)
                .background(GuiTextures.SLOT_ITEM);

        var connectBtn = new ButtonWidget<>()
                .onMouseTapped(_ -> {
                    if (selected[0] != null && !selected[0].destroyed()) {
                        new PacketUavConnect(station.getEntityId(), selected[0].id()).sendToServer();
                    }
                    return true;
                })
                .overlay(IKey.str("Connect"))
                .background(GuiTextures.MC_BUTTON)
                .size(54, 18).marginTop(2);

        var dropBtn = new ButtonWidget<>()
                .onMouseTapped(_ -> {
                    if (selected[0] != null) {
                        new PacketUavDrop(station.getEntityId(), selected[0].id()).sendToServer();
                    }
                    return true;
                })
                .overlay(IKey.str("Drop"))
                .background(GuiTextures.MC_BUTTON)
                .size(54, 18).marginTop(2);

        // Deploy-offset adjust pad (moves where a UAV is spawned/placed relative to the station).
        var posControls = Flow.column().coverChildren().marginTop(2)
                .child(IKey.str("Deploy offset").asWidget().padding(2))
                .child(axisRow(station, "X", 1, 0, 0))
                .child(axisRow(station, "Y", 0, 1, 0))
                .child(axisRow(station, "Z", 0, 0, 1));

        var right = Flow.column().coverChildren()
                .child(viewport)
                .child(Flow.row().coverChildren().marginTop(2)
                        .child(connectBtn)
                        .child(dropBtn.marginLeft(2)))
                .child(posControls);

        var body = Flow.row().coverChildren().padding(2)
                .child(list)
                .child(right.marginLeft(4));

        // Legacy item-slot spawn-and-bind is kept alongside the pairing flow.
        var legacy = Flow.row().coverChildren().marginTop(4)
                .child(new ItemSlot().slot(new ModularSlot(new InvWrapper(station), 0)).background(GuiTextures.SLOT_ITEM))
                .child(IKey.str("Deploy a UAV item to spawn & bind it").asWidget().padding(4).topRel(0.5f).anchorTop(0.5f));

        var inventory = Flow.row().invisible().marginTop(4)
                .child(new ParentWidget<>().name("inventory_wrapper")
                        .child(SlotGroupWidget.playerInventory(false))
                        .coverChildren().padding(5).background(GuiTextures.MC_BACKGROUND));

        var root = Flow.column().coverChildren().padding(5).background(GuiTextures.MC_BACKGROUND)
                .child(IKey.str("UAV Station").asWidget().padding(2))
                .child(body)
                .child(legacy)
                .child(inventory);

        return new ModularPanel("uav_station").coverChildren().child(root);
    }

    private static Flow axisRow(
            MCH_EntityUavStation station, String label, int ux, int uy, int uz) {
        var row = Flow.row().coverChildren().marginTop(1);
        row.child(IKey.str(label).asWidget().size(12, 18).topRel(0.5f).anchorTop(0.5f).padding(2));
        row.child(new ButtonWidget<>()
                .onMouseTapped(_ -> {
                    sendOffset(station, -ux, -uy, -uz);
                    return true;
                })
                .overlay(IKey.str("−")).background(GuiTextures.MC_BUTTON).size(18, 18));
        row.child(new ButtonWidget<>()
                .onMouseTapped(_ -> {
                    sendOffset(station, ux, uy, uz);
                    return true;
                })
                .overlay(IKey.str("+")).background(GuiTextures.MC_BUTTON).size(18, 18).marginLeft(2));
        return row;
    }

    private static void sendOffset(MCH_EntityUavStation station, int dx, int dy, int dz) {
        PacketUavStatus pkt = new PacketUavStatus();
        pkt.posUavX = (byte) Math.clamp(station.offsetX + dx, -127, 127);
        pkt.posUavY = (byte) Math.clamp(station.offsetY + dy, -127, 127);
        pkt.posUavZ = (byte) Math.clamp(station.offsetZ + dz, -127, 127);
        pkt.sendToServer();
    }

    private static ItemStack stackFor(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
