package org.areslib.command.autoaim;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.areslib.command.Command;
import org.areslib.command.Subsystem;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.math.kinematics.KinematicAiming;

/**
 * A Command that continually feeds instantaneous robot velocities to a kinematic calculator,
 * yielding a dynamically updated target-leading angle for a shooter or turret to track.
 */
public class ShootOnTheMoveCommand extends Command {

  private final Supplier<Translation2d> m_robotPositionSupplier;
  private final Supplier<ChassisSpeeds> m_robotVelocitySupplier;
  private final Translation2d m_targetPosition;
  private final double m_projectileSpeedMetersPerSec;
  private final Consumer<Rotation2d> m_aimingOutput;

  /**
   * @param robotPositionSupplier Provides the robot's current pose.
   * @param robotVelocitySupplier Provides field-relative speeds (so X is true field X velocity).
   * @param targetPosition The fixed location of the target on the field.
   * @param projectileSpeedMetersPerSec The fixed exit velocity of the game piece.
   * @param aimingOutput A method that consumes the highly-accurate target-leading heading.
   * @param requirements Any subsystems this command requires (e.g. SwerveDrive or Turret).
   */
  public ShootOnTheMoveCommand(
      Supplier<Translation2d> robotPositionSupplier,
      Supplier<ChassisSpeeds> robotVelocitySupplier,
      Translation2d targetPosition,
      double projectileSpeedMetersPerSec,
      Consumer<Rotation2d> aimingOutput,
      Subsystem... requirements) {

    m_robotPositionSupplier = robotPositionSupplier;
    m_robotVelocitySupplier = robotVelocitySupplier;
    m_targetPosition = targetPosition;
    m_projectileSpeedMetersPerSec = projectileSpeedMetersPerSec;
    m_aimingOutput = aimingOutput;

    addRequirements(requirements);
  }

  @Override
  public void execute() {
    Translation2d currentPos = m_robotPositionSupplier.get();
    ChassisSpeeds velocities = m_robotVelocitySupplier.get();

    KinematicAiming.AimResult result =
        KinematicAiming.calculateAim(
            velocities, currentPos, m_targetPosition, m_projectileSpeedMetersPerSec);

    // Always apply target, even if math falls back to direct line of sight.
    m_aimingOutput.accept(result.requiredHeading);
  }

  @Override
  public boolean isFinished() {
    // Runs continuously until interrupted or a button is released.
    return false;
  }
}
