package org.areslib.command;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import org.areslib.math.controller.PIDController;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.kinematics.ChassisSpeeds;

/**
 * Command for dynamic, odometry-based robot alignment specifically for scoring on game pieces.
 *
 * <p>Overrides the driver's lateral (Y) and rotational (Theta) control to perfectly track a
 * specific field coordinate, while allowing them to maintain forward/backward (X) speed.
 *
 * <p>Perfect for FTC autonomous scoring and teleop assist modes.
 */
public class SmartAssistAlign extends Command {

  private final Consumer<ChassisSpeeds> driveOutput;
  private final DoubleSupplier forwardThrottleSupplier;
  private final Pose2d targetPose;
  private final Pose2d currentPoseSupplier;

  private final PIDController yAlignController;
  private final PIDController thetaAlignController;

  private final double yTolerance;
  private final double thetaTolerance;

  /**
   * Creates a SmartAssistAlign command for semi-autonomous scoring.
   *
   * @param driveOutput Consumer that accepts the corrected ChassisSpeeds
   * @param forwardThrottleSupplier Lambda to the driver's X-axis joystick input
   * @param currentPoseSupplier Supplier for current robot pose
   * @param targetPose The stationary coordinate mapping we want to align to
   * @param alignKp P gain for translation alignment
   * @param alignThetaKp P gain for rotational alignment
   * @param yTolerance Lateral tolerance in meters
   * @param thetaTolerance Rotational tolerance in radians
   */
  public SmartAssistAlign(
      Consumer<ChassisSpeeds> driveOutput,
      DoubleSupplier forwardThrottleSupplier,
      Pose2d currentPoseSupplier,
      Pose2d targetPose,
      double alignKp,
      double alignThetaKp,
      double yTolerance,
      double thetaTolerance) {

    this.driveOutput = driveOutput;
    this.forwardThrottleSupplier = forwardThrottleSupplier;
    this.currentPoseSupplier = currentPoseSupplier;
    this.targetPose = targetPose;
    this.yTolerance = yTolerance;
    this.thetaTolerance = thetaTolerance;

    // These controllers compare the Robot's true position to the target's true position
    this.yAlignController = new PIDController(alignKp, 0, 0);
    this.thetaAlignController = new PIDController(alignThetaKp, 0, 0);

    this.thetaAlignController.enableContinuousInput(-Math.PI, Math.PI);
  }

  @Override
  public void execute() {
    Pose2d currentPose = currentPoseSupplier;

    // 1. Let the driver keep control of the speed advancing towards the target (Field X)
    double fieldVx = forwardThrottleSupplier.getAsDouble();

    // 2. Automate strafing to line up exactly with target Y (Field Y)
    double fieldVy = yAlignController.calculate(currentPose.getY(), targetPose.getY());

    // 3. Automate rotation to point exactly at target heading (Field Theta)
    double omega =
        thetaAlignController.calculate(
            currentPose.getRotation().getRadians(), targetPose.getRotation().getRadians());

    // 4. Critically translate these Field-Relative targets into Robot-Centric kinematics
    ChassisSpeeds robotSpeeds =
        ChassisSpeeds.fromFieldRelativeSpeeds(fieldVx, fieldVy, omega, currentPose.getRotation());

    // Feed to kinematics
    driveOutput.accept(robotSpeeds);
  }

  @Override
  public boolean isFinished() {
    Pose2d currentPose = currentPoseSupplier;
    double yError = Math.abs(currentPose.getY() - targetPose.getY());
    double thetaError =
        Math.abs(
            org.areslib.math.MathUtil.angleModulus(
                currentPose.getRotation().getRadians() - targetPose.getRotation().getRadians()));

    // Converged when within tolerance
    return yError < yTolerance && thetaError < thetaTolerance;
  }

  @Override
  public void end(boolean interrupted) {
    driveOutput.accept(new ChassisSpeeds(0, 0, 0));
  }
}
