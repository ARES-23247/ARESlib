package org.areslib.hardware.wrappers;

import org.areslib.hardware.faults.AresHardwareFaultInjector;
import org.areslib.hardware.faults.FaultMonitor;
import org.areslib.hardware.interfaces.ArrayLidarIO;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

public class ArrayLidarIOSim implements ArrayLidarIO, FaultMonitor {

  private final int resolution;
  private final java.util.function.Supplier<
          org.areslib.hardware.interfaces.OdometryIO.OdometryInputs>
      odometrySupplier;
  private final World<Body> world;
  private boolean faultTripped = false;

  private static final double FIELD_FOV_RADIANS = Math.toRadians(45.0);
  private static final double MAX_RANGE_METERS = 4.0;

  /**
   * Create a simulation configured for 16 (4x4) or 64 (8x8) resolution
   *
   * @param resolution the resolution to use
   * @param odometrySupplier the odometry supplier for the current position
   * @param world the simulation world
   */
  public ArrayLidarIOSim(
      int resolution,
      java.util.function.Supplier<org.areslib.hardware.interfaces.OdometryIO.OdometryInputs>
          odometrySupplier,
      World<Body> world) {
    this.resolution = resolution;
    this.odometrySupplier = odometrySupplier;
    this.world = world;
  }

  /**
   * Defaults to standard full 64 (8x8) resolution
   *
   * @param odometrySupplier the odometry supplier for the current position
   * @param world the simulation world
   */
  public ArrayLidarIOSim(
      java.util.function.Supplier<org.areslib.hardware.interfaces.OdometryIO.OdometryInputs>
          odometrySupplier,
      World<Body> world) {
    this.resolution = 64;
    this.odometrySupplier = odometrySupplier;
    this.world = world;
  }

  @Override
  public void updateInputs(ArrayLidarInputs inputs) {
    if (inputs.distanceZonesMm.length != resolution) {
      inputs.distanceZonesMm = new double[resolution];
    }

    if (org.areslib.core.AresRobot.isSimulation() && AresHardwareFaultInjector.simulateI2CCrash) {
      faultTripped = true;
      for (int i = 0; i < resolution; i++) {
        inputs.distanceZonesMm[i] = 0.0;
      }
      return;
    }

    org.areslib.hardware.interfaces.OdometryIO.OdometryInputs odo = null;
    if (odometrySupplier != null) {
      odo = odometrySupplier.get();
    }

    if (odo == null || world == null) {
      for (int i = 0; i < resolution; i++) {
        inputs.distanceZonesMm[i] = MAX_RANGE_METERS * 1000.0;
      }
      return;
    }

    double rx = odo.xMeters;
    double ry = odo.yMeters;
    double heading = odo.headingRadians;

    int gridDim = (int) Math.sqrt(resolution);
    double rayStepSize = 0.05; // 5cm check increments
    int maxSteps = (int) (MAX_RANGE_METERS / rayStepSize);

    for (int row = 0; row < gridDim; row++) {
      for (int col = 0; col < gridDim; col++) {
        double colFraction = (col + 0.5) / gridDim - 0.5;
        double yawOffset = colFraction * FIELD_FOV_RADIANS;

        double rowFraction = (row + 0.5) / gridDim - 0.5;
        double pitchOffset = rowFraction * FIELD_FOV_RADIANS;

        double rayAngle = heading + yawOffset;
        double cosRay = Math.cos(rayAngle);
        double sinRay = Math.sin(rayAngle);

        double dist2D = MAX_RANGE_METERS;

        // Simple Ray Marching
        for (int step = 1; step <= maxSteps; step++) {
          double currentDist = step * rayStepSize;
          double checkX = rx + (cosRay * currentDist);
          double checkY = ry + (sinRay * currentDist);

          Vector2 point = new Vector2(checkX, checkY);

          boolean hit = false;
          for (Body b : world.getBodies()) {
            // Skip the robot's own body (it sits exactly at the origin of the ray)
            if (b.getMass().getType() == org.dyn4j.geometry.MassType.NORMAL && currentDist < 0.2)
              continue;

            if (b.contains(point)) {
              hit = true;
              break;
            }
          }
          if (hit) {
            dist2D = currentDist;
            break;
          }
        }

        double dist3D_mm = (dist2D / Math.cos(pitchOffset)) * 1000.0;
        int index = (row * gridDim) + col;
        inputs.distanceZonesMm[index] = Math.min(dist3D_mm, MAX_RANGE_METERS * 1000.0);
      }
    }
  }

  @Override
  public boolean hasHardwareFault() {
    return faultTripped;
  }

  @Override
  public String getFaultMessage() {
    return "I2C COMMUNICATION FAILURE: Array LiDAR Array Not Responding.";
  }
}
