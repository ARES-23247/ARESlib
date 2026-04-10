package org.areslib.sim.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.areslib.telemetry.AresTelemetry;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.world.World;

public class DecodeFieldSim implements GameSimulation {
  private final List<Body> gamePieces = new ArrayList<>();
  private final Map<RobotSimState, Integer> robotHeldSamples = new HashMap<>();

  // Virtual Z-axis wrapper
  static class Artifact3DState {
    double z;
    double vz;

    enum State {
      ON_GROUND,
      IN_AIR,
      IN_RAMP
    };

    State state;

    Artifact3DState(double z, double vz, State state) {
      this.z = z;
      this.vz = vz;
      this.state = state;
    }
  }

  private final Map<Body, Artifact3DState> artifactStates = new HashMap<>();

  private final List<Body> redRamp = new ArrayList<>();
  private final List<Body> blueRamp = new ArrayList<>();

  @Override
  public void initField(World<Body> physicsWorld) {
    // 144x144" FTC field
    double fieldSize = 3.6576;
    double halfField = fieldSize / 2.0;
    double wallThick = 0.1;

    createWall(
        physicsWorld,
        wallThick,
        fieldSize + wallThick * 2,
        -halfField - wallThick / 2.0,
        0); // Left
    createWall(
        physicsWorld,
        wallThick,
        fieldSize + wallThick * 2,
        halfField + wallThick / 2.0,
        0); // Right
    createWall(
        physicsWorld,
        fieldSize + wallThick * 2,
        wallThick,
        0,
        -halfField - wallThick / 2.0); // Bottom
    createWall(
        physicsWorld, fieldSize + wallThick * 2, wallThick, 0, halfField + wallThick / 2.0); // Top

    // Obelisk is outside the field perimeter, so it doesn't need a collision body.

    // 4 rows of 3 balls (Artifacts) on each side
    double[] rowsXRed = {0.6, 0.9, 1.2, 1.5};
    double[] rowsXBlue = {-0.6, -0.9, -1.2, -1.5};
    double[] colsY = {-0.6, 0.0, 0.6};

    for (double x : rowsXRed) {
      for (double y : colsY) {
        spawnArtifact(physicsWorld, x, y);
      }
    }
    for (double x : rowsXBlue) {
      for (double y : colsY) {
        spawnArtifact(physicsWorld, x, y);
      }
    }

    spawnArtifact(physicsWorld, 1.6, -1.4);
    spawnArtifact(physicsWorld, 1.6, -1.5);
    spawnArtifact(physicsWorld, 1.6, -1.6);

    spawnArtifact(physicsWorld, -1.6, 1.4);
    spawnArtifact(physicsWorld, -1.6, 1.5);
    spawnArtifact(physicsWorld, -1.6, 1.6);
  }

  private void spawnArtifact(World<Body> physicsWorld, double x, double y) {
    Body sample = new Body();
    // Using circle bounding to mimic the 5" artifacts
    org.dyn4j.dynamics.BodyFixture fixture = sample.addFixture(Geometry.createCircle(0.0635));
    fixture.setDensity(50.0);
    fixture.setFriction(0.3);
    fixture.setRestitution(0.6);

    sample.setMass(MassType.NORMAL);
    sample.translate(x, y);
    sample.setLinearDamping(1.5);
    sample.setAngularDamping(1.5);
    physicsWorld.addBody(sample);
    gamePieces.add(sample);
    artifactStates.put(sample, new Artifact3DState(0.0635, 0.0, Artifact3DState.State.ON_GROUND));
  }

  private void createWall(World<Body> world, double w, double h, double x, double y) {
    Body wall = new Body();
    wall.addFixture(Geometry.createRectangle(w, h));
    wall.translate(x, y);
    wall.setMass(MassType.INFINITE);
    world.addBody(wall);
  }

  @Override
  public void updateField(World<Body> physicsWorld, List<RobotSimState> robots) {
    double dt = 0.02; // Assuming ~50hz update step

    // --- 1. Process Artifact Flight & Interactions ---
    for (Body piece : gamePieces) {
      Artifact3DState state = artifactStates.get(piece);
      if (state == null) continue;

      if (state.state == Artifact3DState.State.IN_AIR) {
        state.z += state.vz * dt;
        state.vz -= 9.8 * dt; // Simulate Gravity

        double px = piece.getTransform().getTranslationX();
        double py = piece.getTransform().getTranslationY();

        // Check if it enters the Red Goal bounds (Top Right)
        if (px > 1.2 && py > 1.2 && state.vz < 0 && state.z < 0.6) {
          state.state = Artifact3DState.State.IN_RAMP;
          piece.getFixture(0).setSensor(true);
          piece.setLinearVelocity(0, 0);
          redRamp.add(piece);
          double targetX = 1.6 - (redRamp.size() - 1) * 0.13;
          piece.translate(targetX - px, 1.6 - py); // Stack sequentially
          continue;
        }

        // Check if it enters Blue Goal bounds (Bottom Left)
        if (px < -1.2 && py < -1.2 && state.vz < 0 && state.z < 0.6) {
          state.state = Artifact3DState.State.IN_RAMP;
          piece.getFixture(0).setSensor(true);
          piece.setLinearVelocity(0, 0);
          blueRamp.add(piece);
          double targetX = -1.6 + (blueRamp.size() - 1) * 0.13;
          piece.translate(targetX - px, -1.6 - py);
          continue;
        }

        // Bounce off field perimeter walls if IN_AIR (since sensor=true bypasses physics walls)
        org.dyn4j.geometry.Vector2 vel = piece.getLinearVelocity();
        double halfField = 3.6576 / 2.0;
        if (Math.abs(px) > halfField - 0.0635) {
          piece.setLinearVelocity(-vel.x * 0.7, vel.y); // dampen and reverse
          piece.translate(Math.signum(px) * (halfField - 0.0635) - px, 0); // push back inside
        }
        if (Math.abs(py) > halfField - 0.0635) {
          piece.setLinearVelocity(vel.x, -vel.y * 0.7); // dampen and reverse
          piece.translate(0, Math.signum(py) * (halfField - 0.0635) - py); // push back inside
        }

        if (state.z <= 0.0635) { // Radius threshold (hit the ground)
          state.z = 0.0635;
          state.vz = 0.0;
          state.state = Artifact3DState.State.ON_GROUND;
          piece.getFixture(0).setSensor(false);
        }
      }
    }

    // --- 2. Process Robot Actions ---
    for (int i = 0; i < robots.size(); i++) {
      RobotSimState robot = robots.get(i);
      int heldSamples = robotHeldSamples.getOrDefault(robot, 0);

      boolean intakeBtn = robot.gamepad.right_bumper;
      boolean outtakeBtn = robot.gamepad.left_bumper;

      double rx = robot.robotBody.getTransform().getTranslationX();
      double ry = robot.robotBody.getTransform().getTranslationY();
      double theta = robot.robotBody.getTransform().getRotationAngle();

      double intakePointX = rx + Math.cos(theta) * 0.25;
      double intakePointY = ry + Math.sin(theta) * 0.25;

      // Unload Ramp Lever Intersections
      // Using coordinates roughly corresponding to standard wall alignments next to classifiers
      if (rx > 1.0 && rx < 1.4 && ry > 1.0 && ry < 1.4) {
        if (!redRamp.isEmpty()) {
          Body b = redRamp.remove(0);
          b.getFixture(0).setSensor(false);
          b.setLinearVelocity(-1.5, -1.5); // Push gently out from the ramp
          artifactStates.get(b).state = Artifact3DState.State.ON_GROUND;
          // Shift remaining balls down the ramp
          for (int j = 0; j < redRamp.size(); j++) {
            Body rem = redRamp.get(j);
            double rx_old = rem.getTransform().getTranslationX();
            rem.translate((1.6 - j * 0.13) - rx_old, 0);
          }
        }
      }
      if (rx > -1.4 && rx < -1.0 && ry > -1.4 && ry < -1.0) {
        if (!blueRamp.isEmpty()) {
          Body b = blueRamp.remove(0);
          b.getFixture(0).setSensor(false);
          b.setLinearVelocity(1.5, 1.5);
          artifactStates.get(b).state = Artifact3DState.State.ON_GROUND;
          // Shift remaining balls down the ramp
          for (int j = 0; j < blueRamp.size(); j++) {
            Body rem = blueRamp.get(j);
            double rx_old = rem.getTransform().getTranslationX();
            rem.translate((-1.6 + j * 0.13) - rx_old, 0);
          }
        }
      }

      if (intakeBtn && heldSamples < 3) {
        Body toRemove = null;
        for (Body piece : gamePieces) {
          Artifact3DState state = artifactStates.get(piece);
          if (state == null || state.state != Artifact3DState.State.ON_GROUND) continue;

          double dx = piece.getTransform().getTranslationX() - intakePointX;
          double dy = piece.getTransform().getTranslationY() - intakePointY;
          if (Math.sqrt(dx * dx + dy * dy) <= 0.25) {
            toRemove = piece;
            break;
          }
        }
        if (toRemove != null) {
          gamePieces.remove(toRemove);
          artifactStates.remove(toRemove);
          physicsWorld.removeBody(toRemove);
          heldSamples++;
        }
      }

      // Ball Shooting Mechanics
      if (outtakeBtn && heldSamples > 0) {
        Body newPiece = new Body();
        newPiece.addFixture(Geometry.createCircle(0.0635));
        newPiece.setMass(MassType.NORMAL);
        newPiece.translate(intakePointX, intakePointY);

        // Compute High-Velocity Shoot Vector
        double force = 8.0;
        newPiece.setLinearVelocity(Math.cos(theta) * force, Math.sin(theta) * force);

        newPiece.getFixture(0).setSensor(true); // Disable physical bumping while in air
        physicsWorld.addBody(newPiece);
        gamePieces.add(newPiece);

        artifactStates.put(newPiece, new Artifact3DState(0.3, 3.5, Artifact3DState.State.IN_AIR));
        heldSamples--;
      }

      robotHeldSamples.put(robot, heldSamples);

      if (i == 0) {
        AresTelemetry.putNumber("Robot/HeldSamples", heldSamples);
        AresTelemetry.putNumberArray(
            "Debug/IntakePoint", new double[] {intakePointX, intakePointY, 0.0, 1.0, 0, 0, 0});
      }
    }
  }

  @Override
  public void telemetryUpdate() {
    double[] gamePieceArray = new double[gamePieces.size() * 7];
    for (int i = 0; i < gamePieces.size(); i++) {
      Body piece = gamePieces.get(i);
      Artifact3DState state = artifactStates.get(piece);

      gamePieceArray[i * 7] = piece.getTransform().getTranslationX();
      gamePieceArray[i * 7 + 1] = piece.getTransform().getTranslationY();
      gamePieceArray[i * 7 + 2] = (state != null) ? state.z : 0.0635;

      double t = piece.getTransform().getRotationAngle();
      gamePieceArray[i * 7 + 3] = Math.cos(t / 2.0);
      gamePieceArray[i * 7 + 4] = 0;
      gamePieceArray[i * 7 + 5] = 0;
      gamePieceArray[i * 7 + 6] = Math.sin(t / 2.0);
    }
    AresTelemetry.putNumberArray("Field/Samples", gamePieceArray);
  }

  @Override
  public int getHeldSamples(RobotSimState robot) {
    return robotHeldSamples.getOrDefault(robot, 0);
  }
}
