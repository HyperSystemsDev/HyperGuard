package dev.hypersystems.hyperguard.listener;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.action.ActionExecutor;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Listens for player connection events to manage player data lifecycle.
 */
public final class PlayerConnectionListener {

    private static PlayerConnectionListener instance;

    private PlayerConnectionListener() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return the listener instance
     */
    @NotNull
    public static PlayerConnectionListener get() {
        if (instance == null) {
            instance = new PlayerConnectionListener();
        }
        return instance;
    }

    /**
     * Initializes the listener.
     */
    public static void init() {
        instance = new PlayerConnectionListener();
        Logger.debug("PlayerConnectionListener initialized");
    }

    /**
     * Registers this listener with the event registry.
     *
     * @param eventRegistry the event registry
     */
    public void register(@NotNull EventRegistry eventRegistry) {
        eventRegistry.register(PlayerConnectEvent.class, this::onPlayerConnect);
        eventRegistry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        Logger.debug("PlayerConnectionListener registered with EventRegistry");
    }

    /**
     * Unregisters this listener.
     *
     * @param eventRegistry the event registry
     */
    public void unregister(@NotNull EventRegistry eventRegistry) {
        // EventRegistry doesn't have an unregister method in some versions
        // The listener will be cleaned up when the plugin shuts down
        Logger.debug("PlayerConnectionListener unregistered");
    }

    /**
     * Handles player connect events.
     * Creates player data when a player joins.
     *
     * @param event the connect event
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        try {
            PlayerRef playerRef = event.getPlayerRef();
            UUID uuid = playerRef.getUuid();
            String username = playerRef.getUsername();

            // Create player data
            HGPlayerData data = HGPlayerManager.get().createPlayerData(uuid, username);

            // Track player in ActionExecutor for message sending
            ActionExecutor.get().trackPlayer(uuid, playerRef);

            Logger.debug("Player connected: %s (%s)", username, uuid);

        } catch (Exception e) {
            Logger.warn("Error handling player connect: %s", e.getMessage());
        }
    }

    /**
     * Handles player disconnect events.
     * Cleans up player data when a player leaves.
     *
     * @param event the disconnect event
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        try {
            PlayerRef playerRef = event.getPlayerRef();
            UUID uuid = playerRef.getUuid();

            // Remove player data
            HGPlayerManager.get().removePlayerData(uuid);

            // Untrack from ActionExecutor
            ActionExecutor.get().untrackPlayer(uuid);

            Logger.debug("Player disconnected: %s (%s)", playerRef.getUsername(), uuid);

        } catch (Exception e) {
            Logger.warn("Error handling player disconnect: %s", e.getMessage());
        }
    }
}
