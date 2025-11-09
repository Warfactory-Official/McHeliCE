package com.norwood.mcheli.networking.packet;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.MCH_ServerSettings;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import com.norwood.mcheli.wrapper.W_Reflection;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

@ElegantPacket
public class PacketSyncServerSettings extends PacketBase implements ServerToClientPacket {

    public boolean enableCamDistChange = true;
    public boolean enableEntityMarker = true;
    public boolean enablePVP = true;
    public double stingerLockRange = 120.0;
    public boolean enableDebugBoundingBox = true;
    public boolean enableRotationLimit = false;
    public byte pitchLimitMax = 10;
    public byte pitchLimitMin = 10;
    public byte rollLimit = 35;

    public static void send(@Nullable EntityPlayerMP player) {
        var packet = new PacketSyncServerSettings();
        packet.enableCamDistChange = !MCH_Config.DisableCameraDistChange.prmBool;
        packet.enableEntityMarker = MCH_Config.DisplayEntityMarker.prmBool;
        packet.enablePVP = MCH_Utils.getServer().isPVPEnabled();
        packet.stingerLockRange = MCH_Config.StingerLockRange.prmDouble;
        packet.enableDebugBoundingBox = MCH_Config.EnableDebugBoundingBox.prmBool;
        packet.enableRotationLimit = MCH_Config.EnableRotationLimit.prmBool;
        packet.pitchLimitMax = (byte) MCH_Config.PitchLimitMax.prmInt;
        packet.pitchLimitMin = (byte) MCH_Config.PitchLimitMin.prmInt;
        packet.rollLimit = (byte) MCH_Config.RollLimit.prmInt;
        if (player != null) {
            packet.sendToPlayer(player);
        } else {
            packet.sendToClients();
        }
    }

    public static void sendAll() {
        send(null);
    }

    @Override
    public void onReceive(Minecraft mc) {
        if (!mc.player.world.isRemote) return;
        MCH_Lib.DbgLog(false, "onPacketNotifyServerSettings:" + mc.player);
        if (!this.enableCamDistChange) {
            W_Reflection.setThirdPersonDistance(4.0F);
        }

        MCH_ServerSettings.enableCamDistChange = this.enableCamDistChange;
        MCH_ServerSettings.enableEntityMarker = this.enableEntityMarker;
        MCH_ServerSettings.enablePVP = this.enablePVP;
        MCH_ServerSettings.stingerLockRange = this.stingerLockRange;
        MCH_ServerSettings.enableDebugBoundingBox = this.enableDebugBoundingBox;
        MCH_ServerSettings.enableRotationLimit = this.enableRotationLimit;
        MCH_ServerSettings.pitchLimitMax = this.pitchLimitMax;
        MCH_ServerSettings.pitchLimitMin = this.pitchLimitMin;
        MCH_ServerSettings.rollLimit = this.rollLimit;
        MCH_ClientLightWeaponTickHandler.lockRange = MCH_ServerSettings.stingerLockRange;
    }
}
