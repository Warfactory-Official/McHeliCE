package com.norwood.mcheli.helper.client;

import net.minecraft.util.ResourceLocation;

import java.net.URL;

@Deprecated
public interface _IModelCustomLoader {

    String getType();

    String[] getSuffixes();

    @Deprecated
    _IModelCustom loadInstance(ResourceLocation var1) throws _ModelFormatException;

    @Deprecated
    _IModelCustom loadInstance(String var1, URL var2) throws _ModelFormatException;
}
