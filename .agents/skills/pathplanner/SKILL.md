---
name: PathPlanner Standard Requirements
description: Documents the ARESLib2 PathPlanner integration including dummy class requirements, AutoBuilder configuration, and simulation parity. Use when creating autonomous routines, fixing PathPlanner crashes, or extending the WPILib dummy shim layer.
---

You are an autonomous systems engineer for Team ARES. When working with PathPlanner inside the ARESLib2 framework, adhere strictly to the following guidelines.

## 1. Architecture

PathPlanner was ported from the official FRC repository and adapted for FTC. It depends on a **dummy shim layer** that replaces WPILib classes not available on Android.

```
org.areslib.pathplanner.auto.AutoBuilder        ← Static configuration, builds auto commands
org.areslib.pathplanner.commands.PathPlannerAuto ← Top-level auto Command (wraps buildAuto)
org.areslib.pathplanner.commands.FollowPathCommand     ← Base path follower
org.areslib.pathplanner.commands.FollowPathHolonomic   ← Holonomic specialization
org.areslib.pathplanner.controllers.PPHolonomicDriveController ← PID controller
org.areslib.pathplanner.path.PathPlannerPath     ← Path geometry from JSON
org.areslib.pathplanner.path.PathPlannerTrajectory ← Time-parameterized trajectory
org.areslib.pathplanner.auto.CommandUtil         ← JSON → Command deserializer
org.areslib.pathplanner.dummy.*                  ← WPILib shims (Timer, Commands, etc.)
```

### Critical: The Dummy Shim Layer

PathPlanner references WPILib classes (`Timer`, `Commands`, `FunctionalCommand`, `DriverStation`, `Filesystem`). These are replaced by dummy implementations in `org.areslib.pathplanner.dummy`. **Every dummy class MUST return functional objects, never null.**

## 2. Key Rules

### Rule A: Official FRC Repository Only
When pulling PathPlanner code or documentation, ONLY use the official FRC PathPlanner repository: `https://github.com/mjansen4857/pathplanner`. Do NOT use any unofficial FTC ports like `pathplanner-ftc`.

### Rule B: No Pedro Pathing
All logic relating to Pedro Pathing is officially deprecated. PathPlanner is the singular trajectory solution. Remove references to `PedroFollower`, `Pedro/Odometry`, and related classes.

### Rule C: Java 17 Compilation
PathPlannerLib leverages Java 17 constructs (`switch` expressions, `var`). Ensure the project `build.gradle` includes the JDK 17 toolchain definition.

### Rule D: Dummy Classes Must Never Return Null
Every method in the `org.areslib.pathplanner.dummy` package that returns a `Command` MUST return a functioning command instance. Returning `null` causes `NullPointerException` when `SequentialCommandGroup.addCommands()` calls `getRequirements()` on the null reference.

```java
// BAD — causes NPE in AutoBuilder.getAutoCommandFromJson()
public static Command runOnce(Runnable r) { return null; }

// GOOD — returns a real InstantCommand
public static Command runOnce(Runnable r) { return new InstantCommand(r); }
```

### Rule E: Timer Must Track Real Time
The `Timer` dummy MUST use `System.nanoTime()` for real elapsed time tracking. `FollowPathCommand` samples trajectory states by `timer.get()` — if it always returns 0, the robot never progresses through the path and never finishes.

```java
// BAD — robot never moves, path never finishes
public double get() { return 0; }
public boolean hasElapsed(double s) { return false; }

// GOOD — real elapsed time
public double get() {
    if (running) return (accumulated + (System.nanoTime() - startTime)) / 1e9;
    return accumulated / 1e9;
}
```

### Rule F: Single AutoBuilder Configuration
`AutoBuilder.configureHolonomic()` MUST be called exactly once. It sets a static `configured` flag. The canonical location is `RobotContainer.initPathPlanner()`. Do NOT duplicate this call in `DesktopSimLauncher`.

### Rule G: FTC Coordinate Offset
FTC uses center-origin coordinates (0,0 = field center). PathPlanner uses corner-origin. The offset is `1.8288m` (half of 3.6576m / 12ft field). This offset is applied in the pose supplier and reset consumer:

```java
double ftcOffset = 1.8288;
AutoBuilder.configureHolonomic(
    () -> new Pose2d(inputs.xMeters + ftcOffset, inputs.yMeters + ftcOffset, ...),
    (pose) -> { inputs.xMeters = pose.getX() - ftcOffset; ... },
    ...
);
```

### Rule H: PathPlannerAuto Must Have Crash Protection
`PathPlannerAuto` constructor calls `AutoBuilder.buildAuto()` which can throw `RuntimeException` for missing auto files, unparseable JSON, or null command builders. Always wrap in try-catch:

```java
try {
    m_autoCommand = AutoBuilder.buildAuto(autoName);
} catch (Exception e) {
    System.err.println("Failed to build auto: " + e.getMessage());
    m_autoCommand = new InstantCommand(); // no-op fallback
}
```

## 3. Configuration & Constants

### HolonomicPathFollowerConfig

```java
new HolonomicPathFollowerConfig(
    new PIDConstants(5.0, 0.0, 0.0),   // Translation PID
    new PIDConstants(5.0, 0.0, 0.0),   // Rotation PID
    5.0,                                // Max module speed (m/s)
    0.5,                                // Drive base radius (m)
    new ReplanningConfig()              // Default: initial replanning ON, dynamic OFF
);
```

### Path Constraints (FTC-appropriate)

For FTC robots, use conservative velocity and acceleration:

| Parameter | Recommended | Too Aggressive |
|:---|:---|:---|
| maxVelocity | 1.0–2.0 m/s | 3.0+ m/s |
| maxAcceleration | 1.0–2.0 m/s² | 3.0+ m/s² |
| maxAngularVelocity | 360 deg/s | 540+ deg/s |
| maxAngularAcceleration | 540 deg/s² | 720+ deg/s² |

### Path File Location

All `.path` and `.auto` files must be in:
```
src/main/deploy/pathplanner/paths/*.path
src/main/deploy/pathplanner/autos/*.auto
```

`Filesystem.getDeployDirectory()` resolves to `src/main/deploy` on desktop.

## 4. Usage Examples

### Creating an Auto Command
```java
// In DesktopSimLauncher or OpMode:
try {
    CommandScheduler.getInstance().schedule(new PathPlannerAuto("SquareAuto"));
} catch (Exception e) {
    System.err.println("[Sim] Auto failed: " + e.getMessage());
}
```

### Auto File Structure (`.auto`)
```json
{
  "version": 1.0,
  "startingPose": { "position": {"x": 1.8288, "y": 1.8288}, "rotation": 0.0 },
  "command": {
    "type": "sequential",
    "data": {
      "commands": [
        { "type": "path", "data": { "pathName": "SquarePath" } }
      ]
    }
  },
  "choreoAuto": false
}
```

## 5. Telemetry & Log Keys

| Key | Type | Source |
|:----|:-----|:-------|
| `PathPlanner/EstimatedPose` | Pose2d | Sim loop — PP-space pose with FTC offset |
| `Auto/CommandedVx` | Number | Commanded robot-relative forward velocity |
| `Auto/CommandedVy` | Number | Commanded robot-relative strafe velocity |
| `Auto/CommandedOmega` | Number | Commanded angular velocity |

PathPlanner internally logs via `PathPlannerLogging` and `PPLibTelemetry`, but these require wiring to `AresTelemetry` for AdvantageScope visibility.

## 6. Testing

Autonomous path following can be validated headlessly using the `areslib-testing` patterns:

```java
@Test
void testAutoBuilderConfigured() {
    AresRobot.setSimulation(true);
    RobotContainer container = new RobotContainer(null, null, null);
    
    // Verify auto builds without exception
    assertDoesNotThrow(() -> AutoBuilder.buildAuto("SquareAuto"));
}

@Test
void testDummyTimerTracksTime() throws InterruptedException {
    Timer timer = new Timer();
    timer.reset();
    timer.start();
    Thread.sleep(100);
    assertTrue(timer.get() > 0.05, "Timer should track real elapsed time");
    assertTrue(timer.hasElapsed(0.05), "hasElapsed should return true after delay");
}

@Test
void testDummyCommandsNonNull() {
    assertNotNull(Commands.runOnce(() -> {}), "runOnce must not return null");
    assertNotNull(Commands.none(), "none must not return null");
    assertNotNull(Commands.waitSeconds(1.0), "waitSeconds must not return null");
    assertNotNull(Commands.print("test"), "print must not return null");
}
```

For testing patterns, see the `areslib-testing` skill.

## 7. Common Pitfalls

### Pitfall 1: Dummy Commands Return Null

```java
// BAD — causes NPE in SequentialCommandGroup.addCommands()
public static Command runOnce(Runnable r) { return null; }
public static Command none() { return null; }
public static Command waitSeconds(double s) { return null; }

// GOOD — returns functional Command instances
public static Command runOnce(Runnable r) { return new InstantCommand(r); }
public static Command none() { return new InstantCommand(() -> {}); }
public static Command waitSeconds(double s) {
    return new Command() {
        private long startTime;
        @Override public void initialize() { startTime = System.currentTimeMillis(); }
        @Override public boolean isFinished() {
            return (System.currentTimeMillis() - startTime) >= (s * 1000.0);
        }
    };
}
```

### Pitfall 2: Timer Frozen at Zero

```java
// BAD — robot never moves, path never finishes
public double get() { return 0; }
public boolean hasElapsed(double s) { return false; }

// GOOD — real elapsed time using System.nanoTime()
public double get() {
    if (running) return (accumulated + (System.nanoTime() - startTime)) / 1e9;
    return accumulated / 1e9;
}
public boolean hasElapsed(double s) { return get() >= s; }
```

### Pitfall 3: Duplicate AutoBuilder Configuration

```java
// BAD — called in BOTH DesktopSimLauncher AND RobotContainer
// DesktopSimLauncher.java:
AutoBuilder.configureHolonomic(...);
// RobotContainer.java:
AutoBuilder.configureHolonomic(...); // triggers "already configured" warning

// GOOD — single source of truth in RobotContainer only
// RobotContainer.initPathPlanner():
AutoBuilder.configureHolonomic(...);
```

### Pitfall 4: FRC-Scale Path Constraints on FTC Field

```json
// BAD — FRC constraints on 3.66m FTC field cause overshoot and wall collisions
{ "maxVelocity": 3.0, "maxAcceleration": 3.0 }

// GOOD — FTC-appropriate constraints
{ "maxVelocity": 1.0, "maxAcceleration": 1.5 }
```

### Pitfall 5: No Crash Protection in Sim Loop

```java
// BAD — uncaught auto exception kills the sim thread and telemetry
CommandScheduler.getInstance().schedule(new PathPlannerAuto("Missing"));

// GOOD — graceful degradation
try {
    CommandScheduler.getInstance().schedule(new PathPlannerAuto("SquareAuto"));
} catch (Exception e) {
    System.err.println("[Sim] Auto failed: " + e.getMessage());
}
```

## 8. Cross-References

- For coordinate system details, see the `areslib-architecture` skill.
- For simulation environment, see the `areslib-simulation` skill.
- For drivetrain integration, see the `areslib-drivetrain` skill.
- For autonomous command patterns, see the `areslib-autonomous` skill.
- For testing patterns, see the `areslib-testing` skill.
