package org.areslib.math.estimator;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link VisionFusionHelper}. */
class VisionFusionHelperTest {

  private static final double EPSILON = 1e-6;

  private TimeInterpolatableBuffer<Pose2d> createBufferWithSamples() {
    TimeInterpolatableBuffer<Pose2d> buffer = TimeInterpolatableBuffer.createBuffer(100);
    buffer.addSample(0.0, new Pose2d(0, 0, new Rotation2d(0)));
    buffer.addSample(0.5, new Pose2d(1, 0, new Rotation2d(0)));
    buffer.addSample(1.0, new Pose2d(2, 0, new Rotation2d(0)));
    return buffer;
  }

  @Test
  @DisplayName("Returns current estimate when buffer is empty")
  void emptyBufferReturnsCurrent() {
    TimeInterpolatableBuffer<Pose2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    Pose2d current = new Pose2d(5, 5, new Rotation2d(1.0));
    double[] stdDevs = {0.1, 0.1, 0.1};

    Pose2d result =
        VisionFusionHelper.applyVisionMeasurement(
            new Pose2d(10, 10, new Rotation2d()), 0.5, current, buffer, stdDevs);

    assertEquals(current.getX(), result.getX(), EPSILON);
    assertEquals(current.getY(), result.getY(), EPSILON);
  }

  @Test
  @DisplayName("Low stdDevs produce strong correction")
  void lowStdDevsStrongCorrection() {
    TimeInterpolatableBuffer<Pose2d> buffer = createBufferWithSamples();
    Pose2d current = new Pose2d(2, 0, new Rotation2d(0));
    double[] stdDevs = {0.0, 0.0, 0.0}; // Perfect trust in vision

    Pose2d visionPose = new Pose2d(2, 1, new Rotation2d(0)); // Vision says we're at y=1

    Pose2d result =
        VisionFusionHelper.applyVisionMeasurement(visionPose, 1.0, current, buffer, stdDevs);

    // With stdDevs = [0,0,0], kX=kY=kTheta=1.0, full correction
    assertEquals(2.0, result.getX(), EPSILON);
    assertEquals(1.0, result.getY(), EPSILON);
  }

  @Test
  @DisplayName("High stdDevs produce weak correction")
  void highStdDevsWeakCorrection() {
    TimeInterpolatableBuffer<Pose2d> buffer = createBufferWithSamples();
    Pose2d current = new Pose2d(2, 0, new Rotation2d(0));
    double[] stdDevs = {99.0, 99.0, 99.0}; // Very low trust in vision

    Pose2d visionPose = new Pose2d(2, 100, new Rotation2d(0)); // Vision says far away

    Pose2d result =
        VisionFusionHelper.applyVisionMeasurement(visionPose, 1.0, current, buffer, stdDevs);

    // kY = 1/(1+99) = 0.01, correction = 100 * 0.01 = 1.0
    assertEquals(1.0, result.getY(), EPSILON);
  }

  @Test
  @DisplayName("Vision correction at current time applies directly")
  void correctionAtCurrentTime() {
    TimeInterpolatableBuffer<Pose2d> buffer = TimeInterpolatableBuffer.createBuffer(100);
    Pose2d current = new Pose2d(5, 0, new Rotation2d(0));
    buffer.addSample(1.0, current);
    double[] stdDevs = {0.0, 0.0, 0.0}; // Full trust

    Pose2d visionPose = new Pose2d(6, 1, new Rotation2d(0.1));

    Pose2d result =
        VisionFusionHelper.applyVisionMeasurement(visionPose, 1.0, current, buffer, stdDevs);

    assertEquals(6.0, result.getX(), EPSILON);
    assertEquals(1.0, result.getY(), EPSILON);
  }

  @Test
  @DisplayName("Measurement timestamp interpolates buffer correctly")
  void interpolatedTimestamp() {
    TimeInterpolatableBuffer<Pose2d> buffer = createBufferWithSamples();
    Pose2d current = new Pose2d(2, 0, new Rotation2d(0));
    double[] stdDevs = {0.0, 0.0, 0.0};

    // At t=0.5 the buffer has pose (1, 0)
    Pose2d visionPose = new Pose2d(1, 2, new Rotation2d(0));

    Pose2d result =
        VisionFusionHelper.applyVisionMeasurement(visionPose, 0.5, current, buffer, stdDevs);

    // The correction should push y from 0 → 2 at t=0.5, then replay forward
    assertTrue(Math.abs(result.getY() - 2.0) < 0.5, "Y should be near 2.0 after replay");
  }
}
