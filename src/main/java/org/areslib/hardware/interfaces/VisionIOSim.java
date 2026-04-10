package org.areslib.hardware.interfaces;

import java.util.Random;
import org.areslib.core.simulation.AresPhysicsWorld;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

/**
 * Simulated implementation of {@link VisionIO} for offline AprilTag testing.
 *
 * <p>This class generates synthetic vision data based on the robot's known position in the dyn4j
 * physics world. It simulates:
 *
 * <ul>
 *   <li>2 AprilTag positions matching the DECODE (2025-2026) field layout (Tag 20: Blue Goal, Tag
 *       24: Red Goal)
 *   <li>Camera field-of-view cone filtering
 *   <li>Distance-based confidence (area) scaling
 *   <li>Gaussian noise on position measurements
 *   <li>Configurable latency injection
 *   <li>Range-limited detection (tags beyond maxRange are invisible)
 * </ul>
 *
 * <p>The robot's true pose is read from the first non-infinite-mass body in the physics world
 * (i.e., the robot chassis), and synthetic {@code botPose3d} quaternion arrays are generated in
 * AdvantageScope-compatible format.
 *
 * @see VisionIO
 * @see AresPhysicsWorld
 */
public class VisionIOSim implements VisionIO {

  /** Configuration for VisionIOSim behavior. */
  public static class Config {
    /** Camera horizontal field of view in radians. Default: 70° (typical Limelight). */
    public double cameraFovRadians = Math.toRadians(70.0);

    /** Maximum detection range in meters. Tags beyond this are invisible. */
    public double maxRangeMeters = 3.0;

    /** Standard deviation of Gaussian noise added to X/Y position (meters). */
    public double positionNoiseStdDev = 0.02;

    /** Standard deviation of Gaussian noise added to heading (radians). */
    public double headingNoiseStdDev = Math.toRadians(1.0);

    /** Simulated pipeline latency in milliseconds. */
    public double latencyMs = 30.0;

    /** Camera mounting angle offset from robot forward (radians). 0 = forward-facing. */
    public double cameraMountingAngleRad = 0.0;
  }

  /**
   * Represents a known AprilTag location on the field. Coordinates are in meters with origin at
   * field center (vision coordinate system).
   */
  @SuppressWarnings("unused")
  private static class AprilTagPose {
    final int id;
    final double x; // meters, field center origin
    final double y; // meters, field center origin
    final double z; // meters, height above floor
    final double yawRad; // tag facing direction

    AprilTagPose(int id, double x, double y, double z, double yawRad) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yawRad = yawRad;
    }
  }

  // ========== DECODE 2025-2026 AprilTag Layout ==========
  // DECODE has only 2 AprilTags usable for robot localization:
  //   - Tag 20: Blue Alliance Goal (corner at approx -1.5, -1.5)
  //   - Tag 24: Red Alliance Goal  (corner at approx +1.5, +1.5)
  //
  // The Obelisk (Tags 21-23) is placed OUTSIDE the field perimeter
  // and is NOT intended for navigation per the game manual.
  //
  // Coordinates are in meters, field-center origin, matching DecodeFieldSim.
  // Goal tags face inward toward field center.
  private static final AprilTagPose[] FIELD_TAGS = {
    // Blue Goal — opposite corner from Red, faces toward field center (+X, +Y direction)
    new AprilTagPose(20, -1.5, -1.5, 0.20, Math.PI / 4.0),

    // Red Goal — faces toward field center (-X, -Y direction)
    new AprilTagPose(24, 1.5, 1.5, 0.20, -3.0 * Math.PI / 4.0),
  };

  private final Config config;
  private final Random rng = new Random(42); // Deterministic seed for reproducibility

  /** Constructs a VisionIOSim with default configuration. */
  public VisionIOSim() {
    this(new Config());
  }

  /**
   * Constructs a VisionIOSim with custom configuration.
   *
   * @param config The simulation parameters (FOV, noise, latency, range).
   */
  public VisionIOSim(Config config) {
    this.config = config;
  }

  @Override
  public void updateInputs(VisionInputs inputs) {
    // Find the robot body in the physics world (first non-infinite body)
    Body robotBody = findRobotBody();
    if (robotBody == null) {
      inputs.hasTarget = false;
      return;
    }

    // Get robot's true pose from physics
    Vector2 robotPos = robotBody.getTransform().getTranslation();
    double robotHeading = robotBody.getTransform().getRotationAngle();
    double cameraHeading = robotHeading + config.cameraMountingAngleRad;

    // Find visible tags
    AprilTagPose bestTag = null;
    double bestDistance = Double.MAX_VALUE;
    int visibleCount = 0;

    for (AprilTagPose tag : FIELD_TAGS) {
      double dx = tag.x - robotPos.x;
      double dy = tag.y - robotPos.y;
      double distance = Math.sqrt(dx * dx + dy * dy);

      // Range check
      if (distance > config.maxRangeMeters) continue;

      // FOV check: is tag within the camera's horizontal cone?
      double angleToTag = Math.atan2(dy, dx);
      double relativeAngle = normalizeAngle(angleToTag - cameraHeading);
      if (Math.abs(relativeAngle) > config.cameraFovRadians / 2.0) continue;

      visibleCount++;
      if (distance < bestDistance) {
        bestDistance = distance;
        bestTag = tag;
      }
    }

    if (visibleCount == 0 || bestTag == null) {
      inputs.hasTarget = false;
      inputs.fiducialCount = 0;
      inputs.ta = 0.0;
      return;
    }

    // Generate synthetic vision data
    inputs.hasTarget = true;
    inputs.fiducialCount = visibleCount;

    // Target offsets (angle from camera center to best tag)
    double dxBest = bestTag.x - robotPos.x;
    double dyBest = bestTag.y - robotPos.y;
    double angleToTarget = Math.atan2(dyBest, dxBest);
    inputs.tx = Math.toDegrees(normalizeAngle(angleToTarget - cameraHeading));
    inputs.ty =
        Math.toDegrees(Math.atan2(bestTag.z - 0.2, bestDistance)); // Assume camera at 0.2m height

    // Target area scales inversely with distance squared (simulates perspective)
    // Normalized so a tag at 0.5m fills ~8% of image, at 2m fills ~0.5%
    inputs.ta = Math.min(10.0, 2.0 / (bestDistance * bestDistance));

    // Generate botPose3d with Gaussian noise — [x, y, z, w, qx, qy, qz]
    double noisyX = robotPos.x + rng.nextGaussian() * config.positionNoiseStdDev;
    double noisyY = robotPos.y + rng.nextGaussian() * config.positionNoiseStdDev;
    double noisyHeading = robotHeading + rng.nextGaussian() * config.headingNoiseStdDev;

    // Convert heading to quaternion (rotation around Z axis)
    double halfAngle = noisyHeading / 2.0;
    double qW = Math.cos(halfAngle);
    double qZ = Math.sin(halfAngle);

    inputs.botPose3d = new double[] {noisyX, noisyY, 0.0, qW, 0.0, 0.0, qZ};

    // MegaTag2 uses multi-tag solving — give it slightly less noise if multiple tags visible
    double mt2NoiseFactor = visibleCount > 1 ? 0.5 : 1.0;
    double mt2X = robotPos.x + rng.nextGaussian() * config.positionNoiseStdDev * mt2NoiseFactor;
    double mt2Y = robotPos.y + rng.nextGaussian() * config.positionNoiseStdDev * mt2NoiseFactor;
    double mt2Heading =
        robotHeading + rng.nextGaussian() * config.headingNoiseStdDev * mt2NoiseFactor;
    double mt2Half = mt2Heading / 2.0;
    inputs.botPoseMegaTag2 =
        new double[] {mt2X, mt2Y, 0.0, Math.cos(mt2Half), 0.0, 0.0, Math.sin(mt2Half)};

    // Latency
    inputs.latencyMs = config.latencyMs;
    inputs.pipelineIndex = 0;

    // Generate telemetry frustum to visualize camera line-of-sight in sim
    org.areslib.math.geometry.Pose3d cameraPose =
        new org.areslib.math.geometry.Pose3d(
            robotPos.x,
            robotPos.y,
            0.20,
            new org.areslib.math.geometry.Rotation3d(0, 0, cameraHeading));
    org.areslib.math.geometry.Pose3d[] frustumPoses =
        org.areslib.core.simulation.FrustumVisualizer.generateFrustum(
            cameraPose, Math.toDegrees(config.cameraFovRadians), 45.0, config.maxRangeMeters);
    inputs.cameraFovFrustum =
        org.areslib.core.simulation.FrustumVisualizer.toFlatArray(frustumPoses);
  }

  /**
   * Finds the robot chassis body in the physics world. The robot is identified as the first body
   * with NORMAL mass type (non-infinite, non-artifact). Artifacts are distinguished by having
   * circular fixtures and low mass.
   *
   * @return The robot Body, or null if not found.
   */
  private Body findRobotBody() {
    AresPhysicsWorld physicsWorld = AresPhysicsWorld.getInstance();
    for (Body body : physicsWorld.getWorld().getBodies()) {
      if (body.getMass().getType() != org.dyn4j.geometry.MassType.INFINITE) {
        // Check if it's the robot (rectangular fixture, high density) vs artifact (circle, low)
        if (!body.getFixtures().isEmpty()
            && body.getFixtures().get(0).getShape() instanceof org.dyn4j.geometry.Rectangle) {
          return body;
        }
      }
    }
    return null;
  }

  /** Normalizes an angle to the range [-PI, PI]. */
  private static double normalizeAngle(double angle) {
    while (angle > Math.PI) angle -= 2.0 * Math.PI;
    while (angle < -Math.PI) angle += 2.0 * Math.PI;
    return angle;
  }
}
