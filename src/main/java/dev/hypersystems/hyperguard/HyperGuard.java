package dev.hypersystems.hyperguard;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperguard.action.ActionExecutor;
import dev.hypersystems.hyperguard.alert.AlertManager;
import dev.hypersystems.hyperguard.check.CheckManager;
import dev.hypersystems.hyperguard.check.CheckType;
import dev.hypersystems.hyperguard.command.HyperGuardCommand;
import dev.hypersystems.hyperguard.config.HyperGuardConfig;
import dev.hypersystems.hyperguard.listener.PlayerConnectionListener;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.player.PositionHistory;
import dev.hypersystems.hyperguard.util.Logger;
import dev.hypersystems.hyperguard.violation.ViolationManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Main plugin entry point for HyperGuard.
 * A server-side anti-cheat plugin for Hytale servers.
 */
public class HyperGuard extends JavaPlugin {

    private static HyperGuard instance;

    private HyperGuardConfig config;
    private PlayerConnectionListener connectionListener;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> vlDecayTask;
    private ScheduledFuture<?> movementCheckTask;

    /**
     * Creates a new HyperGuard instance.
     * Called by the Hytale plugin loader.
     *
     * @param init the plugin initialization data
     */
    public HyperGuard(JavaPluginInit init) {
        super(init);
    }

    /**
     * Gets the plugin instance.
     *
     * @return the instance
     * @throws IllegalStateException if not initialized
     */
    @NotNull
    public static HyperGuard get() {
        if (instance == null) {
            throw new IllegalStateException("HyperGuard not initialized");
        }
        return instance;
    }

    /**
     * Checks if the plugin is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    @Override
    protected void setup() {
        instance = this;

        // Initialize logger
        Logger.init(java.util.logging.Logger.getLogger("HyperGuard"));

        Logger.info("Setting up HyperGuard...");
    }

    @Override
    protected void start() {
        Logger.info("Starting HyperGuard v%s...", getManifest().getVersion());

        try {
            // Initialize configuration
            Path dataFolder = getDataDirectory();
            config = HyperGuardConfig.init(dataFolder);

            // Set debug mode in logger
            Logger.setDebugMode(config.getGeneral().isDebugMode());

            // Initialize managers
            initializeManagers();

            // Register commands
            registerCommands();

            // Register event listeners
            registerEventListeners();

            // Schedule tasks
            scheduleTasks();

            // Register default checks (Phase 2+ will add actual implementations)
            CheckManager.get().registerDefaultChecks();

            Logger.info("HyperGuard v%s started successfully!", getManifest().getVersion());
            Logger.info("Checks: %d registered | Alerts: %s | Debug: %s",
                CheckManager.get().getCheckCount(),
                config.getGeneral().isAlertsEnabled() ? "enabled" : "disabled",
                config.getGeneral().isDebugMode() ? "enabled" : "disabled");

        } catch (Exception e) {
            Logger.severe("Failed to start HyperGuard: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void shutdown() {
        Logger.info("Shutting down HyperGuard...");

        try {
            // Cancel scheduled tasks
            if (vlDecayTask != null) {
                vlDecayTask.cancel(false);
            }

            if (movementCheckTask != null) {
                movementCheckTask.cancel(false);
            }

            // Shutdown scheduler
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // Save configuration
            if (config != null) {
                config.save();
            }

            // Shutdown managers
            if (ViolationManager.get() != null) {
                ViolationManager.get().shutdown();
            }
            if (CheckManager.get() != null) {
                CheckManager.get().shutdown();
            }
            if (HGPlayerManager.get() != null) {
                HGPlayerManager.get().shutdown();
            }

            Logger.info("HyperGuard shut down successfully");

        } catch (Exception e) {
            Logger.severe("Error during shutdown: %s", e.getMessage());
            e.printStackTrace();
        }

        instance = null;
    }

    /**
     * Initializes all managers.
     */
    private void initializeManagers() {
        HGPlayerManager.init();
        ViolationManager.init();
        ActionExecutor.init();
        AlertManager.init();
        CheckManager.init();
        PlayerConnectionListener.init();

        Logger.debug("Managers initialized");
    }

    /**
     * Registers all commands.
     */
    private void registerCommands() {
        HyperGuardCommand mainCommand = new HyperGuardCommand();
        getCommandRegistry().registerCommand(mainCommand);

        Logger.debug("Commands registered");
    }

    /**
     * Registers event listeners.
     */
    private void registerEventListeners() {
        connectionListener = PlayerConnectionListener.get();

        EventRegistry eventRegistry = getEventRegistry();
        if (eventRegistry != null) {
            connectionListener.register(eventRegistry);
            Logger.debug("Event listeners registered");
        } else {
            Logger.warn("EventRegistry not available, some features may not work");
        }
    }

    /**
     * Schedules recurring tasks.
     */
    private void scheduleTasks() {
        // Create scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HyperGuard-Scheduler");
            t.setDaemon(true);
            return t;
        });

        // VL decay task - runs every second (20 ticks)
        int decayIntervalTicks = config.getGeneral().getVlDecayIntervalTicks();
        long decayIntervalMs = decayIntervalTicks * 50L; // Convert ticks to milliseconds

        vlDecayTask = scheduler.scheduleAtFixedRate(
            this::runVLDecayTask,
            decayIntervalMs,
            decayIntervalMs,
            TimeUnit.MILLISECONDS
        );

        Logger.info("Scheduled VL decay task (every %d ticks / %dms)", decayIntervalTicks, decayIntervalMs);

        // Movement check task - runs every tick (50ms)
        movementCheckTask = scheduler.scheduleAtFixedRate(
            this::runMovementChecks,
            50,
            50,
            TimeUnit.MILLISECONDS
        );

        Logger.info("Scheduled movement check task (every tick / 50ms)");
    }

    /**
     * VL decay task - decays all player VLs.
     */
    private void runVLDecayTask() {
        try {
            ViolationManager.get().decayAllPlayers();
        } catch (Exception e) {
            Logger.debug("VL decay task error: %s", e.getMessage());
        }
    }

    /**
     * Movement check task - runs movement checks for all online players.
     */
    private void runMovementChecks() {
        try {
            Collection<HGPlayerData> allPlayers = HGPlayerManager.get().getAllPlayerData();
            for (HGPlayerData playerData : allPlayers) {
                PlayerRef playerRef = playerData.getPlayerRef();
                if (playerRef == null) {
                    continue;
                }
                if (!playerRef.isValid()) {
                    continue;
                }

                // Record position from PlayerRef's cached transform
                try {
                    com.hypixel.hytale.math.vector.Transform transform = playerRef.getTransform();
                    if (transform != null) {
                        // Access position via getPosition() method which returns Vector3d
                        com.hypixel.hytale.math.vector.Vector3d pos = transform.getPosition();
                        com.hypixel.hytale.math.vector.Vector3f rot = transform.getRotation();

                        if (pos != null) {
                            double x = pos.getX();
                            double y = pos.getY();
                            double z = pos.getZ();
                            float yaw = rot != null ? rot.getYaw() : 0;
                            float pitch = rot != null ? rot.getPitch() : 0;

                            // Get onGround from position history heuristic
                            PositionHistory history = playerData.getPositionHistory();
                            PositionHistory.PositionSample lastSample = history.getLatest();

                            boolean onGround = false;
                            if (lastSample != null) {
                                double yDelta = y - lastSample.y();
                                onGround = Math.abs(yDelta) < 0.01 || (yDelta > -0.1 && yDelta <= 0);
                            }

                            playerData.recordPosition(x, y, z, yaw, pitch, onGround);
                        }
                    }
                } catch (Exception e) {
                    // Ignore position recording errors
                }

                // Run movement checks
                CheckManager.get().processChecksByType(CheckType.MOVEMENT, playerRef, playerData);
            }
        } catch (Exception e) {
            Logger.warn("Movement check task error: %s", e.getMessage());
        }
    }

    /**
     * Reloads the plugin configuration.
     */
    public void reload() {
        Logger.info("Reloading HyperGuard...");

        try {
            // Reload configuration
            config.reload();

            // Update debug mode
            Logger.setDebugMode(config.getGeneral().isDebugMode());

            Logger.info("HyperGuard reloaded successfully");

        } catch (Exception e) {
            Logger.severe("Failed to reload HyperGuard: %s", e.getMessage());
        }
    }

    /**
     * Gets the plugin configuration.
     *
     * @return the configuration
     */
    @NotNull
    public HyperGuardConfig getHyperGuardConfig() {
        return config;
    }

    /**
     * Gets the ActionExecutor for tracking players.
     *
     * @return the action executor
     */
    @NotNull
    public ActionExecutor getActionExecutor() {
        return ActionExecutor.get();
    }
}
