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

  private final String filePath;
  private final GhostRecorder recorder;

  private final Consumer<ChassisSpeeds> driveOutput;
  private final Consumer<Boolean>[] booleanOutputs;

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
    filePath = csvFilePath;
    this.recorder = recorder;
    this.driveOutput = driveOutput;
    this.booleanOutputs = booleanOutputs;
  }

  @Override
  public void initialize() {
    boolean success = recorder.loadForPlayback(filePath);
    if (!success) {
      com.qualcomm.robotcore.util.RobotLog.e("GhostPlaybackCommand failed to load: " + filePath);
    } else {
      recorder.startPlayback();
    }
  }

  @Override
  public void execute() {
    if (!recorder.isPlaying()) return;

    // Apply drive velocities
    driveOutput.accept(recorder.getPlaybackSpeeds());

    // Apply boolean outputs inline
    for (int i = 0; i < booleanOutputs.length; i++) {
      booleanOutputs[i].accept(recorder.getPlaybackButton(i));
    }
  }

  @Override
  public boolean isFinished() {
    return !recorder.isPlaying() || recorder.isPlaybackFinished();
  }

  @Override
  public void end(boolean interrupted) {
    recorder.stopPlayback();
    driveOutput.accept(new ChassisSpeeds(0, 0, 0));
    for (Consumer<Boolean> output : booleanOutputs) {
      output.accept(false);
    }
  }
}
