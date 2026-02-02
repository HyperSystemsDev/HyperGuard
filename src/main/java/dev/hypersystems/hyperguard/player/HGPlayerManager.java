package dev.hypersystems.hyperguard.player;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data lifecycle for HyperGuard.
 * Handles creation and cleanup of player tracking data.
 */
public final class HGPlayerManager {

    private static HGPlayerManager instance;

    private final Map<UUID, HGPlayerData> playerData;

    private HGPlayerManager() {
        this.playerData = new ConcurrentHashMap<>();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the player manager instance
     */
    @NotNull
    public static HGPlayerManager get() {
        if (instance == null) {
            instance = new HGPlayerManager();
        }
        return instance;
    }

    /**
     * Initializes the player manager.
     * Called on plugin startup.
     */
    public static void init() {
        instance = new HGPlayerManager();
        Logger.debug("HGPlayerManager initialized");
    }

    /**
     * Shuts down the player manager.
     * Called on plugin shutdown.
     */
    public void shutdown() {
        playerData.clear();
        Logger.debug("HGPlayerManager shut down");
    }

    /**
     * Creates player data for a connecting player.
     *
     * @param uuid the player UUID
     * @param username the player username
     * @return the created player data
     */
    @NotNull
    public HGPlayerData createPlayerData(@NotNull UUID uuid, @NotNull String username) {
        HGPlayerData data = new HGPlayerData(uuid, username);
        playerData.put(uuid, data);
        Logger.debug("Created player data for %s (%s)", username, uuid);
        return data;
    }

    /**
     * Creates player data from a PlayerRef.
     *
     * @param playerRef the player reference
     * @return the created player data
     */
    @NotNull
    public HGPlayerData createPlayerData(@NotNull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();
        return createPlayerData(uuid, username);
    }

    /**
     * Removes player data for a disconnecting player.
     *
     * @param uuid the player UUID
     */
    public void removePlayerData(@NotNull UUID uuid) {
        HGPlayerData data = playerData.remove(uuid);
        if (data != null) {
            Logger.debug("Removed player data for %s (%s)", data.getUsername(), uuid);
        }
    }

    /**
     * Gets player data by UUID.
     *
     * @param uuid the player UUID
     * @return the player data, or null if not found
     */
    @Nullable
    public HGPlayerData getPlayerData(@NotNull UUID uuid) {
        return playerData.get(uuid);
    }

    /**
     * Gets player data from a PlayerRef.
     *
     * @param playerRef the player reference
     * @return the player data, or null if not found
     */
    @Nullable
    public HGPlayerData getPlayerData(@NotNull PlayerRef playerRef) {
        return getPlayerData(playerRef.getUuid());
    }

    /**
     * Gets or creates player data.
     *
     * @param uuid the player UUID
     * @param username the player username
     * @return the existing or new player data
     */
    @NotNull
    public HGPlayerData getOrCreatePlayerData(@NotNull UUID uuid, @NotNull String username) {
        return playerData.computeIfAbsent(uuid, k -> {
            Logger.debug("Created player data for %s (%s)", username, uuid);
            return new HGPlayerData(uuid, username);
        });
    }

    /**
     * Gets or creates player data from a PlayerRef.
     *
     * @param playerRef the player reference
     * @return the existing or new player data
     */
    @NotNull
    public HGPlayerData getOrCreatePlayerData(@NotNull PlayerRef playerRef) {
        return getOrCreatePlayerData(playerRef.getUuid(), playerRef.getUsername());
    }

    /**
     * Checks if player data exists for a UUID.
     *
     * @param uuid the player UUID
     * @return true if data exists
     */
    public boolean hasPlayerData(@NotNull UUID uuid) {
        return playerData.containsKey(uuid);
    }

    /**
     * Gets all player data.
     *
     * @return unmodifiable collection of all player data
     */
    @NotNull
    public Collection<HGPlayerData> getAllPlayerData() {
        return Collections.unmodifiableCollection(playerData.values());
    }

    /**
     * Gets all online player UUIDs.
     *
     * @return unmodifiable set of UUIDs
     */
    @NotNull
    public Set<UUID> getAllPlayerUUIDs() {
        return Collections.unmodifiableSet(playerData.keySet());
    }

    /**
     * Gets the number of tracked players.
     *
     * @return player count
     */
    public int getPlayerCount() {
        return playerData.size();
    }

    /**
     * Finds player data by username (case-insensitive).
     *
     * @param username the username to find
     * @return the player data, or null if not found
     */
    @Nullable
    public HGPlayerData findByUsername(@NotNull String username) {
        for (HGPlayerData data : playerData.values()) {
            if (data.getUsername().equalsIgnoreCase(username)) {
                return data;
            }
        }
        return null;
    }

    /**
     * Decays VL for all players.
     * Called periodically by the VL decay task.
     *
     * @param checkName the check name
     * @param decayAmount the amount to decay
     */
    public void decayAllVL(@NotNull String checkName, double decayAmount) {
        for (HGPlayerData data : playerData.values()) {
            data.decayVL(checkName, decayAmount);
        }
    }
}
