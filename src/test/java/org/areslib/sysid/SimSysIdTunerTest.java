package org.areslib.sysid;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimSysIdTunerTest {

  @TempDir Path tempDir;

  @Test
  void testAutoTuningPipeline() throws IOException {
    // 1. Simulate a dynamic quasistatic voltage ramp and generate data
    List<String> csvLines = new ArrayList<>();
    csvLines.add("time,voltage,velocity");

    double kV_truth = 2.0;
    double kA_truth = 0.5;
    double kS_truth = 1.0;

    // Simulate V = kS + kV*v + kA*a
    // a = (V - kS - kV*v) / kA
    double dt = 0.02;
    double v = 0.0;
    for (double t = 0; t < 10.0; t += dt) {
      double volts = t * 1.5; // Ramp up voltage

      // Plant model physics
      double accel = 0.0;
      if (volts > kS_truth) {
        accel = (volts - kS_truth - kV_truth * v) / kA_truth;
      }
      v += accel * dt;

      csvLines.add(String.format("%.3f,%.3f,%.3f", t, volts, v));
    }

    Path csvFile = tempDir.resolve("sim_sysid_test.csv");
    Files.write(csvFile, csvLines, StandardCharsets.UTF_8);

    Path jsonOut = tempDir.resolve("sysid_results.json");

    // 2. Run tuner
    SimSysIdTuner.SysIdResult res =
        SimSysIdTuner.solveForSimConstants(csvFile.toString(), 1.0, 1.0, 1.0, jsonOut.toString());

    assertNotNull(res);

    // Check if regression solved correctly within some tolerance
    assertEquals(kS_truth, res.kS, 0.5);
    assertEquals(kV_truth, res.kV, 0.5);
    assertEquals(kA_truth, res.kA, 0.5);

    assertTrue(Files.exists(jsonOut));
    String jsonContent = Files.readString(jsonOut);
    assertTrue(jsonContent.contains("\"kV\""));
    assertTrue(jsonContent.contains("\"simMOI\""));
  }
}
