---
name: areslib-architecture
description: Helps write and maintain code mapped to the ARESLib2 FTC framework, detailing coordinate systems, vision fusion architectures, and simulator parity techniques. Use when modifying or adding areslib subsystems, handling Pedro Pathing conversions, injecting vision offsets, or logging 3D poses natively to AdvantageScope.
---

You are an expert FTC Software Engineer for Team ARES. When asked to create new robot subsystems, commands, or mechanisms for an ARESLib2-based project, adhere strictly to the following architectural guidelines.

## 1. Scaffolding Subsystems (IO Abstraction Rule)

Every subsystem MUST have its hardware interaction abstracted behind an IO interface. You must NEVER instantiate hardware directly in the subsystem class. You must generate exactly four files:

1. **`[Name]IO.java`**: Interface with `[Name]Inputs` inner class. All hardware reads update this inputs object.
2. **`[Name]IOReal.java`**: Hardware implementation using FTC `HardwareMap`.
3. **`[Name]IOSim.java`**: Physics simulation using `AresPhysicsWorld` and `dyn4j`.
4. **`[Name]Subsystem.java`**: Business logic. Accepts `[Name]IO` via constructor dependency injection.

```java
public class ElevatorSubsystem extends SubsystemBase {
    private final ElevatorIO io;
    private final ElevatorIO.ElevatorInputs inputs = new ElevatorIO.ElevatorInputs();

    public ElevatorSubsystem(ElevatorIO io) {
        this.io = io;  // Could be IOReal or IOSim
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        AresTelemetry.log("Elevator", inputs);
    }
}
```

Periodically perform `io.updateInputs(inputs)` and log via `AresTelemetry.log()`.
Do NOT use raw `telemetry.addData()`; use `AresTelemetry`. See the `areslib-telemetry` skill.

## 2. Coordinate System Mapping (CRITICAL)

ARESLib2 bridges three coordinate systems:

| System | Origin | Units | Axis Convention |
|:---|:---|:---|:---|
| **dyn4j / WPILib** | Field center (0, 0) | Meters, Radians | X-forward, Y-left, θ CCW+ |
| **Pedro Pathing** | Bottom-left corner (72, 72) | Inches, Radians | X-right, Y-forward |
| **AdvantageScope** | Field center (0, 0) | Meters, Radians | WPILib convention |

### Conversion Rules
```java
// Pedro → WPILib
wpilibX = pedroY;
wpilibY = -pedroX;

// WPILib → Pedro  
pedroX = -wpilibY;
pedroY = wpilibX;

// Meters → Pedro inches (with origin shift)
pedroInches = (meters / 0.0254) + 72.0;
```

All hardware IO wrappers (`OdometryIO`, `VisionIO`) **always emit SI units (meters/radians) with center origin**. The `AresPedroLocalizer` handles conversion to Pedro's coordinate system automatically.

## 3. AdvantageScope 3D Formats

When logging `Pose3d` data, use a **length-7 `double` array** with quaternion rotation:
```
[X_Meters, Y_Meters, Z_Meters, W, X_Quat, Y_Quat, Z_Quat]
```

**Critical:** Do NOT use length-6 arrays `[X, Y, Z, Roll, Pitch, Yaw]`. AdvantageScope misidentifies them as 2x3 trajectories (two 2D poses).

Swerve module state arrays must be packed as `[Angle_rads, Speed_mps, ...]` (angle first, then speed). Reversing this order causes inverted 3D vector rendering.

## 4. Simulation Integration (dyn4j)

When writing `[Name]IOSim.java` layers:
- Register bodies to `AresPhysicsWorld.getInstance()`
- Use proper material properties (see `areslib-simulation` skill)
- Format point clouds as flat `double[]`: `[x1, y1, z1, x2, y2, z2, ...]`
- The simulation loop runs at exactly 50Hz

For deeper simulation guidance, see the `areslib-simulation` skill.

## 5. Fault Management

Always pipe hardware failures through `AresFaultManager`:
```java
if (!motor.isConnected()) {
    AresFaultManager.getInstance().registerAlert(
        new AresAlert("ElevatorMotor", "Motor disconnected")
    );
}
```
See the `areslib-faults` skill for full patterns.

## 6. Command Architecture

ARESLib2 uses a WPILib-ported command architecture. Do NOT use `com.arcrobotics.ftclib`:
- Commands from `org.areslib.command`
- Button bindings via `AresGamepad` and `Trigger`
- Subsystem requirements via `addRequirements()`

See the `areslib-commands` skill for full patterns.

## 7. Multi-Camera Sensor Fusion

`AresSensorFusionSubsystem` uses winner-takes-all selection, not averaging:
- Multiple cameras are processed at the wrapper level (`LimelightVisionWrapper`)
- The wrapper compares Target Area (`TA`) to select the most trustworthy measurement
- Raw camera poses are logged as sequential length-7 arrays for AdvantageScope ghost rendering

See the `areslib-vision` skill for full patterns.

## 8. Testing

All tests use headless JUnit 5 with real IOSim implementations — not mocks:
- Reset `AresPhysicsWorld` and `CommandScheduler` before each test
- Use `AresPhysicsWorld.getInstance().update(0.02)` for physics stepping
- See the `areslib-testing` skill for full patterns

## 9. Engineering Quirks & Hard-Won Lessons

### Gamepad Axis Normalization
FTC gamepads map "stick UP" to **negative Y** (`-1.0`). GUI frameworks (AWT, SDL2) map "UP" to **positive Y** (`+1.0`). The `AresGamepad` wrapper handles this inversion — do NOT double-invert in your code.

### dyn4j Wheel Slip vs Odometry
In high-fidelity simulation, encoder distance and field translation will never perfectly match due to wheel slip. `dyn4j` applies `linearDamping` and mass to the robot body. High damping → Pedro PIDs ramp up voltage → encoders over-count distance. This is physically accurate. To reduce drift, lower `linearDamping`.

### Pedro Pathing Static Configuration
`AresPedroConstants.configure()` may throw if called twice (e.g., across JUnit tests). Always wrap in try/catch or guard with a static `configured` flag.

## 10. Skill Cross-Reference

| Need | Skill |
|:---|:---|
| Drive subsystems & kinematics | `areslib-drivetrain` |
| Path following & autonomous | `areslib-autonomous` |
| Pedro Pathing API | `pedro-pathing` |
| Physics simulation | `areslib-simulation` |
| PID/feedforward/filters | `areslib-math` |
| Vision pipelines | `areslib-vision` |
| Hardware abstractions | `areslib-hardware` |
| Commands & buttons | `areslib-commands` |
| State machines | `areslib-statemachine` |
| Fault management | `areslib-faults` |
| Telemetry & logging | `areslib-telemetry` |
| Testing patterns | `areslib-testing` |
| AdvantageScope layouts | `advantagescope-layouts` |
| HUD & simulation viz | `advantagescope-hud-sim` |
| Build system | `gradle-ftc-desktop` |
| CI/CD pipeline | `areslib-ci` |
| Robot deployment | `robot-dev` |
| Creating new skills | `skill-authoring` |
