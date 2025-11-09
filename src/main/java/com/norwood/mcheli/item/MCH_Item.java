package com.norwood.mcheli.item;

import com.norwood.mcheli.wrapper.W_Item;

public class MCH_Item extends W_Item {

    // @Override
    // public Item setTexture(String par1Str) {
    // this.setTextureName(Tags.MODID + ":" + par1Str);
    // return this;
    // }

    public MCH_Item(int par1) {
        super(par1);
        this.setMaxStackSize(1);
    }
}
