package com.norwood.mcheli.networking.packet.control;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

@RequiredArgsConstructor
@ElegantPacket
public class PacketPlayerControlTank extends PacketPlayerControlBase {

    public final DataPlayerControlAircraft data;

    @Override
    public void onReceive(EntityPlayerMP player) {
        MCH_EntityTank tank = null;
        if (player.getRidingEntity() instanceof MCH_EntityTank) {
            tank = (MCH_EntityTank) player.getRidingEntity();
        } else if (player.getRidingEntity() instanceof MCH_EntitySeat) {
            if (((MCH_EntitySeat) player.getRidingEntity()).getParent() instanceof MCH_EntityTank) {
                tank = (MCH_EntityTank) ((MCH_EntitySeat) player.getRidingEntity()).getParent();
            }
        } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
            if (uavStation.getControlAircract() instanceof MCH_EntityTank) {
                tank = (MCH_EntityTank) uavStation.getControlAircract();
            }
        }

        process(tank, data, player);
    }

    protected void handlePilotControls(MCH_EntityAircraft tank, DataPlayerControlAircraft data, EntityPlayer player) {
        if (!tank.isPilot(player)) return;
        tank.throttleUp = data.isThrottleUp();
        tank.throttleDown = data.isThrottleDown();
        double dx = tank.posX - tank.prevPosX;
        double dz = tank.posZ - tank.prevPosZ;
        double dist = dx * dx + dz * dz;
        if (data.isUseBrake() && tank.getCurrentThrottle() <= 0.03 && dist < 0.01) {
            tank.moveLeft = false;
            tank.moveRight = false;
        }

        tank.setBrake(data.isUseBrake());
    }
}
