package com.norwood.mcheli.helper;

import org.apache.logging.log4j.Logger;

public class MCH_Logger {

    private static Logger logger;

    public static void setLogger(Logger loggerIn) {
        if (logger == null) {
            logger = loggerIn;
        }
    }

    public static Logger get() {
        return logger;
    }
}
