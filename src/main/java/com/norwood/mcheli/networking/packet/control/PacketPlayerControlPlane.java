package com.norwood.mcheli.networking.packet.control;

import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.uav.MCH_EntityUavStation;

import hohserg.elegant.networking.api.ElegantPacket;
import lombok.RequiredArgsConstructor;

@ElegantPacket
@RequiredArgsConstructor
public class PacketPlayerControlPlane extends PacketPlayerControlBase {

    public final DataPlayerControlAircraft controlBaseData;

    @Override
    public void onReceive(EntityPlayerMP player) {
        MCH_EntityPlane plane = null;
        if (player.getRidingEntity() instanceof MCH_EntityPlane) {
            plane = (MCH_EntityPlane) player.getRidingEntity();
        } else if (player.getRidingEntity() instanceof MCH_EntitySeat) {
            if (((MCH_EntitySeat) player.getRidingEntity()).getParent() instanceof MCH_EntityPlane) {
                plane = (MCH_EntityPlane) ((MCH_EntitySeat) player.getRidingEntity()).getParent();
            }
        } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
            if (uavStation.getControlAircract() instanceof MCH_EntityPlane) {
                plane = (MCH_EntityPlane) uavStation.getControlAircract();
            }
        }

        process(plane, controlBaseData, player);
    }

    @Override
    protected void handleHatch(MCH_EntityAircraft aircraft, DataPlayerControlAircraft data) {
        switch (data.switchHatch) {
            case FOLD, UNFOLD -> {
                if (aircraft.getAcInfo().haveHatch()) {
                    aircraft.foldHatch(controlBaseData.switchHatch == DataPlayerControlAircraft.HatchSwitch.UNFOLD);
                } else {
                    ((MCH_EntityPlane) aircraft)
                            .foldWing(data.switchHatch == DataPlayerControlAircraft.HatchSwitch.UNFOLD);
                }
            }
        }
    }

    @Override
    protected void handleVtolSwitch(MCH_EntityAircraft aircraft, DataPlayerControlAircraft data) {
        MCH_EntityPlane plane = (MCH_EntityPlane) aircraft;
        switch (data.switchVtol) {
            case VTOL_OFF -> plane.swithVtolMode(false);
            case VTOL_ON -> plane.swithVtolMode(true);
        }
    }
}
