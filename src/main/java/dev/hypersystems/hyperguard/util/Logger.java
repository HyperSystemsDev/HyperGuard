package dev.hypersystems.hyperguard.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * Logging utility wrapper for HyperGuard.
 * Provides formatted logging with debug mode support.
 */
public final class Logger {

    private static java.util.logging.Logger logger;
    private static boolean debugMode = false;

    private Logger() {
    }

    /**
     * Initializes the logger.
     *
     * @param javaLogger the Java logger to wrap
     */
    public static void init(@NotNull java.util.logging.Logger javaLogger) {
        logger = javaLogger;
    }

    /**
     * Sets debug mode.
     *
     * @param enabled true to enable debug logging
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is on
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     * @param args format arguments
     */
    public static void info(@NotNull String message, @Nullable Object... args) {
        log(Level.INFO, message, args);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     * @param args format arguments
     */
    public static void warn(@NotNull String message, @Nullable Object... args) {
        log(Level.WARNING, message, args);
    }

    /**
     * Logs a severe/error message.
     *
     * @param message the message
     * @param args format arguments
     */
    public static void severe(@NotNull String message, @Nullable Object... args) {
        log(Level.SEVERE, message, args);
    }

    /**
     * Logs an error message.
     * Alias for severe().
     *
     * @param message the message
     * @param args format arguments
     */
    public static void error(@NotNull String message, @Nullable Object... args) {
        severe(message, args);
    }

    /**
     * Logs a debug message (only if debug mode is enabled).
     *
     * @param message the message
     * @param args format arguments
     */
    public static void debug(@NotNull String message, @Nullable Object... args) {
        if (debugMode) {
            log(Level.INFO, "[DEBUG] " + message, args);
        }
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level the log level
     * @param message the message
     * @param args format arguments
     */
    private static void log(@NotNull Level level, @NotNull String message, @Nullable Object... args) {
        if (logger == null) {
            // Fallback to System.out if logger not initialized
            String formatted = format(message, args);
            if (level == Level.SEVERE) {
                System.err.println("[HyperGuard] " + formatted);
            } else {
                System.out.println("[HyperGuard] " + formatted);
            }
            return;
        }

        String formatted = format(message, args);
        logger.log(level, formatted);
    }

    /**
     * Formats a message with arguments.
     *
     * @param message the message format
     * @param args the arguments
     * @return the formatted message
     */
    @NotNull
    private static String format(@NotNull String message, @Nullable Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }

    /**
     * Logs an exception with a message.
     *
     * @param message the message
     * @param throwable the exception
     */
    public static void exception(@NotNull String message, @NotNull Throwable throwable) {
        if (logger != null) {
            logger.log(Level.SEVERE, message, throwable);
        } else {
            System.err.println("[HyperGuard] " + message);
            throwable.printStackTrace();
        }
    }
}
