package org.areslib.faults;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import java.util.ArrayList;
import java.util.List;
import org.areslib.telemetry.AresAutoLogger;

/**
 * Pre-match hardware diagnostics that automatically scans and verifies all registered hardware
 * devices. Designed to be called during {@code robotInit()} before {@code waitForStart()}.
 *
 * <p>Reports results through the {@link AresFaultManager} alert system and telemetry, catching
 * wiring issues, disconnected motors, and unresponsive sensors before a match starts.
 *
 * <pre>{@code
 * // In robotInit():
 * AresDiagnostics.runPreMatchCheck(hardwareMap);
 * }</pre>
 */
public final class AresDiagnostics {

  private AresDiagnostics() {
    throw new AssertionError("Utility class");
  }

  private static final AresAlert diagnosticAlert =
      new AresAlert("Pre-match diagnostics FAILED", AresAlert.AlertType.ERROR);

  /**
   * Scans all motors, servos, and IMU devices in the hardware map, verifying communication.
   *
   * <p>For each device:
   *
   * <ul>
   *   <li><b>DcMotorEx:</b> Reads current position to confirm encoder communication.
   *   <li><b>Servo:</b> Reads current position to confirm PWM communication.
   *   <li><b>IMU:</b> Attempts to read angular orientation.
   * </ul>
   *
   * @param hardwareMap The robot's hardware map from the OpMode.
   * @return True if all devices passed, false if any failed.
   */
  public static boolean runPreMatchCheck(HardwareMap hardwareMap) {
    List<String> passed = new ArrayList<>();
    List<String> failed = new ArrayList<>();

    // Check all motors
    for (DcMotorEx motor : hardwareMap.getAll(DcMotorEx.class)) {
      String name = getDeviceName(hardwareMap, motor);
      try {
        motor.getCurrentPosition(); // Forces I2C communication
        passed.add("Motor: " + name);
      } catch (Exception e) {
        failed.add("Motor: " + name + " (" + e.getMessage() + ")");
      }
    }

    // Check all servos
    for (Servo servo : hardwareMap.getAll(Servo.class)) {
      String name = getDeviceName(hardwareMap, servo);
      try {
        servo.getPosition(); // Forces PWM read
        passed.add("Servo: " + name);
      } catch (Exception e) {
        failed.add("Servo: " + name + " (" + e.getMessage() + ")");
      }
    }

    // Check IMUs
    for (com.qualcomm.robotcore.hardware.IMU imu :
        hardwareMap.getAll(com.qualcomm.robotcore.hardware.IMU.class)) {
      String name = getDeviceName(hardwareMap, imu);
      try {
        imu.getRobotYawPitchRollAngles(); // Forces I2C read
        passed.add("IMU: " + name);
      } catch (Exception e) {
        failed.add("IMU: " + name + " (" + e.getMessage() + ")");
      }
    }

    // Report results
    int total = passed.size() + failed.size();
    boolean allPassed = failed.isEmpty();

    if (allPassed) {
      AresAutoLogger.recordOutput(
          "Diagnostics/Status", String.format("PASS %d/%d devices OK", total, total));
    } else {
      AresAutoLogger.recordOutput(
          "Diagnostics/Status", String.format("FAIL %d/%d devices FAILED", failed.size(), total));
    }

    // Log individual results
    for (String p : passed) {
      AresAutoLogger.recordOutput("Diagnostics/Passed/" + p, "OK");
    }
    for (String f : failed) {
      AresAutoLogger.recordOutput("Diagnostics/Failed/" + f, "FAIL");
    }

    diagnosticAlert.set(!allPassed);

    return allPassed;
  }

  /** Attempts to resolve the user-configured name of a hardware device. */
  private static String getDeviceName(HardwareMap hardwareMap, Object device) {
    try {
      for (HardwareMap.DeviceMapping<?> mapping : hardwareMap.allDeviceMappings) {
        for (java.util.Map.Entry<String, ?> entry : mapping.entrySet()) {
          if (entry.getValue() == device) {
            return entry.getKey();
          }
        }
      }
    } catch (Exception ignored) {
      Thread.yield();
    }
    return device.getClass().getSimpleName();
  }
}
