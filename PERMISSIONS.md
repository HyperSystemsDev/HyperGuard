# Hytale Server Permissions Reference

> **Source:** Decompiled from `HytaleServer.jar` - `com.hypixel.hytale.server.core.permissions`

This document contains all permission nodes discovered in the Hytale Server, extracted from the decompiled source code.

---

## Permission System Overview

### Permission Generation

From `HytalePermissions.java`:

```java
public class HytalePermissions {
    public static final String NAMESPACE = "hytale";
    public static final String COMMAND_BASE = "hytale.command";

    public static String fromCommand(String name) {
        return "hytale.command." + name;  // e.g., "hytale.command.gamemode.self"
    }

    public static String fromCommand(String name, String subCommand) {
        return "hytale.command." + name + "." + subCommand;
    }
}
```

### Default Groups

From `HytalePermissionsProvider.java`:

| Group | Default Permissions |
|-------|---------------------|
| `OP` | `*` (all permissions) |
| `Default` | None |

### Permission Storage

Permissions are stored in `permissions.json` with this structure:

```json
{
  "users": {
    "uuid-here": {
      "permissions": ["permission.node", "..."],
      "groups": ["GroupName", "..."]
    }
  },
  "groups": {
    "GroupName": ["permission.node", "..."]
  }
}
```

---

## Static Permission Constants

These are defined directly in `HytalePermissions.java`:

| Permission Node | Constant Name | Description |
|-----------------|---------------|-------------|
| `hytale.editor.asset` | `ASSET_EDITOR` | Asset editor access |
| `hytale.editor.packs.create` | `ASSET_EDITOR_PACKS_CREATE` | Create asset packs |
| `hytale.editor.packs.edit` | `ASSET_EDITOR_PACKS_EDIT` | Edit asset packs |
| `hytale.editor.packs.delete` | `ASSET_EDITOR_PACKS_DELETE` | Delete asset packs |
| `hytale.editor.builderTools` | `BUILDER_TOOLS_EDITOR` | Builder tools (**camelCase!**) |
| `hytale.editor.brush.use` | `EDITOR_BRUSH_USE` | Use brushes |
| `hytale.editor.brush.config` | `EDITOR_BRUSH_CONFIG` | Configure brushes |
| `hytale.editor.prefab.use` | `EDITOR_PREFAB_USE` | Use prefabs |
| `hytale.editor.prefab.manage` | `EDITOR_PREFAB_MANAGE` | Manage prefabs |
| `hytale.editor.selection.use` | `EDITOR_SELECTION_USE` | Use selection tools |
| `hytale.editor.selection.clipboard` | `EDITOR_SELECTION_CLIPBOARD` | Copy/paste operations |
| `hytale.editor.selection.modify` | `EDITOR_SELECTION_MODIFY` | Modify selections |
| `hytale.editor.history` | `EDITOR_HISTORY` | Undo/redo |
| `hytale.camera.flycam` | `FLY_CAM` | Fly camera mode |
| `hytale.world_map.teleport.coordinate` | `WORLD_MAP_COORDINATE_TELEPORT` | Teleport via coordinates |
| `hytale.world_map.teleport.marker` | `WORLD_MAP_MARKER_TELEPORT` | Teleport via markers |
| `hytale.system.update.notify` | `UPDATE_NOTIFY` | Update notifications |

---

## Command Permissions (Self/Other Pattern)

Many player-targeted commands follow a `.self`/`.other` suffix pattern:
- `.self` - Permission to use the command on yourself
- `.other` - Permission to use the command on other players

### Player Commands

| Permission | Source File | Description |
|------------|-------------|-------------|
| `hytale.command.gamemode.self` | `GameModeCommand.java:28` | Change own gamemode |
| `hytale.command.gamemode.other` | `GameModeCommand.java:59` | Change other's gamemode |
| `hytale.command.give.self` | `GiveCommand.java:36` | Give items to self |
| `hytale.command.give.other` | `GiveCommand.java:95` | Give items to others |
| `hytale.command.kill.self` | `KillCommand.java:24` | Kill self |
| `hytale.command.kill.other` | `KillCommand.java:44` | Kill other players |
| `hytale.command.damage.self` | `DamageCommand.java:32` | Damage self |
| `hytale.command.damage.other` | `DamageCommand.java:65` | Damage others |
| `hytale.command.spawn.self` | `SpawnCommand.java:34` | Teleport self to spawn |
| `hytale.command.spawn.other` | `SpawnCommand.java:95` | Teleport others to spawn |
| `hytale.command.whereami.self` | `WhereAmICommand.java:36` | Show own location |
| `hytale.command.whereami.other` | `WhereAmICommand.java:101` | Show other's location |
| `hytale.command.refer.self` | `ReferCommand.java:44` | Refer self |
| `hytale.command.refer.other` | `ReferCommand.java:42` | Refer others |
| `hytale.command.player.effect.apply.self` | `PlayerEffectApplyCommand.java:35` | Apply effects to self |
| `hytale.command.player.effect.apply.other` | `PlayerEffectApplyCommand.java:69` | Apply effects to others |
| `hytale.command.player.effect.clear.self` | `PlayerEffectClearCommand.java:24` | Clear own effects |
| `hytale.command.player.effect.clear.other` | `PlayerEffectClearCommand.java:47` | Clear others' effects |

### Teleport Commands

| Permission | Source File | Description |
|------------|-------------|-------------|
| `hytale.command.teleport.self` | `TeleportToPlayerCommand.java:33` | Teleport self to player |
| `hytale.command.teleport.other` | `TeleportOtherToPlayerCommand.java:37` | Teleport other to player |
| `hytale.command.teleport.all` | `TeleportAllCommand.java:48` | Teleport all players |
| `hytale.command.teleport.back` | `TeleportBackCommand.java:22` | Teleport back |
| `hytale.command.teleport.forward` | `TeleportForwardCommand.java:22` | Teleport forward |
| `hytale.command.teleport.top` | `TeleportTopCommand.java:32` | Teleport to top |
| `hytale.command.teleport.home` | `TeleportHomeCommand.java:27` | Teleport home |
| `hytale.command.teleport.world` | `TeleportWorldCommand.java:31` | Teleport to world |
| `hytale.command.teleport.history` | `TeleportHistoryCommand.java:23` | View teleport history |

### Warp Commands

| Permission | Source File | Description |
|------------|-------------|-------------|
| `hytale.command.warp.go` | `WarpGoCommand.java:21` | Use warps |
| `hytale.command.warp.set` | `WarpSetCommand.java:36` | Set warps |
| `hytale.command.warp.remove` | `WarpRemoveCommand.java:27` | Remove warps |
| `hytale.command.warp.list` | `WarpListCommand.java:36` | List warps |
| `hytale.command.warp.reload` | `WarpReloadCommand.java:21` | Reload warps |

### Op/Permissions Commands

| Permission | Source File | Description |
|------------|-------------|-------------|
| `hytale.command.op.add` | `OpAddCommand.java:24` | Add operators |
| `hytale.command.op.remove` | `OpRemoveCommand.java:24` | Remove operators |

### Inventory Commands

| Permission | Source File | Description |
|------------|-------------|-------------|
| `hytale.command.invsee` | `InventorySeeCommand.java` | View other inventories |
| `hytale.command.invsee.modify` | `InventorySeeCommand.java:52` | Modify other inventories |
| `hytale.command.spawnitem` | `SpawnItemCommand.java:51` | Spawn items |

---

## Editor/Builder Tool Permissions

These are checked directly (not via `fromCommand()`):

| Permission | Source File | Description |
|------------|-------------|-------------|
| `hytale.editor.brush.config` | `BrushConfigCommand.java:19` | Configure brushes |
| `hytale.editor.selection.modify` | `SetCommand.java:27` | Modify selections |
| `hytale.editor.history` | `RedoCommand.java:21`, `UndoCommand.java:21` | Undo/redo |
| `hytale.editor.prefab.manage` | `PrefabCommand.java:64,297` | Manage prefabs |
| `hytale.editor.prefab.use` | `PrefabCommand.java:275` | Use prefabs |
| `hytale.editor.selection.clipboard` | `CutCommand.java:41`, `PasteCommand.java:31`, `CopyCommand.java:48`, `ClearEntitiesCommand.java:28`, `ObjImportCommand.java:19` | Clipboard operations |
| `hytale.editor.selection.use` | `Pos1Command.java:33`, `Pos2Command.java:33` | Selection tools |

---

## Permission Checking API

### PermissionHolder Interface

Any entity with permissions implements `PermissionHolder`:

```java
public interface PermissionHolder {
    boolean hasPermission(String permission);
}
```

### Permission Provider

The `HytalePermissionsProvider` manages:
- User permissions by UUID
- Group permissions by group name
- User-to-group assignments

### Events

Permission changes trigger events:
- `PlayerPermissionChangeEvent` - When player permissions change
- `GroupPermissionChangeEvent` - When group permissions change
- `PlayerGroupEvent` - When player group membership changes

---

## Wildcard Permissions

The `*` wildcard grants all permissions. This is the default for the `OP` group.

### Pattern Matching

When checking permissions, the server supports:
- Exact match: `hytale.command.gamemode.self`
- Wildcard: `*` (all permissions)

---

## Implementation Notes

1. **Case Sensitivity**: Permission nodes are case-sensitive. Note `builderTools` uses camelCase!
2. **Namespace**: All Hytale permissions start with `hytale.`
3. **Command Pattern**: Command permissions follow `hytale.command.<command>.<variant>`
4. **Self/Other Pattern**: Player-targeted commands use `.self`/`.other` suffixes

---

## Quick Reference

### All Discovered Permission Nodes

```
hytale.camera.flycam
hytale.command.damage.other
hytale.command.damage.self
hytale.command.gamemode.other
hytale.command.gamemode.self
hytale.command.give.other
hytale.command.give.self
hytale.command.invsee
hytale.command.invsee.modify
hytale.command.kill.other
hytale.command.kill.self
hytale.command.op.add
hytale.command.op.remove
hytale.command.player.effect.apply.other
hytale.command.player.effect.apply.self
hytale.command.player.effect.clear.other
hytale.command.player.effect.clear.self
hytale.command.refer.other
hytale.command.refer.self
hytale.command.spawn.other
hytale.command.spawn.self
hytale.command.spawnitem
hytale.command.teleport.all
hytale.command.teleport.back
hytale.command.teleport.forward
hytale.command.teleport.history
hytale.command.teleport.home
hytale.command.teleport.other
hytale.command.teleport.self
hytale.command.teleport.top
hytale.command.teleport.world
hytale.command.warp.go
hytale.command.warp.list
hytale.command.warp.reload
hytale.command.warp.remove
hytale.command.warp.set
hytale.command.whereami.other
hytale.command.whereami.self
hytale.editor.asset
hytale.editor.brush.config
hytale.editor.brush.use
hytale.editor.builderTools
hytale.editor.history
hytale.editor.packs.create
hytale.editor.packs.delete
hytale.editor.packs.edit
hytale.editor.prefab.manage
hytale.editor.prefab.use
hytale.editor.selection.clipboard
hytale.editor.selection.modify
hytale.editor.selection.use
hytale.system.update.notify
hytale.world_map.teleport.coordinate
hytale.world_map.teleport.marker
```
