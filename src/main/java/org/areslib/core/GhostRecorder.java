package org.areslib.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.telemetry.AresAutoLogger;

/**
 * Utility for recording a teleop sequence (Ghost Mode) into a serializable macro.
 *
 * <p>Samples robot velocities and specified binary inputs at deterministic rates and exports them
 * to a JSON file on the local filesystem.
 */
public class GhostRecorder {

  private final GhostData m_data;
  private final Supplier<ChassisSpeeds> m_speedsSupplier;
  private final Supplier<Boolean>[] m_booleanSuppliers;

  private boolean m_isRecording = false;

  /**
   * Constructs a macro recorder.
   *
   * @param speedsSupplier Supplier that provides the current commanding robot ChassisSpeeds.
   * @param booleanSuppliers A varargs array of booleans to track (e.g., intake button, shoot
   *     button). They will be assigned IDs 0, 1, 2... based on varargs order.
   */
  @SafeVarargs
  public GhostRecorder(
      Supplier<ChassisSpeeds> speedsSupplier, Supplier<Boolean>... booleanSuppliers) {
    m_data = new GhostData(AresRobot.LOOP_PERIOD_SECS);
    m_speedsSupplier = speedsSupplier;
    m_booleanSuppliers = booleanSuppliers;
  }

  /** Starts or clears the recording. */
  public void startRecording() {
    m_data.vxMetersPerSecond.clear();
    m_data.vyMetersPerSecond.clear();
    m_data.omegaRadiansPerSecond.clear();
    m_data.buttonMasks.clear();
    m_isRecording = true;
    AresAutoLogger.recordOutput("GhostMode/Recording", 1.0);
  }

  /** Should be called every robot loop (~20ms). Samples data if recording is active. */
  public void update() {
    if (!m_isRecording) return;

    ChassisSpeeds speeds = m_speedsSupplier.get();
    int mask = 0;
    for (int i = 0; i < m_booleanSuppliers.length; i++) {
      if (m_booleanSuppliers[i].get()) {
        mask |= (1 << i);
      }
    }

    m_data.pushFrame(
        speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond, mask);
  }

  /**
   * Stops recording and saves the JSON file to disk.
   *
   * @param filePath The absolute path on the Control Hub (e.g., "/sdcard/FIRST/macros/auto1.json").
   */
  public void stopAndSave(String filePath) {
    m_isRecording = false;
    AresAutoLogger.recordOutput("GhostMode/Recording", 0.0);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try {
      File file = new File(filePath);
      file.getParentFile().mkdirs();
      try (FileWriter writer = new FileWriter(file)) {
        gson.toJson(m_data, writer);
        com.qualcomm.robotcore.util.RobotLog.i("Saved macro to: " + filePath);
      }
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }
}
