package org.areslib.math.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TimeInterpolatableBuffer}. */
class TimeInterpolatableBufferTest {

  private static final double EPSILON = 1e-6;

  @Test
  @DisplayName("Empty buffer returns null")
  void emptyBufferReturnsNull() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    assertNull(buffer.getSample(1.0));
  }

  @Test
  @DisplayName("getSample returns exact value for exact timestamp")
  void exactTimestamp() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    buffer.addSample(1.0, new Translation2d(10, 20));
    Translation2d result = buffer.getSample(1.0);
    assertNotNull(result);
    assertEquals(10.0, result.getX(), EPSILON);
    assertEquals(20.0, result.getY(), EPSILON);
  }

  @Test
  @DisplayName("getSample interpolates between two entries")
  void interpolatesBetween() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    buffer.addSample(0.0, new Translation2d(0, 0));
    buffer.addSample(2.0, new Translation2d(10, 20));

    Translation2d mid = buffer.getSample(1.0);
    assertNotNull(mid);
    assertEquals(5.0, mid.getX(), EPSILON);
    assertEquals(10.0, mid.getY(), EPSILON);
  }

  @Test
  @DisplayName("getSample clamps below minimum")
  void clampsBelowMin() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    buffer.addSample(1.0, new Translation2d(5, 10));
    buffer.addSample(2.0, new Translation2d(15, 30));

    Translation2d result = buffer.getSample(0.0);
    assertEquals(5.0, result.getX(), EPSILON);
  }

  @Test
  @DisplayName("getSample clamps above maximum")
  void clampsAboveMax() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    buffer.addSample(1.0, new Translation2d(5, 10));
    buffer.addSample(2.0, new Translation2d(15, 30));

    Translation2d result = buffer.getSample(99.0);
    assertEquals(15.0, result.getX(), EPSILON);
  }

  @Test
  @DisplayName("clear empties the buffer")
  void clear() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    buffer.addSample(1.0, new Translation2d(5, 10));
    buffer.clear();
    assertNull(buffer.getSample(1.0));
  }

  @Test
  @DisplayName("Buffer evicts oldest when full")
  void eviction() {
    TimeInterpolatableBuffer<Translation2d> buffer = TimeInterpolatableBuffer.createBuffer(3);
    buffer.addSample(1.0, new Translation2d(10, 0));
    buffer.addSample(2.0, new Translation2d(20, 0));
    buffer.addSample(3.0, new Translation2d(30, 0));
    buffer.addSample(4.0, new Translation2d(40, 0)); // evicts 1.0

    // t=1.0 should now clamp to t=2.0 (lowest remaining)
    Translation2d result = buffer.getSample(1.0);
    assertEquals(20.0, result.getX(), EPSILON);
  }

  @Test
  @DisplayName("Pose2d interpolation in buffer")
  void pose2dInterpolation() {
    TimeInterpolatableBuffer<Pose2d> buffer = TimeInterpolatableBuffer.createBuffer(10);
    buffer.addSample(0.0, new Pose2d(0, 0, new Rotation2d(0)));
    buffer.addSample(1.0, new Pose2d(4, 0, new Rotation2d(0)));

    Pose2d mid = buffer.getSample(0.5);
    assertNotNull(mid);
    assertEquals(2.0, mid.getX(), EPSILON);
  }
}
