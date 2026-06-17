package com.norwood.mcheli.wingman;
//WINGMAN — file introduced for the McHeli Wingman feature merge

import com.norwood.mcheli.helper.MCH_Blocks;
import com.norwood.mcheli.helper.MCH_Items;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.wingman.block.WingmanMarkerBlock;
import com.norwood.mcheli.wingman.block.WingmanMarkerTileEntity;
import com.norwood.mcheli.wingman.command.WingmanCommand;
import com.norwood.mcheli.wingman.config.WingmanConfig;
import com.norwood.mcheli.wingman.handler.AutonomousFlightHandler;
import com.norwood.mcheli.wingman.handler.ChunkLoadHandler;
import com.norwood.mcheli.wingman.handler.ProjectileChunkLoader;
import com.norwood.mcheli.wingman.handler.UavChunkStreamer;
import com.norwood.mcheli.wingman.handler.WingmanTickHandler;
import com.norwood.mcheli.wingman.mission.MissionPlan;
import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Bootstrap + shared state for the Wingman subsystem (formerly the standalone
 * McHeli-Wingman addon). It is no longer a separate {@code @Mod}; CE's
 * {@code MCH_MOD} drives its lifecycle by calling {@link #preInit}, {@link #init}
 * and {@link #registerCommand} from the matching CE phases. Client-only
 * registration (block item model, keybind, client handlers) happens in
 * {@link com.norwood.mcheli.wingman.client.WingmanClientReg}.
 */
public final class McHeliWingman {

    public static final Logger logger = LogManager.getLogger("McHeliWingman");

    /**
     * The owning {@code @Mod} instance (CE's {@code MCH_MOD}). Used as the mod
     * object handle for {@link ForgeChunkManager} ticket requests.
     */
    public static Object instance;

    public static WingmanMarkerBlock MARKER_BLOCK;

    private McHeliWingman() {}

    /**
     * Called from MCH_MOD#PreInit (before the block/item registry events fire).
     * Loads config, prepares mission storage, and queues the marker block + item
     * through CE's registries.
     */
    public static void preInit(FMLPreInitializationEvent evt, Object modInstance, CreativeTabs tab) {
        instance = modInstance;
        // Wingman MUST use its own file, NOT evt.getSuggestedConfigurationFile(): Wingman is no longer
        // a separate @Mod, so the suggested file is config/mcheli.cfg — the same file MCH_Config owns.
        // WingmanConfig uses Forge's Configuration, whose save() rewrites the whole file in Forge format
        // and would clobber MCHeli's custom-format config (which loads just before this in MCH_MOD).
        WingmanConfig.load(new File(evt.getModConfigurationDirectory(), "mcheli_wingman.cfg"));
        MissionPlan.init(evt.getModConfigurationDirectory());

        MARKER_BLOCK = new WingmanMarkerBlock();
        if (tab != null) {
            MARKER_BLOCK.setCreativeTab(tab);
        }
        MCH_Blocks.register(MARKER_BLOCK, "wingman_marker");
        MCH_Items.registerBlock(MARKER_BLOCK);

        ChunkLoadHandler chunkLoadHandler = new ChunkLoadHandler();
        ForgeChunkManager.setForcedChunkLoadingCallback(instance, chunkLoadHandler);

        MinecraftForge.EVENT_BUS.register(chunkLoadHandler);
        MinecraftForge.EVENT_BUS.register(new ProjectileChunkLoader());
        MinecraftForge.EVENT_BUS.register(new UavChunkStreamer());
        MinecraftForge.EVENT_BUS.register(new WingmanTickHandler());
        MinecraftForge.EVENT_BUS.register(new AutonomousFlightHandler());

        logger.info("McHeli Wingman pre-init complete (uavControllerRange={})", WingmanConfig.uavControllerRange);
    }

    /** Called from MCH_MOD#init. Registers the tile entity and the server/common tick handlers. */
    public static void init() {
        GameRegistry.registerTileEntity(WingmanMarkerTileEntity.class, MCH_Utils.suffix("wingman_marker_te"));



        logger.info("McHeli Wingman initialized");
    }

    /** Called from MCH_MOD#registerCommand (FMLServerStartedEvent). */
    public static void registerCommand(CommandHandler handler) {
        handler.registerCommand(new WingmanCommand());
    }
}
