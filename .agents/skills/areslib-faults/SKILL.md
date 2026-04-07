---
name: areslib-faults
description: Documents the ARESLib2 fault monitoring system — AresAlert, AresFaultManager, and AresDiagnostics. Use when adding hardware health checks, creating subsystem alerts, or implementing pre-match diagnostics.
---

# ARESLib2 Fault Monitoring System

You are a reliability engineer for Team ARES. When implementing hardware health checks, fault monitoring, or pre-match diagnostics, adhere strictly to the following guidelines.

## Architecture: Two-Layer Fault Pipeline

```
+--------------------------------------------------------------+
|  Layer 1: IO-Level Health Monitoring                         |
|  Package: org.areslib.hardware.faults                        |
|  Classes: FaultMonitor (interface), RobotHealthTracker       |
|  Role:    Polls hardware IO classes every loop tick.          |
|           Detects disconnected sensors / I2C failures.        |
|           Logs to AdvantageScope "System/ActiveFaults".       |
|           Bridges faults UP to Layer 2 via AresAlert.         |
+-------------------------|------------------------------------+
                          | Creates/sets AresAlert objects
+-------------------------v------------------------------------+
|  Layer 2: User-Facing Alert System                           |
|  Package: org.areslib.faults                                 |
|  Classes: AresAlert, AresFaultManager, AresDiagnostics       |
|  Role:    Manages alert lifecycle (active/inactive).          |
|           Drives gamepad rumble + LED color feedback.          |
|           Publishes categorized alerts to telemetry.           |
+--------------------------------------------------------------+
```

### Layer 1: FaultMonitor Interface

Hardware IO wrappers that can detect their own faults should implement `FaultMonitor`:

```java
public class MyOdometryWrapper implements OdometryIO, FaultMonitor {
    private boolean faultTripped = false;

    @Override
    public void updateInputs(OdometryInputs inputs) {
        try {
            // Read hardware...
            faultTripped = false;
        } catch (Exception e) {
            faultTripped = true;
            // Zero outputs gracefully — do NOT throw
            inputs.xMeters = 0;
            inputs.yMeters = 0;
        }
    }

    @Override public boolean hasHardwareFault() { return faultTripped; }
    @Override public String getFaultMessage() { return "Odometry I2C disconnected"; }
}
```

Register all `FaultMonitor` instances with `RobotHealthTracker.getInstance().registerMonitor(wrapper)` during initialization. The tracker is automatically registered with the `CommandScheduler` in `AresCommandOpMode`.

### Layer 2: AresAlert + AresFaultManager

Every subsystem should proactively report hardware faults. The system consists of three classes in `org.areslib.faults`:

## 1. `AresAlert` — Declaring Fault Conditions

Alerts are lightweight objects that represent a potential fault. They auto-register with the `AresFaultManager` on construction.

```java
// Declare as a static or instance field in your subsystem
private final AresAlert motorDisconnectAlert = 
    new AresAlert("Drive Motor 3 disconnected!", AresAlert.AlertType.ERROR);

private final AresAlert loopOverrunAlert =
    new AresAlert("Loop time exceeding 10ms", AresAlert.AlertType.WARNING);
```

### Alert Severity Levels
| Type | Purpose | Driver Feedback |
|:-----|:--------|:----------------|
| `ERROR` | Critical failure — subsystem non-functional (hardware disconnect, encoder fault) | Controller turns RED + rumble |
| `WARNING` | Degraded performance — brownout, slow loop, sensor noise | Logged only |
| `INFO` | General information — state transitions, calibration status | Logged only |

### Activating/Deactivating Alerts
```java
// In your subsystem's periodic():
motorDisconnectAlert.set(!motor.isAlive());  // true = active, false = cleared
loopOverrunAlert.set(loopTimeMs > 10.0);

// Dynamic text updates
motorDisconnectAlert.setText("Motor 3: current draw 0.0A for 500ms");
```

**Key behavior:** `alert.set(true)` will auto-re-register the alert if it was previously cleared by a `FaultManager.reset()` call. This makes static alerts survive OpMode transitions.

## 2. `AresFaultManager` — Central Alert Aggregation

The fault manager collects all active alerts and pushes them to telemetry as string arrays.

### Initialization (in `robotInit()`)
```java
AresFaultManager.initialize(driverGamepad); // Enables rumble + LED feedback
```

### Periodic Update (MUST be called every loop)
```java
// In your main loop / AresCommandOpMode:
AresFaultManager.update();
```

This call:
1. Collects all active alerts by severity level
2. Publishes `Alerts/Errors`, `Alerts/Warnings`, `Alerts/Infos` to telemetry as `String[]`
3. Turns the driver controller LED RED on first error → GREEN when resolved
4. Triggers a 500ms rumble when a NEW error appears

### Reset Between OpModes
```java
// In scheduler reset:
AresFaultManager.reset(); // Clears gamepad reference + all registered alerts
```

## 3. `AresDiagnostics` — Pre-Match Hardware Scan

Call this once during `robotInit()` before `waitForStart()` to automatically verify all hardware:

```java
boolean allDevicesOK = AresDiagnostics.runPreMatchCheck(hardwareMap);
```

This scans:
- **All `DcMotorEx`**: Reads encoder position to verify I2C communication
- **All `Servo`**: Reads position to verify PWM link
- **All `IMU`**: Reads yaw/pitch/roll to verify sensor response

Results are logged to:
- `Diagnostics/Status` → "PASS 8/8 devices OK" or "FAIL 2/8 devices FAILED"
- `Diagnostics/Passed/Motor: leftFront` → "OK" 
- `Diagnostics/Failed/Motor: rightRear` → "FAIL"

If ANY device fails, the `diagnosticAlert` ERROR is activated, turning the controller red.

**Note:** `AresDiagnostics` uses `HardwareMap` directly and thus runs ONLY on real hardware, not in desktop simulation. This is intentional — simulation doesn't need hardware verification.

## 4. Required Pattern for New Subsystems

Every new subsystem SHOULD declare at least one fault alert:

```java
public class ElevatorSubsystem extends SubsystemBase {
    private final AresAlert encoderFault = 
        new AresAlert("Elevator encoder disconnected", AresAlert.AlertType.ERROR);
    private final AresAlert stallAlert =
        new AresAlert("Elevator motor stalled", AresAlert.AlertType.WARNING);
    
    @Override
    public void periodic() {
        // Update IO...
        encoderFault.set(inputs.positionMeters == 0.0 && inputs.appliedVolts > 2.0);
        stallAlert.set(inputs.currentAmps > 30.0 && Math.abs(inputs.velocityMps) < 0.01);
    }
}
```

## Testing

```java
@Test
void testFaultManagerRegistersAlert() {
    AresFaultManager.getInstance().reset();
    AresAlert alert = new AresAlert("TestMotor", "Overcurrent detected");
    AresFaultManager.getInstance().registerAlert(alert);
    
    assertTrue(AresFaultManager.getInstance().getAlerts().contains(alert));
}
```
