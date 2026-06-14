package com.norwood.mcheli.networking.packet;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.wingman.mission.AutonomousState;
import com.norwood.mcheli.wingman.mission.MissionOrder;
import com.norwood.mcheli.wingman.mission.MissionType;
import com.norwood.mcheli.wingman.mission.TaxiRoute;
import com.norwood.mcheli.wingman.registry.TaxiRouteRegistry;
import com.norwood.mcheli.wingman.util.McheliReflect;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client → Server: save/delete a taxi route, or dispatch a mission order.
 */
@ElegantPacket
public class PacketBaseAction implements ClientToServerPacket {

    public static final int SAVE_ROUTE     = 0;
    public static final int DELETE_ROUTE   = 1;
    public static final int DISPATCH_ORDER = 2;

    public int action;

    // SAVE_ROUTE / DELETE_ROUTE
    public String routeId = "", baseId = "", parkingId = "", runwayId = "", runwayBId = "";
    public String waypointsCsv = "";
    public String arrivalWaypointsCsv = "";
    public String arrivalRunwayId = "";
    public int parkingHeading = -1;

    // DISPATCH_ORDER
    public String wingmanUuid = "";
    public String missionTypesCsv = "";
    public String weaponsCsv = "";
    public double targetX = 0, targetZ = 0, orbitRadius = 300, cruiseAlt = 80;
    public int    strikePasses = 2, timeLimitSeconds = 600;
    public String ferryDestBase = "";
    public boolean orbitAttack = false;
    public boolean useVstol    = false;
    public String arrivalRouteId = "";

    public PacketBaseAction() {}

    @Override
    public void onReceive(EntityPlayerMP player) {
        WorldServer ws = player.getServerWorld();
        ws.addScheduledTask(() -> handle(player, ws));
    }

    private void handle(EntityPlayerMP player, WorldServer ws) {
        switch (this.action) {
            case SAVE_ROUTE:     handleSaveRoute(player, ws);     break;
            case DELETE_ROUTE:   handleDeleteRoute(player, ws);   break;
            case DISPATCH_ORDER: handleDispatchOrder(player, ws); break;
        }
    }

    private void handleSaveRoute(EntityPlayerMP player, WorldServer ws) {
        if (this.routeId.isEmpty() || this.baseId.isEmpty()
                || this.parkingId.isEmpty() || this.runwayId.isEmpty()) {
            player.sendMessage(new TextComponentString(
                "§cRoute requires routeId, baseId, parkingId, runwayId."));
            return;
        }
        List<String> wps = new ArrayList<>();
        if (!this.waypointsCsv.isEmpty()) {
            for (String s : this.waypointsCsv.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) wps.add(t);
            }
        }
        List<String> arrWps = new ArrayList<>();
        if (!this.arrivalWaypointsCsv.isEmpty()) {
            for (String s : this.arrivalWaypointsCsv.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) arrWps.add(t);
            }
        }
        TaxiRoute route = new TaxiRoute(
            this.routeId, this.baseId, this.parkingId, this.runwayId, this.runwayBId, wps,
            arrWps, this.parkingHeading, this.arrivalRunwayId);
        TaxiRouteRegistry.save(ws, route);
        player.sendMessage(new TextComponentString("§aRoute §e" + this.routeId + "§a saved."));
    }

    private void handleDeleteRoute(EntityPlayerMP player, WorldServer ws) {
        TaxiRouteRegistry.delete(ws, this.routeId);
        player.sendMessage(new TextComponentString("§aRoute §e" + this.routeId + "§a deleted."));
    }

    private void handleDispatchOrder(EntityPlayerMP player, WorldServer ws) {
        UUID uid;
        try {
            uid = UUID.fromString(this.wingmanUuid);
        } catch (Exception e) {
            player.sendMessage(new TextComponentString("§cInvalid UUID: " + this.wingmanUuid));
            return;
        }
        Entity wingman = ws.getEntityFromUuid(uid);
        if (wingman == null) {
            player.sendMessage(new TextComponentString("§cEntity not found."));
            return;
        }

        MissionOrder order = new MissionOrder();
        order.baseId            = this.baseId;
        order.targetX           = this.targetX;
        order.targetZ           = this.targetZ;
        order.orbitRadius       = this.orbitRadius;
        order.cruiseAlt         = this.cruiseAlt;
        order.strikePasses      = this.strikePasses;
        order.timeLimitSeconds  = this.timeLimitSeconds;
        order.ferryDestBase     = this.ferryDestBase;
        order.orbitAttack       = this.orbitAttack;
        order.useVstol          = this.useVstol;
        order.arrivalRouteId    = this.arrivalRouteId;

        for (String s : this.missionTypesCsv.split(",")) {
            try { order.missionTypes.add(MissionType.valueOf(s.trim().toUpperCase())); }
            catch (Exception ignored) {}
        }
        for (String s : this.weaponsCsv.split(",")) {
            String t = s.trim().toLowerCase();
            if (!t.isEmpty()) order.weapons.add(t);
        }

        if (order.missionTypes.isEmpty()) {
            player.sendMessage(new TextComponentString("§cNo valid mission types."));
            return;
        }

        // Fill fuel before dispatch so a freshly-spawned craft (fuel=0) does not RTB instantly.
        McheliReflect.fillFuel(wingman);

        WingmanEntry entry = WingmanRegistry.get(uid);
        if (entry == null) {
            entry = new WingmanEntry();
            WingmanRegistry.put(uid, entry);
        }
        entry.order                  = order;
        entry.orderTimer             = 0;
        entry.orbitAngle             = 0.0;
        entry.strikePassesRemaining  = order.strikePasses;
        entry.reconMobCount          = 0;
        entry.rtbReason              = "";
        entry.attackMode             = WingmanEntry.ATK_NONE;
        entry.diagTick               = 0;
        entry.autoState              = AutonomousState.NONE;
        entry.vtolHoverMode = false;
        entry.vtolOnSent    = true;

        if (!this.routeId.isEmpty()) {
            TaxiRoute route = TaxiRouteRegistry.findById(ws, this.routeId);
            if (route != null) {
                entry.assignedParkingId = route.parkingId;
                entry.departureRouteId  = this.routeId;
                player.sendMessage(new TextComponentString(
                    "§aTaxi route §e" + route.routeId + "§a → parking §e" + route.parkingId));
            } else {
                player.sendMessage(new TextComponentString(
                    "§cRoute not found: " + this.routeId + " — airborne start"));
                entry.assignedParkingId = "";
                entry.departureRouteId  = "";
            }
        } else {
            entry.assignedParkingId = "";
            entry.departureRouteId  = "";
        }
        entry.weaponType = pickPrimaryWeapon(order);

        player.sendMessage(new TextComponentString(
            "§aOrder dispatched: §e" + this.missionTypesCsv
            + "§a → " + this.wingmanUuid.substring(0, 8) + "..."));
    }

    /** First non-gun weapon as the primary attack weapon; null (all weapons) if gun-only. */
    private String pickPrimaryWeapon(MissionOrder order) {
        for (String w : order.weapons) {
            if (!"gun".equals(w)) return w;
        }
        return null;
    }
}
