package dev.hypersystems.hyperguard.check.movement;

/**
 * Constants for movement physics and anti-cheat calculations.
 * Based on Hytale's movement system physics.
 */
public final class MovementConstants {

    private MovementConstants() {
        // Utility class
    }

    // ==================== Movement Speeds (blocks/tick) ====================

    /**
     * Base walking speed in blocks per tick.
     */
    public static final double WALK_SPEED = 0.216;

    /**
     * Sprinting speed in blocks per tick.
     */
    public static final double SPRINT_SPEED = 0.281;

    /**
     * Sneaking/crouching speed in blocks per tick.
     */
    public static final double SNEAK_SPEED = 0.065;

    /**
     * Swimming speed in blocks per tick.
     */
    public static final double SWIM_SPEED = 0.115;

    /**
     * Climbing speed (ladders, vines) in blocks per tick.
     */
    public static final double CLIMB_SPEED = 0.118;

    /**
     * Flying speed (creative mode) in blocks per tick.
     */
    public static final double FLY_SPEED = 0.5;

    /**
     * Gliding speed (elytra-like) in blocks per tick.
     */
    public static final double GLIDE_SPEED = 1.0;

    // ==================== Physics Constants ====================

    /**
     * Gravity acceleration in blocks per tick squared.
     */
    public static final double GRAVITY = 0.08;

    /**
     * Terminal velocity (max falling speed) in blocks per tick.
     */
    public static final double TERMINAL_VELOCITY = 3.92;

    /**
     * Air resistance/drag coefficient.
     */
    public static final double AIR_DRAG = 0.98;

    /**
     * Jump velocity in blocks per tick.
     */
    public static final double JUMP_VELOCITY = 0.42;

    /**
     * Minimum Y velocity to consider as ascending.
     */
    public static final double MIN_ASCEND_VELOCITY = 0.01;

    // ==================== Exemption Ticks ====================

    /**
     * Ticks to exempt after taking damage (knockback).
     */
    public static final int POST_DAMAGE_EXEMPT_TICKS = 20;

    /**
     * Ticks to exempt after receiving server velocity.
     */
    public static final int POST_VELOCITY_EXEMPT_TICKS = 20;

    /**
     * Ticks to exempt after teleportation.
     */
    public static final int POST_TELEPORT_EXEMPT_TICKS = 40;

    /**
     * Ticks to exempt after joining the server.
     */
    public static final int POST_JOIN_EXEMPT_TICKS = 40;

    // ==================== Detection Thresholds ====================

    /**
     * Maximum consecutive ticks in air before fly detection triggers.
     */
    public static final int FLY_MAX_AIR_TICKS = 40;

    /**
     * Minimum fall distance to check for no-fall damage evasion.
     */
    public static final double NOFALL_MIN_DISTANCE = 3.0;

    /**
     * Y velocity threshold to expect fall damage.
     */
    public static final double NOFALL_DAMAGE_VELOCITY = 0.5;

    /**
     * Consecutive phase violations required before flagging.
     */
    public static final int PHASE_CONSECUTIVE_THRESHOLD = 3;

    /**
     * Minimum movement distance to check for phase through blocks.
     */
    public static final double PHASE_MIN_MOVEMENT = 0.5;

    // ==================== Speed Modifiers ====================

    /**
     * Speed multiplier when in water.
     */
    public static final double WATER_SPEED_MULTIPLIER = 0.5;

    /**
     * Speed multiplier when on ice.
     */
    public static final double ICE_SPEED_MULTIPLIER = 1.3;

    /**
     * Speed multiplier when using speed effects (per level).
     */
    public static final double SPEED_EFFECT_MULTIPLIER = 0.2;

    /**
     * Speed multiplier when using slowness effects (per level).
     */
    public static final double SLOWNESS_EFFECT_MULTIPLIER = 0.15;
}
