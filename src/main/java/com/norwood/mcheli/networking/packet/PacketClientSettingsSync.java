package com.norwood.mcheli.networking.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_EntityRenderer;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;

@ElegantPacket
public class PacketClientSettingsSync extends PacketBase implements ClientToServerPacket {

    public boolean dismountAll = true;
    public boolean heliAutoThrottleDown;
    public boolean planeAutoThrottleDown;
    public boolean tankAutoThrottleDown;
    public boolean shaderSupport = false;

    public static void send() {
        var packet = new PacketClientSettingsSync();
        packet.dismountAll = MCH_Config.DismountAll.prmBool;
        packet.heliAutoThrottleDown = MCH_Config.AutoThrottleDownHeli.prmBool;
        packet.planeAutoThrottleDown = MCH_Config.AutoThrottleDownPlane.prmBool;
        packet.tankAutoThrottleDown = MCH_Config.AutoThrottleDownTank.prmBool;
        packet.shaderSupport = W_EntityRenderer.isShaderSupport();
        packet.sendToServer();
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (player.world.isRemote) return;
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
        if (ac != null) {
            int sid = ac.getSeatIdByEntity(player);
            if (sid == 0) {
                ac.cs_dismountAll = this.dismountAll;
                ac.cs_heliAutoThrottleDown = this.heliAutoThrottleDown;
                ac.cs_planeAutoThrottleDown = this.planeAutoThrottleDown;
                ac.cs_tankAutoThrottleDown = this.tankAutoThrottleDown;
            }

            ac.camera.setShaderSupport(sid, this.shaderSupport);
        }
    }
}
