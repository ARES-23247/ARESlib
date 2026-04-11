package org.areslib.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.telemetry.AresAutoLogger;

/**
 * High-performance Ghost Mode automator for recording and replaying teleop sequences.
 *
 * <p>Records robot velocities and binary button inputs to CSV format using a lock-free background
 * writer thread, guaranteeing zero main-loop blocking from file I/O. Playback deserializes the CSV
 * and feeds the exact recorded inputs back into the drive subsystem for deterministic autonomous
 * replays.
 *
 * <p><b>Concurrency &amp; Hardware Safety:</b> Writing to the Control Hub's flash storage takes
 * milliseconds, which would catastrophically lag the primary control loop. This class uses a
 * thread-safe {@link ConcurrentLinkedQueue} buffer. The main robot loop exclusively pushes
 * memory-light Strings to the queue without locking. A separate background daemon thread wakes up
 * every ~5ms to drain the queue chunks to disk.
 *
 * <p><b>IMPORTANT: Mutual Exclusion.</b> Callers must ensure that recording and playback are
 * mutually exclusive (e.g., via button guards). Running both simultaneously will corrupt data.
 */
public class GhostRecorder {

  private static final ChassisSpeeds ZERO_SPEEDS = new ChassisSpeeds();

  private volatile boolean m_isRecording = false;
  private volatile boolean m_isPlaying = false;

  private final Supplier<ChassisSpeeds> m_speedsSupplier;
  private final Supplier<Boolean>[] m_booleanSuppliers;

  // --- Recording (thread-safe) ---
  private final ConcurrentLinkedQueue<String> m_writeBuffer = new ConcurrentLinkedQueue<>();
  private Thread m_writerThread;

  @SuppressWarnings("PMD.AvoidStringBufferField")
  private final StringBuilder m_rowBuilder = new StringBuilder(128);

  private long m_recordStartNanos;

  // --- Playback Cache ---
  private final List<GhostFrame> m_frames = new ArrayList<>();
  private int m_playIndex = 0;
  private GhostFrame m_currentFrame = new GhostFrame();
  private long m_playbackStartNanos;
  private final ChassisSpeeds m_playbackSpeeds = new ChassisSpeeds();

  /**
   * Constructs a Ghost Mode recorder/player.
   *
   * @param speedsSupplier Supplier that provides the current commanding robot ChassisSpeeds.
   * @param booleanSuppliers A varargs array of booleans to track (e.g., intake button, shoot
   *     button). They will be assigned bit positions 0, 1, 2... based on varargs order.
   */
  @SafeVarargs
  public GhostRecorder(
      Supplier<ChassisSpeeds> speedsSupplier, Supplier<Boolean>... booleanSuppliers) {
    m_speedsSupplier = speedsSupplier;
    m_booleanSuppliers = booleanSuppliers;
  }

  // ── Recording ──────────────────────────────────────────────────────────────

  /**
   * Starts recording. Any existing writer thread is safely terminated first.
   *
   * @param filePath The absolute path on the Control Hub (e.g., "/sdcard/FIRST/macros/auto1.csv").
   */
  public void startRecording(String filePath) {
    // Safely terminate any previous writer thread
    if (m_writerThread != null && m_writerThread.isAlive()) {
      m_isRecording = false;
      m_writerThread.interrupt();
      try {
        m_writerThread.join(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    m_isRecording = true;
    m_writeBuffer.clear();
    m_recordStartNanos = System.nanoTime();
    startWriterThread(new File(filePath));
    AresAutoLogger.recordOutput("GhostMode/Recording", 1.0);
  }

  /**
   * Should be called every robot loop (~20ms). Samples data if recording is active and enqueues it
   * to the lock-free write buffer — zero blocking on the main thread.
   */
  public void update() {
    if (!m_isRecording) return;

    ChassisSpeeds speeds = m_speedsSupplier.get();
    int mask = 0;
    for (int i = 0; i < m_booleanSuppliers.length; i++) {
      if (m_booleanSuppliers[i].get()) {
        mask |= (1 << i);
      }
    }

    double timeSecs = (System.nanoTime() - m_recordStartNanos) / 1_000_000_000.0;

    m_rowBuilder.setLength(0);
    m_rowBuilder
        .append(Math.round(timeSecs * 1000.0) / 1000.0)
        .append(',')
        .append(Math.round(speeds.vxMetersPerSecond * 1000.0) / 1000.0)
        .append(',')
        .append(Math.round(speeds.vyMetersPerSecond * 1000.0) / 1000.0)
        .append(',')
        .append(Math.round(speeds.omegaRadiansPerSecond * 1000.0) / 1000.0)
        .append(',')
        .append(mask);

    m_writeBuffer.offer(m_rowBuilder.toString());
  }

  /** Stops recording. The background writer thread drains remaining buffer entries and exits. */
  public void stopRecording() {
    m_isRecording = false;
    AresAutoLogger.recordOutput("GhostMode/Recording", 0.0);
    // Writer thread will detect recording=false, drain, and exit
  }

  // ── Playback ───────────────────────────────────────────────────────────────

  /**
   * Loads a ghost macro CSV file into memory for playback.
   *
   * @param filePath The path to the CSV file.
   * @return True if loading succeeded, false if the file is missing or corrupt.
   */
  public boolean loadForPlayback(String filePath) {
    m_frames.clear();
    m_playIndex = 0;

    try (BufferedReader br =
        Files.newBufferedReader(java.nio.file.Paths.get(filePath), StandardCharsets.UTF_8)) {
      br.readLine(); // skip header
      String line = br.readLine();
      while (line != null) {
        @SuppressWarnings("StringSplitter")
        String[] v = line.split(",", -1);
        GhostFrame frame = new GhostFrame();
        frame.time = Double.parseDouble(v[0]);
        frame.vx = Double.parseDouble(v[1]);
        frame.vy = Double.parseDouble(v[2]);
        frame.omega = Double.parseDouble(v[3]);
        frame.buttonMask = Integer.parseInt(v[4]);
        m_frames.add(frame);
        line = br.readLine();
      }
    } catch (Exception e) {
      com.qualcomm.robotcore.util.RobotLog.e("GHOST FILE NOT FOUND: " + filePath);
      return false;
    }

    AresAutoLogger.recordOutput("GhostMode/FramesLoaded", m_frames.size());
    return !m_frames.isEmpty();
  }

  /** Starts playback of the loaded frames. Call {@link #loadForPlayback} first. */
  public void startPlayback() {
    if (m_frames.isEmpty()) return;
    m_isPlaying = true;
    m_playIndex = 0;
    m_playbackStartNanos = System.nanoTime();
    AresAutoLogger.recordOutput("GhostMode/Playing", 1.0);
  }

  /**
   * Advances playback to the current timestamp. Must be called every loop.
   *
   * @return The current playback frame's ChassisSpeeds, or zero if not playing.
   */
  public ChassisSpeeds getPlaybackSpeeds() {
    if (!m_isPlaying || m_frames.isEmpty()) return ZERO_SPEEDS;

    double t = (System.nanoTime() - m_playbackStartNanos) / 1_000_000_000.0;

    // Fast-forward index to current playback time
    while (m_playIndex < m_frames.size() - 1 && m_frames.get(m_playIndex + 1).time <= t) {
      m_playIndex++;
    }

    m_currentFrame = m_frames.get(m_playIndex);
    m_playbackSpeeds.vxMetersPerSecond = m_currentFrame.vx;
    m_playbackSpeeds.vyMetersPerSecond = m_currentFrame.vy;
    m_playbackSpeeds.omegaRadiansPerSecond = m_currentFrame.omega;
    return m_playbackSpeeds;
  }

  /**
   * Returns the button mask from the current playback frame.
   *
   * @param buttonIndex The bit index of the button (0-based, matching varargs order).
   * @return True if the button was pressed in the current playback frame.
   */
  public boolean getPlaybackButton(int buttonIndex) {
    if (!m_isPlaying) return false;
    return (m_currentFrame.buttonMask & (1 << buttonIndex)) != 0;
  }

  /** Returns true if playback has reached the end of the recorded frames. */
  public boolean isPlaybackFinished() {
    return m_playIndex >= m_frames.size() - 1;
  }

  /** Stops playback. */
  public void stopPlayback() {
    m_isPlaying = false;
    AresAutoLogger.recordOutput("GhostMode/Playing", 0.0);
  }

  /** Returns true if currently recording. */
  public boolean isRecording() {
    return m_isRecording;
  }

  /** Returns true if currently playing back. */
  public boolean isPlaying() {
    return m_isPlaying;
  }

  // ── Background writer thread ───────────────────────────────────────────────

  private void startWriterThread(File file) {
    m_writerThread =
        new Thread(
            () -> {
              try {
                file.getParentFile().mkdirs();
                try (PrintWriter pw =
                    new PrintWriter(
                        Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
                  pw.println("time,vx,vy,omega,buttons");
                  while (m_isRecording || !m_writeBuffer.isEmpty()) {
                    String line = m_writeBuffer.poll();
                    while (line != null) {
                      pw.println(line);
                      line = m_writeBuffer.poll();
                    }
                    // Flush after each drain cycle to survive unexpected power-offs
                    pw.flush();
                    try {
                      Thread.sleep(5); // ~200 Hz drain rate
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }
                }
              } catch (Exception e) {
                com.qualcomm.robotcore.util.RobotLog.e("GhostWriter failed: " + e.getMessage());
              }
            },
            "GhostWriter");
    m_writerThread.setDaemon(true);
    m_writerThread.start();
  }

  // ── Internal types ─────────────────────────────────────────────────────────

  private static class GhostFrame {
    double time;
    double vx;
    double vy;
    double omega;
    int buttonMask;
  }
}
