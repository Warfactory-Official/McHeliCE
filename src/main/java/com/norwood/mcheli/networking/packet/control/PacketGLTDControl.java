package com.norwood.mcheli.networking.packet.control;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.gltd.MCH_EntityGLTD;
import com.norwood.mcheli.networking.packet.PacketBase;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;

@ElegantPacket
public class PacketGLTDControl extends PacketBase implements ClientToServerPacket {

    public byte switchCameraMode = -1;
    public byte switchWeapon = -1;
    public boolean useWeapon = false;
    // TODO: See what can be done about raw IDs, perhaps forge like registry system
    public int useWeaponOption1 = 0;
    public int useWeaponOption2 = 0;
    public double useWeaponPosX = 0.0;
    public double useWeaponPosY = 0.0;
    public double useWeaponPosZ = 0.0;
    public boolean unmount = false;

    @Override
    public void onReceive(EntityPlayerMP player) {
        if (player.getRidingEntity() instanceof MCH_EntityGLTD gltd) {

            if (this.unmount) {
                Entity riddenByEntity = gltd.getRiddenByEntity();
                if (riddenByEntity != null) {
                    riddenByEntity.dismountRidingEntity();
                }
            } else {
                if (this.switchCameraMode >= 0) {
                    gltd.camera.setMode(0, this.switchCameraMode);
                }

                if (this.switchWeapon >= 0) {
                    gltd.switchWeapon(this.switchWeapon);
                }

                if (this.useWeapon) {
                    gltd.useCurrentWeapon(this.useWeaponOption1, this.useWeaponOption2);
                }
            }
        }
    }
}
