package com.norwood.mcheli.helper.client;

import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.client.model.loader.IVertexModelLoader;
import com.norwood.mcheli.helper.client.model.loader.MetasequoiaModelLoader;
import com.norwood.mcheli.helper.client.model.loader.WavefrontModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Contract;

import java.io.FileNotFoundException;

@SideOnly(Side.CLIENT)
public class MCH_Models {
    private static final IVertexModelLoader objLoader = new WavefrontModelLoader();
    private static final IVertexModelLoader mqoLoader = new MetasequoiaModelLoader();
    private static final IVertexModelLoader[] loaders = new IVertexModelLoader[] { objLoader, mqoLoader };

    @Contract("_ -> new")
    public static _IModelCustom loadModel(String name) throws ModelLoadException {
        ResourceLocation resource = MCH_Utils.suffix("models/" + name);
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        Throwable message = null;
        int fnfeCounter = loaders.length;

        for (IVertexModelLoader loader : loaders) {
            try {
                return loader.load(resourceManager, resource);
            } catch (FileNotFoundException fnfe) {
                MCH_Utils.logger().debug("model file not found '{}' at .{}", resource, loader.getExtension());
                fnfeCounter--;
            } catch (Throwable throwable) {
                MCH_Utils.logger().error("load model error '{}' at .{}", resource, loader.getExtension());
                message = throwable;
            }
        }
        if (fnfeCounter == 0) throw new ModelLoadException(resource + " not found");
        throw new ModelLoadException("model load error for " + resource, message);
    }

    public static class ModelLoadException extends Exception {
        public ModelLoadException(String message) {
            super(message);
        }

        public ModelLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
