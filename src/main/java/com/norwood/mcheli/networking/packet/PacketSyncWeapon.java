package com.norwood.mcheli.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Lib;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@ElegantPacket
public class PacketSyncWeapon extends PacketBase implements ServerToClientPacket {

    final public int entityID_Ac;
    final public int seatID;
    final public int weaponID;
    final public short ammo;
    final public short reserveAmmo;

    @Override
    public void onReceive(Minecraft mc) {
        if (this.entityID_Ac > 0) {
            Entity entity = mc.player.world.getEntityByID(this.entityID_Ac);
            if (entity instanceof MCH_EntityAircraft ac) {
                if (ac.isValidSeatID(this.seatID)) {
                    ac.getWeapon(this.weaponID).setAmmoNum(this.ammo);
                    ac.getWeapon(this.weaponID).setRestAllAmmoNum(this.reserveAmmo);
                    MCH_Lib.DbgLog(true, "onPacketNotifyWeaponID:WeaponID=%d (%d / %d)", this.weaponID, this.ammo,
                            this.reserveAmmo);
                    if (W_Lib.isClientPlayer(ac.getEntityBySeatId(this.seatID))) {
                        MCH_Lib.DbgLog(true, "onPacketNotifyWeaponID:#discard:SeatID=%d, WeaponID=%d", this.seatID,
                                this.weaponID);
                    } else {
                        MCH_Lib.DbgLog(true, "onPacketNotifyWeaponID:SeatID=%d, WeaponID=%d", this.seatID,
                                this.weaponID);
                        ac.updateWeaponID(this.seatID, this.weaponID);
                    }
                }
            }
        }
    }
}
