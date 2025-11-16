package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.MCH_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public class W_McClient {

    public static void playSoundClick(float volume, float pitch) {
        playSound(SoundEvents.UI_BUTTON_CLICK, volume, pitch);
    }

    public static void playSound(SoundEvent sound, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new W_Sound(sound, volume, pitch));
    }

    public static void DEF_playSoundFX(String name, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new W_Sound(new ResourceLocation(name), volume, pitch));
    }

    public static void MOD_playSoundFX(String name, float volume, float pitch) {
        DEF_playSoundFX(MCH_MOD.DOMAIN + ":" + name, volume, pitch);
    }

    public static void addSound(String name) {}

    public static void MOD_bindTexture(String tex) {
        Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(MCH_MOD.DOMAIN, tex));
    }

    public static boolean isGamePaused() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.isGamePaused();
    }

    public static Entity getRenderEntity() {
        return Minecraft.getMinecraft().getRenderViewEntity();
    }

    public static void setRenderEntity(EntityLivingBase entity) {
        Minecraft.getMinecraft().setRenderViewEntity(entity);
        if (W_EntityRenderer.currentShader != null) {
            W_EntityRenderer.activateShader(W_EntityRenderer.currentShader);
        }
    }
}
