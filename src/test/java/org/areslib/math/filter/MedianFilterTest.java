package org.areslib.math.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MedianFilter}.
 */
class MedianFilterTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Single-sample filter is pass-through")
    void singleSample() {
        MedianFilter filter = new MedianFilter(1);
        assertEquals(5.0, filter.calculate(5.0), EPSILON);
        assertEquals(10.0, filter.calculate(10.0), EPSILON);
    }

    @Test
    @DisplayName("Odd window returns middle value")
    void oddWindowMedian() {
        MedianFilter filter = new MedianFilter(3);
        filter.calculate(1.0);
        filter.calculate(100.0);
        double result = filter.calculate(2.0);
        // Sorted: [1, 2, 100], median = 2
        assertEquals(2.0, result, EPSILON);
    }

    @Test
    @DisplayName("Even window returns average of two middle values")
    void evenWindowMedian() {
        MedianFilter filter = new MedianFilter(4);
        filter.calculate(1.0);
        filter.calculate(3.0);
        filter.calculate(5.0);
        double result = filter.calculate(7.0);
        // Sorted: [1, 3, 5, 7], median = (3+5)/2 = 4.0
        assertEquals(4.0, result, EPSILON);
    }

    @Test
    @DisplayName("Spike rejection")
    void spikeRejection() {
        MedianFilter filter = new MedianFilter(5);
        filter.calculate(10.0);
        filter.calculate(10.0);
        filter.calculate(10.0);
        filter.calculate(10.0);
        // Large spike
        double result = filter.calculate(1000.0);
        // Window: [10, 10, 10, 10, 1000], sorted median = 10
        assertEquals(10.0, result, EPSILON);
    }

    @Test
    @DisplayName("Window slides correctly")
    void windowSlides() {
        MedianFilter filter = new MedianFilter(3);
        filter.calculate(1.0);
        filter.calculate(2.0);
        filter.calculate(3.0);
        double result = filter.calculate(100.0);
        // Window: [2, 3, 100], sorted median = 3
        assertEquals(3.0, result, EPSILON);
    }

    @Test
    @DisplayName("Reset clears all samples")
    void reset() {
        MedianFilter filter = new MedianFilter(3);
        filter.calculate(100.0);
        filter.calculate(100.0);
        filter.calculate(100.0);
        filter.reset();
        assertEquals(1.0, filter.calculate(1.0), EPSILON);
    }

    @Test
    @DisplayName("Constructor throws on non-positive size")
    void invalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new MedianFilter(0));
        assertThrows(IllegalArgumentException.class, () -> new MedianFilter(-1));
    }
}
