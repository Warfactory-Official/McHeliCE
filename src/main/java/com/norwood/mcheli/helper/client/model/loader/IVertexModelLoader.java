package com.norwood.mcheli.helper.client.model.loader;

import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.helper.client._IModelCustom;
import com.norwood.mcheli.helper.client._ModelFormatException;
import org.jetbrains.annotations.Contract;

@SideOnly(Side.CLIENT)
public interface IVertexModelLoader {

    String getExtension();

    @Contract("_, _ -> new")
    _IModelCustom load(IResourceManager var1, ResourceLocation var2) throws IOException, _ModelFormatException;

    default ResourceLocation withExtension(ResourceLocation location) {
        return new ResourceLocation(location.getNamespace(), location.getPath() + "." + this.getExtension());
    }
}
