# HyperGuard

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?logo=discord&logoColor=white)](https://discord.gg/SNPjyfkYPc)
[![GitHub](https://img.shields.io/github/stars/HyperSystemsDev/HyperGuard?style=social)](https://github.com/HyperSystemsDev/HyperGuard)

Comprehensive server-side anti-cheat for Hytale. Part of the **HyperSystems** plugin suite.

**Version:** 0.1.0 (Development)
**Game:** Hytale Early Access
**License:** GPLv3

---

## Overview

HyperGuard is a high-accuracy, low false-positive anti-cheat plugin designed specifically for Hytale servers. It provides comprehensive cheat detection while maintaining excellent performance for high-player-count servers.

---

## Key Features

- **Movement Detection** - Speed, fly, phase, no-fall, velocity manipulation
- **Combat Detection** - Reach, killaura, auto-clicker, aim assist, criticals
- **World Interaction** - Fast-break, x-ray patterns, scaffold, nuker
- **Configurable Actions** - Warn, kick, tempban, permanent ban with VL thresholds
- **Staff Tools** - Real-time alerts, player inspection, debug mode
- **Permission Integration** - Bypass nodes for staff, per-check exemptions
- **Performance Optimized** - Designed for 100+ concurrent players

---

## Development Status

**Current Phase:** Phase 2 - Movement Check Calibration

HyperGuard is actively being developed and calibrated for Hytale's unique movement physics.

### Detection Status

| Category | Check | Status |
|----------|-------|--------|
| Movement | Speed | Implemented |
| Movement | Fly | Implemented |
| Movement | Phase | Implemented (testing) |
| Movement | NoFall | Implemented (testing) |
| Movement | Step | Implemented (testing) |
| Movement | Jesus | Planned |
| Movement | Velocity | Planned |
| Combat | Reach | Planned |
| Combat | HitRate | Planned |
| Combat | Killaura | Planned |
| Combat | AutoClicker | Planned |
| World | FastBreak | Planned |
| World | Scaffold | Planned |
| World | Xray | Planned |

---

## Installation

1. Build the plugin or download from releases
2. Place `HyperGuard-<version>.jar` in your server's `mods` directory
3. Start the server to generate default configuration
4. Customize `mods/com.hyperguard_HyperGuard/config.json`
5. Use `/hg reload` to apply changes

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hg alerts [on\|off]` | Toggle staff alerts | `hyperguard.alerts` |
| `/hg check <player>` | View player's violation levels | `hyperguard.command` |
| `/hg violations [player]` | View recent violations | `hyperguard.command` |
| `/hg toggle <check>` | Enable/disable a check | `hyperguard.admin` |
| `/hg reload` | Reload configuration | `hyperguard.admin` |
| `/hg exempt <player> [check]` | Temporarily exempt a player | `hyperguard.admin` |
| `/hg debug <player>` | Enable debug mode for a player | `hyperguard.admin` |
| `/hg info` | Show plugin info and status | `hyperguard.command` |

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperguard.command` | Base command access | op |
| `hyperguard.alerts` | Receive violation alerts | op |
| `hyperguard.admin` | Full admin access | op |
| `hyperguard.bypass` | Bypass all checks | op |
| `hyperguard.bypass.<check>` | Bypass specific check | op |

---

## Configuration

Configuration file: `mods/com.hyperguard_HyperGuard/config.json`

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

## Documentation

| Document | Description |
|----------|-------------|
| [ANTICHEAT_FEASIBILITY.md](ANTICHEAT_FEASIBILITY.md) | Server analysis and feasibility study |
| [PLUGIN_API.md](PLUGIN_API.md) | Hytale plugin development API reference |
| [EVENTS.md](EVENTS.md) | Available events and hook points |
| [PERMISSIONS.md](PERMISSIONS.md) | Permission system documentation |

---

## Building from Source

### Requirements

- Java 21+ (for building)
- Java 25 (for running on Hytale server)
- Gradle 8.12+
- Hytale Server (Early Access)

```bash
./gradlew shadowJar
```

The output JAR will be in `build/libs/`.

---

## Support

- **Discord:** https://discord.gg/SNPjyfkYPc
- **GitHub Issues:** https://github.com/HyperSystemsDev/HyperGuard/issues

---

## Credits

Developed by **HyperSystemsDev**

Part of the **HyperSystems** plugin suite:
- [HyperPerms](https://github.com/HyperSystemsDev/HyperPerms) - Advanced permissions
- [HyperHomes](https://github.com/HyperSystemsDev/HyperHomes) - Home teleportation
- [HyperFactions](https://github.com/HyperSystemsDev/HyperFactions) - Faction management
- [HyperWarp](https://github.com/HyperSystemsDev/HyperWarp) - Warps, spawns, TPA

---

*HyperGuard - Fair Play, Always*
