package com.norwood.mcheli.weapon;

import com.norwood.mcheli.networking.packet.PacketResetGPS;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class GPSPosition {

    public static Map<Integer, GPSPosition> currentGPSPositions = new HashMap<>();

    public static GPSPosition currentClientGPSPosition = new GPSPosition(0, 0, 0);

    public double x, y, z;
    public Entity owner;
    public boolean isActive = false;

    public GPSPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void set(double x, double y, double z, boolean isActive, Entity owner) {
        if (owner.world.isRemote) {
            clientSet(x, y, z, isActive, owner);
            new PacketResetGPS(x, y, z, isActive).sendToServer();
        }
    }

    public static GPSPosition get(Entity owner) {
        return currentGPSPositions.get(owner.getEntityId());
    }

    @SideOnly(Side.CLIENT)
    public static void clientSet(double x, double y, double z, boolean isActive, Entity owner) {
        currentClientGPSPosition.x = x;
        currentClientGPSPosition.y = y;
        currentClientGPSPosition.z = z;
        currentClientGPSPosition.isActive = isActive;
        currentClientGPSPosition.owner = owner;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
