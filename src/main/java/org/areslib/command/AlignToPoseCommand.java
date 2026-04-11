package org.areslib.command;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.areslib.math.controller.ProfiledPIDController;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.kinematics.ChassisSpeeds;

/**
 * A command that drives the robot to a target pose using independent X, Y, and theta PID
 * controllers.
 *
 * <p>Accepts a pose supplier and a ChassisSpeeds output consumer, making it drivetrain-agnostic.
 * Finishes when all three axes are within configurable tolerances.
 *
 * <pre>{@code
 * new AlignToPoseCommand(
 *     targetPose,
 *     () -> poseEstimator.getEstimatedPosition(),
 *     speeds -> driveSubsystem.drive(speeds),
 *     xPID, yPID, thetaPID,
 *     0.02, 0.02, Math.toRadians(2)  // tolerances
 * );
 * }</pre>
 */
public class AlignToPoseCommand extends Command {

  private final Pose2d targetPose;
  private final Supplier<Pose2d> poseSupplier;
  private final Consumer<ChassisSpeeds> output;
  private final ProfiledPIDController xController;
  private final ProfiledPIDController yController;
  private final ProfiledPIDController thetaController;
  private final double xTolerance;
  private final double yTolerance;
  private final double thetaTolerance;

  /**
   * Constructs an AlignToPoseCommand using Profiled PID controllers for smooth snapping.
   *
   * @param targetPose The target field-relative pose to align to.
   * @param poseSupplier Supplier for the current robot pose.
   * @param output Consumer that receives the corrected ChassisSpeeds.
   * @param xController Profiled PID controller for the X axis.
   * @param yController Profiled PID controller for the Y axis.
   * @param thetaController Profiled PID controller for heading (must have continuous input
   *     enabled).
   * @param xTolerance Position tolerance in meters for the X axis.
   * @param yTolerance Position tolerance in meters for the Y axis.
   * @param thetaTolerance Heading tolerance in radians.
   */
  public AlignToPoseCommand(
      Pose2d targetPose,
      Supplier<Pose2d> poseSupplier,
      Consumer<ChassisSpeeds> output,
      ProfiledPIDController xController,
      ProfiledPIDController yController,
      ProfiledPIDController thetaController,
      double xTolerance,
      double yTolerance,
      double thetaTolerance) {
    this.targetPose = targetPose;
    this.poseSupplier = poseSupplier;
    this.output = output;
    this.xController = xController;
    this.yController = yController;
    this.thetaController = thetaController;
    this.xTolerance = xTolerance;
    this.yTolerance = yTolerance;
    this.thetaTolerance = thetaTolerance;
  }

  @Override
  public void initialize() {
    Pose2d current = poseSupplier.get();
    // Reset the profiles to our starting position so we don't aggressively lurch
    xController.reset(current.getX(), 0.0);
    yController.reset(current.getY(), 0.0);
    thetaController.reset(current.getRotation().getRadians(), 0.0);

    xController.setGoal(targetPose.getX());
    yController.setGoal(targetPose.getY());
    thetaController.setGoal(targetPose.getRotation().getRadians());
  }

  @Override
  public void execute() {
    Pose2d current = poseSupplier.get();

    double xSpeed = xController.calculate(current.getX());
    double ySpeed = yController.calculate(current.getY());
    double omegaSpeed = thetaController.calculate(current.getRotation().getRadians());

    output.accept(new ChassisSpeeds(xSpeed, ySpeed, omegaSpeed));
  }

  @Override
  public boolean isFinished() {
    Pose2d current = poseSupplier.get();
    double xError = Math.abs(current.getX() - targetPose.getX());
    double yError = Math.abs(current.getY() - targetPose.getY());
    double thetaError =
        Math.abs(
            org.areslib.math.MathUtil.angleModulus(
                current.getRotation().getRadians() - targetPose.getRotation().getRadians()));

    return xError <= xTolerance && yError <= yTolerance && thetaError <= thetaTolerance;
  }

  @Override
  public void end(boolean interrupted) {
    output.accept(new ChassisSpeeds(0, 0, 0));
  }
}
