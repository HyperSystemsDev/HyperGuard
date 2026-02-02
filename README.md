# HyperGuard

**Comprehensive Server-Side Anti-Cheat for Hytale**

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA)](https://discord.gg/SNPjyfkYPc)
[![GitHub](https://img.shields.io/badge/GitHub-HyperSystemsDev-181717)](https://github.com/HyperSystemsDev)

---

## Overview

HyperGuard is a high-accuracy, low false-positive anti-cheat plugin designed specifically for Hytale servers. It provides comprehensive cheat detection while maintaining excellent performance for high-player-count servers.

### Key Features

- **Movement Detection** - Speed, fly, phase, no-fall, velocity manipulation
- **Combat Detection** - Reach, killaura, auto-clicker, aim assist, criticals
- **World Interaction** - Fast-break, x-ray patterns, scaffold, nuker
- **Configurable Actions** - Warn, kick, tempban, permanent ban with VL thresholds
- **Staff Tools** - Real-time alerts, player inspection, debug mode
- **Permission Integration** - Bypass nodes for staff, per-check exemptions
- **Performance Optimized** - Designed for 100+ concurrent players

---

## Documentation

| Document | Description |
|----------|-------------|
| [ANTICHEAT_FEASIBILITY.md](ANTICHEAT_FEASIBILITY.md) | Complete server analysis and feasibility study |
| [PLUGIN_API.md](PLUGIN_API.md) | Hytale plugin development API reference |
| [EVENTS.md](EVENTS.md) | Available events and hook points |
| [PERMISSIONS.md](PERMISSIONS.md) | Permission system documentation |
| [CLAUDE.md](CLAUDE.md) | AI-assisted development instructions |

---

## Reference Files

The `HyperGuard-reference/` directory contains decompiled server code for understanding the underlying systems:

```
HyperGuard-reference/
├── movement/        # Movement validation (PlayerProcessMovementSystem, Velocity, etc.)
├── combat/          # Hit detection (HitDetectionExecutor)
├── damage/          # Damage pipeline (DamageSystems, Damage, DamageCause)
├── entity/          # Player/entity components (Player, TransformComponent)
├── collision/       # Collision detection (CollisionModule, CollisionResult)
├── protocol/        # Network packets (MouseInteraction, ChangeVelocity)
├── events/          # Event classes (BreakBlockEvent, PlaceBlockEvent)
└── plugin/          # Permission system (PermissionsModule)
```

---

## Detection Capabilities

### Movement Checks
| Check | Description | Status |
|-------|-------------|--------|
| Speed | Detects movement speed exceeding allowed limits | Planned |
| Fly | Detects unauthorized flight | Planned |
| Phase | Detects clipping through blocks | Planned |
| NoFall | Detects fall damage bypass | Planned |
| Jesus | Detects walking on water | Planned |
| Step | Detects instant step up | Planned |
| Velocity | Detects velocity manipulation | Planned |

### Combat Checks
| Check | Description | Status |
|-------|-------------|--------|
| Reach | Detects hitting from excessive distance | Planned |
| HitRate | Detects impossible click rates | Planned |
| Killaura | Detects automated combat | Planned |
| AutoClicker | Detects click pattern consistency | Planned |
| AimAssist | Detects unnatural aim smoothing | Planned |
| Criticals | Detects invalid critical hits | Planned |

### World Checks
| Check | Description | Status |
|-------|-------------|--------|
| FastBreak | Detects breaking blocks too quickly | Planned |
| Scaffold | Detects automated bridging | Planned |
| Xray | Detects suspicious ore discovery patterns | Planned |
| Nuker | Detects mass block destruction | Planned |

---

## Commands

```
/hg alerts [on|off]         - Toggle staff alerts
/hg check <player>          - View player's violation levels
/hg violations [player]     - View recent violations
/hg toggle <check>          - Enable/disable a check
/hg reload                  - Reload configuration
/hg exempt <player> [check] - Temporarily exempt a player
/hg debug <player>          - Enable debug mode for a player
/hg info                    - Show plugin info and status
```

---

## Permissions

```
hyperguard.command              - Base command access
hyperguard.alerts               - Receive violation alerts
hyperguard.admin                - Full admin access
hyperguard.bypass               - Bypass all checks
hyperguard.bypass.<check>       - Bypass specific check
```

---

## Configuration

Configuration is JSON-based and supports per-check customization:

```json
{
  "checks": {
    "speed": {
      "enabled": true,
      "tolerance": 0.1,
      "vlDecayRate": 0.5,
      "actions": [
        {"threshold": 20, "action": "WARN"},
        {"threshold": 50, "action": "KICK"},
        {"threshold": 100, "action": "BAN"}
      ]
    }
  }
}
```

---

## Building

```bash
# Build the plugin
./gradlew shadowJar

# Output: build/libs/HyperGuard-<version>.jar
```

### Requirements
- Java 25 (Temurin recommended)
- Gradle 9.3+

---

## Installation

1. Build the plugin or download from releases
2. Place `HyperGuard-<version>.jar` in your server's `mods/` directory
3. Start the server to generate default configuration
4. Customize `plugins/HyperGuard/config.json`
5. Use `/hg reload` to apply changes

---

## Contributing

This project is developed by [HyperSystemsDev](https://github.com/HyperSystemsDev).

Join our [Discord](https://discord.gg/SNPjyfkYPc) for support and discussion.

---

## License

All rights reserved. This is proprietary software.

---

*Built with care for the Hytale community.*
