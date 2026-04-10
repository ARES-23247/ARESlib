package org.areslib.command;

import java.util.function.Consumer;
import org.areslib.core.GhostRecorder;
import org.areslib.math.kinematics.ChassisSpeeds;

/**
 * An autonomous command that loads a previously recorded CSV ghost macro and plays it back.
 *
 * <p>Feeds logged speeds back to the drivetrain and triggers binary actions at the exact timestamps
 * they were originally pressed in teleop. Relies on {@link GhostRecorder} for parsing.
 */
public class GhostPlaybackCommand extends Command {

  private final String m_filePath;
  private final GhostRecorder m_recorder;

  private final Consumer<ChassisSpeeds> m_driveOutput;
  private final Consumer<Boolean>[] m_booleanOutputs;

  /**
   * Constructs a playback command.
   *
   * @param csvFilePath The absolute path to the CSV macro file.
   * @param recorder A GhostRecorder instance to handle file parsing and timing.
   * @param driveOutput Consumer to feed the recorded ChassisSpeeds into the drivetrain.
   * @param booleanOutputs Varargs array of boolean consumers mapping to the buttons tracked during
   *     recording.
   */
  @SafeVarargs
  public GhostPlaybackCommand(
      String csvFilePath,
      GhostRecorder recorder,
      Consumer<ChassisSpeeds> driveOutput,
      Consumer<Boolean>... booleanOutputs) {
    m_filePath = csvFilePath;
    m_recorder = recorder;
    m_driveOutput = driveOutput;
    m_booleanOutputs = booleanOutputs;
  }

  @Override
  public void initialize() {
    boolean success = m_recorder.loadForPlayback(m_filePath);
    if (!success) {
      com.qualcomm.robotcore.util.RobotLog.e("GhostPlaybackCommand failed to load: " + m_filePath);
    } else {
      m_recorder.startPlayback();
    }
  }

  @Override
  public void execute() {
    if (!m_recorder.isPlaying()) return;

    // Apply drive velocities
    m_driveOutput.accept(m_recorder.getPlaybackSpeeds());

    // Apply boolean outputs inline
    for (int i = 0; i < m_booleanOutputs.length; i++) {
      m_booleanOutputs[i].accept(m_recorder.getPlaybackButton(i));
    }
  }

  @Override
  public boolean isFinished() {
    return !m_recorder.isPlaying() || m_recorder.isPlaybackFinished();
  }

  @Override
  public void end(boolean interrupted) {
    m_recorder.stopPlayback();
    m_driveOutput.accept(new ChassisSpeeds(0, 0, 0));
    for (Consumer<Boolean> output : m_booleanOutputs) {
      output.accept(false);
    }
  }
}
