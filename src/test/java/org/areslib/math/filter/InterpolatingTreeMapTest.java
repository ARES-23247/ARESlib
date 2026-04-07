package org.areslib.math.filter;

import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InterpolatingTreeMap}.
 */
class InterpolatingTreeMapTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("get returns null on empty map")
    void getEmptyMap() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        assertNull(map.get(1.0));
    }

    @Test
    @DisplayName("get returns exact value when key exists")
    void getExactKey() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(1.0, new Translation2d(10, 20));
        Translation2d result = map.get(1.0);
        assertNotNull(result);
        assertEquals(10.0, result.getX(), EPSILON);
        assertEquals(20.0, result.getY(), EPSILON);
    }

    @Test
    @DisplayName("get interpolates between two entries")
    void getInterpolates() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(0.0, new Translation2d(0, 0));
        map.put(10.0, new Translation2d(100, 200));
        
        Translation2d result = map.get(5.0);
        assertEquals(50.0, result.getX(), EPSILON);
        assertEquals(100.0, result.getY(), EPSILON);
    }

    @Test
    @DisplayName("get clamps below minimum key")
    void getClampsBelowMin() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(5.0, new Translation2d(10, 20));
        map.put(10.0, new Translation2d(100, 200));
        
        Translation2d result = map.get(0.0); // Below all keys
        // Should return the lowest value
        assertEquals(10.0, result.getX(), EPSILON);
        assertEquals(20.0, result.getY(), EPSILON);
    }

    @Test
    @DisplayName("get clamps above maximum key")
    void getClampsAboveMax() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(0.0, new Translation2d(10, 20));
        map.put(5.0, new Translation2d(100, 200));
        
        Translation2d result = map.get(99.0);
        assertEquals(100.0, result.getX(), EPSILON);
        assertEquals(200.0, result.getY(), EPSILON);
    }

    @Test
    @DisplayName("Map evicts oldest entries when full")
    void evictsOldest() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(3);
        map.put(1.0, new Translation2d(10, 0));
        map.put(2.0, new Translation2d(20, 0));
        map.put(3.0, new Translation2d(30, 0));
        map.put(4.0, new Translation2d(40, 0)); // Should evict key=1.0
        
        // Getting key 1.0 should now clamp to key 2.0 (lowest remaining)
        Translation2d result = map.get(1.0);
        assertEquals(20.0, result.getX(), EPSILON);
    }

    @Test
    @DisplayName("clear empties the map")
    void clear() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(1.0, new Translation2d(10, 20));
        map.clear();
        assertNull(map.get(1.0));
    }

    @Test
    @DisplayName("Handles duplicate timestamps by updating")
    void duplicateTimestamp() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(1.0, new Translation2d(10, 20));
        map.put(1.0, new Translation2d(99, 99));
        Translation2d result = map.get(1.0);
        assertEquals(99.0, result.getX(), EPSILON);
    }

    @Test
    @DisplayName("Identical floor and ceiling returns lower value")
    void identicalFloorCeiling() {
        InterpolatingTreeMap<Double, Translation2d> map = new InterpolatingTreeMap<>(10);
        map.put(5.0, new Translation2d(10, 20));
        map.put(5.0, new Translation2d(30, 40));
        // Both floor and ceiling are the same entry
        Translation2d result = map.get(5.0);
        assertEquals(30.0, result.getX(), EPSILON);
    }
}
