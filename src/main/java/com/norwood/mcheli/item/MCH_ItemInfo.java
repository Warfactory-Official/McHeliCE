package com.norwood.mcheli.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.IContentData;
import com.norwood.mcheli.wrapper.W_Item;

public class MCH_ItemInfo extends MCH_BaseInfo implements IContentData { // implements

    public final String name;
    public String displayName;
    public HashMap displayNameLang;
    public int itemID;
    public W_Item item;
    public List recipeString;
    public List recipe;
    public boolean isShapedRecipe;
    public int stackSize;

    // public MCH_ItemInfo(String name) {
    // this.name = name;
    // this.displayName = name;
    // this.displayNameLang = new HashMap();
    // this.itemID = 0;
    // this.item = null;
    // this.recipeString = new ArrayList();
    // this.recipe = new ArrayList();
    // this.isShapedRecipe = true;
    // this.stackSize = 1;
    // }

    public MCH_ItemInfo(AddonResourceLocation location, String filePath, String name) {
        super(location, filePath); // required in 1.12.2 or something
        this.name = name;
        this.displayName = name;
        this.displayNameLang = new HashMap<>();
        this.itemID = 0;
        this.item = null;
        this.recipeString = new ArrayList<>();
        this.recipe = new ArrayList<>();
        this.isShapedRecipe = true;
        this.stackSize = 1;
    }

    @Override
    public void onPostReload() {}
}
