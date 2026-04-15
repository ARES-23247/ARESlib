---
name: areslib-operator
description: Helps formulate GamepadEx layouts, haptics feedback, and UI/Dashboard experiences. Use when binding controller buttons, adding rumble feedback, or configuring FTC Dashboard views.
---

# ARESLib Operator UX Skill

You are the user experience lead for Team ARES 23247. When binding controls or designing driver feedback:

## 1. Architecture

The operator interface spans the input and UI packages:

| Class | Purpose |
|---|---|
| `GamepadEx` | Enhanced gamepad with debouncing, combos, and advanced bindings |
| `LEDManager` | Translates robot state into visual LED feedback |
| `LEDHardware` | Hardware interface for LED control (Rev LED strip, Adafruit) |
| `OpMode` | Command bindings, auto chooser, gamepad configuration |
| `TeleopDriveMath` | Pure-function joystick→drivetrain math (tested separately) |

### Controller Setup
ARESLib uses a **single gamepad** setup typical for FTC:
- **Gamepad 1** — Primary driver (drivetrain, intake, scoring, subsystem control)
- **Gamepad 2** (optional) — Copilot for complex mechanisms (optional, team-dependent)

### LED Priority System
```
ARESFaultManager.hasActiveCriticalFaults() → Critical fault flash (highest priority)
ARESPowerManager.getVoltage() < WARNING    → Load shedding amber pulse
Scoring state active                         → Alliance-colored scoring pattern
default                                      → Alliance-colored idle pattern
```

## 2. Key Rules

### Rule A: Every Command Must Be Idempotent
When binding a gamepad button, the command MUST be safe to call repeatedly. If a composite sequence spans multiple states, use proper state machine transitions. Without this, button mashing can leave mechanisms in inconsistent states.

### Rule B: Haptic Feedback for Invisible Events
Drivers can't watch the controller screen during matches. Use gamepad rumble for:
- Game piece collected (intake sensor triggered)
- Alignment locked on target (PID converged)
- Autonomous sequence selected
- Critical fault detected

### Rule C: Use @Config for Manual Overrides
Any driver-adjustable value (arm offset, shot angle trim) MUST use `@Config`. This allows pit crew to adjust without recompiling.

### Rule D: Dashboard Minimalism
The main FTC Dashboard tab shows ONLY:
- Robot pose on field
- Current superstructure state
- Game piece possession
- Active faults/alerts
- @Config variables for live tuning

Raw encoder values, PID errors, and current draws go on a separate debugging tab (not shown during matches).

### Rule E: Debounce All Critical Buttons
Mechanical buttons have bounce. Use `GamepadEx` which automatically debounces, or add explicit debouncing for critical actions like shooting or scoring.

## 3. Current Button Bindings (Example)

### Gamepad 1 - Primary Driver
| Input | Action |
|---|---|
| Left Stick | Drivetrain translation (field-relative, slew-limited) |
| Right Stick X | Drivetrain rotation (with gyro-lock when idle) |
| Left Trigger | Intake deploy + run → STOWED on release |
| Right Trigger | Shoot → SCORE → STOWED on release |
| A (Cross) | Slam intake (INTAKE_RUNNING) → STOWED on release |
| B (Circle) | Stationary SCORE → STOWED on release |
| X (Square) | UNJAM → STOWED on release |
| Y (Triangle) | SCORE → STOWED on release |
| DPad Right | Deploy intake (INTAKE_DOWN, no spin) |
| DPad Left | Retract intake (STOWED) |
| DPad Up | Manual elevator extend |
| DPad Down | Manual elevator retract |
| Left Bumper | Toggle slow mode |
| Right Bumper | Toggle field-relative drive |
| Back | Diagnostic hardware check |
| Start | Auto trajectory recording |

### Gamepad 2 - Copilot (Optional)
| Input | Action |
|---|---|
| Left Trigger | Manual feed (feeder + floor intake) |
| Right Trigger | Fixed target SCORE → STOWED on release |
| Right Bumper | Fixed target SCORE → STOWED on release |
| Left Bumper | Home all mechanisms |
| DPad Down | Reverse intake |
| X | Emergency drivetrain stop |
| A/B/X/Y | Preset scoring positions |

## 4. Adding New Bindings

1. Define the command binding in OpMode using `GamepadEx`:
   ```java
   gamepad1.getGamepadButton(GamepadKeys.Button.A)
       .whenPressed(superstructure.setAbsoluteState(SuperstructureState.INTAKE_RUNNING));
   ```

2. For toggle behavior, use toggle commands:
   ```java
   gamepad1.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
       .whenPressed(new ToggleSlowModeCommand(drive));
   ```

3. For press-and-hold with release action:
   ```java
   gamepad1.getGamepadButton(GamepadKeys.Button.RIGHT_TRIGGER)
       .whileHeld(new ShootCommand())
       .whenReleased(superstructure.setAbsoluteState(SuperstructureState.STOWED));
   ```

4. Add rumble feedback if the event is invisible to the driver:
   ```java
   gamepad1.rumble(250); // 250ms rumble pulse
   ```

5. Update the button map table in this skill's §3 section.
6. Add a test validating the command sequence in the relevant subsystem test.

## 5. Command API

### Standard Bindings
```java
// Press-and-hold with release action
gamepad1.getGamepadButton(GamepadKeys.Button.LEFT_TRIGGER)
    .whileHeld(new IntakeCommand())
    .whenReleased(superstructure.setAbsoluteState(SuperstructureState.STOWED));

// Toggle behavior
gamepad1.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
    .whenPressed(new ToggleFieldRelativeCommand(drive));

// One-shot action
gamepad1.getGamepadButton(GamepadKeys.Button.Y)
    .whenPressed(new ScoreCommand());
```

### Button Combinations
```java
// Use GamepadEx for button combos
new GamepadButtonCombo(gamepad1, GamepadKeys.Button.BACK, GamepadKeys.Button.START)
    .whenPressed(new DiagnosticCheckCommand());
```

## 6. Telemetry

### Driver Input Telemetry
- `Teleop/RawJoystickX` — Raw left stick X axis
- `Teleop/RawJoystickY` — Raw left stick Y axis
- `Teleop/RawJoystickOmega` — Raw right stick X axis
- `Teleop/PostDeadband` — Post-deadband values [x, y, omega]
- `Teleop/FieldRelSpeeds` — Field-relative speeds [vx, vy, omega]
- `Teleop/RobotRelSpeeds` — Robot-relative speeds [vx, vy, omega]
- `Teleop/SlowModeActive` — Boolean: slow mode enabled
- `Teleop/FieldRelativeActive` — Boolean: field-relative drive enabled

### LED Telemetry
- `LED/CurrentPattern` — Active LED pattern name
- `LED/FaultFlashActive` — Boolean: fault flash override active
- `LED/AllianceColor` — Current alliance color (RED/BLUE)

## 7. FTC-Specific Considerations

### Gamepad Differences from FRC
- **Xbox/PS4 controllers**: Common in FTC, different button mappings
- **Logitech gamepads**: Some teams prefer, different deadband characteristics
- **Touch interfaces**: Some teams use phone apps, rare but supported

### GamepadEx Features
- **Debouncing**: All buttons automatically debounced
- **Combos**: Multi-button combinations supported
- **Rumble**: Haptic feedback for game events
- **Deadband curves**: Configurable deadband shapes (linear, quadratic, cubic)

### Driver Station Display
- **Match timer**: Prominent, affects endgame strategy
- **Teleop duration**: 2:00 standard, shorter than FRC
- **Auto transition**: Manual transition (no auto mode like FRC)

### Camera Integration
- **Driver camera**: Typically on gamepad screen or DS
- **Pipeline switching**: Can bind to gamepad buttons
- **Recording**: Can record matches via DS, button to toggle
