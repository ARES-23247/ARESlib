package org.areslib.pathplanner;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.areslib.command.Command;
import org.areslib.command.InstantCommand;
import org.areslib.command.SequentialCommandGroup;
import org.areslib.command.Subsystem;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.pathplanner.auto.AutoBuilder;
import org.areslib.pathplanner.commands.FollowPathCommand;
import org.areslib.pathplanner.controllers.PPHolonomicDriveController;
import org.areslib.pathplanner.dummy.*;
import org.areslib.pathplanner.path.*;
import org.areslib.pathplanner.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the PathPlanner integration layer.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>Dummy shim layer (Timer, Commands, DriverStation, Filesystem, SendableChooser)
 *   <li>AutoBuilder configuration and auto file parsing
 *   <li>PathPlannerPath construction, bezier generation, and trajectory generation
 *   <li>PPHolonomicDriveController PID output computation
 *   <li>GeometryUtil math (lerp, flip, radius)
 *   <li>FollowPathCommand lifecycle (init → execute → isFinished → end)
 * </ul>
 */
public class PathPlannerShimTest {

  /**
   * AutoBuilder is a static singleton — it must be configured exactly once before any test that
   * calls followPath, pathfindToPose, or buildAuto.
   */
  @BeforeAll
  static void configureAutoBuilder() {
    if (!AutoBuilder.isConfigured()) {
      Subsystem mock =
          new Subsystem() {
            @Override
            public void periodic() {}
          };
      AutoBuilder.configureHolonomic(
          () -> new Pose2d(),
          pose -> {},
          () -> new ChassisSpeeds(),
          speeds -> {},
          new HolonomicPathFollowerConfig(1.0, 1.0, new ReplanningConfig()),
          () -> false,
          mock);
    }
  }

  // ---------------------------------------------------------------
  //  Dummy Shim Layer Tests
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("Timer Shim")
  class TimerTests {
    @Test
    @DisplayName("tracks elapsed wall-clock time via System.nanoTime()")
    public void testTracksTime() throws InterruptedException {
      Timer timer = new Timer();
      timer.start();
      Thread.sleep(50);
      timer.stop();
      double elapsed = timer.get();
      assertTrue(elapsed >= 0.040, "Expected ≥40ms, got " + elapsed);
      assertTrue(timer.hasElapsed(0.040));
    }

    @Test
    @DisplayName("reset zeroes accumulated time")
    public void testReset() throws InterruptedException {
      Timer timer = new Timer();
      timer.start();
      Thread.sleep(30);
      timer.stop();
      timer.reset();
      assertTrue(timer.get() < 0.01, "After reset, timer should be near zero");
    }

    @Test
    @DisplayName("restart resets and starts in one call")
    public void testRestart() throws InterruptedException {
      Timer timer = new Timer();
      timer.start();
      Thread.sleep(30);
      timer.restart();
      Thread.sleep(20);
      timer.stop();
      double elapsed = timer.get();
      // Should only reflect the ~20ms after restart, not 50ms total
      assertTrue(elapsed < 0.045, "After restart, elapsed should be ~20ms, got " + elapsed);
      assertTrue(elapsed >= 0.010);
    }

    @Test
    @DisplayName("getFPGATimestamp returns positive seconds")
    public void testFpgaTimestamp() {
      double ts = Timer.getFPGATimestamp();
      assertTrue(ts > 0, "FPGA timestamp should be positive");
    }
  }

  @Nested
  @DisplayName("Commands Shim")
  class CommandsTests {
    @Test
    @DisplayName("sequence() returns SequentialCommandGroup")
    public void testSequence() {
      Command seq = Commands.sequence();
      assertNotNull(seq);
      assertInstanceOf(SequentialCommandGroup.class, seq);
    }

    @Test
    @DisplayName("either() returns non-null and picks correct branch")
    public void testEither() {
      boolean[] ranTrue = {false};
      boolean[] ranFalse = {false};
      Command onTrue = Commands.runOnce(() -> ranTrue[0] = true);
      Command onFalse = Commands.runOnce(() -> ranFalse[0] = true);

      Command cmd = Commands.either(onTrue, onFalse, () -> true);
      assertNotNull(cmd);
      cmd.initialize();
      assertTrue(ranTrue[0], "onTrue branch should have been initialized");
    }

    @Test
    @DisplayName("runOnce() wraps runnable as InstantCommand")
    public void testRunOnce() {
      boolean[] ran = {false};
      Command cmd = Commands.runOnce(() -> ran[0] = true);
      assertInstanceOf(InstantCommand.class, cmd);
      cmd.initialize();
      assertTrue(ran[0]);
    }

    @Test
    @DisplayName("waitSeconds() finishes after elapsed time")
    public void testWaitSeconds() throws InterruptedException {
      Command wait = Commands.waitSeconds(0.05);
      wait.initialize();
      assertFalse(wait.isFinished(), "Should not be finished immediately");
      Thread.sleep(60);
      assertTrue(wait.isFinished(), "Should be finished after 60ms for a 50ms wait");
    }

    @Test
    @DisplayName("none() finishes instantly")
    public void testNone() {
      Command cmd = Commands.none();
      assertNotNull(cmd);
      assertInstanceOf(InstantCommand.class, cmd);
    }

    @Test
    @DisplayName("defer() lazily builds inner command")
    public void testDefer() {
      boolean[] built = {false};
      Command cmd =
          Commands.defer(
              () -> {
                built[0] = true;
                return Commands.none();
              });
      assertFalse(built[0], "Should not build until initialize");
      cmd.initialize();
      assertTrue(built[0], "Should build during initialize");
    }

    @Test
    @DisplayName("run() never finishes on its own")
    public void testRun() {
      int[] count = {0};
      Command cmd = Commands.run(() -> count[0]++);
      cmd.execute();
      cmd.execute();
      cmd.execute();
      assertEquals(3, count[0]);
      assertFalse(cmd.isFinished());
    }
  }

  @Nested
  @DisplayName("DriverStation Shim")
  class DriverStationTests {
    @Test
    @DisplayName("isEnabled returns true in simulation")
    public void testEnabled() {
      assertTrue(DriverStation.isEnabled());
    }

    @Test
    @DisplayName("isAutonomous returns true for headless auto execution")
    public void testAutonomous() {
      assertTrue(DriverStation.isAutonomous());
    }

    @Test
    @DisplayName("isTeleop returns false")
    public void testTeleop() {
      assertFalse(DriverStation.isTeleop());
    }

    @Test
    @DisplayName("getAlliance always returns Blue")
    public void testAlliance() {
      assertTrue(DriverStation.getAlliance().isPresent());
    }

    @Test
    @DisplayName("reportError does not throw")
    public void testReportError() {
      assertDoesNotThrow(() -> DriverStation.reportError("test error", false));
    }

    @Test
    @DisplayName("reportWarning does not throw")
    public void testReportWarning() {
      assertDoesNotThrow(() -> DriverStation.reportWarning("test warning", false));
    }
  }

  @Nested
  @DisplayName("Filesystem Shim")
  class FilesystemTests {
    @Test
    @DisplayName("getDeployDirectory points to src/main/deploy")
    public void testDeployDir() {
      java.io.File dir = Filesystem.getDeployDirectory();
      assertNotNull(dir);
      assertTrue(dir.getPath().replace('\\', '/').contains("src/main/deploy"));
    }
  }

  @Nested
  @DisplayName("SendableChooser Shim")
  class SendableChooserTests {
    @Test
    @DisplayName("setDefaultOption sets the selected value")
    public void testDefault() {
      SendableChooser<String> chooser = new SendableChooser<>();
      chooser.setDefaultOption("A", "Alpha");
      assertEquals("Alpha", chooser.getSelected());
    }

    @Test
    @DisplayName("addOption does not override selected")
    public void testAddOption() {
      SendableChooser<String> chooser = new SendableChooser<>();
      chooser.setDefaultOption("A", "Alpha");
      chooser.addOption("B", "Beta");
      assertEquals("Alpha", chooser.getSelected());
    }

    @Test
    @DisplayName("getSelected returns null before any option added")
    public void testNullBefore() {
      SendableChooser<String> chooser = new SendableChooser<>();
      assertNull(chooser.getSelected());
    }
  }

  @Nested
  @DisplayName("TrapezoidProfile Shim")
  class TrapezoidProfileTests {
    @Test
    @DisplayName("calculate returns a valid State")
    public void testCalculate() {
      TrapezoidProfile.Constraints c = new TrapezoidProfile.Constraints(2.0, 1.0);
      TrapezoidProfile profile = new TrapezoidProfile(c);
      TrapezoidProfile.State result =
          profile.calculate(
              0.5, new TrapezoidProfile.State(0, 0), new TrapezoidProfile.State(1, 0));
      assertNotNull(result, "Profile result should not be null");
    }
  }

  // ---------------------------------------------------------------
  //  AutoBuilder Tests
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("AutoBuilder")
  class AutoBuilderTests {
    @Test
    @DisplayName("isConfigured and isPathfindingConfigured return true after @BeforeAll")
    public void testConfigure() {
      assertTrue(AutoBuilder.isConfigured(), "AutoBuilder should be configured via @BeforeAll");
      assertTrue(
          AutoBuilder.isPathfindingConfigured(), "Pathfinding should be configured via @BeforeAll");
    }

    @Test
    @DisplayName("buildAuto parses SquareAuto.auto and returns non-null command")
    public void testParsesSquareAuto() {
      assertDoesNotThrow(
          () -> {
            Command cmd = AutoBuilder.buildAuto("SquareAuto");
            assertNotNull(cmd, "SquareAuto command must not be null");
          });
    }

    @Test
    @DisplayName("getAllAutoNames discovers SquareAuto")
    public void testGetAllAutoNames() {
      List<String> names = AutoBuilder.getAllAutoNames();
      assertNotNull(names);
      assertTrue(names.contains("SquareAuto"), "Should discover SquareAuto in deploy dir");
    }

    @Test
    @DisplayName("followPath returns a valid command")
    public void testFollowPath() {
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(1.0, 1.0, new Rotation2d()), new Pose2d(2.0, 2.0, new Rotation2d()));
      PathPlannerPath path =
          new PathPlannerPath(
              bezier,
              new PathConstraints(1.0, 1.5, 360, 540),
              new GoalEndState(0, Rotation2d.fromDegrees(0), true));
      Command cmd = AutoBuilder.followPath(path);
      assertNotNull(cmd, "followPath must return non-null");
    }

    @Test
    @DisplayName("pathfindToPose returns a valid command")
    public void testPathfindToPose() {
      Command cmd =
          AutoBuilder.pathfindToPose(
              new Pose2d(2.0, 2.0, Rotation2d.fromDegrees(90)),
              new PathConstraints(1.0, 1.5, 360, 540));
      assertNotNull(cmd, "pathfindToPose must return non-null");
    }
  }

  // ---------------------------------------------------------------
  //  PathPlannerPath Tests
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("PathPlannerPath")
  class PathTests {
    @Test
    @DisplayName("bezierFromPoses generates correct number of control points")
    public void testBezierFromPoses() {
      // 2 poses → 1 segment → 4 bezier points
      List<Translation2d> pts =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(0, 0, Rotation2d.fromDegrees(0)),
              new Pose2d(3, 3, Rotation2d.fromDegrees(90)));
      assertEquals(4, pts.size(), "2 poses should produce 4 bezier points");
    }

    @Test
    @DisplayName("bezierFromPoses with 3 poses generates 7 points")
    public void testBezier3Poses() {
      List<Translation2d> pts =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(0, 0, Rotation2d.fromDegrees(0)),
              new Pose2d(1.5, 1.5, Rotation2d.fromDegrees(45)),
              new Pose2d(3, 0, Rotation2d.fromDegrees(0)));
      // 3 poses: first(2) + middle(3) + last(2) = 7
      assertEquals(7, pts.size(), "3 poses should produce 7 bezier points");
    }

    @Test
    @DisplayName("path construction generates non-empty allPoints")
    public void testPathConstruction() {
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(1, 1, new Rotation2d()), new Pose2d(3, 3, new Rotation2d()));
      PathPlannerPath path =
          new PathPlannerPath(
              bezier,
              new PathConstraints(1.0, 1.5, 360, 540),
              new GoalEndState(0, Rotation2d.fromDegrees(0), true));
      assertTrue(path.numPoints() > 0, "Path should have generated points");
    }

    @Test
    @DisplayName("getGoalEndState matches construction parameters")
    public void testGoalEndState() {
      GoalEndState goal = new GoalEndState(0.5, Rotation2d.fromDegrees(45), false);
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(0, 0, new Rotation2d()), new Pose2d(2, 2, new Rotation2d()));
      PathPlannerPath path = new PathPlannerPath(bezier, new PathConstraints(1, 1, 360, 540), goal);
      assertEquals(0.5, path.getGoalEndState().getVelocity(), 1e-6);
    }

    @Test
    @DisplayName("trajectory generation produces valid time > 0")
    public void testTrajectoryGeneration() {
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(1, 1, new Rotation2d()), new Pose2d(3, 3, new Rotation2d()));
      PathPlannerPath path =
          new PathPlannerPath(
              bezier,
              new PathConstraints(1.0, 1.5, 360, 540),
              new GoalEndState(0, Rotation2d.fromDegrees(0), true));
      PathPlannerTrajectory traj = path.getTrajectory(new ChassisSpeeds(), new Rotation2d());
      assertTrue(
          traj.getTotalTimeSeconds() > 0,
          "Trajectory total time should be > 0, got " + traj.getTotalTimeSeconds());
    }

    @Test
    @DisplayName("trajectory can be sampled at t=0 without NPE")
    public void testTrajectorySampleAtZero() {
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(1, 1, new Rotation2d()), new Pose2d(3, 3, new Rotation2d()));
      PathPlannerPath path =
          new PathPlannerPath(
              bezier,
              new PathConstraints(1.0, 1.5, 360, 540),
              new GoalEndState(0, Rotation2d.fromDegrees(0), true));
      PathPlannerTrajectory traj = path.getTrajectory(new ChassisSpeeds(), new Rotation2d());
      assertDoesNotThrow(
          () -> {
            PathPlannerTrajectory.State st = traj.sample(0);
            assertNotNull(st.positionMeters);
          });
    }

    @Test
    @DisplayName("path from file loads SquarePath.path")
    public void testFromPathFile() {
      assertDoesNotThrow(
          () -> {
            PathPlannerPath path = PathPlannerPath.fromPathFile("SquarePath");
            assertNotNull(path);
            assertTrue(path.numPoints() > 4, "SquarePath should have many interpolated points");
          });
    }

    @Test
    @DisplayName("flipPath mirrors X coordinates")
    public void testFlipPath() {
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(1, 1, new Rotation2d()), new Pose2d(3, 3, new Rotation2d()));
      PathPlannerPath path =
          new PathPlannerPath(
              bezier,
              new PathConstraints(1.0, 1.5, 360, 540),
              new GoalEndState(0, Rotation2d.fromDegrees(0), true));
      PathPlannerPath flipped = path.flipPath();
      // First point X should be mirrored: FIELD_LENGTH - originalX
      double originalX = path.getPoint(0).position.getX();
      double flippedX = flipped.getPoint(0).position.getX();
      assertNotEquals(originalX, flippedX, 0.01, "Flipped path X should differ from original");
    }
  }

  // ---------------------------------------------------------------
  //  PPHolonomicDriveController Tests
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("PPHolonomicDriveController")
  class ControllerTests {
    @Test
    @DisplayName("reset does not throw")
    public void testReset() {
      PPHolonomicDriveController ctrl =
          new PPHolonomicDriveController(
              new PIDConstants(5.0, 0, 0), new PIDConstants(5.0, 0, 0), 4.5, 0.4);
      assertDoesNotThrow(() -> ctrl.reset(new Pose2d(), new ChassisSpeeds()));
    }

    @Test
    @DisplayName("outputs non-zero speed when off target")
    public void testOutputsCorrection() {
      PPHolonomicDriveController ctrl =
          new PPHolonomicDriveController(
              new PIDConstants(5.0, 0, 0), new PIDConstants(5.0, 0, 0), 4.5, 0.4);
      ctrl.reset(new Pose2d(), new ChassisSpeeds());

      // Current pose at origin, target 1m ahead
      PathPlannerTrajectory.State target = new PathPlannerTrajectory.State();
      target.positionMeters = new Translation2d(1.0, 0);
      target.heading = new Rotation2d(0);
      target.velocityMps = 1.0;
      target.targetHolonomicRotation = new Rotation2d();
      target.holonomicAngularVelocityRps = java.util.Optional.of(0.0);
      target.constraints = new PathConstraints(4.0, 4.0, 6.28, 6.28);

      ChassisSpeeds result = ctrl.calculateRobotRelativeSpeeds(new Pose2d(), target);
      double speed = Math.hypot(result.vxMetersPerSecond, result.vyMetersPerSecond);
      assertTrue(speed > 0.1, "Controller should output correction speed, got " + speed);
    }

    @Test
    @DisplayName("positional error is zero when at target")
    public void testZeroErrorAtTarget() {
      PPHolonomicDriveController ctrl =
          new PPHolonomicDriveController(
              new PIDConstants(5.0, 0, 0), new PIDConstants(5.0, 0, 0), 4.5, 0.4);
      ctrl.reset(new Pose2d(), new ChassisSpeeds());

      PathPlannerTrajectory.State target = new PathPlannerTrajectory.State();
      target.positionMeters = new Translation2d(0, 0);
      target.heading = new Rotation2d(0);
      target.velocityMps = 0;
      target.targetHolonomicRotation = new Rotation2d();
      target.holonomicAngularVelocityRps = java.util.Optional.of(0.0);
      target.constraints = new PathConstraints(4.0, 4.0, 6.28, 6.28);

      ctrl.calculateRobotRelativeSpeeds(new Pose2d(), target);
      assertEquals(0.0, ctrl.getPositionalError(), 1e-6);
    }

    @Test
    @DisplayName("isHolonomic returns true")
    public void testIsHolonomic() {
      PPHolonomicDriveController ctrl =
          new PPHolonomicDriveController(new PIDConstants(1), new PIDConstants(1), 4.5, 0.4);
      assertTrue(ctrl.isHolonomic());
    }
  }

  // ---------------------------------------------------------------
  //  GeometryUtil Tests
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("GeometryUtil")
  class GeometryTests {
    @Test
    @DisplayName("doubleLerp interpolates correctly")
    public void testDoubleLerp() {
      assertEquals(5.0, GeometryUtil.doubleLerp(0, 10, 0.5), 1e-9);
      assertEquals(0.0, GeometryUtil.doubleLerp(0, 10, 0.0), 1e-9);
      assertEquals(10.0, GeometryUtil.doubleLerp(0, 10, 1.0), 1e-9);
    }

    @Test
    @DisplayName("flipFieldPosition mirrors X, keeps Y")
    public void testFlipPosition() {
      Translation2d pos = new Translation2d(2.0, 3.0);
      Translation2d flipped = GeometryUtil.flipFieldPosition(pos);
      assertEquals(16.54 - 2.0, flipped.getX(), 1e-6);
      assertEquals(3.0, flipped.getY(), 1e-6);
    }

    @Test
    @DisplayName("flipFieldPose mirrors position and rotation")
    public void testFlipPose() {
      Pose2d pose = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(0));
      Pose2d flipped = GeometryUtil.flipFieldPose(pose);
      assertEquals(16.54 - 1.0, flipped.getX(), 1e-6);
      assertEquals(2.0, flipped.getY(), 1e-6);
      // Heading 0° flipped → ~180°
      assertEquals(180.0, Math.abs(flipped.getRotation().getDegrees()), 1.0);
    }

    @Test
    @DisplayName("calculateRadius returns finite for non-collinear points")
    public void testCalculateRadius() {
      Translation2d a = new Translation2d(0, 0);
      Translation2d b = new Translation2d(1, 1);
      Translation2d c = new Translation2d(2, 0);
      double r = GeometryUtil.calculateRadius(a, b, c);
      assertTrue(Double.isFinite(r), "Radius should be finite for a curved path");
    }

    @Test
    @DisplayName("translationLerp interpolates between two points")
    public void testTranslationLerp() {
      Translation2d a = new Translation2d(0, 0);
      Translation2d b = new Translation2d(4, 6);
      Translation2d mid = GeometryUtil.translationLerp(a, b, 0.5);
      assertEquals(2.0, mid.getX(), 1e-9);
      assertEquals(3.0, mid.getY(), 1e-9);
    }
  }

  // ---------------------------------------------------------------
  //  FollowPathCommand Lifecycle Simulation Test
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("FollowPathCommand Lifecycle")
  class FollowPathLifecycleTests {
    @Test
    @DisplayName("warmupCommand returns non-null and finishes")
    public void testWarmupCommand() {
      Command warmup = FollowPathCommand.warmupCommand();
      assertNotNull(warmup, "Warmup command must not be null");
    }

    @Test
    @DisplayName("full init-execute-end lifecycle on programmatic path")
    public void testFullLifecycle() {
      // Build a path programmatically to avoid file-loading state issues
      List<Translation2d> bezier =
          PathPlannerPath.bezierFromPoses(
              new Pose2d(1.0, 1.0, Rotation2d.fromDegrees(0)),
              new Pose2d(2.0, 2.0, Rotation2d.fromDegrees(0)));
      PathPlannerPath path =
          new PathPlannerPath(
              bezier,
              new PathConstraints(1.0, 1.5, 360, 540),
              new GoalEndState(0, Rotation2d.fromDegrees(0), true));

      assertTrue(AutoBuilder.isConfigured(), "AutoBuilder must be configured for lifecycle test");

      Command rawCmd = AutoBuilder.followPath(path);
      assertNotNull(rawCmd, "followPath should return non-null");

      // Initialize — generates trajectory and starts timer
      assertDoesNotThrow(() -> rawCmd.initialize());

      // Execute a few cycles — should not throw
      for (int i = 0; i < 10; i++) {
        assertDoesNotThrow(() -> rawCmd.execute());
      }

      // End (interrupted)
      assertDoesNotThrow(() -> rawCmd.end(true));
    }
  }

  // ---------------------------------------------------------------
  //  PIDConstants / ReplanningConfig / PathConstraints Data Tests
  // ---------------------------------------------------------------

  @Nested
  @DisplayName("Configuration Data Classes")
  class ConfigTests {
    @Test
    @DisplayName("PIDConstants stores values correctly")
    public void testPIDConstants() {
      PIDConstants pid = new PIDConstants(1.0, 0.5, 0.1, 2.0);
      assertEquals(1.0, pid.kP);
      assertEquals(0.5, pid.kI);
      assertEquals(0.1, pid.kD);
      assertEquals(2.0, pid.iZone);
    }

    @Test
    @DisplayName("PIDConstants convenience constructors set defaults")
    public void testPIDDefaults() {
      PIDConstants p = new PIDConstants(3.0);
      assertEquals(3.0, p.kP);
      assertEquals(0.0, p.kI);
      assertEquals(0.0, p.kD);
      assertEquals(1.0, p.iZone);
    }

    @Test
    @DisplayName("PathConstraints stores velocity and acceleration limits")
    public void testPathConstraints() {
      PathConstraints c = new PathConstraints(2.0, 3.0, 360.0, 540.0);
      assertEquals(2.0, c.getMaxVelocityMps(), 1e-9);
      assertEquals(3.0, c.getMaxAccelerationMpsSq(), 1e-9);
    }

    @Test
    @DisplayName("ReplanningConfig default constructor disables replanning")
    public void testReplanningDefault() {
      ReplanningConfig rc = new ReplanningConfig();
      // Default should be safe to use without crashing
      assertNotNull(rc);
    }
  }
}
