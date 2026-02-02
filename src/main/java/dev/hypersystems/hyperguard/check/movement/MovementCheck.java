package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.alert.AlertManager;
import dev.hypersystems.hyperguard.check.Check;
import dev.hypersystems.hyperguard.check.CheckType;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.PositionHistory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for movement-related anti-cheat checks.
 * Uses PositionHistory and HGPlayerData flags instead of ECS components
 * to avoid thread-safety issues with component access.
 */
public abstract class MovementCheck extends Check {

    /**
     * Creates a new movement check.
     *
     * @param name the check name
     */
    protected MovementCheck(@NotNull String name) {
        super(name, CheckType.MOVEMENT);
    }

    /**
     * Checks if a player has movement exemptions that should skip checks.
     *
     * @param playerData the player data
     * @return true if the player should be exempt
     */
    protected boolean hasMovementExemption(@NotNull HGPlayerData playerData) {
        // Flying (creative mode) - exempt from most checks
        if (playerData.isFlying()) {
            return true;
        }

        // Post-damage exemption
        if (playerData.getTicksSinceDamage() < MovementConstants.POST_DAMAGE_EXEMPT_TICKS) {
            return true;
        }

        // Post-velocity exemption
        if (playerData.getTicksSinceVelocity() < MovementConstants.POST_VELOCITY_EXEMPT_TICKS) {
            return true;
        }

        // Post-teleport exemption
        if (playerData.getTicksSinceTeleport() < MovementConstants.POST_TELEPORT_EXEMPT_TICKS) {
            return true;
        }

        return false;
    }

    /**
     * Gets the expected maximum horizontal speed.
     *
     * Uses SPRINT_SPEED as the baseline since we cannot reliably detect movement state
     * (ECS components are not accessible from the background scheduler thread).
     * This prevents false positives when players are sprinting but the state shows "walking".
     *
     * @param playerData the player data
     * @param tolerance additional tolerance multiplier (e.g., 0.30 for 30% tolerance)
     * @return the maximum expected speed in blocks per tick
     */
    protected double getExpectedMaxSpeed(@NotNull HGPlayerData playerData, double tolerance) {
        double baseSpeed;

        // Flying and gliding have separate physics and are typically exempted,
        // but if they reach here, use their appropriate speeds
        if (playerData.isFlying()) {
            baseSpeed = MovementConstants.FLY_SPEED;
        } else if (playerData.isGliding()) {
            baseSpeed = MovementConstants.GLIDE_SPEED;
        } else {
            // Always use sprint speed as baseline since we can't reliably detect
            // walking vs sprinting state from the async check thread.
            // This prevents false positives from state detection failures.
            baseSpeed = MovementConstants.SPRINT_SPEED;
        }

        // Apply tolerance
        return baseSpeed * (1.0 + tolerance);
    }

    /**
     * Gets the current and previous position samples from history.
     *
     * @param playerData the player data
     * @return array of [current, previous] samples, or null if insufficient history
     */
    @Nullable
    protected PositionHistory.PositionSample[] getPositionSamples(@NotNull HGPlayerData playerData) {
        PositionHistory history = playerData.getPositionHistory();
        if (history.size() < 2) {
            return null;
        }

        PositionHistory.PositionSample current = history.getLatest();
        PositionHistory.PositionSample previous = history.getPrevious();

        if (current == null || previous == null) {
            return null;
        }

        return new PositionHistory.PositionSample[] { current, previous };
    }

    /**
     * Calculates horizontal speed from position delta.
     *
     * @param deltaX the X position change
     * @param deltaZ the Z position change
     * @return the horizontal speed in blocks per tick
     */
    protected double calculateHorizontalSpeed(double deltaX, double deltaZ) {
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    /**
     * Gets a description of the current movement state.
     *
     * @param playerData the player data
     * @return a brief state description
     */
    protected String getStateDescription(@NotNull HGPlayerData playerData) {
        if (playerData.isFlying()) return "flying";
        if (playerData.isGliding()) return "gliding";
        if (playerData.isSprinting()) return "sprinting";
        if (playerData.isSwimming()) return "swimming";
        if (playerData.isClimbing()) return "climbing";
        if (playerData.isSneaking()) return "sneaking";
        return "walking";
    }

    /**
     * Sends a debug message to a player if they have debug mode enabled.
     *
     * @param player the player
     * @param playerData the player data
     * @param message the message
     */
    protected void sendDebug(@NotNull PlayerRef player, @NotNull HGPlayerData playerData, @NotNull String message) {
        if (playerData.isDebugMode()) {
            AlertManager.get().sendDebug(player, playerData, getName(), message);
        }
    }

    /**
     * Sends a formatted debug message to a player if they have debug mode enabled.
     *
     * @param player the player
     * @param playerData the player data
     * @param format the message format
     * @param args the format arguments
     */
    protected void sendDebug(@NotNull PlayerRef player, @NotNull HGPlayerData playerData, @NotNull String format, Object... args) {
        if (playerData.isDebugMode()) {
            String message = String.format(format, args);
            AlertManager.get().sendDebug(player, playerData, getName(), message);
        }
    }
}
