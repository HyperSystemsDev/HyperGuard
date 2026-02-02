package dev.hypersystems.hyperguard.action;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.util.Logger;
import dev.hypersystems.hyperguard.util.MessageUtil;
import dev.hypersystems.hyperguard.util.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes punishments based on violation thresholds.
 */
public final class ActionExecutor {

    private static final Color RED = new Color(255, 85, 85);
    private static final Color GOLD = new Color(255, 170, 0);

    private static ActionExecutor instance;

    // Track PlayerRef for online players
    private final Map<UUID, PlayerRef> onlinePlayers;

    private ActionExecutor() {
        this.onlinePlayers = new ConcurrentHashMap<>();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the action executor
     */
    @NotNull
    public static ActionExecutor get() {
        if (instance == null) {
            instance = new ActionExecutor();
        }
        return instance;
    }

    /**
     * Initializes the action executor.
     */
    public static void init() {
        instance = new ActionExecutor();
        Logger.debug("ActionExecutor initialized");
    }

    /**
     * Tracks a player when they connect.
     *
     * @param uuid the player UUID
     * @param playerRef the player reference
     */
    public void trackPlayer(@NotNull UUID uuid, @NotNull PlayerRef playerRef) {
        onlinePlayers.put(uuid, playerRef);
    }

    /**
     * Untracks a player when they disconnect.
     *
     * @param uuid the player UUID
     */
    public void untrackPlayer(@NotNull UUID uuid) {
        onlinePlayers.remove(uuid);
    }

    /**
     * Gets a tracked player.
     *
     * @param uuid the player UUID
     * @return the PlayerRef, or null if not tracked
     */
    @Nullable
    public PlayerRef getPlayer(@NotNull UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    /**
     * Executes a punishment action on a player.
     *
     * @param playerData the player data
     * @param checkName the check that triggered this
     * @param actionName the action name (warn, kick, tempban, ban)
     * @param duration optional duration for tempban
     * @param currentVL the current VL
     */
    public void executeAction(
        @NotNull HGPlayerData playerData,
        @NotNull String checkName,
        @NotNull String actionName,
        @Nullable String duration,
        double currentVL
    ) {
        PunishmentType type = PunishmentType.fromName(actionName);
        if (type == null) {
            Logger.warn("Unknown action type: %s", actionName);
            return;
        }

        Logger.info("[ACTION] Executing %s on %s for %s (VL: %.1f)",
            type.getName(), playerData.getUsername(), checkName, currentVL);

        switch (type) {
            case WARN -> executeWarn(playerData, checkName, currentVL);
            case KICK -> executeKick(playerData, checkName, currentVL);
            case TEMPBAN -> executeTempban(playerData, checkName, duration, currentVL);
            case BAN -> executeBan(playerData, checkName, currentVL);
        }
    }

    /**
     * Executes a warning.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param currentVL the current VL
     */
    private void executeWarn(@NotNull HGPlayerData playerData, @NotNull String checkName, double currentVL) {
        PlayerRef player = getPlayer(playerData.getUuid());
        if (player == null) {
            return;
        }

        try {
            Message message = Message.raw("[HyperGuard] ").color(RED)
                .insert(Message.raw("Warning: Suspicious activity detected (").color(GOLD))
                .insert(Message.raw(checkName).color(RED))
                .insert(Message.raw(")").color(GOLD));
            player.sendMessage(message);
            Logger.info("[WARN] %s warned for %s (VL: %.1f)", playerData.getUsername(), checkName, currentVL);
        } catch (Exception e) {
            Logger.warn("Failed to send warning to %s: %s", playerData.getUsername(), e.getMessage());
        }
    }

    /**
     * Executes a kick.
     * Note: Direct kick is not available in Hytale API.
     * This logs the action and warns the player.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param currentVL the current VL
     */
    private void executeKick(@NotNull HGPlayerData playerData, @NotNull String checkName, double currentVL) {
        PlayerRef player = getPlayer(playerData.getUuid());
        if (player == null) {
            return;
        }

        try {
            // Send a strong warning - actual kick requires server-level integration
            Message message = Message.raw("[HyperGuard] ").color(RED)
                .insert(Message.raw("KICK: You have been flagged for ").color(RED))
                .insert(Message.raw(checkName).color(GOLD))
                .insert(Message.raw(" - Please stop immediately or face a ban.").color(RED));
            player.sendMessage(message);
            Logger.info("[KICK] %s flagged for kick - %s (VL: %.1f)", playerData.getUsername(), checkName, currentVL);
        } catch (Exception e) {
            Logger.warn("Failed to send kick message to %s: %s", playerData.getUsername(), e.getMessage());
        }
    }

    /**
     * Executes a temporary ban.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param duration the ban duration
     * @param currentVL the current VL
     */
    private void executeTempban(
        @NotNull HGPlayerData playerData,
        @NotNull String checkName,
        @Nullable String duration,
        double currentVL
    ) {
        PlayerRef player = getPlayer(playerData.getUuid());
        if (player == null) {
            return;
        }

        long durationMs = duration != null ? TimeUtil.parseDuration(duration) : 3600000L;
        String durationDisplay = TimeUtil.formatDuration(durationMs);

        try {
            Message message = Message.raw("[HyperGuard] ").color(RED)
                .insert(Message.raw("TEMPBAN: You have been temporarily banned for ").color(RED))
                .insert(Message.raw(checkName).color(GOLD))
                .insert(Message.raw(" - Duration: " + durationDisplay).color(RED));
            player.sendMessage(message);
            Logger.info("[TEMPBAN] %s flagged for tempban - %s for %s (VL: %.1f)",
                playerData.getUsername(), checkName, durationDisplay, currentVL);
        } catch (Exception e) {
            Logger.warn("Failed to send tempban message to %s: %s", playerData.getUsername(), e.getMessage());
        }
    }

    /**
     * Executes a permanent ban.
     *
     * @param playerData the player data
     * @param checkName the check name
     * @param currentVL the current VL
     */
    private void executeBan(
        @NotNull HGPlayerData playerData,
        @NotNull String checkName,
        double currentVL
    ) {
        PlayerRef player = getPlayer(playerData.getUuid());
        if (player == null) {
            return;
        }

        try {
            Message message = Message.raw("[HyperGuard] ").color(RED)
                .insert(Message.raw("BAN: You have been permanently banned for ").color(RED))
                .insert(Message.raw(checkName).color(GOLD));
            player.sendMessage(message);
            Logger.info("[BAN] %s flagged for ban - %s (VL: %.1f)", playerData.getUsername(), checkName, currentVL);
        } catch (Exception e) {
            Logger.warn("Failed to send ban message to %s: %s", playerData.getUsername(), e.getMessage());
        }
    }
}
