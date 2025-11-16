package com.norwood.mcheli.wrapper;

import com.norwood.mcheli.MCH_MOD;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class W_SoundManagerFunc {

    public static void DEF_playEntitySound(SoundManager sm, String name, Entity entity, float volume, float pitch,
                                           boolean par5) {
        sm.playSound(new W_Sound(new ResourceLocation(name), volume, pitch, entity.posX, entity.posY, entity.posZ));
    }

    public static void MOD_playEntitySound(SoundManager sm, String name, Entity entity, float volume, float pitch,
                                           boolean par5) {
        DEF_playEntitySound(sm, MCH_MOD.DOMAIN + ":" + name, entity, volume, pitch, par5);
    }
}
