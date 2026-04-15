package org.areslib.subsystems.vision;

import org.areslib.command.SubsystemBase;
import org.areslib.hardware.interfaces.VisionIO;
import org.areslib.telemetry.AresAutoLogger;

/**
 * Core vision subsystem that wraps a {@link VisionIO} interface to provide validated
 * AprilTag-derived pose estimates and configurable confidence scoring.
 *
 * <p>This subsystem performs three levels of sanity checking on incoming vision data:
 *
 * <ol>
 *   <li>Z-elevation ghost rejection (robot floating/underground)
 *   <li>Field boundary validation (robot outside legal play area)
 *   <li>Target area gating (target too small to be reliable)
 * </ol>
 *
 * @see VisionIO
 * @see AresSensorFusionSubsystem
 */
public class AresVisionSubsystem extends SubsystemBase {

  private final VisionIO io;
  private final VisionIO.VisionInputs inputs = new VisionIO.VisionInputs();

  private final double maxTrustAreaPercent;

  private static final double CLOSE_DISTANCE = 2.0;
  private static final double FAR_DISTANCE = 6.0;
  private static final double MIN_WEIGHT = 0.1;
  private static final double MAX_TAG_DISTANCE = 3.0;

  /** Pre-allocated cache for {@link #getVisionMeasurementStdDevs}. */
  private final double[] stdDevCache = new double[3];

  private java.util.function.Supplier<org.areslib.math.geometry.Rotation2d> yawSupplier = null;

  private double calculateDistanceWeight(double distance) {
    double baseWeight =
        MIN_WEIGHT
            + (1.0 - MIN_WEIGHT)
                * (1.0
                    / (Math.max(
                        1e-3,
                        1.0
                            + Math.exp(
                                (distance - CLOSE_DISTANCE)
                                    / (FAR_DISTANCE - CLOSE_DISTANCE)
                                    * 4.0))));

    if (distance > FAR_DISTANCE) {
      double farDistancePenalty = Math.pow(0.5, (distance - FAR_DISTANCE) / 2.0);
      return baseWeight * farDistancePenalty;
    }

    return baseWeight;
  }

  /**
   * Constructs a core vision subsystem, parameterized for specific game or camera heuristics.
   *
   * @param io The hardware IO interface (e.g., LimelightVisionWrapper).
   * @param minTargetAreaPercent Absolute minimum target size (% of image) to be considered valid.
   * @param maxTrustAreaPercent Target size (% of image) corresponding to 100% confidence.
   */
  public AresVisionSubsystem(VisionIO io, double maxTrustAreaPercent) {
    this.io = io;
    this.maxTrustAreaPercent = maxTrustAreaPercent;
  }

  @Override
  public void periodic() {
    if (yawSupplier != null) {
      io.updateRobotOrientation(yawSupplier.get());
    }

    // Automatically fetch network tables or driver inputs
    io.updateInputs(inputs);

    // This line performs magic: It automatically diffs the fields within 'inputs'
    // and pushes the changes across network tables into AdvantageScope for logging.
    AresAutoLogger.processInputs("Vision", inputs);
  }

  /**
   * Configures a provider to continuously feed the exact WPILib/ARESLib Field-Centric robot yaw
   * into the vision system over USB for Megatag2 processing.
   *
   * @param yawSupplier A method reference, e.g., {@code () -> swerve.getPose().getRotation()}
   */
  public void setYawSupplier(
      java.util.function.Supplier<org.areslib.math.geometry.Rotation2d> yawSupplier) {
    this.yawSupplier = yawSupplier;
  }

  public boolean hasTarget() {
    return inputs.hasTarget;
  }

  /**
   * Gets the horizontal offset from the crosshair.
   *
   * @return Horizontal offset from crosshair to target (Tx) in degrees.
   */
  public double getTargetXOffset() {
    return inputs.tx;
  }

  /**
   * Gets the vertical offset from the crosshair.
   *
   * @return Vertical offset from crosshair to target (Ty) in degrees.
   */
  public double getTargetYOffset() {
    return inputs.ty;
  }

  /**
   * Gets the target area as a percentage.
   *
   * @return Target Area (Ta) in percent of image.
   */
  public double getTargetArea() {
    return inputs.ta;
  }

  /**
   * Calculates trust coefficient dynamically based on AprilTag latency and visible surface area.
   *
   * <p><b>IMPORTANT: Coordinate Frame Assumption</b>: This subsystem and its consumer {@link
   * AresSensorFusionSubsystem} assume the vision system's botPose3d X/Y axes are pre-aligned to the
   * PathPlanner frame (no axis swap). If using a WPILib-standard Limelight with MegaTag2, this is
   * typically true. If using a custom PhotonVision setup where X=forward and Y=left, the axes must
   * be swapped before fusion blending.
   *
   * @return Field-centric 2D pose estimated by the vision system. Null if target isn't trustworthy.
   */
  public org.areslib.math.geometry.Pose2d getEstimatedGlobalPose() {
    if (!inputs.hasTarget) return null;

    // Sanity Check 1: Is the robot floating?
    // If the vision system thinks the robot's center is floating above the field
    // or buried deep underground, it is a ghost reflection.
    double zElevationMeters = inputs.botPose3d[2];
    if (Math.abs(zElevationMeters) > org.areslib.core.FieldConstants.MAX_ELEVATION_METERS) {
      return null;
    }

    // Sanity Check 2: Are we physically outside the FTC Field?
    // Uses centralized field constants for consistent bounds across the codebase.
    double xMeters = inputs.botPose3d[0];
    double yMeters = inputs.botPose3d[1];
    if (Math.abs(xMeters) > org.areslib.core.FieldConstants.MAX_VISION_POSITION_METERS
        || Math.abs(yMeters) > org.areslib.core.FieldConstants.MAX_VISION_POSITION_METERS) {
      return null;
    }

    // Quaternion yaw extraction.
    // IMPORTANT: This assumes botPose3d layout is [x, y, z, w, qx, qy, qz] (Hamilton convention).
    // If your vision system outputs [x, y, z, qx, qy, qz, w] (JPL convention), swap indices:
    //   w = botPose3d[6], qX = botPose3d[3], qY = botPose3d[4], qZ = botPose3d[5]
    double quaternionW = inputs.botPose3d[3];
    double qX = inputs.botPose3d[4];
    double qY = inputs.botPose3d[5];
    double qZ = inputs.botPose3d[6];

    // Validate quaternion is normalized — a non-unit quaternion indicates wrong index mapping
    double qNormSq = quaternionW * quaternionW + qX * qX + qY * qY + qZ * qZ;
    if (qNormSq < 0.5 || qNormSq > 1.5) {
      return null; // Quaternion is wildly non-unit, reject this measurement
    }

    double yawRadians =
        Math.atan2(2.0 * (quaternionW * qZ + qX * qY), 1.0 - 2.0 * (qY * qY + qZ * qZ));

    return new org.areslib.math.geometry.Pose2d(
        xMeters, yMeters, new org.areslib.math.geometry.Rotation2d(yawRadians));
  }

  /**
   * Calculates dynamic standard deviations for the vision measurement (X, Y, Theta) based on Elite
   * team heuristics (Team 5940 B.R.E.A.D. and Team 254).
   *
   * @param currentAngularVelocityRadPerSec The current rotational velocity of the drivetrain.
   * @return A double array [xStdDev, yStdDev, thetaStdDev] or null if the measurement should be
   *     rejected.
   */
  public double[] getVisionMeasurementStdDevs(double currentAngularVelocityRadPerSec) {
    if (!inputs.hasTarget) return null;

    // Reject poses if angular velocity is too high (Team 254 Pattern: motion blur / skew destroys
    // PnP)
    if (Math.abs(currentAngularVelocityRadPerSec) > 1.5) { // ~85 deg/sec
      stdDevCache[0] = Double.POSITIVE_INFINITY;
      stdDevCache[1] = Double.POSITIVE_INFINITY;
      stdDevCache[2] = Double.POSITIVE_INFINITY;
      return stdDevCache;
    }

    // Elite Scaling Pattern (Team 5940 B.R.E.A.D.)
    double distance = inputs.avgTagDistanceMeters;
    // If distance wasn't provided by the IO layer, approximate from Tag Area inversely
    if (distance <= 0.01) {
      distance =
          (inputs.ta > org.areslib.math.MathUtil.EPSILON)
              ? 3.0 / Math.sqrt(inputs.ta)
              : 5.0; // Rough heuristic fallback
    }

    double distanceWeight = calculateDistanceWeight(distance);
    if (org.areslib.math.MathUtil.epsilonCheck(distanceWeight)) {
      distanceWeight = org.areslib.math.MathUtil.EPSILON;
    }

    if (inputs.fiducialCount >= 2) {
      // Highly trusted multi-tag
      stdDevCache[0] = 0.2 / distanceWeight;
      stdDevCache[1] = 0.2 / distanceWeight;
      stdDevCache[2] = 0.1 / distanceWeight;
      return stdDevCache;
    } else if (inputs.fiducialCount == 1) {
      // Single tag disambiguation & distance falloff
      if (inputs.minTagAmbiguity > 0.15) {
        return null; // Reject high ambiguity single tags
      }

      if (distance > MAX_TAG_DISTANCE) {
        return null; // B.R.E.A.D. 2025: Strict 3.0 meter cutoff for single tags
      }

      // B.R.E.A.D. Distance-Squared Polynomial fallback
      double xyStd = (0.03 * Math.pow(distance, 2)) / distanceWeight;
      double thetaStd = (0.05 * Math.pow(distance, 2)) / distanceWeight;
      stdDevCache[0] = xyStd;
      stdDevCache[1] = xyStd;
      stdDevCache[2] = thetaStd;
      return stdDevCache;
    }

    // Fallback based on area (Old ARESLib heuristic) if fiducialCount isn't populated
    if (inputs.ta < org.areslib.math.MathUtil.EPSILON) return null;
    if (org.areslib.math.MathUtil.epsilonCheck(maxTrustAreaPercent)) return null;
    double confidence = Math.min(inputs.ta / maxTrustAreaPercent, 1.0);
    double fallbackStd = (1.0 - confidence) * 2.0 + 0.1;
    stdDevCache[0] = fallbackStd;
    stdDevCache[1] = fallbackStd;
    stdDevCache[2] = fallbackStd * 2.0;
    return stdDevCache;
  }

  public void setPipeline(int index) {
    io.setPipeline(index);
  }
}
