package org.areslib.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link GhostRecorder} covering recording, playback, CSV serialization, and edge cases.
 */
public class GhostRecorderTest {

  @TempDir File tempDir;

  private ChassisSpeeds currentSpeeds;
  private boolean button0Pressed;
  private boolean button1Pressed;
  private GhostRecorder recorder;

  @BeforeEach
  void setUp() {
    currentSpeeds = new ChassisSpeeds();
    button0Pressed = false;
    button1Pressed = false;

    recorder = new GhostRecorder(() -> currentSpeeds, () -> button0Pressed, () -> button1Pressed);
  }

  @AfterEach
  void tearDown() {
    if (recorder.isRecording()) {
      recorder.stopRecording();
    }
    if (recorder.isPlaying()) {
      recorder.stopPlayback();
    }
  }

  @Test
  void testInitialization() {}

  @Test
  void testRecordingLifecycle() throws Exception {
    String path = new File(tempDir, "test_macro.csv").getAbsolutePath();

    assertFalse(recorder.isRecording());

    recorder.startRecording(path);
    assertTrue(recorder.isRecording());

    // Record a few frames
    currentSpeeds.vxMetersPerSecond = 1.0;
    currentSpeeds.vyMetersPerSecond = 0.5;
    currentSpeeds.omegaRadiansPerSecond = 0.2;
    button0Pressed = true;

    for (int i = 0; i < 5; i++) {
      recorder.update();
      Thread.sleep(5);
    }

    recorder.stopRecording();
    assertFalse(recorder.isRecording());

    // Wait for writer thread to drain
    Thread.sleep(100);

    // Verify the CSV was written
    File csvFile = new File(path);
    assertTrue(csvFile.exists(), "CSV file should be created");

    String content = new String(Files.readAllBytes(csvFile.toPath()), StandardCharsets.UTF_8);
    assertTrue(content.startsWith("time,vx,vy,omega,buttons"), "CSV should have header");

    String[] lines = content.trim().split("\n");
    assertTrue(lines.length > 1, "Should have at least header + 1 data row");
  }

  @Test
  void testPlaybackFromFile() throws Exception {
    // Create a test CSV manually
    String path = new File(tempDir, "playback_test.csv").getAbsolutePath();
    StringBuilder csv = new StringBuilder();
    csv.append("time,vx,vy,omega,buttons\n");
    csv.append("0.0,1.0,0.5,0.2,1\n");
    csv.append("0.02,1.5,0.3,0.1,0\n");
    csv.append("0.04,0.0,0.0,0.0,0\n");
    Files.write(new File(path).toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));

    assertTrue(recorder.loadForPlayback(path), "Should load successfully");
    assertFalse(recorder.isPlaying());

    recorder.startPlayback();
    assertTrue(recorder.isPlaying());

    ChassisSpeeds speeds = recorder.getPlaybackSpeeds();
    // First frame should give us the first recorded velocities
    assertEquals(1.0, speeds.vxMetersPerSecond, 0.01);
    assertEquals(0.5, speeds.vyMetersPerSecond, 0.01);
    assertEquals(0.2, speeds.omegaRadiansPerSecond, 0.01);

    // Button 0 should be pressed in frame 0
    assertTrue(recorder.getPlaybackButton(0));
    assertFalse(recorder.getPlaybackButton(1));

    recorder.stopPlayback();
    assertFalse(recorder.isPlaying());
  }

  @Test
  void testLoadMissingFileReturnsFalse() {
    assertFalse(
        recorder.loadForPlayback("/nonexistent/path/ghost.csv"),
        "Loading missing file should return false");
  }

  @Test
  void testPlaybackReturnsZeroWhenNotPlaying() {
    ChassisSpeeds speeds = recorder.getPlaybackSpeeds();
    assertEquals(0.0, speeds.vxMetersPerSecond, 0.001);
    assertEquals(0.0, speeds.vyMetersPerSecond, 0.001);
    assertEquals(0.0, speeds.omegaRadiansPerSecond, 0.001);
  }

  @Test
  void testButtonMaskBitPacking() throws Exception {
    // Verify that button mask correctly uses bit positions
    String path = new File(tempDir, "button_test.csv").getAbsolutePath();
    StringBuilder csv = new StringBuilder();
    csv.append("time,vx,vy,omega,buttons\n");
    csv.append("0.0,0.0,0.0,0.0,3\n"); // Both buttons pressed (bit 0 + bit 1 = 3)
    Files.write(new File(path).toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));

    recorder.loadForPlayback(path);
    recorder.startPlayback();

    // Must call getPlaybackSpeeds() first to populate currentFrame
    recorder.getPlaybackSpeeds();

    assertTrue(recorder.getPlaybackButton(0), "Bit 0 should be set");
    assertTrue(recorder.getPlaybackButton(1), "Bit 1 should be set");
    assertFalse(recorder.getPlaybackButton(2), "Bit 2 should NOT be set");

    recorder.stopPlayback();
  }

  @Test
  void testStartingRecordingTwiceDoesNotCrash() throws Exception {
    String path1 = new File(tempDir, "macro1.csv").getAbsolutePath();
    String path2 = new File(tempDir, "macro2.csv").getAbsolutePath();

    recorder.startRecording(path1);
    recorder.update();

    // Starting a second recording should safely terminate the first
    recorder.startRecording(path2);
    recorder.update();

    recorder.stopRecording();
    Thread.sleep(100);

    assertTrue(new File(path2).exists(), "Second recording file should exist");
  }

  @Test
  void testPlaybackButtonReturnsFalseWhenNotPlaying() {
    assertFalse(recorder.getPlaybackButton(0));
    assertFalse(recorder.getPlaybackButton(5));
  }

  @Test
  void testIsPlaybackFinishedOnSingleFrame() throws Exception {
    String path = new File(tempDir, "single_frame.csv").getAbsolutePath();
    StringBuilder csv = new StringBuilder();
    csv.append("time,vx,vy,omega,buttons\n");
    csv.append("0.0,1.0,0.0,0.0,0\n");
    Files.write(new File(path).toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));

    recorder.loadForPlayback(path);
    recorder.startPlayback();

    // With a single frame, playback should immediately be "finished"
    assertTrue(recorder.isPlaybackFinished());

    recorder.stopPlayback();
  }
}
