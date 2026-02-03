# Changelog

All notable changes to HyperGuard will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Movement Detection** - Speed, fly, phase, no-fall, step checks
- **Combat Detection** - Reach, killaura, auto-clicker, aim assist (planned)
- **World Interaction** - Fast-break, x-ray patterns, scaffold, nuker (planned)
- **Configurable Actions** - Warn, kick, tempban, permanent ban with VL thresholds
- **Staff Tools** - Real-time alerts, player inspection, debug mode
- **Permission Integration** - Bypass nodes for staff, per-check exemptions
- **Performance Optimized** - Designed for 100+ concurrent players

### Movement Checks

| Check | Status |
|-------|--------|
| Speed | Implemented |
| Fly | Implemented |
| Phase | Testing |
| NoFall | Testing |
| Step | Testing |
| Jesus | Planned |
| Velocity | Planned |

### Commands

- `/hg alerts [on|off]` - Toggle staff alerts
- `/hg check <player>` - View player's violation levels
- `/hg violations [player]` - View recent violations
- `/hg toggle <check>` - Enable/disable a check
- `/hg reload` - Reload configuration
- `/hg exempt <player> [check]` - Temporarily exempt a player
- `/hg debug <player>` - Enable debug mode for a player
- `/hg info` - Show plugin info and status

## [0.1.0] - TBD

### Added

- Initial development release
- Movement check framework
- Calibrated movement constants for Hytale physics
- Staff alert system
