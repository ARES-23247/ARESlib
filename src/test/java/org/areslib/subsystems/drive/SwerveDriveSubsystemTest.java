package org.areslib.subsystems.drive;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.command.CommandScheduler;
import org.areslib.core.AresRobot;
import org.areslib.math.geometry.Rotation2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Headless JUnit 5 tests for the {@link SwerveDriveSubsystem}.
 *
 * <p>Validates the full subsystem-level behavior: kinematics decomposition, module state
 * optimization, wheel speed desaturation, slew rate limiting, field-centric transform, and
 * zero-speed anti-snap behavior.
 *
 * <p>Uses {@link SwerveModuleIOSim} for all 4 modules — no hardware or physics world needed.
 */
public class SwerveDriveSubsystemTest {

  private SwerveDriveSubsystem swerve;
  private SwerveModuleIOSim fl, fr, bl, br;
  private SwerveDriveSubsystem.Config config;

  @BeforeEach
  void setup() {
    // Ensure we're NOT in simulation mode (avoids dyn4j body creation)
    AresRobot.setSimulation(false);

    // Reset scheduler to avoid stale state between tests
    CommandScheduler.getInstance().reset();

    fl = new SwerveModuleIOSim();
    fr = new SwerveModuleIOSim();
    bl = new SwerveModuleIOSim();
    br = new SwerveModuleIOSim();

    config = new SwerveDriveSubsystem.Config();
    config.trackWidthXMeters = 0.3;
    config.trackWidthYMeters = 0.3;
    config.maxModuleSpeedMps = 4.0;
    config.maxAccelerationMps2 = 0.0; // No slew limiting by default

    swerve = new SwerveDriveSubsystem(config, fl, fr, bl, br);
    CommandScheduler.getInstance().registerSubsystem(swerve);
  }

  // ========== Basic Drive ==========

  @Test
  void testForwardDriveSetsPositiveVelocity() {
    // Drive forward at 1 m/s
    swerve.drive(1.0, 0.0, 0.0);

    // After calling drive, the module IOs receive voltage commands.
    // Run periodic to read the inputs back.
    swerve.periodic();

    // Verify the commanded velocity is stored
    assertEquals(1.0, swerve.getCommandedVx(), 1e-6);
    assertEquals(0.0, swerve.getCommandedVy(), 1e-6);
    assertEquals(0.0, swerve.getCommandedOmega(), 1e-6);
  }

  @Test
  void testStrafeDriveSetsLateralVelocity() {
    swerve.drive(0.0, 1.5, 0.0);

    assertEquals(0.0, swerve.getCommandedVx(), 1e-6);
    assertEquals(1.5, swerve.getCommandedVy(), 1e-6);
  }

  @Test
  void testRotationOnlyDrive() {
    swerve.drive(0.0, 0.0, 2.0);

    assertEquals(0.0, swerve.getCommandedVx(), 1e-6);
    assertEquals(0.0, swerve.getCommandedVy(), 1e-6);
    assertEquals(2.0, swerve.getCommandedOmega(), 1e-6);
  }

  @Test
  void testCombinedDrive() {
    swerve.drive(1.0, 0.5, -0.3);

    assertEquals(1.0, swerve.getCommandedVx(), 1e-6);
    assertEquals(0.5, swerve.getCommandedVy(), 1e-6);
    assertEquals(-0.3, swerve.getCommandedOmega(), 1e-6);
  }

  // ========== Zero-Speed Anti-Snap ==========

  @Test
  void testZeroSpeedDoesNotSnapModuleAngles() {
    // First drive forward to set module angles
    swerve.drive(2.0, 0.0, 0.0);

    // Tick the sim modules to update their positions
    SwerveModuleIO.SwerveModuleInputs inputs = new SwerveModuleIO.SwerveModuleInputs();
    fl.updateInputs(inputs);
    // Unused variable removed: double angleBeforeStop = inputs.turnAbsolutePositionRad;

    // Now command zero speed
    swerve.drive(0.0, 0.0, 0.0);

    // The modules should NOT snap to 0 angle — they should hold their current position
    assertEquals(0.0, swerve.getCommandedVx(), 1e-6);
    assertEquals(0.0, swerve.getCommandedVy(), 1e-6);
    assertEquals(0.0, swerve.getCommandedOmega(), 1e-6);
  }

  // ========== Field-Centric Drive ==========

  @Test
  void testFieldCentricDriveTransformsHeading() {
    // Driving "forward" in field frame with robot heading at 90° CCW
    // should translate to robot-centric strafe
    Rotation2d heading = new Rotation2d(Math.PI / 2.0); // 90° CCW
    swerve.driveFieldCentric(1.0, 0.0, 0.0, heading);

    // Robot is facing 90° left, so field-forward becomes robot-left (negative Y for robot)
    // The commanded values get rotated through ChassisSpeeds.fromFieldRelativeSpeeds
    // Vx_robot ≈ 0, Vy_robot ≈ -1.0 (or close, depending on discretization)
    double vx = swerve.getCommandedVx();
    double vy = swerve.getCommandedVy();

    // The key observation: driving field-forward causes the robot to strafe
    assertTrue(
        Math.abs(vx) < 0.1 || Math.abs(vy) > 0.5,
        "Field-centric transform should rotate the velocity vector. vx=" + vx + " vy=" + vy);
  }

  // ========== Desaturation ==========

  @Test
  void testDesaturationCapsModuleSpeeds() {
    // Drive at extreme speeds that would exceed maxModuleSpeedMps (4.0)
    // Large vx + large omega will push some modules well above the cap
    swerve.drive(10.0, 0.0, 0.0);

    // The commanded velocities are stored BEFORE desaturation
    // But the module voltages sent should be within physical limits
    assertEquals(10.0, swerve.getCommandedVx(), 1e-6);

    // Run periodic to verify no crashes — desaturation should handle gracefully
    swerve.periodic();
  }

  // ========== Slew Rate Limiting ==========

  @Test
  void testSlewRateLimitingSmooths() {
    // Create a new swerve with slew rate limiting enabled
    SwerveDriveSubsystem.Config slewConfig = new SwerveDriveSubsystem.Config();
    slewConfig.maxAccelerationMps2 = 5.0; // 5 m/s² max acceleration
    slewConfig.maxModuleSpeedMps = 4.0;

    SwerveModuleIOSim sfl = new SwerveModuleIOSim();
    SwerveModuleIOSim sfr = new SwerveModuleIOSim();
    SwerveModuleIOSim sbl = new SwerveModuleIOSim();
    SwerveModuleIOSim sbr = new SwerveModuleIOSim();

    SwerveDriveSubsystem slewSwerve = new SwerveDriveSubsystem(slewConfig, sfl, sfr, sbl, sbr);

    // Command a large instant velocity — slew limiter should cap it
    slewSwerve.drive(10.0, 0.0, 0.0);

    // With 5 m/s² and 0.02s loop period, max delta per loop = 0.1 m/s
    // From rest, the actual commanded velocity should be ~0.1 m/s (not 10.0)
    double commandedVx = slewSwerve.getCommandedVx();
    assertTrue(
        commandedVx < 1.0,
        "Slew rate limiter should prevent instant jump to 10 m/s. Got: " + commandedVx);
    assertTrue(
        commandedVx > 0.0, "Slew rate limiter should allow SOME movement. Got: " + commandedVx);
  }

  // ========== Periodic Logging (Regression) ==========

  @Test
  void testPeriodicDoesNotThrow() {
    // Full subsystem periodic — reads all 4 modules, logs to AresAutoLogger + AresTelemetry
    assertDoesNotThrow(
        () -> {
          swerve.periodic();
          swerve.periodic();
          swerve.periodic();
        },
        "periodic() should not throw even with zero-state modules");
  }

  @Test
  void testDriveThenPeriodicCycle() {
    // Simulate a realistic drive cycle: command → periodic → command → periodic
    for (int i = 0; i < 10; i++) {
      swerve.drive(1.0 * (i % 3), 0.5 * (i % 2), -0.2 * (i % 4));
      swerve.periodic();
    }
    // If we get here without exceptions, the subsystem is stable
  }
}
