package com.norwood.mcheli.multithread;

import com.norwood.mcheli.MCH_ClientProxy;
import com.norwood.mcheli.MCH_ModelManager;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helicopter.MCH_HeliInfoManager;
import com.norwood.mcheli.plane.MCP_PlaneInfo;
import com.norwood.mcheli.plane.MCP_PlaneInfoManager;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.tank.MCH_TankInfoManager;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfoManager;
import com.norwood.mcheli.weapon.MCH_DefaultBulletModels;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.norwood.mcheli.ship.MCH_ShipInfoManager;



public class MultiThreadModelManager {


    /**
     * <p> Starts the MultiThreaded model loading process. </p>
     * Called from {@link MCH_ClientProxy#registerModels()}. Essentially loads the models in the background.
     * Since this can be done at runtime, there's no issue with this.
     * The load is balanced during loading generally and into the first seconds into login.
     * As of now, there is no way to tell whether the process is done other than to check the log for the 5 prints.
     *
     * @param proxy The unique ClientProxy object that called this.
     */

    public static void start(MCH_ClientProxy proxy) {

        waitForData("helicopter", MCH_HeliInfoManager.map);
        waitForData("plane", MCP_PlaneInfoManager.map);
        waitForData("ship", MCH_ShipInfoManager.map);
        waitForData("tank", MCH_TankInfoManager.map);
        waitForData("vehicle", MCH_VehicleInfoManager.map);

        MCH_ModelManager.load("blocks", "drafting_table");

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        CompletableFuture<Void> heliFuture = CompletableFuture.runAsync(() ->
                MCH_HeliInfoManager.map.values().forEach(info ->
                        proxy.registerModelsHeli((MCH_HeliInfo) info, false)), executor
        ).thenRun(() -> completion("helicopter"));

        CompletableFuture<Void> planeFuture = CompletableFuture.runAsync(() ->
                MCP_PlaneInfoManager.map.values().forEach(info ->
                        proxy.registerModelsPlane((MCP_PlaneInfo) info, false)), executor
        ).thenRun(() -> completion("plane"));

        CompletableFuture<Void> shipFuture = CompletableFuture.runAsync(() ->
                MCH_ShipInfoManager.map.values().forEach(info ->
                        proxy.registerModelsShip((MCH_ShipInfo) info, false)), executor
        ).thenRun(() -> completion("ship"));

        CompletableFuture<Void> tankFuture = CompletableFuture.runAsync(() ->
                MCH_TankInfoManager.map.values().forEach(info ->
                        proxy.registerModelsTank((MCH_TankInfo) info, false)), executor
        ).thenRun(() -> completion("tank"));

        CompletableFuture<Void> vehicleFuture = CompletableFuture.runAsync(() ->
                MCH_VehicleInfoManager.map.values().forEach(info ->
                        proxy.registerModelsVehicle((MCH_VehicleInfo) info, false)), executor
        ).thenRun(() -> completion("vehicle"));

        CompletableFuture<Void> bulletFuture = CompletableFuture.runAsync(() -> {
            proxy.registerModels_Bullet();
            loadDefaultBulletModels(proxy);
        }, executor).thenRun(() -> completion("bullet"));

        CompletableFuture<Void> throwableFuture = CompletableFuture.runAsync(MCH_ClientProxy::registerModels_Throwable, executor)
                .thenRun(() -> completion("throwable"));

        // Wait for all tasks to complete before shutting down the executor
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                heliFuture, planeFuture, tankFuture, vehicleFuture, bulletFuture, throwableFuture
        );

        // Ensure proper shutdown after completion
        allTasks.thenRun(() -> {
            executor.shutdown();
            System.out.println("All model rendering tasks completed successfully.");
        }).exceptionally(ex -> {
            System.err.println("Error during model rendering: " + ex.getMessage());
            ex.printStackTrace();
            executor.shutdown();
            return null;
        });
    }
    private static void loadDefaultBulletModels(MCH_ClientProxy proxy) {
        proxy.registerModels_Bullet();
        MCH_DefaultBulletModels.Bullet = proxy.loadBulletModel("bullet");
        MCH_DefaultBulletModels.AAMissile = proxy.loadBulletModel("aamissile");
        MCH_DefaultBulletModels.ATMissile = proxy.loadBulletModel("asmissile");
        MCH_DefaultBulletModels.ASMissile = proxy.loadBulletModel("asmissile");
        MCH_DefaultBulletModels.Bomb = proxy.loadBulletModel("bomb");
        MCH_DefaultBulletModels.Rocket = proxy.loadBulletModel("rocket");
        MCH_DefaultBulletModels.Torpedo = proxy.loadBulletModel("torpedo");
    }

    private static void waitForData(String name, java.util.Map<?, ?> map) {
        int retries = 0;
        while (map.isEmpty() && retries < 500) { // 500 x 10ms = 5 seconds max
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
            retries++;
        }
        if (map.isEmpty()) {
            System.err.println("Warning: " + name + " map is still empty after waiting.");
        } else {
            System.out.println(name + " data ready with " + map.size() + " entries.");
        }
    }



    public static void completion(String type) {
        System.out.println("Successfully registered " + type + " models");
    }

}