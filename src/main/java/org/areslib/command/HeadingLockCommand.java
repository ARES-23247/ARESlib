package org.areslib.command;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import org.areslib.math.controller.PIDController;

/**
 * A command that stabilizes the robot's heading when the driver isn't actively rotating.
 *
 * <p>When the driver releases the rotation stick, the command locks the current heading and applies
 * PID correction to prevent drift during strafing. When the driver starts rotating again, the lock
 * is released and raw input passes through.
 *
 * <p>This is drivetrain-agnostic — it works with any drive type by accepting suppliers and
 * consumers.
 *
 * <pre>{@code
 * new HeadingLockCommand(
 *     () -> -gamepad1.right_stick_x,  // rotation input
 *     () -> imu.getYaw(),             // current heading (radians)
 *     omega -> driveOmega = omega,    // omega output
 *     0.5, 0.0, 0.02,                // PID gains
 *     0.05                            // deadband
 * );
 * }</pre>
 */
public class HeadingLockCommand extends Command {

  private final DoubleSupplier rotationInput;
  private final DoubleSupplier gyroHeading;
  private final DoubleConsumer omegaOutput;
  private final PIDController headingPID;
  private final double deadband;

  private boolean isLocked = false;
  private double lockedHeading = 0.0;
  private int idleCount = 0;
  private static final int IDLE_LOOPS_TO_LOCK = 5; // 100ms at 50Hz

  /**
   * Constructs a HeadingLockCommand.
   *
   * @param rotationInput Supplier for the driver's rotation joystick input (-1 to 1).
   * @param gyroHeading Supplier for the current gyro heading in radians.
   * @param omegaOutput Consumer that receives the corrected omega output.
   * @param kP Heading PID proportional gain.
   * @param kI Heading PID integral gain.
   * @param kD Heading PID derivative gain.
   * @param deadband The rotation input deadband below which heading lock engages.
   */
  public HeadingLockCommand(
      DoubleSupplier rotationInput,
      DoubleSupplier gyroHeading,
      DoubleConsumer omegaOutput,
      double kP,
      double kI,
      double kD,
      double deadband) {
    this.rotationInput = rotationInput;
    this.gyroHeading = gyroHeading;
    this.omegaOutput = omegaOutput;
    headingPID = new PIDController(kP, kI, kD);
    headingPID.enableContinuousInput(-Math.PI, Math.PI);
    this.deadband = deadband;
  }

  @Override
  public void initialize() {
    isLocked = false;
    idleCount = 0;
    headingPID.reset();
  }

  @Override
  public void execute() {
    double rotInput = rotationInput.getAsDouble();
    double currentHeading = gyroHeading.getAsDouble();

    if (Math.abs(rotInput) > deadband) {
      // Driver is actively rotating — pass through raw input
      isLocked = false;
      idleCount = 0;
      headingPID.reset();
      omegaOutput.accept(rotInput);
    } else {
      idleCount++;

      if (!isLocked && idleCount >= IDLE_LOOPS_TO_LOCK) {
        // Lock the heading after being idle for enough loops
        isLocked = true;
        lockedHeading = currentHeading;
      }

      if (isLocked) {
        // Apply PID correction to hold locked heading
        double correction = headingPID.calculate(currentHeading, lockedHeading);
        omegaOutput.accept(correction);
      } else {
        omegaOutput.accept(0.0);
      }
    }
  }

  /**
   * Returns whether the heading lock is currently active.
   *
   * @return The current value.
   */
  public boolean isLocked() {
    return isLocked;
  }
}
