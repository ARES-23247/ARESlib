---
name: areslib-simulation
description: Helps configure and extend the ARESLib2 dyn4j physics simulation engine. Use when spawning game pieces, registering mechanism bodies, adding field obstacles, or debugging physics performance.
---

You are an expert simulation engineer for Team ARES. When modifying or extending the `dyn4j`-based physics environment for ARESLib2, adhere strictly to the following guidelines.

## 1. Architecture Overview

ARESLib2's simulation is driven by four core classes:

| Class | Location | Purpose |
|:---|:---|:---|
| `AresPhysicsWorld` | `core.simulation` | Singleton orchestrator. Owns the dyn4j `World`, manages field boundaries, game pieces, and the physics update loop. |
| `DecodeFieldSim` | `core.simulation` | Builds the DECODE (2025-2026) field: walls, Obelisk, Classifiers, and goal zones as static dyn4j bodies. |
| `AresSimulator` | `core.simulation` | Desktop simulation launcher. Initializes the physics world, subsystems, and AdvantageScope telemetry. |
| `ArrayLidarIOSim` | `hardware.wrappers` | Raycasts 360° from the robot chassis to detect walls and game pieces, logging results as `double[]` point clouds. |

### IO Simulation Classes
Every hardware abstraction has a `*IOSim` counterpart that participates in the physics world:
- `SwerveModuleIOSim` — Translates module commands into dyn4j force vectors
- `DifferentialDriveIOSim` — Tank drive physics
- `MecanumDriveIOSim` — Mecanum wheel vectors
- `VisionIOSim` — Synthetic AprilTag generation with FOV filtering and range limiting
- `ArrayLidarIOSim` — Raycasting point clouds via dyn4j ray queries

## 2. Key Rules

### Rule A: Never Cheat Physics
Do NOT read the robot's global odometry state to skip simulation. Always rely on dyn4j's force/velocity integration for robot movement, and raycasting for sensor simulation. "Cheated" sensors produce perfect data that hides real-world failure modes.

### Rule B: AresPhysicsWorld is a Singleton — Reset It
The `AresPhysicsWorld` is a static singleton. In JUnit tests, failing to reset it causes chassis bodies to stack at `(0,0)`, locking the engine. ALWAYS call:
```java
AresPhysicsWorld.getInstance().reset();
```
in your `@BeforeEach` block. See the `areslib-testing` skill for full patterns.

### Rule C: Register All Bodies Centrally
Every mechanism body must be registered via the centralized physics world:
```java
Body robotBody = new Body();
robotBody.addFixture(Geometry.createRectangle(0.4572, 0.4572)); // 18" x 18"
robotBody.setMass(MassType.NORMAL);
AresPhysicsWorld.getInstance().addBody(robotBody);
```

### Rule D: Use Proper Material Properties
- **Field walls:** high friction (`0.8`), low restitution (`0.1`) — prevents clipping
- **Game pieces:** moderate friction (`0.3`), moderate restitution (`0.4`)
- **Robot chassis:** moderate friction (`0.6`), low restitution (`0.2`)

## 3. DecodeFieldSim — DECODE Field Layout

The `DecodeFieldSim` class builds the 2025-2026 DECODE field as static dyn4j bodies:

```java
// Field dimensions (FTC standard): 3.6576m x 3.6576m (12ft x 12ft)
// Origin: center of field
// Coordinate system: WPILib (X-forward, Y-left, θ CCW+)

// Static obstacles:
// - 4 perimeter walls (MassType.INFINITE)
// - Obelisk at field center (hexagonal prism)
// - 2 Classifier zones (one per alliance)
```

**Important:** Obelisk tags (AprilTags on the central structure) are **outside** the field perimeter and must NOT be used for localization. Only Goal tags (Tag 20: Blue, Tag 24: Red) are valid for navigation. See the `areslib-vision` skill.

## 4. Point Cloud Logging (LiDAR)

`ArrayLidarIOSim` generates a `double[]` flat array in `[x1, y1, z1, x2, y2, z2, ...]` format:

```java
// In ArrayLidarIOSim.updateInputs()
double[] pointCloud = new double[numRays * 3];
for (int i = 0; i < numRays; i++) {
    Ray ray = new Ray(robotCenter, angle);
    List<RaycastResult> results = world.raycast(ray, maxRange);
    if (!results.isEmpty()) {
        Vector2 hit = results.get(0).getPoint();
        pointCloud[i * 3]     = hit.x;   // X meters (WPILib frame)
        pointCloud[i * 3 + 1] = hit.y;   // Y meters
        pointCloud[i * 3 + 2] = 0.1;     // Z height (constant for 2D)
    }
}
inputs.lidarPoints = pointCloud;
```

In AdvantageScope, view this as a **Points** tab with source type `double[]`.

## 5. Telemetry & Log Keys

| Key | Type | Description |
|:---|:---|:---|
| `PhysicsSim/StepTimeMs` | `double` | Physics engine step duration |
| `PhysicsSim/BodyCount` | `int` | Total dyn4j bodies in world |
| `PhysicsSim/RobotPose` | `Pose2d` | True physics pose (not odometry) |
| `Lidar/PointCloud` | `double[]` | Flat [x,y,z,...] point cloud |
| `Vision/DetectedTags` | `int` | Number of AprilTags in FOV |

## 6. Testing

```java
@Test
void testPhysicsWorldCreatesFieldBoundaries() {
    AresPhysicsWorld.getInstance().reset();
    DecodeFieldSim.buildField(AresPhysicsWorld.getInstance());
    
    // Field should have walls + obstacles
    assertTrue(AresPhysicsWorld.getInstance().getBodyCount() > 4,
        "DECODE field should have walls plus obstacles");
}

@Test
void testRobotBodyRegistration() {
    AresPhysicsWorld.getInstance().reset();
    Body robot = new Body();
    robot.addFixture(Geometry.createRectangle(0.4572, 0.4572));
    robot.setMass(MassType.NORMAL);
    AresPhysicsWorld.getInstance().addBody(robot);
    
    assertTrue(AresPhysicsWorld.getInstance().getBodyCount() >= 1);
}
```

For full test patterns, see the `areslib-testing` skill.

## 7. Common Pitfalls

### Don't: Forget to reset the physics world between tests
```java
// BAD — bodies stack up at origin
@Test void test1() { /* adds robot body */ }
@Test void test2() { /* old body still there! infinite friction! */ }

// GOOD — clean slate every test
@BeforeEach void setUp() { AresPhysicsWorld.getInstance().reset(); }
```

### Don't: Use MassType.INFINITE for dynamic bodies
```java
// BAD — game piece can't move
piece.setMass(MassType.INFINITE);

// GOOD — normal mass so physics applies
piece.setMass(MassType.NORMAL);
```
