package dev.hypersystems.hyperguard.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing and formatting time durations.
 */
public final class TimeUtil {

    // Pattern to match duration strings like "1h", "30m", "2d", "1w"
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(?:(\\d+)w)?\\s*(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?",
        Pattern.CASE_INSENSITIVE
    );

    // Simple pattern for single unit durations
    private static final Pattern SIMPLE_DURATION_PATTERN = Pattern.compile(
        "(\\d+)\\s*([smhdw])",
        Pattern.CASE_INSENSITIVE
    );

    private TimeUtil() {
    }

    /**
     * Parses a duration string to milliseconds.
     * Supports formats like: "1h", "30m", "2d", "1w", "1h30m", "1d12h"
     *
     * @param duration the duration string
     * @return duration in milliseconds, or 0 if invalid
     */
    public static long parseDuration(@Nullable String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }

        duration = duration.trim().toLowerCase();

        // Try simple format first (e.g., "30m", "1h")
        Matcher simpleMatcher = SIMPLE_DURATION_PATTERN.matcher(duration);
        if (simpleMatcher.matches()) {
            long value = Long.parseLong(simpleMatcher.group(1));
            char unit = simpleMatcher.group(2).charAt(0);
            return value * getUnitMillis(unit);
        }

        // Try complex format (e.g., "1h30m", "1d12h30m")
        long total = 0;
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        if (matcher.matches()) {
            // Weeks
            if (matcher.group(1) != null) {
                total += Long.parseLong(matcher.group(1)) * getUnitMillis('w');
            }
            // Days
            if (matcher.group(2) != null) {
                total += Long.parseLong(matcher.group(2)) * getUnitMillis('d');
            }
            // Hours
            if (matcher.group(3) != null) {
                total += Long.parseLong(matcher.group(3)) * getUnitMillis('h');
            }
            // Minutes
            if (matcher.group(4) != null) {
                total += Long.parseLong(matcher.group(4)) * getUnitMillis('m');
            }
            // Seconds
            if (matcher.group(5) != null) {
                total += Long.parseLong(matcher.group(5)) * getUnitMillis('s');
            }
        }

        return total;
    }

    /**
     * Gets milliseconds for a time unit.
     *
     * @param unit the unit character
     * @return milliseconds per unit
     */
    private static long getUnitMillis(char unit) {
        return switch (unit) {
            case 's' -> TimeUnit.SECONDS.toMillis(1);
            case 'm' -> TimeUnit.MINUTES.toMillis(1);
            case 'h' -> TimeUnit.HOURS.toMillis(1);
            case 'd' -> TimeUnit.DAYS.toMillis(1);
            case 'w' -> TimeUnit.DAYS.toMillis(7);
            default -> 0;
        };
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     *
     * @param millis the duration in milliseconds
     * @return formatted string like "1d 2h 30m"
     */
    @NotNull
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0s";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        StringBuilder sb = new StringBuilder();

        if (weeks > 0) {
            sb.append(weeks).append("w ");
            days %= 7;
        }
        if (days > 0) {
            sb.append(days).append("d ");
            hours %= 24;
        }
        if (hours > 0) {
            sb.append(hours % 24).append("h ");
            minutes %= 60;
        }
        if (minutes > 0 && weeks == 0) {
            sb.append(minutes % 60).append("m ");
            seconds %= 60;
        }
        if (seconds > 0 && days == 0 && weeks == 0) {
            sb.append(seconds % 60).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Formats a duration in a short form (largest unit only).
     *
     * @param millis the duration in milliseconds
     * @return formatted string like "2h" or "3d"
     */
    @NotNull
    public static String formatDurationShort(long millis) {
        if (millis <= 0) {
            return "0s";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) {
            return weeks + "w";
        } else if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Formats a timestamp as "X ago".
     *
     * @param timestamp the timestamp in milliseconds
     * @return formatted string like "5m ago"
     */
    @NotNull
    public static String formatTimeAgo(long timestamp) {
        long elapsed = System.currentTimeMillis() - timestamp;
        if (elapsed < 0) {
            return "just now";
        }
        return formatDurationShort(elapsed) + " ago";
    }

    /**
     * Converts ticks to milliseconds.
     *
     * @param ticks the ticks (20 ticks = 1 second)
     * @return milliseconds
     */
    public static long ticksToMillis(long ticks) {
        return ticks * 50; // 1 tick = 50ms
    }

    /**
     * Converts milliseconds to ticks.
     *
     * @param millis the milliseconds
     * @return ticks
     */
    public static long millisToTicks(long millis) {
        return millis / 50;
    }

    /**
     * Formats ticks as a duration string.
     *
     * @param ticks the ticks
     * @return formatted duration
     */
    @NotNull
    public static String formatTicks(long ticks) {
        return formatDuration(ticksToMillis(ticks));
    }
}
