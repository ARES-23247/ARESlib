package org.areslib.math.filter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KalmanFilter1DTest {

    @Test
    void testConstantVelocityTracking() {
        // True system: moves at 5.0 units/sec
        // Initial state at 0
        KalmanFilter1D filter = new KalmanFilter1D(0.01, 0.1);
        filter.reset(0, 0);

        double truePos = 0.0;
        double trueVel = 5.0;
        double dt = 0.02;

        for (int i = 0; i < 200; i++) {
            truePos += trueVel * dt;
            
            // Generate noisy fake measurements
            double simulatedPosNoise = (Math.random() - 0.5) * 0.5; // +/- 0.25m noise
            double measuredPos = truePos + simulatedPosNoise;

            filter.predict(dt);
            // We feed it a noisy position, variance ~ 0.1
            filter.updatePosition(measuredPos, 0.1);
        }

        // After 200 iterations (4 seconds), it should have inferred the true velocity
        // very closely despite only receiving noisy position measurements.
        assertEquals(trueVel, filter.getVelocity(), 0.5, "Velocity should converge near true velocity");
        assertEquals(truePos, filter.getPosition(), 0.5, "Position should converge near true position");
    }
}
