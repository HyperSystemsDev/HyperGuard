# Hytale Anti-Cheat Plugin Feasibility Analysis

> **Document Version:** 1.0
> **Last Updated:** February 2026
> **Source:** Decompiled from `HytaleServer.jar`
> **Target:** HyperAntiCheat Plugin Development

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Server Architecture Overview](#server-architecture-overview)
3. [Plugin API Capabilities](#plugin-api-capabilities)
4. [Movement Systems Analysis](#movement-systems-analysis)
5. [Combat & Damage Systems Analysis](#combat--damage-systems-analysis)
6. [Network Protocol Analysis](#network-protocol-analysis)
7. [Event System Deep Dive](#event-system-deep-dive)
8. [Anti-Cheat Check Feasibility Matrix](#anti-cheat-check-feasibility-matrix)
9. [Implementation Recommendations](#implementation-recommendations)
10. [Code References & Examples](#code-references--examples)
11. [Appendix: Key Classes Reference](#appendix-key-classes-reference)

---

## Executive Summary

### Overall Feasibility: **HIGHLY FEASIBLE** (9/10)

The Hytale server provides an excellent foundation for building a comprehensive anti-cheat system. Key findings:

| Category | Rating | Notes |
|----------|--------|-------|
| Plugin API | ⭐⭐⭐⭐⭐ | Full-featured with events, commands, tasks |
| Movement Tracking | ⭐⭐⭐⭐⭐ | Dual velocity system (server + client) |
| Combat Validation | ⭐⭐⭐⭐⭐ | Ray-based hit detection with LOS |
| Network Access | ⭐⭐⭐⭐ | Packet interception possible |
| Event Hooks | ⭐⭐⭐⭐⭐ | 77+ hookable events |
| Built-in Validation | ⭐⭐⭐⭐ | Some anti-cheat already exists |

### Key Advantages

1. **Server-Authoritative Architecture**: Server maintains authoritative state for positions and physics
2. **Dual Velocity Tracking**: Both server-calculated and client-reported velocities are tracked
3. **Client Timestamps**: Packets include client timestamps for timing analysis
4. **Rich Event System**: Clean hooks without needing to patch core code
5. **ECS Pattern**: Efficient entity queries and component access

### Primary Challenges

1. **No Direct Packet Interception API**: Must work through events and entity components
2. **Client-Side Prediction**: Some movement is predicted client-side
3. **Latency Considerations**: Must account for network delay in validations
4. **Limited Documentation**: Requires reverse engineering effort

---

## Server Architecture Overview

### Package Structure

```
com.hypixel.hytale/
├── server/
│   ├── core/
│   │   ├── entity/           # Entity system (Player, LivingEntity)
│   │   │   ├── entities/     # Player, NPC implementations
│   │   │   ├── damage/       # Damage data components
│   │   │   ├── knockback/    # Knockback system
│   │   │   └── movement/     # Movement state components
│   │   ├── modules/
│   │   │   ├── entity/       # Entity module (movement, collision)
│   │   │   │   ├── player/   # Player-specific systems
│   │   │   │   ├── damage/   # Damage processing
│   │   │   │   └── hitboxcollision/
│   │   │   ├── physics/      # Physics & velocity
│   │   │   ├── collision/    # Collision detection
│   │   │   └── interaction/  # Player interactions
│   │   ├── event/            # Event system
│   │   │   └── events/       # All event classes
│   │   ├── plugin/           # Plugin API
│   │   ├── receiver/         # Packet receivers
│   │   ├── io/               # Network I/O
│   │   └── universe/         # World/Player management
│   ├── npc/                  # NPC AI systems
│   └── worldgen/             # World generation
├── protocol/
│   ├── packets/              # All packet definitions
│   │   ├── player/           # Player packets
│   │   ├── entities/         # Entity packets
│   │   ├── interaction/      # Interaction packets
│   │   └── world/            # World packets
│   └── io/                   # Packet I/O utilities
├── component/                # ECS component system
├── math/
│   └── hitdetection/         # Hit detection algorithms
├── builtin/                  # Built-in features
│   ├── parkour/
│   ├── sprintforce/
│   ├── crouchslide/
│   └── blockphysics/
└── event/                    # Core event bus
```

### Entity Component System (ECS)

The server uses a custom ECS architecture:

```java
// Core ECS types
Store<EntityStore>              // Entity storage
ArchetypeChunk<EntityStore>     // Grouped entities by component signature
ComponentType<EntityStore, T>   // Component type definition
Ref<EntityStore>                // Entity reference
CommandBuffer<EntityStore>      // Deferred operations
Query<EntityStore>              // Entity queries
```

**Key Implications for Anti-Cheat:**
- Efficient bulk queries for player validation
- Components can be added/removed at runtime
- Systems process entities in parallel where safe

---

## Plugin API Capabilities

### Plugin Lifecycle

```java
public class AntiCheatPlugin extends JavaPlugin {

    public AntiCheatPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Phase 1: Register commands, events, configs
        // Called during server startup
    }

    @Override
    protected void start() {
        // Phase 2: Plugin fully initialized
        // Server is accepting connections
    }

    @Override
    protected void shutdown() {
        // Phase 3: Cleanup
        // Save data, cancel tasks
    }
}
```

### Available Registries

| Registry | Purpose | Anti-Cheat Use |
|----------|---------|----------------|
| `CommandRegistry` | Register commands | Admin commands, player check commands |
| `EventRegistry` | Subscribe to events | Hook player actions |
| `TaskRegistry` | Schedule tasks | Periodic violation checks |
| `EntityRegistry` | Custom entity types | (Limited use) |
| `BlockStateRegistry` | Block states | X-ray detection data |
| `AssetRegistry` | Custom assets | (Limited use) |
| `EntityStoreRegistry` | Entity storage | Player data persistence |
| `ChunkStoreRegistry` | Chunk storage | Block tracking |

### Event Registration Patterns

```java
// Basic event registration
getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

// Priority-based registration
getEventRegistry().register(EventPriority.HIGH, BreakBlockEvent.class, this::onBlockBreak);

// Cancellable events
getEventRegistry().register(PlayerChatEvent.class, event -> {
    if (shouldBlock(event)) {
        event.setCancelled(true);
    }
});

// Async event handling
getEventRegistry().registerAsync(PlayerChatEvent.class, future -> {
    return future.thenApply(event -> {
        // Async processing
        return event;
    });
});

// Global listeners (all keys)
getEventRegistry().registerGlobal(ChunkUnloadEvent.class, this::onChunkUnload);
```

### Task Scheduling

```java
TaskRegistry tasks = getTaskRegistry();

// One-time delayed task
tasks.scheduleTask("checkViolations", () -> {
    processViolationQueue();
}, 20); // 20 ticks delay

// Repeating task (ideal for periodic checks)
tasks.scheduleRepeatingTask("movementAnalysis", () -> {
    analyzeAllPlayerMovement();
}, 0, 1); // Every tick
```

### Permission Integration

```java
// Check player permissions
playerRef.hasPermission("anticheat.bypass");
playerRef.hasPermission("anticheat.bypass.speed");
playerRef.hasPermission("anticheat.alerts");

// Permission provider access
PermissionsModule perms = PermissionsModule.get();
PermissionProvider provider = perms.getProvider();

// Add bypass permissions
provider.addUserPermissions(uuid, Set.of("anticheat.bypass"));
provider.addGroupPermissions("Staff", Set.of("anticheat.bypass"));
```

### Logging System

```java
HytaleLogger logger = getLogger();

logger.info("Player %s flagged for speed", playerName);
logger.warning("High violation count for %s", playerName);
logger.severe("Critical error in check: %s", checkName);

// With exception logging
logger.at(Level.SEVERE).withCause(exception).log("Check failed");
```

---

## Movement Systems Analysis

### Core Movement Classes

#### PlayerProcessMovementSystem

**Location:** `com.hypixel.hytale.server.core.modules.entity.player.PlayerProcessMovementSystem`

This is the primary movement validation system. Key features:

```java
public class PlayerProcessMovementSystem extends EntityTickingSystem<EntityStore> {

    // Query components needed for movement processing
    private final Query<EntityStore> query = Query.and(
        playerComponentType,
        playerRefComponentType,
        transformComponentType,
        boundingBoxComponentType,
        velocityComponentType,
        collisionResultComponentType,
        positionDataComponentType
    );

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        // EXISTING ANTI-CHEAT: Detects large position jumps
        if (collisionResultComponent.getCollisionPositionOffsetCopy().squaredLength() >= 100.0) {
            if (playerComponent.getGameMode() == GameMode.Adventure) {
                Entity.LOGGER.at(Level.WARNING).log(
                    "%s, %s: Jump in location in processMovementBlockCollisions %s",
                    playerRefComponent.getUsername(),
                    playerRefComponent.getUuid(),
                    collisionResultComponent.getCollisionPositionOffsetCopy().length()
                );
            }
            // Reset velocity on suspicious movement
            playerComponent.resetVelocity(velocityComponent);
        }

        // Process collision results
        CollisionModule.get().findIntersections(world, boundingBox, startPos, result, true, false);

        // Process velocity sample
        playerComponent.processVelocitySample(dt, positionOffset, velocityComponent);
    }
}
```

**Anti-Cheat Implications:**
- Position delta >10 blocks already triggers warning
- Velocity is reset on suspicious jumps
- Collision detection validates movement path
- Gamemode-aware logging exists

#### PlayerInput

**Location:** `com.hypixel.hytale.server.core.modules.entity.player.PlayerInput`

Handles all client input types:

```java
public class PlayerInput implements Component<EntityStore> {

    private final List<InputUpdate> inputUpdateQueue = new ObjectArrayList<>();

    // Input types sent from client:

    public static class AbsoluteMovement implements InputUpdate {
        private double x, y, z;

        @Override
        public void apply(CommandBuffer<EntityStore> buffer, ArchetypeChunk<EntityStore> chunk, int index) {
            playerComponent.moveTo(playerRef, x, y, z, buffer);
        }
    }

    public static class RelativeMovement implements InputUpdate {
        private double x, y, z;

        @Override
        public void apply(...) {
            Vector3d position = transformComponent.getPosition();
            playerComponent.moveTo(ref, position.x + x, position.y + y, position.z + z, buffer);
        }
    }

    public static class SetClientVelocity implements InputUpdate {
        private final Vector3d velocity;

        @Override
        public void apply(...) {
            velocityComponent.setClient(velocity);
        }
    }

    public record SetMovementStates(MovementStates movementStates) implements InputUpdate {
        // Sets: onGround, inFluid, rolling, etc.
    }

    public record SetBody(Direction direction) implements InputUpdate {
        // Body rotation (pitch, yaw, roll)
    }

    public record SetHead(Direction direction) implements InputUpdate {
        // Head rotation
    }

    public static class WishMovement implements InputUpdate {
        // Desired movement direction
    }
}
```

**Anti-Cheat Implications:**
- All movement types are captured in queue
- Can intercept and validate before application
- Client velocity is tracked separately
- Movement states (onGround, etc.) are client-reported

#### Velocity Component

**Location:** `com.hypixel.hytale.server.core.modules.physics.component.Velocity`

```java
public class Velocity implements Component<EntityStore> {

    // Server-authoritative velocity
    protected final Vector3d velocity = new Vector3d();

    // Client-reported velocity (for comparison)
    protected final Vector3d clientVelocity = new Vector3d();

    // Velocity change instructions
    protected final List<Instruction> instructions = new ObjectArrayList<>();

    public void set(double x, double y, double z) {
        velocity.assign(x, y, z);
    }

    public void setClient(double x, double y, double z) {
        clientVelocity.assign(x, y, z);
    }

    public void addForce(double x, double y, double z) {
        velocity.add(x, y, z);
    }

    public double getSpeed() {
        return velocity.length();
    }

    public void addInstruction(Vector3d velocity, VelocityConfig config, ChangeVelocityType type) {
        instructions.add(new Instruction(velocity, config, type));
    }
}
```

**Anti-Cheat Implications:**
- **CRITICAL**: Dual velocity system allows desync detection
- Compare `velocity` (server) vs `clientVelocity` (client)
- Velocity instructions track all changes
- Speed calculation available

### Movement State Tracking

**MovementStates** (from protocol):
```java
public class MovementStates {
    public boolean onGround;
    public boolean inFluid;
    public boolean rolling;      // Safety roll
    public boolean crouching;
    public boolean sprinting;
    public boolean climbing;
    public boolean swimming;
    public boolean flying;
    public boolean gliding;
}
```

### Player Component Movement Data

**Location:** `com.hypixel.hytale.server.core.entity.entities.Player`

```java
public class Player extends LivingEntity {

    // Velocity sampling for smoothing
    private static final int MAX_VELOCITY_SAMPLE_COUNT = 2;
    private static final int VELOCITY_SAMPLE_LENGTH = 12;
    private static final double[][] velocitySampleWeights = {{1.0}, {0.9, 0.1}};
    private final double[] velocitySamples = new double[12];
    private int velocitySampleCount;
    private int velocitySampleIndex = 4;

    // Fall distance tracking
    private double currentFallDistance;

    // Spawn protection
    protected long lastSpawnTimeNanos;
    public static final long RESPAWN_INVULNERABILITY_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(3000L);

    public void processVelocitySample(float dt, Vector3d positionOffset, Velocity velocity) {
        // Samples velocity over time for smoothing
    }

    public void resetVelocity(Velocity velocityComponent) {
        // Called when suspicious movement detected
    }

    public void setCurrentFallDistance(double distance) {
        this.currentFallDistance = distance;
    }

    public double getCurrentFallDistance() {
        return currentFallDistance;
    }
}
```

### Collision System

**Location:** `com.hypixel.hytale.server.core.modules.collision.CollisionModule`

```java
public class CollisionModule {

    public void findIntersections(
        World world,
        Box boundingBox,
        Vector3d position,
        CollisionResult result,
        boolean includeTriggers,
        boolean includeEntities
    ) {
        // Finds all collisions for a bounding box at position
    }
}
```

**CollisionResultComponent:**
```java
public class CollisionResultComponent {

    public boolean isPendingCollisionCheck();
    public Vector3d getCollisionStartPosition();
    public Vector3d getCollisionPositionOffset();
    public CollisionResult getCollisionResult();

    public void resetLocationChange();
}
```

---

## Combat & Damage Systems Analysis

### Damage Pipeline

The damage system follows a multi-phase pipeline:

```
┌─────────────────────────────────────────────────────────────────────┐
│                      DAMAGE PIPELINE                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. GATHER PHASE (GatherDamageGroup)                                │
│     ├── FallDamagePlayers        - Calculate fall damage            │
│     ├── FallDamageNPCs           - NPC fall damage                  │
│     ├── CanBreathe               - Drowning/suffocation             │
│     └── OutOfWorldDamage         - Void damage                      │
│                                                                      │
│  2. FILTER PHASE (FilterDamageGroup)                                │
│     ├── FilterUnkillable         - Check invulnerability            │
│     ├── FilterPlayerWorldConfig  - Check PvP settings               │
│     ├── FilterNPCWorldConfig     - Check NPC damage settings        │
│     ├── PlayerDamageFilterSystem - Spawn protection                 │
│     ├── ArmorDamageReduction     - Calculate armor mitigation       │
│     ├── ArmorKnockbackReduction  - Reduce knockback                 │
│     ├── WieldingDamageReduction  - Shield/block reduction           │
│     └── WieldingKnockbackReduction                                  │
│                                                                      │
│  3. APPLY PHASE                                                      │
│     └── ApplyDamage              - Subtract health, check death     │
│                                                                      │
│  4. INSPECT PHASE (InspectDamageGroup)                              │
│     ├── ApplyParticles           - Spawn hit particles              │
│     ├── ApplySoundEffects        - Play hit sounds                  │
│     ├── HitAnimation             - Play hurt animation              │
│     ├── EntityUIEvents           - Combat text display              │
│     ├── ReticleEvents            - Crosshair feedback               │
│     ├── PlayerHitIndicators      - Damage direction indicator       │
│     ├── DamageArmor              - Damage armor durability          │
│     ├── DamageAttackerTool       - Damage weapon durability         │
│     ├── DamageStamina            - Consume stamina on block         │
│     ├── TrackLastDamage          - Record damage time               │
│     └── RecordLastCombat         - Track combat state               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Damage Class

**Location:** `com.hypixel.hytale.server.core.modules.entity.damage.Damage`

```java
public class Damage {

    // Damage source types
    public static final Damage.Source NULL_SOURCE = ...;

    public interface Source {}

    public static class EntitySource implements Source {
        private final Ref<EntityStore> ref;

        public Ref<EntityStore> getRef() {
            return ref;
        }
    }

    // Metadata keys
    public static final MetaKey<Vector4d> HIT_LOCATION;
    public static final MetaKey<Float> HIT_ANGLE;
    public static final MetaKey<KnockbackComponent> KNOCKBACK_COMPONENT;
    public static final MetaKey<Particles> IMPACT_PARTICLES;
    public static final MetaKey<SoundEffect> IMPACT_SOUND_EFFECT;
    public static final MetaKey<Boolean> BLOCKED;
    public static final MetaKey<Boolean> CAN_BE_PREDICTED;
    public static final MetaKey<Float> STAMINA_DRAIN_MULTIPLIER;

    // Core properties
    private Source source;
    private DamageCause cause;
    private float amount;
    private float initialAmount;
    private boolean cancelled;

    public Damage(Source source, DamageCause cause, float amount) {
        this.source = source;
        this.cause = cause;
        this.amount = amount;
        this.initialAmount = amount;
    }

    // Getters/setters
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    // Metadata access
    public <T> T getIfPresentMetaObject(MetaKey<T> key);
    public <T> void putMetaObject(MetaKey<T> key, T value);
}
```

### Hit Detection System

**Location:** `com.hypixel.hytale.math.hitdetection.HitDetectionExecutor`

```java
public class HitDetectionExecutor {

    private final Matrix4d pvmMatrix = new Matrix4d();      // Projection-View-Model
    private final Matrix4d invPvMatrix = new Matrix4d();    // Inverse for unprojection
    private final Vector4d origin = new Vector4d();          // Ray origin
    private final HitDetectionBuffer buffer = new HitDetectionBuffer();

    private MatrixProvider projectionProvider;
    private MatrixProvider viewProvider;
    private LineOfSightProvider losProvider = LineOfSightProvider.DEFAULT_TRUE;
    private int maxRayTests = 10;

    public HitDetectionExecutor setOrigin(double x, double y, double z) {
        origin.assign(x, y, z, 1.0);
        return this;
    }

    public HitDetectionExecutor setLineOfSightProvider(LineOfSightProvider losProvider) {
        this.losProvider = losProvider;
        return this;
    }

    // Test a single point
    public boolean test(Vector4d point, Matrix4d modelMatrix) {
        setupMatrices(modelMatrix);
        return testPoint(point);
    }

    // Test a model (collection of quads)
    public boolean test(Quad4d[] model, Matrix4d modelMatrix) {
        setupMatrices(modelMatrix);
        return testModel(model);
    }

    private boolean testPoint(Vector4d point) {
        pvmMatrix.multiply(point, buffer.transformedPoint);

        // Frustum check
        if (!buffer.transformedPoint.isInsideFrustum()) {
            return false;
        }

        // Unproject and LOS check
        Vector4d hit = buffer.transformedPoint;
        invPvMatrix.multiply(hit);
        hit.perspectiveTransform();

        return losProvider.test(origin.x, origin.y, origin.z, hit.x, hit.y, hit.z);
    }

    private boolean testModel(Quad4d[] model) {
        int testsDone = 0;
        double minDistanceSquared = Double.POSITIVE_INFINITY;

        for (Quad4d quad : model) {
            if (testsDone++ == maxRayTests) {
                return false;  // Too many tests
            }

            quad.multiply(pvmMatrix, buffer.transformedQuad);

            if (insideFrustum()) {
                Vector4d hit = buffer.tempHitPosition;
                // ... calculate hit position

                // Distance check
                double dx = origin.x - hit.x;
                double dy = origin.y - hit.y;
                double dz = origin.z - hit.z;
                double distanceSquared = dx*dx + dy*dy + dz*dz;

                // LOS check
                if (distanceSquared < minDistanceSquared &&
                    losProvider.test(origin.x, origin.y, origin.z, hit.x, hit.y, hit.z)) {
                    minDistanceSquared = distanceSquared;
                    buffer.hitPosition.assign(hit);
                }
            }
        }

        return minDistanceSquared != Double.POSITIVE_INFINITY;
    }

    public Vector4d getHitLocation() {
        return buffer.hitPosition;
    }
}
```

**Anti-Cheat Implications:**
- Ray-based hit detection validates targeting
- Line-of-sight checks prevent through-wall hits
- Distance calculation available for reach checks
- Frustum culling ensures target is visible
- Max ray tests prevents DoS

### Fall Damage System

**Location:** `com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.FallDamagePlayers`

```java
public static class FallDamagePlayers extends EntityTickingSystem<EntityStore> {

    static final float CURVE_MODIFIER = 0.58F;
    static final float CURVE_MULTIPLIER = 2.0F;
    public static final double MIN_DAMAGE = 10.0;

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        PlayerInput playerInput = chunk.getComponent(index, PlayerInput.getComponentType());
        Velocity velocity = chunk.getComponent(index, Velocity.getComponentType());

        // Get client-reported Y velocity
        double yVelocity = Math.abs(velocity.getClientVelocity().getY());

        MovementConfig movementConfig = MovementConfig.getAssetMap().getAsset(configIndex);
        float minFallSpeedToEngageRoll = movementConfig.getMinFallSpeedToEngageRoll();

        // Process movement state updates from input queue
        for (PlayerInput.InputUpdate update : playerInput.getMovementUpdateQueue()) {
            if (update instanceof PlayerInput.SetClientVelocity velocityEntry) {
                yVelocity = Math.abs(velocityEntry.getVelocity().y);
            }
            else if (update instanceof PlayerInput.SetMovementStates movementStates) {
                if (movementStates.movementStates().onGround &&
                    player.getCurrentFallDistance() > 0.0) {

                    if (yVelocity > minFallSpeedToEngageRoll &&
                        !movementStates.movementStates().inFluid) {

                        // Calculate damage
                        double damagePercentage = Math.pow(
                            CURVE_MODIFIER * (yVelocity - minFallSpeedToEngageRoll),
                            CURVE_MULTIPLIER
                        ) + MIN_DAMAGE;

                        float maxHealth = healthStatValue.getMax();
                        double healthModifier = maxHealth / 100.0;
                        int damageInt = (int) Math.floor(healthModifier * damagePercentage);

                        // Rolling mitigates damage
                        if (movementStates.movementStates().rolling) {
                            if (yVelocity <= movementConfig.getMaxFallSpeedRollFullMitigation()) {
                                damageInt = 0;
                            } else if (yVelocity <= movementConfig.getMaxFallSpeedToEngageRoll()) {
                                damageInt = (int)(damageInt * (1.0 - mitigationPercent / 100.0));
                            }
                        }

                        if (damageInt > 0) {
                            Damage damage = new Damage(Damage.NULL_SOURCE, DamageCause.FALL, damageInt);
                            DamageSystems.executeDamage(index, chunk, buffer, damage);
                        }
                    }

                    player.setCurrentFallDistance(0.0);
                }
            }
        }
    }
}
```

**Anti-Cheat Implications:**
- Fall distance tracked server-side
- Client velocity used for damage calculation
- Rolling state affects damage - can detect invalid rolls
- Can compare expected damage vs actual

---

## Network Protocol Analysis

### Packet System Overview

**Core Classes:**
- `Packet` - Base packet interface
- `PacketRegistry` - Packet type registration
- `PacketIO` - Serialization utilities
- `PacketEncoder` / `PacketDecoder` - Netty handlers

### Key Packets for Anti-Cheat

#### MouseInteraction Packet (ID: 111)

**Location:** `com.hypixel.hytale.protocol.packets.player.MouseInteraction`

```java
public class MouseInteraction implements Packet {

    public static final int PACKET_ID = 111;

    public long clientTimestamp;           // Client-reported time
    public int activeSlot;                 // Hotbar slot
    public String itemInHandId;            // Item being used
    public Vector2f screenPoint;           // Screen coordinates
    public MouseButtonEvent mouseButton;   // Click event
    public MouseMotionEvent mouseMotion;   // Mouse movement
    public WorldInteraction worldInteraction;  // Target block/entity
}
```

**WorldInteraction Structure:**
```java
public class WorldInteraction {
    public Vector3i blockPosition;     // Targeted block
    public int blockFace;              // Face clicked
    public int entityId;               // Targeted entity network ID
    public Vector3d hitPosition;       // Exact hit location
}
```

**Anti-Cheat Uses:**
- `clientTimestamp` - Detect timing manipulation, measure CPS
- `screenPoint` - Validate aim consistency
- `worldInteraction.entityId` - Track target switching
- `worldInteraction.hitPosition` - Validate hit location

#### Movement Packets

Movement is handled through the `PlayerInput` component queue rather than discrete packets, but the data comes from network:

```java
// Client sends position updates that populate:
PlayerInput.AbsoluteMovement(x, y, z)
PlayerInput.RelativeMovement(dx, dy, dz)
PlayerInput.SetClientVelocity(velocity)
PlayerInput.SetMovementStates(states)
PlayerInput.SetBody(direction)
PlayerInput.SetHead(direction)
```

#### ChangeVelocity Packet

**Location:** `com.hypixel.hytale.protocol.packets.entities.ChangeVelocity`

```java
public class ChangeVelocity implements Packet {
    public int entityId;
    public Vector3d velocity;
    public ChangeVelocityType type;   // SET, ADD, etc.
    public VelocityConfig config;      // Animation config
}
```

### Packet Validation

The protocol includes validation:

```java
public static ValidationResult validateStructure(ByteBuf buffer, int offset) {
    if (buffer.readableBytes() - offset < 52) {
        return ValidationResult.error("Buffer too small: expected at least 52 bytes");
    }

    // Field validation...
    if (itemInHandIdLen > 4096000) {
        return ValidationResult.error("ItemInHandId exceeds max length 4096000");
    }

    return ValidationResult.OK;
}
```

### Packet Interception Points

**PlayerPacketWatcher:**
```java
// Location: com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher
public interface PlayerPacketWatcher {
    void onPacket(Packet packet);
}
```

**PlayerPacketFilter:**
```java
// Location: com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter
public interface PlayerPacketFilter {
    boolean shouldFilter(Packet packet);
}
```

---

## Event System Deep Dive

### Event Architecture

```java
// Core interfaces
public interface IBaseEvent<KeyType> {}
public interface IEvent<KeyType> extends IBaseEvent<KeyType> {}      // Sync
public interface IAsyncEvent<KeyType> extends IBaseEvent<KeyType> {} // Async
public interface ICancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
```

### Complete Event Catalog

#### Player Events
| Event | Cancellable | Async | Key Data |
|-------|-------------|-------|----------|
| `PlayerConnectEvent` | No | No | PlayerRef, World |
| `PlayerDisconnectEvent` | No | No | PlayerRef |
| `PlayerReadyEvent` | No | No | PlayerRef |
| `PlayerChatEvent` | Yes | Yes | Sender, Content, Targets |
| `PlayerInteractEvent` | No | No | PlayerRef |
| `PlayerCraftEvent` | No | No | PlayerRef, Recipe |
| `PlayerMouseButtonEvent` | No | No | Button, Position |
| `PlayerMouseMotionEvent` | No | No | Motion delta |
| `AddPlayerToWorldEvent` | No | No | PlayerRef, World |
| `DrainPlayerFromWorldEvent` | No | No | PlayerRef, World |
| `PlayerSetupConnectEvent` | No | No | Connection setup |
| `PlayerSetupDisconnectEvent` | No | No | Connection teardown |

#### ECS/Block Events
| Event | Cancellable | Key Data |
|-------|-------------|----------|
| `BreakBlockEvent` | Yes | Position, BlockType, ItemInHand |
| `PlaceBlockEvent` | Yes | Position, BlockType |
| `DamageBlockEvent` | No | Position, Damage |
| `UseBlockEvent` | No | Position, BlockType |
| `DropItemEvent` | No | ItemStack, Position |
| `InteractivelyPickupItemEvent` | No | ItemStack |
| `SwitchActiveSlotEvent` | No | OldSlot, NewSlot |
| `ChangeGameModeEvent` | No | OldMode, NewMode |
| `CraftRecipeEvent` | No | Recipe, Result |

#### Entity Events
| Event | Key Data |
|-------|----------|
| `EntityRemoveEvent` | Entity reference |
| `LivingEntityInventoryChangeEvent` | Inventory changes |
| `LivingEntityUseBlockEvent` | Block interaction |

#### World Events
| Event | Key Data |
|-------|----------|
| `AddWorldEvent` | World reference |
| `RemoveWorldEvent` | World reference |
| `StartWorldEvent` | World reference |
| `AllWorldsLoadedEvent` | (none) |
| `ChunkPreLoadProcessEvent` | Chunk coordinates |
| `ChunkSaveEvent` | Chunk data |
| `ChunkUnloadEvent` | Chunk coordinates |
| `MoonPhaseChangeEvent` | Phase |

#### Permission Events
| Event | Key Data |
|-------|----------|
| `PlayerPermissionChangeEvent` | UUID, Permissions, Added/Removed |
| `GroupPermissionChangeEvent` | GroupName, Permissions, Added/Removed |
| `PlayerGroupEvent` | UUID, GroupName, Added/Removed |

#### Server Events
| Event | Key Data |
|-------|----------|
| `BootEvent` | (none) |
| `ShutdownEvent` | (none) |
| `PrepareUniverseEvent` | Universe reference |

### Event Registration Examples

```java
@Override
protected void setup() {
    EventRegistry events = getEventRegistry();

    // Movement-related (via player ready and tick)
    events.register(PlayerReadyEvent.class, this::onPlayerReady);
    events.register(PlayerConnectEvent.class, this::onPlayerConnect);
    events.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

    // Block interaction
    events.register(EventPriority.HIGH, BreakBlockEvent.class, this::onBlockBreak);
    events.register(EventPriority.HIGH, PlaceBlockEvent.class, this::onBlockPlace);

    // Combat (via mouse events)
    events.register(PlayerMouseButtonEvent.class, this::onMouseButton);

    // Inventory
    events.register(SwitchActiveSlotEvent.class, this::onSlotSwitch);

    // Chat (can cancel)
    events.register(PlayerChatEvent.class, event -> {
        if (isSpamming(event.getSender())) {
            event.setCancelled(true);
        }
    });
}

private void onPlayerReady(PlayerReadyEvent event) {
    // Initialize player tracking data
    PlayerRef player = event.getPlayerRef();
    initializePlayerData(player);
}

private void onBlockBreak(BreakBlockEvent event) {
    // Check break speed
    if (isBreakingTooFast(event)) {
        event.setCancelled(true);
        flagViolation(event.getPlayer(), "FastBreak");
    }
}
```

---

## Anti-Cheat Check Feasibility Matrix

### Movement Checks

| Check | Feasibility | Server Support | Implementation Difficulty | Notes |
|-------|-------------|----------------|---------------------------|-------|
| **Speed Hack** | ⭐⭐⭐⭐⭐ | Excellent | Easy | Compare position delta vs max allowed |
| **Fly Hack** | ⭐⭐⭐⭐⭐ | Excellent | Easy | Track onGround + gravity |
| **Teleport/Phase** | ⭐⭐⭐⭐⭐ | Built-in | Already done | >10 block jump detection exists |
| **NoFall** | ⭐⭐⭐⭐⭐ | Excellent | Easy | Compare fall distance vs damage |
| **No-Clip** | ⭐⭐⭐⭐⭐ | Excellent | Medium | Collision system validates |
| **Jesus/WaterWalk** | ⭐⭐⭐⭐ | Good | Medium | Check inFluid + onGround states |
| **Spider/WallClimb** | ⭐⭐⭐⭐ | Good | Medium | Validate climbing state |
| **Step** | ⭐⭐⭐⭐ | Good | Medium | Check Y delta on ground |
| **Velocity** | ⭐⭐⭐⭐⭐ | Excellent | Easy | Dual velocity comparison |
| **Timer** | ⭐⭐⭐ | Moderate | Hard | Requires packet timing analysis |

### Combat Checks

| Check | Feasibility | Server Support | Implementation Difficulty | Notes |
|-------|-------------|----------------|---------------------------|-------|
| **Reach** | ⭐⭐⭐⭐⭐ | Excellent | Medium | HitDetectionExecutor provides distance |
| **Hit Rate/CPS** | ⭐⭐⭐⭐ | Good | Easy | clientTimestamp in MouseInteraction |
| **Killaura** | ⭐⭐⭐ | Moderate | Hard | Requires pattern analysis |
| **Auto-Clicker** | ⭐⭐⭐⭐ | Good | Medium | Analyze click timing variance |
| **Aim Assist** | ⭐⭐⭐ | Moderate | Hard | Analyze aim smoothness |
| **No Swing** | ⭐⭐⭐⭐ | Good | Easy | Check animation state |
| **Hit Through Walls** | ⭐⭐⭐⭐⭐ | Excellent | Easy | LineOfSightProvider |
| **Multi-Aura** | ⭐⭐⭐ | Moderate | Hard | Track simultaneous targets |
| **Criticals** | ⭐⭐⭐⭐ | Good | Medium | Validate fall distance for crits |

### World Interaction Checks

| Check | Feasibility | Server Support | Implementation Difficulty | Notes |
|-------|-------------|----------------|---------------------------|-------|
| **Fast Break** | ⭐⭐⭐⭐⭐ | Excellent | Easy | DamageBlockEvent timing |
| **X-Ray** | ⭐⭐⭐ | Moderate | Hard | Statistical ore ratio analysis |
| **Auto-Mine** | ⭐⭐⭐ | Moderate | Medium | Pattern detection |
| **Scaffold** | ⭐⭐⭐⭐ | Good | Medium | Block placement patterns |
| **Fast Place** | ⭐⭐⭐⭐ | Good | Easy | PlaceBlockEvent timing |
| **Nuker** | ⭐⭐⭐⭐ | Good | Medium | Multi-block break detection |
| **Ghost Hand** | ⭐⭐⭐⭐ | Good | Medium | Range validation on interaction |

### Inventory Checks

| Check | Feasibility | Server Support | Implementation Difficulty | Notes |
|-------|-------------|----------------|---------------------------|-------|
| **Inventory Hack** | ⭐⭐⭐⭐ | Good | Medium | Transaction validation |
| **Auto-Armor** | ⭐⭐⭐⭐ | Good | Medium | Timing on armor equip |
| **Auto-Tool** | ⭐⭐⭐ | Moderate | Medium | Tool switch patterns |
| **Chest Stealer** | ⭐⭐⭐⭐ | Good | Easy | Container interaction speed |

### Miscellaneous Checks

| Check | Feasibility | Server Support | Implementation Difficulty | Notes |
|-------|-------------|----------------|---------------------------|-------|
| **Chat Spam** | ⭐⭐⭐⭐⭐ | Excellent | Easy | PlayerChatEvent is cancellable |
| **Command Spam** | ⭐⭐⭐⭐ | Good | Easy | Track command frequency |
| **Bad Packets** | ⭐⭐⭐ | Moderate | Medium | Protocol validation exists |
| **Blink** | ⭐⭐⭐ | Moderate | Hard | Requires packet timing |
| **Derp/Headless** | ⭐⭐⭐⭐ | Good | Easy | Validate rotation values |

---

## Implementation Recommendations

### Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HyperAntiCheat                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                        Core Manager                             │ │
│  │  - Plugin lifecycle                                             │ │
│  │  - Configuration loading                                        │ │
│  │  - Check registration                                           │ │
│  │  - Player data management                                       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │   Movement    │  │    Combat     │  │    World/Block        │   │
│  │    Module     │  │    Module     │  │      Module           │   │
│  ├───────────────┤  ├───────────────┤  ├───────────────────────┤   │
│  │ SpeedCheck    │  │ ReachCheck    │  │ FastBreakCheck        │   │
│  │ FlyCheck      │  │ HitRateCheck  │  │ XrayCheck             │   │
│  │ PhaseCheck    │  │ KillauraCheck │  │ ScaffoldCheck         │   │
│  │ NoFallCheck   │  │ AimCheck      │  │ NukerCheck            │   │
│  │ VelocityCheck │  │ CriticalsCheck│  │ GhostHandCheck        │   │
│  │ JesusCheck    │  │ AutoClickCheck│  │                       │   │
│  │ StepCheck     │  │               │  │                       │   │
│  └───────────────┘  └───────────────┘  └───────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Violation Manager                            │ │
│  │  - VL accumulation with decay                                   │ │
│  │  - Configurable thresholds per check                           │ │
│  │  - Action triggers (warn, kick, tempban, ban)                  │ │
│  │  - Exemption handling (gamemode, permission)                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │
│  │   Alert        │  │    Logging      │  │    Config           │  │
│  │   System       │  │    System       │  │    System           │  │
│  ├────────────────┤  ├─────────────────┤  ├─────────────────────┤  │
│  │ Staff alerts   │  │ File logging    │  │ Per-check settings  │  │
│  │ Console output │  │ DB logging      │  │ VL thresholds       │  │
│  │ Webhook support│  │ Audit trail     │  │ Action definitions  │  │
│  └────────────────┘  └─────────────────┘  └─────────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                      Admin Interface                            │ │
│  │  - /ac alerts [on/off]       - Toggle staff alerts             │ │
│  │  - /ac check <player>        - View player's violations        │ │
│  │  - /ac violations            - View recent violations          │ │
│  │  - /ac toggle <check>        - Enable/disable checks           │ │
│  │  - /ac reload                - Reload configuration            │ │
│  │  - /ac exempt <player>       - Temporarily exempt player       │ │
│  │  - /ac debug <player>        - Debug mode for player           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Player Data Model

```java
public class ACPlayerData {

    // Identity
    private final UUID uuid;
    private final PlayerRef playerRef;

    // Position tracking
    private Vector3d lastPosition;
    private Vector3d lastVelocity;
    private long lastPositionTime;
    private Queue<Vector3d> positionHistory;  // Last N positions

    // Movement state
    private boolean wasOnGround;
    private double fallDistance;
    private int airTicks;
    private int groundTicks;

    // Combat tracking
    private long lastAttackTime;
    private int recentClicks;
    private Queue<Long> clickHistory;  // For CPS calculation
    private Ref<EntityStore> lastTarget;
    private Queue<Ref<EntityStore>> targetHistory;  // For aura detection

    // Block interaction
    private Vector3i lastBreakPosition;
    private long lastBreakTime;
    private int breakingTicks;

    // Violation tracking
    private Map<String, Double> violationLevels;  // Check name -> VL
    private Map<String, Long> lastViolationTime;

    // Exemptions
    private Set<String> exemptChecks;
    private long exemptUntil;

    // Methods
    public void update(float dt) {
        decayViolationLevels(dt);
    }

    public void addViolation(String check, double amount) {
        double current = violationLevels.getOrDefault(check, 0.0);
        violationLevels.put(check, current + amount);
        lastViolationTime.put(check, System.currentTimeMillis());
    }

    public double getVL(String check) {
        return violationLevels.getOrDefault(check, 0.0);
    }

    private void decayViolationLevels(float dt) {
        // Decay VL over time
        for (Map.Entry<String, Double> entry : violationLevels.entrySet()) {
            double decayed = entry.getValue() - (DECAY_RATE * dt);
            if (decayed <= 0) {
                violationLevels.remove(entry.getKey());
            } else {
                entry.setValue(decayed);
            }
        }
    }
}
```

### Check Base Class

```java
public abstract class Check {

    protected final String name;
    protected final CheckType type;
    protected boolean enabled;
    protected double vlDecayRate;
    protected Map<Integer, CheckAction> actions;  // VL threshold -> action

    public Check(String name, CheckType type) {
        this.name = name;
        this.type = type;
        this.enabled = true;
        this.vlDecayRate = 1.0;
        this.actions = new HashMap<>();
    }

    public abstract void check(ACPlayerData data, Object... context);

    protected void flag(ACPlayerData data, double vl, String details) {
        if (!isExempt(data)) {
            data.addViolation(name, vl);
            processActions(data, details);
            notifyStaff(data, vl, details);
        }
    }

    protected boolean isExempt(ACPlayerData data) {
        // Check bypass permissions
        if (data.getPlayerRef().hasPermission("anticheat.bypass." + name.toLowerCase())) {
            return true;
        }
        // Check gamemode
        if (data.getGameMode() == GameMode.Creative) {
            return true;
        }
        // Check temporary exemption
        if (data.isExemptFrom(name)) {
            return true;
        }
        return false;
    }

    private void processActions(ACPlayerData data, String details) {
        double vl = data.getVL(name);
        for (Map.Entry<Integer, CheckAction> entry : actions.entrySet()) {
            if (vl >= entry.getKey()) {
                entry.getValue().execute(data, name, vl, details);
            }
        }
    }
}
```

### Recommended MVP Implementation Order

#### Phase 1: Foundation
1. Plugin structure and lifecycle
2. Player data tracking system
3. Configuration system
4. Violation manager
5. Basic command interface

#### Phase 2: Movement Checks
1. **SpeedCheck** - Easiest starting point
   - Compare position delta per tick vs max expected
   - Account for sprint, effects, etc.

2. **FlyCheck**
   - Track `airTicks` when not `onGround`
   - Validate against expected gravity

3. **NoFallCheck**
   - Compare `currentFallDistance` vs damage taken
   - Detect when client reports landing without damage

#### Phase 3: Combat Checks
1. **ReachCheck**
   - Calculate distance between attacker and target
   - Use `HitDetectionExecutor` logic

2. **HitRateCheck**
   - Track clicks per second
   - Flag impossibly high CPS

#### Phase 4: World Checks
1. **FastBreakCheck**
   - Track time between `DamageBlockEvent` and `BreakBlockEvent`
   - Compare against expected break time for block/tool

2. **ScaffoldCheck**
   - Detect placement while moving backward
   - Track placement rate while bridging

#### Phase 5: Advanced Checks
1. **KillauraCheck** - Pattern analysis
2. **XrayCheck** - Statistical analysis
3. **VelocityCheck** - Dual velocity comparison

### Configuration Example

```json
{
  "general": {
    "alertsEnabled": true,
    "logToFile": true,
    "logToDatabase": false,
    "debugMode": false
  },

  "checks": {
    "speed": {
      "enabled": true,
      "maxSpeed": 0.7,
      "sprintMultiplier": 1.3,
      "vlDecayRate": 0.5,
      "actions": {
        "20": "WARN",
        "50": "KICK",
        "100": "TEMPBAN:1h"
      }
    },

    "fly": {
      "enabled": true,
      "maxAirTicks": 40,
      "vlDecayRate": 0.3,
      "actions": {
        "30": "WARN",
        "60": "KICK",
        "120": "BAN"
      }
    },

    "reach": {
      "enabled": true,
      "maxReach": 4.5,
      "combatReach": 3.0,
      "vlDecayRate": 0.4,
      "actions": {
        "15": "WARN",
        "40": "KICK"
      }
    },

    "fastbreak": {
      "enabled": true,
      "tolerance": 0.9,
      "vlDecayRate": 0.6,
      "actions": {
        "10": "CANCEL",
        "30": "WARN",
        "60": "KICK"
      }
    }
  },

  "exemptions": {
    "gamemodes": ["Creative", "Spectator"],
    "permissions": ["anticheat.bypass"]
  }
}
```

---

## Code References & Examples

### Speed Check Implementation

```java
public class SpeedCheck extends Check {

    private static final double BASE_SPEED = 0.1;  // Blocks per tick walking
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double SNEAK_MULTIPLIER = 0.3;

    public SpeedCheck() {
        super("Speed", CheckType.MOVEMENT);
    }

    @Override
    public void check(ACPlayerData data, Object... context) {
        PlayerRef player = data.getPlayerRef();

        // Get current position from TransformComponent
        TransformComponent transform = getComponent(player, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d currentPos = transform.getPosition();
        Vector3d lastPos = data.getLastPosition();

        if (lastPos == null) {
            data.setLastPosition(currentPos);
            return;
        }

        // Calculate horizontal distance (ignore Y for basic speed check)
        double dx = currentPos.x - lastPos.x;
        double dz = currentPos.z - lastPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Get movement states
        MovementStatesComponent states = getComponent(player, MovementStatesComponent.getComponentType());
        MovementStates ms = states.getMovementStates();

        // Calculate expected max speed
        double maxSpeed = BASE_SPEED;

        if (ms.sprinting) {
            maxSpeed *= SPRINT_MULTIPLIER;
        }
        if (ms.crouching) {
            maxSpeed *= SNEAK_MULTIPLIER;
        }

        // TODO: Account for effects (speed potion, etc.)
        // TODO: Account for ice, slime blocks, etc.

        // Add tolerance for latency
        maxSpeed *= 1.1;

        // Check
        if (horizontalDistance > maxSpeed) {
            double vl = (horizontalDistance - maxSpeed) * 10;
            flag(data, vl, String.format(
                "dist=%.3f, max=%.3f, sprint=%b, sneak=%b",
                horizontalDistance, maxSpeed, ms.sprinting, ms.crouching
            ));
        }

        data.setLastPosition(currentPos);
    }
}
```

### Reach Check Implementation

```java
public class ReachCheck extends Check {

    private static final double MAX_REACH = 4.5;  // Survival reach
    private static final double CREATIVE_REACH = 5.0;

    public ReachCheck() {
        super("Reach", CheckType.COMBAT);
    }

    public void onAttack(ACPlayerData attacker, Ref<EntityStore> targetRef, Vector4d hitLocation) {
        // Get attacker position
        TransformComponent attackerTransform = getComponent(
            attacker.getPlayerRef(),
            TransformComponent.getComponentType()
        );
        if (attackerTransform == null) return;

        // Get target position
        TransformComponent targetTransform = getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) return;

        Vector3d attackerPos = attackerTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();

        // Calculate distance
        double dx = attackerPos.x - targetPos.x;
        double dy = attackerPos.y - targetPos.y;
        double dz = attackerPos.z - targetPos.z;
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Determine max reach based on gamemode
        double maxReach = attacker.getGameMode() == GameMode.Creative
            ? CREATIVE_REACH
            : MAX_REACH;

        // Add small tolerance for latency
        maxReach += 0.5;

        // Check
        if (distance > maxReach) {
            double vl = (distance - maxReach) * 5;
            flag(attacker, vl, String.format(
                "distance=%.2f, max=%.2f, target=%s",
                distance, maxReach, targetRef.toString()
            ));
        }
    }
}
```

### FastBreak Check Implementation

```java
public class FastBreakCheck extends Check {

    public FastBreakCheck() {
        super("FastBreak", CheckType.WORLD);
    }

    public void onBreakBlock(BreakBlockEvent event, ACPlayerData data) {
        Vector3i blockPos = event.getTargetBlock();
        BlockType blockType = event.getBlockType();
        ItemStack tool = event.getItemInHand();

        // Calculate expected break time
        float hardness = blockType.getHardness();
        float toolMultiplier = getToolMultiplier(tool, blockType);

        // Base break time in ticks (20 ticks = 1 second)
        float expectedTicks = (hardness * 30) / toolMultiplier;

        // Get actual break time
        long breakTime = System.currentTimeMillis() - data.getBreakStartTime();
        float actualTicks = breakTime / 50.0f;  // 50ms per tick

        // Tolerance factor
        float tolerance = 0.9f;  // Allow 90% of expected time

        if (actualTicks < expectedTicks * tolerance) {
            double vl = ((expectedTicks * tolerance) - actualTicks) / expectedTicks * 10;

            flag(data, vl, String.format(
                "block=%s, expected=%.1f ticks, actual=%.1f ticks, tool=%s",
                blockType.getId(), expectedTicks, actualTicks,
                tool != null ? tool.getItem().getId() : "none"
            ));

            // Cancel the break
            event.setCancelled(true);
        }
    }

    private float getToolMultiplier(ItemStack tool, BlockType blockType) {
        if (tool == null) return 1.0f;

        // TODO: Implement proper tool effectiveness calculation
        // This would check if the tool is correct for the block type
        // and apply efficiency enchantments, etc.

        return 1.0f;
    }
}
```

---

## Appendix: Key Classes Reference

### Entity System

| Class | Location | Purpose |
|-------|----------|---------|
| `Player` | `server.core.entity.entities.Player` | Player entity component |
| `LivingEntity` | `server.core.entity.LivingEntity` | Base for living entities |
| `Entity` | `server.core.entity.Entity` | Base entity class |
| `PlayerRef` | `server.core.universe.PlayerRef` | Player reference with utilities |
| `TransformComponent` | `server.core.modules.entity.component.TransformComponent` | Position, rotation |
| `Velocity` | `server.core.modules.physics.component.Velocity` | Velocity tracking |
| `BoundingBox` | `server.core.modules.entity.component.BoundingBox` | Collision bounds |
| `MovementStatesComponent` | `server.core.entity.movement.MovementStatesComponent` | Movement flags |

### Movement System

| Class | Location | Purpose |
|-------|----------|---------|
| `PlayerProcessMovementSystem` | `server.core.modules.entity.player` | Main movement processor |
| `PlayerInput` | `server.core.modules.entity.player` | Client input handling |
| `PlayerVelocityInstructionSystem` | `server.core.universe.system` | Velocity application |
| `CollisionModule` | `server.core.modules.collision` | Collision detection |
| `CollisionResultComponent` | `server.core.modules.entity.component` | Collision results |

### Damage System

| Class | Location | Purpose |
|-------|----------|---------|
| `Damage` | `server.core.modules.entity.damage` | Damage event data |
| `DamageSystems` | `server.core.modules.entity.damage` | Damage processing |
| `DamageModule` | `server.core.modules.entity.damage` | Module registration |
| `DamageCause` | `server.core.modules.entity.damage` | Damage type enum |
| `KnockbackComponent` | `server.core.entity.knockback` | Knockback data |

### Hit Detection

| Class | Location | Purpose |
|-------|----------|---------|
| `HitDetectionExecutor` | `math.hitdetection` | Ray-based hit testing |
| `HitDetectionBuffer` | `math.hitdetection` | Hit test buffers |
| `HitboxCollision` | `server.core.modules.entity.hitboxcollision` | Entity hitboxes |
| `LineOfSightProvider` | `math.hitdetection` | LOS checking interface |

### Protocol

| Class | Location | Purpose |
|-------|----------|---------|
| `MouseInteraction` | `protocol.packets.player` | Combat/interaction packet |
| `ChangeVelocity` | `protocol.packets.entities` | Velocity update packet |
| `Packet` | `protocol` | Base packet interface |
| `PacketRegistry` | `protocol` | Packet type registry |

### Events

| Class | Location | Purpose |
|-------|----------|---------|
| `BreakBlockEvent` | `server.core.event.events.ecs` | Block break event |
| `PlaceBlockEvent` | `server.core.event.events.ecs` | Block place event |
| `PlayerConnectEvent` | `server.core.event.events.player` | Player join |
| `PlayerChatEvent` | `server.core.event.events.player` | Chat message |
| `EventRegistry` | `server.core.plugin.event` | Event registration |

### Plugin API

| Class | Location | Purpose |
|-------|----------|---------|
| `JavaPlugin` | `server.core.plugin` | Plugin base class |
| `CommandRegistry` | `server.core.plugin` | Command registration |
| `TaskRegistry` | `server.core.plugin` | Task scheduling |
| `PermissionHolder` | `server.core.permissions` | Permission interface |

---

## Conclusion

The Hytale server provides an excellent foundation for a comprehensive anti-cheat system. The key advantages are:

1. **Rich Event System** - Clean hooks without patching
2. **Dual Velocity Tracking** - Server vs client comparison
3. **Ray-Based Hit Detection** - Built-in reach validation
4. **ECS Architecture** - Efficient entity queries
5. **Built-in Validation** - Some anti-cheat exists to extend

The recommended approach is to start with simple movement checks (Speed, Fly) and progressively add more sophisticated detection as the plugin matures.

---

*Document generated from decompiled HytaleServer.jar analysis*
*For HyperAntiCheat plugin development*
