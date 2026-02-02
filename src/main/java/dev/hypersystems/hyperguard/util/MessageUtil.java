package dev.hypersystems.hyperguard.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for formatting and colorizing chat messages.
 */
public final class MessageUtil {

    // Pattern to match color codes like &a, &l, etc.
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");

    // Section sign used by Minecraft/Hytale for colors
    private static final char SECTION_SIGN = '\u00A7';

    private MessageUtil() {
    }

    /**
     * Colorizes a message by replacing & color codes with section signs.
     *
     * @param message the message to colorize
     * @return the colorized message
     */
    @NotNull
    public static String colorize(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return COLOR_PATTERN.matcher(message).replaceAll(SECTION_SIGN + "$1");
    }

    /**
     * Strips color codes from a message.
     *
     * @param message the message to strip
     * @return the message without color codes
     */
    @NotNull
    public static String stripColor(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Remove both & codes and section sign codes
        return message.replaceAll("[&\u00A7][0-9a-fk-or]", "");
    }

    /**
     * Formats a HyperGuard message with the standard prefix.
     *
     * @param message the message
     * @return the formatted message
     */
    @NotNull
    public static String format(@NotNull String message) {
        return colorize("&c&lHyperGuard &8| &7" + message);
    }

    /**
     * Formats an error message.
     *
     * @param message the message
     * @return the formatted error message
     */
    @NotNull
    public static String error(@NotNull String message) {
        return colorize("&c&lHyperGuard &8| &c" + message);
    }

    /**
     * Formats a success message.
     *
     * @param message the message
     * @return the formatted success message
     */
    @NotNull
    public static String success(@NotNull String message) {
        return colorize("&c&lHyperGuard &8| &a" + message);
    }

    /**
     * Formats a warning message.
     *
     * @param message the message
     * @return the formatted warning message
     */
    @NotNull
    public static String warning(@NotNull String message) {
        return colorize("&c&lHyperGuard &8| &e" + message);
    }

    /**
     * Formats a debug message.
     *
     * @param message the message
     * @return the formatted debug message
     */
    @NotNull
    public static String debug(@NotNull String message) {
        return colorize("&c&lHG Debug &8| &7" + message);
    }

    /**
     * Creates a horizontal line for command output.
     *
     * @return the formatted line
     */
    @NotNull
    public static String line() {
        return colorize("&8&m----------------------------------------");
    }

    /**
     * Formats a key-value pair for display.
     *
     * @param key the key
     * @param value the value
     * @return the formatted string
     */
    @NotNull
    public static String keyValue(@NotNull String key, @Nullable Object value) {
        return colorize("&7" + key + ": &f" + (value != null ? value.toString() : "null"));
    }

    /**
     * Formats a VL display.
     *
     * @param vl the VL value
     * @return the formatted VL string
     */
    @NotNull
    public static String formatVL(double vl) {
        String color;
        if (vl >= 80) {
            color = "&c"; // Red for high VL
        } else if (vl >= 50) {
            color = "&6"; // Orange for medium VL
        } else if (vl >= 20) {
            color = "&e"; // Yellow for low VL
        } else {
            color = "&a"; // Green for minimal VL
        }
        return colorize(color + String.format("%.1f", vl));
    }

    /**
     * Formats a boolean as enabled/disabled.
     *
     * @param value the boolean value
     * @return "enabled" or "disabled" with appropriate color
     */
    @NotNull
    public static String formatEnabled(boolean value) {
        return value ? colorize("&aenabled") : colorize("&cdisabled");
    }

    /**
     * Formats a boolean as on/off.
     *
     * @param value the boolean value
     * @return "on" or "off" with appropriate color
     */
    @NotNull
    public static String formatOnOff(boolean value) {
        return value ? colorize("&aon") : colorize("&coff");
    }
}
