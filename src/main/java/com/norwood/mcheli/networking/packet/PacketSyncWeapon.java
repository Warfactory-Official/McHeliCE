package com.norwood.mcheli.networking.packet;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.weapon.MCH_WeaponSet;
import com.norwood.mcheli.wrapper.W_Lib;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@RequiredArgsConstructor
@ElegantPacket
public class PacketSyncWeapon extends PacketBase implements ServerToClientPacket {

    final public int entityID_Ac;
    final public int seatID;
    final public int weaponID;
    final public short ammo;
    final public short reserveAmmo;
    final public short cooldown;
    final public short reloadCooldown;

    @Override
    public void onReceive(Minecraft mc) {
        if (this.entityID_Ac > 0) {
            Entity entity = mc.player.world.getEntityByID(this.entityID_Ac);
            if (entity instanceof MCH_EntityAircraft ac) {
                if (ac.isValidSeatID(this.seatID)) {
                    MCH_WeaponSet wset = ac.getWeapon(this.weaponID);
                    wset.setAmmo(this.ammo);
                    wset.setReserveAmmo(this.reserveAmmo);
                    wset.cooldown = this.cooldown;
                    wset.reloadCooldown = this.reloadCooldown;
                    MCH_Logger.debugLog(true, "onPacketNotifyWeaponID:WeaponID=%d (%d / %d)", this.weaponID, this.ammo, this.reserveAmmo);
                    if (W_Lib.isClientPlayer(ac.getEntityBySeatId(this.seatID))) {
                        MCH_Logger.debugLog(true, "onPacketNotifyWeaponID:#discard:SeatID=%d, WeaponID=%d", this.seatID, this.weaponID);
                    } else {
                        MCH_Logger.debugLog(true, "onPacketNotifyWeaponID:SeatID=%d, WeaponID=%d", this.seatID, this.weaponID);
                        ac.updateWeaponID(this.seatID, this.weaponID);
                    }
                }
            }
        }
    }
}
