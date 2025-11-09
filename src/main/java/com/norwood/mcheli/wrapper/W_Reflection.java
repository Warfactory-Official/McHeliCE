package com.norwood.mcheli.wrapper;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import com.norwood.mcheli.helper.client.MCH_CameraManager;

public class W_Reflection {

    public static RenderManager getRenderManager(Render<?> render) {
        return render.getRenderManager();
    }

    public static void restoreDefaultThirdPersonDistance() {
        setThirdPersonDistance(4.0F);
    }

    public static float getThirdPersonDistance() {
        return MCH_CameraManager.getThirdPeasonCameraDistance();
    }

    // Why is this here?
    public static void setThirdPersonDistance(float dist) {
        if (!(dist < 0.1)) {
            MCH_CameraManager.setThirdPeasonCameraDistance(dist);
        }
    }

    public static void setCameraRoll(float roll) {
        MCH_CameraManager.setCameraRoll(roll);
    }

    public static void restoreCameraZoom() {
        setCameraZoom(1.0F);
    }

    public static void setCameraZoom(float zoom) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, mc.entityRenderer, zoom, "field_78503_V");
            MCH_CameraManager.setCameraZoom(zoom);
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    public static void setCreativeDigSpeed(int n) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            ObfuscationReflectionHelper.setPrivateValue(PlayerControllerMP.class, mc.playerController, n,
                    "field_78781_i");
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    public static ItemRenderer getItemRenderer() {
        return Minecraft.getMinecraft().entityRenderer.itemRenderer;
    }

    @Deprecated
    public static void setItemRenderer(ItemRenderer r) {}

    @Nonnull
    public static ItemStack getItemRendererMainHand() {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(ItemRenderer.class, getItemRenderer(), "field_187467_d");
        } catch (Exception var1) {
            var1.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    public static void setItemRendererMainHand(ItemStack itemToRender) {
        try {
            ObfuscationReflectionHelper.setPrivateValue(ItemRenderer.class, getItemRenderer(), itemToRender,
                    "field_187467_d");
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    public static void setItemRendererMainProgress(float equippedProgress) {
        try {
            ObfuscationReflectionHelper.setPrivateValue(ItemRenderer.class, getItemRenderer(), equippedProgress,
                    "field_187469_f");
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }
}
