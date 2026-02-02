package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.PositionHistory;
import org.jetbrains.annotations.NotNull;

/**
 * Detects noclip/walking through blocks (phase hack).
 * Uses position history to detect impossible movement through terrain.
 * 
 * Note: Full collision validation requires main thread access.
 * This check uses heuristics based on movement patterns.
 */
public final class PhaseCheck extends MovementCheck {

    private static final String CONSECUTIVE_VIOLATIONS_KEY = "phase_violations";
    private static final String LAST_VALID_Y_KEY = "phase_lastvalidy";

    public PhaseCheck() {
        super("phase");
    }

    @Override
    public void process(@NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        // Check if exempt
        if (isExempt(playerData)) {
            resetViolations(playerData);
            return;
        }

        // Check permission bypass
        if (hasExemptPermission(player)) {
            resetViolations(playerData);
            return;
        }

        // Check for movement exemptions
        if (hasMovementExemption(playerData)) {
            resetViolations(playerData);
            return;
        }

        // Flying - exempt
        if (playerData.isFlying()) {
            resetViolations(playerData);
            return;
        }

        // Gliding - exempt
        if (playerData.isGliding()) {
            resetViolations(playerData);
            return;
        }

        // Get position samples
        PositionHistory.PositionSample[] samples = getPositionSamples(playerData);
        if (samples == null) {
            return;
        }

        PositionHistory.PositionSample current = samples[0];
        PositionHistory.PositionSample previous = samples[1];

        // Check for suspicious vertical movement through blocks
        checkVerticalPhase(player, playerData, current, previous);

        // Check for impossible horizontal movement speed (potential phase)
        checkHorizontalPhase(player, playerData, current, previous);
    }

    /**
     * Check for vertical phasing - moving up through blocks without jumping.
     */
    private void checkVerticalPhase(@NotNull PlayerRef player,
                                     @NotNull HGPlayerData playerData,
                                     @NotNull PositionHistory.PositionSample current,
                                     @NotNull PositionHistory.PositionSample previous) {
        double yDelta = current.y() - previous.y();

        // Check for rising without being on ground (potential vertical phase)
        // Normal jump gives ~0.42 initial velocity, anything higher is suspicious
        if (yDelta > 0.5 && !previous.onGround() && !playerData.isClimbing()) {
            int violations = playerData.incrementCustomInt(CONSECUTIVE_VIOLATIONS_KEY);

            sendDebug(player, playerData, "verticalPhase: yDelta=%.3f consecutive=%d", yDelta, violations);

            if (violations >= MovementConstants.PHASE_CONSECUTIVE_THRESHOLD) {
                double vlAmount = Math.min(yDelta * 5.0, 10.0);

                String details = String.format(
                    "verticalPhase: yDelta=%.3f consecutive=%d",
                    yDelta, violations
                );

                flag(playerData, vlAmount, details);
            }
        }
    }

    /**
     * Check for horizontal phasing - moving impossibly fast (through blocks).
     */
    private void checkHorizontalPhase(@NotNull PlayerRef player,
                                       @NotNull HGPlayerData playerData,
                                       @NotNull PositionHistory.PositionSample current,
                                       @NotNull PositionHistory.PositionSample previous) {
        double deltaX = current.x() - previous.x();
        double deltaZ = current.z() - previous.z();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // If moving more than 1 block per tick horizontally, very suspicious
        // This could indicate phasing through walls
        if (horizontalDistance > 1.0) {
            int violations = playerData.incrementCustomInt(CONSECUTIVE_VIOLATIONS_KEY);

            sendDebug(player, playerData, "horizontalPhase: dist=%.3f consecutive=%d", horizontalDistance, violations);

            if (violations >= MovementConstants.PHASE_CONSECUTIVE_THRESHOLD) {
                double vlAmount = Math.min(horizontalDistance * 3.0, 10.0);

                String details = String.format(
                    "horizontalPhase: distance=%.3f consecutive=%d",
                    horizontalDistance, violations
                );

                flag(playerData, vlAmount, details);
            }
        } else {
            // Reset if movement is normal
            if (horizontalDistance < 0.5) {
                resetViolations(playerData);
            }
        }
    }

    private void resetViolations(@NotNull HGPlayerData playerData) {
        playerData.resetCustomInt(CONSECUTIVE_VIOLATIONS_KEY);
    }
}
