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

## Development Status

**Current Phase:** Phase 2 - Movement Check Calibration

HyperGuard is actively being developed and calibrated for Hytale's unique movement physics. Movement checks have been implemented and are being fine-tuned to minimize false positives while maintaining detection accuracy.

### Recent Updates (Phase 2)

- **Speed Check**: Calibrated with empirical Hytale movement values
  - Walking: ~0.28-0.37 blocks/tick
  - Sprinting: ~0.56-0.60 blocks/tick
  - Sprint-jumping: ~0.75-0.91 blocks/tick
  - Speed-based state inference (walking/sprinting/sprint-jumping)

- **Fly Check**: Improved jump detection
  - Dual detection: ground-based and velocity-change based
  - 15-tick grace period for jump ascent
  - Bunny hop / consecutive jump support
  - Gravity and hovering detection

- **Thread-Safe Architecture**: All checks run on background scheduler
  - Position history tracking via `PositionHistory` ring buffer
  - Movement state inference from speed (ECS components unavailable async)

---

## Detection Capabilities

### Movement Checks
| Check | Description | Status |
|-------|-------------|--------|
| Speed | Detects movement speed exceeding allowed limits | ✅ Implemented |
| Fly | Detects unauthorized flight and gravity bypass | ✅ Implemented |
| Phase | Detects clipping through blocks | ✅ Implemented (needs testing) |
| NoFall | Detects fall damage bypass | ✅ Implemented (needs testing) |
| Step | Detects instant step up | ✅ Implemented (needs testing) |
| Jesus | Detects walking on water | 🔜 Planned |
| Velocity | Detects velocity manipulation | 🔜 Planned |

### Combat Checks
| Check | Description | Status |
|-------|-------------|--------|
| Reach | Detects hitting from excessive distance | 🔜 Planned |
| HitRate | Detects impossible click rates | 🔜 Planned |
| Killaura | Detects automated combat | 🔜 Planned |
| AutoClicker | Detects click pattern consistency | 🔜 Planned |
| AimAssist | Detects unnatural aim smoothing | 🔜 Planned |
| Criticals | Detects invalid critical hits | 🔜 Planned |

### World Checks
| Check | Description | Status |
|-------|-------------|--------|
| FastBreak | Detects breaking blocks too quickly | 🔜 Planned |
| Scaffold | Detects automated bridging | 🔜 Planned |
| Xray | Detects suspicious ore discovery patterns | 🔜 Planned |
| Nuker | Detects mass block destruction | 🔜 Planned |

---

## Testing Status

### Needs Further Testing
- **Phase Check**: Vertical phase detection may trigger on stairs/slopes
- **NoFall Check**: Fall damage threshold calibration needed
- **Step Check**: Step height validation needs testing
- **Speed Check**: Edge cases with doors/interactions may need exemptions

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
  "general": {
    "alertsEnabled": true,
    "loggingEnabled": true,
    "debugMode": false,
    "bypassPermission": "hyperguard.bypass",
    "alertPermission": "hyperguard.alerts",
    "joinExemptionTicks": 100,
    "teleportExemptionTicks": 40
  },
  "checks": {
    "speed": {
      "enabled": true,
      "tolerance": 0.10,
      "vlMultiplier": 1.0,
      "vlDecayRate": 0.5,
      "maxVL": 100.0,
      "thresholds": [
        {"threshold": 20.0, "action": "warn"},
        {"threshold": 50.0, "action": "kick"},
        {"threshold": 100.0, "action": "ban"}
      ]
    }
  }
}
```

### Calibrated Movement Constants (Hytale)

| Constant | Value | Description |
|----------|-------|-------------|
| `WALK_SPEED` | 0.37 | Walking speed (blocks/tick) |
| `SPRINT_SPEED` | 0.90 | Max sprint-jump speed (blocks/tick) |
| `JUMP_VELOCITY` | 0.42 | Initial jump velocity |
| `GRAVITY` | 0.08 | Gravity acceleration |
| `TERMINAL_VELOCITY` | 3.92 | Max falling speed |

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
