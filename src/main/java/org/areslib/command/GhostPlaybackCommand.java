package org.areslib.command;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;
import org.areslib.core.AresRobot;
import org.areslib.core.GhostData;
import org.areslib.math.kinematics.ChassisSpeeds;

/**
 * An autonomous command that loads a previously recorded JSON teleop macro and plays it back.
 *
 * <p>Feeds logged speeds back to the drivetrain and triggers binary actions at the exact loops they
 * were originally pressed in teleop.
 */
public class GhostPlaybackCommand extends Command {

  private final String m_filePath;
  private GhostData m_data;

  private final Consumer<ChassisSpeeds> m_driveOutput;
  private final Consumer<Boolean>[] m_booleanOutputs;

  private int m_frameIndex = 0;
  private double m_accumulator = 0.0;

  /**
   * Constructs a playback command.
   *
   * @param jsonFilePath The absolute path to the JSON macro file.
   * @param driveOutput Consumer to feed the recorded ChassisSpeeds into the drivetrain.
   * @param booleanOutputs Varargs array of boolean consumers mapping to the buttons tracked during
   *     recording.
   */
  @SafeVarargs
  public GhostPlaybackCommand(
      String jsonFilePath,
      Consumer<ChassisSpeeds> driveOutput,
      Consumer<Boolean>... booleanOutputs) {
    m_filePath = jsonFilePath;
    m_driveOutput = driveOutput;
    m_booleanOutputs = booleanOutputs;
  }

  @Override
  public void initialize() {
    m_frameIndex = 0;
    m_accumulator = 0.0;

    File file = new File(m_filePath);
    if (file.exists()) {
      try (FileReader reader = new FileReader(file)) {
        Gson gson = new Gson();
        m_data = gson.fromJson(reader, GhostData.class);
      } catch (IOException e) {
        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
        m_data = null;
      }
    } else {
      com.qualcomm.robotcore.util.RobotLog.e(
          String.valueOf("Ghost macro file not found: " + m_filePath));
      m_data = null;
    }
  }

  @Override
  public void execute() {
    if (m_data == null || m_data.vxMetersPerSecond.isEmpty()) {
      return;
    }

    m_accumulator += AresRobot.LOOP_PERIOD_SECS;

    // Catch up frame index if loops drop
    while (m_frameIndex < m_data.vxMetersPerSecond.size() - 1
        && (m_frameIndex * m_data.periodSeconds) < m_accumulator) {
      m_frameIndex++;
    }

    if (m_frameIndex < m_data.vxMetersPerSecond.size()) {
      // Apply drive velocities
      double vx = m_data.vxMetersPerSecond.get(m_frameIndex);
      double vy = m_data.vyMetersPerSecond.get(m_frameIndex);
      double omega = m_data.omegaRadiansPerSecond.get(m_frameIndex);
      m_driveOutput.accept(new ChassisSpeeds(vx, vy, omega));

      // Apply boolean masks
      int mask = m_data.buttonMasks.get(m_frameIndex);
      for (int i = 0; i < m_booleanOutputs.length; i++) {
        boolean isActive = (mask & (1 << i)) != 0;
        m_booleanOutputs[i].accept(isActive);
      }
    }
  }

  @Override
  public boolean isFinished() {
    if (m_data == null) return true;
    return m_frameIndex >= m_data.vxMetersPerSecond.size() - 1;
  }

  @Override
  public void end(boolean interrupted) {
    m_driveOutput.accept(new ChassisSpeeds(0, 0, 0));
    for (Consumer<Boolean> output : m_booleanOutputs) {
      output.accept(false);
    }
  }
}
