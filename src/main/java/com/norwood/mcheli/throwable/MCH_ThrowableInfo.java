package com.norwood.mcheli.throwable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Color;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.info.IItemContent;
import com.norwood.mcheli.wrapper.W_Item;

public class MCH_ThrowableInfo extends MCH_BaseInfo implements IItemContent {

    public final String name;
    public String displayName;
    public final HashMap<String, String> displayNameLang = new HashMap<>();
    public int itemID = 0;
    public W_Item item = null;
    public List<String> recipeString = new ArrayList<>();
    public final List<IRecipe> recipe = new ArrayList<>();
    public boolean isShapedRecipe = true;
    public int power = 0;
    public float acceleration = 1.0F;
    public float accelerationInWater = 1.0F;
    public float dispenseAcceleration = 1.0F;
    public int explosion = 0;
    public int delayFuse = 0;
    public float bound = 0.2F;
    public int timeFuse = 0;
    public boolean flaming = false;
    public int stackSize = 1;
    public float soundVolume = 1.0F;
    public float soundPitch = 1.0F;
    public float proximityFuseDist = 0.0F;
    public float accuracy = 0.0F;
    public int aliveTime = 10;
    public int bomblet = 0;
    public float bombletDiff = 0.3F;
    public _IModelCustom model = null;
    public float smokeSize = 10.0F;
    public int smokeNum = 0;
    public float smokeVelocityVertical = 1.0F;
    public float smokeVelocityHorizontal = 1.0F;
    public float gravity = 0.0F;
    public float gravityInWater = -0.04F;
    public String particleName = "explode";
    public boolean disableSmoke = true;
    public MCH_Color smokeColor = new MCH_Color();

    public MCH_ThrowableInfo(AddonResourceLocation location, String path) {
        super(location, path);
        this.name = location.getPath();
        this.displayName = location.getPath();
    }

    @Override
    public boolean validate() throws Exception {
        this.timeFuse *= 20;
        this.aliveTime *= 20;
        this.delayFuse *= 20;
        return super.validate();
    }

    @Override
    public void onPostReload() {
        item = (W_Item) ForgeRegistries.ITEMS.getValue(new ResourceLocation(MCH_MOD.MOD_ID, name));
    }

    @Override
    public Item getItem() {
        return this.item;
    }

    public static class RoundItem {

        public final int num;
        public final Item item;

        public RoundItem(MCH_ThrowableInfo paramMCH_ThrowableInfo, int n, Item i) {
            this.num = n;
            this.item = i;
        }
    }
}
