package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.protocol.MovementStates;
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

        // Get movement data from ECS
        MovementData movementData = getMovementData(player);
        if (movementData == null) {
            return;
        }

        MovementStates states = movementData.getStates();

        // Check for movement exemptions
        if (hasMovementExemption(playerData, states)) {
            resetFallTracking(playerData);
            return;
        }

        // Flying - exempt (no fall damage in flight mode)
        if (states.flying) {
            resetFallTracking(playerData);
            return;
        }

        // Gliding - modified (reduced fall damage)
        if (states.gliding) {
            resetFallTracking(playerData);
            return;
        }

        // Swimming/InFluid - exempt (water negates fall damage)
        if (states.inFluid || states.swimming) {
            resetFallTracking(playerData);
            return;
        }

        // Climbing - exempt (ladder/vine negates fall damage)
        if (states.climbing) {
            resetFallTracking(playerData);
            return;
        }

        // Rolling - exempt (roll negates fall damage in some games)
        if (states.rolling) {
            resetFallTracking(playerData);
            return;
        }

        // Mounting - exempt (mount handles fall damage)
        if (states.mounting) {
            resetFallTracking(playerData);
            return;
        }

        // Get position history
        PositionHistory history = playerData.getPositionHistory();
        if (history.size() < 2) {
            return;
        }

        PositionHistory.PositionSample current = history.getLatest();
        PositionHistory.PositionSample previous = history.getPrevious();

        if (current == null || previous == null) {
            return;
        }

        boolean wasOnGround = playerData.getCustomInt(WAS_ON_GROUND_KEY) == 1;
        boolean isOnGround = states.onGround;

        double currentY = current.y();
        double previousY = previous.y();
        double yDelta = currentY - previousY;

        // Track fall
        if (!isOnGround && yDelta < 0) {
            // Falling - accumulate distance
            double fallDistance = playerData.getCustomDouble(FALL_DISTANCE_KEY);
            fallDistance += Math.abs(yDelta);
            playerData.setCustomDouble(FALL_DISTANCE_KEY, fallDistance);

            // Track start Y if just started falling
            if (wasOnGround) {
                playerData.setCustomDouble(FALL_START_Y_KEY, previousY);
            }

            // Track max fall velocity
            double maxYVel = playerData.getCustomDouble(MAX_Y_VEL_KEY);
            double currentFallSpeed = Math.abs(yDelta);
            if (currentFallSpeed > maxYVel) {
                playerData.setCustomDouble(MAX_Y_VEL_KEY, currentFallSpeed);
            }
        }

        // Check for landing (transition from air to ground)
        if (!wasOnGround && isOnGround) {
            checkLanding(playerData, states);
        }

        // Update ground state for next tick
        playerData.setCustomInt(WAS_ON_GROUND_KEY, isOnGround ? 1 : 0);
    }

    /**
     * Checks if fall damage should have been received on landing.
     */
    private void checkLanding(@NotNull HGPlayerData playerData, @NotNull MovementStates states) {
        double fallDistance = playerData.getCustomDouble(FALL_DISTANCE_KEY);
        double maxYVel = playerData.getCustomDouble(MAX_Y_VEL_KEY);
        double startY = playerData.getCustomDouble(FALL_START_Y_KEY);

        // Reset tracking after checking
        resetFallTracking(playerData);

        // Check if fall was significant enough to expect damage
        if (fallDistance < MovementConstants.NOFALL_MIN_DISTANCE) {
            return;
        }

        if (maxYVel < MovementConstants.NOFALL_DAMAGE_VELOCITY) {
            return;
        }

        // Calculate expected damage based on fall velocity
        // Formula: damage = (0.58 * (maxYVel - 0.5))^2 + 10%
        double baseDamage = Math.pow(0.58 * (maxYVel - 0.5), 2);
        double expectedDamage = baseDamage * 1.1; // +10% margin

        // Only flag if damage was expected and wasn't received recently
        if (expectedDamage > 0.5) {
            long ticksSinceDamage = playerData.getTicksSinceDamage();

            // Allow some ticks for damage to register
            double tolerance = getTolerance();
            int damageWindowTicks = (int) (5 + (5 * tolerance));

            if (ticksSinceDamage > damageWindowTicks) {
                // No damage received when it should have been
                double vlAmount = Math.min(fallDistance * 0.5, 10.0);

                String details = String.format(
                    "fallDist=%.2f maxVel=%.3f expectedDmg=%.2f lastDmg=%d ticks ago",
                    fallDistance,
                    maxYVel,
                    expectedDamage,
                    ticksSinceDamage
                );

                flag(playerData, vlAmount, details);
            }
        }
    }

    /**
     * Resets all fall tracking data.
     */
    private void resetFallTracking(@NotNull HGPlayerData playerData) {
        playerData.resetCustomDouble(FALL_DISTANCE_KEY);
        playerData.resetCustomDouble(MAX_Y_VEL_KEY);
        playerData.resetCustomDouble(FALL_START_Y_KEY);
    }
}
