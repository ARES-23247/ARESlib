package org.areslib.subsystems.drive;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style hardware abstraction interface for a Mecanum Drivetrain.
 *
 * <p>Defines the IO layer for a four-motor omnidirectional holonomic drive.
 */
public interface MecanumDriveIO {

  /** Loggable data object containing the state of all four mecanum wheel pods. */
  class MecanumDriveInputs implements AresLoggableInputs {
    /** Integrated encoder position of the front-left motor in meters. */
    public double frontLeftPositionMeters = 0.0;

    /** Integrated encoder velocity of the front-left motor in meters per second. */
    public double frontLeftVelocityMps = 0.0;

    /** Integrated encoder position of the front-right motor in meters. */
    public double frontRightPositionMeters = 0.0;

    /** Integrated encoder velocity of the front-right motor in meters per second. */
    public double frontRightVelocityMps = 0.0;

    /** Integrated encoder position of the rear-left motor in meters. */
    public double rearLeftPositionMeters = 0.0;

    /** Integrated encoder velocity of the rear-left motor in meters per second. */
    public double rearLeftVelocityMps = 0.0;

    /** Integrated encoder position of the rear-right motor in meters. */
    public double rearRightPositionMeters = 0.0;

    /** Integrated encoder velocity of the rear-right motor in meters per second. */
    public double rearRightVelocityMps = 0.0;
  }

  /**
   * Retrieves the latest sensor data from the backend implementations (real or sim) and packs them
   * into the supplied {@link MecanumDriveInputs} object.
   *
   * @param inputs The input object reference to be populated with current state.
   */
  default void updateInputs(MecanumDriveInputs inputs) {}

  /**
   * Commands the four motors of the mecanum drivetrain using raw voltage.
   *
   * @param frontLeft Target output voltage for the front-left motor.
   * @param frontRight Target output voltage for the front-right motor.
   * @param rearLeft Target output voltage for the rear-left motor.
   * @param rearRight Target output voltage for the rear-right motor.
   */
  default void setVoltages(
      double frontLeft, double frontRight, double rearLeft, double rearRight) {}
}
