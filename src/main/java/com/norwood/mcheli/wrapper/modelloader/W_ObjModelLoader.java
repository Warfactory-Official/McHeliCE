package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.client._IModelCustomLoader;
import com.norwood.mcheli.helper.client._ModelFormatException;
import net.minecraft.util.ResourceLocation;

import java.net.URL;

public class W_ObjModelLoader implements _IModelCustomLoader {
    private static final String[] types = new String[]{"obj"};

    @Override
    public String getType() {
        return "OBJ model";
    }

    @Override
    public String[] getSuffixes() {
        return types;
    }

    @Override
    public _IModelCustom loadInstance(ResourceLocation resource) throws _ModelFormatException {
//        return new W_WavefrontObject(resource);
    }

    @Override
    public _IModelCustom loadInstance(String resourceName, URL resource) throws _ModelFormatException {
        return new W_WavefrontObject(resourceName, resource);
    }
}
