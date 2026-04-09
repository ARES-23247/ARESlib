package org.areslib.hardware.interfaces;

import org.areslib.math.geometry.Rotation2d;

/** Standardizes IMU fetching across native BNO055, generic REV Hub IMUs, or GoBilda Pinpoint. */
public interface AresIMU {

  /**
   * Returns continuous yaw angle wrapped as a Rotation2d.
   *
   * @return Continuous yaw angle wrapped as a Rotation2d.
   */
  Rotation2d getYaw();
}
