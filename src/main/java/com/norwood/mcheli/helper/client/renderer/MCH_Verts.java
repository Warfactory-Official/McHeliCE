package com.norwood.mcheli.helper.client.renderer;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;

public class MCH_Verts {

    public static final VertexFormatElement TEX_2S = new VertexFormatElement(0, EnumType.SHORT, EnumUsage.UV, 2);
    public static final VertexFormat POS_COLOR_LMAP = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(TEX_2S);
}
