# Hytale Server Plugin API Reference

> **Source:** Decompiled from `HytaleServer.jar` - `com.hypixel.hytale.server.core.plugin`

This document contains the plugin development API for Hytale Server, extracted from the decompiled source code.

---

## Plugin Architecture Overview

### Plugin Types

| Type | Class | Description |
|------|-------|-------------|
| `PLUGIN` | `JavaPlugin` | Standard server plugin (JAR) |
| `ADDON` | `PluginBase` | Asset addon |

### Plugin States

```java
public enum PluginState {
    NONE,       // Initial state
    SETUP,      // setup() called
    START,      // start() called
    ENABLED,    // Fully enabled
    SHUTDOWN,   // shutdown() called
    DISABLED    // Plugin disabled
}
```

---

## Creating a Plugin

### Basic Plugin Structure

```java
package com.example.myplugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class MyPlugin extends JavaPlugin {

    public MyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Called during plugin setup phase
        // Register commands, events, configs here
        getLogger().info("MyPlugin setting up...");
    }

    @Override
    protected void start() {
        // Called when plugin starts
        // Plugin is now enabled
        getLogger().info("MyPlugin started!");
    }

    @Override
    protected void shutdown() {
        // Called when plugin shuts down
        // Clean up resources here
        getLogger().info("MyPlugin shutting down...");
    }
}
```

### Plugin Manifest (plugin.json)

```json
{
  "Group": "com.example",
  "Name": "MyPlugin",
  "Version": "1.0.0",
  "Description": "My awesome Hytale plugin",
  "Authors": [
    {
      "Name": "Your Name",
      "Email": "you@example.com",
      "Url": "https://example.com"
    }
  ],
  "Website": "https://github.com/you/myplugin",
  "Main": "com.example.myplugin.MyPlugin",
  "ServerVersion": ">=1.0.0",
  "Dependencies": {
    "com.example/OtherPlugin": ">=1.0.0"
  },
  "OptionalDependencies": {
    "com.example/OptionalPlugin": ">=1.0.0"
  },
  "LoadBefore": {
    "com.example/LoadAfterMe": "*"
  },
  "DisabledByDefault": false,
  "IncludesAssetPack": true
}
```

### Manifest Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `Group` | String | No | Plugin group/organization |
| `Name` | String | **Yes** | Plugin name |
| `Version` | Semver | No | Plugin version |
| `Description` | String | No | Plugin description |
| `Authors` | Array | No | Author information |
| `Website` | String | No | Plugin website |
| `Main` | String | No | Main class path |
| `ServerVersion` | SemverRange | No | Compatible server versions |
| `Dependencies` | Map | No | Required dependencies |
| `OptionalDependencies` | Map | No | Optional dependencies |
| `LoadBefore` | Map | No | Plugins to load after this |
| `DisabledByDefault` | Boolean | No | Start disabled |
| `IncludesAssetPack` | Boolean | No | Contains asset pack |
| `SubPlugins` | Array | No | Nested sub-plugins |

---

## Plugin Base API

### Available Registries

From `PluginBase`, you have access to:

```java
// Logging
HytaleLogger getLogger();

// Plugin identification
PluginIdentifier getIdentifier();
PluginManifest getManifest();
Path getDataDirectory();
PluginState getState();
String getBasePermission();  // e.g., "com.example.myplugin"

// Registration APIs
CommandRegistry getCommandRegistry();
EventRegistry getEventRegistry();
ClientFeatureRegistry getClientFeatureRegistry();
BlockStateRegistry getBlockStateRegistry();
EntityRegistry getEntityRegistry();
TaskRegistry getTaskRegistry();
AssetRegistry getAssetRegistry();

// Storage
ComponentRegistryProxy<EntityStore> getEntityStoreRegistry();
ComponentRegistryProxy<ChunkStore> getChunkStoreRegistry();
```

---

## Command Registration

### Registering Commands

```java
@Override
protected void setup() {
    // Register a command
    getCommandRegistry().registerCommand(new MyCommand());
}
```

### Creating Commands

```java
public class MyCommand extends AbstractCommand {

    private final RequiredArg<String> playerArg;
    private final OptionalArg<Integer> amountArg;
    private final FlagArg silentFlag;

    public MyCommand() {
        super("mycommand", "server.commands.mycommand.description");

        // Set permission
        requirePermission("myplugin.command.mycommand");

        // Required arguments
        playerArg = withRequiredArg("player", "Target player", ArgumentTypes.STRING);

        // Optional arguments (prefixed with -amount)
        amountArg = withOptionalArg("amount", "Amount", ArgumentTypes.INTEGER);

        // Flag arguments (-silent)
        silentFlag = withFlagArg("silent", "Silent mode");

        // Add subcommands
        addSubCommand(new MySubCommand());
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        String playerName = playerArg.get(context);
        Integer amount = amountArg.get(context);  // May be null
        boolean silent = silentFlag.get(context);

        // Command implementation
        context.sender().sendMessage(Message.raw("Hello, " + playerName));

        return null;  // or CompletableFuture for async
    }
}
```

### Argument Types

```java
// Built-in argument types
ArgumentTypes.STRING          // String
ArgumentTypes.INTEGER         // Integer
ArgumentTypes.FLOAT           // Float
ArgumentTypes.DOUBLE          // Double
ArgumentTypes.BOOLEAN         // Boolean
ArgumentTypes.PLAYER          // Player reference
ArgumentTypes.WORLD           // World reference
ArgumentTypes.POSITION        // Vector3i position
ArgumentTypes.BLOCK_TYPE      // Block type
ArgumentTypes.ITEM_TYPE       // Item type
ArgumentTypes.GAME_MODE       // GameMode enum
```

---

## Event Registration

### Registering Event Listeners

```java
@Override
protected void setup() {
    EventRegistry events = getEventRegistry();

    // Simple registration
    events.register(PlayerConnectEvent.class, this::onPlayerConnect);

    // With priority
    events.register(EventPriority.HIGH, PlayerChatEvent.class, this::onPlayerChat);

    // Global listener (all keys)
    events.registerGlobal(ChunkUnloadEvent.class, this::onChunkUnload);

    // Async event
    events.registerAsync(PlayerChatEvent.class, this::onPlayerChatAsync);
}

private void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef player = event.getPlayerRef();
    getLogger().info(player.getUsername() + " connected!");
}

private void onPlayerChat(PlayerChatEvent event) {
    if (event.getContent().contains("badword")) {
        event.setCancelled(true);
    }
}

private CompletableFuture<PlayerChatEvent> onPlayerChatAsync(CompletableFuture<PlayerChatEvent> future) {
    return future.thenApply(event -> {
        // Async processing
        return event;
    });
}
```

---

## Configuration System

### Using Configs

```java
public class MyPlugin extends JavaPlugin {

    private Config<MyConfig> config;

    public MyPlugin(JavaPluginInit init) {
        super(init);
        // Must be called before setup()
        config = withConfig(MyConfig.CODEC);
    }

    @Override
    protected void setup() {
        MyConfig cfg = config.get();
        getLogger().info("Message: " + cfg.getMessage());
    }
}
```

### Config Class with Codec

```java
public class MyConfig {
    public static final BuilderCodec<MyConfig> CODEC = BuilderCodec
        .builder(MyConfig.class, MyConfig::new)
        .append(new KeyedCodec<>("message", Codec.STRING),
            (c, v) -> c.message = v,
            c -> c.message)
        .add()
        .append(new KeyedCodec<>("maxPlayers", Codec.INTEGER),
            (c, v) -> c.maxPlayers = v,
            c -> c.maxPlayers)
        .add()
        .build();

    private String message = "Hello World";
    private int maxPlayers = 100;

    public String getMessage() { return message; }
    public int getMaxPlayers() { return maxPlayers; }
}
```

Config file saved to: `plugins/<plugin-name>/config.json`

---

## Permissions API

### Checking Permissions

```java
// In commands
context.sender().hasPermission("myplugin.admin");

// With PlayerRef
playerRef.hasPermission("myplugin.feature");

// Permission holder interface
public interface PermissionHolder {
    boolean hasPermission(String permission);
}
```

### Permission Provider

Access the permission system:

```java
PermissionsModule perms = PermissionsModule.get();
PermissionProvider provider = perms.getProvider();

// User permissions
provider.addUserPermissions(uuid, Set.of("perm1", "perm2"));
provider.removeUserPermissions(uuid, Set.of("perm1"));
Set<String> userPerms = provider.getUserPermissions(uuid);

// Group permissions
provider.addGroupPermissions("Admin", Set.of("*"));
provider.removeGroupPermissions("Admin", Set.of("perm1"));
Set<String> groupPerms = provider.getGroupPermissions("Admin");

// User-group membership
provider.addUserToGroup(uuid, "Admin");
provider.removeUserFromGroup(uuid, "Admin");
Set<String> userGroups = provider.getGroupsForUser(uuid);
```

---

## Task Registry

### Scheduling Tasks

```java
TaskRegistry tasks = getTaskRegistry();

// Schedule a task
tasks.scheduleTask("myTask", () -> {
    // Task logic
}, 20);  // Delay in ticks

// Repeating task
tasks.scheduleRepeatingTask("myRepeatingTask", () -> {
    // Runs periodically
}, 0, 100);  // Initial delay, period
```

---

## Logging

### Using the Logger

```java
HytaleLogger logger = getLogger();

// Log levels
logger.info("Information message");
logger.warning("Warning message");
logger.severe("Error message");

// With formatting
logger.at(Level.INFO).log("Player %s joined", playerName);

// With exception
logger.at(Level.SEVERE).withCause(exception).log("Error occurred");
```

---

## Asset Registry

### Registering Assets

```java
AssetRegistry assets = getAssetRegistry();

// Register custom assets
assets.register(myAssetType, myAsset);
```

---

## Entity Registry

### Registering Entity Types

```java
EntityRegistry entities = getEntityRegistry();

// Register custom entity types
entities.register(myEntityType);
```

---

## Block State Registry

### Registering Block States

```java
BlockStateRegistry blocks = getBlockStateRegistry();

// Register custom block states
blocks.register(myBlockState);
```

---

## Plugin Lifecycle

### Lifecycle Phases

1. **Construction** - Plugin JAR loaded, constructor called
2. **PreLoad** - `preLoad()` - Configs loaded async
3. **Setup** - `setup()` - Register commands, events, etc.
4. **Start** - `start()` - Plugin fully initialized
5. **Enabled** - Plugin running normally
6. **Shutdown** - `shutdown()` - Cleanup on disable/server stop

### State Checks

```java
boolean isEnabled();   // SETUP, START, or ENABLED
boolean isDisabled();  // NONE, DISABLED, or SHUTDOWN
PluginState getState();
```

---

## Best Practices

### 1. Register in setup(), not start()

```java
@Override
protected void setup() {
    // Correct: Register commands/events here
    getCommandRegistry().registerCommand(new MyCommand());
    getEventRegistry().register(PlayerConnectEvent.class, this::onConnect);
}
```

### 2. Clean up in shutdown()

```java
@Override
protected void shutdown() {
    // Save data, cancel tasks, close connections
    saveData();
}
```

### 3. Use the data directory

```java
Path configPath = getDataDirectory().resolve("custom-data.json");
```

### 4. Handle async properly

```java
// Return CompletableFuture for async commands
@Override
protected CompletableFuture<Void> execute(CommandContext context) {
    return CompletableFuture.runAsync(() -> {
        // Async work
    }).thenRun(() -> {
        context.sender().sendMessage(Message.raw("Done!"));
    });
}
```

### 5. Use proper permission namespaces

```java
// Good: namespaced permissions
requirePermission("myplugin.command.mycommand");

// Good: use base permission
requirePermission(getBasePermission() + ".admin");
```

---

## Example Plugin

```java
package com.example.welcomeplugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class WelcomePlugin extends JavaPlugin {

    public WelcomePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
        getCommandRegistry().registerCommand(new WelcomeCommand());
        getLogger().info("WelcomePlugin setup complete!");
    }

    @Override
    protected void start() {
        getLogger().info("WelcomePlugin started!");
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        event.getPlayerRef().sendMessage(
            Message.raw("Welcome to the server, " + event.getPlayerRef().getUsername() + "!")
        );
    }

    @Override
    protected void shutdown() {
        getLogger().info("WelcomePlugin shutting down...");
    }
}
```

---

## API Package Structure

| Package | Description |
|---------|-------------|
| `com.hypixel.hytale.server.core.plugin` | Plugin base classes |
| `com.hypixel.hytale.server.core.command` | Command system |
| `com.hypixel.hytale.server.core.event` | Event system |
| `com.hypixel.hytale.server.core.permissions` | Permissions |
| `com.hypixel.hytale.server.core.universe` | World/Player APIs |
| `com.hypixel.hytale.server.core.entity` | Entity APIs |
| `com.hypixel.hytale.server.core.inventory` | Inventory APIs |
| `com.hypixel.hytale.server.core.task` | Task scheduling |
| `com.hypixel.hytale.common.plugin` | Shared plugin types |
| `com.hypixel.hytale.event` | Event bus core |
| `com.hypixel.hytale.codec` | Serialization codecs |
