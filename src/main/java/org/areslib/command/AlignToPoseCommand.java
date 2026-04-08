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

  private final Pose2d m_targetPose;
  private final Supplier<Pose2d> m_poseSupplier;
  private final Consumer<ChassisSpeeds> m_output;
  private final ProfiledPIDController m_xController;
  private final ProfiledPIDController m_yController;
  private final ProfiledPIDController m_thetaController;
  private final double m_xTolerance;
  private final double m_yTolerance;
  private final double m_thetaTolerance;

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
    m_targetPose = targetPose;
    m_poseSupplier = poseSupplier;
    m_output = output;
    m_xController = xController;
    m_yController = yController;
    m_thetaController = thetaController;
    m_xTolerance = xTolerance;
    m_yTolerance = yTolerance;
    m_thetaTolerance = thetaTolerance;
  }

  @Override
  public void initialize() {
    Pose2d current = m_poseSupplier.get();
    // Reset the profiles to our starting position so we don't aggressively lurch
    m_xController.reset(current.getX(), 0.0);
    m_yController.reset(current.getY(), 0.0);
    m_thetaController.reset(current.getRotation().getRadians(), 0.0);

    m_xController.setGoal(m_targetPose.getX());
    m_yController.setGoal(m_targetPose.getY());
    m_thetaController.setGoal(m_targetPose.getRotation().getRadians());
  }

  @Override
  public void execute() {
    Pose2d current = m_poseSupplier.get();

    double xSpeed = m_xController.calculate(current.getX());
    double ySpeed = m_yController.calculate(current.getY());
    double omegaSpeed = m_thetaController.calculate(current.getRotation().getRadians());

    m_output.accept(new ChassisSpeeds(xSpeed, ySpeed, omegaSpeed));
  }

  @Override
  public boolean isFinished() {
    Pose2d current = m_poseSupplier.get();
    double xError = Math.abs(current.getX() - m_targetPose.getX());
    double yError = Math.abs(current.getY() - m_targetPose.getY());
    double thetaError =
        Math.abs(
            org.areslib.math.MathUtil.angleModulus(
                current.getRotation().getRadians() - m_targetPose.getRotation().getRadians()));

    return xError <= m_xTolerance && yError <= m_yTolerance && thetaError <= m_thetaTolerance;
  }

  @Override
  public void end(boolean interrupted) {
    m_output.accept(new ChassisSpeeds(0, 0, 0));
  }
}
