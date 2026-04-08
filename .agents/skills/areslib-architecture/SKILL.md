---
name: areslib-architecture
description: Helps write and maintain code mapped to the ARESLib2 FTC framework, detailing coordinate systems, vision fusion architectures, and simulator parity techniques. Use when modifying or adding areslib subsystems, injecting vision offsets, or logging 3D poses natively to AdvantageScope.
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
        AresAutoLogger.recordOutput("Elevator/PositionMeters", inputs.positionMeters);
    }
}
```

Periodically perform `io.updateInputs(inputs)` and log via `AresAutoLogger.recordOutput()`.
Do NOT use raw `telemetry.addData()`; use `AresAutoLogger` / `AresTelemetry`. See the `areslib-telemetry` skill.

## 2. Coordinate System Mapping (CRITICAL)

ARESLib2 bridges two coordinate systems:

| System | Origin | Units | Axis Convention |
|:---|:---|:---|:---|
| **dyn4j / WPILib / PathPlanner** | Field center (0, 0) | Meters, Radians | X-forward, Y-left, θ CCW+ |
| **AdvantageScope** | Field center (0, 0) | Meters, Radians | WPILib convention |

PathPlanner uses WPILib convention natively, so no coordinate conversion is needed between them.
However, native FTC hardware wrappers (Pinpoint, OTOS) typically output FTC standards (`+X Right, +Y Forward`). 

### Conversion Rules

**CRITICAL BUG PREVENTION**: All Odometry hardware implementations must manually swap and negate incoming position and velocity coordinates BEFORE injecting them into `inputs.positionMeters` or `inputs.velocityMps`. 
- `WPILib X` = `FTC Y` (Forward)
- `WPILib Y` = `-FTC X` (Left)

Failure to do this will cause kinematics desynchronization where forward commands cause the robot to strafe.

**ALWAYS use `CoordinateUtil`** (in `org.areslib.core`) for all conversions. Never write raw `* 0.0254`, `/ 25.4`, or `/ 1000.0` in application code.

```java
// Raw unit conversions
double meters = CoordinateUtil.inchesToMeters(inches);
double inches = CoordinateUtil.metersToInches(meters);
double inches = CoordinateUtil.mmToInches(mm);      // LiDAR distance zones
double meters = CoordinateUtil.mmToMeters(mm);       // Pinpoint odometry

// Fusion math (used by AresSensorFusionSubsystem)
double gain = CoordinateUtil.computeVisionKalmanGain(confidence);
double blended = CoordinateUtil.lerp(current, target, weight);
double heading = CoordinateUtil.shortestAngleLerp(currentRad, targetRad, weight);
```

All hardware IO wrappers (`OdometryIO`, `VisionIO`) **always emit SI units (meters/radians) with center origin**.

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

### Subsystem vs IOSim Physics Boundaries
**CRITICAL**: `dyn4j` physics simulation must be built entirely inside the `IOSim` files.
- **Subsystem (`[Name]Subsystem.java`)**: Contains ONLY business logic, PID controllers, constraints, and hardware-agnostic state machines. Never instantiate `dyn4j` physics bodies here.
- **IO Simulation (`[Name]IOSim.java`)**: Defines physical dimensions, mass, gearing, and friction. It attaches a `dyn4j` Body to `AresPhysicsWorld`, applies forces/voltages calculated by the Subsystem, and publishes the resulting kinematics positions back to the Subsystem's generic Inputs block.

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
In high-fidelity simulation, encoder distance and field translation will never perfectly match due to wheel slip. `dyn4j` applies `linearDamping` and mass to the robot body. High damping causes path-following PIDs to ramp up voltage, making encoders over-count distance. This is physically accurate. To reduce drift, lower `linearDamping`.

For the full skill routing table, see the `areslib` skill.
