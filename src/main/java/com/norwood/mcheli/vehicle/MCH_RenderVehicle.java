package com.norwood.mcheli.vehicle;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_RenderAircraft;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Entity;
import com.norwood.mcheli.wrapper.W_Lib;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MCH_RenderVehicle extends MCH_RenderAircraft<MCH_EntityVehicle> {

    public static final IRenderFactory<MCH_EntityVehicle> FACTORY = MCH_RenderVehicle::new;

    public MCH_RenderVehicle(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 2.0F;
    }

    @Override
    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch,
                               float roll, float tickTime) {
        MCH_VehicleInfo vehicleInfo;
        if (entity instanceof MCH_EntityVehicle vehicle) {
            vehicleInfo = vehicle.getVehicleInfo();
            if (vehicleInfo != null) {
                if (vehicle.getRiddenByEntity() != null && !vehicle.isDestroyed()) {
                    vehicle.isUsedPlayer = true;
                    vehicle.lastRiderYaw = vehicle.getRiddenByEntity().rotationYaw;
                    vehicle.lastRiderPitch = vehicle.getRiddenByEntity().rotationPitch;
                } else if (!vehicle.isUsedPlayer) {
                    vehicle.lastRiderYaw = vehicle.rotationYaw;
                    vehicle.lastRiderPitch = vehicle.rotationPitch;
                }

                posY += W_Entity.GLOBAL_Y_OFFSET;
                this.renderDebugHitBox(vehicle, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(vehicle, posX, posY, posZ, yaw, pitch, roll);
                GlStateManager.translate(posX, posY, posZ);
                GlStateManager.rotate(yaw, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                this.bindTexture("textures/vehicles/" + vehicle.getTextureName() + ".png", vehicle);
                renderBody(vehicleInfo.model);
                MCH_WeaponSet ws = vehicle.getFirstSeatWeapon();
                this.drawPart(vehicle, vehicleInfo, yaw, pitch, ws, tickTime);
            }
        }
    }

    public void drawPart(MCH_EntityVehicle vehicle, MCH_VehicleInfo info, float yaw, float pitch, MCH_WeaponSet ws,
                         float tickTime) {
        float rotBrl = ws.prevRotBarrel + (ws.rotBarrel - ws.prevRotBarrel) * tickTime;
        int index = 0;

        for (MCH_VehicleInfo.VPart vp : info.partList) {
            index = this.drawPart(vp, vehicle, info, yaw, pitch, rotBrl, tickTime, ws, index);
        }
    }

    int drawPart(
                 MCH_VehicleInfo.VPart vp,
                 MCH_EntityVehicle vehicle,
                 MCH_VehicleInfo info,
                 float yaw,
                 float pitch,
                 float rotBrl,
                 float tickTime,
                 MCH_WeaponSet ws,
                 int index) {
        GlStateManager.pushMatrix();
        float recoilBuf = 0.0F;
        if (index < ws.getWeaponsCount()) {
            MCH_WeaponSet.Recoil r = ws.recoilBuf[index];
            recoilBuf = r.prevRecoilBuf + (r.recoilBuf - r.prevRecoilBuf) * tickTime;
        }

        int bkIndex = index;
        if (vp.rotPitch || vp.rotYaw || vp.type == 1) {
            GlStateManager.translate(vp.pos.x, vp.pos.y, vp.pos.z);
            if (vp.rotYaw) {
                GlStateManager.rotate(-vehicle.lastRiderYaw + yaw, 0.0F, 1.0F, 0.0F);
            }

            if (vp.rotPitch) {
                float p = MCH_Lib.RNG(vehicle.lastRiderPitch, info.minRotationPitch, info.maxRotationPitch);
                GlStateManager.rotate(p - pitch, 1.0F, 0.0F, 0.0F);
            }

            if (vp.type == 1) {
                GlStateManager.rotate(rotBrl, 0.0F, 0.0F, -1.0F);
            }

            GlStateManager.translate(-vp.pos.x, -vp.pos.y, -vp.pos.z);
        }

        if (vp.type == 2) {
            GlStateManager.translate(0.0, 0.0, -vp.recoilBuf * recoilBuf);
        }

        if (vp.type == 2 || vp.type == 3) {
            index++;
        }

        if (vp.child != null) {
            for (MCH_VehicleInfo.VPart vcp : vp.child) {
                index = this.drawPart(vcp, vehicle, info, yaw, pitch, rotBrl, recoilBuf, ws, index);
            }
        }

        if ((vp.drawFP || !W_Lib.isClientPlayer(vehicle.getRiddenByEntity()) || !W_Lib.isFirstPerson()) &&
                (vp.type != 3 || !vehicle.isWeaponNotCooldown(ws, bkIndex))) {
            renderPart(vp.model, info.model, vp.modelName);
            MCH_ModelManager.render("vehicles", vp.modelName);
        }

        GlStateManager.popMatrix();
        return index;
    }

    protected ResourceLocation getEntityTexture(MCH_EntityVehicle entity) {
        return TEX_DEFAULT;
    }
}
