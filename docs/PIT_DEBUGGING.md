# ARESLib2 Pit Debugging Flowchart

## Quick Reference: What To Check When Something Breaks

```
Robot isn't moving?
├── Controller light is RED → Hardware fault detected
│   ├── Open AdvantageScope → "System/ActiveFaults" key
│   │   Shows exact device name (e.g., "Motor FL encoder disconnected")
│   ├── Check "Alerts/Errors" key for all active error alerts
│   ├── Fix hardware → light turns GREEN automatically
│   └── Run pre-match check: AresDiagnostics.runPreMatchCheck(hardwareMap)
│
├── Controller light is normal → Not a detected hardware fault
│   ├── Check "Console" key in AdvantageScope for [ERR] messages
│   ├── Check Driver Station logs (logcat) for RuntimeException
│   └── Is the CommandScheduler running?
│       ├── Check "Scheduler/ActiveCommands" telemetry
│       └── Verify your OpMode extends AresCommandOpMode
│
└── Robot moves but wrong direction → Coordinate system issue
    ├── Check "Drive/Pose" in AdvantageScope
    │   Should show a valid field pose in WPILib coordinates (meters)
    ├── Is alliance configured correctly?
    │   RED = -90° offset, BLUE = +90° offset
    └── Check odometry source (OTOS vs Pinpoint vs Dead Wheels)
        └── "Odometry/xMeters" and "Odometry/yMeters" should update
```

## Fault System Architecture (For Debugging)

```
When a sensor disconnects, this is what happens:

  Hardware IO class (e.g., OtosOdometryWrapper)
    │ Catches exception, sets faultTripped = true
    │ Implements FaultMonitor interface
    │
    ▼
  RobotHealthTracker.periodic()           ← Runs every loop (registered in CommandScheduler)
    │ Polls all FaultMonitor devices
    │ Logs to "System/ActiveFaults" (AdvantageScope)
    │ Creates AresAlert and sets it active
    │
    ▼
  AresFaultManager.update()               ← Also runs every loop
    │ Reads all active AresAlerts
    │ Publishes to "Alerts/Errors", "Alerts/Warnings"
    │ Controls gamepad LED (RED = fault, GREEN = clear)
    │ Triggers gamepad rumble on NEW faults
    │
    ▼
  Driver sees RED light + feels rumble
```

## Common Scenarios

### "Pre-match diagnostics FAILED"
- Caused by `AresDiagnostics.runPreMatchCheck()` in `robotInit()`
- One or more devices didn't respond to a test read
- Check `Diagnostics/Failed/*` keys in AdvantageScope for exact device names
- Usually: loose USB cable, wrong device name in hardwareMap config

### "Motor stalling" or unexpected behavior
- Check `Power/batteryVoltage` — if below 9V, `masterPowerScale` reduces all motor output
- Check `Power/totalCurrentAmps` — if above 15A, load shedding kicks in
- Both are logged live in AdvantageScope

### Path following is wrong
- Check `Drive/Pose` vs `Vision/Pose` — they should roughly agree
- If they diverge: odometry is drifting, vision confidence is too low, or coordinate conversion is wrong
- The sensor fusion Kalman gain is logged at `SensorFusion/KalmanGain`

### Simulation faults triggering on real robot
- This should be impossible now (guarded by `AresRobot.isSimulation()`)
- If you see "encoder shatter" or "I2C crash" on real hardware, check that `AresRobot.setSimulation(true)` is NOT being called in your competition OpMode

## Key AdvantageScope Fields

| Field | What It Shows |
|-------|--------------|
| `System/ActiveFaults` | String array of currently faulted devices |
| `Alerts/Errors` | All active ERROR-level alerts |
| `Alerts/Warnings` | All active WARNING-level alerts |
| `Diagnostics/Status` | Pre-match check result (PASS/FAIL) |
| `Power/batteryVoltage` | Live battery voltage |
| `Power/masterPowerScale` | Current power limiting factor (0.0-1.0) |
| `Drive/Pose` | Current robot pose in WPILib coordinates (meters) |
| `LiDAR/FieldHits` | Detected obstacle positions |
| `Console` | General system messages |
