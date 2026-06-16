package com.norwood.mcheli.gui;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.norwood.mcheli.factories.UavStationGuiData;
import com.norwood.mcheli.factories.UavStationGuiData.UavEntry;
import com.norwood.mcheli.networking.packet.PacketUavConnect;
import com.norwood.mcheli.networking.packet.PacketUavDrop;
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

        // Client-side selection holder (mutated by per-row select buttons).
        final UavEntry[] selected = {data.getEntries().isEmpty() ? null : data.getEntries().get(0)};

        var list = new ListWidget<>()
                .children(data.getEntries(), entry -> {
                    var row = new Row().height(20).marginBottom(2).padding(2)
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
                            .style(TextFormatting.GREEN).asWidget().padding(2));
                    row.child(IKey.str("●")
                            .style(entry.reachable() ? TextFormatting.GREEN : TextFormatting.RED)
                            .asWidget().padding(2)
                            .tooltip(t -> t.addLine(entry.reachable() ? "In range" : "Out of range / offline")));
                    return row;
                })
                .scrollDirection(GuiAxis.Y)
                .size(230, 130);

        var viewport = new WidgetUavCameraFeed(() -> selected[0])
                .size(110, 90)
                .background(GuiTextures.SLOT_ITEM);

        var connectBtn = new ButtonWidget<>()
                .onMouseTapped(_ -> {
                    if (selected[0] != null) {
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

        var right = new Column().coverChildren()
                .child(viewport)
                .child(new Row().coverChildren().marginTop(2)
                        .child(connectBtn)
                        .child(dropBtn.marginLeft(2)));

        var body = new Row().coverChildren().padding(2)
                .child(list)
                .child(right.marginLeft(4));

        // Legacy item-slot spawn-and-bind is kept alongside the pairing flow.
        var legacy = new Row().coverChildren().marginTop(4)
                .child(new ItemSlot().slot(new ModularSlot(new InvWrapper(station), 0)).background(GuiTextures.SLOT_ITEM))
                .child(IKey.str("Deploy a UAV item to spawn & bind it").asWidget().padding(4).alignY(Alignment.Center));

        var inventory = new Row().invisible().marginTop(4)
                .child(new ParentWidget<>().name("inventory_wrapper")
                        .child(SlotGroupWidget.playerInventory(false))
                        .coverChildren().padding(5).background(GuiTextures.MC_BACKGROUND));

        var root = new Column().coverChildren().padding(5).background(GuiTextures.MC_BACKGROUND)
                .child(IKey.str("UAV Station").asWidget().padding(2))
                .child(body)
                .child(legacy)
                .child(inventory);

        return new ModularPanel("uav_station").coverChildren().child(root);
    }

    private static ItemStack stackFor(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
