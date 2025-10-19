package com.norwood.mcheli.ship;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class MCH_ShipInfo extends MCH_AircraftInfo {
    public MCH_ItemShip item = null;
    public final List<MCH_AircraftInfo.DrawnPart> nozzles = new ArrayList<>();
    public final List<MCH_ShipInfo.Rotor> rotorList = new ArrayList<>();
    public final List<MCH_ShipInfo.Wing> wingList = new ArrayList<>();
    public boolean isEnableVtol = false;
    public boolean isDefaultVtol;
    public float vtolYaw = 0.3F;
    public float vtolPitch = 0.2F;
    public boolean isEnableAutoPilot = false;
    public boolean isVariableSweepWing = false;
    public float sweepWingSpeed = this.speed;

    public MCH_ShipInfo(AddonResourceLocation location, String path) {
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

        this.speed = (float) (this.speed * MCH_Config.AllShipSpeed.prmDouble);
        this.sweepWingSpeed = (float) (this.sweepWingSpeed * MCH_Config.AllShipSpeed.prmDouble);
        return super.validate();
    }

    @Override
    public void loadItemData(String item, String data) {
        super.loadItemData(item, data);
        if (item.compareTo("addpartrotor") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 6) {
                float m = s.length >= 7 ? this.toFloat(s[6], -180.0F, 180.0F) / 90.0F : 1.0F;
                MCH_ShipInfo.Rotor e = new Rotor(
                        this,
                        this.toFloat(s[0]),
                        this.toFloat(s[1]),
                        this.toFloat(s[2]),
                        this.toFloat(s[3]),
                        this.toFloat(s[4]),
                        this.toFloat(s[5]),
                        m,
                        "rotor" + this.rotorList.size()
                );
                this.rotorList.add(e);
            }
        } else if (item.compareTo("addblade") == 0) {
            int idx = this.rotorList.size() - 1;
            MCH_ShipInfo.Rotor r = !this.rotorList.isEmpty() ? this.rotorList.get(idx) : null;
            if (r != null) {
                String[] s = data.split("\\s*,\\s*");
                if (s.length == 8) {
                    MCH_ShipInfo.Blade b = new Blade(
                            this,
                            this.toInt(s[0]),
                            this.toInt(s[1]),
                            this.toFloat(s[2]),
                            this.toFloat(s[3]),
                            this.toFloat(s[4]),
                            this.toFloat(s[5]),
                            this.toFloat(s[6]),
                            this.toFloat(s[7]),
                            "blade" + idx
                    );
                    r.blades.add(b);
                }
            }
        } else if (item.compareTo("addpartwing") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 7) {
                MCH_ShipInfo.Wing n = new Wing(
                        this,
                        this.toFloat(s[0]),
                        this.toFloat(s[1]),
                        this.toFloat(s[2]),
                        this.toFloat(s[3]),
                        this.toFloat(s[4]),
                        this.toFloat(s[5]),
                        this.toFloat(s[6]),
                        "wing" + this.wingList.size()
                );
                this.wingList.add(n);
            }
        } else if (item.equalsIgnoreCase("AddPartPylon")) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length >= 7 && !this.wingList.isEmpty()) {
                MCH_ShipInfo.Wing w = this.wingList.get(this.wingList.size() - 1);
                if (w.pylonList == null) {
                    w.pylonList = new ArrayList<>();
                }

                MCH_ShipInfo.Pylon n = new Pylon(
                        this,
                        this.toFloat(s[0]),
                        this.toFloat(s[1]),
                        this.toFloat(s[2]),
                        this.toFloat(s[3]),
                        this.toFloat(s[4]),
                        this.toFloat(s[5]),
                        this.toFloat(s[6]),
                        w.modelName + "_pylon" + w.pylonList.size()
                );
                w.pylonList.add(n);
            }
        } else if (item.compareTo("addpartnozzle") == 0) {
            String[] s = data.split("\\s*,\\s*");
            if (s.length == 6) {
                MCH_AircraftInfo.DrawnPart n = new DrawnPart(
                        this,
                        this.toFloat(s[0]),
                        this.toFloat(s[1]),
                        this.toFloat(s[2]),
                        this.toFloat(s[3]),
                        this.toFloat(s[4]),
                        this.toFloat(s[5]),
                        "nozzle" + this.nozzles.size()
                );
                this.nozzles.add(n);
            }
        } else if (item.compareTo("variablesweepwing") == 0) {
            this.isVariableSweepWing = this.toBool(data);
        } else if (item.compareTo("sweepwingspeed") == 0) {
            this.sweepWingSpeed = this.toFloat(data, 0.0F, 5.0F);
        } else if (item.compareTo("enablevtol") == 0) {
            this.isEnableVtol = this.toBool(data);
        } else if (item.compareTo("defaultvtol") == 0) {
            this.isDefaultVtol = this.toBool(data);
        } else if (item.compareTo("vtolyaw") == 0) {
            this.vtolYaw = this.toFloat(data, 0.0F, 1.0F);
        } else if (item.compareTo("vtolpitch") == 0) {
            this.vtolPitch = this.toFloat(data, 0.01F, 1.0F);
        } else if (item.compareTo("enableautopilot") == 0) {
            this.isEnableAutoPilot = this.toBool(data);
        }
    }

    @Override
    public String getDirectoryName() {
        return "ships";
    }

    @Override
    public String getKindName() {
        return "ship";
    }

    @Override
    public void onPostReload() {
        MCH_MOD.proxy.registerModelsShip(this, true);
    }

    public static class Blade extends MCH_AircraftInfo.DrawnPart {
        public final int numBlade;
        public final int rotBlade;

        public Blade(MCH_ShipInfo paramMCH_ShipInfo, int num, int r, float px, float py, float pz, float rx, float ry, float rz, String name) {
            super(paramMCH_ShipInfo, px, py, pz, rx, ry, rz, name);
            this.numBlade = num;
            this.rotBlade = r;
        }
    }

    public static class Pylon extends MCH_AircraftInfo.DrawnPart {
        public final float maxRotFactor;
        public final float maxRot;

        public Pylon(MCH_ShipInfo paramMCH_ShipInfo, float px, float py, float pz, float rx, float ry, float rz, float mr, String name) {
            super(paramMCH_ShipInfo, px, py, pz, rx, ry, rz, name);
            this.maxRot = mr;
            this.maxRotFactor = this.maxRot / 90.0F;
        }
    }

    public static class Rotor extends MCH_AircraftInfo.DrawnPart {
        public final float maxRotFactor;
        public final List<MCH_ShipInfo.Blade> blades = new ArrayList<>();

        public Rotor(MCH_ShipInfo paramMCH_ShipInfo, float x, float y, float z, float rx, float ry, float rz, float mrf, String model) {
            super(paramMCH_ShipInfo, x, y, z, rx, ry, rz, model);
            this.maxRotFactor = mrf;
        }
    }

    public static class Wing extends MCH_AircraftInfo.DrawnPart {
        public final float maxRotFactor;
        public final float maxRot;
        public List<MCH_ShipInfo.Pylon> pylonList;

        public Wing(MCH_ShipInfo paramMCH_ShipInfo, float px, float py, float pz, float rx, float ry, float rz, float mr, String name) {
            super(paramMCH_ShipInfo, px, py, pz, rx, ry, rz, name);
            this.maxRot = mr;
            this.maxRotFactor = this.maxRot / 90.0F;
            this.pylonList = null;
        }
    }
}
