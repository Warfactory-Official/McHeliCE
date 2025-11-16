package com.norwood.mcheli.helicopter;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class MCH_HeliInfo extends MCH_AircraftInfo {

    public MCH_ItemHeli item = null;
    public boolean isEnableFoldBlade = false;
    public final List<MCH_HeliInfo.Rotor> rotorList = new ArrayList<>();

    public MCH_HeliInfo(AddonResourceLocation location, String path) {
        super(location, path);
        // this.minRotationPitch = -20.0F;
        // this.maxRotationPitch = 20.0F;
    }

    public float getMinRotationPitch() {
        return -20F;
    }

    public float getMaxRotationPitch() {
        return 20F;
    }

    @Override
    public boolean validate() throws Exception {
        this.speed = (float) (this.speed * MCH_Config.AllHeliSpeed.prmDouble);
        return super.validate();
    }

    @Override
    public float getDefaultSoundRange() {
        return 80.0F;
    }

    @Override
    public float getDefaultRotorSpeed() {
        return 79.99F;
    }

    @Override
    public int getDefaultMaxZoom() {
        return 8;
    }

    @Override
    public Item getItem() {
        return this.item;
    }

    @Override
    public String getDefaultHudName(int seatId) {
        if (seatId <= 0) {
            return "heli";
        } else {
            return seatId == 1 ? "heli_gnr" : "gunner";
        }
    }

    @Override
    public String getDirectoryName() {
        return "helicopters";
    }

    @Override
    public String getKindName() {
        return "helicopter";
    }

    @Override
    public void onPostReload() {
        item = (MCH_ItemHeli) ForgeRegistries.ITEMS.getValue(new ResourceLocation(MCH_MOD.MOD_ID, name));
        MCH_MOD.proxy.registerModelsHeli(this, true);
    }

    @Override
    public String toString() {
        return "MCH_HeliInfo{" +
                "item=" + item +
                ", isEnableFoldBlade=" + isEnableFoldBlade +
                ", rotorList=" + rotorList +
                '}' + super.toString();
    }

    public static class Rotor extends MCH_AircraftInfo.DrawnPart {

        public final int bladeNum;
        public final int bladeRot;
        public final boolean haveFoldFunc;
        public final boolean oldRenderMethod;

        public Rotor(DrawnPart other, int bladeNum, int bladeRot, boolean haveFoldFunc, boolean oldRenderMethod) {
            super(other);
            this.bladeNum = bladeNum;
            this.bladeRot = bladeRot;
            this.haveFoldFunc = haveFoldFunc;
            this.oldRenderMethod = oldRenderMethod;
        }

        public Rotor(
                     MCH_HeliInfo paramMCH_HeliInfo, int bladeNum, int bladeRot, float x, float y, float z, float rx,
                     float ry, float rz, String model, boolean haveFoldFunc, boolean oldRenderMethod) {
            super(paramMCH_HeliInfo, x, y, z, rx, ry, rz, model);
            this.bladeNum = bladeNum;
            this.bladeRot = bladeRot;
            this.haveFoldFunc = haveFoldFunc;
            this.oldRenderMethod = oldRenderMethod;
        }

        @Override
        public String toString() {
            return "Rotor{" +
                    "bladeNum=" + bladeNum +
                    ", bladeRot=" + bladeRot +
                    ", haveFoldFunc=" + haveFoldFunc +
                    ", oldRenderMethod=" + oldRenderMethod +
                    ", pos=" + pos +
                    ", rot=" + rot +
                    ", modelName='" + modelName + '\'' +
                    ", model=" + model +
                    '}';
        }
    }
}
