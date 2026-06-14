package com.norwood.mcheli.wingman.command;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.wingman.config.WingmanConfig;
import com.norwood.mcheli.networking.packet.PacketPlannerData;
import com.norwood.mcheli.wingman.util.McheliReflect;
import com.norwood.mcheli.wingman.wingman.WingmanEntry;
import com.norwood.mcheli.wingman.wingman.WingmanRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * /wingman <subcommand>
 *
 *   follow [uuid]             — assign nearest aircraft (or UUID) as wingman
 *   stop                      — remove all wingmen from your aircraft
 *   status                    — list all active wingmen
 *   dist <side> <alt> <rear>  — set formation distances at runtime
 *   maxwings <n>              — set max wingmen per aircraft
 *   engage [uuid]             — set MANUAL attack on UUID (or player's mount lock-target)
 *   auto                      — set AUTO attack (nearest hostile)
 *   hold                      — stop attacking, return to formation
 *   spawnuav [type]           — spawn UAV; omit type to list
 */
public class WingmanCommand extends CommandBase {

    private static final double SEARCH_RANGE_SQ = 512.0 * 512.0;

    @Override public String getName()                     { return "wingman"; }
    @Override public int    getRequiredPermissionLevel()  { return 0; }
    @Override public String getUsage(ICommandSender s)    {
        return "/wingman <follow|stop|status|dist|maxwings|engage|auto|hold|weapon|spawnuav> [args]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("§cPlayer-only command."));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (args.length == 0) { player.sendMessage(new TextComponentString("§7Usage: " + getUsage(sender))); return; }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "follow":   executeFollow(player, args);    break;
            case "stop":     executeStop(player);             break;
            case "status":   executeStatus(player);           break;
            case "dist":     executeDist(player, args);       break;
            case "maxwings": executeMaxWings(player, args);   break;
            case "engage":   executeEngage(player, args);     break;
            case "auto":     executeAuto(player);             break;
            case "hold":     executeHold(player);             break;
            case "weapon":   executeWeapon(player, args);     break;
            case "minalt":   executeMinAlt(player, args);     break;
            case "maxalt":   executeMaxAlt(player, args);     break;
            case "alt":      executeAlt(player, args);        break;
            case "spawnuav": executeSpawnUav(player, args);   break;
            case "marker":   executeMarker(player, args);     break;
            case "route":    executeRoute(player, args);      break;
            case "mission":  executeMission(player, server, args); break;
            case "order":    executeOrder(player, args);           break;
            case "gui":      executeGui(player);                   break;
            default: player.sendMessage(new TextComponentString("§7Usage: " + getUsage(sender)));
        }
    }

    // =========================================================================
    // follow [uuid]
    // =========================================================================

    private void executeFollow(EntityPlayerMP player, String[] args) {
        Entity leader = player.getRidingEntity();
        if (!McheliReflect.isAircraft(leader)) {
            player.sendMessage(new TextComponentString("§cYou must be inside a McHeli aircraft."));
            return;
        }
        int slot = WingmanRegistry.countForLeader(leader);
        if (slot >= WingmanConfig.maxWingmen) {
            player.sendMessage(new TextComponentString("§cMaximum " + WingmanConfig.maxWingmen + " wingmen per aircraft."));
            return;
        }
        Entity wingman;
        if (args.length >= 2) {
            UUID uid = parseUUID(player, args[1]);
            if (uid == null) return;
            wingman = ((WorldServer) player.world).getEntityFromUuid(uid);
            if (wingman == null) { player.sendMessage(new TextComponentString("§cEntity not found: " + args[1])); return; }
        } else {
            wingman = findNearestAircraft(player, leader);
            if (wingman == null) { player.sendMessage(new TextComponentString("§cNo unassigned aircraft found nearby.")); return; }
        }
        if (!McheliReflect.isAircraft(wingman)) { player.sendMessage(new TextComponentString("§cNot a McHeli aircraft.")); return; }
        if (wingman == leader) { player.sendMessage(new TextComponentString("§cCannot follow your own aircraft.")); return; }
        // プレイヤーが乗っている機体（直接またはヒットボックス経由）を除外
        if (wingman.isRidingOrBeingRiddenBy(player)) { player.sendMessage(new TextComponentString("§cCannot assign your own aircraft as wingman.")); return; }

        WingmanRegistry.put(wingman.getUniqueID(), new WingmanEntry(leader, slot));
        player.sendMessage(new TextComponentString("§aWingman assigned (slot " + slot + "): " + shortId(wingman)));
    }

    // =========================================================================
    // stop
    // =========================================================================

    private void executeStop(EntityPlayerMP player) {
        Entity leader = player.getRidingEntity();
        if (!McheliReflect.isAircraft(leader)) { player.sendMessage(new TextComponentString("§cNot inside a McHeli aircraft.")); return; }
        int before = WingmanRegistry.countForLeader(leader);
        WingmanRegistry.removeForLeader(leader);
        player.sendMessage(new TextComponentString("§aStopped " + before + " wingman(s)."));
    }

    // =========================================================================
    // status
    // =========================================================================

    private void executeStatus(EntityPlayerMP player) {
        Map<UUID, WingmanEntry> all = WingmanRegistry.snapshot();
        if (all.isEmpty()) { player.sendMessage(new TextComponentString("§7No active wingmen.")); return; }
        player.sendMessage(new TextComponentString(
                "§e=== Wingman Status (side=" + WingmanConfig.formationSideDist
                + " alt=" + WingmanConfig.formationAltOffset
                + " rear=" + WingmanConfig.formationRearDist + ") ==="));
        for (Map.Entry<UUID, WingmanEntry> e : all.entrySet()) {
            WingmanEntry entry = e.getValue();
            String leader = entry.leader != null ? entry.leader.getClass().getSimpleName() : "none";
            String atk = entry.attackMode != WingmanEntry.ATK_NONE
                    ? " atk=" + entry.attackMode + (entry.manualTargetId != null ? ":" + entry.manualTargetId.toString().substring(0, 8) : "")
                    : "";
            player.sendMessage(new TextComponentString(
                    "§7[" + e.getKey().toString().substring(0, 8) + "...] §f"
                    + "slot=" + entry.formationSlot + " state=" + entry.state
                    + " leader=" + leader + atk));
        }
    }

    // =========================================================================
    // dist <side> <alt> <rear>
    // =========================================================================

    private void executeDist(EntityPlayerMP player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(new TextComponentString(
                    "§7Usage: /wingman dist <side> <altitude> <rear>  (current: side="
                    + WingmanConfig.formationSideDist + " alt=" + WingmanConfig.formationAltOffset
                    + " rear=" + WingmanConfig.formationRearDist + ")"));
            return;
        }
        try {
            double side = Double.parseDouble(args[1]);
            double alt  = Double.parseDouble(args[2]);
            double rear = Double.parseDouble(args[3]);
            if (side < 0 || rear < 0) { player.sendMessage(new TextComponentString("§cSide and rear must be non-negative.")); return; }
            WingmanConfig.formationSideDist = side;
            WingmanConfig.formationAltOffset = alt;
            WingmanConfig.formationRearDist = rear;
            player.sendMessage(new TextComponentString("§aFormation: side=" + side + " alt=" + alt + " rear=" + rear));
        } catch (NumberFormatException ex) {
            player.sendMessage(new TextComponentString("§cInvalid number."));
        }
    }

    // =========================================================================
    // maxwings <n>
    // =========================================================================

    private void executeMaxWings(EntityPlayerMP player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponentString("§7Current max: " + WingmanConfig.maxWingmen));
            return;
        }
        try {
            int n = Integer.parseInt(args[1]);
            if (n < 1 || n > 64) { player.sendMessage(new TextComponentString("§cValue must be 1–64.")); return; }
            WingmanConfig.maxWingmen = n;
            player.sendMessage(new TextComponentString("§aMax wingmen set to " + n));
        } catch (NumberFormatException ex) {
            player.sendMessage(new TextComponentString("§cInvalid number."));
        }
    }

    // =========================================================================
    // engage [uuid]  — MANUAL attack
    // =========================================================================

    private void executeEngage(EntityPlayerMP player, String[] args) {
        Entity leader = player.getRidingEntity();
        List<Map.Entry<UUID, WingmanEntry>> wingmen = WingmanRegistry.snapshotForLeader(leader);
        if (wingmen.isEmpty()) { player.sendMessage(new TextComponentString("§cNo wingmen assigned to your aircraft.")); return; }

        UUID targetId = null;
        if (args.length >= 2) {
            targetId = parseUUID(player, args[1]);
            if (targetId == null) return;
        }

        for (Map.Entry<UUID, WingmanEntry> e : wingmen) {
            e.getValue().attackMode = WingmanEntry.ATK_MANUAL;
            e.getValue().manualTargetId = targetId;
        }
        String tStr = targetId != null ? targetId.toString().substring(0, 8) + "..." : "(player lock)";
        player.sendMessage(new TextComponentString("§aEngaging: " + wingmen.size() + " wingman(s) → target=" + tStr));
    }

    // =========================================================================
    // auto  — AUTO attack
    // =========================================================================

    private void executeAuto(EntityPlayerMP player) {
        Entity leader = player.getRidingEntity();
        List<Map.Entry<UUID, WingmanEntry>> wingmen = WingmanRegistry.snapshotForLeader(leader);
        if (wingmen.isEmpty()) { player.sendMessage(new TextComponentString("§cNo wingmen assigned.")); return; }
        for (Map.Entry<UUID, WingmanEntry> e : wingmen) {
            e.getValue().attackMode = WingmanEntry.ATK_AUTO;
            e.getValue().manualTargetId = null;
        }
        player.sendMessage(new TextComponentString("§aAuto-attack enabled for " + wingmen.size() + " wingman(s)."));
    }

    // =========================================================================
    // weapon [type|clear]  — 使用する武器種を指定 (McHeli weaponInfo.type)
    // =========================================================================

    private void executeWeapon(EntityPlayerMP player, String[] args) {
        Entity leader = player.getRidingEntity();
        List<Map.Entry<UUID, WingmanEntry>> wingmen = WingmanRegistry.snapshotForLeader(leader);
        if (wingmen.isEmpty()) { player.sendMessage(new TextComponentString("§cNo wingmen assigned.")); return; }

        if (args.length < 2) {
            String cur = wingmen.get(0).getValue().weaponType;
            player.sendMessage(new TextComponentString(
                "§7Current weapon type: " + (cur != null ? "§e" + cur : "§7(any — first available)")
                + "\n§7Usage: /wingman weapon <type>  or  /wingman weapon clear"));
            return;
        }

        String type = args[1].equalsIgnoreCase("clear") ? null : args[1];
        for (Map.Entry<UUID, WingmanEntry> e : wingmen) {
            e.getValue().weaponType = type;
        }
        if (type == null) {
            player.sendMessage(new TextComponentString("§aWeapon type cleared (any weapon)."));
        } else {
            player.sendMessage(new TextComponentString("§aWeapon type set to §e" + type + "§a for " + wingmen.size() + " wingman(s)."));
        }
    }

    // =========================================================================
    // hold  — stop attacking
    // =========================================================================

    private void executeHold(EntityPlayerMP player) {
        Entity leader = player.getRidingEntity();
        List<Map.Entry<UUID, WingmanEntry>> wingmen = WingmanRegistry.snapshotForLeader(leader);
        if (wingmen.isEmpty()) { player.sendMessage(new TextComponentString("§cNo wingmen assigned.")); return; }
        for (Map.Entry<UUID, WingmanEntry> e : wingmen) {
            e.getValue().attackMode = WingmanEntry.ATK_NONE;
            e.getValue().manualTargetId = null;
        }
        player.sendMessage(new TextComponentString("§aHold — attack stopped for " + wingmen.size() + " wingman(s)."));
    }

    // =========================================================================
    // minalt [value]  — minimum Y altitude during attack runs
    // =========================================================================

    private void executeMinAlt(EntityPlayerMP player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponentString(
                    "§7Current min attack altitude: §e" + WingmanConfig.minAttackAltitude
                    + "§7  (0 = no floor)\n§7Usage: /wingman minalt <y>"));
            return;
        }
        try {
            double val = Double.parseDouble(args[1]);
            if (val < 0) { player.sendMessage(new TextComponentString("§cValue must be >= 0.")); return; }
            WingmanConfig.minAttackAltitude = val;
            String msg = val == 0
                    ? "§aMin attack altitude cleared (no floor)."
                    : "§aMin attack altitude set to §eY=" + val + "§a.";
            player.sendMessage(new TextComponentString(msg));
        } catch (NumberFormatException ex) {
            player.sendMessage(new TextComponentString("§cInvalid number."));
        }
    }

    // =========================================================================
    // maxalt [value]  — maximum Y altitude during attack runs
    // =========================================================================

    private void executeMaxAlt(EntityPlayerMP player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponentString(
                    "§7Current max attack altitude: §e" + WingmanConfig.maxAttackAltitude
                    + "§7  (0 = no ceiling)\n§7Usage: /wingman maxalt <y>"));
            return;
        }
        try {
            double val = Double.parseDouble(args[1]);
            if (val < 0) { player.sendMessage(new TextComponentString("§cValue must be >= 0.")); return; }
            WingmanConfig.maxAttackAltitude = val;
            String msg = val == 0
                    ? "§aMax attack altitude cleared (no ceiling)."
                    : "§aMax attack altitude set to §eY=" + val + "§a.";
            player.sendMessage(new TextComponentString(msg));
        } catch (NumberFormatException ex) {
            player.sendMessage(new TextComponentString("§cInvalid number."));
        }
    }

    // =========================================================================
    // alt clear  — reset both minalt and maxalt
    // =========================================================================

    private void executeAlt(EntityPlayerMP player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            WingmanConfig.minAttackAltitude = 0.0;
            WingmanConfig.maxAttackAltitude = 0.0;
            player.sendMessage(new TextComponentString("§aAttack altitude limits cleared (min=0, max=0)."));
        } else {
            player.sendMessage(new TextComponentString(
                    "§7Current: min=§e" + WingmanConfig.minAttackAltitude
                    + "§7  max=§e" + WingmanConfig.maxAttackAltitude
                    + "§7\n§7Usage: /wingman alt clear"));
        }
    }

    // =========================================================================
    // spawnuav [type]
    // =========================================================================

    private void executeSpawnUav(EntityPlayerMP player, String[] args) {
        if (args.length < 2) { listUavTypes(player); return; }
        spawnUav(player, args[1]);
    }

    private void listUavTypes(EntityPlayerMP player) {
        try {
            List<String> planes = new ArrayList<>(), helis = new ArrayList<>();
            collectUavNames("plane", planes);
            collectUavNames("heli",  helis);
            if (planes.isEmpty() && helis.isEmpty()) {
                player.sendMessage(new TextComponentString("§7No UAV types found.")); return;
            }
            player.sendMessage(new TextComponentString("§eAvailable UAV types (/wingman spawnuav <type>):"));
            for (String n : planes) player.sendMessage(new TextComponentString("§7  [plane] " + n));
            for (String n : helis)  player.sendMessage(new TextComponentString("§7  [heli]  " + n));
        } catch (Exception ex) {
            player.sendMessage(new TextComponentString("§cFailed to list UAV types: " + ex.getMessage()));
        }
    }

    private void collectUavNames(String reg, List<String> out) {
        if ("plane".equals(reg)) {
            for (MCH_PlaneInfo info : ContentRegistries.plane().values()) {
                if (info.isUAV) out.add(info.name);
            }
        } else if ("heli".equals(reg)) {
            for (MCH_HeliInfo info : ContentRegistries.heli().values()) {
                if (info.isUAV) out.add(info.name);
            }
        }
    }

    private void spawnUav(EntityPlayerMP player, String typeName) {
        WorldServer ws = (WorldServer) player.world;

        // Spawn 8 blocks in front of the player at eye height
        double yawRad = Math.toRadians(player.rotationYaw);
        double spawnX = player.posX - Math.sin(yawRad) * 8;
        double spawnY = player.posY + 1;
        double spawnZ = player.posZ + Math.cos(yawRad) * 8;

        // Try plane, then helicopter — whichever has a matching type name.
        for (MCH_EntityAircraft entity : new MCH_EntityAircraft[]{
                new MCH_EntityPlane(ws), new MCH_EntityHeli(ws)}) {
            // setTypeName sets the ID_TYPE DataParameter (so writeSpawnData sends the correct
            // type name and the client calls changeType in readSpawnData → model loads) AND
            // invokes changeType(typeName) to populate acInfo.
            entity.setTypeName(typeName);

            MCH_AircraftInfo acInfo = entity.getAcInfo();
            if (acInfo == null) {
                // Type not found in this class — discard and try next
                entity.setDead();
                continue;
            }

            // Set the texture name from acInfo (first texture by default), mirroring the
            // item-based spawn path (MCP_ItemPlane / MCH_ItemHeli).
            entity.setTextureName(acInfo.getTextureName());

            entity.setLocationAndAngles(spawnX, spawnY, spawnZ, player.rotationYaw, 0);
            ws.spawnEntity(entity);

            player.sendMessage(new TextComponentString(
                    "§aSpawned: " + entity.getKindName() + " (" + entity.getUniqueID().toString().substring(0, 8) + "...)"));
            return;
        }
        player.sendMessage(new TextComponentString(
                "§cCould not spawn \"" + typeName + "\". Use /wingman spawnuav for valid types."));
    }

    // =========================================================================
    // marker <list|id|type> — WingmanMarkerBlock 管理
    // =========================================================================

    private void executeMarker(EntityPlayerMP player, String[] args) {
        WorldServer ws = (WorldServer) player.world;
        if (args.length < 2) {
            player.sendMessage(new TextComponentString("§7Usage: /wingman marker <list|id <id>|type <type>>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list": {
                List<com.norwood.mcheli.wingman.registry.MarkerRegistry.MarkerInfo> markers =
                    com.norwood.mcheli.wingman.registry.MarkerRegistry.snapshot(ws);
                if (markers.isEmpty()) { player.sendMessage(new TextComponentString("§7No markers registered.")); return; }
                player.sendMessage(new TextComponentString("§e=== Wingman Markers ==="));
                for (com.norwood.mcheli.wingman.registry.MarkerRegistry.MarkerInfo m : markers) {
                    player.sendMessage(new TextComponentString(
                        "§7[" + m.type.name() + "] §fid=" + (m.id.isEmpty() ? "(none)" : m.id)
                        + " §7@ " + m.pos.getX() + "," + m.pos.getY() + "," + m.pos.getZ()));
                }
                break;
            }
            case "id": {
                if (args.length < 3) { player.sendMessage(new TextComponentString("§7Usage: /wingman marker id <id>")); return; }
                // プレイヤーが見ているブロックを対象にする
                net.minecraft.util.math.RayTraceResult rt = player.rayTrace(8, 1.0f);
                if (rt == null || rt.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                    player.sendMessage(new TextComponentString("§cLook at a Wingman Marker block (within 8 blocks).")); return;
                }
                net.minecraft.util.math.BlockPos bp = rt.getBlockPos();
                com.norwood.mcheli.wingman.registry.MarkerRegistry.setId(ws, bp, args[2]);
                net.minecraft.tileentity.TileEntity te = ws.getTileEntity(bp);
                if (te instanceof com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity) {
                    ((com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity) te).setMarkerId(args[2]);
                    com.norwood.mcheli.wingman.registry.MarkerRegistry.register(ws, bp, (com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity) te);
                }
                player.sendMessage(new TextComponentString("§aMarker id set to §e" + args[2]));
                break;
            }
            case "type": {
                if (args.length < 3) {
                    player.sendMessage(new TextComponentString(
                        "§7Usage: /wingman marker type <parking|runway_a|runway_b|waypoint>"));
                    return;
                }
                net.minecraft.util.math.RayTraceResult rt2 = player.rayTrace(8, 1.0f);
                if (rt2 == null || rt2.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                    player.sendMessage(new TextComponentString("§cLook at a Wingman Marker block (within 8 blocks).")); return;
                }
                net.minecraft.util.math.BlockPos bp2 = rt2.getBlockPos();
                net.minecraft.tileentity.TileEntity te2 = ws.getTileEntity(bp2);
                if (!(te2 instanceof com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity)) {
                    player.sendMessage(new TextComponentString("§cNot a Wingman Marker block.")); return;
                }
                com.norwood.mcheli.wingman.block.MarkerType newType;
                try {
                    newType = com.norwood.mcheli.wingman.block.MarkerType.valueOf(args[2].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(new TextComponentString("§cUnknown type: " + args[2]
                        + " §7(parking / runway_a / runway_b / waypoint)"));
                    return;
                }
                com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity wte2 =
                    (com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity) te2;
                wte2.setMarkerType(newType);
                if (wte2.getMarkerId().isEmpty()) {
                    wte2.setMarkerId(com.norwood.mcheli.wingman.block.WingmanMarkerBlock.autoId(ws, newType));
                }
                com.norwood.mcheli.wingman.registry.MarkerRegistry.register(ws, bp2, wte2);
                player.sendMessage(new TextComponentString(
                    "§aMarker type set to " + newType.displayName()
                    + " §7id=§e" + wte2.getMarkerId()));
                break;
            }
            default:
                player.sendMessage(new TextComponentString("§7Usage: /wingman marker <list|id <id>|type <type>>"));
        }
    }

    // =========================================================================
    // route <create|list|delete|show> — ミッションルート管理
    // =========================================================================

    private void executeRoute(EntityPlayerMP player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponentString("§7Usage: /wingman route <create <name> <node...>|list|delete <name>|show <name>>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create": {
                if (args.length < 4) {
                    player.sendMessage(new TextComponentString(
                        "§7Usage: /wingman route create <name> <node> [node...]\n"
                        + "§7Nodes: flyto:x,y,z  takeoff:id  land:id  attack:radius  loiter:ticks  park:id"));
                    return;
                }
                String name = args[2];
                List<com.norwood.mcheli.wingman.mission.MissionNode> nodes = new ArrayList<>();
                for (int i = 3; i < args.length; i++) {
                    try { nodes.add(com.norwood.mcheli.wingman.mission.MissionNode.parse(args[i])); }
                    catch (Exception e) { player.sendMessage(new TextComponentString("§cInvalid node: " + args[i])); return; }
                }
                com.norwood.mcheli.wingman.mission.MissionPlan.put(name, nodes);
                player.sendMessage(new TextComponentString("§aRoute §e" + name + "§a saved (" + nodes.size() + " nodes)."));
                break;
            }
            case "list": {
                java.util.Set<String> names = com.norwood.mcheli.wingman.mission.MissionPlan.names();
                if (names.isEmpty()) { player.sendMessage(new TextComponentString("§7No routes defined.")); return; }
                player.sendMessage(new TextComponentString("§e=== Routes ==="));
                for (String n : names) player.sendMessage(new TextComponentString("§7  " + n));
                break;
            }
            case "delete": {
                if (args.length < 3) { player.sendMessage(new TextComponentString("§7Usage: /wingman route delete <name>")); return; }
                boolean removed = com.norwood.mcheli.wingman.mission.MissionPlan.remove(args[2]);
                player.sendMessage(removed
                    ? new TextComponentString("§aRoute §e" + args[2] + "§a deleted.")
                    : new TextComponentString("§cRoute not found: " + args[2]));
                break;
            }
            case "show": {
                if (args.length < 3) { player.sendMessage(new TextComponentString("§7Usage: /wingman route show <name>")); return; }
                List<com.norwood.mcheli.wingman.mission.MissionNode> nodes = com.norwood.mcheli.wingman.mission.MissionPlan.get(args[2]);
                if (nodes == null) { player.sendMessage(new TextComponentString("§cRoute not found: " + args[2])); return; }
                player.sendMessage(new TextComponentString("§e=== Route: " + args[2] + " ==="));
                for (int i = 0; i < nodes.size(); i++)
                    player.sendMessage(new TextComponentString("§7  [" + i + "] " + nodes.get(i)));
                break;
            }
            default:
                player.sendMessage(new TextComponentString("§7Usage: /wingman route <create|list|delete|show>"));
        }
    }

    // =========================================================================
    // mission <assign|abort|status> — ミッション割り当て・管理
    // =========================================================================

    private void executeMission(EntityPlayerMP player, MinecraftServer server, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponentString("§7Usage: /wingman mission <assign <uuid> <route>|abort [uuid]|status>"));
            return;
        }
        WorldServer ws = (WorldServer) player.world;
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "assign": {
                if (args.length < 4) {
                    player.sendMessage(new TextComponentString("§7Usage: /wingman mission assign <uuid> <route>"));
                    return;
                }
                UUID uid = parseUUID(player, args[2]);
                if (uid == null) return;
                Entity wingman = ws.getEntityFromUuid(uid);
                if (wingman == null) { player.sendMessage(new TextComponentString("§cEntity not found.")); return; }
                List<com.norwood.mcheli.wingman.mission.MissionNode> nodes = com.norwood.mcheli.wingman.mission.MissionPlan.get(args[3]);
                if (nodes == null) { player.sendMessage(new TextComponentString("§cRoute not found: " + args[3])); return; }

                com.norwood.mcheli.wingman.wingman.WingmanEntry entry = WingmanRegistry.get(uid);
                if (entry == null) {
                    // 未登録の場合は自律エントリとして登録
                    entry = new com.norwood.mcheli.wingman.wingman.WingmanEntry();
                    WingmanRegistry.put(uid, entry);
                }
                entry.mission      = nodes;
                entry.missionIndex = 0;
                entry.missionNodeTimer = 0;
                entry.autoState    = com.norwood.mcheli.wingman.mission.AutonomousState.ENROUTE;
                player.sendMessage(new TextComponentString(
                    "§aMission §e" + args[3] + "§a assigned to " + shortId(wingman) + " (" + nodes.size() + " nodes)."));
                break;
            }
            case "abort": {
                if (args.length >= 3) {
                    UUID uid = parseUUID(player, args[2]);
                    if (uid == null) return;
                    com.norwood.mcheli.wingman.wingman.WingmanEntry entry = WingmanRegistry.get(uid);
                    if (entry != null) {
                        entry.mission   = null;
                        entry.autoState = com.norwood.mcheli.wingman.mission.AutonomousState.NONE;
                    }
                    player.sendMessage(new TextComponentString("§aMission aborted for " + args[2]));
                } else {
                    // 全自律機を停止
                    int count = 0;
                    for (com.norwood.mcheli.wingman.wingman.WingmanEntry e : WingmanRegistry.snapshot().values()) {
                        if (e.isAutonomous()) { e.mission = null; e.autoState = com.norwood.mcheli.wingman.mission.AutonomousState.NONE; count++; }
                    }
                    player.sendMessage(new TextComponentString("§aAborted " + count + " autonomous mission(s)."));
                }
                break;
            }
            case "status": {
                boolean any = false;
                for (Map.Entry<UUID, com.norwood.mcheli.wingman.wingman.WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
                    com.norwood.mcheli.wingman.wingman.WingmanEntry entry = e.getValue();
                    if (!entry.isAutonomous()) continue;
                    any = true;
                    com.norwood.mcheli.wingman.mission.MissionNode cur = entry.currentNode();
                    player.sendMessage(new TextComponentString(
                        "§7[" + e.getKey().toString().substring(0, 8) + "] "
                        + "§fstate=" + entry.autoState
                        + " node=" + entry.missionIndex + "/" + entry.mission.size()
                        + " §7(" + (cur != null ? cur.toString() : "done") + ")"));
                }
                if (!any) player.sendMessage(new TextComponentString("§7No active autonomous missions."));
                break;
            }
            default:
                player.sendMessage(new TextComponentString("§7Usage: /wingman mission <assign|abort|status>"));
        }
    }

    // =========================================================================
    // order <dispatch|abort|status|park>  — MissionOrder 発令・管理
    //
    //   dispatch <uuid> <baseId> <CAP|CAS|...> <targetX> <targetZ> [options...]
    //   abort [uuid]
    //   status
    //   park <uuid> <parkingId>   — 駐機スポットを手動アサイン（ミッション前準備）
    // =========================================================================

    private void executeOrder(EntityPlayerMP player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponentString(
                "§7Usage: /wingman order <dispatch|abort|status|park> [args]"));
            return;
        }
        WorldServer ws = (WorldServer) player.world;
        switch (args[1].toLowerCase(Locale.ROOT)) {

            case "dispatch": {
                // /wingman order dispatch <uuid> <baseId> <types,csv> <targetX> <targetZ>
                //   [orbitRadius] [cruiseAlt] [strikePasses] [timeLimitMin]
                if (args.length < 7) {
                    player.sendMessage(new TextComponentString(
                        "§7Usage: /wingman order dispatch <uuid> <baseId> <types> <targetX> <targetZ>"
                        + " [orbitR] [alt] [passes] [min]"));
                    return;
                }
                UUID uid = parseUUID(player, args[2]);
                if (uid == null) return;
                Entity wingman = ws.getEntityFromUuid(uid);
                if (wingman == null) { player.sendMessage(new TextComponentString("§cEntity not found.")); return; }

                com.norwood.mcheli.wingman.mission.MissionOrder order = new com.norwood.mcheli.wingman.mission.MissionOrder();
                order.baseId = args[3];
                for (String t : args[4].split(",")) {
                    try { order.missionTypes.add(com.norwood.mcheli.wingman.mission.MissionType.valueOf(t.trim().toUpperCase())); }
                    catch (Exception ignored) {}
                }
                if (order.missionTypes.isEmpty()) {
                    player.sendMessage(new TextComponentString("§cNo valid mission types in: " + args[4]));
                    return;
                }
                try {
                    order.targetX = Double.parseDouble(args[5]);
                    order.targetZ = Double.parseDouble(args[6]);
                } catch (NumberFormatException ex) {
                    player.sendMessage(new TextComponentString("§cInvalid coordinates.")); return;
                }
                if (args.length > 7)  try { order.orbitRadius     = Double.parseDouble(args[7]); } catch (Exception ignored) {}
                if (args.length > 8)  try { order.cruiseAlt        = Double.parseDouble(args[8]); } catch (Exception ignored) {}
                if (args.length > 9)  try { order.strikePasses     = Integer.parseInt(args[9]);   } catch (Exception ignored) {}
                if (args.length > 10) try { order.timeLimitSeconds = Integer.parseInt(args[10]);  } catch (Exception ignored) {}

                // デフォルト武器をミッション種別から設定
                for (com.norwood.mcheli.wingman.mission.MissionType mt : order.missionTypes) {
                    order.weapons.addAll(mt.defaultWeapons());
                }

                com.norwood.mcheli.wingman.wingman.WingmanEntry entry = WingmanRegistry.get(uid);
                if (entry == null) {
                    entry = new com.norwood.mcheli.wingman.wingman.WingmanEntry();
                    WingmanRegistry.put(uid, entry);
                }
                entry.order                 = order;
                entry.orderTimer            = 0;
                entry.orbitAngle            = 0.0;
                entry.strikePassesRemaining = order.strikePasses;
                entry.reconMobCount         = 0;
                entry.rtbReason             = "";
                entry.autoState             = com.norwood.mcheli.wingman.mission.AutonomousState.NONE;
                entry.weaponType            = pickPrimaryWeapon(order);

                player.sendMessage(new TextComponentString(
                    "§aOrder dispatched: §e" + args[4]
                    + "§a to " + shortId(wingman)
                    + " §7(base=" + order.baseId + " target=" + (int)order.targetX + "," + (int)order.targetZ + ")"));
                break;
            }

            case "abort": {
                if (args.length >= 3) {
                    UUID uid = parseUUID(player, args[2]);
                    if (uid == null) return;
                    com.norwood.mcheli.wingman.wingman.WingmanEntry entry = WingmanRegistry.get(uid);
                    if (entry != null) {
                        entry.order     = null;
                        entry.autoState = com.norwood.mcheli.wingman.mission.AutonomousState.NONE;
                    }
                    player.sendMessage(new TextComponentString("§aOrder aborted for " + args[2].substring(0, 8)));
                } else {
                    int count = 0;
                    for (com.norwood.mcheli.wingman.wingman.WingmanEntry e : WingmanRegistry.snapshot().values()) {
                        if (e.hasOrder()) { e.order = null; e.autoState = com.norwood.mcheli.wingman.mission.AutonomousState.NONE; count++; }
                    }
                    player.sendMessage(new TextComponentString("§aAborted " + count + " order(s)."));
                }
                break;
            }

            case "status": {
                boolean any = false;
                for (Map.Entry<UUID, com.norwood.mcheli.wingman.wingman.WingmanEntry> e : WingmanRegistry.snapshot().entrySet()) {
                    com.norwood.mcheli.wingman.wingman.WingmanEntry entry = e.getValue();
                    if (!entry.hasOrder()) continue;
                    any = true;
                    com.norwood.mcheli.wingman.mission.MissionOrder o = entry.order;
                    player.sendMessage(new TextComponentString(
                        "§7[" + e.getKey().toString().substring(0, 8) + "] "
                        + "§fstate=" + entry.autoState
                        + " §7types=" + o.missionTypes
                        + " timer=" + entry.orderTimer / 20 + "s"
                        + " rtb=" + (entry.rtbReason.isEmpty() ? "-" : entry.rtbReason)));
                }
                if (!any) player.sendMessage(new TextComponentString("§7No active MissionOrders."));
                break;
            }

            case "park": {
                // /wingman order park <uuid> <parkingId>
                if (args.length < 4) {
                    player.sendMessage(new TextComponentString("§7Usage: /wingman order park <uuid> <parkingId>"));
                    return;
                }
                UUID uid = parseUUID(player, args[2]);
                if (uid == null) return;
                com.norwood.mcheli.wingman.wingman.WingmanEntry entry = WingmanRegistry.get(uid);
                if (entry == null) {
                    entry = new com.norwood.mcheli.wingman.wingman.WingmanEntry();
                    WingmanRegistry.put(uid, entry);
                }
                entry.assignedParkingId = args[3];
                entry.autoState = com.norwood.mcheli.wingman.mission.AutonomousState.PARKED;
                player.sendMessage(new TextComponentString(
                    "§aParking assigned: §e" + args[3] + "§a for " + args[2].substring(0, 8)));
                break;
            }

            default:
                player.sendMessage(new TextComponentString(
                    "§7Usage: /wingman order <dispatch|abort|status|park>"));
        }
    }

    /** GUN以外の最初の武器を主攻撃武器として返す。 */
    private String pickPrimaryWeapon(com.norwood.mcheli.wingman.mission.MissionOrder order) {
        for (String w : order.weapons) {
            if (!"gun".equals(w)) return w;
        }
        return null;
    }

    // =========================================================================
    // gui  — open Mission Planner GUI on the client
    // =========================================================================

    private void executeGui(EntityPlayerMP player) {
        WorldServer ws = (WorldServer) player.world;
        PacketPlannerData pkt = new PacketPlannerData();

        // Collect ALL McHeli aircraft in the loaded world
        Map<UUID, WingmanEntry> registry = WingmanRegistry.snapshot();
        for (Entity entity : new ArrayList<>(ws.loadedEntityList)) {
            if (!McheliReflect.isAircraft(entity)) continue;
            WingmanEntry entry = registry.get(entity.getUniqueID());
            PacketPlannerData.UavDto dto = new PacketPlannerData.UavDto();
            dto.uuid = entity.getUniqueID().toString();
            dto.name = getAircraftTypeName(entity);
            if (entry == null) {
                dto.state = "UNASSIGNED";
                dto.nodeIdx = 0;
                dto.nodeCount = 0;
            } else {
                dto.state = entry.isAutonomous() ? entry.autoState.name() : (entry.leader != null ? "FOLLOWING" : "IDLE");
                dto.nodeIdx = entry.missionIndex;
                dto.nodeCount = (entry.mission != null) ? entry.mission.size() : 0;
            }
            pkt.uavs.add(dto);
        }

        // Collect routes
        for (String name : com.norwood.mcheli.wingman.mission.MissionPlan.names()) {
            List<com.norwood.mcheli.wingman.mission.MissionNode> nodes = com.norwood.mcheli.wingman.mission.MissionPlan.get(name);
            PacketPlannerData.RouteDto dto = new PacketPlannerData.RouteDto();
            dto.name = name;
            if (nodes != null) {
                for (com.norwood.mcheli.wingman.mission.MissionNode n : nodes) dto.nodes.add(n.toString());
            }
            pkt.routes.add(dto);
        }

        // Collect markers
        for (com.norwood.mcheli.wingman.registry.MarkerRegistry.MarkerInfo m : com.norwood.mcheli.wingman.registry.MarkerRegistry.snapshot(ws)) {
            PacketPlannerData.MarkerDto dto = new PacketPlannerData.MarkerDto();
            dto.type = m.type.name();
            dto.id   = m.id;
            dto.x    = m.pos.getX();
            dto.y    = m.pos.getY();
            dto.z    = m.pos.getZ();
            pkt.markers.add(dto);
        }

        // Player position for "My Pos" button
        pkt.playerX = player.posX;
        pkt.playerY = player.posY;
        pkt.playerZ = player.posZ;

        pkt.sendToPlayer(player);
    }

    private String getAircraftTypeName(Entity entity) {
        if (entity == null) return "Unknown";
        // getTypeName() returns the model name (e.g. "MQ-9"), getKindName() returns kind (e.g. "plane")
        return McheliReflect.getAircraftName(entity);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Entity findNearestAircraft(EntityPlayerMP player, Entity leader) {
        double bestSq = SEARCH_RANGE_SQ;
        Entity best = null;
        for (Entity e : new ArrayList<>(player.world.loadedEntityList)) {
            if (e == leader || e == player) continue;
            if (!McheliReflect.isAircraft(e)) continue;
            if (WingmanRegistry.get(e.getUniqueID()) != null) continue;
            double dSq = leader.getDistanceSq(e);
            if (dSq < bestSq) { bestSq = dSq; best = e; }
        }
        return best;
    }

    private UUID parseUUID(EntityPlayerMP player, String s) {
        try { return UUID.fromString(s); }
        catch (IllegalArgumentException ex) {
            player.sendMessage(new TextComponentString("§cInvalid UUID: " + s));
            return null;
        }
    }

    private static String shortId(Entity e) {
        return e.getUniqueID().toString().substring(0, 8) + "...";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args,
                "follow", "stop", "status", "dist", "maxwings", "engage", "auto", "hold",
                "weapon", "minalt", "maxalt", "alt", "spawnuav",
                "marker", "route", "mission", "order", "gui");
        if (args.length == 2 && args[0].equalsIgnoreCase("alt"))
            return getListOfStringsMatchingLastWord(args, "clear");
        if (args.length == 2 && args[0].equalsIgnoreCase("weapon"))
            return getListOfStringsMatchingLastWord(args,
                    "gun", "cannon", "missile", "rocket", "bomb", "torpedo",
                    "machinegun1", "machinegun2", "cas",
                    "asmissile", "aamissile", "atmissile", "tvmissile",
                    "mkrocket", "targetingpod", "clear");
        if (args.length == 2 && args[0].equalsIgnoreCase("marker"))
            return getListOfStringsMatchingLastWord(args, "list", "id", "type");
        if (args.length == 3 && args[0].equalsIgnoreCase("marker") && args[1].equalsIgnoreCase("type"))
            return getListOfStringsMatchingLastWord(args, "parking", "runway_a", "runway_b", "waypoint");
        if (args.length == 2 && args[0].equalsIgnoreCase("route"))
            return getListOfStringsMatchingLastWord(args, "create", "list", "delete", "show");
        if (args.length == 2 && args[0].equalsIgnoreCase("mission"))
            return getListOfStringsMatchingLastWord(args, "assign", "abort", "status");
        if (args.length == 2 && args[0].equalsIgnoreCase("order"))
            return getListOfStringsMatchingLastWord(args, "dispatch", "abort", "status", "park");
        return Collections.emptyList();
    }
}
