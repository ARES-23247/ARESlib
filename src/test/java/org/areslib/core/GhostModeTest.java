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
  public void testRecordingStateAndCSV() throws Exception {
    // Prepare mock outputs
    when(mockSpeedsSupplier.get()).thenReturn(new ChassisSpeeds(2.0, 0.0, 1.5));
    when(mockButtonSupplier.get()).thenReturn(true);

    String tmpPath = "build/tmp/testMacro.csv";
    ghostRecorder.startRecording(tmpPath);
    assertTrue(ghostRecorder.isRecording(), "Should be recording after startRecording()");

    // Emulate 3 periodic ticks
    ghostRecorder.update();
    ghostRecorder.update();
    ghostRecorder.update();

    // Stop recording and allow writer thread to flush
    ghostRecorder.stopRecording();
    assertFalse(ghostRecorder.isRecording(), "Should stop recording after stopRecording()");

    // Wait for background writer to finish
    Thread.sleep(100);

    // Assert file exists and contains CSV data
    File file = new File(tmpPath);
    assertTrue(file.exists(), "CSV Macro should be written to disk");

    // Read file contents to verify CSV schema
    String csv =
        new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);

    assertTrue(csv.contains("time,vx,vy,omega,buttons"), "CSV header should exist");
    assertTrue(csv.contains("2.0"), "Should contain mocked 2.0 vx value");
    assertTrue(csv.contains("1.5"), "Should contain mocked omega value");
    // Binary 0b1 = 1 because we only supplied 1 truthy boolean
    assertTrue(csv.contains(",1"), "Should contain bitmask calculation '1'");

    // Cleanup
    file.delete();
  }

  @Test
  public void testPlaybackLoadsCSV() throws Exception {
    // Create a test CSV file
    String tmpPath = "build/tmp/testPlayback.csv";
    new File("build/tmp").mkdirs();
    Files.write(
        new File(tmpPath).toPath(),
        "time,vx,vy,omega,buttons\n0.0,1.0,0.5,0.3,0\n0.02,1.1,0.6,0.35,1\n"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));

    boolean loaded = ghostRecorder.loadForPlayback(tmpPath);
    assertTrue(loaded, "Should load a valid CSV file");

    ghostRecorder.startPlayback();
    assertTrue(ghostRecorder.isPlaying(), "Should be playing after startPlayback()");

    ChassisSpeeds speeds = ghostRecorder.getPlaybackSpeeds();
    assertNotNull(speeds, "Should return non-null speeds during playback");

    ghostRecorder.stopPlayback();
    assertFalse(ghostRecorder.isPlaying(), "Should stop playing after stopPlayback()");

    // Cleanup
    new File(tmpPath).delete();
  }
}
