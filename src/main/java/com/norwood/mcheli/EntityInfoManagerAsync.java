package com.norwood.mcheli;

import com.norwood.mcheli.flare.MCH_EntityChaff;
import com.norwood.mcheli.helicopter.MCH_EntityHeli;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.networking.packet.PacketEntityInfoSync;
import com.norwood.mcheli.plane.MCH_EntityPlane;
import com.norwood.mcheli.weapon.MCH_IEntityLockChecker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EntityInfoManagerAsync {

    public static final Map<Integer, EntityInfo> serverEntities = new ConcurrentHashMap<>();
    private final ExecutorService radarExecutor;
    private int tickCounter;
    private long snapshotSeq = 0L;

    public EntityInfoManagerAsync() {
        FMLCommonHandler.instance().bus().register(this);
        this.radarExecutor = initializeExecutor();
    }

    /**
     * Attempts to use Java 21+ Virtual Threads.
     * Falls back to a Fixed Thread Pool based on CPU cores if on Java 8-20.
     */
    private ExecutorService initializeExecutor() {
        try {
            Method newVirtualMethod = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            MCH_Logger.info("Java 21+ detected. Initializing Virtual Threads for Radar System.");
            return (ExecutorService) newVirtualMethod.invoke(null);
        } catch (Exception e) {
            int threads = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 4);
            MCH_Logger.info("Virtual Threads not available. Using Fixed Thread Pool (" + threads + " threads) for Radar.");
            return Executors.newFixedThreadPool(threads);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            if (tickCounter % 10 == 0) {
                snapshotSeq++;
                dispatchAsyncRadarSync();
            }
        }
    }

    private void dispatchAsyncRadarSync() {
        MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (mcServer == null) return;


        List<PlayerSnapshot> players = new ArrayList<>();
        List<VehicleSnapshot> vehicles = new ArrayList<>();

        for (WorldServer world : mcServer.worlds) {
            for (Entity entity : world.loadedEntityList) {
                if (entity instanceof EntityPlayerMP player) {
                    players.add(new PlayerSnapshot(player, player.posX, player.posZ));
                }

                if (shouldTrack(world, entity)) {
                    EntityInfo info = EntityInfo.createInfo(entity);
                    serverEntities.put(entity.getEntityId(), info); // Update cache
                    vehicles.add(new VehicleSnapshot(info, entity.posX, entity.posZ));
                }
            }
        }

        cleanupCache(mcServer);

        final long currentSeq = this.snapshotSeq;

        for (PlayerSnapshot player : players) {
            radarExecutor.submit(() -> computeAndSendRadar(player, vehicles, currentSeq));
        }
    }


    private void computeAndSendRadar(PlayerSnapshot player, List<VehicleSnapshot> vehicles, long seq) {
        List<EntityInfo> visibleEntities = new ArrayList<>();
        double maxDistSq = 2000.0 * 2000.0; // 2000 block range squared

        for (VehicleSnapshot vehicle : vehicles) {
            double dx = player.x - vehicle.x;
            double dz = player.z - vehicle.z;

            if ((dx * dx + dz * dz) <= maxDistSq) {
                visibleEntities.add(vehicle.info);
            }
        }

        if (!visibleEntities.isEmpty()) {
            new PacketEntityInfoSync(visibleEntities, seq).
                    sendToPlayer(player.playerMP);
        }
    }

    private void cleanupCache(MinecraftServer mcServer) {
        serverEntities.entrySet().removeIf(entry -> {
            EntityInfo info = entry.getValue();
            Entity entity = getEntityFromAnyWorld(mcServer, info.entityId);
            return entity == null || entity.isDead || (System.currentTimeMillis() - info.lastUpdateTime > 5_000L);
        });
    }


    private Entity getEntityFromAnyWorld(MinecraftServer mcServer, int entityId) {
        for (WorldServer world : mcServer.worlds) {
            Entity entity = world.getEntityByID(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }


    private boolean shouldTrack(WorldServer world, Entity entity) {
        if (entity.isDead) return false;

        boolean isInterest =
                entity instanceof EntityPlayer ||
                entity instanceof MCH_IEntityLockChecker;

        if (!isInterest) return false;

        if (entity instanceof MCH_EntityPlane || entity instanceof MCH_EntityHeli || entity instanceof MCH_EntityChaff) {

            BlockPos pos = new BlockPos(entity.posX, 0, entity.posZ);
            int heightAboveGround = (int) (entity.posY - world.getHeight(pos).getY());

            if (heightAboveGround < 30) return false;

            double velocitySq = (entity.motionX * entity.motionX) +
                    (entity.motionY * entity.motionY) +
                    (entity.motionZ * entity.motionZ);

            if (velocitySq < 0.25) return false; // 0.5 * 0.5
        }

        return true;
    }


    private record PlayerSnapshot(EntityPlayerMP playerMP, double x, double z) {
    }

    private record VehicleSnapshot(EntityInfo info, double x, double z) {
    }
}
