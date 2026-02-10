package com.norwood.mcheli.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.norwood.mcheli.wrapper.W_EntityContainer;

public class ContainerGui {

    public static ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings, W_EntityContainer container) {
        syncManager.bindPlayerInventory(data.getPlayer());
        var containerGuiHandler = container.getInventory();

        final int maxColumns = 12;
        final int slotSize = 18;
        final int minRows = 1;
        final int maxRows = 5;

        int totalSlots = containerGuiHandler.getSlots();

        int displayColumns = Math.max(1, Math.min(totalSlots, maxColumns));

        int rowCount = (int) Math.ceil((double) totalSlots / displayColumns);
        int displayRows = Math.max(minRows, Math.min(rowCount, maxRows));

        int gridWidth = displayColumns * slotSize;
        int gridHeight = displayRows * slotSize;

        var grid = new Column().name("trunk")
                .margin(1)
                .padding(1)
                .child(IKey.str("Storage (%d)", totalSlots).asWidget().alignX(Alignment.BottomLeft))
                .child(new Grid()
                        .margin(1)
                        .mapTo(displayColumns, totalSlots, (slot) ->
                                new ItemSlot()
                                        .slot(new ModularSlot(containerGuiHandler, slot))
                                        .size(slotSize)
                        )
                        .size(gridWidth + 4, gridHeight)
                        .scrollable(new VerticalScrollData())
                ).coverChildren().background(GuiTextures.MENU_BACKGROUND).margin(1);

        var inventory = new Row().invisible().child(
                        new ParentWidget<>().name("inventory_wrapper")
                                .child(
                                        SlotGroupWidget.playerInventory(false)
                                )
                                .coverChildren()
                                .padding(5)
                                .marginBottom(10)
                                .background(GuiTextures.MC_BACKGROUND)
                )
                .coverChildren();

        int panelWidth = Math.max(gridWidth + 30, 180);

        return new ModularPanel("container")
                .size(panelWidth, gridHeight + 125)
                .invisible()
                .child(
                        new Column()
                                .bottom(0)
                                .alignX(Alignment.Center)
                                .child(IKey.str(container.getName()).asWidget().padding(4, 2).background(GuiTextures.MC_BACKGROUND))
                                .child(grid)
                                .child(inventory)
                );
    }
}
