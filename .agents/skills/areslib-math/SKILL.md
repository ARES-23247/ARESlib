---
name: areslib-math
description: Reference for ARESLib2's ported WPILib math library — PID controllers, feedforward models, motion profiles, filters, and geometry. Use when tuning mechanisms, implementing control loops, or working with pose estimation instead of reimplementing from scratch.
---

# ARESLib2 Math & Controls Library


You are a controls engineer for Team ARES. When implementing PID controllers, feedforward models, motion profiles, or pose estimation, adhere strictly to the following guidelines.
ARESLib2 ships with a complete port of WPILib's math utilities in `org.areslib.math.*`. **Do NOT reimplement PID, feedforward, or motion profiling from scratch** — use these classes.

## 1. Controllers (`org.areslib.math.controller`)

### `PIDController`
Standard Proportional-Integral-Derivative controller.
```java
PIDController pid = new PIDController(kP, kI, kD);
pid.setTolerance(0.01);                  // Position tolerance
pid.setIntegratorRange(-0.5, 0.5);       // Anti-windup clamp

// In periodic:
double output = pid.calculate(measured, setpoint);
boolean atGoal = pid.atSetpoint();
```

### `ProfiledPIDController`
PID controller that internally generates a `TrapezoidProfile` for smooth acceleration.
```java
ProfiledPIDController ppid = new ProfiledPIDController(kP, kI, kD,
    new TrapezoidProfile.Constraints(maxVelocity, maxAcceleration));
ppid.setGoal(targetPosition);

// In periodic — automatically profiles the setpoint:
double output = ppid.calculate(measuredPosition);
```

**When to use which:**
- `PIDController` → Simple setpoint tracking (heading lock, wheel speed)
- `ProfiledPIDController` → Smooth position control (elevator, arm, turret)

### `BangBangController`
On/off controller for velocity targets (flywheel spinup).
```java
BangBangController bang = new BangBangController(toleranceRPM);
double output = bang.calculate(measuredRPM, targetRPM); // Returns 1.0 or 0.0
```

## 2. Feedforward Models (`org.areslib.math.controller`)

### `SimpleMotorFeedforward`
For systems with no gravity component (drivetrain wheels, flywheels).
```java
SimpleMotorFeedforward ff = new SimpleMotorFeedforward(kS, kV, kA);
double voltage = ff.calculate(velocitySetpoint, accelerationSetpoint);
// kS = static friction, kV = velocity gain, kA = acceleration gain
```

### `ArmFeedforward`
For rotating arms where gravity torque changes with angle.
```java
ArmFeedforward ff = new ArmFeedforward(kS, kG, kV, kA);
double voltage = ff.calculate(armAngleRad, velocityRadPerSec, accelRadPerSecSq);
// kG = gravity compensation (cos(angle) term)
```

### `ElevatorFeedforward`
For linear mechanisms fighting constant gravity.
```java
ElevatorFeedforward ff = new ElevatorFeedforward(kS, kG, kV, kA);
double voltage = ff.calculate(velocityMps, accelerationMpsSq);
// kG = constant gravity offset (always applied)
```

**Selection guide:**
| Mechanism | Feedforward Class | Gravity? |
|:----------|:-----------------|:---------|
| Drivetrain wheels | `SimpleMotorFeedforward` | No |
| Flywheel / shooter | `SimpleMotorFeedforward` | No |
| Rotating arm / wrist | `ArmFeedforward` | Yes (angle-dependent) |
| Elevator / linear slides | `ElevatorFeedforward` | Yes (constant) |

## 3. Motion Profiling (`org.areslib.math.controller`)

### `TrapezoidProfile`
Generates smooth trapezoidal velocity profiles for position targets.
```java
TrapezoidProfile.Constraints constraints = 
    new TrapezoidProfile.Constraints(maxVelocity, maxAcceleration);
TrapezoidProfile profile = new TrapezoidProfile(constraints,
    new TrapezoidProfile.State(goalPosition, 0),   // goal
    new TrapezoidProfile.State(currentPos, currentVel)); // current

// Sample at a given time:
TrapezoidProfile.State setpoint = profile.calculate(dtSeconds);
double targetPos = setpoint.position;
double targetVel = setpoint.velocity;
```

## 4. Filters (`org.areslib.math.filter`)

| Class | Purpose | Example Usage |
|:------|:--------|:-------------|
| `KalmanFilter1D` | Optimal state estimation for noisy sensors | Odometry fusion |
| `MedianFilter` | Removes spike noise from sensor readings | Distance sensor cleanup |
| `LinearFilter` | Low-pass / high-pass / moving average | Smoothing current draw |
| `SlewRateLimiter` | Limits rate of change of a value | Joystick ramping |
| `Debouncer` | Requires a signal to be stable for N seconds | Button press validation |

### `SlewRateLimiter` — Most commonly used
```java
SlewRateLimiter limiter = new SlewRateLimiter(rateLimit); // units/sec max change
double smoothedValue = limiter.calculate(rawInput);
```

## 5. Geometry (`org.areslib.math.geometry`)

Full WPILib geometry port: `Pose2d`, `Pose3d`, `Rotation2d`, `Rotation3d`, `Translation2d`, `Translation3d`, `Transform2d`, `Transform3d`, `Twist2d`.

## 6. Anti-Patterns

### Don't: Reimplement PID
```java
// BAD — error-prone, no anti-windup, no tolerance checking
double error = target - measured;
integral += error * dt;
double output = kP * error + kI * integral + kD * (error - lastError) / dt;

// GOOD — use the built-in controller
PIDController pid = new PIDController(kP, kI, kD);
double output = pid.calculate(measured, target);
```

### Don't: Use SimpleMotorFeedforward for an elevator
```java
// BAD — elevator will backdrive under gravity
double voltage = simpleFF.calculate(velocity);

// GOOD — ElevatorFeedforward adds constant gravity compensation
double voltage = elevatorFF.calculate(velocity, acceleration);
```

## Testing

```java
@Test
void testPIDControllerConverges() {
    PIDController pid = new PIDController(1.0, 0.0, 0.0);
    pid.setSetpoint(10.0);
    
    double output = pid.calculate(0.0);  // Error = 10.0
    assertEquals(10.0, output, 0.001, "P-only controller output should equal error * kP");
}
```
