package com.norwood.mcheli.block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.norwood.mcheli.MCH_IRecipeList;
import com.norwood.mcheli.MCH_ItemRecipe;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.gui.MCH_GuiSliderVertical;
import com.norwood.mcheli.helicopter.MCH_HeliInfoManager;
import com.norwood.mcheli.helper.MCH_Recipes;
import com.norwood.mcheli.networking.packet.PacketDrafttingTableCreate;
import com.norwood.mcheli.plane.MCP_PlaneInfoManager;
import com.norwood.mcheli.ship.MCH_ShipInfoManager;
import com.norwood.mcheli.tank.MCH_TankInfoManager;
import com.norwood.mcheli.vehicle.MCH_VehicleInfoManager;
import com.norwood.mcheli.wrapper.W_GuiButton;
import com.norwood.mcheli.wrapper.W_GuiContainer;
import com.norwood.mcheli.wrapper.W_KeyBinding;
import com.norwood.mcheli.wrapper.W_McClient;
import com.norwood.mcheli.wrapper.modelloader.W_ModelCustom;

public class MCH_DraftingTableGui extends W_GuiContainer {

    private GuiTextField searchField;

    public static final int RECIPE_HELI = 0;
    public static final int RECIPE_PLANE = 1;
    public static final int RECIPE_VEHICLE = 2;
    public static final int RECIPE_TANK = 3;
    public static final int RECIPE_ITEM = 4;
    public static final int BUTTON_HELI = 10;
    public static final int BUTTON_PLANE = 11;
    public static final int BUTTON_VEHICLE = 12;
    public static final int BUTTON_TANK = 13;
    public static final int BUTTON_ITEM = 14;
    public static final int BUTTON_NEXT = 20;
    public static final int BUTTON_PREV = 21;
    public static final int BUTTON_CREATE = 30;
    public static final int BUTTON_SELECT = 40;
    public static final int BUTTON_NEXT_PAGE = 50;
    public static final int BUTTON_PREV_PAGE = 51;
    public static final int SCREEN_MAIN = 0;
    public static final int SCREEN_LIST = 1;
    public static float modelZoom = 1.0F;
    public static float modelRotX = 0.0F;
    public static float modelRotY = 0.0F;
    public static float modelPosX = 0.0F;
    public static float modelPosY = 0.0F;
    private final EntityPlayer thePlayer;
    public MCH_IRecipeList currentList;
    public MCH_CurrentRecipe current;
    public final List<List<GuiButton>> screenButtonList;
    public int screenId = 0;
    private MCH_GuiSliderVertical listSlider;
    private GuiButton buttonCreate;
    private GuiButton buttonNext;
    private GuiButton buttonPrev;
    private GuiButton buttonNextPage;
    private GuiButton buttonPrevPage;
    private int drawFace;
    private int buttonClickWait;

    public MCH_DraftingTableGui(EntityPlayer player, int posX, int posY, int posZ) {
        super(new MCH_DraftingTableGuiContainer(player, posX, posY, posZ));
        this.thePlayer = player;
        this.xSize = 400;
        this.ySize = 240;
        this.screenButtonList = new ArrayList<>();
        this.drawFace = 0;
        this.buttonClickWait = 0;
        MCH_Lib.DbgLog(player.world, "MCH_DraftingTableGui.MCH_DraftingTableGui");
    }

    public static void initModelTransform() {
        modelRotX = 0.0F;
        modelRotY = 0.0F;
        modelPosX = 0.0F;
        modelPosY = 0.0F;
        modelZoom = 1.0F;
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        super.initGui();
        this.buttonList.clear();
        this.screenButtonList.clear();
        this.screenButtonList.add(new ArrayList<>());
        this.screenButtonList.add(new ArrayList<>());
        List<GuiButton> list = this.screenButtonList.get(0);
        GuiButton btnHeli = new GuiButton(10, this.guiLeft + 20, this.guiTop + 20, 90, 20, "Helicopter List");
        GuiButton btnPlane = new GuiButton(11, this.guiLeft + 20, this.guiTop + 40, 90, 20, "Plane List");
        GuiButton btnShip = new GuiButton(15, super.guiLeft + 20, super.guiTop + 120, 90, 20, "Ship List");
        GuiButton btnVehicle = new GuiButton(12, this.guiLeft + 20, this.guiTop + 60, 90, 20, "Vehicle List");
        GuiButton btnTank = new GuiButton(13, this.guiLeft + 20, this.guiTop + 80, 90, 20, "Tank List");
        GuiButton btnItem = new GuiButton(14, this.guiLeft + 20, this.guiTop + 100, 90, 20, "Item List");
        btnHeli.enabled = MCH_HeliInfoManager.getInstance().getRecipeListSize() > 0;
        btnPlane.enabled = MCP_PlaneInfoManager.getInstance().getRecipeListSize() > 0;
        btnVehicle.enabled = MCH_VehicleInfoManager.getInstance().getRecipeListSize() > 0;
        btnTank.enabled = MCH_TankInfoManager.getInstance().getRecipeListSize() > 0;
        btnItem.enabled = MCH_ItemRecipe.getInstance().getRecipeListSize() > 0;
        btnShip.enabled = MCH_ShipInfoManager.getInstance().getRecipeListSize() > 0;
        list.add(btnHeli);
        list.add(btnPlane);
        list.add(btnVehicle);
        list.add(btnTank);
        list.add(btnItem);
        list.add(btnShip);
        this.buttonCreate = new GuiButton(30, this.guiLeft + 120, this.guiTop + 89, 50, 20, "Create");
        this.buttonPrev = new GuiButton(21, this.guiLeft + 120, this.guiTop + 111, 36, 20, "<<");
        this.buttonNext = new GuiButton(20, this.guiLeft + 155, this.guiTop + 111, 35, 20, ">>");
        list.add(this.buttonCreate);
        list.add(this.buttonPrev);
        list.add(this.buttonNext);
        this.buttonPrevPage = new GuiButton(51, this.guiLeft + 210, this.guiTop + 210, 60, 20, "Prev Page");
        this.buttonNextPage = new GuiButton(50, this.guiLeft + 270, this.guiTop + 210, 60, 20, "Next Page");
        list.add(this.buttonPrevPage);
        list.add(this.buttonNextPage);
        list = this.screenButtonList.get(1);
        int y = 0;

        for (int i = 0; y < 3; y++) {
            for (int x = 0; x < 2; i++) {
                int px = this.guiLeft + 30 + x * 140;
                int py = this.guiTop + 40 + y * 70;
                list.add(new GuiButton(40 + i, px, py, 45, 20, "Select"));
                x++;
            }
        }

        this.listSlider = new MCH_GuiSliderVertical(0, this.guiLeft + 360, this.guiTop + 20, 20, 200, "", 0.0F, 0.0F,
                0.0F, 1.0F);
        list.add(this.listSlider);

        for (List<GuiButton> guiButtons : this.screenButtonList) {
            list = guiButtons;

            this.buttonList.addAll(list);
        }

        // search stuff
        int x = this.guiLeft + 10;
        int yy = this.guiTop + 5;
        searchField = new GuiTextField(0, this.fontRenderer, x, yy, 100, 12);
        searchField.setMaxStringLength(50);
        searchField.setEnableBackgroundDrawing(true);
        searchField.setVisible(true);
        searchField.setFocused(false);

        this.switchScreen(0);
        initModelTransform();
        modelRotX = 180.0F;
        modelRotY = 90.0F;

        // default behavior
        if (searchField.getText().trim().isEmpty()) {
            // it's empty, give our default results
            if (MCH_ItemRecipe.getInstance().getRecipeListSize() > 0) {
                this.switchRecipeList(MCH_ItemRecipe.getInstance());
            } else if (MCH_HeliInfoManager.getInstance().getRecipeListSize() > 0) {
                this.switchRecipeList(MCH_HeliInfoManager.getInstance());
            } else if (MCP_PlaneInfoManager.getInstance().getRecipeListSize() > 0) {
                this.switchRecipeList(MCP_PlaneInfoManager.getInstance());
            } else if (MCH_VehicleInfoManager.getInstance().getRecipeListSize() > 0) {
                this.switchRecipeList(MCH_VehicleInfoManager.getInstance());
            } else if (MCH_TankInfoManager.getInstance().getRecipeListSize() > 0) {
                this.switchRecipeList(MCH_TankInfoManager.getInstance());
            } else if (MCH_ShipInfoManager.getInstance().getRecipeListSize() > 0) {
                this.switchRecipeList(MCH_ShipInfoManager.getInstance());
            } else {
                this.switchRecipeList(MCH_ItemRecipe.getInstance());
            }
        }
    }

    public void updateListSliderSize(int listSize) {
        int s = listSize / 2;
        if (listSize % 2 != 0) {
            s++;
        }

        if (s > 3) {
            this.listSlider.valueMax = s - 3;
        } else {
            this.listSlider.valueMax = 0.0F;
        }

        this.listSlider.setSliderValue(0.0F);
    }

    public void switchScreen(int id) {
        this.screenId = id;

        for (GuiButton guiButton : this.buttonList) {
            W_GuiButton.setVisible(guiButton, false);
        }

        if (id < this.screenButtonList.size()) {
            for (GuiButton b : this.screenButtonList.get(id)) {
                W_GuiButton.setVisible(b, true);
            }
        }

        if (this.getScreenId() == 0 && this.current != null && this.current.getDescMaxPage() > 1) {
            W_GuiButton.setVisible(this.buttonNextPage, true);
            W_GuiButton.setVisible(this.buttonPrevPage, true);
        } else {
            W_GuiButton.setVisible(this.buttonNextPage, false);
            W_GuiButton.setVisible(this.buttonPrevPage, false);
        }
    }

    public void setCurrentRecipe(MCH_CurrentRecipe currentRecipe) {
        modelPosX = 0.0F;
        modelPosY = 0.0F;

        if (currentRecipe == null) {
            this.current = null;
            this.drawFace = 0; // Reset drawFace
            return;
        }

        this.current = currentRecipe;

        if (this.current == null || currentRecipe == null ||
                !this.current.recipe.getRecipeOutput().isItemEqual(currentRecipe.recipe.getRecipeOutput())) {
            this.drawFace = 0;
        }

        this.current = currentRecipe;
        if (this.getScreenId() == 0 && this.current != null && this.current.getDescMaxPage() > 1) {
            W_GuiButton.setVisible(this.buttonNextPage, true);
            W_GuiButton.setVisible(this.buttonPrevPage, true);
        } else {
            W_GuiButton.setVisible(this.buttonNextPage, false);
            W_GuiButton.setVisible(this.buttonPrevPage, false);
        }
    }

    public MCH_IRecipeList getCurrentList() {
        return this.currentList;
    }

    public void switchRecipeList(MCH_IRecipeList list) {
        if (this.getCurrentList() != list) {
            this.setCurrentRecipe(new MCH_CurrentRecipe(list, 0));
            this.currentList = list;
            this.updateListSliderSize(list.getRecipeListSize());
        } else {
            this.listSlider.setSliderValue((float) this.current.index / 2F);
        }
    }

    public void updateScreen() {
        super.updateScreen();
        if (this.buttonClickWait > 0) {
            this.buttonClickWait--;
        }
    }

    public void onGuiClosed() {
        super.onGuiClosed();
        MCH_Lib.DbgLog(this.thePlayer.world, "MCH_DraftingTableGui.onGuiClosed");
    }

    protected void actionPerformed(@NotNull GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (this.buttonClickWait <= 0) {
            if (button.enabled) {
                this.buttonClickWait = 3;
                switch (button.id) {
                    case 10 -> {
                        initModelTransform();
                        modelRotX = 180.0F;
                        modelRotY = 90.0F;
                        this.switchRecipeList(MCH_HeliInfoManager.getInstance());
                        this.switchScreen(1);
                    }
                    case 11 -> {
                        initModelTransform();
                        modelRotX = 90.0F;
                        modelRotY = 180.0F;
                        this.switchRecipeList(MCP_PlaneInfoManager.getInstance());
                        this.switchScreen(1);
                    }
                    case 12 -> {
                        initModelTransform();
                        modelRotX = 180.0F;
                        modelRotY = 90.0F;
                        this.switchRecipeList(MCH_VehicleInfoManager.getInstance());
                        this.switchScreen(1);
                    }
                    case 13 -> {
                        initModelTransform();
                        modelRotX = 180.0F;
                        modelRotY = 90.0F;
                        this.switchRecipeList(MCH_TankInfoManager.getInstance());
                        this.switchScreen(1);
                    }
                    case 14 -> {
                        this.switchRecipeList(MCH_ItemRecipe.getInstance());
                        this.switchScreen(1);
                    }

                    case 15 -> {
                        initModelTransform();
                        modelRotX = 90.0F;
                        modelRotY = 180.0F;
                        this.switchRecipeList(MCH_ShipInfoManager.getInstance());
                        this.switchScreen(1);
                    }

                    case 20 -> {
                        int page = this.current.getDescCurrentPage();
                        if (this.current.isCurrentPageTexture()) {
                            page = 0;
                        }
                        int index = (this.current.index + 1) % this.getCurrentList().getRecipeListSize();
                        this.setCurrentRecipe(new MCH_CurrentRecipe(this.getCurrentList(), index));
                        this.current.setDescCurrentPage(page);
                    }
                    case 21 -> {
                        int page = this.current.getDescCurrentPage();
                        if (this.current.isCurrentPageTexture()) {
                            page = 0;
                        }
                        int index = this.current.index - 1;
                        if (index < 0) {
                            index = this.getCurrentList().getRecipeListSize() - 1;
                        }
                        this.setCurrentRecipe(new MCH_CurrentRecipe(this.getCurrentList(), index));
                        this.current.setDescCurrentPage(page);
                    }
                    case 30 -> PacketDrafttingTableCreate.send(this.current.recipe);
                    case 40, 41, 42, 43, 44, 45 -> {
                        int index = (int) this.listSlider.getSliderValue() * 2 + (button.id - 40);
                        if (index < this.getCurrentList().getRecipeListSize()) {
                            this.setCurrentRecipe(new MCH_CurrentRecipe(this.getCurrentList(), index));
                            this.switchScreen(0);
                        }
                    }
                    case 50 -> {
                        if (this.current != null) {
                            this.current.switchNextPage();
                        }
                    }
                    case 51 -> {
                        if (this.current != null) {
                            this.current.switchPrevPage();
                        }
                    }
                    default -> {}
                }
            }
        }
    }

    private boolean listContainsSearch(MCH_IRecipeList list, String searchText) {
        searchText = searchText.toLowerCase();
        for (int i = 0; i < list.getRecipeListSize(); i++) {
            IRecipe r = list.getRecipe(i);
            if (r != null && r.getRecipeOutput() != null &&
                    r.getRecipeOutput().getDisplayName().toLowerCase().contains(searchText)) {
                return true;
            }
        }
        return false;
    }

    public class FilteredRecipeList implements MCH_IRecipeList {

        private final List<IRecipe> filtered;

        public FilteredRecipeList(MCH_IRecipeList base, String searchText) {
            filtered = new ArrayList<>();
            for (int i = 0; i < base.getRecipeListSize(); i++) {
                IRecipe r = base.getRecipe(i);
                if (r != null && r.getRecipeOutput() != null) {
                    String name = r.getRecipeOutput().getDisplayName();
                    if (name != null && name.toLowerCase().contains(searchText.toLowerCase())) {
                        filtered.add(r);
                    }
                }
            }
        }

        @Override
        public int getRecipeListSize() {
            return filtered.size();
        }

        @Override
        public IRecipe getRecipe(int index) {
            return filtered.get(index);
        }
    }

    private void updateEnableCreateButton() {
        MCH_DraftingTableGuiContainer container = (MCH_DraftingTableGuiContainer) this.inventorySlots;
        this.buttonCreate.enabled = false;
        if (!container.getSlot(container.outputSlotIndex).getHasStack()) {
            this.buttonCreate.enabled = MCH_Recipes.canCraft(this.thePlayer, this.current.recipe);
        }

        if (this.thePlayer.capabilities.isCreativeMode) {
            this.buttonCreate.enabled = true;
        }
    }

    protected void keyTyped(char par1, int keycode) throws IOException {
        // search bar shit
        if (searchField.textboxKeyTyped(par1, keycode)) {

            String searchText = searchField.getText().trim();

            if (searchText.isEmpty()) {
                // Search field is empty, reset to default or ignore
                return;
            }

            // attempt 2
            // String searchText = searchField.getText();

            // Try each recipe list to find the first that matches the filter
            if (listContainsSearch(MCH_ItemRecipe.getInstance(), searchText)) {
                this.switchRecipeList(MCH_ItemRecipe.getInstance());
            } else if (listContainsSearch(MCH_HeliInfoManager.getInstance(), searchText)) {
                this.switchRecipeList(MCH_HeliInfoManager.getInstance());
            } else if (listContainsSearch(MCP_PlaneInfoManager.getInstance(), searchText)) {
                this.switchRecipeList(MCP_PlaneInfoManager.getInstance());
            } else if (listContainsSearch(MCH_VehicleInfoManager.getInstance(), searchText)) {
                this.switchRecipeList(MCH_VehicleInfoManager.getInstance());
            } else if (listContainsSearch(MCH_TankInfoManager.getInstance(), searchText)) {
                this.switchRecipeList(MCH_TankInfoManager.getInstance());
            } else if (listContainsSearch(MCH_ShipInfoManager.getInstance(), searchText)) {
                this.switchRecipeList(MCH_ShipInfoManager.getInstance());
            }
            // its shit code but it works

            // Try filtering each manager in order
            for (MCH_IRecipeList baseList : Arrays.asList(
                    MCH_ItemRecipe.getInstance(),
                    MCH_HeliInfoManager.getInstance(),
                    MCP_PlaneInfoManager.getInstance(),
                    MCH_VehicleInfoManager.getInstance(),
                    // MCH_TankInfoManager.getInstance()))
                    MCH_TankInfoManager.getInstance(),
                    MCH_ShipInfoManager.getInstance())) {

                FilteredRecipeList filtered = new FilteredRecipeList(baseList, searchText);
                if (filtered.getRecipeListSize() > 0) {
                    this.switchRecipeList(filtered);
                    break;
                }
            }

            return; // Prevent other key logic when typing in search bar

            // Get the search text
            // String searchText = searchField.getText().toLowerCase();
            //
            //// Filter the recipes
            // List<IRecipe> filteredRecipes = new ArrayList<>();
            // if (originalRecipes != null) { // Ensure originalRecipes is not null
            // for (IRecipe recipe : originalRecipes) {
            // if (recipe != null && recipe.getRecipeOutput() != null) { // Null checks for recipe and its output
            // String displayName = recipe.getRecipeOutput().getDisplayName();
            // if (displayName != null && displayName.toLowerCase().contains(searchText)) {
            // filteredRecipes.add(recipe);
            // }
            // }
            // }
            // }
            // currently our search bar is crashing the game so we're going to comment this stuff out for now.

            // Update the filteredRecipeList
            // filteredRecipeList = new MCH_IRecipeList() {
            // @Override
            // public int getRecipeListSize() {
            // return filteredRecipes.size();
            // }
            //
            // @Override
            // public IRecipe getRecipe(int index) {
            // return filteredRecipes.get(index);
            // }
            // };
            // crashing the game

            // Switch to the filtered list
            // this.switchRecipeList(filteredRecipeList);
            // return; // Prevent default behavior when typing in the search box
        } else if (keycode == 1 ||
                keycode == W_KeyBinding.getKeyCode(Minecraft.getMinecraft().gameSettings.keyBindInventory)) {
                    if (this.getScreenId() == 0) {
                        this.mc.player.closeScreen();
                    } else {
                        this.switchScreen(0);
                    }
                }

        if (this.getScreenId() == 0) {
            if (keycode == 205) {
                this.actionPerformed(this.buttonNext);
            }

            if (keycode == 203) {
                this.actionPerformed(this.buttonPrev);
            }
        } else if (this.getScreenId() == 1) {
            if (keycode == 200) {
                this.listSlider.scrollDown(1.0F);
            }

            if (keycode == 208) {
                this.listSlider.scrollUp(1.0F);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        super.drawGuiContainerForegroundLayer(mx, my);
        this.zLevel = 0.0F;
        GlStateManager.enableBlend();
        if (this.getScreenId() == 0) {
            ArrayList<String> list = new ArrayList<>();
            if (this.current != null) {
                if (this.current.isCurrentPageTexture()) {
                    GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
                    this.mc.getTextureManager().bindTexture(this.current.getCurrentPageTexture());
                    this.drawTexturedModalRect(210, 20, 170, 190, 0, 0, 340, 380);
                } else if (this.current.isCurrentPageAcInfo()) {
                    for (int i = 0; i < this.current.infoItem.size(); i++) {
                        this.fontRenderer.drawString(this.current.infoItem.get(i), 210, 40 + 10 * i, -9491968);
                        String data = this.current.infoData.get(i);
                        if (!data.isEmpty()) {
                            this.fontRenderer.drawString(data, 280, 40 + 10 * i, -9491968);
                        }
                    }
                } else {
                    W_McClient.MOD_bindTexture("textures/gui/drafting_table.png");
                    this.drawTexturedModalRect(340, 215, 45, 15, 400, 60, 90, 30);
                    if (mx >= 350 && mx <= 400 && my >= 214 && my <= 230) {
                        boolean lb = Mouse.isButtonDown(0);
                        boolean rb = Mouse.isButtonDown(1);
                        boolean mb = Mouse.isButtonDown(2);
                        list.add((lb ? TextFormatting.AQUA : "") + "Mouse left button drag : Rotation model");
                        list.add((rb ? TextFormatting.AQUA : "") + "Mouse right button drag : Zoom model");
                        list.add((mb ? TextFormatting.AQUA : "") + "Mouse middle button drag : Move model");
                    }
                }
            }

            this.drawString(this.current.displayName, 120, 20, -1);
            this.drawItemRecipe(this.current.recipe, 121, 34);
            if (!list.isEmpty()) {
                this.drawHoveringText(list, mx - 30, my, this.fontRenderer);
            }
        }

        if (this.getScreenId() == 1) {
            int index = 2 * (int) this.listSlider.getSliderValue();
            int ix = 0;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 2; c++) {
                    if (index + ix < this.getCurrentList().getRecipeListSize()) {
                        int rx = 110 + 140 * c;
                        int ry = 20 + 70 * r;
                        String s = this.getCurrentList().getRecipe(index + ix).getRecipeOutput().getDisplayName();
                        this.drawCenteredString(s, rx, ry, -1);
                    }

                    ix++;
                }
            }

            W_McClient.MOD_bindTexture("textures/gui/drafting_table.png");
            ix = 0;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 2; c++) {
                    if (index + ix < this.getCurrentList().getRecipeListSize()) {
                        int rx = 80 + 140 * c - 1;
                        int ry = 30 + 70 * r - 1;
                        this.drawTexturedModalRect(rx, ry, 400, 0, 75, 54);
                    }

                    ix++;
                }
            }

            ix = 0;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 2; c++) {
                    if (index + ix < this.getCurrentList().getRecipeListSize()) {
                        int rx = 80 + 140 * c;
                        int ry = 30 + 70 * r;
                        this.drawItemRecipe(this.getCurrentList().getRecipe(index + ix), rx, ry);
                    }

                    ix++;
                }
            }
        }
    }

    protected void handleMouseClick(@NotNull Slot slotIn, int slotId, int clickedButton, @NotNull ClickType clickType) {
        if (this.getScreenId() != 1) {
            super.handleMouseClick(slotIn, slotId, clickedButton, clickType);
        }
    }

    private int getScreenId() {
        return this.screenId;
    }

    public void drawItemRecipe(IRecipe recipe, int x, int y) {
        if (recipe != null) {
            recipe.getRecipeOutput();
            recipe.getRecipeOutput().getItem();
            RenderHelper.enableGUIStandardItemLighting();
            NonNullList<Ingredient> ingredients = recipe.getIngredients();

            for (int i = 0; i < ingredients.size(); i++) {
                this.drawIngredient(ingredients.get(i), x + i % 3 * 18, y + i / 3 * 18);
            }

            this.drawItemStack(recipe.getRecipeOutput(), x + 54 + 3, y + 18);
            RenderHelper.disableStandardItemLighting();
        }
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dx = Mouse.getEventDX();
        int dy = Mouse.getEventDY();
        if (this.getScreenId() == 0 && Mouse.getX() > this.mc.displayWidth / 2) {
            if (Mouse.isButtonDown(0) && (dx != 0 || dy != 0)) {
                modelRotX = (float) (modelRotX - dy / 2.0);
                modelRotY = (float) (modelRotY - dx / 2.0);
                if (modelRotX > 360.0F) {
                    modelRotX -= 360.0F;
                }

                if (modelRotX < -360.0F) {
                    modelRotX += 360.0F;
                }

                if (modelRotY > 360.0F) {
                    modelRotY -= 360.0F;
                }

                if (modelRotY < -360.0F) {
                    modelRotY += 360.0F;
                }
            }

            if (Mouse.isButtonDown(2) && (dx != 0 || dy != 0)) {
                modelPosX = (float) (modelPosX + dx / 2.0);
                modelPosY = (float) (modelPosY - dy / 2.0);
                if (modelRotX > 1000.0F) {
                    modelRotX = 1000.0F;
                }

                if (modelRotX < -1000.0F) {
                    modelRotX = -1000.0F;
                }

                if (modelRotY > 1000.0F) {
                    modelRotY = 1000.0F;
                }

                if (modelRotY < -1000.0F) {
                    modelRotY = -1000.0F;
                }
            }

            if (Mouse.isButtonDown(1) && dy != 0) {
                modelZoom = (float) (modelZoom + dy / 100.0);
                if (modelZoom < 0.1) {
                    modelZoom = 0.1F;
                }

                if (modelZoom > 10.0F) {
                    modelZoom = 10.0F;
                }
            }
        }

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (this.getScreenId() == 1) {
                if (wheel > 0) {
                    this.listSlider.scrollDown(1.0F);
                } else {
                    this.listSlider.scrollUp(1.0F);
                }
            } else if (this.getScreenId() == 0) {
                if (wheel > 0) {
                    this.actionPerformed(this.buttonPrev);
                } else {
                    this.actionPerformed(this.buttonNext);
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.getScreenId() == 0) {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } else {
            List<Slot> inventory = this.inventorySlots.inventorySlots;
            this.inventorySlots.inventorySlots = new ArrayList<>();
            super.drawScreen(mouseX, mouseY, partialTicks);
            this.inventorySlots.inventorySlots = inventory;
        }

        if (this.getScreenId() == 0 && this.current.isCurrentPageModel()) {
            RenderHelper.enableGUIStandardItemLighting();
            this.drawModel(partialTicks);
        }

        searchField.drawTextBox();
    }

    public void drawModel(float partialTicks) {
        W_ModelCustom model = this.current.getModel();
        double scl = 162.0 / (MathHelper.abs(model.size) < 0.01 ? 0.01 : model.size);
        this.mc.getTextureManager().bindTexture(this.current.getModelTexture());
        GlStateManager.pushMatrix();
        double cx = (model.maxX - model.minX) * 0.5 + model.minX;
        double cy = (model.maxY - model.minY) * 0.5 + model.minY;
        double cz = (model.maxZ - model.minZ) * 0.5 + model.minZ;
        if (this.current.modelRot == 0) {
            GlStateManager.translate(cx * scl, cz * scl, 0.0);
        } else {
            GlStateManager.translate(cz * scl, cy * scl, 0.0);
        }

        GlStateManager.translate(this.guiLeft + 300 + modelPosX, this.guiTop + 110 + modelPosY, 550.0);
        GL11.glRotated(modelRotX, 1.0, 0.0, 0.0);
        GL11.glRotated(modelRotY, 0.0, 1.0, 0.0);
        GlStateManager.scale(scl * modelZoom, scl * modelZoom, -scl * modelZoom);
        GlStateManager.disableRescaleNormal();;
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        int faceNum = model.getFaceNum();
        if (this.drawFace < faceNum * 2) {
            GL11.glColor4d(0.1F, 0.1F, 0.1F, 1.0);
            GlStateManager.disableTexture2D();
            GL11.glPolygonMode(1032, 6913);
            float lw = GL11.glGetFloat(2849);
            GL11.glLineWidth(1.0F);
            model.renderAll(this.drawFace - faceNum, this.drawFace);
            MCH_RenderAircraft.renderCrawlerTrack(null, this.current.getAcInfo(), partialTicks);
            GL11.glLineWidth(lw);
            GL11.glPolygonMode(1032, 6914);
            GlStateManager.enableTexture2D();
        }

        if (this.drawFace >= faceNum) {
            GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
            model.renderAll(0, this.drawFace - faceNum);
            MCH_RenderAircraft.renderCrawlerTrack(null, this.current.getAcInfo(), partialTicks);
        }

        GlStateManager.enableRescaleNormal();;
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
        if (this.drawFace < 10000000) {
            this.drawFace = (int) (this.drawFace + 20.0F);
        }
    }

    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float z = this.zLevel;
        this.zLevel = 0.0F;
        W_McClient.MOD_bindTexture("textures/gui/drafting_table.png");
        if (this.getScreenId() == 0) {
            this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        }

        if (this.getScreenId() == 1) {
            this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, this.ySize, this.xSize, this.ySize);
            List<GuiButton> list = this.screenButtonList.get(1);
            int index = (int) this.listSlider.getSliderValue() * 2;

            for (int i = 0; i < 6; i++) {
                W_GuiButton.setVisible(list.get(i), index + i < this.getCurrentList().getRecipeListSize());
            }
        }

        this.zLevel = z;
    }

    public void drawTexturedModalRect(int par1, int par2, int par3, int par4, int par5, int par6) {
        float w = 0.001953125F;
        float h = 0.001953125F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(par1, par2 + par6, this.zLevel).tex((par3) * w, (par4 + par6) * h).endVertex();
        buffer.pos(par1 + par5, par2 + par6, this.zLevel).tex((par3 + par5) * w, (par4 + par6) * h).endVertex();
        buffer.pos(par1 + par5, par2, this.zLevel).tex((par3 + par5) * w, (par4) * h).endVertex();
        buffer.pos(par1, par2, this.zLevel).tex((par3) * w, (par4) * h).endVertex();
        tessellator.draw();
    }

    public void drawTexturedModalRect(int dx, int dy, int dw, int dh, int u, int v, int tw, int th) {
        float w = 0.001953125F;
        float h = 0.001953125F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(dx, dy + dh, this.zLevel).tex((u) * w, (v + th) * h).endVertex();
        buffer.pos(dx + dw, dy + dh, this.zLevel).tex((u + tw) * w, (v + th) * h).endVertex();
        buffer.pos(dx + dw, dy, this.zLevel).tex((u + tw) * w, (v) * h).endVertex();
        buffer.pos(dx, dy, this.zLevel).tex((u) * w, (v) * h).endVertex();
        tessellator.draw();
    }
}
