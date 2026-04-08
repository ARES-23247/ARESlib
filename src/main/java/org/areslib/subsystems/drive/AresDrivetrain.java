package org.areslib.subsystems.drive;

import org.areslib.command.Subsystem;

/**
 * Common interface for all ARESLib drivetrain subsystems. This guarantees that physics engines and
 * generic auto routines can seamlessly extract operational velocities regardless of whether the
 * physical robot is Swerve, Mecanum, or Differential.
 */
public interface AresDrivetrain extends Subsystem {
  /**
   * Gets the current commanded forward/backward linear velocity in meters per second.
   *
   * @return Commanded Vx (Robot-Centric)
   */
  double getCommandedVx();

  /**
   * Gets the current commanded strafe linear velocity in meters per second.
   *
   * @return Commanded Vy (Robot-Centric)
   */
  double getCommandedVy();

  /**
   * Gets the current commanded angular velocity in radians per second.
   *
   * @return Commanded Omega
   */
  double getCommandedOmega();
}
