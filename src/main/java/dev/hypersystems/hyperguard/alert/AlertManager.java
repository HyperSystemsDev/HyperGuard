package dev.hypersystems.hyperguard.alert;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.action.ActionExecutor;
import dev.hypersystems.hyperguard.config.HyperGuardConfig;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.util.Logger;
import dev.hypersystems.hyperguard.violation.Violation;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.UUID;

/**
 * Manages staff alert notifications for violations.
 */
public final class AlertManager {

    private static final Color RED = new Color(255, 85, 85);
    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GRAY = Color.GRAY;
    private static final Color WHITE = Color.WHITE;

    private static AlertManager instance;

    private AlertManager() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return the alert manager
     */
    @NotNull
    public static AlertManager get() {
        if (instance == null) {
            instance = new AlertManager();
        }
        return instance;
    }

    /**
     * Initializes the alert manager.
     */
    public static void init() {
        instance = new AlertManager();
        Logger.debug("AlertManager initialized");
    }

    /**
     * Broadcasts a violation alert to all staff with alerts enabled.
     *
     * @param violation the violation to broadcast
     */
    public void broadcastViolation(@NotNull Violation violation) {
        HyperGuardConfig config = HyperGuardConfig.get();
        if (!config.getGeneral().isAlertsEnabled()) {
            return;
        }

        Message message = buildViolationMessage(violation);

        // Send to all tracked players who have alerts enabled
        for (HGPlayerData data : HGPlayerManager.get().getAllPlayerData()) {
            if (!data.areAlertsEnabled()) {
                continue;
            }

            // Check if player has alert permission
            if (hasAlertPermission(data.getUuid())) {
                PlayerRef player = ActionExecutor.get().getPlayer(data.getUuid());
                if (player != null) {
                    try {
                        player.sendMessage(message);
                    } catch (Exception e) {
                        Logger.debug("Failed to send alert to %s: %s", data.getUsername(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Builds a violation alert message.
     *
     * @param violation the violation
     * @return the formatted message
     */
    @NotNull
    private Message buildViolationMessage(@NotNull Violation violation) {
        return Message.raw("[HG] ").color(RED)
            .insert(Message.raw(violation.username() + " ").color(WHITE))
            .insert(Message.raw(violation.checkName() + " ").color(RED))
            .insert(Message.raw("(" + violation.checkType() + ") ").color(GRAY))
            .insert(Message.raw("VL: " + String.format("%.1f", violation.totalVL())).color(GOLD));
    }

    /**
     * Broadcasts a custom alert message to all staff.
     *
     * @param text the message to broadcast
     */
    public void broadcastAlert(@NotNull String text) {
        HyperGuardConfig config = HyperGuardConfig.get();

        Message message = Message.raw("[HG] ").color(RED)
            .insert(Message.raw(text).color(WHITE));

        for (HGPlayerData data : HGPlayerManager.get().getAllPlayerData()) {
            if (!data.areAlertsEnabled()) {
                continue;
            }

            if (hasAlertPermission(data.getUuid())) {
                PlayerRef player = ActionExecutor.get().getPlayer(data.getUuid());
                if (player != null) {
                    try {
                        player.sendMessage(message);
                    } catch (Exception e) {
                        Logger.debug("Failed to send alert to %s: %s", data.getUsername(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Sends an alert to a specific player.
     *
     * @param player the player to send to
     * @param text the message
     */
    public void sendAlert(@NotNull PlayerRef player, @NotNull String text) {
        Message message = Message.raw("[HG] ").color(RED)
            .insert(Message.raw(text).color(WHITE));
        try {
            player.sendMessage(message);
        } catch (Exception e) {
            Logger.debug("Failed to send alert: %s", e.getMessage());
        }
    }

    /**
     * Checks if a player has the alert permission.
     * Uses reflection to check for HyperPerms integration.
     *
     * @param uuid the player UUID
     * @return true if player has permission
     */
    private boolean hasAlertPermission(@NotNull UUID uuid) {
        String alertPermission = HyperGuardConfig.get().getGeneral().getAlertPermission();

        try {
            // Try HyperPerms integration
            Class<?> hyperPermsClass = Class.forName("com.hyperperms.HyperPerms");
            Object hyperPerms = hyperPermsClass.getMethod("get").invoke(null);
            Object permissionManager = hyperPermsClass.getMethod("getPermissionManager").invoke(hyperPerms);
            Boolean result = (Boolean) permissionManager.getClass()
                .getMethod("hasPermission", java.util.UUID.class, String.class)
                .invoke(permissionManager, uuid, alertPermission);
            return result != null && result;
        } catch (ClassNotFoundException e) {
            // HyperPerms not installed - assume all tracked players can see alerts
            // In production, this should integrate with native permissions
            return true;
        } catch (Exception e) {
            Logger.debug("Permission check failed for %s: %s", uuid, e.getMessage());
            return true;
        }
    }
}
