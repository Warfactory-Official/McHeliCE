package com.norwood.mcheli.helper;

import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

import static com.norwood.mcheli.MCH_Lib.getTime;


public class MCH_Logger {

    private static Logger logger;

    /**
     * Sets the Forge-provided logger instance.
     * This should be called once during mod initialization.
     *
     * @param loggerIn the logger provided by Forge
     */
    public static void setLogger(Logger loggerIn) {
        if (logger == null) {
            logger = loggerIn;
        }
    }

    /**
     * Returns the active logger instance.
     *
     * @return the logger, or null if not yet initialized
     */
    public static Logger get() {
        return logger;
    }

    /**
     * Logs an informational message.
     *
     * @param msg    the message format
     * @param params formatting parameters
     */
    public static void info(String msg, Object... params) {
        if (logger != null) {
            logger.info(msg, params);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param msg    the message format
     * @param params formatting parameters
     */
    public static void warn(String msg, Object... params) {
        if (logger != null) {
            logger.warn(msg, params);
        }
    }

    /**
     * Logs an error message.
     *
     * @param msg    the message format
     * @param params formatting parameters
     */
    public static void error(String msg, Object... params) {
        if (logger != null) {
            logger.error(msg, params);
        }
    }

    /**
     * Logs a debug-level message.
     *
     * @param msg    the message format
     * @param params formatting parameters
     */
    public static void debug(String msg, Object... params) {
        if (logger != null) {
            logger.debug(msg, params);
        }
    }

    /**
     * Logs a trace-level message.
     *
     * @param msg    the message format
     * @param params formatting parameters
     */
    public static void trace(String msg, Object... params) {
        if (logger != null) {
            logger.trace(msg, params);
        }
    }

    /**
     * Logs an error message with an exception.
     *
     * @param msg the message
     * @param t   the throwable to log
     */
    public static void error(String msg, Throwable t) {
        if (logger != null) {
            logger.error(msg, t);
        }
    }

    /**
     * Logs a warning message with an exception.
     *
     * @param msg the message
     * @param t   the throwable to log
     */
    public static void warn(String msg, Throwable t) {
        if (logger != null) {
            logger.warn(msg, t);
        }
    }

    /**
     * Logs a formatted informational message with side and timestamp context.
     *
     * @param format the message format
     * @param data   formatting parameters
     */
    public static void log(String format, Object... data) {
        if (logger == null) return;

        String side = sideTag(MCH_MOD.proxy.isRemote());
        logger.info("[{}][mcheli]{} " + format, getTime(), side, data);
    }

    /**
     * Logs a formatted informational message with world context.
     *
     * @param world  the world context
     * @param format the message format
     * @param data   formatting parameters
     */
    public static void log(World world, String format, Object... data) {
        if (logger == null) return;

        logger.info("[{}][mcheli]{} " + format, getTime(), worldTag(world), data);
    }

    /**
     * Logs a formatted informational message with entity context.
     *
     * @param entity the entity context
     * @param format the message format
     * @param data   formatting parameters
     */
    public static void log(Entity entity, String format, Object... data) {
        if (entity != null) {
            log(entity.world, format, data);
        } else {
            log((World) null, format, data);
        }
    }

    /**
     * Logs a debug-level message if debug logging is enabled.
     *
     * @param isRemote whether the context is client-side
     * @param format   the message format
     * @param data     formatting parameters
     */
    public static void debugLog(boolean isRemote, String format, Object... data) {
        if (!MCH_Config.DebugLog || logger == null) return;

        logger.debug("{} " + format, sideTag(isRemote), data);
    }

    /**
     * Logs a debug-level message with world context if debug logging is enabled.
     *
     * @param world  the world context
     * @param format the message format
     * @param data   formatting parameters
     */
    public static void debugLog(World world, String format, Object... data) {
        if (world == null) {
            debugLog(false, "[UnknownWorld] " + format, data);
        } else {
            debugLog(world.isRemote, "[World] " + format, data);
        }
    }

    private static String sideTag(boolean isRemote) {
        return isRemote ? "[Client]" : "[Server]";
    }

    private static String worldTag(World world) {
        if (world == null) return "[UnknownWorld]";
        return world.isRemote ? "[ClientWorld]" : "[ServerWorld]";
    }
}

