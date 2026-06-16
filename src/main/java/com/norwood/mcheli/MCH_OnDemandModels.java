package com.norwood.mcheli;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.wrapper.modelloader.MCH_LazyModel;
import com.norwood.mcheli.wrapper.modelloader.ModelVBO;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SideOnly(Side.CLIENT)
public final class MCH_OnDemandModels {

    private enum State {UNLOADED, LOADING, LOADED}

    private static final class Entry {
        final MCH_AircraftInfo info;
        final String mapKey;            // MCH_ModelManager key for the body
        final Consumer<Boolean> loader; // runs registerModelsX(info, reload): parse + schedule upload
        volatile State state = State.UNLOADED;
        volatile long lastUsedMs;
        int idleSweeps;

        Entry(MCH_AircraftInfo info, String mapKey, Consumer<Boolean> loader) {
            this.info = info;
            this.mapKey = mapKey;
            this.loader = loader;
        }
    }

    /** Keyed by the shared per-vehicle  instance. */
    private static final Map<MCH_AircraftInfo, Entry> ENTRIES = new ConcurrentHashMap<>();


    private static final ExecutorService LOADER = createLoader();

    private static ExecutorService createLoader() {
        try {
            Method m = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            ExecutorService vt = (ExecutorService) m.invoke(null);
            MCH_Logger.log("[OnDemandModels] model loader: virtual threads (per-task)");
            return vt;
        } catch (Throwable noVirtualThreads) {
            int n = fallbackThreads();
            MCH_Logger.log("[OnDemandModels] model loader: fixed pool of " + n + " (virtual threads unavailable)");
            return Executors.newFixedThreadPool(n, daemonFactory());
        }
    }

    private static int fallbackThreads() {
        int c = MCH_Config.ExperimentalOnDemandModelLoaderThreads != null
                ? MCH_Config.ExperimentalOnDemandModelLoaderThreads.prmInt : 0;
        return c > 0 ? c : Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    }

    private static ThreadFactory daemonFactory() {
        AtomicInteger idx = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "MCHeli-OnDemandModelLoader-" + idx.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    private static final int SWEEP_INTERVAL_TICKS = 200;
    private static final long SWEEP_INTERVAL_MS = SWEEP_INTERVAL_TICKS * 50L;

    private static int tickCounter;

    public MCH_OnDemandModels() {
    }

    public static boolean isEnabled() {
        return MCH_Config.ExperimentalAsyncOnDemandModelLoading != null
                && MCH_Config.ExperimentalAsyncOnDemandModelLoading.prmBool;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private static long lifetimeMs() {
        int s = MCH_Config.ExperimentalOnDemandModelLifetimeSeconds.prmInt;
        return s < 0 ? -1L : s * 1000L;
    }


    public static void install(MCH_AircraftInfo info, String mapKey, Consumer<Boolean> loader) {
        if (info == null) {
            return;
        }
        ENTRIES.put(info, new Entry(info, mapKey, loader));
        info.model = new MCH_LazyModel(info);
    }


    public static void notifyRendered(MCH_AircraftInfo info) {
        if (info == null) {
            return;
        }
        Entry e = ENTRIES.get(info);
        if (e == null) {
            return;
        }
        e.lastUsedMs = now();
        e.idleSweeps = 0;
        if (e.state == State.UNLOADED) {
            e.state = State.LOADING;
            submitLoad(e);
        }
    }

    private static void submitLoad(Entry e) {
        LOADER.execute(() -> {
            boolean ok = true;
            try {

                e.loader.accept(true);
            } catch (Throwable t) {
                ok = false;
                MCH_Logger.error("[OnDemandModels] load failed for " + e.mapKey, t);
            }
            final boolean success = ok;
            Minecraft.getMinecraft().addScheduledTask(() -> e.state = success ? State.LOADED : State.UNLOADED);
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isEnabled()) {
            return;
        }
        if (++tickCounter < SWEEP_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        sweep();
    }

    private static void sweep() {
        long life = lifetimeMs();
        if (life < 0) {
            return; // eviction disabled (-1)
        }
        int maxIdleSweeps = (int) Math.max(1, Math.ceil((double) life / SWEEP_INTERVAL_MS));
        long now = now();
        for (Entry e : ENTRIES.values()) {
            if (e.state != State.LOADED) {
                continue;
            }
            boolean usedSinceLastSweep = (now - e.lastUsedMs) < SWEEP_INTERVAL_MS;
            if (usedSinceLastSweep) {
                e.idleSweeps = 0;
            } else if (++e.idleSweeps >= maxIdleSweeps) {
                evict(e);
            }
        }
    }

    private static void evict(Entry e) {
        e.state = State.UNLOADED;
        e.idleSweeps = 0;
        MCH_AircraftInfo info = e.info;
        if (info.model instanceof ModelVBO mv) {
            mv.delete();
        }
        MCH_ModelManager.remove(e.mapKey);
        info.model = new MCH_LazyModel(info);
        if (MCH_Config.DebugLog) {
            MCH_Logger.debug("[OnDemandModels] evicted {}", e.mapKey);
        }
    }


    public static void onReloadAircraft(Runnable reinstaller) {
        if (!isEnabled()) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ModelVBO.deleteAll();
            ENTRIES.clear();
            if (reinstaller != null) {
                reinstaller.run();
            }
        });
    }
}
