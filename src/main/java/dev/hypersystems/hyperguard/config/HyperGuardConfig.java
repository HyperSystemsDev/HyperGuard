package dev.hypersystems.hyperguard.config;

import com.google.gson.*;
import dev.hypersystems.hyperguard.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Main configuration for HyperGuard.
 * Handles loading, saving, and providing access to all configuration settings.
 */
public final class HyperGuardConfig {

    private static final String CONFIG_FILE = "config.json";

    private static HyperGuardConfig instance;

    private final Path configFile;
    private final Gson gson;

    private GeneralConfig general;
    private final Map<String, CheckConfig> checks;

    private HyperGuardConfig(@NotNull Path dataFolder) {
        this.configFile = dataFolder.resolve(CONFIG_FILE);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        this.general = new GeneralConfig();
        this.checks = new HashMap<>();

        // Initialize default check configs
        initializeDefaultChecks();
    }

    /**
     * Initialize the configuration singleton.
     *
     * @param dataFolder the plugin data folder
     * @return the configuration instance
     */
    @NotNull
    public static HyperGuardConfig init(@NotNull Path dataFolder) {
        instance = new HyperGuardConfig(dataFolder);
        instance.load();
        return instance;
    }

    /**
     * Get the configuration instance.
     *
     * @throws IllegalStateException if not initialized
     */
    @NotNull
    public static HyperGuardConfig get() {
        if (instance == null) {
            throw new IllegalStateException("HyperGuardConfig not initialized");
        }
        return instance;
    }

    /**
     * Check if the configuration has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Initialize default check configurations.
     */
    private void initializeDefaultChecks() {
        // Movement checks
        checks.put("speed", createDefaultCheckConfig());
        checks.put("fly", createDefaultCheckConfig());
        checks.put("nofall", createDefaultCheckConfig());
        checks.put("phase", createDefaultCheckConfig());
        checks.put("step", createDefaultCheckConfig());

        // Combat checks
        checks.put("reach", createDefaultCheckConfig());
        checks.put("killaura", createDefaultCheckConfig());
        checks.put("autoclicker", createDefaultCheckConfig());
        checks.put("hitbox", createDefaultCheckConfig());

        // World checks
        checks.put("scaffold", createDefaultCheckConfig());
        checks.put("nuker", createDefaultCheckConfig());
        checks.put("fastplace", createDefaultCheckConfig());
        checks.put("fastbreak", createDefaultCheckConfig());
    }

    /**
     * Create a default check configuration.
     *
     * @return new default CheckConfig
     */
    @NotNull
    private CheckConfig createDefaultCheckConfig() {
        return new CheckConfig();
    }

    /**
     * Load configuration from file.
     */
    public void load() {
        if (!Files.exists(configFile)) {
            save();
            Logger.info("Created default configuration file");
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            loadFromJson(json);
            Logger.info("Configuration loaded");
        } catch (Exception e) {
            Logger.warn("Failed to load configuration: %s", e.getMessage());
            Logger.info("Using default configuration values");
        }
    }

    /**
     * Save configuration to file.
     */
    public void save() {
        try {
            Files.createDirectories(configFile.getParent());

            JsonObject json = toJson();
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(json, writer);
            }

            Logger.debug("Configuration saved");
        } catch (IOException e) {
            Logger.severe("Failed to save configuration: %s", e.getMessage());
        }
    }

    /**
     * Reload configuration from file.
     */
    public void reload() {
        load();
    }

    /**
     * Load configuration from JSON.
     *
     * @param json the JSON object
     */
    private void loadFromJson(@NotNull JsonObject json) {
        // Load general config
        if (json.has("general") && json.get("general").isJsonObject()) {
            loadGeneralConfig(json.getAsJsonObject("general"));
        }

        // Load check configs
        if (json.has("checks") && json.get("checks").isJsonObject()) {
            loadCheckConfigs(json.getAsJsonObject("checks"));
        }
    }

    /**
     * Load general configuration settings.
     *
     * @param json the general config JSON
     */
    private void loadGeneralConfig(@NotNull JsonObject json) {
        if (json.has("alertsEnabled")) {
            general.setAlertsEnabled(json.get("alertsEnabled").getAsBoolean());
        }
        if (json.has("loggingEnabled")) {
            general.setLoggingEnabled(json.get("loggingEnabled").getAsBoolean());
        }
        if (json.has("debugMode")) {
            general.setDebugMode(json.get("debugMode").getAsBoolean());
        }
        if (json.has("vlDecayIntervalTicks")) {
            general.setVlDecayIntervalTicks(json.get("vlDecayIntervalTicks").getAsInt());
        }
        if (json.has("exemptGamemodes") && json.get("exemptGamemodes").isJsonArray()) {
            general.getExemptGamemodes().clear();
            for (JsonElement element : json.getAsJsonArray("exemptGamemodes")) {
                general.getExemptGamemodes().add(element.getAsString());
            }
        }
        if (json.has("bypassPermission")) {
            general.setBypassPermission(json.get("bypassPermission").getAsString());
        }
        if (json.has("alertPermission")) {
            general.setAlertPermission(json.get("alertPermission").getAsString());
        }
        if (json.has("joinExemptionTicks")) {
            general.setJoinExemptionTicks(json.get("joinExemptionTicks").getAsInt());
        }
        if (json.has("teleportExemptionTicks")) {
            general.setTeleportExemptionTicks(json.get("teleportExemptionTicks").getAsInt());
        }
    }

    /**
     * Load check configurations.
     *
     * @param json the checks JSON object
     */
    private void loadCheckConfigs(@NotNull JsonObject json) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String checkName = entry.getKey().toLowerCase();
            if (entry.getValue().isJsonObject()) {
                CheckConfig config = loadCheckConfig(entry.getValue().getAsJsonObject());
                checks.put(checkName, config);
            }
        }
    }

    /**
     * Load a single check configuration.
     *
     * @param json the check config JSON
     * @return the CheckConfig
     */
    @NotNull
    private CheckConfig loadCheckConfig(@NotNull JsonObject json) {
        CheckConfig config = new CheckConfig();

        if (json.has("enabled")) {
            config.setEnabled(json.get("enabled").getAsBoolean());
        }
        if (json.has("tolerance")) {
            config.setTolerance(json.get("tolerance").getAsDouble());
        }
        if (json.has("vlMultiplier")) {
            config.setVlMultiplier(json.get("vlMultiplier").getAsDouble());
        }
        if (json.has("vlDecayRate")) {
            config.setVlDecayRate(json.get("vlDecayRate").getAsDouble());
        }
        if (json.has("maxVL")) {
            config.setMaxVL(json.get("maxVL").getAsDouble());
        }
        if (json.has("thresholds") && json.get("thresholds").isJsonArray()) {
            config.clearThresholds();
            for (JsonElement element : json.getAsJsonArray("thresholds")) {
                if (element.isJsonObject()) {
                    JsonObject threshold = element.getAsJsonObject();
                    double thresholdValue = threshold.get("threshold").getAsDouble();
                    String action = threshold.get("action").getAsString();
                    String duration = threshold.has("duration") ?
                        threshold.get("duration").getAsString() : null;
                    config.addThreshold(new ActionThreshold(thresholdValue, action, duration));
                }
            }
        }

        return config;
    }

    /**
     * Convert configuration to JSON.
     *
     * @return the JSON object
     */
    @NotNull
    private JsonObject toJson() {
        JsonObject root = new JsonObject();

        // General config
        root.add("general", generalToJson());

        // Check configs
        JsonObject checksJson = new JsonObject();
        for (Map.Entry<String, CheckConfig> entry : checks.entrySet()) {
            checksJson.add(entry.getKey(), checkConfigToJson(entry.getValue()));
        }
        root.add("checks", checksJson);

        return root;
    }

    /**
     * Convert general config to JSON.
     *
     * @return the JSON object
     */
    @NotNull
    private JsonObject generalToJson() {
        JsonObject json = new JsonObject();
        json.addProperty("alertsEnabled", general.isAlertsEnabled());
        json.addProperty("loggingEnabled", general.isLoggingEnabled());
        json.addProperty("debugMode", general.isDebugMode());
        json.addProperty("vlDecayIntervalTicks", general.getVlDecayIntervalTicks());

        JsonArray gamemodes = new JsonArray();
        for (String gamemode : general.getExemptGamemodes()) {
            gamemodes.add(gamemode);
        }
        json.add("exemptGamemodes", gamemodes);

        json.addProperty("bypassPermission", general.getBypassPermission());
        json.addProperty("alertPermission", general.getAlertPermission());
        json.addProperty("joinExemptionTicks", general.getJoinExemptionTicks());
        json.addProperty("teleportExemptionTicks", general.getTeleportExemptionTicks());

        return json;
    }

    /**
     * Convert a check config to JSON.
     *
     * @param config the check config
     * @return the JSON object
     */
    @NotNull
    private JsonObject checkConfigToJson(@NotNull CheckConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", config.isEnabled());
        json.addProperty("tolerance", config.getTolerance());
        json.addProperty("vlMultiplier", config.getVlMultiplier());
        json.addProperty("vlDecayRate", config.getVlDecayRate());
        json.addProperty("maxVL", config.getMaxVL());

        JsonArray thresholds = new JsonArray();
        for (ActionThreshold threshold : config.getThresholds()) {
            JsonObject thresholdJson = new JsonObject();
            thresholdJson.addProperty("threshold", threshold.getThreshold());
            thresholdJson.addProperty("action", threshold.getAction());
            if (threshold.hasDuration()) {
                thresholdJson.addProperty("duration", threshold.getDuration());
            }
            thresholds.add(thresholdJson);
        }
        json.add("thresholds", thresholds);

        return json;
    }

    // ==================== Getters ====================

    /**
     * Gets the general configuration.
     *
     * @return the general config
     */
    @NotNull
    public GeneralConfig getGeneral() {
        return general;
    }

    /**
     * Gets the configuration for a specific check.
     *
     * @param checkName the check name (case-insensitive)
     * @return the check config, or null if not found
     */
    @Nullable
    public CheckConfig getCheckConfig(@NotNull String checkName) {
        return checks.get(checkName.toLowerCase());
    }

    /**
     * Gets or creates a configuration for a specific check.
     *
     * @param checkName the check name
     * @return the check config
     */
    @NotNull
    public CheckConfig getOrCreateCheckConfig(@NotNull String checkName) {
        return checks.computeIfAbsent(checkName.toLowerCase(), k -> new CheckConfig());
    }

    /**
     * Gets all check configurations.
     *
     * @return unmodifiable map of check configs
     */
    @NotNull
    public Map<String, CheckConfig> getAllCheckConfigs() {
        return Collections.unmodifiableMap(checks);
    }

    /**
     * Checks if a check is enabled.
     *
     * @param checkName the check name
     * @return true if enabled
     */
    public boolean isCheckEnabled(@NotNull String checkName) {
        CheckConfig config = getCheckConfig(checkName);
        return config != null && config.isEnabled();
    }
}
