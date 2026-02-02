package dev.hypersystems.hyperguard.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a VL threshold that triggers an action.
 * When a player's VL reaches this threshold, the specified action is executed.
 */
public final class ActionThreshold {

    private final double threshold;
    private final String action;
    private final String duration;

    /**
     * Creates a new action threshold.
     *
     * @param threshold the VL threshold that triggers this action
     * @param action the action name (warn, kick, tempban, ban)
     * @param duration optional duration for tempban (e.g., "1h", "30m")
     */
    public ActionThreshold(double threshold, @NotNull String action, @Nullable String duration) {
        this.threshold = threshold;
        this.action = action.toLowerCase();
        this.duration = duration;
    }

    /**
     * Creates a new action threshold without a duration.
     *
     * @param threshold the VL threshold
     * @param action the action name
     */
    public ActionThreshold(double threshold, @NotNull String action) {
        this(threshold, action, null);
    }

    /**
     * Gets the VL threshold.
     *
     * @return the threshold value
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Gets the action name.
     *
     * @return the action (warn, kick, tempban, ban)
     */
    @NotNull
    public String getAction() {
        return action;
    }

    /**
     * Gets the optional duration for tempban.
     *
     * @return the duration string, or null if not applicable
     */
    @Nullable
    public String getDuration() {
        return duration;
    }

    /**
     * Checks if this threshold has a duration.
     *
     * @return true if duration is set
     */
    public boolean hasDuration() {
        return duration != null && !duration.isEmpty();
    }

    @Override
    public String toString() {
        if (hasDuration()) {
            return String.format("ActionThreshold{threshold=%.1f, action='%s', duration='%s'}",
                threshold, action, duration);
        }
        return String.format("ActionThreshold{threshold=%.1f, action='%s'}", threshold, action);
    }
}
