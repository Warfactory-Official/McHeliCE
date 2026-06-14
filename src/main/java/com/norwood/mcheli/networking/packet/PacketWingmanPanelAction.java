package com.norwood.mcheli.networking.packet;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.config.WingmanConfig;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * Client → Server: wingman-panel actions.
 */
@ElegantPacket
public class PacketWingmanPanelAction implements ClientToServerPacket {

    public static final int FOLLOW    = 0;
    public static final int STOP      = 1;
    public static final int STOP_ALL  = 2;
    public static final int AUTO      = 3;
    public static final int HOLD      = 4;
    public static final int WEAPON    = 5;
    public static final int FORMATION = 6;

    public int    action      = 0;
    public String uuid        = "";
    public String extra       = "";

    // FORMATION
    public double sideDist  = 20.0;
    public double altOffset = 0.0;
    public double rearDist  = 30.0;
    public int    maxWings  = 4;
    public double minAlt    = 0.0;
    public double maxAlt    = 0.0;

    public PacketWingmanPanelAction() {}

    @Override
    public void onReceive(EntityPlayerMP player) {
        WorldServer ws = (WorldServer) player.world;
        ws.addScheduledTask(() -> handle(player, ws));
    }

    private void handle(EntityPlayerMP player, WorldServer ws) {
        switch (this.action) {

            case FOLLOW: {
                Entity leader = player.getRidingEntity();
                if (leader == null) break;
                UUID uid = tryParseUUID(this.uuid);
                if (uid == null) break;
                Entity wingman = ws.getEntityFromUuid(uid);
                if (wingman == null) break;
                int slot = WingmanRegistry.countForLeader(leader);
                if (slot >= WingmanConfig.maxWingmen) break;
                WingmanRegistry.put(uid, new WingmanEntry(leader, slot));
                break;
            }

            case STOP: {
                UUID uid = tryParseUUID(this.uuid);
                if (uid == null) break;
                WingmanRegistry.remove(uid);
                break;
            }

            case STOP_ALL: {
                Entity leader = player.getRidingEntity();
                if (leader == null) break;
                WingmanRegistry.removeForLeader(leader);
                break;
            }

            case AUTO: {
                UUID uid = tryParseUUID(this.uuid);
                if (uid == null) break;
                WingmanEntry e = WingmanRegistry.get(uid);
                if (e != null) {
                    e.attackMode = WingmanEntry.ATK_AUTO;
                    e.manualTargetId = null;
                }
                break;
            }

            case HOLD: {
                UUID uid = tryParseUUID(this.uuid);
                if (uid == null) break;
                WingmanEntry e = WingmanRegistry.get(uid);
                if (e != null) {
                    e.attackMode = WingmanEntry.ATK_NONE;
                    e.manualTargetId = null;
                }
                break;
            }

            case WEAPON: {
                UUID uid = tryParseUUID(this.uuid);
                if (uid == null) break;
                WingmanEntry e = WingmanRegistry.get(uid);
                if (e != null) {
                    e.weaponType = this.extra.isEmpty() ? null : this.extra;
                }
                break;
            }

            case FORMATION: {
                WingmanConfig.formationSideDist   = this.sideDist;
                WingmanConfig.formationAltOffset  = this.altOffset;
                WingmanConfig.formationRearDist   = this.rearDist;
                WingmanConfig.maxWingmen          = Math.max(1, Math.min(64, this.maxWings));
                WingmanConfig.minAttackAltitude   = Math.max(0, this.minAlt);
                WingmanConfig.maxAttackAltitude   = Math.max(0, this.maxAlt);
                break;
            }
        }
    }

    private static UUID tryParseUUID(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
