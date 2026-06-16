package com.norwood.mcheli.core;

import com.norwood.mcheli.MCH_MOD;
import lombok.Getter;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.norwood.mcheli.core" })
@IFMLLoadingPlugin.SortingIndex(2088)
public class MCHCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static final Logger coreLogger = LogManager.getLogger("MCH CoreMod");
    @Getter
    private static final MCHCore.Brand brand;
    private static boolean runtimeDeobfEnabled = false;
    private static boolean hardCrash = true;


    private static Boolean removeClientTrackingRestrictions = null;

    /** Effective value of the "remove client tracking restrictions" toggle for this session. */
    public static boolean isRemoveClientTrackingRestrictions() {
        if (removeClientTrackingRestrictions == null) {
            removeClientTrackingRestrictions = MCH_MOD.DEBUG_LD || readClientTrackingSetting(Launch.minecraftHome);
            coreLogger.info("Experimental: remove client tracking restrictions = {}", removeClientTrackingRestrictions);
        }
        return removeClientTrackingRestrictions;
    }

    static {
        if (Launch.classLoader.getResource("catserver/server/CatServer.class") != null) {
            brand = MCHCore.Brand.CAT_SERVER;
        } else if (Launch.classLoader.getResource("com/mohistmc/MohistMC.class") != null) {
            brand = MCHCore.Brand.MOHIST;
        } else if (Launch.classLoader.getResource("org/magmafoundation/magma/Magma.class") != null) {
            brand = MCHCore.Brand.MAGMA;
        } else if (Launch.classLoader.getResource("com/cleanroommc/boot/Main.class") != null) {
            brand = MCHCore.Brand.CLEANROOM;
        } else {
            brand = MCHCore.Brand.FORGE;
        }
    }

    static void fail(String className, Throwable t) {
        coreLogger.fatal(
                "Error transforming class {}. This is a coremod clash! Please report this on our issue tracker",
                className, t);
        if (hardCrash) {
            coreLogger.info("Crashing! To suppress the crash, launch Minecraft with -Dmch.core.disablecrash");
            throw new IllegalStateException("MCH CoreMod transformation failure: " + className, t);
        }
    }

    public static boolean runtimeDeobfEnabled() {
        return runtimeDeobfEnabled;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ AviatorCodeGeneratorTransformer.class.getName() };
    }

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList("mixins.mcheli.json", "mixins.mcheli.longdistance.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        if ("mixins.mcheli.longdistance.json".equals(mixinConfig)) {
            return isRemoveClientTrackingRestrictions();
        }
        return true;
    }

    @Override
    public String getModContainerClass() {
        return MCHCoreContainer.class.getName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        runtimeDeobfEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
        String prop = System.getProperty("mch.core.disablecrash");
        if (prop != null) {
            hardCrash = false;
            coreLogger.info("Crash suppressed with -Dmch.core.disablecrash");
        }
    }

    /**
     * Reads the {@code ExperimentalRemoveClientTrackingRestrictions} boolean directly out of
     * {@code config/mcheli.cfg}. Hand-parsed (the file is a simple {@code key = value} list)
     * because the Forge/MCHeli config machinery is not available this early. Missing file or key
     * means the feature is off.
     */
    private static boolean readClientTrackingSetting(File gameDir) {
        File cfg = new File(gameDir != null ? gameDir : new File("."), "config/mcheli.cfg");
        if (!cfg.isFile()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(cfg), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                if (line.substring(0, eq).trim().equalsIgnoreCase("ExperimentalRemoveClientTrackingRestrictions")) {
                    return line.substring(eq + 1).trim().equalsIgnoreCase("true");
                }
            }
        } catch (IOException e) {
            coreLogger.warn("Could not read {} for ExperimentalRemoveClientTrackingRestrictions: {}",
                    cfg.getAbsolutePath(), e.toString());
        }
        return false;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public enum Brand {
        FORGE,
        CAT_SERVER,
        MOHIST,
        MAGMA,
        CLEANROOM
    }
}
