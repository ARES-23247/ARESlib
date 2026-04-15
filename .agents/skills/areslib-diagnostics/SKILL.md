---
name: areslib-diagnostics
description: Helps architect pre-match system checks, hardware sweep assertions, and bus dropout tracking. Use when adding health checks, creating fault alerts, or implementing pre-match test routines.
---

# ARESLib Diagnostics & Health Skill

You are a systems reliability engineer for Team ARES 23247. When writing fault detection or pre-match checks:

## 1. Architecture

The diagnostics system spans the fault management package:

| Class | Package | Purpose |
|---|---|---
| `ARESFaultManager` | `com.areslib.faults` | Static registry of critical/warning faults. Tracks new faults since last check. |
| `Alert` | `com.areslib.faults` | Dashboard-compatible alert with severity levels (INFO, WARNING, CRITICAL) |
| `ARESDiagnosticCheck` | `com.areslib.auto` | Pre-match sequential system sweep command |
| `SystemCheckCommand` | `org.firstinspires.ftc.teamcode.commands` | Robot-specific test mode routines |

### Fault Flow
```
Hardware dropout → Hardware layer detects error → ARESFaultManager.registerCriticalFault()
                                                     → Alert("Motor Disconnected", CRITICAL).set(true)
                                                     → LEDManager shows fault flash
```

## 2. Key Rules

### Rule A: Never Crash on Hardware Failure
If a motor drops off the bus mid-match, the hardware layer MUST catch the error, log it via `ARESFaultManager`, and continue operating. A dead intake must NOT crash the drivetrain.

### Rule B: Alerts Use ARESLib's Custom Alert Class
Use `com.areslib.fault.Alert`, NOT standard logging mechanisms. ARESLib's version has `resetAll()` for test cleanup. If you use standard logging, test suites will leak state.

### Rule C: Faults Are Static — Reset in Tests
`ARESFaultManager` and `Alert` both use static state. EVERY test `@BeforeEach` block MUST call `ARESTestHarness.reset()` which handles both resets (plus other singletons):
```java
ARESTestHarness.reset(); // Clears ARESFaultManager, Alert, etc.
```
Failing to reset causes fault state to bleed across tests, producing false failures.

### Rule D: System Sweeps Must Implement SystemTestable
`ARESDiagnosticCheck` doesn't just check connectivity — it commands mechanisms to physical positions and asserts encoder deltas match expected travel. To achieve this predictably, every major hardware subsystem MUST implement the `SystemTestable` interface and provide a `getSystemCheckCommand()` that can be sequenced during the pre-match ritual. If an elevator is commanded to 0.5m but only reads 0.01m, the gearbox is stripped.

### Rule E: Handle FTC-Specific Failure Modes
FTC has unique failure modes that must be monitored:
- **Expansion Hub disconnection**: Monitor bulk read latency spikes
- **Servo overstall**: Detect servos drawing excessive current or not reaching setpoints
- **Motor overheating**: Many Rev motors lack temperature sensors — infer from current patterns
- **USB camera disconnection**: Monitor frame timestamps for gaps indicating disconnect

## 3. Adding New Fault Checks

1. In the hardware layer's `updateInputs()`, check for hardware errors:
   ```java
   if (motorStatus == MotorStatus.DISCONNECTED) {
       ARESFaultManager.registerCriticalFault("Module" + id + " motor dropout");
       new Alert("Module " + id + " Motor Fault", AlertType.CRITICAL).set(true);
   }
   ```
2. For boot-time checks, add firmware version validation in the hardware constructor.
3. For pre-match sweeps, add a new stage to `SystemCheckCommand`:
   ```java
   // Stage: Climber travel test
   Commands.sequence(
       climber.setTargetCommand(0.5),
       Commands.waitSeconds(2.0),
       Commands.runOnce(() -> assertTrue(climber.getPosition() > 0.4))
   );
   ```

## 4. Command API
```java
// In OpMode setup, bind to test mode:
if (isTestMode()) {
    schedule(new SystemCheckCommand(drive, elevator, arm));
}
```

## 5. Telemetry
- `Diagnostics/HasCriticalFaults` — Boolean: any critical fault active
- `Diagnostics/NewCriticalFault` — Most recent fault string
- `Diagnostics/TotalFaultCount` — Cumulative fault count
- `Alerts/*` — Individual alert states visible in FTC Dashboard
- `SystemCheck/Stage` — Current diagnostic stage name
- `SystemCheck/Passed` — Boolean: all stages passed

## 6. FTC-Specific Diagnostics

### Expansion Hub Health
- Monitor bulk read latency — >50ms indicates impending failure
- Track I2C bus errors — accumulation indicates loose cables
- Check 5V rail voltage — sagging indicates too many servos

### Motor-Specific Checks
- **Rev Core Hex**: Monitor current spikes — sustained >8A indicates stall
- **Hex Motor**: Different current profile — adjust thresholds accordingly
- **Through Bore Encoder**: Verify velocity consistency — jitter indicates cable issues

### Servo Health
- Most servos don't report position — detect stalls via repeated command without movement
- Monitor servo power rail voltage — droop indicates too many servos on one hub

### Battery Monitoring
- FTC batteries sag more aggressively than FRC
- Monitor voltage during high-current maneuvers
- Alert if voltage drops below 11.5V during match

### Camera Monitoring
- Monitor Vuforia/AprilTag frame rates
- Detect USB disconnection via frame timestamp gaps
- Fallback to dead reckoning if vision fails
