---
name: areslib-vision
description: Helps write and configure Vision pipelines (PhotonVision, Limelight) inside ARESLib. Use when injecting AprilTag 3D Odometry poses, configuring `VisionIO` interfaces, or mapping vision data properly into the AdvantageScope 3D field layout.
---

# ARESLib Vision Architecture


You are an expert vision engineer for Team ARES. When configuring AprilTag pipelines, injecting vision poses into odometry, or setting up VisionIO interfaces, adhere strictly to the following guidelines.
The `ARESLib` framework natively supports multi-camera sensor fusion, but strict interface implementation rules must be followed to ensure AdvantageKit logs the 3D poses natively without breaking the determinism of the simulation.

## 1. IO Abstraction Requirement
All vision target generation MUST be hidden behind a `VisionIO` layer. Three implementations exist:
- **`VisionIO`** (`org.areslib.hardware.interfaces.VisionIO`) — Base interface
- **`LimelightVisionWrapper`** (`org.areslib.hardware.wrappers.LimelightVisionWrapper`) — Real Limelight hardware
- **`ArrayVisionIOSim`** (`org.areslib.hardware.wrappers.ArrayVisionIOSim`) — Simulated vision for desktop testing

## 2. Actual `VisionInputs` Data Structure (from `VisionIO.java`)
The inputs class implements `AresLoggableInputs` and contains these exact fields:

```java
class VisionInputs implements AresLoggableInputs {
    public boolean hasTarget = false;           // True if a valid target is visible
    public double tx = 0.0;                     // Target X offset (degrees)
    public double ty = 0.0;                     // Target Y offset (degrees)
    public double ta = 0.0;                     // Target area (% of image)
    public double[] botPose3d = new double[7];  // Primary robot pose [x,y,z,w,i,j,k]
    public double[] botPoseMegaTag2 = new double[7]; // Secondary MegaTag2 pose
    public double latencyMs = 0.0;              // Pipeline latency in ms
    public int pipelineIndex = 0;               // Active pipeline index
    public int fiducialCount = 0;               // Number of visible AprilTags
    public double minTagAmbiguity = 0.0;        // Lowest ambiguity (0.0 to 1.0)
    public double avgTagDistanceMeters = 0.0;   // Average distance to tags
    public boolean isMegatag2 = false;          // True if using MegaTag 2.0
    public double[] rawCameraPoses = new double[0]; // Packed multi-camera poses
}
```

**Critical format note:** `botPose3d` is a 7-element quaternion array `[x, y, z, w, i, j, k]` — NOT Euler angles. See `areslib-architecture` skill for quaternion conversion formulas.

## 3. Sensor Fusion (AresSensorFusionSubsystem & Pose Estimators)

The `AresVisionSubsystem` dynamically calculates WPILib-compatible standard deviations using Elite FRC team heuristics (Team 5940 B.R.E.A.D. and Team 254).
It no longer uses simple `lerp()` blending. All math relies on the `SwerveDrivePoseEstimator` or `AresHardwarePoseEstimator` Kalman filters.

```java
// 1. Get the exact angular velocity of the robot (from ChassisSpeeds or IMU)
double currentOmegaRadPerSec = driveSubsystem.getCommandedOmega();

// 2. Compute dynamic standard deviations
// (Auto-rejects poses during high-omega motion blur, scales by distance/ambiguity)
double[] visionStdDevs = visionSubsystem.getVisionMeasurementStdDevs(currentOmegaRadPerSec);

// 3. Apply into the Pose Estimator (handles time-rollback latency compensation)
if (visionStdDevs != null) {
    Pose2d visionPose = visionSubsystem.getEstimatedGlobalPose();
    if (visionPose != null) {
        // Assume pipeline subtracts latency from current FPGA timestamp
        double mapTimestamp = Timer.getFPGATimestamp() - (visionInputs.latencyMs / 1000.0);
        poseEstimator.addVisionMeasurement(visionPose, mapTimestamp, visionStdDevs);
    }
}
```

**Key rules:**
- Poses are rejected (`visionStdDevs == null`) if there's high ambiguity (`>0.15`).
- Poses are infinity-weighted (ignored) if angular velocity is `> 1.5 rad/s`.
- Multi-tag gives strong confidence. Single-tag uses a quadratic distance scalar (`C * distance^2`).
- **NEVER** overwrite odometry directly — always fuse via the `<T>PoseEstimator`.

## 4. Multi-Camera Fusion Pattern
When using multiple Limelights, the `LimelightVisionWrapper` employs **Winner-Takes-All** logic:
- Each camera's `Target Area (ta)` is compared
- The camera with the LARGEST `ta` (closest to target) provides the primary `botPose3d`
- ALL cameras' poses are packed into `rawCameraPoses` as sequential 7-element arrays for ghost visualization in AdvantageScope

## 5. AdvantageScope 3D Rendering (MCP)
When setting up `layout.json` tabs via MCP:
- Inject `botPose3d` into a `Field3d` tab as `log_type: "Pose3d"`
- Inject `rawCameraPoses` as `log_type: "Pose3d[]"` to render multi-camera ghosts
- Ensure the 7-element quaternion format is used (NOT 6-element Euler)

## 6. Available Methods on `VisionIO`
```java
void updateInputs(VisionInputs inputs);  // Populate inputs from hardware
void setPipeline(int index);             // Switch vision pipeline
```

## 7. Anti-Patterns

### Don't: Use tx/ty for 3D pose estimation
```java
// BAD — tx/ty are 2D angular offsets, not positions
double robotX = visionInputs.tx;

// GOOD — use the full 3D pose array
double robotX = visionInputs.botPose3d[0]; // X in meters
```

### Don't: Ignore latency compensation
```java
// BAD — stale vision data applied at current timestamp
poseEstimator.addVisionMeasurement(pose, Timer.getFPGATimestamp(), stdDevs);

// GOOD — subtract pipeline latency
poseEstimator.addVisionMeasurement(pose,
    Timer.getFPGATimestamp() - (visionInputs.latencyMs / 1000.0), stdDevs);
```

## Testing

```java
@Test
void testVisionIOSimDetectsTag() {
    AresPhysicsWorld.getInstance().reset();
    Body robot = new Body();
    robot.addFixture(Geometry.createRectangle(0.4572, 0.4572));
    robot.setMass(MassType.NORMAL);
    AresPhysicsWorld.getInstance().addBody(robot);

    VisionIOSim sim = new VisionIOSim();
    VisionIO.VisionInputs inputs = new VisionIO.VisionInputs();
    sim.updateInputs(inputs);

    assertTrue(inputs.hasTarget, "Should detect at least one AprilTag");
}
```
