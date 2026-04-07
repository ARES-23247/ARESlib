---
name: pedro-pathing
description: Helps write autonomous and teleop code using Pedro Pathing library. Use when creating paths, building autonomous routines, setting up path following, working with Follower, PathChain, BezierLine, or heading interpolation.
---

# Pedro Pathing


You are an autonomous pathing engineer for Team ARES. When creating paths, building autonomous routines, or configuring Pedro Pathing followers, adhere strictly to the following guidelines.
Pedro Pathing is a path follower using Bezier curves for smooth autonomous navigation.

## Quick Start

### Autonomous OpMode Structure

```java
@Autonomous(name = "My Auto")
public class MyAuto extends OpMode {
    private Follower follower;
    private Timer pathTimer;
    private int pathState;

    // Define poses (x, y, heading in radians)
    private final Pose startPose = new Pose(7, 6.75, Math.toRadians(0));
    private final Pose targetPose = new Pose(24, 48, Math.toRadians(90));

    @Override
    public void init() {
        pathTimer = new Timer();
        follower = Constants.createFollower(hardwareMap);
        buildPaths();
        follower.setStartingPose(startPose);
    }

    @Override
    public void loop() {
        follower.update();  // MUST call every loop
        autonomousPathUpdate();
        telemetry.update();
    }
}
```

### Building Paths

```java
// Straight line path
Path straightPath = new Path(new BezierLine(startPose, endPose));
straightPath.setLinearHeadingInterpolation(startPose.getHeading(), endPose.getHeading());

// PathChain (multiple segments or single path via builder)
PathChain myChain = follower.pathBuilder()
    .addPath(new BezierLine(startPose, endPose))
    .setLinearHeadingInterpolation(startPose.getHeading(), endPose.getHeading())
    .build();
```

### Following Paths

```java
follower.followPath(myPath);           // Follow without holding endpoint
follower.followPath(myChain, true);    // Hold endpoint position when done
```

### State Machine Pattern

```java
public void autonomousPathUpdate() {
    switch (pathState) {
        case 0:
            follower.followPath(firstPath);
            setPathState(1);
            break;
        case 1:
            if (!follower.isBusy()) {
                // Path complete, do action then next path
                follower.followPath(nextPath, true);
                setPathState(2);
            }
            break;
    }
}

public void setPathState(int state) {
    pathState = state;
    pathTimer.resetTimer();
}
```

### TeleOp with Pedro

```java
@Override
public void loop() {
    follower.update();
    follower.setTeleOpMovementVectors(
        -gamepad1.left_stick_y,   // forward
        -gamepad1.left_stick_x,   // strafe
        -gamepad1.right_stick_x,  // turn
        true                      // robot-centric (false for field-centric)
    );
}
```

## Key Concepts & v2.x Namespace Changes

Historically in v1.x, curves were in `com.pedropathing.pathgen`. **As of v2.x, these have moved:**
- **`com.pedropathing.geometry.Pose`**: Position (x, y) + heading in radians. Field is 144x144 inches, origin bottom-left.
- **`com.pedropathing.geometry.Point` / `BezierPoint`**: Used to build curves.
- **`com.pedropathing.geometry.BezierLine`**: Straight path between two points.
- **`com.pedropathing.geometry.BezierCurve`**: Curved path with control points (minimum 3 points).
- **`com.pedropathing.paths.Path`**: Represents an actionable trajectory.
- **`com.pedropathing.paths.PathChain`**: Multiple path segments as one unit.
- **`Follower`**: Main controller - call `update()` every loop cycle.

## Transition Conditions

Check when path is complete:
- `!follower.isBusy()` - Path finished
- `pathTimer.getElapsedTimeSeconds() > 1.5` - Time elapsed
- `follower.getPose().getX() > 36` - Position threshold

## Anti-Patterns

### Don't: Forget to call update()

```java
// BAD - Robot won't follow paths
@Override
public void loop() {
    autonomousPathUpdate();  // Missing follower.update()!
}

// GOOD - Always call update() first
@Override
public void loop() {
    follower.update();  // MUST be called every loop
    autonomousPathUpdate();
}
```

### Don't: Use `BezierCurve` for straight 2-point lines

```java
// BAD - Will crash with "java.lang.IllegalArgumentException: Too few control points"
Path straight = new Path(new BezierCurve(startPose, endPose)); 

// GOOD - Always use BezierLine for dual-point generation
Path straight = new Path(new BezierLine(startPose, endPose));
```

## Common Pitfalls & Limitations

### 1. Drivetrain Vector Mapping (Field-Centric Issue)
Because Pedro Pathing tracks position via its own `(0, 144)` coordinate system, its internal PID outputs (`follower.getDriveVector()`) are returned as **Field-Centric** vectors. 
If your hardware layer (or simulated `SwerveDriveSubsystem`) expects **Robot-Centric** chassis speeds (forward, strafe), you MUST manually rotate Pedro's drive vector by `-currentHeading` before applying it. Failure to do this means the robot will drive backwards/sideways upon turning.

### 2. Do NOT Set Bounds Based on Center-Origin
If your field physics operate on a center-origin `(0,0)` system natively, **do not enforce glitch-protection bounds based on `-72` to `72`**. Because Pedro Pathing always operates in a bottom-left `0` to `144` inch box internally, doing this will inadvertently clip the robot in Pedro's logic. Clamp positional noise natively using `0` to `(144 + robot_overhang)` limits.

### 3. Don't use standard setPose() to set Starting Position

```java
// BAD - Robot thinks it's at (0, 0, 0)
@Override
public void init() {
    follower = Constants.createFollower(hardwareMap);
    buildPaths();  // Paths may be wrong!
}

// GOOD - Set actual starting position
@Override
public void init() {
    follower = Constants.createFollower(hardwareMap);
    follower.setStartingPose(startPose);
    buildPaths();
}
```

### Don't: Use holdEnd when you don't need it

```java
// BAD - Robot fights to hold position between paths
follower.followPath(path1, true);  // Holds endpoint
follower.followPath(path2, true);  // Unnecessary hold

// GOOD - Only hold on final path or when needed
follower.followPath(path1);        // No hold between paths
follower.followPath(path2, true);  // Hold at end
```

### Don't: Use degrees for heading

```java
// BAD - Heading in degrees causes wrong rotation
Pose target = new Pose(24, 48, 90);  // 90 degrees? No!

// GOOD - Always use radians
Pose target = new Pose(24, 48, Math.toRadians(90));
```

## Reference Documentation

- [HEADING_INTERPOLATION.md](HEADING_INTERPOLATION.md) - All heading options
- [CONSTRAINTS.md](CONSTRAINTS.md) - Path completion constraints
- [CONSTANTS_SETUP.md](CONSTANTS_SETUP.md) - Robot configuration
- [TUNING.md](TUNING.md) - Tuning process overview
