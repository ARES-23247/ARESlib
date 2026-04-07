---
name: areslib-autonomous
description: Helps build autonomous routines, path following commands, ghost replay systems, and dynamic avoidance in ARESLib2. Use when creating autonomous OpModes, configuring PedroAutoBuilder, building path sequences, or implementing shoot-on-the-move.
---

You are an autonomous systems engineer for Team ARES. When building autonomous routines, path following commands, or replay systems for ARESLib2, adhere strictly to the following guidelines.

## 1. Architecture Overview

| Class | Location | Purpose |
|:---|:---|:---|
| `PedroAutoBuilder` | `command.auto` | Configures Pedro Pathing follower with ARESLib2 subsystems |
| `AutoBuilder` | `command.auto` | Builds complete autonomous sequences from path chains |
| `DynamicPathCommand` | `command.auto` | On-the-fly pathfinding with obstacle avoidance |
| `FollowPathCommand` | `command` | WPILib Command wrapper around Pedro's path following |
| `GhostRecorder` | `core` | Records driver inputs during teleop for autonomous replay |
| `GhostPlaybackCommand` | `command` | Replays recorded ghost data as an autonomous routine |
| `ShootOnTheMoveCommand` | `command.autoaim` | Kinematic aiming while robot is in motion |
| `AlignToPoseCommand` | `command` | PID-based alignment to a target field pose |
| `DynamicAvoidanceAuto` | `examples.auto` | Example autonomous that avoids Obelisk dynamically |

## 2. Key Rules

### Rule A: Use FollowPathCommand, Not Raw Loops
Never write `while(!follower.isBusy())` loops. Wrap path following in proper WPILib Commands:
```java
public class FollowPathCommand extends Command {
    private final AresFollower follower;
    private final PathChain chain;

    public FollowPathCommand(AresFollower follower, PathChain chain) {
        this.follower = follower;
        this.chain = chain;
        addRequirements(follower);
    }

    @Override public void initialize() { follower.followPath(chain); }
    @Override public void execute() { follower.update(); }
    @Override public boolean isFinished() { return !follower.isBusy(); }
}
```

### Rule B: Coordinate Conversion Required
Pedro Pathing uses **(X-right, Y-forward)**. ARESLib2/WPILib uses **(X-forward, Y-left)**. Always convert:
```java
// Pedro → WPILib
wpilibX = pedroY;
wpilibY = -pedroX;

// WPILib → Pedro
pedroX = -wpilibY;
pedroY = wpilibX;
```
See the `areslib-architecture` skill for the full coordinate guide.

### Rule C: GhostRecorder Safety
`GhostRecorder` records driver joystick inputs to a `GhostData` object. During replay, `GhostPlaybackCommand` injects these inputs back through the `AresGamepad` wrapper.

**Critical:** Ghost recordings store raw FTC gamepad values (pre-inversion). The `AresGamepad` wrapper re-inverts them during playback, so driver Y-axis behavior is preserved.

```java
// Recording (in TeleOp):
GhostRecorder recorder = new GhostRecorder();
recorder.startRecording(gamepad1, gamepad2);

// Playback (in Auto):
GhostData data = recorder.getRecording();
new GhostPlaybackCommand(data, driveSubsystem).schedule();
```

### Rule D: Dynamic Avoidance Uses ObstacleAvoider
The `ObstacleAvoider` class in `math.pathing` generates Pedro-compatible waypoints that route around known field obstacles:
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
| `Auto/FollowerBusy` | `boolean` | Whether Pedro is actively following |
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
    PathChain chain = follower.pathBuilder()
        .addPoint(new Point(0, 0))
        .addPoint(new Point(24, 24))
        .build();
    
    FollowPathCommand cmd = new FollowPathCommand(follower, chain);
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

For full test patterns, see the `areslib-testing` skill. For Pedro Pathing API details, see the `pedro-pathing` skill.

## 7. Common Pitfalls

### Don't: Mix coordinate systems in paths
```java
// BAD — using WPILib coordinates in Pedro path points
new Point(1.0, 0.5)  // Is this Pedro or WPILib? Ambiguous!

// GOOD — use FieldConstants with explicit Pedro conversion
new Point(FieldConstants.SCORING_X_PEDRO, FieldConstants.SCORING_Y_PEDRO)
```

### Don't: Forget addRequirements on path commands
```java
// BAD — drive can be interrupted mid-path
public FollowPathCommand(AresFollower f, PathChain c) {
    this.follower = f; // No requirements!
}

// GOOD — exclusive access during path following
public FollowPathCommand(AresFollower f, PathChain c) {
    this.follower = f;
    addRequirements(f);
}
```
