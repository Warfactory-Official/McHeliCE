package com.norwood.mcheli.helper.client.model.loader;

import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.client._ModelFormatException;
import com.norwood.mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class WavefrontModelLoader implements IVertexModelLoader {

    @Override
    public _IModelCustom load(IResourceManager resourceManager, ResourceLocation location) throws IOException,
                                                                                           _ModelFormatException {
        ResourceLocation modelLocation = this.withExtension(location);
        IResource resource = resourceManager.getResource(modelLocation);
        return new W_WavefrontObject(modelLocation, resource);
    }

    @Override
    public String getExtension() {
        return "obj";
    }
}
