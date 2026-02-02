# Hytale Server Events Reference

> **Source:** Decompiled from `HytaleServer.jar` - `com.hypixel.hytale.event`

This document contains all events discovered in the Hytale Server, extracted from the decompiled source code.

---

## Event System Overview

### Event Interfaces

| Interface | Description |
|-----------|-------------|
| `IBaseEvent<KeyType>` | Base event interface |
| `IEvent<KeyType>` | Synchronous event |
| `IAsyncEvent<KeyType>` | Asynchronous event |
| `ICancellable` | Event that can be cancelled |
| `IProcessedEvent` | Event that tracks processing state |

### Event Bus

The `EventBus` class manages all event registrations and dispatching:

```java
// Get the event bus from HytaleServer
EventBus eventBus = HytaleServer.get().getEventBus();

// Register a sync event listener
eventBus.register(PlayerConnectEvent.class, event -> {
    // Handle event
});

// Register with priority
eventBus.register(EventPriority.HIGH, PlayerChatEvent.class, event -> {
    // Handle with high priority
});

// Register async event
eventBus.registerAsync(PlayerChatEvent.class, future -> {
    return future.thenApply(event -> {
        // Handle async
        return event;
    });
});
```

### Event Priorities

```java
public enum EventPriority {
    LOWEST,   // First to be called
    LOW,
    NORMAL,   // Default
    HIGH,
    HIGHEST,  // Last to be called (before monitors)
    MONITOR   // For monitoring only, should not modify
}
```

---

## Player Events

Located in `com.hypixel.hytale.server.core.event.events.player`

### PlayerConnectEvent

Fired when a player connects to the server.

```java
public class PlayerConnectEvent implements IEvent<Void> {
    Holder<EntityStore> getHolder();   // Entity holder
    PlayerRef getPlayerRef();          // Player reference
    World getWorld();                  // Target world
    void setWorld(World world);        // Set target world
}
```

---

### PlayerDisconnectEvent

Fired when a player disconnects from the server.

```java
public class PlayerDisconnectEvent implements IEvent<Void> {
    PlayerRef getPlayerRef();
}
```

---

### PlayerChatEvent

Fired when a player sends a chat message. **Cancellable** and **Async**.

```java
public class PlayerChatEvent implements IAsyncEvent<String>, ICancellable {
    PlayerRef getSender();              // Message sender
    void setSender(PlayerRef sender);
    List<PlayerRef> getTargets();       // Message recipients
    void setTargets(List<PlayerRef>);
    String getContent();                // Message content
    void setContent(String content);
    Formatter getFormatter();           // Message formatter
    void setFormatter(Formatter);
    boolean isCancelled();
    void setCancelled(boolean);
}
```

---

### PlayerReadyEvent

Fired when a player is fully ready (loaded into world).

---

### PlayerSetupConnectEvent / PlayerSetupDisconnectEvent

Fired during connection setup/teardown phases.

---

### PlayerInteractEvent

Fired when a player interacts with something.

---

### PlayerCraftEvent

Fired when a player crafts an item.

---

### PlayerMouseButtonEvent / PlayerMouseMotionEvent

Fired for mouse input events.

---

### AddPlayerToWorldEvent / DrainPlayerFromWorldEvent

Fired when player is added to or removed from a world.

---

### PlayerRefEvent

Base event with PlayerRef data.

---

## ECS (Entity Component System) Events

Located in `com.hypixel.hytale.server.core.event.events.ecs`

### BreakBlockEvent

Fired when a block is broken. **Cancellable**.

```java
public class BreakBlockEvent extends CancellableEcsEvent {
    ItemStack getItemInHand();        // Item used to break
    Vector3i getTargetBlock();        // Block position
    void setTargetBlock(Vector3i);
    BlockType getBlockType();         // Type of block broken
}
```

---

### PlaceBlockEvent

Fired when a block is placed. **Cancellable**.

```java
public class PlaceBlockEvent extends CancellableEcsEvent {
    Vector3i getPosition();
    BlockType getBlockType();
}
```

---

### DamageBlockEvent

Fired when a block takes damage.

---

### UseBlockEvent

Fired when a player uses/interacts with a block.

---

### DropItemEvent

Fired when an item is dropped.

---

### InteractivelyPickupItemEvent

Fired when a player picks up an item interactively.

---

### SwitchActiveSlotEvent

Fired when player switches active inventory slot.

---

### ChangeGameModeEvent

Fired when gamemode changes.

---

### CraftRecipeEvent

Fired when a recipe is crafted.

---

### DiscoverZoneEvent

Fired when a zone is discovered.

---

## Entity Events

Located in `com.hypixel.hytale.server.core.event.events.entity`

### EntityEvent

Base entity event class.

---

### EntityRemoveEvent

Fired when an entity is removed from the world.

---

### LivingEntityInventoryChangeEvent

Fired when a living entity's inventory changes.

---

### LivingEntityUseBlockEvent

Fired when a living entity uses a block.

---

## Permission Events

Located in `com.hypixel.hytale.server.core.event.events.permissions`

### PlayerPermissionChangeEvent

Fired when a player's permissions change.

```java
public class PlayerPermissionChangeEvent implements IEvent<Void> {
    UUID getPlayerUUID();
    Set<String> getPermissions();
    boolean isAdded();  // true = added, false = removed
}
```

---

### GroupPermissionChangeEvent

Fired when a group's permissions change.

```java
public class GroupPermissionChangeEvent implements IEvent<Void> {
    String getGroupName();
    Set<String> getPermissions();
    boolean isAdded();
}
```

---

### PlayerGroupEvent

Fired when a player's group membership changes.

---

## World Events

Located in `com.hypixel.hytale.server.core.universe.world.events`

### WorldEvent

Base world event class.

---

### AddWorldEvent

Fired when a world is added.

---

### RemoveWorldEvent

Fired when a world is removed.

---

### StartWorldEvent

Fired when a world starts.

---

### AllWorldsLoadedEvent

Fired when all worlds have been loaded.

---

### ChunkEvent

Base chunk event class.

---

### ChunkPreLoadProcessEvent

Fired before chunk loading processing.

---

### ChunkSaveEvent

Fired when a chunk is saved.

---

### ChunkUnloadEvent

Fired when a chunk is unloaded.

---

### MoonPhaseChangeEvent

Fired when the moon phase changes.

---

## Server Events

### BootEvent

Fired when the server boots up.

```java
public class BootEvent implements IEvent<Void> {
    // Server boot complete
}
```

---

### ShutdownEvent

Fired when the server shuts down.

```java
public class ShutdownEvent implements IEvent<Void> {
    // Server shutdown initiated
}
```

---

### PrepareUniverseEvent

Fired when the universe is being prepared.

---

## Plugin Events

Located in `com.hypixel.hytale.server.core.plugin.event`

### PluginEvent

Base plugin event.

---

### PluginSetupEvent

Fired during plugin setup phase.

---

## Asset Events

Located in `com.hypixel.hytale.assetstore.event`

| Event | Description |
|-------|-------------|
| `RegisterAssetStoreEvent` | Asset store registered |
| `RemoveAssetStoreEvent` | Asset store removed |
| `LoadedAssetsEvent` | Assets loaded |
| `RemovedAssetsEvent` | Assets removed |
| `GenerateAssetsEvent` | Assets generation |
| `AssetMonitorEvent` | Asset monitoring |
| `AssetStoreMonitorEvent` | Asset store monitoring |
| `AssetsEvent` | Base assets event |
| `AssetStoreEvent` | Base asset store event |

---

## Module Events

### KillFeedEvent

Located in `com.hypixel.hytale.server.core.modules.entity.damage.event`

Fired for kill feed entries.

---

### GenerateDefaultLanguageEvent

Located in `com.hypixel.hytale.server.core.modules.i18n.event`

Fired when generating default language entries.

---

## Prefab Events

Located in `com.hypixel.hytale.server.core.prefab.event`

### PrefabPasteEvent

Fired when a prefab is pasted.

---

### PrefabPlaceEntityEvent

Fired when an entity is placed from a prefab.

---

## Builtin Feature Events

### Asset Editor Events

Located in `com.hypixel.hytale.builtin.asseteditor.event`

| Event | Description |
|-------|-------------|
| `AssetEditorAssetCreatedEvent` | Asset created in editor |
| `AssetEditorSelectAssetEvent` | Asset selected in editor |
| `AssetEditorActivateButtonEvent` | Button activated in editor |
| `AssetEditorRequestDataSetEvent` | Data set requested |
| `AssetEditorFetchAutoCompleteDataEvent` | Autocomplete data fetch |
| `AssetEditorClientDisconnectEvent` | Editor client disconnected |
| `AssetEditorUpdateWeatherPreviewLockEvent` | Weather preview lock update |
| `EditorClientEvent` | Base editor client event |

---

### Adventure Events

Located in `com.hypixel.hytale.builtin.adventure.objectives.events`

| Event | Description |
|-------|-------------|
| `TreasureChestOpeningEvent` | Treasure chest being opened |

---

### Instance Events

Located in `com.hypixel.hytale.builtin.instances.event`

| Event | Description |
|-------|-------------|
| `DiscoverInstanceEvent` | Instance discovered |

---

### Portal Events

Located in `com.hypixel.hytale.builtin.portals.components.voidevent`

| Event | Description |
|-------|-------------|
| `VoidEvent` | Void event triggered |

---

## Using Events in Plugins

### Basic Event Registration

```java
public class MyPlugin extends JavaPlugin {
    @Override
    protected void setup() {
        // Register event listener
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        getLogger().info("Player connected: " + event.getPlayerRef().getUsername());
    }
}
```

### Cancellable Events

```java
getEventRegistry().register(PlayerChatEvent.class, event -> {
    String message = event.getContent();

    // Check for banned words
    if (containsBannedWord(message)) {
        event.setCancelled(true);
        event.getSender().sendMessage("Message blocked!");
    }
});
```

### Priority-Based Registration

```java
// Register with high priority (called later)
getEventRegistry().register(EventPriority.HIGH, BreakBlockEvent.class, event -> {
    // This handler runs after NORMAL and LOW priority handlers
});

// Monitor priority - for logging/metrics only
getEventRegistry().register(EventPriority.MONITOR, BreakBlockEvent.class, event -> {
    // Don't modify the event here, just observe
    logBlockBreak(event);
});
```

### Async Event Handling

```java
getEventRegistry().registerAsync(PlayerChatEvent.class, future -> {
    return future.thenApply(event -> {
        // Perform async operations (database lookup, etc.)
        String enhancedMessage = lookupPlayerTitle(event.getSender()) + ": " + event.getContent();
        event.setContent(enhancedMessage);
        return event;
    });
});
```

### Global Event Listeners

```java
// Listen to all instances of an event regardless of key
getEventRegistry().registerGlobal(ChunkUnloadEvent.class, event -> {
    // Called for all chunk unloads
});
```

---

## Event Count Summary

| Category | Count |
|----------|-------|
| Player Events | 13 |
| ECS Events | 10 |
| Entity Events | 4 |
| Permission Events | 3 |
| World Events | 9 |
| Server Events | 3 |
| Plugin Events | 2 |
| Asset Events | 9 |
| Module Events | 2 |
| Prefab Events | 2 |
| Builtin Events | 10+ |
| **Total** | **~77** |
