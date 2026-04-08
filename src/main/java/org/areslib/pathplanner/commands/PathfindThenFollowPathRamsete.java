package org.areslib.pathplanner.commands;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.areslib.command.SequentialCommandGroup;
import org.areslib.command.Subsystem;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.pathplanner.path.PathConstraints;
import org.areslib.pathplanner.path.PathPlannerPath;
import org.areslib.pathplanner.util.ReplanningConfig;

/** A command group that first pathfinds to a goal path and then follows the goal path. */
public class PathfindThenFollowPathRamsete extends SequentialCommandGroup {
  /**
   * Constructs a new PathfindThenFollowPathRamsete command group.
   *
   * @param goalPath the goal path to follow
   * @param pathfindingConstraints the path constraints for pathfinding
   * @param poseSupplier a supplier for the robot's current pose
   * @param currentRobotRelativeSpeeds a supplier for the robot's current robot relative speeds
   * @param robotRelativeOutput a consumer for the output speeds (robot relative)
   * @param b Tuning parameter (b &gt; 0 rad^2/m^2) for which larger values make convergence more
   *     aggressive like a proportional term.
   * @param zeta Tuning parameter (0 rad^-1 &lt; zeta &lt; 1 rad^-1) for which larger values provide
   *     more damping in response.
   * @param replanningConfig Path replanning configuration
   * @param shouldFlipPath Should the target path be flipped to the other side of the field? This
   *     will maintain a global blue alliance origin.
   * @param requirements the subsystems required by this command (drive subsystem)
   */
  public PathfindThenFollowPathRamsete(
      PathPlannerPath goalPath,
      PathConstraints pathfindingConstraints,
      Supplier<Pose2d> poseSupplier,
      Supplier<ChassisSpeeds> currentRobotRelativeSpeeds,
      Consumer<ChassisSpeeds> robotRelativeOutput,
      double b,
      double zeta,
      ReplanningConfig replanningConfig,
      BooleanSupplier shouldFlipPath,
      Subsystem... requirements) {
    addCommands(
        new org.areslib.pathplanner.dummy.PathfindRamsete(
            goalPath,
            pathfindingConstraints,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            b,
            zeta,
            replanningConfig,
            shouldFlipPath,
            requirements),
        new org.areslib.pathplanner.dummy.FollowPathRamsete(
            goalPath,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            b,
            zeta,
            replanningConfig,
            shouldFlipPath,
            requirements));
  }

  /**
   * Constructs a new PathfindThenFollowPathRamsete command group.
   *
   * @param goalPath the goal path to follow
   * @param pathfindingConstraints the path constraints for pathfinding
   * @param poseSupplier a supplier for the robot's current pose
   * @param currentRobotRelativeSpeeds a supplier for the robot's current robot relative speeds
   * @param robotRelativeOutput a consumer for the output speeds (robot relative)
   * @param replanningConfig Path replanning configuration
   * @param shouldFlipPath Should the target path be flipped to the other side of the field? This
   *     will maintain a global blue alliance origin.
   * @param requirements the subsystems required by this command (drive subsystem)
   */
  public PathfindThenFollowPathRamsete(
      PathPlannerPath goalPath,
      PathConstraints pathfindingConstraints,
      Supplier<Pose2d> poseSupplier,
      Supplier<ChassisSpeeds> currentRobotRelativeSpeeds,
      Consumer<ChassisSpeeds> robotRelativeOutput,
      ReplanningConfig replanningConfig,
      BooleanSupplier shouldFlipPath,
      Subsystem... requirements) {
    addCommands(
        new org.areslib.pathplanner.dummy.PathfindRamsete(
            goalPath,
            pathfindingConstraints,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            replanningConfig,
            shouldFlipPath,
            requirements),
        new org.areslib.pathplanner.dummy.FollowPathRamsete(
            goalPath,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            replanningConfig,
            shouldFlipPath,
            requirements));
  }
}
