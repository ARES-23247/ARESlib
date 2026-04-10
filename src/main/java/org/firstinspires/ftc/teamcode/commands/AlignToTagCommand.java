package org.firstinspires.ftc.teamcode.commands;

import static org.firstinspires.ftc.teamcode.Constants.AlignConstants.*;

import org.areslib.command.Command;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.areslib.subsystems.vision.AresVisionSubsystem;

/**
 * AlignToTagCommand standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * AlignToTagCommand}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class AlignToTagCommand extends Command {
  private final SwerveDriveSubsystem drive;
  private final AresVisionSubsystem vision;

  // Proportional Gains (Teams should tune these real-world limits)
  private final double targetArea;

  public AlignToTagCommand(
      SwerveDriveSubsystem drive, AresVisionSubsystem vision, double targetAreaPercent) {
    this.drive = drive;
    this.vision = vision;
    this.targetArea = targetAreaPercent;
    addRequirements(drive);
  }

  @Override
  public void execute() {
    if (vision.hasTarget()) {
      double tx = vision.getTargetXOffset();
      double ta = vision.getTargetArea();

      // tx is in DEGREES (±29.8° FOV). ALIGN_kPx converts degrees → m/s.
      // At max offset (29.8°): strafeCmd ≈ 29.8 * 0.05 = 1.49 m/s.
      double strafeCmd = -tx * ALIGN_kPx;

      // ta is in PERCENT of image area. ALIGN_kPy converts area-error% → m/s.
      // At 5% target with 0% actual: forwardCmd = 5.0 * 0.15 = 0.75 m/s.
      double forwardCmd = (targetArea - ta) * ALIGN_kPy;

      // Send autonomous velocities to drive base, zero rotation
      drive.drive(forwardCmd, strafeCmd, 0.0);
    } else {
      // Stop if no target found to avoid blind flight
      drive.drive(0.0, 0.0, 0.0);
    }
  }

  @Override
  public void end(boolean interrupted) {
    drive.drive(0.0, 0.0, 0.0);
  }

  @Override
  public boolean isFinished() {
    // Run continuously while button is held
    return false;
  }
}
