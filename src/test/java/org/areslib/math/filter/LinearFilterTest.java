package org.areslib.math.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LinearFilter} (moving average).
 */
class LinearFilterTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Single-sample filter is pass-through")
    void singleSampleFilter() {
        LinearFilter filter = LinearFilter.movingAverage(1);
        assertEquals(5.0, filter.calculate(5.0), EPSILON);
        assertEquals(10.0, filter.calculate(10.0), EPSILON);
    }

    @Test
    @DisplayName("Moving average of constant value is that value")
    void constantValue() {
        LinearFilter filter = LinearFilter.movingAverage(5);
        for (int i = 0; i < 10; i++) {
            filter.calculate(42.0);
        }
        assertEquals(42.0, filter.calculate(42.0), EPSILON);
    }

    @Test
    @DisplayName("Moving average converges")
    void movingAverageConverges() {
        LinearFilter filter = LinearFilter.movingAverage(3);
        filter.calculate(1.0);
        filter.calculate(2.0);
        double result = filter.calculate(3.0);
        assertEquals(2.0, result, EPSILON); // (1+2+3)/3 = 2.0
    }

    @Test
    @DisplayName("Moving average window slides correctly")
    void windowSlides() {
        LinearFilter filter = LinearFilter.movingAverage(3);
        filter.calculate(1.0);
        filter.calculate(2.0);
        filter.calculate(3.0);
        double result = filter.calculate(6.0);
        assertEquals((2.0 + 3.0 + 6.0) / 3.0, result, EPSILON);
    }

    @Test
    @DisplayName("Reset clears all samples")
    void reset() {
        LinearFilter filter = LinearFilter.movingAverage(3);
        filter.calculate(100.0);
        filter.calculate(100.0);
        filter.calculate(100.0);
        filter.reset();
        assertEquals(1.0, filter.calculate(1.0), EPSILON);
    }

    @Test
    @DisplayName("Constructor throws on non-positive size")
    void invalidSize() {
        assertThrows(IllegalArgumentException.class, () -> LinearFilter.movingAverage(0));
        assertThrows(IllegalArgumentException.class, () -> LinearFilter.movingAverage(-1));
    }
}
