package dev.hypersystems.hyperguard.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for accessing ECS components from player references.
 * Provides type-safe access to common components used by anti-cheat checks.
 *
 * Note: Actual component access depends on the Hytale API version.
 * This class provides the framework for component access in Phase 2+.
 */
public final class ComponentUtil {

    private ComponentUtil() {
    }

    /**
     * Gets the entity reference from a PlayerRef.
     *
     * @param playerRef the player reference
     * @return the entity reference, or null if not available
     */
    @Nullable
    public static Ref<EntityStore> getEntityRef(@NotNull PlayerRef playerRef) {
        try {
            return playerRef.getReference();
        } catch (Exception e) {
            Logger.debug("Failed to get entity ref for %s: %s", playerRef.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the entity store from a PlayerRef.
     *
     * @param playerRef the player reference
     * @return the entity store, or null if not available
     */
    @Nullable
    public static Store<EntityStore> getEntityStore(@NotNull PlayerRef playerRef) {
        Ref<EntityStore> ref = getEntityRef(playerRef);
        if (ref == null) {
            return null;
        }
        try {
            return ref.getStore();
        } catch (Exception e) {
            Logger.debug("Failed to get entity store for %s: %s", playerRef.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets a component from a PlayerRef by class name.
     * Uses reflection to handle different API versions.
     *
     * @param playerRef the player reference
     * @param componentClassName the full class name of the component
     * @return the component, or null if not available
     */
    @Nullable
    public static Object getComponent(@NotNull PlayerRef playerRef, @NotNull String componentClassName) {
        Ref<EntityStore> ref = getEntityRef(playerRef);
        if (ref == null) {
            return null;
        }

        try {
            Store<EntityStore> store = ref.getStore();
            Class<?> componentClass = Class.forName(componentClassName);
            Object componentType = componentClass.getMethod("getComponentType").invoke(null);
            return store.getClass()
                .getMethod("getComponent", Ref.class, componentType.getClass())
                .invoke(store, ref, componentType);
        } catch (Exception e) {
            Logger.debug("Failed to get component %s for %s: %s",
                componentClassName, playerRef.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the player's current position using reflection.
     *
     * @param playerRef the player reference
     * @return array of [x, y, z], or null if not available
     */
    @Nullable
    public static double[] getPosition(@NotNull PlayerRef playerRef) {
        try {
            Object transform = getComponent(playerRef, "com.hypixel.hytale.component.components.TransformComponent");
            if (transform == null) {
                return null;
            }

            Class<?> transformClass = transform.getClass();
            double x = (double) transformClass.getMethod("getPositionX").invoke(transform);
            double y = (double) transformClass.getMethod("getPositionY").invoke(transform);
            double z = (double) transformClass.getMethod("getPositionZ").invoke(transform);

            return new double[] {x, y, z};
        } catch (Exception e) {
            Logger.debug("Failed to get position for %s: %s", playerRef.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the player's current rotation using reflection.
     *
     * @param playerRef the player reference
     * @return array of [yaw, pitch], or null if not available
     */
    @Nullable
    public static float[] getRotation(@NotNull PlayerRef playerRef) {
        try {
            Object transform = getComponent(playerRef, "com.hypixel.hytale.component.components.TransformComponent");
            if (transform == null) {
                return null;
            }

            Class<?> transformClass = transform.getClass();
            float yaw = (float) transformClass.getMethod("getRotationY").invoke(transform);
            float pitch = (float) transformClass.getMethod("getRotationX").invoke(transform);

            return new float[] {yaw, pitch};
        } catch (Exception e) {
            Logger.debug("Failed to get rotation for %s: %s", playerRef.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the player's current velocity using reflection.
     *
     * @param playerRef the player reference
     * @return array of [vx, vy, vz], or null if not available
     */
    @Nullable
    public static double[] getVelocity(@NotNull PlayerRef playerRef) {
        try {
            Object velocity = getComponent(playerRef, "com.hypixel.hytale.component.components.Velocity");
            if (velocity == null) {
                return null;
            }

            Class<?> velocityClass = velocity.getClass();
            double vx = (double) velocityClass.getMethod("getX").invoke(velocity);
            double vy = (double) velocityClass.getMethod("getY").invoke(velocity);
            double vz = (double) velocityClass.getMethod("getZ").invoke(velocity);

            return new double[] {vx, vy, vz};
        } catch (Exception e) {
            Logger.debug("Failed to get velocity for %s: %s", playerRef.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the player is on the ground using reflection.
     *
     * @param playerRef the player reference
     * @return true if on ground, false otherwise or if unknown
     */
    public static boolean isOnGround(@NotNull PlayerRef playerRef) {
        try {
            Object states = getComponent(playerRef, "com.hypixel.hytale.component.components.MovementStatesComponent");
            if (states == null) {
                return false;
            }

            return (boolean) states.getClass().getMethod("isOnGround").invoke(states);
        } catch (Exception e) {
            Logger.debug("Failed to check on ground for %s: %s", playerRef.getUsername(), e.getMessage());
            return false;
        }
    }
}
