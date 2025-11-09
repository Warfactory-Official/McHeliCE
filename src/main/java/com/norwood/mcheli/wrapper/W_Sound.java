package com.norwood.mcheli.wrapper;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

import com.norwood.mcheli.helper.MCH_SoundEvents;

public class W_Sound extends MovingSound {

    protected W_Sound(ResourceLocation r, float volume, float pitch, double x, double y, double z) {
        super(MCH_SoundEvents.getSound(r), SoundCategory.MASTER);
        this.setVolumeAndPitch(volume, pitch);
        this.setPosition(x, y, z);
    }

    protected W_Sound(ResourceLocation r, float volume, float pitch) {
        this(MCH_SoundEvents.getSound(r), volume, pitch);
    }

    protected W_Sound(SoundEvent soundEvent, float volume, float pitch) {
        super(soundEvent, SoundCategory.MASTER);
        this.setVolumeAndPitch(volume, pitch);
        Entity entity = W_McClient.getRenderEntity();
        if (entity != null) {
            this.setPosition(entity.posX, entity.posY, entity.posZ);
        }
    }

    public void setRepeat(boolean b) {
        this.repeat = b;
    }

    public void setSoundParam(Entity e, float v, float p) {
        this.setPosition(e);
        this.setVolumeAndPitch(v, p);
    }

    public void setVolumeAndPitch(float v, float p) {
        this.setVolume(v);
        this.setPitch(p);
    }

    public void setVolume(float v) {
        this.volume = v;
    }

    public void setPitch(float p) {
        this.pitch = p;
    }

    public void setPosition(double x, double y, double z) {
        this.xPosF = (float) x;
        this.yPosF = (float) y;
        this.zPosF = (float) z;
    }

    public void setPosition(Entity e) {
        this.setPosition(e.posX, e.posY, e.posZ);
    }

    public void update() {}
}
