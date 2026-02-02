package dev.hypersystems.hyperguard.check;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Categories of anti-cheat checks.
 */
public enum CheckType {

    /**
     * Movement-related checks (speed, fly, phase, etc.).
     */
    MOVEMENT("Movement"),

    /**
     * Combat-related checks (reach, killaura, etc.).
     */
    COMBAT("Combat"),

    /**
     * World interaction checks (scaffold, nuker, etc.).
     */
    WORLD("World");

    private final String displayName;

    CheckType(@NotNull String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets a check type by name (case-insensitive).
     *
     * @param name the type name
     * @return the check type, or null if not found
     */
    @Nullable
    public static CheckType fromName(@NotNull String name) {
        for (CheckType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
