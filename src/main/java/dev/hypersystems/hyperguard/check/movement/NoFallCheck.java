package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.PositionHistory;
import org.jetbrains.annotations.NotNull;

/**
 * Detects fall damage avoidance (no-fall hack).
 * Tracks fall distance and checks if damage is received on landing.
 */
public final class NoFallCheck extends MovementCheck {

    private static final String FALL_DISTANCE_KEY = "nofall_distance";
    private static final String MAX_Y_VEL_KEY = "nofall_maxvel";
    private static final String WAS_ON_GROUND_KEY = "nofall_wasonground";
    private static final String FALL_START_Y_KEY = "nofall_startY";

    public NoFallCheck() {
        super("nofall");
    }

    @Override
    public void process(@NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        // Check if exempt
        if (isExempt(playerData)) {
            resetFallTracking(playerData);
            return;
        }

        // Check permission bypass
        if (hasExemptPermission(player)) {
            resetFallTracking(playerData);
            return;
        }

        // Check for movement exemptions
        if (hasMovementExemption(playerData)) {
            resetFallTracking(playerData);
            return;
        }

        // Flying - exempt (no fall damage in flight mode)
        if (playerData.isFlying()) {
            resetFallTracking(playerData);
            return;
        }

        // Gliding - exempt
        if (playerData.isGliding()) {
            resetFallTracking(playerData);
            return;
        }

        // Swimming - exempt (water negates fall damage)
        if (playerData.isSwimming()) {
            resetFallTracking(playerData);
            return;
        }

        // Climbing - exempt
        if (playerData.isClimbing()) {
            resetFallTracking(playerData);
            return;
        }

        // Get position samples
        PositionHistory.PositionSample[] samples = getPositionSamples(playerData);
        if (samples == null) {
            return;
        }

        PositionHistory.PositionSample current = samples[0];
        PositionHistory.PositionSample previous = samples[1];

        boolean wasOnGround = playerData.getCustomInt(WAS_ON_GROUND_KEY) == 1;
        boolean isOnGround = current.onGround();

        double currentY = current.y();
        double previousY = previous.y();
        double yDelta = currentY - previousY;

        // Track fall
        if (!isOnGround && yDelta < 0) {
            double fallDistance = playerData.getCustomDouble(FALL_DISTANCE_KEY);
            fallDistance += Math.abs(yDelta);
            playerData.setCustomDouble(FALL_DISTANCE_KEY, fallDistance);

            if (wasOnGround) {
                playerData.setCustomDouble(FALL_START_Y_KEY, previousY);
            }

            double maxYVel = playerData.getCustomDouble(MAX_Y_VEL_KEY);
            double currentFallSpeed = Math.abs(yDelta);
            if (currentFallSpeed > maxYVel) {
                playerData.setCustomDouble(MAX_Y_VEL_KEY, currentFallSpeed);
            }

            sendDebug(player, playerData, "falling: dist=%.2f yVel=%.3f", fallDistance, currentFallSpeed);
        }

        // Check for landing
        if (!wasOnGround && isOnGround) {
            checkLanding(player, playerData);
        }

        // Update ground state
        playerData.setCustomInt(WAS_ON_GROUND_KEY, isOnGround ? 1 : 0);
    }

    private void checkLanding(@NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        double fallDistance = playerData.getCustomDouble(FALL_DISTANCE_KEY);
        double maxYVel = playerData.getCustomDouble(MAX_Y_VEL_KEY);

        // Reset tracking after checking
        resetFallTracking(playerData);

        // Check if fall was significant
        if (fallDistance < MovementConstants.NOFALL_MIN_DISTANCE) {
            sendDebug(player, playerData, "landed: dist=%.2f (below threshold)", fallDistance);
            return;
        }

        if (maxYVel < MovementConstants.NOFALL_DAMAGE_VELOCITY) {
            sendDebug(player, playerData, "landed: vel=%.3f (below damage threshold)", maxYVel);
            return;
        }

        // Calculate expected damage
        double baseDamage = Math.pow(0.58 * (maxYVel - 0.5), 2);
        double expectedDamage = baseDamage * 1.1;

        if (expectedDamage > 0.5) {
            long ticksSinceDamage = playerData.getTicksSinceDamage();
            double tolerance = getTolerance();
            int damageWindowTicks = (int) (5 + (5 * tolerance));

            sendDebug(player, playerData, "landed: dist=%.2f vel=%.3f dmgExpected=%.2f lastDmg=%d ticks",
                fallDistance, maxYVel, expectedDamage, ticksSinceDamage);

            if (ticksSinceDamage > damageWindowTicks) {
                double vlAmount = Math.min(fallDistance * 0.5, 10.0);

                String details = String.format(
                    "fallDist=%.2f maxVel=%.3f expectedDmg=%.2f lastDmg=%d ticks ago",
                    fallDistance, maxYVel, expectedDamage, ticksSinceDamage
                );

                flag(playerData, vlAmount, details);
            }
        }
    }

    private void resetFallTracking(@NotNull HGPlayerData playerData) {
        playerData.resetCustomDouble(FALL_DISTANCE_KEY);
        playerData.resetCustomDouble(MAX_Y_VEL_KEY);
        playerData.resetCustomDouble(FALL_START_Y_KEY);
    }
}
