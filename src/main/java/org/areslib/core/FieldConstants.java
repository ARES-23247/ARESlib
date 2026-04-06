package org.areslib.core;

/**
 * Centralized field boundary constants for the FTC competition environment.
 * <p>
 * Pedro Pathing operates in inches with origin at field center.
 * Vision systems (Limelight) operate in meters with origin at field center.
 * Both coordinate systems are defined here to ensure consistency across
 * bounds-checking in {@code AresFollower} and {@code VisionSubsystem}.
 */
public final class FieldConstants {

    private FieldConstants() {} // Utility class — no instantiation

    // ========== Pedro Pathing Coordinates (inches, origin = field center) ==========

    /** Field width/height in inches (12 feet = 144 inches). */
    public static final double FIELD_SIZE_INCHES = 144.0;

    /** Half-field size in inches. */
    public static final double HALF_FIELD_INCHES = FIELD_SIZE_INCHES / 2.0;

    /** Tolerance padding in inches for robot overhang beyond field walls. */
    public static final double WALL_PADDING_INCHES = 12.0;

    /** Minimum valid X/Y in Pedro coordinates (bottom-left origin 0). */
    public static final double MIN_POSITION_INCHES = -WALL_PADDING_INCHES;

    /** Maximum valid X/Y in Pedro coordinates (bottom-left origin 0). */
    public static final double MAX_POSITION_INCHES = FIELD_SIZE_INCHES + WALL_PADDING_INCHES;

    // ========== Vision Coordinates (meters, origin = field center) ==========

    /** Half-field size in meters (field is 3.6576m per side). */
    public static final double HALF_FIELD_METERS = FIELD_SIZE_INCHES * 0.0254 / 2.0;

    /** Maximum valid absolute X/Y in vision coordinates (with generous buffer). */
    public static final double MAX_VISION_POSITION_METERS = 2.5;

    /** Maximum valid robot elevation before rejecting as ghost reflection. */
    public static final double MAX_ELEVATION_METERS = 0.5;
}
