package dev.hypersystems.hyperguard.violation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Immutable record representing a violation event.
 * Created when a check flags a player.
 *
 * @param uuid the player UUID
 * @param username the player username
 * @param checkName the check that flagged
 * @param checkType the type of check (MOVEMENT, COMBAT, WORLD)
 * @param vlAdded the VL added for this violation
 * @param totalVL the total VL after this violation
 * @param details additional details about the violation
 * @param timestamp when the violation occurred
 */
public record Violation(
    @NotNull UUID uuid,
    @NotNull String username,
    @NotNull String checkName,
    @NotNull String checkType,
    double vlAdded,
    double totalVL,
    @Nullable String details,
    long timestamp
) {

    /**
     * Creates a new violation with the current timestamp.
     *
     * @param uuid the player UUID
     * @param username the player username
     * @param checkName the check name
     * @param checkType the check type
     * @param vlAdded the VL added
     * @param totalVL the total VL
     * @param details optional details
     * @return the new Violation
     */
    @NotNull
    public static Violation create(
        @NotNull UUID uuid,
        @NotNull String username,
        @NotNull String checkName,
        @NotNull String checkType,
        double vlAdded,
        double totalVL,
        @Nullable String details
    ) {
        return new Violation(uuid, username, checkName, checkType, vlAdded, totalVL, details, System.currentTimeMillis());
    }

    /**
     * Creates a new violation without details.
     *
     * @param uuid the player UUID
     * @param username the player username
     * @param checkName the check name
     * @param checkType the check type
     * @param vlAdded the VL added
     * @param totalVL the total VL
     * @return the new Violation
     */
    @NotNull
    public static Violation create(
        @NotNull UUID uuid,
        @NotNull String username,
        @NotNull String checkName,
        @NotNull String checkType,
        double vlAdded,
        double totalVL
    ) {
        return create(uuid, username, checkName, checkType, vlAdded, totalVL, null);
    }

    /**
     * Formats the violation as a display string.
     *
     * @return formatted violation string
     */
    @NotNull
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(username)
            .append(" flagged ")
            .append(checkName)
            .append(" (")
            .append(checkType)
            .append(") VL: ")
            .append(String.format("%.1f", totalVL))
            .append(" (+")
            .append(String.format("%.1f", vlAdded))
            .append(")");

        if (details != null && !details.isEmpty()) {
            sb.append(" [").append(details).append("]");
        }

        return sb.toString();
    }

    /**
     * Formats the violation as an alert message for staff.
     *
     * @return formatted alert message
     */
    @NotNull
    public String toAlertMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("&c&lHG &8| &7")
            .append(username)
            .append(" &8| &c")
            .append(checkName)
            .append(" &8(&7")
            .append(checkType)
            .append("&8) &7VL: &c")
            .append(String.format("%.1f", totalVL));

        if (details != null && !details.isEmpty()) {
            sb.append(" &8[&7").append(details).append("&8]");
        }

        return sb.toString();
    }

    /**
     * Gets the age of this violation in milliseconds.
     *
     * @return age in milliseconds
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Gets the age of this violation in seconds.
     *
     * @return age in seconds
     */
    public long getAgeSeconds() {
        return getAgeMs() / 1000;
    }
}
