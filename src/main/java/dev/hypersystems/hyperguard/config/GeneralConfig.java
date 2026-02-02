package dev.hypersystems.hyperguard.config;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * General configuration settings for HyperGuard.
 * Contains global settings that apply to all checks and systems.
 */
public final class GeneralConfig {

    private boolean alertsEnabled;
    private boolean loggingEnabled;
    private boolean debugMode;
    private int vlDecayIntervalTicks;
    private final Set<String> exemptGamemodes;
    private String bypassPermission;
    private String alertPermission;
    private int joinExemptionTicks;
    private int teleportExemptionTicks;

    /**
     * Creates a new general config with default values.
     */
    public GeneralConfig() {
        this.alertsEnabled = true;
        this.loggingEnabled = true;
        this.debugMode = false;
        this.vlDecayIntervalTicks = 20; // 1 second
        this.exemptGamemodes = new HashSet<>();
        this.exemptGamemodes.add("Creative");
        this.exemptGamemodes.add("Spectator");
        this.bypassPermission = "hyperguard.bypass";
        this.alertPermission = "hyperguard.alerts";
        this.joinExemptionTicks = 100; // 5 seconds
        this.teleportExemptionTicks = 40; // 2 seconds
    }

    /**
     * Checks if staff alerts are enabled.
     *
     * @return true if alerts are enabled
     */
    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    /**
     * Sets whether staff alerts are enabled.
     *
     * @param alertsEnabled true to enable
     */
    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
    }

    /**
     * Checks if logging is enabled.
     *
     * @return true if logging is enabled
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Sets whether logging is enabled.
     *
     * @param loggingEnabled true to enable
     */
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is on
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Sets debug mode.
     *
     * @param debugMode true to enable debug mode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Gets the VL decay interval in ticks.
     *
     * @return decay interval (default 20 ticks = 1 second)
     */
    public int getVlDecayIntervalTicks() {
        return vlDecayIntervalTicks;
    }

    /**
     * Sets the VL decay interval.
     *
     * @param vlDecayIntervalTicks the interval in ticks
     */
    public void setVlDecayIntervalTicks(int vlDecayIntervalTicks) {
        this.vlDecayIntervalTicks = Math.max(1, vlDecayIntervalTicks);
    }

    /**
     * Gets the set of gamemode names that are exempt from checks.
     *
     * @return set of exempt gamemode names
     */
    @NotNull
    public Set<String> getExemptGamemodes() {
        return exemptGamemodes;
    }

    /**
     * Checks if a gamemode is exempt.
     *
     * @param gamemode the gamemode name
     * @return true if exempt
     */
    public boolean isGamemodeExempt(@NotNull String gamemode) {
        return exemptGamemodes.stream()
            .anyMatch(exempt -> exempt.equalsIgnoreCase(gamemode));
    }

    /**
     * Gets the bypass permission node.
     *
     * @return the bypass permission
     */
    @NotNull
    public String getBypassPermission() {
        return bypassPermission;
    }

    /**
     * Sets the bypass permission.
     *
     * @param bypassPermission the permission node
     */
    public void setBypassPermission(@NotNull String bypassPermission) {
        this.bypassPermission = bypassPermission;
    }

    /**
     * Gets the alert permission node.
     * Staff with this permission receive violation alerts.
     *
     * @return the alert permission
     */
    @NotNull
    public String getAlertPermission() {
        return alertPermission;
    }

    /**
     * Sets the alert permission.
     *
     * @param alertPermission the permission node
     */
    public void setAlertPermission(@NotNull String alertPermission) {
        this.alertPermission = alertPermission;
    }

    /**
     * Gets the join exemption duration in ticks.
     * Players are exempt from checks for this duration after joining.
     *
     * @return the exemption duration in ticks
     */
    public int getJoinExemptionTicks() {
        return joinExemptionTicks;
    }

    /**
     * Sets the join exemption duration.
     *
     * @param joinExemptionTicks duration in ticks
     */
    public void setJoinExemptionTicks(int joinExemptionTicks) {
        this.joinExemptionTicks = Math.max(0, joinExemptionTicks);
    }

    /**
     * Gets the teleport exemption duration in ticks.
     * Players are exempt from checks for this duration after teleporting.
     *
     * @return the exemption duration in ticks
     */
    public int getTeleportExemptionTicks() {
        return teleportExemptionTicks;
    }

    /**
     * Sets the teleport exemption duration.
     *
     * @param teleportExemptionTicks duration in ticks
     */
    public void setTeleportExemptionTicks(int teleportExemptionTicks) {
        this.teleportExemptionTicks = Math.max(0, teleportExemptionTicks);
    }
}
