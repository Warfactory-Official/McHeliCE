package com.norwood.mcheli.block;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import com.norwood.mcheli.MCH_IRecipeList;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_AircraftInfoManager;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.plane.MCP_PlaneInfoManager;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.ship.MCH_ShipInfoManager;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfoManager;
import com.norwood.mcheli.wrapper.modelloader.W_ModelCustom;

public class MCH_CurrentRecipe {

    public final IRecipe recipe;
    public final int index;
    public final String displayName;
    public final List<ResourceLocation> descTexture;
    private final MCH_AircraftInfo acInfo;
    public List<String> infoItem;
    public List<String> infoData;
    public int modelRot;
    private int descMaxPage;
    private int descPage;
    private W_ModelCustom model;
    private ResourceLocation modelTexture;

    public MCH_CurrentRecipe(MCH_IRecipeList list, int idx) {
        if (list.getRecipeListSize() > 0) {
            this.recipe = list.getRecipe(idx);
        } else {
            this.recipe = null;
        }

        this.index = idx;
        this.displayName = this.recipe != null ? this.recipe.getRecipeOutput().getDisplayName() : "None";
        this.descTexture = this.getDescTexture(this.recipe);
        this.descPage = 0;
        this.descMaxPage = this.descTexture.size();
        MCH_AircraftInfo info = null;
        if (list instanceof MCH_AircraftInfoManager) {
            info = ((MCH_AircraftInfoManager<?>) list).getAcInfoFromItem(this.recipe);
            if (info != null) {
                this.descMaxPage++;
                String dir = info.getDirectoryName();
                String name = info.name;
                this.model = MCH_ModelManager.get(dir, name);
                if (this.model != null) {
                    this.modelTexture = new ResourceLocation(Tags.MODID, "textures/" + dir + "/" + name + ".png");
                    this.descMaxPage++;
                    if (list instanceof MCP_PlaneInfoManager || list instanceof MCH_ShipInfoManager) {
                        this.modelRot = 0;
                    } else {
                        this.modelRot = 1;
                    }
                }
            }
        }

        this.getAcInfoText(info);
        this.acInfo = info;
    }

    private void getAcInfoText(MCH_AircraftInfo info) {
        this.infoItem = new ArrayList<>();
        this.infoData = new ArrayList<>();
        if (info != null) {
            this.getAcInfoTextSub("Name", info.getItemStack().getDisplayName());
            this.getAcInfoTextSub("HP", "" + info.maxHp);
            int seatNum = !info.isUAV ? info.getNumSeat() : info.getNumSeat() - 1;
            this.getAcInfoTextSub("Num of Seat", "" + seatNum);
            this.getAcInfoTextSub("GunnerMode", info.isEnableGunnerMode ? "YES" : "NO");
            this.getAcInfoTextSub("NightVision", info.isEnableNightVision ? "YES" : "NO");
            this.getAcInfoTextSub("Radar", info.isEnableEntityRadar ? "YES" : "NO");
            this.getAcInfoTextSub("Inventory", "" + info.inventorySize);
            if (info instanceof MCH_PlaneInfo pinfo) {
                this.getAcInfoTextSub("VTOL", pinfo.isEnableVtol ? "YES" : "NO");
            }

            if (info instanceof MCH_ShipInfo pinfo) {
                this.getAcInfoTextSub("Submersible", pinfo.isEnableVtol ? "YES" : "NO");
            }

            if (info.getWeaponCount() > 0) {
                this.getAcInfoTextSub("Armed----------------");

                for (int i = 0; i < info.getWeaponCount(); i++) {
                    String type = info.getWeaponSetById(i).type;
                    MCH_WeaponInfo winfo = MCH_WeaponInfoManager.get(type);
                    if (winfo != null) {
                        this.getAcInfoTextSub(winfo.getWeaponTypeName(), winfo.displayName);
                    } else {
                        this.getAcInfoTextSub("ERROR", "Not found weapon " + (i + 1));
                    }
                }
            }
        }
    }

    private void getAcInfoTextSub(String item, String data) {
        this.infoItem.add(item + " :");
        this.infoData.add(data);
    }

    private void getAcInfoTextSub(String item) {
        this.infoItem.add(item);
        this.infoData.add("");
    }

    public void switchNextPage() {
        if (this.descMaxPage >= 2) {
            this.descPage = (this.descPage + 1) % this.descMaxPage;
        } else {
            this.descPage = 0;
        }
    }

    public void switchPrevPage() {
        this.descPage--;
        if (this.descPage < 0 && this.descMaxPage >= 2) {
            this.descPage = this.descMaxPage - 1;
        } else {
            this.descPage = 0;
        }
    }

    public int getDescCurrentPage() {
        return this.descPage;
    }

    public void setDescCurrentPage(int page) {
        if (this.descMaxPage > 0) {
            this.descPage = page < this.descMaxPage ? page : this.descMaxPage - 1;
        } else {
            this.descPage = 0;
        }
    }

    public int getDescMaxPage() {
        return this.descMaxPage;
    }

    @Nullable
    public ResourceLocation getCurrentPageTexture() {
        return this.descPage < this.descTexture.size() ? this.descTexture.get(this.descPage) : null;
    }

    public W_ModelCustom getModel() {
        return this.model;
    }

    public ResourceLocation getModelTexture() {
        return this.modelTexture;
    }

    @Nullable
    public MCH_AircraftInfo getAcInfo() {
        return this.acInfo;
    }

    public boolean isCurrentPageTexture() {
        return this.descPage >= 0 && this.descPage < this.descTexture.size();
    }

    public boolean isCurrentPageModel() {
        return this.getAcInfo() != null && this.getModel() != null && this.descPage == this.descTexture.size();
    }

    public boolean isCurrentPageAcInfo() {
        return this.getAcInfo() != null && this.descPage == this.descMaxPage - 1;
    }

    private List<ResourceLocation> getDescTexture(@Nullable IRecipe r) {
        List<ResourceLocation> list = new ArrayList<>();
        if (r != null) {
            for (int i = 0; i < 20; i++) {
                String itemName = r.getRecipeOutput().getTranslationKey();
                if (itemName.startsWith("tile.")) {
                    itemName = itemName.substring(5);
                }

                if (itemName.contains(":")) {
                    itemName = itemName.substring(itemName.indexOf(":") + 1);
                }

                itemName = "textures/drafting_table_desc/" + itemName + "#" + i + ".png";
                File filePng = new File(MCH_MOD.sourcePath, "/assets/mcheli/" + itemName);
                if (filePng.exists()) {
                    list.add(new ResourceLocation(Tags.MODID, itemName));
                }
            }
        }

        return list;
    }
}
