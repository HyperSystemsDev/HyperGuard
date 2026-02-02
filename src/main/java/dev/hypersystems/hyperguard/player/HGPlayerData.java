package dev.hypersystems.hyperguard.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Per-player data tracking for HyperGuard.
 * Stores VL, exemptions, position history, and movement states.
 */
public final class HGPlayerData {

    private final UUID uuid;
    private final String username;
    private final long joinTime;

    // Position tracking
    private final PositionHistory positionHistory;

    // VL tracking per check
    private final Map<String, Double> violationLevels;

    // Triggered thresholds (to avoid repeat actions)
    private final Map<String, Set<Double>> triggeredThresholds;

    // Exemptions
    private final Set<String> exemptChecks;
    private boolean globallyExempt;

    // Timestamps for temporary exemptions
    private long lastTeleportTime;
    private long lastDamageTime;
    private long lastVelocityTime;

    // Movement states
    private boolean sprinting;
    private boolean sneaking;
    private boolean swimming;
    private boolean climbing;
    private boolean flying;
    private boolean gliding;

    // Debug mode for this player
    private boolean debugMode;

    // Alert toggle for staff
    private boolean alertsEnabled;

    /**
     * Creates new player data.
     *
     * @param uuid the player UUID
     * @param username the player username
     */
    public HGPlayerData(@NotNull UUID uuid, @NotNull String username) {
        this.uuid = uuid;
        this.username = username;
        this.joinTime = System.currentTimeMillis();
        this.positionHistory = new PositionHistory();
        this.violationLevels = new HashMap<>();
        this.triggeredThresholds = new HashMap<>();
        this.exemptChecks = new HashSet<>();
        this.globallyExempt = false;
        this.lastTeleportTime = 0;
        this.lastDamageTime = 0;
        this.lastVelocityTime = 0;
        this.sprinting = false;
        this.sneaking = false;
        this.swimming = false;
        this.climbing = false;
        this.flying = false;
        this.gliding = false;
        this.debugMode = false;
        this.alertsEnabled = true;
    }

    // ==================== Identification ====================

    /**
     * Gets the player UUID.
     *
     * @return the UUID
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the player username.
     *
     * @return the username
     */
    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     * Gets the time the player joined.
     *
     * @return join timestamp in milliseconds
     */
    public long getJoinTime() {
        return joinTime;
    }

    /**
     * Gets the time since the player joined in ticks.
     *
     * @return ticks since join
     */
    public long getTicksSinceJoin() {
        return (System.currentTimeMillis() - joinTime) / 50;
    }

    // ==================== Position Tracking ====================

    /**
     * Gets the position history.
     *
     * @return the position history
     */
    @NotNull
    public PositionHistory getPositionHistory() {
        return positionHistory;
    }

    /**
     * Records a new position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param yaw the yaw rotation
     * @param pitch the pitch rotation
     * @param onGround whether on ground
     */
    public void recordPosition(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        positionHistory.add(x, y, z, yaw, pitch, onGround);
    }

    // ==================== Violation Levels ====================

    /**
     * Gets the VL for a check.
     *
     * @param checkName the check name
     * @return the VL, or 0 if none
     */
    public double getVL(@NotNull String checkName) {
        return violationLevels.getOrDefault(checkName.toLowerCase(), 0.0);
    }

    /**
     * Sets the VL for a check.
     *
     * @param checkName the check name
     * @param vl the new VL
     */
    public void setVL(@NotNull String checkName, double vl) {
        violationLevels.put(checkName.toLowerCase(), Math.max(0, vl));
    }

    /**
     * Adds VL to a check.
     *
     * @param checkName the check name
     * @param amount the amount to add
     * @return the new total VL
     */
    public double addVL(@NotNull String checkName, double amount) {
        String key = checkName.toLowerCase();
        double newVL = violationLevels.getOrDefault(key, 0.0) + amount;
        violationLevels.put(key, Math.max(0, newVL));
        return newVL;
    }

    /**
     * Decays VL for a check.
     *
     * @param checkName the check name
     * @param decayAmount the amount to decay
     */
    public void decayVL(@NotNull String checkName, double decayAmount) {
        String key = checkName.toLowerCase();
        double current = violationLevels.getOrDefault(key, 0.0);
        double newVL = current - decayAmount;

        if (newVL <= 0) {
            violationLevels.remove(key);
            // Clear triggered thresholds when VL reaches 0
            triggeredThresholds.remove(key);
        } else {
            violationLevels.put(key, newVL);
        }
    }

    /**
     * Gets all non-zero VLs.
     *
     * @return unmodifiable map of check names to VLs
     */
    @NotNull
    public Map<String, Double> getAllVLs() {
        return Collections.unmodifiableMap(violationLevels);
    }

    /**
     * Clears all VLs.
     */
    public void clearAllVLs() {
        violationLevels.clear();
        triggeredThresholds.clear();
    }

    // ==================== Threshold Tracking ====================

    /**
     * Checks if a threshold has been triggered for a check.
     *
     * @param checkName the check name
     * @param threshold the threshold value
     * @return true if already triggered
     */
    public boolean hasTriggeredThreshold(@NotNull String checkName, double threshold) {
        Set<Double> thresholds = triggeredThresholds.get(checkName.toLowerCase());
        return thresholds != null && thresholds.contains(threshold);
    }

    /**
     * Marks a threshold as triggered.
     *
     * @param checkName the check name
     * @param threshold the threshold value
     */
    public void markThresholdTriggered(@NotNull String checkName, double threshold) {
        triggeredThresholds
            .computeIfAbsent(checkName.toLowerCase(), k -> new HashSet<>())
            .add(threshold);
    }

    /**
     * Clears triggered thresholds for a check.
     *
     * @param checkName the check name
     */
    public void clearTriggeredThresholds(@NotNull String checkName) {
        triggeredThresholds.remove(checkName.toLowerCase());
    }

    // ==================== Exemptions ====================

    /**
     * Checks if the player is exempt from a check.
     *
     * @param checkName the check name
     * @return true if exempt
     */
    public boolean isExempt(@NotNull String checkName) {
        return globallyExempt || exemptChecks.contains(checkName.toLowerCase());
    }

    /**
     * Sets exemption for a specific check.
     *
     * @param checkName the check name
     * @param exempt true to exempt
     */
    public void setExempt(@NotNull String checkName, boolean exempt) {
        if (exempt) {
            exemptChecks.add(checkName.toLowerCase());
        } else {
            exemptChecks.remove(checkName.toLowerCase());
        }
    }

    /**
     * Checks if the player is globally exempt.
     *
     * @return true if globally exempt
     */
    public boolean isGloballyExempt() {
        return globallyExempt;
    }

    /**
     * Sets global exemption.
     *
     * @param exempt true to exempt from all checks
     */
    public void setGloballyExempt(boolean exempt) {
        this.globallyExempt = exempt;
    }

    /**
     * Gets all exempt check names.
     *
     * @return unmodifiable set of exempt checks
     */
    @NotNull
    public Set<String> getExemptChecks() {
        return Collections.unmodifiableSet(exemptChecks);
    }

    // ==================== Temporary Exemptions ====================

    /**
     * Records a teleport event.
     */
    public void recordTeleport() {
        this.lastTeleportTime = System.currentTimeMillis();
    }

    /**
     * Gets the time since last teleport in ticks.
     *
     * @return ticks since teleport, or Long.MAX_VALUE if never teleported
     */
    public long getTicksSinceTeleport() {
        if (lastTeleportTime == 0) {
            return Long.MAX_VALUE;
        }
        return (System.currentTimeMillis() - lastTeleportTime) / 50;
    }

    /**
     * Records a damage event.
     */
    public void recordDamage() {
        this.lastDamageTime = System.currentTimeMillis();
    }

    /**
     * Gets the time since last damage in ticks.
     *
     * @return ticks since damage, or Long.MAX_VALUE if never damaged
     */
    public long getTicksSinceDamage() {
        if (lastDamageTime == 0) {
            return Long.MAX_VALUE;
        }
        return (System.currentTimeMillis() - lastDamageTime) / 50;
    }

    /**
     * Records a velocity change event.
     */
    public void recordVelocity() {
        this.lastVelocityTime = System.currentTimeMillis();
    }

    /**
     * Gets the time since last velocity change in ticks.
     *
     * @return ticks since velocity change, or Long.MAX_VALUE if never
     */
    public long getTicksSinceVelocity() {
        if (lastVelocityTime == 0) {
            return Long.MAX_VALUE;
        }
        return (System.currentTimeMillis() - lastVelocityTime) / 50;
    }

    // ==================== Movement States ====================

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSwimming() {
        return swimming;
    }

    public void setSwimming(boolean swimming) {
        this.swimming = swimming;
    }

    public boolean isClimbing() {
        return climbing;
    }

    public void setClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isGliding() {
        return gliding;
    }

    public void setGliding(boolean gliding) {
        this.gliding = gliding;
    }

    // ==================== Debug & Alerts ====================

    /**
     * Checks if debug mode is enabled for this player.
     *
     * @return true if debug mode is on
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Sets debug mode for this player.
     *
     * @param debugMode true to enable
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Checks if alerts are enabled for this player (staff only).
     *
     * @return true if alerts are enabled
     */
    public boolean areAlertsEnabled() {
        return alertsEnabled;
    }

    /**
     * Sets alerts enabled for this player.
     *
     * @param alertsEnabled true to enable
     */
    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
    }
}
