package org.areslib.core.simulation;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

/**
 * Mathematically pseudo-simulates a 3D projectile launched from a robot. Follows zero-allocation
 * principles by pre-computing trajectories and reusing arrays where possible.
 */
public class ProjectileSimulation {

  // Standard projectile gravity in m/s^2 (adjust if necessary based on real-world measurements
  // backing dyn4j scale)
  public static final double GRAVITY = 11.0;

  private final double initialX;
  private final double initialY;
  private final double initialZ; // Height

  private final double initialVelX;
  private final double initialVelY;
  private final double initialVelZ;

  private double timeElapsedSec = 0.0;
  private boolean isGrounded = false;
  private double touchGroundHeightOffset = 0.0635; // Default to DECODE Artifact Radius

  /**
   * Constructs a ProjectileSimulation using 3D position and velocity arrays to avoid object
   * allocation.
   *
   * @param initialPos Array containing {x, y, z} in meters
   * @param initialVel Array containing {vx, vy, vz} in meters per second
   */
  public ProjectileSimulation(double[] initialPos, double[] initialVel) {
    this.initialX = initialPos[0];
    this.initialY = initialPos[1];
    this.initialZ = initialPos[2];

    this.initialVelX = initialVel[0];
    this.initialVelY = initialVel[1];
    this.initialVelZ = initialVel[2];
  }

  public void setTouchGroundHeightOffset(double touchGroundHeightOffset) {
    this.touchGroundHeightOffset = touchGroundHeightOffset;
  }

  /**
   * Steps the simulation time forward.
   *
   * @param dtSeconds time passed in seconds
   */
  public void step(double dtSeconds) {
    if (isGrounded) return;
    timeElapsedSec += dtSeconds;

    double currentZ =
        initialZ + initialVelZ * timeElapsedSec - 0.5 * GRAVITY * timeElapsedSec * timeElapsedSec;
    double currentVelZ = initialVelZ - GRAVITY * timeElapsedSec;

    if (currentZ <= touchGroundHeightOffset && currentVelZ < 0) {
      isGrounded = true;
    }
  }

  public boolean isGrounded() {
    return isGrounded;
  }

  public boolean hasGoneOutOfField() {
    double currentX = initialX + initialVelX * timeElapsedSec;
    double currentY = initialY + initialVelY * timeElapsedSec;
    // FTC field is +- 1.83m from center
    final double edgeTolerance = 0.5;
    return currentX < (-1.83 - edgeTolerance)
        || currentX > (1.83 + edgeTolerance)
        || currentY < (-1.83 - edgeTolerance)
        || currentY > (1.83 + edgeTolerance);
  }

  /**
   * Populates the passed array with the current {x,y,z} pseudo-3D location.
   *
   * @param outPos A pre-allocated double[3] to store the current position
   */
  public void getCurrentPosition(double[] outPos) {
    outPos[0] = initialX + initialVelX * timeElapsedSec;
    outPos[1] = initialY + initialVelY * timeElapsedSec;
    outPos[2] =
        initialZ + initialVelZ * timeElapsedSec - 0.5 * GRAVITY * timeElapsedSec * timeElapsedSec;
  }

  /**
   * Converts this active projectile back into a Dyn4j simulated Field Body.
   *
   * @param worldWrapper the global simulation wrapper
   */
  public void spawnPhysicalBody(AresPhysicsWorld worldWrapper) {
    double currentX = initialX + initialVelX * timeElapsedSec;
    double currentY = initialY + initialVelY * timeElapsedSec;

    Body artifact = new Body();
    BodyFixture fixture =
        artifact.addFixture(Geometry.createCircle(DecodeFieldSim.ARTIFACT_RADIUS_METERS));

    fixture.setDensity(50.0);
    fixture.setFriction(0.3);
    fixture.setRestitution(0.6);

    artifact.translate(currentX, currentY);
    artifact.setMass(MassType.NORMAL);
    artifact.setUserData("DECODE_ARTIFACT");

    artifact.setLinearVelocity(initialVelX, initialVelY);

    artifact.setLinearDamping(1.5);
    artifact.setAngularDamping(1.5);

    worldWrapper.addBody(artifact);
  }
}
