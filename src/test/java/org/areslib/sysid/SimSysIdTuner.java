package org.areslib.sysid;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ejml.simple.SimpleMatrix;

/**
 * Offline System Identification tuner for ARESLib, ported from MARSLib.
 *
 * <p>Parses SysId exported CSV data (from {@link SysIdJSONExporter}) and performs OLS linear
 * regression to calculate mechanistic constants (kS, kV, kA). Then back-calculates the true
 * simulated Mass / Moment of Inertia for injection into dyn4j physics bodies.
 *
 * <p><b>Workflow:</b>
 *
 * <ol>
 *   <li>Run a SysId routine on the robot using {@link SysIdRoutine}
 *   <li>Export data using {@link SysIdJSONExporter} (produces a CSV file)
 *   <li>Copy the CSV to your laptop
 *   <li>Run this class's {@code main()} method from your IDE to compute constants
 *   <li>Plug the calculated kS/kV/kA into your subsystem constants
 * </ol>
 *
 * <p><b>Dependencies:</b> This class requires {@code org.ejml:ejml-simple} (linear algebra). It is
 * included as a {@code testImplementation} dependency, so it runs on desktop JVM only — never on
 * the robot.
 *
 * <p><b>Mathematical Model:</b> The standard linear DC motor model is: {@code V = kS*sign(vel) +
 * kV*vel + kA*accel}. OLS regression solves for the [kS, kV, kA] coefficient vector that minimizes
 * the squared residuals of this equation over all recorded data points.
 */
@SuppressWarnings({
  "PMD.SystemPrintln",
  "PMD.AvoidInstantiatingObjectsInLoops",
  "PMD.GuardLogStatement"
})
public class SimSysIdTuner {

  private static final Logger LOGGER = Logger.getLogger(SimSysIdTuner.class.getName());

  /** A single synchronized telemetry sample of time, velocity, and voltage. */
  public static class DataPoint {
    public final double time;
    public final double velocity;
    public final double voltage;

    public DataPoint(double time, double velocity, double voltage) {
      this.time = time;
      this.velocity = velocity;
      this.voltage = voltage;
    }
  }

  /**
   * Executes OLS regression over a SysId CSV export file.
   *
   * <p>The CSV must have a header row and columns: {@code time,voltage,velocity}.
   *
   * @param csvFilePath Path to the SysId CSV export file.
   * @param gearing Motor-to-mechanism gear ratio (e.g., 3.7 for a 3.7:1 gearbox).
   * @param torqueConstant Motor Kt in N*m/A. For FTC motors: GoBilda 5202-0002-0019 = ~0.025, REV
   *     HD Hex = ~0.018.
   * @param resistance Motor internal resistance in Ohms. GoBilda 5202 = ~5.0, REV HD Hex = ~7.0.
   */
  public static void solveForSimConstants(
      String csvFilePath, double gearing, double torqueConstant, double resistance) {
    LOGGER.info("==========================================");
    LOGGER.info("ARESLib Simulation Auto-Tuner Initialized");
    LOGGER.info("Target File: " + csvFilePath);
    LOGGER.info("Extracting Dynamics...");

    try {
      List<DataPoint> data = parseCsvData(csvFilePath);

      if (data.size() < 10) {
        LOGGER.warning("[ERROR] Insufficient data points found (" + data.size() + " < 10).");
        return;
      }

      LOGGER.info("Extracted " + data.size() + " telemetry frames.");
      LOGGER.info("Solving OLS Regression Matrix (Y = X * Beta)...");

      int n = data.size() - 1;
      SimpleMatrix y = new SimpleMatrix(n, 1);
      SimpleMatrix x = new SimpleMatrix(n, 3);

      for (int i = 0; i < n; i++) {
        DataPoint p1 = data.get(i);
        DataPoint p2 = data.get(i + 1);

        double dt = p2.time - p1.time;
        if (dt <= 0.001) dt = 0.02; // Safeguard: assume 20ms loop if timestamps identical

        double accel = (p2.velocity - p1.velocity) / dt;

        y.set(i, 0, p1.voltage);
        // V = kS*sign(vel) + kV*vel + kA*accel
        x.set(i, 0, Math.signum(p1.velocity));
        x.set(i, 1, p1.velocity);
        x.set(i, 2, accel);
      }

      SimpleMatrix xt = x.transpose();
      SimpleMatrix xtx = xt.mult(x);

      if (Math.abs(xtx.determinant()) < 1e-10) {
        LOGGER.warning("[ERROR] Dataset mathematically singular. Cannot perform regression.");
        LOGGER.warning("This usually means the motor didn't move enough during the test.");
        return;
      }

      SimpleMatrix beta = xtx.invert().mult(xt).mult(y);

      double kS = beta.get(0, 0);
      double kV = beta.get(1, 0);
      double kA = beta.get(2, 0);

      // True Moment of Inertia = (kA * Gearing * TorqueConstant) / Resistance
      double calcMOI = Math.abs((kA * gearing * torqueConstant) / resistance);

      LOGGER.info("------------------------------------------");
      LOGGER.info("System Identification Constants:");
      LOGGER.info(String.format("kS (Static Friction): %.4f V", Math.abs(kS)));
      LOGGER.info(String.format("kV (Velocity):        %.4f V / (rad/s)", Math.abs(kV)));
      LOGGER.info(String.format("kA (Acceleration):    %.4f V / (rad/s^2)", Math.abs(kA)));
      LOGGER.info("------------------------------------------");
      LOGGER.info("Dyn4j / Simulation Injectable Properties:");
      LOGGER.info(String.format("True Simulated Mass / MOI: %.5f kg*m^2", calcMOI));
      LOGGER.info("------------------------------------------");
      LOGGER.info("Copy these into your Constants file:");
      LOGGER.info(String.format("  public static final double kS = %.4f;", Math.abs(kS)));
      LOGGER.info(String.format("  public static final double kV = %.4f;", Math.abs(kV)));
      LOGGER.info(String.format("  public static final double kA = %.4f;", Math.abs(kA)));
      LOGGER.info(String.format("  public static final double SIM_MOI = %.5f;", calcMOI));
      LOGGER.info("==========================================");

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "[ERROR] Failed to read CSV file.", e);
    }
  }

  /**
   * Parses a SysId CSV file into a list of DataPoints.
   *
   * @param csvFilePath Path to the CSV.
   * @return List of parsed DataPoints, sorted by time.
   * @throws IOException If the file cannot be read.
   */
  private static List<DataPoint> parseCsvData(String csvFilePath) throws IOException {
    List<DataPoint> data = new ArrayList<>();
    try (BufferedReader br =
        Files.newBufferedReader(Paths.get(csvFilePath), StandardCharsets.UTF_8)) {
      br.readLine(); // skip header
      String line = br.readLine();
      while (line != null) {
        @SuppressWarnings("StringSplitter")
        String[] values = line.split(",", -1);
        if (values.length >= 3) {
          double time = Double.parseDouble(values[0].trim());
          double voltage = Double.parseDouble(values[1].trim());
          double velocity = Double.parseDouble(values[2].trim());
          data.add(new DataPoint(time, velocity, voltage));
        }
        line = br.readLine();
      }
    }
    return data;
  }

  /**
   * Desktop entrypoint for offline tuning. Run this from your IDE.
   *
   * <p>Modify the parameters below to match your mechanism and motor.
   */
  public static void main(String[] args) {
    // --- EXECUTION TEMPLATE (FTC) ---
    // Update these values for your specific mechanism:
    //
    // solveForSimConstants(
    //     "/sdcard/FIRST/sysid/sysid_drive_2026-04-10.csv",
    //     3.7,     // gear ratio (motor:output)
    //     0.025,   // Kt for GoBilda 5202 in N*m/A
    //     5.0      // Motor resistance in Ohms
    // );
    //
    // For REV HD Hex motors:
    // solveForSimConstants(
    //     "/sdcard/FIRST/sysid/sysid_arm_2026-04-10.csv",
    //     20.0,    // gear ratio (typical arm gearbox)
    //     0.018,   // Kt for REV HD Hex
    //     7.0      // Motor resistance
    // );

    LOGGER.info("SimSysIdTuner ready. Uncomment the above template with your values.");
  }
}
