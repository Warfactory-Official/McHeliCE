package com.norwood.mcheli.networking.packet.control;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_EntitySeat;
import com.norwood.mcheli.chain.MCH_EntityChain;
import com.norwood.mcheli.container.MCH_EntityContainer;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helper.MCH_CriteriaTriggers;
import com.norwood.mcheli.networking.data.DataPlayerControlAircraft;
import com.norwood.mcheli.networking.data.DataPlayerControlVehicle;
import com.norwood.mcheli.uav.MCH_EntityUavStation;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

@RequiredArgsConstructor
@ElegantPacket
public class PacketPlayerControlHeli extends PacketPlayerControlBase {

    public final DataPlayerControlVehicle controlBaseData;

    protected void process(MCH_EntityAircraft aircraft, DataPlayerControlAircraft data, EntityPlayer player) {
        if (aircraft == null) return;
        handleUnmount(aircraft, data);
        handleEjectSeat(aircraft, data, player);
        handleFold(aircraft, data);
        handleVtolSwitch(aircraft, data);
        handleModeSwitch(aircraft, data);
        handleSearchLight(aircraft, data);
        handleCameraMode(aircraft, data, player);
        handleWeaponSwitch(aircraft, data, player);
        handleUseWeapon(aircraft, data, player);
        handlePilotControls(aircraft, data, player);
        handleFlare(aircraft, data);
        handleChain(aircraft, data, player);
        handleGui(aircraft, data, player);
        handleHatch(aircraft, data);
        handleFreeLook(aircraft, data);
        handleGear(aircraft, data);
        handleRack(aircraft, data);
        handleGunnerStatus(aircraft, data);
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        MCH_EntityHeli heli = null;
        if (player.getRidingEntity() instanceof MCH_EntityHeli) {
            heli = (MCH_EntityHeli) player.getRidingEntity();
        } else if (player.getRidingEntity() instanceof MCH_EntitySeat) {
            if (((MCH_EntitySeat) player.getRidingEntity()).getParent() instanceof MCH_EntityHeli) {
                heli = (MCH_EntityHeli) ((MCH_EntitySeat) player.getRidingEntity()).getParent();
            }
        } else if (player.getRidingEntity() instanceof MCH_EntityUavStation uavStation) {
            if (uavStation.getControlAircract() instanceof MCH_EntityHeli) {
                heli = (MCH_EntityHeli) uavStation.getControlAircract();
            }
        }
        process(heli, controlBaseData, player);
    }

    // This looks odd, propably can be a bool
    protected void handleFold(MCH_EntityAircraft aircraft, DataPlayerControlAircraft data) {
        var heliData = (DataPlayerControlVehicle) data;
        switch (heliData.getBladeStatus()) {
            case UNFOLD -> ((MCH_EntityHeli) aircraft).setFoldBladeStat((byte) 3);
            case FOLD -> ((MCH_EntityHeli) aircraft).setFoldBladeStat((byte) 1);
        }
    }

    @Override
    protected void handleChain(MCH_EntityAircraft aircraft, DataPlayerControlAircraft data, EntityPlayer player) {
        var heliData = (DataPlayerControlVehicle) data;

        if (heliData.getUnhitchChainId() >= 0) {
            Entity e = player.world.getEntityByID(heliData.getUnhitchChainId());
            if (e instanceof MCH_EntityChain) {
                if (((MCH_EntityChain) e).towedEntity instanceof MCH_EntityContainer &&
                        MCH_Lib.getBlockIdY(aircraft, 3, -20) == 0 && player instanceof EntityPlayerMP) {
                    MCH_CriteriaTriggers.RELIEF_SUPPLIES.trigger((EntityPlayerMP) player);
                }

                e.setDead();
            }
        }
    }
}
