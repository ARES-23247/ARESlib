package org.areslib.core.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A centralized executor service for offloading heavy, non-hardware compute (like Vision Processing
 * or complex Odometry kinematics) off the main FTC thread.
 *
 * <p>Elite Feature: Decoupling math from the main loop allows the hardware loop (reading sensors
 * and writing to motors) to run unhindered at max speeds.
 */
public class AresAsyncExecutor {
  private static ScheduledExecutorService executor;

  private static class AsyncLoop {
    final Runnable task;
    final long periodMs;

    AsyncLoop(Runnable task, long periodMs) {
      this.task = task;
      this.periodMs = periodMs;
    }
  }

  private static final List<AsyncLoop> registeredLoops = new ArrayList<>();

  // Internal initialization state
  private static boolean isRunning = false;

  /**
   * Registers a looping function to run asynchronously at a specific update rate. Must be called
   * before start().
   *
   * @param task The task to run.
   * @param frequencyHz The frequency to run the task in Hertz (e.g. 100hz = 10ms loop).
   */
  public static void registerLoop(Runnable task, int frequencyHz) {
    if (isRunning) {
      throw new IllegalStateException(
          "Cannot register new Async tasks after the executor has started.");
    }

    long periodMs = 1000L / frequencyHz;
    Runnable safeTask =
        () -> {
          try {
            task.run();
          } catch (Exception e) {
            com.qualcomm.robotcore.util.RobotLog.e(
                String.valueOf("ARES Async Thread Crashed: " + e.getMessage()));
            com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
          }
        };
    registeredLoops.add(new AsyncLoop(safeTask, periodMs));
  }

  /** Starts all registered async loops. This is automatically called by AresCommandOpMode. */
  public static void start() {
    if (isRunning) return;

    executor = Executors.newScheduledThreadPool(Math.max(1, registeredLoops.size()));

    for (AsyncLoop loop : registeredLoops) {
      executor.scheduleAtFixedRate(loop.task, 0, loop.periodMs, TimeUnit.MILLISECONDS);
    }

    isRunning = true;
  }

  /**
   * Starts a one-off asynchronous task.
   *
   * @param task The task to run in the background.
   */
  public static void runAsync(Runnable task) {
    if (executor == null || executor.isShutdown()) {
      executor = Executors.newScheduledThreadPool(4); // lazy init pool
    }
    executor.submit(task);
  }

  /**
   * Shuts down the asynchronous threads. Automatically called by AresCommandOpMode at the end of
   * the match.
   */
  public static void stop() {
    if (executor != null && !executor.isShutdown()) {
      executor.shutdownNow();
    }
    registeredLoops.clear();
    isRunning = false;
  }
}
