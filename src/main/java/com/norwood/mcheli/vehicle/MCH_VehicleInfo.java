package com.norwood.mcheli.vehicle;

import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class MCH_VehicleInfo extends MCH_AircraftInfo {
    public MCH_ItemVehicle item = null;
    public boolean isEnableMove = false;
    public boolean isEnableRot = false;
    public final List<MCH_VehicleInfo.VPart> partList = new ArrayList<>();

    public MCH_VehicleInfo(AddonResourceLocation location, String path) {
        super(location, path);
    }
    public float getMinRotationPitch() {
        return -90.0F;
    }
    public float getMaxRotationPitch() {
        return 90.0F;
    }
    @Override
    public Item getItem() {
        return this.item;
    }
    @Override
    public boolean validate() throws Exception {
        return super.validate();
    }
    @Override
    public String getDefaultHudName(int seatId) {
        return "vehicle";
    }
    @Override
    public String getDirectoryName() {
        return "vehicles";
    }
    @Override
    public String getKindName() {
        return "vehicle";
    }
    @Override
    public void onPostReload() {
           item = (MCH_ItemVehicle) ForgeRegistries.ITEMS.getValue(new ResourceLocation(MCH_MOD.MOD_ID, name));
        MCH_MOD.proxy.registerModelsVehicle(this, true);
    }

    public static class VPart extends MCH_AircraftInfo.DrawnPart {
        public final boolean rotPitch;
        public final boolean rotYaw;
        public final int type;
        public final boolean drawFP;
        public final float recoilBuf;
        public List<MCH_VehicleInfo.VPart> child;

        public VPart(
                MCH_VehicleInfo paramMCH_VehicleInfo, float x, float y, float z, String model, boolean drawfp, boolean roty, boolean rotp, int type, float rb
        ) {
            super(paramMCH_VehicleInfo, x, y, z, 0.0F, 0.0F, 0.0F, model);
            this.rotYaw = roty;
            this.rotPitch = rotp;
            this.type = type;
            this.child = null;
            this.drawFP = drawfp;
            this.recoilBuf = rb;
        }

        public VPart(DrawnPart other, boolean rotPitch, boolean rotYaw, int type, boolean drawFP, float recoilBuf) {
            super(other);
            this.rotPitch = rotPitch;
            this.rotYaw = rotYaw;
            this.type = type;
            this.drawFP = drawFP;
            this.recoilBuf = recoilBuf;
            this.child = null;
        }
    }
}
