package dev.hypersystems.hyperguard.violation;

import dev.hypersystems.hyperguard.action.ActionExecutor;
import dev.hypersystems.hyperguard.alert.AlertManager;
import dev.hypersystems.hyperguard.config.ActionThreshold;
import dev.hypersystems.hyperguard.config.CheckConfig;
import dev.hypersystems.hyperguard.config.HyperGuardConfig;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Manages violations, VL accumulation, decay, and threshold actions.
 */
public final class ViolationManager {

    private static ViolationManager instance;

    // Recent violations for lookup
    private final Deque<Violation> recentViolations;
    private static final int MAX_RECENT_VIOLATIONS = 1000;

    private ViolationManager() {
        this.recentViolations = new ConcurrentLinkedDeque<>();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the violation manager
     */
    @NotNull
    public static ViolationManager get() {
        if (instance == null) {
            instance = new ViolationManager();
        }
        return instance;
    }

    /**
     * Initializes the violation manager.
     */
    public static void init() {
        instance = new ViolationManager();
        Logger.debug("ViolationManager initialized");
    }

    /**
     * Shuts down the violation manager.
     */
    public void shutdown() {
        recentViolations.clear();
        Logger.debug("ViolationManager shut down");
    }

    /**
     * Flags a player for a check violation.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param checkType the check type (MOVEMENT, COMBAT, WORLD)
     * @param baseVL the base VL to add
     * @param details optional violation details
     * @return the created Violation, or null if not flagged (player exempt, check disabled, etc.)
     */
    @Nullable
    public Violation flag(
        @NotNull HGPlayerData playerData,
        @NotNull String checkName,
        @NotNull String checkType,
        double baseVL,
        @Nullable String details
    ) {
        // Check if player is exempt
        if (playerData.isExempt(checkName) || playerData.isGloballyExempt()) {
            return null;
        }

        // Get check config
        HyperGuardConfig config = HyperGuardConfig.get();
        CheckConfig checkConfig = config.getCheckConfig(checkName);
        if (checkConfig == null || !checkConfig.isEnabled()) {
            return null;
        }

        // Calculate actual VL to add
        double vlToAdd = baseVL * checkConfig.getVlMultiplier();

        // Add VL and cap at maxVL
        double newTotalVL = playerData.addVL(checkName, vlToAdd);
        if (newTotalVL > checkConfig.getMaxVL()) {
            newTotalVL = checkConfig.getMaxVL();
            playerData.setVL(checkName, newTotalVL);
        }

        // Create violation record
        Violation violation = Violation.create(
            playerData.getUuid(),
            playerData.getUsername(),
            checkName,
            checkType,
            vlToAdd,
            newTotalVL,
            details
        );

        // Store in recent violations
        addRecentViolation(violation);

        // Log violation
        if (config.getGeneral().isLoggingEnabled()) {
            Logger.info("[VIOLATION] %s", violation.toDisplayString());
        }

        // Alert staff
        if (config.getGeneral().isAlertsEnabled()) {
            AlertManager.get().broadcastViolation(violation);
        }

        // Check thresholds and execute actions
        checkThresholds(playerData, checkName, newTotalVL, checkConfig);

        return violation;
    }

    /**
     * Flags a player without details.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param checkType the check type
     * @param baseVL the base VL to add
     * @return the created Violation, or null if not flagged
     */
    @Nullable
    public Violation flag(
        @NotNull HGPlayerData playerData,
        @NotNull String checkName,
        @NotNull String checkType,
        double baseVL
    ) {
        return flag(playerData, checkName, checkType, baseVL, null);
    }

    /**
     * Checks thresholds and executes actions.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param currentVL the current VL
     * @param checkConfig the check config
     */
    private void checkThresholds(
        @NotNull HGPlayerData playerData,
        @NotNull String checkName,
        double currentVL,
        @NotNull CheckConfig checkConfig
    ) {
        for (ActionThreshold threshold : checkConfig.getThresholds()) {
            if (currentVL >= threshold.getThreshold()) {
                // Check if already triggered
                if (!playerData.hasTriggeredThreshold(checkName, threshold.getThreshold())) {
                    // Mark as triggered
                    playerData.markThresholdTriggered(checkName, threshold.getThreshold());

                    // Execute action
                    ActionExecutor.get().executeAction(
                        playerData,
                        checkName,
                        threshold.getAction(),
                        threshold.getDuration(),
                        currentVL
                    );
                }
            }
        }
    }

    /**
     * Decays VL for all players and all checks.
     * Called periodically by the decay task.
     */
    public void decayAllPlayers() {
        HyperGuardConfig config = HyperGuardConfig.get();

        for (Map.Entry<String, CheckConfig> entry : config.getAllCheckConfigs().entrySet()) {
            String checkName = entry.getKey();
            CheckConfig checkConfig = entry.getValue();

            if (checkConfig.isEnabled() && checkConfig.getVlDecayRate() > 0) {
                HGPlayerManager.get().decayAllVL(checkName, checkConfig.getVlDecayRate());
            }
        }
    }

    /**
     * Adds a violation to recent history.
     *
     * @param violation the violation to add
     */
    private void addRecentViolation(@NotNull Violation violation) {
        recentViolations.addFirst(violation);

        // Trim if over limit
        while (recentViolations.size() > MAX_RECENT_VIOLATIONS) {
            recentViolations.removeLast();
        }
    }

    /**
     * Gets recent violations for a player.
     *
     * @param uuid the player UUID
     * @param limit maximum number to return
     * @return list of recent violations
     */
    @NotNull
    public List<Violation> getRecentViolations(@NotNull UUID uuid, int limit) {
        List<Violation> result = new ArrayList<>();
        for (Violation v : recentViolations) {
            if (v.uuid().equals(uuid)) {
                result.add(v);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Gets all recent violations.
     *
     * @param limit maximum number to return
     * @return list of recent violations
     */
    @NotNull
    public List<Violation> getRecentViolations(int limit) {
        List<Violation> result = new ArrayList<>();
        for (Violation v : recentViolations) {
            result.add(v);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    /**
     * Gets violations for a specific check.
     *
     * @param checkName the check name
     * @param limit maximum number to return
     * @return list of violations
     */
    @NotNull
    public List<Violation> getViolationsByCheck(@NotNull String checkName, int limit) {
        List<Violation> result = new ArrayList<>();
        String lowerCheck = checkName.toLowerCase();
        for (Violation v : recentViolations) {
            if (v.checkName().equalsIgnoreCase(lowerCheck)) {
                result.add(v);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Clears all recent violations.
     */
    public void clearRecentViolations() {
        recentViolations.clear();
    }

    /**
     * Gets the total violation count.
     *
     * @return total count
     */
    public int getTotalViolationCount() {
        return recentViolations.size();
    }
}
