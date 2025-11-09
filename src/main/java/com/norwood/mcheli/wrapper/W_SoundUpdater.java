package com.norwood.mcheli.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;

import com.norwood.mcheli.helper.MCH_Utils;

public class W_SoundUpdater {

    protected final SoundHandler theSoundHnadler;
    protected W_Sound es;

    public W_SoundUpdater(Minecraft minecraft, Entity entity) {
        this.theSoundHnadler = minecraft.getSoundHandler();
    }

    public void initEntitySound(String name) {
        this.es = new W_Sound(MCH_Utils.suffix(name), 1.0F, 1.0F);
        this.es.setRepeat(true);
    }

    public boolean isValidSound() {
        return this.es != null;
    }

    public void playEntitySound(String name, Entity entity, float volume, float pitch, boolean par5) {
        if (this.isValidSound()) {
            this.es.setSoundParam(entity, volume, pitch);
            this.theSoundHnadler.playSound(this.es);
        }
    }

    public void stopEntitySound(Entity entity) {
        if (this.isValidSound()) {
            this.theSoundHnadler.stopSound(this.es);
        }
    }

    public boolean isEntitySoundPlaying(Entity entity) {
        return this.isValidSound() && this.theSoundHnadler.isSoundPlaying(this.es);
    }

    public void setEntitySoundPitch(Entity entity, float pitch) {
        if (this.isValidSound()) {
            this.es.setPitch(pitch);
        }
    }

    public void setEntitySoundVolume(Entity entity, float volume) {
        if (this.isValidSound()) {
            this.es.setVolume(volume);
        }
    }

    public void updateSoundLocation(Entity entity) {
        if (this.isValidSound()) {
            this.es.setPosition(entity);
        }
    }

    public void updateSoundLocation(double x, double y, double z) {
        if (this.isValidSound()) {
            this.es.setPosition(x, y, z);
        }
    }
}
