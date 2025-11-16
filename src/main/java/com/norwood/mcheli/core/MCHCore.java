package com.norwood.mcheli.core;

import lombok.Getter;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.norwood.mcheli.core" })
@IFMLLoadingPlugin.SortingIndex(2088)
public class MCHCore implements IFMLLoadingPlugin {

    static final Logger coreLogger = LogManager.getLogger("MCH CoreMod");
    @Getter
    private static final MCHCore.Brand brand;
    private static boolean runtimeDeobfEnabled = false;
    private static boolean hardCrash = true;

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
        return new String[] {RenderGlobalTransformer.class.getName(), EntityRenderHooks.class.getName(), EntityTrackerEntryTransformer.class.getName() };
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
