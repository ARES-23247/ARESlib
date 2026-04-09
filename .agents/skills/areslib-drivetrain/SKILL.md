---
name: areslib-drivetrain
description: Documents the ARESLib2 drivetrain subsystem layer — Swerve, Mecanum, and Differential drive implementations with IO abstraction. Use when configuring drive kinematics, adding new drive types, or tuning odometry.
---

# ARESLib2 Drivetrain Subsystems


You are an expert drivetrain engineer for Team ARES. When configuring drive kinematics or tuning odometry, adhere strictly to the following guidelines.
ARESLib2 supports three drivetrain types, all following the IO abstraction pattern for seamless sim/real switching.

## 1. Architecture

### Swerve Drive (4-module independent steering)
- `SwerveModuleIO.java` — Per-module interface (drive + steer motors)
- `SwerveModuleIOReal.java` — REV/CTRE hardware implementation
- `SwerveModuleIOSim.java` — dyn4j physics body with force application
- `SwerveDriveSubsystem.java` — Kinematics, odometry, field-centric control
- `SwerveDriveKinematics.java` — Module state ↔ chassis speed conversion
- `SwerveDriveOdometry.java` — Wheel-based pose tracking
- `SwerveDrivePoseEstimator.java` — Kalman-fused odometry + vision

### Mecanum Drive (holonomic, fixed wheels)
- `MecanumDriveIO.java` — Interface for 4 mecanum motors
- `MecanumDriveIOReal.java` / `MecanumDriveIOSim.java`
- `MecanumDriveSubsystem.java` — Kinematics + odometry
- `MecanumDriveKinematics.java` / `MecanumDriveOdometry.java`

### Differential Drive (tank/west coast)
- `DifferentialDriveIO.java` — Interface for left/right motor groups
- `DifferentialDriveIOReal.java` / `DifferentialDriveIOSim.java`
- `DifferentialDriveSubsystem.java` — Kinematics + odometry

### Common Interface
All drivetrains implement `AresDrivetrain.java`:
```java
public interface AresDrivetrain {
    void drive(ChassisSpeeds speeds);
    Pose2d getPose();
    void resetPose(Pose2d pose);
}
```

## 2. Coordinate System
**All drivetrains use WPILib convention:**
- X = forward (positive toward opponent wall)
- Y = left (positive toward your left when standing behind robot)
- θ = counter-clockwise positive

**PathPlanner and ARESLib2 both use WPILib convention**, so no coordinate conversion is needed.

For unit conversions (inches/mm to meters), use `CoordinateUtil`. See the `areslib-architecture` skill.

## 3. Key Configuration

### Swerve Module Positions (relative to robot center, meters)
```java
Translation2d[] modulePositions = {
    new Translation2d(+trackWidth/2, +wheelBase/2),  // FL
    new Translation2d(+trackWidth/2, -wheelBase/2),  // FR
    new Translation2d(-trackWidth/2, +wheelBase/2),  // BL
    new Translation2d(-trackWidth/2, -wheelBase/2),  // BR
};
```

### Motor & Encoder Config (set in IOReal constructors)
- Drive motor gear ratio, wheel radius, max speed
- Steer motor gear ratio, absolute encoder offset
- PID gains for drive velocity and steer position

## 4. Simulation (IOSim)

Each IOSim creates a `dyn4j` rigid body:
```java
// In SwerveDriveSubsystem constructor (sim mode)
simChassis = new Body();
simChassis.addFixture(Geometry.createRectangle(chassisWidthM, chassisLengthM));
simChassis.setMass(MassType.NORMAL);
AresPhysicsWorld.getInstance().addBody(simChassis);
```

The physics world handles wall collisions automatically. Forces are applied per-module based on desired speeds.

## 5. Odometry & Pose Estimation

Two levels of pose tracking:
1. **Odometry** — wheel-only, no drift correction
2. **PoseEstimator** — Kalman filter fusing wheels + vision

```java
// Inject vision measurement
poseEstimator.addVisionMeasurement(visionPose, timestampSeconds);
```

## 6. TeleOp Integration

Example TeleOp patterns exist in `examples/`:
- `SwerveRevOctoQuadTeleOp` — Swerve with REV + OctoQuad
- `MecanumStandardTeleOp` — Basic mecanum control
- `DifferentialAdvancedTeleOp` — Tank with heading lock

## 7. Testing

```java
@Test
void testFieldCentricTransform() {
    // Create sim modules + subsystem
    SwerveModuleIOSim[] modules = new SwerveModuleIOSim[4];
    // ... setup ...
    SwerveDriveSubsystem drive = new SwerveDriveSubsystem(modules);
    
    // Command forward in field frame
    drive.drive(new ChassisSpeeds(1.0, 0.0, 0.0));
    drive.periodic();
    
    // Verify module states
    // ...
}
```

## 8. Common Pitfalls

- **Always use WPILib convention** — X-forward, Y-left, θ CCW+
- **Always reset odometry** at autonomous start
- **Module order matters** — FL, FR, BL, BR consistently
- **Wheel desaturation** — if any module exceeds max speed, all modules scale proportionally
