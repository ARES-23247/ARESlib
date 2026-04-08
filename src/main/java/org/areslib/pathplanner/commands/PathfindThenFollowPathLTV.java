package org.areslib.pathplanner.commands;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.areslib.command.SequentialCommandGroup;
import org.areslib.command.Subsystem;
import org.areslib.math.Vector;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.math.numbers.N2;
import org.areslib.math.numbers.N3;
import org.areslib.pathplanner.path.PathConstraints;
import org.areslib.pathplanner.path.PathPlannerPath;
import org.areslib.pathplanner.util.ReplanningConfig;

/** A command group that first pathfinds to a goal path and then follows the goal path. */
public class PathfindThenFollowPathLTV extends SequentialCommandGroup {
  /**
   * Constructs a new PathfindThenFollowPathLTV command group.
   *
   * @param goalPath the goal path to follow
   * @param pathfindingConstraints the path constraints for pathfinding
   * @param poseSupplier a supplier for the robot's current pose
   * @param currentRobotRelativeSpeeds a supplier for the robot's current robot relative speeds
   * @param robotRelativeOutput a consumer for the output speeds (robot relative)
   * @param qelems The maximum desired error tolerance for each state.
   * @param relems The maximum desired control effort for each input.
   * @param dt Period of the robot control loop in seconds (default 0.02)
   * @param replanningConfig Path replanning configuration
   * @param shouldFlipPath Should the target path be flipped to the other side of the field? This
   *     will maintain a global blue alliance origin.
   * @param requirements the subsystems required by this command (drive subsystem)
   */
  public PathfindThenFollowPathLTV(
      PathPlannerPath goalPath,
      PathConstraints pathfindingConstraints,
      Supplier<Pose2d> poseSupplier,
      Supplier<ChassisSpeeds> currentRobotRelativeSpeeds,
      Consumer<ChassisSpeeds> robotRelativeOutput,
      Vector<N3> qelems,
      Vector<N2> relems,
      double dt,
      ReplanningConfig replanningConfig,
      BooleanSupplier shouldFlipPath,
      Subsystem... requirements) {
    addCommands(
        new org.areslib.pathplanner.dummy.PathfindLTV(
            goalPath,
            pathfindingConstraints,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            qelems,
            relems,
            dt,
            replanningConfig,
            shouldFlipPath,
            requirements),
        new org.areslib.pathplanner.dummy.FollowPathLTV(
            goalPath,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            qelems,
            relems,
            dt,
            replanningConfig,
            shouldFlipPath,
            requirements));
  }

  /**
   * Constructs a new PathfindThenFollowPathLTV command group.
   *
   * @param goalPath the goal path to follow
   * @param pathfindingConstraints the path constraints for pathfinding
   * @param poseSupplier a supplier for the robot's current pose
   * @param currentRobotRelativeSpeeds a supplier for the robot's current robot relative speeds
   * @param robotRelativeOutput a consumer for the output speeds (robot relative)
   * @param dt Period of the robot control loop in seconds (default 0.02)
   * @param replanningConfig Path replanning configuration
   * @param shouldFlipPath Should the target path be flipped to the other side of the field? This
   *     will maintain a global blue alliance origin.
   * @param requirements the subsystems required by this command (drive subsystem)
   */
  public PathfindThenFollowPathLTV(
      PathPlannerPath goalPath,
      PathConstraints pathfindingConstraints,
      Supplier<Pose2d> poseSupplier,
      Supplier<ChassisSpeeds> currentRobotRelativeSpeeds,
      Consumer<ChassisSpeeds> robotRelativeOutput,
      double dt,
      ReplanningConfig replanningConfig,
      BooleanSupplier shouldFlipPath,
      Subsystem... requirements) {
    addCommands(
        new org.areslib.pathplanner.dummy.PathfindLTV(
            goalPath,
            pathfindingConstraints,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            dt,
            replanningConfig,
            shouldFlipPath,
            requirements),
        new org.areslib.pathplanner.dummy.FollowPathLTV(
            goalPath,
            poseSupplier,
            currentRobotRelativeSpeeds,
            robotRelativeOutput,
            dt,
            replanningConfig,
            shouldFlipPath,
            requirements));
  }
}
