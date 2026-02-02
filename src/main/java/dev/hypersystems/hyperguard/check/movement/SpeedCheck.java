package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.PositionHistory;
import org.jetbrains.annotations.NotNull;

/**
 * Detects players moving faster than allowed speeds.
 * Checks horizontal movement speed against expected values based on movement state.
 */
public final class SpeedCheck extends MovementCheck {

    public SpeedCheck() {
        super("speed");
    }

    @Override
    public void process(@NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        // Check if exempt
        if (isExempt(playerData)) {
            sendDebug(player, playerData, "exempt (general)");
            return;
        }

        // Check permission bypass
        if (hasExemptPermission(player)) {
            sendDebug(player, playerData, "exempt (permission)");
            return;
        }

        // Check for movement exemptions
        if (hasMovementExemption(playerData)) {
            sendDebug(player, playerData, "exempt (movement)");
            return;
        }

        // Flying players are exempt from speed checks
        if (playerData.isFlying()) {
            sendDebug(player, playerData, "exempt (flying)");
            return;
        }

        // Gliding players have different physics
        if (playerData.isGliding()) {
            sendDebug(player, playerData, "exempt (gliding)");
            return;
        }

        // Get position samples
        PositionHistory.PositionSample[] samples = getPositionSamples(playerData);
        if (samples == null) {
            sendDebug(player, playerData, "insufficient position history");
            return;
        }

        PositionHistory.PositionSample current = samples[0];
        PositionHistory.PositionSample previous = samples[1];

        double deltaX = current.x() - previous.x();
        double deltaZ = current.z() - previous.z();

        // Calculate horizontal speed
        double horizontalSpeed = calculateHorizontalSpeed(deltaX, deltaZ);

        // Skip if no significant movement
        if (horizontalSpeed < 0.001) {
            return; // Don't spam debug for idle
        }

        // Get expected max speed with tolerance
        double tolerance = getTolerance();
        double expectedMaxSpeed = getExpectedMaxSpeed(playerData, tolerance);

        // Infer movement state from observed speed
        String state = inferStateFromSpeed(horizontalSpeed);

        // Send debug output
        sendDebug(player, playerData, "speed=%.3f max=%.3f state=%s", horizontalSpeed, expectedMaxSpeed, state);

        // Check if speed exceeds expected
        if (horizontalSpeed > expectedMaxSpeed) {
            double speedDiff = horizontalSpeed - expectedMaxSpeed;
            double vlAmount = calculateVL(speedDiff, expectedMaxSpeed);

            String details = String.format(
                "speed=%.3f expected=%.3f diff=%.3f state=%s",
                horizontalSpeed,
                expectedMaxSpeed,
                speedDiff,
                state
            );

            flag(playerData, vlAmount, details);
        }
    }

    /**
     * Calculates VL based on speed difference.
     *
     * @param speedDiff the speed difference
     * @param maxSpeed the expected max speed
     * @return the VL amount (0-10)
     */
    private double calculateVL(double speedDiff, double maxSpeed) {
        // Scale VL based on how much they exceeded the limit
        double vlRatio = (speedDiff / maxSpeed) * 10.0;
        return Math.min(vlRatio, 10.0);
    }

    /**
     * Infers movement state from observed speed.
     * Since we can't access ECS components from the async thread,
     * we infer the state based on speed thresholds.
     *
     * Observed Hytale speeds:
     * - Walking: ~0.28-0.37 blocks/tick
     * - Sprinting: ~0.56-0.60 blocks/tick
     * - Sprint-jumping: ~0.70-0.75 blocks/tick
     *
     * @param speed the observed horizontal speed
     * @return inferred state description
     */
    private String inferStateFromSpeed(double speed) {
        if (speed < 0.05) {
            return "idle";
        } else if (speed <= 0.40) {
            return "walking";
        } else if (speed <= 0.65) {
            return "sprinting";
        } else {
            return "sprint-jumping";
        }
    }
}
