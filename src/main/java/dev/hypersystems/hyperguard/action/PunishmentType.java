package dev.hypersystems.hyperguard.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Types of punishments that can be executed by HyperGuard.
 */
public enum PunishmentType {

    /**
     * Warning message to the player.
     */
    WARN("warn", false, false),

    /**
     * Kick the player from the server.
     */
    KICK("kick", false, true),

    /**
     * Temporary ban the player.
     */
    TEMPBAN("tempban", true, true),

    /**
     * Permanent ban the player.
     */
    BAN("ban", false, true);

    private final String name;
    private final boolean requiresDuration;
    private final boolean removesPlayer;

    PunishmentType(@NotNull String name, boolean requiresDuration, boolean removesPlayer) {
        this.name = name;
        this.requiresDuration = requiresDuration;
        this.removesPlayer = removesPlayer;
    }

    /**
     * Gets the action name.
     *
     * @return the name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Checks if this punishment requires a duration.
     *
     * @return true if duration is required
     */
    public boolean requiresDuration() {
        return requiresDuration;
    }

    /**
     * Checks if this punishment removes the player from the server.
     *
     * @return true if player is removed
     */
    public boolean removesPlayer() {
        return removesPlayer;
    }

    /**
     * Gets a punishment type by name (case-insensitive).
     *
     * @param name the action name
     * @return the punishment type, or null if not found
     */
    @Nullable
    public static PunishmentType fromName(@NotNull String name) {
        String lower = name.toLowerCase();
        for (PunishmentType type : values()) {
            if (type.name.equals(lower)) {
                return type;
            }
        }
        return null;
    }
}
