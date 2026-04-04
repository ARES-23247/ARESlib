package org.areslib.hardware.wrappers;

import org.areslib.hardware.sensors.AresAbsoluteEncoder;
import java.util.function.DoubleSupplier;

/**
 * Wrapper for the REV Through Bore Encoder used in Absolute Mode (PWM).
 * Since standard FTC Control Hubs cannot natively read high-frequency PWM inputs,
 * this wrapper expects to receive the pulse width (in microseconds) as determined
 * by a device like an OctoQuad, SRS Hub, or custom hardware interface.
 */
public class RevEncoderWrapper implements AresAbsoluteEncoder {
    
    public enum EncoderVersion {
        V1(1.0, 1024.0),
        V2(3.884, 998.06);

        public final double minPulseUs;
        public final double maxPulseUs;

        EncoderVersion(double minPulseUs, double maxPulseUs) {
            this.minPulseUs = minPulseUs;
            this.maxPulseUs = maxPulseUs;
        }
    }

    // The DoubleSupplier provides the pulse width reading in microseconds
    private final DoubleSupplier pulseWidthSubmicrosecondSupplier;
    private final EncoderVersion version;
    private final boolean inverted;
    private double offset = 0.0; // Radians

    /**
     * Creates a new REV Through Bore absolute encoder wrapper reading from a PWM source.
     * @param pulseWidthUsSupplier A supplier returning the PWM pulse width in microseconds (e.g., from an OctoQuad).
     * @param version The version of the REV Through Bore Encoder (V1 or V2).
     */
    public RevEncoderWrapper(DoubleSupplier pulseWidthUsSupplier, EncoderVersion version) {
        this(pulseWidthUsSupplier, version, false);
    }

    /**
     * Creates a new REV Through Bore absolute encoder wrapper reading from a PWM source.
     * @param pulseWidthUsSupplier A supplier returning the PWM pulse width in microseconds.
     * @param version The version of the REV Through Bore Encoder (V1 or V2).
     * @param inverted Whether the encoder direction should be inverted.
     */
    public RevEncoderWrapper(DoubleSupplier pulseWidthUsSupplier, EncoderVersion version, boolean inverted) {
        this.pulseWidthSubmicrosecondSupplier = pulseWidthUsSupplier;
        this.version = version;
        this.inverted = inverted;
    }

    /**
     * Sets the zero offset for the encoder.
     * @param offsetRadians The offset in radians.
     */
    @Override
    public void setOffset(double offsetRadians) {
        this.offset = offsetRadians;
    }

    @Override
    public void setDistancePerPulse(double distance) {
        // Absolute encoders generally don't use this as output is already radians
    }

    @Override
    public double getAbsolutePositionRad() {
        // Fetch raw pulse width in microseconds
        double rawPulseUs = pulseWidthSubmicrosecondSupplier.getAsDouble();
        
        // Ensure constraints in case sensor bounces
        if (rawPulseUs < version.minPulseUs) rawPulseUs = version.minPulseUs;
        if (rawPulseUs > version.maxPulseUs) rawPulseUs = version.maxPulseUs;

        // The absolute position scales linearly from the minimum pulse to the maximum pulse
        double range = version.maxPulseUs - version.minPulseUs;
        double normalizedPos = (rawPulseUs - version.minPulseUs) / range;
        
        // Convert to radians (0 to 2PI)
        double positionRadians = normalizedPos * 2 * Math.PI;

        if (inverted) {
            positionRadians = (2 * Math.PI) - positionRadians;
        }

        // Apply offset and wrap between 0 and 2PI
        double adjustedPos = (positionRadians - offset) % (2 * Math.PI);
        if (adjustedPos < 0) {
            adjustedPos += 2 * Math.PI;
        }
        
        return adjustedPos;
    }

    @Override
    public double getPosition() {
        return getAbsolutePositionRad();
    }

    @Override
    public double getVelocity() {
        // Pure absolute encoders generally don't accurately measure velocity natively
        // Velocity must be calculated via delta pos / delta time externally
        return 0.0;
    }
}
