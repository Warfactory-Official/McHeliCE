package com.norwood.mcheli;

import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.client.MCH_Models;
import com.norwood.mcheli.helper.client.IModelCustom;
import com.norwood.mcheli.wrapper.W_ModelBase;
import com.norwood.mcheli.wrapper.modelloader.ModelVBO;
import com.norwood.mcheli.wrapper.modelloader.W_ModelCustom;
import lombok.Setter;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class MCH_ModelManager extends W_ModelBase {

    private static final MCH_ModelManager instance = new MCH_ModelManager();
    private static final ConcurrentHashMap<String, IModelCustom> map = new ConcurrentHashMap<>();
    private static final ModelRenderer defaultModel;
    @Setter
    private static boolean forceReloadMode = false;

    static {
        defaultModel = new ModelRenderer(instance, 0, 0);
        defaultModel.addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10, 0.0F);
    }

    private MCH_ModelManager() {
    }

    public static void makeVBO() {
        map.forEach((k, v) -> {
            if (v != null) v.toVBO();
        });
    }

    public static IModelCustom load(@NotNull String path, @NotNull String name, boolean noThrow) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(name);
        if (path.isEmpty() || name.isEmpty()) throw new IllegalArgumentException();
        return load(path + "/" + name, noThrow);
    }

    public static IModelCustom load(@NotNull String path, @NotNull String name) {
        return load(path,name,false);
    }



    @Nullable
    public static IModelCustom load(String name, boolean noThrow) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException();
        IModelCustom obj = map.get(name);
        if (obj != null) {
            if (!forceReloadMode) {
                return obj;
            }

            if (obj instanceof ModelVBO)
                ((ModelVBO) obj).delete();
            map.remove(name);
        }

        IModelCustom model;

        try {
            model = MCH_Models.loadModel(name);
        } catch (MCH_Models.ModelLoadException mle) {
            if (!noThrow)
                MCH_Utils.logger().catching(mle);
            model = null;
        }
        if (model != null) map.put(name, model);
        return model;
    }
    public static IModelCustom load(String name) {
        return load(name, false);
    }

    public static void render(String path, String name) {
        render(path + "/" + name);
    }

    public static void render(String name) {
        IModelCustom model = map.get(name);
        if (model != null) {
            model.renderAll();
        } else if (defaultModel == null) {
        }
    }

    public static void renderPart(String name, String partName) {
        IModelCustom model = map.get(name);
        if (model != null) {
            model.renderPart(partName);
        }
    }

    public static void render(String path, String name, int startFace, int maxFace) {
        IModelCustom model = map.get(path + "/" + name);
        if (model instanceof W_ModelCustom) {
            ((W_ModelCustom) model).renderAll(startFace, maxFace);
        }
    }

    public static W_ModelCustom get(String path, String name) {
        IModelCustom model = map.get(path + "/" + name);
        return model instanceof W_ModelCustom ? (W_ModelCustom) model : null;
    }
}
