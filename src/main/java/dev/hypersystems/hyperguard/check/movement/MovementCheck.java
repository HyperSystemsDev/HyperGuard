package dev.hypersystems.hyperguard.check.movement;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hypersystems.hyperguard.check.Check;
import dev.hypersystems.hyperguard.check.CheckType;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for movement-related anti-cheat checks.
 * Provides common functionality for accessing movement ECS components.
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
     * Gets movement data for a player.
     *
     * @param playerRef the player reference
     * @return the movement data, or null if unavailable
     */
    @Nullable
    protected MovementData getMovementData(@NotNull PlayerRef playerRef) {
        if (!playerRef.isValid()) {
            return null;
        }

        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return null;
        }

        try {
            Store<EntityStore> store = entityRef.getStore();
            if (store == null) {
                return null;
            }

            TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
            Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
            MovementStatesComponent movementStates = store.getComponent(entityRef, MovementStatesComponent.getComponentType());

            if (transform == null || velocity == null || movementStates == null) {
                return null;
            }

            return new MovementData(transform, velocity, movementStates);
        } catch (Exception e) {
            // Component access failed
            return null;
        }
    }

    /**
     * Checks if a player has movement exemptions that should skip checks.
     *
     * @param playerData the player data
     * @param states the movement states
     * @return true if the player should be exempt
     */
    protected boolean hasMovementExemption(@NotNull HGPlayerData playerData, @NotNull MovementStates states) {
        // Flying (creative mode) - exempt from most checks
        if (states.flying) {
            return true;
        }

        // Mounting (riding entity) - exempt from most checks
        if (states.mounting) {
            return true;
        }

        // Sitting - exempt
        if (states.sitting) {
            return true;
        }

        // Sleeping - exempt
        if (states.sleeping) {
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
     * Gets the expected maximum horizontal speed based on movement states.
     *
     * @param states the movement states
     * @param tolerance additional tolerance multiplier (e.g., 0.15 for 15% tolerance)
     * @return the maximum expected speed in blocks per tick
     */
    protected double getExpectedMaxSpeed(@NotNull MovementStates states, double tolerance) {
        double baseSpeed;

        if (states.flying) {
            baseSpeed = MovementConstants.FLY_SPEED;
        } else if (states.gliding) {
            baseSpeed = MovementConstants.GLIDE_SPEED;
        } else if (states.sprinting) {
            baseSpeed = MovementConstants.SPRINT_SPEED;
        } else if (states.swimming) {
            baseSpeed = MovementConstants.SWIM_SPEED;
        } else if (states.climbing) {
            baseSpeed = MovementConstants.CLIMB_SPEED;
        } else if (states.crouching || states.forcedCrouching) {
            baseSpeed = MovementConstants.SNEAK_SPEED;
        } else if (states.walking || states.running) {
            baseSpeed = MovementConstants.WALK_SPEED;
        } else {
            // Idle or other states
            baseSpeed = MovementConstants.WALK_SPEED;
        }

        // Apply water modifier
        if (states.inFluid) {
            baseSpeed *= MovementConstants.WATER_SPEED_MULTIPLIER;
        }

        // Apply tolerance
        return baseSpeed * (1.0 + tolerance);
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
     * Record class for holding movement-related ECS components.
     */
    public record MovementData(
        @NotNull TransformComponent transform,
        @NotNull Velocity velocity,
        @NotNull MovementStatesComponent movementStates
    ) {
        /**
         * Gets the current position.
         *
         * @return the position vector
         */
        @NotNull
        public Vector3d getPosition() {
            return transform.getPosition();
        }

        /**
         * Gets the current velocity.
         *
         * @return the velocity vector
         */
        @NotNull
        public Vector3d getVelocity() {
            return velocity.getVelocity();
        }

        /**
         * Gets the current movement states.
         *
         * @return the movement states
         */
        @NotNull
        public MovementStates getStates() {
            return movementStates.getMovementStates();
        }

        /**
         * Checks if the player is on the ground.
         *
         * @return true if on ground
         */
        public boolean isOnGround() {
            return movementStates.getMovementStates().onGround;
        }

        /**
         * Checks if the player is in a fluid.
         *
         * @return true if in fluid
         */
        public boolean isInFluid() {
            return movementStates.getMovementStates().inFluid;
        }

        /**
         * Checks if the player is climbing.
         *
         * @return true if climbing
         */
        public boolean isClimbing() {
            return movementStates.getMovementStates().climbing;
        }

        /**
         * Checks if the player is flying.
         *
         * @return true if flying
         */
        public boolean isFlying() {
            return movementStates.getMovementStates().flying;
        }

        /**
         * Checks if the player is gliding.
         *
         * @return true if gliding
         */
        public boolean isGliding() {
            return movementStates.getMovementStates().gliding;
        }
    }
}
