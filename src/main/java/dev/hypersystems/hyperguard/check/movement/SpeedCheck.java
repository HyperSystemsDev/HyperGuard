package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.protocol.MovementStates;
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
            return;
        }

        // Check permission bypass
        if (hasExemptPermission(player)) {
            return;
        }

        // Get movement data from ECS
        MovementData movementData = getMovementData(player);
        if (movementData == null) {
            return;
        }

        MovementStates states = movementData.getStates();

        // Check for movement exemptions
        if (hasMovementExemption(playerData, states)) {
            return;
        }

        // Flying players are exempt from speed checks
        if (states.flying) {
            return;
        }

        // Gliding players have different physics
        if (states.gliding) {
            return;
        }

        // Mounting players are exempt (mount controls speed)
        if (states.mounting) {
            return;
        }

        // Get position history
        PositionHistory history = playerData.getPositionHistory();
        if (history.size() < 2) {
            return;
        }

        // Calculate position delta from history
        PositionHistory.PositionSample current = history.getLatest();
        PositionHistory.PositionSample previous = history.getPrevious();

        if (current == null || previous == null) {
            return;
        }

        double deltaX = current.x() - previous.x();
        double deltaZ = current.z() - previous.z();

        // Calculate horizontal speed
        double horizontalSpeed = calculateHorizontalSpeed(deltaX, deltaZ);

        // Skip if no significant movement
        if (horizontalSpeed < 0.001) {
            return;
        }

        // Get expected max speed with tolerance
        double tolerance = getTolerance();
        double expectedMaxSpeed = getExpectedMaxSpeed(states, tolerance);

        // Account for rolling state (has burst speed)
        if (states.rolling) {
            expectedMaxSpeed *= 1.5;
        }

        // Account for sliding state
        if (states.sliding) {
            expectedMaxSpeed *= 1.3;
        }

        // Account for mantling (climbing up ledges)
        if (states.mantling) {
            expectedMaxSpeed *= 1.2;
        }

        // Account for jumping boost
        if (states.jumping) {
            expectedMaxSpeed *= 1.1;
        }

        // Check if speed exceeds expected
        if (horizontalSpeed > expectedMaxSpeed) {
            double speedDiff = horizontalSpeed - expectedMaxSpeed;
            double vlAmount = calculateVL(speedDiff, expectedMaxSpeed);

            String details = String.format(
                "speed=%.3f expected=%.3f diff=%.3f state=%s",
                horizontalSpeed,
                expectedMaxSpeed,
                speedDiff,
                getStateDescription(states)
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
     * Gets a description of the current movement state.
     *
     * @param states the movement states
     * @return a brief state description
     */
    private String getStateDescription(@NotNull MovementStates states) {
        if (states.sprinting) return "sprinting";
        if (states.swimming) return "swimming";
        if (states.climbing) return "climbing";
        if (states.crouching) return "crouching";
        if (states.walking) return "walking";
        if (states.running) return "running";
        if (states.idle) return "idle";
        return "unknown";
    }
}
