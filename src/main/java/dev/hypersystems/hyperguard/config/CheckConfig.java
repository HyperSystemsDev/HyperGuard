package dev.hypersystems.hyperguard.config;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Configuration for a specific anti-cheat check.
 * Contains settings for enabling/disabling, tolerance, VL handling, and action thresholds.
 */
public final class CheckConfig {

    private boolean enabled;
    private double tolerance;
    private double vlMultiplier;
    private double vlDecayRate;
    private double maxVL;
    private final List<ActionThreshold> thresholds;

    /**
     * Creates a new check configuration with default values.
     */
    public CheckConfig() {
        this.enabled = true;
        this.tolerance = 0.1;
        this.vlMultiplier = 1.0;
        this.vlDecayRate = 0.5;
        this.maxVL = 100.0;
        this.thresholds = new ArrayList<>();

        // Default thresholds
        thresholds.add(new ActionThreshold(20.0, "warn"));
        thresholds.add(new ActionThreshold(50.0, "kick"));
        thresholds.add(new ActionThreshold(100.0, "ban"));
    }

    /**
     * Creates a check configuration with custom values.
     *
     * @param enabled whether the check is enabled
     * @param tolerance the tolerance/leniency value
     * @param vlMultiplier multiplier for VL additions
     * @param vlDecayRate VL decay rate per second
     * @param maxVL maximum VL cap
     * @param thresholds list of action thresholds
     */
    public CheckConfig(boolean enabled, double tolerance, double vlMultiplier,
                       double vlDecayRate, double maxVL, @NotNull List<ActionThreshold> thresholds) {
        this.enabled = enabled;
        this.tolerance = tolerance;
        this.vlMultiplier = vlMultiplier;
        this.vlDecayRate = vlDecayRate;
        this.maxVL = maxVL;
        this.thresholds = new ArrayList<>(thresholds);

        // Sort by threshold ascending
        this.thresholds.sort(Comparator.comparingDouble(ActionThreshold::getThreshold));
    }

    /**
     * Checks if this check is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this check is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the tolerance/leniency value.
     * Higher values mean more leniency.
     *
     * @return the tolerance value
     */
    public double getTolerance() {
        return tolerance;
    }

    /**
     * Sets the tolerance value.
     *
     * @param tolerance the new tolerance
     */
    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * Gets the VL multiplier.
     * VL additions are multiplied by this value.
     *
     * @return the VL multiplier
     */
    public double getVlMultiplier() {
        return vlMultiplier;
    }

    /**
     * Sets the VL multiplier.
     *
     * @param vlMultiplier the new multiplier
     */
    public void setVlMultiplier(double vlMultiplier) {
        this.vlMultiplier = vlMultiplier;
    }

    /**
     * Gets the VL decay rate per second.
     *
     * @return the decay rate
     */
    public double getVlDecayRate() {
        return vlDecayRate;
    }

    /**
     * Sets the VL decay rate.
     *
     * @param vlDecayRate the new decay rate
     */
    public void setVlDecayRate(double vlDecayRate) {
        this.vlDecayRate = vlDecayRate;
    }

    /**
     * Gets the maximum VL cap.
     *
     * @return the max VL
     */
    public double getMaxVL() {
        return maxVL;
    }

    /**
     * Sets the maximum VL.
     *
     * @param maxVL the new max VL
     */
    public void setMaxVL(double maxVL) {
        this.maxVL = maxVL;
    }

    /**
     * Gets the action thresholds (sorted by threshold ascending).
     *
     * @return unmodifiable list of thresholds
     */
    @NotNull
    public List<ActionThreshold> getThresholds() {
        return Collections.unmodifiableList(thresholds);
    }

    /**
     * Adds a new threshold.
     *
     * @param threshold the threshold to add
     */
    public void addThreshold(@NotNull ActionThreshold threshold) {
        thresholds.add(threshold);
        thresholds.sort(Comparator.comparingDouble(ActionThreshold::getThreshold));
    }

    /**
     * Clears all thresholds.
     */
    public void clearThresholds() {
        thresholds.clear();
    }

    /**
     * Creates a copy of this config.
     *
     * @return a new CheckConfig with the same values
     */
    @NotNull
    public CheckConfig copy() {
        return new CheckConfig(enabled, tolerance, vlMultiplier, vlDecayRate, maxVL, thresholds);
    }
}
