package org.areslib.math.kinematics;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;

class KinematicAimingTest {

  private static final double EPSILON = 1e-4;

  @Test
  void testStationaryRobot() {
    ChassisSpeeds zeroSpeed = new ChassisSpeeds(0, 0, 0);
    Translation2d robotPos = new Translation2d(0, 0);
    Translation2d targetPos = new Translation2d(10, 0);

    // Robot stationary, target 10m directly ahead (+X).
    KinematicAiming.AimResult result =
        KinematicAiming.calculateAim(zeroSpeed, robotPos, targetPos, 20.0);

    assertTrue(result.isValid);
    assertEquals(0.5, result.timeOfFlight, EPSILON); // 10m / 20m/s
    assertEquals(0.0, result.requiredHeading.getRadians(), EPSILON); // Aim exactly at target
    assertEquals(10.0, result.virtualTarget.getX(), EPSILON);
    assertEquals(0.0, result.virtualTarget.getY(), EPSILON);
  }

  @Test
  void testMovingOrthogonally() {
    // Robot moves at 5m/s in +Y (strafing left)
    ChassisSpeeds strafeSpeed = new ChassisSpeeds(0, 5.0, 0);
    Translation2d robotPos = new Translation2d(0, 0);
    Translation2d targetPos = new Translation2d(10, 0); // Target straight ahead (+X)

    KinematicAiming.AimResult result =
        KinematicAiming.calculateAim(strafeSpeed, robotPos, targetPos, 20.0);

    assertTrue(result.isValid);

    // The robot is moving +Y. In order for the projectile to hit the target,
    // it must be fired with a -Y component to cancel out the robot's +Y motion.
    // Therefore, the virtual target and required heading should lean towards -Y (negative angle).
    assertTrue(result.requiredHeading.getRadians() < 0.0);
    assertTrue(result.virtualTarget.getY() < 0.0);
  }

  @Test
  void testMovingAwayImpossible() {
    // Robot moves at 30m/s away from the target
    ChassisSpeeds escapeSpeed = new ChassisSpeeds(-30.0, 0, 0);
    Translation2d robotPos = new Translation2d(0, 0);
    Translation2d targetPos = new Translation2d(10, 0);

    // Projectile is only 20m/s. Conceptually impossible to hit.
    KinematicAiming.AimResult result =
        KinematicAiming.calculateAim(escapeSpeed, robotPos, targetPos, 20.0);

    assertFalse(result.isValid, "Should be marked invalid if physically impossible");
  }
}
