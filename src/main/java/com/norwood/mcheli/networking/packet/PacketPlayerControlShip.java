package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.networking.handlers.PlayerControlBaseData;
import com.norwood.mcheli.plane.MCP_EntityPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayerMP;

@ElegantPacket
@RequiredArgsConstructor
public class PacketPlayerControlShip  extends  PacketPlayerControlBase{

    public final PlayerControlBaseData controlBaseData;

    @Override
    public void onReceive(EntityPlayerMP player) {
        MCH_EntityShip ship = null;
        if (player.getRidingEntity() instanceof MCH_EntityShip) {
            ship = (MCH_EntityShip) player.getRidingEntity();
        } else if (player.getRidingEntity() instanceof MCH_EntitySeat) {
            if (((MCH_EntitySeat) player.getRidingEntity()).getParent() instanceof MCH_EntityShip) {
                ship = (MCH_EntityShip) ((MCH_EntitySeat) player.getRidingEntity()).getParent();
            }
        } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
            if (uavStation.getControlAircract() instanceof MCH_EntityShip) {
                ship = (MCH_EntityShip) uavStation.getControlAircract();
            }
        }
        process(ship, controlBaseData, player);

    }


    @Override
    protected void handleHatch(MCH_EntityAircraft aircraft, PlayerControlBaseData data) {
        switch (data.switchHatch) {
            case FOLD, UNFOLD -> {
                if (aircraft.getAcInfo().haveHatch()) {
                    aircraft.foldHatch(controlBaseData.switchHatch == PlayerControlBaseData.HatchSwitch.UNFOLD);
                } else {
                    ((MCP_EntityPlane) aircraft).foldWing(data.switchHatch == PlayerControlBaseData.HatchSwitch.UNFOLD);
                }
            }
        }
    }


    @Override
    protected void handleVtolSwitch(MCH_EntityAircraft aircraft, PlayerControlBaseData data) {
        MCP_EntityPlane plane = (MCP_EntityPlane) aircraft;
        switch (data.switchVtol) {
            case VTOL_OFF -> plane.swithVtolMode(false);
            case VTOL_ON -> plane.swithVtolMode(true);
        }
    }
}
