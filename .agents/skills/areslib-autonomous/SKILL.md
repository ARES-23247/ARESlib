---
name: areslib-autonomous
description: Helps build autonomous routines, path following commands, ghost replay systems, and dynamic avoidance in ARESLib2. Use when creating autonomous OpModes, building path sequences, or implementing shoot-on-the-move.
---

You are an expert autonomous systems engineer for Team ARES. When building autonomous routines, path following commands, or replay systems for ARESLib2, adhere strictly to the following guidelines.

> **Cross-Reference**: For PathPlanner-specific configuration (dummy shim layer, AutoBuilder setup, FTC coordinate offsets), see the `pathplanner` skill.

## 1. Architecture Overview

| Class | Location | Purpose |
|:---|:---|:---|
| `AutoBuilder` | `command.auto` | Builds complete autonomous sequences from PathPlanner paths |
| `DynamicPathCommand` | `command.auto` | On-the-fly pathfinding with obstacle avoidance |
| `FollowPathCommand` | `command` | WPILib Command wrapper around path following |
| `GhostRecorder` | `core` | Records driver inputs during teleop for autonomous replay |
| `GhostPlaybackCommand` | `command` | Replays recorded ghost data as an autonomous routine |
| `ShootOnTheMoveCommand` | `command.autoaim` | Kinematic aiming while robot is in motion |
| `AlignToPoseCommand` | `command` | PID-based alignment to a target field pose |
| `DynamicAvoidanceAuto` | `examples.auto` | Example autonomous that avoids Obelisk dynamically |

## 2. Key Rules

### Rule A: Use PathPlanner Commands, Not Raw Loops
Never write `while(!follower.isBusy())` loops. Use PathPlanner's built-in command wrappers:
```java
// Option 1: Schedule a full auto routine by name
CommandScheduler.getInstance().schedule(new PathPlannerAuto("SquareAuto"));

// Option 2: Follow a single path inline
PathPlannerPath path = PathPlannerPath.fromPathFile("MyPath");
Command followCmd = AutoBuilder.followPath(path);
CommandScheduler.getInstance().schedule(followCmd);
```

### Rule B: Coordinate System
ARESLib2 uses **WPILib convention** everywhere (X-forward, Y-left, theta CCW+). PathPlanner also uses WPILib convention natively, so no coordinate conversion is needed between them.

All conversions between internal frames are handled by `CoordinateUtil`. See the `areslib-architecture` skill for the full coordinate guide.

### Rule C: GhostRecorder Safety
`GhostRecorder` records robot ChassisSpeeds and binary button inputs to a CSV file using a **lock-free background writer thread**, ensuring zero main-loop blocking from file I/O. During replay, `GhostPlaybackCommand` reads the CSV and feeds the speeds and buttons back to their consumers deterministically.

**Critical:** Ensure that `GhostRecorder` and `GhostPlaybackCommand` are mutually exclusive.

```java
// Initialization (RobotContainer):
GhostRecorder recorder = new GhostRecorder(
    () -> driveSubsystem.getSpeeds(), 
    () -> driverGamepad.rightTrigger() > 0.5 // example varargs boolean supplier
);

// Recording (in TeleOp):
// recorder.update() MUST be called in periodic!
recorder.startRecording("/sdcard/FIRST/macros/auto1.csv");

// Playback (in Auto):
Command playbackCmd = new GhostPlaybackCommand(
    "/sdcard/FIRST/macros/auto1.csv",
    recorder,
    speeds -> driveSubsystem.drive(speeds),
    isShooting -> shooter.setShooting(isShooting) // corresponds to rightTrigger
);
playbackCmd.schedule();
```
### Rule D: Dynamic Avoidance Uses ObstacleAvoider
The `ObstacleAvoider` class in `math.pathing` generates waypoints that route around known field obstacles:
```java
ObstacleAvoider avoider = new ObstacleAvoider();
avoider.addObstacle(FieldConstants.OBELISK_CENTER, FieldConstants.OBELISK_RADIUS);
List<Point> waypoints = avoider.findPath(startPose, endPose);
```

## 3. Building Autonomous Sequences

Use `SequentialCommandGroup` to chain path segments with actions:
```java
new SequentialCommandGroup(
    new FollowPathCommand(follower, pathToScoring),
    new InstantCommand(() -> scorer.score()),
    new WaitCommand(0.5),
    new FollowPathCommand(follower, pathToIntake),
    new InstantCommand(() -> intake.startIntake()),
    new WaitCommand(1.0),
    new FollowPathCommand(follower, pathBackToScoring)
);
```

## 4. ShootOnTheMoveCommand

`ShootOnTheMoveCommand` uses `KinematicAiming` to compute intercept angles while the robot is translating:
```java
KinematicAiming aimer = new KinematicAiming(
    targetPose,           // Where to aim
    projectileVelocity,   // meters/second
    robotVelocity         // current ChassisSpeeds
);
Rotation2d aimAngle = aimer.getCompensatedHeading();
```
The command continuously updates the heading lock while the path follower handles translation. Requires both the drive subsystem and the shooter subsystem.

## 5. Telemetry & Log Keys

| Key | Type | Description |
|:---|:---|:---|
| `Auto/ActivePath` | `String` | Name of the currently executing path |
| `Auto/FollowerBusy` | `boolean` | Whether the follower is actively tracking |
| `Auto/TargetPose` | `Pose2d` | Current path target pose |
| `Auto/GhostRecording` | `boolean` | Whether ghost recording is active |
| `Auto/AimAngle` | `double` | Shoot-on-the-move compensated heading (rad) |

## 6. Testing

Autonomous tests should use the full physics simulation, not mocks:
```java
@Test
void testFollowPathCommandCompletes() {
    AresPhysicsWorld.getInstance().reset();
    CommandScheduler.getInstance().cancelAll();
    
    // Build path
    PathPlannerPath path = PathPlannerPath.fromPathFile("ScorePath");
    
    FollowPathCommand cmd = new FollowPathCommand(follower, path);
    CommandScheduler.getInstance().schedule(cmd);
    
    // Run 150 ticks (3 seconds)
    for (int i = 0; i < 150; i++) {
        CommandScheduler.getInstance().run();
        AresPhysicsWorld.getInstance().update(0.02);
    }
    
    assertFalse(CommandScheduler.getInstance().isScheduled(cmd),
        "Path should complete within 3 seconds");
}
```

For full test patterns, see the `areslib-testing` skill.

## 7. Common Pitfalls

### Don't: Forget addRequirements on path commands
```java
// BAD — drive can be interrupted mid-path
public FollowPathCommand(AresFollower f, PathPlannerPath p) {
    this.follower = f; // No requirements!
}

// GOOD — exclusive access during path following
public FollowPathCommand(AresFollower f, PathPlannerPath p) {
    this.follower = f;
    addRequirements(f);
}
```
