package org.areslib.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Supplier;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GhostModeTest {

  @Mock private Supplier<ChassisSpeeds> mockSpeedsSupplier;

  @Mock private Supplier<Boolean> mockButtonSupplier;

  private GhostRecorder ghostRecorder;

  @BeforeEach
  public void setup() {
    // Initialize GhostRecorder with mocked lambda suppliers
    ghostRecorder = new GhostRecorder(mockSpeedsSupplier, mockButtonSupplier);
    AresRobot.setSimulation(true);
  }

  @Test
  public void testRecordingStateAndMasks() throws Exception {
    // Prepare mock outputs
    when(mockSpeedsSupplier.get()).thenReturn(new ChassisSpeeds(2.0, 0.0, 1.5));
    when(mockButtonSupplier.get()).thenReturn(true);

    ghostRecorder.startRecording();

    // Emulate 3 periodic ticks
    ghostRecorder.update();
    ghostRecorder.update();
    ghostRecorder.update();

    // Save to a temporary mock path
    String tmpPath = "build/tmp/testMacro.json";
    ghostRecorder.stopAndSave(tmpPath);

    // Assert file exists and the math evaluates
    File file = new File(tmpPath);
    assertTrue(file.exists(), "JSON Macro should be written to disk");

    // Read file contents to verify JSON schema
    String json =
        new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);

    assertTrue(json.contains("\"vxMetersPerSecond\": ["), "VX key should exist");
    assertTrue(json.contains("2.0"), "Should contain mocked 2.0 vx value");
    assertTrue(json.contains("1.5"), "Should contain mocked omega value");
    assertTrue(json.contains("\"buttonMasks\": ["), "Mask key should exist");

    // Binary 0b1 = 1 because we only supplied 1 truthy boolean
    assertTrue(json.contains("1"), "Should contain bitmask calculation '1'");

    // Cleanup
    file.delete();
  }
}
