package org.areslib.hardware.faults;

/**
 * A simulation-only tool that allows unit tests (or manual telemetry overrides) to artificially
 * inject catastrophic hardware failures, testing the RobotHealthTracker and the robot's physical
 * drift response.
 */
public class AresHardwareFaultInjector {

  public static boolean simulateI2CCrash = false;
  public static boolean simulateEncoderShatter = false;

  /** Activating this will cause the Odometry simulated IO to instantly return tracking zeros. */
  public static void triggerEncoderShatter() {
    simulateEncoderShatter = true;
  }

  /** Activating this will cause the Lidar simulated IO to instantly stop returning raycasts. */
  public static void triggerI2CCrash() {
    simulateI2CCrash = true;
  }

  /** Resets all simulated faults. */
  public static void reset() {
    simulateI2CCrash = false;
    simulateEncoderShatter = false;
  }
}
