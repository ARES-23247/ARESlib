package org.areslib.core;

/**
 * Centralized field boundary constants for the FTC competition environment.
 *
 * <p>PathPlanner operates in meters following WPILib coordinates (origin at bottom-right of blue
 * alliance wall). Vision systems (Limelight) operate in meters with origin at field center. Both
 * coordinate systems are defined here to ensure consistency across bounds-checking in {@code
 * FollowPathCommand} and {@code VisionSubsystem}.
 */
public final class FieldConstants {

  private FieldConstants() {} // Utility class — no instantiation

  // ========== Unit Conversion Constants ==========

  /** Conversion factor from meters to inches. */
  public static final double METERS_TO_INCHES = 1.0 / 0.0254;

  /** Conversion factor from inches to meters. */
  public static final double INCHES_TO_METERS = 0.0254;

  // ========== PathPlanner Coordinates (meters) ==========

  /** Field width/height in inches (12 feet = 144 inches). */
  public static final double FIELD_SIZE_INCHES = 144.0;

  /** Half-field size in inches — used for center-origin ↔ bottom-left-origin conversion. */
  public static final double HALF_FIELD_INCHES = FIELD_SIZE_INCHES / 2.0;

  /** Tolerance padding in inches for robot overhang beyond field walls. */
  public static final double WALL_PADDING_INCHES = 12.0;

  /** Minimum valid X/Y in WPILib/PathPlanner coordinates (with padding). */
  public static final double MIN_POSITION_INCHES = -WALL_PADDING_INCHES;

  /** Maximum valid X/Y in WPILib/PathPlanner coordinates (with padding). */
  public static final double MAX_POSITION_INCHES = FIELD_SIZE_INCHES + WALL_PADDING_INCHES;

  // ========== Vision Coordinates (meters, origin = field center) ==========

  /** Half-field size in meters (field is 3.6576m per side). */
  public static final double HALF_FIELD_METERS = FIELD_SIZE_INCHES * INCHES_TO_METERS / 2.0;

  /** Maximum valid absolute X/Y in vision coordinates (with generous buffer). */
  public static final double MAX_VISION_POSITION_METERS = 2.5;

  /** Maximum valid robot elevation before rejecting as ghost reflection. */
  public static final double MAX_ELEVATION_METERS = 0.5;

  // ========== Robot Physical Constants ==========

  /** Standard FTC robot frame size in meters (18 inches). */
  public static final double ROBOT_SIZE_METERS = 18.0 * INCHES_TO_METERS; // 0.4572m

  /** Wall collision friction coefficient for dyn4j inter-body contact. */
  public static final double SIM_WALL_FRICTION = 0.2;

  /** Wall collision restitution (bounce) coefficient for dyn4j. */
  public static final double SIM_WALL_RESTITUTION = 0.1;

  /** Synthetic linear damping applied to the robot body to model carpet friction. */
  public static final double SIM_LINEAR_DAMPING = 1.0;

  /** Synthetic angular damping applied to the robot body to model rotational floor friction. */
  public static final double SIM_ANGULAR_DAMPING = 1.0;

  // ========== Alliance Configuration ==========

  /**
   * Driver alliance selector for field-centric heading offset.
   *
   * <p>In FTC, the driver stations are positioned on the <b>sides</b> (±Y walls) of the field, not
   * the ends. The field-centric heading must be offset so that "forward" on the joystick
   * corresponds to "away from the driver" regardless of alliance.
   *
   * <ul>
   *   <li>{@link #RED} — Driver stands on -Y wall, faces +Y. Heading offset = +90°.
   *   <li>{@link #BLUE} — Driver stands on +Y wall, faces -Y. Heading offset = -90°.
   * </ul>
   */
  public enum Alliance {
    /** Red alliance: driver station on +Y wall, robot drives toward -Y. -90° heading offset. */
    RED(-Math.PI / 2.0),
    /** Blue alliance: driver station on -Y wall, robot drives toward +Y. +90° heading offset. */
    BLUE(Math.PI / 2.0);

    private final double headingOffsetRadians;

    Alliance(double headingOffsetRadians) {
      this.headingOffsetRadians = headingOffsetRadians;
    }

    /**
     * @return The heading offset in radians to apply when converting field-centric inputs.
     */
    public double getHeadingOffsetRadians() {
      return headingOffsetRadians;
    }
  }
}
