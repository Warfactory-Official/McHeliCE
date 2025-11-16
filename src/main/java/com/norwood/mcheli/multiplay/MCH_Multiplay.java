package com.norwood.mcheli.multiplay;

import com.norwood.mcheli.MCH_Lib;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.entity.IEntitySinglePassenger;
import com.norwood.mcheli.networking.packet.PacketMarkPos;
import com.norwood.mcheli.networking.packet.PacketSpotEnemy;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.ship.MCH_EntityShip;
import com.norwood.mcheli.tank.MCH_EntityTank;
import com.norwood.mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.command.CommandException;
import net.minecraft.command.server.CommandScoreboard;
import net.minecraft.command.server.CommandTeleport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.*;

public class MCH_Multiplay {

    public static final MCH_TargetType[][] ENTITY_SPOT_TABLE = new MCH_TargetType[][] {
            { MCH_TargetType.NONE, MCH_TargetType.NONE },
            { MCH_TargetType.OTHER_MOB, MCH_TargetType.OTHER_MOB },
            { MCH_TargetType.MONSTER, MCH_TargetType.MONSTER },
            { MCH_TargetType.NONE, MCH_TargetType.NO_TEAM_PLAYER },
            { MCH_TargetType.NONE, MCH_TargetType.SAME_TEAM_PLAYER },
            { MCH_TargetType.NONE, MCH_TargetType.OTHER_TEAM_PLAYER },
            { MCH_TargetType.NONE, MCH_TargetType.NONE },
            { MCH_TargetType.NONE, MCH_TargetType.NO_TEAM_PLAYER },
            { MCH_TargetType.NONE, MCH_TargetType.SAME_TEAM_PLAYER },
            { MCH_TargetType.NONE, MCH_TargetType.OTHER_TEAM_PLAYER }
    };

    public static boolean canSpotEntityWithFilter(int filter, Entity entity) {
        if (entity instanceof MCH_EntityPlane || entity instanceof MCH_EntityShip) { // spaghetti
            return (filter & 32) != 0;
        } else if (entity instanceof MCH_EntityHeli) {
            return (filter & 16) != 0;
        } else if (entity instanceof MCH_EntityVehicle || entity instanceof MCH_EntityTank) {
            return (filter & 8) != 0;
        } else if (entity instanceof EntityPlayer) {
            return (filter & 4) != 0;
        } else if (!(entity instanceof EntityLivingBase)) {
            return false;
        } else {
            return isMonster(entity) ? (filter & 2) != 0 : (filter & 1) != 0;
        }
    }

    public static boolean isMonster(Entity entity) {
        return entity.getClass().toString().toLowerCase().contains("monster");
    }

    public static MCH_TargetType canSpotEntity(Entity user, double posX, double posY, double posZ, Entity target,
                                               boolean checkSee) {
        if (!(user instanceof EntityLivingBase spotter)) {
            return MCH_TargetType.NONE;
        } else {
            int col = spotter.getTeam() == null ? 0 : 1;
            int row = 0;
            if (target instanceof EntityLivingBase) {
                if (!isMonster(target)) {
                    row = 1;
                } else {
                    row = 2;
                }
            }

            if (spotter.getTeam() != null) {
                if (target instanceof EntityPlayer player) {
                    player.getTeam();
                    if (spotter.isOnSameTeam(player)) {
                        row = 4;
                    } else {
                        row = 5;
                    }
                } else if (target instanceof MCH_EntityAircraft ac) {
                    EntityPlayer rideEntity = ac.getFirstMountPlayer();
                    if (rideEntity == null) {
                        row = 6;
                    } else {
                        rideEntity.getTeam();
                        if (spotter.isOnSameTeam(rideEntity)) {
                            row = 8;
                        } else {
                            row = 9;
                        }
                    }
                }
            } else if (target instanceof EntityPlayer || target instanceof MCH_EntityAircraft) {
                row = 0;
            }

            MCH_TargetType ret = ENTITY_SPOT_TABLE[row][col];
            if (checkSee && ret != MCH_TargetType.NONE) {
                Vec3d vs = new Vec3d(posX, posY, posZ);
                Vec3d ve = new Vec3d(target.posX, target.posY + target.getEyeHeight(), target.posZ);
                RayTraceResult mop = target.world.rayTraceBlocks(vs, ve);
                if (mop != null && mop.typeOfHit == Type.BLOCK) {
                    ret = MCH_TargetType.NONE;
                }
            }

            return ret;
        }
    }

    public static boolean canAttackEntity(DamageSource ds, Entity target) {
        return canAttackEntity(ds.getTrueSource(), target);
    }

    public static boolean canAttackEntity(Entity attacker, Entity target) {
        if (attacker != null && target != null) {
            EntityPlayer attackPlayer = null;
            EntityPlayer targetPlayer = null;
            if (attacker instanceof EntityPlayer) {
                attackPlayer = (EntityPlayer) attacker;
            }

            if (target instanceof EntityPlayer) {
                targetPlayer = (EntityPlayer) target;
            } else if (target instanceof IEntitySinglePassenger &&
                    ((IEntitySinglePassenger) target).getRiddenByEntity() instanceof EntityPlayer) {
                        targetPlayer = (EntityPlayer) ((IEntitySinglePassenger) target).getRiddenByEntity();
                    }

            if (target instanceof MCH_EntityAircraft ac) {
                if (ac.getRiddenByEntity() instanceof EntityPlayer) {
                    targetPlayer = (EntityPlayer) ac.getRiddenByEntity();
                }
            }

            return attackPlayer == null || targetPlayer == null || attackPlayer.canAttackPlayer(targetPlayer);
        }

        return true;
    }

    public static void jumpSpawnPoint(EntityPlayer player) {
        MCH_Lib.DbgLog(false, "JumpSpawnPoint");
        CommandTeleport cmd = new CommandTeleport();
        if (cmd.checkPermission(MCH_Utils.getServer(), player)) {
            MinecraftServer minecraftServer = MCH_Utils.getServer();

            for (String playerName : minecraftServer.getPlayerList().getOnlinePlayerNames()) {
                try {
                    EntityPlayerMP jumpPlayer = CommandTeleport.getPlayer(minecraftServer, player, playerName);
                    BlockPos cc = null;
                    if (jumpPlayer.dimension == player.dimension) {
                        cc = jumpPlayer.getBedLocation(jumpPlayer.dimension);
                        cc = EntityPlayer.getBedSpawnLocation(minecraftServer.getWorld(jumpPlayer.dimension), cc, true);

                        if (cc == null) {
                            cc = jumpPlayer.world.provider.getRandomizedSpawnPoint();
                        }
                    }

                    if (cc != null) {
                        String[] cmdStr = new String[] {
                                playerName,
                                String.format("%.1f", cc.getX() + 0.5),
                                String.format("%.1f", cc.getY() + 0.1),
                                String.format("%.1f", cc.getZ() + 0.5)
                        };
                        cmd.execute(minecraftServer, player, cmdStr);
                    }
                } catch (CommandException var10) {
                    var10.printStackTrace();
                }
            }
        }
    }

    public static void shuffleTeam(EntityPlayer player) {
        Collection<ScorePlayerTeam> teams = player.world.getScoreboard().getTeams();
        int teamNum = teams.size();
        MCH_Lib.DbgLog(false, "ShuffleTeam:%d teams ----------", teamNum);
        if (teamNum > 0) {
            CommandScoreboard cmd = new CommandScoreboard();
            if (cmd.checkPermission(MCH_Utils.getServer(), player)) {
                List<String> list = Arrays.asList(MCH_Utils.getServer().getPlayerList().getOnlinePlayerNames());
                Collections.shuffle(list);
                ArrayList<String> listTeam = new ArrayList<>();

                for (ScorePlayerTeam o : teams) {
                    listTeam.add(o.getName());
                }

                Collections.shuffle(listTeam);
                int i = 0;

                for (int j = 0; i < list.size(); i++) {
                    listTeam.set(j, listTeam.get(j) + " " + list.get(i));
                    if (++j >= teamNum) {
                        j = 0;
                    }
                }

                for (String s : listTeam) {
                    String exe_cmd = "teams join " + s;
                    String[] process_cmd = exe_cmd.split(" ");
                    if (process_cmd.length > 3) {
                        MCH_Lib.DbgLog(false, "ShuffleTeam:" + exe_cmd);

                        try {
                            cmd.execute(MCH_Utils.getServer(), player, process_cmd);
                        } catch (CommandException var11) {
                            var11.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static boolean spotEntity(
                                     EntityLivingBase player,
                                     @Nullable MCH_EntityAircraft ac,
                                     double posX,
                                     double posY,
                                     double posZ,
                                     int targetFilter,
                                     float spotLength,
                                     int markTime,
                                     float angle) {
        boolean ret = false;
        if (!player.world.isRemote) {
            float acRoll = 0.0F;
            if (ac != null) {
                acRoll = ac.getRotRoll();
            }

            Vec3d vv = MCH_Lib.RotVec3(0.0, 0.0, 1.0, -player.rotationYaw, -player.rotationPitch, -acRoll);
            double tx = vv.x;
            double tz = vv.z;
            List<Entity> list = player.world
                    .getEntitiesWithinAABBExcludingEntity(player,
                            player.getEntityBoundingBox().grow(spotLength, spotLength, spotLength));
            List<Integer> entityList = new ArrayList<>();
            Vec3d pos = new Vec3d(posX, posY, posZ);

            for (Entity entity : list) {
                if (canSpotEntityWithFilter(targetFilter, entity)) {
                    MCH_TargetType stopType = canSpotEntity(player, posX, posY, posZ, entity, true);
                    if (stopType != MCH_TargetType.NONE && stopType != MCH_TargetType.SAME_TEAM_PLAYER) {
                        double dist = entity.getDistanceSq(pos.x, pos.y, pos.z);
                        if (dist > 1.0 && dist < spotLength * spotLength) {
                            double cx = entity.posX - pos.x;
                            double cy = entity.posY - pos.y;
                            double cz = entity.posZ - pos.z;
                            double h = MCH_Lib.getPosAngle(tx, tz, cx, cz);
                            double v = Math.atan2(cy, Math.sqrt(cx * cx + cz * cz)) * 180.0 / Math.PI;
                            v = Math.abs(v + player.rotationPitch);
                            if (h < angle * 2.0F && v < angle * 2.0F) {
                                entityList.add(entity.getEntityId());
                            }
                        }
                    }
                }
            }

            if (!entityList.isEmpty()) {
                int[] entityId = new int[entityList.size()];

                for (int ix = 0; ix < entityId.length; ix++) {
                    entityId[ix] = entityList.get(ix);
                }

                sendSpotedEntityListToSameTeam(player, markTime, entityId);
                ret = true;
            } else {
                ret = false;
            }
        }

        return ret;
    }

    public static void sendSpotedEntityListToSameTeam(EntityLivingBase player, int count, int[] entityId) {
        PlayerList svCnf = MCH_Utils.getServer().getPlayerList();

        for (EntityPlayerMP notifyPlayer : svCnf.getPlayers()) {
            if (player == notifyPlayer || player.isOnSameTeam(notifyPlayer)) {
                PacketSpotEnemy.send(notifyPlayer, count, entityId);

            }
        }
    }

    public static boolean markPoint(EntityPlayer player, double posX, double posY, double posZ) {
        Vec3d vs = new Vec3d(posX, posY, posZ);
        Vec3d ve = MCH_Lib.Rot2Vec3(player.rotationYaw, player.rotationPitch);
        ve = vs.add(ve.x * 300.0, ve.y * 300.0, ve.z * 300.0);
        RayTraceResult mop = player.world.rayTraceBlocks(vs, ve, true);
        if (mop != null && mop.typeOfHit == Type.BLOCK) {
            sendMarkPointToSameTeam(player, mop.getBlockPos().getX(), mop.getBlockPos().getY() + 2,
                    mop.getBlockPos().getZ());
            return true;
        } else {
            sendMarkPointToSameTeam(player, 0, 1000, 0);
            return false;
        }
    }

    public static void sendMarkPointToSameTeam(EntityPlayer player, int x, int y, int z) {
        PlayerList svCnf = MCH_Utils.getServer().getPlayerList();

        for (EntityPlayer notifyPlayer : svCnf.getPlayers()) {
            if (player == notifyPlayer || player.isOnSameTeam(notifyPlayer)) {
                new PacketMarkPos(x, y, z).sendToPlayer((EntityPlayerMP) player);
            }
        }
    }
}
