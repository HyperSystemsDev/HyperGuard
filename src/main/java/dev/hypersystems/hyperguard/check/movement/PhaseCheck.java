package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.modules.collision.CollisionModule;
import com.hypixel.hytale.server.core.modules.collision.CollisionResult;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.PositionHistory;
import org.jetbrains.annotations.NotNull;

/**
 * Detects noclip/walking through blocks (phase hack).
 * Uses collision validation to check if player is inside solid blocks.
 */
public final class PhaseCheck extends MovementCheck {

    private static final String CONSECUTIVE_VIOLATIONS_KEY = "phase_violations";

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

        // Get movement data from ECS
        MovementData movementData = getMovementData(player);
        if (movementData == null) {
            return;
        }

        MovementStates states = movementData.getStates();

        // Check for movement exemptions
        if (hasMovementExemption(playerData, states)) {
            resetViolations(playerData);
            return;
        }

        // Flying - exempt (can clip in creative)
        if (states.flying) {
            resetViolations(playerData);
            return;
        }

        // Gliding - exempt (different collision handling)
        if (states.gliding) {
            resetViolations(playerData);
            return;
        }

        // Mounting - exempt
        if (states.mounting) {
            resetViolations(playerData);
            return;
        }

        // Check current position for collision
        checkPositionValidity(player, playerData, movementData);

        // Check path for phase through blocks on large movements
        checkPathForPhasing(player, playerData, movementData);
    }

    /**
     * Validates if the player's current position is inside a solid block.
     */
    private void checkPositionValidity(@NotNull PlayerRef player,
                                        @NotNull HGPlayerData playerData,
                                        @NotNull MovementData movementData) {
        Ref<EntityStore> entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            World world = store.getExternalData().getWorld();
            if (world == null) {
                return;
            }

            // Get player bounding box
            BoundingBox boundingBoxComponent = store.getComponent(entityRef, BoundingBox.getComponentType());
            if (boundingBoxComponent == null) {
                return;
            }

            Box playerBox = boundingBoxComponent.getBoundingBox();
            if (playerBox == null) {
                return;
            }

            Vector3d position = movementData.getPosition();

            // Validate position using collision module
            CollisionModule collisionModule = CollisionModule.get();
            if (collisionModule == null) {
                return;
            }

            CollisionResult result = new CollisionResult();
            int validateResult = collisionModule.validatePosition(world, playerBox, position, result);

            // VALIDATE_INVALID (-1) means player is inside a solid block
            if (validateResult == CollisionModule.VALIDATE_INVALID) {
                int violations = playerData.incrementCustomInt(CONSECUTIVE_VIOLATIONS_KEY);

                // Only flag after consecutive violations to prevent lag false positives
                if (violations >= MovementConstants.PHASE_CONSECUTIVE_THRESHOLD) {
                    double vlAmount = Math.min(violations * 2.0, 10.0);

                    String details = String.format(
                        "pos=(%.2f, %.2f, %.2f) consecutive=%d",
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        violations
                    );

                    flag(playerData, vlAmount, details);
                }
            } else {
                // Valid position, reset violations
                resetViolations(playerData);
            }
        } catch (Exception e) {
            // Collision check failed, ignore
        }
    }

    /**
     * Checks the movement path for phasing through blocks on large movements.
     */
    private void checkPathForPhasing(@NotNull PlayerRef player,
                                      @NotNull HGPlayerData playerData,
                                      @NotNull MovementData movementData) {
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

        // Calculate movement distance
        double deltaX = current.x() - previous.x();
        double deltaY = current.y() - previous.y();
        double deltaZ = current.z() - previous.z();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        // Only check path for significant movements
        if (distance < MovementConstants.PHASE_MIN_MOVEMENT) {
            return;
        }

        Ref<EntityStore> entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            World world = store.getExternalData().getWorld();
            if (world == null) {
                return;
            }

            // Get player bounding box
            BoundingBox boundingBoxComponent = store.getComponent(entityRef, BoundingBox.getComponentType());
            if (boundingBoxComponent == null) {
                return;
            }

            Box playerBox = boundingBoxComponent.getBoundingBox();
            if (playerBox == null) {
                return;
            }

            // Create movement vector
            Vector3d startPos = new Vector3d(previous.x(), previous.y(), previous.z());
            Vector3d movementVector = new Vector3d(deltaX, deltaY, deltaZ);

            // Check for collisions along the path
            CollisionResult result = new CollisionResult();
            boolean isLongDistance = CollisionModule.findCollisions(
                playerBox, startPos, movementVector, result, store
            );

            // If there were collisions but player ended up past them, they phased through
            if (result.getBlockCollisionCount() > 0) {
                // Player moved through a block
                int violations = playerData.incrementCustomInt(CONSECUTIVE_VIOLATIONS_KEY);

                if (violations >= MovementConstants.PHASE_CONSECUTIVE_THRESHOLD) {
                    double vlAmount = Math.min(distance * 5.0, 10.0);

                    String details = String.format(
                        "pathPhase: from=(%.2f, %.2f, %.2f) to=(%.2f, %.2f, %.2f) dist=%.2f",
                        previous.x(), previous.y(), previous.z(),
                        current.x(), current.y(), current.z(),
                        distance
                    );

                    flag(playerData, vlAmount, details);
                }
            }
        } catch (Exception e) {
            // Path check failed, ignore
        }
    }

    /**
     * Resets consecutive violation tracking.
     */
    private void resetViolations(@NotNull HGPlayerData playerData) {
        playerData.resetCustomInt(CONSECUTIVE_VIOLATIONS_KEY);
    }
}
