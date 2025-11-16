package com.norwood.mcheli.helper.debug;

import com.norwood.mcheli.MCH_ClientProxy;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.MCH_Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugBootstrap {

    private static final Logger LOGGER = LogManager.getLogger("Debug log");

    public static void init() {
        MCH_Logger.setLogger(LOGGER);
        MCH_MOD.instance = new MCH_MOD();
        MCH_MOD.proxy = new MCH_ClientProxy();
    }
}
