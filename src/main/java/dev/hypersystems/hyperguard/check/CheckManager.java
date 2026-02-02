package dev.hypersystems.hyperguard.check;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.check.movement.FlyCheck;
import dev.hypersystems.hyperguard.check.movement.NoFallCheck;
import dev.hypersystems.hyperguard.check.movement.PhaseCheck;
import dev.hypersystems.hyperguard.check.movement.SpeedCheck;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages registration and execution of anti-cheat checks.
 */
public final class CheckManager {

    private static CheckManager instance;

    private final Map<String, Check> checks;
    private final Map<CheckType, List<Check>> checksByType;

    private CheckManager() {
        this.checks = new ConcurrentHashMap<>();
        this.checksByType = new EnumMap<>(CheckType.class);

        // Initialize lists for each type
        for (CheckType type : CheckType.values()) {
            checksByType.put(type, new ArrayList<>());
        }
    }

    /**
     * Gets the singleton instance.
     *
     * @return the check manager
     */
    @NotNull
    public static CheckManager get() {
        if (instance == null) {
            instance = new CheckManager();
        }
        return instance;
    }

    /**
     * Initializes the check manager.
     */
    public static void init() {
        instance = new CheckManager();
        Logger.debug("CheckManager initialized");
    }

    /**
     * Shuts down the check manager.
     */
    public void shutdown() {
        checks.clear();
        for (List<Check> list : checksByType.values()) {
            list.clear();
        }
        Logger.debug("CheckManager shut down");
    }

    /**
     * Registers a check.
     *
     * @param check the check to register
     */
    public void registerCheck(@NotNull Check check) {
        String name = check.getName().toLowerCase();

        if (checks.containsKey(name)) {
            Logger.warn("Check '%s' is already registered, replacing", name);
            // Remove from type list
            Check existing = checks.get(name);
            checksByType.get(existing.getType()).remove(existing);
        }

        checks.put(name, check);
        checksByType.get(check.getType()).add(check);

        Logger.debug("Registered check: %s (%s)", check.getName(), check.getType().getDisplayName());
    }

    /**
     * Unregisters a check.
     *
     * @param name the check name
     */
    public void unregisterCheck(@NotNull String name) {
        Check check = checks.remove(name.toLowerCase());
        if (check != null) {
            checksByType.get(check.getType()).remove(check);
            Logger.debug("Unregistered check: %s", name);
        }
    }

    /**
     * Gets a check by name.
     *
     * @param name the check name
     * @return the check, or null if not found
     */
    @Nullable
    public Check getCheck(@NotNull String name) {
        return checks.get(name.toLowerCase());
    }

    /**
     * Gets all registered checks.
     *
     * @return unmodifiable collection of checks
     */
    @NotNull
    public Collection<Check> getAllChecks() {
        return Collections.unmodifiableCollection(checks.values());
    }

    /**
     * Gets all checks of a specific type.
     *
     * @param type the check type
     * @return unmodifiable list of checks
     */
    @NotNull
    public List<Check> getChecksByType(@NotNull CheckType type) {
        return Collections.unmodifiableList(checksByType.get(type));
    }

    /**
     * Gets all check names.
     *
     * @return unmodifiable set of check names
     */
    @NotNull
    public Set<String> getCheckNames() {
        return Collections.unmodifiableSet(checks.keySet());
    }

    /**
     * Checks if a check is registered.
     *
     * @param name the check name
     * @return true if registered
     */
    public boolean hasCheck(@NotNull String name) {
        return checks.containsKey(name.toLowerCase());
    }

    /**
     * Gets the number of registered checks.
     *
     * @return check count
     */
    public int getCheckCount() {
        return checks.size();
    }

    /**
     * Enables or disables a check.
     *
     * @param name the check name
     * @param enabled true to enable
     * @return true if check was found
     */
    public boolean setCheckEnabled(@NotNull String name, boolean enabled) {
        Check check = getCheck(name);
        if (check != null) {
            check.setEnabled(enabled);
            Logger.info("Check '%s' %s", name, enabled ? "enabled" : "disabled");
            return true;
        }
        return false;
    }

    /**
     * Processes all enabled checks for a player.
     *
     * @param player the player
     * @param playerData the player data
     */
    public void processAllChecks(@NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        for (Check check : checks.values()) {
            if (check.isEnabled()) {
                try {
                    check.process(player, playerData);
                } catch (Exception e) {
                    Logger.warn("Error processing check '%s' for %s: %s",
                        check.getName(), playerData.getUsername(), e.getMessage());
                }
            }
        }
    }

    /**
     * Processes checks of a specific type for a player.
     *
     * @param type the check type
     * @param player the player
     * @param playerData the player data
     */
    public void processChecksByType(@NotNull CheckType type, @NotNull PlayerRef player, @NotNull HGPlayerData playerData) {
        for (Check check : checksByType.get(type)) {
            if (check.isEnabled()) {
                try {
                    check.process(player, playerData);
                } catch (Exception e) {
                    Logger.warn("Error processing check '%s' for %s: %s",
                        check.getName(), playerData.getUsername(), e.getMessage());
                }
            }
        }
    }

    /**
     * Registers all default checks.
     * Called during plugin initialization.
     */
    public void registerDefaultChecks() {
        // Movement checks (Phase 2)
        registerCheck(new SpeedCheck());
        registerCheck(new FlyCheck());
        registerCheck(new NoFallCheck());
        registerCheck(new PhaseCheck());

        Logger.info("Check framework ready - %d checks registered", checks.size());
    }
}
