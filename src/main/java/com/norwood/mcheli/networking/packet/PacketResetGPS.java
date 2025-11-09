package com.norwood.mcheli.networking.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.weapon.GPSPosition;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;

@ElegantPacket
public class PacketResetGPS implements ClientToServerPacket {

    public final double targetPosX;
    public final double targetPosY;
    public final double targetPosZ;
    public final boolean isActive;

    public PacketResetGPS(double targetPosX, double targetPosY, double targetPosZ, boolean isActive) {
        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.targetPosZ = targetPosZ;
        this.isActive = isActive;
    }

    @Override
    public void onReceive(EntityPlayerMP player) {
        GPSPosition gpsPosition = new GPSPosition(targetPosX, targetPosY, targetPosZ);
        gpsPosition.isActive = isActive;
        gpsPosition.owner = player;
        GPSPosition.currentGPSPositions.put(player.getEntityId(), gpsPosition);
    }
}
