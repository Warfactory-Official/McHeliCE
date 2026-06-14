package com.norwood.mcheli.helper.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IModelCustom {

    String getType();

    @SideOnly(Side.CLIENT)
    void renderAll();

    @SideOnly(Side.CLIENT)
    void renderOnly(String... var1);

    @SideOnly(Side.CLIENT)
    void renderPart(String var1);

    @SideOnly(Side.CLIENT)
    void renderAllExcept(String... var1);

    @SideOnly(Side.CLIENT)
    IModelCustom toVBO();
}
