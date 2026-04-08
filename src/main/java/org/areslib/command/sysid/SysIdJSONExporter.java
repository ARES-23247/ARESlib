package org.areslib.command.sysid;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Automatically intercepts WPILib-style SysId routine data and serializes it strictly to the
 * official `sysid-tool` JSON format. This allows teams to completely skip python `.wpilog` parsers
 * and just directly load tests into the WPILib desktop app for immediate optimal gain tuning!
 */
public class SysIdJSONExporter {

  private static final List<TestRecord> records = new ArrayList<>();

  public static class TestRecord {
    public final String testName;
    public final List<Double> data = new ArrayList<>();

    public TestRecord(String testName) {
      this.testName = testName;
    }

    /**
     * Standard layout required by WPILib SysId schema: [timestamp, voltage, position, velocity]
     *
     * @param timestamp The time in seconds
     * @param voltage The motor voltage
     * @param position The position
     * @param velocity The velocity
     */
    public void addFrame(double timestamp, double voltage, double position, double velocity) {
      data.add(timestamp);
      data.add(voltage);
      data.add(position);
      data.add(velocity);
    }
  }

  /**
   * Track a new diagnostic test.
   *
   * @param testName The name of the sysid test
   * @return The test record
   */
  public static TestRecord startTest(String testName) {
    TestRecord record = new TestRecord(testName);
    records.add(record);
    return record;
  }

  /**
   * Dumps the accumulated TestRecords into a clean JSON string matching WPILib Schema, and saves it
   * locally.
   */
  public static void export() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"sysid\": true,\n");
    sb.append("  \"test\": \"AresMechanism\",\n");
    sb.append("  \"units\": \"Meters\",\n");
    sb.append("  \"data\": [\n");

    for (int i = 0; i < records.size(); i++) {
      TestRecord rec = records.get(i);
      sb.append("    {\n");
      sb.append("      \"test\": \"").append(rec.testName).append("\",\n");
      sb.append("      \"data\": [");
      for (int j = 0; j < rec.data.size(); j++) {
        sb.append(String.format(Locale.US, "%.5f", rec.data.get(j)));
        if (j < rec.data.size() - 1) sb.append(", ");
      }
      sb.append("]\n");
      sb.append("    }");
      if (i < records.size() - 1) sb.append(",");
      sb.append("\n");
    }
    sb.append("  ]\n");
    sb.append("}\n");

    File file = new File("SysId_Output.json");
    try (FileWriter fw = new FileWriter(file)) {
      fw.write(sb.toString());
      System.out.println("SysId Data Successfully Exported to: " + file.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
