package dev.hypersystems.hyperguard.check;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.config.CheckConfig;
import dev.hypersystems.hyperguard.config.HyperGuardConfig;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.violation.Violation;
import dev.hypersystems.hyperguard.violation.ViolationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for all anti-cheat checks.
 * Provides common functionality for flagging, exemption checking, and config access.
 */
public abstract class Check {

    private final String name;
    private final CheckType type;
    private boolean enabled;

    /**
     * Creates a new check.
     *
     * @param name the check name (should match config key)
     * @param type the check type
     */
    protected Check(@NotNull String name, @NotNull CheckType type) {
        this.name = name.toLowerCase();
        this.type = type;
        this.enabled = true;
    }

    /**
     * Gets the check name.
     *
     * @return the name
     */
    @NotNull
    public final String getName() {
        return name;
    }

    /**
     * Gets the check type.
     *
     * @return the type
     */
    @NotNull
    public final CheckType getType() {
        return type;
    }

    /**
     * Checks if this check is enabled.
     *
     * @return true if enabled
     */
    public final boolean isEnabled() {
        CheckConfig config = getConfig();
        return enabled && (config == null || config.isEnabled());
    }

    /**
     * Sets whether this check is enabled.
     *
     * @param enabled true to enable
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the configuration for this check.
     *
     * @return the check config, or null if not found
     */
    @Nullable
    public final CheckConfig getConfig() {
        return HyperGuardConfig.get().getCheckConfig(name);
    }

    /**
     * Gets the tolerance value from config.
     *
     * @return the tolerance, or 0.1 as default
     */
    protected final double getTolerance() {
        CheckConfig config = getConfig();
        return config != null ? config.getTolerance() : 0.1;
    }

    /**
     * Flags a player for this check.
     *
     * @param playerData the player data
     * @param baseVL the base VL to add
     * @param details optional details
     * @return the violation, or null if not flagged
     */
    @Nullable
    protected final Violation flag(@NotNull HGPlayerData playerData, double baseVL, @Nullable String details) {
        return ViolationManager.get().flag(playerData, name, type.getDisplayName(), baseVL, details);
    }

    /**
     * Flags a player without details.
     *
     * @param playerData the player data
     * @param baseVL the base VL to add
     * @return the violation, or null if not flagged
     */
    @Nullable
    protected final Violation flag(@NotNull HGPlayerData playerData, double baseVL) {
        return flag(playerData, baseVL, null);
    }

    /**
     * Checks if a player is exempt from this check.
     *
     * @param playerData the player data
     * @return true if exempt
     */
    protected final boolean isExempt(@NotNull HGPlayerData playerData) {
        // Check global exemption
        if (playerData.isGloballyExempt()) {
            return true;
        }

        // Check specific exemption
        if (playerData.isExempt(name)) {
            return true;
        }

        // Check join exemption
        HyperGuardConfig config = HyperGuardConfig.get();
        if (playerData.getTicksSinceJoin() < config.getGeneral().getJoinExemptionTicks()) {
            return true;
        }

        // Check teleport exemption
        if (playerData.getTicksSinceTeleport() < config.getGeneral().getTeleportExemptionTicks()) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a player is exempt by permission.
     *
     * @param player the player
     * @return true if has bypass permission
     */
    protected final boolean hasExemptPermission(@NotNull PlayerRef player) {
        String bypassPermission = HyperGuardConfig.get().getGeneral().getBypassPermission();

        // Check global bypass
        if (hasPermission(player, bypassPermission)) {
            return true;
        }

        // Check check-specific bypass
        return hasPermission(player, bypassPermission + "." + name);
    }

    /**
     * Checks if a player has a permission.
     * Uses reflection to check HyperPerms integration.
     *
     * @param player the player
     * @param permission the permission node
     * @return true if player has permission
     */
    protected final boolean hasPermission(@NotNull PlayerRef player, @NotNull String permission) {
        try {
            Class<?> hyperPermsClass = Class.forName("com.hyperperms.HyperPerms");
            Object hyperPerms = hyperPermsClass.getMethod("get").invoke(null);
            Object permissionManager = hyperPermsClass.getMethod("getPermissionManager").invoke(hyperPerms);
            Boolean result = (Boolean) permissionManager.getClass()
                .getMethod("hasPermission", java.util.UUID.class, String.class)
                .invoke(permissionManager, player.getUuid(), permission);
            return result != null && result;
        } catch (ClassNotFoundException e) {
            // HyperPerms not installed - no fallback available
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets player data for a PlayerRef.
     *
     * @param player the player
     * @return the player data, or null if not found
     */
    @Nullable
    protected final HGPlayerData getPlayerData(@NotNull PlayerRef player) {
        return HGPlayerManager.get().getPlayerData(player);
    }

    /**
     * Checks if a player is in an exempt gamemode.
     *
     * @param player the player
     * @return true if in exempt gamemode
     */
    protected final boolean isInExemptGamemode(@NotNull PlayerRef player) {
        // This would require checking the player's current gamemode
        // Implementation depends on how Hytale exposes gamemode info
        // For now, return false as gamemode checking needs ECS component access
        return false;
    }

    /**
     * Called to process the check for a player.
     * Override this in subclasses to implement check logic.
     *
     * @param player the player to check
     * @param playerData the player data
     */
    public abstract void process(@NotNull PlayerRef player, @NotNull HGPlayerData playerData);

    @Override
    public String toString() {
        return String.format("Check{name='%s', type=%s, enabled=%s}", name, type, isEnabled());
    }
}
