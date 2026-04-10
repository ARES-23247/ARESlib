---
name: areslib-hardware
description: Documents the ARESLib hardware abstraction layer — motor wrappers, sensor wrappers, odometry pods, and coprocessor interfaces. Use when adding new hardware devices, configuring sensor inputs, or building IOReal implementations.
---

# ARESLib Hardware Abstraction Layer


You are an expert hardware abstraction engineer for Team ARES. When adding new hardware devices, configuring sensor inputs, or building IOReal implementations, adhere strictly to the following guidelines.
The hardware layer wraps all FTC SDK device classes into framework-compatible interfaces. This isolates Android-only code so the rest of the framework runs on desktop JVM for simulation and testing.

## 1. Architecture

### Device Hierarchy
```
AresMotor          → DcMotorExWrapper (REV motors)
AresServo          → ServoWrapper, CRServoWrapper
AresEncoder        → RevEncoderWrapper, OctoQuadEncoderWrapper, SrsHubEncoderWrapper
AresAbsoluteEncoder→ AnalogAbsoluteEncoderWrapper
AresIMU            → RevIMUWrapper
AresDistanceSensor → RevDistanceSensorWrapper
AresColorSensor    → AresRevColorSensor, AresSrsColorSensor
AresAnalogSensor   → AnalogInputWrapper, AnalogSensorWrapper
AresDigitalSensor  → DigitalSensorWrapper
AresOdometry       → PinpointOdometryWrapper, OtosOdometryWrapper
```

### Coprocessor Layer
```
hardware/coprocessors/photon/
  PhotonCore.java        — Photon optimization framework
  PhotonLynxModule.java  — Optimized LynxModule command batching
```

### Sensor Hub Integration
```
AresOctoQuadDriver.java  — OctoQuad 8-channel encoder hub
SRSHub.java              — Smart Robot Server sensor hub
```

## 2. Key Abstraction: AresMotor

```java
// AresMotor wraps DcMotorEx with:
// - Automatic direction handling
// - Voltage compensation
// - Current monitoring for fault detection
// - Position/velocity reading

AresMotor motor = new AresMotor(hardwareMap, "motorName");
motor.setPower(0.5);           // Open-loop
motor.setVelocity(100.0);     // Closed-loop (ticks/sec)
double current = motor.getCurrent(); // For fault detection
```

## 3. Key Abstraction: AresEncoder

```java
// Three encoder implementations with identical interface:
AresEncoder encoder;

// REV Through-Bore via Control Hub
encoder = new RevEncoderWrapper(hardwareMap, "encoder0");

// OctoQuad USB encoder hub (8 channels)
encoder = new OctoQuadEncoderWrapper(octoQuad, channel);

// Smart Robot Server encoder
encoder = new SrsHubEncoderWrapper(srsHub, port);

// Same interface for all:
double position = encoder.getPosition();    // ticks
double velocity = encoder.getVelocity();    // ticks/sec
encoder.setDirection(Direction.REVERSE);
```

## 4. Odometry Pods

Two integrated odometry solutions:

### GoBILDA Pinpoint
```java
AresOdometry odo = new PinpointOdometryWrapper(hardwareMap, "pinpoint");
Pose2d pose = odo.getPosition();
odo.resetPosition(startPose);
```

### SparkFun OTOS (Optical Tracking)
```java
AresOdometry odo = new OtosOdometryWrapper(hardwareMap, "otos");
Pose2d pose = odo.getPosition();
```

## 5. Motor Physics Model (AresMotorModel)

Used by IOSim implementations to simulate realistic motor behavior:
```java
// Models voltage → torque → acceleration → velocity → position
AresMotorModel model = new AresMotorModel(
    stallTorqueNm,    // e.g., 3.36 for REV HD Hex
    freeSpeedRadPerS, // e.g., 594.4 for 6000 RPM
    stallCurrentA,    // e.g., 9.8
    freeCurrentA,     // e.g., 0.3
    gearRatio,        // e.g., 5.23
    massKg            // mechanism mass
);
model.update(voltage, dt); // Returns new velocity, position
```

## 6. Gamepad (AresGamepad)

Wraps SDK Gamepad with deadband, curve, and trigger support:
```java
AresGamepad gp = new AresGamepad(gamepad1);
double forward = gp.leftStickY();     // Deadbanded
boolean pressed = gp.aPressed();      // Rising edge
boolean held    = gp.aHeld();         // Continuous
```

## 7. Telemetry Logging

All wrappers log via `AresTelemetry`:
- Motor: power, velocity, current, position
- Encoder: position, velocity
- IMU: heading, angular velocity
- Odometry: x, y, heading

## 8. Adding New Hardware

When adding a new sensor or actuator:

1. **Create the abstract class** in `hardware/` (e.g., `AresNewSensor.java`)
2. **Create the wrapper** in `hardware/wrappers/` (e.g., `NewSensorWrapper.java`)
3. **If it's an IO source**, create `NewSensorIO.java` interface, `NewSensorIOReal.java`, `NewSensorIOSim.java`
4. **Register in AresHardwareManager** if it needs fault monitoring
5. **Add tests** using Mockito or IOSim pattern
6. **Create a matching skill** using `skill-authoring`

## 9. Common Pitfalls

- **Never import `com.qualcomm.*` in test files** — use IOSim or Mockito
- **Always null-check hardwareMap lookups** — the `AresHardwareManager` handles this
- **CRServo has no encoder** — don't try to read position from continuous rotation servos
- **OctoQuad requires firmware v3** — check with `AresOctoQuadDriver.getFirmwareVersion()`
