package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.PositionHistory;
import org.jetbrains.annotations.NotNull;

/**
 * Detects unauthorized flight and gravity bypass.
 * Uses two detection methods: air time tracking and gravity consistency checks.
 */
public final class FlyCheck extends MovementCheck {

    private static final String AIR_TICKS_KEY = "flycheck_airticks";
    private static final String LAST_Y_KEY = "flycheck_lasty";
    private static final String EXPECTED_Y_VEL_KEY = "flycheck_expected_yvel";

    public FlyCheck() {
        super("fly");
    }

    @Override
    public void process(@NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        // Check if exempt
        if (isExempt(playerData)) {
            resetAirTicks(playerData);
            return;
        }

        // Check permission bypass
        if (hasExemptPermission(player)) {
            resetAirTicks(playerData);
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
            resetAirTicks(playerData);
            return;
        }

        // Flying (creative mode) - exempt
        if (states.flying) {
            resetAirTicks(playerData);
            return;
        }

        // Gliding - exempt (elytra-like behavior)
        if (states.gliding) {
            resetAirTicks(playerData);
            return;
        }

        // Mounting - exempt (mount controls physics)
        if (states.mounting) {
            resetAirTicks(playerData);
            return;
        }

        // Run both detection methods
        checkAirTime(playerData, movementData, states);
        checkGravity(playerData, movementData, states);
    }

    /**
     * Air Time Check: Track consecutive ticks in air without ground contact.
     * Flags after too many ticks without touching ground, fluid, or climbing.
     */
    private void checkAirTime(@NotNull HGPlayerData playerData,
                              @NotNull MovementData movementData,
                              @NotNull MovementStates states) {
        boolean inAir = !states.onGround && !states.inFluid && !states.climbing && !states.mantling;

        if (inAir) {
            int airTicks = playerData.incrementCustomInt(AIR_TICKS_KEY);

            // Flag after exceeding threshold
            if (airTicks > MovementConstants.FLY_MAX_AIR_TICKS) {
                double vlAmount = Math.min((airTicks - MovementConstants.FLY_MAX_AIR_TICKS) * 0.5, 10.0);

                String details = String.format(
                    "airTicks=%d threshold=%d",
                    airTicks,
                    MovementConstants.FLY_MAX_AIR_TICKS
                );

                flag(playerData, vlAmount, details);
            }
        } else {
            // Reset air ticks when grounded, in fluid, or climbing
            resetAirTicks(playerData);
        }
    }

    /**
     * Gravity Check: Verify Y velocity follows expected physics.
     * Detects ascending when should be falling or hovering in place.
     */
    private void checkGravity(@NotNull HGPlayerData playerData,
                              @NotNull MovementData movementData,
                              @NotNull MovementStates states) {
        // Skip gravity checks in certain states
        if (states.onGround || states.inFluid || states.climbing || states.mantling || states.swimJumping) {
            resetGravityTracking(playerData);
            return;
        }

        // Get position history for Y delta
        PositionHistory history = playerData.getPositionHistory();
        if (history.size() < 2) {
            return;
        }

        PositionHistory.PositionSample current = history.getLatest();
        PositionHistory.PositionSample previous = history.getPrevious();

        if (current == null || previous == null) {
            return;
        }

        double currentY = current.y();
        double previousY = previous.y();
        double actualYDelta = currentY - previousY;

        // Get expected Y velocity from last tick
        double expectedYVel = playerData.getCustomDouble(EXPECTED_Y_VEL_KEY);

        // First tick tracking, skip check
        if (expectedYVel == 0.0 && playerData.getCustomDouble(LAST_Y_KEY) == 0.0) {
            updateGravityTracking(playerData, currentY, actualYDelta);
            return;
        }

        // Check for jumping (gives upward velocity)
        if (states.jumping) {
            // Allow jump velocity
            updateGravityTracking(playerData, currentY, MovementConstants.JUMP_VELOCITY);
            return;
        }

        // Calculate what the Y velocity should be after gravity
        double nextExpectedYVel = expectedYVel - MovementConstants.GRAVITY;

        // Allow tolerance for precision errors
        double tolerance = getTolerance();
        double toleranceAmount = Math.max(0.05, Math.abs(expectedYVel) * tolerance);

        // Check if ascending when should be falling
        if (expectedYVel < -MovementConstants.MIN_ASCEND_VELOCITY) {
            // Should be falling
            if (actualYDelta > toleranceAmount) {
                // Ascending when should be falling = likely fly hack
                double vlAmount = Math.min(Math.abs(actualYDelta - expectedYVel) * 5.0, 10.0);

                String details = String.format(
                    "yDelta=%.3f expected=%.3f ascending when should fall",
                    actualYDelta,
                    expectedYVel
                );

                flag(playerData, vlAmount, details);
            }
        }

        // Check for hovering (very small Y movement when should be falling fast)
        if (expectedYVel < -0.3 && Math.abs(actualYDelta) < 0.01) {
            double vlAmount = 5.0;

            String details = String.format(
                "hovering: yDelta=%.3f expected=%.3f",
                actualYDelta,
                expectedYVel
            );

            flag(playerData, vlAmount, details);
        }

        // Update tracking for next tick
        updateGravityTracking(playerData, currentY, actualYDelta);
    }

    /**
     * Resets air tick tracking.
     */
    private void resetAirTicks(@NotNull HGPlayerData playerData) {
        playerData.resetCustomInt(AIR_TICKS_KEY);
    }

    /**
     * Resets gravity tracking.
     */
    private void resetGravityTracking(@NotNull HGPlayerData playerData) {
        playerData.resetCustomDouble(LAST_Y_KEY);
        playerData.resetCustomDouble(EXPECTED_Y_VEL_KEY);
    }

    /**
     * Updates gravity tracking for next tick.
     */
    private void updateGravityTracking(@NotNull HGPlayerData playerData, double currentY, double currentYVel) {
        playerData.setCustomDouble(LAST_Y_KEY, currentY);
        // Next expected velocity is current minus gravity (capped at terminal velocity)
        double nextExpected = Math.max(currentYVel - MovementConstants.GRAVITY, -MovementConstants.TERMINAL_VELOCITY);
        playerData.setCustomDouble(EXPECTED_Y_VEL_KEY, nextExpected);
    }
}
