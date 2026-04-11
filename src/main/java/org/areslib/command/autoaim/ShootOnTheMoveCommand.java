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

  private final Supplier<Translation2d> robotPositionSupplier;
  private final Supplier<ChassisSpeeds> robotVelocitySupplier;
  private final Translation2d targetPosition;
  private final double projectileSpeedMetersPerSec;
  private final Consumer<Rotation2d> aimingOutput;

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

    this.robotPositionSupplier = robotPositionSupplier;
    this.robotVelocitySupplier = robotVelocitySupplier;
    this.targetPosition = targetPosition;
    this.projectileSpeedMetersPerSec = projectileSpeedMetersPerSec;
    this.aimingOutput = aimingOutput;

    addRequirements(requirements);
  }

  @Override
  public void execute() {
    Translation2d currentPos = robotPositionSupplier.get();
    ChassisSpeeds velocities = robotVelocitySupplier.get();

    KinematicAiming.AimResult result =
        KinematicAiming.calculateAim(
            velocities, currentPos, targetPosition, projectileSpeedMetersPerSec);

    // Always apply target, even if math falls back to direct line of sight.
    aimingOutput.accept(result.requiredHeading);
  }

  @Override
  public boolean isFinished() {
    // Runs continuously until interrupted or a button is released.
    return false;
  }
}
