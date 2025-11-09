package com.norwood.mcheli.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.W_Entity;

import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

@ElegantPacket
public class PacketNotifyAmmoNum extends PacketBase implements ServerToClientPacket {

    public int entityID_Ac = -1;
    public boolean all = false;
    public byte weaponID = -1;
    public byte num = 0;
    public short[] ammo = new short[0];
    public short[] restAmmo = new short[0];

    public static void sendAllAmmoNum(MCH_EntityAircraft ac, EntityPlayer target) {
        var packet = new PacketNotifyAmmoNum();
        packet.entityID_Ac = W_Entity.getEntityId(ac);
        packet.all = true;
        packet.num = (byte) ac.getWeaponNum();
        packet.ammo = new short[packet.num];
        packet.restAmmo = new short[packet.num];

        for (int i = 0; i < packet.num; i++) {
            packet.ammo[i] = (short) ac.getWeapon(i).getAmmoNum();
            packet.restAmmo[i] = (short) ac.getWeapon(i).getRestAllAmmoNum();
        }

        send(packet, ac, target);
    }

    public static void sendAmmoNum(MCH_EntityAircraft ac, EntityPlayer target, int wid) {
        sendAmmoNum(ac, target, wid, ac.getWeapon(wid).getAmmoNum(), ac.getWeapon(wid).getRestAllAmmoNum());
    }

    public static void sendAmmoNum(MCH_EntityAircraft ac, EntityPlayer target, int wid, int ammo, int rest_ammo) {
        var packet = new PacketNotifyAmmoNum();
        packet.entityID_Ac = W_Entity.getEntityId(ac);
        packet.all = false;
        packet.weaponID = (byte) wid;
        packet.ammo = new short[] { (short) ammo };
        packet.restAmmo = new short[] { (short) rest_ammo };
        send(packet, ac, target);
    }

    public static void send(PacketNotifyAmmoNum packet, MCH_EntityAircraft ac, EntityPlayer target) {
        if (target == null) {
            for (int i = 0; i < ac.getSeatNum() + 1; i++) {
                Entity entity = ac.getEntityBySeatId(i);
                if (entity instanceof EntityPlayerMP) {
                    packet.sendToPlayer((EntityPlayerMP) entity);
                }
            }
        } else {
            packet.sendToPlayer((EntityPlayerMP) target);
        }
    }

    @Override
    public void onReceive(Minecraft mc) {
        var player = mc.player;
        if (player == null || !player.world.isRemote) return;

        if (this.entityID_Ac <= 0) return;

        Entity entity = player.world.getEntityByID(this.entityID_Ac);
        if (!(entity instanceof MCH_EntityAircraft ac)) return;

        StringBuilder msg = new StringBuilder("onPacketNotifyAmmoNum:");
        msg.append(ac.getAcInfo() != null ? ac.getAcInfo().displayName : "null").append(":");

        if (this.all) {
            msg.append("All=true, Num=").append(this.num);

            for (int i = 0; i < ac.getWeaponNum() && i < this.num; i++) {
                ac.getWeapon(i).setAmmoNum(this.ammo[i]);
                ac.getWeapon(i).setRestAllAmmoNum(this.restAmmo[i]);
                msg.append(", [").append(this.ammo[i]).append("/").append(this.restAmmo[i]).append("]");
            }

        } else if (this.weaponID < ac.getWeaponNum()) {
            msg.append("All=false, WeaponID=").append(this.weaponID)
                    .append(", ").append(this.ammo[0])
                    .append("/").append(this.restAmmo[0]);

            ac.getWeapon(this.weaponID).setAmmoNum(this.ammo[0]);
            ac.getWeapon(this.weaponID).setRestAllAmmoNum(this.restAmmo[0]);

        } else {
            msg.append("Error: WeaponID out of bounds: ").append(this.weaponID);
        }

        MCH_Lib.DbgLog(entity.world, msg.toString());
    }
}
