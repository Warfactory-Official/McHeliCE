package com.norwood.mcheli.plane;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;

public class MCH_PlaneInfo extends MCH_AircraftInfo {

    public MCP_ItemPlane item = null;
    public final List<MCH_AircraftInfo.DrawnPart> nozzles = new ArrayList<>();
    public final List<MCH_PlaneInfo.Rotor> rotorList = new ArrayList<>();
    public final List<MCH_PlaneInfo.Wing> wingList = new ArrayList<>();
    public boolean isEnableVtol = false;
    public boolean isDefaultVtol;
    public float vtolYaw = 0.3F;
    public float vtolPitch = 0.2F;
    public boolean isEnableAutoPilot = false;
    public boolean isVariableSweepWing = false;
    public float sweepWingSpeed = this.speed;

    public MCH_PlaneInfo(AddonResourceLocation location, String path) {
        super(location, path);
    }

    @Override
    public Item getItem() {
        return this.item;
    }

    @Override
    public float getDefaultRotorSpeed() {
        return 47.94F;
    }

    public boolean haveNozzle() {
        return !this.nozzles.isEmpty();
    }

    public boolean haveRotor() {
        return !this.rotorList.isEmpty();
    }

    public boolean haveWing() {
        return !this.wingList.isEmpty();
    }

    @Override
    public float getMaxSpeed() {
        return 1.8F;
    }

    @Override
    public int getDefaultMaxZoom() {
        return 8;
    }

    @Override
    public String getDefaultHudName(int seatId) {
        if (seatId <= 0) {
            return "plane";
        } else {
            return seatId == 1 ? "plane" : "gunner";
        }
    }

    @Override
    public boolean validate() throws Exception {
        if (this.haveHatch() && this.haveWing()) {
            this.wingList.clear();
            this.hatchList.clear();
        }

        this.speed = (float) (this.speed * MCH_Config.AllPlaneSpeed.prmDouble);
        this.sweepWingSpeed = (float) (this.sweepWingSpeed * MCH_Config.AllPlaneSpeed.prmDouble);
        return super.validate();
    }

    @Override
    public String getDirectoryName() {
        return "planes";
    }

    @Override
    public String getKindName() {
        return "plane";
    }

    @Override
    public void onPostReload() {
        item = (MCP_ItemPlane) ForgeRegistries.ITEMS.getValue(new ResourceLocation(MCH_MOD.MOD_ID, name));
        MCH_MOD.proxy.registerModelsPlane(this, true);
    }

    public static class Blade extends MCH_AircraftInfo.DrawnPart {

        public final int numBlade;
        public final int rotBlade;

        public Blade(MCH_PlaneInfo paramMCP_PlaneInfo, int num, int r, float px, float py, float pz, float rx, float ry,
                     float rz, String name) {
            super(paramMCP_PlaneInfo, px, py, pz, rx, ry, rz, name);
            this.numBlade = num;
            this.rotBlade = r;
        }

        public Blade(DrawnPart other, int numBlade, int rotBlade) {
            super(other);
            this.numBlade = numBlade;
            this.rotBlade = rotBlade;
        }
    }

    public static class Pylon extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final float maxRot;

        public Pylon(DrawnPart other, float maxRot) {
            super(other);
            this.maxRot = maxRot;
            this.maxRotFactor = maxRot / 90F;
        }

        public Pylon(MCH_PlaneInfo paramMCP_PlaneInfo, float px, float py, float pz, float rx, float ry, float rz,
                     float mr, String name) {
            super(paramMCP_PlaneInfo, px, py, pz, rx, ry, rz, name);
            this.maxRot = mr;
            this.maxRotFactor = this.maxRot / 90.0F;
        }
    }

    public static class Rotor extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final List<MCH_PlaneInfo.Blade> blades = new ArrayList<>();

        public Rotor(DrawnPart other, float maxRotFactor) {
            super(other);
            this.maxRotFactor = maxRotFactor;
        }

        public Rotor(MCH_PlaneInfo paramMCP_PlaneInfo, float x, float y, float z, float rx, float ry, float rz,
                     float mrf, String model) {
            super(paramMCP_PlaneInfo, x, y, z, rx, ry, rz, model);
            this.maxRotFactor = mrf;
        }
    }

    public static class Wing extends MCH_AircraftInfo.DrawnPart {

        public final float maxRotFactor;
        public final float maxRot;
        public List<MCH_PlaneInfo.Pylon> pylonList;

        public Wing(DrawnPart other, float maxRot) {
            super(other);
            this.maxRot = maxRot;
            this.maxRotFactor = maxRot / 90F;
            this.pylonList = null;
        }

        public Wing(MCH_PlaneInfo paramMCP_PlaneInfo, float px, float py, float pz, float rx, float ry, float rz,
                    float mr, String name) {
            super(paramMCP_PlaneInfo, px, py, pz, rx, ry, rz, name);
            this.maxRot = mr;
            this.maxRotFactor = this.maxRot / 90.0F;
            this.pylonList = null;
        }
    }
}
