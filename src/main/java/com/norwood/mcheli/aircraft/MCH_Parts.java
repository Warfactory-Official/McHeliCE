package com.norwood.mcheli.aircraft;

import net.minecraft.entity.Entity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.wrapper.W_WorldFunc;

public class MCH_Parts {

    public final Entity parent;
    public final EntityDataManager dataManager;
    public final int shift;
    public final DataParameter<Integer> dataKey;
    public final String partName;
    public float prevRotation = 0.0F;
    public float rotation = 0.0F;
    public float rotationMax = 90.0F;
    public float rotationInv = 1.0F;
    public final MCH_Parts.Sound soundStartSwichOn = new Sound(this);
    public final MCH_Parts.Sound soundEndSwichOn = new Sound(this);
    public final MCH_Parts.Sound soundSwitching = new Sound(this);
    public final MCH_Parts.Sound soundStartSwichOff = new Sound(this);
    public final MCH_Parts.Sound soundEndSwichOff = new Sound(this);
    private boolean status = false;

    public MCH_Parts(Entity parent, int shiftBit, DataParameter<Integer> dataKey, String name) {
        this.parent = parent;
        this.dataManager = parent.getDataManager();
        this.shift = shiftBit;
        this.dataKey = dataKey;
        this.status = (this.getDataWatcherValue() & 1 << this.shift) != 0;
        this.partName = name;
    }

    public int getDataWatcherValue() {
        return this.dataManager.get(this.dataKey);
    }

    public void setStatusServer(boolean stat) {
        this.setStatusServer(stat, true);
    }

    public void setStatusServer(boolean stat, boolean playSound) {
        if (!this.parent.world.isRemote && this.getStatus() != stat) {
            MCH_Lib.DbgLog(false, "setStatusServer(ID=%d %s :%s -> %s)", this.shift, this.partName,
                    this.getStatus() ? "ON" : "OFF", stat ? "ON" : "OFF");
            this.updateDataWatcher(stat);
            this.playSound(this.soundSwitching);
            if (!stat) {
                this.playSound(this.soundStartSwichOff);
            } else {
                this.playSound(this.soundStartSwichOn);
            }

            this.update();
        }
    }

    protected void updateDataWatcher(boolean stat) {
        int currentStatus = this.getDataWatcherValue();
        int mask = 1 << this.shift;
        if (!stat) {
            this.dataManager.set(this.dataKey, currentStatus & ~mask);
        } else {
            this.dataManager.set(this.dataKey, currentStatus | mask);
        }

        this.status = stat;
    }

    public boolean getStatus() {
        return this.status;
    }

    public boolean isOFF() {
        return !this.status && this.rotation <= 0.02F;
    }

    public boolean isON() {
        return this.status && this.rotation >= this.rotationMax - 0.02F;
    }

    public void updateStatusClient(int statFromDataWatcher) {
        if (this.parent.world.isRemote) {
            this.status = (statFromDataWatcher & 1 << this.shift) != 0;
        }
    }

    public void update() {
        this.prevRotation = this.rotation;
        if (this.getStatus()) {
            if (this.rotation < this.rotationMax) {
                this.rotation = this.rotation + this.rotationInv;
                if (this.rotation >= this.rotationMax) {
                    this.playSound(this.soundEndSwichOn);
                }
            }
        } else if (this.rotation > 0.0F) {
            this.rotation = this.rotation - this.rotationInv;
            if (this.rotation <= 0.0F) {
                this.playSound(this.soundEndSwichOff);
            }
        }
    }

    public void forceSwitch(boolean onoff) {
        this.updateDataWatcher(onoff);
        this.rotation = this.prevRotation = this.rotationMax;
    }

    public float getFactor() {
        return this.rotationMax > 0.0F ? this.rotation / this.rotationMax : 0.0F;
    }

    public void playSound(MCH_Parts.Sound snd) {
        if (!snd.name.isEmpty() && !this.parent.world.isRemote) {
            W_WorldFunc.MOD_playSoundAtEntity(this.parent, snd.name, snd.volume, snd.pitch);
        }
    }

    public static class Sound {

        public String name = "";
        public float volume = 1.0F;
        public float pitch = 1.0F;

        public Sound(MCH_Parts paramMCH_Parts) {}

        public void setPrm(String n, float v, float p) {
            this.name = n;
            this.volume = v;
            this.pitch = p;
        }
    }
}
