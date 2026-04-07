---
name: areslib-vision
description: Helps write and configure Vision pipelines (PhotonVision, Limelight) inside ARESLib2. Use when injecting AprilTag 3D Odometry poses, configuring `VisionIO` interfaces, or mapping vision data properly into the AdvantageScope 3D field layout.
---

# ARESLib2 Vision Architecture


You are a vision engineer for Team ARES. When configuring AprilTag pipelines, injecting vision poses into odometry, or setting up VisionIO interfaces, adhere strictly to the following guidelines.
The `ARESLib2` framework natively supports multi-camera sensor fusion, but strict interface implementation rules must be followed to ensure AdvantageKit logs the 3D poses natively without breaking the determinism of the simulation.

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
    public double[] rawCameraPoses = new double[0]; // Multi-camera packed poses (N*7)
}
```

**Critical format note:** `botPose3d` is a 7-element quaternion array `[x, y, z, w, i, j, k]` — NOT Euler angles. See `areslib-architecture` skill for quaternion conversion formulas.

## 3. Sensor Fusion (AresSensorFusionSubsystem)

The `AresSensorFusionSubsystem` handles the blending of vision and odometry poses. All math is centralized in `CoordinateUtil`:

```java
// Coordinate conversion: vision (meters, center origin) -> WPILib (meters, center origin)
double visionXInches = CoordinateUtil.centerMetersToBottomLeftInches(visionPose.getX());
double visionYInches = CoordinateUtil.centerMetersToBottomLeftInches(visionPose.getY());

// Kalman gain: higher confidence = more trust in vision
double kalmanGain = CoordinateUtil.computeVisionKalmanGain(confidence);
double blendWeight = Math.min(kalmanGain, maxVisionTrustFactor); // Cap per-tick jump

// Blend position and heading
double fusedX = CoordinateUtil.lerp(odomX, visionXInches, blendWeight);
double fusedY = CoordinateUtil.lerp(odomY, visionYInches, blendWeight);
double fusedHeading = CoordinateUtil.shortestAngleLerp(odomHeading, visionHeading, blendWeight);
```

**Key rules:**
- Confidence below 0.05 is rejected entirely (no correction applied)
- The blend weight is capped by `maxVisionTrustFactor` to prevent teleportation
- `shortestAngleLerp` prevents 360-degree wraparound snapping
- **NEVER** overwrite odometry directly — always blend via fusion

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
