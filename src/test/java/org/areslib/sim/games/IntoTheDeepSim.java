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

public class IntoTheDeepSim implements GameSimulation {
  private final List<Body> gamePieces = new ArrayList<>();
  private final Map<RobotSimState, Integer> robotHeldSamples = new HashMap<>();

  @Override
  public void initField(World<Body> physicsWorld) {
    // 144x144" FTC field. AdvantageScope centered at (0,0)
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

    // Initial Sample Spawns
    double[][] sampleSpawns = {{0.5, 0.5}, {0.5, 0.0}, {0.5, -0.5}};
    for (double[] spawn : sampleSpawns) {
      Body sample = new Body();
      sample.addFixture(Geometry.createRectangle(0.0889, 0.0381)); // 3.5" x 1.5" ITD Block
      sample.setMass(MassType.NORMAL);
      sample.translate(spawn[0], spawn[1]);
      physicsWorld.addBody(sample);
      gamePieces.add(sample);
    }
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
    for (int i = 0; i < robots.size(); i++) {
      RobotSimState robot = robots.get(i);
      int heldSamples = robotHeldSamples.getOrDefault(robot, 0);

      // ITD Intake Controls (Bumper mapped)
      boolean intakeBtn = robot.gamepad.right_bumper;
      boolean outtakeBtn = robot.gamepad.left_bumper;

      // Intake Spatial Origin (0.25m in front of robot)
      double rx = robot.robotBody.getTransform().getTranslationX();
      double ry = robot.robotBody.getTransform().getTranslationY();
      double theta = robot.robotBody.getTransform().getRotationAngle();

      double intakePointX = rx + Math.cos(theta) * 0.25;
      double intakePointY = ry + Math.sin(theta) * 0.25;

      // Process Intake Collisions
      if (intakeBtn) {
        Body toRemove = null;
        for (Body piece : gamePieces) {
          double dx = piece.getTransform().getTranslationX() - intakePointX;
          double dy = piece.getTransform().getTranslationY() - intakePointY;
          if (Math.sqrt(dx * dx + dy * dy) <= 0.15) { // 15cm capture radius
            toRemove = piece;
            break;
          }
        }
        if (toRemove != null) {
          gamePieces.remove(toRemove);
          physicsWorld.removeBody(toRemove);
          heldSamples++;
        }
      }

      // Process Outtake Displacement
      if (outtakeBtn && heldSamples > 0) {
        Body newPiece = new Body();
        newPiece.addFixture(Geometry.createRectangle(0.0889, 0.0381));
        newPiece.setMass(MassType.NORMAL);
        newPiece.translate(intakePointX, intakePointY);
        newPiece.getTransform().setRotation(theta);
        physicsWorld.addBody(newPiece);
        gamePieces.add(newPiece);
        heldSamples--;
      }

      robotHeldSamples.put(robot, heldSamples);

      // Log telemetry solely for the primary robot index for visual clarity tracking
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
      gamePieceArray[i * 7] = piece.getTransform().getTranslationX();
      gamePieceArray[i * 7 + 1] = piece.getTransform().getTranslationY();
      gamePieceArray[i * 7 + 2] = 0.019; // 0.75 inches up (half height of 1.5in block)

      double t = piece.getTransform().getRotationAngle();
      gamePieceArray[i * 7 + 3] = Math.cos(t / 2.0); // qw
      gamePieceArray[i * 7 + 4] = 0; // qx
      gamePieceArray[i * 7 + 5] = 0; // qy
      gamePieceArray[i * 7 + 6] = Math.sin(t / 2.0); // qz
    }
    AresTelemetry.putNumberArray("Field/Samples", gamePieceArray);
  }

  @Override
  public int getHeldSamples(RobotSimState robot) {
    return robotHeldSamples.getOrDefault(robot, 0);
  }
}
