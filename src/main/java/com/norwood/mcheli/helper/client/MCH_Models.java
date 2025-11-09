package com.norwood.mcheli.helper.client;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.client.model.loader.IVertexModelLoader;
import com.norwood.mcheli.helper.client.model.loader.MetasequoiaModelLoader;
import com.norwood.mcheli.helper.client.model.loader.WavefrontModelLoader;

@SideOnly(Side.CLIENT)
public class MCH_Models {

    private static final IVertexModelLoader objLoader = new WavefrontModelLoader();
    private static final IVertexModelLoader mqoLoader = new MetasequoiaModelLoader();

    public static _IModelCustom loadModel(String name) throws IllegalArgumentException, _ModelFormatException {
        ResourceLocation resource = MCH_Utils.suffix("models/" + name);
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        IVertexModelLoader[] loaders = new IVertexModelLoader[] { objLoader, mqoLoader };
        _IModelCustom model = null;

        for (IVertexModelLoader loader : loaders) {
            try {

                model = loader.load(resourceManager, resource);
            } catch (FileNotFoundException var10) {
                MCH_Utils.logger().debug("model file not found '{}' at .{}", resource, loader.getExtension());
            } catch (IOException var11) {
                MCH_Utils.logger().error("load model error '{}' at .{}", resource, loader.getExtension(), var11);
                return null;
            }

            if (model != null) {
                break;
            }
        }

        return model;
    }
}
