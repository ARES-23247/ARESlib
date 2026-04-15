---
name: areslib-control-theory
description: Helps architect PID loops, Feedforwards, slew limiters, and kinematics in ARESLib mechanisms. Use when tuning mechanisms, implementing control loops, or working with drivetrain dynamics.
---

# ARESLib Control Theory Skill

You are a controls engineer for Team ARES 23247. When implementing motor control or tuning mechanisms:

## 1. Architecture

ARESLib's control hierarchy follows the **Feedforward > Feedback** principle:

| Layer | Runs On | Purpose |
|---|---|---
| Feedforward (kS, kG, kV, kA) | Control Hub (subsystem) | Predicts voltage from physics model |
| PID (kP, kI, kD) | Expansion Hub (motor controller) | Corrects error from FF prediction |
| SlewRateLimiter | Control Hub (teleop) | Constrains driver input acceleration |
| Motion Profile | Expansion Hub (PID) | Generates smooth position/velocity trajectories |

### Feedforward Classes
- `ElevatorFeedforward` — For linear mechanisms with gravity (`kS + kG + kV·v + kA·a`)
- `ArmFeedforward` — For rotational mechanisms with gravity (`kS + kG·cos(θ) + kV·v + kA·a`)
- `SimpleMotorFeedforward` — For flywheels/intakes without gravity (`kS + kV·v + kA·a`)

All feedforward gains are stored as `@Config` parameters so they can be tuned live from FTC Dashboard without redeploying code.

## 2. Key Rules

### Rule A: Tune Feedforward Before PID
You MUST characterize the mechanism with SysId and set `kS`, `kG`, `kV` before touching `kP`. A well-tuned FF should get the mechanism to ~90% accuracy. PID only corrects the remaining error. If you start with PID, the system will oscillate or overshoot.

### Rule B: Discretize Drivetrain Speeds
Never pass raw drivetrain velocity commands directly to modules. Always apply proper discretization to compensate for coordinate skew during simultaneous translation and rotation within a loop tick. Without this, the robot curves instead of driving straight while spinning.

### Rule C: Clamp Driver Inputs with SlewRateLimiter
Wrap all teleop joystick inputs through `SlewRateLimiter` before passing to drivetrain. Without rate limiting, full-stick acceleration causes wheel slip, destroying odometry accuracy. Typical limits: 0.3 m/s² translation, 1.0 rad/s² rotation.

### Rule D: Use @Config for All Gains
Never hardcode `kP = 5.0` in the constructor. Use `@Config public double kP = 5.0;`. This allows real-time tuning via FTC Dashboard and ensures gains are logged for post-match analysis.

### Rule E: Selecting the Right Motor Control Mode
When writing motor control logic, strictly adhere to these mechanism mappings for FTC (Rev Robotics):
* **Intakes / Conveyors / Feeders:** `RUN_WITHOUT_ENCODER` or `RUN_USING_ENCODER` (Open Loop).
* **Drivetrain Wheels / Flywheels:** `VelocityControl` (Closed Loop to maintain RPM using RevPID).
* **Servos / Clevises:** `PositionControl` (Fast positional snapping with profiled limits).
* **Elevators / Linear Slides / Pivot Arms:** `PositionControl with Motion Profiling` (Closed Loop with Trapezoidal profiling to prevent destructive mechanical jerks).

## 3. Adding Control Loops to New Mechanisms

1. **Run SysId** using the subsystem's built-in SysId commands (quasistatic and dynamic).
2. **Extract gains** from the SysId analysis tool (kS, kG, kV, kA).
3. **Create `@Config` parameters** in the subsystem for each gain.
4. **Instantiate the correct `Feedforward` class** based on mechanism type.
5. **Set PID on the motor controller** via the hardware layer's configuration method.
6. **Apply FF as voltage offset** in the subsystem's `setTargetPosition()` or `setTargetVelocity()`.

## 4. Constants
All control gains live in the mechanism's Constants class:
- `ElevatorConstants.kS/kG/kV/kA` — Elevator feedforward defaults
- `ArmConstants.kS/kG/kV/kA` — Arm feedforward defaults
- `DriveConstants.SLEW_RATE_TRANSLATION/ROTATION` — Driver input rate limits

## 5. Telemetry
- `{Mechanism}/kS`, `kG`, `kV`, `kA` — Current feedforward gains
- `{Mechanism}/TargetPosition` — Commanded setpoint
- `{Mechanism}/Position` — Actual position
- `{Mechanism}/AppliedVolts` — Total voltage (FF + PID output)
- `{Mechanism}/PositionError` — Setpoint minus actual
- `Drivetrain/DesiredSpeeds` — Pre-discretize commanded speeds
- `Drivetrain/DiscretizedSpeeds` — Post-discretize corrected speeds

## 6. FTC-Specific Considerations

### Motor Controller Differences from FRC
- **Rev Robotics Hub**: Uses `RUN_USING_ENCODER` for velocity control, `RUN_TO_POSITION` for position control
- **PID tuning**: Use `Motor.setVelocityPIDFCoefficients(kP, kI, kD, kF, kS)` where kF is feedforward
- **Encoder resolution**: Typically higher resolution than FRC (e.g., 28 ticks per revolution for through-bore encoders)

### Loop Timing
- **Control Hub**: Main loop runs at ~50Hz (20ms)
- **Expansion Hub**: Motor control loop runs at ~50Hz via bulk reads
- **Teleop**: Gamepad updates at ~50Hz

### Hardware-Specific Tuning
- **Rev Robotics motors**: Lower stall current than CIMs — adjust kS accordingly
- **Hex motors**: Different torque curves — verify feedforward models match motor specs
- **Battery voltage**: FTC 12V system sags more aggressively — add voltage compensation if needed
