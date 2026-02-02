package dev.hypersystems.hyperguard.check.movement;

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
    private static final String WAS_ON_GROUND_KEY = "flycheck_wasonground";
    private static final String JUMP_TICKS_KEY = "flycheck_jumpticks";
    private static final String LAST_Y_DELTA_KEY = "flycheck_lastydelta";

    // Number of ticks to allow ascending after a jump
    private static final int JUMP_GRACE_TICKS = 15;

    // Minimum upward velocity to consider as a new jump
    private static final double JUMP_VELOCITY_THRESHOLD = 0.30;

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

        // Check for movement exemptions
        if (hasMovementExemption(playerData)) {
            resetAirTicks(playerData);
            return;
        }

        // Flying (creative mode) - exempt
        if (playerData.isFlying()) {
            resetAirTicks(playerData);
            sendDebug(player, playerData, "exempt (flying)");
            return;
        }

        // Gliding - exempt (elytra-like behavior)
        if (playerData.isGliding()) {
            resetAirTicks(playerData);
            sendDebug(player, playerData, "exempt (gliding)");
            return;
        }

        // Get position samples
        PositionHistory.PositionSample[] samples = getPositionSamples(playerData);
        if (samples == null) {
            return;
        }

        PositionHistory.PositionSample current = samples[0];
        PositionHistory.PositionSample previous = samples[1];

        // Run both detection methods
        checkAirTime(player, playerData, current, previous);
        checkGravity(player, playerData, current, previous);
    }

    /**
     * Air Time Check: Track consecutive ticks in air without ground contact.
     */
    private void checkAirTime(@NotNull PlayerRef player,
                              @NotNull HGPlayerData playerData,
                              @NotNull PositionHistory.PositionSample current,
                              @NotNull PositionHistory.PositionSample previous) {
        // Use onGround from position sample
        boolean onGround = current.onGround();
        boolean inWater = playerData.isSwimming();
        boolean climbing = playerData.isClimbing();

        boolean inAir = !onGround && !inWater && !climbing;

        if (inAir) {
            int airTicks = playerData.incrementCustomInt(AIR_TICKS_KEY);

            sendDebug(player, playerData, "airTicks=%d (max=%d)", airTicks, MovementConstants.FLY_MAX_AIR_TICKS);

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
     * Properly accounts for jumping by detecting:
     * 1. Player leaving ground with upward velocity
     * 2. Sudden upward velocity change (bunny hopping / consecutive jumps)
     */
    private void checkGravity(@NotNull PlayerRef player,
                              @NotNull HGPlayerData playerData,
                              @NotNull PositionHistory.PositionSample current,
                              @NotNull PositionHistory.PositionSample previous) {
        boolean currentOnGround = current.onGround();
        boolean wasOnGround = playerData.getCustomInt(WAS_ON_GROUND_KEY) == 1;

        // Update ground tracking for next tick
        playerData.setCustomInt(WAS_ON_GROUND_KEY, currentOnGround ? 1 : 0);

        // Skip gravity checks in certain states
        if (currentOnGround || playerData.isSwimming() || playerData.isClimbing()) {
            resetGravityTracking(playerData);
            return;
        }

        double currentY = current.y();
        double previousY = previous.y();
        double actualYDelta = currentY - previousY;
        double lastYDelta = playerData.getCustomDouble(LAST_Y_DELTA_KEY);

        // Update last Y delta for next tick
        playerData.setCustomDouble(LAST_Y_DELTA_KEY, actualYDelta);

        // Detect jump method 1: was on ground last tick, now in air and moving up
        boolean jumpFromGround = wasOnGround && actualYDelta > 0.1;

        // Detect jump method 2: sudden upward velocity change (was falling/neutral, now ascending)
        // This catches bunny hopping where ground detection might miss the brief landing
        boolean jumpFromVelocityChange = lastYDelta < 0.1 && actualYDelta > JUMP_VELOCITY_THRESHOLD;

        if (jumpFromGround || jumpFromVelocityChange) {
            // Player just jumped - start/reset jump grace period
            playerData.setCustomInt(JUMP_TICKS_KEY, JUMP_GRACE_TICKS);
            // Initialize expected velocity to jump velocity
            updateGravityTracking(playerData, currentY, MovementConstants.JUMP_VELOCITY);
            return;
        }

        // Decrement jump grace ticks if active
        int jumpTicks = playerData.getCustomInt(JUMP_TICKS_KEY);
        if (jumpTicks > 0) {
            playerData.setCustomInt(JUMP_TICKS_KEY, jumpTicks - 1);
            // During jump grace period, allow ascending - just update tracking
            updateGravityTracking(playerData, currentY, actualYDelta);
            return;
        }

        // Get expected Y velocity from last tick
        double expectedYVel = playerData.getCustomDouble(EXPECTED_Y_VEL_KEY);

        // First tick tracking after landing, skip check
        if (expectedYVel == 0.0 && playerData.getCustomDouble(LAST_Y_KEY) == 0.0) {
            updateGravityTracking(playerData, currentY, actualYDelta);
            return;
        }

        // Allow tolerance for precision errors
        double tolerance = getTolerance();
        double toleranceAmount = Math.max(0.15, Math.abs(expectedYVel) * tolerance);

        // Check if ascending when should be falling (after jump grace period)
        if (expectedYVel < -MovementConstants.MIN_ASCEND_VELOCITY) {
            if (actualYDelta > toleranceAmount) {
                double vlAmount = Math.min(Math.abs(actualYDelta - expectedYVel) * 5.0, 10.0);

                String details = String.format(
                    "yDelta=%.3f expected=%.3f (ascending when should fall)",
                    actualYDelta,
                    expectedYVel
                );

                flag(playerData, vlAmount, details);
            }
        }

        // Check for hovering (staying at same height when should fall)
        if (expectedYVel < -0.5 && Math.abs(actualYDelta) < 0.02) {
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

    private void resetAirTicks(@NotNull HGPlayerData playerData) {
        playerData.resetCustomInt(AIR_TICKS_KEY);
    }

    private void resetGravityTracking(@NotNull HGPlayerData playerData) {
        playerData.resetCustomDouble(LAST_Y_KEY);
        playerData.resetCustomDouble(EXPECTED_Y_VEL_KEY);
        playerData.resetCustomDouble(LAST_Y_DELTA_KEY);
        playerData.resetCustomInt(JUMP_TICKS_KEY);
    }

    private void updateGravityTracking(@NotNull HGPlayerData playerData, double currentY, double currentYVel) {
        playerData.setCustomDouble(LAST_Y_KEY, currentY);
        double nextExpected = Math.max(currentYVel - MovementConstants.GRAVITY, -MovementConstants.TERMINAL_VELOCITY);
        playerData.setCustomDouble(EXPECTED_Y_VEL_KEY, nextExpected);
    }
}
