---
name: areslib-telemetry
description: Defines the ARESLib2 telemetry pipeline — AresAutoLogger, AresTelemetry backends, and AdvantageKit-style @AutoLog IO pattern. Use when logging subsystem data, configuring log backends, or ensuring telemetry parity between real hardware and simulation.
---

# ARESLib2 Telemetry Architecture


You are an expert telemetry engineer for Team ARES. When logging subsystem data, configuring log backends, or ensuring telemetry parity between real hardware and simulation, adhere strictly to the following guidelines.
## CRITICAL RULE
**NEVER** use `telemetry.addData()`, `SmartDashboard.putNumber()`, or any direct FTC/WPILib telemetry call.
ALL data must flow through the `AresAutoLogger` → `AresTelemetry` → Backend pipeline.

## 1. The Logging Pipeline

```
[Subsystem periodic()]
        │
        ▼
AresAutoLogger.processInputs("Prefix", inputs)
        │
        ▼
AresTelemetry.putNumber() / putNumberArray() / putString()
        │
        ▼
┌───────────────────────────────┐
│   Registered Backends         │
│  ┌─────────────────────────┐  │
│  │ RlogServerBackend       │──│──▶ AdvantageScope (live)
│  │ WpiLogBackend           │──│──▶ .wpilog file (replay)
│  │ AndroidDashboardBackend │──│──▶ FTC Dashboard
│  └─────────────────────────┘  │
└───────────────────────────────┘
```

## 2. `AresAutoLogger` — The Central Hub

This is the ONLY class you call for structured IO logging. It uses cached reflection to flatten `AresLoggableInputs` fields into telemetry keys.

### Logging IO Inputs (Primary Pattern)
```java
// Inside any Subsystem's periodic():
AresAutoLogger.processInputs("Elevator", elevatorInputs);
// This auto-flattens ALL public fields in elevatorInputs:
//   → "Elevator/positionMeters"
//   → "Elevator/velocityMps"
//   → "Elevator/currentAmps"
```

### Logging Arbitrary Outputs
```java
// Single values
AresAutoLogger.recordOutput("Drive/TargetHeading", targetHeadingRad);
AresAutoLogger.recordOutput("Auto/CurrentState", "SCORING");

// Arrays (for AdvantageScope visualization)
AresAutoLogger.recordOutputArray("Drive/SwerveStates", swerveStateArray);

// String arrays (for fault display)
AresAutoLogger.recordOutput("Alerts/Active", new String[]{"Motor 3 disconnected"});
```

### Supported Field Types in `AresLoggableInputs`
| Java Type | Telemetry Method | Notes |
|:----------|:-----------------|:------|
| `double` | `putNumber()` | Standard numeric |
| `int` | `putNumber()` | Cast to double |
| `boolean` | `putNumber()` | 1.0 = true, 0.0 = false |
| `String` | `putString()` | Text values |
| `double[]` | `putNumberArray()` | Pose3d, LiDAR arrays, swerve states |
| `SwerveModuleState[]` | `putNumberArray()` | Auto-packed as `[angle, speed, angle, speed...]` |

## 3. `AresTelemetry` — Backend Distribution

`AresTelemetry` is a static hub that fans out data to ALL registered backends simultaneously.

### Registering Backends
```java
// In AresCommandOpMode.robotInit():
AresTelemetry.registerBackend(new RlogServerBackend());      // Live → AdvantageScope
AresTelemetry.registerBackend(new WpiLogBackend(logFile));    // Persistent .wpilog
AresTelemetry.registerBackend(new AndroidDashboardBackend()); // FTC Dashboard
```

### Backend Lifecycle
- `registerBackend()` is **idempotent** — re-registering the same class replaces the old instance.
- Call `AresTelemetry.clearBackends()` during scheduler reset to prevent stale accumulation.
- Call `AresTelemetry.update()` at the END of every periodic loop to flush buffered data.

### Convenience Helpers
```java
// Log a Pose2d as a 3-element array [x, y, heading]
AresTelemetry.putPose2d("Odometry/Pose", xMeters, yMeters, headingRad);

// Log SwerveModuleStates in AdvantageScope format [angle, speed, angle, speed, ...]
AresTelemetry.logSwerveStates("Drive/MeasuredStates", moduleStates);

// Log differential/mecanum speeds
AresTelemetry.logDifferentialSpeeds("Drive/WheelSpeeds", speeds);
AresTelemetry.logMecanumSpeeds("Drive/WheelSpeeds", speeds);
```

## 4. Anti-Patterns

### Don't: Use raw FTC telemetry
```java
// BAD — invisible to AdvantageScope, breaks simulation parity
telemetry.addData("Elevator Height", elevator.getPosition());

// GOOD — flows through all backends simultaneously
AresAutoLogger.recordOutput("Elevator/Height", elevator.getPosition());
```

### Don't: Call processInputs with non-AresLoggableInputs objects
```java
// BAD — will silently skip all fields
AresAutoLogger.processInputs("Elevator", someRandomObject);

// GOOD — inputs class must implement AresLoggableInputs
public static class ElevatorInputs implements AresLoggableInputs { ... }
```

### Don't: Forget to call AresTelemetry.update()
```java
// BAD — data is buffered but never flushed to backends
periodic() {
    AresAutoLogger.processInputs("Drive", inputs);
    // Missing: AresTelemetry.update();
}
```
